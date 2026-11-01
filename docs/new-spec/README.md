# Lost Cities: New Horizons new spec

How this mod differs from Lost Cities 1.20 and how it fits into GT New Horizons. Upstream's behavior is in [docs/original-spec/](../original-spec/README.md). Builders read both: the original-spec sheet for what a system does, and the matching new-spec area for what changes and how it connects to the pack.

Target pack: **GTNH 2.9.0** ([0002](../design/0002-target-pack.md)), with RWG2 Continents as the default overworld generator.

Status: **`00-pack-integration` is written and accepted** (spikes S2 and S3). `05-assets` is started with its block catalog only. Areas `01` to `09` are otherwise still to be written: each is a deviation table against its original-spec area, built from the design records and the [decision log](../design/decision-log.md) ([ROADMAP](../ROADMAP.md)).

## What this mod is for

Give a GTNH overworld a lived-in look and feel. Players come across abandoned cities, highways and railways in RWG terrain. They loot them, and can take over a complex of buildings and build their base into it. The cities are upstream's, faithful in layout and style. A GTNH layer makes them belong to the pack: pack blocks, pack loot at EV or below, pack mobs, and coexistence with GT ores and RWG ([0003](../design/0003-world-presence.md), [0004](../design/0004-gtnh-layer-and-tier-ceiling.md)).

Every change here serves that, and every system uses what the pack already has before adding anything of its own (`gt-first-design`).

## How to read

- Each area mirrors an original-spec area, plus `00-pack-integration` for cross-cutting pack topics. Its `index.md` has a **deviation table**: every original sheet, what this mod does instead, and the decision behind it.
- **Status words.**
  - *Accepted* means a decision record in [docs/design/](../design/README.md) settles it; the record is linked.
  - *Proposed* means this spec suggests it and the owner has not decided. Proposals that need the owner are in [OPEN-QUESTIONS.md](OPEN-QUESTIONS.md) with a recommendation.
  - *Verified* facts about other mods cite the file and line at the pack's tag; *unverified* ones say so and must be checked from source before code (`gtnh-integration-research`).
- Numbers carry units: blocks, chunks, block-Y levels, ticks.

## Areas

| Area | What changes |
|---|---|
| [`00-pack-integration`](00-pack-integration/index.md) | RWG2 and classic RWG, GT ore veins, the block-mapping table, loot and mob sources, other pack worldgen and structures, every pack mod this mod touches |
| `01-core` | 1.7.10 lifecycle and registration, GTNHLib config, commands, network, the API |
| `02-profiles-dimensions` | Profiles on the overworld only; the Lost City dimension is not ported ([0003](../design/0003-world-presence.md)) |
| `03-terrain` | The overlay on RWG terrain, the 1.7.10 height range, blending, explosions and damage |
| `04-city-layout` | Layout carried over as is, apart from 1.7.10 and RWG constraints |
| `05-assets` | The asset system in plain Java, 1.20 blocks mapped to 1.7.10 pack blocks, the bundled assets. Started, catalog only: the [decorative block catalog](05-assets/block-catalog/index.md) and its variety pools (S4, D25, D28) |
| `06-highways-railways` | Highways and railways over RWG terrain |
| `07-scattered` | Scattered structures among pack structures |
| `08-loot-mobs-blocks` | Pack loot at EV or below, pack mobs and vanilla spawners |
| `09-client-ui` | Whatever client UI survives without a world type of our own |
| [OPEN-QUESTIONS.md](OPEN-QUESTIONS.md) | What is still undecided, who or what settles it, and the recommendation |

## Decisions this spec builds on

| Record | Settles |
|---|---|
| [0001](../design/0001-port-policy-and-licensing.md) | Upstream `1.20` at `f18a094d` is the baseline; MIT code is ported under the quality gate; upstream assets are reused |
| [0002](../design/0002-target-pack.md) | GTNH 2.9.0 and its dependency versions |
| [0003](../design/0003-world-presence.md) | The overworld only, over RWG; no Lost City dimension; PersonalSpace after the port |
| [0004](../design/0004-gtnh-layer-and-tier-ceiling.md) | Faithful cities with a GTNH layer; nothing above EV in the overworld; balance waits for beta; buildings players can move into |
