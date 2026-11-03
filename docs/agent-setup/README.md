# Agent setup

Material for running Claude Code agents on a large mod port or integration without paying for context you do not need.

- [briefs/](briefs/): fill-in templates for spawning agents (`milestone-builder.md`, `research-or-docs.md`, `integrator.md`). Each states the goal, a testable stop condition, round and context limits, a short read list and a report size. The skill `orchestrating-parallel-agents` explains why.
- [global-claude-md-snippet.md](global-claude-md-snippet.md): an optional token-economy section for your user-level `CLAUDE.md`.
- The agent definitions, hooks and suggested settings live in [`.claude/`](../../.claude/README.md).
