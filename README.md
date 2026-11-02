# Lost Cities: New Horizons

Abandoned cities, highways and railways for the **GT New Horizons** overworld (Minecraft 1.7.10). This is a backport of McJty's [Lost Cities](https://github.com/McJtyMods/LostCities), built for GTNH 2.9.0 and its Realistic World Gen terrain.

Explore cities that were here before you, salvage what they left behind, or move into a block of buildings and build your base inside it.

> [!NOTE]
> The foundation is in place (milestone M01): the mod loads in GTNH 2.9.0, but it generates nothing yet. See the [roadmap](docs/ROADMAP.md).

## What it is

- **Lost Cities, faithfully.** Upstream's city layouts, buildings, streets, highways and railways, carried over to 1.7.10.
- **At home in GTNH.** Cities generate over the pack's RWG overworld and are built from the pack's blocks. Their loot comes from the pack and stays at EV or below, and GT ore veins are left alone.

## Building from source

```sh
./gradlew build        # the jar is in build/libs/
./gradlew runClient    # development client
```

Contributors start with [CONTRIBUTING.md](CONTRIBUTING.md). The design records are in [docs/design/](docs/design/README.md), and the behavior spec in [docs/original-spec/](docs/original-spec/README.md) and [docs/new-spec/](docs/new-spec/README.md).

## License

MIT; see [LICENSE](LICENSE). Lost Cities is © 2017 McJty, under the MIT License; see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
