# 02 · Profiles and dimensions

How Lost Cities: New Horizons configures generation per world: the profile and its options, the built-in profiles, how a dimension gets its profile and keeps it, landscape types, world styles and city-style overrides, set against upstream's [02-profiles-dimensions](../../original-spec/02-profiles-dimensions/index.md). Upstream's behavior is there; this area records what changes and why.

The three facts that shape everything here:

- **Overworld only.** Cities generate in dimension 0 over RWG2 Continents and classic RWG ([0003](../../design/0003-world-presence.md)). The Lost City dimension is not ported.
- **The profile is pinned.** It is server configuration, resolved once and written to `data/lostcitiesnh.dat` when the world is created; there is no world-preset screen ([0005](../../design/0005-rwg-overlay.md) §7, [pinned-profile.md](pinned-profile.md)).
- **Only the `default` landscape.** Floating, space, cavern, spheres and cavern-spheres need terrain RWG does not make; they and the seven profiles that use them wait for the post-beta PersonalSpace work, where this mod controls the terrain (D12).

Status: rows that cite a design record or a decision-log row are *Accepted*. Rows or details marked *Proposed* are listed with a recommendation in [Proposed, not decided](#proposed-not-decided). Facts about other mods are *verified* with a pinned permalink ([new-spec README](../README.md#how-to-read)).

## Contents

- [pinned-profile.md](pinned-profile.md): how a world's profile is resolved, pinned, shown and re-pinned, what the snapshot holds, and what happens when something changes later
- [profile-options.md](profile-options.md): all 163 upstream options, option by option, and the 8 new ones

## How to read the deviation table

The columns are as in [01-core](../01-core/index.md#how-to-read-the-deviation-table): **Original** links upstream's sheet; **Here** is *same*, *changed*, *dropped*, *deferred* or *new*; **What changes** names the 1.7.10 counterpart; **Decision** links the record or names the [decision-log](../../design/decision-log.md) row ("bug register" means only [CONTRIBUTING § Workflow](../../../CONTRIBUTING.md#workflow) item 3); **Avoids** lists the [BUGS.md](../../original-spec/BUGS.md) IDs.

## Deviations

### Profile system

Upstream: [profile-system.md](../../original-spec/02-profiles-dimensions/profile-system.md).

| Original | Here | What changes | Decision | Avoids |
|---|---|---|---|---|
| [`LostCityProfile` and the JSON format](../../original-spec/02-profiles-dimensions/profile-system.md#data-format) (five categories, `public`, no version, no inheritance) | same | Upstream's format and keys, so upstream profile files load. `Configuration` (the typed key store) becomes plain Java over Gson on the Java 8 API (no `List.of`, no records). Dropped keys are read and ignored ([profile-options.md](profile-options.md#rules-for-every-option)). | [0005](../../design/0005-rwg-overlay.md) §7, [CONTRIBUTING § Ported code](../../../CONTRIBUTING.md#ported-code) | |
| [Startup](../../original-spec/02-profiles-dimensions/profile-system.md#startup-both-sides-at-mod-construction) (built-ins rewritten as read-only files into `config/lostcities/profiles/` on every start, then all files re-read) | changed | Built-ins live in the jar and are not written out. User profiles are the JSON files in `config/lostcitiesnh/profiles/`, read in `preInit`; `/lostcities saveprofile` copies a built-in there to edit. Whether a user file may reuse a built-in's name is *Proposed* (P-PROF-2). | [0005](../../design/0005-rwg-overlay.md) §7, D13 | PROF-09 |
| [Reading one profile](../../original-spec/02-profiles-dimensions/profile-system.md#reading-one-profile-from-json-both): errors | changed | A malformed file, a wrong value type, an unknown enum value or a failed cross-option check rejects that profile with one log line naming the file, the key and the reason. Mod loading continues, and so does reading the remaining files. A dimension mapped to a rejected profile gets no cities and says why. | [CONTRIBUTING § Code standards](../../../CONTRIBUTING.md#code-standards) (fail loudly, never crash mid-generation) | PROF-02, PROF-03 |
| [Reading one profile](../../original-spec/02-profiles-dimensions/profile-system.md#reading-one-profile-from-json-both): ranges | changed | Declared ranges and allowed values are enforced on load, as are the min/max pairs upstream leaves unchecked. How a violation is handled is *Proposed* (P-PROF-1). | Bug register; *Proposed* (P-PROF-1) | PROF-04, PROF-06, PROF-16 |
| [Profile name from the file name](../../original-spec/02-profiles-dimensions/profile-system.md#startup-both-sides-at-mod-construction) (up to the first `.`) | changed | The file name minus its `.json` extension, so `my.city.json` is `my.city` and two files cannot claim one name. | Bug register | PROF-22 |
| [Copying](../../original-spec/02-profiles-dimensions/profile-system.md#copying-both) (`copyFrom`, `createProfile`, `ILostCityProfile` setters) | same | One-time copy, no live inheritance, as upstream. The API side (callbacks that never ran, `setCityChancle`, the uneven four-argument `setCityLevelHeights`) is [01-core](../01-core/index.md#api)'s, *Proposed* (P-CORE-3). | *Proposed* ([01 · P-CORE-3](../01-core/index.md#proposed-not-decided)) | PROF-01, PROF-19 |
| [Lazy block resolution](../../original-spec/02-profiles-dimensions/profile-system.md#lazy-block-resolution-server) (`liquidBlock`, `baseBlock`) | changed | Resolved once in `postInit`, after the block-mapping table, against the full registry; an unknown name logs once and falls back to water or stone, as upstream. | D14, D19 | |
| Persistence of the chosen profile | changed | Upstream stores a name (or `customized`'s JSON) per world and follows live definitions. Here every resolved value is pinned ([pinned-profile.md](pinned-profile.md)). | [0005](../../design/0005-rwg-overlay.md) §7 | PROF-09 |

### Profile options

Upstream: [profile-options.md](../../original-spec/02-profiles-dimensions/profile-options.md). The option-by-option table is [profile-options.md](profile-options.md); the rows here are the groups that change.

| Original | Here | What changes | Decision | Avoids |
|---|---|---|---|---|
| [The 163 options](../../original-spec/02-profiles-dimensions/profile-options.md) | same | 134 kept, with upstream's defaults and meanings. | [0004](../../design/0004-gtnh-layer-and-tier-ceiling.md) §1 | |
| [Height-dependent options](../../original-spec/02-profiles-dimensions/profile-options.md#120-height-assumptions) | changed | RWG's sea level is 63, so upstream's numbers carry over; `buildingMaxFloors` is limited to 22; `bedrockLayer` counts from Y 0; `seaLevel` −1 means 63; every range above 255 or below 0 is clamped to 0 to 255 at load, with a warning. | [0005](../../design/0005-rwg-overlay.md) §6 | PROF-14 |
| [Dead and config-only options](../../original-spec/02-profiles-dimensions/index.md#key-facts) (`horizon`, `fogRed`, `fogGreen`, `fogBlue`, `fogDensity`, `sphereSurfaceVariation`, `outsideSurfaceVariation`, `outsideGroundLevel`, `generateNether`) | dropped | Read and ignored. | D12 | PROF-08, PROF-17 |
| [Legacy street and highway options](../../original-spec/02-profiles-dimensions/profile-options.md#highways-read-by-06-highways-railways) (`streetGenerationMode`, `highwayGenerationMode`, `highwayRequiresTwoCities`, the three highway noise options; `highwayDistanceMask` *Proposed*) | dropped | Only the hierarchical streets and the intercity highway planner are ported. | D11; *Proposed* (P-PROF-5) | |
| [Sphere and monorail options, `cityAvoidVoid`](../../original-spec/02-profiles-dimensions/profile-options.md#category-cityspheres-14-options-read-by-03-terrain-unless-noted) | deferred | Kept in the format; no effect until the landscapes return. | D12 | PROF-07 |
| [`landscapeType`](../../original-spec/02-profiles-dimensions/profile-options.md#terrain-look-and-damage-layers-read-by-03-terrain-unless-noted) | changed | `default` only; anything else rejects the profile with a message. | [0005](../../design/0005-rwg-overlay.md) §7, D12 | PROF-15 |
| [`icon`, `description`, `extraDescription`, `warning`](../../original-spec/02-profiles-dimensions/profile-options.md#identity-and-blocks) | changed | No icons. Texts are shown by the profile command; built-ins' texts are lang keys and true to the options. | [0001](../../design/0001-port-policy-and-licensing.md) §5, [CONTRIBUTING § Assets and localization](../../../CONTRIBUTING.md#assets-and-localization) | PROF-18, UI-10 |
| [Upstream's mod-config avoidance keys](../../original-spec/01-core/config.md#server-worldserverconfiglostcities-servertoml) | changed | Become profile options (below). | [0006](../../design/0006-build-around-other-worldgen.md) §3 | CORE-07, CORE-09 |

#### Options that are new

From the design records, all pinned with the profile; the full table is [profile-options.md § Options that are new](profile-options.md#options-that-are-new).

| Option | Default | Decision |
|---|---|---|
| The lake and dungeon veto in city chunks, back from upstream 1.12 (name *Proposed*, P-PROF-6) | on | [0005](../../design/0005-rwg-overlay.md) §3 |
| `villageTreatment`, `landmarkTreatment` | `BUILD_AROUND` | [0006](../../design/0006-build-around-other-worldgen.md) §3 |
| `landmarkMargin`, `softEdgeWidth`, `villageTransitionRings`, `villageLinkDistance` | 2 blocks, 6 columns, 2 rings, 4 chunks | [0006](../../design/0006-build-around-other-worldgen.md) §1, §3, §4 |
| The vanilla loot categories opt-in (`08-loot-mobs-blocks` names it) | off | D20 |

### Built-in profiles

Upstream: [builtin-profiles.md](../../original-spec/02-profiles-dimensions/builtin-profiles.md). Ten of the seventeen are ported, with upstream's overrides except where a row says otherwise.

| Original | Here | What changes | Decision | Avoids |
|---|---|---|---|---|
| [`default`](../../original-spec/02-profiles-dimensions/builtin-profiles.md#overrides) | same | The profile `dimensionProfiles` names by default. Spawn is the pack's. How "a city a short walk from spawn" (D21) is reached is *Proposed* (P-PROF-3). | [0005](../../design/0005-rwg-overlay.md) §7, D21 | |
| [`nodamage`, `rarecities`, `onlycities`, `safe`, `ancient`](../../original-spec/02-profiles-dimensions/builtin-profiles.md#overrides) | same | `rarecities`' `highwayRequiresTwoCities = false` has no effect (legacy highways, D11). `nodamage`'s text no longer promises disabled ravines unless the lake veto covers them (P-PROF-6). | D11 | PROF-18 |
| [`tallbuildings`](../../original-spec/02-profiles-dimensions/builtin-profiles.md#overrides) | changed | `explosionMaxHeight` and `miniExplosionMaxHeight` 255 instead of 256 (the 1.7.10 build limit); `buildingMaxFloors` 19 is inside the new 0 to 22 range. | [0005](../../design/0005-rwg-overlay.md) §6 | |
| [`wasteland`](../../original-spec/02-profiles-dimensions/builtin-profiles.md#identity) | changed | Values as upstream. The extra description's advice (Biomes O' Plenty and the Wastify mod) is dropped: Wastify is not in the pack. | [CONTRIBUTING § Assets and localization](../../../CONTRIBUTING.md#assets-and-localization) | PROF-18 |
| [`atlantis`](../../original-spec/02-profiles-dimensions/builtin-profiles.md#overrides) | same | `seaLevel` 89 fills city parts with water to Y 89, and explosion damage floods to the same level. The surrounding RWG sea stays at 63, as upstream's vanilla sea does. The Lost Worlds warning is dropped. | [0005](../../design/0005-rwg-overlay.md) §6 | PROF-14 |
| [`largecities`](../../original-spec/02-profiles-dimensions/builtin-profiles.md#identity) | changed | Values as upstream; gets its own description. | Bug register | PROF-18 |
| [`cavern`, `floating`, `space`, `biosphere`, `biosphere_caves`, `bio_wasteland`, `void_outside`](../../original-spec/02-profiles-dimensions/builtin-profiles.md#overrides) | deferred | Not shipped before beta; their landscapes need terrain RWG lacks. First candidates for PersonalSpace. | D12 | PROF-07 |
| `customized` (the GUI's profile) | dropped | No profile screen; owners copy and edit a JSON file instead. | [0005](../../design/0005-rwg-overlay.md) §7 | UI-02, UI-12 |
| [Profile icons](../../original-spec/02-profiles-dimensions/builtin-profiles.md#identity) | dropped | Screenshots of Minecraft textures, never displayed upstream. | [0001](../../design/0001-port-policy-and-licensing.md) §5 | UI-10 |

### Profile selection and dimension info

Upstream: [profile-selection.md](../../original-spec/02-profiles-dimensions/profile-selection.md). The pin is in [pinned-profile.md](pinned-profile.md).

| Original | Here | What changes | Decision | Avoids |
|---|---|---|---|---|
| [Dimension-to-profile mapping](../../original-spec/02-profiles-dimensions/profile-selection.md#the-dimension-to-profile-mapping-server) (`dimensionsWithProfiles`, `selectedProfile`, `<CHECK>`) | changed | `dimensionProfiles = ["0=default"]` with integer dimension IDs, resolved once at pin time; unknown names are logged and leave the dimension without cities. A dedicated server gets cities without any hand edit. | [0005](../../design/0005-rwg-overlay.md) §7 | CORE-04, PROF-05 |
| [Profile cache](../../original-spec/02-profiles-dimensions/profile-selection.md#the-dimension-to-profile-mapping-server) (static, unsynchronized, reset on server events) | dropped | The snapshot is read once per world on the server thread. | [0005](../../design/0005-rwg-overlay.md) §7 | CORE-05, PROF-21 |
| [API entry points](../../original-spec/02-profiles-dimensions/profile-selection.md#the-dimension-to-profile-mapping-server) (`registerDimension`, `setOverworldProfile`) | dropped | Config and the pin only; no client class reachable from common code. | [0005](../../design/0005-rwg-overlay.md) §7 | CORE-06, PROF-23, CORE-01, PROF-24 |
| [`generateNether`](../../original-spec/02-profiles-dimensions/profile-selection.md#the-dimension-to-profile-mapping-server) (Nether mapped to `cavern`) | dropped | Overworld only. | D12, [0003](../../design/0003-world-presence.md) | PROF-08 |
| [`DefaultDimensionInfo` construction](../../original-spec/02-profiles-dimensions/profile-selection.md#building-the-dimension-info-server) | changed | One per-world context, built when the overlay wrapper is installed, from the pinned snapshot: the world seed, the integer dimension, the profile, the world style, the hierarchical street planner, the intercity highway planner (synchronous, no saved hubs) and the height oracle. It is dropped on `WorldEvent.Unload`. No thread-local or `volatile` state: 1.7.10 generates on the server thread. The world style is checked when the profile loads. | [0005](../../design/0005-rwg-overlay.md) §1, §4, D11 | PROF-13 |
| [`IDimensionInfo` methods](../../original-spec/02-profiles-dimensions/profile-selection.md#what-idimensioninfo-provides) | changed | `getRandom` (a stateful per-thread `Random`) goes: every choice draws from a `Random` seeded from the world seed and the coordinates it decides for. `getBiome` reads 1.7.10's 2D `WorldChunkManager.getBiomeGenAt(x, z)`, which on RWG is a pure function of the seed. Heights come from the oracle, not a heightmap of generated terrain. | [CONTRIBUTING § World generation rules](../../../CONTRIBUTING.md#world-generation-rules), [0005](../../design/0005-rwg-overlay.md) §4, D9 | |
| [Highway city constraint](../../original-spec/02-profiles-dimensions/profile-selection.md#highway-city-constraint) | same | The biome at the chunk's (8, 8) column through the 2D provider; `06-highways-railways` owns it. | D11 | |
| [Outside profile](../../original-spec/02-profiles-dimensions/profile-selection.md#outside-profile) | deferred | Only for the sphere landscapes. | D12 | PROF-07 |
| [Persisted generation modes](../../original-spec/02-profiles-dimensions/profile-selection.md#persisted-generation-modes) (`LostCityWorldGenData`) | dropped | One street mode and one highway mode exist, so there is nothing to persist; the "new world" marker goes with it. | D11 | |
| [Config keys for selection](../../original-spec/02-profiles-dimensions/profile-selection.md#config-keys-01-core-owns-the-file-listed-here-because-they-drive-selection) | changed | [01-core § Mod config](../01-core/index.md#mod-config) owns them. | [0005](../../design/0005-rwg-overlay.md) §7 | |

### Landscape types

Upstream: [landscape-types.md](../../original-spec/02-profiles-dimensions/landscape-types.md).

| Original | Here | What changes | Decision | Avoids |
|---|---|---|---|---|
| [`default`](../../original-spec/02-profiles-dimensions/landscape-types.md#what-each-type-changes) | same | Every branch upstream takes for `default` (bedrock layer, terrain correction around cities, clearing above buildings, street borders to bedrock, 15 blocks cleared above highways) is the overlay's. | [0005](../../design/0005-rwg-overlay.md) | |
| [`floating`, `space`, `cavern`, `spheres`, `cavernspheres`](../../original-spec/02-profiles-dimensions/landscape-types.md#what-each-type-changes) | deferred | They adapt cities to Lost Worlds terrain, which GTNH lacks. Their branches stay on file in the original spec for the PersonalSpace work after beta. | D12, [0003](../../design/0003-world-presence.md) §5 | PROF-07 |
| [`LandscapeType` allowed list](../../original-spec/02-profiles-dimensions/landscape-types.md) | changed | The declared list and the check are one list: `default`. | D12 | PROF-15 |

### World styles and biomes

Upstream: [world-styles.md](../../original-spec/02-profiles-dimensions/world-styles.md). The world-style JSON format is `05-assets`'; this table covers the behavior.

| Original | Here | What changes | Decision | Avoids |
|---|---|---|---|---|
| [Compilation](../../original-spec/02-profiles-dimensions/world-styles.md#compilation-server) | same | Plain Java at asset load (D14), Java 8 classes for the 1.20 records. | D14 | |
| [Biome matching](../../original-spec/02-profiles-dimensions/world-styles.md#biome-matching) (biome IDs and 1.20 biome tags) | changed | 1.7.10 biomes have names and numeric IDs, and Forge's `BiomeDictionary` types stand in for tags. RWG's biome providers return RWG's own base biomes, tagged with `BiomeDictionary` types: no RWG biome is vanilla `desert` or `mesa`, and every RWG ocean is also `BEACH` (*verified:* RWG2 [BaseBiomes.java#L73-L97](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/biomes/base/BaseBiomes.java#L73-L97), classic RWG [BaseBiomes.java#L49-L73](https://github.com/GTNewHorizons/Realistic-World-Gen/blob/alpha-1.5.3/src/main/java/rwg/biomes/base/BaseBiomes.java#L49-L73); [ChunkManagerRealistic.java#L526-L538](https://github.com/GTNewHorizons/Realistic-World-Gen-2/blob/2.0.6/src/main/java/rwg2/world/ChunkManagerRealistic.java#L526-L538)). Taken literally, `standard`'s desert style would never appear on RWG. The translation is *Proposed* (P-PROF-4). | *Proposed* (P-PROF-4) | |
| [Main biome of a chunk](../../original-spec/02-profiles-dimensions/world-styles.md#main-biome-of-a-chunk) (3D noise biome at terrain height, `BiomeInfo` time-evicted cache) | changed | The 2D biome at block (8, 8) of the chunk, cached in a bounded per-world cache. | [0005](../../design/0005-rwg-overlay.md) §8 (B5) | |
| [City chance multiplier](../../original-spec/02-profiles-dimensions/world-styles.md#city-chance-multiplier) | same | Over the translated matchers (P-PROF-4). | | |
| [City style choice](../../original-spec/02-profiles-dimensions/world-styles.md#city-style-choice) | changed | The city-center draw includes the world seed, and every chunk of a city takes its center's style. A world style must resolve a style for every biome (*Proposed*: at least one selector without a biome filter, checked at load), and `cityStyleAlternative` must exist when its threshold is on. | D10; bug register | PROF-10, PROF-11, PROF-12 |
| [Bundled world styles](../../original-spec/02-profiles-dimensions/world-styles.md#bundled-world-styles) (`standard`, `standard_everywhere`) | changed | Converted with upstream's values and the translated matchers (P-PROF-4). The scattered entries are `07-scattered`'s; its oil rig's `is_deep_ocean` filter has no RWG counterpart (no RWG biome separates deep ocean). | D14; *Proposed* (P-PROF-4) | |
| [`BiomeSelectionStrategy`](../../original-spec/02-profiles-dimensions/world-styles.md) | dropped | Dead code upstream. | Bug register | |

### Effective city settings

Upstream: [effective-city-settings.md](../../original-spec/02-profiles-dimensions/effective-city-settings.md).

| Original | Here | What changes | Decision | Avoids |
|---|---|---|---|---|
| [Resolution and constraints](../../original-spec/02-profiles-dimensions/effective-city-settings.md#resolution) | same | A final Java 8 value class in `layout`, built from the pinned profile and the city style. | D6 | |

### The Lost City dimension

Upstream: [lost-city-dimension.md](../../original-spec/02-profiles-dimensions/lost-city-dimension.md).

| Original | Here | What changes | Decision | Avoids |
|---|---|---|---|---|
| [Dimension and dimension type](../../original-spec/02-profiles-dimensions/lost-city-dimension.md#dimension-dimensionlostcityjson) | dropped | Not ported, nor its bed teleporter ([01-core](../01-core/index.md#bed-teleporter)). | [0003](../../design/0003-world-presence.md), D12 | PROF-20 |
| [Biome modifiers](../../original-spec/02-profiles-dimensions/lost-city-dimension.md#biome-modifiers-forgebiome_modifier) (features in every `#minecraft:is_overworld` biome, any dimension) | changed | No features in biomes: the overlay wraps the chunk provider of each dimension `dimensionProfiles` names, dimension 0 by default, and of no other. | [0005](../../design/0005-rwg-overlay.md) §1, [CONTRIBUTING § World generation rules](../../../CONTRIBUTING.md#world-generation-rules) (dimensions are config-gated) | PROF-08 |

## Bug register coverage

Every `PROF` ID in [BUGS.md](../../original-spec/BUGS.md) appears in a row above or in [profile-options.md](profile-options.md): PROF-01 to PROF-24. PROF-01, PROF-05, PROF-21, PROF-23 and PROF-24 are the same bugs as CORE-03, CORE-04, CORE-05, CORE-06 and CORE-01, also listed in [01-core](../01-core/index.md#bug-register-coverage). PROF-06 is also TERR-03, CITY-04 and CITY-14, and PROF-10 and PROF-11 are also CITY-03 and CITY-01 (fixed by D10); their consumers are `03-terrain`'s and `04-city-layout`'s.

## Proposed, not decided

| ID | Topic | Proposal and recommendation | Settles it |
|---|---|---|---|
| P-PROF-1 | Out-of-range values when a profile loads | **Recommend** clamping a single numeric value to its range with a warning, as [0005](../../design/0005-rwg-overlay.md) §6 already does for heights, and rejecting the whole profile for anything a clamp cannot fix: a wrong type, an unknown enum value, an inverted min/max pair, a failed cross-option check. Never fail mod loading. | Orchestrator, at M09 |
| P-PROF-2 | User profile files that reuse a built-in's name | **Recommend** rejecting them with a message: a pinned world records a name, and a name that means different things on different servers makes bug reports ambiguous. Owners copy with `saveprofile <profile> <newName>`. | Owner, at M09 |
| P-PROF-3 | "A city a short walk from spawn" (D21, [spawn.md](../00-pack-integration/spawn.md#decisions)) | Upstream's `citySpawnMultiplier1` and `citySpawnMultiplier2` (declared range 0 to 1) only scale the city factor of cities that already cover a chunk ([City.java#L378-L392](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/City.java#L378-L392)); they cannot add a city center near spawn, and with the factor already 1.0 by default they can only shrink cities. **Recommend** measuring the distance from spawn to the nearest city over the S2 seeds with the M03 layout tests first; if it is too far, add a new option that forces a city center within a radius of (0, 0), seeded from the world seed, rather than stretching the multipliers. Either way the values go on the [beta balance list](../beta-balance-list.md). | Owner, after the M03 measurement |
| P-PROF-4 | Biome matchers on 1.7.10 and RWG | **Recommend** that matchers accept Forge `BiomeDictionary` type names (`#OCEAN`, `#RIVER`, `#BEACH`, `#SANDY`, `#MESA` and so on) besides biome names, and that `standard` is converted as: ocean multiplier 0.1 for `#OCEAN`, river 0.3 for `#RIVER`; `citystyle_desert` for biomes that are `#SANDY` and `#DRY` and neither `#OCEAN` nor `#RIVER`, or `#MESA` (so RWG's Hot Desert, vanilla and Biomes O' Plenty deserts and mesas qualify). The syntax is `05-assets`'; the alpha screenshots check that desert cities appear. | Orchestrator with the `05-assets` lane, at M02 |
| P-PROF-5 | `highwayDistanceMask` | **Recommend** dropping it with legacy highways: the intercity planner never reads it, and switching highways off is the highways module flag's job. Its "0 = off" intent then holds in every mode, which is what `06-highways-railways`' HWY-06 asks. | `06-highways-railways` lane |
| P-PROF-6 | Names and category of the new options | **Recommend** the `lostcity` category for all of them, and keeping upstream 1.12's name `preventLakesRavinesInCities` for the veto only if the S2 prototype also wraps the cave carver for ravines ([0005](../../design/0005-rwg-overlay.md) §3 leaves that open); otherwise `preventLakesInCities`. Built-in texts (`nodamage`'s "ravines are disabled in cities") follow what it covers. | Orchestrator, at M09 |
| P-PROF-7 | Snapshot layout and mismatch handling | **Recommend** the NBT layout and the "log and carry on" rules in [pinned-profile.md](pinned-profile.md#stored-layout): the profile stored as its upstream-format JSON so one loader reads files and snapshots, one entry per dimension for the PersonalSpace work, and no case that refuses to load a world. | Orchestrator, at M04 (the pin lands in M04) |
