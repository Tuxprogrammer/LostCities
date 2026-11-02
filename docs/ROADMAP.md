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
- [x] Each M05R note is fixed, with a test where one can be written, or its cause is recorded. *2026-11-02: AR-1, AR-8, AR-16 and AR-18 by the Assets half. AR-2: the stray grass and sand were this mod's leveling leaving terrain one block above the street (TERR-39, D154), fixed with regression tests. AR-4 and AR-14: the rise cap (`cityMaxRise`, D148; `RiseCapTest`); the walls and the column were upstream's border and foundation skirts under chunks that had climbed a mountain. AR-10 to AR-13: the margin (D149, D150; `SewingTest`, `SoftEdgesTest`, `LandmarkZonesTest`). AR-7 and AR-11: lanes across the margin, graded, with stairs and half slabs, and plazas where a road ends at a margin (D151, D152; `VillageLanesTest`, `MarginLaneTest`). AR-15: the monument shot is replaced by the rise view (D155).*
- [x] M05's exit-gate checks still hold on both world types: hashes match between fresh worlds, budgets B1 to B5, no cascades, `city.walk`. *2026-11-02, the merged M05R jar (both halves and M09): two fresh worlds per world type on seed 4242 print the same `city.real` hashes; B1 to B5 hold (B3 8.5 to 9.0 % on RWG2 Continents and 12.3 to 12.5 % on classic RWG, B4 at most 4.2 and 7.4 ms; [0005](design/0005-rwg-overlay.md) §8); no cascade warning names this mod first; `city.walk` and `city.sewing` pass, with no margin chunk city.*
- [x] Both review shot lists rerun with the final jar, and every M05R note checked against them. *2026-11-02, the merged jar: streets clean on both world types (AR-2); the edge, above and rise views show each city stopping before its mountain with no raised park or gray wall (AR-4, AR-14); the soft edge and reserved views show whole roads and parks inside a natural margin that slopes to the village without a cliff (AR-10 to AR-13); the RWG2 village view shows the lane running down the margin on stairs and slabs into the village path (AR-7, AR-11). The shot pages are under `reference/remote/shots/` (`shots-20261102-133029-alpha-rwg2`, `shots-20261102-133557-alpha-rwg`).*

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

## M08 · GTNH layer

**Lane:** Integration. **Parallel with:** M05R, M09.

**Entry gate:** the alpha gate; [00-pack-integration](new-spec/00-pack-integration/index.md) and [08-loot-mobs-blocks](new-spec/08-loot-mobs-blocks/index.md) written, records [0004](design/0004-gtnh-layer-and-tier-ceiling.md) and [0006](design/0006-build-around-other-worldgen.md) accepted (met). Upstream's loot and spawner behavior: [original-spec/08-loot-mobs-blocks](original-spec/08-loot-mobs-blocks/index.md).

**Scope:**
- **Loot at EV or below** ([loot-and-mobs.md](new-spec/00-pack-integration/loot-and-mobs.md) Q5, D20): this mod's `ChestGenHooks` categories, one per tier band (`city_supplies`, `city_lv`, `city_mv`, `city_hv`, `city_ev`) plus `rail_dungeon`, filled from pack items at `postInit` (GT components and circuits by tier, GT materials, EnhancedLootBags of the matching tier, pack food and supplies, Railcraft for rail dungeons); upstream's `chestloot` choosing among them by floor; the tier map and the dev self-check that fails on anything above EV or unmapped; the vanilla categories behind a profile opt-in, off by default.
- **Pack mobs and spawners** (Q6, D20): vanilla spawners with upstream's mobs as 1.7.10 names; every name checked against the entity registry, with a fallback; EnderZoo's overworld mobs as an optional `hardmobs` set, off by default.
- **GT ore-vein coexistence** ([ore-veins.md](new-spec/00-pack-integration/ore-veins.md), D18): veins pass through cities and no building block is an ore host; the boulder restore pass (option B) only if the alpha screenshots show boulder damage.
- **Other pack worldgen** ([other-worldgen.md](new-spec/00-pack-integration/other-worldgen.md), D21): the T2 denial completed with Railcraft's quarry populator; the structure oracles under `compat` for Roguelike Dungeons, Witchery, LootGames, AE2 and RWG2's terrain landmarks, wired beside VillageNames' and pinned in the snapshot; the Roguelike Dungeons steering mixin, which also keeps towers out of scattered footprints (D31); the AE2 crater basin ([0006](design/0006-build-around-other-worldgen.md) §1, §3).

