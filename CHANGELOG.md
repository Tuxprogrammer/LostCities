# Changelog

All notable changes to Lost Cities: New Horizons. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions come from git tags.

## 0.2.1-beta (2026-11-06)

### Fixed

- Single-player crash (`ArrayIndexOutOfBoundsException` in `FootprintGuard.drops`) when the client applied a block update while the integrated server was populating a chunk. The worldgen guard now never runs for the client's world.

## 0.2.0-beta (2026-11-06)

City realism. New worlds only for the new layout; existing worlds keep their generation.

### Added

- **Taller, denser cities:** floors stretch to an 8-block pitch for roomier interiors, and building density now falls off in rings from the city center.
- **Real lot sizes:** construction lots, foundations and plazas take multi-chunk footprints instead of one chunk, and cities get districts: a central plaza, industrial yards and a park belt.
- **Pack houses:** the village houses of other pack mods (Forestry bee houses among them) are built on lots at the city edge, alongside five original houses and low-rises.
- **Furnished buildings:** rooms get palettes by building type, loot and spawner rules per room, and sparse set dressing, including GregTech machines and partial multiblocks in industrial rooms.
- **A city link network:** rarecities centers sit on a jittered grid and each city links to its three nearest neighbors. Highways and railways follow the links, with an optional beltway ring, interchanges and cut-throughs (`cityHighwayLayout`).
- **`/lostcities find`** locates `gtroom`, `furnished`, `packhouse`, `beltway` and `district` targets, for checking the new content in a world.
- **Smoother roads and rails:** ramps have a smooth surface, railways end at buffer stops where a landmark gaps the track, and a highway stays flat where another route turns off it.

### Changed

- **Existing worlds keep their old generation.** Every new option declares a legacy value that worlds pinned before it applies, so new chunks match old ones and no seams appear. To get the new layout, start a new world, or run `/lostcities profile pin <profile> confirm` on the existing one (chunks already generated will not match).
- Door steps stay open past station walls and on lower roofs, and roof doors need a roof at their floor.
- Unkept part chests are cleared under the room rules, and pack houses build deterministically.
- World generation cost with all of this on is measured and within budget: about 11.5 % over RWG's own time on RWG2 Continents and about 8 % on classic RWG; see the README known limits.

### Fixed

- Clearing a chest the room rules did not keep no longer loads the neighboring chunk during population (cascading worldgen).
- Boot crashes: a farmland border no longer redefines city-style palette characters in `nh_residential` (StackOverflow at boot), and world-reading GregTech machines are terrain stand-ins swapped back at population (skylight StackOverflow).
- Blocked street doors: snow from RWG's snow pass and tree crowns from the park beside a building no longer land in the two blocks in front of a door. Doors were 94 % open on RWG2 Continents seed 1 and 6 % open on a snowy classic RWG seed; both are 100 % now.
- Other mods' trees beside a cabin or building, and GregTech's stone boulders, no longer write into the footprint. The boulder generator runs after every other generator, so a late mixin on it (when GregTech is loaded) now brackets it with the footprint guard.

### Known issues

- The link network, beltways and districts need grid-placed cities, so they appear in `rarecities` worlds; the default profile scatters cities and keeps the older highway and railway layout.
- Partial GT multiblocks have no hatches. The Forestry bee house is built without its flowers.
- A GT machine in a room whose furnishing never ran (a chunk saved mid-generation) stays a silverfish-stone stand-in.

## 0.1.1-beta (2026-11-05)

City shape and ocean fixes; see `.changelogs/0.1.1-beta.md`.

## 0.1.0-beta (2026-11-02)

