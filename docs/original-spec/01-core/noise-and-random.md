# Random and noise generators

The pseudo-random generator and the three noise classes in `varia`. Their exact output defines city placement, highways, ruins, rubble and terrain transitions, so a port that wants layouts to match upstream for the same seed must reproduce them bit for bit. Where each is seeded and sampled is documented by the calling area; this sheet fixes the generators themselves.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `varia/QualityRandom` (a `java.util.Random` subclass with its own 64-bit generator). `varia/NoiseGeneratorSimplex` (2D simplex noise with a seeded permutation). `varia/NoiseGeneratorPerlin` (octave sum of `NoiseGeneratorSimplex`). `varia/PerlinNoiseGenerator14` (octave sum of vanilla `SimplexNoise`, copied from Minecraft 1.14). |
| 2 | **Inputs** | A `long` seed (`QualityRandom`, `PerlinNoiseGenerator14`) or a `RandomSource` that is consumed (`NoiseGeneratorSimplex`, `NoiseGeneratorPerlin`); sample coordinates. |
| 3 | **Numbers** | See each section. |
| 4 | **Behavior** | See each section. |
| 5 | **Determinism** | All four are deterministic given their seed or source state. `QualityRandom()` without arguments seeds from `System.nanoTime()`; no caller uses it. `QualityRandom.setSeed` is inherited and does not reseed its generator. The `java.util.Random` methods built on `next(bits)` (`nextInt(bound)`, `nextFloat`, `nextDouble`, `nextBoolean`) use the same algorithms in Java 8 and Java 17, and upstream uses no Java 17-only `Random` method, so a Java 8 port can match. |
| 6 | **Data formats** | None. |
| 7 | **Output** | Pseudo-random numbers and noise values. |
| 8 | **Relationships** | `QualityRandom`: `BuildingInfo` per-building and bridge randoms, `Railway` rail-chunk type, `Scattered` (`04`, `06`, `07`). `NoiseGeneratorPerlin`: rubble, leaves, ruin and floating-island bottom-layer buffers in `LostCityTerrainFeature` (`03`). `NoiseGeneratorSimplex`: terrain-transition noise in `LostCityTerrainFeature` (`03`). `PerlinNoiseGenerator14`: `CityRarityMap` (`04`) and the legacy highway noise in `Highway` (`06`). |
| 9 | **1.20 dependencies** | `RandomSource` and `LegacyRandomSource` (1.7.10 uses `java.util.Random`); `net.minecraft.world.level.levelgen.synth.SimplexNoise` inside `PerlinNoiseGenerator14` (1.7.10 has `NoiseGeneratorSimplex`). Whether the 1.7.10 classes reproduce them is *unverified*; see [Equivalence with 1.7.10](#equivalence-with-1710). |
| 10 | **Bugs** | `CORE-25` |

## QualityRandom

A combined generator (a 64-bit linear congruential step, a xorshift step and a multiply-with-carry step) behind the `java.util.Random` interface.

| Constant | Value |
|---|---|
| Initial `v` | 4101842887655102017 |
| Initial `w` | 1 |
| LCG multiplier | 2862933555777941757 |
| LCG increment | 7046029254386353087 |
| Xorshift on `v` | `>>> 17`, `<< 31`, `>>> 8` |
| Multiply-with-carry | `w = 4294957665 × (w & 0xFFFFFFFF) + (w >>> 32)` |
| Output mix | `x = u ^ (u << 21)`, `x ^= x >>> 35`, `x ^= x << 4`, result `(x + v) ^ w` |

1. Construction with `seed`: `u = seed ^ v`; draw once; `v = u`; draw once; `w = v`; draw once.
2. `nextLong()`: advance `u` (LCG), `v` (xorshift) and `w` (multiply-with-carry), then mix as in the table. Arithmetic is 64-bit two's complement with wraparound.
3. `next(bits)`: the top `bits` bits of `nextLong()`. Every other `Random` method derives from these two.

## NoiseGeneratorSimplex

2D simplex noise. The code is 1.7.10's `NoiseGeneratorSimplex` with a `RandomSource` in place of `java.util.Random` ([Equivalence with 1.7.10](#equivalence-with-1710)).

1. Construction from a `RandomSource`: `xo`, `yo`, `zo` = `nextDouble() × 256` each, in that order; permutation `p[0..255] = 0..255`; for `l` from 0 to 255, swap `p[l]` with `p[nextInt(256 − l) + l]` and copy `p[l]` to `p[l + 256]`. This consumes 3 doubles and 256 ints from the source.
2. Gradients: the 12 vectors (±1, ±1, 0), (±1, 0, ±1), (0, ±1, ±1) in vanilla order; only their first two components are used.
3. Skew `F2 = 0.5 × (√3 − 1)`, unskew `G2 = (3 − √3) / 6`.
4. `getValue(x, y)`: standard 2D simplex with corner falloff `0.5 − d²` raised to the fourth power, result × 70. Offsets `xo`, `yo` are not applied. `fastFloor(v)` returns `(int) v` for `v > 0` and `(int) v − 1` otherwise, so whole non-positive numbers floor one too low (as in 1.7.10's `NoiseGeneratorSimplex`).
5. `add(buffer, x, z, xWidth, zWidth, xScale, zScale, factor)`: for each cell (z outer, x inner), sample at `((x + i) × xScale + xo, (z + j) × zScale + yo)` and add `70 × value × factor` into the buffer in row-major order.

## NoiseGeneratorPerlin

`levels` octaves of `NoiseGeneratorSimplex`, all built from the same `RandomSource` in turn, so each octave is different.

1. `getValue(x, y)`: sum over octaves `i = 0..levels−1` of `octave_i.getValue(x × s, y × s) / s` with `s = 1, 0.5, 0.25, …` (each octave samples at half the frequency and twice the amplitude of the last).
2. `getRegion(buffer, x, z, xWidth, zWidth, xScale, zScale, factor)`: reuse the buffer if large enough (zeroed), else allocate; then for octave `j` with `d0 = factor^j` and `d1 = 0.5^j`, add the octave over the region at scales `xScale × d0 × d1`, `zScale × d0 × d1` and amplitude `0.55 / d1`.

All callers in `03-terrain` use 4 octaves.

## PerlinNoiseGenerator14

`levels` octaves of vanilla `SimplexNoise`, each constructed from a fresh `LegacyRandomSource(seed)` with the same seed, so every octave has the same permutation and offsets (`CORE-25`).

1. `getValue(x, z)` = `noiseAt(x, z, false)`.
2. `noiseAt(x, y, useOffsets)`: sum over octaves of `octave.getValue(x × s + (useOffsets ? xo : 0), y × s + (useOffsets ? yo : 0)) / s` with `s` halving from 1.
3. `getSurfaceNoiseValue(x, y, z, scale)` = `noiseAt(x, y, true) × 0.55`; `z` and `scale` are ignored. *Unused* at the pin.

Callers use 4 octaves. `Highway` builds its X and Z noise from the same seed, so the two are the same function with the main and secondary scales swapped (`06-highways-railways`).

## Equivalence with 1.7.10

This is the one statement in the spec on whether 1.7.10's `java.util.Random` and noise classes reproduce upstream's values; other areas link here.

Settled from code:

- `varia/NoiseGeneratorSimplex` and `varia/NoiseGeneratorPerlin` match 1.7.10's `net.minecraft.world.gen.NoiseGeneratorSimplex` and `NoiseGeneratorPerlin` statement for statement: the draw order in the constructor (3 `nextDouble`, then 256 `nextInt(256 − l)`), the gradient table, `fastFloor`, the corner falloff, the factor 70, the region loop and its 0.55 amplitude. The only difference is the parameter type, `RandomSource` instead of `java.util.Random`. (Compared against the decompiled 1.7.10 MCP source in the local RetroFuturaGradle cache; not part of this repository.) Given the same random draws, the 1.7.10 classes give the same values.
- `PerlinNoiseGenerator14` is not 1.7.10's `NoiseGeneratorPerlin`. It builds every octave from a fresh `LegacyRandomSource(seed)` (`CORE-25`); `NoiseGeneratorPerlin` draws all octaves from one random in turn. Reproducing it takes one simplex generator per octave, each from a new random with the same seed.

*Verified* by M03 (decision log D45), with values recorded from Minecraft 1.20.1's own classes on the build server (`tools/upstream-parity/`, values in `src/test/resources/parity/`):

1. `new LegacyRandomSource(seed)` gives the same sequence as `new java.util.Random(seed)` for `nextInt()`, `nextInt(bound)`, `nextLong()`, `nextFloat()`, `nextDouble()` and `nextBoolean()`, for seeds including 0, ±1, `Long.MIN_VALUE` and `Long.MAX_VALUE` (`UpstreamRandomEquivalenceTest`). `fork()` has no `java.util.Random` counterpart.
2. 1.20's `SimplexNoise(RandomSource)` draws its table as 1.7.10's `NoiseGeneratorSimplex` does, and `getValue(x, y)` computes the same value, with one difference: 1.20 floors the skewed cell coordinate with a true floor (`Mth.floor`), while 1.7.10's class and upstream's `varia` copy use `fastFloor`, which is one lower for whole non-positive values (for example at (0, 0)). The values match bit for bit with a true floor over a grid that includes negative, whole and zero coordinates (`SimplexNoiseTest`).

So every `RandomSource` choice upstream makes can be reproduced with `java.util.Random` on Java 8; `PerlinNoiseGenerator14` (the city rarity map) is reproduced over a simplex with a true floor (`layout.noise.SimplexNoise`, `CityRarityMapTest` against recorded upstream values); and upstream's own `varia/NoiseGeneratorSimplex` and `NoiseGeneratorPerlin`, which keep `fastFloor`, are reproduced by 1.7.10's vanilla classes or a port that keeps `fastFloor`.

## On 1.12

`upstream/1.12`'s `QualityRandom` masks `w` with the `int` literal `0xffffffff`, which sign-extends to all 64 bits and does no masking; 1.20 uses the `long` mask `0xFFFFFFFFL`. Sequences therefore differ between the branches for the same seed. 1.12 has no copy of the noise classes in `varia`.

## Deviations in the new mod

See [new-spec 01-core § Noise and random](../../new-spec/01-core/index.md#noise-and-random).
