# Built-in profiles

The 17 profiles `ProfileSetup.initStandardProfiles` defines in code (`config/ProfileSetup.java:23-443`). Each starts from the class defaults in [profile-options.md](profile-options.md) and sets only the fields listed here. They are written to `config/lostcities/profiles/<name>.json` as read-only files on every start and then re-read ([profile-system.md](profile-system.md#startup-both-sides-at-mod-construction)). This catalog sits alongside the [profile system sheet](profile-system.md).

Text in the description, extra description and warning columns is the exact hard-coded English shown in the GUI. Icons are under `assets/lostcities/textures/gui/`.

## Identity

| Profile | Public | Description | Extra description | Warning | Icon |
|---|---|---|---|---|---|
| `default` | yes | `Default generation, common cities, explosions` (class default) | – | – | `icon_default.png` |
| `cavern` | yes | `This profile is meant for a cavern type world. There are lights in the building but the outside is very dark.` | `This is very hard. It's recommended you enable a bonus chest!` | `Use this in combination with the Lost Worlds 'caves' world type` | `icon_cavern.png` |
| `nodamage` | yes | `Like default but no explosion damage` | `Ruins and rubble are disabled and ravines are disabled in cities` | – | `icon_nodamage.png` |
| `floating` | yes | `Cities on floating islands` | `Note! No mineshafts or strongholds in this profile!` | `Preferably use this in combination with the Lost Worlds 'islands' world type` | `icon_floating.png` |
| `space` | yes | `Cities in floating glass bubbles` | `Note! No villages, mineshafts or strongholds in this profile!` | `Preferably use this in combination with the Lost Worlds 'spheres' world type` | `icon_space.png` |
| `biosphere_caves` | no | `Spheres in large caverns` | – | `Use this in combination with the Lost Worlds 'cavespheres' world type` | `icon_cavespheres.png` |
| `biosphere` | yes | `Jungles in big glass bubbles on a barren landscape` | – | `Preferably use this in combination with the Lost Worlds 'normal' world type` | `icon_biosphere.png` |
| `rarecities` | yes | `Cities are rare` | – | – | `icon_rarecities.png` |
| `onlycities` | yes | `The entire world is a city` | – | – | `icon_onlycities.png` |
| `tallbuildings` | yes | `Very tall buildings (performance heavy)` | – | – | `icon_tallbuildings.png` |
| `safe` | yes | `Safe mode: no spawners, lighting but no loot` | – | – | `icon_safe.png` |
| `ancient` | yes | `Ancient jungle city, vines and leafs, ruined buildings` | – | – | `icon_ancient.png` |
| `wasteland` | yes | `Wasteland, no water, bare land` | `This profile works best with Biomes O Plenty and the Wastify mod` | – | `icon_wasteland.png` |
| `atlantis` | yes | `Drowned cities, raised waterlevel (to 89)` | – | `Preferably use this in combination with the Lost Worlds 'atlantis' world type` | `icon_atlantis.png` |
| `bio_wasteland` | no | `Private wasteland for biospheres` | – | – | none |
| `void_outside` | no | `Private wasteland for space` | – | – | none |
| `largecities` | yes | none set: shows the default's `Default generation, common cities, explosions` (`PROF-18`) | – | – | `icon_default.png` |

Unused icon files: `icon_bubbles.png`, `icon_chisel.png`, `icon_realistic.png` (their profiles, `waterbubbles`, `chisel` and `realistic`, are commented out, as are `customized` and `water_empty`).

Several texts promise behavior no option implements at the pin: "ravines are disabled", "No mineshafts or strongholds", "No villages". They are carried over from older versions (`PROF-18`).

## Overrides

Values that differ from the class default. Float literals assigned to the `double` field `cityChance` keep their float rounding: `0.03f` is 0.029999999329447746, `0.7f` is 0.699999988079071, `0.9f` is 0.8999999761581421, `0.8f` is 0.800000011920929, `0.008f` is 0.00800000037997961.

| Profile | Overrides |
|---|---|
| `default` | none |
| `cavern` | `landscapeType` cavern; `horizon` 128; `fogRed`/`fogGreen`/`fogBlue` 0.0; `fogDensity` 0.02; `explosionChance` 0; `miniExplosionChance` 0; `generateLighting` true; `railwaysEnabled` false; `groundLevel` 40; `seaLevel` 32; level heights 44, 52, 60, 68, 76, 82, 90, 98 |
| `nodamage` | `explosionChance` 0; `miniExplosionChance` 0; `ruinChance` 0; `rubbleLayer` false |
| `floating` | `cityChance` 0.03f; `landscapeType` floating; `horizon` 0; `highwaySupports` false; `buildingMaxCellars` 1; `railwaysCanEnd` true; `railwaysEnabled` false; `railwayStationsEnabled` false; `highwayDistanceMask` 15; `groundLevel` 50; level heights 50, 56, 62, 68, 76, 84, 92, 100 |
| `space` | `landscapeType` space; `outsideProfile` `void_outside`; `spawnSphere` `<in>`; `horizon` 0; `citySphereChance` 0.9; `citySphereClearAbove` 8; `citySphereClearAboveUntilAir` true; `citySphereClearBelow` 8; `citySphereClearBelowUntilAir` true; `railwaysCanEnd` true; `railwaysEnabled` false; `railwayStationsEnabled` false; `highwayDistanceMask` 0; `bridgeSupports` false; `highwaySupports` false; `rubbleLayer` false; `groundLevel` 71 (same); `explosionChance` 0.0001; `miniExplosionChance` 0.001; `cityChance` 0.7f; `cityMaxRadius` 90; `cityThreshold` 0.05; level heights 60, 66, 72, 78, 86, 94, 100, 108; `buildingChance` 0.3 (same); `generateLighting` true |
| `biosphere_caves` | `landscapeType` cavernspheres; `horizon` 30; `monorailChance` 0.0; `outsideProfile` `cavern`; `outsideSurfaceVariation` 0.5; `spawnSphere` `<in>`; `explosionChance` 0.0; `miniExplosionChance` 0.01; `miniExplosionMinHeight` 60 (same); `miniExplosionMaxHeight` 75; `miniExplosionMinRadius` 5 (same); `miniExplosionMaxRadius` 10; `railwaysCanEnd` true; `railwaysEnabled` false; `railwayStationsEnabled` false; `highwayDistanceMask` 0; `ruinChance` 0.7; `ruinMinlevelPercent` 0.3; `ruinMaxlevelPercent` 0.8; `rubbleLayer` false; `citySphereChance` 0.5; `citySphereClearAbove` 30; `cityChance` 0.9f; `cityMinRadius` 50 (same); `cityMaxRadius` 65; `cityThreshold` 0.05; level heights 60, 66, 72, 78, 86, 94, 100, 108; `buildingChance` 0.3 (same); `generateLighting` true |
| `biosphere` | as `biosphere_caves` except: `landscapeType` spheres; `outsideProfile` `bio_wasteland`; `cityChance` 0.8f; public |
| `rarecities` | `cityChance` 0.001; `ruinChance` 0; `highwayRequiresTwoCities` false; `railwaysCanEnd` true |
| `onlycities` | `cityChance` 0.2; `cityMaxRadius` 256 |
| `tallbuildings` | `buildingMinFloors` 4; `buildingMinFloorsChance` 8; `buildingMaxFloorsChance` 15; `buildingMaxFloors` 19; `debrisToNearbyChunkFactor` 175; `explosionChance` 0.006; `explosionMaxHeight` 256; `explosionMaxRadius` 60; `explosionMinHeight` 130; `miniExplosionChance` 0.09; `miniExplosionMaxHeight` 256; `miniExplosionMaxRadius` 14; `miniExplosionMinRadius` 3; `ruinChance` 0.01 |
| `safe` | `generateSpawners` false; `generateLighting` true; `generateLoot` false |
| `ancient` | `randomLeafBlockThickness` 6; `randomLeafBlockChance` 0.05; `vineChance` 0.1; `explosionChance` 0; `miniExplosionChance` 0; `rubbleLayer` true (same); `rubbleDirtScale` 2.0; `rubbleLeaveScale` 2.0; `ruinChance` 0.9; `ruinMinlevelPercent` 0.0; `ruinMaxlevelPercent` 0.9 |
| `wasteland` | `vineChance` 0.003; `randomLeafBlockChance` 0.01; `rubbleLayer` true (same); `rubbleDirtScale` 2.0; `rubbleLeaveScale` 0.0; `ruinChance` 0.5; `ruinMinlevelPercent` 0.5; `ruinMaxlevelPercent` 0.9; `avoidWater` true; `avoidFoliage` true |
| `atlantis` | `seaLevel` 89; `ruinChance` 0.1 |
| `bio_wasteland` | `groundLevel` 71 (same); `avoidWater` true; `cityChance` 0.008f; `cityMinRadius` 30; `cityMaxRadius` 80; `vineChance` 0.0; `randomLeafBlockChance` 0.0; `rubbleLayer` true (same); `rubbleDirtScale` 2.0; `rubbleLeaveScale` 0.0; `buildingMaxCellars` 1; `ruinChance` 1.0; `ruinMinlevelPercent` 0.1; `ruinMaxlevelPercent` 0.4; `avoidFoliage` true |
| `void_outside` | `groundLevel` 71 (same); `avoidWater` true; `cityChance` 0; `cityMinRadius` 30; `cityMaxRadius` 80; `vineChance` 0.0; `railwayStationsEnabled` false; `railwaysEnabled` false; `highwayDistanceMask` 0; `bridgeSupports` false; `highwaySupports` false; `randomLeafBlockChance` 0.0; `rubbleLayer` true (same); `rubbleDirtScale` 2.0; `rubbleLeaveScale` 0.0; `buildingMaxCellars` 1; `ruinChance` 1.0; `ruinMinlevelPercent` 0.1; `ruinMaxlevelPercent` 0.4; `avoidFoliage` true |
| `largecities` | `cityChance` -1 (perlin mode); `cityPerlinScale` 7.0; `cityPerlinOffset` 0.2; `cityPerlinInnerScale` 0.1 (same); `cityThreshold` 0.1; `cityStyleThreshold` 0.4; `cityStyleAlternative` `citystyle_border`; `generateLighting` true; `buildingMaxFloors` 9; `buildingMaxFloorsChance` 7; `buildingChance` 0.4 |

## Relationships between built-ins

- `space` uses `void_outside` outside its spheres; `biosphere` uses `bio_wasteland`; `biosphere_caves` uses the public `cavern` profile. The outside profile supplies `cityChance`, the radius range, `groundLevel`, `seaLevel` and the city level for chunks outside spheres ([profile-selection.md](profile-selection.md#outside-profile)).
- The mod config maps the Lost City dimension `lostcities:lostcity` to `biosphere` and `lostworlds:abyss` to `biosphere_caves` by default ([profile-selection.md](profile-selection.md)).
- The three private profiles exist only to serve as outside profiles or dimension profiles; the GUI hides them (09-client-ui).
- `cavern` is also the profile given to the Nether when the overworld profile sets `generateNether` (`PROF-08`).
- Every built-in uses world style `standard`. `standard_everywhere` is reachable only through a user profile ([world-styles.md](world-styles.md)).
