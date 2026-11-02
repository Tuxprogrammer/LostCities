# Block mapping: 1.20 blocks to GTNH 2.9.0 blocks

Upstream's assets name about 120 distinct blocks by their 1.20 IDs with blockstate properties. In 1.7.10 a block is a registry name plus metadata, and many 1.20 blocks don't exist in vanilla. This sheet maps every block upstream's assets and code use to one 1.7.10 block in GTNH 2.9.0 as the pack configures it, with a fallback for when the supplying mod is absent. Et Futurum Requiem supplies most of the modern blocks, but the pack's own config switches some of them off (granite, andesite and diorite; prismarine and the sea lantern; the iron trapdoor; wood-variant doors), so Botania and Chisel fill the stone and lamp gaps and MalisisDoors supplies the doors and the iron trapdoor. Every choice also respects the ore rule from [ore-veins.md](ore-veins.md): no building block is a block GT ore can replace.

This sheet gives one block per upstream block: the baseline, and the fallback when a mod is absent. Variety beyond it, drawn from every decorative block in the pack, is in the [block catalog](../05-assets/block-catalog/index.md) (D25, D28).

Status: *Accepted* (spike S3; [decision log](../../design/decision-log.md) D19, with the doors and the iron trapdoor from D28, the oil rig's diamond look-alike from D30, and the moss block and the gas station's iron from D31). Settles open question Q4. The assets lane (M02) turns these tables into the block-name map that `AssetValidationTest` and the dev self-check read.

## Answer to Q4

**Et Futurum Requiem first, then Botania, Chisel and MalisisDoors, then vanilla.** Which blocks exist and their registry names are read from source at the pack's tags and the pack's shipped config. The visual choices are confirmed or redirected at the alpha screenshot review. The blockstate-to-metadata details marked *unverified* below are checked in M02.

