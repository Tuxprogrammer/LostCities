# Changelog

All notable changes to Lost Cities: New Horizons. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions come from git tags.

## Unreleased

### Added

- The foundation (milestone M01): the mod loads on GTNH 2.9.0 with GTNHLib as its one hard dependency, and generates nothing yet.
- `config/lostcitiesnh.cfg` with one switch per feature area in the `worldgen` category (cities, highways, railways, scattered structures), all on by default and applying to new worlds only; editable from the Mods list through GTNHLib's config screen. See [docs/config.md](docs/config.md).
- The `lostcitiesnh` network channel, with no packets yet.
- `/lostcities test` (aliases `/lost`, `/lostcitiesnh`; subcommand alias `scenario`): the in-world scenario harness for development and real-pack testing, operator only and off in a normal game.
- English lang file for every message and config label.
