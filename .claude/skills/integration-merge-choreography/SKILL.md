---
name: integration-merge-choreography
description: Use when several branches or worktree lanes land on one integration branch - deciding who merges what, when to re-verify after the main branch moves, how the single integrator lands lanes, and how shared test resources (one real-pack server, one GPU, one lock) are scheduled.
---

# One integrator lands lanes; lanes never re-merge

A rule that says "merge main into your lane, run the full suite, and if main moved, start again" makes every landing anywhere restart verification in every open lane. Cost multiplies with the number of lanes, and agents also queue for hours on one shared lock. The fix is a single integrator.

## Rules

1. **Lanes never merge the integration branch.** A lane works on its base and verifies its own change (tiers T1 and T2, see `mod-port-budget`). The narrow exception: rebase once if the lane cannot build against a changed API, and say so in the report.
2. **One integrator lands lanes in a queue.** It is the only writer to the integration branch while it runs. For each lane:
   1. `git merge --no-ff <lane>` (one revertible commit per lane).
   2. Run build and unit tests, plus the lane's own scenarios and the cheap smoke suite.
   3. Keep the merge on pass; `git reset --hard HEAD~1` on fail, note the failure line, and move on.

   Run the full real-pack or visual suite **once per batch** of landed lanes, not once per lane.
3. **Landing guards.**
   - Never land by replaying or copying trees onto a moved main: that silently reverts other people's work. Merge commits only.
   - Refuse an empty diff.
   - Check that any landing script you did not write has these guards before using it.
4. **Re-verify only what a merge can break.**
   - The merge touched only docs or other packages: build and unit tests.
   - The merge touched shared worldgen, registration or config: add the smoke scenarios.
   - Gates (alpha, beta, release): full suite, every world type.
5. **The integrator is cheap.** Run it on a mid-tier model at medium effort with a fresh context per batch (`.claude/agents/port-integrator.md`, brief in `docs/agent-setup/briefs/integrator.md`). Escalate conflicts that need design judgment to the orchestrator; do not resolve them inside a big context.
6. **Shared heavy resources get a schedule, not a lock to poll.** Give each lane its own instance where possible (see `remote-build-runner`). Otherwise the integrator owns the shared one, and lanes ask for a run instead of waiting on the lock.
7. **Stop rule for a moving main.** If main moves during your verification and the change does not touch your files, land anyway with the cheap checks. Do not loop.
8. **Bisecting a failing batch:** revert the batch's merges one at a time, at most 3 runs, then stop and report.
