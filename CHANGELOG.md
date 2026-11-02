# Changelog

All notable changes to Lost Cities: New Horizons. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions come from git tags.

## Unreleased

### Added

- The foundation (milestone M01): the mod loads on GTNH 2.9.0 with GTNHLib as its one hard dependency, and generates nothing yet.
- `config/lostcitiesnh.cfg` with one switch per feature area in the `worldgen` category (cities, highways, railways, scattered structures), all on by default and applying to new worlds only; editable from the Mods list through GTNHLib's config screen. See [docs/config.md](docs/config.md).
- The `lostcitiesnh` network channel, with no packets yet.
- `/lostcities test` (aliases `/lost`, `/lostcitiesnh`; subcommand alias `scenario`): the in-world scenario harness for development and real-pack testing, operator only and off in a normal game.
- English lang file for every message and config label.
- The RWG overlay (milestone M04), not yet fed by the city layout: on RWG2 Continents, RWG2 and classic RWG worlds, the mod wraps the overworld's generator and, after RWG builds each chunk, levels city chunks to their ground, blends the land around them, writes their content, breaks it with explosions and debris, removes floating fragments and grows vines; after RWG populates a chunk it fixes torches and places chests and spawners. RWG's trees, boulders and ruins are kept off streets and buildings, and lakes, lava lakes, dungeons and oil deposits are kept out of cities. Other mods' landmarks are left untouched, met with soft slopes instead of walls, and reached by village lanes. A world pins its profile when it is created.
- The `profiles` config category: `dimensionProfiles` and `enableOnExistingWorlds`. See [docs/config.md](docs/config.md).
- `/lostcities profile` (and `profile pin <profile> confirm`) and `/lostcities timers`, operator only.
- The city layout (milestone M03), not yet wired into world generation: upstream Lost Cities' city detection, city styles, hierarchical streets and bridges, buildings and multibuildings, parks, stairs and corridors, with every upstream bug in that logic fixed; and the rules for building cities around other mods' structures: villages sewn in as a city's old town with lanes to their gates, plazas and road detours around landmarks, and buildings that step down toward an AE2 meteorite's crater.
