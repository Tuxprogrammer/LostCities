# 0016 · Furnish pass: sparse room furniture and GT machines as set dressing

- Status: Accepted (city-realism phase 6b; decision log D330 to D339). Built as described: `worldgen/city/furnish/` (`RoomScanner`, `FurnishPass`, `FurnishPalette(s)`, `RoomKind`), palettes in `data/lostcitiesnh/furnish/` (D336), tile entities and GT MetaTileEntities completed by a `FURNISH` population job (D337, D338); no partial multiblocks yet (D339).
- Gate: beta
- Amends: building interiors of [docs/original-spec](../original-spec/README.md) (upstream writes no furniture beyond what parts draw)
- Date: 2026-11-06

## Context

Building floors are empty shells apart from what the parts draw. The owner wants lived-in rooms (plan phase 6) but furniture costs framerate: tile entities and custom renderers must stay few, with a hard per-chunk cap. GT machines up to EV may be placed as abandoned set dressing; every one is a ticking tile entity. Research: `reference/research-furniture-blocks.md`, `reference/research-gt-machines.md` (§1 placement, §4 unsafe list, §5 structures).

## Decision

1. **One pass, one call site.** `worldgen/city/furnish/FurnishPass` runs once per building chunk from `CityChunkWriter.writeBuilding`, after the overlay loop (so overlays never overwrite furniture). It receives the floor base heights the loop already computed. Nothing in `LayoutPlanner` or building selection changes.
2. **Own randomness.** The pass seeds its own `Random` from world seed, chunk and floor. It never draws from the writer's `Random`, so turning it on or off changes no other block, and the existing goldens stay byte-identical with it off.
3. **Room detection from written blocks.** Per floor, cells at floor + 1 that are air, with a solid block below and air above, are flood-filled (4-connected) into rooms. A *keep-clear* mask removes every cell within one block of a door, stair, ladder, trapdoor or rail, and every cell on the floor's doorway lines. Items go only on wall-adjacent cells. After choosing a room's items, the pass re-floods the remaining free cells; an item that would split the room or cut a door from the rest is dropped. This is what keeps `city.walk` from regressing.
4. **Room type** comes from the floor and the building: cellars are `basement` (workshop and industrial palette), ground floors of non-residential buildings are `shop` or `kitchen`, upper floors are `office` or `residential` by building tag, falling back to a seeded pick. Each room draws one palette.
5. **Palettes are data.** `data/lostcities/lostcities/furnish/<room>.json`: `room`, `density` (share of eligible wall cells), `maxItems`, `maxTiles`, and weighted `items`, each either a block (`block` registry name, `meta`, `tile: true|false`, optional `facingMeta` table) or a GT machine (`gt` machine id, `baseMeta`, `controller: true|false`). Blocks resolve softly by registry name; a missing mod skips the item, registration never depends on it.
6. **Hard tile-entity cap.** At most 6 tile entities per chunk from the pass (research recommendation), at most 2 per room, GT machines included. A tile item over the cap is replaced by the palette's plain fallback or skipped.
7. **GT machines.** Placed as `gregtech:gt.blockmachines` at the base-type meta from the palette (0 ULV, 1 LV to HV, 2 EV and controllers), then a `TileTemplate` with `mID` (int) and `mFacing` (short, 2 to 5, facing away from the wall) through the existing `CityTarget.tileEntity` population path. A code-side deny list (§4: debug ids, pollution creator, teleporter, tesseract, repellators, world accelerator, lightning rod, miners, rock breakers, pumps, pollution cleaners, ME hatches) rejects ids whatever the palette says. Controllers only at x and z in 2..13 of the chunk.
8. **Decay** follows the ruin pass: a ruined building is furnished at half density, and blocks the damage pass removes take furniture with them.
9. **Profile option `furnish`** (boolean). `TerrainSettings` defaults to `false` (legacy); the option is declared with `add(new ProfileOption(...))` defaulting to `true` for new worlds, plus `legacyDefault("furnish", "false")` (D282 pinning).

## Consequences

- Older worlds keep empty interiors; new worlds get furniture.
- Furniture never crosses the chunk: it is placed only in the chunk being written, and tile entities go through the population phase's chunk-only `tileAt` (no cascade).
- Lane 6c extended the palettes (data) and added the building table; lane 6d moved spawners and loot (decisions 10 to 12).
- Every placed GT machine ticks; the cap is the only bound on that cost, so it is a hard limit in code, not a palette value.

## Room rules (lanes 6c and 6d)

10. **Building table.** With `roomRules` on, `RoomKind.forFloor(building, floor, seed, true)` maps every shipped building: workshops, oil rigs and radio towers are industrial; `town*`, `center*`, `library*` offices; shops and `shopping*` shops (a quarter kitchens) at ground level and offices above; restaurants kitchens two ground floors in three; gas stations shops; towers (`building1`, `building8`, `multi1`, `huge*`, from `DensityRings.classOf`) and high-rises (`multi2` to `multi5`) shops or kitchens at ground level and offices above (a quarter of high-rise upper floors residential). Anything else keeps the name rules. Palettes gained props: lanterns and clay tables (residential), clay desks, OpenComputers cases and filing chests (office), glass display cases (shop), smoker, fridge and sink (kitchen), technical panels (basement), blast furnaces (industrial).
11. **Loot and spawners in fitting rooms.** `RoomFit` says chests fit cellars, offices, kitchens and industrial floors, and spawners cellars and industrial floors only. In the job dry run, a part's chest outside a fitting room loses its loot, and a part's spawner outside one becomes air (the population phase clears an empty mob); roofs and streets fit nothing (below-ground corridors count as cellars). Surviving ones are thinned by a deterministic per-position roll of their own (`CHEST_KEEP` 0.5, `SPAWNER_KEEP` 0.4), so no other draw moves.
12. **Palette chests and spawners.** A palette may place `minecraft:chest` (with a `loot` table of this mod's categories, EV at most, weights) and `minecraft:mob_spawner` (with a `mobs` table). `FurnishPalette.read` rejects them in room kinds that do not fit. They are tile entities, so the 6 per chunk and 2 per room caps count them. The population `FURNISH` job fills the chest (same scratch-inventory path as part chests) and sets the spawner's mob (an unregistered mob clears the block).
13. **Option.** `roomRules` (boolean), `TerrainSettings` default `false`, option default `true`, `legacyDefault("roomRules", "false")` (D282). Legacy goldens do not change.

Verification (2026-11-06, instance 7, fresh worlds): classic RWG seed 777, `city.real` and `city.walk` pass (doors 160/162), no cascade trace names this mod, no missing-block warning. RWG2 Continents seed 4242: `city.walk` doors 74/78 fail identically on the `gtnh` baseline jar (stonebrick-blocked doors of building7 and building8, not furniture), so it is not from this lane.
