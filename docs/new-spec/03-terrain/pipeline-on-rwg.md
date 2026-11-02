# The terrain pipeline on RWG

Where each stage of upstream's per-chunk pipeline ([generation-pipeline.md](../../original-spec/03-terrain/generation-pipeline.md)) runs once the city is an overlay on RWG, what each phase may touch, and the three pieces of terrain work upstream does not have: the RWG decoration footprint, soft-edge strips toward landmark cores, and village lanes.

Status: *Accepted* from [0005](../../design/0005-rwg-overlay.md) and [0006](../../design/0006-build-around-other-worldgen.md), with the footprint's volume (P03-1), the strip's ramp (P03-13) and the lane's path blocks (P03-6) settled by the owner's answers in D31.

## The two phases

The delegating chunk provider ([0005](../../design/0005-rwg-overlay.md#1-hook-a-delegating-chunk-provider-q1) §1) gives the overlay two moments per chunk C:

| Phase | Runs | May read | May write | Never |
|---|---|---|---|---|
| **Terrain phase** | After RWG's `provideChunk(C)` returns, before C joins the loaded map | The layout plan for C and its neighbors (pure: seed, coordinates, the pinned profile snapshot, assets, oracle heights, structure oracles); C's own blocks | C's `ExtendedBlockStorage` sections, through the chunk buffer | Neighbor chunks, loaded or not; world lookups for C itself, which joins the loaded map only after `provideChunk` returns ([rwg.md](../00-pack-integration/rwg.md#forge-1710-facts-the-overlay-relies-on)), so block tests use block classes and the ore dictionary, never `IBlockAccess` methods; the live RWG generator; `world.rand`; tile entities; biomes; replacing the `Chunk` object |
| **Population phase** | After RWG's `populate(C)` returns (snow and ice included), before every `IWorldGenerator` | The plan; the world inside C | Blocks and tile entities inside C, through the world | Any block outside C; RWG's or an event's `Random` (it seeds its own from the world seed and C's coordinates) |

During RWG's own `populate`, two of our listeners act without being a phase: the `Populate` vetoes (`LAKE`, `LAVA`, `DUNGEON`, `CUSTOM`) when the population area touches a city chunk, and the footprint write guard, active for the whole of every delegate `populate` ([below](#the-rwg-decoration-footprint)).

## The plan the overlay reads

The overlay asks one pure interface what each chunk holds: `worldgen.plan.CityPlan.chunk(x, z)`, which returns an immutable `ChunkPlan` (decision log D71). It never reads the world, and the overlay wraps it in a bounded cache (`CachedCityPlan`, 4,096 chunks, budget B5). The real source is `worldgen.city.LayoutCityPlan`, over M03's `LayoutPlanner` and M02's assets, parts and palettes (D-M05J-1); the synthetic test city (D72) stays the pinned test-only source.

| Field | Meaning | Read by |
|---|---|---|
| `kind` | `NONE`, `CITY` or `LANDMARK_CORE` | every step |
| `cityLevel`, `groundY` | The city level and its ground Y (`groundLevel + 6 × level`); for a core, its oracle height; for land, the level its height gives | leveling, borders, blend corners (L0, E1), explosions' center Y |
| `naturalHeight`, `ocean` | The oracle height and whether the biome is an ocean | `getMinHeightAt`, `clearToMax`, the blend's free corners and ocean corners |
| `hasBuilding`, `floors`, `cellars` | The building; its bottom is `groundY − 6 × cellars`, never below Y 2 (TERR-31: the plan rejects a building that cannot fit) | `makeRoomForBuilding`, debris, vines, the fragment pass |
| `ruinHeight`, `explosionChance` | The ruin fraction; the city style's explosion chance | ruins, `DamageArea` |
| `style` (`StyleBlocks`) | Base, liquid, border, wall, rubble, filler, iron bars, leaf and rubble tables, damaged forms | street base, borders, skirts, damage, debris |
| `content` (`ChunkContent`) | Writes the building or street itself into the buffer, after leveling and room-making, skipping soft-edge columns; on a non-city chunk, the bridge span over it (never on a core) | the city chunk step; the blend step for bridges |
| `footprintBottom(x, z)` | Lowest Y of a footprint column, or none | the write guard, the population hash |
| `lane` (`LanePath`) | The village lane's columns and the village's material | lanes |
| `jobs` (`PopulationJob`) | Torches, chests (with their loot category), spawners (with their mob), tile entities (with their template), command blocks; listed lazily by a dry run of the content writer (D-M05J-2) | the population phase |
| `keepsClear(x, z)` | The columns in front of a building's street door, which the rubble layer leaves clear (D-M05J-14) | the rubble layer |
| `label` | What stands here, for the finder and logs | `/lostcities find`, scenarios |
| `borderOpenings(side)` | Columns where a street border is one lower, for slopes, stairs and bridges from the neighbor | borders |

The terrain phase owns everything else upstream's `doCityChunk` and `doNormalChunk` do around the content: high water, leveling, room for buildings, `clearToMax` (exposed for the content's roof), street base and borders, ruins and the street rubble layer (`worldgen.terrain.Ruins`), soft-edge strips and their retaining walls, lanes, explosion damage, the fragment pass, debris and vines. The blend's L1 and L2 meshes are built from the plans' city grounds (`TerrainBlend`), so the layout's own corner heights need no separate field. The layout's own `TerrainView` (D51) is implemented over the same oracle by `worldgen.overlay.WorldTerrainView`.

