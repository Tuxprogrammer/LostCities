# Upstream file coverage

Every file on the upstream `1.20` branch at `f18a094d` (`git ls-tree -r --name-only 1.20`), and the area and sheet(s) of this spec that cover it. The area is the one whose Coverage section counts the file as in scope; some files are in scope for two areas. For Java, the sheets are those of the owning area whose Identity field names the class, and **Also named in** lists other areas whose sheets name it in their Identity field. For resources, the sheets are those that document the file's contents, and **Also named in** lists other areas that read or cite it. The 207 parts and the other bundled JSON assets are grouped by folder with counts.

## Totals

| | Files |
|---|---|
| Files at the pin | 612 |
| In scope for the spec | 596 |
| · Java under `src/main/java/mcjty/lostcities/` | 230 |
| · Java under `src/api/java/` | 1 |
| · Resources under `src/main/resources/` | 359 |
| · Resources under `src/generated/resources/` | 6 |
| Named by at least one sheet | 594 |
| Documented only in an area index, no sheet | 1 (`DimensionDictionary.java`) |
| **Uncovered** | 1 (`pack.mcmeta`) |
| Build and meta files, not behavior | 16 |

In-scope files per area, as each area's Coverage section counts them. The areas overlap on 42 files (27 Java, 15 resources), so the column sums to 637, not 595.

| Area | Java | Resources | Total |
|---|---|---|---|
| `01-core` | 67 (including `src/api`) | 2 | 69 |
| `02-profiles-dimensions` | 19 | 6 | 25 |
| `03-terrain` | 17 | 6 | 23 |
| `04-city-layout` | 35 | 0 | 35 |
| `05-assets` | 69 | 322 | 391 |
| `06-highways-railways` | 28 | 0 | 28 |
| `07-scattered` | 7 | 8 | 15 |
| `08-loot-mobs-blocks` | 2 | 11 | 13 |
| `09-client-ui` | 14 | 24 | 38 |

Overlaps: all of `worldgen/lost/cityassets/`, `worldgen/lost/regassets/` and `regassets/data/` are counted by `05-assets` and, for 26 of them, also by the area that owns their behavior (`02`, `04`, `06`, `07`); `api/RailChunkType.java` by `01-core` and `06-highways-railways`; the two biome modifiers by `02` and `03`; the two world styles by `02` and `05`; the scattered and stuff assets by `05` and `07`; the three conditions by `05` and `08`.

## Uncovered

