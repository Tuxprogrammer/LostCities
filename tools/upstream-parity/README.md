# Upstream parity values

Reference values from upstream Lost Cities 1.20 (`f18a094d`) and from Minecraft 1.20.1, recorded once so that the unit tests can check that the ported layout reproduces upstream's output for the same seed (decision log, M03 rows). The values live in [`src/test/resources/parity/`](../../src/test/resources/parity/); the tests that read them are under `src/test/java/com/tuxprogrammer/lostcities/layout/`.

Nothing here runs in the mod's build. The harness is run by hand, on the build server (decision log D4), whenever the values need recording again: after an upstream re-pin, or when a test needs a new case.

## What is recorded

| File | Source | Checked by |
|---|---|---|
| `quality_random.txt` | upstream `varia/QualityRandom`, compiled as it is | `QualityRandomTest` |
| `legacy_random.txt` | Minecraft 1.20.1 `LegacyRandomSource` | `UpstreamRandomEquivalenceTest`: equals `java.util.Random` |
| `simplex_noise.txt` | Minecraft 1.20.1 `SimplexNoise.getValue(x, y)` | `SimplexNoiseTest` |
| `city_rarity.txt` | upstream `CityRarityMap` formula over Minecraft's `SimplexNoise` (what `PerlinNoiseGenerator14` computes) | `CityRarityMapTest` |
| `resource_location_hash.txt` | Minecraft 1.20.1 `ResourceLocation.hashCode()` | `DimensionKeyTest` |
| `streets_<case>.txt` | upstream `HierarchicalStreetPlanner`, compiled as it is | `HierarchicalStreetPlannerTest` |
| `bridge_hashes.txt` | upstream `HierarchicalBridgePlanner`'s hash methods, lifted unchanged | `HierarchicalBridgePlannerTest` |

Upstream's planners above `HierarchicalStreetPlanner` (`City`, `BuildingInfo`, `MultiChunk`) read Minecraft's world, registries and biomes throughout, so they cannot run outside a 1.20 server; their behavior is checked against the spec's numbers instead ([docs/testing/](../../docs/testing/README.md#layout-parity)).

## How to record

From the repository (the local `1.20` branch must be at `f18a094d`; the script checks):

```sh
python3 -I tools/upstream-parity/stage.py <staging dir>          # upstream's pure classes + Recorder.java
cp tools/upstream-parity/record.sh <staging dir>/
# copy <staging dir> to the build server, under LC_REMOTE_ROOT, then there:
JAVA_HOME=<a JDK 17+> bash <staging dir>/record.sh <staging dir> <minecraft cache dir> <out dir>
# copy <out dir>/*.txt back into src/test/resources/parity/
```

`record.sh` downloads the Minecraft 1.20.1 server jar and Mojang's server mappings from Mojang's version manifest into the cache directory on its first run and checks the jar's SHA-1. `Recorder.java` reaches Minecraft's classes by reflection, resolving their obfuscated names through the mappings, so neither Mojang's code nor upstream's is copied into this repository; upstream's files are read from git and compiled in the staging directory only.
