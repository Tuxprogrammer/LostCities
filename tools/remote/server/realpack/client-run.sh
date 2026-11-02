#!/usr/bin/env bash
# client-run.sh [--display <80-89>] [--shot <name>] [--wait <s>] [--timeout <s>] [--server <host:port>]
#               [--user <name>] [--heap <size>] [--extra-shot <name>:<seconds>] (repeatable)
#               [--mod <jar>] (a lostcitiesnh jar for this run; servers with it reject clients without it)
# Launch the real GTNH 2.9.0 client (client/, built by client-prepare.py) headless on our own Xvfb
# display with Mesa software GL (llvmpipe), wait until the main menu is up (or, with --server, until
# the client has joined), wait --wait more seconds, screenshot the display into
# runs/client-<id>/<shot>.png, then stop only the JVM and Xvfb this script started.
# Offline account (--user, default LCBot): it can only join an online-mode=false server.
set -u
LC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RP="$LC/realpack"
CL="$RP/client"
. "$LC/env.sh"
DISP=80 SHOT=menu WAIT=20 TIMEOUT=1200 SERVER="" USER_NAME=LCBot HEAP=8G
EXTRA=()
MOD=""
while [ $# -gt 0 ]; do
    case "$1" in
        --display) DISP="$2"; shift 2 ;;
        --shot) SHOT="$2"; shift 2 ;;
        --wait) WAIT="$2"; shift 2 ;;
        --timeout) TIMEOUT="$2"; shift 2 ;;
        --server) SERVER="$2"; shift 2 ;;
        --user) USER_NAME="$2"; shift 2 ;;
        --heap) HEAP="$2"; shift 2 ;;
        --extra-shot) EXTRA+=("$2"); shift 2 ;;
        --mod) MOD="$2"; shift 2 ;;
        -h|--help) sed -n '2,9p' "$0"; exit 0 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done
[[ "$SHOT" =~ ^[A-Za-z0-9_.-]+$ ]] || { echo "bad --shot: $SHOT" >&2; exit 2; }
[[ "$USER_NAME" =~ ^[A-Za-z0-9_]{3,16}$ ]] || { echo "bad --user: $USER_NAME" >&2; exit 2; }
[ -f "$CL/launch.json" ] || { echo "run client-prepare.py first" >&2; exit 2; }
exec 7>"$RP/client.lock"; flock -n 7 || { echo "another client-run.sh is running" >&2; exit 3; }

RUN_ID="client-$(date +%Y%m%d-%H%M%S)"
RUN_DIR="$RP/runs/$RUN_ID"; mkdir -p "$RUN_DIR" "$CL/tmp"
LOG="$RUN_DIR/client.log"; touch "$RUN_DIR/stamp"
T0=$(date +%s) STATUS=error PID="" XPID="" READY_S="" PEAK_KB=0

