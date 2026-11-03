---
name: port-workflow
description: Use when planning a large port or rewrite - setting up a roadmap with entry and exit gates, recording design decisions, and applying the definition of done to a change. For lanes and landing see integration-merge-choreography; for agents see orchestrating-parallel-agents.
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

## Branches, landing and parallel work

- Small changes that meet the definition of done below may be committed straight to the default branch.
- Parallel or long work happens in a lane: its own branch, its own git worktree and its own build slot, named for the milestone (`m03/<feature>`).
- **Lanes never merge the default branch.** A lane verifies its own change (tiers T1 and T2). A single integrator merges finished lanes in a queue with `--no-ff`, re-verifies only what the merge can break, and runs the full suite once per batch. The rules, including landing guards, are in `integration-merge-choreography`.
- Never trust a reported test result, including your own earlier one; the integrator's run is the one that counts.

## Agents, budgets and long jobs

- Agent scope, models, briefs, stop rules and round limits: `orchestrating-parallel-agents`.
- Context limits and handoff files: `agent-context-budget`.
- Verification tiers, budgets per milestone and documentation depth: `mod-port-budget`.
- Every long job writes a finished marker, and you wait on the marker with one Monitor: `waiting-on-remote-jobs`.
- When something reports "waiting on X", check X's marker yourself rather than trusting an estimate.

## Definition of done (per change)

- Implements its spec sheet, deviations recorded.
- Format, build and unit tests pass; regression tests for avoided original bugs.
- Headless boot clean; self-check passes; scenario exists for world-touching features; multiplayer checked for synced ones.
- Tier-gated recipes, visible in NEI, no duplicated GT materials.
- Every string is a lang key; every asset is our own or free-licensed, with its provenance and license recorded (your licensing decision record).
- Javadoc and `// Spec:` comments; no commented-out code.
- Roadmap box ticked and docs updated in the same commit or pull request.

Checklist for a commit or pull request: [docs/templates/pull_request_template.md](../../../docs/templates/pull_request_template.md).
