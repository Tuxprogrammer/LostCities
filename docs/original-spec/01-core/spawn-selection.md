# Spawn selection and player data

How upstream picks the world spawn of a new world to honor the profile's spawn options (spawn biome, predefined city or sphere, inside or outside a building, a named building or part), how it re-applies that spawn when the first player joins, and the unused per-player capability. The spawn options themselves are profile options owned by `02-profiles-dimensions`; their defaults are repeated here because the algorithm depends on them.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `setup/ForgeEventHandlers` (`onCreateSpawnPosition`, `setSpawnPoint`, `findSafeSpawnPoint`, `findSafeSpawnPointInChunk`, `findSafeSpawnPointAtColumn`, `isValidStandingPosition`, `isOutsideBuilding`, `isForcedBuildingSpawnChunk`, `getSqRadius`, `onPlayerFirstJoin`, `onEntityConstructing`, the `spawnPositions` map). `setup/SpawnBuildingPrefilter` (cheap pre-check for forced building spawns). `playerdata/PlayerProperties`, `PlayerSpawnSet`, `PropertiesDispatcher` (capability `lostcities:spawnset`). Profile options: `spawnBiome`, `spawnCity`, `spawnSphere`, `spawnNotInBuilding`, `forceSpawnInBuilding`, `forceSpawnBuildings`, `forceSpawnParts`, `spawnCheckRadius`, `spawnRadiusIncrease`, `spawnCheckAttempts`, `landscapeType`, `groundLevel`. |
| 2 | **Inputs** | The new world's overworld `ServerLevel` and its dimension info; the profile; the biome registry; predefined cities and spheres (`05-assets`); `BuildingInfo` and chunk characteristics (`04-city-layout`); city spheres (`03-terrain`); the block states of the generated world. |
| 3 | **Numbers** | See [Numbers](#numbers). |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | No random sources. The result is a pure function of seed, profile, assets and the generated terrain, because the search order is fixed (a square spiral from chunk 0,0, then columns in a fixed order, then Y upward). It reads real block states, so it forces those chunks to generate in full, synchronously, during world creation. |
| 6 | **Data formats** | Capability NBT: compound with boolean `spawnSet`. The world spawn is written to the level data (vanilla). No JSON. |
| 7 | **Output** | The world spawn position (angle 0) set in the world settings and the level; the vanilla spawn search (and with it the bonus chest, `CORE-17`) skipped; an entry in `spawnPositions` used to re-apply the spawn and teleport the first player to log in. |
| 8 | **Relationships** | Calls `LostCityWorldGenData.initializeNewWorld` (`03-terrain`/`04-city-layout`: marks the world as new so it uses the hierarchical street and highway planners), `LostCityFeature.getDimensionInfo` (`03`), `BuildingInfo.getBuildingInfo`, `getChunkCharacteristics`, `getProfile`, `isCityRaw`, `getBuildingBottomHeight`, `getFloorAtY` (`04`), `City.isChunkOccupied`, `City.getCityStyle`, `CityStyle.maySelectBuilding` (`04`), `CitySphere.getCitySphere`, `isCitySphereCenter`, `squaredDistance` (`03`), `AssetRegistries.PREDEFINED_CITIES`, `PREDEFINED_SPHERES`, `MULTI_BUILDINGS` (`05`), the `CharacteristicsEvent` listener list ([api.md](api.md)). |
| 9 | **1.20 dependencies** | `LevelEvent.CreateSpawnPosition` (cancelable new-world spawn hook); `PlayerLoggedInEvent`; Forge capabilities attached through `AttachCapabilitiesEvent`; biome registry lookup by `ResourceLocation` and `Holder<Biome>`; `BlockState.isFaceSturdy`; `ServerLevel.setDefaultSpawnPos` with an angle; world height (columns scanned between `groundLevel - 5` and Y 124 regardless of the -64..320 range); landscape types `SPHERES`, `CAVERNSPHERES`. |
| 10 | **Bugs** | `CORE-11`, `CORE-12`, `CORE-13`, `CORE-14`, `CORE-15`, `CORE-16`, `CORE-17`, `CORE-37` |

## Numbers

| Item | Value | Changed by |
|---|---|---|
| `spawnCheckRadius` default | 200 blocks (range 1 to 100000) | profile |
| `spawnRadiusIncrease` default | 100 blocks (range 1 to 100000) | profile |
| `spawnCheckAttempts` default | 20000 chunks (range 1 to 1000000) | profile |
| `spawnBiome`, `spawnCity`, `spawnSphere` defaults | empty | profile |
| `spawnNotInBuilding`, `forceSpawnInBuilding` defaults | `false` | profile |
| `forceSpawnBuildings`, `forceSpawnParts` defaults | empty lists | profile |
| Radius fraction for city and sphere targets | 0.8 of the asset radius; squared and truncated to int | none |
| Spiral center | chunk (0, 0) | none |
| Target ring for a radius `r` blocks | `max(0, (r + 15) >> 4)` chunks | none |
| Columns tried per chunk | (8, 8) first, then x 0..15 outer, z 0..15 inner, skipping (8, 8) | none |
| Biome and sphere predicate sample height | Y 128 | none |
| Column scan | Y from `groundLevel - 5` up to Y 124 inclusive (stops below 125) | `groundLevel` of the profile at that column |
| Standing position | block face-sturdy on top, not bedrock, the two blocks above are air; spawn is one block above it | none |
| Part check height for forced parts | the building's lowest level, looked up with the constant 128 passed to `getFloorAtY` | none |
| Spawn angle | 0.0 | none |
| Chunks searched with defaults | up to 20000, a square of about 141 by 141 chunks (about ±1130 blocks) around chunk 0,0 | `spawnCheckAttempts` |

## Behavior detail

### New-world spawn: `onCreateSpawnPosition` (server)

Runs on `LevelEvent.CreateSpawnPosition`, which vanilla fires once, for the overworld of a new world.

1. Mark the world as new (`LostCityWorldGenData.initializeNewWorld`) so it uses the non-legacy street and highway generation, then bump the feature's dirty counter so any dimension info built earlier is rebuilt with that marker.
2. Get the dimension info. If the overworld has no Lost Cities profile, return and let vanilla choose the spawn.
3. Build the position predicate `isSuitable` (default: always true) from the first non-empty option, in this order:
   1. `spawnBiome`: look up the biome. If missing, log `Cannot find biome '<name>' for the player to spawn in !` and keep the default. Else the biome at the position must be that biome.
   2. `spawnCity`: look up the predefined city. If missing, log `Cannot find city '<name>' for the player to spawn in !`. Else the position must be in the city's dimension and its horizontal squared distance from the city center (`chunkX * 16 + 8`, `chunkZ * 16 + 8`) must be below `(radius * 0.8)²`.
   3. `spawnSphere`:
      - `<in>`: the city sphere for the position's chunk must be enabled, and the squared distance from the sphere center (at the center's Y) must be below `(int(radius) * 0.8)²`.
      - `<out>`: true where that sphere is disabled; else the squared distance must exceed `radius²`.
      - any other value: a predefined sphere by name. If missing, log `Cannot find sphere '<name>' for the player to spawn in !`. Else as for a city.
   Any option that set a predicate marks `needsCheck`.
