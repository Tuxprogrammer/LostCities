# 09 · Client UI

What of upstream's client side survives in Lost Cities: New Horizons, set against upstream's [09-client-ui](../../original-spec/09-client-ui/index.md). Upstream's client UI exists for one purpose: choosing and customizing a profile on the Create World screen and handing it to the integrated server. This mod has no world type of its own and pins the profile from server configuration ([0005](../../design/0005-rwg-overlay.md) §7), so **the client needs nothing to generate cities**, in single player or multiplayer. What survives is small:

- **lang files**, because every player-visible string, including command output sent by the server, is a lang key;
- **GTNHLib's config screen** for the mod config, from the Mods list, which GTNHLib builds on its own (P-UI-1, D31);
- a **Create World profile picker**, deferred until after beta: [0005](../../design/0005-rwg-overlay.md) §7 makes it "a later client-UI item, not alpha", and D31 sets its scope (P-UI-2).

The profile editor, its pages and its live preview are deferred with the picker. Server owners and single players tune a world by copying a built-in profile to `config/lostcitiesnh/profiles/`, editing the JSON and naming it in `dimensionProfiles` before creating the world ([0005](../../design/0005-rwg-overlay.md) §7 "Tuning").

Status: rows that cite a design record or a decision-log row are *Accepted*. The live rows (lang keys, the config screen and the single-player profile choice it carries) are built and checked: `LangKeysTest` over the code, the `commands.permissions` scenario over every command's output, and a client run that opens the config screen from the Mods list (M10, [docs/testing/](../../testing/README.md)). The *deferred* rows stay deferred until after beta. The former proposals P-UI-1 (settled by research) and P-UI-2 were accepted by decision log D31 and are recorded in their rows; nothing is open ([Proposed, not decided](#proposed-not-decided)). Whatever client code is written lives under `lostcities.client` and is never referenced from common code ([CONTRIBUTING § Architecture](../../../CONTRIBUTING.md#architecture), `networking-side-safety`).

## Contents

No sheets: every row fits the table.

## How to read the deviation table

