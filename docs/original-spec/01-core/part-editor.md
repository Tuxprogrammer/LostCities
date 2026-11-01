# Part editor

An in-game workflow for asset authors: generate a world with the profile option `editMode`, so generation records which part went where; walk into a generated part, re-place it, edit its blocks by hand, and export the result as part JSON with a list of block states that the palette does not cover yet.

| # | Field | Contents |
|---|---|---|
| 1 | **Identity** | `editor/EditModeData` (saved data `LostCityEditData`: parts recorded per chunk). `editor/EditorInfo` (per-player edit session, in memory only). `editor/Editor` (places a part and builds the session's reverse palette). Commands `createpart`, `editpart`, `resumeedit`, `listparts`, `locatepart`, `exportpart` ([commands.md](commands.md)). Profile option `editMode` (default `false`, `02-profiles-dimensions`); also a toggle on the GUI's Various page (`09-client-ui`). |
| 2 | **Inputs** | Parts and palettes (`05-assets`); the chunk's `BuildingInfo` and compiled palette (`04-city-layout`); world blocks; the player's UUID and position. |
| 3 | **Numbers** | See [Numbers](#numbers). |
| 4 | **Behavior** | See [Behavior detail](#behavior-detail). |
| 5 | **Determinism** | Recording happens during generation, so `EditModeData` content follows generation order, and the per-chunk lists are in generation order. The data is not used to generate anything. Export output order: slices in part order; the `missingpalette` entries in `HashMap` order of block states, which is not stable between runs (`CORE-29`). |
| 6 | **Data formats** | Saved data and export file below. |
| 7 | **Output** | Blocks placed in the world; the saved data file; an export JSON file in the server's working directory. |
| 8 | **Relationships** | `LostCityTerrainFeature` (`03-terrain`) calls `EditModeData.addPartData` for every part it places when the profile has `editMode`. The export encodes through `PaletteRE.CODEC` and `BuildingPartRE.CODEC` (`05-assets`) and `Tools.stateToString` ([utilities.md](utilities.md)). |
| 9 | **1.20 dependencies** | `SavedData` in the overworld's `DimensionDataStorage`; Mojang codecs (`DataResult`, `JsonOps`) for export; `BlockState` with property strings (`block[prop=value,…]`); `TickTask` scheduling on the server; `ResourceKey<Level>` stored as a string. |
| 10 | **Bugs** | `CORE-28`, `CORE-29`, `CORE-30`, `CORE-31`, `CORE-34`, `CORE-35` |

## Numbers

| Item | Value |
|---|---|
| Saved-data name | `LostCityEditData` (file `data/LostCityEditData.dat` of the overworld) |
| Part lookup in a chunk | a recorded part matches when `recordedY <= playerY < recordedY + sliceCount` |
| `createpart`/`editpart` placement flag | `UPDATE_ALL` (3) |
| Session scheduling | `TickTask` with tick 3 passed to `doRunTask`, which runs the task at once |
| Export character pool | `a`–`z`, `A`–`Z`, `0`–`9`, then ``!@#$%^&*()_+-=[]{}|;:'<>,.?/`~`` (30 symbols), then Unicode U+0370 to U+04FF (Greek and Cyrillic blocks, 400 code points); 492 characters in total |

## Behavior detail

### Recording (server, during generation)

When the profile has `editMode`, each part written into a chunk is appended to `EditModeData` as `(part name, Y of its bottom)` under the chunk coordinate. The name is the part's short name: the path alone for the `lostcities` namespace, `namespace:path` otherwise. Recording happens on world-generation threads into a plain `HashMap` (`CORE-34`).

### Starting a session: `Editor.startEditing(part, player, start, level, dimensionInfo, clear)` (server)

1. Take the `BuildingInfo` of the chunk containing `start` and its compiled palette; if the part or the chunk's building has a local palette, combine them (part palette, then building palette, over the compiled palette).
2. Create the player's session: part name and bottom location `start`, with an empty reverse palette. A new session replaces the old one.
3. Run at once on the server: if `clear`, for every part cell (x, y, z) set the block at the chunk-relative position (x, `start.y + y`, z) to the palette's state for the cell's character, or air when the character has no state. Then, for every cell whose character has a palette state, read the world block there and record `state → character` in the reverse palette. When several characters produce the same state, the last one read wins.

### Commands (server)

1. `createpart <name> <pos>`: look up the part by full ID (lookup errors count as not found) and start a session at `pos` with `clear` on. The player's current chunk does not need edit-mode records.
2. `editpart`: requires edit mode. Walk the parts recorded for the player's chunk in recorded order. An unknown part name fails with `Unknown part '<part>' in this chunk!`. The first part whose vertical span contains the player's Y starts a session at (player x, recorded Y, player z) with `clear` on, after sending `Start editing part '<part>'!`. No match: `Could not find a part to edit in this chunk!`.
3. `resumeedit`: as `editpart` with `clear` off, so the world keeps the player's edits and only the reverse palette is rebuilt.
4. `listparts`: requires edit mode; sends `Found '<part>' at <y>` for every record in the chunk.
5. `locatepart <name>`: requires edit mode; spiral of radius 30 chunks; for every record whose stored name equals the argument's full `namespace:path` string, sends `Found at <x>,<y>,<z>` with the chunk center. Stored names of `lostcities` parts have no namespace, so they never match (`CORE-30`). The intended 7-hit limit only leaves the inner loop (`CORE-31`).
6. `exportpart <file>`:
   1. require a session (`You are not editing anything!`, red) and the part (`Error finding part '<part>'!`, red);
   2. rebuild the combined palette for the session's chunk as in step 1 of the session start;
   3. start the set of used characters from the palette's characters;
   4. for every slice Y, every row Z and every column X of the part, read the block at the chunk-relative position (x, `bottom.y + y`, z); its character is the session's reverse-palette entry, else an earlier new assignment, else the first unused character from the pool (recorded as new). If the pool runs out, the text `null` is written into the row (`CORE-29`);
   5. build the JSON (below) and write it to `<file>` in the server's working directory (`CORE-28`); report `Exported part to '<file>'!` or, on an I/O error, `Error writing file '<file>'!` (red).

Sessions are kept in a static map keyed by player UUID; they are not saved, not removed when a player leaves, and survive a world change in single player (`CORE-35`).

## Data formats

### Saved data `LostCityEditData`

| Key | Type | Meaning |
|---|---|---|
| `data` | list of compounds | One entry per recorded part |
| `data[].level` | string | Dimension ID, for example `minecraft:overworld` |
| `data[].x`, `data[].z` | int | Chunk coordinates |
| `data[].part` | string | Part short name |
| `data[].y` | int | Block Y of the part's bottom |

The data is always stored in the overworld's storage, whatever dimension the part was in. Loading tolerates missing keys (empty string, 0).

### Export file

A pretty-printed JSON object without HTML escaping:

| Key | When | Content |
|---|---|---|
| `__comment__` | always | With unknown states: `'missingpalette' represents all blockstates that it couldn't find in the palette. These have to be put in a palette. 'exportedpart' is the actual exported part`. Without: `'exportedpart' is the actual exported part` |
| `missingpalette` | when new characters were assigned | A palette (`PaletteRE` codec, `05-assets`): an object whose `palette` list has one entry per new character, with `char` and `block` set to the state string `namespace:block[prop=value,…]` and every other field absent |
| `exportedpart` | always | A part (`BuildingPartRE` codec): `xsize`, `zsize`, `slices` (list of slices, each a list of row strings, one character per column), `refpalette` set to the part's reference palette name when it has one |

## Deviations in the new mod

Not decided here; see `docs/new-spec/` (area 01-core).
