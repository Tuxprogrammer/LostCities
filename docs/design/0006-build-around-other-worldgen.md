# 0006 · Building cities around other world generation

- Status: Accepted (decision log D26); §1 amended by D30 (deep railways under a core) and D31 (shallow railways dip under a core); §3 and §5 amended by D31 (the lane's path blocks; scattered footprints in the steering mixin); §1, §3 and §4 amended by the alpha review round M05R (decision log D149 to D153): the edge ring is the city's margin, never city, and the soft edge, the lanes and the detours move with it; §3 and §4 amended by M12 ([0009](0009-city-edges-and-character.md) §5): detours and primaries beside a margin are tertiary, the margin's bank and strip are dithered, lanes are relaxed
- Gate: M03 and M04 exit (the layout and terrain rules below); M08 (the compat oracles and the Roguelike mixin)
- Amends: D21 in the [decision log](decision-log.md) (treatments T1 and T4 for villages, Roguelike Dungeons, Witchery, LootGames, AE2 meteorites and RWG2 landmarks); [0005](0005-rwg-overlay.md) §3a and §5 (cores excluded from the footprint guard and the blend)
- Date: 2026-11-01

## Context

Decision log D24 sets the owner's rule: Lost Cities generation gets out of the way of other world generation without jarring the landscape or cutting it sharply. A Roguelike tower, a temple or a house that would land in a city becomes a feature of the city. Nearby villages are sewn in: paths lead into the city, and the city never writes over village chunks.

[other-worldgen.md](../new-spec/00-pack-integration/other-worldgen.md) sorts other generators into four treatments: avoid in the layout (T1), deny the populate event (T2), coexist (T3), and a guard mixin (T4). Avoidance alone leaves a hole in the city with a one-chunk non-city ring, as upstream's `StructureAvoidance` does, and letting AE2 meteorites crater a city slices its buildings. Neither builds around anything.

The research is in [coexistence.md](../new-spec/00-pack-integration/coexistence.md). The facts that drive this record:

- **Most surface structures are predictable without generating chunks.** VillageNames villages (site and the full plan of every piece), Witchery (site, and the union of its possible footprints), LootGames (site and footprint), AE2 meteorites (site, size, lava) and RWG2's landmarks (the chunk manager's predicates) are pure functions of the world seed, coordinates, the biome provider and other mods' config. Ground heights are read from the world at population in each case; the height oracle ([0005](0005-rwg-overlay.md) §4) estimates them.
- **Roguelike Dungeons is partly predictable.** Its 50 candidate points per 51-chunk cell are pure; which one wins depends on caves, lakes and grass under it.
- **Thaumcraft is not predictable**, and its site tests reject city blocks.
- **Other structures are written after the city.** Village pieces are placed in RWG's `populate`, mod structures in `IWorldGenerator`s, AE2 meteorites on a later tick. Our terrain phase runs before all of them.
- **AE2's crater cuts far.** Its paraboloid removes everything tall within about 50 blocks of the impact, which is the sharp cutoff D24 rules out.
- **Upstream** keeps avoided structures' chunks and a one-chunk ring out of the city, does not flatten the structure's chunk, and blends the ring toward the city. It learns about structures from 1.20's structure references, which 1.7.10 does not have, and its answer depends on generation order (SCAT-01, SCAT-02, SCAT-03).

## Decision

### 1. Landmarks and landmark lots

A **landmark** is another generator's structure the layout knows about. The layout package (pure Java) represents it as:

| Field | Meaning |
|---|---|
| `kind` | Village, Roguelike, Witchery, LootGames, meteorite, terrain landmark |
| `footprint` | One or more rectangles in block X and Z |
| `certainty` | `EXACT` (the footprint is the structure), `UNION` (the structure lies somewhere inside), `CANDIDATE` (predicted; may not appear) |
| `groundEstimate` | Block Y, from the height oracle |
| `gates` | Villages only: points on the footprint where a village path ends toward the outside, with their direction |
| `heightCap` | Meteorites only: the paraboloid the city must stay under |

Around each landmark the layout derives three zones, whole chunks as upstream's layout is:

