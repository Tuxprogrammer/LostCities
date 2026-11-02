# Landmark zones in the layout

How each per-chunk decision of upstream's layout ([04-city-layout](../../original-spec/04-city-layout/index.md)) changes in the zones [0006](../../design/0006-build-around-other-worldgen.md#1-landmarks-and-landmark-lots) §1 puts around other mods' structures. 0006 settles the zones and their rules; this sheet lays them against upstream's decisions one by one, so the layout code has one place to read them.

Status: *Accepted* from 0006 and D26, except the lines marked *Proposed*, which are collected in the area's [Proposed, not decided](index.md#proposed-not-decided).

## Zones

| Zone | Chunks | Source |
|---|---|---|
| **Core** | Every chunk a landmark's footprint, grown by `landmarkMargin` (2 blocks), touches | 0006 §1 |
| **Edge ring** | City chunks among a core's eight neighbors | 0006 §1 |
| **Transition ring 1, 2** | Villages only: the next `villageTransitionRings` (2) rings of city chunks outside the edge ring | 0006 §1 |
| **Crater basin** | City chunks an AE2 meteorite's height cap reaches | 0006 §1, [coexistence.md § AE2](../00-pack-integration/coexistence.md#ae2-meteorites) |
| **Village link** | Non-city land between a city's edge and a village within `villageLinkDistance` (4 chunks) of it | 0006 §3 |

Terrain landmarks (RWG2 volcanoes, lava-cave openings, river junctions) have a core and no edge ring. Landmarks come from the pure `StructureOracle` the glue builds from the enabled per-mod oracles ([0006](../../design/0006-build-around-other-worldgen.md#2-how-the-layout-learns-about-landmarks-structure-oracles) §2). Zones are derived per chunk, before the characteristics stage, and are part of the immutable plan.

## Decisions by zone

"Upstream" means the decision is made as [04-city-layout](../../original-spec/04-city-layout/index.md) makes it.

| Decision | Core | Edge ring | Transition rings | Crater basin |
|---|---|---|---|---|
| City membership (`isCity`) | Never city | Upstream (raw city stays city) | Upstream | Upstream |
| Building possible (`couldHaveBuilding`) | No | No | Upstream draw `bc`, compared with `buildingChance × 0.5` | Upstream |
| Multibuilding | May not cover the chunk (`canPlaceBuilding` rejects the position) | May not cover the chunk | Allowed; *Proposed*: its floor cap is the strictest cap over its footprint | Allowed under the cap |
| Floors | n/a | n/a | At most 2 in ring 1, 4 in ring 2, applied after upstream's floor draw | Reduced after upstream's draw until the roof stays below the meteorite's paraboloid; a chunk left with no floor is an open lot |
| Cellars | n/a | None | Upstream | Upstream |
| Planned road | None: the road is clipped | A road passing by; a severed primary road's detour; or a plaza where a secondary or tertiary road ends facing the core | Upstream | Upstream |
| Street type | n/a | Open lot → `PARK`, upstream's hierarchical open-lot rule; plaza → `FULL` (`street_full` parts) | Upstream; open lots become parks as upstream's hierarchical open lots do | Upstream |
| Corridors | None | None (E7), whatever the draws say | Upstream | Upstream |
| Rail dungeon | None | None (E7) | Upstream | Upstream |
| Bridges | A core chunk is never a bridge gap chunk | Upstream | Upstream | Upstream |
| Highways, railways | None (`06-highways-railways`) | A highway may pass as a road passing by; railways as upstream (E7 names only cellars, corridors, subways and rail dungeons) | Upstream | Upstream |
| Scattered structures | None (`07-scattered`) | None | Upstream | Upstream |
| City level | n/a | From the height oracle, as everywhere | Upstream | Upstream |
| Terrain | Untouched, except a village lane's surface path | Sides facing the core get soft-edge strips instead of border walls (`03-terrain`) | Upstream | Upstream |
| RWG decoration | Stays | Stays in parks and strips (outside the footprint) | Upstream footprint rule | Upstream footprint rule |
| `Populate` vetoes (lakes, dungeons, oil) | Not vetoed | Vetoed as city chunks | Vetoed | Vetoed |

**Random consumption.** Every zone rule takes effect after upstream's draws for the chunk, so a chunk in a zone consumes its building random exactly as upstream would: `bc` is drawn and then ignored in the edge ring, corridor and dungeon candidates are drawn and then cleared, floors are drawn and then capped. 0006 §3 states this for height caps; this sheet applies it to every zone rule, so a zone never shifts the parts, palettes and doors a chunk draws.

## Roads around a core

From [0006](../../design/0006-build-around-other-worldgen.md#3-streets-buildings-and-the-planner) §3:

1. The hierarchical road field ([street-planner.md](../../original-spec/04-city-layout/street-planner.md)) is computed as upstream; a core chunk is not city, so the effective road is clipped there.
2. A **primary** road severed by a core detours through edge-ring chunks on the shorter side of the ring (ties: north, then west); those chunks become primary roads.
3. A **secondary or tertiary** road ends in the edge-ring chunk facing the landmark, which becomes a plaza: upstream's `FULL` street type, which upstream defines but never reaches.
4. **Village lanes.** For each village gate, at most one per side of the core (the one nearest the city's center), the nearest effective road connects to the gate's edge-ring chunk by a straight or L-shaped run through edge-ring and transition chunks, forced to the tertiary class. From the gate chunk it continues as the surface path `03-terrain` writes.
5. Villages inside a city's radius are surrounded by it: the city factor is not lowered near them, and each becomes the city's old town. Villages outside a city but within `villageLinkDistance` get gates and a surface lane across the land between.

## Profile options

Upstream's avoidance keys are replaced ([0006](../../design/0006-build-around-other-worldgen.md#3-streets-buildings-and-the-planner) §3); `02-profiles-dimensions` owns them, M09 ships them, and the structure oracles' inputs from other mods' config join the pinned profile snapshot.

| Upstream key | Here |
|---|---|
| `avoidVillages` (true) | `villageTreatment`: `BUILD_AROUND` (default), `AVOID` (core and a non-city ring, no sewing: upstream's behavior), `IGNORE` |
| `avoidStructures` (vanilla structure IDs) | `landmarkTreatment` per kind: `BUILD_AROUND` (default), `AVOID`, `IGNORE` |
| `avoidVillagesAdjacent`, `avoidStructuresAdjacent` | Replaced by the edge ring, which always exists for `BUILD_AROUND` |
| `avoidFlattening` (true) | Always true for cores |
| none | `landmarkMargin` 2 blocks, `softEdgeWidth` 6, `villageTransitionRings` 2, `villageLinkDistance` 4 chunks |
