# Open questions

What the new spec has not settled yet. Each row says what settles it: research in a roadmap spike, a milestone, or the owner, with a recommendation once the research behind it is done. A settled question leaves this list, and its answer goes into the area sheet and a design record or decision-log row.

## Settled

Questions Q1 to Q13 are settled:

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

## Open

Every *Proposed* item the area indexes `01` to `09` leave open, one row each; the area's "Proposed, not decided" section has the full proposal. The moss block is one item that `03-terrain` and `05-assets` both list. In `03-terrain`, `04-city-layout` and `06-highways-railways` the ID is the item's number in that list.

**What settles it.** A milestone (M02 to M11, [ROADMAP](../ROADMAP.md)) settles a technical item: it builds the recommendation unless its tests show otherwise, and records the outcome in the area sheet. **Owner** marks an item that changes what players see or amends a decision record; it goes to the owner with the recommendation, and the milestone named after it builds the answer.

| ID | Topic | Area | Recommendation | Settles it |
|---|---|---|---|---|
| P-CORE-1 | API acquisition and `getLostInfo` | [01-core](01-core/index.md#proposed-not-decided) | A static `LostCitiesAPI.get()`, never `null`; queries only until a pack mod asks for more | M10 |
| P-CORE-2 | Which debug and authoring commands are ported, and their permission | [01-core](01-core/index.md#proposed-not-decided) | Operator level 2 for all; port `debug`, `map`, `locate`, `createbuilding`; drop `testfill` | **Owner**, then M10 |
| P-CORE-3 | API profile registration, query interfaces and events | [01-core](01-core/index.md#proposed-not-decided) | Register from a base profile until `FMLServerAboutToStartEvent`; the four chunk events; no `CharacteristicsEvent` yet | M10 |
| P-CORE-4 | Part-editor file names | [01-core](01-core/index.md#proposed-not-decided) | `data/lostcitiesnh_edit.dat`; exports to `config/lostcitiesnh/export/parts/`, outside the live assets | M10 |
| P-CORE-5 | Recording a player's first join | [01-core](01-core/index.md#proposed-not-decided) | A boolean in the player's persisted NBT; the chosen spot in `data/lostcitiesnh.dat` | M09 |
| P-PROF-1 | Out-of-range values when a profile loads | [02-profiles-dimensions](02-profiles-dimensions/index.md#proposed-not-decided) | Clamp single values with a warning; reject the profile for anything a clamp cannot fix; never fail mod loading | M09 |
| P-PROF-2 | User profile files that reuse a built-in's name | [02-profiles-dimensions](02-profiles-dimensions/index.md#proposed-not-decided) | Reject them with a message; owners copy with `saveprofile` | M09 |
| P-PROF-4 | Biome matchers and the desert city style on RWG | [02-profiles-dimensions](02-profiles-dimensions/index.md#proposed-not-decided) | `BiomeDictionary` types in 05's syntax; desert style for `type:SANDY` excluding `type:OCEAN`, plus `type:MESA` | M02 |
| P-PROF-5 | `highwayDistanceMask` | [02-profiles-dimensions](02-profiles-dimensions/index.md#proposed-not-decided) | Drop it with legacy highways | M06 |
| P-PROF-6 | Names and category of the new options | [02-profiles-dimensions](02-profiles-dimensions/index.md#proposed-not-decided) | All in `lostcity`; the veto's name follows whether ravines are covered | M09 |
| P-PROF-7 | Snapshot layout and mismatch handling | [02-profiles-dimensions](02-profiles-dimensions/index.md#proposed-not-decided) | Profile stored as upstream-format JSON, one entry per dimension; log and carry on | M04 |
| P-PROF-8 | A city style for every biome (PROF-12) | [02-profiles-dimensions](02-profiles-dimensions/index.md#proposed-not-decided) | Every world style holds a selector without a biome filter, checked at load | M02 |
| P03-1 | The RWG decoration footprint's volume | [03-terrain](03-terrain/index.md#proposed-not-decided) | Every city-written column from its lowest block to Y 255; parks, yards and strips outside | M04 |
| P03-2 | Wrapping the cave carver in city chunks | [03-terrain](03-terrain/index.md#proposed-not-decided) | Don't wrap it unless the screenshots show caves breaking cellars | M04 |
| P03-3 | Sampler-group division for `heightSampleSize` | [03-terrain](03-terrain/index.md#proposed-not-decided) | `Math.floorDiv`, so every group is N × N chunks | M04 |
| P03-4 | Lowering under water (TERR-23) | [03-terrain](03-terrain/index.md#proposed-not-decided) | Cleared blocks at or below `waterLevel − 1` become liquid | M04 |
| P03-5 | Primary-road surface | [03-terrain](03-terrain/index.md#proposed-not-decided) | The street character resolves through the primary-roads pool on `PRIMARY` chunks only | M04 |
| P03-6 | The village lane's path block | [03-terrain](03-terrain/index.md#proposed-not-decided) | Gravel, unless the village oracle reports the village's own path block | M05 |
| P03-7, P05-7 | The default rubble table's moss block | [03-terrain](03-terrain/index.md#proposed-not-decided), [05-assets](05-assets/index.md#p05-7-moss-block) | `minecraft:mossy_cobblestone`; the row is in [block-mapping.md](00-pack-integration/block-mapping.md#blocks-upstreams-code-names) | M02 |
| P03-8 | Light from blocks written in the terrain phase | [03-terrain](03-terrain/index.md#proposed-not-decided) | Vanilla's light pass, plus a relight of each listed light source in the population phase | M04 |
| P03-9 | Vine support | [03-terrain](03-terrain/index.md#proposed-not-decided) | A pure rule from the neighbor's plan instead of `canSurvive` | M04 |
| P03-10 | Sapling growth | [03-terrain](03-terrain/index.md#proposed-not-decided) | Grow only small trees that stay inside the populated chunk, with our own seeded `Random` | M04 |
| P03-11 | Floating-fragment numbers | [03-terrain](03-terrain/index.md#proposed-not-decided) | 1.12's 0.05 / 0.4 / 50 as constants; on the [beta balance list](beta-balance-list.md) | M04 |
| P03-12 | Error chat | [03-terrain](03-terrain/index.md#proposed-not-decided) | No chat message: log once, and count failed chunks in the operator timer command | **Owner**, then M04 |
| P03-13 | The soft-edge strip's ramp | [03-terrain](03-terrain/index.md#proposed-not-decided) | Linear across the strip | M04 |
| P03-14 | `buildingMinFloors`' range | [03-terrain](03-terrain/index.md#proposed-not-decided) | 0 to 22, as `buildingMaxFloors` | M09 |
| P04-1 | The dimension ID in the layout hashes | [04-city-layout](04-city-layout/index.md#proposed-not-decided) | `minecraft:overworld` for dimension 0, so tests compare with upstream 1.20's values | M03 |
| P04-2 | CITY-10's fix | [04-city-layout](04-city-layout/index.md#proposed-not-decided) | One building-random stream shared by the characteristics stage and `BuildingInfo` | M03 |
| P04-3 | CITY-21's tie-break | [04-city-layout](04-city-layout/index.md#proposed-not-decided) | The chunk's own style wins a tie; otherwise the first by name | M03 |
| P04-4 | CITY-35's rule for internal walls | [04-city-layout](04-city-layout/index.md#proposed-not-decided) | Every internal multibuilding wall may get random doorways | M03 |
| P04-5 | Multibuildings in transition rings | [04-city-layout](04-city-layout/index.md#proposed-not-decided) | Allowed, under the strictest floor cap in the footprint | M03 |
| P04-6 | Predefined content on a landmark core | [04-city-layout](04-city-layout/index.md#proposed-not-decided) | The core wins; the content is skipped with one log line | M03 |
| P05-1 | Folder layout | [05-assets](05-assets/index.md#p05-1-folder-layout) | Upstream's tree verbatim in the jar, mirrored under `config/lostcitiesnh/assets/` | M02 |
| P05-2 | One name form | [05-assets](05-assets/index.md#p05-2-one-name-form) | Normalize every asset name to a full id at load and compare full ids everywhere | M02 |
| P05-3 | Asset-set hash warning | [05-assets](05-assets/index.md#p05-3-asset-set-hash-warning) | Log one warning naming the changed files, and carry on | M04 |
| P05-4 | Native block references | [05-assets](05-assets/index.md#p05-4-native-block-references) | 1.12's `modid:name@meta`; `ore:<name>` in matchers | M02 |
| P05-5 | Desert rubble (ASSET-20) | [05-assets](05-assets/index.md#p05-5-desert-rubble) | A `sandstone_rubble` variant shaped like `stonebrick_rubble` | M02 |
| P05-6 | Biome vocabulary | [05-assets](05-assets/index.md#p05-6-biome-vocabulary) | `type:<BiomeDictionary type>` and `name:<biome name>`; the same desert rule as P-PROF-4 | M02 |
| P06-1 | Highways around landmark cores | [06-highways-railways](06-highways-railways/index.md#proposed-not-decided) | Prefer a clear shape, then detour, then dead-end one chunk before the core | M06 |
| P06-2 | Hub samples in a core | [06-highways-railways](06-highways-railways/index.md#proposed-not-decided) | Skip them | M06 |
| P06-3 | HWY-09: one level where routes overlap | [06-highways-railways](06-highways-railways/index.md#proposed-not-decided) | Each overlapping route takes the lowest level among its overlaps, in one step | M06 |
| P06-4 | Railways that cannot pass under a landmark core | [06-highways-railways](06-highways-railways/index.md#proposed-not-decided) | `NONE` in the core, `RAILS_END_HERE` before it; no staircase under a core | M06 |
| P06-5 | A per-world switch for highways (HWY-06) | [06-highways-railways](06-highways-railways/index.md#proposed-not-decided) | Profile option `highwaysEnabled`, default true | M06 |
| P06-6 | `highwayNetworkLevel`'s range | [06-highways-railways](06-highways-railways/index.md#proposed-not-decided) | Clamp to the highest level whose tallest part ends at or below Y 255 (28 at ground 71) | M06 |
| P06-7 | The railway's lowest Y | [06-highways-railways](06-highways-railways/index.md#proposed-not-decided) | Reject a profile that puts the railway's part Y below 5 | M06 |
| P06-8 | Planning cost | [06-highways-railways](06-highways-railways/index.md#proposed-not-decided) | Synchronous planner plus a time-sliced tick task that computes hub cells ahead of players | M06 |
| P07-1 | Selection biome (SCAT-07) | [07-scattered](07-scattered/index.md#p07-1-selection-biome) | Keep an entry when its filter accepts the biome of any chunk of the area | M07 |
| P07-2 | Deep ocean for the oil rig | [07-scattered](07-scattered/index.md#p07-2-deep-ocean-for-the-oil-rig) | `type:OCEAN` with the oracle floor at Y 48 or lower under every footprint chunk | M07 |
| P07-3 | Landmark rings outside cities | [07-scattered](07-scattered/index.md#p07-3-landmark-rings-outside-cities) | Scattered footprints also stay out of a core's eight neighbors outside cities | M07 |
| P07-4 | Population denial under scattered structures | [07-scattered](07-scattered/index.md#p07-4-population-denial-under-scattered-structures) | Deny lakes, lava, dungeons and oil where a population window touches a scattered footprint | **Owner**, then M07 |
| P07-5 | Roguelike towers and scattered structures | [07-scattered](07-scattered/index.md#p07-5-roguelike-towers-and-scattered-structures) | The steering mixin also rejects points inside a scattered footprint | **Owner**, then M08 |
| P07-6 | Floors in scattered structures | [07-scattered](07-scattered/index.md#p07-6-floors-in-scattered-structures) | Count from the plan's lowest level, no cellars | M07 |
| P08-1 | Block lists in 1.7.10 | [08-loot-mobs-blocks](08-loot-mobs-blocks/index.md#p08-1-block-lists-in-1710) | Compute each tag's list once at `postInit` from the live registry, by rule | M04 |
| P08-2 | The dispenser table | [08-loot-mobs-blocks](08-loot-mobs-blocks/index.md#p08-2-the-dispenser-table) | Drop it from the converted `chestloot` | M08 |
| P-UI-1 | GTNHLib's config screen | [09-client-ui](09-client-ui/index.md#proposed-not-decided) | Register it; mark worldgen keys as applying to new worlds | **Owner**, then M01 or M09 |
| P-UI-2 | Scope of the deferred Create World picker | [09-client-ui](09-client-ui/index.md#proposed-not-decided) | After beta, single player only, picking among loaded profiles | **Owner**, after beta |
