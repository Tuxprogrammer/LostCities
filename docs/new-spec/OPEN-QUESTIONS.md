# Open questions

What the new spec has not settled yet. Each row says what settles it: research in a roadmap spike, a milestone, or the owner, with a recommendation once the research behind it is done. A settled question leaves the open list, and its answer goes into the area sheet and a design record or decision-log row.

## Settled

### Q1 to Q13

| # | Topic | Settled in |
|---|---|---|
| Q1 | The overlay hook on RWG | [0005](../design/0005-rwg-overlay.md) §1 |
| Q2 | Terrain blend and height mapping | [0005](../design/0005-rwg-overlay.md) §5, §6 |
| Q3 | GT ore veins | Decision log D18, [ore-veins.md](00-pack-integration/ore-veins.md) |
| Q4 | Blocks 1.7.10 vanilla lacks | D19, [block-mapping.md](00-pack-integration/block-mapping.md) |
| Q5 | Loot | D20, [loot-and-mobs.md](00-pack-integration/loot-and-mobs.md) |
| Q6 | Mobs and spawners | D20, [loot-and-mobs.md](00-pack-integration/loot-and-mobs.md) |
| Q7 | Profile selection | [0005](../design/0005-rwg-overlay.md) §7 |
| Q8 | Other overworld structures | D21, [other-worldgen.md](00-pack-integration/other-worldgen.md) |
| Q9 | Spawn | D21, [spawn.md](00-pack-integration/spawn.md) |
| Q10 | Time budget | [0005](../design/0005-rwg-overlay.md) §8 |
| Q11 | The oil rig's diamond blocks | D30 (1): a non-valuable pale-aqua look-alike, [block-mapping.md](00-pack-integration/block-mapping.md#vanilla-blocks-with-a-1710-equivalent) |
| Q12 | Railways under landmark cores | D30 (2), amending [0006](../design/0006-build-around-other-worldgen.md#1-landmarks-and-landmark-lots) §1 |
| Q13 | A city near spawn | D30 (3), amending D21; [spawn.md](00-pack-integration/spawn.md#decisions) |

### The areas' proposals (D31)

Every *Proposed* item the area indexes `01` to `09` left open was answered by the owner on 2026-11-01 and is recorded in decision log D31. Each area index now carries the answer, marked *Accepted* (D31) with its item ID, in the deviation row it belongs to; its "Proposed, not decided" section says "None". Most items were accepted as recommended. Research at the pack's tags settled the facts behind the technical ones, and changed five recommendations. The owner overrode three.

| Area | Items | Overrides and changes |
|---|---|---|
| [01-core](01-core/index.md) | P-CORE-1 to P-CORE-5 | P-CORE-2 (owner): operator level 2; `debug`, `map`, `locate` and `createbuilding` ported, `testfill` dropped |
| [02-profiles-dimensions](02-profiles-dimensions/index.md) | P-PROF-1, P-PROF-2, P-PROF-4 to P-PROF-8 | P-PROF-4 (research and owner): `type:SANDY` minus `type:OCEAN`, without `type:MESA`, which matches nothing on RWG; an RWG realistic-biome matcher for canyons and oases in M02; BOP's Kelp Forest and Coral Reef in the ocean multiplier |
| [03-terrain](03-terrain/index.md) | P03-1 to P03-14 | P03-6 (research): the lane follows VillageNames' per-column path rule. P03-7 (research): `chisel:moss:0`. P03-8 (research): no explicit relight; ArchaicFix's Phosphor is a recorded dependency. P03-10 (**owner override**): saplings are never grown at generation. P03-12 (owner): no chat; log and count |
| [04-city-layout](04-city-layout/index.md) | P04-1 to P04-6 | P04-1 (research): a `ResourceLocation` hash helper beside `minecraft:overworld` |
| [05-assets](05-assets/index.md) | P05-1 to P05-7 | P05-4 (research): native references are `modid:name:meta`, meta required. P05-6 as P-PROF-4; P05-7 as P03-7 |
| [06-highways-railways](06-highways-railways/index.md) | P06-1 to P06-8 | P06-4 (**owner override**): shallow railways dip under a core and climb back. P06-6, P06-7 (research): the refined clamps |
| [07-scattered](07-scattered/index.md) | P07-1 to P07-6 | P07-2 (research): deep ocean is an oracle floor at Y 45 or lower; no oil rigs on classic RWG. P07-4, P07-5 (owner): population denial and Roguelike steering also cover scattered footprints, extending [0006](../design/0006-build-around-other-worldgen.md#5-order-and-determinism) §5 |
| [08-loot-mobs-blocks](08-loot-mobs-blocks/index.md) | P08-1, P08-2 | P08-2 (research): no bundled asset places a dispenser; the table is dropped |
| [09-client-ui](09-client-ui/index.md) | P-UI-1, P-UI-2 | P-UI-1 (research): GTNHLib's automatic config screen, worldgen keys marked `@Config.RequiresWorldRestart`. P-UI-2 (owner): the Create World picker after beta |
| Blocks (D29) | The gas station's iron | **Owner:** the highway gas station's and restaurant parking's iron blocks are a second exception to D29's cap, like the cabin ([block catalog](05-assets/block-catalog/index.md#decorative-metals-d29)) |

## Open

None open. A new question gets a row here with its recommendation and what settles it: a milestone (M02 to M11, [ROADMAP](../ROADMAP.md)) for a technical item, which builds the recommendation unless its tests show otherwise and records the outcome in the area sheet, or **Owner** for an item that changes what players see or amends a decision record.
