# 0002 · Target pack: GTNH 2.9.1

- Status: Accepted
- Gate: none (foundation)
- Amends: none
- Date: 2026-11-01; target moved to 2.9.1 on 2026-11-22 (D-291-1)

## Context

Integration has to be written against what the pack actually ships (`gtnh-integration-research`). GTNH 2.9.0 is the current stable release (manifest dated 2026-10-31). The owner plays it on their own instance and server. GregGuns and TrainHorizons pinned the earlier 2.9.0-RC-2.

## Decision

1. **The target is GTNH 2.9.1** (from 0.4.2-beta; 2.9.0 up to 0.4.1-beta), as listed in the release manifest `releases/manifests/2.9.1.json` of [GTNewHorizons/DreamAssemblerXXL](https://github.com/GTNewHorizons/DreamAssemblerXXL).
2. **Every dependency is pinned to the version the target ships**, with the `:dev` classifier from the GTNH Maven. Optional integrations are `compileOnly`, and runtime-only development helpers are `devOnlyNonPublishable`. No mod is shaded. The versions this mod is expected to touch:

   | Mod | 2.9.0 version | 2.9.1 version |
   |---|---|---|
   | GT5-Unofficial | 5.09.54.265 | 5.09.54.275 |
   | GTNHLib | 0.11.54 | 0.11.58 |
   | NotEnoughItems | 2.8.171-GTNH | 2.8.175-GTNH |
   | Hodgepodge | 2.7.221 | 2.7.226 |
   | Angelica | 2.2.43 | 2.2.43 |
   | Realistic World Gen | alpha-1.5.3 | alpha-1.5.3 |
   | Chisel | 2.17.34-GTNH | 2.17.37-GTNH |
   | ChiselTones | 1.2.0-GTNH | 1.2.0-GTNH |
   | Ztones | 2.2.2 | 2.2.2 |
   | BiblioCraft | 1.11.7 | 1.11.7 |
   | Carpenter's Blocks | 3.7.3-GTNH | 3.7.3-GTNH |
   | Et Futurum Requiem | 2.6.65-GTNH | 2.6.66-GTNH |
   | Railcraft | 9.17.36 | 9.17.37 |
   | Galacticraft | 3.4.37-GTNH | 3.4.39-GTNH |
   | Twilight Forest | 2.7.45 | 2.7.47 |
   | BetterQuesting | 3.8.92-GTNH | 3.8.94-GTNH |
   | EnhancedLootBags | 1.3.5 | 1.3.5 |
   | ServerUtilities | 2.4.22 | 2.5.4 |
   | StructureLib | 1.4.47 | 1.4.47 |
   | ModularUI2 | 2.3.94-1.7.10 | 2.3.96-1.7.10 |
   | NewHorizonsCoreMod | 2.9.99 | 2.9.105 |

   This table lists candidates, not commitments. Which mods are actually integrated is settled in [docs/new-spec/](../new-spec/README.md).
3. **The extracted pack lives in `reference/gtnh/2.9.0/`** (git-ignored), with a `MOD-INDEX.md` of every mod's version, jar, mod IDs, license and source tag. Integration code reads the source at that tag first and decompiles only as a fallback.
4. **The real-pack check uses the same version.** Alpha and beta gates ran the release jar on a GTNH 2.9.0 dedicated server and client; from 0.4.2-beta the real-pack checks run on GTNH 2.9.1.

## Consequences

- **The 2.9.1 bump (D-291-1).** RWG2 2.0.6 → 2.0.9 moved its config into GTNHLib config classes (`ContinentalConfig`, `MigrationConfig`, `SettingsConfig`), which made 0.4.1-beta throw `NoSuchFieldError` on RWG2 Continents. `compat.rwg2.Rwg2Config` reads each value by reflection, trying 2.0.9's class first and then `ConfigRWG`, so one jar runs on both. RWG2's junction landmarks moved to `RiverJunctionLandmark`; the oracle still covers them. The 2.9.1 server pack ships `level-type=rwg2_continents`; 2.9.0's shipped `rwg`.

- A pack bump is a deliberate change: update this record's table, the pins in `dependencies.gradle`, the reference pack, and re-run the real-pack gate.
- GregGuns' and TrainHorizons' RC-2 reference packs are not reused as they are, because versions differ, for example GT5-Unofficial `.205` against `.265`.

## Alternatives considered

- **2.9.0-RC-2:** shared tooling with the other mods, but older than what the owner plays.
- **Latest daily:** closest to 2.10, but the APIs churn under the integration work.

## Still open

None.
