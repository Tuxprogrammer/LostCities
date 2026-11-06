---
name: agent-context-budget
description: Use in any agent session that will run more than about 60 tool calls, that reads large docs or logs, or whose context may pass 150k tokens - and whenever told to hand off, checkpoint or restart. Covers how to see your context size, when to write a handoff and stop, and what a handoff file holds.
---

# Context budget for long agent sessions

Every API call re-reads the whole context, and an agent session grows its context by a few thousand tokens per call. Cost therefore grows with the square of the call count: a 400-call agent costs about 16 times a 100-call one, not 4 times. In a review of a large port, calls made above 200k context were most of the spend.

## Rules

1. **Know your size.** If a hook reports context size (`.claude/hooks/context-guard.py`), obey it. If not, estimate: about 36k at start, plus about 2.5k per call, plus anything big you read.
2. **Hand off at 200k, or at 120 calls**, whichever comes first, unless the brief sets another limit. The exception is when autocompaction is on and fires at or before 200k; the hook says so. Then keep working, because the summary arrives before the limit, and the 120-call rule still applies. Do not "finish this one thing" past the limit.
3. **Write the handoff and stop.** Write `handoff/<task>.md`, at most 60 lines, holding:
   - goal and stop condition, copied from the brief;
   - commits made, with SHAs, and what is verified, with the evidence path;
   - what failed and why, in one line each;
   - next 3 steps exactly, with file paths;
   - open questions.

   Then end with a 5-line report that names the handoff path. The orchestrator starts a fresh agent from it.
4. **Never wait with a big context.** Before any wait longer than 4 minutes, either hand off first or keep the wait to cheap polls (see `waiting-on-remote-jobs`).
5. **Load narrowly at start.** Read the brief, then `grep` or `sed -n` the sections you need. CLAUDE.md is already in context, so do not read it again. Never `cat` a whole spec area or the decision log.
6. **Do not re-read what you wrote.** Edit and Write succeed or error; trust them.
7. **One task per context.** If the brief lists more than one deliverable with its own verification, finish and hand off between them.

## Fresh agent from a handoff

Read only the handoff file and the files it names. Do not reload the brief's whole "read first" list.
