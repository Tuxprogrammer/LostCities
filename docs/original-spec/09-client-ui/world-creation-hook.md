# World-creation hook and client events

How the configuration screen is reached: a **Cities** button and a picture added to the vanilla Create World screen's More tab. Also the client-side cleanup on logout, and the fog handlers that exist only as comments.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `setup/ClientSetup` (registers the handler on client setup). `setup/ClientEventHandlers` (`onGuiPost`, `onGuiDraw`, `onPlayerLoggedOut`; commented-out `onFogEvent`, `onFogDensity`). Texture `lostcities:textures/gui/configicon.png`. |
| 2 | **Inputs** | Screen init and render events for `CreateWorldScreen`; the screen's tab manager (made accessible by the access transformer, `01-core/lifecycle.md`); the player-logout event. |
| 3 | **Numbers** | See [Numbers](#numbers). |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | Not world generation. The logout handler resets shared statics that world generation reads (`Config` profile cache and client choice, the dirty counter). |
| 6 | **Data formats** | None. |
| 7 | **Output** | A button and an image on the Create World screen; opening [profile-screen.md](profile-screen.md); reset client state on logout. |
| 8 | **Relationships** | Opens `GuiLCConfig` ([profile-screen.md](profile-screen.md)). Calls `LostCitySetup.CLIENT_SETUP.reset()` ([profile-selection-state.md](profile-selection-state.md)), `Config.reset()` (`01-core/config.md`), and bumps `LostCityFeature.globalDimensionInfoDirtyCounter` (`01-core/lifecycle.md`). |
| 9 | **1.20 dependencies** | `ScreenEvent.Init.Post` and `ScreenEvent.Render`; `CreateWorldScreen` with a tab manager and `MoreTab`; `Button.builder`; `GuiGraphics.blit` with UV and texture size; `PlayerEvent.PlayerLoggedOutEvent` (server-side in 1.20); `FMLClientSetupEvent`. |
| 10 | **Bugs** | `UI-07`, `UI-09` |

## Numbers

| Item | Value |
|---|---|
| Button | label `Cities`, at (screen width − 100, 40), 70 × 20 px |
| Image | `configicon.png` (256 × 256 px), drawn at (screen width − 100, 60), 70 × 70 px; UV offset (256, 256), region 256 × 256, texture size 256 × 256 |
| Visibility | only while the More tab is the active tab |

## Behavior detail

1. (client) On client setup, register one `ClientEventHandlers` instance on the Forge bus.
2. (client) After any `CreateWorldScreen` initializes (including re-initialization on resize and on return from the configuration screen), build the **Cities** button, hide it, add it to the screen and remember it in a field. Pressing it opens the configuration screen with the Create World screen as parent.
3. (client) On every render of a `CreateWorldScreen`, if a button was remembered, show it exactly when the active tab is the More tab; while shown, draw `configicon.png` scaled into a 70 × 70 square under the button. The UV offset of 256 on a 256-pixel texture starts the sample one full texture width in, which shows the whole image only because the texture wraps (`UI-09`).
4. (client, but see below) On `PlayerLoggedOutEvent`: `LostCitySetup.CLIENT_SETUP.reset()` (forget the chosen profile, the customized profile and the profile list), `Config.reset()` (forget the client choice and the profile cache), and bump the dirty counter. The comment says "to clean up client-side and single player". In 1.20 this event fires on the logical server when a player disconnects: in single player it runs on the integrated server thread when the host leaves, and when any LAN guest leaves; on a client connected to a dedicated server it never fires (`UI-07`).
5. (client) Fog handlers that set fog color and density from the profile's `FOG_RED`, `FOG_GREEN`, `FOG_BLUE` and `FOG_DENSITY` are commented out; profile fog has no effect at the pin.

## On 1.12

`upstream/1.12` reaches the configuration screen through the world type's **Customize** button (`LostWorldType.onCustomizeButton` opens `GuiLostCityConfiguration` with the `GuiCreateWorld` parent), which is how 1.7.10 world types customize too. Its `ClientEventHandlers` applies profile fog color and density in `EntityViewRenderEvent.FogColors` and `FogDensity` for Lost Cities worlds.

## Deviations in the new mod

Not decided here; see `docs/new-spec/` (area 09-client-ui). [0003](../../design/0003-world-presence.md) makes cities an overlay on RWG rather than a world type, which changes where this entry point can live.
