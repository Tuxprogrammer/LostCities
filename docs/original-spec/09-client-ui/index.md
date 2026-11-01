# 09 · Client UI

## Overview

The client side of upstream Lost Cities: the **Cities** button on the Create World screen, the profile configuration screen with its five themed pages, the All page and its live preview, the state object behind it, the client-side event handlers, the lang files and the GUI textures. It documents Lost Cities 1.20-7.5.7 for Minecraft 1.20.1 (upstream branch `1.20` at `f18a094d`). Source paths are relative to `src/main/java/mcjty/lostcities/` unless they start with `src/`.

## Contents

- [world-creation-hook.md](world-creation-hook.md): the Cities button and image on the Create World screen, client logout cleanup, the commented-out fog handlers
- [profile-screen.md](profile-screen.md): `GuiLCConfig` layout with pixel positions, every page and field, Done/Cancel and how the choice reaches the server
- [profile-selection-state.md](profile-selection-state.md): `LostCitySetup`: profile cycling, customization, world-style cycling
- [config-widgets.md](config-widgets.md): the field classes and the scrollable All page (`ProfileConfigElement`)
- [preview.md](preview.md): `NullDimensionInfo` and the city map, transport map and city/damage side views
- [lang-and-textures.md](lang-and-textures.md): every lang key with its text, every hard-coded string, every texture

## Key facts

- **The screen only exists on world creation.** It is opened from the Create World screen's More tab and returns there. Its only output that matters to the world is `Config.profileFromClient` (and `jsonFromClient` for a customized profile), which the integrated server copies into the new world's server config on first use (`01-core/config.md`).
- **Only a profile named `customized` can be edited.** Pressing **Customize** copies the chosen standard profile into a non-public `customized` profile in the global standard-profile map, at once and even if the screen is then cancelled.
- **The preview runs the real layout code** on a fixed 62 × 58 chunk fake landscape, through `NullDimensionInfo`, sharing the static layout caches with real generation (cleared on every change).
- **No player-visible client text is localized.** Every label, tooltip and message is a literal English string; the shipped lang keys name world types that no longer exist.
- **Profile icons are never shown.** 17 icon PNGs ship; 14 are assigned to profiles but `LostCityProfile.getIcon` has no caller, and three belong to commented-out profiles. Only `configicon.png` is drawn.
- **Every image is an in-game screenshot**, so each shows Minecraft's own block textures.
- **Client-only classes are reachable from common code:** the API's `setOverworldProfile` calls `GuiLCConfig.selectProfile` (`CORE-01`).

## Related areas

- `01-core`: `Config.profileFromClient` and profile resolution, the dirty counter and lifecycle cleanup, the API entry that reuses `selectProfile`, the access transformer that opens `CreateWorldScreen`'s tab manager
- `02-profiles-dimensions`: every profile option edited here, `Configuration` validation, standard profiles, descriptions, warnings and icon assignments
- `03-terrain`: `LostCityTerrainFeature` and `ChunkHeightmap` built by the preview
- `04-city-layout`: `BuildingInfo` GUI variants, street planner, caches cleared by the preview
- `05-assets`: world-style files listed by the world-style button
- `06-highways-railways`: highway and railway queries and the intercity planner used by the transport preview

## Coverage

- Source files read: **38 of 38** in scope, each in full: `gui/GuiLCConfig.java`, `gui/LostCitySetup.java`, `gui/NullDimensionInfo.java`, `gui/elements/*` (9); `setup/ClientSetup.java`, `setup/ClientEventHandlers.java`; `src/main/resources/assets/lostcities/lang/*` (6); `src/main/resources/assets/lostcities/textures/gui/*.png` (18, each viewed and measured).
- The other in-scope files of the S1 brief (69) are covered in `01-core`, for 107 of 107 across both areas.
- Lang keys covered: 3 of 3 (`generator.lc_cavern`, `generator.lostcities`, `generator.lostcities_bop`), with all 9 translated strings in 5 legacy files.
- Textures covered: 18 of 18.
- Out-of-scope files read, in part: `config/ProfileSetup.java` (profile list and icon assignments), `config/LostCityProfile.java` (`isPublic`, `iconFile`, `getIcon`), `worldgen/LostCityFeature.java` (dirty-counter handling), `changelog.txt` (the `lc_cavern` entry), and on `upstream/1.12` (`e8900023`) `ClientEventHandlers.java` and the world-type Customize hook (by search).
- Corrections to earlier statements: none (first version).

## Known limits

- Nothing was run. Layout, colors and positions are read from code; how the screen looks is *inferred*. Text widths and wrapping depend on the font and GUI scale.
- Texture descriptions are observed from the image files, not in game.
- Whether `UI-06` (the null biome registry) is reachable depends on whether any GUI-variant layout path asks the preview dimension for a biome; that is in `04-city-layout`'s code and was not traced.
- The `Configuration` view (option names, comments, ranges, cross-option rules) is `02-profiles-dimensions`' to document; this area documents only how the GUI uses it.

