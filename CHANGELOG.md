# Changelog

All notable changes to Lost Cities: New Horizons. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions come from git tags.

## Unreleased

### Added

- The foundation (milestone M01): the mod loads on GTNH 2.9.0 with GTNHLib as its one hard dependency, and generates nothing yet.
- `config/lostcitiesnh.cfg` with one switch per feature area in the `worldgen` category (cities, highways, railways, scattered structures), all on by default and applying to new worlds only; editable from the Mods list through GTNHLib's config screen. See [docs/config.md](docs/config.md).
- The `lostcitiesnh` network channel, with no packets yet.
- `/lostcities test` (aliases `/lost`, `/lostcitiesnh`; subcommand alias `scenario`): the in-world scenario harness for development and real-pack testing, operator only and off in a normal game.
- English lang file for every message and config label.
- The asset system (milestone M02): upstream Lost Cities' 322 JSON assets (buildings, parts, palettes, styles, city and world styles, conditions, scattered structures and stuff) load at startup, validated and resolved to GTNH 2.9.0 blocks, with a vanilla fallback for every block when its mod is absent. Asset overrides go in `config/lostcitiesnh/assets/<namespace>/<type>/<name>.json` and replace the bundled file of the same name.
- Desert cities get rubble on their ruined buildings, from new sandstone rubble mixes.
- `decorativeMetals` config category: one switch per metal at or below EV for metal accents in buildings, all on by default, new worlds only.
- The RWG overlay (milestone M04): on RWG2 Continents, RWG2 and classic RWG worlds, the mod wraps the overworld's generator and, after RWG builds each chunk, levels city chunks to their ground, blends the land around them, writes their content, breaks it with explosions and debris, removes floating fragments and grows vines; after RWG populates a chunk it fixes torches and places chests and spawners. RWG's trees, boulders and ruins are kept off streets and buildings, and lakes, lava lakes, dungeons and oil deposits are kept out of cities. Other mods' landmarks are left untouched, met with soft slopes instead of walls, and reached by village lanes. A world pins its profile when it is created.
- The `profiles` config category: `dimensionProfiles` and `enableOnExistingWorlds`. See [docs/config.md](docs/config.md).
- `/lostcities profile` (and `profile pin <profile> confirm`) and `/lostcities timers`, operator only.
- Real cities (milestone M05, the World lane's half): on RWG2 Continents, RWG2 and classic RWG worlds, the city layout and the bundled assets now generate upstream Lost Cities' cities: buildings floor by floor with their cellars, doors, fillers and corridor openings; streets, plazas, parks, fountains, stairs, building fronts and rail corridors; bridges with their supports over water; ruined buildings and rubble on the streets. Chests hold a small set of low-tier loot until the GTNH loot tables come, spawners spawn upstream's mobs, and other mods' trees no longer overhang streets and roofs. A world also remembers the asset set and the decorative-metal switches it was created with, and warns at start when the assets have changed since.
- `/lostcities find` (test hooks only): where the nearest city center, primary street, building, city edge, soft-edge strip, reserved lot or sewn village is, with a viewpoint for `/lostcities view`; with `best` after the radius, the soft edge, reserved lot or village gate in that radius that shows the city and the landmark together most clearly, with a view that frames both.
- Cities plan their ground a few chunks ahead of world generation, a little each server tick, so a city reaching new land (a village's lanes, a block of large buildings) no longer stalls the generation of one chunk on classic RWG.
- A soft-edge slope beside a pond or river in a village meets the water at its waterline, as a shore.
- The city layout (milestone M03): upstream Lost Cities' city detection, city styles, hierarchical streets and bridges, buildings and multibuildings, parks, stairs and corridors, with every upstream bug in that logic fixed; and the rules for building cities around other mods' structures: villages sewn in as a city's old town with lanes to their gates, plazas and road detours around landmarks, and buildings that step down toward an AE2 meteorite's crater.
- VillageNames structure oracles (milestone M05): where VillageNames villages and ocean monuments will generate, predicted from the seed, VillageNames' config and the biome provider without generating anything, with each village's full plan, its gates and its ground height.
- For development only, off in a normal game: a clean screenshot mode for the client (`-Dlostcitiesnh.capture=true` with the test hooks), which hides the GUI, hand, chat, tutorial toast and HUD overlays and reports when a viewpoint has loaded, and a shot-list runner that takes review screenshots on the real pack (`tools/remote/lc-remote.sh shots`).
- With VillageNames installed, a seed now builds the same villages on every server run: the order in which Forge asks mods for village buildings is fixed, where before it changed from run to run.
- Profiles (milestone M09): ten built-in profiles (`default`, `nodamage`, `rarecities`, `onlycities`, `tallbuildings`, `safe`, `ancient`, `wasteland`, `atlantis`, `largecities`) and your own in `config/lostcitiesnh/profiles/`, in upstream Lost Cities' JSON format, so upstream profile files load. Every option is checked when a profile is read: numbers out of range are clamped, and a broken file is rejected with one log line without stopping the game. Each world keeps every option it was created with. Choose a world's profile with `dimensionProfiles` (on a server, or from the Mods list's config screen before creating a world in single player). See [docs/config.md](docs/config.md#profiles).
- `/lostcities profile list`, `profile info <profile>`, `profile reload` and `/lostcities saveprofile <profile> <newName>`, operator only.
- Spawn options: a profile can ask for a spawn in a biome, in a predefined city, never in a building, or inside a building (placing each player exactly on their first join); the spawn is found from the city layout without generating chunks, and the bonus chest still comes when asked for.
- Parks are filled in with tall grass, flowers, ferns and small shrubs suited to their biome, instead of bare grass with a few flowers.
- The pack's variety in cities (milestone M11): buildings draw their walls, trims, bands, glass and quartz from GTNH's decorative blocks (Chisel, Railcraft, Et Futurum, Botania, Ztones, Tinkers', Thaumcraft, GregTech stones and more), each building its own mix within its upstream color family, and about two in five keep upstream's walls. Desert cities stay pale and muted. Interiors get wood, tile and stone floors, office ceilings, ceiling lights in nearly half the buildings, kitchens, workshops and studies, and varied bookshelves and fences; parks and planters get flowers from Et Futurum, Biomes O' Plenty and Botania. Without one of those mods, its blocks give way to upstream's. `worldgen.paletteVariety` switches it off for new worlds; see [docs/config.md](docs/config.md).

### Changed

- Explosion craters and ruined buildings are much rarer and craters smaller by default: damage is an occasional accent, not the look of a city. A profile can still ask for upstream Lost Cities' amounts.
- Desert cities use a muted palette: sandstone, white and light gray terracotta and smooth sandstone, without red or orange buildings, floors or park borders.

### Fixed

- A street stair no longer loses one of its steps when an explosion lands nearby: the cleanup after explosions only removes loose blocks that the explosion could have reached.
