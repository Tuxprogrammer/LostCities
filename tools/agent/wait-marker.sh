#!/usr/bin/env bash
# wait-marker.sh - block until a done-marker exists (local file, or a remote file over ssh), then
# print ONE line: its first 400 bytes, or TIMEOUT. Meant to be the command of a single Monitor
# (or a run_in_background Bash) so the agent wakes up exactly once.
#
# Usage: wait-marker.sh <marker-path> [timeout-seconds=2400] [ssh-target]
#   KEEPWARM=240 wait-marker.sh ...   also print a one-word line every 240 s (keeps a 5-minute
#                                     prompt cache warm for an agent with a large context;
#                                     each wake-up costs ~0.1x context instead of 1.25x).
set -u
marker="$1"; timeout="${2:-2400}"; host="${3:-}"
keep="${KEEPWARM:-0}"
exists() { if [ -n "$host" ]; then ssh -o BatchMode=yes "$host" test -e "'$marker'"; else test -e "$marker"; fi; }
show() { if [ -n "$host" ]; then ssh -o BatchMode=yes "$host" head -c 400 "'$marker'"; else head -c 400 "$marker"; fi; }
t0=$SECONDS; last=$SECONDS
until exists; do
  [ $((SECONDS - t0)) -ge "$timeout" ] && { echo "TIMEOUT after ${timeout}s waiting for $marker"; exit 1; }
  if [ "$keep" -gt 0 ] && [ $((SECONDS - last)) -ge "$keep" ]; then echo "still-running $((SECONDS - t0))s"; last=$SECONDS; fi
  sleep 15
done
show | tr '\n' ' '; echo
