# 08 · Loot, mobs and blocks

## Overview

This area documents what Lost Cities 1.20-7.5.7 (upstream `1.20` at `f18a094d`) puts into chests and spawners and how its block tags steer generation: the condition assets that choose a loot table or a mob for a position, the two Lost Cities chest loot tables entry by entry, the vanilla tables the conditions hand out, spawner NBT, the profile switches that remove loot and spawners, and the six block tags with their shipped contents, data generator and consumers. Lost Cities registers no blocks or items at the pin, so there is no item or block content to document beyond these.

## Contents

- [conditions.md](conditions.md): condition evaluation (context values, tests, weighted pick), the `chestloot`, `easymobs` and `hardmobs` conditions with resulting probabilities, the palette entries that use them
- [chest-loot.md](chest-loot.md): chest placement and loot assignment, the `lostcitychest` and `raildungeonchest` tables, determinism of loot
- [spawners.md](spawners.md): spawner placement, mob choice and NBT
- [block-tags.md](block-tags.md): the six block tags, shipped contents, data generation and consumers

## Key facts

- **Loot is a two-step choice.** A palette entry names a condition (`chestloot`); the condition picks a loot table by weight among entries whose tests match the position; vanilla rolls the table later from a stored seed. The bundled condition mostly hands out vanilla 1.20 tables: `minecraft:chests/simple_dungeon` alone is 48 % of the tables given to ground-level chests.
- **Lost Cities' own tables are rich**: about 2.7 diamonds, 2.7 gold ingots and 1.8 emeralds per `lostcitychest`, and up to 9 diamonds per roll in `raildungeonchest`. They are reached only on floor 4 and up, deep cellars and rail dungeons.
- **Floors are counted with truncating division**, so cellar chests and spawners report one floor too high (`LOOT-01`); with default cellar counts the "deep cellar" loot entry is almost unreachable.
- **About 36 % of chests in single-chunk buildings stay empty** with default settings (`buildingWithoutLootChance` 0.2 then `chestWithoutLootChance` 0.2); multibuildings skip the building roll (`LOOT-10`).
- **Spawners hold one vanilla mob each**: zombie, skeleton or spider (`easymobs`), or blaze or witch (`hardmobs`), with vanilla timing.
- **Block tags are data, not code.** The shipped tag files are the source of truth, and two of them (`easybreakable`, `lights`) differ from what the data generator would produce now (`LOOT-07`, `LOOT-08`).
- **Loot randomness shares a stream with vines and follows `HashMap` order** (`LOOT-05`), against this project's determinism rules.

## Related areas

- `02-profiles-dimensions`: the options `generateLoot`, `generateSpawners`, `generateLighting`, `chestWithoutLootChance`, `buildingWithoutLootChance`, `groundLevel`, `vineChance`.
- `03-terrain`: part writing, `ChunkFixer` post-todos, explosion damage that uses `easybreakable` and `notbreakable`, terrain correction that uses `foliage`.
- `04-city-layout`: `BuildingInfo` (`noLoot`, cellars, floors, city ground level), part rotation.
- `05-assets`: condition and palette JSON formats; palette `common.json` characters `1`, `4`, `C`.
- `06-highways-railways`: rail dungeon parts `rail_dungeon1` and `rail_dungeon2` and their depth.
- `07-scattered`: scattered structures write parts through the same loot and spawner path.

## Coverage

- Source files read: 13 of 13 in scope, all in full.
  - Java (2): `worldgen/LostTags`, `datagen/LCBlockTags`.
  - Generated resources (6): `src/generated/resources/data/lostcities/tags/blocks/{easybreakable,foliage,lights,needspoi,notbreakable,rotatable}.json`.
  - Resources (5): `data/lostcities/loot_tables/chests/lostcitychest.json`, `raildungeonchest.json`; `data/lostcities/lostcities/conditions/chestloot.json`, `easymobs.json`, `hardmobs.json`.
