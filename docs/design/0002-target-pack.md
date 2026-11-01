# 0002 · Target pack: GTNH 2.9.0

- Status: Accepted
- Gate: none (foundation)
- Amends: none
- Date: 2026-11-01

## Context

Integration has to be written against what the pack actually ships (`gtnh-integration-research`). GTNH 2.9.0 is the current stable release (manifest dated 2026-10-31). The owner plays it on their own instance and server. GregGuns and TrainHorizons pinned the earlier 2.9.0-RC-2.

## Decision

1. **The target is GTNH 2.9.0**, as listed in the release manifest `releases/manifests/2.9.0.json` of [GTNewHorizons/DreamAssemblerXXL](https://github.com/GTNewHorizons/DreamAssemblerXXL).
2. **Every dependency is pinned to the version 2.9.0 ships**, with the `:dev` classifier from the GTNH Maven. Optional integrations are `compileOnly`, and runtime-only development helpers are `devOnlyNonPublishable`. No mod is shaded. The versions this mod is expected to touch:

   | Mod | 2.9.0 version |
   |---|---|
   | GT5-Unofficial | 5.09.54.265 |
   | GTNHLib | 0.11.54 |
   | NotEnoughItems | 2.8.171-GTNH |
   | Hodgepodge | 2.7.221 |
   | Angelica | 2.2.43 |
   | Realistic World Gen | alpha-1.5.3 |
   | Chisel | 2.17.34-GTNH |
   | ChiselTones | 1.2.0-GTNH |
   | Ztones | 2.2.2 |
   | BiblioCraft | 1.11.7 |
   | Carpenter's Blocks | 3.7.3-GTNH |
   | Et Futurum Requiem | 2.6.65-GTNH |
   | Railcraft | 9.17.36 |
   | Galacticraft | 3.4.37-GTNH |
   | Twilight Forest | 2.7.45 |
   | BetterQuesting | 3.8.92-GTNH |
   | EnhancedLootBags | 1.3.5 |
   | ServerUtilities | 2.4.22 |
   | StructureLib | 1.4.47 |
   | ModularUI2 | 2.3.94-1.7.10 |
   | NewHorizonsCoreMod | 2.9.99 |

   This table lists candidates, not commitments. Which mods are actually integrated is settled in [docs/new-spec/](../new-spec/README.md).
3. **The extracted pack lives in `reference/gtnh/2.9.0/`** (git-ignored), with a `MOD-INDEX.md` of every mod's version, jar, mod IDs, license and source tag. Integration code reads the source at that tag first and decompiles only as a fallback.
4. **The real-pack check uses the same version.** Alpha and beta gates run the release jar on a GTNH 2.9.0 dedicated server and client.

## Consequences

- A pack bump is a deliberate change: update this record's table, the pins in `dependencies.gradle`, the reference pack, and re-run the real-pack gate.
- GregGuns' and TrainHorizons' RC-2 reference packs are not reused as they are, because versions differ, for example GT5-Unofficial `.205` against `.265`.

## Alternatives considered

- **2.9.0-RC-2:** shared tooling with the other mods, but older than what the owner plays.
- **Latest daily:** closest to 2.10, but the APIs churn under the integration work.

## Still open

None.
