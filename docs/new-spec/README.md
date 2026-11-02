# Lost Cities: New Horizons new spec

How this mod differs from Lost Cities 1.20 and how it fits into GT New Horizons. Upstream's behavior is in [docs/original-spec/](../original-spec/README.md). Builders read both: the original-spec sheet for what a system does, and the matching new-spec area for what changes and how it connects to the pack.

Target pack: **GTNH 2.9.0** ([0002](../design/0002-target-pack.md)), on both of its overworld generators: RWG2 Continents, the single-player default, and classic RWG, the dedicated-server default (decision log D33).

Status: **all areas `00` to `09` are written** (spikes S2 to S4). `00-pack-integration` is accepted. Areas `01` to `09` are deviation tables against their original-spec areas, built from the design records and the [decision log](../design/decision-log.md); their rows are *Accepted* where they cite a decision. Every item they once proposed was answered by the owner in decision log D31, so the new spec has no open questions ([OPEN-QUESTIONS](OPEN-QUESTIONS.md)); the milestones in the [ROADMAP](../ROADMAP.md) build the answers.

## What this mod is for

Give a GTNH overworld a lived-in look and feel. Players come across abandoned cities, highways and railways in RWG terrain. They loot them, and can take over a complex of buildings and build their base into it. The cities are upstream's, faithful in layout and style. A GTNH layer makes them belong to the pack: pack blocks, pack loot at EV or below, pack mobs, and coexistence with GT ores and RWG ([0003](../design/0003-world-presence.md), [0004](../design/0004-gtnh-layer-and-tier-ceiling.md)).

Every change here serves that, and every system uses what the pack already has before adding anything of its own (`gt-first-design`).

## How to read

- Each area mirrors an original-spec area, plus `00-pack-integration` for cross-cutting pack topics. Its `index.md` has a **deviation table**: every original sheet, what this mod does instead, and the decision behind it.
- **Status words.**
  - *Accepted* means a decision record in [docs/design/](../design/README.md) settles it; the record is linked.
  - *Proposed* means this spec suggests it and the owner has not decided. Every proposal is listed in [OPEN-QUESTIONS.md](OPEN-QUESTIONS.md) with its recommendation and what settles it: the milestone that builds it, or the owner. Areas `01` to `09` hold none at present; the [integration-opportunities.md](00-pack-integration/integration-opportunities.md) survey's owner rows are decided (D28) and its unbuilt rows wait until after beta (D-BA-2).
  - *Verified* facts about other mods cite the file and line at the pack's tag; *unverified* ones say so and must be checked from source before code (`gtnh-integration-research`).
- Numbers carry units: blocks, chunks, block-Y levels, ticks.

## Areas

| Area | What changes |
|---|---|
| [`00-pack-integration`](00-pack-integration/index.md) | RWG2 and classic RWG, GT ore veins, the block-mapping table, loot and mob sources, other pack worldgen and structures, every pack mod this mod touches |
| [`01-core`](01-core/index.md) | 1.7.10 lifecycle and registration, GTNHLib config, commands, network, the API |
| [`02-profiles-dimensions`](02-profiles-dimensions/index.md) | Profiles on the overworld only; the Lost City dimension is not ported ([0003](../design/0003-world-presence.md)) |
| [`03-terrain`](03-terrain/index.md) | The overlay on RWG terrain, the 1.7.10 height range, blending, explosions and damage |
| [`04-city-layout`](04-city-layout/index.md) | Layout carried over as is, apart from 1.7.10 and RWG constraints |
| [`05-assets`](05-assets/index.md) | The asset system in plain Java, 1.20 blocks mapped to 1.7.10 pack blocks, the bundled assets, and the [decorative block catalog](05-assets/block-catalog/index.md) with its variety pools (S4, D25, D28, D29) |
| [`06-highways-railways`](06-highways-railways/index.md) | Highways and railways over RWG terrain |
| [`07-scattered`](07-scattered/index.md) | Scattered structures among pack structures |
| [`08-loot-mobs-blocks`](08-loot-mobs-blocks/index.md) | Pack loot at EV or below, pack mobs and vanilla spawners |
| [`09-client-ui`](09-client-ui/index.md) | Whatever client UI survives without a world type of our own |
| [OPEN-QUESTIONS.md](OPEN-QUESTIONS.md) | What is still undecided (none at present), who or what settles it, and the recommendation; and where each settled question was answered |

## Decisions this spec builds on

| Record | Settles |
|---|---|
| [0001](../design/0001-port-policy-and-licensing.md) | Upstream `1.20` at `f18a094d` is the baseline; MIT code is ported under the quality gate; upstream assets are reused |
| [0002](../design/0002-target-pack.md) | GTNH 2.9.0 and its dependency versions |
| [0003](../design/0003-world-presence.md) | The overworld only, over RWG; no Lost City dimension; PersonalSpace after the port |
| [0004](../design/0004-gtnh-layer-and-tier-ceiling.md) | Faithful cities with a GTNH layer; nothing above EV in the overworld; balance waits for beta; buildings players can move into |