- In-scope files not covered: none.
- Out-of-scope files read, for behavior only: `worldgen/lost/cityassets/Condition.java`, `ConditionContext.java`, `worldgen/lost/regassets/data/ConditionPart.java`, `ConditionTest.java`, `DataTools.java` (part), `datagen/DataGenerators.java`, `setup/Registration.java`, `worldgen/ChunkFixer.java` (part), `worldgen/GenerationContext.java` (part), `varia/Tools.java` (part), `worldgen/lost/DamageArea.java` (part), `worldgen/lost/BuildingInfo.java` (parts), `worldgen/LostCityTerrainFeature.java` (part writing, spawner, loot and tag code), `data/lostcities/lostcities/palettes/common.json` (loot and mob entries), `data/lostcities/lostcities/parts/rail_dungeon1.json` (header), `build.gradle` (generated resource directory), and `upstream/1.12` `assets/lostcities/loot_tables/chests/lostcitychest.json`.
- Loot tables: 2 of 2, every pool and entry. Conditions: 3 of 3, every entry. Block tags: 6 of 6, every value.
- Lang keys in scope: 0 of 0. Hard-coded English in scope: the error and log messages quoted in the sheets.
- Corrections to earlier statements: none (first version, 2026-11-01).

## Known limits

- Nothing was run. Probabilities are computed from the weights in the files; nothing was observed in a world.
- The contents of the 33 vanilla 1.20.1 loot tables reached through `chestloot` are not reproduced; they are vanilla data.
- Which floor a rail dungeon chest reports depends on railway depth, owned by 06-highways-railways; the rail dungeon rows in [conditions.md](conditions.md#chestloot-used-by-palette-character-c) cover both possibilities.
- The POI behind `needspoi` and the light-update path behind `lights` are described from code; their in-game effect is *inferred*.
- The vanilla spawner defaults in [spawners.md](spawners.md) come from knowledge of 1.20.1, not from Lost Cities code.

## 1.20 dependencies

| Mechanism | Where | Why it matters for 1.7.10 |
|---|---|---|
| Loot table JSON with number providers, and deferred seeded chest loot | `loot_tables/chests/*.json`; `LostCityTerrainFeature.createLoot` → `RandomizableContainerBlockEntity.setLootTable` | 1.7.10 has no loot table files or deferred loot; chests are filled at generation (`ChestGenHooks`, `WeightedRandomChestContent`). |
| 1.20 vanilla loot table ids | `conditions/chestloot.json` (33 tables) | 1.7.10 `ChestGenHooks` has only `dungeonChest`, `mineshaftCorridor`, `pyramidDesertyChest`, `pyramidJungleChest`, `pyramidJungleDispenser`, `strongholdCorridor`, `strongholdLibrary`, `strongholdCrossing`, `villageBlacksmith` and `bonusChest`; buried treasure, end city, igloo, pillager outpost, shipwreck, underwater ruin, mansion and most village tables have no counterpart. |
| Namespaced entity ids in `SpawnData` | `conditions/easymobs.json`, `hardmobs.json`; `handleSpawner` (`SpawnData` codec) | 1.7.10 spawners take entity names (`Zombie`, `Skeleton`, `Spider`, `Blaze`, `Witch`) through `MobSpawnerBaseLogic`. |
| Block entity NBT attached to a proto-chunk, and post-todos run on the world | `handleSpawner`, `handleBlockEntity`, `handleLoot`, `ChunkFixer` | 1.7.10 creates tile entities when the block is set in a chunk; there is no proto-chunk stage. |
| Block tags and data-pack tag loading | `LostTags`, the six generated tag files, `Tools.hasTag` | 1.7.10 has no block tags; the Ore Dictionary covers only some groups (`logWood`, `treeLeaves`, `treeSapling`, `blockGlass`, `paneGlass`). |
| Forge and vanilla tag references | `#forge:glass` (generator), `#minecraft:leaves`, `flowers`, `bamboo_blocks`, `logs`, `coral_plants`, `saplings`, `stairs`, `doors`, `climbable` | No tags to reference in 1.7.10. |
| Data generation | `LCBlockTags`, `DataGenerators` (`GatherDataEvent`) | No data generation in 1.7.10. |
| Per-state light emission | `lights` generator (`getLightEmission` of the default state) | 1.7.10 light comes from `Block.getLightValue`, per block and metadata. |
| Points of interest | `needspoi` tag and its dirt-then-replace placement | 1.7.10 has no POI system or villager workstations. |
| Block state rotation | `rotatable` tag, `BlockState.rotate`, rail shape transform | 1.7.10 rotation is per-block metadata handling. |
| Blocks added after 1.7.10 | `lights` (soul fire and torches, lanterns, campfires, sea lantern, end rod, magma, sea pickle, conduit, shroomlight, crying obsidian, amethyst, sculk, froglights, `light`, lava cauldron), `needspoi` (barrel, blast furnace, smoker, composter, fletching table, lectern, stonecutter, loom, smithing table, grindstone), `easybreakable` (tinted glass, sea lantern, conduit; stained glass is metadata in 1.7.10), `foliage` (bamboo, coral), `notbreakable` (end gateway) | Absent from vanilla 1.7.10; some exist in pack mods such as Et Futurum Requiem. |
| Block states in palette entries | `minecraft:chest[facing=north]`, `minecraft:spawner` (palettes, 05-assets) | 1.7.10 uses `chest` with facing metadata and `mob_spawner`. |
| Biome ids for `inbiome` tests | `ConditionContext.getBiome` | 1.7.10 biomes are numeric IDs and names. |
| Condition assets through DFU codecs and a data-pack registry | `ConditionRE`, `ConditionPart`, `ConditionTest` | Plain Java parsing in 1.7.10. |
| Y levels | `floor` from city ground level in 6-block steps; `level` from the profile's `groundLevel` | Relative values port as they are; only `groundLevel` itself is a 1.20 number (02-profiles-dimensions). |

## Suspected bugs

| ID | Title | Location | What looks wrong | Severity |
|---|---|---|---|---|
| LOOT-01 | Condition floor index is one too high below ground | `worldgen/LostCityTerrainFeature.java:2016` | `floor` (and `level`) use truncating integer division (also `:2015`, `:2057-2058`), so a chest or spawner standing in cellar k reports floor `-(k - 1)`: cellar 1 counts as the ground floor (`ground` true, `cellar` false) and the `chestloot` entry for floor ≤ -3 needs a fourth cellar. | gameplay |
| LOOT-02 | `belowpart` tests the current part | `worldgen/lost/cityassets/ConditionContext.java:85` | The test compares against `getPart()`, not the part below; the context's below-part is also always `<none>` (`worldgen/LostCityTerrainFeature.java:2017`). | gameplay |
| LOOT-03 | A condition with no matching entry crashes generation | `worldgen/LostCityTerrainFeature.java:2077` | `createLoot` passes a `null` result into `new ResourceLocation`; `getRandomSpawnerMob` throws `Condition '<name>' did not return a valid mob!` (`:2032`). Any condition whose entries are all conditional aborts the chunk. | crash |
| LOOT-04 | Weighted pick can return null | `varia/Tools.java:117` | `getRandomFromList` returns `null` if float rounding leaves the remainder above 0 after the last entry; callers treat that as "no match" (`LOOT-03`). Rare. | crash |
| LOOT-05 | Loot draws depend on vines and `HashMap` order | `worldgen/ChunkFixer.java:189` | After reseeding, vine generation draws from the same random before the chest post-todos, which run in `HashMap<BlockPos, Runnable>` order (`worldgen/lost/BuildingInfo.java:131`), so `vineChance` or any added post-todo changes every chest's table and seed. | worldgen |
| LOOT-06 | Dispenser loot table used for chests | `data/lostcities/lostcities/conditions/chestloot.json:45` | `minecraft:chests/jungle_temple_dispenser` is offered to chests, which then hold only arrows. | gameplay |
| LOOT-07 | Shipped `easybreakable` tag drifts from its generator | `datagen/LCBlockTags.java:246` | The generator now emits `#forge:glass`, but the shipped `src/generated/.../easybreakable.json` lists glowstone, beacon, sea lantern, conduit and panes from an older generator; regenerating would silently change explosion damage. | worldgen |
| LOOT-08 | `lights` tag misses conditional and modded light sources | `datagen/LCBlockTags.java:247` | Built once from default-state light emission of vanilla blocks at data-generation time, so lit furnaces, lit redstone lamps, candles, glow lichen and every modded light are not queued for light updates. | visual |
| LOOT-09 | Only stairs and doors rotate with parts | `worldgen/LostCityTerrainFeature.java:1994` | The `rotatable` tag holds only `#minecraft:stairs` and `#minecraft:doors` (plus rails handled separately); chests (`facing=north` in the palette), wall torches, ladders, trapdoors, furnaces, signs and other directional blocks keep their unrotated facing in rotated parts. | visual |
| LOOT-10 | Multibuildings ignore `buildingWithoutLootChance` | `worldgen/lost/BuildingInfo.java:995` | `noLoot` is only rolled for single-chunk buildings; multibuilding chunks copy the top-left value (`:883`), which is never set, so they always have loot and spawners. | gameplay |

<!-- Merged and deduplicated into BUGS.md. Suspected, not confirmed; no fixes designed here. -->
