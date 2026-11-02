# Changelog

All notable changes to Lost Cities: New Horizons. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions come from git tags.

## Unreleased

### Added

- The foundation (milestone M01): the mod loads on GTNH 2.9.0 with GTNHLib as its one hard dependency, and generates nothing yet.
- `config/lostcitiesnh.cfg` with one switch per feature area in the `worldgen` category (cities, highways, railways, scattered structures), all on by default and applying to new worlds only; editable from the Mods list through GTNHLib's config screen. See [docs/config.md](docs/config.md).
- The `lostcitiesnh` network channel, with no packets yet.
- `/lostcities test` (aliases `/lost`, `/lostcitiesnh`; subcommand alias `scenario`): the in-world scenario harness for development and real-pack testing, operator only and off in a normal game.
- English lang file for every message and config label.
- The asset system (milestone M02): upstream Lost Cities' 322 JSON assets (buildings, parts, palettes, styles, city and world styles, conditions, scattered structures and stuff) load at startup, validated and resolved to GTNH 2.9.0 blocks, with a vanilla fallback for every block when its mod is absent. Asset overrides go in `config/lostcitiesnh/assets/<namespace>/<type>/<name>.json` and replace the bundled file of the same name. Nothing generates from them yet.
- Desert cities get rubble on their ruined buildings, from new sandstone rubble mixes.
- `decorativeMetals` config category: one switch per metal at or below EV for metal accents in buildings, all on by default, new worlds only.
- The city layout (milestone M03), not yet wired into world generation: upstream Lost Cities' city detection, city styles, hierarchical streets and bridges, buildings and multibuildings, parks, stairs and corridors, with every upstream bug in that logic fixed; and the rules for building cities around other mods' structures: villages sewn in as a city's old town with lanes to their gates, plazas and road detours around landmarks, and buildings that step down toward an AE2 meteorite's crater.
