# City styles in the world

Which city style (the asset that picks buildings, streets, palettes and parts) governs each chunk. Source: `City.getCityStyle`, `getCityStyleInt`, `getCityStyleForCityCenter` in `worldgen/lost/City.java`, and the street-majority vote in `BuildingInfo.computeChunkCharacteristics` (`worldgen/lost/BuildingInfo.java:415-435`), on `f18a094d`.

## 1. Identity

| Member | Role |
|---|---|
| `City.getCityStyle(coord, provider, profile)` | Cached style of one chunk |
| `City.getCityStyleInt` | The uncached computation |
| `City.getCityStyleForCityCenter` | Style a city center would give (also used, incorrectly, for ordinary chunks: CITY-03) |
| `WorldStyle.getRandomCityStyle` | Weighted pick among the world style's city styles allowed for a biome (`05-assets`) |
| Street-majority vote | In `computeChunkCharacteristics`, for city chunks that cannot hold a building |

Profile options: `CITY_STYLE_THRESHOLD` (`cityStyleThreshold`), `CITY_STYLE_ALTERNATIVE` (`cityStyleAlternative`), plus the detection options in [city-detection.md](city-detection.md). Asset: the world style's `citystyles` list with biome filters and weights (`05-assets`); a predefined city's `citystyle`.

## 2. Inputs

Seed, chunk coordinates, profile, world style, the main biome of the chunk, predefined cities, the city factor field (centers, radii or Perlin rarity).

## 3. Numbers

| Constant / option | Default | Range | Meaning |
|---|---|---|---|
| `CITY_STYLE_THRESHOLD` | -1 | config 0..1 | A contribution whose factor is below this uses the alternative style. -1 disables it (factors are never negative) |
| `CITY_STYLE_ALTERNATIVE` | `""` | string | Name of the alternative city style |
| Style seed | `seed + chunkZ * 593441843 + chunkX * 217645177` | | `java.util.Random` |
| Center-style seed | `chunkZ * 899809363 + chunkX * 256203221` | | `java.util.Random`, no world seed |
| Center search radius | `(CITY_MAXRADIUS + 15) / 16` | chunks | Same square as the city factor |
| Majority window | 3 × 3 chunks, center counted twice | | Street chunks only |

## 4. Behavior

All (server).

### Style for a center (`getCityStyleForCityCenter`)

1. If a predefined city has its center at this chunk and names a city style, return that name. (The `citystyle` field is required by the codec, so the fallback below never runs for predefined cities.)
2. Otherwise seed a `java.util.Random` from the center-style seed (no world seed).
3. Ask the world style for a random city style for the chunk's main biome: collect every `citystyles` entry whose biome filter matches, then a weighted pick with one `nextFloat` (`Tools.getRandomFromList`). No match gives `null`.

### Style of a chunk (`getCityStyleInt`)

1. Seed `cityStyleRandom` from the style seed (includes the world seed).
2. Build a list of `(weight, styleName)` contributions:
   - **Perlin mode** (`CITY_CHANCE < 0`): one contribution. Weight is the rarity value at this chunk (raw, before height, biome and spawn adjustments). Name is the alternative style if the weight is below `CITY_STYLE_THRESHOLD`, else `getCityStyleForCityCenter(this chunk)`.
   - **Center mode**: for each city center in the search square whose radius reaches this chunk (no same-profile filter here, unlike the city factor), weight `(r - d) / r` and name as above. The name always comes from `getCityStyleForCityCenter(this chunk)`, not from the center chunk (CITY-03), so every contribution carries the same per-chunk name unless the alternative applies.
3. No contributions (the chunk is outside every city): `worldStyle.getRandomCityStyle(this chunk, cityStyleRandom)`.
4. Otherwise a weighted pick over the contributions with one `nextFloat` from `cityStyleRandom`. The pick can return `null` when float rounding leaves a remainder (CITY-09); the name is then `null`.
5. Resolve the name with `AssetRegistries.CITYSTYLES.get`. A `null` or unknown name yields whatever the registry returns for it (`05-assets`).

Because of CITY-03, the effective style is a per-chunk random pick from the biome-allowed city styles, seeded by chunk coordinates only. Cities are not single-style; the street-majority vote and the multibuilding style counting below are what smooth it. *Inferred* from code; not observed in game.

### Cache (`getCityStyle`)

`CITY_STYLE_CACHE` is a `TimedCache` keyed by `ChunkCoord` only. The `profile` argument is not part of the key, so in a city-sphere world the first caller's profile decides (CITY-06). Entries expire after `cacheCleanupSeconds` (default 300 s) without access and are recomputed identically.

### Street-majority vote (in `computeChunkCharacteristics`)

For a chunk that is city but cannot hold a building (`isCity && !couldHaveBuilding`):

1. For each of the 9 chunks in the 3 × 3 window (this chunk included), get `getCityStyle(neighbor, provider, thisChunkProfile)` and count its name. Count this chunk's own style a second time.
2. Take the most frequent name (`Counter.getMostOccuring`); ties are broken by `HashMap` iteration order (CITY-21).
3. Resolve that name to the chunk's style.

Every other chunk (buildings, non-city chunks) uses `getCityStyle(chunk)` directly. The aim, per the code comment, is to stop streets from switching style when two styled cities meet.

### Multibuildings

`MultiChunk` counts the styles of every chunk in its area and picks styles for multibuildings in proportion ([multibuildings.md](multibuildings.md)).

## 5. Determinism

| Random | Seed | Draws | World seed? |
|---|---|---|---|
| `cityStyleRandom` | `seed + chunkZ * 593441843L + chunkX * 217645177L` | 1 `nextFloat` | Yes |
| Center style | `chunkZ * 899809363L + chunkX * 256203221L` | 1 `nextFloat` (if any style matches the biome) | **No** (CITY-01) |

Order dependence: the cache key omits the profile (CITY-06); majority ties follow `HashMap` order (CITY-21). Otherwise a pure function of coordinates, seed, profile, assets and biome.

## 6. Data formats

Reads the world style's city-style list and a predefined city's `citystyle` (formats in `05-assets` and [predefined-content.md](predefined-content.md)). No NBT.

## 7. Output

The `CityStyle` stored in `LostChunkCharacteristics.cityStyle` and returned by `BuildingInfo.getCityStyle()`. It drives building selection, palettes, street parts, parks, fountains, bridges, stairs, fronts, rail dungeons, corridor blocks and stuff tags.

## 8. Relationships

- Called by: `computeChunkCharacteristics`, `checkBuildingPossibility` (building chance), `MultiChunk`, `BuildingInfo.getCityInfo` (API).
- Calls: `WorldStyle.getRandomCityStyle`, `BiomeInfo` (`03-terrain`), `AssetRegistries.CITYSTYLES` (`05-assets`).

## 9. 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Biome holders and biome-tag predicates | `WorldStyle.getRandomCityStyle` | City-style biome filters must map to 1.7.10 biomes or `BiomeDictionary` types |
| Data-pack registry for city styles | `AssetRegistries.CITYSTYLES` | Assets load from the jar in 1.7.10 |

## 10. Bugs

CITY-01, CITY-03, CITY-06, CITY-09, CITY-21.

## Deviations in the new mod

None recorded yet.
