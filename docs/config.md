# Configuration

Lost Cities: New Horizons reads one file, `config/lostcitiesnh.cfg`, through GTNHLib's annotation config. Single player's integrated server reads the same file. In game, the **Config** button in the Mods list opens GTNHLib's automatic config screen for it; the mod has no config screen of its own ([09-client-ui](new-spec/09-client-ui/index.md), D31).

## `worldgen`: module flags

One switch per feature area. A switch turns that area's generation off; it never adds or removes blocks or items, because the mod registers none, so a client and a server with different settings can always join each other.

These keys apply to **new worlds only**. A world copies them into its pinned profile when it is created ([0005](design/0005-rwg-overlay.md) §7), and editing them later does not change a world that already exists. The config screen marks the category as needing a world restart.

| Key | Default | What it does |
|---|---|---|
| `worldgen.cities` | `true` | Generate cities: buildings, streets, bridges and their parks |
| `worldgen.highways` | `true` | Generate highways between and through cities |
| `worldgen.railways` | `true` | Generate railways, stations and subways |
| `worldgen.scatteredStructures` | `true` | Generate scattered structures outside cities |
| `worldgen.dimensionProfiles` | `0=default` | Which dimensions get cities and with which profile, one `<dimension id>=<profile>` per entry. The profile is pinned into the world's `data/lostcitiesnh.dat` when the world is created ([0005](design/0005-rwg-overlay.md) §7). Only world types with a height oracle get cities: `RWG2_CONTINENTS`, `RWG2`, `RWG` and `flat` |
| `worldgen.enableOnExistingWorlds` | `false` | Give cities to worlds created before the mod was added, at their next load. Chunks generated before will not match the new ones at the border. `/lostcities profile pin <profile> confirm` opts in one world instead |

In the file, the keys sit in the `worldgen` category:

```
worldgen {
    B:cities=true
    S:dimensionProfiles <
        0=default
     >
    B:enableOnExistingWorlds=false
    B:highways=true
    B:railways=true
    B:scatteredStructures=true
}
```

Every profile value, the module flags and the world type are pinned into the world when it is created; `/lostcities profile` shows what a dimension was pinned with, and `/lostcities timers` prints the overlay's generation timers and budgets ([0005](design/0005-rwg-overlay.md) §8).

`ConfigDocsTest` keeps this table in step with the code: every option has a row here with its default.

## Development and testing switches

JVM properties, not config keys. They exist for development and real-pack testing and are off in a normal game.

| Property | Effect |
|---|---|
| `-Dlostcitiesnh.testHooks=true` | Turns on the scenario command and harness outside the development environment ([docs/testing/](testing/README.md)) |
| `-Dlostcitiesnh.selfCheck=true` or `false` | Runs the dev self-check at `postInit` outside the development environment, or skips it inside |
| `-Dlostcitiesnh.scenarios=<selector>` | Runs the selected scenarios once the server has started, where the test hooks are on |
| `-Dlostcitiesnh.stopAfterScenarios=true` | Stops the server after that run's summary line |
| `-Dlostcitiesnh.planSource=synthetic` | With the test hooks on, pins the synthetic test city (M04) into new worlds instead of the layout, for the overlay scenarios and screenshots ([docs/testing/](testing/README.md)) |
