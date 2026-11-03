#!/usr/bin/env bash
# quiet-run.sh - run any command, keep the full log on disk, print only what an agent needs.
#
# Usage: quiet-run.sh [-n MAXLINES] [-p 'extra|grep|pattern'] -- <command...>
# Prints: one status line (exit code, seconds, log path), then at most MAXLINES (default 30)
# lines matching errors, failures, test summaries, or the extra pattern. Exit code is the command's.
# Example: tools/agent/quiet-run.sh -- tools/remote/remote.sh build m03 HEAD
set -u
max=30
extra=''
while [ $# -gt 0 ]; do
  case "$1" in
    -n) max="$2"; shift 2 ;;
    -p) extra="$2"; shift 2 ;;
    --) shift; break ;;
    *) break ;;
  esac
done
logdir="${QUIET_RUN_LOGDIR:-${TMPDIR:-/tmp}/quiet-run}"
mkdir -p "$logdir"
log="$logdir/$(date +%Y%m%d-%H%M%S)-$$.log"
start=$(date +%s)
"$@" >"$log" 2>&1
rc=$?
dur=$(( $(date +%s) - start ))
pat='BUILD (SUCCESSFUL|FAILED)|FAILURE:|error:|ERROR|Exception|FAILED|tests completed|[0-9]+ (tests|failures)|PASS|FAIL|status='
[ -n "$extra" ] && pat="$pat|$extra"
echo "quiet-run: rc=$rc ${dur}s lines=$(wc -l <"$log") log=$log"
grep -E "$pat" "$log" | grep -vE '^\s*at ' | awk '!seen[$0]++' | tail -n "$max"
exit $rc
