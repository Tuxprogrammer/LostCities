# Commands

Every command under `/lostcities` (alias `/lost`), with arguments, permission level, behavior and exact output. The part-editor commands are summarized here and specified in [part-editor.md](part-editor.md).

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `commands/ModCommands` (registers the tree, the `/lost` alias, vanilla `ResetChunksCommand`, and the part and building suggestion providers). One class per subcommand: `CommandCreateBuilding`, `CommandCreatePart`, `CommandDebug`, `CommandEditPart`, `CommandExportPart`, `CommandListParts`, `CommandLocate`, `CommandLocatePart`, `CommandMap`, `CommandResumeEdit`, `CommandSaveProfile`, `CommandStats`, `CommandTestFill`. Profile options: `editMode` (`EDITMODE`). |
| 2 | **Inputs** | Command source (player, position, level, permission level); arguments; the dimension info and `BuildingInfo` of chunks (`04-city-layout`); asset registries (`05-assets`); `EditModeData` and `EditorInfo` ([part-editor.md](part-editor.md)); standard profiles (`02`); chunk-generation statistics. |
| 3 | **Numbers** | See the [command table](#command-table) and [Numbers](#numbers). |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | Only `testfill` is random, from a static unseeded `java.util.Random`; it is a test tool, not world generation. `createbuilding` writes into the chunk-info cache that generation also reads (`CORE-32`). |
| 6 | **Data formats** | Files written to the server's working directory: `<profile>.json` (`saveprofile`; profile JSON, `02`) and `<name>` (`exportpart`; format in [part-editor.md](part-editor.md)). |
| 7 | **Output** | Chat feedback; standard-output dumps (`debug`, `map`); blocks placed (`createbuilding`, `createpart`, `testfill`); files written. |
| 8 | **Relationships** | Called from `ForgeEventHandlers.commandRegister` ([lifecycle.md](lifecycle.md)). Calls `LostCityFeature.getDimensionInfo`, `BuildingInfo`, `Highway`, `Railway`, `CitySphere`, `HierarchicalBridgePlanner`, the intercity highway planner (`03`, `04`, `06`), `AssetRegistries` (`05`), `Editor` and `EditModeData` ([part-editor.md](part-editor.md)), `Statistics` ([utilities.md](utilities.md)). |
| 9 | **1.20 dependencies** | Brigadier command tree with `requires` permission predicates, `ResourceLocationArgument`, `BlockPosArgument`, suggestion providers, `sendSuccess` with a supplier; vanilla `ResetChunksCommand`; `CommandSourceStack.getPlayerOrException`; block states (`testfill` iterates every possible state); `level.getMaxBuildHeight()`. |
| 10 | **Bugs** | `CORE-24` (`stats` average), `CORE-26` (`debug` and `map` open to every player), `CORE-27` (`saveprofile`), `CORE-28` and `CORE-29` (`exportpart`), `CORE-30` and `CORE-31` (`locatepart`), `CORE-32` (`createbuilding`), `CORE-33` (`resetchunks`) |

## Command table

Permission levels: 0 = every player; 1 = moderator (level 1, for example spawn-protection bypass); 2 = game master. Every subcommand needs a player as the source (it calls `getPlayerOrException`) except `saveprofile`. "Edit mode" means the dimension's profile has `editMode` set; otherwise the command fails with `This world was not created with edit mode enabled. This command is not possible!`. A dimension with no Lost Cities profile fails with `This dimension doesn't support Lost Cities!` where noted.

| Command | Arguments | Perm | Needs | Behavior | Output |
|---|---|---|---|---|---|
| `createbuilding` | `name` resource location (building suggestions), `floors` int 1 to 20, `cellars` int 0 to 10, `pos` block position | 1 | player; LC dimension | Builds the named building in the chunk containing `pos`, bottom of the lowest cellar at `pos.y` (below) | Failure `Cannot find building: <name>!`; else none |
| `createpart` | `name` resource location (part suggestions), `pos` block position | 1 | player; LC dimension | Places the part at `pos` and starts an edit session ([part-editor.md](part-editor.md)) | Failure (red) `Error finding part '<name>'!`; else none |
| `debug` | none | 0 | player | Dumps the chunk's generation state to the server's standard output (below) | None to the player |
| `editpart` | none | 1 | player; LC dimension; edit mode | Re-places the part the player stands in and starts a session | `Start editing part '<part>'!`; failures below |
| `exportpart` | `name` single word (letters, digits, `_ - . +`) | 1 | player; LC dimension | Writes the edited part to file `<name>` in the server's working directory | `Exported part to '<name>'!`; failures below |
| `listparts` | none | 1 | player; LC dimension; edit mode | Lists parts recorded in the player's chunk | One line per part: `Found '<part>' at <y>` |
| `locate` | `name` resource location (building suggestions) | 1 | player; LC dimension | Spiral search for the building around the player | Up to 7 lines `Found at <x>,<groundLevel>,<z>`; nothing if none |
| `locatepart` | `name` resource location (part suggestions) | 1 | player; LC dimension; edit mode | Spiral search in edit-mode records | Lines `Found at <x>,<y>,<z>` (`CORE-30`, `CORE-31`) |
| `map` | none | 0 | player | Prints a 41 by 41 chunk map to standard output | None to the player |
| `resumeedit` | none | 1 | player; LC dimension; edit mode | Like `editpart` but does not re-place blocks | As `editpart` |
| `saveprofile` | `profile` single word | 0 | none | Writes the named standard profile to `<profile>.json` in the server's working directory | See below |
| `stats` | none | 0 | player | Chunk-generation timing for the dimension | Three yellow lines; failure (red) `No dimension info found!` |
| `testfill` | none | 1 | player | Fills a 5 by 5 by 5 cube centered on the player with random test blocks | None |
| (vanilla) `resetchunks` | `[range]` and `[skipOldChunks]`, vanilla | 2 (vanilla) | vanilla | Registered at the top level (not under `/lostcities`); vanilla's own development command that regenerates chunks around the source | Vanilla |

`/lost` is a Brigadier redirect to the `/lostcities` node, so every subcommand works under both names.

## Numbers

| Item | Value |
|---|---|
| `createbuilding` floor height | `FLOORHEIGHT` = 6 blocks (`03-terrain`) |
| `createbuilding` clear volume | the whole 16 by 16 chunk from `pos.y` up to the level's maximum build height (exclusive) |
| `createbuilding` block update flag | `UPDATE_CLIENTS` (2): no neighbor updates |
| `locate`, `locatepart` search | `BlockPos.spiralAround` on chunk coordinates, radius 30 chunks (61 by 61 = 3721 chunks), starting east, then south |
| `locate` result limit | stops after the 7th hit |
| Reported position | chunk center `chunkX * 16 + 8`, `chunkZ * 16 + 8`; Y is the building's `groundLevel` (`locate`) or the recorded part Y (`locatepart`) |
| `map` area | chunks from player chunk −20 to +20 on both axes |
| `testfill` cube | offsets −2 to +2 on X, Y, Z; 125 blocks; update flag `UPDATE_ALL` (3) |
| `testfill` block pool | 88 blocks: 17 stairs, 17 slabs, 9 walls, 8 planks, `acacia_slab`, `glass`, `glowstone`, `oak_fence`, `oak_fence_gate`, 16 stained glass colors, 16 wool colors; each with a uniformly random state from all possible states |
| `stats` sample | last 100 chunk generation times in milliseconds ([utilities.md](utilities.md)) |

## Behavior detail

### `createbuilding` (server)

1. Look up the building by full resource location (a name typed without a namespace parses as `minecraft:` and is not found).
2. Get the player's level and the dimension info.
3. Take the `BuildingInfo` of the chunk containing `pos` from the shared cache and overwrite it with the building, cellar and floor counts and `pos.y` as ground level (`setBuildingType`, `04-city-layout`). This changes the cached info that generation and other queries read for that chunk (`CORE-32`).
4. Set every block of the chunk from `pos.y` to the top of the world to air.
5. For floor index `f` from `-cellars` to the info's floor count inclusive: place the floor's part, then its second part if any, at height `h`; then `h += 6`. Parts are written column by column from their vertical slices using the info's compiled palette, combined with the part's and the building's local palettes when either exists. A palette character with no entry throws `Could not find entry '<c>' in the palette for part '<part>'!`, which the command system reports as an unexpected error.

### `debug` (server)

For the player's chunk, prints to standard output (not to the player), one `name = value` line each: profile name; building type name; multibuilding name or null; floors; floors below ground; city level; city ground level; is city; chunk X and Z; city style name (from chunk characteristics); street type; street generation mode; raw planned road type; effective planned road type; planned road connections N, S, W, E; primary block coordinates and bounds; secondary roads X and Z; street density; tertiary segment; the profile's multibuilding street-conflict setting; whether a multibuilding suppressed a road; planned primary bridge X and Z; final city content (one of `NORMAL_TERRAIN`, `MULTI_BUILDING`, `BUILDING`, `PLANNED_ROAD`, `PREDEFINED_STREET`, `OPEN_LOT`, `PARK`, `LEGACY_STREET_OR_PARK`); ruin height; tunnel at levels 0 and 1; highway X and Z levels; highway generation mode; highway classification and route hits; then either the legacy highway result or a dump of the intercity planner (planning cell, settings, current-cell hub, nearby hubs, candidate, selected and accepted connections, maximum degree, each accepted route's owner, shape, length, level, penalty and segments, whether the chunk is on a route, cache statistics and planning-service parallelism); relative distance to the city center; rail chunk type, level, direction and rails; city-sphere center, enabled and radius; explosion count; the chunk heightmap height; the profile's minimum and maximum floors and city chance; whether the chunk is ocean. All computed synchronously on the server thread (`CORE-26`).

### `editpart`, `resumeedit`, `listparts`, `locatepart`, `createpart`, `exportpart`

See [part-editor.md](part-editor.md). Shared failures: `Unknown part '<part>' in this chunk!`, `Could not find a part to edit in this chunk!`, (red) `You are not editing anything!`, (red) `Error finding part '<part>'!`, (red) `Error writing file '<name>'!`.

### `locate` (server)

Walks the spiral of chunks around the player's chunk. For each chunk computes its `BuildingInfo` (generating layout data, not blocks) and, if it has a building whose full ID equals the argument, sends `Found at <x>,<groundLevel>,<z>` (not broadcast to operators). Stops after 7 hits; sends nothing when there are none.

### `map` (server)

Prints 41 lines to standard output, north to south, each 41 characters west to east: `B` city chunk with a building, `+` other city chunk, `.` non-city chunk with a highway on either axis (level 0 or above), space otherwise.

### `saveprofile` (server)

1. Look up the standard profile by name. If missing, send `Could not find profile '<name>'!` in red as a success message broadcast to operators.
2. Serialize it (`toJson(false)`, `02-profiles-dimensions`) with pretty printing and HTML escaping off, and write `<name>.json` in the server's working directory (`CORE-27`).
3. On a file error send `Error saving profile '<name>'!` in red; on success `Saved profile '<name>'!` in green, both as success messages broadcast to operators.

### `stats` (server)

Sends three yellow lines, not broadcast: `Average time: <float>ms`, `Min time: <long>ms`, `Max time: <long>ms`, from the dimension feature's `Statistics` ([utilities.md](utilities.md), `CORE-24`).

### `testfill` (server)

For each of the 125 positions in the cube, picks a block uniformly from the pool and one of its possible states uniformly, and sets it with full updates. The code comment calls it a 3 by 3 fill; it is 5 by 5 by 5.

### Suggestions

`getPartSuggestionProvider` and `getBuildingSuggestionProvider` offer every loaded part or building ID (full `namespace:path`) from `AssetRegistries`.

## On 1.12

`upstream/1.12` has four commands built on `CommandBase` (an API 1.7.10 shares): `CommandBuildPart`, `CommandDebug`, `CommandExportBuilding`, `CommandExportPart`.

## Deviations in the new mod

Not decided here; see `docs/new-spec/` (area 01-core).
