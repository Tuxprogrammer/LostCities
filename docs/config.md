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

In the file, the keys sit in the `worldgen` category:

```
worldgen {
    B:cities=true
    B:highways=true
    B:railways=true
    B:scatteredStructures=true
}
```

## `profiles`: where cities generate

These keys too apply to **new worlds only**: the profile is resolved and pinned into the world when the world is created.

| Key | Default | What it does |
|---|---|---|
| `profiles.dimensionProfiles` | `0=default` | Which dimensions get cities and with which profile, one `<dimension id>=<profile>` per entry. The profile is pinned into the world's `data/lostcitiesnh.dat` when the world is created ([0005](design/0005-rwg-overlay.md) §7). Only world types with a height oracle get cities: `RWG2_CONTINENTS`, `RWG2`, `RWG` and `flat` |
| `profiles.enableOnExistingWorlds` | `false` | Give cities to worlds created before the mod was added, at their next load. Chunks generated before will not match the new ones at the border. `/lostcities profile pin <profile> confirm` opts in one world instead |

```
profiles {
    S:dimensionProfiles <
        0=default
     >
    B:enableOnExistingWorlds=false
}
```

Every profile value, the module flags and the world type are pinned into the world when it is created; `/lostcities profile` shows what a dimension was pinned with, and `/lostcities timers` prints the overlay's generation timers and budgets ([0005](design/0005-rwg-overlay.md) §8).

`ConfigDocsTest` keeps this table in step with the code: every option has a row here with its default.

## `decorativeMetals`: metal accents

One switch per decorative metal at or below EV (decision log D29). With a switch on, that metal's blocks, from every mod, may appear in buildings as small accents under a per-building cap: stripe bands, trims, crates and the odd panel, never whole walls, floors or roofs. With it off, each of those accents uses another block instead. Treasure and chest loot are loot and ignore the switches. Two places keep upstream's look while their metal is on: the cabin's copper walls and stairs, and the highway gas station's and restaurant parking's iron canopy columns, pumps and lamp posts.

These keys apply to **new worlds only**, like the `worldgen` keys: a world pins them when it is created. The config screen marks the category as needing a world restart.

| Key | Default | What it does |
|---|---|---|
| `decorativeMetals.iron` | `true` | Iron accents, and the gas station's iron canopy and pumps |
| `decorativeMetals.gold` | `true` | Gold accents |
| `decorativeMetals.castIron` | `true` | Cast iron accents |
| `decorativeMetals.copper` | `true` | Copper accents, and the cabin's copper walls and stairs |
| `decorativeMetals.tin` | `true` | Tin accents |
| `decorativeMetals.bronze` | `true` | Bronze accents |
| `decorativeMetals.lead` | `true` | Lead accents |
| `decorativeMetals.silver` | `true` | Silver accents |
| `decorativeMetals.nickel` | `true` | Nickel accents |
| `decorativeMetals.invar` | `true` | Invar accents |
| `decorativeMetals.electrum` | `true` | Electrum accents |
| `decorativeMetals.steel` | `true` | Steel accents |
| `decorativeMetals.blackSteel` | `true` | Black steel accents |
| `decorativeMetals.damascusSteel` | `true` | Damascus steel accents |
| `decorativeMetals.meteoricIron` | `true` | Meteoric iron accents |
| `decorativeMetals.electricalSteel` | `true` | Electrical steel accents |
| `decorativeMetals.darkSteel` | `true` | Dark steel accents |
| `decorativeMetals.aluminum` | `true` | Aluminum accents |
| `decorativeMetals.aluminumBrass` | `true` | Aluminum brass accents |
| `decorativeMetals.cobalt` | `true` | Cobalt accents |
| `decorativeMetals.stainlessSteel` | `true` | Stainless steel accents |
| `decorativeMetals.titanium` | `true` | Titanium accents |
| `decorativeMetals.desh` | `true` | Desh accents |
| `decorativeMetals.thaumium` | `true` | Thaumium accents |

## Development and testing switches

JVM properties, not config keys. They exist for development and real-pack testing and are off in a normal game.

| Property | Effect |
|---|---|
| `-Dlostcitiesnh.testHooks=true` | Turns on the scenario command and harness outside the development environment ([docs/testing/](testing/README.md)) |
| `-Dlostcitiesnh.selfCheck=true` or `false` | Runs the dev self-check at `postInit` outside the development environment, or skips it inside |
| `-Dlostcitiesnh.scenarios=<selector>` | Runs the selected scenarios once the server has started, where the test hooks are on |
| `-Dlostcitiesnh.stopAfterScenarios=true` | Stops the server after that run's summary line |
| `-Dlostcitiesnh.assetReport=true` | Logs the full asset report at `postInit`: every problem, every fallback block taken and every variety-pool block the registry lacks ([asset-format.md](new-spec/05-assets/asset-format.md#the-block-name-map)) |
| `-Dlostcitiesnh.planSource=synthetic` | With the test hooks on, pins the synthetic test city (M04) into new worlds instead of the layout, for the overlay scenarios and screenshots ([docs/testing/](testing/README.md)) |
