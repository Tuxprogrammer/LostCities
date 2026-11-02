# Profile selection state

`LostCitySetup`: the small state object behind the configuration screen. It holds which profile is chosen, the customized copy, and the list the profile button cycles through, and it implements profile cycling, customization and world-style cycling. A static instance, `CLIENT_SETUP`, remembers the last confirmed choice while the game runs.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `gui/LostCitySetup`; static `CLIENT_SETUP` (refresh action does nothing). Profile names `default` (preferred first) and `customized`. |
| 2 | **Inputs** | `ProfileSetup.STANDARD_PROFILES` and each profile's `isPublic` flag (`02-profiles-dimensions`); the client's selected resource packs (for world styles). |
| 3 | **Numbers** | See [Behavior detail](#behavior-detail). |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | Profile order: `default` first, then the other public profiles in alphabetical order. World-style order: the order of the resource listing (sorted by resource location in vanilla, inferred). |
| 6 | **Data formats** | Reads world-style files only by name: `data/<namespace>/lostcities/worldstyles/**.json` (`05-assets`). |
| 7 | **Output** | Selection state for the screen; the global `customized` profile replaced on **Customize**; a profile's world style changed. |
| 8 | **Relationships** | Used by `GuiLCConfig` ([profile-screen.md](profile-screen.md)) and the elements ([config-widgets.md](config-widgets.md)); reset by the logout handler ([world-creation-hook.md](world-creation-hook.md)). |
| 9 | **1.20 dependencies** | `PackRepository`, `MultiPackResourceManager`, `PackType.SERVER_DATA`, `listResources` (data packs read on the client); `Component` with `ChatFormatting`; `Optional`. |
| 10 | **Bugs** | `UI-01`, `UI-02`, `UI-03` |

## Behavior detail

State: `profiles` (list of names, or `null` until first needed), `profile` (name, or `null` meaning disabled), `customizedProfile` (or `null`).

1. (client) **`toggleProfile`:**
   1. if `profiles` is `null`, build it from the standard profiles whose `isPublic` is true, sorted with `default` first and the rest alphabetically;
   2. if no profile is chosen, choose the first; else move to the next; past the last, or if the current name is not in the list, go back to none (`Disabled`);
   3. run the refresh action.
2. (client) **`customize`** (the screen only enables it when a non-`customized` profile is chosen; otherwise it throws `Cannot happen!`):
   1. create a non-public profile named `customized`;
   2. put it into the global standard profiles under `customized`, replacing any earlier one;
   3. append `customized` to `profiles` (fails when the list was never built, for example after reopening the screen and pressing Customize first, `UI-01`; repeated presses append duplicates, `UI-02`);
   4. copy every option from the chosen profile into it; choose `customized`; refresh.
3. (client) **`toggleWorldStyle`:**
   1. open a resource manager over every resource pack selected on the client, reading the server-data side (`UI-03`: never closed; world data packs are not included);
   2. list `lostcities/worldstyles/**.json`; turn each into a name: the file name without folders and extension, prefixed with `namespace:` unless the namespace is `lostcities`;
   3. find the chosen profile's current world style in the list; move to the next one, wrapping; if absent, take the first (an empty list throws);
   4. set it on the chosen profile; refresh.
4. (client) **`get`**: none → empty; `customized` → the customized profile (may be empty); otherwise the standard profile of that name.
5. (client) Labels: profile label is the name or `Disabled`; world-style label is the profile's world style or `n.a.`; profile info is `<description>\n` + `<extra description>\n` in aqua + `<warning>` in red, or `Click here to select a profile for your Lost Cities` with no profile.
6. (client) **`copyFrom(other)`** copies the chosen name and the customized profile, not the list. **`reset`** clears all three.

## Deviations in the new mod

See [new-spec 09-client-ui § Profile configuration screen](../../new-spec/09-client-ui/index.md#profile-configuration-screen).
