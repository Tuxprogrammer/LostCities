# Roadmap

<!-- docs/ROADMAP.md. Milestones in one lane run in order; lanes run in parallel once their entry gates hold. Tick a box only in the commit or pull request that completes it. -->

## How to read this roadmap

- **Milestones (M01…)** each have an **entry gate** (true before work starts) and an **exit gate** (the definition of done).
- **Decision gates (D1…)** are design questions settled, as records in `docs/design/`, before the milestones they block.
- **Spikes (S1…)** are research or spec work that must land before the milestones named in them.
- **Lanes** are the agent teams. Each owns a feature area's code, tests and docs, so lanes don't collide; each milestone names its lane and what it runs in parallel with.
- Each milestone's content ships behind a module toggle that changes behavior only, never registration.
- Every change also meets the definition of done in the `port-workflow` skill.

```
M01 Foundation ──► D1–Dn ──► M02 Core mechanic ──┬─► M03 …  (lane A) ──┐
                                                 └─► M04 …  (lane B) ──┴─► Alpha gate ──► … ──► Beta gate ──► Polish
```

## M01 · Foundation

**Lane:** Core. **Parallel with:** the spikes.

**Entry gate:** the repo builds from the template; CI is green.

**Scope:** dependencies pinned to the target pack; package skeleton; per-module registries; config with module flags; network channel with validated packet base classes; creative tab and lang file; unit-test harness; in-world scenario command; dev self-check.

**Exit gate:**
- [ ] `./gradlew spotlessApply build` passes, with at least one real unit test.
- [ ] `./gradlew runServer` boots with the pack's core dependencies and no errors from this mod.
- [ ] `./gradlew runClient` reaches the main menu.
- [ ] Module flags load, save, sync from the server, and are documented.
- [ ] Packet base classes reject out-of-range, wrong-dimension and unauthorized requests, with tests.

## Decision gates

### D1 · Tier map. *Blocks M02 and later.*
Every item gets one voltage tier and a gating material. Record: `docs/design/NNNN-tier-map.md`.

### D2 · Deconfliction. *Blocks the machine milestones.*
Keep, replace or drop each system of the original against what GT and the pack provide.

### D3 · Energy model.
EU only; electric-item API; charging rules per tier.

<!-- Add the gates your port needs: combat or damage model, world presence, art direction, end goal (standalone or pack inclusion) ... -->

## Spikes

| Spike | Delivers | Before | Lane |
|---|---|---|---|
| **S1 · <topic>** | <the sheet, record or source check it produces> | <milestone> | <lane> |

## M02 · <Core mechanic>

**Lane:** <lane>. **Parallel with:** <milestones or spikes>.

**Entry gate:** M01 done; D1 accepted.

**Scope:** ...

**Exit gate:**
- [ ] ...
- [ ] Scenario tests for every world-touching feature.
- [ ] Regression tests for the original's bugs this milestone could reproduce.

## Beta gate (review)

**Entry gate:** all content milestones done.

**Exit gate:**
- [ ] Each milestone audited against its exit gate.
- [ ] Release jar tested on a dedicated server of the exact target pack.
- [ ] Multiplayer and client pass, batched into one session.
