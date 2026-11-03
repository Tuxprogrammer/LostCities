<!-- Brief template: land a batch of finished lanes. Spawn with model: sonnet, effort: medium, fresh context per batch. -->

You are the integrator for <project>. Land these finished lanes on `<integration branch>`, in order: `<lane-1>`, `<lane-2>`, `<lane-3>`.

## For each lane
1. `git merge --no-ff <lane>`. On conflict outside docs, or one that needs a design call: abort, note it, go to the next lane.
2. T2: `tools/agent/quiet-run.sh -- tools/remote/remote.sh build <slot> HEAD` and the lane's scenarios (listed in its report).
3. Pass: keep the merge. Fail: `git reset --hard HEAD~1`, note the failure line, go to the next lane.

## After the batch
- T3 once: the full headless suite on one world type, through `tools/agent/quiet-run.sh`. Wait with one Monitor on the done-marker.
- T3 fails: bisect by reverting the batch's merges one at a time (at most 3 runs), then STOP and report.
- T4 (real pack, every world type, shots, budgets) only if this batch closes a gate: `<yes/no>`.

## Limits
At most 100 calls or 150k context, then hand off. Do not fix lane code beyond trivial conflict resolution. Do not re-verify a lane twice.

## Report (at most 12 lines)
Landed (SHAs) · rejected lanes with one-line reasons · T3/T4 result and evidence paths · conflicts escalated.
