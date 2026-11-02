#!/usr/bin/env bash
# lc-build.sh <slot> [gradle args...]       default args: spotlessCheck build
# Runs ./gradlew --no-daemon in <root>/wt/<slot> with our env.sh, logging to
# <root>/logs/build-<slot>-<stamp>.log (and logs/build-<slot>-latest.log).
# No daemon: every build is one JVM that exits with the build, so nothing is left running on the
# shared box. Gradle's own heap is 4g (GRADLE_USER_HOME/gradle.properties); forked test and
# compile JVMs are capped by the buildscript. Exit status is Gradle's.
#
# Dev runs (runServer, runClient) use the slot's run/ dir; lc-build.sh writes the dev server's
# server.properties there first, with server-port from LC_DEV_PORT (default 25810, range 25810-25899).
# LC_BUILD_TIMEOUT (seconds, default 3600) kills the build (exit 124) after that long.
# LC_STDIN=<file or fifo> feeds Gradle's stdin (default /dev/null; lc-devserver.sh uses a fifo), and
# LC_LOG=<path> overrides the log path.
set -uo pipefail
LC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
. "$LC/env.sh"
slot="${1:?usage: lc-build.sh <slot> [gradle args...]}"; shift
[[ "$slot" =~ ^[A-Za-z0-9_.-]+$ ]] || { echo "bad slot name: $slot" >&2; exit 2; }
W="$LC/wt/$slot"
[ -x "$W/gradlew" ] || { echo "no checkout at $W (run lc-sync.sh first)" >&2; exit 2; }
[ $# -gt 0 ] || set -- spotlessCheck build
PORT="${LC_DEV_PORT:-25810}"
if ! [[ "$PORT" =~ ^258[1-9][0-9]$ ]]; then echo "LC_DEV_PORT must be 25810-25899, got $PORT" >&2; exit 2; fi
exec 8>"$LC/wt/.$slot.lock"; flock -n 8 || { echo "slot $slot is busy" >&2; exit 3; }

# Dev server settings: our port, loopback only, offline (dev accounts), eula accepted.
mkdir -p "$W/run/server"
for d in "$W/run" "$W/run/server"; do
    echo "eula=true" > "$d/eula.txt"
    p="$d/server.properties"
    touch "$p"
    for kv in "server-port=$PORT" "server-ip=127.0.0.1" "online-mode=false" "view-distance=6" "snooper-enabled=false"; do
        k="${kv%%=*}"
        if grep -q "^$k=" "$p"; then sed -i "s|^$k=.*|$kv|" "$p"; else echo "$kv" >> "$p"; fi
    done
done

mkdir -p "$LC/logs"
stamp="$(date +%Y%m%d-%H%M%S)"
log="${LC_LOG:-$LC/logs/build-$slot-$stamp.log}"
ln -sfn "$(basename "$log")" "$LC/logs/build-$slot-latest.log"
echo "slot=$slot sha=$(git -C "$W" rev-parse HEAD) args=$* log=$log" | tee "$log"
cd "$W"
# timeout leads its own process group and signals the whole group (Gradle and any JVM it forks).
timeout -k 30 "${LC_BUILD_TIMEOUT:-3600}" ./gradlew --no-daemon --console=plain "$@" >> "$log" 2>&1 < "${LC_STDIN:-/dev/null}" &
gpid=$!
echo "$gpid" > "$LC/logs/build-$slot.pid"
trap 'kill -TERM -"$gpid" 2>/dev/null; sleep 15; kill -KILL -"$gpid" 2>/dev/null; rm -f "$LC/logs/build-$slot.pid"; exit 130' INT TERM HUP
wait "$gpid"; rc=$?
[ "$rc" = 124 ] && echo "lc-build: timed out after ${LC_BUILD_TIMEOUT:-3600}s" | tee -a "$log"
rm -f "$LC/logs/build-$slot.pid"
grep -E "^BUILD (SUCCESSFUL|FAILED)|What went wrong" -A3 "$log" | head -8
if ls build/test-results/test/*.xml >/dev/null 2>&1; then
    grep -h -o 'tests="[0-9]*" skipped="[0-9]*" failures="[0-9]*" errors="[0-9]*"' build/test-results/test/*.xml \
        | awk -F'"' '{t+=$2;s+=$4;f+=$6;e+=$8} END {print "TESTS total="t" skipped="s" failures="f" errors="e}'
fi
echo "exit=$rc log=$log"
exit "$rc"
