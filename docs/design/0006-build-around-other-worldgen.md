# 0006 · Building cities around other world generation

- Status: Proposed
- Gate: M03 and M04 exit (the layout and terrain rules below); M08 (the compat oracles and the Roguelike mixin)
- Amends: D21 in the [decision log](decision-log.md) (treatments T1 and T4 for villages, Roguelike Dungeons, Witchery, LootGames, AE2 meteorites and RWG2 landmarks); [0005](0005-rwg-overlay.md) §3a and §5 (cores excluded from the footprint guard and the blend)
- Date: 2026-11-01

## Context

Decision log D24 sets the owner's rule: Lost Cities generation gets out of the way of other world generation without jarring the landscape or cutting it sharply. A Roguelike tower, a temple or a house that would land in a city becomes a feature of the city. Nearby villages are sewn in: paths lead into the city, and the city never writes over village chunks.

D21 handled other structures with four treatments ([other-worldgen.md](../new-spec/00-pack-integration/other-worldgen.md)): avoid in the layout (T1), deny the populate event (T2), coexist (T3), and a guard mixin (T4) if the alpha screenshots showed conflicts. T1 left holes in the city with a one-chunk non-city ring, as upstream's `StructureAvoidance` does; AE2 meteorites were allowed to crater cities. Neither builds around anything.

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
| **Core** | Every chunk the footprint, grown by `landmarkMargin` (2 blocks), touches | Nothing. Not a city chunk; no building, street, foundation, cellar, corridor, highway, railway, blend, decoration clearing or T2 denial. RWG's terrain and decoration stay |
| **Edge ring** | City chunks among the core's eight-neighbors | City chunks without buildings, cellars, corridors, subways or rail dungeons. Each is a park, a plaza (where a road ends facing the landmark), or a road passing by. Sides facing the core get soft-edge strips (§4) |
| **Transition rings** | Villages only: the next `villageTransitionRings` (2) rings of city chunks | Building chance × 0.5; at most 2 floors in the first, 4 in the second. Open lots become parks as upstream's hierarchical open lots do |

