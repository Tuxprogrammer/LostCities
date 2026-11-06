# 0011 · City edges and character

- Status: Accepted (M12, decision log D218 to D224)
- Gate: M12 exit
- Amends: [building-info.md](../original-spec/04-city-layout/building-info.md) step 12.7 (floor counts); [0006](0006-build-around-other-worldgen.md) §3 (detours); D152 (lane steps)
- Date: 2026-11-02

## Context

The alpha review ([reviews/2026-11-02-alpha.md](../reviews/2026-11-02-alpha.md)) asked for a city that fades into RWG terrain instead of ending in a wall of buildings (AR-5), for edges that show growth that never happened (AR-6), and for damage that looks partly repaired (AR-9). The owner: "the buildings need to taper in height from the city center in an organic way… leaving a few 'Empty' plots or Construction plots near the city edges… set them to a brown or red clay like block… dig some of them down one or two or maybe 4 blocks… a variant with concrete pillars showing the early stages of highrise building", and "damaged parts partially filled back in… with a similar but not identical material".

Upstream already scales the floor-count bound with the city factor (step 12.7: `bound = minChance + (cityFactor + 0.1) × (maxChance − minChance)`), but with the default profile the bound only moves from 4 to 6, and the factor knows nothing of where a city actually stops: a margin around a village (D149), the rise cap at a mountain (D148), or another city's rim. The alpha's edge shots show 4 to 6-floor buildings against both.

## Decision

### 1. Urban intensity

`layout.Urbanity` gives every city chunk an intensity `I` from 0 (the very edge) to 1 (the dense core), a pure function of the seed and the layout, cached per chunk:

- **Edge distance** `e`: the Euclidean distance, in chunks, to the nearest chunk within 3 that is not city (any reason: RWG land, a margin, the rise cap, the sea), divided by 4 and capped at 1. 
- **Core** `c`: the square root of the city factor above the threshold, `sqrt((f − t) / (1 − t))`.
- **Noise** `n`: two octaves of simplex noise over chunk coordinates (wavelengths of about 7 and 3 chunks), from −1 to 1, seeded from the world seed.
- `I = clamp(e^0.8 × (0.25 + 0.75 c) + 0.18 n · cityTaper, 0, 1)`, and with `cityTaper` 0 the old factor is used unchanged.

The noise makes districts that rise or fall against the radial trend, so equal distances from the center give different heights; the edge term makes every kind of edge low.

### 2. Tapered heights

In step 12.7 the intensity replaces the city factor in upstream's bound, so the draw is upstream's. Then, from a separate taper random (no change to the building stream):

- **High-rise core:** where `I > 0.65`, with chance `0.7 × (I − 0.65) / 0.35`, the building gains `1 + ⌊u × (maxFloors − f)⌋` floors, up to `buildingMaxFloors`.
- **Edge cap:** floors are at most `max(minFloors + 1, ⌈maxFloors × (0.18 + 0.82 I)⌉)`: 2 at the very edge, about 4 at `I` 0.3, all of them in the core (default profile, `buildingMaxFloors` 8).
- The building's own floor limits and the zone caps apply after, as before. Each step is scaled by `cityTaper` (default 1, range 0 to 1).

### 3. Edge lots

After the plan's draws, a separate lot random turns some low-intensity chunks into lots:

