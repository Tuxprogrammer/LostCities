# Changelog

All notable changes to Lost Cities: New Horizons. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versions come from git tags.

## 0.4.0-beta (2026-11-07)

Faster chunk generation, mostly around the highways and railways between cities. Same install: one jar on the server and every client, GT New Horizons 2.9.0. Worlds made with 0.3.0-beta keep working; see the upgrade notes for how links change at the edge of old ground.

### Changed
- Faster chunk generation near city links. On the `perf.sites` scenario (RWG2 continents, seed 4242), server generator time drops by about 10 % on `rarecities` (30.6 s to 27.5 s over six sites) and its worst chunk from 2.4 s to 0.63–0.87 s. On the default profile it drops by about 7 % (31.3 s to 29.2 s) and its worst chunk from 0.62 s to 0.40 s. The overlay's own terrain time is down by about two thirds on `rarecities` (3.5 s to 1.1–1.5 s) and by about a quarter on the default profile (3.1 s to 2.2–2.3 s). What changed:
  - Highway routing asks a coarse per-cell "can a link pass here" mask instead of building full landmark zones. It caches each route and each cell's links and bounds its cell scans. The landmark, village and height oracles keep larger caches (the world cache ceiling B5 is raised to 48 MB, D-perf-1).
  - Landmark types a profile ignores (by default Roguelike Dungeons and Witchery) are no longer looked up at all (D-perf2-oracles-1).
  - The landmark oracles are asked once per 16 × 16-chunk super-cell instead of once per 8 × 8-chunk cell. On the slowest site this cuts the queries about sixfold (D-perf2-route-1, D-perf2-route-3).
  - A link that meets a landmark is detoured around the blocked stretch only, with a bounded search, instead of redrawing the whole line up to 30 times (D-perf2-route-2).
  - City links are chosen from geometry: neighbors in a relative-neighborhood graph, topped up so every city has its 2 nearest. Only the links that reach a chunk are routed, and choosing a cell's links no longer routes every link nearby (D-perf2-links-1 to 3).
- On RWG2 worlds, city plans and city-link routes are worked out on a background thread while the server thread waits on terrain or sits idle between ticks. Routes are worked out in bounded steps, so the background thread never holds the planner for more than about 2 ms (D-perf2-worker-1). Fetches that are not safe off-thread still run on the server thread. Turn it off with `-Dlostcitiesnh.planWorker=false`; worlds come out the same either way.
- Diagnostics: `-Dlostcitiesnh.perfCounters=true` logs per-stage timing counters, with a `.worker` suffix for the background thread's share, and `lostcities test perf.sites` (test hooks on) times chunk generation over a row of sites.

### Upgrade notes
- The city-link network is chosen differently, and detours around landmarks take new shapes. In a world made by an earlier version, links in chunks already generated stay as they are. Chunks generated after the upgrade follow the new network, so a highway or railway can end at the edge of the old ground, or a new one can start there. Cities and their own streets are unchanged.

### Known issues
- The first chunks after a teleport next to an unrouted city link can still take about 0.6 s. The background planner works ahead of a walking player but cannot get ahead of a jump, so this chunk is still over the 10 ms per-chunk budget (B4).

## 0.3.0-beta (2026-11-06)

Bigger, farther-apart cities and a round of city fixes from playtesting 0.2.1-beta. Same install: one jar on the server and every client, GT New Horizons 2.9.0. Worlds made with 0.2.x keep working; see the upgrade notes.

