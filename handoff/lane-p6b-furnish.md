# Handoff: lane p6b-furnish (city-realism phase 6b)

Branch `lane/p6b-furnish` from `gtnh` at `9d0740af`. Build slot `p6b`, real-pack `--instance 7`. Handed off at the start because the session began with about 152k of its 200k context used; no Java is written yet.

## Done

- Design fixed and committed: `docs/design/0016-furnish-pass.md` (status Proposed), decision rows D-p6b-1 to D-p6b-6 in `docs/design/decision-log.md`. Read 0016 first; it is the spec for the code below.

## Facts already established (no need to re-read)

- Call site: `worldgen/city/CityChunkWriter.writeBuilding()` (about line 199). Add one call after the `for (PartWriter.Placement overlay : overlays)` loop. Collect the floor base heights (`height` per `f`, from `-cellars` to `floors - 1`) in the existing loop; the floor slab is at `height`, standing cells at `height + 1`. Skip when `target.dryRun()` or the option is off.
- Writes: `CityTarget.set(x, y, z, BlockRef)` for plain blocks; `CityTarget.tileEntity(x, y, z, TileTemplate, Transform)` queues NBT that `overlay/PopulationPhase.tileEntity` applies through `TemplateNbt.apply` on a chunk-only `tileAt` (no cascade). Check that `TileTemplate` can carry an int `mID` and short `mFacing` (`asset/block/TileTemplate.java`, `worldgen/city/TileTemplates.java`). GT's `BaseMetaTileEntity.readFromNBT` rebuilds the MTE from `mID`, so NBT alone works if `TemplateNbt.apply` goes through `readFromNBT` (verify; otherwise call `setInitialValuesAsNBT` reflectively, soft, by class name).
- Option pinning pattern: `config/profile/ProfileOptions.java` around lines 483 and 543 (`packHouseChance` and its `legacyDefault`), `worldgen/terrain/TerrainSettings.java` lines 83, 151, 279, 423; snapshot test `config/profile/ProfileSnapshotTest.java` around lines 178-191 (`aSnapshotWithoutPackHouseChanceGetsNoPackHouses`).
- Data folder: `src/main/resources/data/lostcities/lostcities/` (add `furnish/`).
- GT (research-gt-machines.md): block `gregtech:gt.blockmachines`; meta 0 ULV, 1 LV-HV, 2 EV and controllers; always write `mID`; `mFacing` 2-5. Starter: industrial = hulls (id 14 range), transformers 21-24, battery buffers 182-184, LV-HV macerator/compressor/centrifuge/assembler; basement = alloy smelter 201-202, lathe 291, wiremill 351, bender 221, locker 151. Deny: 238, 349, 11304-11306, 951, 1145, 833-834, 1146-1149, 11100-11103, 1174-1175, 679-681, 441-444, 1140-1142, 758-761, 2710-2718. Look up exact ids for hulls and the LV-HV basic machines in §2 of that file. Partial multiblocks: §5 lists casings; start with a loose EBF controller plus a few casings, inside x,z 2..13.
- Furniture starter (research-furniture-blocks.md): residential = Chisel bookshelf and carpet, Thaumcraft candle, vanilla bed and flower pot; office = Chisel bookshelf, Thaumcraft table, Chisel laboratory panels; shop = Cooking for Blockheads counter (meta facing), Chisel carpet; kitchen = furnace, cauldron, crafting table, CfB counter/oven sparingly; industrial = GT machines, Et Futurum barrels, Chisel factory. No Carpenter's or BiblioCraft in bulk, no typewriter desk, no lantern (self-removes).

## Next steps (in order)

1. `furnish/FurnishPalette` + loader (JSON, soft registry-name resolution, deny list) and five starter palettes; test: all palettes parse, deny-listed ids rejected.
2. `furnish/RoomScanner` (pure, over a `get(x,y,z)` view): floor cells, keep-clear mask, flood fill, connectivity re-check. Tests: doors/stairs/ladders keep a clear 1-block ring; a room stays connected after placement.
3. `furnish/FurnishPass`: room type, seeded `Random` (world seed, chunk, floor; never the writer's), per-room and per-chunk tile caps (6 / 2, constants), GT placement. Tests: tile count per chunk <= 6 over a 32x32-chunk region; same seed same blocks.
4. Option `furnish` + snapshot test; fixture variant with furnishing on and a new golden; existing goldens unchanged.
5. Docs: rows in `docs/config.md` and `profile-options.md`; set 0016 to Accepted.
6. Remote `spotlessApply build` on slot `p6b`, pull back the server's formatting, commit. Then `--instance 7`, fresh RWG2_CONTINENTS seed 4242: `city.real`, `city.walk`, a log line or scenario showing a sample machine's MTE class and facing, no error spam over a few hundred ticks. Then one perf comparison against `gtnh` on the same seed (stop if over about 5 %).

Limits still apply: at most 2 verification rounds, no merge of `gtnh`, no subagents.