- A single building chunk with `I < 0.4` becomes a lot with chance `edgeLotChance × (1 − I / 0.4)` (default 0.45), and an open lot without a park part with half that chance; never with a highway or rail, on a lane, in a multibuilding, on predefined content or in a village's transition ring. A lot holds no park, fountain, front or stair.
- **Kinds**, by weight: bare 0.35, dug 0.35 (depth 1, 2 or 4 by 0.3, 0.35, 0.35), foundation 0.3 (depth 1, 2 or 4 likewise). A chunk with a rail corridor or rail dungeon under it is only ever bare.
- **Ground:** the lot's surface at city ground is a deterministic mix, per column, of coarse dirt (`minecraft:dirt:1`, which never grows grass), terracotta (`minecraft:hardened_clay`), brown and red terracotta, and a little gravel: the brown and red clay ground the owner asked for. Nothing on it grows: no park, greenery, vegetation or leaf piles, and the chunk is in the footprint guard, so RWG's trees and grass are dropped.
- **Pits:** a dug lot keeps a 3-block rim at city ground on every side (door approaches stay whole) and digs the 10 × 10 inside down by its depth, with the same clay mix on the floor and a ramp of coarse dirt from one side, the way machinery would get in. 
- **Foundations:** a pit whose floor is a slab of light gray concrete (`minecraft:light_gray_concrete`, mapped to Et Futurum's concrete and vanilla's light gray terracotta), with a grid of 2 × 2 concrete columns (4 columns in a shallow pit, 9 in a 4-deep one) rising 1 to 3 floors above city ground, each topped with iron bars as rebar, and at two floors or more a concrete deck over one bay at the first floor.
- **Fence:** dug lots are fenced in oak fence at the pit's edge, with a 4-block gap in the middle of each side, where door approaches and the ramp meet the rim.
- The lot clears everything above city ground, its street base is filled before it digs, and it gets no rubble or leaf piles.
- Lots are labeled `empty lot`, `excavation (depth n)` and `foundation (depth n)`, and `/lostcities find lot` finds the nearest.

**Addition (Phase 2e, D297 to D304): pack houses.** An empty (bare) lot is picked at `packHouseChance` (default 0.25) by a hash of the world seed and chunk, so the choice never depends on generation order. Its population phase calls a pack mod's own village-piece generator the way a village does (`VillagerRegistry.getVillageComponent`, vanilla's factories for vanilla pieces) on a random seeded from the world seed and chunk, then `addComponentParts` with the chunk's box. Nothing is copied. The house is centered on the lot at the lot's ground, its door toward a neighboring city chunk, and pieces over 12 blocks wide are skipped, so no write comes within 2 blocks of the chunk edge and no neighbor chunk is loaded. Houses come from vanilla, Railcraft, Tinkers' Construct, Forestry and Witchery, each in a `compat` package and skipped when the mod is absent or its config disables the house. The villager counters are raised so no villagers spawn.

**Addition (city-realism phase 3, D325 to D328): construction lots at real sizes.** With `lotFootprints` (default true; worlds pinned before it read false and keep the lots above), a ring construction lot (D308) stands in for a building of its ring's class instead of being one chunk:

- **Cells.** Lots are drawn per aligned 2 x 2 chunk cell, like multibuildings per multichunk, so footprints never overlap and every chunk reaches the same draw from the cell alone, in any planning order. The cell's chance is the ring's lot chance x 4 / the ring's mean footprint area, so lots cover the same share of a ring as before. The ring distance is read at the cell's top-left chunk.
- **Class and size.** A class is drawn by the ring's acceptance weights (D307), then a footprint by the class's size weights (1x1 / 2x1 / 2x2): tower 0.4 / 0.2 / 0.4, high-rise 0.1 / 0.3 / 0.6, mid-rise 0.7 / 0.2 / 0.1, low-rise 0.85 / 0.15 / 0, house 1 / 0 / 0, from the 2b audit (every high-rise is a 2 x 2 multibuilding). A 2 x 1 lies either way round in either half of the cell; a 1 x 1 in any quarter.
- **Fit.** The footprint stands only if every chunk could hold an edge lot (a single building chunk or open lot, no highway, rail, lane, predefined content or village ring) at one city level; otherwise it shrinks to its top-left chunk, otherwise the cell has none. The planner asks the other chunks for their plan before lots, never for their full plan, so no plan waits on itself.
- **Depth.** The class's cellars (tower 2 to 3, high-rise 1 to 3, mid-rise 1 to 2, low-rise 0 to 1, house 0), limited by the profile's cellar maximum and the world's floor, x `floorPitch`. A pit with no cellars is a bare lot; a foundation with none is a slab at grade. A dug lot stands for a building with cellars and follows upstream's rule for one (D329): no rail corridor runs through its chunks, and a rail dungeon stays only below its cellars. (Unlike a single-chunk edge lot, which stays bare over a corridor; corridor candidates are drawn for most chunks without cellars, so that rule left almost every multi-chunk lot bare.)
- **Frame.** A foundation's frame rises 1 to the class's floors (house 1 to 2, low-rise 2 to 3, mid-rise 3 to 6, high-rise 5 and tower 7 to `buildingMaxFloors`) at the pinned pitch, under the roof cap. Per footprint it is a concrete skeleton with rebar or a wooden scaffold (oak fence posts, plank decks); at each floor below the top a deck covers one bay of the chunk's column grid.
- **Writing.** Each chunk writes only its own columns, in footprint coordinates: rim, walls and fence follow the footprint's outline with a gap in the middle of each chunk's outer side, the pit runs across inner chunk borders, and one ramp climbs from one side, steeper where the pit is deeper than the ramp is long. Nothing is written outside the chunk being generated.
- Edge lots keep their own per-chunk roll; a multi-chunk bare lot gets no pack house. Lots of more than one chunk are labeled with their size, such as `excavation (depth 16, 2x2)`.