The first build for playtesting: the whole mod, unpolished, for GT New Horizons 2.9.0. Install the same jar on the server and every client, and use a new world ([README](README.md#installing)).

### Added

- **Cities** in the overworld on RWG2 Continents, RWG2, classic RWG and flat worlds: upstream Lost Cities' buildings floor by floor with cellars, doors, fillers and stairwells; streets, plazas, parks, fountains, stairs, building fronts and rail corridors; bridges with supports over water; ruined buildings, explosion craters, debris and rubble; cobwebs and chains hanging in buildings. All 322 of upstream's asset files are bundled.
- **Cities set into RWG terrain:** a city levels its ground, blends into the land around it, and stops or steps down before high ground instead of climbing it. RWG's trees, boulders and ruins, and other mods' trees and cacti, stay off streets and buildings; lakes, lava lakes, dungeons, NHCore's oil and Railcraft's quarried stone keep out of cities.
- **City edges and character:** building heights taper from a high-rise core to low buildings at the edge, by district rather than in rings; empty lots, excavations dug 1, 2 or 4 blocks and unfinished concrete foundations appear where a city thins out; some explosion damage was partly repaired with a similar material, and broken windows boarded up.
- **The pack's variety:** walls, trims, bands, glass and quartz from GTNH's decorative blocks (Chisel, Railcraft, Et Futurum, Botania, Ztones, Tinkers', Thaumcraft, GregTech stones and more), each building its own mix within its upstream color family, about two in five keeping upstream's look; interiors with wood, tile and stone floors, office ceilings, ceiling lights, kitchens, workshops and studies; flowers and shrubs from Et Futurum, Biomes O' Plenty and Botania in parks. Neighboring buildings' colors stay within a contrast limit, also where a desert city meets a standard one. Desert cities are pale and muted.
- **GTNH loot and mobs:** city chests draw from six categories, `lostcitiesnh:city_supplies`, `city_lv`, `city_mv`, `city_hv`, `city_ev` and `rail_dungeon`: GT components, circuits and materials, the pack's loot bags, food and supplies, Railcraft gear for rail dungeons; nothing above EV, and EV rarely, on high floors and in deep cellars. Server owners retune a category with a TooMuchLoot file of the same name. Spawners use upstream's mobs; a mob missing from the pack becomes a skeleton or no spawner, never a pig spawner. GT ore veins pass through cities.
- **Building around other mods:** VillageNames villages are sewn into cities as an old town, with lanes to their gates; cities leave room for ocean monuments, Roguelike Dungeons towers (which no longer land inside cities), Witchery's structures, LootGames dungeons, AE2 meteorites (stepping down so a crater cuts nothing) and RWG2's volcanoes, lava caves and river junctions, meeting them with soft slopes instead of walls.
- **Scattered structures:** cabins and radio towers in the countryside between cities, oil rigs in RWG2's deep ocean, each set level into the land with slopes back to the terrain around it.
- **Highways and railways:** on RWG2 Continents, RWG2 and classic RWG worlds, upstream Lost Cities' intercity highways now link cities, as tunnels through hills, open roads in cities and bridges with supports over valleys, with bends, T-junctions and crossings. A highway leaves a city at its street level and reaches the next one the same way, by ramps you can walk up without jumping, and it goes around villages and other mods' landmarks, keeping out of the green margin a city leaves around them, or ends before one on a plaza. Lakes, lava and dungeons stay out of highways, Biomes O' Plenty's poison and other lakes stay out of tunnels and railway lines, and trees stay off their decks. Gas stations and restaurants stand beside highways. The railway grid runs under the land, with stations at the surface or underground (with a staircase up) where it meets a city, three-way splits, bends and rail dungeons; a railway dips under a landmark that lies too low above it and climbs back after it.
- **Profiles:** ten built-in profiles (`default`, `nodamage`, `rarecities`, `onlycities`, `tallbuildings`, `safe`, `ancient`, `wasteland`, `atlantis`, `largecities`) and your own in `config/lostcitiesnh/profiles/`, in upstream's JSON format; every value is checked on load, and a broken file is rejected with one log line. A world keeps every option it was created with. Profile options of this port: `preventLakesInCities`, `villageTreatment`, `landmarkTreatment`, `landmarkMargin`, `softEdgeWidth`, `cityMaxRise`, `cityTaper`, `edgeLotChance`, `damageRepairChance`, `facadeContrast`, `villageTransitionRings`, `villageLinkDistance`, `vanillaLootCategories`, `enderZooHardMobs`.
- **Spawn options:** a profile can ask for a spawn in a biome, in a city, never in a building or inside one; the spawn is found from the city plan without generating chunks, and the bonus chest still comes when asked for.
- **Config** (`config/lostcitiesnh.cfg`, also from the Mods list's Config button): module switches (`worldgen`), the profile per dimension (`profiles`), and one switch per decorative metal at or below EV (`decorativeMetals`). All apply to new worlds only. See [docs/config.md](docs/config.md).
- **Operator commands** (`/lostcities`, aliases `/lost` and `/lostcitiesnh`): `profile`, `saveprofile`, `locate`, `debug`, `map`, `timers` (`timings`), `createbuilding`, and the part editor's `listparts`, `locatepart`, `editpart`, `resumeedit`, `createpart` and `exportpart`, which write only under `config/lostcitiesnh/`.
- **An API** for other mods ([docs/api.md](docs/api.md)): what Lost Cities planned for any chunk, extra profiles, and four generation events.
- English lang file for every message and config label.
- For development and real-pack testing only, off in a normal game: the scenario harness (`/lostcities test`), `view` and `find`, and a clean screenshot mode, all behind `-Dlostcitiesnh.testHooks=true`.

### Changed

From upstream Lost Cities:

- Explosion craters and ruined buildings are much rarer and craters smaller by default: damage is an accent, not the look of a city. A profile can still ask for upstream's amounts.
- Desert cities use a muted palette (sandstone, white and light gray terracotta, smooth sandstone) without red, orange or red-brick buildings.
- Roads beside a village are small streets; slopes at a city's edge step down in ragged contours; village lanes climb one block at a time inside a chunk.
- Every command needs operator level 2.
- `buildingMaxFloors` and `buildingMinFloors` take 0 to 22, so no building passes the 1.7.10 height limit.

### Fixed

Upstream bugs this port does not have are listed with their tests in [docs/original-spec/BUGS-COVERAGE.md](docs/original-spec/BUGS-COVERAGE.md). Among them: city layouts that ignored the world seed, crashes on malformed profiles and assets, unknown blocks silently becoming air, `exportpart` writing anywhere in the server directory, and loot that changed with unrelated blocks nearby.

### Upgrade notes

- **Use a new world.** Alpha-era worlds keep their pinned profile but were made with city layout version 1 to 3; this build is version 4, and desert facades and the palette variety changed. New chunks beside old ones will show seams (cut streets and buildings). The server logs a warning per dimension when this applies. Nothing is removed from a save: the mod registers no blocks or items.
- Config keys of the alpha builds are unchanged.
- Requires GTNH 2.9.0 (GTNHLib 0.11.54 or newer).

### Known issues

- No monorails yet (deferred, D12). Highway ramps' slab half-steps contrast with the road surface, so ramps look striped; a rail can have a gap where a junction sits under a shallow landmark.
- A village lane can rise two blocks where it crosses a chunk border.
- On some seeds a highway has steps taller than half a block or without ground, or a railway has a broken track (found by the three-seed checks on seeds 1 and 777; seed 4242 passes every scenario).
- Where an AE2 meteorite's crater cuts city ground, the city ends at the pit's edge with no slope.
- Other mods' trees beside a cabin can lean over it; a GregTech boulder can cut into a building.
- On classic RWG, city generation adds about 15 % to RWG's own generation time over a large region.
- Loot, spawners and city density are not balanced yet ([beta balance list](docs/new-spec/beta-balance-list.md)). No achievements or in-game guide yet.

### Provenance

Ported from McJty's Lost Cities (MIT, branch `1.20` at `f18a094d`) and rewritten for 1.7.10 with AI assistance (Claude); see [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) and [docs/assets/CREDITS.md](docs/assets/CREDITS.md).