| Zone | Chunks | What the city does there |
|---|---|---|
| **Core** | Every chunk the footprint, grown by `landmarkMargin` (2 blocks), touches | Nothing but a village lane's surface path (§3). Not a city chunk; no building, street, foundation, cellar, corridor, highway, railway (except a deep railway, below), blend, decoration clearing or T2 denial. RWG's terrain and decoration stay |
| **Edge ring (the margin)** | The core's eight-neighbors | Never city: the city builds inside it (alpha review AR-10). Natural ground with RWG's decoration, which the blend slopes from the city's edge to the core's ground and a soft-edge strip meets to the core's own edge columns (§4). Nothing city-made but a village lane, graded down the slope with stairs and half slabs (§3) |
| **Transition rings** | Villages only: the next `villageTransitionRings` (2) rings of city chunks, outside the margin | Building chance × 0.5; at most 2 floors in the first, 4 in the second. Open lots become parks as upstream's hierarchical open lots do. The village reads as the city's old town. A street that runs toward the margin ends in a plaza in the first ring, and a primary road's detour runs through it (§3) |

An AE2 meteorite that lands in a city gets a **crater basin**: its core is natural ground for the impact, and its heightCap applies to every city chunk it reaches, capping a building's floors so its roof stays below the paraboloid. The city steps down toward the impact instead of being sliced by the crater (§3, [coexistence.md § AE2](../new-spec/00-pack-integration/coexistence.md#ae2-meteorites)).

Terrain landmarks (RWG2 volcanoes, lava-cave openings, river junctions) have a core and a margin like every other kind: the city ends a chunk short of them, and the blend (§4, E1) shapes the land between.