1. **Where a 1.7.10 vanilla block is the same block**, use it, with metadata from the blockstate (the [vanilla table](#vanilla-blocks-with-a-1710-equivalent)). About 80 of upstream's blocks are in this group, several under different names (`stone_bricks` is `stonebrick`, `smooth_stone_slab` is `stone_slab`, `cut_sandstone` is `sandstone:2`).
2. **Where vanilla has nothing, use Et Futurum Requiem 2.6.65-GTNH** if the pack enables that block: concrete, deepslate bricks and tiles, blackstone, copper, chain, lantern, barrel, smooth stone, smooth quartz, quartz bricks, red sandstone, red nether bricks, and the extra stairs and walls.
3. **Where the pack disables the Et Futurum block, use Botania 1.13.42-GTNH, Chisel 2.17.34-GTNH or MalisisDoors 1.19.13-GTNH**: Botania's sea lantern and its 1.8 diorite (block, stairs and wall), Chisel's polished diorite as the second choice, MalisisDoors' wood-variant doors and iron trapdoor (D28).
4. **Every pack block has a vanilla fallback**, used when its mod is absent (dev environments, other packs). The dev self-check reports each fallback it takes ([CONTRIBUTING § World generation rules](../../../CONTRIBUTING.md#world-generation-rules)).
5. **Never an ore host.** Upstream's raw `minecraft:stone` in building variants maps to smooth stone; `etfuturum:deepslate`, `etfuturum:tuff` (meta 0), GT's raw granite, marble and basalt (metas 0 and 8), netherrack, end stone and the planet stones GT treats as ore hosts are never building blocks. Colored terracotta stays `minecraft:stained_hardened_clay`, a GT boulder host, as [ore-veins.md](ore-veins.md) accepts for alpha; the dressed GT stones are boulder hosts on the same terms, used only for variety (D28).
6. **No silverfish.** Every infested block maps to its plain block, so no city holds a `minecraft:monster_egg` ([No infested blocks](#no-infested-blocks)).

## How the mapping works

- **Names, not IDs.** Every target is `modid:name` plus metadata, resolved once at asset load against the live block registry ([CONTRIBUTING § World generation rules](../../../CONTRIBUTING.md#world-generation-rules)). Mod IDs are case-sensitive: `etfuturum`, `Botania`, `chisel`, `malisisdoors`, `Ztones`, `minecraft`. *Verified:* `reference/gtnh/2.9.0/MOD-INDEX.md`, built from each jar's `mcmod.info` and `@Mod` annotations.
- **Registry name prefixing.** FML prefixes a registered name with the registering mod's ID unless the name already carries that exact prefix, so a block registered as `stone2Stairs` by Botania is `Botania:stone2Stairs`. *Verified:* FML 1.7.10 [GameData.java#L950-L972][fml-prefix].
- **Fallback chain.** A mapping lists a primary and a fallback. Resolution takes the first that exists; the fallback is always a vanilla block, so resolution never fails.
- **Blockstate properties become metadata** by the rules in [Blockstate properties](#blockstate-properties-to-metadata). Properties that 1.7.10 computes from neighbors (stair `shape`, fence and pane connections, `waterlogged`) are dropped.
- **`minecraft:structure_void` is not a block.** Upstream uses it in palettes to mean "leave what is there". The asset model keeps it as a marker that writes nothing.
- **Block-entity data** (palette `tag` objects: furnace contents, spawner mobs, chest loot) names items by string ID in 1.20. 1.7.10 item NBT stores a numeric ID that differs per world, so the writer resolves item names to IDs when it places the block, never in the asset. 1.7.10 `ItemStack.readFromNBT` reads `id` as a short, `Count` as a byte and `Damage` as a short (*decompiled:* `net.minecraft.item.ItemStack` of the 1.7.10 workspace the build decompiles; inferred from decompiled bytecode and may be wrong). Chests and spawners are covered in [loot-and-mobs.md](loot-and-mobs.md). Doors, trapdoors and fence gates are tile-entity blocks in this pack ([Doors and trapdoors](#doors-and-trapdoors)); `setBlock` creates their tile entity, so they need no data.

## Pack configuration that decides availability

Et Futurum Requiem registers a block only when its config flag is on; the registry name is the lowercased enum name ([ModBlocks.java#L937-L951][efr-init]). The pack's flags, from `config/etfuturum/blocksitems.cfg` (*verified*):

| Flag (line) | Value | Blocks it controls |
|---|---|---|
| `enableConcrete` (261) | true | `etfuturum:concrete` (16 colors) |
| `enableDeepslate` (403) | true | `cobbled_deepslate`, `polished_deepslate`, `deepslate_bricks`, their slabs, stairs and walls |
| `enableBlackstone` (255), `masterNetherToggle` (466) | true, true | `blackstone`, its slab, stairs and wall |
| `enableCopper` (394) | true | `copper_block`, the cut-copper stairs and slabs, and the copper doors and trapdoors (`copper_door`, `copper_trapdoor` and their weathered and waxed forms) |
| `enableChain` (258), `enableLantern` (280), `enableBarrel` (115) | true | `chain`, `lantern`, `barrel` |
| `enableSmoothStone` (304), `enableSmoothQuartz` (298), `enableQuartzBricks` (292) | true | `smooth_stone`, `smooth_quartz`, `quartz_bricks` |
| `enableRedSandstone` (439), `enableRedNetherBricks` (295) | true | `red_sandstone` and its slabs; `red_netherbrick` |
| `enableExtraVanillaStairs` (268), `enableExtraVanillaWalls` (271), `enableExtraVanillaSlabs` (265) | true | `stone_stairs`, `mossy_stone_brick_stairs`, `stone_wall` and others |
| `enableVanillaWoodRedstone` (234) | true | `pressure_plate_spruce` and the other wood pressure plates and buttons |
| **`enableStones`** (448) | **false** | No Et Futurum granite, andesite or diorite, nor their slabs, stairs and walls |
| **`enablePrismarine`** (436) | **false** | No Et Futurum prismarine or **sea lantern** |
| **`enableIronTrapdoor`** (150) | **false** | No Et Futurum iron trapdoor; MalisisDoors supplies one, and the copper trapdoors are metal trapdoors |
| **`enableVanillaDoors`** (225), `enableNewDoors` (172) | **false** | No Et Futurum spruce, birch, jungle, acacia or dark oak doors; MalisisDoors supplies them |
| `enableTuff` (454) | true | `etfuturum:tuff`: an ore host, never used |

The Java flag names behind these keys: [ConfigBlocksItems.java#L185-L328][efr-config]. MalisisDoors registers its wood-variant doors and iron trapdoor ([Registers.java#L152-L174][malisis-doors], [#L233-L247][malisis-trapdoor]; every block is enabled in `config/malisisdoors.cfg`), and the pack's `config/malisisdoors.cfg` sets `modifyVanillaDoors=true` (line 16), which re-registers `minecraft:wooden_door`, `minecraft:iron_door`, `minecraft:trapdoor` and `minecraft:fence_gate` as MalisisDoors blocks with a tile entity, under the same names and metadata ([Registers.java#L84-L89][malisis-replace]). Chisel enables every feature this sheet uses (`config/chisel.cfg`, `features` category; a feature missing from the file defaults to on, [Configurations.java#L305-L321][chisel-config]). Chisel registers each group as `chisel:<name>` ([CarvableHelper.java#L171-L179][chisel-register]). Botania registers its sea lantern and 1.8 stones unconditionally; `18stones.enabled` in `config/Botania.cfg` only switches their recipes ([ModBlocks.java#L305][botania-sealamp], [ModFluffBlocks.java#L262-L315][botania-fluff]).

## Vanilla blocks with a 1.7.10 equivalent

Registry names *verified* against the block registry of the Minecraft 1.7.10 client jar. Metadata values follow the pre-flattening data values; M02 checked the directional ones against the 1.7.10 block classes ([Blockstate properties](#blockstate-properties-to-metadata)). Colors use dye order (white 0, orange 1, magenta 2, light blue 3, yellow 4, lime 5, pink 6, gray 7, light gray 8, cyan 9, purple 10, blue 11, brown 12, green 13, red 14, black 15), which Et Futurum's concrete also follows ([ModBlocks.java#L268-L271][efr-concrete]).

| 1.20 block | 1.7.10 block : meta | Note |
|---|---|---|
| `air` | `minecraft:air` | |
| `anvil` | `minecraft:anvil` : facing | |
| `bookshelf` | `minecraft:bookshelf` | |
| `brewing_stand` | `minecraft:brewing_stand` | Et Futurum's `enableNewBrewingStand=true` changes its behavior, not its name (*unverified*) |
| `brick_slab` | `minecraft:stone_slab` : 4 (+8 top) | |
| `brick_stairs` | `minecraft:brick_stairs` | |
| `bricks` | `minecraft:brick_block` | |
| `cauldron` | `minecraft:cauldron` | |
| `chest` | `minecraft:chest` : facing | Loot: [loot-and-mobs.md](loot-and-mobs.md) |
| `cobblestone_wall` | `minecraft:cobblestone_wall` : 0 | A different block from `minecraft:cobblestone`, so not a boulder host |
| `cobweb` | `minecraft:web` | |
| `cracked_stone_bricks` | `minecraft:stonebrick` : 2 | |
| `crafting_table` | `minecraft:crafting_table` | |
| `cut_sandstone` | `minecraft:sandstone` : 2 | 1.7.10's "smooth sandstone" is 1.13's cut sandstone |
| `dandelion` | `minecraft:yellow_flower` : 0 | |
| `diamond_block`, `emerald_block`, `gold_block`, `iron_block` (the `!` treasure mix; `iron_block` also as `i` in the `highway_gas_station` and `highway_restaurant_parking` parts) | `minecraft:diamond_block` and so on | Treasure is loot; the mix in `common.json` is on the [beta balance list](../beta-balance-list.md). The two parts' iron blocks (20 in `highway_gas_station`'s canopy columns and pumps, 4 in `highway_restaurant_parking`'s lamp posts) stay `minecraft:iron_block` while `decorativeMetals.iron` is on, the second exception to D29's accent cap after the cabin; with it off they take `chisel:factoryblock` : 12, then `minecraft:double_stone_slab` : 8 (D31, [decorative metals](../05-assets/block-catalog/index.md#decorative-metals-d29)) |
| `diamond_block` in the oil rig (`M`) | `Ztones:tile.iszmBlock` : 8, then `chisel:hempcrete` : 3, then `minecraft:stained_hardened_clay` : 3 | A non-valuable look-alike: no diamond block is placed outside loot (D30, D28 rule 7). Iszm 8 is a smooth pale aqua, the catalog's closest match to the diamond block's color; Chisel's light-blue hempcrete and light-blue terracotta are bluer stand-ins when Ztones or Chisel is absent ([ztones.md](../05-assets/block-catalog/ztones.md), [chisel.md](../05-assets/block-catalog/chisel.md)) |
| `dirt` | `minecraft:dirt` : 0 | |
| `enchanting_table` | `minecraft:enchanting_table` | |
| `furnace` | `minecraft:furnace` : facing | Its `Items` tag needs item IDs resolved at placement |
| `glass`, `glass_pane` | `minecraft:glass`, `minecraft:glass_pane` | |
| `<color>_stained_glass`, `<color>_stained_glass_pane` | `minecraft:stained_glass`, `minecraft:stained_glass_pane` : color | white, black, blue, gray, light blue in upstream |
| `<color>_terracotta` | `minecraft:stained_hardened_clay` : color | black, brown, cyan, gray, light gray, orange, red: 22 references in 10 files. A GT boulder host ([ore-veins.md](ore-veins.md)) |
| `glowstone` | `minecraft:glowstone` | |
| `grass_block` | `minecraft:grass` | |
| `infested_stone_bricks` | `minecraft:stonebrick` : 0 | The plain block: no silverfish ([No infested blocks](#no-infested-blocks)) |
| `iron_bars` | `minecraft:iron_bars` | |
| `iron_door` | `minecraft:iron_door` : door halves | A MalisisDoors block with a tile entity in this pack ([Doors and trapdoors](#doors-and-trapdoors)) |
| `ladder` | `minecraft:ladder` : facing | |
| `lever` | `minecraft:lever` : face and facing | |
| `mossy_stone_bricks` | `minecraft:stonebrick` : 1 | |
| `oak_fence` | `minecraft:fence` | |
| `oak_planks` | `minecraft:planks` : 0 | |
| `oak_pressure_plate` | `minecraft:wooden_pressure_plate` | |
| `oak_sapling`, `spruce_sapling`, `birch_sapling`, `acacia_sapling` | `minecraft:sapling` : 0, 1, 2, 4 (+8 for `stage=1`) | |
| `obsidian` | `minecraft:obsidian` | |
| `poppy` | `minecraft:red_flower` : 0 | |
| `potted_cactus` | `minecraft:flower_pot` plus its tile entity holding a cactus | 1.7.10 keeps pot contents in `TileEntityFlowerPot`, as `Item` and `Data` integers, not metadata (*decompiled:* `net.minecraft.tileentity.TileEntityFlowerPot`); the map's row carries the template `{"Item": "minecraft:cactus", "Data": 0}`, with the item named and resolved at placement |
| `powered_rail` | `minecraft:golden_rail` : shape (+8 powered) | |
| `quartz_block` | `minecraft:quartz_block` : 0 | |
| `quartz_slab` | `minecraft:stone_slab` : 7 (+8 top) | |
| `quartz_stairs` | `minecraft:quartz_stairs` | |
| `rail` | `minecraft:rail` : shape | |
| `redstone_torch`, `redstone_wall_torch` | `minecraft:redstone_torch` : 5 standing, 1 to 4 on a wall | |
| `sandstone` | `minecraft:sandstone` : 0 | |
| `smooth_sandstone` | `minecraft:double_stone_slab` : 9 | Not in upstream's assets: only this mod's muted desert palettes name it ([05-assets](../05-assets/index.md), AR-16). The smooth sandstone double slab shows the sandstone top on every face; Et Futurum's smooth sandstone is off in the pack (`enableSmoothSandStone=false`) |
| `smooth_stone_slab` | `minecraft:stone_slab` : 0 (+8 top); `type=double` is `minecraft:double_stone_slab` : 0 | The 1.7.10 stone slab is the smooth stone slab |
| `spawner` | `minecraft:mob_spawner` plus its tile entity | Mobs: [loot-and-mobs.md](loot-and-mobs.md) |
| `stone_brick_slab` | `minecraft:stone_slab` : 5 (+8 top) | |
| `stone_brick_stairs` | `minecraft:stone_brick_stairs` | |
| `stone_bricks` | `minecraft:stonebrick` : 0 | |
| `stone_pressure_plate` | `minecraft:stone_pressure_plate` | |
| `torch`, `wall_torch` | `minecraft:torch` : 5 standing, 1 to 4 on a wall | |
| `water` | `minecraft:water` : 0 | `minecraft:water` is still water in 1.7.10 |

### No infested blocks

Upstream puts `infested_stone_bricks` in three palettes (`bricks_border`, `bricks_standard`, `quartzbricks_border`). This mod never places `minecraft:monster_egg`: buildings are meant to be moved into, and silverfish make players tear them down. Any infested block an asset names, upstream's or an override's, maps to its plain block, or, where the plain block is an ore host, to the non-host block that stands in for it:

| 1.20 block | 1.7.10 block : meta | Note |
|---|---|---|
| `infested_stone_bricks` | `minecraft:stonebrick` : 0 | The only infested block upstream uses |
| `infested_mossy_stone_bricks`, `infested_cracked_stone_bricks`, `infested_chiseled_stone_bricks` | `minecraft:stonebrick` : 1, 2, 3 | |
| `infested_cobblestone` | `minecraft:cobblestone` | A GT boulder host, not an ore host ([ore-veins.md](ore-veins.md)) |
| `infested_stone` | as `stone` below: `etfuturum:smooth_stone` : 0, fallback `minecraft:double_stone_slab` : 8 | Raw `minecraft:stone` is an ore host |
| `infested_deepslate` | `etfuturum:deepslate_bricks` : 0, fallback `minecraft:stonebrick` : 0 | Raw `etfuturum:deepslate` is an ore host |

## Blocks 1.7.10 vanilla lacks

| 1.20 block (uses) | Primary : meta | Fallback : meta | Evidence (*verified* unless marked) |
|---|---|---|---|
| `stone` in variants `stoneandbrick` and `stoneandbrick_rubble` (2) | `etfuturum:smooth_stone` : 0 | `minecraft:double_stone_slab` : 8 | Raw `minecraft:stone` is a GT ore host ([ore-veins.md](ore-veins.md)); [stoneandbrick.json][up-stoneandbrick] is 1000 of 1064 weight stone. Smooth stone uses the stone-slab-top texture ([ModBlocks.java#L237-L238][efr-smooth]); the double slab at meta 8 is the same look |
| `stone_stairs` (4) | `etfuturum:stone_stairs` | `minecraft:stone_brick_stairs` | 1.20's `stone_stairs` are plain stone stairs, and `etfuturum:stone_stairs` uses the plain stone texture ([etfuturum.md](../05-assets/block-catalog/etfuturum.md#corrections-to-the-d19-mapping)). It is a separate block from `minecraft:stone`, so not an ore host. **1.7.10's `minecraft:stone_stairs` is cobblestone stairs**, so a name-for-name match is wrong. [ModBlocks.java#L413][efr-stairs] |
| `mossy_stone_brick_stairs` (1) | `etfuturum:mossy_stone_brick_stairs` | `minecraft:stone_brick_stairs` | [ModBlocks.java#L411][efr-stairs] |
| `smooth_stone` (1) | `etfuturum:smooth_stone` : 0 | `minecraft:double_stone_slab` : 8 | [ModBlocks.java#L237-L238][efr-smooth] |
| `smooth_quartz` (3) | `etfuturum:smooth_quartz` : 0 | `minecraft:quartz_block` : 0 | [ModBlocks.java#L243-L244][efr-smooth] |
| `quartz_bricks` (2) | `etfuturum:quartz_bricks` : 0 | `minecraft:quartz_block` : 0 | [ModBlocks.java#L245-L246][efr-smooth] |
| `<color>_concrete`: black, cyan, gray, red, yellow (11) | `etfuturum:concrete` : color | `minecraft:stained_hardened_clay` : color | [ModBlocks.java#L268-L271][efr-concrete]. Chisel's `chisel:concrete` is gray only and speeds up walking; Chisel's hempcrete (16 colors) and GT's concrete are variety in the [block catalog](../05-assets/block-catalog/index.md#colored-concrete) |
| `cobbled_deepslate_slab` (1) | `etfuturum:deepslate_slab` : 0 (+8 top) | `minecraft:stone_slab` : 3 | Slab subtypes `cobbled_deepslate`, `polished_deepslate` ([ModBlocks.java#L379-L382][efr-deepslate-slab]). Not `etfuturum:deepslate`, the ore host |
| `cobbled_deepslate_stairs` (1) | `etfuturum:cobbled_deepslate_stairs` | `minecraft:stone_stairs` (cobblestone stairs) | [ModBlocks.java#L417][efr-deepslate-stairs] |
| `deepslate_tiles` (2) | `etfuturum:deepslate_bricks` : 2 | `minecraft:stonebrick` : 0 | Subtypes: 0 bricks, 1 cracked bricks, 2 tiles, 3 cracked tiles, 4 chiseled ([ModBlocks.java#L301-L308][efr-deepslate]) |
| `cracked_deepslate_tiles` (2) | `etfuturum:deepslate_bricks` : 3 | `minecraft:stonebrick` : 2 | As above |
| `polished_blackstone` (2) | `etfuturum:blackstone` : 1 | `minecraft:stonebrick` : 0 | Subtypes: 0 blackstone, 1 polished, 2 polished bricks, 3 cracked polished bricks, 4 chiseled ([BlockBlackstone.java#L14][efr-blackstone]) |
| `polished_blackstone_bricks` (3) | `etfuturum:blackstone` : 2 | `minecraft:stonebrick` : 0 | As above |
| `cracked_polished_blackstone_bricks` (3) | `etfuturum:blackstone` : 3 | `minecraft:stonebrick` : 2 | As above |
| `polished_blackstone_slab` (1) | `etfuturum:blackstone_slab` : 1 (+8 top) | `minecraft:stone_slab` : 5 | Subtypes `blackstone`, `polished_blackstone`, `polished_blackstone_bricks` ([BlockBlackstoneSlab.java#L9][efr-blackstone-slab]) |
| `polished_blackstone_stairs` (1) | `etfuturum:polished_blackstone_stairs` | `minecraft:stone_brick_stairs` | [ModBlocks.java#L532][efr-blackstone-stairs] |
| `exposed_copper` (1) | `etfuturum:copper_block` : 1 | `minecraft:hardened_clay` | Subtypes 0 to 3 copper to oxidized, 4 to 7 cut, 8 to 15 waxed ([BlockCopper.java#L19-L22][efr-copper]). Unwaxed, as upstream, so it keeps weathering |
| `exposed_cut_copper_stairs` (4) | `etfuturum:exposed_cut_copper_stairs` | `minecraft:brick_stairs` | [ModBlocks.java#L430][efr-copper-stairs] |
| `red_sandstone` (1) | `etfuturum:red_sandstone` : 0 | `minecraft:sandstone` : 0 | [ModBlocks.java#L216][efr-redsand] |
| `red_sandstone_slab` `type=double` (5) | `etfuturum:double_red_sandstone_slab` : 0 | `minecraft:double_stone_slab` : 1 | Subtypes `red_sandstone`, `cut_red_sandstone` ([BlockRedSandstoneSlab.java#L14][efr-redsand-slab], [ModBlocks.java#L343-L344][efr-redsand-slabs]) |
| `red_nether_bricks` (1) | `etfuturum:red_netherbrick` : 0 | `minecraft:nether_brick` | Subtypes 0 red, 1 cracked, 2 chiseled ([BlockNewNetherBrick.java#L12][efr-netherbrick]) |
| `chain` (1) | `etfuturum:chain` : 0 (axis Y; 1 X, 2 Z) | `minecraft:iron_bars` | [BlockChain.java#L50-L62][efr-chain] |
| `lantern` `hanging=true` (1) | `etfuturum:lantern` : 1 (0 standing) | `minecraft:glowstone` | Placing against a ceiling gives meta 1 ([BlockLantern.java#L73-L82][efr-lantern]) |
| `barrel` (4) | `etfuturum:barrel` : facing (0 down, 1 up, 2 north, 3 south, 4 west, 5 east; +8 open) | `minecraft:chest` : facing | Piston-style orientation ([BlockBarrel.java#L78-L93][efr-barrel]); has an inventory, so loot rules apply |
| `spruce_pressure_plate` (1) | `etfuturum:pressure_plate_spruce` | `minecraft:wooden_pressure_plate` | [ModBlocks.java#L634][efr-plate] |
| `sea_lantern` (3) | `Botania:seaLamp` : 0 | `minecraft:glowstone` | Et Futurum's sea lantern is off (`enablePrismarine=false`). Botania's is a light-emitting glass block named "Sea Lantern" ([ModBlocks.java#L305][botania-sealamp]; `tile.botania:seaLamp.name=Sea Lantern` in Botania's `en_US.lang`) |
| `polished_diorite` (1) | `Botania:stone` : 6 | `chisel:diorite` : 10, then `minecraft:quartz_block` : 0 | Botania's 1.8 stones: 0 to 3 andesite, basalt, diorite, granite; 4 to 7 polished; 8 to 11 bricks; 12 to 15 chiseled ([Block18Stone.java#L38-L77][botania-18stone]; names in Botania's `en_US.lang`). Chisel's `diorite` meta 10 is `polished_diorite` ([Features.java#L1025-L1049][chisel-diorite]) |
| `polished_diorite_stairs` (8) | `Botania:stone2Stairs` (diorite stairs) | `minecraft:quartz_stairs` | No polished-diorite stairs exist in the pack while `enableStones=false`. Botania makes stairs for its raw and brick stones only, named `stone<meta>Stairs` ([ModFluffBlocks.java#L309-L314][botania-fluff], [BlockLivingStairs.java#L8][botania-stairs]) |
| `diorite_wall` (1) | `Botania:stone0Wall` : 2 | `minecraft:cobblestone_wall` : 0 | One wall block, metas 0 to 3 andesite, basalt, diorite, granite ([Block18StoneWall.java#L23][botania-wall], [BlockModWall.java#L39-L54][botania-modwall]) |
| `iron_trapdoor` (1) | `malisisdoors:iron_trapdoor` : trapdoor bits | `etfuturum:waxed_copper_trapdoor` : trapdoor bits, then `minecraft:trapdoor` | Et Futurum's iron trapdoor is off (`enableIronTrapdoor=false`). MalisisDoors' iron trapdoor has a tile entity, created by `setBlock` ([Registers.java#L233-L247][malisis-trapdoor]). Et Futurum's copper trapdoors are metal trapdoors without one, in copper colors ([etfuturum.md](../05-assets/block-catalog/etfuturum.md#lights-glass-furniture-doors-and-barrels)) |
| `structure_void` (3) | none: a "leave as is" marker | none | See [How the mapping works](#how-the-mapping-works) |

`andesite` appears only in the example stuff file's block condition, beside `#forge:stone`: the condition maps to `Botania:stone` : 0 and `chisel:andesite` : 0, and `#forge:stone` to the `stone` ore-dictionary name.

### Doors and trapdoors

The pack runs MalisisDoors 1.19.13-GTNH with `modifyVanillaDoors=true`, so every door this mod places is a MalisisDoors block with a tile entity and an opening animation, whatever its name ([Pack configuration](#pack-configuration-that-decides-availability)). MalisisDoors also registers the wood-variant doors and the iron trapdoor that Et Futurum's config leaves out, and D28 adopts them. Doors use vanilla's door metadata (the lower half creates the tile entity; the upper half has none) and trapdoors vanilla's trapdoor bits ([Door.java#L372-L386][malisis-door-te]).

| 1.20 block | Primary | Fallback |
|---|---|---|
| `oak_door` (code) | `minecraft:wooden_door` | (vanilla already) |
| `birch_door`, `acacia_door`, `dark_oak_door`, `spruce_door`, `jungle_door` (code) | `malisisdoors:door_birch`, `door_acacia`, `door_dark_oak`, `door_spruce`, `door_jungle` | `minecraft:wooden_door` |
| `iron_door` (code and parts) | `minecraft:iron_door` | (vanilla already) |
| `iron_trapdoor` (oil rig) | `malisisdoors:iron_trapdoor` | `etfuturum:waxed_copper_trapdoor`, then `minecraft:trapdoor` |

Without MalisisDoors the vanilla doors are plain vanilla blocks again and have no tile entity; the writer handles both cases. Further door and trapdoor variety (glass sliding, laboratory, factory and garage doors, Natura and Twilight Forest woods, Et Futurum copper) is in the [block catalog](../05-assets/block-catalog/index.md#doors).

## Blocks upstream's code names

Upstream's Java places a few blocks directly. These map as follows. The rest of its `Blocks.*` references place nothing in the overworld: air and the `structure_void` marker; block tests (water, lava, bedrock, dirt, the command block); the block tags; the `testfill` debug command; and the Lost City dimension's bed teleporter, which is not ported (D12).

| Upstream use | 1.20 block | 1.7.10 block | Evidence |
|---|---|---|---|
| Profile `baseBlock` (city foundation fill) | `stone` | `minecraft:stone` : 0, deliberately an ore host so veins continue under cities | [LostCityProfile.java#L777][up-profile]; [ore-veins.md](ore-veins.md) |
| Profile `liquidBlock` | `water` | `minecraft:water` | [LostCityProfile.java#L764][up-profile-liquid] |
| Random building doors | birch, acacia, dark oak, spruce, oak, jungle, iron doors | `malisisdoors:door_birch`, `door_acacia`, `door_dark_oak`, `door_spruce`, `door_jungle`, each with `minecraft:wooden_door` as the fallback; oak `minecraft:wooden_door`; iron `minecraft:iron_door` ([Doors and trapdoors](#doors-and-trapdoors)) | [BuildingInfo.java#L1329-L1340][up-doors] |
| Highway and corridor details | `iron_bars`, `glowstone`, `rail`, `powered_rail`, `dirt` | the vanilla rows above | [LostCityTerrainFeature.java][up-terrain] |
| Default leaf table, when the city style sets no `leaves` block (128 slots: 20 jungle, 20 spruce, 88 oak, all persistent) | `jungle_leaves`, `spruce_leaves`, `oak_leaves` with `persistent=true` | `minecraft:leaves` : 3, 1, 0, each +4 (the no-decay bit) | [LostCityTerrainFeature.java#L116-L146][up-leaves] |
| Default rubble table, when the city style sets no `rubbledirt` block (128 slots: 20 mossy stone bricks, 40 mossy cobblestone, 68 moss blocks) | `mossy_stone_bricks`, `mossy_cobblestone`, `moss_block` | `minecraft:stonebrick` : 1, `minecraft:mossy_cobblestone`, and `chisel:moss` : 0 as the next row maps it; with Chisel the table is mossy stone bricks 20, mossy cobblestone 40, Chisel moss 68, and without it mossy stone bricks 20, mossy cobblestone 108 | [LostCityTerrainFeature.java#L158-L190][up-rubble] |
| `moss_block` (68 of the rubble table's 128 slots; no asset names it) | `moss_block` | `chisel:moss` : 0, Chisel's plain moss, whose texture matches the modern moss block, with `minecraft:mossy_cobblestone` as the fallback (D31; [03-terrain](../03-terrain/index.md), [05-assets](../05-assets/index.md)). It joins the set of blocks the rubble pass recognizes as rubble. Et Futurum's moss block is behind `enableMossAzalea=false` ([etfuturum.md](../05-assets/block-catalog/etfuturum.md)), and 1.7.10 vanilla has none | [LostCityTerrainFeature.java#L171][up-moss]; [Features.java#L4658-L4679][chisel-moss] |
| Park grass, when the city style sets no `grass` block | `grass_block` | `minecraft:grass` | [LostCityTerrainFeature.java#L1546-L1548][up-grass] |
| Torch fix-ups (`fixTorches`) | `torch`, `wall_torch` | `minecraft:torch` : 5 standing, 1 to 4 on a wall | [LostCityTerrainFeature.java#L393-L412][up-torches] |
| Vines on building walls (the world style's default `vine*` settings) | `vine` with one side set | `minecraft:vine` : the side bit (south 1, west 2, north 4, east 8; checked in M02 against `BlockVine.onBlockPlaced`) | [WorldSettings.java#L40-L52][up-vines] |
| Edge lots (this mod, M12: `lot.*` code names; [0008](../../design/0008-city-edges-and-character.md) §3) | none upstream | ground: `minecraft:dirt` : 1 (coarse dirt), `minecraft:hardened_clay` : 0, `minecraft:stained_hardened_clay` : 12 and 14, `minecraft:gravel` : 0; foundations `minecraft:light_gray_concrete`, mapped to `etfuturum:concrete` : 8 with `minecraft:stained_hardened_clay` : 8 as the fallback; fence `minecraft:fence` : 0; rebar the `iron_bars` row | D-M12-5 |
| Partial repairs (this mod, M12: `repair.*` code names; 0008 §4) | none upstream | boards `minecraft:planks` : 0 to 2; materials `minecraft:cobblestone`, `stonebrick` : 0, `brick_block`, `hardened_clay`, `stained_hardened_clay` : 0 to 15, `sandstone` : 0 and 2, `double_stone_slab` : 8, `quartz_block` : 0, `planks` : 0, 1, 5, and the sixteen `{color}_concrete` rows; never `minecraft:stone` : 0, a GT ore host | D-M12-6 |
| Bottom of the world | `bedrock` | not placed: the overlay leaves RWG's bedrock alone, and generation never places unbreakable blocks ([0004](../../design/0004-gtnh-layer-and-tier-ceiling.md)) | [ChunkDriver.java#L47][up-chunkdriver] |

## Blockstate properties to metadata

Checked in M02. Each rule is implemented by `asset.block.MetaRule`, with a unit test per rule (`MetaRuleTest`). The directional encodings were read from the 1.7.10 block classes the build decompiles (*decompiled:* `BlockStairs.onBlockPlacedBy`, `BlockTorch.onBlockPlaced`, `BlockLadder.onBlockPlaced`, `BlockLever.onBlockPlaced`, `BlockTrapDoor.onBlockPlaced`, `BlockAnvil.onBlockPlacedBy`, `BlockVine.onBlockPlaced`, `ItemDoor`'s placement; inferred from decompiled bytecode and may be wrong): they match the encodings 1.8 to 1.12 kept for the same blocks, which the 1.13 flattening turned into these properties. Anvils are symmetric front to back, so the anvil's two opposite encodings look the same. The mapping data is `data/lostcitiesnh/blockmap/mapping.json` ([asset-format.md § The block-name map](../05-assets/asset-format.md#the-block-name-map)).

| Property set | 1.7.10 metadata |
|---|---|
| Stairs `facing`, `half` | east 0, west 1, south 2, north 3; +4 for `half=top`. `shape` is dropped (computed from neighbors) |
| Slabs `type` | bottom: base meta; top: +8; double: the matching double-slab block |
| Doors `facing`, `half`, `hinge`, `open`, `powered` | lower half: east 0, south 1, west 2, north 3, +4 open; upper half: 8, +1 right hinge, +2 powered |
| Wall torches and redstone wall torches `facing` | east 1, west 2, south 3, north 4; standing 5 |
| Ladders, furnaces, chests `facing` | north 2, south 3, west 4, east 5 |
| Lever `face=wall`, `facing`, `powered` | east 1, west 2, south 3, north 4; +8 powered |
| Rails `shape` | north_south 0, east_west 1, ascending_east 2, ascending_west 3, ascending_north 4, ascending_south 5, south_east 6, south_west 7, north_west 8, north_east 9; powered rails use 0 to 5, +8 powered |
| Trapdoor `facing`, `half`, `open` | two hinge bits by facing, +4 open, +8 top half |
| Saplings `stage` | +8 for stage 1 |
| Fence, pane, bar and wall connections; `waterlogged`; `powered=false` on plates | dropped |

## Candidates considered and not used

| Candidate | Why not |
|---|---|
| `etfuturum:deepslate`, `etfuturum:tuff` | GT ore hosts ([ore-veins.md](ore-veins.md)) |
| `gregtech:gt.blockgranites`, `gregtech:gt.blockstones` (granite, marble, basalt, all variants) | No upstream block needs them. Every meta is a GT boulder host and the raw metas 0 and 8 are ore hosts too; the other metas join the [variety pools](../05-assets/block-catalog/index.md) on terracotta's terms (D28) |
| Chisel's stained hardened clay (`chisel:hardened_clay` and the colored groups) | Patterned bricks and tiles only, no plain colors ([Features.java#L1712-L1812][chisel-clay]); plain terracotta stays vanilla |
| Chisel's `concrete` for colored concrete | One gray color, and it speeds up walking. It is a full cube in this version: `config/chisel.cfg` sets `fullBlockConcrete=false` (line 220), but Chisel 2.17.34-GTNH reads the setting and uses it nowhere ([BlockConcrete.java][chisel-concrete]; [chisel.md](../05-assets/block-catalog/chisel.md#behavior-to-know-before-placing-chisel-blocks)) |
| Et Futurum granite, andesite, diorite, prismarine, iron trapdoor, wood doors | Off in the pack's config (table above). Its copper doors and trapdoors are on; they are variety in the [block catalog](../05-assets/block-catalog/index.md#doors), not a mapping for an upstream block |
| Carpenter's Blocks, BiblioCraft, ArchitectureCraft | Every block is a tile entity that needs extra NBT; nothing in upstream's assets needs them. They are variety in the [block catalog](../05-assets/block-catalog/index.md) |
| Ztones, ChiselTones | No 1.20 look-alikes needed, with one exception: Ztones' Iszm 8 stands in for the oil rig's diamond blocks (D30), read from the decompile because Ztones has no published source (`MOD-INDEX.md`). Otherwise both are variety in the [block catalog](../05-assets/block-catalog/index.md) |

Chisel's catalog (`factoryblock`, `laboratoryblock`, `technical`, `road`, `brutalism`, `sheetmetal`, `lantern`, `iron_bars`, metal blocks such as `steelblock` and `copperblock`) is the natural source for the GTNH-themed buildings that come after beta ([0004](../../design/0004-gtnh-layer-and-tier-ceiling.md)), with GT's casings up to EV ([reserved](../05-assets/block-catalog/index.md#reserved-for-the-gtnh-themed-buildings), D28); its groups and metas are read in [Features.java][chisel-features].

## Deviations from upstream this mapping causes

To record in `05-assets` when it is written:

- Raw stone in the `stoneandbrick` variants becomes smooth stone.
- Infested blocks become their plain blocks: no silverfish.
- The oil rig's diamond blocks become a non-valuable pale-aqua look-alike (D30).
- The default rubble table's moss blocks become Chisel moss, or mossy cobblestone without Chisel (D31).
- Polished-diorite stairs become plain-diorite stairs.
- Doors, trapdoors and fence gates are MalisisDoors blocks with a tile entity and an opening animation. Without MalisisDoors, every wooden door is oak and the iron trapdoor is a copper or wooden trapdoor.
- Blocks with a tile entity (furnace contents, flower pot, spawner, chests, barrels) get their NBT built at placement with per-world item IDs.

## To check in game (alpha)

- Every primary resolves in the real pack, and the dev self-check reports no fallback. *Checked in M02:* the real-pack scenario `assets.pack_resolution` took no fallback on a fresh classic RWG world and a fresh RWG2 Continents world.
- The `!` treasure mix and other valuable blocks: note how often they appear, for the [beta balance list](../beta-balance-list.md).
- Botania's diorite next to Chisel and vanilla quartz: whether the polished-diorite family reads as one material.
- Copper weathers in place over time, as in upstream.

## Decisions

Recorded in the [decision log](../../design/decision-log.md) as D19:

- The mapping on this sheet is accepted as written, with the one change that every infested block maps to its plain block, so cities hold no silverfish.
- The visual choices are confirmed or redirected at the alpha screenshot review.
- The boulder-host question for terracotta is settled in [ore-veins.md](ore-veins.md) (D18).

Recorded as D30 (item 1): the oil rig's diamond blocks map to a non-valuable look-alike, `Ztones:tile.iszmBlock` : 8, so no diamond block is placed outside loot.

Recorded as D31: the moss block maps to `chisel:moss` : 0 with `minecraft:mossy_cobblestone` as the fallback, and the gas station's and restaurant parking's iron blocks keep upstream's look while the iron flag is on, with `chisel:factoryblock` : 12 and then `minecraft:double_stone_slab` : 8 when it is off.

Recorded as D28 (rule 5): MalisisDoors is adopted, so upstream's door woods map to its wood-variant doors and the iron trapdoor to its iron trapdoor, each with the fallback above. D28 also sets the rules for the variety drawn on top of this mapping ([block catalog](../05-assets/block-catalog/index.md)).

[fml-prefix]: https://github.com/MinecraftForge/FML/blob/a099592d3d1418245e3af65eda195da244287188/src/main/java/cpw/mods/fml/common/registry/GameData.java#L950-L972
[efr-init]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L937-L951
[efr-config]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/configuration/configs/ConfigBlocksItems.java#L185-L328
[efr-smooth]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L237-L246
[efr-concrete]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L268-L271
[efr-deepslate]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L301-L308
[efr-deepslate-slab]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L379-L382
[efr-deepslate-stairs]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L417
[efr-stairs]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L405-L415
[efr-blackstone]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/blocks/BlockBlackstone.java#L14
[efr-blackstone-slab]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/blocks/BlockBlackstoneSlab.java#L9
[efr-blackstone-stairs]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L526-L535
[efr-copper]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/blocks/BlockCopper.java#L19-L22
[efr-copper-stairs]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L429-L436
[efr-redsand]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L216
[efr-redsand-slab]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/blocks/BlockRedSandstoneSlab.java#L14
[efr-redsand-slabs]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L343-L344
[efr-netherbrick]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/blocks/BlockNewNetherBrick.java#L12
[efr-chain]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/blocks/BlockChain.java#L50-L62
[efr-lantern]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/blocks/BlockLantern.java#L73-L82
[efr-barrel]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/blocks/BlockBarrel.java#L78-L93
[efr-plate]: https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L634
[chisel-config]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/config/Configurations.java#L305-L321
[chisel-register]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/com/cricketcraft/chisel/api/carving/CarvableHelper.java#L171-L179
[chisel-diorite]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java#L1025-L1049
[chisel-clay]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java#L1712-L1812
[chisel-features]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java
[chisel-concrete]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/block/BlockConcrete.java
[chisel-moss]: https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java#L4658-L4679
[malisis-replace]: https://github.com/GTNewHorizons/MalisisDoors/blob/1.19.13-GTNH/src/main/java/net/malisis/doors/Registers.java#L84-L89
[malisis-doors]: https://github.com/GTNewHorizons/MalisisDoors/blob/1.19.13-GTNH/src/main/java/net/malisis/doors/Registers.java#L152-L174
[malisis-trapdoor]: https://github.com/GTNewHorizons/MalisisDoors/blob/1.19.13-GTNH/src/main/java/net/malisis/doors/Registers.java#L233-L247
[malisis-door-te]: https://github.com/GTNewHorizons/MalisisDoors/blob/1.19.13-GTNH/src/main/java/net/malisis/doors/door/block/Door.java#L372-L386
[botania-sealamp]: https://github.com/GTNewHorizons/Botania/blob/1.13.42-GTNH/src/main/java/vazkii/botania/common/block/ModBlocks.java#L305
[botania-fluff]: https://github.com/GTNewHorizons/Botania/blob/1.13.42-GTNH/src/main/java/vazkii/botania/common/block/ModFluffBlocks.java#L262-L315
[botania-18stone]: https://github.com/GTNewHorizons/Botania/blob/1.13.42-GTNH/src/main/java/vazkii/botania/common/block/decor/Block18Stone.java#L38-L77
[botania-stairs]: https://github.com/GTNewHorizons/Botania/blob/1.13.42-GTNH/src/main/java/vazkii/botania/common/block/decor/stairs/BlockLivingStairs.java#L8
[botania-wall]: https://github.com/GTNewHorizons/Botania/blob/1.13.42-GTNH/src/main/java/vazkii/botania/common/block/decor/walls/Block18StoneWall.java#L23
[botania-modwall]: https://github.com/GTNewHorizons/Botania/blob/1.13.42-GTNH/src/main/java/vazkii/botania/common/block/decor/walls/BlockModWall.java#L39-L54
[up-stoneandbrick]: https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/lostcities/variants/stoneandbrick.json
[up-profile]: https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/config/LostCityProfile.java#L777
[up-profile-liquid]: https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/config/LostCityProfile.java#L764
[up-doors]: https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/BuildingInfo.java#L1329-L1340
[up-terrain]: https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java
[up-chunkdriver]: https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/ChunkDriver.java#L47
[up-leaves]: https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java#L116-L146
[up-rubble]: https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java#L158-L190
[up-moss]: https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java#L171
[up-grass]: https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java#L1546-L1548
[up-torches]: https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java#L393-L412
[up-vines]: https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/regassets/data/WorldSettings.java#L40-L52
