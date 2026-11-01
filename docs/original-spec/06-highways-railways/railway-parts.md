# Railway part placement, stations and rail dungeons

What gets written into a chunk with railway: the part per rail type, water variants, the surface clearing above surface stations, the reduction from three tracks to one or two, staircases up from underground stations, and rail dungeons with their connecting tunnels.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `worldgen/gen/Railways` (`generateRailways`, `generateRailwayDungeons`), asset data `worldgen/lost/regassets/data/RailwayParts` (world style `parts.railways`) and `RailSettings` (city style `railblocks.railmain`), `api/RailChunkType`. `BuildingInfo.railDungeon` (chosen in `04-city-layout`). Profile option `railwayDungeonChance`. |
| 2 | **Inputs** | The chunk's `BuildingInfo` (ground level, city level, city style, compiled palette), its `RailChunkInfo` ([railway-layout.md](railway-layout.md)), its neighbors' rail infos and rail dungeons, the chunk heightmap, blocks already in the chunk buffer, the chunk-generation random. |
| 3 | **Numbers** | See [Numbers](#numbers). |
| 4 | **Behavior** | See [Behavior](#behavior). |
| 5 | **Determinism** | Part picks from the chunk-generation random; the rail dungeon from the building random. See [Determinism](#determinism). |
| 6 | **Data formats** | `RailwayParts` JSON, `RailSettings` JSON. See [Data formats](#data-formats). |
| 7 | **Output** | Railway part blocks, cleared space above surface stations, track rows filled in, staircase parts, rail dungeons and their tunnels. |
| 8 | **Relationships** | Called from `LostCityTerrainFeature.generate` after the city or normal chunk and before damage; reads [railway-layout.md](railway-layout.md); overwrites highways where they overlap. |
| 9 | **1.20 dependencies** | `BlockState` identity for water detection and rail states; `Blocks.RAIL`, `Blocks.POWERED_RAIL`; codecs. |
| 10 | **Bugs** | `HWY-12`, `HWY-13`. |

## Numbers

| Item | Value | Notes |
|---|---|---|
| Part Y | `groundLevel + railLevel × 6` | Same for dungeons (using the adjacent rail's level) and stations |
| Water test points (part-local x, y above part Y, z) | (3, +2, 3), (12, +2, 3), (3, +2, 12), (12, +2, 12), (3, +4, 7), (12, +4, 8) | All six must equal the profile's liquid block |
| Surface-station clearing | from the part's top to heightmap height + 4 | Only if the part top is lower |
| Track rows | z = 5, 7, 9 (part-local) | One track: rows 5 and 9 are filled with the rail main block, row 7 stays. Two tracks: row 7 is filled |
| Track-fill Y | part Y + 1 for station and horizontal types; part Y + 1 to part Y + height − 1 for descending types (only where a rail block is) | |
| Dungeon tunnel, north side | 4 deep (z 0..3) floor and air, 3 deep (z 0..2) walls and ceiling | |
| Dungeon tunnel, south side | 5 deep (z 15..11) floor and air, 4 deep (z 15..12) walls and ceiling | Asymmetric, `HWY-13` |
| Dungeon tunnel cross-section | floor at x 6..7 (Y + 1), air at Y + 2..3, walls x 5 and x 8 (Y + 2..4), ceiling x 6..7 (Y + 4) | All "rail" blocks are the rail main block |
| `railwayDungeonChance` | 0.01 per chunk | 0..1 |

Bundled railway parts (all 16 × 16; `05-assets` owns the catalog), with heights in blocks: `station_underground` 6, `station_open` 2, `station_openroof` 7, `station_underground_stairs` 6, `station_staircase` 6, `station_staircase_surface` 5, `rails_horizontal` 6, `rails_horizontal_end` 6, `rails_horizontal_water` 6, `rails_vertical` 6, `rails_vertical_water` 6, `rails_3split` 6, `rails_bend` 6, `rails_flat` 4, `rails_down1` 12, `rails_down2` 18, `rail_dungeon1` 6, `rail_dungeon2` 6. The bundled city styles set `railmain` to `y`; the world style sets `railwayavoidance` `ignore` and `railpartheight6` 1.

## Behavior

### When it runs (server)

1. (server) After the city or normal chunk (buildings, streets, highways) and the city-sphere center part; before torches, explosions and debris. `generateRailways` runs only when the chunk's rail type is not `NONE`; `generateRailwayDungeons` runs for every chunk.

### Part per type (server)

| Type | Part list | Mirror | Notes |
|---|---|---|---|
| `STATION_SURFACE`, `STATION_EXTENSION_SURFACE` | if the rail level is below the chunk's city level: `stationunderground`; else the list stored in the rail info (open or open roof), or `stationopen` if none | none | Then clear above (step 3) |
| `STATION_UNDERGROUND` | `stationundergroundstairs` | none | Then staircase (step 6) |
| `STATION_EXTENSION_UNDERGROUND` | `stationunderground` | none | |
| `RAILS_END_HERE` | `railshorizontalend` | mirrored in X when `EAST` | |
| `HORIZONTAL` | `railshorizontal`, or `railshorizontalwater` when neither X neighbor is a station and all six water points are liquid | none | Dungeon tunnels (step 5) |
| `VERTICAL` | `railsvertical`, or `railsverticalwater` on six liquid points | X when `EAST` | Not rotated; the part itself runs north-south |
| `THREE_SPLIT` | `rails3split` | X when `EAST` | |
| `GOING_DOWN_TWO_FROM_SURFACE`, `GOING_DOWN_FURTHER` | `railsdown2` | X when `EAST` | |
| `GOING_DOWN_ONE_FROM_SURFACE` | `railsdown1` | X when `EAST` | |
| `DOUBLE_BEND` | `railsbend` | X when `EAST` | |
| any other | `railsflat` | none | *Unreachable*: every type has a case |

1. (server) Part Y = `groundLevel + railLevel × 6`. Pick from the list (one entry → that entry; else the chunk-generation random) and generate it with hard-air mode `AIR`.
2. (server) The water test reads the chunk buffer, so it sees terrain and anything placed earlier in this chunk.
3. (server) Surface stations and surface extensions: if the part's top is below the chunk heightmap height + 4, clear every column from the part top up to that height (air, no water).
4. (server) Look up the city style's rail main block character in the compiled palette. If missing, throw `RuntimeException` (`HWY-12`).
5. (server) `HORIZONTAL` only: if the north neighbor has a rail dungeon, carve the north tunnel; if the south neighbor has one, carve the south tunnel (dimensions in Numbers).
6. (server) Track reduction, when the rail info has fewer than 3 tracks:
   - station types and `HORIZONTAL`: one track → fill rows z 5 and 9 at part Y + 1 across all 16 x with the rail main block; two tracks → fill row z 7;
   - descending types: the same rows, but over the part's height and only where the block is a vanilla rail or powered rail (any state);
   - `THREE_SPLIT`, `VERTICAL`, `DOUBLE_BEND`, `RAILS_END_HERE`: nothing.
7. (server) `STATION_UNDERGROUND` staircase: for each level from rail level + 1 up to city level − 1, generate a `stationstaircase` part at that level's Y; then a `stationstaircasesurface` part at the city level's Y. All with mode `AIR`, unrotated.

### Rail dungeons (server)

1. (server) In `BuildingInfo` (`04-city-layout`), each chunk draws `nextFloat() < railwayDungeonChance` from its building random; on success, if the chunk has no building or the railway level is below the building's lowest floor (city level − cellars), it picks a part from the city style's `raildungeons` selector with the same random. Every chunk makes the draw, rail or not.
2. (server) During generation, a chunk with a rail dungeon looks at its north neighbor's rail info, then its south neighbor's. If either is `HORIZONTAL`, the dungeon part is generated at that rail's Y, unrotated, mode `AIR`. Otherwise nothing is placed.
3. (server) The neighboring `HORIZONTAL` chunk carves the tunnel toward the dungeon (step 5 above) whenever the dungeon chunk has a dungeon part, whether or not the dungeon chose that side.

## Determinism

- **Part picks:** `getRandomPart` uses the chunk-generation random (see [highway-parts.md](highway-parts.md)); all bundled lists have one entry, so no draw happens with bundled assets.
- **Rail dungeon:** `BuildingInfo.getBuildingRandom` (`QualityRandom(seed + chunkZ × 341873128712 + chunkX × 132897987541)`), at a fixed point in the building random's sequence (`04-city-layout`).
- **Neighbors:** reads neighbor rail infos and `BuildingInfo` (planning data); never loads or writes neighbor chunks.
- **Water test:** depends on the blocks already in this chunk's buffer, which are deterministic for the chunk.
- **Order:** inherits `HWY-07` from the layout when `block_railway` avoidance is used.

## Data formats

`RailwayParts` (world style `parts.railways`; field format owned by `05-assets`). Each field accepts a part name or a list; the JSON key is written without underscores, and the default part name with them:

| JSON key | Default part | Used for |
|---|---|---|
| `stationunderground` | `station_underground` | Underground extensions; surface stations below city level |
| `stationopen` | `station_open` | Surface station without roof |
| `stationopenroof` | `station_openroof` | Surface station with roof |
| `stationundergroundstairs` | `station_underground_stairs` | Underground station |
| `stationstaircase` | `station_staircase` | Each level between the station and the city |
| `stationstaircasesurface` | `station_staircase_surface` | Top of the staircase at city level |
| `railshorizontal` | `rails_horizontal` | Horizontal |
| `railshorizontalend` | `rails_horizontal_end` | Rails end |
| `railshorizontalwater` | `rails_horizontal_water` | Horizontal under water |
| `railsvertical` | `rails_vertical` | Vertical |
| `railsverticalwater` | `rails_vertical_water` | Vertical under water |
| `rails3split` | `rails_3split` | Three-way split |
| `railsbend` | `rails_bend` | Double bend |
| `railsflat` | `rails_flat` | Fallback, unreachable |
| `railsdown1` | `rails_down1` | Down one level |
| `railsdown2` | `rails_down2` | Down two levels |

`RailSettings` (city style `railblocks`): `railmain`, optional one-character string, the palette character for the rail main block; inherited from the parent city style when absent (`04-city-layout`).

City style `selectors.raildungeons`: list of weighted part selectors (`05-assets`). Profile `railwayDungeonChance`. World style `settings.railwayavoidance` (`ignore` or `block_railway`, required when `settings` is present) and `railpartheight6` (int, default 1): read by `BuildingInfo` and `MultiChunk`.

## Output

Rail parts, cleared air above surface stations, solid rows replacing unused tracks, staircases from underground stations to the street, rail dungeons with their two-wide tunnels.

## Relationships

- Called by `LostCityTerrainFeature.generate` (`03-terrain`).
- Uses `LostCityTerrainFeature.generatePart`, `clearRange`, `getRailStates`, `getRandomPart` (`04-city-layout`, `05-assets`).
- `BuildingInfo.railDungeon` and cellar rules (`04-city-layout`); loot inside dungeon parts (`08-loot-mobs-blocks`).

## 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| `BlockState` identity comparisons with the profile liquid | water test | 1.7.10 compares block and metadata; flowing vs still water have different IDs. |
| All states of `Blocks.RAIL` and `Blocks.POWERED_RAIL` | `getRailStates` for track reduction | 1.7.10 rails are block plus metadata (`Blocks.rail`, `Blocks.golden_rail`). |
| DFU codecs (`Tools.listOrStringList`) | `RailwayParts`, `RailSettings` | Plain JSON parsing in 1.7.10. |
| Y range | part Y down to 5 with offset -8 | See [railway-layout.md](railway-layout.md). |

## Bugs

- `HWY-12`: a rail main block missing from the palette throws `RuntimeException` during generation.
- `HWY-13`: the dungeon tunnel is 4 (walls 3) blocks deep on the north side and 5 (walls 4) on the south side.

## Deviations in the new mod

None recorded yet; the new spec decides.
