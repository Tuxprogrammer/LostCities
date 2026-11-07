# Height mapping: Y 0 to 255 on RWG

Every height-dependent number and profile option upstream uses in terrain, layout, damage, highways and railways, with this mod's value and range. [0005](../../design/0005-rwg-overlay.md#6-height-mapping-q2) §6 sets the rule and assigns the full per-option list to this area.

Status: *Accepted* from 0005 §6 for the rule and the rows it names. The rows that apply the rule where 0005 does not name the option were settled by the owner's answers in D31: `buildingMinFloors` (P03-14) here, and the highway and railway limits (P06-6, P06-7) owned by [06-highways-railways](../06-highways-railways/index.md#railways-railway-layoutmd-railway-partsmd). Option defaults and ranges are upstream's, from [profile-options.md](../../original-spec/02-profiles-dimensions/profile-options.md); `02-profiles-dimensions` owns the options themselves.

## The rule

1. **Sea level is the same**, so numbers relative to it carry over unchanged. RWG2 and classic RWG fill water for `y < 63`, so the top water block is Y 62, as in the 1.20 overworld ([rwg.md](../00-pack-integration/rwg.md#rwg2-206-one-overworld-chunk)).
2. **Only the build range changes**: Y 0 to 255 (16 sections) instead of −64 to 319. RWG places bedrock at Y 0 to 4.
3. **An option whose range reaches above 255 or below 0 is clamped to 0 to 255 at load, with a warning** (0005 §6). For options counted in city levels (6 blocks each), the clamp applies to the resulting Y instead, as the highway and railway rows say (D31).
4. Hard-coded world limits (0, 1, 255, 256) in upstream's loops become named constants for the 1.7.10 limits, and the 256 "corner exists" sentinel becomes an explicit flag (TERR-24, CITY-22).

## Build range and water

| Number | Upstream 1.20 | Here | Notes |
|---|---|---|---|
| Build range | Y −64 to 319 | Y 0 to 255 | 1.7.10 |
| Sections staged per chunk | 24 | 16 | |
| Minimum build height (`getMinBuildHeight`) | −64 | 0 | Bottom bounds below |
| Maximum build height (`getMaxBuildHeight`) | 320 | 256 | Exclusive, as upstream uses it |
| Sea level (`seaLevel` −1) | 63 (generator) | 63 | RWG's fill |
| `seaLevel` override | −1 to 256 | −1 to 255 | −1 → 63 |
| `groundLevel` | 71, range 2 to 256 | 71, range 2 to 255 | Relative to sea level, unchanged |
| `bedrockLayer` | 1, range 0 to 10, counted from Y −64 | read, no effect; a warning when above 0 | 0005 §6: RWG's bedrock fills Y 0 to 4, and generation places no unbreakable block |
| Bottom of a building (`minBuild + 2`) | Y −62 | Y 2 | Cellars are dropped one at a time until the bottom is above it |
| Base under streets (stop) | first solid block or `minBuild + bedrockLayer` | first solid block | On RWG at worst its bedrock |

## City levels

| Option | Upstream default | Upstream range | Here | Notes |
|---|---|---|---|---|
| `cityLevel0Height` … `cityLevel7Height` | 75, 83, 91, 99, 107, 115, 123, 131 | 1 to 384 | same defaults, range 1 to 255 | City ground = 71 + 6 × level: Y 71 to 119. Upstream's thresholds; no RWG-tuned profile ships (D73), and the [rise cap](#rise-cap) keeps a city from climbing RWG's relief (D148) |
| `FLOORHEIGHT` | 6 | constant | 6 | Blocks per level and per floor |
| `cityMinHeight` | 50 | −1024 to 2048 | 50, range 0 to 255 | Keeps cities off deep RWG ocean (floor 34 ± 5.5) |
| `cityMaxHeight` | 150 | −1024 to 2048 | 150, range 0 to 255 | Keeps cities off RWG2 mountain chains (base 120) and high mountains, as upstream keeps them off peaks |
| `cityMaxRise` | none | none | 2 city levels, range 0 to 8 (8: no cap) | New ([Rise cap](#rise-cap)): how far a city chunk's level may rise above its city's base level |
| `heightSampleSize` (upstream's mod config) | 3 | 1 to 100 | 3, range 1 to 100, a pinned profile value (D73) | Chunks per side; not a height |
| `oceanCorrectionBorder` | 4 | −255 to 255 | same | An offset: ocean corner = `groundLevel − 4` = Y 67 |

## Rise cap

Upstream lets a city climb any slope up to `cityMaxHeight`: each chunk takes the level of its own terrain, so a city whose center lies in a valley follows RWG up a mountainside, levels 4 to 7 above its streets, with parks 40 to 60 blocks up and buildings ending against the slope (alpha review AR-4, AR-14). The owner asked for the "happy medium" between cutting the mountain flat and raising the city to the sky. Deviation (pinned profile value `cityMaxRise`, default 2; decision log D148):

| Rule | Value |
|---|---|
| Base level | The city level of the median of five oracle heights, at the strongest city center reaching the chunk and 3 chunks from it on each axis, so one hill or hollow at the center does not move the whole city. Cached per center |
| Cap | A city chunk's level is at most base + `cityMaxRise`: two levels, 12 blocks of city ground |
| No rise cutoff | Superseded (D-rise-rim-1): terrain more than a level over the cap is still city, never dropped. Its level is cut to the cap and the terrain phase lowers every column to the city ground, so the city has no holes. Where the city meets the uncut hill outside it, the hill is left as a clean vertical cut face |
| Lower ground | Uncapped: a city on a hill steps down its sides as upstream's does |
| No cap | Perlin mode (no centers), and `cityMaxRise` 8 or more |

The cap is decided from the oracle in the layout, before any chunk's characteristics, so every chunk agrees on it whatever order chunks generate in. With it, the tallest step between two neighboring city chunks is the one upstream already had between levels, a wall with stairs, and no stone-brick column or foundation wall rises more than about two levels above the street beside it (AR-14).

## Building envelope

| Number | Upstream 1.20 | Here | Notes |
|---|---|---|---|
| Height cap per chunk | `cityGround + 6 × floors < maxBuild − 8` (Y 312) | the same formula, so below Y 248 | Floors drop one at a time until it holds. With at most 21 floors at city level 8 (ground Y 119), the top floor's base stays below Y 248 |
| Part tops | up to `maxBuild − 1` | at most Y 254 | 0005 §6: the layout clamps floors per chunk so nothing passes Y 254; a building whose roof part would pass it drops a floor |
| `buildingMaxFloors` | 8, range 0 to 60 | 8, range 0 to 22 | 0005 §6 |
| `buildingMinFloors` | 0, range 0 to 60 | 0, range 0 to 22 (P03-14, D31) | The same range as `buildingMaxFloors`, since upstream lets the minimum win over the maximum; the per-chunk cap still applies |
| `buildingMinFloorsChance`, `buildingMaxFloorsChance` | 4 and 6, range 1 to 60 | same | Bounds of a draw, not heights |
| `buildingMinCellars`, `buildingMaxCellars` | 0 and 3, range 0 to 20; city level added to the maximum | same | The bottom bound (Y 2) drops cellars that would reach it |
| Clear above the roof (`clearToMax`) | to `min(maxBuild − 8, sampled height + 10)` | to `min(248, oracle height + 10)` | |
| Corridors | floor at `groundLevel − 6` (Y 65) | same | Relative to the profile ground |

## Blend

| Number | Upstream 1.20 | Here | Notes |
|---|---|---|---|
| `terrainFixLowerMinOffset`, `…MaxOffset` | −4, −3 (range −40 to 40) | same | Offsets |
| `terrainFixUpperMinOffset`, `…MaxOffset` | −1, +1 | same | |
| L1 and L2 slack | 20 + 0..4 (edge), 25 + 0..4 (diagonal) | same | Blocks |
| Unconstrained corner substitute | oracle height ± 90 | same, clamped to Y 0 to 255 before use | |
| "Fixed corner" test | `< 256` | explicit flag | 0005 §5 |
| Landmark core corner | none | the core's oracle height, offsets −1 to +1 | E1 |
| Kept surface layers | 6 | 6 | |
| Soft-edge strip | none | `softEdgeWidth` 6 columns ± 2, in the margin chunk beside a core after the blend | [pipeline-on-rwg.md](pipeline-on-rwg.md#soft-edge-strips) |
| Terrain exactly at a lowering target | left standing one block above it (TERR-39) | lowered like any higher terrain | A street no longer runs under a line of grass or sand following the contour (AR-2) |

## Damage

| Option or number | Upstream default | Upstream range | Here | Notes |
|---|---|---|---|---|
| `explosionMinHeight`, `explosionMaxHeight` | 75, 90 | 1 to 256 | same defaults, range 1 to 255 | Center Y = `6 × cityLevel + min + nextInt(max − min)`, absolute, as upstream |
| `miniExplosionMinHeight`, `miniExplosionMaxHeight` | 60, 100 | 1 to 256 | same defaults, range 1 to 255 | |
| Explosion radii | 15 to 35, 5 to 12 | 1 to 3000 | 12 to 24, 4 to 9 by default, same range | Blocks, not heights. Smaller by default so damage is an accent ([index.md](index.md#explosions-damage-debris-ruins-rubble-damage-ruins-rubblemd), AR-8) |
| Damage pass | Y 0 to 255 | Y 0 to 255 | | Upstream's hard-coded range is the 1.7.10 range (TERR-19) |
| Debris start | neighbor's top + 10, or `minBuild − 1` above the cap | neighbor's top + 10, capped at Y 255 | TERR-04 |
| Ruin column | `base + noise` to building top + 10 | the same, capped at Y 255 | TERR-29 |
| Ruin leaves fall | no lower bound | stop at Y 1 | TERR-30 |

## Highways and railways

`06-highways-railways` owns these; they are listed here so every height has one home.

| Option or number | Upstream default | Upstream range | Here | Notes |
|---|---|---|---|---|
| Highway ground | `groundLevel + 6 × level` | | same | Level 0 = Y 71 ([Highways.java#L58](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/gen/Highways.java#L58)) |
| `highwayNetworkLevel` | 0 | 0 to 32 | 0, clamped at load with a warning, in any level mode, to `floor((255 − (maxHighwayPartHeight − 1) − groundLevel) / 6)`, where `maxHighwayPartHeight` is the tallest loaded highway part (12 blocks, `highway_open`, in the bundled set): 28 at `groundLevel` 71. A profile with `groundLevel` above 244 is rejected, since no level fits (P06-6, D31) | Upstream's 32 is Y 263. Only level mode 4 reads the option ([IntercityHighwayPlanner.java#L335](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/highway/IntercityHighwayPlanner.java#L335)) |
| Highway clearance | 15 blocks above open and bridge parts ([Highways.java#L73-L77](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/gen/Highways.java#L73-L77)) | | 15 blocks, capped at Y 255 (P06-6, D31) | |
| Highway support columns | at most 40 blocks down while empty | | same | Stop at RWG's bedrock, as at any solid block |
| Highway support parts | down to the minimum build height while empty | | down to Y 1 | |
| Highway tunnel test (non-city chunk) | height > `groundLevel + 6 × level + 3` | | same, oracle height | |
| Railway level | −3 + `railwayLevelOffset` | offset −8 to 2 | same: rail Y = `groundLevel + 6 × (−3 + railwayLevelOffset)` ([Railway.java#L19-L23](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/Railway.java#L19-L23)) | Y 53 by default; Y 5 to 65 over the offset range at `groundLevel` 71, all above RWG's bedrock |
| Railway Y floor | none | | Y 5, above RWG's bedrock at Y 0 to 4: a `railwayLevelOffset` that puts the rail below it is clamped up to the lowest offset that reaches Y 5, with a warning, as other out-of-range values are; a `groundLevel` below 11, where even the highest offset (2) cannot reach Y 5, is rejected, since no clamp fixes it (P06-7, P-PROF-1, D31) | |
| Surface-station clearing | to height + 4 | | same, oracle height | |
| `monorailOffset` | −2 | −100 to 100 | deferred with monorails | D12 |

## Options that go with the deferred landscapes

`cityAvoidVoid`'s probe from Y 255, the cavern heightmap band (`max(groundLevel − 20, 1)` to 100), the space border cutoff (Y > 5), sphere center Y, `outsideGroundLevel` and `horizon` belong to landscapes and features not ported before beta (D12) or dropped with the world-preset screen ([0005](../../design/0005-rwg-overlay.md#7-profile-selection-q7) §7). Their values stay on file in the original spec.
