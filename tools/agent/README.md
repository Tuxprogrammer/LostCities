# tools/agent

Small helpers that keep agent context cheap. Each is a single file with a usage comment at the top. Skills that use them: `quiet-tool-output`, `waiting-on-remote-jobs`, `cheap-screenshot-review`.

| Tool | Does |
|---|---|
| `quiet-run.sh [-n N] [-p REGEX] -- <cmd>` | Runs a command, keeps the full log in `$QUIET_RUN_LOGDIR` (default `/tmp/quiet-run`), prints a status line and at most N matching error or summary lines. Exit code is the command's. |
| `wait-marker.sh <marker> [timeout] [ssh-target]` | Blocks until a done-marker exists (locally or over ssh), then prints one line. `KEEPWARM=240` also prints a heartbeat every 240 s. Use it as the command of one Monitor. |
| `contact-sheet.sh <shot-dir> [out-dir] [tile] [width]` | Tiles screenshots into labelled sheets with ImageMagick `montage`. |
| `run-summary.py <run-dir> [--pattern REGEX]` | One screen of facts from a run directory: `run.done`, PASS/FAIL/ERROR counts, first failures, budget lines folded to min/median/max (lines like `[MYMOD-BUDGET] terrain 12.3ms`). Standard library only. |

Example: `tools/agent/quiet-run.sh -- tools/remote/remote.sh build lane-a HEAD`
