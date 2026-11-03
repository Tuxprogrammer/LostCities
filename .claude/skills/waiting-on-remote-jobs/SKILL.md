---
name: waiting-on-remote-jobs
description: Use when starting or waiting on any job that runs longer than about a minute - remote builds, Gradle, Minecraft servers or clients, shot runs, benchmarks, CI - or when tempted to poll, sleep, tail a log or stream progress with Monitor.
---

# Waiting on long jobs without burning context

Each wake-up re-reads your whole context. A subagent's prompt cache lives about **5 minutes**, so a wake-up after more than 5 minutes idle rewrites the whole context at a premium (about 1.25x), while a wake-up inside 5 minutes costs about 0.1x. Verify these numbers against current pricing; the shape of the advice holds either way.

## Rules

1. **Start the job detached and make it write a marker.** The job writes a `run.done` file, or a final `status=` line, holding a one-line summary. The scripts in `tools/remote/` already do this.
2. **Wait with one Monitor that emits exactly one line**, when the marker appears or the timeout hits (`tools/agent/wait-marker.sh`, or inline):
   ```
   until [ -e "$DONE" ] || [ $SECONDS -gt 2400 ]; do sleep 20; done; head -c 400 "$DONE" 2>/dev/null || echo TIMEOUT
   ```
   Never stream progress lines through Monitor. 40 progress events are 40 full-context calls.
3. **Choose your wait shape by context size and expected duration:**

   | Context | Expected wait | Do |
   |---|---|---|
   | < 100k | any | one Monitor, as in rule 2 |
   | > 100k | < 5 min | one Monitor |
   | > 100k | 5-30 min | either a keep-warm Monitor that emits one line every 4 minutes until done (`KEEPWARM=240 tools/agent/wait-marker.sh ...`), or hand off first and let a fresh agent pick up the result |
   | > 300k | > 10 min | hand off before starting the wait |

4. **Read results, not logs.** On completion, read the summary line, then `grep` for the few lines you need (see `quiet-tool-output`). Never `cat` or `Read` a whole run log.
5. **A held lock is not a reason to sit and poll.** If a shared resource is busy, report "blocked on <lock>" with the holder and stop, or switch to work that does not need it. Do not retry in a loop.
6. **Do not re-run to double-check a run that wrote a clear pass or fail marker.**
7. **When something reports "waiting on X",** check X's marker yourself within its expected runtime; do not trust runtime estimates.
8. **Kill only what you started.** Leave nothing running when you hand off or finish.
