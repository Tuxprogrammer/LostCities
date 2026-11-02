# 0008 · The public API

- Status: Accepted
- Gate: none (M10's API scope, P-CORE-1 and P-CORE-3 accepted by D31)
- Amends: none
- Date: 2026-11-02

## Context

Upstream Lost Cities has an API in `mcjty.lostcities.api` ([api.md](../original-spec/01-core/api.md)): IMC hands out `ILostCities`, whose `getLostInfo` answers per-chunk queries from `BuildingInfo`; `ILostCitiesPre` takes profile callbacks; four Forge events fire during generation. On 1.7.10 IMC cannot carry an object, and five upstream bugs sit in that API: CORE-01 (common code reaches a client screen), CORE-02 (a cast fails for a client world), CORE-03 (profile callbacks never run), CORE-06 (dimension registrations lost on every cache reset) and CORE-38 (internal types in signatures, dead and misspelled members). D31 accepted P-CORE-1 (a static accessor; `getLostInfo` on a `World`, `null` where it cannot answer) and P-CORE-3 (queries only, profile callbacks frozen at server start, the four chunk events; `CharacteristicsEvent` deferred). This record fixes the shape M10 builds. The guide for mod authors is [docs/api.md](../api.md).

## Decision

1. **Entry point.** `LostCitiesAPI.get()` loads `core.api.LostCitiesImpl.INSTANCE` by name on first use, so the API package names nothing outside itself and a mod can compile against a copy of it. `LostCitiesAPI.VERSION` is `1.0`; added members raise the minor part, changed or removed ones the major part. Part metadata keys are constants there (`META_*`).
2. **Queries.** `ILostCities.getLostInfo(World)` answers `null` for `null`, a client world, and a world without real cities. `ILostCityInformation` is a view over the world's overlay: `getChunkInfo`, `getGroundLevel`, `getRealHeight`, the dimension and profile, and the building, multibuilding and city-style registries. Every answer is read from the immutable layout (D9) and the terrain phase's damage areas, so it agrees with generation, generates nothing and changes nothing generation reads.
3. **Chunk answers.** `ILostChunkInfo` keeps upstream's members (`isCity`, `getCityStyle`, `getCityInfo`, `getBuildingType`, `getMultiBuildingInfo`, `getRailType`, `getRailLevel`, `getCityLevel`, `getNumFloors`, `getNumCellars`, `getDamage`, `getRuinLevel`, `getExplosions`, `getMaxHighwayLevel`) and adds what the port's layout knows and a pack mod asks for: the chunk's coordinates, `getType` (upstream's `debug` "final city content" as an enum), `getCityGroundLevel`, `getRoadType`, the highway level per axis, `getLandmarkZone` ([0006](0006-build-around-other-worldgen.md)) and `getScatteredStructure`. Enums are the API's own (`LostChunkType`, `LostRoadType`, `LostLandmarkZone`, `RailChunkType`), never the layout's.
4. **Extension points.** Highway levels and rails come from the planner's `InfrastructureView`, which M06 fills; scattered structures from `ApiSource.scattered`, which M07 implements. Until they land every chunk answers none.
5. **Assets.** `ILostCityBuilding`, `ILostCityMultiBuilding` and `ILostCityCityStyle` show plain values only; upstream's getters that returned internal palettes or palette characters are dropped.
6. **Profiles.** `registerProfileSetup(Consumer<ILostCityProfileSetup>)` until FML's server-about-to-start event freezes the set. The callbacks run then and on every `profile reload` (`config.profile.ApiProfiles`): each `createProfile(name, base)` copies a built-in or file profile under a new `[a-z0-9_]+` name no built-in or file has; setters write option text; the result is validated as a profile file is and rejected with a log line when it fails. API profiles are pinned like any other, so a world outlives the mod that made its profile.
7. **Events.** `LostCityEvent.PreGenCityChunk` (cancelable: skips the building or street and its borders), `PostGenCityChunk`, `PostGenOutsideChunk` (every non-city chunk of a world with cities) and `PreExplosion` (cancelable: skips damage and debris), posted on `MinecraftForge.EVENT_BUS` from the terrain phase through the `ChunkEvents` hook, with the chunk's blocks as `ILostChunkBlocks`. Listeners must be deterministic and stay in their chunk.

## Consequences

- New packages: `api` and `api.event` (public), `core.api` (implementation, `ApiScenario`). `TerrainPhase` gains the `ChunkEvents` hook, `LayoutPlanner` exposes its `InfrastructureView`, `ProfileRegistry` holds API profiles beside files, and the mod handles `FMLServerAboutToStartEvent`.
- With no listener and no API profile, generation is unchanged: the fresh-world hashes of seed 4242 are the same before and after on both world types (ROADMAP M10).
- Tests: `LostCityInformationImplTest` over the fixture world; `ILostCitiesTest`, `ILostCityProfileTest` and `ApiProfilesTest` for CORE-01, -02, -03, -06 and -38 (no internal type in any API signature); the `api.live` scenario in the real pack (events fire, a listener reads blocks, cancelling skips the building).

## Alternatives considered

- **IMC with a function, as upstream.** 1.7.10's IMC carries strings, NBT and item stacks only.
- **Returning the layout's own types** (`ChunkPlan`, `PlannedRoadType`). Simpler, but every layout change would break other mods, which is CORE-38's leak.
- **`CharacteristicsEvent` now.** A listener would change the layout invisibly to the pin, so a world could generate differently with and without a mod; deferred until a pack mod needs it.
- **Firing `PostGenOutsideChunk` only near cities.** Cheaper, but upstream fires it for every chunk of a Lost Cities dimension, and a mod may rely on that.

## Still open

Nothing. Sphere queries wait for the sphere landscapes (D12).
