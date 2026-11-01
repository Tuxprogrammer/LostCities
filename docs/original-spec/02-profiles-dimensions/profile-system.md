# Profile system

How a Lost Cities profile is defined, stored, loaded, copied and validated. The options themselves are in [profile-options.md](profile-options.md); the built-in profiles are in [builtin-profiles.md](builtin-profiles.md); which profile a dimension uses is in [profile-selection.md](profile-selection.md).

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `config/LostCityProfile` (one profile: 163 public option fields plus name, public flag, description, icon and world style; implements `api/ILostCityProfile`). `config/Configuration` (a small typed key/value store with categories, min/max and allowed values; used to read, write and copy profiles). `config/ProfileSetup` (the built-in profiles and the profile directory). `config/LostCityProfileSetupImp` (the `ILostCityProfileSetup` API: make a profile from a base). Enums used by options: `LandscapeType`, `StreetGenerationMode`, `HighwayGenerationMode`, `MultiBuildingStreetConflict`; `BiomeSelectionStrategy` is unused. Static registry: `ProfileSetup.STANDARD_PROFILES` (a `HashMap<String, LostCityProfile>`). |
| 2 | **Inputs** | The built-in definitions in code; every `*.json` file in `config/lostcities/profiles/`; callbacks registered through `ILostCitiesPre.registerProfileSetupCallback` (never reached, `PROF-01`); a profile JSON string passed in from the world-creation GUI or the server config (`customized`, see [profile-selection.md](profile-selection.md)). |
| 3 | **Numbers** | 5 option categories: `lostcity` (107 options), `explosions` (12), `cities` (25), `cityspheres` (14), `client` (5); total 163. Railway spacing constants: `MIN_RAILWAY_SPACING` 4, `MAX_RAILWAY_SPACING` 128, `DEFAULT_RAILWAY_SPACING` 10 (chunks). Icon size stated in the option comment: 64 × 64 pixels. 17 built-in profiles, 14 public and 3 private. |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | No randomness. Outcomes that depend on iteration order: the order categories and keys are written to JSON follows `HashMap` order (cosmetic); profile files are read in `File.listFiles` order, so two files that map to the same profile name (`PROF-22`) resolve in a filesystem-dependent order. A world does not snapshot its profile, so editing a profile file or changing built-in defaults changes generation of new chunks in existing worlds (`PROF-09`). |
| 6 | **Data formats** | Profile JSON, see [Data format](#data-format). No NBT. The whole profile also travels as a JSON string inside a network buffer (`toBytes`, 01-core). |
| 7 | **Output** | `STANDARD_PROFILES` filled with every built-in and user profile; one read-only JSON file per profile in `config/lostcities/profiles/`. Consumers read option fields directly (`profile.GROUNDLEVEL` and so on). |
| 8 | **Relationships** | Called from the mod constructor (`LostCities` → `ModSetup.preInit` → `ProfileSetup.setupProfiles`, 01-core). Read by `Config.getProfileForDimension` and `LostCityFeature` ([profile-selection.md](profile-selection.md)), by the GUI (09-client-ui: `GuiLCConfig`, `LostCitySetup`, `ProfileConfigElement`), by the commands (01-core: `CommandSaveProfile`, edit-mode commands), and by nearly every generator (03, 04, 06, 07, 08). |
| 9 | **1.20 dependencies** | Forge `FMLPaths.CONFIGDIR`; Gson; `FriendlyByteBuf` for sync; `ForgeRegistries.BLOCKS` and `BlockState` for `liquidBlock` and `baseBlock`; Java records and `List.of`/`List.copyOf` (Java 16+ API) in `Configuration`. See the index's 1.20 dependencies table. |
| 10 | **Bugs** | `PROF-01`, `PROF-02`, `PROF-03`, `PROF-04`, `PROF-09`, `PROF-15`, `PROF-16`, `PROF-17`, `PROF-19`, `PROF-22` |

## Behavior detail

### Startup (both sides, at mod construction)

1. (both) The mod constructor calls `ModSetup.preInit`, which calls `ProfileSetup.setupProfiles` once. This runs before Forge has loaded any config and before any IMC message is processed.
2. (both) `initStandardProfiles` builds the 17 built-in profiles in code ([builtin-profiles.md](builtin-profiles.md)) and puts each into `STANDARD_PROFILES` under its name. A built-in starts from the class defaults ([profile-options.md](profile-options.md)) and overwrites a few fields.
3. (both) Every callback in `ModSetup.profileSetups` is given a `LostCityProfileSetupImp`. The list is always empty at this point, because callbacks are only added by the `ILostCitiesPre` IMC handler, which runs later in `FMLConstructModEvent` (`PROF-01`).
4. (both) `config/lostcities/profiles/` is created if missing.
5. (both) Every entry of `STANDARD_PROFILES` except one named `customized` (none exists at this point) is serialized with `toJson(readonly = true)` and written, pretty-printed and without HTML escaping, to `<name>.json`, overwriting any existing file. A write failure logs `Couldn't save profile '<name>'!` and continues.
6. (both) Every `*.json` file in the directory is read as UTF-8. The profile name is the part of the file name before the first `.`. The text is parsed into a new `LostCityProfile(name, json)` and put into `STANDARD_PROFILES`, replacing a built-in of the same name. So built-ins end up as parsed copies of the files just written, and user files add new profiles.
   - An `IOException` logs `Couldn't read profile '<file name>'!` and stops reading the remaining files (`PROF-03`).
   - Any other failure (malformed JSON, a wrong value type, an unknown `landscapeType`, a failed road, railway or highway check) is not caught and aborts mod construction (`PROF-02`).

Log lines written by this step (English, hard-coded): `Creating standard profiles into 'config/lostcities/profiles'` and `Reading existing profiles from 'config/lostcities/profiles'`.

### Reading one profile from JSON (both)

1. Fields start at the class defaults.
2. `Configuration.fromJson` walks the root object. Every top-level key becomes a category. For each member of a category object:
   - a JSON number is stored as a `Double`, with min and max both 0;
   - a string as a `String`; a boolean as a `Boolean`;
   - an array as a `String[]` (each element read as a string);
   - nested objects and nulls are ignored.
   Top-level primitives such as `"public": true` create an empty category and are otherwise ignored.
3. `public` is read from the root: absent means public; present must be a boolean.
4. `init(config)` reads every option through typed getters. A key present in the store returns the stored value, converted: a `Double` becomes `float` or `int` (truncated toward zero) as the option needs; a key absent from the store is added with the class default and the code's declared min, max and allowed values, and the default is returned.
5. Declared ranges and allowed values are **not** applied on this path: `Configuration.Value.constrain()` is only called by the GUI when a user edits one value (`PROF-04`). The only checks are the explicit ones in step 6.
6. Explicit checks, each throwing `IllegalArgumentException` (or `RuntimeException` for the landscape type):
   - `landscapeType` must be one of the six `LandscapeType` names (`default`, `floating`, `space`, `cavern`, `spheres`, `cavernspheres`); otherwise `Bad landscape type: <value>!`.
   - `streetGenerationMode`, `highwayGenerationMode` and `multiBuildingStreetConflict` must name an enum constant, case-insensitive.
   - Secondary-road minimum counts may not exceed the maximums; `tertiaryRoadMinLength` may not exceed `tertiaryRoadMaxLength`.
   - `primaryRoadSpacingX` and `primaryRoadSpacingZ` must be 8–128; `primaryRoadOptionalChance` 0–1; `primaryRoadForceEvery` 1–16.
   - `plannedPrimaryBridgeChance` 0–1; `plannedPrimaryBridgeMaxLength` 1–64.
   - Secondary-road counts 0–128; `minimumRoadSeparation` and `minimumRoadEdgeDistance` 2–32.
   - `tertiaryRoadChance` 0–1, `tertiaryRoadMinLength` at least 1, `tertiaryRoadMaxLength` at most 32.
   - `railwaySpacingNorthSouth` and `railwaySpacingEastWest` must be even and 4–128.
   - `highwayHubSampleSpacing` may not exceed `highwayPlanningCellSize`; `highwayMinimumHubDistance` may not exceed `highwayMaximumHubDistance`.

   The exact English messages are in [profile-options.md](profile-options.md#validation-messages).
7. Unknown keys and keys in the wrong category are silently ignored. A value of the wrong JSON type for its option (a string where a number is expected, a number where a boolean is expected) throws `ClassCastException` (`PROF-02`).
8. The cached icon, liquid block and base block are cleared so they are resolved again on first use.

### Writing a profile to JSON (both)

`toJson(readonly)` runs `init` against an empty `Configuration`, which records every option with its current value, then emits one object per category. When `readonly` is true each category object also gets the key `__readonly__` with the text `This profile is read only and cannot be modified! If you want to make a new profile based on this then you can make a copy to a new name`. The root gets `"public": <isPublic>`. `Float` and `Double` values are written with Java's shortest round-trip text, so a float field set from a float literal round-trips exactly. String arrays become JSON arrays.

### Copying (both)

- `copyFrom(other)` serializes `other` into a fresh `Configuration` and re-reads it into this profile. The name and public flag of the target are kept. This is the only form of "inheritance" between profiles at the pin: a copy taken once, not a live parent link.
- `LostCityProfileSetupImp.createProfile(name, baseProfile)` (API): looks up `baseProfile` in `STANDARD_PROFILES` (throws `Unknown base profile '<base>'!` if missing), creates a public profile `name`, copies the base into it, registers it under `name` and returns it as `ILostCityProfile`.
- `ILostCityProfile` setters let an API user change the description, world style, city chance (`setCityChancle`, sic), ruin chance with its two level percentages, ground level, city level heights and ocean correction border. The four-argument `setCityLevelHeights(l0, l1, l2, l3)` sets levels 4–7 to `l3 + 30`, `l3 + 40`, `l3 + 50` and `l3 + 60` (`PROF-19`).

### Lazy block resolution (server)

`getLiquidBlock()` and `getBaseBlock()` look the stored block name up in the Forge block registry on first call and cache the default state. An unknown name logs `Bad liquid block: <name>!` or `Bad base block: <name>!` and falls back to water or stone. The cache field is unsynchronized but the computed value is the same on every thread.

### Landscape predicates

`isDefault`, `isFloating`, `isSpace` test one type each. `isCavern` is true for `cavern` and `cavernspheres`. `isVoidSpheres` is true for `spheres` only. `isSpheres` is true for `spheres` and `cavernspheres`. What each type does is in [landscape-types.md](landscape-types.md).

## Data format

A profile file is one JSON object:

| Key | Type | Meaning |
|---|---|---|
| `public` | boolean, default `true` | Whether the profile is offered in the world-creation GUI (09-client-ui). Built-ins `biosphere_caves`, `bio_wasteland` and `void_outside` are `false`. |
| `lostcity` | object | General options (107 keys). |
| `explosions` | object | Explosion and damage options (12 keys). |
| `cities` | object | City placement options (25 keys). |
| `cityspheres` | object | City sphere options (14 keys). |
| `client` | object | Client display options (5 keys, all unused). |
| `<category>.__readonly__` | string | Present in files written for built-ins. Ignored on read. |

Every key, type, default and range is in [profile-options.md](profile-options.md). The format has no version field and no inheritance key.

## Lang

None. Profile descriptions, warnings and option comments are hard-coded English (see [builtin-profiles.md](builtin-profiles.md) for the exact description strings). The option comments appear as GUI tooltips (09-client-ui).

## On 1.12

`upstream/1.12` (`e8900023`), `src/main/java/mcjty/lostcities/config/LostCityProfile.java` (601 lines) has real profile inheritance: a constructor takes an `inheritFrom` profile, and every option's default is read from the parent (`inheritFrom.orElse(this).<FIELD>`). Profiles are Forge `.cfg` files built by `ConfigSetup` and `LostCityConfiguration`. It also has options that 1.20 dropped: a `structures` category, `BIOME_SELECTION_STRATEGY`, `CITY_BIOME_FACTORS`, `ALLOWED_BIOME_FACTORS`, `MANUAL_BIOME_MAPPINGS`, `GENERATOR_OPTIONS`, `WATERLEVEL_OFFSET` and the `AVOID_GENERATED_*` switches.

## Deviations in the new mod

To be decided in `docs/new-spec/02-profiles-dimensions`. None recorded here.