**Exit gate:**
- [x] Loot: the bundled defaults keep the EV ceiling, the band rules and the loot-bag families (`LootDefaultsTest`, `LootRulesTest`); the dev self-check walks every registered category in the real pack and finds nothing unmapped or above EV; on both world types `gtnh.layer` finds filled city chests holding only tier-mapped items at EV or below. *Real pack, seed 4242, instance 6, final jar: the dev self-check passes (119 entries in 6 categories; GT 5.09.54.265 has no wrought-iron ingot, so that entry was dropped); `gtnh.layer` on classic RWG: 94 filled chests, 232 items (Stone 113, Steam 66, LV 47, MV 4, EV 2), 0 unmapped or above EV; RWG2 Continents: 23 chests, 52 items (up to MV), 0 bad.*
- [x] Spawners: on both world types every city spawner names the mob its plan drew, from upstream's set (`Zombie`, `Skeleton`, `Spider`, `Blaze`, `Witch`), never a pig, and each name creates its entity; the EnderZoo set replaces `hardmobs` only when its option is on (`JobTargetTest`). *Classic RWG: 245 spawners (Zombie 88, Skeleton 92, Spider 62, Witch 2, Blaze 1), RWG2 Continents: 85; every one the planned mob, none a pig, each creating its entity.*
- [x] Ore veins: on both world types GT ore blocks stand in the natural stone under city chunks and none above the city ground in a building chunk (`gtnh.layer`); the alpha review showed no boulder damage, so option B is not built (recorded). *Classic RWG: 72,277 GT ore blocks under the city ground, 0 above it in building chunks; RWG2 Continents: 35,485 and 0. Earlier rounds found 1 to 11 ore blocks inside GT boulders that cut into terracotta (counted apart, D200).*
- [x] Each structure oracle is pure, cached and unit-tested with fakes, and its real-pack scenario matches the structures the other mod placed, with none of them inside a city chunk or edge ring: `oracle.roguelike`, `oracle.witchery`, `oracle.lootgames`, `oracle.ae2`, `oracle.rwg2` (RWG2 Continents only). *Unit tests per oracle; on both world types `oracle.roguelike` (hits 60 % classic, 68 % RWG2, no tower in a city chunk or edge ring), `oracle.witchery` (6 of 8 lots stand on classic, 4 of 8 on RWG2, the rest explained by stored biomes; every structure block inside its lot), `oracle.lootgames` (65 of 65 sites agree with LootGames' own test), `oracle.ae2` (every meteorite at its predicted site and size) and `oracle.rwg2` (RWG2 only; 0 of 22,887 protected columns missed) pass.*
- [x] The Roguelike steering mixin applies in the real pack and refuses every candidate whose lot touches a city chunk or edge ring; scattered footprints are honored through the layout's seam, answered by M07's scattered planner. *The late mixin is applied in the real pack (`Mixing MixinRoguelikeDungeon`); `oracle.roguelike` finds no tower in a city chunk or edge ring on either world type.*
- [x] A meteorite near a city leaves no city block under its crater's paraboloid (`oracle.ae2`). *`oracle.ae2` passes on both world types; chunks whose city ground the cap would cut leave the city (D216).*
- [ ] M05's checks still hold on both world types with the M08 jar: `city.real` hashes match between two fresh worlds, budgets B1 to B5 hold (`city.budget`), no cascade warning has this mod's code as its first frame, and `city.walk` passes. *Combined jar (M07, M10 and M11 merged), two fresh worlds each: classic RWG terrain `1ca59831…`, population `7b3c1e4d…` in both; RWG2 Continents `a4f7894e…`, `ac2a9991…` in both. `city.walk` passes on both; no cascade warning has this mod's code as its first frame. RWG2 Continents: B1 to B5 pass (B3 11.5 %, B4 5.4 ms). Classic RWG: B1, B2, B4 (7.1 ms) and B5 pass, B3 is 15.0 % (2,818.7 of 18,784.4 ms), at the limit; before M07 merged the M08 jar held 14.2 and 14.6 %. Left to the combined performance pass after wave 2.*
## M07 · Scattered structures

**Lane:** Layout. **Parallel with:** M06, M08, M10, M11, M12.

**Entry gate:** the alpha gate; M05R done, so landmark cores and their margins (edge rings) are final (D148 to D155); [07-scattered](new-spec/07-scattered/index.md) and [0006](design/0006-build-around-other-worldgen.md) written (met).

**Scope:**
- upstream's scattered structures in pure `layout` code: areas, the chance with `scatteredChanceMultiplier`, weighted selection, normal and near-highway placement over M06's `InfrastructureView`, terrain stats from the height oracle, the floor and part draws replayed per chunk, the railing openings M06 asks for; every SCAT bug of the ported logic fixed;
- scattered footprints stay out of landmark cores, edge rings and the city's blend reach, which replace upstream's structure avoidance; oil rigs only in RWG2's deep ocean (oracle floor at Y 45 or lower), none on classic RWG (D31);
- writing them chunk by chunk in the terrain phase, with their chests, spawners and tile entities as population jobs, and the write guard over their volume;
- the ground under a scattered structure leveled and ramped into RWG's terrain on every side, so it never stands on a cliff, floats or is cut (alpha review AR-10 to AR-12);
- lakes, lava, dungeons and oil denied where a population window touches a footprint (P07-4, D31); the footprint test M08's Roguelike steering mixin reads;
- upstream's stuff columns (chains and cobwebs) in city chunks;
- `/lostcities find scattered[:<name>]`, the `scattered.real` scenario, and scattered shots in the review lists.

**Exit gate:**
- [x] The scattered planner, the writer, the ground fit and the stuff writer pass the ported-code gate, with unit tests for upstream's numbers and a `doesNotReproduce` test for every SCAT bug they could reproduce. *`ScatteredPlannerTest` (SCAT-01 to SCAT-03, SCAT-05 to SCAT-07, SCAT-09 to SCAT-12, SCAT-14, SCAT-15), `ScatteredWriterTest` (SCAT-08, SCAT-13), `StuffWriterTest` (ASSET-21, SCAT-20 to SCAT-23), `GroundFitTest`, `ScatteredGroundTest`, `ScatteredStructuresTest`; 807 tests pass after merging `gtnh` at 44d9e709.*
- [x] Scattered structures generate at the rates their world style specifies, measured from plans over a large region on both world types, and no footprint touches a landmark core, an edge ring, a city chunk or the blend's reach (`scattered.real`). *2026-11-02, seed 4242, 65 × 65 areas: every entry's selections within its odds (RWG2: radio tower 391 for 389 ± 18, cabin 257 for 260 ± 15, oil rig 144 for 128 ± 11; classic RWG: 547 for 571 ± 22, 366 for 381 ± 19); 131 structures placed on RWG2 and 66 on classic RWG, none in a zone or within 2 chunks of a city or core. Most selected land structures not placed are near a city (RWG2 444 of 648, classic RWG 661 of 913); the gas station and restaurant wait for M06's highways.*
- [x] No lake, lava lake, dungeon or oil under a footprint, and every generated structure stands whole on ground that meets RWG's terrain without a cliff (`scattered.real` on both world types). *Three radio towers and an oil rig on RWG2, two radio towers and a cabin on classic RWG: every block standing (472/472, 3996/3996, 536/536), no dungeon spawner, lava or lake water, no terrain inside; steps of 3+ blocks the fit made: 0 of 7,820 on five, 28 of 7,820 (0.4 %) on the classic tower at Y 145. 112 and 63 populate steps denied beside them.*
- [x] Oil rigs: some on RWG2 Continents, only in deep ocean; none on classic RWG. *RWG2: 86 in the 65 × 65 areas, all over an oracle floor of Y 45 or lower; classic RWG: 0.*
- [x] M05's checks still hold on both world types, seed 4242, on instance 2: fresh-world hashes match (`city.real` and `scattered.real`), B1 to B5 hold, no cascade warning has this mod's code first, `city.walk` passes. *2026-11-02, the M07 jar merged with M11 and M10: fresh worlds per type print the same hashes, three each (RWG2 `city.real` 55dc1557…/6666e91e…, `scattered.real` e934d9dd…; classic 44e77e87…/6ea4b3bf…, 12244d01…); B1 to B5 pass (B3 10.1 to 10.6 % RWG2, 12.8 to 13.7 % classic, as M11's 13.5 to 13.7 %; B4 at most 5.0 ms; B5 15.1 MB); 64 to 85 cascade traces per run, none with this mod's code first; `city.walk`, `city.sewing` and `assets.catalog` pass.*
- [x] The review shot lists take a scattered structure (and an oil rig on RWG2), run with the M07 jar, every image looked at. *2026-11-02: `scattered`, `tower` and `oilrig` on RWG2 (`reference/remote/shots/shots-20261102-153203-alpha-rwg2`), `scattered` and `tower` on classic RWG (`shots-20261102-155133-alpha-rwg`), all settled: each structure whole on level ground that slopes into the land around it; the oil rig in open deep ocean. Cacti stand between the classic radio tower's legs: a decoration the write guard does not stop yet.*


## M10 · Commands, API and editor

**Lane:** Core. **Parallel with:** M06, M07, M08, M11, M12.

**Entry gate:** the alpha gate and M05R; M09's profile commands landed (D139); [01-core § Commands, Part editor, API](new-spec/01-core/index.md#commands), [commands.md](new-spec/01-core/commands.md) and [09-client-ui](new-spec/09-client-ui/index.md) written (met).

**Scope:**
- every operator command of [commands.md](new-spec/01-core/commands.md) that M01 to M09 did not land: `debug`, `map`, `locate`, `createbuilding`, and the part editor's `createpart`, `editpart`, `resumeedit`, `listparts`, `locatepart` and `exportpart`; `timings` as the spec's name for `timers`;
- the part editor: `editMode` live, part records written on the server thread into `data/lostcitiesnh_edit.dat`, edit sessions owned by the world and dropped on logout and unload, exports to `config/lostcitiesnh/export/parts/`;
- the public API in `lostcities.api`: `LostCitiesAPI.get()`, per-world queries of cities, buildings, streets, highways, rails, damage and the asset registries, profile registration frozen at server start, and the four chunk events; extension points where M06's highways and M07's scattered structures land; a guide for mod authors in `docs/api.md`;
- the live parts of [09-client-ui](new-spec/09-client-ui/index.md): lang keys for every player-visible string, GTNHLib's config screen from the Mods list, and the single-player profile choice it carries (D141); the deferred rows stay deferred.

**Exit gate:**
- [x] Every subcommand of commands.md is live or recorded as dropped; each checks operator level 2, rejects a non-operator, sends only lang keys, and writes only under `config/lostcitiesnh/` (`commands.*` scenarios on the dev server and on a real-pack server). *2026-11-02: `commands.permissions` passes on the dev server (no cities there: every subcommand answers `no_cities` or its own error) and on real-pack instance 3 on both world types, seed 4242: an ordinary player is refused all 17 subcommands under the three command names, an operator gets lang-key answers to 22 command lines (`debug` six lines, `map`, seven `locate` hits, `timers`/`timings`, the editor's gate and errors); `CreatePartSubcommandTest`, `ChunkReportTest`, `ExportPartSubcommandTest` (CORE-26, CORE-28).*
- [x] The part editor works end to end in an `editMode` world: records, `listparts`, `locatepart`, `editpart`, `resumeedit`, `createpart` and `exportpart`, with every CORE bug it could reproduce (CORE-26 to CORE-35) covered by a `doesNotReproduce` test. *`commands.editor` on real-pack classic RWG pinned with `editMode` (seed 4242): five records in the generated `building2` chunk, `editpart`, a sponge edit exported into `missingpalette`, `resumeedit` keeps it, `locatepart`, `createpart` at Y 120, `createbuilding`, and logout ends the session. Tests for CORE-26, -28, -29, -30, -31, -32, -34 and -35 (`EditRecordsTest`, `PartExportTest`, `LocatePartSubcommandTest`, `CreateBuildingSubcommandTest`, `EditPartSubcommandTest`, `EditSessionTest`, `ChunkReportTest`, `MapSubcommandTest`, `ExportPartSubcommandTest`).*
- [x] The API answers for a live world and returns `null` for a client world or a dimension without cities (CORE-02); a test drives it over a fixture world; API profiles survive `profile reload` and freeze before any world is pinned (CORE-03, CORE-06); no internal class appears in an API signature (CORE-38); `docs/api.md` documents it. *`LostCityInformationImplTest`, `ILostCitiesTest`, `ILostCityProfileTest`, `ApiProfilesTest`; `api.live` on real-pack RWG and RWG2 Continents: the four events fire, a listener reads blocks, a cancelled `PreGenCityChunk` leaves 0 building blocks where a kept one has 386 to 468; `null` on the dev server's world without cities. [0008](design/0008-public-api.md), [docs/api.md](api.md).*
- [x] The client-UI rows that are live are complete: every lang key in code is defined (`LangKeysTest`), the Mods list opens GTNHLib's config screen in a client run, and the deferred rows carry current notes. *Real-pack client with `ConfigScreenProbe`: Config in the Mods list opens GTNHLib's `SimpleGuiConfig` with the `worldgen`, `profiles` (D141's single-player choice) and `decorativeMetals` categories, 33 elements, every label and tooltip translated.*
- [x] A dedicated server never loads a client class (`ClientIsolationTest`), and the fresh-world hashes of seed 4242 (`city.real`) are unchanged on RWG2 Continents and classic RWG. *`ClientIsolationTest` passes; the real-pack dedicated server runs every M10 scenario. `city.real` with `gtnh`'s jar (44d9e709, M11 merged) and with M10 merged into it, fresh worlds on instance 3: classic RWG terrain `fad0e8d4…` population `6ea4b3bf…` in both, M10's in a world pinned with `editMode`; RWG2 Continents terrain `ab526d1a…` population `6666e91e…` in both. Before the merge, the same held against the merge base f1d8c398.*
## M11 · Full asset catalog

**Lane:** Assets. **Parallel with:** M06, M07, M08, M10, M12.

**Entry gate:** the alpha gate; S4's catalog and variety pools written (met); M02's asset system and M05's conversion of the alpha city's assets (met: all 322 upstream files and this mod's five are bundled and load with no error).

**Scope:** every bundled upstream building, part, palette and style converted and checked in the real pack, with palettes drawing the pack's variety ([0007](design/0007-palette-variety.md)): the catalog's pools turned into option palettes a building draws per role (walls, accents and bands per wall family, glass, quartz, interior floors, ceilings, lights, furniture, bookshelves, fences, planters, park flora), with a switch (`worldgen.paletteVariety`). From the alpha review: richer interiors (AR-3), pack variety in the muted desert (D145, AR-16), pack flora in parks (D146). Upstream's other landscape types (floating islands, space, caverns) are after beta (D12). Palette selection across neighbors (feathering, construction lots, repairs) is M12's.

**Exit gate:**
- [x] Every upstream asset file is bundled and loads with no error over vanilla alone and over the full pack (`AssetValidationTest`); every block an asset or a variety option names is catalogued (`PaletteVarietyTest`), and every part generates under every style's palettes and the variety with no undefined character and no unregistered block (`AssetCatalogCheckTest`). *2026-11-02: 750 tests pass; 327 assets (upstream's 322 and this mod's 5), 359 variety options in 53 groups.*
- [x] In the real pack on both world types (instance 4, seed 4242): `assets.pack_resolution` finds no fallback, no unresolved pool block and no unavailable variety block; `assets.catalog` passes. *2026-11-02: 0 fallbacks, 0 of 366 pool blocks unresolved, 359 of 359 options available; the catalog wrote 112,896 parts (55 million blocks, 428 distinct) with no undefined character, no unregistered block and no block that can regenerate its own chunk (D165).*
- [x] The variety follows its rules, with tests: a seed of its own (the layout's draws unchanged), the wall family kept, the baseline's share, unavailable options back to upstream's look, at most 30 of 100 floors spawn-proof, no speed block, a muted desert; the interior characters land only on floors and ceilings inside buildings. *`PaletteVarietyTest`, `LayoutAssetsTest`, `VarietySeedTest`, `InteriorCharactersTest`, `ParkGreeneryTest`, `GenerationSafetyTest`; the layout goldens are unchanged.*
- [x] M05's exit-gate checks hold on both world types with the variety on: two fresh worlds hash identically (`city.real`), B1 to B5 hold, no cascade warning names this mod first, `city.walk` passes. *2026-11-02, jar `83a6178` (the tree of `256b4233`): RWG2 Continents terrain `ab526d1a…` population `6666e91e…` in both worlds, classic RWG `fad0e8d4…` and `6ea4b3bf…` in both; B3 10.3 % (RWG2) and 13.5 to 13.7 % (classic), B4 at most 4.3 and 7.8 ms; one classic run that shared the box with a shot run had a 17 ms VillageNames start in a prefetch tick (B4), and its rerun passed; 0 cascade warnings with this mod first; `city.walk` as before the variety (floors 95.7 % and 98.9 %, doors 100 % and 97.9 %).*
- [x] Both review shot lists rerun with the final jar; interiors, facades, parks and the desert checked against AR-3, AR-16 and D146. *2026-11-02, 8 of 8 shots each (`reference/remote/shots/shots-20261102-145549-alpha-rwg2`, `shots-20261102-150836-alpha-rwg`): interiors with panel ceilings and a checkered kitchen floor, pack flowers in parks, concrete and hempcrete facades, a pale desert. The first RWG2 run found the chunk-regeneration crash (D165).*

## M12 · City edges and character

**Lane:** Layout, with Assets and World. **Parallel with:** M06, M07, M08, M10, M11.

**Entry gate:** M05R done (met: D148 to D155 merged); the alpha review's notes AR-5, AR-6, AR-9 and AR-17 triaged to M12 ([reviews/2026-11-02-alpha.md](reviews/2026-11-02-alpha.md), D131); [0012](design/0012-facade-color-feathering.md) accepted before its code lands.

**Scope:** the city's character from its center to its edge ([0011](design/0011-city-edges-and-character.md)):
- **Tapering (AR-5):** an urban intensity per chunk from the city factor, the distance to the city's edge (margins, the rise cap and RWG land alike) and low-frequency noise; building heights follow it, with a high-rise core and low buildings at the edge, so the city fades into RWG terrain organically, not in rings.
- **Edge lots (AR-6):** empty and construction lots where the intensity is low: bare clay-and-dirt ground with no foliage, pits dug 1, 2 or 4 blocks for a basement, and foundations with concrete pillars for a high-rise's first stage, in pack blocks, deterministic.
- **Partial repairs (AR-9):** some explosions' damage is partly filled back in with a similar but not identical material, a visible patch; windows boarded.
- **Color feathering (AR-17):** a building's facade is one color, and neighboring buildings' facades stay within a contrast limit across style and biome changes ([0012](design/0012-facade-color-feathering.md)): block colors measured from the pack's textures and shipped as data, and palettes chosen against neighbors in an order-independent way.
- **M05R leftovers:** the margin's bank no longer steps down in long straight 1-block terraces; a lane never rises two blocks between neighboring columns without a step; no wide road runs along or into a village's margin.

**Exit gate:**
- [x] 0012 is accepted and live: `colors.tsv` holds every contact-sheet block, measured by `tools/block-colors/measure.py`; palette choice keeps every pair of neighboring buildings with palette freedom within `facadeContrast`, deterministically and independent of the order chunks are planned in (unit tests over the test world, a style boundary and reversed planning order). *`colors.tsv`: 7,761 blocks. `FacadePlannerTest`, desert and standard cities meeting at seed 4242: 24 neighbor clashes without feathering, 0 with it; the same palettes planned forward and backward with a 64-entry cache (D-M12-1, D-M12-2).*
- [x] Tapering is live: mean floors rise with the intensity, buildings beside the city's edge stay low, and the intensity varies along a ring of equal distance from the center (unit tests). *`UrbanityTest` (D-M12-3).*
- [x] Edge lots are live: bare, dug (1, 2, 4) and foundation lots appear only where the intensity is low, in pack blocks that resolve on the real pack, with no foliage and RWG's decoration guarded out (unit tests; `find lot` in the real pack). *`UrbanityTest`, `EdgeLotsTest`; real pack: `assets.pack_resolution` 0 fallbacks, `find lot` an excavation 2 deep at intensity 0.22 (classic RWG) and 1 deep at 0.23 (RWG2 Continents) (D-M12-4, D-M12-5).*
- [x] Repairs are live: a repaired explosion's patch is the same across chunk borders, similar but not identical to what it replaces, and only in buildings and city ground (unit tests). *`RepairsTest`; `find repair` in both world types (D-M12-6).*
- [x] The M05R leftovers are fixed, each with a test. *`SewingTest` and the layout goldens (tertiary detours and margin roads), `LaneRelaxTest`, the terrain goldens (dithered banks) (D-M12-7 to D-M12-9).*
- [x] Every new option is pinned, ranged and documented (`ProfileOptionsDocsTest`); the layout version is raised. *`cityTaper`, `edgeLotChance`, `damageRepairChance`, `facadeContrast`; layout version 4 (D-M12-10).*
- [ ] On real-pack instance 5, seed 4242, RWG2 Continents and classic RWG: two fresh worlds per type print the same `city.real` hashes; B1 to B5 hold (D81's method); no cascade warning names this mod's code first; `city.walk` and `city.sewing` pass. *2026-11-02, jar `5d7377a` (M12 merged with M07, M08, M10 and M11): RWG2 Continents terrain `7dd4b58a…` in both fresh worlds, classic RWG `28e38a4e…` in both; B1, B2, B4 and B5 pass in all four (B4 worst p99.9 7.2 and 8.6 ms classic, 6.5 and 5.5 ms RWG2; B5 15.1 MB); B3 12.0 and 12.4 % on RWG2, 14.4 and 15.2 % on classic RWG, where `gtnh` alone already measures 15.0 % (M07 and M08) and the combined performance pass after wave 2 owns it. M12's own share on classic RWG, against `gtnh` at `b500967` in the same session: 14.5 against 14.2 %, terrain on city chunks 1,016 to 1,077 against 1,192 ms and the prefetcher 1,024 to 1,230 against 1,057 ms, inside the run-to-run spread. None of the 83, 58, 69 and 65 cascade traces has this mod's code first. `assets.pack_resolution` (0 fallbacks) and `assets.catalog` pass with the lot and repair blocks; `city.walk` stairs 100 % on both (a multibuilding's stairwell now counts for all its chunks) and `city.sewing` pass.*
- [x] Both alpha shot lists rerun with the final jar, with shots added for tapering, a construction lot, a repair and a style boundary, and every image checked. *2026-11-02, jar `5d7377a`: `reference/remote/shots/shots-20261102-161730-alpha-rwg2` (15 of 15) and `shots-20261102-162937-alpha-rwg` (13 of 14; the soft edge unsettled after 340 s in a forest of tall trees, taken anyway). Edges are low (2 to 3 floors beside the mountain), lots show bare clay and dirt with fenced pits, feather views pair sandstone with gray stone brick, the village view has no primary road beside the margin, and the margin banks step in ragged contours. Still open for review: the taper view stands inside the city on both world types, so it shows a street more than a skyline, and the tertiary roads beside a margin still pave the whole chunk in the style's street parts.*

## M06 to M12 · After the alpha

Scopes are written from S1 to S3 when they land. In outline:

| Milestone | Lane | Covers |
|---|---|---|
| M06 · Highways and railways | Layout | Highways, the intercity highway planner, railways, stations, subways and monorails over RWG terrain; highways detour around landmark cores or end before them, and shallow railways dip under a core and climb back (D31) ([0006](design/0006-build-around-other-worldgen.md)) |
| M07 · Scattered structures | Layout | Scattered structures and decorative stuff; scattered structures stay out of landmark cores and edge rings, which replace upstream's structure avoidance ([0006](design/0006-build-around-other-worldgen.md)); lakes, lava, dungeons and oil denied under their footprints; oil rigs only in RWG2's deep ocean, none on classic RWG, which has no oceans (D31) |
| M08 · GTNH layer | Integration | Loot at EV or below, pack mobs and spawners, GT ore-vein coexistence, other pack worldgen: the structure oracles under `compat` (Roguelike Dungeons, Witchery, LootGames, AE2, RWG2; the VillageNames oracle lands in M05), the Roguelike Dungeons steering mixin (which also keeps towers out of scattered footprints, D31), and the AE2 crater basin ([0006](design/0006-build-around-other-worldgen.md)) |
| M09 · Profiles and config | Core | Every profile option that survives, server-owner config, profile selection without a world-preset screen |
| M10 · Commands, API and editor | Core | Operator commands, the public API, the in-game part editor |
| M11 · Full asset catalog | Assets | Every bundled building, part, palette and style converted and checked in game, with the pack's variety in palettes, interiors and parks ([M11](#m11--full-asset-catalog)) |
| M12 · City edges and character | Layout, with Assets | Written in full [above](#m12--city-edges-and-character) |

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
