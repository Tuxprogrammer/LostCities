# The pinned profile

How a world gets its profile and keeps it. Upstream stores only a profile name per world ([profile-selection.md](../../original-spec/02-profiles-dimensions/profile-selection.md#profiles-are-referenced-by-name)) and hands the choice over from the world-creation screen through a sentinel in a per-world config file ([01-core config.md](../../original-spec/01-core/config.md#profile-resolution-getprofilefordimensiondimension-server)). Here there is no world-creation screen and no per-world config: the profile is server configuration, resolved once and **pinned** into the world's saved data when the world is created, so later edits to config, profile files or built-ins never change a world that already exists.

Status: *Accepted* where [0005](../../design/0005-rwg-overlay.md) §7, [0006](../../design/0006-build-around-other-worldgen.md) §2, D22 and D29 settle it; the stored layout and the mismatch handling are *Accepted* as recommended (P-PROF-7, decision log D31).

## Lifecycle

| Step | Rule | Decision |
|---|---|---|
| Resolve | `dimensionProfiles` (`config/lostcitiesnh.cfg`, default `["0=default"]`) names a profile per integer dimension ID. Profiles are the built-ins from the jar plus the JSON files in `config/lostcitiesnh/profiles/`. Only `default`-landscape profiles are valid on RWG. | [0005](../../design/0005-rwg-overlay.md) §7 |
| World type check | The overlay installs only on world types with a height oracle: `RWG2_CONTINENTS`, `RWG2`, `RWG`, `flat`. Others are skipped with a log line and get no snapshot. | [0005](../../design/0005-rwg-overlay.md) §7 |
| Pin, new world | When the overlay wrapper is installed on a world whose world info is not yet initialized, in `WorldEvent.CreateSpawnPosition` before any chunk exists, the profile is resolved and the snapshot written. | [0005](../../design/0005-rwg-overlay.md) §1, §7 |
| Pin, existing world | No snapshot means no cities, unless `enableOnExistingWorlds = true`; then the snapshot is taken at that load. The border between old and new chunks is the server owner's concern. The per-world route is the operator command `/lostcities profile pin <profile> confirm` ([commands.md](../01-core/commands.md)), which pins that one world. | [0005](../../design/0005-rwg-overlay.md) §7, D22, [0003](../../design/0003-world-presence.md) |
| Use | Every later load reads the snapshot, never the config, profile files or built-in definitions. | [0005](../../design/0005-rwg-overlay.md) §7 |
| Show, re-pin | `/lostcities profile` shows the pin; re-pinning needs an explicit `confirm` and logs that already-generated chunks will not match. | [0005](../../design/0005-rwg-overlay.md) §7 |
| Client | The mod is required on both sides, with FML's default version check, because command output uses lang keys the client resolves. No client feature reads the profile, so there is no sync packet. Single player's integrated server reads the same config file. | [0005](../../design/0005-rwg-overlay.md) §7 |

## What the snapshot holds

| Content | Why | Decision |
|---|---|---|
| Profile name | Shown by the profile command and the log | [0005](../../design/0005-rwg-overlay.md) §7 |
| Every resolved profile value | Generation reads only these; editing a profile file or a built-in changes new worlds only | [0005](../../design/0005-rwg-overlay.md) §7 |
| `floorPitch` among those values. An option may declare a legacy default (`ProfileOptions.legacyDefault`; today `floorPitch` 6 and `packHouseChance` 0, D282), which a snapshot written before the option existed reads instead of the current default, so a world pinned before 0014 keeps 6-block floors and new worlds get 8 | Old worlds keep their buildings byte-identical; new worlds get taller rooms | [0014](../../design/0014-floor-pitch.md), D295 |
| Asset-set hash | Detects that the loaded buildings, parts, palettes and styles differ from those the world was created with | [0005](../../design/0005-rwg-overlay.md) §7 |
| Layout version | Detects a deliberate layout change in a mod update ([CONTRIBUTING § World generation rules](../../../CONTRIBUTING.md#world-generation-rules)) | [0005](../../design/0005-rwg-overlay.md) §7 |
| World type name | The oracle the world was pinned for | [0005](../../design/0005-rwg-overlay.md) §7 |
| Other mods' config values each structure oracle reads (VillageNames, Roguelike Dungeons, Witchery, LootGames, AE2, RWG2) | Structure sites move when they change; the server logs it | [0006](../../design/0006-build-around-other-worldgen.md) §2 |
| The decorative metal flags | They change palettes, so they belong to the world | D29 |
| The other mod-config keys that change generated blocks: the worldgen module flags. Upstream's mod-config `heightSampleSize` is a profile option here, pinned with the other values; upstream's `forceSaplingGrowth` is dropped (saplings are never grown at generation, P03-10, D31) | Same rule as D29; upstream's per-installation `heightSampleSize` is CORE-10 | [CONTRIBUTING § World generation rules](../../../CONTRIBUTING.md#world-generation-rules) ("existing worlds stay valid"); D73; bug register |

Assets themselves are not copied into the snapshot: only their hash. A world created with one asset set and loaded with another generates new chunks with the new set; the hash makes that visible.

## Stored layout

*Accepted* (P-PROF-7, D31, D73, D136). One `WorldSavedData` named `lostcitiesnh` in the overworld's `MapStorage`, so the file is `data/lostcitiesnh.dat`, as [0005](../../design/0005-rwg-overlay.md) §7 names it, with one entry per dimension for the PersonalSpace work. The profile is stored as one string per option, by option key, in the same canonical text the profile loader produces, so a value reads back exactly; the snapshot pins every option the port keeps (format 3), and a key it lacks (a world pinned by an older version) reads as the option's default.

| NBT key | Type | Content |
|---|---|---|
| `format` | int | 3; readers accept formats 1 and 2 and tolerate missing keys ([CONTRIBUTING § Code standards](../../../CONTRIBUTING.md#code-standards)) |
| `dimensions` | compound | One entry per pinned dimension, keyed by the decimal dimension ID (`"0"` by default; PersonalSpace dimensions after beta) |
| `dimensions.<id>.profile` | string | The resolved profile's name |
| `dimensions.<id>.values` | compound | Every kept profile option, by key, as canonical text |
| `dimensions.<id>.modules` | compound | The worldgen module flags, by config key |
| `dimensions.<id>.decorativeMetals` | compound | The decorative metal flags, by config key |
| `dimensions.<id>.assetSetHash` | string | SHA-256 over the loaded asset files, in sorted path order |
| `dimensions.<id>.assetFiles` | compound | Every asset file's SHA-256, by `<folder>/<id>` (P05-3) |
| `dimensions.<id>.structureSettings` | string | The structure oracles' config inputs, one line (VillageNames' today, D119) |
| `dimensions.<id>.planSource` | string | `layout`, or `synthetic` for the M04 test city |
| `dimensions.<id>.layoutVersion` | int | The mod's layout version at pin time |
| `dimensions.<id>.worldType` | string | `WorldType.getWorldTypeName()` |
| `dimensions.<id>.createdBy` | string | The mod version that pinned it |
| `spawns.<id>` | int array | The spawn the profile's spawn options chose, `[x, y, z]` (the feet's block), when they chose one ([01-core § Spawn selection](../01-core/index.md#spawn-selection)) |

## On a later load

*Accepted* (P-PROF-7, D31); the oracle-input row is [0006](../../design/0006-build-around-other-worldgen.md) §2's. No mismatch refuses to load a world: each is logged and generation carries on.

| Mismatch | Handling |
|---|---|
| Oracle inputs differ | Log that structure sites have moved for new chunks; carry on |
| Asset hash differs | Log one warning that new chunks may not match old ones at the border; carry on with the loaded assets |
| Layout version differs | The same warning, naming both versions; carry on with the current code, since old layout code is not kept |
| A pinned value does not read (a key a newer version dropped, a value out of range, a key an older version lacked) | Unknown keys are ignored; a missing or unreadable value reads as the option's default, an out-of-range one is clamped, and each problem is logged once; never refuse to load the world |
| World type differs from the pinned one | Log and install with the oracle for the current type |

## Bugs this design must not reproduce

PROF-09 (worlds follow live profile definitions), CORE-10 (layout keys per installation), CORE-04 and PROF-05 (an unknown profile name crashes: here it is resolved once at pin time and logged), CORE-05 and PROF-21 (an unsynchronized static profile cache), CORE-06 and PROF-23 (registrations lost on cache resets).