The columns are as in [01-core](../01-core/index.md#how-to-read-the-deviation-table). A *deferred* row says what a later build must do and which upstream bugs it must avoid, so the bug IDs stay covered even though nothing is built before beta.

## Deviations

### World-creation hook and client events

Upstream: [world-creation-hook.md](../../original-spec/09-client-ui/world-creation-hook.md).

| Original | Here | What changes | Decision | Avoids |
|---|---|---|---|---|
| [**Cities** button and image on the Create World screen](../../original-spec/09-client-ui/world-creation-hook.md#behavior-detail) (More tab, access transformer on `CreateWorldScreen`) | deferred | 1.7.10's `GuiCreateWorld` has no tabs. The later build adds its button in Forge's `GuiScreenEvent.InitGuiEvent.Post` and handles it in `ActionPerformedEvent` ([GuiScreenEvent.java#L70](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/src/main/java/net/minecraftforge/client/event/GuiScreenEvent.java#L70), [#L131](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/src/main/java/net/minecraftforge/client/event/GuiScreenEvent.java#L131), *verified* against the pack's 10.13.4.1614 jar), shown only when the selected world type has a height oracle. No access transformer. The button's image is new art, not a screenshot ([0001](../../design/0001-port-policy-and-licensing.md) §5) and not sampled through texture wrapping. The picker hands its choice to the integrated server as a one-shot value the pin step reads and clears in `WorldEvent.CreateSpawnPosition`, through the client proxy, so the client writes nothing into common state and a dedicated server never loads the class (P-UI-2). | [0005](../../design/0005-rwg-overlay.md) §7, D31 | UI-09 |
| [Logout cleanup](../../original-spec/09-client-ui/world-creation-hook.md#behavior-detail) (`PlayerLoggedOutEvent` resets the client choice, `Config` and the dirty counter) | dropped | No client state to reset: there is no profile handoff, profile cache or dirty counter ([01-core](../01-core/index.md#lifecycle-and-registration)). If the deferred picker keeps state, it resets it on 1.7.10's client-side `FMLNetworkEvent.ClientDisconnectionFromServerEvent` ([FMLNetworkEvent.java#L73](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/fml/src/main/java/cpw/mods/fml/common/network/FMLNetworkEvent.java#L73)), never on a server-side logout event, and never touches server state. | [0005](../../design/0005-rwg-overlay.md) §7 | UI-07 |
| [Fog handlers](../../original-spec/09-client-ui/world-creation-hook.md#behavior-detail) (commented out upstream; live on 1.12) | dropped | The `client` profile options are dropped; a horizon needs a world type of its own. | D12, [0005](../../design/0005-rwg-overlay.md) §7 | PROF-17 |

### Profile configuration screen

Upstream: [profile-screen.md](../../original-spec/09-client-ui/profile-screen.md), [profile-selection-state.md](../../original-spec/09-client-ui/profile-selection-state.md), [config-widgets.md](../../original-spec/09-client-ui/config-widgets.md).

| Original | Here | What changes | Decision | Avoids |
|---|---|---|---|---|
| [`GuiLCConfig`](../../original-spec/09-client-ui/profile-screen.md) (profile picker, Customize, five themed pages, All page, Done and Cancel) | deferred | Deferred with the Create World button until after beta. Its scope: single player only, picking among the loaded profiles for the world about to be created, and nothing else; no option editor or preview, because JSON files and `saveprofile` cover tuning, and the editor and preview are where most of upstream's UI bugs are (P-UI-2). A later build of any editor must: build its widgets once per `initGui` from a cleared list; never write a standard profile; apply edits only on Done; and validate every field, booleans included, through the same load-time checks profiles get ([02-profiles-dimensions](../02-profiles-dimensions/index.md#profile-system)). | [0005](../../design/0005-rwg-overlay.md) §7, D31 | UI-05, UI-12, UI-13, UI-14, UI-15 |
| [`LostCitySetup`](../../original-spec/09-client-ui/profile-selection-state.md) (profile cycling, Customize, world-style cycling) | deferred | With the picker. Cycling reads the profile list and the world styles the asset loader already has (D14); there are no data packs to list on 1.7.10, and no global `customized` profile to replace. | D14, [0005](../../design/0005-rwg-overlay.md) §7 | UI-01, UI-02, UI-03 |
| [Field widgets and the All page](../../original-spec/09-client-ui/config-widgets.md) | deferred | 1.7.10 has `GuiScreen`, `GuiButton` and `GuiTextField` with hand-drawn tooltips ([API-MAP](../../original-spec/API-MAP.md#client-and-gui)). Not built before beta. | [0005](../../design/0005-rwg-overlay.md) §7 | UI-13, UI-14, UI-15 |
| `customized` profile and `jsonFromClient` | dropped | No profile is customized in a GUI. | [0005](../../design/0005-rwg-overlay.md) §7 | UI-02, UI-12 |
| [API `setOverworldProfile`](../../original-spec/09-client-ui/profile-screen.md#behavior-detail) (common code calling `GuiLCConfig.selectProfile`) | dropped | [01-core](../01-core/index.md#api). | [0005](../../design/0005-rwg-overlay.md) §7 | CORE-01 |

### Preview

Upstream: [preview.md](../../original-spec/09-client-ui/preview.md).

| Original | Here | What changes | Decision | Avoids |
|---|---|---|---|---|
| [`NullDimensionInfo` and the city, transport and side-view previews](../../original-spec/09-client-ui/preview.md) | deferred | Not built before beta. The layout package is pure Java (D6), so a later preview can run it against a fake height oracle and fake structure oracle without a world. It must build its preview context once per change, not per frame; never ask a null biome registry; use 1.7.10 biomes (`extremeHills` and `extremeHillsEdge` for the 1.18+ peaks) in its fake landscape; clip its damage overlay to its box and measure heights from the profile's ground level; and keep its caches apart from the world's. | D6, [0005](../../design/0005-rwg-overlay.md) §7 | UI-04, UI-06, UI-08 |

### Lang and textures

Upstream: [lang-and-textures.md](../../original-spec/09-client-ui/lang-and-textures.md).

| Original | Here | What changes | Decision | Avoids |
|---|---|---|---|---|
| [Lang keys](../../original-spec/09-client-ui/lang-and-textures.md#lang-keys) (`generator.lc_cavern`, `generator.lostcities`, `generator.lostcities_bop` and five legacy `.lang` files) | dropped | All three name world types that no longer exist upstream, and this mod adds none. | [0003](../../design/0003-world-presence.md), [0005](../../design/0005-rwg-overlay.md) | UI-11 |
| [Hard-coded client text](../../original-spec/09-client-ui/lang-and-textures.md#hard-coded-client-text) and every server message | changed | `assets/lostcitiesnh/lang/en_US.lang` is the source of truth, with keys `lostcitiesnh.<area>.<key>`: command output (`lostcitiesnh.command.*`), config-screen labels and tooltips (`lostcitiesnh.<category>.<field>`, GTNHLib's default lang-key pattern), built-in profile texts (`lostcitiesnh.profile.*`) and any later GUI text. Server messages are sent as `ChatComponentTranslation`, so the client resolves them, which is why the mod is required on both sides ([0005](../../design/0005-rwg-overlay.md#7-profile-selection-q7) §7). Vanilla's own messages for a refused, malformed or out-of-range command (`commands.generic.*`) stay vanilla's. `LangKeysTest` enforces it over the code, and the `commands.permissions` scenario over what every subcommand sends. Translations are welcome as further `.lang` files. | [CONTRIBUTING § Assets and localization](../../../CONTRIBUTING.md#assets-and-localization), [§ Testing](../../../CONTRIBUTING.md#testing) | UI-11 |
| [Textures](../../original-spec/09-client-ui/lang-and-textures.md#textures) (`configicon.png` and 17 profile icons) | dropped | All 18 are in-game screenshots showing Minecraft's textures; the profile icons were never displayed. Any image a later picker needs is made anew. | [0001](../../design/0001-port-policy-and-licensing.md) §5 | UI-09, UI-10 |

### What is new on the client

| Original | Here | What changes | Decision | Avoids |
|---|---|---|---|---|
| (none) Mod config screen | new | Registering the config with GTNHLib's `ConfigurationManager.registerConfig` ([ConfigurationManager.java#L66](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.54/src/main/java/com/gtnewhorizon/gtnhlib/config/ConfigurationManager.java#L66)) is all it takes: GTNHLib's client mixin on FML's `GuiModList` enables the **Config** button for any registered mod and opens a `SimpleGuiConfig` built from the annotated config when the mod has no `guiFactory` ([MixinGuiModList.java#L31-L62](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.54/src/main/java/com/gtnewhorizon/gtnhlib/mixins/early/fml/MixinGuiModList.java#L31-L62), applied on the client as `AUTO_CONFIG_GUI`, [Mixins.java#L16](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.54/src/main/java/com/gtnewhorizon/gtnhlib/mixins/Mixins.java#L16)). So the mod writes no `guiFactory` and no GUI code. Nothing is hidden. Worldgen keys sit in their own category marked `@Config.RequiresWorldRestart` ([Config.java#L205-L207](https://github.com/GTNewHorizons/GTNHLib/blob/0.11.54/src/main/java/com/gtnewhorizon/gtnhlib/config/Config.java#L205-L207)), and their comments say they apply to new worlds only, since they are pinned at world creation (P-UI-1). The `profiles` category is how single player picks a world's profile before creating it (D141). Checked in the real-pack client by `ConfigScreenProbe` (`-Dlostcitiesnh.configScreenProbe=true`): from the Mods list, Config opens GTNHLib's screen with the `worldgen`, `profiles` and `decorativeMetals` categories, and every label and tooltip translates | D31, D141 | |
| (none) Client rendering | none | Cities are ordinary pack blocks; nothing is rendered by this mod, so `client-rendering-shaders` has nothing to apply to. | D19, D28 | |

## Bug register coverage

Every `UI` ID in [BUGS.md](../../original-spec/BUGS.md) appears in a row above: UI-01 to UI-15. Most sit in *deferred* rows: nothing is built before beta, and the rows state what a later build must do to avoid them.

## Proposed, not decided

None. P-UI-1 and P-UI-2 were settled by decision log D31; each is recorded in its row above.
