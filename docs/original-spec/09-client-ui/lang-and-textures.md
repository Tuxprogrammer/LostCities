# Lang files, player-visible text and textures

Every lang key upstream ships, every hard-coded player-visible string in the client UI, and every texture under `src/main/resources` (all GUI images). Command and chat strings are listed with their commands in `01-core`.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `assets/lostcities/lang/en_us.json`, `en_US.lang`, `de_DE.lang`, `en_UD.lang`, `es_AR.lang`, `zh_CN.lang`; `assets/lostcities/textures/gui/*.png` (18 files). |
| 2 | **Inputs** | None. |
| 3 | **Numbers** | Image sizes below. |
| 4 | **Behavior** | Minecraft 1.20 loads only `.json` lang files; the five `.lang` files are ignored. No code at the pin translates a lang key. |
| 5 | **Determinism** | Not applicable. |
| 6 | **Data formats** | JSON lang (key to text); legacy `key=value` `.lang` files; PNG images. |
| 7 | **Output** | One configuration-button image is drawn ([world-creation-hook.md](world-creation-hook.md)); nothing else. |
| 8 | **Relationships** | Profile icons are named by `ProfileSetup` through `LostCityProfile.setIconFile`, and the profile option `icon` (`02-profiles-dimensions`); `LostCityProfile.getIcon` is never called. |
| 9 | **1.20 dependencies** | JSON lang files keyed `generator.*` for world presets (the 1.12 keys are for world types). |
| 10 | **Bugs** | `UI-09`, `UI-10`, `UI-11` |

## Lang keys

All three keys name world generators that do not exist at the pin (`UI-11`).

