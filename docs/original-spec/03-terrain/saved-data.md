# World generation saved data

`LostCityWorldGenData`: the one piece of per-world saved data in this area. It pins which street-generation mode and highway-generation mode each dimension uses, so an upgrade of the mod never changes the layout of an existing world. The modes themselves are documented by 04-city-layout (streets) and 06-highways-railways (highways); the highway route cache (`LostCityHighwayData`) is 06's.

## 1 · Identity

| Piece | Role |
|---|---|
| `worldgen/LostCityWorldGenData` | `SavedData` named `LostCityWorldGenData`, stored in the **overworld's** data storage for all dimensions. |
| `LostCityWorldGenData.initializeNewWorld` | Marks a world as new; called from `ForgeEventHandlers.onCreateSpawnPoint` (the explicit new-world signal). |
| `DefaultDimensionInfo` constructor | Reads the street and highway modes for its dimension. |
| `config/StreetGenerationMode`, `config/HighwayGenerationMode` | Enums: streets `LEGACY`, `HIERARCHICAL_GRID_V1`; highways `LEGACY`, `INTERCITY_NETWORK_V1` (02-profiles-dimensions and the owning areas). |

Profile options: `STREET_GENERATION_MODE`, `HIGHWAY_GENERATION_MODE` (the *requested* modes).

## 2 · Inputs

The server's overworld data storage, the dimension key (as its string ID), the profile's requested modes, the new-world event.

## 3 · Numbers

| Item | Value |
|---|---|
| Saved-data name | `LostCityWorldGenData` (file `data/LostCityWorldGenData.dat` in the overworld folder) |
| Default for a world without the file | not new: every dimension resolves to `LEGACY` for both modes |

## 4 · Behavior

1. (server) `get(level)`: fetch from the overworld's storage, creating an empty instance if absent; throw if there is no overworld.
2. (server) A freshly constructed instance (no file on disk) has both "new world" flags false.
3. (server) When a world is created, `onCreateSpawnPoint` calls `initializeNewWorld`, which sets both flags and marks the data dirty, then bumps the global dirty counter so any dimension info built earlier is rebuilt with the new-world flags.
4. (server) `getStreetMode(dimension, requested)` (synchronized):
   1. If a mode is stored for the dimension, return it.
   2. Otherwise resolve: the requested mode if the world was marked new, else `LEGACY`.
   3. Store the resolved mode (and mark dirty) only if the world was marked new.
5. (server) `getHighwayMode` does the same with the highway flag and map.
6. (server) Loading: unknown stored mode names are logged as errors and replaced with `LEGACY`.

Consequence: a world created before this data existed, or loaded without passing through the new-world event, always generates with `LEGACY` modes, whatever the profile requests, and never stores anything.

## 5 · Determinism

No randomness. The stored mode is fixed at first use in a new world, so later profile changes do not alter it. Saving iterates `HashMap`s, which only affects key order in the NBT.

## 6 · Data formats

NBT compound:

| Key | Type | Meaning |
|---|---|---|
| `newWorldStreetModes` | byte (boolean) | world was created with this data; street modes may follow the profile |
| `newWorldHighwayModes` | byte (boolean) | same for highway modes |
| `streetModes` | compound: dimension ID (string key) → enum name (string) | pinned street mode per dimension |
| `highwayModes` | compound: dimension ID → enum name | pinned highway mode per dimension |

Missing keys read as false or empty.

## 7 · Output

The street and highway modes each dimension's `DefaultDimensionInfo` uses for its whole life.

## 8 · Relationships

`DefaultDimensionInfo` (02-profiles-dimensions), `ForgeEventHandlers.onCreateSpawnPoint` (01-core), street planner (04-city-layout), highway planner and `LostCityHighwayData` (06-highways-railways).

## 9 · 1.20 dependencies

`SavedData`, `DimensionDataStorage.computeIfAbsent` with a loader and factory, `LevelEvent.CreateSpawnPosition`. 1.7.10 has `WorldSavedData` with `MapStorage` (`world.mapStorage` is global; `perWorldStorage` is per dimension) and `WorldEvent.CreateSpawnPosition`.

## 10 · Bugs

None found.

## Deviations in the new mod

None recorded yet. A new mod has no legacy worlds, so whether `LEGACY` modes are ported at all is a new-spec question.
