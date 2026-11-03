---
name: mod-port-budget
description: Use when planning or running a large mod rewrite, backport or modpack integration with autonomous agents - setting the token budget, verification tiers T1-T4, documentation depth and read-first lists for the whole project, or when a phase or milestone is over budget.
---

# Budget guardrails for mod ports and pack integrations

Autonomous agent work gets expensive when each milestone agent loads tens of thousands of tokens of specs, verifies on every world type, and is told never to stop at a first pass. Set limits before the work starts.

## Set before autonomous work starts

- **Budget per milestone**, in calls and in agents, written in the roadmap next to the exit gate. When a milestone reaches 1.5x its budget, the orchestrator stops it and re-plans; it does not push on.
- **Model plan.** Name in the roadmap the phases that run on a mid-tier model: research, catalogs, spec transcription, audits, applying decisions, integration. Reserve the top model for design and hard code (`orchestrating-parallel-agents`).

## Verification tiers

Write the tiers into CONTRIBUTING (template: `CONTRIBUTING.template.md`):

| Tier | When | What |
|---|---|---|
| T1 | every change | build, unit and static tests |
| T2 | lane landing | T1 + that lane's scenarios + a 2-minute smoke boot |
| T3 | batch of landings | full headless suite, one world type |
| T4 | gates only (alpha, beta, release) | real pack, every world type, shot list, budgets |

- Lanes run T1 and T2 only; the integrator runs T3 once per batch (`integration-merge-choreography`).
- A perf or budget target that misses at T3 is reported with numbers and becomes its own task; it does not loop inside the lane.

## Documentation

- **Spec depth.** Write a spec sheet to the level needed to build and test it, not upstream line by line. Add detail when an implementer asks for it.
- **Map files.** Every docs area has an `index.md` map of at most 40 lines. Briefs point at map lines, never at "every area it depends on".
- **Decision log.** Keep a one-line-per-decision index file, and archive closed rows each milestone. Agents `grep` the log; they never `cat` it.
- **ROADMAP.** One milestone per file, or anchors, so an agent reads one section.
- **No "read CLAUDE.md first" in briefs.** It is already in context.

## Report the cost

At each milestone landing, record calls, agents and weighted tokens (from your usage tooling) beside the exit-gate evidence. A milestone that cost twice its peers is a planning bug, not a fact of life.
