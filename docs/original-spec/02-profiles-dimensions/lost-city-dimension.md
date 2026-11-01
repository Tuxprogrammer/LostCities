# The Lost City dimension and feature attachment

The data-pack files that create the dedicated Lost City dimension and attach the Lost Cities features to overworld biomes. (Design record [0003](../../design/0003-world-presence.md) drops the dimension for this port; it is documented here as upstream behavior.)

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `data/lostcities/dimension/lostcity.json` (level stem `lostcities:lostcity`); `data/lostcities/dimension_type/lostcity.json` (dimension type `lostcities:lostcity`); `data/lostcities/forge/biome_modifier/lostcities.json` and `lostcity_spheres.json` (Forge biome modifiers). Keys in code: `Registration.LOSTCITY`, `DIMENSION_TYPE`, `DIMENSION` (all `lostcities:lostcity`); features `lostcities:lostcity` (`LostCityFeature`) and `lostcities:spheres` (`LostCitySphereFeature`). Placed and configured features `lostcities:lostcities` and `lostcities:spheres` live in `data/lostcities/worldgen/` (out of scope, read for context; 03-terrain). |
| 2 | **Inputs** | The server seed (`forge:use_server_seed`); vanilla overworld noise settings and biome preset. |
| 3 | **Numbers** | Dimension type: `min_y` 0, `height` 256, `logical_height` 256, `coordinate_scale` 1, `ambient_light` 0, `monster_spawn_light_level` 2, `monster_spawn_block_light_limit` 2. Placed feature `lostcities:lostcities`: `minecraft:count` 1. |
| 4 | **Behavior** | See below. |
| 5 | **Determinism** | The dimension uses the server seed (the file's `"seed": 0` is replaced by Forge's `use_server_seed`). |
| 6 | **Data formats** | Vanilla 1.20 dimension, dimension type and Forge biome modifier JSON; field values below. |
| 7 | **Output** | A dimension `lostcities:lostcity` present in every world, and the two features running once per chunk in every biome tagged `#minecraft:is_overworld`, in any dimension. |
| 8 | **Relationships** | The mod config maps the dimension to the `biosphere` profile ([profile-selection.md](profile-selection.md)). Travel into it uses the bed teleporter and the server config `specialBedBlock` (default `minecraft:diamond_block`), owned by 01-core. The features call `LostCityFeature.getDimensionInfo`, which returns `null` (no generation) for dimensions without a profile. |
| 9 | **1.20 dependencies** | Data-pack dimensions and dimension types; `minecraft:noise` generator with `minecraft:overworld` noise settings and the `multi_noise` overworld biome preset; Forge biome modifiers (`forge:add_features`) and generation steps (`raw_generation`, `top_layer_modification`); biome tag `#minecraft:is_overworld`; the world height model (`min_y`, `height`, `logical_height`). |
| 10 | **Bugs** | `PROF-08`, `PROF-20` |

## Dimension (`dimension/lostcity.json`)

| Field | Value | Meaning |
|---|---|---|
| `type` | `lostcities:lostcity` | The dimension type below. |
| `generator.type` | `minecraft:noise` | Vanilla noise generator. |
| `generator.seed` | 0 | Overridden by `forge:use_server_seed`. |
| `generator.settings` | `minecraft:overworld` | Vanilla overworld noise settings (built for Y -64..320). |
| `generator.biome_source` | `minecraft:multi_noise`, preset `overworld` | Vanilla overworld biomes. |
| `forge:use_server_seed` | `true` | Use the world's seed. |

## Dimension type (`dimension_type/lostcity.json`)

| Field | Value |
|---|---|
| `name` | `Lost City Dimension` (not a vanilla field; ignored) |
| `ultrawarm` | false |
| `natural` | true |
| `height` | 256 |
| `min_y` | 0 |
| `logical_height` | 256 |
| `coordinate_scale` | 1 |
| `shrunk` | false (not a 1.20 field; ignored) |
| `has_skylight` | true |
| `has_ceiling` | false |
| `ambient_light` | 0 |
| `piglin_safe` | false |
| `bed_works` | true |
| `respawn_anchor_works` | true |
| `has_raids` | false |
| `monster_spawn_light_level` | 2 |
| `monster_spawn_block_light_limit` | 2 |
| `infiniburn` | `#minecraft:infiniburn_overworld` |
| `effects` | `minecraft:overworld` |

The overworld noise settings describe terrain from Y -64 to 320, but this dimension type spans Y 0 to 255. Vanilla clamps noise generation to the dimension's height, so the dimension is an overworld cut to 0..255: no deepslate layers below 0 and terrain above 255 truncated (*inferred*; not generated; `PROF-20`). Monster spawning uses light level 2 and block light limit 2 rather than the 1.20 overworld's 0..7 and 0.

## Biome modifiers (`forge/biome_modifier/`)

| File | `type` | `biomes` | `features` | `step` |
|---|---|---|---|---|
| `lostcities.json` | `forge:add_features` | `#minecraft:is_overworld` | `lostcities:lostcities` | `raw_generation` |
| `lostcity_spheres.json` | `forge:add_features` | `#minecraft:is_overworld` | `lostcities:spheres` | `top_layer_modification` |

1. (server) At world load, Forge adds the city feature to the `raw_generation` step and the sphere feature to the `top_layer_modification` step of every biome in `#minecraft:is_overworld`.
2. (server) Features run per chunk for any dimension whose chunks use such biomes: the overworld, the Lost City dimension, and other mods' dimensions that reuse overworld biomes (Lost Worlds' `abyss`, mapped by default to `biosphere_caves`).
3. (server) Each feature asks for the dimension's info; without a profile it does nothing.
4. Nether and End biomes never get the features, so a profile mapped to the Nether (`generateNether`) never runs (`PROF-08`).

## Lang

None in scope. The dimension name `Lost City Dimension` is a hard-coded, unused JSON value.

## On 1.12

`upstream/1.12` registers the dimension in code (`dimensions/ModDimensions`, `LostWorldProvider`) with its own chunk generator; there are no data-pack files.

## Deviations in the new mod

The dimension is not ported ([0003](../../design/0003-world-presence.md)); details belong in `docs/new-spec/02-profiles-dimensions`.