A meteorite's heightCap applies to every city chunk it reaches: a building's floors are capped so its roof stays below the paraboloid (§3, [coexistence.md § AE2](../new-spec/00-pack-integration/coexistence.md#ae2-meteorites)).

Terrain landmarks (RWG2 volcanoes, lava-cave openings, river junctions) have a core and no edge ring: the city ends there, and the chunk-scale blend (§4, E1) shapes the land between.

Chunk granularity keeps upstream's planner, multibuilding placement, street rendering and blend unchanged in kind. The cost is that a small structure on a chunk corner reserves up to four chunks; the soft-edge strip and the edge ring's parks make that space read as a green rather than a gap.

### 2. How the layout learns about landmarks: structure oracles

- `layout` defines `StructureOracle`: given a chunk rectangle, it returns the landmarks whose footprint touches it. The layout calls one aggregate oracle, built by the glue from every enabled per-mod oracle. Layout code sees only the interface and the landmark data, never a Minecraft type.
- Each per-mod oracle lives in `compat.<mod>`, guarded by `Loader.isModLoaded`, and is **pure**: it reads the world seed, coordinates, the biome provider (pure on RWG; [rwg.md](../new-spec/00-pack-integration/rwg.md)), the height oracle, and the other mod's config values. It never reads blocks, loads or generates a chunk, reads another mod's saved data, or reseeds `world.rand`.
- Results are cached per grid cell in bounded caches owned by the world (budget B5 of [0005](0005-rwg-overlay.md)).

| Oracle | Package | How |
|---|---|---|
| Villages | `compat.villagenames` | Computes VillageNames' grid seed with vanilla's `setRandomSeed` formula and tests the biome name through the biome provider. For an accepted start, seeds a `Random` exactly as `MapGenBase` does and builds the start through VillageNames' own `getStructureStart` on a private `MapGenVillageVN` instance (an accessor mixin reaches the protected method and sets its world). Footprint: every piece's box; gates: the ends of path pieces that face outward. Ground: the oracle's median height over each box |
| Roguelike Dungeons | `compat.roguelike` | Reproduces the grid and the FML generator seed, generates the 50 candidates, and tests each against the biome provider and the height oracle with `validLocation`'s rules. The first that passes is a `CANDIDATE` landmark: 21 × 21 blocks around it |
| Witchery | `compat.witchery` | The 20-chunk grid and the biome-type test, from the contract read from the decompiled jar (no code copied). A `UNION` landmark: 23 × 23 blocks over the candidate chunk and its west, north and north-west neighbors |
| LootGames | `compat.lootgames` | The rhomb-grid test. An `EXACT` landmark: the 21 × 21 room and the +Z entrance tunnel |
| AE2 | `compat.ae2` | The grid cell's position, size and lava from the seeded draws. An `EXACT` core of the crater size, and a heightCap paraboloid with the conservative center height (surface − 14 − r + 1) |
| RWG2 landmarks | `compat.rwg2` | The world-less chunk manager the height oracle already builds: volcano vicinity, lava-cave opening (70 blocks), river junction (strength ≥ 0.70 near a mountain chain), each grown by 8 blocks |

The values each oracle reads from another mod's config are added to the pinned profile snapshot ([0005](0005-rwg-overlay.md) §7). If they differ at a later load, the server logs that structure sites have moved for new chunks.

### 3. Streets, buildings and the planner

- **Multibuildings** may not cover a core or edge-ring chunk (`canPlaceBuilding` rejects the position, as it rejects predefined content). Because the oracle is pure, this decision is made once and holds (no SCAT-02).
- **Roads.** The hierarchical street field stays as upstream has it; a core chunk is not city, so the road is clipped there.
  - A **primary** road severed by a core detours through edge-ring chunks on the shorter side of the ring (ties north, then west), which become roads of the same class.
  - **Secondary and tertiary** roads end in the edge-ring chunk facing the landmark, which becomes a **plaza**: upstream's `FULL` street type, which upstream defines but never reaches, gets this use.
- **Village lanes.** For each village gate (at most one per side of the core, the one nearest the city's center), the nearest effective road connects to the gate's edge-ring chunk by a straight or L-shaped run through edge-ring and transition chunks, forced to the tertiary class. In the gate chunk the street continues as a 3-block-wide surface path down or up the soft-edge strip to the core's edge. Whether it may continue over core-chunk ground outside every piece box to meet the village path is owner question 1 (default here: it stops at the core's edge).
- **Villages near a city**, outside it but within `villageLinkDistance` (4 chunks) of its edge, get gates and a surface lane across the land between, written in the terrain phase as part of the city's footprint.
- **Height caps** (meteorite basins) reduce floors after upstream's floor draw, so random consumption is unchanged.
- **City level** near a landmark comes from the same height oracle as the landmark's ground estimate.
- Profile options replace upstream's avoidance keys:

| Upstream key | Here |
|---|---|
| `avoidVillages` (true) | `villageTreatment`: `BUILD_AROUND` (default), `AVOID` (core and a non-city ring, no sewing: upstream's behavior), `IGNORE` |
| `avoidStructures` (vanilla ids) | `landmarkTreatment` per kind: `BUILD_AROUND` (default), `AVOID`, `IGNORE` |
| `avoidVillagesAdjacent`, `avoidStructuresAdjacent` | Replaced by the edge ring, which always exists for `BUILD_AROUND` |
| `avoidFlattening` (true) | Always true for cores |
| (none) | `landmarkMargin` (2 blocks), `softEdgeWidth` (6), `villageTransitionRings` (2), `villageLinkDistance` (4 chunks) |

### 4. Edge rules

The rules E1 to E8 of [coexistence.md § Soft edges](../new-spec/00-pack-integration/coexistence.md#soft-edges) are adopted. In short:

- **E1, cores are fixed ground in the blend.** Upstream's L0 step treats a corner shared by a core chunk and a non-city chunk as fixed at the core's oracle height (−1 to +1), as it fixes city corners at the city ground. L1, L2 and the transition noise are upstream's. Core chunks are never reshaped.
- **E2 to E4, soft-edge strips instead of walls.** A city chunk's side facing a core has no border wall. A strip `softEdgeWidth` columns wide, varied ±2 by the transition noise, moves the ground from the city ground to the column's own natural height at the core's edge, keeping the column's own top block. The terrain phase already reads its own chunk's RWG columns ([0005](0005-rwg-overlay.md) §4), and those are pure. Where the step is more than twice the strip, a retaining border at the inner edge takes the rest, no taller than needed.
- **E5 to E8:** edge-ring parks keep RWG's decoration; density falls off; nothing city-made underground in the edge ring; lanes are 3 wide and follow the strip.

### 5. Order and determinism

- Every layout input is pure; the layout never reads blocks other mods placed, `data/Village.dat`, AE2's spawn data, or the live structure generators' maps. Those fill in generation order and are used only by tests that check the oracles.
- Other structures' blocks land after our terrain phase. They land in cores, on RWG's ground, so they behave as in the wild.
- Our population phase writes only inside our footprint; cores are outside it.
- **Steering.** One compat mixin, on `Dungeon.validLocation` (Roguelike Dungeons), makes a candidate invalid inside a city chunk or edge ring unless it is that dungeon's predicted site. The dungeon lands in its lot, outside the city, or not at all, whatever order chunks generate in.
- **Misses.** A lot whose structure does not appear stays natural ground. A structure that lands in the city anyway (Thaumcraft) is accepted; a per-generator guard (T4) follows only if the alpha screenshots show it. No restore pass ever rewrites another mod's blocks.

### 6. What replaces D21's treatments

| D21 | Here |
|---|---|
| T1, villages avoided | Built around and sewn in (§1 to §4) |
| T1, LootGames, RWG2 landmarks | Built around (LootGames as a lot, RWG2 landmarks as terrain landmarks) |
| T4 after alpha, Roguelike Dungeons | Built around a predicted lot, with the steering mixin in M08 |
| T4 after alpha, Witchery | Built around the union lot; no mixin needed |
| T3, AE2 meteorites | A crater basin (proposed; owner question 2) |
| T2 (dungeons, lakes, oil, quarries) | Unchanged in city chunks; not applied in cores |
| T3 (underground structures, small decoration, Thaumcraft) | Unchanged; Thaumcraft watched at alpha |

## Consequences

- **M03 (layout):**
  - `StructureOracle`, `Landmark` and the zone derivation in `layout`, with test fakes.
  - Core and edge-ring rules in `computeChunkCharacteristics` and `BuildingInfo`; multibuilding rejection; transition-ring density; height caps.
  - Road detours, plazas (`FULL` made reachable), lanes and gates.
  - Golden layout tests with fixed fake oracles: a village inside a city, one at its edge, one 3 chunks out; a severed primary road; a meteorite basin.
- **M04 (terrain):**
  - No writes in cores, including the footprint write guard and T2 denial.
  - The E1 blend anchor; soft-edge strips; lane writing on blended ground.
  - Oracle calls inside the time budget (B1 to B5).
- **M06 (highways and railways):** cores are obstacles; the planners route around them or end before them.
- **M07 (scattered structures):** upstream's `StructureAvoidance` is replaced by this record, not ported; scattered structures stay out of cores and edge rings.
- **M08 (GTNH layer):**
  - The per-mod oracles in `compat.villagenames`, `compat.roguelike`, `compat.witchery`, `compat.lootgames`, `compat.ae2` and `compat.rwg2`.
  - The Roguelike steering mixin.
  - Real-pack checks that the oracles match placed structures.
- **M09 (profiles):** the options in §3; the oracle inputs in the pinned snapshot.
- **Alpha screenshot set (M05)** adds: a village inside a city and one at its edge, with lanes; a plaza facing a landmark; soft-edge strips; an empty lot; a crater basin if chosen.
- **Spec deviations** to record: `07-scattered` (structure avoidance replaced); `04-city-layout` (chunk characteristics, multibuildings, street planner clip, `FULL` street type, floor caps); `03-terrain` (blend anchors, no border wall toward a core, lanes); `02-profiles-dimensions` (the options).
- **Dependencies:** VillageNames, Roguelike Dungeons, LootGames and AE2 as `compileOnly`; Witchery through its jar's class names only. All under `compat`, all optional.
- **Licensing:** calling VillageNames' and Roguelike Dungeons' classes and mixing into them is integration under D23; no GPL code is copied. Witchery's grid is a numeric contract learned from the decompiled jar, written as our own code.
- **Pack changes:** none.

## Alternatives considered

| Alternative | Why not |
|---|---|
| Upstream's avoidance (D21's T1): a hole in the city and a non-city ring | Leaves holes rather than features, has no paths into villages, and its ring is a hard city edge. Kept as the `AVOID` option |
| Learn structures from the live generators (`structureMap`, `data/Village.dat`, AE2's spawn data) | They fill in generation order, and only within 8 chunks of generated chunks, so layout would depend on order. Used only by tests |
| Let other structures land and restore the city afterwards | Either their structure or ours is destroyed; restore passes are slow and span chunks |
| Deny every structure in cities (T4 for all) | Simple and clean for the city, but the landscape loses villages, towers and meteorites wherever cities are, against D24 |
| Sub-chunk lots everywhere | Upstream's layout, planner and blend are chunk-granular; rewriting them at block granularity is a large deviation. Soft-edge strips and lanes give the sub-chunk detail where it shows |
| Generate other mods' structures ourselves, as our own multibuildings | Copies GPL and closed content and duplicates their generators |
| Our own copy of VillageNames' village layout | GPL code we would copy and keep in step; calling VillageNames' constructor is exact and allowed |
| A full non-city green belt around every landmark | Fine for villages and terrain landmarks, but a 4 × 4-chunk hole for a 7 × 7 shack. Edge-ring parks keep the city continuous |
| AE2: let the crater carve (D21) | Slices tall buildings within about 50 blocks: a sharp cutoff |

## Still open

Owner questions, each with the recommendation in [coexistence.md § Questions for the owner](../new-spec/00-pack-integration/coexistence.md#questions-for-the-owner):

1. Surface-only lanes over village core chunks, outside every piece box (recommended), or lanes that stop at the core's edge.
2. AE2 meteorites: crater basin (recommended), guard, or let them carve.
3. Build the Roguelike steering mixin in M08 regardless of the alpha screenshots (recommended).
4. Cities surround villages within their radius (recommended), or stop short of them.

For the prototype and the alpha screenshots:

- The village oracle against `data/Village.dat` over many starts, and the side effects of a private `Start` construction.
- How often VillageNames villages start in RWG2's biomes.
- The Roguelike prediction's hit rate.
- The look of edge strips, plazas, lanes, empty lots and crater basins; `softEdgeWidth` and the transition-ring numbers; how often retaining borders appear.
- Thaumcraft mounds in city parks.
- The oracles' cost against the budget.