## 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| `CreateWorldScreen` tabs (`tabManager`, `MoreTab`) opened by access transformer; `ScreenEvent.Init.Post`/`Render` | `setup/ClientEventHandlers` | 1.7.10's `GuiCreateWorld` has no tabs; 1.12 used the world type's Customize button, and 0003 rules out a separate world type, so the entry point must be redesigned |
| `GuiGraphics`, `Button.builder`, `Tooltip`, `EditBox` responders, `MultiLineTextWidget`, `setTooltipForNextRenderPass` | `gui/*` | 1.7.10 has `GuiScreen`, `GuiButton`, `GuiTextField` and manual tooltip drawing |
| `PlayerEvent.PlayerLoggedOutEvent` used for client cleanup | `setup/ClientEventHandlers` | A server-side event in both versions; 1.7.10 has `FMLNetworkEvent.ClientDisconnectionFromServerEvent` for the client side |
| Data-pack listing on the client (`PackRepository`, `MultiPackResourceManager`, `PackType.SERVER_DATA`) | `gui/LostCitySetup.toggleWorldStyle` | 1.7.10 has no data packs; world styles come from wherever the port loads assets |
| `IDimensionInfo` built around `WorldGenLevel`, `Holder<Biome>`, `ResourceKey<Level>` | `gui/NullDimensionInfo` | 1.7.10 uses `World`, `BiomeGenBase` and integer dimensions |
| 1.18+ biome keys `STONY_PEAKS`, `JAGGED_PEAKS` | `gui/NullDimensionInfo` | 1.7.10 has `extremeHills` and `extremeHillsEdge` instead |
| JSON lang files and world-preset `generator.*` keys | `assets/lostcities/lang/*` | 1.7.10 loads `.lang` files; keys must follow the `lostcitiesnh` namespace (CONTRIBUTING) |
| Java records (`ConfigUpdateResult`), `List.of`, switch expressions, `String.isBlank`, `Optional.ifPresentOrElse`/`isEmpty` | `gui/*` | Java 8 API forbids `List.of`, `isBlank`, `ifPresentOrElse`, `Optional.isEmpty` |

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| UI-01 | Customize crashes when the profile list was never built | `gui/LostCitySetup.java:95` | Reopening the screen with a remembered profile and pressing Customize first appends to a `null` list | crash |
| UI-02 | Customize duplicates list entries and replaces the global profile at once | `gui/LostCitySetup.java:94` | Each press appends another `customized` to the cycle list, and the global `customized` profile is replaced even if the screen is cancelled | gameplay |
| UI-03 | World-style cycling leaks resource managers and misses world packs | `gui/LostCitySetup.java:140` | Opens a resource manager over all client packs on every click and never closes it; lists only client-selected packs; throws when no world style is found | performance |
| UI-04 | Preview rebuilds a whole dimension every frame | `gui/GuiLCConfig.java:469` | A new `NullDimensionInfo` (street planner, highway planner with its service, terrain feature) per frame, twice on the Transport page (also line 343) | performance |
| UI-05 | Elements accumulate on every resize | `gui/GuiLCConfig.java:116` | `init` adds a fresh set of page elements to a list it never clears; stale elements keep drawing labels and the old All-page header and scrollbar at the old width, and keep writing values | visual |
| UI-06 | Preview biome lookup dereferences a null registry | `gui/NullDimensionInfo.java:292` | `biomeRegistry` is always `null`, so any preview path that asks for a biome throws | crash |
| UI-07 | Client cleanup hooked to a server-side event | `setup/ClientEventHandlers.java:75` | Never fires for a client on a dedicated server (state carries over between servers); on an integrated or LAN server fires whenever any player leaves, resetting `Config` and the profile cache while the world runs | gameplay |
| UI-08 | Damage preview uses a fixed ground and can draw outside the box | `gui/GuiLCConfig.java:425` | Heights are offset by a hard-coded 65; maximum heights are ignored; blots are not clipped to the preview box | cosmetic |
| UI-09 | Config-button image relies on texture wrapping | `setup/ClientEventHandlers.java:56` | UV offset (256, 256) on a 256-pixel texture samples one full texture away | cosmetic |
| UI-10 | Profile icons are dead assets with a wrong documented size | `src/main/resources/assets/lostcities/textures/gui/` | `getIcon` has no caller, three icons belong to commented-out profiles, and the `icon` option says 64 × 64 while the files are 128 × 128 | cosmetic |
| UI-11 | No localization; lang files are obsolete | `src/main/resources/assets/lostcities/lang/en_us.json:2` | All UI text is hard-coded English; the only JSON key names a removed world type, and the `.lang` files are not loaded by 1.20 | cosmetic |
| UI-12 | Stale custom JSON survives a switch to a standard profile | `gui/GuiLCConfig.java:552` | `selectProfile(name, null)` leaves `jsonFromClient` set, so a new world stores an unused `selectedCustomJson` | cosmetic |
| UI-13 | Boolean fields skip validation | `gui/elements/BooleanElement.java:28` | Toggles write the profile directly, bypassing the range and cross-option checks numeric fields get | cosmetic |
| UI-14 | Rejected numbers stay on screen | `gui/elements/IntElement.java:31` | The box keeps the rejected text (red) while the profile keeps the old value; same in `FloatElement` and `DoubleElement` | cosmetic |
| UI-15 | Refreshing fields writes values back into shared profiles | `gui/elements/IntElement.java:74` | `update` sets the box text, which fires the change handler and re-applies the whole configuration to the selected profile, including a shared standard profile that is not being edited (same in `FloatElement`, `DoubleElement`) | cosmetic |

<!-- Merged and deduplicated into BUGS.md. Suspected, not confirmed; no fixes designed here. -->
