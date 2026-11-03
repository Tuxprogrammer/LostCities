# API for other mods

Lost Cities: New Horizons has a small public API in `com.tuxprogrammer.lostcities.api`. Another mod can use it to ask, for any chunk of a world with cities, what Lost Cities planned there: whether it is a city, which building, how many floors, which street or road, which highway or railway level, whether it is reserved for another mod's landmark, and which explosions damaged it. A mod can also add profiles and listen to four generation events. The design is in [0008](design/0008-public-api.md), and it maps upstream Lost Cities' API in [01-core](new-spec/01-core/index.md#api).

Everything here is server-side. The API never answers for a client world.

## Contents

- [Getting the API](#getting-the-api)
- [Querying a world](#querying-a-world)
- [Profiles from your mod](#profiles-from-your-mod)
- [Generation events](#generation-events)
- [What is not in the API](#what-is-not-in-the-api)
- [Changes from upstream](#changes-from-upstream)

## Getting the API

Depend on the mod at compile time only (`compileOnly`), and guard every use:

```java
if (Loader.isModLoaded(LostCitiesAPI.MODID)) {
    ILostCities api = LostCitiesAPI.get();
}
```

`LostCitiesAPI.get()` is safe from any phase and never returns `null` once the mod is loaded. `LostCitiesAPI.VERSION` is `1.0`: the minor part goes up when a member is added, the major part when one changes or goes. The API package names no class outside itself, apart from Minecraft, Forge and FML types, so a compile-time copy of `com.tuxprogrammer.lostcities.api` is all your build needs.

## Querying a world

```java
ILostCityInformation info = LostCitiesAPI.get().getLostInfo(world);
if (info != null) {
    ILostChunkInfo chunk = info.getChunkInfo(chunkX, chunkZ);
    if (chunk.isCity() && chunk.getType() == LostChunkType.BUILDING) {
        int roofY = chunk.getCityGroundLevel() + 6 * chunk.getNumFloors();
    }
}
```

- `getLostInfo(world)` returns `null` for a client world, for `null`, and for a dimension with no pinned profile or no real cities (the server's `dimensionProfiles`, [config.md](config.md#profiles)).
- Call it on the server thread. The answers come from the same layout generation uses, so they agree with what is generated, or will be. Asking generates and loads no chunk and changes nothing generation reads; it is cheap for a chunk near generated land, and costs a terrain-height sample for a chunk far from it.
- Don't keep an `ILostCityInformation` across a world unload.

`ILostCityInformation`:

| Member | Answer |
|---|---|
| `getDimension()`, `getProfile()` | The dimension ID and the name of the profile pinned into the world |
| `getChunkInfo(chunkX, chunkZ)` | A chunk's `ILostChunkInfo`, never `null` |
| `getGroundLevel()` | The profile's ground level: the street of city level 0, block Y |
| `getRealHeight(level)` | `groundLevel + 6 × level`: a city level's street, block Y. The air you stand in starts one block higher |
| `getBuildings()`, `getMultiBuildings()`, `getCityStyles()` | The loaded assets of each type, `get(name)` (`building1` or `lostcities:building1`) and `getAll()` |

`ILostChunkInfo`:

| Member | Answer |
|---|---|
| `isCity()` | Whether the chunk belongs to a city |
| `getType()` | `LostChunkType`: `NONE` (not a city), `BUILDING`, `MULTI_BUILDING`, `PLANNED_ROAD`, `PREDEFINED_STREET`, `STREET`, `PARK`, `OPEN_LOT`, or `LANDMARK` (a core reserved for another mod's structure) |
| `getCityStyle()` | The city style's id, `null` outside a city |
| `getCityInfo()` | The city's radius (blocks) and style when the chunk is a city's center, else `null` |
| `getBuildingType()`, `getMultiBuildingInfo()` | The building's id, and for a multibuilding its id, this chunk's offset and the size in chunks; `null` without one |
| `getCityLevel()`, `getCityGroundLevel()` | The city level and its street's block Y |
| `getNumFloors()`, `getNumCellars()` | The building's floors above ground (the roof level counted, as upstream) and cellars; 0 without a building |
| `getRoadType()` | `LostRoadType`: the planned road through the chunk, `NONE` to `PRIMARY` |
| `getHighwayXLevel()`, `getHighwayZLevel()`, `getMaxHighwayLevel()` | The city level of a highway along each axis, −1 for none; on a ramp, the higher of its two levels ([0013](design/0013-highway-and-railway-networks.md)) |
| `getRailType()`, `getRailLevel()` | The railway piece (`RailChunkType`, with `isStation()` and `isSurface()`) and its level; on a slope that dips under a landmark, the higher level it reaches; `NONE` and −1 without one |
| `getLandmarkZone()` | `LostLandmarkZone`: `CORE` is another mod's structure, `EDGE_RING` the city's margin around it, `AVOID_RING` the ring of a landmark the profile avoids ([0006](design/0006-build-around-other-worldgen.md)) |
| `getScatteredStructure()` | The id of the scattered structure (its world-style entry, such as `lostcities:cabin` or `lostcities:oilrig`) whose footprint holds the chunk, `null` for none. Its blocks, ground and loot stand there in a generated chunk, and lakes, lava, dungeons and oil keep out of it ([0009](design/0009-scattered-structures-in-rwg-terrain.md)) |
| `getRuinLevel()` | The block Y the ruin of a ruined building centers on, −1 when it is not ruined |
| `getDamage(chunkY)` | The explosion damage at the center of 16-block section `chunkY` (0 to 15): 0 for none, above 1 destroys blocks |
| `getExplosions()` | The explosions that reach the chunk: center and radius |

Highways and railways answer from the networks the layout plans (M06, [0013](design/0013-highway-and-railway-networks.md)): the intercity highways with their ramps and plazas, and the railway grid with its stations and its dips under landmarks. Scattered structures answer from their own layout, pure like the rest, so a chunk not generated yet answers what will stand there.

## Profiles from your mod

A mod can add profiles, which a server then names in `dimensionProfiles` like a built-in one:

```java
// preInit, init or postInit:
LostCitiesAPI.get().registerProfileSetup(setup -> {
    ILostCityProfile profile = setup.createProfile("mymod_ruins", "wasteland");
    profile.setDescription("Ruined cities for My Mod");
    profile.setCityChance(0.03);
    profile.setRuinChance(0.6f, 0.2f, 0.8f);
    profile.setOption("buildingMaxFloors", "4");
});
```

- Register before the first server starts. When a server is about to start the set of callbacks is frozen, before any world is pinned; registering later throws `IllegalStateException`.
- Callbacks run then, and again on every `/lostcities profile reload`, so a reload never loses your profiles. Keep them deterministic: the same callback must build the same profile every time.
- `createProfile(name, base)`: `name` is lower-case letters, digits and `_`, and is not a built-in's or a profile file's name; `base` is a built-in or a file profile. Anything else throws `IllegalArgumentException`, which leaves out that callback's profiles with one log line.
- The setters are upstream's (`setDescription`, `setWorldStyle`, `setCityChance`, `setRuinChance`, `setGroundLevel`, `setCityLevelHeights` with eight levels, `setOceanCorrectionBorder`), and `setOption(key, value)` reaches every profile option [config.md](config.md#every-profile-option) lists, with its value as a profile file writes it.
- When the callback returns, the profile is checked as a profile file is: numbers out of range are clamped, and a failed check rejects the profile with a log line. `/lostcities profile info <name>` shows what it set.
- A world pins every option of its profile when it is created, so a world keeps generating the same way even if your mod later changes the profile or is removed.

## Generation events

Four events are posted on `MinecraftForge.EVENT_BUS` from the terrain phase of each chunk of a world with cities, on the server thread. Each is a `LostCityEvent` with `getWorld()`, `getLostCities()` (the world's `ILostCityInformation`), `getChunkX()`, `getChunkZ()` and `getBlocks()`, the chunk's blocks while they are written (`ILostChunkBlocks`: `getBlock`, `getMeta` and `setBlock` at chunk-local X and Z 0 to 15, Y 0 to 255).

| Event | When | Cancel |
|---|---|---|
| `LostCityEvent.PreGenCityChunk` | Before a city chunk's building or street is written; the chunk is already leveled to its city ground | Skips the building or street, and the street's borders |
| `LostCityEvent.PostGenCityChunk` | After a city chunk's building or street, before ruins, damage, highways and railways | No |
| `LostCityEvent.PostGenOutsideChunk` | After every chunk outside a city, blended, bridged or untouched, with its highways and railways written | No |
| `LostCityEvent.PreExplosion` | Before explosion damage and debris, in a chunk near a city | Skips both |

```java
@SubscribeEvent
public void onCityChunk(LostCityEvent.PostGenCityChunk event) {
    ILostChunkInfo chunk = event.getLostCities().getChunkInfo(event.getChunkX(), event.getChunkZ());
    if (chunk.getType() == LostChunkType.PARK) {
        event.getBlocks().setBlock(8, chunk.getCityGroundLevel() + 1, 8, Blocks.sapling, 0);
    }
}
```

The rules a listener must keep, because cities are reproducible from the seed ([CONTRIBUTING § World generation rules](../CONTRIBUTING.md#world-generation-rules)):

- Be deterministic: what you write is a function of the world seed and the chunk, never of time, an unseeded `Random` or the order chunks generate in.
- Write only through `getBlocks()`, into the chunk you were given. Never load, generate or write another chunk, and never read or write blocks through `getWorld()` during the event.
- Tile entities do not exist yet at this stage; place them in your own populate step.

With no listener the events change nothing.

## What is not in the API

- **Sphere queries** (`getSphere`): they wait for the sphere landscapes, after beta (D12).
- **`CharacteristicsEvent`**: it would let a listener change the layout, which is pure, deterministic and pinned per world, behind the pin's back. It is deferred until a mod needs it (P-CORE-3, D31).
- **Registering a dimension** (`registerDimension`) and **choosing the overworld's profile** (`setOverworldProfile`): which dimension has cities, and with which profile, is the server owner's `dimensionProfiles` setting, pinned per world ([0005](design/0005-rwg-overlay.md) §7).
- **Internal types**: palettes, block characters and the layout's own classes do not appear in any signature, so the mod can change them without breaking yours.

## Changes from upstream

Upstream's API ([api.md](original-spec/01-core/api.md)) is reached by IMC with a function, which 1.7.10's IMC cannot carry; here it is a static accessor. The rest of the mapping, and the upstream bugs it avoids (CORE-01, CORE-02, CORE-03, CORE-06, CORE-38), are in [01-core § API](new-spec/01-core/index.md#api).
