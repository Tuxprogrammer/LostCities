# Bed teleporter to the Lost City dimension

Upstream lets a player travel between any dimension and the Lost City dimension (`lostcities:lostcity`) by sleeping in a specially built bed. [0003](../../design/0003-world-presence.md) drops the dimension from this port; the behavior is documented so the decision is informed and nothing depends on it by accident.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `setup/ForgeEventHandlers` (`onPlayerSleepInBedEvent`, `isValidSpawnBed`, `findLocation`; dead code `findValidTeleportLocation` in two overloads). `varia/CustomTeleporter` (cross-dimension teleport). `setup/Registration.DIMENSION` (`lostcities:lostcity`). Config key `specialBedBlock` ([config.md](config.md)). The dimension and dimension type JSON files are data (`02-profiles-dimensions`). |
| 2 | **Inputs** | The bed position from the sleep event; blocks around and under the bed; the server's levels. |
| 3 | **Numbers** | See [Numbers](#numbers). |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | No random sources. Not part of world generation. |
| 6 | **Data formats** | None (no NBT, no JSON). |
| 7 | **Output** | The sleeping player is moved to the other dimension; sleeping is refused; possibly one cobblestone block placed in the destination dimension. |
| 8 | **Relationships** | Uses `WorldTools.getOverworld` ([utilities.md](utilities.md)), `ComponentFactory` and the dimension registered by data (`02-profiles-dimensions`). The default `dimensionsWithProfiles` maps the dimension to the `biosphere` profile ([config.md](config.md)). |
| 9 | **1.20 dependencies** | `PlayerSleepInBedEvent` with `BedSleepingProblem` results; `Entity.changeDimension` with Forge's `ITeleporter`; `PortalForcer`; `ResourceKey<Level>` dimension keys; `AbstractSkullBlock` (covers every skull and head, floor or wall); `BedBlock.getBedDirection`. |
| 10 | **Bugs** | `CORE-18`, `CORE-19`, `CORE-39` |

## Numbers

| Item | Value |
|---|---|
| Qualifying block under both bed halves | `specialBedBlock`, default `minecraft:diamond_block` |
| Skulls required | 6: one beyond the head end, one on each side of the head half, one beyond the foot end, one on each side of the foot half |
| Landing search start | bed position + 5 blocks up |
| Landing descent stop | while the probed block is air (see `CORE-18`) |
| Landing offset | one block above the first non-air block found |
| Teleport target | block center (x + 0.5, y, z + 0.5) |
| Dead-code teleport search | rings `r` 1 to 49 chunks, `dy` 0 to 254 blocks, upward probe only below Y 250, downward only above Y 1 |

## Behavior detail

1. (server) On `PlayerSleepInBedEvent`, ignore client-side worlds and events without a position.
2. (server) Check the bed (`isValidSpawnBed`):
   1. the block at the event position must be a bed; take its facing (the direction from foot to head);
   2. parse `specialBedBlock` and look it up in the block registry (an unknown name resolves to air and a malformed one throws, `CORE-39`); the block under the event position and the block under the other half (one step opposite the facing) must both be that block;
   3. six skull or head blocks must stand at: event position + facing; event position + clockwise of facing; event position + counter-clockwise of facing; two steps opposite the facing; one step opposite, then clockwise of the opposite direction; one step opposite, then counter-clockwise of the opposite direction.
   The event position is the head half in vanilla 1.20 (inferred from vanilla, not this repository), so the skulls ring the bed.
3. (server) If the bed does not qualify, do nothing; vanilla sleeping continues.
4. (server) If the player is in the Lost City dimension: refuse sleep (`OTHER_PROBLEM`), find a landing spot in the overworld and teleport there.
5. (server) Otherwise (any other dimension): refuse sleep; look up the Lost City dimension. If it does not exist, send the player the red message `Error finding Lost City dimension: lostcities:lostcity!` (hard-coded English). Else find a landing spot there and teleport.
6. (server) Landing spot (`findLocation`): start at the bed position + 5 in the destination level and walk down while the block is air. The loop condition tests the start position's Y rather than the walking position's, so a column that is air all the way down never ends (`CORE-18`). If the block under the found block is empty, place cobblestone at the bed's coordinates in the destination level. Land one block above the found block. If the start block is already solid, the player lands inside terrain (`CORE-19`).
7. (server) Teleport (`CustomTeleporter.teleportToDimension`): `changeDimension` with a teleporter that runs the default repositioning and then sets the entity's position to the target. The class also extends `PortalForcer` with a constructor and a `placeEntity` override that loads the target block and sets position and zero velocity; nothing constructs it, so that part is dead code.

## On 1.12

`upstream/1.12` has the same bed and skull test, returns `DENY` instead of a sleeping problem, does nothing if `DIMENSION_ID` is `-1`, uses integer dimension IDs (overworld `0`, Lost City dimension default `111`), and finds the landing spot with `getTopSolidOrLiquidBlock` plus the ring search that 1.20 keeps only as dead code. When it finds nothing it places cobblestone at the bed coordinates and lands the player one block above the bed coordinates, which is why the cobblestone goes there; 1.20 changed the landing spot but kept the cobblestone position.

## Deviations in the new mod

[0003](../../design/0003-world-presence.md): the Lost City dimension, and with it this teleporter, is not ported.