4. Build the chunk predicate `isSuitableChunk` (default: always true) from the first that applies:
   1. `spawnNotInBuilding`: the chunk must not be a city chunk with a building.
   2. `forceSpawnBuildings` or `forceSpawnParts` non-empty: turn both lists into immutable sets (a duplicate entry throws, `CORE-13`), build a prefilter for the building set, and use the forced-building test below. This applies even when `forceSpawnInBuilding` is false; the profile comments say otherwise.
   3. `forceSpawnInBuilding`: the forced-building test with empty sets (any building).
   Each marks `needsCheck`.
5. Decide whether to search: for landscape types `DEFAULT` and `SPHERES`, only when `needsCheck`; for `FLOATING`, `SPACE`, `CAVERN` and `CAVERNSPHERES`, always. Otherwise return and let vanilla choose.
6. Search (below). Set the spawn in the event's level settings and on the level, angle 0. Store the position under the dimension in `spawnPositions`. Cancel the event, which skips vanilla's own spawn search and the bonus chest that vanilla places inside it (`CORE-17`; inferred from vanilla 1.20.1's `MinecraftServer.setInitialSpawn`, not from this repository).

### Forced-building test (server)

For a chunk:
1. Ask the prefilter. `NOT_CITY` or `STYLE_MISMATCH` rejects.
2. Compute the chunk characteristics. If the building set is non-empty, the chunk's building type must exist and its full ID string must be in the set.
3. Unless the profile is `space` or `spheres` and the chunk is a city-sphere center, the characteristics must say the chunk could have a building.
4. The chunk's `BuildingInfo` must be a city chunk with a building.
5. If the part set is non-empty and the building's lowest level is known, the part at that level must exist and its full ID must be in the set.

### Prefilter: `SpawnBuildingPrefilter` (server)

Creation:
1. With an empty building set, a `space` or `spheres` profile, or any listener registered for `LostCityEvent.CharacteristicsEvent`, the prefilter passes everything. Listener detection builds a throwaway event and asks its listener list for bus IDs 0 to 1023, stopping at the first `IndexOutOfBoundsException` (no more buses) and treating any other runtime exception as "has listeners".
2. Otherwise load all multibuildings. If any multibuilding contains a listed building, or loading throws, use only the raw city check.
3. Otherwise use the raw city check and the style check.

Test, for a chunk:
1. Raw city check off: `PASS`.
2. `BuildingInfo.isCityRaw` false: `NOT_CITY`.
3. Style check off, or the chunk is already occupied by a city (`City.isChunkOccupied`), or no city style: `PASS`.
4. `PASS` if the city style may select one of the buildings, else `STYLE_MISMATCH`.

### Search: `findSafeSpawnPoint` (server)

1. Start with `radius = spawnCheckRadius`, ring 0 and a checked-chunk count of 0.
2. Visit rings from the current ring up to `max(0, (radius + 15) >> 4)`. Ring 0 is chunk (0, 0). Ring `r` is walked as: top edge (`z = -r`, `x` from `-r` to `r`), right edge (`x = r`, `z` from `-r + 1` to `r`), bottom edge (`z = r`, `x` from `r - 1` down to `-r`), left edge (`x = -r`, `z` from `r - 1` down to `-r + 1`).
3. For each chunk, count it, then try it (below). Return the first position found. After a chunk fails, if the count exceeds `spawnCheckAttempts`, log `Can't find a valid spawn position!` and throw a `RuntimeException` with the same text, which aborts world creation (`CORE-14`).
4. When the rings up to the target are done, add `spawnRadiusIncrease` to `radius` and continue with the next ring. Rings are never revisited, so the walk is one continuous spiral; the radius options only decide how far each pass goes, and the attempt limit is what ends a failed search.

Per chunk (`findSafeSpawnPointInChunk`): reject if the chunk predicate fails; else try column (8, 8), then every other column in the order given in [Numbers](#numbers).

Per column (`findSafeSpawnPointAtColumn`): reject if `isSuitable` fails at Y 128. Get the profile for that chunk (`BuildingInfo.getProfile`, which picks the outside profile for sphere worlds). Scan Y upward from `groundLevel - 5` while below 125, and return the block above the first standing position. Bottom-up scanning returns the lowest open spot, which can be a cave or a cellar (`CORE-16`).

### First-login correction: `onPlayerFirstJoin` (server)

The comment in the code says single player may ignore the spawn set during creation because `level.dat` does not exist yet, so the position is also kept in `spawnPositions`.

On every `PlayerLoggedInEvent` for a server player (not only the first):
1. If `spawnPositions` has an entry for the player's current dimension and the level's shared spawn differs from it: set the level's default spawn and the level data's spawn to the stored position (angle 0), teleport the player to its block center (x + 0.5, y, z + 0.5) keeping their rotation, and remove the entry.
2. If the spawn already equals the stored position, nothing happens and the entry stays (`CORE-11`). The map is an instance field that is never cleared on server stop (`CORE-12`).

### Capability `lostcities:spawnset` (both)

Every `Player` gets a `PropertiesDispatcher` exposing `PlayerSpawnSet` (one boolean, default `false`), serialized as `{spawnSet: byte}`. Nothing reads or writes the flag, and it is not copied when a player respawns or changes dimension. It is dead data (`CORE-37`).

## On 1.12

`upstream/1.12` (`ForgeEventHandlers.java` at the package root, `WorldEvent.CreateSpawnPosition`, an API 1.7.10 also has) searches differently: it only runs for worlds of the Lost Cities world type; it samples random columns from a `java.util.Random` seeded with the world seed (two `nextFloat` calls discarded first), 200 samples per pass inside a square of half-width `radius` blocks (starting at 200, plus 100 per pass), and throws after more than 10000 samples. It has the biome, city and sphere predicates and the not-in-building option but no forced-building or part options and no prefilter, applies to `DEFAULT` and `CAVERN` only when a predicate is set and always to `FLOATING` and `SPACE`, uses the same Y range (`groundLevel - 5` to 124) and a standing test of "top solid, full cube, opaque, two air above", and sets the spawn with `World.setSpawnPoint`. It has no first-login correction.

## Deviations in the new mod

Not decided here; see `docs/new-spec/` (area 01-core).
