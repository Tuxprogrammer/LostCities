#!/usr/bin/env bash
# Boot the real GTNH 2.9.0 dedicated server headless, optionally with a Lost Cities: New Horizons jar,
# run console commands once it is up, stop it cleanly, and write a run.done marker. See README.txt.
#
# usage: run.sh [--mod <jar>] [--jvm '<extra jvm args>'] [--cmds '<cmd1>; <cmd2>; ...']
#               [--fresh] [--world <folder>] [--level-type <type>] [--seed <seed>]
#               [--timeout <s>] [--cmd-wait <s>] [--run-timeout <s>]
#               [--set-config '<file under config/>:<key>=<value>'] (repeatable; restored after the run)
#               [--view-distance <3-15>] (server.properties view-distance for this run; restored after it)
# In --cmds, 'wait:<s>' sleeps s seconds and 'until:<regex>' waits (polling every 5 s, at most
# --run-timeout seconds) for a console line matching the extended regex; neither is sent to the server.
set -u

RP="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LC="$(dirname "$RP")"
SRV="$RP/server"
JAVA="${REALPACK_JAVA:-$LC/shared/jdk21/bin/java}"
HEAP="${REALPACK_HEAP:-8G}"
PACK_LEVEL_TYPE=rwg      # what the GTNH 2.9.0 server zip ships in server.properties (classic RWG)
export JAVA_TOOL_OPTIONS="-Duser.home=$LC/shared/home"

MOD="" JVM_EXTRA="" CMDS="" TIMEOUT=900 FRESH=0 CMD_WAIT=2 RUN_TIMEOUT=21600 UNTIL="" WORLD=""
LEVEL_TYPE="$PACK_LEVEL_TYPE" SEED="" VIEW_DISTANCE="" ORIG_VIEW_DISTANCE=""
SETCFG=() RESTORE=()
while [ $# -gt 0 ]; do
    case "$1" in
        --mod) MOD="$2"; shift 2 ;;
        --jvm) JVM_EXTRA="$2"; shift 2 ;;
        --cmds) CMDS="$2"; shift 2 ;;
        --timeout) TIMEOUT="$2"; shift 2 ;;
        --cmd-wait) CMD_WAIT="$2"; shift 2 ;;
        --run-timeout) RUN_TIMEOUT="$2"; shift 2 ;;
        --fresh) FRESH=1; shift ;;
        --world) WORLD="$2"; shift 2 ;;
        --level-type) LEVEL_TYPE="$2"; shift 2 ;;
        --seed) SEED="$2"; shift 2 ;;
        --set-config) SETCFG+=("$2"); shift 2 ;;
        --view-distance) VIEW_DISTANCE="$2"; shift 2 ;;
        -h|--help) sed -n '2,11p' "$0"; exit 0 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done
if [ -n "$WORLD" ] && ! [[ "$WORLD" =~ ^[A-Za-z0-9_-]+$ ]]; then echo "bad --world: $WORLD" >&2; exit 2; fi
if ! [[ "$LEVEL_TYPE" =~ ^[A-Za-z0-9_]+$ ]]; then echo "bad --level-type: $LEVEL_TYPE" >&2; exit 2; fi
if [ -n "$SEED" ] && ! [[ "$SEED" =~ ^-?[A-Za-z0-9_]+$ ]]; then echo "bad --seed: $SEED" >&2; exit 2; fi
if [ -n "$VIEW_DISTANCE" ] && { ! [[ "$VIEW_DISTANCE" =~ ^[0-9]+$ ]] || [ "$VIEW_DISTANCE" -lt 3 ] || [ "$VIEW_DISTANCE" -gt 15 ]; }; then
    echo "bad --view-distance: $VIEW_DISTANCE (3-15)" >&2; exit 2
fi

# One run at a time: the server dir, port and world are shared. Our own lock, in our own dir.
exec 9>"$RP/run.lock"
if ! flock -n 9; then echo "another run.sh holds $RP/run.lock" >&2; exit 3; fi

RUN_ID="$(date +%Y%m%d-%H%M%S)"
RUN_DIR="$RP/runs/$RUN_ID"
mkdir -p "$RUN_DIR"
CONSOLE="$RUN_DIR/console.log"
FIFO="$RUN_DIR/stdin.fifo"
DONE="$RP/run.done"
rm -f "$DONE"

PID="" STATUS="error" EXIT_CODE="" BOOT_S="" PEAK_KB=0 T0="$(date +%s)"
WORLD_DIR="${WORLD:-$(sed -n 's/^level-name=//p' "$SRV/server.properties")}"
WORLD_DIR="${WORLD_DIR:-World}"

