# Roadmap

<!-- Milestones in one lane run in order; lanes run in parallel once their entry gates hold. Tick a box only in the commit or pull request that completes it. -->

Lost Cities: New Horizons backports McJty's Lost Cities to GT New Horizons: the same cities, highways and railways, generated over the pack's RWG overworld and built from the pack's blocks. Upstream's behavior is in [docs/original-spec/](original-spec/README.md), what changes for the pack in [docs/new-spec/](new-spec/README.md), and the accepted decisions in [docs/design/](design/README.md).

Two gates matter most:

- **Alpha:** a city generates in a fresh GTNH 2.9.0 overworld on RWG2 Continents (the real pack, dedicated server). It is deterministic, has no cascading generation and stays within the time budget, and a player can walk its streets and enter its buildings. The owner's review here is light: they look at a screenshot set and redirect from there.
- **Beta, the MVP:** the whole mod, unpolished. Every spec sheet and design record is functional and live in the mod, so only polish and balance remain ([0004](design/0004-gtnh-layer-and-tier-ceiling.md)). This is where the real review happens: interactive back-and-forth with the owner to iron out the GTNH integrations and the advanced features.

## How to read this roadmap

- **Spikes (S1…)** are research or spec work that must land before the milestones named in them. They run in parallel with whatever is under way.
- **Milestones (M01…)** each have an **entry gate** (true before work starts), a scope and an **exit gate** (the definition of done).
- **Lanes** are the agent teams. A lane owns its feature area's code, tests and docs, so lanes don't collide. Milestones in different lanes run **in parallel** once their entry gates hold.
- Each milestone's content ships behind a module toggle that changes behavior only, never registration (`config-safe-registration`).
- Every change meets the [definition of done](../CONTRIBUTING.md#definition-of-done): spec sheet cited, deviations recorded, regression tests for upstream bugs it could reproduce (`doesNotReproduce<ID>_…`, IDs from [BUGS.md](original-spec/BUGS.md)), ported code through the gate, headless worldgen scenario clean.
- The planning phase (S1 to S3 and M01) is interactive: open questions go to the owner with a recommendation. Milestone scopes below are refined from the specs when S1 to S3 land, and their exit gates are written in full then.

```
 S1 Behavior spec ──────────┐
 S2 RWG overlay research ───┼──► (new spec written) ──┐
 S3 Pack research ──────────┘                         │
 M01 Foundation ──────────────────────────────────────┤
                                                      ▼
                      ┌───────────────┬───────────────┴───────────────┐
                      ▼               ▼                               ▼
               M02 Asset system  M03 Layout (pure Java)        M04 RWG overlay and terrain
                      └───────────────┴───────────────┬───────────────┘
                                                      ▼
                                             M05 First city in the world
                                                      ▼
                                                 ALPHA GATE
                                                      │
               ┌──────────────┬──────────────┬────────┴─────┬──────────────┬──────────────┐
               ▼              ▼              ▼              ▼              ▼              ▼
          M06 Highways   M07 Scattered  M08 GTNH layer  M09 Profiles   M10 Commands,  M11 Full asset
          and railways   structures     (loot, mobs,    and config     API, editor    catalog
                                        ore veins)
          (M05R alpha review round; M12 city edges and character)
               └──────────────┴──────────────┴────────┬─────┴──────────────┴──────────────┘
                                                      ▼
                                              BETA GATE (MVP) ──► Polish and balance ──► Release
                                                      │
                                                      └──► After beta: PersonalSpace cities; GTNH-themed buildings
```

## Lanes

| Lane | Milestones | Owns |
|---|---|---|
| **Spec** | S1 | The original spec, the bug register, coverage and the API map |
| **Core** | M01, M09, M10 | Registration, config, profiles, commands, network, the API, static checks, the scenario harness, the dev self-check |
| **Assets** | M02, M11 | The JSON asset system, block mapping, the bundled asset catalog |
| **Layout** | M03, M06, M07 | City, street, building, highway, railway and scattered-structure layout (pure Java) |
| **World** | S2, M04, M05 | The RWG overlay, terrain, writing parts into chunks, population, performance |
| **Integration** | S3, M08 | Pack research, ores, loot, mobs, other pack worldgen |

## Spikes

Status: **S1 to S4 are done** (2026-11-01). S3 is done now that the new-spec areas `00` to `09` are written, each of `01` to `09` a deviation table built from the decisions already taken. S4's catalog and variety pools follow decision log D25, D28 and D29. Open questions Q1 to Q13 are settled, and so is every *Proposed* item the areas listed: the owner answered them in decision log D31, and the new spec has no open questions ([OPEN-QUESTIONS](new-spec/OPEN-QUESTIONS.md)). The milestones below build the answers.

| Spike | Delivers | Before | Lane |
|---|---|---|---|
| **S1 · Behavior spec** | All nine original-spec areas per [CONVENTIONS.md](original-spec/CONVENTIONS.md), [BUGS.md](original-spec/BUGS.md), `COVERAGE.md` (every upstream Java file and asset type named) and `API-MAP.md` (every 1.20 API and its 1.7.10 counterpart) | M02, M03, M04 | Spec |
| **S2 · RWG overlay** | How RWG2 Continents and classic RWG generate a chunk, read from source at the 2.9.0 tags; a throwaway prototype that overlays a flat test city; a design record for the hook, the terrain blend, the height mapping and the time budget (open questions Q1, Q2, Q7, Q10) | M04 | World |
| **S3 · Pack research** | The extracted 2.9.0 reference pack and `MOD-INDEX.md`; the block-mapping table; GT ore-vein generation and how it meets structures; loot and mob sources; other overworld structures (Q3 to Q6, Q8, Q9); then the new-spec areas | M04, M08 | Integration |
| **S4 · Decorative block catalog** | Every decorative and construction block GTNH 2.9.0 enables, with contact sheets, roles and fit against upstream's look; variety pools per palette role (decision log D25). Lives in `docs/new-spec/05-assets/block-catalog/` | M02 (palette format), M11 | Assets |

## M01 · Foundation

**Lane:** Core. **Parallel with:** S1, S2, S3.

**Entry gate:** this roadmap, [CONTRIBUTING.md](../CONTRIBUTING.md) and design records 0001 to 0004 are committed.

**Scope:**
- dependencies pinned to GTNH 2.9.0;
- the package skeleton ([CONTRIBUTING § Architecture](../CONTRIBUTING.md#architecture));
- JUnit 5 wired into the build;
- the static checks (`ClientIsolationTest`, `LayoutPurityTest`, `Java8ApiTest`, `PortedCodeGateTest`, `LangKeysTest`, `AssetValidationTest`);
- the chunk-buffer interface and golden-chunk test harness;
- module flags on GTNHLib config, with the automatic config screen GTNHLib gives it (no `guiFactory`; worldgen keys marked as needing a new world, D31);
- the network channel with validated packet base classes;
- the in-world scenario command and harness;
- the dev self-check;
- [THIRD-PARTY-NOTICES.md](../THIRD-PARTY-NOTICES.md) and the changelog;
- CI green.

**Exit gate:**
- [x] `./gradlew spotlessApply build` passes, with at least one real unit test and every static check running (each proven by a fixture that makes it fail).
- [x] `./gradlew runServer` boots with the pack's core dependencies and no errors from this mod.
- [x] `./gradlew runClient` reaches the main menu.
- [x] The scenario harness runs a trivial scenario on a fresh world and reports its result.
- [x] Module flags load, save and are documented.
- [x] Packet base classes reject out-of-range, wrong-dimension and unauthorized requests, with tests.

## M02 · Asset system

**Lane:** Assets. **Parallel with:** M03, M04.

**Entry gate:** M01 done; S1's `05-assets` area written (met); S3's block-mapping table drafted (met: accepted as D19).

**Scope:** every asset type loaded and validated in plain Java; 1.20 block names mapped to 1.7.10 pack blocks, with `modid:name:meta` native references and the moss block as `chisel:moss:0`; palette compilation and variants; asset overrides; the bundled assets converted, with a `sandstone_rubble` variant for desert rubble; biome matchers, including the RWG realistic-biome matcher under `compat.rwg` for canyons and oases (decision log D31).

## M03 · Layout

**Lane:** Layout. **Parallel with:** M02, M04.

**Entry gate:** M01 done; S1's `02-profiles-dimensions` and `04-city-layout` areas written (met).

**Scope:** city detection, rarity and spheres; city styles; street and bridge planning; building and multibuilding selection; chunk plans; building around other mods' structures ([0006](design/0006-build-around-other-worldgen.md)): the `StructureOracle` interface and landmark data, landmark cores, edge rings and village transition rings, road detours and plazas, village sewing (gates and lanes), and meteorite height caps; all in `layout`, with golden tests against fake oracles.

**Exit gate:**
- [x] City detection, rarity, styles, levels, the hierarchical street and bridge planners, multibuilding placement and the per-chunk plan (`LayoutPlanner`, `ChunkPlan`, `ChunkLayout`) live in `layout`, pure Java (`LayoutPurityTest`).
- [x] The pure planners reproduce upstream's output for the same seed, checked against values recorded from upstream and Minecraft 1.20.1 (decision log D45); the rest against the spec's numbers.
- [x] Every CITY bug in the ported logic has a `doesNotReproduce` test, and the ported code passes the gate.
- [x] Landmark zones, detours, plazas, gates, lanes and crater caps, with layout goldens against fake oracles for the cases 0006 names.
- [x] The layout is deterministic across generation order, cache size and oracle order (`DeterminismTest`).
- [x] D30's spawn-distance measurement is done: no near-spawn option is needed (D53).

## M04 · RWG overlay and terrain

**Lane:** World. **Parallel with:** M02, M03.

**Entry gate:** M01 done; S2's design record accepted (met: [0005](design/0005-rwg-overlay.md)); S1's `03-terrain` area written (met).

**Scope:** the overlay hook on RWG2 Continents and classic RWG; city terrain and its blend into RWG terrain; the 1.7.10 height mapping; population, with the footprint write guard active during every RWG `populate`, no sapling growth at generation, and a scenario check that ArchaicFix's Phosphor lights every city light source (D31); explosions and damage; the per-chunk time budget; around landmarks ([0006](design/0006-build-around-other-worldgen.md)): no writes in cores, soft-edge strips instead of border walls, blend corners fixed at a core's natural height, and village lanes laid on the ground.

## M05 · First city in the world

**Lane:** World, with Assets and Layout. **Parallel with:** none (integration).

**Entry gate:** M02, M03 and M04 done.

**Scope:** layout, assets and the overlay joined, so a full city with streets, buildings and parts generates in the overworld. The VillageNames structure oracle and village sewing, with lanes laid in the blocks VillageNames' own paths use (D31), and the VillageNames ocean-monument oracle (D32), come in here, ahead of the other oracles in M08, so the alpha screenshots can show a village sewn into a city (decision log D27).

**Exit gate (the alpha gate):**
- [x] On a fresh GTNH 2.9.0 dedicated server with RWG2 Continents, a city generates within the expected distance of spawn for a fixed seed. *M05J, seed 4242: the nearest city chunk 7 blocks from spawn (center to spawn), the nearest city center 30 blocks (classic RWG: 24 and 90 blocks; D53: median 57, worst 102).*
- [x] Two fresh worlds with the same seed and profile generate identical city chunks. *M05J: `city.real`'s 17 × 17-chunk hashes match between fresh worlds on both world types.*
- [x] No cascading-generation warnings; the per-chunk budget holds. *M05J: no ArchaicFix cascade warning has this mod's code as its first frame on either world type. Alpha follow-ups: with the plan prefetcher (D123, D125) B1 to B5 hold on both world types in two fresh worlds each, seed 4242: B4's worst p99.9 7.8 and 7.4 ms on classic RWG (was 18.9), 4.1 and 3.6 ms on RWG2 Continents; B3 13.1 and 12.8 %, 9.7 and 9.1 % ([0005](design/0005-rwg-overlay.md) §8); still no cascade warning with this mod's code as its first frame.*
- [x] A player can walk the streets and enter buildings; floors, walls and stairs are sound. *M05J: `city.walk` passes on both world types (floors 96 to 99 %, walls 96 %, stairs 93 to 95 %, doors 98 to 100 %).*
- [x] Classic RWG produces a city too. *M05J: seed 4242 has a desert city at the spawn.*
- [x] Owner review (light): a screenshot set of the real-pack world (city from above, street level, building interiors, the edge where the city meets RWG terrain, on RWG2 Continents and classic RWG), with landmark shots: a village sewn into a city, a reserved lot, and a soft edge ([0006](design/0006-build-around-other-worldgen.md)). The owner's notes become fixes or roadmap items. *2026-11-02: reviewed; the notes and their triage are in [reviews/2026-11-02-alpha.md](reviews/2026-11-02-alpha.md) (AR-1 to AR-21). The alpha gate is passed; the review's fixes are M05R, its larger asks M12 (D131).*

## M05R · Alpha review round

**Lane:** World, with Assets. **Parallel with:** M08, M09.

**Entry gate:** the alpha gate.

**Scope:** the alpha review's notes marked M05R in [reviews/2026-11-02-alpha.md](reviews/2026-11-02-alpha.md). Clean streets (AR-2). A cap on how far a city's ground rises above its base, so cities stop or step down before high terrain (AR-4, AR-14). A city builds inside its own margin, which the soft edge then stitches to the village; slopes have stairs and slabs and never take a road, a highway or half a park (AR-10 to AR-13). Small roads for sewing that run down the slope (AR-7). Much lower damage (AR-8). A muted desert palette (AR-16). Denser park greenery (AR-18). The missing stair step (AR-1). A review shot list without the monument (AR-15).

**Exit gate:**
- [ ] Each M05R note is fixed, with a test where one can be written, or its cause is recorded.
- [x] M05's exit-gate checks still hold on both world types: hashes match between fresh worlds, budgets B1 to B5, no cascades, `city.walk`. *2026-11-02, the World half's jar: two fresh worlds per world type on seed 4242 print the same `city.real` hashes; B1 to B5 hold (B3 9.1 % on RWG2 Continents and 12.8 % on classic RWG, B4 at most 3.9 and 7.5 ms; [0005](design/0005-rwg-overlay.md) §8); no cascade warning names this mod first; `city.walk` and `city.sewing` pass, with no margin chunk city.*
- [ ] Both review shot lists rerun with the final jar, and every M05R note checked against them.

## M09 · Profiles and config

**Lane:** Core. **Parallel with:** M05R, M08.

**Entry gate:** the alpha gate; [02-profiles-dimensions](new-spec/02-profiles-dimensions/index.md) and [01-core § Mod config](new-spec/01-core/index.md#mod-config) written (met).

**Scope:** every upstream profile option that survives, in upstream's JSON format, with ranges enforced and upstream's checks plus the missing ones; the ten built-in profiles RWG can carry; profile files in `config/lostcitiesnh/profiles/`, `saveprofile`, `profile list`, `profile info` and `profile reload`; every kept option pinned and read from the pin (the layout, the terrain, the world style, `liquidBlock` and `baseBlock`); spawn selection from the profile's spawn options ([01-core § Spawn selection](new-spec/01-core/index.md#spawn-selection)); profile selection without a world-preset screen (`dimensionProfiles`, checked at start, and the Mods list's config screen in single player); server-owner config documented in full. Options whose readers belong to M06, M07 or M10 are checked and pinned here, and read there.

**Exit gate:**
- [x] Every option of new-spec [profile-options.md](new-spec/02-profiles-dimensions/profile-options.md) is in the option catalog with its type, default, range and status; the counts match upstream's (107, 12, 25, 14, 5); every live option reaches its reader; docs/config.md documents every option and config key with its default (`ProfileOptionsDocsTest`, `ConfigDocsTest`). *`ProfileOptionsTest`: 172 options, 142 live or pinned for M06, M07 and M10, 13 deferred, 17 dropped.*
- [x] Profile files in upstream's format load; ranges clamp, and wrong types, unknown values, inverted pairs and failed checks reject one file without stopping the game; the ported code passes the gate with a `doesNotReproduce` test for each bug it avoids. *`ProfileJsonTest`, `ProfileValidatorTest`, `ProfileRegistryTest`, `BuiltinProfilesTest`, `SpawnSearchTest`: PROF-02, 03, 04, 06, 09, 12, 13, 15, 16, 18 and 22, CORE-13 to 16.*
- [x] The default profile moves no city: the layout goldens and `city.real`'s hashes are unchanged. *Real pack, RWG2 Continents, seed 4242: terrain `c19b9656…`, population `3ad82cc0…`, the alpha's values.*
- [x] A non-default profile changes generation as specified: unit tests per built-in, and `profile.effects` on the dev server and the real-pack server with a non-default profile. *`CityProfilesTest`; real pack, 48 × 48 chunks at seed 4242: `default` at most 6 floors, 20 ruined buildings, 153 damaged chunks; `tallbuildings` at most 19 floors, 5 ruined, 412 damaged. The dev server's world has no pinned profile, so there it ran the profile commands (`list`, `info`, `saveprofile`, `reload`) only.*
- [x] Spawn options pick the spawn from the layout without generating chunks (`profile.spawn` with `forceSpawnInBuilding` in the real pack). *Spawn at 40 78 40, a building chunk, after 18 chunks of the spiral, in both fresh worlds.*
- [x] A server and a client with different configs still join. *Real pack: the server with `highways` and `iron` off and `nodamage` mapped to dimension 0, the client with its default config; the client joined and played until the server stopped.*
- [x] Two fresh worlds of the same seed and a non-default profile hash identically (`city.real`). *`tallbuildings` with `forceSpawnInBuilding`, RWG2 Continents, seed 4242: terrain `c2fbbf50…`, population `7ab26747…` in both.*

## M06 to M12 · After the alpha

Scopes are written from S1 to S3 when they land. In outline:

| Milestone | Lane | Covers |
|---|---|---|
| M06 · Highways and railways | Layout | Highways, the intercity highway planner, railways, stations, subways and monorails over RWG terrain; highways detour around landmark cores or end before them, and shallow railways dip under a core and climb back (D31) ([0006](design/0006-build-around-other-worldgen.md)) |
| M07 · Scattered structures | Layout | Scattered structures and decorative stuff; scattered structures stay out of landmark cores and edge rings, which replace upstream's structure avoidance ([0006](design/0006-build-around-other-worldgen.md)); lakes, lava, dungeons and oil denied under their footprints; oil rigs only in RWG2's deep ocean, none on classic RWG, which has no oceans (D31) |
| M08 · GTNH layer | Integration | Loot at EV or below, pack mobs and spawners, GT ore-vein coexistence, other pack worldgen: the structure oracles under `compat` (Roguelike Dungeons, Witchery, LootGames, AE2, RWG2; the VillageNames oracle lands in M05), the Roguelike Dungeons steering mixin (which also keeps towers out of scattered footprints, D31), and the AE2 crater basin ([0006](design/0006-build-around-other-worldgen.md)) |
| M09 · Profiles and config | Core | Every profile option that survives, server-owner config, profile selection without a world-preset screen |
| M10 · Commands, API and editor | Core | Operator commands, the public API, the in-game part editor |
| M11 · Full asset catalog | Assets | Every bundled building, part, palette and style converted and checked in game |
| M12 · City edges and character | Layout, with Assets | From the alpha review (AR-5, AR-6, AR-9, AR-17): building heights that taper organically from the center toward the edge; empty and construction lots near the edges (bare clay ground with no foliage, some dug 1 to 4 blocks for a basement, a variant with concrete pillars); damage partly repaired with a similar but not identical material; neighboring buildings' colors that feather across biome and style changes (a design record first) |

## Beta gate (MVP)

**Entry gate:** M01 to M12 and M05R done.

**Exit gate:**
- [ ] Every original-spec sheet is implemented or has a recorded deviation; every new-spec sheet is live.
- [ ] Every reproducible bug in BUGS.md has its regression test.
- [ ] The full scenario suite passes on fresh worlds; the real-pack server and client run is clean on GTNH 2.9.0, on RWG2 Continents and classic RWG.
- [ ] The owner's interactive review: play sessions with back-and-forth on the GTNH integrations and advanced features, each round's findings fixed before the next. It ends with the owner's sign-off and a list of balance items for the polish pass.

## After beta

- **PersonalSpace:** cities inside a personal dimension, on a flat world ([0003](design/0003-world-presence.md)). Upstream's other landscape types (floating islands, glass-bubble space cities with monorails, caverns, biospheres) are the first candidates there, with terrain this mod generates (decision log D12).
- **GTNH-themed additions:** new buildings and styles beside upstream's ([0004](design/0004-gtnh-layer-and-tier-ceiling.md)).
- **Building geometry variety:** the other half of D25. Buildings that differ in shape, not only in blocks, so cities feel less constructed. Planned after the block variety work, as its own design record.
