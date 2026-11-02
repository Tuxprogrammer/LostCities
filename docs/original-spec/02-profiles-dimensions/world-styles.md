# World styles and biome selection

How the profile's world style picks city styles and scales city chance by biome, how biomes are matched and sampled, and the two bundled world styles. The JSON field reference for world styles is owned by `05-assets`; this sheet documents behavior and lists the fields it reads.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `worldgen/lost/cityassets/WorldStyle` (compiled world style asset). Data records in `worldgen/lost/regassets/data/`: `CityStyleSelector` (weighted city style with optional biome filter), `CityBiomeMultiplier` (city chance multiplier per biome filter), `BiomeMatcher` (biome predicate), `WorldSettings` (railway avoidance, rail part height, vine states). `worldgen/lost/BiomeInfo` (cached main biome per chunk). Loader record `regassets/WorldStyleRE` (05-assets). Unused enum `config/BiomeSelectionStrategy` (`original`, `randomized`, `varied`; *dead code*). Assets: `data/lostcities/lostcities/worldstyles/standard.json`, `standard_everywhere.json`. Profile option `worldStyle`. |
| 2 | **Inputs** | The world style asset; the chunk coordinate; the chunk's terrain height (heightmap, 03-terrain); the biome from `IDimensionInfo.getBiome`; a `java.util.Random` supplied by the caller. |
| 3 | **Numbers** | Default city chance multiplier when no filter matches: 1.0. Maximum multiplier floor: 1.0. Biome sample point: block (chunkX × 16 + 8, terrain height, chunkZ × 16 + 8). `WorldSettings` defaults: `railwayavoidance` required, `railpartheight6` 1, vines are `minecraft:vine` with the one matching side set to true. Bundled values in [the catalog](#bundled-world-styles). |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | `getRandomCityStyle` draws one `nextFloat()` from the caller's random. Callers (04-city-layout, `City`): the city-center choice uses `new Random(chunkZ × 899809363 + chunkX × 256203221)` with no world seed (`PROF-11`); the fallback choice uses `new Random(worldSeed + chunkZ × 593441843 + chunkX × 217645177)`. Selector order is the JSON array order. `BiomeInfo` caches one biome per chunk in a static time-evicted cache; eviction only costs recomputation, the value is a pure function of the world. |
| 6 | **Data formats** | Fields read here (full schema in 05-assets): `citystyles` (required list of `{factor: float, citystyle: string, biomes?: BiomeMatcher}`), `citybiomemultipliers` (optional list of `{multiplier: float, biomes: BiomeMatcher}`), `settings` (optional `WorldSettings`: `railwayavoidance` `ignore`/`block_railway` required inside the object, `railpartheight6` int default 1, `vinewest`/`vineeast`/`vinesouth`/`vinenorth` block states). `BiomeMatcher` is `{if_all?: [biome list], if_any?: [biome list], excluding?: [biome list]}` where each biome list entry is a biome id, a `#tag`, or a list of ids. Other world style fields (`outsidestyle`, `multisettings`, `cityspheres`, `scattered`, `parts`, `bridgesupport`, `highwaysupport`, `bridgesupportpart`, `highwaysupportpart`) are passed through to their consumers. |
| 7 | **Output** | A city style name (or `null`) per call; a float multiplier per chunk; the world settings record. |
| 8 | **Relationships** | Built by `AssetRegistries.WORLDSTYLES` (05-assets) for `DefaultDimensionInfo` ([profile-selection.md](profile-selection.md)). `getRandomCityStyle` is called by `City.getCityStyleForCityCenter` and `City.getCityStyleInt` (04-city-layout). `getCityChanceMultiplier` is called by `City.getCityFactor` (04-city-layout) and the highway city constraint (06-highways-railways). `BiomeInfo` is also used by `LostCityTerrainFeature`, `gen/Scattered`, `gen/Stuff` and `BuildingInfo`. The world settings are read by `BuildingInfo` (railway avoidance, rail part height) and `ChunkFixer` (vines). The other parts of a world style are consumed by 03, 04, 06 and 07. |
| 9 | **1.20 dependencies** | `Holder<Biome>`, `HolderSet`, `Biome.LIST_CODEC` and biome tags (`#minecraft:is_ocean`, `#minecraft:is_river`, `#minecraft:is_beach`, `#minecraft:is_deep_ocean`); 3D noise biomes sampled at a Y; DFU `Codec`/`RecordCodecBuilder`/`StringRepresentable`; `BlockState` codecs for vine states; data-pack registries for assets; Java records. |
| 10 | **Bugs** | `PROF-10`, `PROF-11`, `PROF-12`, `PROF-13` |

## Behavior detail

### Compilation (server)

When the world style is first requested from the registry, `WorldStyle` copies the loader record:

1. Each `citystyles` entry becomes a pair (predicate, (factor, city style name)). An entry without `biomes` gets an always-true predicate.
2. Each `citybiomemultipliers` entry becomes a pair (biome matcher, multiplier). A missing list gives no pairs.
3. Missing `settings` gives `WorldSettings.DEFAULT` (railway avoidance `ignore`, rail part height 1, default vines); missing `multisettings` gives `MultiSettings.DEFAULT` (area size 10, minimum 1, maximum 5, correct-style factor 0.8, attempts 50; 04-city-layout).

### Biome matching

`BiomeMatcher.test(biome)`:

1. If `if_all` is present, every listed set must contain the biome; otherwise false.
2. If `if_any` is present, at least one listed set must contain the biome; otherwise false.
3. If `excluding` is present, no listed set may contain the biome.
4. A matcher with no fields matches everything.

### Main biome of a chunk

`BiomeInfo.getBiomeInfo(provider, chunk)` returns the cached main biome, computing it once: take the chunk's heightmap height, then call `provider.getBiome` at block (chunkX × 16 + 8, height, chunkZ × 16 + 8). On the server this is the generator's noise biome at that 3D position. The cache is cleared at server start and stop and evicts entries after the mod config's `cacheCleanupSeconds` (default 300 s).

### City chance multiplier

`getCityChanceMultiplier(biome)` walks the multiplier list in order and returns the multiplier of the first matching entry, else 1.0. `getCityChanceMultiplier(provider, chunk)` does the same for the chunk's main biome. `City.getCityFactor` multiplies the city factor by it only after the height check passes and only when the factor is above 0.0001, then the result is clamped to 0..1 (04-city-layout).

`getMaximumCityChanceMultiplier()` returns the largest multiplier, at least 1.0, or positive infinity if any multiplier is not finite. The highway planner uses it as an upper bound.

### City style choice

`getRandomCityStyle(provider, chunk, random)`:

1. Take the chunk's main biome.
2. Collect every selector whose predicate matches, keeping JSON order.
3. Weighted pick: draw `r = random.nextFloat() × total factor`, subtract each factor in order, and return the first entry that brings `r` to zero or below.
4. Return its city style name, or `null` if no selector matched (`PROF-12`).

How the caller combines this with city circles (04-city-layout, summarized because it decides which world-style call happens):

- Perlin mode (`cityChance` < 0): the chunk's own city factor is compared with `cityStyleThreshold`; below it the city style is `cityStyleAlternative`, otherwise the "city center" style computed for the chunk itself. One candidate, so the weighted pick returns it.
- Chance mode: for every city center within `(cityMaxRadius + 15) / 16` chunks whose circle covers the chunk, add a candidate weighted by `(radius - distance) / radius`, whose style is the alternative when that weight is below `cityStyleThreshold`, otherwise the "city center" style. The center style is computed with the current chunk's coordinates, not the center's (`PROF-10`).
- No candidates: one fallback call to `getRandomCityStyle` with the seeded fallback random.
- The chosen name is looked up in the city style registry; `null` or an unknown name fails there (`PROF-12`).

## Bundled world styles

Both are in `data/lostcities/lostcities/worldstyles/`. Every built-in profile uses `standard`; `standard_everywhere` is reachable only by a user profile setting `worldStyle` to it.

| Field | `standard` | `standard_everywhere` |
|---|---|---|
| `outsidestyle` | `outside` | `outside` |
| `bridgesupport` / `highwaysupport` | `v` / `v` | `v` / `v` |
| `settings` | `railwayavoidance` `ignore`, `railpartheight6` 1 | absent (same defaults) |
| `multisettings` | area size 10, minimum 1, maximum 5, `correctstylefactor` 0.8, `attempts` 50 | area size 10, minimum 1, maximum 5 (others default to 0.8 and 50) |
| `scattered.areasize` | 8 | 8 |
| `scattered.chance` | 0.7 | 0.1 |
| `scattered.weightnone` | 30 | 20 |
| scattered `radiotower` | weight 15, max height difference 3, excluding `#minecraft:is_ocean`, `#minecraft:is_river`, `#minecraft:is_beach` | same |
| scattered `oilrig` | weight 4, max height difference 100, if any `#minecraft:is_deep_ocean` | weight 1, otherwise same |
| scattered `cabin` | weight 10, max height difference 2, excluding ocean, river, beach | same |
| scattered `highway_gas_station` | weight 12, near highway, max height difference 12, excluding ocean, river, beach | same |
| scattered `highway_restaurant` | weight 9, near highway, max height difference 12, excluding ocean, river, beach | same |
| `citybiomemultipliers` | 0.1 if any `#minecraft:is_ocean`; then 0.3 if any `#minecraft:is_river` | none (1.0 everywhere) |
| `citystyles` | `citystyle_standard` factor 0.5, any biome; `citystyle_desert` factor 9.0 if any of `minecraft:desert`, `minecraft:badlands` | same |

What the `citystyles` lists produce, per chunk main biome:

| Main biome | `citystyle_standard` | `citystyle_desert` |
|---|---|---|
| `minecraft:desert` or `minecraft:badlands` | 0.5 / 9.5 = 5.26 % | 9.0 / 9.5 = 94.74 % |
| any other (including eroded and wooded badlands) | 100 % | 0 % |

The scattered entries are consumed by 07-scattered; this table records the values only.

## On 1.12

`upstream/1.12` has no world-style biome multipliers; biome influence comes from the profile options `CITY_BIOME_FACTORS` (default `river=0`, `frozen_river=0`, `ocean=.7`, `frozen_ocean=.7`, `deep_ocean=.4`) and `CITY_DEFAULT_BIOME_FACTOR` 1.0, and `BIOME_SELECTION_STRATEGY` drives a filtered biome provider. At the pin the enum survives as dead code.

## Deviations in the new mod

See [new-spec 02-profiles-dimensions § World styles and biomes](../../new-spec/02-profiles-dimensions/index.md#world-styles-and-biomes).
