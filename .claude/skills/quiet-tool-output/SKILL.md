---
name: quiet-tool-output
description: Use whenever about to run a command or read a file whose output may be large - builds, test runs, logs, git diff or log, cat of docs or sources, find or ls over trees, spilled tool-results files - so only what is needed enters context.
---

# Keep tool output small

Every token a tool returns is re-read on every later call. In cost reviews, shell output was a large share of cache-read spend, and the biggest single items were multi-file `cat`s that spilled to a file and were then read back whole.

## Rules

- **Builds and tests.** Run them through a quiet wrapper (`tools/agent/quiet-run.sh`) that prints a status line, the counts and at most 30 matching error lines. The full log goes to a file you can `grep`.
- **Docs.** `grep -n '^#' FILE` first, then `sed -n 'A,Bp'` for the section you need. Never `cat` more than one doc in a command. Never `cat` a doc over about 300 lines.
- **Spilled output** ("Output too large ... saved to tool-results/..."). Never `Read` the file whole; `grep` it or read a bounded range.
- **Logs.** `grep -E 'PASS|FAIL|ERROR|status=' LOG | tail -40`. Use `grep -c` when a count answers the question. For a finished run directory, `python3 -I tools/agent/run-summary.py <run-dir>` prints one screen of facts.
- **git.** Use `git diff --stat`, `git log --oneline -n 10`, and `git show --stat`. Look at a full diff only per file, and only when you will edit that file.
- **Search.** Use `rg -l` or `grep -l` to find files, then read the hit with `-n -C 3`. Bound `find` with `-maxdepth` and `| head -50`.
- **Source.** Read the method or class you need by line range, not the whole file. Do not re-read a file you just edited.
- **Upstream or other mods.** Read the one file at the exact path. Use the semantic search tool (`tools/semantic-search/`) to find where something is, instead of opening several files.
- **Your own commands.** Long heredoc scripts stay in context as your output, too. Put reusable ones in a scratch file and run that.

The optional `.claude/hooks/output-guard.py` blocks the worst of these patterns (see `.claude/README.md`).
