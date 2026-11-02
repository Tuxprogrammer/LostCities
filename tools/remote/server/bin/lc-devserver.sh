#!/usr/bin/env bash
# lc-devserver.sh <slot> [--timeout <s>] [--cmds '<cmd>; <cmd>; wait:<s>; until:<regex>']
# Headless dev-server check: runs `gradlew runServer` in wt/<slot> through lc-build.sh (port from
# LC_DEV_PORT, default 25810), waits for "Done (", sends --cmds, then "stop", and waits for Gradle to
# exit. Kills only the build it started (lc-build.sh's process group) if boot or stop times out.
# Writes logs/devserver-<slot>-<stamp>.log and logs/devserver-<slot>.done (status, boot_seconds, ...).
set -uo pipefail
LC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
slot="${1:?usage: lc-devserver.sh <slot> [--timeout <s>] [--cmds '...']}"; shift
TIMEOUT=900 CMDS=""
while [ $# -gt 0 ]; do
    case "$1" in
        --timeout) TIMEOUT="$2"; shift 2 ;;
        --cmds) CMDS="$2"; shift 2 ;;
        *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
done
[[ "$slot" =~ ^[A-Za-z0-9_.-]+$ ]] || { echo "bad slot name: $slot" >&2; exit 2; }
stamp="$(date +%Y%m%d-%H%M%S)"
log="$LC/logs/devserver-$slot-$stamp.log"
done_f="$LC/logs/devserver-$slot.done"
fifo="$LC/logs/devserver-$slot.fifo"
rm -f "$done_f" "$fifo"; mkfifo "$fifo"
exec 3<>"$fifo"
T0=$(date +%s) STATUS=error BOOT_S="" RC=""
LC_STDIN="$fifo" LC_LOG="$log" LC_BUILD_TIMEOUT=$(( TIMEOUT + 3600 )) "$LC/bin/lc-build.sh" "$slot" runServer > /dev/null 2>&1 &
bpid=$!
alive() { kill -0 "$bpid" 2>/dev/null; }
finish() {
    if alive; then STATUS="$STATUS+killed"; kill -TERM "$bpid" 2>/dev/null; for _ in $(seq 1 60); do alive || break; sleep 1; done; fi
    wait "$bpid" 2>/dev/null; [ -z "$RC" ] && RC=$?
    exec 3>&-; rm -f "$fifo"
    printf 'status=%s\nboot_seconds=%s\nwall_seconds=%s\nexit=%s\nlog=%s\n' "$STATUS" "$BOOT_S" "$(( $(date +%s) - T0 ))" "$RC" "$log" | tee "$done_f"
    [ "$STATUS" = ok ]
}
trap 'finish; exit $?' EXIT
trap 'STATUS=interrupted; exit 130' INT TERM HUP
until grep -q 'Done (' "$log" 2>/dev/null; do
    alive || { STATUS=died-during-boot; exit 1; }
    [ "$(date +%s)" -ge $(( T0 + TIMEOUT )) ] && { STATUS=boot-timeout; exit 1; }
    sleep 2
done
BOOT_S=$(( $(date +%s) - T0 )); echo "dev server up after ${BOOT_S}s"
IFS=';' read -r -a parts <<< "$CMDS"
for c in "${parts[@]}"; do
    c="$(echo "$c" | sed 's/^ *//; s/ *$//')"; [ -z "$c" ] && continue
    case "$c" in
        wait:*) sleep "${c#wait:}" ;;
        until:*) re="${c#until:}"; while alive && ! grep -aqE "$re" "$log"; do sleep 5; done ;;
        *) echo ">> $c"; printf '%s\n' "$c" >&3; sleep 2 ;;
    esac
done
printf 'stop\n' >&3
STATUS=stop-timeout
for _ in $(seq 1 180); do alive || break; sleep 1; done
if ! alive; then wait "$bpid"; RC=$?; [ "$RC" = 0 ] && STATUS=ok || STATUS=gradle-failed; fi
