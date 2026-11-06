# The terrain pipeline on RWG

Where each stage of upstream's per-chunk pipeline ([generation-pipeline.md](../../original-spec/03-terrain/generation-pipeline.md)) runs once the city is an overlay on RWG, what each phase may touch, and the three pieces of terrain work upstream does not have: the RWG decoration footprint, soft-edge strips toward landmark cores, and village lanes.

Status: *Accepted* from [0005](../../design/0005-rwg-overlay.md) and [0006](../../design/0006-build-around-other-worldgen.md), with the footprint's volume (P03-1), the strip's ramp (P03-13) and the lane's path blocks (P03-6) settled by the owner's answers in D31; the leveling fix (TERR-39), the margin's strip and the lane's grade and steps come from the alpha review round (AR-2, AR-7, AR-10, AR-11; D149 to D152, D154).

## The two phases

The delegating chunk provider ([0005](../../design/0005-rwg-overlay.md#1-hook-a-delegating-chunk-provider-q1) §1) gives the overlay two moments per chunk C:

| Phase | Runs | May read | May write | Never |
|---|---|---|---|---|
| **Terrain phase** | After RWG's `provideChunk(C)` returns, before C joins the loaded map | The layout plan for C and its neighbors (pure: seed, coordinates, the pinned profile snapshot, assets, oracle heights, structure oracles); C's own blocks | C's `ExtendedBlockStorage` sections, through the chunk buffer | Neighbor chunks, loaded or not; world lookups for C itself, which joins the loaded map only after `provideChunk` returns ([rwg.md](../00-pack-integration/rwg.md#forge-1710-facts-the-overlay-relies-on)), so block tests use block classes and the ore dictionary, never `IBlockAccess` methods; the live RWG generator; `world.rand`; tile entities; biomes; replacing the `Chunk` object |
| **Population phase** | After RWG's `populate(C)` returns (snow and ice included), before every `IWorldGenerator` | The plan; the world inside C | Blocks and tile entities inside C, through the world | Any block outside C; RWG's or an event's `Random` (it seeds its own from the world seed and C's coordinates) |

During RWG's own `populate`, two of our listeners act without being a phase: the `Populate` vetoes (`LAKE`, `LAVA`, `DUNGEON`, `CUSTOM`, `RAILCRAFT_QUARRY`) when the population area touches a city chunk, and the first four where it touches a scattered structure's footprint (P07-4, D201), and the footprint write guard, active for the whole of every delegate `populate` ([below](#the-rwg-decoration-footprint)).

## The plan the overlay reads

The overlay asks one pure interface what each chunk holds: `worldgen.plan.CityPlan.chunk(x, z)`, which returns an immutable `ChunkPlan` (decision log D71). It never reads the world, and the overlay wraps it in a bounded cache (`CachedCityPlan`, 4,096 chunks, budget B5). The real source is `worldgen.city.LayoutCityPlan`, over M03's `LayoutPlanner` and M02's assets, parts and palettes (D99); the synthetic test city (D72) stays the pinned test-only source.

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
| `inGuardBand(y)` | Whether a Y is in the chunk's guarded band: the Y range of its highway tunnels, railway parts and rail dungeon, in every column (M06) | the write guard |
| `lane` (`LanePath`) | The village lane's columns and the village's material | lanes |
| `jobs` (`PopulationJob`) | Torches, chests (with their loot category), spawners (with their mob), tile entities (with their template), command blocks; listed lazily by a dry run of the content writer (D100) | the population phase |
| `keepsClear(x, z)` | The columns in front of a building's street door, which the rubble layer leaves clear (D112) | the rubble layer |
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
| 5 | City chunk (`doCityChunk`) or normal chunk (`doNormalChunk`: blend, bridges, scattered; highways move to step 7) | Terrain phase | Leveling lowers terrain standing exactly one block above the city ground too (TERR-39). Blend corners next to a core are fixed at its natural height (E1); in the margin beside a core a soft-edge strip then meets the core's edge columns, and a lane is graded and stepped |
| 6 | Sphere centerpiece | Deferred | D12 |
| 7 | Highways, railways and rail dungeons | Terrain phase | Last in the chunk, after the city's content, street borders, ruins and rubble, or after the blend and a bridge outside a city, so nothing later in the chunk cuts them; highways before railways. In a landmark core only a railway deep enough to pass under it ([06-highways-railways](../06-highways-railways/index.md), [0013](../../design/0013-highway-and-railway-networks.md) §4) |
| 8 | `fixTorches` | Population phase | [0005](../../design/0005-rwg-overlay.md#3-population-phase) §3 lists torch and attachment fix-ups there |
| 9 | Reseed to a world-independent seed | Terrain phase | The seed now includes the world seed (TERR-27) |
| 10 | `PreExplosionEvent`, explosion damage, debris | Terrain phase | Pure and confined to C; followed by the floating-fragment pass ported from 1.12 (TERR-28) |
| 11 | Early commit for second parts | None | Writes are direct, so there is nothing to commit |
| 12 | Commit and heightmap raise | Terrain phase, last step | Recompute the height map and sky light (`generateSkylightMap`) for C once, RWG's own pass on C deferred to here (TERR-13, D122) |
| 13a | `ChunkFixer`: vines | Terrain phase | Vines in C's own columns, against neighbors' walls as the plan describes them (TERR-11) |
| 13b | `ChunkFixer`: post-todo jobs (loot, light refresh, saplings, command blocks; POI swap) | Population phase | POI swap dropped (no POI in 1.7.10); light refresh dropped, since ArchaicFix's Phosphor lights every source (P03-8); saplings placed, never grown (P03-10) |
| 13c | `ChunkFixer`: border vine cleanup | Dropped | TERR-26 |
| 14 | Close the context; `Statistics` | Both phases | The wrapper's timers ([0005](../../design/0005-rwg-overlay.md#8-time-budget-and-how-it-is-measured-q10) §8) |
| — | `GlobalTodo` tick jobs | Dropped | Forced sapling growth is not ported: saplings are never grown at generation (P03-10; TERR-20, TERR-21) |

The population phase, in order: data-bearing tile entities (chests filled from our `ChestGenHooks` categories, spawners, furnaces and other palette `tag` blocks) with their 1.7.10 NBT layout; torches; sapling stages (a sapling stays a sapling and grows by random ticks later; *Accepted*, P03-10, D31); the command-block tick. It writes no light updates: ArchaicFix's Phosphor, on in the pack, lights every light source in the chunk when it runs vanilla's light population pass ([LightingHooks.java#L329-L369](https://github.com/embeddedt/ArchaicFix/blob/0.8.2/src/main/java/org/embeddedt/archaicfix/lighting/world/lighting/LightingHooks.java#L329-L369); *Accepted*, P03-8, D31), and the headless overlay scenario checks that every glowstone block in a fresh test city is lit.

## The RWG decoration footprint

[0005](../../design/0005-rwg-overlay.md#3a-rwg-sets-the-landscape-the-city-is-set-into-it) §3a: RWG builds its landscape first, terrain and decoration; the city is set into it. Inside the city's footprint RWG's decoration is cleared; outside it stays.

| Inside the footprint | Outside the footprint |
|---|---|
| Building volumes and their foundations (cellars, border skirts) | Park and yard columns, fountains' surroundings |
| Streets, plazas, stairs, slopes and their base | The margin around a core, apart from a lane's columns |
| Bridges and their supports | Blended non-city chunks |
| Corridors | Landmark cores, entirely (village pieces land on RWG's untouched ground) |
| Highway and railway cross-sections | Chunks the city does not touch |

**Mechanism** (0005 §3a). A write guard: one mixin on the chunk's block setter (`Chunk.func_150807_a`) that drops writes inside footprint volumes, answered from pure layout data. It keeps a stack of frames: active around each delegate `populate`, inactive around our population phase, and switched off by `PopulateChunkEvent.Post` (lowest priority), so RWG's snow and ice pass (D75). It is active during every delegate `populate`, not only the chunk being populated, because decoration uses the vanilla +8 offset and lands in up to four chunks, and cascaded big trees reach chunk +2; the guard looks up the chunk each write lands in. The same guard covers mineshaft and stronghold pieces RWG writes in `populate` (P03-2). The real-pack scenario runs measure the spill with ArchaicFix's `logCascadingWorldgen`, off in the pack's config and switched on for those runs ([docs/testing](../../testing/README.md)). The guard tells RWG's writes apart on both world types ([0005](../../design/0005-rwg-overlay.md#still-open) "Still open" item 4), so the footprint restore 0005 §3a keeps as a fallback is not built.

**Door approaches, other mods' trees and GregTech boulders** (D285 to D288). The frames above have four additions. (1) After `Post` the frame is not off but in snow mode: it drops only snow layers (material snow), and only in a door approach (the street chunk's two door columns, `ChunkPlan.keepsClear`, from one to two blocks above the city ground), because snow pass writes after `Post` landed in doorways (a snowy classic RWG seed: 8 of 134 doors open before, 134 of 134 after). (2) In an approach, tree parts (logs, leaves, plants, vines, cacti) are dropped in every frame, in or out of the footprint, because a park beside a building is outside the footprint (RWG's trees and other mods' crowns overhung doors: RWG2 seed 1, 200 of 212 doors open before, 228 of 228 after). (3) GregTech's world generator runs after our closing tree generator (GT5-Unofficial@5.09.54.265:src/mixin/java/gregtech/mixin/mixins/early/forge/GameRegistryMixin.java moves it to the end), so no tree frame covers its stone boulders (`WorldgenStone.executeWorldgen`, which replaces stone, cobblestone and stained clay with `World.setBlock`: GT5-Unofficial@5.09.54.265:src/main/java/gregtech/common/WorldgenStone.java#L69-L240). A late mixin (`MixinGtWorldgenStone`, only with GregTech loaded) pushes a stone frame around each call, which drops every write in the footprint, below it and outside it nothing; GT's ore veins and small ores are other classes and pass. (4) Other mods' trees keep the tree frame of before, now with (2) added. Tests: `FootprintGuardTest` (approach, snow, stone frame) and `LateMixinLoaderTest`.

**Volume** (*Accepted*, P03-1, D31). The outcome the owner asked for is no trees on streets and roofs (D22), so a footprint column must reach above the surface it protects: for each column the city writes in a building, street, bridge, highway or railway chunk, everything from its lowest written block up to Y 255, plus, for highways, the 15-block clearance above open and bridge parts by the same rule. A highway's footprint starts at its deck (open roads, bridges, ramps and plazas); a tunnel's and an underground railway's leave the ground above them to RWG; a surface station's starts at its floor. Park, yard, edge-ring and soft-edge columns are outside it. RWG's snow and ice, added after `PopulateChunkEvent.Post`, pass the guard. `IWorldGenerator`s run after our population phase inside a tree frame (D109): only tree parts written into a footprint are dropped (logs, leaves, and the wood, leaves, plants, vine and cactus materials), so the IC2 rubber trees and HarvestCraft fruit trees that M04 found overhanging streets (31 leaf blocks in its test city, D75) stop at the footprint, and Natura's saguaros stay out of scattered structures (D234), while GT's ore veins and every other block pass. A park tree whose crown overhangs a street or roof is cut at the footprint's edge.

## Soft-edge strips

Rules E2 to E4 of [coexistence.md § Soft edges](../00-pack-integration/coexistence.md#soft-edges), adopted by [0006](../../design/0006-build-around-other-worldgen.md#4-edge-rules) §4. The layout puts the city's margin, a ring of non-city chunks, around every landmark core (AR-10), so a city chunk never faces a core. The strip lies in the margin chunk, the non-city chunk C beside the core:

1. Before the blend, C's edge column on each side facing a core is read: its solid surface through plants, or the top of the water standing on it.
2. The blend (E1) slopes C from the city's ground at its city corners toward the core's oracle height at its core corners.
3. A strip on each side facing the core then ramps the blended ground back to the edge columns read in step 1.

| Item | Value | Source |
|---|---|---|
| Width | `softEdgeWidth` 6 columns, varied ±2 per column along the side by upstream's terrain transition noise | E2, E4 |
| Inner edge | The blended height of C's column at the strip's width from the edge | E2 |
| Outer edge | The column's own RWG height at the core's edge, read from C's own columns before the blend: its solid surface, or the top of the water standing on it, so a pond or river in the core meets the strip at its waterline | E2; [0005](../../design/0005-rwg-overlay.md#4-height-oracle) §4; D129 |
| Surface | Each column keeps its own top block, moved with upstream's `moveDown` (6 kept layers) or `moveUp` | E2 |
| Corners | A column in two strips follows the side it is nearer to | E2 |
| Decoration | The margin is outside the footprint: RWG's trees and boulders stay | E5 |
| Ramp shape | Linear across the strip (*Accepted*, P03-13, D31) | D31 |

A strip is written only in C's own columns, so it never writes into the core, and nothing the city builds lies in it, so it never cuts a road, a highway or a park (AR-10, AR-12, AR-13). On the city's side of the margin, a street chunk keeps upstream's border toward lower non-city land, opened over the 3 columns where a lane arrives.

A plan that puts a city chunk right beside a core, which only the synthetic test city still does, gets the strip inside the city chunk instead, from the city ground to the core's edge, and a retaining border of the city style's `borderBlock`, no taller than the remaining step, where the step is more than twice the strip's width (E3).

## Village lanes

*Deviation (D283, fixed after 0.1.0-beta):* two lane chunks that touch pick their border columns from the plan alone, so a lane never rises more than one block across a chunk border, in any generation order. Each lane border column that faces a lane column in a non-city, non-core neighbor is pinned into `[s, s + 1]`, `s` being the floor of the mean of the two chunks' oracle heights, before the lane is relaxed; borders against a city or core keep the city's rules.

*Deviation (D284, fixed after 0.1.0-beta):* a non-city chunk under an AE2 crater's paraboloid carries the cap in its plan, and the blend keeps its free corners at or under it, so the margin slopes from the city down under the crater instead of ending in the pit's wall.

[0006](../../design/0006-build-around-other-worldgen.md#3-streets-buildings-and-the-planner) §3, written in the terrain phase before RWG's `populate` places the village pieces:

1. In the gate chunk, the margin chunk in front of the gate, a 3-block-wide surface path runs from the city's street, through the opening in its border, down or up the margin to the core's edge (E8). Before it is laid its columns are graded: each moves, keeping its top blocks, to a straight ramp from the street's ground on the city side to the core's edge column on the far side (read before the blend), so the lane runs down the slope rather than stopping at its top (AR-7); a column beside the lane more than a block from it then moves halfway to it, so the lane is neither a causeway nor a trench.
2. Across the core, a surface-only path to the end of the village path: the lane's blocks replace the natural surface, never within `landmarkMargin` (2 blocks) of a village piece box. Besides the supports and the lily-pad clearing that VillageNames' own rule adds and the steps (below), nothing is written above or below. This is the only thing the city ever writes in a core.
3. For villages outside a city but within `villageLinkDistance` (4 chunks) of its edge, the same surface lane runs across the land between, as part of the city's footprint.

**Path blocks** (*Accepted*, P03-6, D31). Each lane column follows VillageNames' per-column path rule, `setPathSpecificBlock` ([StructureVillageVN.java#L1224-L1328](https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/village/StructureVillageVN.java#L1224-L1328)), so the lane meets the village's paths in the same blocks. From the column's top solid or liquid block (raised to sea level − 1 if lower), scanning down to sea level − 1:

| Surface | Lane block |
|---|---|
| Grass or dirt with air above | The pack's path block: `etfuturum:grass_path`, first in the pack's `Mod Priority: Grass Path` list (`config/VillageNames4/general.cfg`), or `minecraft:gravel` when none is ([ModObjects.java#L3056-L3086](https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/integration/ModObjects.java#L3056-L3086)) |
| Sand | Gravel, with cobblestone in the block below |
| Lava | Cobblestone, with cobblestone posts down through the lava at even X and Z |
| Water, ice or mud | Planks, with the block above cleared (a lily pad) and log posts down through the water at even X and Z |
| Anything else | The next block down; a column that matches nothing keeps its surface |

A column that matches nothing down to sea level − 1 is paved as sand would be when its surface is natural ground the rule names nothing for (terracotta, stained clay, stone, sandstone, gravel, clay, mycelium, snow), so a lane stays continuous on classic RWG's mesas; a city's own blocks are never paved (deviation, D110).

Every block goes through the village's biome material swaps ([StructureVillageVN.java#L945-L1003](https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/village/StructureVillageVN.java#L945-L1003) for sand and mesa villages): a desert village's gravel and cobblestone become sandstone, a mesa village's hardened clay. The material comes from the main biome of each chunk the lane crosses (`MESA` mesa, `SANDY` and `HOT` sand, else plain; `LayoutCityPlan.material`), the biome test VillageNames' village types follow; the village oracle does not report the village's own type. Where a lane runs in each chunk is D111: 3 wide, along a link chunk's connections, in line with the gate in the gate chunk, and from the gate straight out across the core. The rule is pure over C's own blocks, as the terrain phase requires, so it reads no world.

**Steps** (deviation, AR-11). Once laid, a lane column whose lane neighbor along X or Z in the same chunk is exactly one block higher gets a step on top of its path block: a stair of the path's material facing up the slope (cobblestone, sandstone on sandstone, brick on hardened clay) where the lane rises again beyond that neighbor, else a bottom half slab of it, so a single step is a half-block landing. Rises of two or more blocks are left as they are; the grade keeps the margin's lane to one block a column where its two ends are no more than 15 blocks apart. VillageNames' own paths and the first lanes climbed in whole blocks, which nobody walks.

## Measurement points

| Point | What | Source |
|---|---|---|
| Delegate `provideChunk`, terrain phase, delegate `populate`, population phase, oracle calls | Fixed-bucket `System.nanoTime` histograms, split by city, blend and untouched chunks; budgets B1 to B5 | [0005](../../design/0005-rwg-overlay.md#8-time-budget-and-how-it-is-measured-q10) §8 |
| After the terrain phase | Golden-chunk hash | [0005](../../design/0005-rwg-overlay.md#consequences) |
| After our population phase | A second golden hash, never after the whole of population | [0005](../../design/0005-rwg-overlay.md#consequences) |