### 4. Partial repairs

- Each explosion is repaired with chance `damageRepairChance` (default 0.5), decided from the world seed and the explosion's center, so every chunk it reaches agrees.
- A repaired explosion fills its damage back in up to a repair line: from the sphere's bottom up to `bottom + (0.45 + 0.4 u) × diameter` (u from the explosion), varied by ±1 per column. Above the line the damage stays.
- Only in city chunks: inside buildings (at or above their lowest cellar) and, in streets and parks, up to city ground. Nature outside the city is not repaired.
- **Patch material:** a block that would break becomes its patch. Glass and panes become planks (boarded windows; oak, spruce or birch per explosion). An opaque block's patch is the block from a list of plain repair materials (cobblestone, stone bricks, bricks, plain and stained terracotta, sandstone, smooth stone, quartz and the Et Futurum concretes; never vanilla stone, a GT ore host) whose measured color is nearest a CIE76 difference of 10 from the original's (hue included, unlike 0012's `J`, so a red wall is not patched in cyan), with at least 6 and at most 22, one of the three nearest by a hash of the explosion and the block. The same block in one repair always gets the same patch: a visible, consistent patch, not noise.
- Blocks whose damaged form would have been chosen (cracked bricks, iron bars) keep that roll; repairs replace only what would have become air or liquid.

### 5. The M05R leftovers

- **Terraces.** `TerrainBlend.reshape` truncated a straight interpolation, so a margin's bank stepped down in straight 1-block terraces along the chunk axis. Outside city corners the cut now adds a per-column dither and a weak transition-noise wobble, weighted 0 at city corners, so the street edge keeps its height and the bank's steps follow ragged contours. The soft-edge strip's ramp is dithered the same way.
- **Lane rises.** Before a lane is laid, its columns are relaxed so that no two lane neighbors differ by more than one block: each column moves to the floor of the mean of the lowest and highest heights its neighbors allow, columns on the chunk's border keep their height, except against a neighboring lane chunk, where they are pinned into a one-block window both chunks derive from the plan (D283), so the lane is continuous across the chunk border. A non-city chunk under an AE2 crater's cap keeps its blend corners at or under the cap (D284). The gate ramp is clamped to one block per column from the city side. Every remaining one-block rise gets its stair or slab (D152).
- **Wide roads at a village.** A detour is a tertiary road, not a primary one, and a primary road chunk touching a village's margin (its 3 × 3 window) becomes tertiary: the primary narrows to a small road around the old town and widens again past it (AR-7, AR-10).

## Consequences

- Five profile options, pinned with the world: `cityTaper`, `edgeLotChance`, `damageRepairChance`, `facadeContrast` (0012), and their rows in [config.md](../config.md).
- The layout version rises; a world pinned before keeps generating with the new rules and logs that new chunks may not match (D153).
- New-spec deviations: the floor count, edge lots, repairs, detour width and lane relaxation, recorded in new-spec 03-terrain and 04-city-layout.
- Code-named blocks for the lots and the repair list resolve through the block-name map, so a pack without Et Futurum falls back to vanilla.

## Alternatives considered

- **Rings by distance from the center.** Upstream's factor alone gives concentric bands, the "rings" the owner ruled out, and ignores margins and the rise cap.
- **Lots as parts (JSON assets).** A pit's depth, a foundation's height and the ground mix vary per lot; parts would need many copies and M11 owns the asset catalog. Writing them in code keeps them deterministic and small.
- **Repairing every damaged block.** Then damage would vanish; a partial line keeps the story of the damage.

## Still open

- The intensity's weights and the lot chance are set from the alpha's shots; the beta review may tune them.
