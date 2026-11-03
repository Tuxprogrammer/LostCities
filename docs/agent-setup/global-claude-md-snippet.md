<!-- Optional addition to your user-level ~/.claude/CLAUDE.md. Copy it yourself; nothing in this repo edits your home directory. Keep it this short: it loads in every session and every subagent. -->

## Token economy

- **Agent cost grows with the square of its length.** Brief agents for one deliverable. Have them hand off at 200k context or 120 calls (skill `agent-context-budget`).
- **Pass `model` on every Agent spawn.** Use Sonnet at medium effort for research, catalogs, docs passes, audits, integration and shot review. Use Opus only for design and hard code (`orchestrating-parallel-agents`).
- **Every brief has a stop condition, at most 2 verification rounds, and "report and stop".** Never write "never stop at a first pass".
- **Waiting on a job longer than a minute:** one Monitor that prints one line when a done-marker appears. No progress streams, no polling loops (`waiting-on-remote-jobs`).
- **Tool output:** build and test through a quiet wrapper; `grep` or `sed -n` docs and logs; never read a spilled tool-results file whole (`quiet-tool-output`).
- **Screenshots:** contact sheet first, single images only for suspects (`cheap-screenshot-review`).
- **"Agent is waiting on its own background work" notifications need no reply.**