| Key | File | Text | Used by |
|---|---|---|---|
| `generator.lc_cavern` | `en_us.json` | `Lost Cavern` | nothing (a former `lc_cavern` world type, per the upstream changelog) |
| `generator.lostcities` | `en_US.lang` | `Lost Cities` | nothing (1.12 world type; `.lang` not loaded in 1.20) |
| `generator.lostcities_bop` | `en_US.lang` | `Lost Cities (BoP)` | nothing (1.12 Biomes O' Plenty world type) |

Translations of the two `.lang` keys:

| File | `generator.lostcities` | `generator.lostcities_bop` |
|---|---|---|
| `de_DE.lang` | `Verlassene Städte` | `Verlassene Städte (BoP)` |
| `en_UD.lang` | `sǝᴉʇᴉƆ ʇsoꞀ` | `(Ԁoᗺ) sǝᴉʇᴉƆ ʇsoꞀ` |
| `es_AR.lang` | `Ciudades perdidas` | `Ciudades perdidas (BoP)` |
| `zh_CN.lang` | `失落的城市` | none |

## Hard-coded client text

Every string below is a literal in code, not a lang key (`UI-11`).

| String | Where |
|---|---|
| `Cities` | Create World button |
| `Lost City Configuration` | Screen title |
| `Profile:` | Top-bar text |
| `Disabled`, `n.a.` | Profile and world-style button labels when unset |
| `Customize`, `Done`, `Cancel`, `Rnd` | Buttons |
| `Cities`, `Buildings`, `Damage`, `Transport`, `Various`, `All` | Page names on the mode button |
| `Select a standard profile for your Lost City worldgen` | Profile button tooltip (replaced at once, never shown) |
| `Select the worldstyle to use for this profile` | World-style tooltip |
| `Create a customized version of the currently selected profile` | Customize tooltip |
| `Switch between different configuration pages` | Mode tooltip |
| `Randomize the seed for the preview (does not affect the generated world)` | Rnd tooltip |
| `Click here to select a profile for your Lost Cities` | Profile tooltip with no profile |
| `Cities are the main feature of Lost Cities. In this page you can control the rarity of them` | Cities page text |
| Field labels: `Rarity:`, `Scale/Offset:`, `Inner scale`, `Threshold:`, `Radius:`, `Parks:`, `Open lot parks:`, `Fountains:`, `Buildings:`, `Floors:`, `Floor Chance:`, `Cellars:`, `Scattered Chance:`, `Rubble:`, `Ruins:`, `Explosion:`, `Height:`, `Min/exp:`, `1st perlin:`, `2nd perlin:`, `Perlin:`, `Distance mask:`, `Railways:`, `Rail offset:`, `Rail N-S gap:`, `Rail E-W gap:`, `Spawners:`, `Lighting:`, `Loot:`, `Vines:`, `Leafs:`, `Nether:`, `Edit mode:` | Themed pages |
| Prefixes `%`, `-`, `+` | Damage page |
| `On`, `Off` | Boolean buttons |
| `Invalid value: <error>` | Field tooltips on rejection |
| `No profile is selected`, `Value must be one of <list>`, `Value must be between <min> and <max>` | Rejection reasons (cross-option messages come from profile validation, `02`) |
| `All profile settings (<first>-<last> of <count>) - scroll to browse` | All page header |
| `Allowed: `, `Range: `, ` to `, `Enter a comma-separated list` | All page tooltips |
| Profile descriptions, extra descriptions and warnings | Defined in `ProfileSetup` (`02`), shown in the profile tooltip |

## Textures

All are 8-bit PNGs in `assets/lostcities/textures/gui/`. Descriptions are observed from the image files, not from the game. Every image is an in-game screenshot of Lost Cities terrain, so it shows Minecraft's own block textures.

| File | Size | Alpha | Depicts | Used by |
|---|---|---|---|---|
| `configicon.png` | 256 × 256 | yes; rows 192 to 255 (the bottom quarter, 16384 pixels) are fully transparent, the rest opaque | A city street with grass verges, an office tower with a rooftop antenna and a gray block building | Cities button image (drawn at 70 × 70) |
| `icon_ancient.png` | 128 × 128 | no | Buildings overgrown by jungle trees | `ancient` profile icon (never displayed) |
| `icon_atlantis.png` | 128 × 128 | no | A flooded city: a brick building rising from open water | `atlantis` profile icon (never displayed) |
| `icon_biosphere.png` | 128 × 128 | no | A translucent yellow glass dome over a city on a terrain island | `biosphere` profile icon (never displayed) |
| `icon_bubbles.png` | 128 × 128 | no | A blue glass sphere holding a city, in a blue sky | Unreferenced (its `waterbubbles` profile is commented out) |
| `icon_cavern.png` | 128 × 128 | no | A city in a large lit cave, with water and grass under a stone ceiling | `cavern` profile icon (never displayed) |
| `icon_cavespheres.png` | 128 × 128 | channel present, every pixel opaque | A dark glass sphere with a city inside, set in stone | `biosphere_caves` profile icon (never displayed) |
| `icon_chisel.png` | 128 × 128 | no | Street-level view of pale buildings along a grassy road | Unreferenced (its `chisel` profile is commented out) |
| `icon_default.png` | 128 × 128 | no | A city on a river with a bridge | `default` and `largecities` profile icons (never displayed) |
| `icon_floating.png` | 128 × 128 | no | A floating island carrying buildings and trees in open sky | `floating` profile icon (never displayed) |
| `icon_nodamage.png` | 128 × 128 | no | An intact city with a park, a domed pavilion and steps | `nodamage` profile icon (never displayed) |
| `icon_onlycities.png` | 128 × 128 | no | Dense city blocks and streets with no countryside | `onlycities` profile icon (never displayed) |
| `icon_rarecities.png` | 128 × 128 | no | Open grassland with a highway bridge over a river and a distant city | `rarecities` profile icon (never displayed) |
| `icon_realistic.png` | 128 × 128 | no | High view of forested land with rivers, lakes and scattered buildings | Unreferenced (its `realistic` profile is commented out) |
| `icon_safe.png` | 128 × 128 | no | A city at night with lit windows | `safe` profile icon (never displayed) |
| `icon_space.png` | 128 × 128 | no | A glass sphere holding a city, floating in a bright sky | `space` profile icon (never displayed) |
| `icon_tallbuildings.png` | 128 × 128 | no | Tall skyscrapers | `tallbuildings` profile icon (never displayed) |
| `icon_wasteland.png` | 128 × 128 | no | A gray, ruined, treeless city with a highway | `wasteland` profile icon (never displayed) |

The profile option `icon` describes the icon as 64 × 64; the files are 128 × 128 (`UI-10`).

## Deviations in the new mod

See [new-spec 09-client-ui § Lang and textures](../../new-spec/09-client-ui/index.md#lang-and-textures) and `CONTRIBUTING.md` § Assets and localization (lang keys in the `lostcitiesnh` namespace; no Mojang assets in the jar).