alive() { [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; }
sample_rss() { local h; h="$(awk '/^VmHWM:/ {print $2}' "/proc/$PID/status" 2>/dev/null)"; [ -n "$h" ] && [ "$h" -gt "$PEAK_KB" ] && PEAK_KB="$h"; }
shoot() { xwd -root -silent -display ":$DISP" 2>/dev/null | convert xwd:- "$RUN_DIR/$1.png" 2>/dev/null; }
finish() {
    if alive; then kill -TERM "$PID" 2>/dev/null; for _ in $(seq 1 20); do alive || break; sleep 1; done; alive && kill -KILL "$PID"; fi
    [ -n "$PID" ] && wait "$PID" 2>/dev/null
    lc_xvfb_stop "$DISP" >/dev/null 2>&1
    rm -f "$CL"/game/mods/lostcitiesnh-*.jar
    cp "$CL/game/logs/fml-client-latest.log" "$RUN_DIR/" 2>/dev/null
    ls "$CL"/game/crash-reports/*.txt 2>/dev/null | while read -r c; do [ "$c" -nt "$RUN_DIR/stamp" ] && cp "$c" "$RUN_DIR/"; done
    printf 'status=%s\nready_seconds=%s\npeak_rss_kb=%s\nwall_seconds=%s\nrun_dir=%s\nshot=%s\n' \
        "$STATUS" "$READY_S" "$PEAK_KB" "$(( $(date +%s) - T0 ))" "$RUN_DIR" "$RUN_DIR/$SHOT.png" | tee "$RUN_DIR/run.done"
    [ "$STATUS" = ok ]
}
trap 'finish; exit $?' EXIT
trap 'STATUS=interrupted; exit 130' INT TERM HUP

# Our jars never stay in the client: clear old ones, install the one for this run.
rm -f "$CL"/game/mods/lostcitiesnh-*.jar
if [ -n "$MOD" ]; then
    [ -f "$MOD" ] || { echo "no such jar: $MOD" >&2; STATUS=bad-mod; exit 2; }
    cp "$MOD" "$CL/game/mods/lostcitiesnh-$(basename "$MOD" | sed 's/^lostcitiesnh-//')"
fi
lc_xvfb "$DISP" || { STATUS=xvfb-failed; exit 1; }
export DISPLAY=":$DISP" LIBGL_ALWAYS_SOFTWARE=1 GALLIUM_DRIVER=llvmpipe __GLX_VENDOR_LIBRARY_NAME=mesa ALSOFT_DRIVERS=null

# Build the java command line from launch.json.
mapfile -t ARGS < <(python3 -I - "$CL" "$USER_NAME" "$SERVER" <<'PY'
import json, sys, uuid, hashlib
cl, user, server = sys.argv[1], sys.argv[2], sys.argv[3]
d = json.load(open(cl + "/launch.json"))
off = uuid.UUID(bytes=hashlib.md5(("OfflinePlayer:" + user).encode()).digest(), version=3).hex
sub = {"auth_player_name": user, "version_name": "1.7.10", "game_directory": cl + "/game", "assets_root": cl + "/assets",
       "assets_index_name": d["assetIndex"], "auth_uuid": off, "auth_access_token": "0", "user_properties": "{}", "user_type": "legacy"}
out = d["jvmArgs"] + ["-Djava.library.path=" + cl + "/natives", "-Djava.io.tmpdir=" + cl + "/tmp",
       "-Dorg.lwjgl.system.SharedLibraryExtractPath=" + cl + "/tmp/lwjgl", "-cp", ":".join(d["classpath"]), d["mainClass"]]
for a in d["gameArgs"].split():
    if a.startswith("${") and a.endswith("}"): a = sub[a[2:-1]]
    out.append(a)
for t in d["tweakers"]: out += ["--tweakClass", t]
out += ["--width", "1600", "--height", "900"]
if server:
    h, _, p = server.partition(":"); out += ["--server", h, "--port", p or "25565"]
print("\n".join(out))
PY
)
cd "$CL/game" || exit 1
"$JAVA_HOME/bin/java" -Xms"$HEAP" -Xmx"$HEAP" "${ARGS[@]}" > "$LOG" 2>&1 < /dev/null &
PID=$!; echo "$PID" > "$RUN_DIR/client.pid"
echo "$RUN_ID: client pid $PID on :$DISP, log $LOG"

# Ready: the main menu, i.e. the texture atlases rebuilt after "Forge Mod Loader has successfully loaded"
# (vanilla's first sound-engine start and atlas come before mod loading and do not count); with
# --server, the first chat line after joining.
menu_ready() { awk '/Forge Mod Loader has successfully loaded/ {f=1} f && /Created: [0-9]+x[0-9]+ textures\/items-atlas/ {ok=1} END {exit !ok}' "$LOG"; }
joined() { grep -aqE '\[CHAT\]|joined the game' "$LOG"; }
STATUS=timeout
while [ "$(date +%s)" -lt $(( T0 + TIMEOUT )) ]; do
    if ! alive; then STATUS=died; break; fi
    if { [ -z "$SERVER" ] && menu_ready; } || { [ -n "$SERVER" ] && joined; }; then READY_S=$(( $(date +%s) - T0 )); STATUS=ok; break; fi
    sample_rss; sleep 5
done
if [ "$STATUS" = ok ]; then for _ in $(seq 1 "$WAIT"); do alive || break; sample_rss; sleep 1; done; fi
shoot "$SHOT"
# Extra screenshots, each <seconds> after the one before, for scripted viewpoints the server moves the player to.
for spec in "${EXTRA[@]}"; do
    name="${spec%%:*}"; secs="${spec#*:}"
    [[ "$name" =~ ^[A-Za-z0-9_.-]+$ ]] && [[ "$secs" =~ ^[0-9]+$ ]] || { echo "bad --extra-shot: $spec" >&2; continue; }
    for _ in $(seq 1 "$secs"); do alive || break; sample_rss; sleep 1; done
    alive && shoot "$name" && echo "extra shot: $RUN_DIR/$name.png"
done
grep -aq "Minecraft Crash Report\|A fatal error" "$LOG" && STATUS="$STATUS+crash"
exit 0
