# Lost Cities behavior spec

A description of **Lost Cities 1.20-7.5.7 for Minecraft 1.20.1** (upstream branch `1.20`, commit `f18a094d`, 2026-10-04): how it behaves, every number and tunable, its asset formats, and its suspected bugs. Lost Cities is MIT, so code may be ported ([0001](../design/0001-port-policy-and-licensing.md)). This spec is not a clean-room substitute for the code. It is the **behavior contract** that unit tests and the ported-code gate check against, and the register of upstream bugs that must not come back. How this mod differs from upstream, and how it fits GTNH, is in [docs/new-spec/](../new-spec/README.md).

Status: **first complete pass done (2026-11-01).** All nine areas are written: 77 sheets and catalogs, covering 596 of the 596 in-scope upstream files ([COVERAGE](COVERAGE.md)), and 203 unique suspected bugs ([BUGS](BUGS.md)). Roadmap spike S1 ([ROADMAP](../ROADMAP.md)).

| Area | Covers | Bug prefix |
|---|---|---|
| `01-core` | Mod lifecycle and registration, the mod config, commands, network, player data (spawn selection), the public API, the in-game part editor, events, shared utilities (`varia`) | `CORE` |
| `02-profiles-dimensions` | `LostCityProfile` and every option, profile selection and inheritance, world styles, landscape types (normal, floating, space, cavern), biome selection, dimension setup | `PROF` |
| `03-terrain` | The terrain feature and chunk driver, heightmaps and noise, city spheres, explosions and damage, chunk fixing, how city terrain blends into vanilla terrain | `TERR` |
| `04-city-layout` | City detection and rarity, city styles in the world, building and multibuilding placement, the street and bridge planners, chunk plans, corridors, doors, supports, parks and fountains | `CITY` |
| `05-assets` | The JSON asset system: every asset type and field (parts, buildings, multibuildings, palettes, variants, styles, city styles, conditions, predefined cities and spheres, stuff, world styles), how assets load, compile and override, and a catalog of the bundled assets | `ASSET` |
| `06-highways-railways` | Highways and the intercity highway planner, railways, stations, subways and monorails | `HWY` |
| `07-scattered` | Scattered structures, structure avoidance, decorative "stuff" (chains, cobwebs) | `SCAT` |
| `08-loot-mobs-blocks` | Chest loot, spawners and mob conditions, block tags, any blocks or items the mod registers | `LOOT` |
| `09-client-ui` | The world-creation and profile GUI, client events and client-only settings | `UI` |

Reference files, created with the areas:

| File | What it holds |
|---|---|
| [CONVENTIONS.md](CONVENTIONS.md) | The standard every area meets |
| [BUGS.md](BUGS.md) | Every suspected upstream bug, merged across areas |
| [COVERAGE.md](COVERAGE.md) | Every upstream Java file and asset type, and the sheet that covers it |
| [API-MAP.md](API-MAP.md) | Each 1.20 API upstream relies on and its 1.7.10 counterpart, or "none" |

## Sources

- The upstream code and assets at `f18a094d`: `git show 1.20:<path>`, or `semantic_search` with `source: upstream` ([tools/semantic-search/](../../tools/semantic-search/README.md)).
- McJty's documentation: [asset structure](https://mcjty.eu/docs/mods/lost-cities/asset_structure), [city generation](https://mcjty.eu/docs/mods/lost-cities/city_generation), [profile options](https://mcjty.eu/docs/mods/lost-cities/profile_options) and [world-generation thread safety](https://mcjty.eu/docs/mods/lost-cities/thread_safety). Where these disagree with the code, the code at the pin wins, and the sheet notes the difference.
- The upstream notes on the branch (`HIGHWAY_MAJOR_CITY_PLAN.md`, `plan_performance.md`, `profile_plan.md`, `todo.txt`, `changelog.txt`) describe intent and plans. They are not behavior; a sheet cites them only to explain the code.