sample_rss() {
    local hwm
    hwm="$(awk '/^VmHWM:/ {print $2}' "/proc/$PID/status" 2>/dev/null)"
    [ -n "$hwm" ] && [ "$hwm" -gt "$PEAK_KB" ] && PEAK_KB="$hwm"
}
alive() { [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; }

finish() {
    # Never leave the JVM we started running; touch no other process.
    if alive; then
        STATUS="${STATUS}+killed"
        kill -TERM "$PID" 2>/dev/null
        for _ in $(seq 1 30); do alive || break; sleep 1; done
        alive && kill -KILL "$PID" 2>/dev/null
    fi
    [ -n "$PID" ] && wait "$PID" 2>/dev/null
    exec 3>&- 2>/dev/null
    # Put back every config file --set-config changed.
    for f in "${RESTORE[@]}"; do
        cp "$RUN_DIR/cfg-backup/$f" "$SRV/config/$f" 2>/dev/null
    done
    rm -f "$FIFO"
    # Leave server.properties at the pack's values between runs.
    sed -i "s|^level-type=.*|level-type=$PACK_LEVEL_TYPE|; s|^level-seed=.*|level-seed=|" "$SRV/server.properties"
    [ -n "$ORIG_VIEW_DISTANCE" ] && sed -i "s|^view-distance=.*|view-distance=$ORIG_VIEW_DISTANCE|" "$SRV/server.properties"
    for f in fml-server-latest.log latest.log; do
        [ -f "$SRV/logs/$f" ] && cp "$SRV/logs/$f" "$RUN_DIR/" 2>/dev/null
    done
    ls "$SRV"/crash-reports/*.txt 2>/dev/null | while read -r c; do
        [ "$c" -nt "$RUN_DIR/stamp" ] && cp "$c" "$RUN_DIR/"
    done
    {
        echo "status=$STATUS"
        echo "exit_code=$EXIT_CODE"
        echo "run_id=$RUN_ID"
        echo "run_dir=$RUN_DIR"
        echo "log=$RUN_DIR/console.log"
        echo "fml_log=$RUN_DIR/fml-server-latest.log"
        echo "boot_seconds=$BOOT_S"
        echo "peak_rss_kb=$PEAK_KB"
        echo "wall_seconds=$(( $(date +%s) - T0 ))"
        echo "mod=${MOD:-none}"
        echo "jvm_extra=$JVM_EXTRA"
        echo "until=${UNTIL:-none}"
        echo "world=$WORLD_DIR"
        echo "fresh=$FRESH"
        echo "level_type=$LEVEL_TYPE"
        echo "seed=${SEED:-random}"
        echo "view_distance=${VIEW_DISTANCE:-unchanged}"
    } | tee "$RUN_DIR/run.done" > "$DONE"
    cat "$DONE"
    [ "$STATUS" = ok ] || exit 1
}
trap finish EXIT
trap 'STATUS="interrupted"; exit 130' INT TERM HUP

touch "$RUN_DIR/stamp"

# Our jars never ship in the pack: clear old ones so every run is explicit.
rm -f "$SRV"/mods/lostcitiesnh-*.jar
if [ -n "$MOD" ]; then
    [ -f "$MOD" ] || { echo "no such jar: $MOD" >&2; STATUS="bad-mod"; exit 2; }
    cp "$MOD" "$SRV/mods/lostcitiesnh-$(basename "$MOD" | sed 's/^lostcitiesnh-//')"
fi
if [ "$FRESH" = 1 ]; then rm -rf "${SRV:?}/$WORLD_DIR"; fi
if [ -f "$SRV/$WORLD_DIR/level.dat" ] && [ "$LEVEL_TYPE" != "$PACK_LEVEL_TYPE" ]; then
    echo "note: $WORLD_DIR already exists; level-type only applies to a new world (use --fresh)" >&2
fi
# level-type and level-seed are per run: set from the flags here, put back to the pack's values in finish().
sed -i "s|^level-type=.*|level-type=$LEVEL_TYPE|; s|^level-seed=.*|level-seed=$SEED|" "$SRV/server.properties"
# view-distance likewise, only when asked for: the old value is kept and put back in finish().
if [ -n "$VIEW_DISTANCE" ]; then
    ORIG_VIEW_DISTANCE="$(sed -n 's/^view-distance=//p' "$SRV/server.properties" | tail -1)"
    ORIG_VIEW_DISTANCE="${ORIG_VIEW_DISTANCE:-6}"
    sed -i "s|^view-distance=.*|view-distance=$VIEW_DISTANCE|" "$SRV/server.properties"
    echo "view-distance=$VIEW_DISTANCE for this run (was $ORIG_VIEW_DISTANCE)"
fi
# Per-run config overrides: '<file under config/>:<key>=<value>', backed up first and restored in finish().
for spec in "${SETCFG[@]}"; do
    f="${spec%%:*}"; kv="${spec#*:}"; key="${kv%%=*}"; val="${kv#*=}"
    if ! [[ "$f" =~ ^[A-Za-z0-9_./-]+$ ]] || [[ "$f" == *..* ]] || ! [[ "$key" =~ ^[A-Za-z0-9_]+$ ]] \
        || ! [[ "$val" =~ ^[A-Za-z0-9_.-]+$ ]] || [ ! -f "$SRV/config/$f" ]; then
        echo "bad --set-config: $spec" >&2; STATUS="error"; exit 2
    fi
    if [ ! -f "$RUN_DIR/cfg-backup/$f" ]; then
        mkdir -p "$(dirname "$RUN_DIR/cfg-backup/$f")"
        cp "$SRV/config/$f" "$RUN_DIR/cfg-backup/$f"
        RESTORE+=("$f")
    fi
    sed -i "s|^\([[:space:]]*[A-Z]:$key=\).*|\1$val|" "$SRV/config/$f"
    grep -q "^[[:space:]]*[A-Z]:$key=$val\$" "$SRV/config/$f" || { echo "--set-config: no key $key in $f" >&2; STATUS="error"; exit 2; }
    echo "config: $f $key=$val (for this run)"
done
WORLD_ARGS=""
[ -n "$WORLD" ] && WORLD_ARGS="--world $WORLD"

mkfifo "$FIFO"
exec 3<>"$FIFO"   # held read-write so the server's stdin never sees EOF and opening never blocks

cd "$SRV" || exit 1
# shellcheck disable=SC2086
"$JAVA" -Xms"$HEAP" -Xmx"$HEAP" -Dfml.readTimeout=180 -Duser.language=en -Dfml.queryResult=confirm \
    $JVM_EXTRA @java9args.txt -jar lwjgl3ify-forgePatches.jar nogui $WORLD_ARGS < "$FIFO" > "$CONSOLE" 2>&1 &
PID=$!
echo "$PID" > "$RUN_DIR/server.pid"
echo "run $RUN_ID: started pid $PID, console $CONSOLE"

deadline=$(( T0 + TIMEOUT ))
until grep -q 'Done (' "$CONSOLE" 2>/dev/null; do
    if ! alive; then STATUS="died-during-boot"; wait "$PID"; EXIT_CODE=$?; PID=""; exit 1; fi
    if [ "$(date +%s)" -ge "$deadline" ]; then STATUS="boot-timeout"; exit 1; fi
    sample_rss; sleep 1
done
BOOT_S=$(( $(date +%s) - T0 ))
echo "server up after ${BOOT_S}s"

if [ -n "$CMDS" ]; then
    IFS=';' read -r -a parts <<< "$CMDS"
    for c in "${parts[@]}"; do
        c="$(echo "$c" | sed 's/^ *//; s/ *$//')"
        [ -z "$c" ] && continue
        case "$c" in
            wait:*)
                echo ">> (wait ${c#wait:} s)"
                for _ in $(seq 1 "${c#wait:}"); do alive || break; sample_rss; sleep 1; done
                continue ;;
            until:*)
                re="${c#until:}"; udl=$(( $(date +%s) + RUN_TIMEOUT )); echo ">> (until /$re/)"
                UNTIL="timeout"
                while alive && [ "$(date +%s)" -lt "$udl" ]; do
                    if grep -aqE "$re" "$CONSOLE"; then UNTIL="matched"; break; fi
                    sample_rss; sleep 5
                done
                alive || UNTIL="died"
                echo ">> until: $UNTIL"
                continue ;;
        esac
        echo ">> $c"
        printf '%s\n' "$c" >&3
        for _ in $(seq 1 "$CMD_WAIT"); do sample_rss; sleep 1; done
    done
fi

sample_rss
printf 'stop\n' >&3
STATUS="stop-timeout"
for _ in $(seq 1 180); do
    alive || break
    sample_rss; sleep 1
done
if ! alive; then
    wait "$PID"; EXIT_CODE=$?; PID=""
    STATUS="ok"
fi
exit 0
