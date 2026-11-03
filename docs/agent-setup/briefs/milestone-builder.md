<!-- Brief template: one build deliverable. Spawn with model: opus (design or hard code) or sonnet (routine), plus isolation: worktree. Fill the <> fields; delete nothing in CAPS. -->

You are building **<one deliverable, e.g. "the widget placement oracle">** for <project>. Worktree branch `<branch>`, build slot `<slot>`, real-pack instance `--instance <n>` (skill `remote-build-runner`).

## Goal
<one sentence>

## Done means (stop condition)
- <testable check 1, e.g. "scenario widgets.oracle passes on a fresh world">
- <testable check 2>
When these pass: commit, write the report, STOP. Do not polish, widen scope, or re-verify unchanged work.

## Limits (hard)
- VERIFICATION ROUNDS: at most 2. If round 2 fails, report the failure, the evidence path and your best diagnosis, then STOP.
- CONTEXT: at 200k tokens or 120 calls, write `handoff/<branch>.md` (skill `agent-context-budget`) and STOP.
- DO NOT merge `<integration branch>` into this branch. DO NOT spawn agents.
- Perf or budget targets: measure once at the end; if missed, report the numbers and STOP. A new task will chase them.

## Read only
- `<doc path>` lines <A–B> (or `grep -n '<anchor>'`)
- `<doc path>` section "<heading>"
- `<source path>`
(CLAUDE.md is already loaded. Do not `cat` whole docs, the decision log or the roadmap.)

## Verification (tiers T1 and T2 only)
- `tools/agent/quiet-run.sh -- tools/remote/remote.sh build <slot> HEAD`, then this deliverable's scenarios: `<cmd>`.
- Wait on remote runs with one Monitor on the run's done-marker (skill `waiting-on-remote-jobs`).
- Screenshots: contact sheet only (skill `cheap-screenshot-review`).

## Decisions
Append one row per decision to `docs/design/decision-log.md` as `D-<branch>-N` (one line each). No design record unless the decision changes a public behavior.

## Report (at most 15 lines)
Commits (SHAs) · what is live · evidence file paths · decisions (IDs) · gaps against "Done means".