### Changed
- `rarecities`: cities cover four times the area (radius 200 to 511 blocks) and stand four times as far apart (one per 256 x 256 chunk cell, jitter 64); links reach up to 640 chunks so every city keeps at least two highway links. New worlds only (see upgrade notes).
- GT gear in cities is Steam age to EV: high-pressure (steel) steam machines and coal boilers join the bronze ones, and ULV is gone. Plain machine casings stand in for machines inside rooms, about two casings per real machine, so complete machines are rarer. Casings are never building material any more: the industrial yard shed is built from pack factory blocks with an iron roof.
- Link highways that cross now meet at the same height and join in a crossing or T instead of one passing over the other on a bridge; only crossings too close to both cities to ramp stay stacked.
- Cities now build over other mods' structures instead of leaving holes for them, except villages (and RWG2 volcanoes, lava caves and river junctions, unchanged). LootGames dungeons and AE2 meteorites no longer reserve any chunks; Witchery structures and Roguelike Dungeons towers no longer do either, and are skipped where they would stand on a city (Witchery) or steered off it (Roguelike). A profile can opt one back in with `landmarkTreatment` entries such as `witchery=BUILD_AROUND`.
- Cities cut into hills instead of stopping at them: a chunk inside a city is no longer dropped for rising too high, so no more missing blocks at the rim. `cityMaxRise` still caps how high the city's streets climb; the terrain above is cut away.
- Furnishing: GT machines now turn up in shops, offices, residential and kitchen rooms as well as industrial floors, with fewer in basements (same per-chunk tile-entity cap); the district plaza monument is a pillar of stone brick instead of GT casings.
- The mod's own houses now have 3-block-high rooms (5-block floors), and the workshop a 5-block-high bay under 3-block-high upper floors; towers and upstream buildings keep the world's floor pitch.

### Fixed
- District plazas are no longer cut in half by a highway or a change of street level across them: a big city's 2 x 2 plaza is drawn only where all four chunks fit, otherwise a one-chunk plaza with its own monument; and a subway station under a plaza puts its entrance in a neighboring chunk instead of breaking into the plaza (the station stays underneath; no entrance if no neighbor is free) (D-plaza-road-1, D-plaza-station-1).
- Buildings: escalators and other stair runs that climb from one floor into the next (such as the quartz escalator in the shopping mall) no longer break at the upper floor's slab at the default 8-block floor pitch (D-stair-xfloor-1).
- Buildings near explosion damage no longer get holes in their floors, and their spawners no longer end up as pig spawners: the floating-fragment cleanup leaves a building's floors, walls and furniture where they were built.
- No more pig spawners in cities: a spawner, chest or GT machine that explosion debris knocked off its floor is still given its mob, loot or machine, or becomes air; and furnished GT machines show their proper name and model at once instead of a nameless ".name" block until the world is reloaded.
- Highway tunnels are lit: ceiling glowstone every few blocks (light 8 or more over the road), whether or not `generateLighting` is on. New chunks only.
- Barrels are no longer empty: furnished barrels in shops, basements and industrial rooms, and the barrels on oil rigs and in cabins, now hold city loot like chests do (supplies to LV in shops; the room tables elsewhere; the rig and cabin use the upstream chest-loot condition, filtered to this pack's tiers).
- Scattered structures (cabins, gas stations, restaurants, oil rigs, radio towers) always hold loot in their barrels and chests: the "building without loot" and "chest without loot" chances no longer apply to them, which left about one cabin barrel in three empty (D-cabin-loot-1).
- City link network: a link blocked by a landmark now detours around it instead of being dropped, so cities are no longer leaves with one highway; a highway whose end plaza missed the city runs on as a spur to the first city chunk; a landmark inside a city moves its beltway ring instead of ruling the beltway out (D390 to D392).

### Upgrade notes
- Existing worlds change at the border of newly generated chunks: a city around an old LootGames, meteorite, Witchery or Roguelike lot fills that lot in new chunks, so a cut may show where old and new chunks meet. Chunks already generated are not touched.
- `rarecities` worlds created before 0.3.0-beta keep their 64-chunk grid and city sizes, because a world pins its profile values when it is created. Create a new world to get the 4x cities.
- Every other change applies to newly generated chunks only; chunks already generated are not touched.

### Known issues
- With a floor pitch of 9 or 10 (not the default 8), the escalators in the shopping mall buildings still break at the upper floor.
- A few link highway crossings still pass one over the other where neither highway has room to ramp to a shared level, mostly where two links leave a city along the same row.

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

- GT machine casings are no longer building material: yard sheds are factory block under an iron roof, and casings appear scattered in rooms as stand-ins for full machines, making real machines rarer to find.
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
