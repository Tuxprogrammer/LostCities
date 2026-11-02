# 0009 · Scattered structures in RWG terrain

- Status: Accepted (decision log D-M07-1 to D-M07-17)
- Gate: M07 ([ROADMAP](../ROADMAP.md#m07--scattered-structures))
- Amends: [0005](0005-rwg-overlay.md) §2 (a non-city chunk's terrain phase gains the ground fit) and [0006](0006-build-around-other-worldgen.md) Consequences, M07 (where footprints may stand)
- Date: 2026-11-02

## Context

Upstream's scattered structures (cabins, radio towers, oil rigs, and gas stations and restaurants beside highways) are placed one per area of 8 × 8 chunks, at a height taken from the terrain under them, and written over that terrain as it is ([original-spec/07-scattered](../original-spec/07-scattered/scattered-structures.md)). Nothing fits the ground to the structure:

- a part's spaces keep the world's blocks, so ground higher than the floor stands inside a cabin, and ground lower than its foundation leaves the foundation in the air;
- a full-chunk part (the gas station's pavement, the restaurant's parking) ends at the chunk's edge as a wall cut into a hill, or a step over a dip;
- upstream's own terrain correction reshapes non-city chunks near a city before the structure is written, after its terrain stats were measured, so a structure near a city can be buried or left floating.

The alpha review (AR-10 to AR-12) set the bar for anything this mod sets into RWG's land: no cliff where it meets the terrain, and nothing cut or half placed. The task for M07 restates it for scattered structures.

The overlay writes chunk by chunk, each chunk reading only its own blocks and pure plans ([0005](0005-rwg-overlay.md) §2), so any fitting must be decided per column from pure data and the column's own surface, and two chunks fitted apart must still meet.

## Decision

### 1. Where a structure may stand

Upstream's placement is kept: areas, the chance with `scatteredChanceMultiplier`, the weighted pick, two position draws or the near-highway search, and the terrain stats from the height oracle. A footprint chunk must also pass these tests, each an immutable pure answer:

| Rule | Why |
|---|---|
| Not in any landmark zone (core, margin, transition or avoid ring) and under no meteorite cap | Replaces upstream's structure avoidance ([0006](0006-build-around-other-worldgen.md)); a crater would cut the structure |
| No city chunk and no landmark core within 2 chunks (`ScatteredPlanner.BLEND_CLEARANCE`) | The terrain blend holds a chunk beside a city within a few blocks of the city's ground and one two chunks out within about 22 blocks, after the plan measured them; three chunks out it moves only ground more than about 40 blocks off the city's, which the height-difference test all but rules out. So the ground is RWG's own, as the stats measured it. Raw city is tested first, the cheap superset; a raw city chunk's characteristics then say whether it is city |
| Not a bridge or highway chunk | Upstream's `avoidScattered` |
| Two structures of neighboring areas may not touch (footprints within one chunk); the one of the smaller area index, Z then X, keeps its place | Neither may stand in the other's ground fit. Both are raw plans, so the answer does not depend on order |
| An `ocean` structure needs the oracle floor at Y 45 or lower under every footprint chunk | RWG2's deep ocean (D31); classic RWG has no ocean biome, so no oil rig |
| A structure that stands on the ground (not `ocean`) has its ground at or above Y 62, the sea's surface | Below it, the leveled pad lies under the water RWG filled to Y 62, and the cabin's floor is flooded; the first classic RWG run found a cabin with its ground at Y 60 |
| The structure's top stays at or below Y 254 and its first floor at or above Y 2 | The 1.7.10 build range |
| The oracle's ground 6 blocks beyond the footprint lies within 8 blocks of the structure's ground, and 12 blocks beyond within 12, at sixteen points each (corners, quarters and middles of each side); for a structure on supports only higher ground counts | The ground fit's fade (§2) steepens the land by the difference times its weight's slope, at most 1.5 / 13 a block: within 8 blocks that is about one block a column, so the fit never makes a wall. Upstream's samples under the footprint alone let a cabin stand on a mountain ledge; the first classic RWG run found a radio tower at Y 145 whose fit left 215 steps of 3 blocks or more |
| Every footprint chunk's building and every floor's part resolve at plan time | A structure is whole or absent, never half placed |

### 2. The ground fit

Each structure that stands on the ground (not an `ocean` one) gets a **ground fit** ([`GroundFit`](../../src/main/java/com/tuxprogrammer/lostcities/worldgen/plan/GroundFit.java)):

- **Pads**: per footprint chunk, the smallest rectangle holding every column a floor's part, overlay part or (for `repeatslice`) support part fills, turned with the plan; for `clear`, which empties its whole chunk, the whole chunk.
- **Ground Y**: the top block of the height the structure was placed at, `lowestLevel − heightoffset − 1`. A cabin's three foundation slices end level with it; a gas station's pavement stands one block above it.
- **Per column**: with `d` the distance from the column to the nearest pad, a weight `w` is 1 for `d ≤ 1` (the pad and a walkway around it) and falls with a smooth step to 0 over a fade of `R` blocks beyond the walkway: `R` is 12, or 1.5 blocks per block the column's surface lies off the ground Y, at most 28. The column's surface `s` moves to `round(s + (groundY − s) × w)`, lowered with `moveDown` (surface layers kept, water joined) or raised with `moveUp`. Plants, logs and leaves on the pad and walkway are cleared.
- **Modes**: a structure on supports (`repeatslice`: the gas station and restaurant beside an elevated highway) is only cut down (`CUT`); everything else is cut and filled (`LEVEL`).

The target depends only on the column's own surface and its distance to the pads, so every chunk the fade reaches (the footprint and two chunks around it) applies its share alone, and neighbors meet: where the weight reaches 0 the ground is RWG's untouched. The fade steepens the land by the difference times the smooth step's slope, at most 1.5 × the difference over `R + 1` blocks, so a fade as long as 1.5 blocks per block of difference adds about a block a column at most: a ravine's rim or a river bank beside a structure, which the oracle does not see, is met by a longer slope, not a wall. The first classic RWG run, with a fixed 12-block fade, raised a ravine's rim 17 blocks below a radio tower in 3-block steps. The terrain phase applies the fits after the blend and before the chunk's content.

### 3. Writing, guarding and defending

- The footprint chunks are written in the terrain phase from the plan's shares ([`ScatteredWriter`](../../src/main/java/com/tuxprogrammer/lostcities/worldgen/city/ScatteredWriter.java)); chests, spawners and tile entities are population jobs from a dry run of the same writer, with floors counted from the plan's lowest level (P07-6).
- The write guard covers the pads from two blocks below the lower of the lowest level and the lowest ground sample, up to Y 255. The walkway and the fade stay outside it, so RWG's trees grow around a cabin, not in it.
- Lakes, lava lakes, dungeons and NHCore's oil are denied where a population window touches a footprint chunk (P07-4, D31). `ChunkPlan.isScattered()` and `ScatteredPlanner.inFootprint` are what M08's Roguelike steering mixin reads (P07-5).
- Explosions start only in city chunks by default, and footprints keep two chunks (32 blocks) from cities, beyond the default largest blast (radius 24), so a default blast never reaches one; with `explosionsInCitiesOnly` off or larger blasts, scattered structures take damage as upstream's did.

### 4. Stuff

Upstream's stuff columns (chains and cobwebs hanging in buildings) are a city chunk's last terrain-phase step (`ChunkPlan.finish()`), after ruins and rubble and before explosion damage, as upstream orders them, with the fixes of the new spec ([07-scattered § Stuff](../new-spec/07-scattered/index.md#stuff)).

## Consequences

- The layout version becomes 3 (`ProfileSnapshot.LAYOUT_VERSION`): a world pinned before M07 logs that its new chunks may not match.
- Fewer structures stand near cities than upstream's: none within 2 chunks of one. The `scattered.real` scenario prints the rates and why selected structures were not placed.
- M06 reads `ScatteredPlanner.opensRailing` for the railing openings of upstream's `getHighwayRailingOpenings`, and its highways feed the near-highway placement through `InfrastructureView`; until M06's highways exist no gas station or restaurant is placed.
- The plan caches (512 areas twice, 256 ground fits) count in budget B5's estimate.

## Alternatives considered

| Alternative | Why not |
|---|---|
| Upstream's behavior: write over the terrain as it is | Cabins with dirt inside or floating foundations, full-chunk parts ending in walls: the cliffs the alpha review ruled out |
| Treat footprint chunks as fixed corners of the city blend | The blend's window lets the neighbors end 3 or 4 blocks below the footprint's edge (a step at the chunk border) and reshapes the land two chunks out with up to 20 blocks of slack per chunk: far more than a cabin needs |
| Ramp only inside the footprint chunk | A full-chunk part leaves no room, and a cabin one column from its chunk's edge would meet its neighbor with a step |
| Keep the footprint and its fade inside the area by narrowing the position draw | Changes upstream's draws and positions for every area |
| Let structures stand near cities and fit them to the blended ground | The terrain stats would describe ground that no longer exists, and the fade would have to climb whatever the blend left, up to 20 blocks a chunk |
