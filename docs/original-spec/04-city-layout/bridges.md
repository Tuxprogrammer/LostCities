# Bridges and bridge supports

Bridges carry a street across non-city chunks (water or lower ground) between two city chunks. Legacy mode chains random per-chunk candidates; hierarchical mode bridges water gaps on primary roads. Source: `BuildingInfo.hasXBridge`/`hasZBridge`/`calculate*Bridge`/`isSuitableForBridge`/`hasBridge` (`worldgen/lost/BuildingInfo.java:1495-1684`), `worldgen/street/HierarchicalBridgePlanner.java`, `worldgen/gen/Bridges.java`, `worldgen/gen/Supports.java`, on `f18a094d`.

## 1. Identity

| Member | Role |
|---|---|
| `BuildingInfo.hasXBridge` / `hasZBridge` | Bridge part for this chunk along X or Z, or `null`; memoized under the dimension lock |
| `BuildingInfo.xBridge`, `zBridge` | Legacy random candidate flags |
| `bridgeType`, `largeBridgeType` | Per-chunk bridge part picks ([building-info.md](building-info.md)) |
| `HierarchicalBridgePlanner.getBridgeInfo` | Hierarchical span finder |
| `PlannedBridgeInfo` | `(id, orientation, minimumEndpoint, maximumEndpoint, gapLength)` |
| `Bridges.generateBridges` | Writes the bridge and its supports |
| `Supports.generatePart` | Repeats a support part downward |

Profile: `BRIDGE_CHANCE`, `PLANNED_PRIMARY_BRIDGE_CHANCE`, `PLANNED_PRIMARY_BRIDGE_MAX_LENGTH`, `BRIDGE_SUPPORTS`, `GENERATE_LIGHTING`, `GROUNDLEVEL`. Assets: city style `bridges`, `largebridges`, `bridgesupport` (character), `bridgesupportpart`; world style `bridgesupport`, `bridgesupportpart`; bridge part metadata `support` (`05-assets`).

## 2. Inputs

`BuildingInfo` of the chunk and of every chunk along the span, raw city flags, the street planner, water biomes, the heightmap, highway levels.

## 3. Numbers

| Item | Default | Range | Unit |
|---|---|---|---|
| `BRIDGE_CHANCE` | 0.7 | 0..1 | per axis per non-city chunk (legacy) |
| `PLANNED_PRIMARY_BRIDGE_CHANCE` | 1.0 | 0..1 | per eligible span |
| `PLANNED_PRIMARY_BRIDGE_MAX_LENGTH` | 12 | 1..64 | gap chunks |
| `BRIDGE_SUPPORTS` | true | | |
| Bridge Y | hierarchical `GROUNDLEVEL`; legacy `GROUNDLEVEL + 1` | | block Y (legacy parts are authored one block higher) |
| Block-pillar support | x 7..8, z 7..8, from `waterLevel - 10` to `groundLevel` | | blocks |
| Side connection strip | x 0 or 15 (z 6..9) for X bridges; z 0 or 15 (x 6..9) for Z bridges, at `GROUNDLEVEL` | | blocks |
| `BRIDGE_SPAN_SALT` | `0x31d6a7f04c82be59` | | |
| `BRIDGE_INTERSECTION_SALT` | `0x67a14ce3b9052df8` | | |
| Orientation XOR | X `0x243f6a8885a308d3`, Z `0x13198a2e03707344` | | |
| Endpoint offsets | `0x9e3779b97f4a7c15`, `0xc2b2ae3d27d4eb4f`, `0x165667b19e3779f9`, `0x85ebca77c2b2ae63` | | |

## 4. Behavior

All (server).

### Suitability (`isSuitableForBridge(i)`, legacy)

False in a `space` world when this chunk has a monorail. Otherwise true when chunk `i` is lower than the asking chunk (`i.cityLevel < cityLevel`) or `i`'s main biome is a water biome (`#is_ocean`, `#is_deep_ocean`, `#is_beach`, `#is_river`).

### Legacy X bridge (`calculateXBridge`)

1. Not a candidate (`!xBridge`) or not suitable: `null`.
2. Odd chunk Z (`chunkZ % 2 != 0`, negative odd included): if the chunk north or south already has an X bridge, `null` (even rows win).
3. Walk west from the west neighbor while the chunk is not city, is an X candidate and is suitable; at each step repeat the odd-row check against that chunk's north and south neighbors, and remember that chunk's `bridgeType`. The walk ends at chunk `min`.
4. `min` must be city, without a building, at city level 0; else `null` (bridges only at level 0).
5. Walk east the same way to chunk `max`, with the same odd-row check; `max` must be city, without a building, level 0.
6. The bridge part is the `bridgeType` of the westmost gap chunk (next to `min`), or this chunk's own if it is next to `min`.
7. Mark every gap chunk between `min` and `max` with that X bridge and with "no Z bridge", so they skip their own walks.

### Legacy Z bridge (`calculateZBridge`)

The same along Z, with the parity rule on chunk X, plus: a chunk that has an X bridge never has a Z bridge, and the walk aborts if any gap chunk has an X bridge. The level-0, city and no-building tests apply to both ends.

### Hierarchical bridge (`HierarchicalBridgePlanner.getBridgeInfo`)

