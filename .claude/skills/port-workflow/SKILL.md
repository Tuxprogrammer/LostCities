---
name: port-workflow
description: Use when planning or running a large port or rewrite - setting up a roadmap, recording design decisions, splitting work across branches, worktrees or parallel agents, verifying and landing branches, or supervising long-running jobs. Also use when an agent reports "waiting", a run seems stuck, or two lines of work touch the same main branch.
---

# Workflow for a large port

A port of a big mod is hundreds of changes. The workflow exists so that each one is small, gated, independently verified and recoverable.

## Roadmap with gates

Write `docs/ROADMAP.md` (template: [docs/templates/roadmap.md](../../../docs/templates/roadmap.md)):
- **Milestones in order**, each with an **entry gate** (what must be true before work starts) and an **exit gate** (checkbox definition of done). A box is ticked only in the commit or pull request that completes it.
- **Decision gates**: design questions (tier map, energy model, machine deconfliction, damage or combat model, world presence, art direction) that block the milestones depending on them.
- Each milestone's content ships behind a module toggle (see `config-safe-registration`), defaulting on once its exit gate is met.
- End with a review gate that audits every milestone against its exit gate before calling the project beta.

Typical order for a content mod: foundation (deps, packages, registries, config, network, test harness, self-check) → decision gates → core mechanic → content families in parallel → production chain and machines → utility → entities → worldgen → review gate → polish.

## Decision records

- One file per decision in `docs/design/NNNN-short-title.md` (template: [docs/templates/decision-record.md](../../../docs/templates/decision-record.md)): context, decision precise enough to test, consequences, alternatives.
- **Accept by default** during an MVP push when the owner has said so: make the sensible call, write it as `Accepted`, proceed, and list notable decisions in your report. Ask only about credentials, destructive or irreversible actions, real spending, and legal or licensing calls.
- When a decision changes, edit its record in place; git history is the audit trail. Merge records that cover the same decision, and fix every link to a record you merge or delete.
- Keep an open-items ledger. Whoever orchestrates owns answering subagents' open questions; record each answer where it applies (code comment, spec, record) rather than escalating.

## Verify before landing

Never trust a reported test result, including your own earlier one. Changes may be committed straight to the default branch when they meet the definition of done below. Work done on its own branch or in a worktree (a parallel agent, a long change) lands only through these steps:
1. Merge the current default branch **into** the branch.
2. Rebuild from clean, run formatting, unit and static tests, the headless boot, and the **full** scenario suite (not just the new scenarios).
3. Land only if the default branch is still an ancestor of the verified commit (`git merge-base --is-ancestor <main> <branch>`). If it moved, go back to step 1. Tree-based or "replay" landings on a moved main silently revert other people's work.
4. Refuse to land an empty diff.
5. If other sessions are verifying against the same main, announce before landing and hold if one is mid-verify.
6. Use `--no-ff` merges so each feature is one revertible commit.

Put steps 3–4 in a landing script and check that the guards exist before using any landing script you did not write.

## Long-running jobs and agents

- **Every long job writes a finished marker**: a final log line ("all done") or a `.done` file. Wait with a bounded poll of that marker (for example every 60 s, hard timeout), never on a completion event alone. Missed events stall everything; extra wake-ups are cheap.
- When something reports "waiting on X", check X's log yourself within its expected runtime. Don't trust runtime estimates.
- **Scope agents tightly**: one family, one model or one bug per agent, a clear stop condition, a terse report format. Delegate mechanical multi-step work (verify and merge, conflict fixes, generator passes) to cheaper agents; keep the expensive one on decisions.
- **Compact large contexts**: past roughly 250k tokens, have the agent write a short handoff file (state, what worked, what failed, open limits) and stop; start a fresh agent from that file.
- Give parallel agents disjoint file ownership (code vs assets vs docs) so they don't collide.
- **Never run commands that can block on an approval prompt** in unattended runs: write temp files only to a dedicated scratch directory by absolute path, never `../` out of the worktree, no broad `rm -rf` (even inside an `ssh` string), no edits to `.git/` internals or global config. Leave leftovers listed for a human instead.
- Cap parallel memory-heavy jobs (Minecraft JVMs, multiprocessing pools) to what the machine can hold.

## Definition of done (per change)

- Implements its spec sheet, deviations recorded.
- Format, build and unit tests pass; regression tests for avoided original bugs.
- Headless boot clean; self-check passes; scenario exists for world-touching features; multiplayer checked for synced ones.
- Tier-gated recipes, visible in NEI, no duplicated GT materials.
- Every string is a lang key; every asset is our own or free-licensed, with its provenance and license recorded (decision 0001).
- Javadoc and `// Spec:` comments; no commented-out code.
- Roadmap box ticked and docs updated in the same commit or pull request.

Checklist for a commit or pull request: [docs/templates/pull_request_template.md](../../../docs/templates/pull_request_template.md).