## Stage map

Upstream's stages are numbered as in [generation-pipeline.md](../../original-spec/03-terrain/generation-pipeline.md#lostcityterrainfeaturegenerate-server).

| # | Upstream stage | Here | Notes |
|---|---|---|---|
| 1 | Open the generation context (seeded random, palette sequence, buffers) | Terrain phase | Seeds mix in the world seed everywhere (TERR-27) |
| 2 | Attach the `ChunkDriver` | Terrain phase | The chunk buffer over C's sections; no staging |
| 3 | Fetch the heightmap and `BuildingInfo` | Terrain phase | The plan from `layout`, built from oracle heights and structure oracles; validated completely before the first write (TERR-25) |
| 4 | City or normal; floating void probe | Terrain phase | The void probe is deferred with the floating landscape (D12). A **landmark core** stops here: nothing is written but a village lane's surface path |
| 5 | City chunk (`doCityChunk`) or normal chunk (`doNormalChunk`: blend, bridges, highways, scattered) | Terrain phase | Toward a core, soft-edge strips replace border walls; blend corners next to a core are fixed at its natural height (E1) |
| 6 | Sphere centerpiece | Deferred | D12 |
| 7 | Railways and rail dungeons | Terrain phase | `06-highways-railways` |
| 8 | `fixTorches` | Population phase | [0005](../../design/0005-rwg-overlay.md#3-population-phase) §3 lists torch and attachment fix-ups there |
| 9 | Reseed to a world-independent seed | Terrain phase | The seed now includes the world seed (TERR-27) |
| 10 | `PreExplosionEvent`, explosion damage, debris | Terrain phase | Pure and confined to C; followed by the floating-fragment pass ported from 1.12 (TERR-28) |
| 11 | Early commit for second parts | None | Writes are direct, so there is nothing to commit |
| 12 | Commit and heightmap raise | Terrain phase, last step | Recompute the height map and sky light (`generateSkylightMap`) for C (TERR-13) |
| 13a | `ChunkFixer`: vines | Terrain phase | Vines in C's own columns, against neighbors' walls as the plan describes them (TERR-11) |
| 13b | `ChunkFixer`: post-todo jobs (loot, light refresh, saplings, command blocks; POI swap) | Population phase | POI swap dropped (no POI in 1.7.10); light refresh dropped, since ArchaicFix's Phosphor lights every source (P03-8); saplings placed, never grown (P03-10) |
| 13c | `ChunkFixer`: border vine cleanup | Dropped | TERR-26 |
| 14 | Close the context; `Statistics` | Both phases | The wrapper's timers ([0005](../../design/0005-rwg-overlay.md#8-time-budget-and-how-it-is-measured-q10) §8) |
| — | `GlobalTodo` tick jobs | Dropped | Forced sapling growth is not ported: saplings are never grown at generation (P03-10; TERR-20, TERR-21) |

The population phase, in order: data-bearing tile entities (chests filled from our `ChestGenHooks` categories, spawners, furnaces and other palette `tag` blocks) with their 1.7.10 NBT layout; torches; sapling stages (a sapling stays a sapling and grows by random ticks later; *Accepted*, P03-10, D31); the command-block tick. It writes no light updates: ArchaicFix's Phosphor, on in the pack, lights every light source in the chunk when it runs vanilla's light population pass ([LightingHooks.java#L329-L369](https://github.com/embeddedt/ArchaicFix/blob/0.8.2/src/main/java/org/embeddedt/archaicfix/lighting/world/lighting/LightingHooks.java#L329-L369); *Accepted*, P03-8, D31), and M04's scenario checks lamps and glowstone at night.

## The RWG decoration footprint

[0005](../../design/0005-rwg-overlay.md#3a-rwg-sets-the-landscape-the-city-is-set-into-it) §3a: RWG builds its landscape first, terrain and decoration; the city is set into it. Inside the city's footprint RWG's decoration is cleared; outside it stays.

| Inside the footprint | Outside the footprint |
|---|---|
| Building volumes and their foundations (cellars, border skirts) | Park and yard columns, fountains' surroundings |
| Streets, plazas, stairs, slopes and their base | Edge-ring parks and soft-edge strips |
| Bridges and their supports | Blended non-city chunks |
| Corridors | Landmark cores, entirely (village pieces land on RWG's untouched ground) |
| Highway and railway cross-sections | Chunks the city does not touch |

**Mechanism** (0005 §3a). A write guard: one mixin on the chunk's block setter (`Chunk.func_150807_a`) that drops writes inside footprint volumes, answered from pure layout data. It keeps a stack of frames: active around each delegate `populate`, inactive around our population phase, and switched off by `PopulateChunkEvent.Post` (lowest priority), so RWG's snow and ice pass (D75). It is active during every delegate `populate`, not only the chunk being populated, because decoration uses the vanilla +8 offset and lands in up to four chunks, and cascaded big trees reach chunk +2; the guard looks up the chunk each write lands in. The same guard covers mineshaft and stronghold pieces RWG writes in `populate` (P03-2). The prototype measures the spill with ArchaicFix's `logCascadingWorldgen`, off in the pack's config and switched on for the scenario. A footprint restore after decoration is the fallback, used only if the guard cannot tell the writes apart.

**Volume** (*Accepted*, P03-1, D31). The outcome the owner asked for is no trees on streets and roofs (D22), so a footprint column must reach above the surface it protects: for each column the city writes in a building, street, bridge, highway or railway chunk, everything from its lowest written block up to Y 255, plus, for highways, the 15-block clearance above open and bridge parts by the same rule. Park, yard, edge-ring and soft-edge columns are outside it. RWG's snow and ice, added after `PopulateChunkEvent.Post`, pass the guard. `IWorldGenerator`s run after our population phase inside a tree frame (D-M05J-11): only tree parts written into a footprint are dropped (logs, leaves, and the wood, leaves, plants and vine materials), so the IC2 rubber trees and HarvestCraft fruit trees that M04 found overhanging streets (31 leaf blocks in its test city, D75) stop at the footprint, while GT's ore veins and every other block pass. Cut canopies where a park tree overhangs a street are judged in the S2 screenshots ([0005](../../design/0005-rwg-overlay.md#still-open) "Still open" item 4).

## Soft-edge strips

Rules E2 to E4 of [coexistence.md § Soft edges](../00-pack-integration/coexistence.md#soft-edges), adopted by [0006](../../design/0006-build-around-other-worldgen.md#4-edge-rules) §4. They replace `generateBorders`' border wall ([city-chunk-terrain.md](../../original-spec/03-terrain/city-chunk-terrain.md#street-edge-borders-generateborders-part-2-server)) on every side of a city chunk that faces a landmark core.

| Item | Value | Source |
|---|---|---|
| Width | `softEdgeWidth` 6 columns, varied ±2 per column along the side by upstream's terrain transition noise | E2, E4 |
| Inner edge | The city ground of the chunk | E2 |
| Outer edge | The column's own RWG height at the core's edge, read from C's own columns | E2; [0005](../../design/0005-rwg-overlay.md#4-height-oracle) §4 |
| Surface | Each column keeps its own top block, moved with upstream's `moveDown` (6 kept layers) or `moveUp` | E2 |
| Steep step | When the height difference exceeds twice the strip's width, the strip ramps as far as it can and a retaining border of the city style's `borderBlock`, no taller than the remaining step, stands at the strip's inner edge | E3 |
| Decoration | Outside the footprint: RWG's trees and boulders stay | E5 |
| Ramp shape | Linear across the strip (*Accepted*, P03-13, D31). The E3 threshold means a strip absorbs at most 2 blocks of height per column | D31 |

A strip is written only in the city chunk's own columns, so it never writes into the core.

## Village lanes

[0006](../../design/0006-build-around-other-worldgen.md#3-streets-buildings-and-the-planner) §3, written in the terrain phase before RWG's `populate` places the village pieces:

1. In the gate's edge-ring chunk the street continues as a 3-block-wide surface path, down or up the soft-edge strip to the core's edge (E8).
2. Across the core, a surface-only path to the end of the village path: the lane's blocks replace the natural surface, never within `landmarkMargin` (2 blocks) of a village piece box. Besides the supports and the lily-pad clearing that VillageNames' own rule adds (below), nothing is written above or below. This is the only thing the city ever writes in a core.
3. For villages outside a city but within `villageLinkDistance` (4 chunks) of its edge, the same surface lane runs across the land between, as part of the city's footprint.

**Path blocks** (*Accepted*, P03-6, D31). Each lane column follows VillageNames' per-column path rule, `setPathSpecificBlock` ([StructureVillageVN.java#L1224-L1328](https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/village/StructureVillageVN.java#L1224-L1328)), so the lane meets the village's paths in the same blocks. From the column's top solid or liquid block (raised to sea level − 1 if lower), scanning down to sea level − 1:

| Surface | Lane block |
|---|---|
| Grass or dirt with air above | The pack's path block: `etfuturum:grass_path`, first in the pack's `Mod Priority: Grass Path` list (`config/VillageNames4/general.cfg`), or `minecraft:gravel` when none is ([ModObjects.java#L3056-L3086](https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/integration/ModObjects.java#L3056-L3086)) |
| Sand | Gravel, with cobblestone in the block below |
| Lava | Cobblestone, with cobblestone posts down through the lava at even X and Z |
| Water, ice or mud | Planks, with the block above cleared (a lily pad) and log posts down through the water at even X and Z |
| Anything else | The next block down; a column that matches nothing keeps its surface |

A column that matches nothing down to sea level − 1 is paved as sand would be when its surface is natural ground the rule names nothing for (terracotta, stained clay, stone, sandstone, gravel, clay, mycelium, snow), so a lane stays continuous on classic RWG's mesas; a city's own blocks are never paved (deviation, D-M05J-12).

Every block goes through the village's biome material swaps ([StructureVillageVN.java#L945-L1003](https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/village/StructureVillageVN.java#L945-L1003) for sand and mesa villages): a desert village's gravel and cobblestone become sandstone, a mesa village's hardened clay. Until the village oracle reports the material type, it comes from the gate chunk's biome (`MESA` mesa, `SANDY` and `HOT` sand, else plain); a lane to a village outside the city uses that village's. Where a lane runs in each chunk is D-M05J-13: 3 wide, along a link chunk's connections, in line with the gate in the gate chunk, and from the gate straight out across the core. The rule is pure over C's own blocks, as the terrain phase requires, so it reads no world.

## Measurement points

| Point | What | Source |
|---|---|---|
| Delegate `provideChunk`, terrain phase, delegate `populate`, population phase, oracle calls | Fixed-bucket `System.nanoTime` histograms, split by city, blend and untouched chunks; budgets B1 to B5 | [0005](../../design/0005-rwg-overlay.md#8-time-budget-and-how-it-is-measured-q10) §8 |
| After the terrain phase | Golden-chunk hash | [0005](../../design/0005-rwg-overlay.md#consequences) |
| After our population phase | A second golden hash, never after the whole of population | [0005](../../design/0005-rwg-overlay.md#consequences) |