For orientation X the road is a horizontal primary (constant Z); for Z a vertical primary.
1. `null` unless hierarchical mode, the source chunk is not city, it lies on a primary of that orientation, and it is a **gap chunk**: not raw city, on that primary, at a primary intersection only if the orientation equals the dimension's preferred intersection orientation, and either a water biome or a heightmap height below the chunk's `waterLevel` (catches inland lakes).
2. Search toward lower coordinates, up to `maxLength + 1` steps: the first chunk that is raw city and has a raw primary road is the minimum endpoint; a non-gap chunk first, or running out of steps, gives `null`. Same toward higher coordinates.
3. `gapLength = max - min - 1` must be within `1..maxLength`.
4. Both endpoints' `BuildingInfo` must be primary roads (effective) at city level 0 ("higher levels need ramp assets").
5. `id = spanHash(seed, dimension, orientation, min, max)`; the span exists when `unitDouble(id) < PLANNED_PRIMARY_BRIDGE_CHANCE`.
6. `preferredIntersectionOrientation` = X when bit 0 of `mix64(seed ^ dimHash ^ INTERSECTION_SALT)` is 0, else Z; one value per world and dimension.

In hierarchical mode `hasXBridge`/`hasZBridge` return the minimum endpoint's `largeBridgeType` if set, else its `bridgeType`. The legacy candidate flags are ignored. At a primary intersection only the preferred orientation can be a gap, so a chunk never has both; the generator checks X first anyway.

### Generation (`Bridges.generateBridges`)

Runs for non-city chunks (`doNormalChunk`, `03-terrain`), before highways and scattered structures.
1. If either highway level is 0, generate nothing. Any other level allows a bridge, although a level-1 highway overlaps the top of a 7-high bridge part (CITY-38).
2. Use the X bridge part if any, else the Z part.
3. Compile the part's palette. For every column `(x, z)`, write the part's slices upward from the bridge Y. X bridges read the part at `(x, z)`; Z bridges read it transposed at `(z, x)` with no block-state rotation (CITY-30). Torch entries become torch to-dos when `GENERATE_LIGHTING`, else air.
4. Supports, when `BRIDGE_SUPPORTS` and a support character or part exists:
   - support source: the part's `support` metadata character (part palette); otherwise the city style's `bridgesupport` character, else the world style's; and the city style's `bridgesupportpart`, else the world style's part only when the city style set neither (chunk palette);
   - a support character that is missing from the palette throws;
   - interior chunks (both neighbors along the orientation also have a bridge in that orientation): a support part, repeated downward from the bridge Y - 1 (rotated with `MIRROR_90_X` for Z bridges), or a 2 × 2 pillar of the support block at x/z 7..8 from `waterLevel - 10` to `groundLevel`;
   - end chunks (a neighbor along the orientation has no bridge) with a support block: a 4-block strip at `GROUNDLEVEL` on that edge (z 6..9 for X, x 6..9 for Z).

### Support parts (`Supports.generatePart`)

1. A part with no slices throws.
2. For every column of the part with a vertical slice: transform the column with `rotateX/rotateZ`; start at the top Y and move down while the world block is empty (air, water, lava; `03-terrain`) and above the minimum build height.
3. At each step write the part's slice character, cycling from the top slice down and wrapping, skipping air and structure-void entries, with the block state transformed. Each column stops independently.

## 5. Determinism

- Legacy: per-chunk candidates come from the building random (seeded); walks are deterministic but write results into other chunks' cached `BuildingInfo`s. A recomputed neighbor reaches the same answer, so the writes are a cache, not a decision. Recursion across rows is unbounded in cost (CITY-23).
- Hierarchical: pure hashing of seed, dimension id and endpoint coordinates.

## 6. Data formats

City and world style bridge fields, part `support` metadata (`05-assets`). No NBT.

## 7. Output

Bridge blocks and supports in non-city chunks; `hasBridge` also blocks scattered structures (`07-scattered`).

## 8. Relationships

- Called by: `doNormalChunk` (`03-terrain`), `Scattered.avoidScattered` (`07`), support-neighbor checks.
- Calls: `LostCityTerrainFeature.isWaterBiome`, `computePalette`, `transformBlockState`, `isEmpty` (`03`), [street-planner.md](street-planner.md), [transforms.md](transforms.md).

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Biome tags `#is_ocean`, `#is_deep_ocean`, `#is_beach`, `#is_river` | water-biome test | `BiomeDictionary` types (`OCEAN`, `BEACH`, `RIVER`, `WATER`) in 1.7.10 |
| Minimum build height | `Supports` lower bound | 0 in 1.7.10 |
| `Blocks.STRUCTURE_VOID` | `Supports` skip | No structure void in 1.7.10 |
| Block-state `rotate` | support transform | Metadata rotation in 1.7.10 |
| Dimension id string in the span hash | `spanHash` | Integer dimension IDs in 1.7.10 |

## 10. Bugs

CITY-23, CITY-28, CITY-30, CITY-38.

## Deviations in the new mod

See [new-spec 04-city-layout § Streets, parks, stairs, bridges](../../new-spec/04-city-layout/index.md#streets-parks-stairs-bridges-street-plannermd-streets-parks-stairsmd-bridgesmd).
