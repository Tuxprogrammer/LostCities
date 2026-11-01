# Configuration preview

The live pictures on the right of the configuration screen: a city map over a fixed fake landscape (Cities page), a transport map (Transport page), and a side view of a city with optional explosion damage (Buildings and Damage pages). The map runs the real layout code against `NullDimensionInfo`, a stand-in dimension with no world.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `gui/NullDimensionInfo` (fake `IDimensionInfo`: fixed biome map and heights, own street planner, optional intercity highway planner, terrain feature). `GuiLCConfig.renderPreviewMap`, `renderPreviewTransports`, `renderPreviewCity`, `soften`, `refreshPreview`. |
| 2 | **Inputs** | The selected profile; the preview seed (default 3439249320423, or from **Rnd**); the fixed biome map. |
| 3 | **Numbers** | See [Numbers](#numbers). |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | The map is the layout code's own output for the preview seed, so it is deterministic for a given seed and profile. The city side view uses `java.util.Random(seed)`; the damage overlay uses `java.util.Random(333)`. The preview shares the static layout caches with real generation, keyed by `minecraft:overworld` chunk coordinates, and clears them on every change, on **Done** and on **Cancel**. |
| 6 | **Data formats** | None. |
| 7 | **Output** | Pixels only. |
| 8 | **Relationships** | Calls `BuildingInfo.getChunkCharacteristicsGui` and `hasBuildingGui` (`04-city-layout`), `Railway.getRailChunkType`, `Highway.getXHighwayLevel`/`getZHighwayLevel` (`06`), `HierarchicalStreetPlanner` and `StreetPlannerSettings` (`04`), `IntercityHighwayPlanner`, `HighwayPlannerSettings`, `ApproximateCityPotential`, `HighwayCityLevelSource`, `HighwayPlanningService` (`06`), `LostCityTerrainFeature` and `ChunkHeightmap` (`03`), `WorldStyle`/`WorldStyleRE` (`05`). |
| 9 | **1.20 dependencies** | `IDimensionInfo` shaped around `WorldGenLevel`, `Holder<Biome>`, `ResourceKey<Level>`; 1.18+ biome keys (`STONY_PEAKS`, `JAGGED_PEAKS`); `LegacyRandomSource`; `GuiGraphics.fill` with ARGB colors. |
| 10 | **Bugs** | `UI-04`, `UI-06`, `UI-08` |

## Numbers

### Fake landscape

A 62 × 58 chunk map (`PREVIEW_WIDTH` × `PREVIEW_HEIGHT`). Outside it every chunk is `p`. Rows run north to south, columns west to east, one character per chunk. In words: a desert patch in the north-west corner (rows 0–4), a river running from the top center south-east into an ocean that fills the east half from row 5 down to about row 25, a beach strip along the ocean's west shore, a mountain range with jagged peaks inside it in the west-center (rows 18–32, columns 5–25), and plains everywhere else, including the whole south (rows 33–57).

| Char | Meaning | Biome | Heightmap height (Y) | Map color |
|---|---|---|---|---|
| `p` | plains | `minecraft:plains` | 65 | `0x005500` |
| `-` | ocean | `minecraft:ocean` | 60 | `0x000066` |
| `=` | river | `minecraft:river` | 65 | `0x000066` |
| `#` | mountain edge | `minecraft:stony_peaks` | 95 | `0x447744` |
| `+` | mountain | `minecraft:jagged_peaks` | 125 | `0x335533` |
| `*` | beach | `minecraft:beach` | 65 | `0xCCCC55` |
| `d` | desert | `minecraft:desert` | 65 | `0xCCCC55` |
| other | | plains | 65 | `0x005500` |

The exact 62 × 58 grid is `NullDimensionInfo.biomeMap` at the pin.

### Map drawing

| Item | Value |
|---|---|
| Origin | (W − 190, 32) |
| Cell | 3 × 3 px per chunk; preview spans 186 × 174 px |
| Biome fill | opaque (`0xFF000000` + color) |
| City overlay | 2 × 2 px at the cell's top-left: `0x995555` for a city chunk, `0xFFFFFF` when it has a building (opaque) |
| Soft mode (Transport page base) | each RGB channel divided by 3 (integer) |
| Transport overlay | 3 × 3 px, translucent: rail `0x99992222`, highway `0x99FFFFFF`, both `0x99777777` |

### City side view (Buildings and Damage pages)

| Item | Value |
|---|---|
| Box | left `W − 157`, top 50, width 150 |
| Sky | Y 50 to 170, `0xFF0099BB` |
| Ground | Y 170 to 200, `0xFF996633` |
| Ground line (`base`) | Y 170 |
| Columns | 14 (index 0 to 13; column 0 is never drawn), 10 px apart, 9 px wide |
| Floor or cellar block | 3 px tall, 4 px pitch; floors `0xFFFFFFFF` upward from the ground line, cellars `0xFF333333` downward |
| City radius used | 190 (blocks) |
| Column distance | `|x × 16 − 112|` blocks from the center column 7 |
| Damage scale | horizontal `10 / 16` px per block, vertical `4 / 6` px per block |
| Damage pixel | 1 × 1 px, `0x66FF0000` |
| Explosion centers | x `left + 75` (normal) and `left + 35` (mini); y `170 − (minHeight − 65) × 4/6` |
| Damage random | `java.util.Random(333)`, shared by both explosions in that order |

## Behavior detail

1. (client) **Preview dimension.** For each map drawn, build a `NullDimensionInfo(profile, seed)`:
   - a world style named `standard` with every optional field empty;
   - a hierarchical street planner seeded with the seed and `minecraft:overworld`, configured from the profile;
   - if the profile's highway generation mode is `INTERCITY_NETWORK_V1`: an intercity highway planner (seed, `minecraft:overworld`, settings from the profile, approximate city potential, city levels from this dimension, no hub persistence) and a planning service around it; otherwise none, and asking for them throws `The inter-city highway planner is unavailable in LEGACY mode` or `… planner service is unavailable in LEGACY mode`;
   - `java.util.Random(seed)` and a terrain feature built with `LegacyRandomSource(seed)`.
   The dimension is always `minecraft:overworld`, has no world (`getWorld` is `null`), uses the profile as its own outside profile, and returns heightmaps from the table above. Its biome lookup reads a registry field that is always `null` (`UI-06`). After drawing, `shutdown` closes the planning service. A new dimension is built every frame, twice on the Transport page (`UI-04`).
2. (client) **Cities page map:** for each chunk (z outer, x inner) fill the biome cell; compute the GUI variant of the chunk characteristics; for city chunks, draw the city or building dot.
3. (client) **Transport page:** draw the map in soft colors, then for each chunk take the rail chunk type and the X and Z highway levels; draw the overlay for rails (type not `NONE`), highways (either level 0 or above), or both.
4. (client) **Buildings page side view:** draw sky and ground. With `Random(seed)`, for columns 1 to 13 within the radius: `factor = (190 − distance) / 190`; `spread = int(minFloorsChance + (factor + 0.1) × (maxFloorsChance − minFloorsChance))`, at least 1; floors `= minFloors + nextInt(spread) + 1`, clamped to `[minFloors + 1, maxFloors + 1]`; draw that many floor blocks; cellars `= minCellars + (maxCellars ≤ 0 ? 0 : nextInt(maxCellars + 1))`; draw that many cellar blocks.
5. (client) **Damage page:** the same side view, then two damage blots. For each (normal explosion first, then mini), over the rectangle within the scaled radius of the center, convert the pixel offset back to blocks, and where the distance is below `maxRadius − 3`, draw a red pixel with probability `3 × (maxRadius − distance) / maxRadius`. Heights are measured from Y 65 regardless of the profile, the maximum height is ignored, and blots can fall outside the box (`UI-08`).
6. (client) **Various and All pages:** no preview.
7. (client) **`refreshPreview`:** clear the caches of `BuildingInfo`, `MultiChunk`, `Highway`, `Railway`, `City`, `CitySphere` and `Scattered`. Runs on every value change, profile or style change, **Rnd**, **Done** and **Cancel**.

## Deviations in the new mod

Not decided here; see `docs/new-spec/` (area 09-client-ui).
