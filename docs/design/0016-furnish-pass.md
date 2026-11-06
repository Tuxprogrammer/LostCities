# 0016 · Furnish pass: sparse room furniture and GT machines as set dressing

- Status: Proposed (city-realism phase 6b; decision log D-p6b-1 to D-p6b-6). Implementation pending, see `handoff/lane-p6b-furnish.md`.
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
- Lane 6c extends the palettes without code changes; lane 6d moves spawners and loot separately.
- Every placed GT machine ticks; the cap is the only bound on that cost, so it is a hard limit in code, not a palette value.