| File | What it is | Note |
|---|---|---|
| `src/main/resources/pack.mcmeta` | Pack metadata: `pack_format` 15, description `Resources used for The Lost Cities` | No sheet or area index names it. |
| `src/api/java/ivorius/reccomplex/dimensions/DimensionDictionary.java` | Stale third-party Recurrent Complex API class, not compiled by the 1.20 build | No sheet names it; its contents are recorded in [01-core Key facts](01-core/index.md#key-facts) and it is counted in `01-core`'s Coverage. |

## Java

Paths are relative to `src/main/java/mcjty/lostcities/`.

### Package root (3)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `LostCities.java` | 01-core | [lifecycle](01-core/lifecycle.md) | |
| `LostCitiesImp.java` | 01-core | [api](01-core/api.md), [lifecycle](01-core/lifecycle.md) | |
| `LostCitiesPreImp.java` | 01-core | [api](01-core/api.md), [lifecycle](01-core/lifecycle.md) | |

### `api/` (20)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `IChunkPrimerFactory.java` | 01-core | [api](01-core/api.md) | |
| `ILostChunkInfo.java` | 01-core | [api](01-core/api.md) | 04-city-layout |
| `ILostCities.java` | 01-core | [api](01-core/api.md) | 04-city-layout, 05-assets |
| `ILostCitiesPre.java` | 01-core | [api](01-core/api.md) | |
| `ILostCityAsset.java` | 01-core | [api](01-core/api.md) | 05-assets |
| `ILostCityAssetRegistry.java` | 01-core | [api](01-core/api.md) | 05-assets |
| `ILostCityBuilding.java` | 01-core | [api](01-core/api.md) | 05-assets |
| `ILostCityCityStyle.java` | 01-core | [api](01-core/api.md) | 05-assets |
| `ILostCityInfo.java` | 01-core | [api](01-core/api.md) | |
| `ILostCityInformation.java` | 01-core | [api](01-core/api.md) | |
| `ILostCityMultiBuilding.java` | 01-core | [api](01-core/api.md) | 05-assets |
| `ILostCityProfile.java` | 01-core | [api](01-core/api.md) | 02-profiles-dimensions |
| `ILostCityProfileSetup.java` | 01-core | [api](01-core/api.md) | 02-profiles-dimensions |
| `ILostExplosion.java` | 01-core | [api](01-core/api.md) | 03-terrain |
| `ILostSphere.java` | 01-core | [api](01-core/api.md) | 03-terrain |
| `ILostWorldsChunkGenerator.java` | 01-core | [api](01-core/api.md) | 03-terrain |
| `LostChunkCharacteristics.java` | 01-core | [api](01-core/api.md) | 04-city-layout |
| `LostCityEvent.java` | 01-core | [api](01-core/api.md) | 04-city-layout |
| `MultiPos.java` | 01-core | [api](01-core/api.md) | 04-city-layout |
| `RailChunkType.java` | 01-core, 06-highways-railways | [api](01-core/api.md), [railway-layout](06-highways-railways/railway-layout.md), [railway-parts](06-highways-railways/railway-parts.md) | |

### `commands/` (14)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `CommandCreateBuilding.java` | 01-core | [commands](01-core/commands.md) | |
| `CommandCreatePart.java` | 01-core | [commands](01-core/commands.md) | |
| `CommandDebug.java` | 01-core | [commands](01-core/commands.md) | |
| `CommandEditPart.java` | 01-core | [commands](01-core/commands.md) | |
| `CommandExportPart.java` | 01-core | [commands](01-core/commands.md) | |
| `CommandListParts.java` | 01-core | [commands](01-core/commands.md) | |
| `CommandLocate.java` | 01-core | [commands](01-core/commands.md) | |
| `CommandLocatePart.java` | 01-core | [commands](01-core/commands.md) | |
| `CommandMap.java` | 01-core | [commands](01-core/commands.md) | |
| `CommandResumeEdit.java` | 01-core | [commands](01-core/commands.md) | |
| `CommandSaveProfile.java` | 01-core | [commands](01-core/commands.md) | |
| `CommandStats.java` | 01-core | [commands](01-core/commands.md) | |
| `CommandTestFill.java` | 01-core | [commands](01-core/commands.md) | |
| `ModCommands.java` | 01-core | [commands](01-core/commands.md) | |

### `config/` (9)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `BiomeSelectionStrategy.java` | 02-profiles-dimensions | [profile-system](02-profiles-dimensions/profile-system.md), [world-styles](02-profiles-dimensions/world-styles.md) | |
| `Configuration.java` | 02-profiles-dimensions | [profile-system](02-profiles-dimensions/profile-system.md) | |
| `HighwayGenerationMode.java` | 02-profiles-dimensions | [profile-system](02-profiles-dimensions/profile-system.md) | 03-terrain, 06-highways-railways |
| `LandscapeType.java` | 02-profiles-dimensions | [landscape-types](02-profiles-dimensions/landscape-types.md), [profile-system](02-profiles-dimensions/profile-system.md) | 03-terrain |
| `LostCityProfile.java` | 02-profiles-dimensions | [landscape-types](02-profiles-dimensions/landscape-types.md), [profile-system](02-profiles-dimensions/profile-system.md) | 03-terrain, 09-client-ui |
| `LostCityProfileSetupImp.java` | 02-profiles-dimensions | [profile-system](02-profiles-dimensions/profile-system.md) | |
| `MultiBuildingStreetConflict.java` | 02-profiles-dimensions | [profile-system](02-profiles-dimensions/profile-system.md) | |
| `ProfileSetup.java` | 02-profiles-dimensions | [profile-system](02-profiles-dimensions/profile-system.md) | |
| `StreetGenerationMode.java` | 02-profiles-dimensions | [profile-system](02-profiles-dimensions/profile-system.md) | 03-terrain, 04-city-layout |

### `datagen/` (2)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `DataGenerators.java` | 01-core | [lifecycle](01-core/lifecycle.md) | 08-loot-mobs-blocks |
| `LCBlockTags.java` | 08-loot-mobs-blocks | [block-tags](08-loot-mobs-blocks/block-tags.md) | |

### `editor/` (3)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `EditModeData.java` | 01-core | [part-editor](01-core/part-editor.md) | |
| `Editor.java` | 01-core | [part-editor](01-core/part-editor.md) | |
| `EditorInfo.java` | 01-core | [part-editor](01-core/part-editor.md) | |

### `gui/` (3)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `GuiLCConfig.java` | 09-client-ui | [preview](09-client-ui/preview.md), [profile-screen](09-client-ui/profile-screen.md) | 03-terrain |
| `LostCitySetup.java` | 09-client-ui | [profile-screen](09-client-ui/profile-screen.md), [profile-selection-state](09-client-ui/profile-selection-state.md) | |
| `NullDimensionInfo.java` | 09-client-ui | [preview](09-client-ui/preview.md) | 02-profiles-dimensions |

### `gui/elements/` (9)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `BooleanElement.java` | 09-client-ui | [config-widgets](09-client-ui/config-widgets.md) | |
| `ButtonExt.java` | 09-client-ui | [config-widgets](09-client-ui/config-widgets.md) | |
| `DoubleElement.java` | 09-client-ui | [config-widgets](09-client-ui/config-widgets.md) | |
| `FloatElement.java` | 09-client-ui | [config-widgets](09-client-ui/config-widgets.md) | |
| `GuiElement.java` | 09-client-ui | [config-widgets](09-client-ui/config-widgets.md) | |
| `IntElement.java` | 09-client-ui | [config-widgets](09-client-ui/config-widgets.md) | |
| `ProfileConfigElement.java` | 09-client-ui | [config-widgets](09-client-ui/config-widgets.md) | |
| `TextExt.java` | 09-client-ui | [config-widgets](09-client-ui/config-widgets.md) | |
| `WidgetElement.java` | 09-client-ui | [config-widgets](09-client-ui/config-widgets.md) | |

### `network/` (3)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `PacketHandler.java` | 01-core | [network](01-core/network.md) | |
| `PacketRequestProfile.java` | 01-core | [network](01-core/network.md) | |
| `PacketReturnProfileToClient.java` | 01-core | [network](01-core/network.md) | |

### `playerdata/` (3)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `PlayerProperties.java` | 01-core | [spawn-selection](01-core/spawn-selection.md) | |
| `PlayerSpawnSet.java` | 01-core | [spawn-selection](01-core/spawn-selection.md) | |
| `PropertiesDispatcher.java` | 01-core | [spawn-selection](01-core/spawn-selection.md) | |

### `setup/` (9)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `ClientEventHandlers.java` | 09-client-ui | [world-creation-hook](09-client-ui/world-creation-hook.md) | 03-terrain |
| `ClientSetup.java` | 09-client-ui | [world-creation-hook](09-client-ui/world-creation-hook.md) | 01-core |
| `Config.java` | 01-core | [bed-teleporter](01-core/bed-teleporter.md), [config](01-core/config.md), [lifecycle](01-core/lifecycle.md), [utilities](01-core/utilities.md) | 02-profiles-dimensions, 03-terrain, 04-city-layout, 05-assets, 07-scattered |
| `CustomRegistries.java` | 05-assets | [loading-and-lookup](05-assets/loading-and-lookup.md) | |
| `ForgeEventHandlers.java` | 01-core | [bed-teleporter](01-core/bed-teleporter.md), [lifecycle](01-core/lifecycle.md), [spawn-selection](01-core/spawn-selection.md) | 03-terrain |
| `ModSetup.java` | 01-core | [lifecycle](01-core/lifecycle.md) | |
| `Registration.java` | 01-core | [bed-teleporter](01-core/bed-teleporter.md), [lifecycle](01-core/lifecycle.md) | 02-profiles-dimensions |
| `SpawnBuildingPrefilter.java` | 01-core | [spawn-selection](01-core/spawn-selection.md) | |
| `TerrainEventHandlers.java` | 03-terrain | [generation-pipeline](03-terrain/generation-pipeline.md) | |

### `varia/` (14)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `ChunkCoord.java` | 01-core | [utilities](01-core/utilities.md) | |
| `ComponentFactory.java` | 01-core | [utilities](01-core/utilities.md) | |
| `Counter.java` | 01-core | [utilities](01-core/utilities.md) | |
| `CustomTeleporter.java` | 01-core | [bed-teleporter](01-core/bed-teleporter.md) | |
| `GeometryTools.java` | 01-core | [utilities](01-core/utilities.md) | |
| `NoiseGeneratorPerlin.java` | 01-core | [noise-and-random](01-core/noise-and-random.md) | |
| `NoiseGeneratorSimplex.java` | 01-core | [noise-and-random](01-core/noise-and-random.md) | |
| `PerlinNoiseGenerator14.java` | 01-core | [noise-and-random](01-core/noise-and-random.md) | 06-highways-railways |
| `QualityRandom.java` | 01-core | [noise-and-random](01-core/noise-and-random.md) | |
| `Statistics.java` | 01-core | [utilities](01-core/utilities.md) | 03-terrain |
| `TimedCache.java` | 01-core | [utilities](01-core/utilities.md) | 03-terrain, 04-city-layout |
| `TodoQueue.java` | 01-core | [utilities](01-core/utilities.md) | 03-terrain |
| `Tools.java` | 01-core | [utilities](01-core/utilities.md) | 05-assets, 08-loot-mobs-blocks |
| `WorldTools.java` | 01-core | [utilities](01-core/utilities.md) | |

### `worldgen/` (16)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `ChunkDriver.java` | 03-terrain | [chunk-driver-and-parts](03-terrain/chunk-driver-and-parts.md), [concurrency-caches-determinism](03-terrain/concurrency-caches-determinism.md), [generation-pipeline](03-terrain/generation-pipeline.md) | |
| `ChunkFixer.java` | 03-terrain | [generation-pipeline](03-terrain/generation-pipeline.md), [post-processing](03-terrain/post-processing.md) | 08-loot-mobs-blocks |
| `ChunkHeightmap.java` | 03-terrain | [heightmap-sampling](03-terrain/heightmap-sampling.md) | |
| `DefaultDimensionInfo.java` | 02-profiles-dimensions | [profile-selection](02-profiles-dimensions/profile-selection.md) | 03-terrain, 06-highways-railways |
| `ErrorLogger.java` | 03-terrain | [concurrency-caches-determinism](03-terrain/concurrency-caches-determinism.md), [generation-pipeline](03-terrain/generation-pipeline.md) | |
| `GenerationContext.java` | 03-terrain | [chunk-driver-and-parts](03-terrain/chunk-driver-and-parts.md), [concurrency-caches-determinism](03-terrain/concurrency-caches-determinism.md), [generation-pipeline](03-terrain/generation-pipeline.md) | |
| `GlobalTodo.java` | 03-terrain | [concurrency-caches-determinism](03-terrain/concurrency-caches-determinism.md), [post-processing](03-terrain/post-processing.md) | |
| `HeightGenOpt.java` | 03-terrain | [heightmap-sampling](03-terrain/heightmap-sampling.md) | |
| `IDimensionInfo.java` | 02-profiles-dimensions | [profile-selection](02-profiles-dimensions/profile-selection.md) | 03-terrain, 09-client-ui |
| `LostCityFeature.java` | 03-terrain | [concurrency-caches-determinism](03-terrain/concurrency-caches-determinism.md), [generation-pipeline](03-terrain/generation-pipeline.md) | 02-profiles-dimensions |
| `LostCityHighwayData.java` | 06-highways-railways | [planning-service-and-persistence](06-highways-railways/planning-service-and-persistence.md) | |
| `LostCitySphereFeature.java` | 03-terrain | [city-spheres-and-landscapes](03-terrain/city-spheres-and-landscapes.md), [generation-pipeline](03-terrain/generation-pipeline.md) | 02-profiles-dimensions |
| `LostCityTerrainFeature.java` | 03-terrain | [building-and-street-writing](03-terrain/building-and-street-writing.md), [chunk-driver-and-parts](03-terrain/chunk-driver-and-parts.md), [city-chunk-terrain](03-terrain/city-chunk-terrain.md), [city-spheres-and-landscapes](03-terrain/city-spheres-and-landscapes.md), [concurrency-caches-determinism](03-terrain/concurrency-caches-determinism.md), [damage-ruins-rubble](03-terrain/damage-ruins-rubble.md), [generation-pipeline](03-terrain/generation-pipeline.md), [heightmap-sampling](03-terrain/heightmap-sampling.md), [post-processing](03-terrain/post-processing.md), [terrain-blend](03-terrain/terrain-blend.md) | 04-city-layout, 05-assets, 08-loot-mobs-blocks |
| `LostCityWorldGenData.java` | 03-terrain | [saved-data](03-terrain/saved-data.md) | 06-highways-railways |
| `LostTags.java` | 08-loot-mobs-blocks | [block-tags](08-loot-mobs-blocks/block-tags.md) | |
| `NoiseChunkOpt.java` | 03-terrain | [heightmap-sampling](03-terrain/heightmap-sampling.md) | |

### `worldgen/gen/` (10)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `Bridges.java` | 04-city-layout | [bridges](04-city-layout/bridges.md) | |
| `Corridors.java` | 04-city-layout | [corridors](04-city-layout/corridors.md) | |
| `Doors.java` | 04-city-layout | [doors-and-connections](04-city-layout/doors-and-connections.md) | |
| `Highways.java` | 06-highways-railways | [highway-parts](06-highways-railways/highway-parts.md) | |
| `Monorails.java` | 06-highways-railways | [monorails](06-highways-railways/monorails.md) | |
| `Railways.java` | 06-highways-railways | [railway-parts](06-highways-railways/railway-parts.md) | |
| `Scattered.java` | 07-scattered | [scattered-structures](07-scattered/scattered-structures.md) | 05-assets, 06-highways-railways |
| `Spheres.java` | 03-terrain | [city-spheres-and-landscapes](03-terrain/city-spheres-and-landscapes.md) | 02-profiles-dimensions |
| `Stuff.java` | 07-scattered | [stuff](07-scattered/stuff.md) | |
| `Supports.java` | 04-city-layout | [bridges](04-city-layout/bridges.md) | |

### `worldgen/highway/` (17)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `ApproximateCityPotential.java` | 06-highways-railways | [city-potential](06-highways-railways/city-potential.md) | |
| `BoundedFutureCache.java` | 06-highways-railways | [planning-service-and-persistence](06-highways-railways/planning-service-and-persistence.md) | |
| `CityPotential.java` | 06-highways-railways | [city-potential](06-highways-railways/city-potential.md), [intercity-planner](06-highways-railways/intercity-planner.md) | |
| `HighwayAxis.java` | 06-highways-railways | [highway-occupancy](06-highways-railways/highway-occupancy.md), [intercity-planner](06-highways-railways/intercity-planner.md) | |
| `HighwayCityLevelSource.java` | 06-highways-railways | [city-potential](06-highways-railways/city-potential.md) | |
| `HighwayConnectionKey.java` | 06-highways-railways | [intercity-planner](06-highways-railways/intercity-planner.md) | |
| `HighwayHub.java` | 06-highways-railways | [intercity-planner](06-highways-railways/intercity-planner.md) | |
| `HighwayHubPersistence.java` | 06-highways-railways | [intercity-planner](06-highways-railways/intercity-planner.md), [planning-service-and-persistence](06-highways-railways/planning-service-and-persistence.md) | |
| `HighwayInfo.java` | 06-highways-railways | [highway-occupancy](06-highways-railways/highway-occupancy.md), [intercity-planner](06-highways-railways/intercity-planner.md) | 04-city-layout |
| `HighwayLevelSource.java` | 06-highways-railways | [city-potential](06-highways-railways/city-potential.md), [intercity-planner](06-highways-railways/intercity-planner.md) | |
| `HighwayPlannerSettings.java` | 06-highways-railways | [intercity-planner](06-highways-railways/intercity-planner.md) | |
| `HighwayPlanningService.java` | 06-highways-railways | [planning-service-and-persistence](06-highways-railways/planning-service-and-persistence.md) | |
| `HighwayRoute.java` | 06-highways-railways | [intercity-planner](06-highways-railways/intercity-planner.md) | |
| `HighwaySector.java` | 06-highways-railways | [intercity-planner](06-highways-railways/intercity-planner.md) | |
| `HighwaySegment.java` | 06-highways-railways | [intercity-planner](06-highways-railways/intercity-planner.md) | |
| `HubKey.java` | 06-highways-railways | [intercity-planner](06-highways-railways/intercity-planner.md) | |
| `IntercityHighwayPlanner.java` | 06-highways-railways | [intercity-planner](06-highways-railways/intercity-planner.md) | |

### `worldgen/lost/` (14)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `BiomeInfo.java` | 02-profiles-dimensions | [world-styles](02-profiles-dimensions/world-styles.md) | |
| `BuildingInfo.java` | 04-city-layout | [bridges](04-city-layout/bridges.md), [building-info](04-city-layout/building-info.md), [city-detection](04-city-layout/city-detection.md), [city-level](04-city-layout/city-level.md), [corridors](04-city-layout/corridors.md), [streets-parks-stairs](04-city-layout/streets-parks-stairs.md), [transforms](04-city-layout/transforms.md) | 03-terrain, 05-assets, 06-highways-railways, 07-scattered, 08-loot-mobs-blocks |
| `City.java` | 04-city-layout | [city-detection](04-city-layout/city-detection.md), [city-settings](04-city-layout/city-settings.md), [city-styles](04-city-layout/city-styles.md), [corridors](04-city-layout/corridors.md), [multibuildings](04-city-layout/multibuildings.md), [predefined-content](04-city-layout/predefined-content.md), [street-planner](04-city-layout/street-planner.md), [streets-parks-stairs](04-city-layout/streets-parks-stairs.md) | 01-core, 02-profiles-dimensions, 03-terrain, 07-scattered |
| `CityRarityMap.java` | 04-city-layout | [city-detection](04-city-layout/city-detection.md) | |
| `CitySphere.java` | 03-terrain | [city-spheres-and-landscapes](03-terrain/city-spheres-and-landscapes.md), [concurrency-caches-determinism](03-terrain/concurrency-caches-determinism.md) | 06-highways-railways |
| `DamageArea.java` | 03-terrain | [damage-ruins-rubble](03-terrain/damage-ruins-rubble.md) | |
| `Direction.java` | 04-city-layout | [transforms](04-city-layout/transforms.md) | |
| `Explosion.java` | 03-terrain | [damage-ruins-rubble](03-terrain/damage-ruins-rubble.md) | |
| `Highway.java` | 06-highways-railways | [highway-occupancy](06-highways-railways/highway-occupancy.md), [legacy-highways](06-highways-railways/legacy-highways.md) | |
| `MultiChunk.java` | 04-city-layout | [chunk-characteristics](04-city-layout/chunk-characteristics.md), [multibuildings](04-city-layout/multibuildings.md) | |
| `Orientation.java` | 04-city-layout | [transforms](04-city-layout/transforms.md) | |
| `Railway.java` | 06-highways-railways | [railway-layout](06-highways-railways/railway-layout.md) | |
| `StructureAvoidance.java` | 07-scattered | [structure-avoidance](07-scattered/structure-avoidance.md) | 04-city-layout |
| `Transform.java` | 04-city-layout | [transforms](04-city-layout/transforms.md) | 05-assets |

### `worldgen/lost/cityassets/` (19)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `AssetRegistries.java` | 05-assets | [loading-and-lookup](05-assets/loading-and-lookup.md) | 04-city-layout, 07-scattered |
| `Building.java` | 05-assets | [buildings-and-multibuildings](05-assets/buildings-and-multibuildings.md), [conditions](05-assets/conditions.md) | 03-terrain, 04-city-layout |
| `BuildingPart.java` | 05-assets | [parts-and-placement](05-assets/parts-and-placement.md) | |
| `CityStyle.java` | 05-assets | [styles-and-city-styles](05-assets/styles-and-city-styles.md) | |
| `CompiledPalette.java` | 05-assets | [palettes-and-variants](05-assets/palettes-and-variants.md) | |
| `Condition.java` | 05-assets | [conditions](05-assets/conditions.md) | 08-loot-mobs-blocks |
| `ConditionContext.java` | 05-assets | [conditions](05-assets/conditions.md) | 08-loot-mobs-blocks |
| `EffectiveCitySettings.java` | 02-profiles-dimensions, 05-assets | [effective-city-settings](02-profiles-dimensions/effective-city-settings.md), [styles-and-city-styles](05-assets/styles-and-city-styles.md) | 04-city-layout |
| `IBuildingPart.java` | 05-assets | [parts-and-placement](05-assets/parts-and-placement.md) | 03-terrain |
| `MultiBuilding.java` | 05-assets | [buildings-and-multibuildings](05-assets/buildings-and-multibuildings.md) | |
| `Palette.java` | 05-assets | [palettes-and-variants](05-assets/palettes-and-variants.md) | 03-terrain, 08-loot-mobs-blocks |
| `PredefinedCity.java` | 04-city-layout, 05-assets | [predefined-content](04-city-layout/predefined-content.md), [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md) | |
| `PredefinedSphere.java` | 04-city-layout, 05-assets | [predefined-content](04-city-layout/predefined-content.md), [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md) | |
| `RegistryAssetRegistry.java` | 05-assets | [loading-and-lookup](05-assets/loading-and-lookup.md) | |
| `ScatteredBuilding.java` | 05-assets, 07-scattered | [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md), [scattered-structures](07-scattered/scattered-structures.md) | |
| `StuffObject.java` | 05-assets, 07-scattered | [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md), [stuff](07-scattered/stuff.md) | |
| `Style.java` | 05-assets | [palettes-and-variants](05-assets/palettes-and-variants.md), [styles-and-city-styles](05-assets/styles-and-city-styles.md) | 04-city-layout |
| `Variant.java` | 05-assets | [palettes-and-variants](05-assets/palettes-and-variants.md) | |
| `WorldStyle.java` | 02-profiles-dimensions, 05-assets | [world-styles](02-profiles-dimensions/world-styles.md), [world-styles](05-assets/world-styles.md) | 04-city-layout, 06-highways-railways |

### `worldgen/lost/regassets/` (14)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `BuildingPartRE.java` | 05-assets | [parts-and-placement](05-assets/parts-and-placement.md) | |
| `BuildingRE.java` | 05-assets | [buildings-and-multibuildings](05-assets/buildings-and-multibuildings.md) | |
| `CityStyleRE.java` | 05-assets | [styles-and-city-styles](05-assets/styles-and-city-styles.md) | 02-profiles-dimensions |
| `ConditionRE.java` | 05-assets | [conditions](05-assets/conditions.md) | 08-loot-mobs-blocks |
| `IAsset.java` | 05-assets | [loading-and-lookup](05-assets/loading-and-lookup.md) | |
| `MultiBuildingRE.java` | 05-assets | [buildings-and-multibuildings](05-assets/buildings-and-multibuildings.md) | |
| `PaletteRE.java` | 05-assets | [palettes-and-variants](05-assets/palettes-and-variants.md) | |
| `PredefinedCityRE.java` | 05-assets | [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md) | |
| `PredefinedSphereRE.java` | 05-assets | [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md) | |
| `ScatteredRE.java` | 05-assets | [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md) | |
| `StuffSettingsRE.java` | 05-assets | [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md) | 07-scattered |
| `StyleRE.java` | 05-assets | [styles-and-city-styles](05-assets/styles-and-city-styles.md) | |
| `VariantRE.java` | 05-assets | [palettes-and-variants](05-assets/palettes-and-variants.md) | |
| `WorldStyleRE.java` | 05-assets | [world-styles](05-assets/world-styles.md) | 02-profiles-dimensions |

### `worldgen/lost/regassets/data/` (35)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `BiomeMatcher.java` | 02-profiles-dimensions, 05-assets | [world-styles](02-profiles-dimensions/world-styles.md), [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md), [world-styles](05-assets/world-styles.md) | |
| `BlockEntry.java` | 05-assets | [palettes-and-variants](05-assets/palettes-and-variants.md) | |
| `BlockMatcher.java` | 05-assets | [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md) | |
| `BuildingSettings.java` | 04-city-layout, 05-assets | [city-settings](04-city-layout/city-settings.md), [styles-and-city-styles](05-assets/styles-and-city-styles.md) | |
| `CityBiomeMultiplier.java` | 02-profiles-dimensions, 05-assets | [world-styles](02-profiles-dimensions/world-styles.md), [world-styles](05-assets/world-styles.md) | |
| `CityProfileOverrides.java` | 02-profiles-dimensions, 05-assets | [effective-city-settings](02-profiles-dimensions/effective-city-settings.md), [styles-and-city-styles](05-assets/styles-and-city-styles.md) | |
| `CitySphereSettings.java` | 04-city-layout, 05-assets | [city-settings](04-city-layout/city-settings.md), [world-styles](05-assets/world-styles.md) | |
| `CityStyleSelector.java` | 02-profiles-dimensions, 05-assets | [world-styles](02-profiles-dimensions/world-styles.md), [world-styles](05-assets/world-styles.md) | |
| `ConditionPart.java` | 05-assets | [conditions](05-assets/conditions.md) | 08-loot-mobs-blocks |
| `ConditionTest.java` | 05-assets | [buildings-and-multibuildings](05-assets/buildings-and-multibuildings.md), [conditions](05-assets/conditions.md) | 08-loot-mobs-blocks |
| `CorridorSettings.java` | 04-city-layout, 05-assets | [city-settings](04-city-layout/city-settings.md), [styles-and-city-styles](05-assets/styles-and-city-styles.md) | |
| `DataTools.java` | 05-assets | [loading-and-lookup](05-assets/loading-and-lookup.md) | |
| `GeneralSettings.java` | 05-assets | [styles-and-city-styles](05-assets/styles-and-city-styles.md) | |
| `HighwayParts.java` | 05-assets, 06-highways-railways | [world-styles](05-assets/world-styles.md), [highway-parts](06-highways-railways/highway-parts.md) | |
| `MonorailParts.java` | 05-assets, 06-highways-railways | [world-styles](05-assets/world-styles.md), [monorails](06-highways-railways/monorails.md) | |
| `MultiSettings.java` | 04-city-layout, 05-assets | [city-settings](04-city-layout/city-settings.md), [multibuildings](04-city-layout/multibuildings.md), [world-styles](05-assets/world-styles.md) | |
| `ObjectSelector.java` | 05-assets | [styles-and-city-styles](05-assets/styles-and-city-styles.md) | |
| `PaletteEntry.java` | 05-assets | [palettes-and-variants](05-assets/palettes-and-variants.md) | |
| `PaletteSelector.java` | 05-assets | [styles-and-city-styles](05-assets/styles-and-city-styles.md) | |
| `ParkSettings.java` | 04-city-layout, 05-assets | [city-settings](04-city-layout/city-settings.md), [styles-and-city-styles](05-assets/styles-and-city-styles.md) | |
| `PartMeta.java` | 05-assets | [parts-and-placement](05-assets/parts-and-placement.md) | |
| `PartRef.java` | 05-assets | [buildings-and-multibuildings](05-assets/buildings-and-multibuildings.md) | |
| `PartSelector.java` | 05-assets | [world-styles](05-assets/world-styles.md) | |
| `PredefinedBuilding.java` | 04-city-layout, 05-assets | [predefined-content](04-city-layout/predefined-content.md), [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md) | |
| `PredefinedStreet.java` | 04-city-layout, 05-assets | [predefined-content](04-city-layout/predefined-content.md), [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md) | |
| `RailSettings.java` | 05-assets, 06-highways-railways | [styles-and-city-styles](05-assets/styles-and-city-styles.md), [railway-parts](06-highways-railways/railway-parts.md) | |
| `RailwayParts.java` | 05-assets, 06-highways-railways | [world-styles](05-assets/world-styles.md), [railway-parts](06-highways-railways/railway-parts.md) | |
| `ResourceLocationMatcher.java` | 05-assets | [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md) | |
| `ScatteredReference.java` | 05-assets, 07-scattered | [world-styles](05-assets/world-styles.md), [scattered-structures](07-scattered/scattered-structures.md) | |
| `ScatteredSettings.java` | 05-assets, 07-scattered | [world-styles](05-assets/world-styles.md), [scattered-structures](07-scattered/scattered-structures.md) | |
| `Selectors.java` | 05-assets | [styles-and-city-styles](05-assets/styles-and-city-styles.md) | |
| `SphereSettings.java` | 04-city-layout, 05-assets | [city-settings](04-city-layout/city-settings.md), [styles-and-city-styles](05-assets/styles-and-city-styles.md) | |
| `StreetParts.java` | 05-assets | [styles-and-city-styles](05-assets/styles-and-city-styles.md) | 04-city-layout |
| `StreetSettings.java` | 04-city-layout, 05-assets | [city-settings](04-city-layout/city-settings.md), [styles-and-city-styles](05-assets/styles-and-city-styles.md) | |
| `WorldSettings.java` | 02-profiles-dimensions, 05-assets | [world-styles](02-profiles-dimensions/world-styles.md), [world-styles](05-assets/world-styles.md) | |

### `worldgen/plan/` (4)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `ChunkPlan.java` | 04-city-layout | [chunk-planner](04-city-layout/chunk-planner.md) | |
| `ChunkPlanner.java` | 04-city-layout | [chunk-planner](04-city-layout/chunk-planner.md), [city-level](04-city-layout/city-level.md) | 06-highways-railways |
| `ChunkStage.java` | 04-city-layout | [chunk-planner](04-city-layout/chunk-planner.md) | |
| `Staged.java` | 04-city-layout | [chunk-characteristics](04-city-layout/chunk-characteristics.md), [chunk-planner](04-city-layout/chunk-planner.md) | 03-terrain |

### `worldgen/street/` (9)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `EffectiveStreetResolver.java` | 04-city-layout | [street-planner](04-city-layout/street-planner.md) | |
| `HierarchicalBridgePlanner.java` | 04-city-layout | [bridges](04-city-layout/bridges.md) | |
| `HierarchicalStreetPlanner.java` | 04-city-layout | [street-planner](04-city-layout/street-planner.md) | |
| `PlannedBridgeInfo.java` | 04-city-layout | [bridges](04-city-layout/bridges.md), [street-planner](04-city-layout/street-planner.md) | |
| `PlannedRoadType.java` | 04-city-layout | [street-planner](04-city-layout/street-planner.md) | |
| `PlannedStreetInfo.java` | 04-city-layout | [street-planner](04-city-layout/street-planner.md) | |
| `RoadDirection.java` | 04-city-layout | [street-planner](04-city-layout/street-planner.md) | |
| `StreetPlannerSettings.java` | 04-city-layout | [street-planner](04-city-layout/street-planner.md) | |
| `TertiaryRoadSegment.java` | 04-city-layout | [street-planner](04-city-layout/street-planner.md) | |
### `src/api/java/` (1)

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `ivorius/reccomplex/dimensions/DimensionDictionary.java` | 01-core | none; [01-core Key facts](01-core/index.md#key-facts) | |

## Resources

Paths are relative to `src/main/resources/`.

### Mod metadata

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `META-INF/mods.toml` | 01-core | [lifecycle](01-core/lifecycle.md) | |
| `META-INF/accesstransformer.cfg` | 01-core | [lifecycle](01-core/lifecycle.md) | 03-terrain (index), 09-client-ui (index) |
| `pack.mcmeta` | none | **uncovered** | |

### Dimension, world generation and biome modifiers

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `data/lostcities/dimension/lostcity.json` | 02-profiles-dimensions | [lost-city-dimension](02-profiles-dimensions/lost-city-dimension.md) | 03-terrain (index) |
| `data/lostcities/dimension_type/lostcity.json` | 02-profiles-dimensions | [lost-city-dimension](02-profiles-dimensions/lost-city-dimension.md) | 03-terrain (index) |
| `data/lostcities/forge/biome_modifier/lostcities.json` | 02-profiles-dimensions, 03-terrain | [lost-city-dimension](02-profiles-dimensions/lost-city-dimension.md), [generation-pipeline](03-terrain/generation-pipeline.md) | |
| `data/lostcities/forge/biome_modifier/lostcity_spheres.json` | 02-profiles-dimensions, 03-terrain | [lost-city-dimension](02-profiles-dimensions/lost-city-dimension.md), [generation-pipeline](03-terrain/generation-pipeline.md) | |
| `data/lostcities/worldgen/configured_feature/lostcities.json` | 03-terrain | [generation-pipeline](03-terrain/generation-pipeline.md) | |
| `data/lostcities/worldgen/configured_feature/spheres.json` | 03-terrain | [generation-pipeline](03-terrain/generation-pipeline.md) | |
| `data/lostcities/worldgen/placed_feature/lostcities.json` | 03-terrain | [generation-pipeline](03-terrain/generation-pipeline.md) | |
| `data/lostcities/worldgen/placed_feature/spheres.json` | 03-terrain | [generation-pipeline](03-terrain/generation-pipeline.md) | |

### Loot tables

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `data/lostcities/loot_tables/chests/lostcitychest.json` | 08-loot-mobs-blocks | [chest-loot](08-loot-mobs-blocks/chest-loot.md) | 05-assets |
| `data/lostcities/loot_tables/chests/raildungeonchest.json` | 08-loot-mobs-blocks | [chest-loot](08-loot-mobs-blocks/chest-loot.md) | 05-assets |

### Bundled Lost Cities assets

Under `data/lostcities/lostcities/`. Each folder is one asset type; every file in it is cataloged.

| Folder | Files | Area | Sheet(s) | Also named in |
|---|---|---|---|---|
| `buildings/` | 37 | 05-assets | [catalog](05-assets/catalog.md#buildings), [buildings-and-multibuildings](05-assets/buildings-and-multibuildings.md) | |
| `multibuildings/` | 14 | 05-assets | [catalog](05-assets/catalog.md#multibuildings), [buildings-and-multibuildings](05-assets/buildings-and-multibuildings.md) | |
| `parts/` | 207 | 05-assets | [catalog](05-assets/catalog.md#part-families), [parts-and-placement](05-assets/parts-and-placement.md), [block-inventory](05-assets/block-inventory.md) | 06-highways-railways (dimensions of the 36 highway, rail, station, monorail and rail-dungeon parts) |
| `palettes/` | 30 | 05-assets | [catalog-palettes](05-assets/catalog-palettes.md), [palettes-and-variants](05-assets/palettes-and-variants.md), [block-inventory](05-assets/block-inventory.md) | 08-loot-mobs-blocks (`common.json` loot and mob characters) |
| `variants/` | 12 | 05-assets | [catalog-palettes](05-assets/catalog-palettes.md#variants), [palettes-and-variants](05-assets/palettes-and-variants.md) | |
| `styles/` | 4 | 05-assets | [catalog](05-assets/catalog.md#styles), [styles-and-city-styles](05-assets/styles-and-city-styles.md) | |
| `citystyles/` | 5 | 05-assets | [catalog](05-assets/catalog.md#city-styles), [styles-and-city-styles](05-assets/styles-and-city-styles.md) | 02-profiles-dimensions, 07-scattered |
| `conditions/` | 3 | 05-assets, 08-loot-mobs-blocks | [catalog](05-assets/catalog.md#conditions), [conditions (05)](05-assets/conditions.md), [conditions (08)](08-loot-mobs-blocks/conditions.md), [chest-loot](08-loot-mobs-blocks/chest-loot.md), [spawners](08-loot-mobs-blocks/spawners.md) | |
| `worldstyles/` | 2 | 02-profiles-dimensions, 05-assets | [world-styles (02)](02-profiles-dimensions/world-styles.md), [world-styles (05)](05-assets/world-styles.md), [catalog](05-assets/catalog.md#world-styles) | 04-city-layout, 06-highways-railways, 07-scattered |
| `scattered/` | 5 | 05-assets, 07-scattered | [scattered-structures](07-scattered/scattered-structures.md), [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md), [catalog](05-assets/catalog.md#scattered) | |
| `stuff/` | 3 | 05-assets, 07-scattered | [stuff](07-scattered/stuff.md), [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md), [catalog](05-assets/catalog.md#stuff) | |
| `predefinedcities/`, `predefinedspheres/` | 0 | 05-assets, 04-city-layout | [scattered-stuff-predefined](05-assets/scattered-stuff-predefined.md), [predefined-content](04-city-layout/predefined-content.md) | Upstream ships none; the formats are documented. |

### Client assets

| Folder | Files | Area | Sheet(s) | Also named in |
|---|---|---|---|---|
| `assets/lostcities/lang/` (`en_us.json`, `en_US.lang`, `de_DE.lang`, `en_UD.lang`, `es_AR.lang`, `zh_CN.lang`) | 6 | 09-client-ui | [lang-and-textures](09-client-ui/lang-and-textures.md) | |
| `assets/lostcities/textures/gui/` (`configicon.png` and 17 `icon_*.png`) | 18 | 09-client-ui | [lang-and-textures](09-client-ui/lang-and-textures.md) | 02-profiles-dimensions (icon file names) |

## Generated resources

Under `src/generated/resources/data/lostcities/tags/blocks/`, added to the main resources by `build.gradle`.

| File | Area | Sheet(s) | Also named in |
|---|---|---|---|
| `easybreakable.json` | 08-loot-mobs-blocks | [block-tags](08-loot-mobs-blocks/block-tags.md) | 03-terrain, 05-assets |
| `foliage.json` | 08-loot-mobs-blocks | [block-tags](08-loot-mobs-blocks/block-tags.md) | 03-terrain, 05-assets |
| `lights.json` | 08-loot-mobs-blocks | [block-tags](08-loot-mobs-blocks/block-tags.md) | 03-terrain, 05-assets |
| `needspoi.json` | 08-loot-mobs-blocks | [block-tags](08-loot-mobs-blocks/block-tags.md) | 03-terrain, 05-assets |
| `notbreakable.json` | 08-loot-mobs-blocks | [block-tags](08-loot-mobs-blocks/block-tags.md) | 03-terrain, 05-assets |
| `rotatable.json` | 08-loot-mobs-blocks | [block-tags](08-loot-mobs-blocks/block-tags.md) | 04-city-layout, 05-assets |

## Build and meta files

Not behavior. Upstream notes are cited by sheets only to explain intent ([README](README.md#sources)).

| File | Status |
|---|---|
| `build.gradle` | not behavior (read by `01-core` and `08-loot-mobs-blocks` for source sets and the generated resource folder) |
| `gradle.properties` | not behavior |
| `gradlew` | not behavior |
| `gradlew.bat` | not behavior |
| `gradle/wrapper/gradle-wrapper.jar` | not behavior |
| `gradle/wrapper/gradle-wrapper.properties` | not behavior |
| `.gitignore` | not behavior |
| `.github/stale.yml` | not behavior |
| `LICENSE` | not behavior (MIT; the port policy is [0001](../design/0001-port-policy-and-licensing.md)) |
| `README.md` | not behavior |
| `AGENTS.md` | not behavior |
| `changelog.txt` | not behavior (upstream notes; cited for intent) |
| `HIGHWAY_MAJOR_CITY_PLAN.md` | not behavior (upstream notes; cited for intent) |
| `plan_performance.md` | not behavior (upstream notes; cited for intent) |
| `profile_plan.md` | not behavior (upstream notes; cited for intent) |
| `todo.txt` | not behavior (upstream notes) |
