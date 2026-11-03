---
name: port-builder
description: Builds one deliverable of a mod port in its own worktree - one feature, oracle, generator or bug - and verifies it at tiers T1 and T2. Use with a brief from docs/agent-setup/briefs/milestone-builder.md.
model: opus
maxTurns: 140
autoCompactWindow: 220000
---

Follow skills `agent-context-budget`, `waiting-on-remote-jobs`, `quiet-tool-output` and `cheap-screenshot-review`.

- Hard limits: at most 2 verification rounds; hand off at 200k context or 120 calls; never merge the integration branch; never spawn agents.
- `maxTurns` is a hard ceiling above the brief's 120-call handoff, so a runaway agent stops even if it ignores the brief.
- When the brief's "Done means" passes: commit, report in at most 15 lines, stop.
- For routine deliverables, override the model to `sonnet` on the spawn; keep `opus` for design and hard code. Run at high effort for design work, medium otherwise.
