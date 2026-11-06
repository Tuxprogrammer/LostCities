# Claude Code setup

What is in this folder and how to turn it on. Nothing here is active until you copy it: there is deliberately no `settings.json`.

| Piece | What it is |
|---|---|
| `skills/` | Skills Claude Code loads when a task matches a skill's `description`. Active as soon as the folder exists. Mapped in the root `CLAUDE.md`. |
| `agents/` | Three subagent definitions: `port-researcher`, `port-builder`, `port-integrator`. Each sets `model`, `maxTurns` and `autoCompactWindow`. Active as soon as the folder exists. |
| `hooks/context-guard.py` | PostToolUse hook. Reads the transcript, and above 150k context (`CTX_WARN`) tells the agent its size. It also works out whether autocompaction is on and where it fires (settings, agent frontmatter `autoCompactWindow`, env). If compaction fires at or before 200k (`CTX_STOP`), the note is informational and asks for no handoff. Otherwise, above 200k, it tells the agent to write a handoff and stop. `CTX_AUTOCOMPACT=0/1` overrides the detection. |
| `hooks/output-guard.py` | PreToolUse hook for Bash and Read. Blocks reading a spilled `tool-results` file whole, `cat` of two or more `.md` files, and `cat` of a decision log or roadmap. `CONTACT_SHEETS=1` also blocks opening a single image when a `sheets/` folder sits next to it. |
| `settings.example.json` | Suggested settings: hook wiring, a lower `bashOutputMaxChars`, an `autoCompactWindow`, and default subagent model and effort through `env`. |

## Enabling the hooks and settings

1. Copy `settings.example.json` to `.claude/settings.json` (shared with the team) or `.claude/settings.local.json` (just you), and delete the `_comment` key.
2. Trim what you do not want. The hooks and the settings are independent.
3. Test on one session first, and confirm a hook fires: read a large spilled file, or let a session pass 150k context.
4. The hooks call `python3 -I` with `$CLAUDE_PROJECT_DIR`, so they work from any worktree.

A user-level `CLAUDE.md` snippet for the same token-economy rules is in `docs/agent-setup/global-claude-md-snippet.md`; copy it yourself if you want it. This repo never touches `~/.claude/`.

## Verified versus only suggested

The token-economy review checked these in a Claude Code 2.1.316 binary (it did not run them end to end). Re-check against your version.

**Present in the binary:**
- Settings `bashOutputMaxChars` (default 30,000; range 4,000 to 128,000), `autoCompactWindow` (its 100k to 1M range is inferred), `promptCacheTtl`, `subagentPromptCacheTtl` (`"5m"` or `"1h"`) and `idleCompaction`.
- Agent frontmatter keys `maxTurns` and `autoCompactWindow`.
- Environment variables `CLAUDE_CODE_SUBAGENT_MODEL`, `CLAUDE_CODE_SUBAGENT_EFFORT`, `CLAUDE_CODE_SUBAGENT_PROMPT_CACHE_TTL`, `ENABLE_PROMPT_CACHING_1H` and `CLAUDE_AUTOCOMPACT_PCT_OVERRIDE`.
- The Agent tool's `model` and `effort` parameters.
- Hook events `PreToolUse`, `PostToolUse`, `SubagentStart`, `SubagentStop` and `PreCompact`, with the `transcript_path`, `additionalContext` and `updatedInput` fields.

**Only suggested, not verified:**
- The exact JSON `context-guard.py` prints to inject `additionalContext` (it follows the documented `hookSpecificOutput` form).
- That exit code 2 with a stderr message blocks a call in `output-guard.py` (the documented PreToolUse convention).
- Whether `effort` works as an agent frontmatter key. The agent files therefore leave it out and say which effort to run them at.
- Any way to stop "agent is waiting" notifications reaching the orchestrator. None was found; the skills tell the orchestrator to ignore them.
- The prompt-cache arithmetic in `waiting-on-remote-jobs` (a 5-minute cache, about 1.25x to rewrite and 0.1x to hit). Check current pricing.
