# Legacy highways (`LEGACY` mode)

The original noise-driven highway layout, still used by worlds whose persisted highway mode is `LEGACY` (every world created before the mode was persisted, or a world created with `highwayGenerationMode = LEGACY`).

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `worldgen/lost/Highway` (private `getHighwayLevel`, `hasXHighway`, `hasZHighway`, `HighwayNoise`), using `varia/PerlinNoiseGenerator14` (out of scope, `01-core`). Profile options `highwayDistanceMask`, `highwayMainPerlinScale`, `highwaySecondaryPerlinScale`, `highwayPerlinFactor`, `highwayRequiresTwoCities`, `highwayLevelFromCities`, `highwayNetworkLevel`. |
| 2 | **Inputs** | World seed (`provider.getSeed()`), dimension key, chunk coordinate, profile, raw city membership (`BuildingInfo.isCityRaw`) and city level (`BuildingInfo.getCityLevel`) of the highway's two end chunks, city spheres (space and spheres profiles). |
| 3 | **Numbers** | See [Numbers](#numbers). |
| 4 | **Behavior** | See [Behavior](#behavior). |
| 5 | **Determinism** | Two noise generators, both seeded with the world seed only. Static caches. See [Determinism](#determinism). |
| 6 | **Data formats** | Profile options only; nothing persisted. |
| 7 | **Output** | X level and Z level for the chunk (or -1); classification and empty route hits via the facade. |
| 8 | **Relationships** | Called only through `Highway.computeHighwayInfo` ([highway-occupancy.md](highway-occupancy.md)). Reads `04-city-layout` (`isCityRaw`, city level) and `03-terrain` (`CitySphere`). |
| 9 | **1.20 dependencies** | `SimplexNoise` with `LegacyRandomSource` inside `PerlinNoiseGenerator14`. |
| 10 | **Bugs** | `HWY-03`, `HWY-04`, `HWY-05`, `HWY-15`. |

## Numbers

| Option | Default | Range | Meaning |
|---|---|---|---|
| `highwayDistanceMask` | 7 | 0..2³¹-1 | A highway on an axis can only sit on rows (X highways) or columns (Z highways) whose perpendicular chunk coordinate ANDed with the mask is 0. Default 7 → every 8th chunk row or column. 0 → no legacy highways at all. "Must be a power of 2 minus 1" per its comment; not enforced. |
| `highwayMainPerlinScale` | 50.0 | 1..1000 | Divisor of the chunk coordinate along the highway's own axis |
| `highwaySecondaryPerlinScale` | 10.0 | 1..1000 | Divisor of the chunk coordinate across the highway |
| `highwayPerlinFactor` | 2.0 | -100..100 | A chunk is "highway-ish" when the noise value is above this |
| `highwayRequiresTwoCities` | true | boolean | Both ends must be city chunks (true) or one is enough (false) |
| `highwayLevelFromCities` | 3 | 0..4 | Level rule, see below |
| `highwayNetworkLevel` | 0 | 0..32 | Fixed level for mode 4 |
| Minimum highway length | end − start ≥ 5 chunks | constant | So at least 6 chunks long |
| Noise octaves | 4 | constant | `PerlinNoiseGenerator14(seed, 4)` |

Profile overrides in `ProfileSetup` (`02-profiles-dimensions`): `floating` sets the mask to 15; `space`, `biosphere`, `biosphere_caves` and `void_outside` set it to 0; `rarecities` sets `highwayRequiresTwoCities = false`.

Noise: `PerlinNoiseGenerator14` sums 4 simplex layers, each built from a `LegacyRandomSource` with the same seed. Layer *i* is sampled at the coordinate times 2⁻ⁱ and weighted by 2ⁱ, so weights are 1, 2, 4, 8 and the sum ranges roughly over ±15. The default threshold of 2.0 therefore selects the upper part of that range.

## Behavior

For one chunk and one axis (X shown; Z is the mirror with the axes swapped) (server):

1. (server) If the cache for this axis holds the chunk, return the cached level.
2. (server) If `highwayDistanceMask ≤ 0`, cache -1 and return -1.
3. (server) If the chunk's coordinate across the axis (chunk Z for an X highway) ANDed with the mask is not 0, cache -1 and return -1.
4. (server) If the dimension profile is a space or spheres profile and the chunk intersects a city sphere, cache -1 and return -1.
5. (server) Test the chunk: an X highway is present when `noiseX(chunkX / mainScale, chunkZ / secondaryScale) > perlinFactor`. A Z highway uses `noiseZ(chunkX / secondaryScale, chunkZ / mainScale)`. Divisions are floating point.
6. (server) If absent, cache -1 and return -1.
7. (server) Walk west (lower X) one chunk at a time while the test holds; the last chunk that passed is the start. Walk east the same way for the end. The walk has no bound and ignores the mask and sphere checks.
8. (server) If end − start < 5, return -1 **without caching**.
9. (server) Check the ends: with `highwayRequiresTwoCities` both the start and end chunks must be raw city chunks; otherwise one is enough. If not valid, return -1 without caching.
10. (server) Choose the level by `highwayLevelFromCities`: 0 → city level of the start (west or north) chunk; 1 → minimum of both ends; 2 → maximum; 3 → integer average (truncating); 4 → `highwayNetworkLevel`. Any other value throws "Bad value for 'highwayLevelFromCities'!".
11. (server) Cache that level for every chunk from start to end on this axis, and return it.

Edge cases:

- A chunk can be on both an X highway and a Z highway; the facade classifies it as a same-level or multi-level intersection.
- Highways are straight and end at their start and end chunks (which are city chunks or one city chunk); there is no bend, no T junction and no ramp in legacy mode.
- The city-sphere check in step 4 uses `provider.getProfile()`, not the `profile` argument.

## Determinism

- **Random sources:** two `PerlinNoiseGenerator14`, both constructed with `provider.getSeed()` and 4 octaves, created lazily per dimension key and kept in a static map. Because both use the same seed, the X and Z noise fields are the same field sampled with swapped scales (`HWY-03`). On 1.12 the Z generator used `seed ^ 879190747`.
- **Caches:** two static `ConcurrentHashMap<ChunkCoord, Integer>` (X and Z levels) and one static map of noise generators per dimension. They are unbounded and cleared only by `Highway.cleanCache()` at server start and stop and by the GUI preview (`HWY-15`). Writes from concurrent threads write identical values, so order does not change results.
- **Order dependence:** none in the result. Performance does depend on order: a chunk on a too-short or city-less highway is never cached, so every query re-walks the whole run (`HWY-05`).
- **Termination:** the walk in step 7 relies on the noise eventually dropping below the threshold. With `highwayPerlinFactor` at or below about -15 (allowed down to -100) every chunk passes and the walk never ends (`HWY-04`).

## Data formats

Profile options only (table above). Nothing is written to NBT.

## Output

The X and Z levels per chunk, cached; turned into a `HighwayInfo` by the facade.

## Relationships

- Called by `Highway.computeHighwayInfo` when the mode is `LEGACY`.
- Calls `BuildingInfo.isCityRaw` and `BuildingInfo.getCityLevel` (`04-city-layout`), `CitySphere.intersectsWithCitySphere` (`03-terrain`).

## 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| `SimplexNoise` seeded through `LegacyRandomSource` | `PerlinNoiseGenerator14` | 1.7.10 has `NoiseGeneratorSimplex` and `NoiseGeneratorPerlin` seeded from `java.util.Random`; values differ, so a legacy layout cannot be reproduced bit for bit. |

## On 1.12

`dimensions/world/lost/Highway.java` on `upstream/1.12` runs the same walk, mask, threshold and minimum length. Differences: the Z noise is seeded with `seed ^ 879190747` (separate fields); it uses `NoiseGeneratorPerlin` with `java.util.Random`; caches are plain `HashMap`s; the level modes are 0 to 3 only (no network level); the sphere exclusion applies to space profiles only.

## Bugs

- `HWY-03`: both noise generators share one seed, so X and Z highway fields are correlated (a regression from 1.12).
- `HWY-04`: the extent walk is unbounded; a very low `highwayPerlinFactor` hangs the server.
- `HWY-05`: rejected highways are not cached, so their chunks repeat the full walk on every query.
- `HWY-15`: static, unbounded caches.

## Deviations in the new mod

None recorded yet; the new spec decides.