**Deep railways** (decision log D30) are the one exception to "no railway" in a core. A railway part whose top is at least 8 blocks below the core chunk's lowest oracle ground may pass under the core. Nothing changes at the surface: a surface station whose spot is inside a core is still dropped. A rail line too shallow to pass under descends before the core until it is deep enough, runs under it and climbs back on the far side, so the network stays continuous (D31); M06 designs the descent from upstream's descending rail parts ([06-highways-railways](../new-spec/06-highways-railways/index.md#railways-railway-layoutmd-railway-partsmd)).

Chunk granularity keeps upstream's planner, multibuilding placement, street rendering and blend unchanged in kind. The cost is that a small structure on a chunk corner reserves up to four chunks and its margin a ring around them; the margin's natural ground, with RWG's trees on it, makes that space read as a green rather than a gap. The first alpha set put the city's parks and roads in the ring and stitched them to the core with a strip inside the city chunk; the owner's review found the strip eating half a park and half a highway, and a primary road running along a village's pond (AR-10 to AR-13). A whole chunk of margin, with the city's content only inside it, is what the owner asked for: "the city build in a margin, and then the slope stitches that margin into the village".

### 2. How the layout learns about landmarks: structure oracles

- `layout` defines `StructureOracle`: given a chunk rectangle, it returns the landmarks whose footprint touches it. The layout calls one aggregate oracle, built by the glue from every enabled per-mod oracle. Layout code sees only the interface and the landmark data, never a Minecraft type.
- Each per-mod oracle lives in `compat.<mod>`, guarded by `Loader.isModLoaded`, and is **pure**: it reads the world seed, coordinates, the biome provider (pure on RWG; [rwg.md](../new-spec/00-pack-integration/rwg.md)), the height oracle, and the other mod's config values. It never reads blocks, loads or generates a chunk, reads another mod's saved data, or reseeds `world.rand`.
- Results are cached per grid cell in bounded caches owned by the world (budget B5 of [0005](0005-rwg-overlay.md)).

| Oracle | Package | How |
|---|---|---|
| Villages | `compat.villagenames` | Computes VillageNames' grid seed with vanilla's `setRandomSeed` formula on a `Random` of its own and tests the biome name through the biome provider. For an accepted start, seeds the `Random` of a private `MapGenVillageVN` subclass exactly as `MapGenBase` does (so it is Hodgepodge's `StdLCG` in the pack, as the live generator's is) and calls VillageNames' own protected `getStructureStart` from the subclass; no mixin (D84, D85). FML's village-creation handlers are ordered by class name at `postInit`, without which the same seed builds other villages on each server run (D86). Footprint: every piece's box; gates: path ends nothing continues whose way out is clear of every piece by `landmarkMargin` (D87). Ground: the height oracle's median at the center and corners of each building (D88) |
| Ocean monuments (D32) | `compat.villagenames` | VillageNames' 32-chunk grid (separation 5, salt 10387313) and its biome-name test through the biome provider: deep ocean over 9 × 9 blocks, ocean or river over 15 × 15. An `EXACT` core: the 58 × 58 building. Ground: the sea floor's median |
| Roguelike Dungeons | `compat.roguelike` | Reproduces the grid and the FML generator seed, generates the 50 candidates, and tests each against the biome provider and the height oracle with `validLocation`'s rules. The first that passes is a `CANDIDATE` landmark: 21 × 21 blocks around it |
| Witchery | `compat.witchery` | The 20-chunk grid and the biome-type test, from the contract read from the decompiled jar (no code copied). A `UNION` landmark: 23 × 23 blocks over the candidate chunk and its west, north and north-west neighbors |
| LootGames | `compat.lootgames` | The rhomb-grid test. An `EXACT` landmark: the 21 × 21 room and the +Z entrance tunnel |
| AE2 | `compat.ae2` | The grid cell's position, size and lava from the seeded draws. An `EXACT` core of the crater size, and a heightCap paraboloid with the conservative center height (surface − 14 − r + 1) |
| RWG2 landmarks | `compat.rwg2` | The world-less chunk manager the height oracle already builds: volcano vicinity, lava-cave opening (70 blocks), river junction (strength ≥ 0.70 near a mountain chain), each grown by 8 blocks |

The values each oracle reads from another mod's config are added to the pinned profile snapshot ([0005](0005-rwg-overlay.md) §7). If they differ at a later load, the server logs that structure sites have moved for new chunks.

### 3. Streets, buildings and the planner

- **Multibuildings** may not cover a core or edge-ring chunk (`canPlaceBuilding` rejects the position, as it rejects predefined content). Because the oracle is pure, this decision is made once and holds (no SCAT-02).
- **Roads.** The hierarchical street field stays as upstream has it; neither a core chunk nor its margin is city, so the road is clipped there.
  - A **primary** road severed by a core or its margin detours through the ring of city chunks just outside the margin, on the shorter side (ties north, then west); those chunks become tertiary roads, and a primary road chunk whose 3 × 3 window holds a margin narrows to tertiary too ([0009](0009-city-edges-and-character.md) §5). No major road runs through or beside the margin, so none runs closer to a village than a chunk of green and a small road (AR-7, AR-10).
  - **Secondary and tertiary** roads, and a primary road with no detour, end in the city chunk facing the margin, which becomes a **plaza**: upstream's `FULL` street type, which upstream defines but never reaches, gets this use.
- **Village lanes.** For each village gate (at most one per side of the core, the one nearest the city's center), the gate chunk is the margin chunk in front of the gate. In a city, the lane crosses that chunk straight and goes on from the city chunk beyond it, by a straight or L-shaped run through transition chunks to the nearest effective road; those chunks are forced to the tertiary class, and a secondary or tertiary road is preferred to a primary one up to 2 chunks farther, so a village is sewn in with small roads (AR-7). In the gate chunk the lane is a 3-block-wide surface path, graded in a straight ramp from the city street's ground to the core's edge, the column on each side meeting it halfway, and given a stair or half slab wherever it rises a block (AR-7, AR-11); the street's border opens where it arrives. From the core's edge it crosses the core's ground to meet the village path's end as a **surface-only** path, never within `landmarkMargin` (2 blocks) of a village piece box. Each column takes the block VillageNames' own paths would take there (grass path on grass, gravel on sand, planks over water, through the village's material swaps; D31, [pipeline-on-rwg.md](../new-spec/03-terrain/pipeline-on-rwg.md#village-lanes)), replacing the natural top block; nothing else is written above or below it except the supports that rule adds (cobblestone under gravel on sand, posts under planks, the block cleared above water). It and its steps are the only thing the city writes in a core, and they are written in the terrain phase, before the village's own pieces.
- **Villages inside a city's radius** are surrounded by it: the city factor is not lowered near them, so the city continues on every side and the village becomes its old town, sewn in by the margin, the transition rings and a lane on each side that has a gate.
- **Villages near a city**, outside it but within `villageLinkDistance` (4 chunks) of its edge, get gates and a surface lane across the land between, written in the terrain phase as part of the city's footprint.
- **Height caps** (meteorite basins) reduce floors after upstream's floor draw, so random consumption is unchanged.
- **City level** near a landmark comes from the same height oracle as the landmark's ground estimate.
- Profile options replace upstream's avoidance keys:

| Upstream key | Here |
|---|---|
| `avoidVillages` (true) | `villageTreatment`: `BUILD_AROUND` (default), `AVOID` (core and a non-city ring, no sewing: upstream's behavior), `IGNORE` |
| `avoidStructures` (vanilla ids) | `landmarkTreatment` per kind: `BUILD_AROUND` (default), `AVOID`, `IGNORE` |
| `avoidVillagesAdjacent`, `avoidStructuresAdjacent` | Replaced by the margin (edge ring), which always exists for `BUILD_AROUND` |
| `avoidFlattening` (true) | Always true for cores |
| (none) | `landmarkMargin` (2 blocks), `softEdgeWidth` (6), `villageTransitionRings` (2), `villageLinkDistance` (4 chunks) |

### 4. Edge rules

The rules E1 to E8 of [coexistence.md § Soft edges](../new-spec/00-pack-integration/coexistence.md#soft-edges) are adopted. In short:

- **E1, cores are fixed ground in the blend.** Upstream's L0 step treats a corner shared by a core chunk and a non-city chunk as fixed at the core's oracle height (−1 to +1), as it fixes city corners at the city ground. L1, L2 and the transition noise are upstream's. Core chunks are never reshaped. The margin chunk between a city and a core is therefore sloped by the blend from the city's ground on its inner side to the core's oracle height on its outer side, over its whole 16 columns.
- **E2 to E4, a soft-edge strip in the margin.** The city's street chunks next to the margin keep upstream's border toward lower non-city land, opened where a lane arrives. In the margin chunk, the oracle height the blend aimed at is the core chunk's, not its edge columns', so after the blend a strip `softEdgeWidth` columns wide, varied ±2 by the transition noise, ramps the blended ground to each core-side edge column's own natural height read before the blend, keeping the column's own top block. Where water stands on that edge column (a village's pond or river), its natural height is the water's surface, so the strip meets the water at its waterline as a shore (decision log D129). Nothing the city builds is in the strip, so it never cuts a road, a highway or a park (AR-10, AR-12, AR-13). The retaining border of E3 remains only for a plan that puts a city chunk right beside a core, which the layout no longer does (the synthetic test city still does).
- **E5 to E8:** the margin keeps RWG's decoration; density falls off; nothing city-made underground in the margin, which is not city; lanes are 3 wide, graded down the margin's slope with stairs and half slabs.

### 5. Order and determinism

- Every layout input is pure; the layout never reads blocks other mods placed, `data/Village.dat`, AE2's spawn data, or the live structure generators' maps. Those fill in generation order and are used only by tests that check the oracles.
- Other structures' blocks land after our terrain phase. They land in cores, on RWG's ground, so they behave as in the wild.
- Our population phase writes only inside our footprint; cores are outside it.
- **Steering.** One compat mixin, on `Dungeon.validLocation` (Roguelike Dungeons), makes a candidate invalid inside a city chunk or edge ring unless it is that dungeon's predicted site, and invalid inside a scattered structure's footprint ([07-scattered](../new-spec/07-scattered/index.md), D31). The dungeon lands in its lot, outside the city, or not at all, whatever order chunks generate in.
- **Misses.** A lot whose structure does not appear stays natural ground. A structure that lands in the city anyway (Thaumcraft) is accepted; a per-generator guard (T4) follows only if the alpha screenshots show it. No restore pass ever rewrites another mod's blocks.

### 6. Treatments by generator

In the terms of [other-worldgen.md](../new-spec/00-pack-integration/other-worldgen.md), building around replaces avoidance (T1) and the guard (T4) for these generators, and coexistence (T3) for AE2 meteorites. T2 and the other T3 features are unchanged.

| Generator | Treatment |
|---|---|
| VillageNames villages | Built around and sewn in (§1 to §4); inside a city's radius, the city's old town |
| LootGames | Built around as a lot |
| RWG2 landmarks | Terrain landmarks |
| Roguelike Dungeons | Built around a predicted lot, with the steering mixin built in M08, not deferred to the alpha screenshots |
| Witchery | Built around the union lot; no mixin needed |
| AE2 meteorites | A crater basin (§1, §3) |
| Dungeons, lakes, oil, quarries (T2) | Denied in city chunks; not denied in cores |
| Underground structures, small decoration, Thaumcraft (T3) | Unchanged; Thaumcraft watched at alpha |

## Consequences

- **M03 (layout):**
  - `StructureOracle`, `Landmark` and the zone derivation in `layout`, with test fakes.
  - Core and edge-ring rules in `computeChunkCharacteristics` and `BuildingInfo`; multibuilding rejection; transition-ring density; height caps.
  - Road detours, plazas (`FULL` made reachable), lanes and gates.
  - Golden layout tests with fixed fake oracles: a village inside a city, one at its edge, one 3 chunks out; a severed primary road; a meteorite basin.
- **M04 (terrain):**
  - No writes in cores, including the footprint write guard and T2 denial, apart from village lanes' surface paths.
  - The E1 blend anchor; soft-edge strips; lane writing on blended ground and across village cores.
  - Oracle calls inside the time budget (B1 to B5).
- **M06 (highways and railways):** cores are obstacles; the planners route around them or end before them, and railways pass under them, shallow lines dipping down to the depth deep ones run at (§1, D30, D31).
- **M07 (scattered structures):** upstream's `StructureAvoidance` is replaced by this record, not ported; scattered structures stay out of cores and edge rings.
- **M08 (GTNH layer):**
  - The per-mod oracles in `compat.villagenames`, `compat.roguelike`, `compat.witchery`, `compat.lootgames`, `compat.ae2` and `compat.rwg2`.
  - The Roguelike steering mixin, whatever the alpha screenshots show.
  - Real-pack checks that the oracles match placed structures.
- **M09 (profiles):** the options in §3; the oracle inputs in the pinned snapshot.
- **Alpha screenshot set (M05)** adds: a village sewn into a city as its old town and one at its edge, with lanes meeting the village paths; a plaza facing a landmark; soft-edge strips; an empty reserved lot; a crater basin.
- **Spec deviations** to record: `07-scattered` (structure avoidance replaced); `04-city-layout` (chunk characteristics, multibuildings, street planner clip, `FULL` street type, floor caps); `03-terrain` (blend anchors, no border wall toward a core, lanes); `02-profiles-dimensions` (the options).
- **Dependencies:** VillageNames, Roguelike Dungeons, LootGames and AE2 as `compileOnly`; Witchery through its jar's class names only. All under `compat`, all optional.
- **Licensing:** calling VillageNames' and Roguelike Dungeons' classes and mixing into them is integration under D23; no GPL code is copied. Witchery's grid is a numeric contract learned from the decompiled jar, written as our own code.
- **Pack changes:** none.

## Alternatives considered

| Alternative | Why not |
|---|---|
| Upstream's avoidance (T1): a hole in the city and a non-city ring | Leaves holes rather than features, has no paths into villages, and its ring is a hard city edge. Kept as the `AVOID` option |
| Learn structures from the live generators (`structureMap`, `data/Village.dat`, AE2's spawn data) | They fill in generation order, and only within 8 chunks of generated chunks, so layout would depend on order. Used only by tests |
| Let other structures land and restore the city afterwards | Either their structure or ours is destroyed; restore passes are slow and span chunks |
| Deny every structure in cities (T4 for all) | Simple and clean for the city, but the landscape loses villages, towers and meteorites wherever cities are, against D24 |
| Sub-chunk lots everywhere | Upstream's layout, planner and blend are chunk-granular; rewriting them at block granularity is a large deviation. Soft-edge strips and lanes give the sub-chunk detail where it shows |
| Generate other mods' structures ourselves, as our own multibuildings | Copies GPL and closed content and duplicates their generators |
| Our own copy of VillageNames' village layout | GPL code we would copy and keep in step; calling VillageNames' constructor is exact and allowed |
| Edge-ring parks, plazas and passing roads, with the strip inside the city chunk (the first alpha design) | The owner's review: the strip ate half a park, its stone features and half a highway, and put a primary road beside a village's pond (AR-10 to AR-13). A one-chunk margin of natural ground costs a ring of chunks around a small structure, which reads as a green |
| A wider non-city green belt around every landmark | More land lost to the city than the margin needs; the transition rings already thin the city out around villages |
| AE2: let the crater carve | Slices tall buildings within about 50 blocks: a sharp cutoff |
| AE2: a guard mixin that keeps meteorites out of cities | No cut and no extra layout rules, but no meteorites wherever cities are, against D24 |
| Village lanes that stop at the core's edge | Leaves up to about 15 blocks of natural ground between the city's path and the village's, so the two never meet |
| Lower the city factor near villages, so cities stop short of them | Open country around villages, but a gap in a city near every village instead of an old town |
| Build the Roguelike steering mixin only if the alpha screenshots show towers in cities | Without it, a dungeon whose predicted lot fails lands on a city street |

## Still open

For the prototype and the alpha screenshots:

- The Roguelike prediction's hit rate.
- The look of margins, plazas, lanes, empty lots and crater basins; `softEdgeWidth` and the transition-ring numbers (the alpha review round's shots, M05R, show the margins and lanes).
- Thaumcraft mounds in city parks.
- The oracles' cost against the budget.
