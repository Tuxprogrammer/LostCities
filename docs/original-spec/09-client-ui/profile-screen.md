# Profile configuration screen

`GuiLCConfig`, titled "Lost City Configuration": pick a standard profile for the new world's overworld, optionally customize it, edit the customized profile's options on five themed pages or one scrollable page of every option, and see a live preview. On **Done** the choice is handed to the server through `Config.profileFromClient` (`01-core/config.md`).

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `gui/GuiLCConfig` (the screen; record `ConfigUpdateResult(applied, value, error)`; static `selectProfile`). Uses `LostCitySetup` ([profile-selection-state.md](profile-selection-state.md)), the element classes ([config-widgets.md](config-widgets.md)) and the preview ([preview.md](preview.md)). Profile options edited: every option of `LostCityProfile.toConfiguration()` (`02-profiles-dimensions`); the themed pages use the 47 listed below. |
| 2 | **Inputs** | The parent screen; `LostCitySetup.CLIENT_SETUP`; `ProfileSetup.STANDARD_PROFILES`; mouse, keyboard and scroll input. |
| 3 | **Numbers** | See [Layout](#layout) and [Pages](#pages). |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | The preview seed starts at 3439249320423 and **Rnd** draws a new one from an unseeded `java.util.Random`; it never reaches the world. The screen writes into `ProfileSetup.STANDARD_PROFILES` (`customized`), which an integrated server later reads. |
| 6 | **Data formats** | `Config.jsonFromClient` holds the customized profile as JSON (`LostCityProfile.toJson(false)`, `02`). No NBT. |
| 7 | **Output** | `LostCitySetup.CLIENT_SETUP` updated; `Config.profileFromClient` and `Config.jsonFromClient` set; the `customized` standard profile overwritten; the dirty counter bumped and the profile cache reset; the global layout caches cleared. |
| 8 | **Relationships** | Opened by the Cities button ([world-creation-hook.md](world-creation-hook.md)). `selectProfile` is also the backend of the API's `setOverworldProfile` (`01-core/api.md`, `CORE-01`). Clears the caches of `BuildingInfo`, `MultiChunk`, `Highway`, `Railway`, `City`, `CitySphere`, `Scattered` (`03`, `04`, `06`, `07`). |
| 9 | **1.20 dependencies** | `Screen`, `GuiGraphics`, `Button.builder`, `Tooltip`, `AbstractWidget`, `setTooltipForNextRenderPass`, `mouseScrolled(x, y, delta)`; the `Configuration`/`Configuration.Value` profile model with allowed values and min/max (`02`). |
| 10 | **Bugs** | `UI-04`, `UI-05`, `UI-08`, `UI-12`, and through the state object `UI-01`, `UI-02`, `UI-03` |

## Layout

Coordinates in GUI pixels from the screen's top-left; `W` and `H` are the scaled screen width and height.

| Element | Position | Size | Label or content | Tooltip |
|---|---|---|---|---|
| Text | (10, 16) | | `Profile:` (white) | none |
| Profile button | (55, 10) | 80 × 20 | current profile name, or `Disabled` | the profile's description, extra description (aqua) and warning (red), or `Click here to select a profile for your Lost Cities` |
| World-style button | (145, 10) | 120 × 20 | the profile's world style, or `n.a.` | `Select the worldstyle to use for this profile` |
| Customize button | (275, 10) | 70 × 20 | `Customize` | `Create a customized version of the currently selected profile` |
| Mode button | (355, 10) | 70 × 20 | current page name | `Switch between different configuration pages` |
| Rnd button | (W − 35, 35) | 30 × 20 | `Rnd` | `Randomize the seed for the preview (does not affect the generated world)` |
| Done button | (10, H − 22) | 120 × 20 | `Done` | none |
| Cancel button | (W − 130, H − 22) | 120 × 20 | `Cancel` | none |
| Page fields | per page, below | | | |
| Preview | right side ([preview.md](preview.md)) | | | |

The profile button is created with the tooltip `Select a standard profile for your Lost City worldgen`, but `updateValues` replaces it at the end of init with the profile information above, so that text is never shown. Each frame the screen shows only the first hovered visible widget's tooltip.

### Enable rules (re-applied every frame)

| Control | Active when |
|---|---|
| Customize | a profile is selected and it is not `customized` |
| World style | the selected profile is `customized` |
| Mode | a profile is selected (customizable or customized) |
| Every page field | the selected profile is `customized` (text fields become read-only, buttons inactive) |
| `cityPerlinScale`, `cityPerlinOffset`, `cityPerlinInnerScale` | additionally, the profile's `cityChance` is below 0 (perlin rarity mode) |

## Pages

The mode button cycles `Cities` → `Buildings` → `Damage` → `Transport` → `Various` → `All` → `Cities`; the first page is `Cities`. A field is visible only on its page. Rows start at Y 40 and step 21 px. Labels are drawn at x 10, y + 5, white; prefixes at x − 8, y + 5. Boolean fields are 60 × 16 buttons reading `On` or `Off`; int and float fields are 45 × 16 text boxes; double fields have the listed width. The attribute is the profile configuration key (`02-profiles-dimensions`).

### Cities (left = 110)

Also an info text at (10, 30), wrapped at 230 px, at most 3 rows: `Cities are the main feature of Lost Cities. In this page you can control the rarity of them`.

| Y | Label | Fields (x: attribute, type) |
|---|---|---|
| 61 | `Rarity:` | 110: `cities.cityChance`, double, 120 wide |
| 82 | `Scale/Offset:` | 110: `cities.cityPerlinScale`, double, 45; 165: `cities.cityPerlinOffset`, double, 45 |
| 103 | `Inner scale` | 110: `cities.cityPerlinInnerScale`, double, 45 |
| 124 | `Threshold:` | 110: `cities.cityThreshold`, float |
| 145 | `Radius:` | 110: `cities.cityMinRadius`, int; 165: `cities.cityMaxRadius`, int |
| 166 | `Parks:` | 110: `lostcity.parkChance`, float |
| 187 | `Open lot parks:` | 110: `lostcity.openLotParkChance`, float |
| 208 | `Fountains:` | 110: `lostcity.fountainChance`, float |

### Buildings (left = 110)

| Y | Label | Fields |
|---|---|---|
| 40 | `Buildings:` | 110: `lostcity.buildingChance`, float |
| 82 | `Floors:` | 110: `lostcity.buildingMinFloors`, int; 165: `lostcity.buildingMaxFloors`, int |
| 103 | `Floor Chance:` | 110: `lostcity.buildingMinFloorsChance`, int; 165: `lostcity.buildingMaxFloorsChance`, int |
| 124 | `Cellars:` | 110: `lostcity.buildingMinCellars`, int; 165: `lostcity.buildingMaxCellars`, int |
| 166 | `Scattered Chance:` | 110: `lostcity.scatteredChanceMultiplier`, float |

### Damage (left = 70)

| Y | Label | Fields (prefix) |
|---|---|---|
| 40 | `Rubble:` | 70: `lostcity.rubbleLayer`, boolean |
| 61 | `Ruins:` | 70: `lostcity.ruinChance`, float (`%`); 150: `lostcity.ruinMinlevelPercent`, float (`-`); 210: `lostcity.ruinMaxlevelPercent`, float (`+`) |
| 82 | `Explosion:` | 70: `explosions.explosionChance`, float (`%`); 150: `explosions.explosionMinRadius`, int (`-`); 210: `explosions.explosionMaxRadius`, int (`+`) |
| 103 | `Height:` | 150: `explosions.explosionMinHeight`, int; 210: `explosions.explosionMaxHeight`, int |
| 124 | `Min/exp:` | 70: `explosions.miniExplosionChance`, float (`%`); 150: `explosions.miniExplosionMinRadius`, int (`-`); 210: `explosions.miniExplosionMaxRadius`, int (`+`) |
| 145 | `Height:` | 150: `explosions.miniExplosionMinHeight`, int; 210: `explosions.miniExplosionMaxHeight`, int |

### Transport (left = 110)

| Y | Label | Field |
|---|---|---|
| 40 | `1st perlin:` | `lostcity.highwayMainPerlinScale`, float |
| 61 | `2nd perlin:` | `lostcity.highwaySecondaryPerlinScale`, float |
| 82 | `Perlin:` | `lostcity.highwayPerlinFactor`, float |
| 103 | `Distance mask:` | `lostcity.highwayDistanceMask`, int |
| 124 | `Railways:` | `lostcity.railwaysEnabled`, boolean |
| 145 | `Rail offset:` | `lostcity.railwayLevelOffset`, int |
| 166 | `Rail N-S gap:` | `lostcity.railwaySpacingNorthSouth`, int |
| 187 | `Rail E-W gap:` | `lostcity.railwaySpacingEastWest`, int |

### Various (left = 110)

| Y | Label | Field |
|---|---|---|
| 40 | `Spawners:` | `lostcity.generateSpawners`, boolean |
| 61 | `Lighting:` | `lostcity.generateLighting`, boolean |
| 82 | `Loot:` | `lostcity.generateLoot`, boolean |
| 103 | `Vines:` | `lostcity.vineChance`, float |
| 124 | `Leafs:` | `lostcity.randomLeafBlockChance`, float |
| 166 | `Nether:` | `lostcity.generateNether`, boolean |
| 187 | `Edit mode:` | `lostcity.editMode`, boolean |

### All

Every profile option in one scrolling list ([config-widgets.md](config-widgets.md#profileconfigelement)).

## Behavior detail

1. (client) **Open.** Copy the chosen profile and customized profile from `CLIENT_SETUP` into a local `LostCitySetup` whose refresh action is the screen's `refreshPreview`. The local profile list is not copied, so it is rebuilt on the first profile toggle.
2. (client) **Init** (also on every resize): add the top bar and bottom buttons, build all five themed pages and the All page into the screen's element list, then `updateValues`. The element list is not emptied first, so each resize adds another set (`UI-05`).
3. (client) **`updateValues`**: every element reloads its value from the selected profile; clear the layout caches; refresh the profile button's tooltip.
4. (client) **Buttons:**
   - Profile: `toggleProfile` then `updateValues`;
   - World style: `toggleWorldStyle` then `updateValues`;
   - Customize: `customize` then `updateValues`;
   - Mode: next page;
   - Rnd: new preview seed, then clear caches;
   - Cancel: clear caches, return to the parent; nothing is applied, but an earlier **Customize** has already replaced the global `customized` profile (`UI-02`);
   - Done: below.
5. (client) **Editing a value** (`updateProfileValue(attribute, value)`), called by number fields and the All page:
   1. no profile: reject with `No profile is selected`;
   2. build the profile's configuration, remember the old value, set the new one;
   3. if the definition constrains it (out of range or not an allowed value), reject with `Value must be one of <list>` or `Value must be between <min> and <max>`, returning the old value;
   4. try applying the whole configuration to a scratch profile `gui_validation`; if that throws (cross-option rules, `02`), reject with the exception message;
   5. otherwise apply it to the real profile, clear caches, and return success with the stored value.
   Boolean buttons on the themed pages bypass this and write directly ([config-widgets.md](config-widgets.md)).
6. (client) **Done:**
   1. clear caches; copy the local state into `CLIENT_SETUP`;
   2. if the profile is `customized` and a customized profile exists: copy it into the global `customized` profile, then `selectProfile("customized", it)`;
   3. else `selectProfile(name, null)`, where the name may be `null` ("Disabled");
   4. return to the parent screen, bump the dirty counter, reset the profile cache.
7. (client) **`selectProfile(name, profile)`** (static): set `Config.profileFromClient`; bump the dirty counter; reset the profile cache; if a profile is given, copy it into the global `customized` profile and set `Config.jsonFromClient` to its JSON. Without a profile, `jsonFromClient` keeps any earlier value (`UI-12`).
8. (client) **Render**, every frame: background; re-apply enable and visibility rules for the current page; draw `Profile:` and every element's labels; draw the page's preview ([preview.md](preview.md); a new preview dimension is built every frame, `UI-04`); draw widgets; show the hovered widget's tooltip.
9. (client) **Scroll**: on the All page, scroll the option list by 3 rows per notch; elsewhere default handling.
10. (client) **Tick**: tick every element (text cursor blink).

## On 1.12

`upstream/1.12`'s `gui/GuiLostCityConfiguration` is a `GuiScreen` opened from the world type's Customize button, listing profiles (with `GuiMutableLabel` for text) and returning to `GuiCreateWorld`. 1.7.10 offers the same `GuiScreen`/`GuiButton`/`GuiTextField` toolkit.

## Deviations in the new mod

Not decided here; see `docs/new-spec/` (area 09-client-ui).
