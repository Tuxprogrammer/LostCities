# Twilight Forest

Family D. Mod ID `TwilightForest`, jar `TwilightForest-2.7.45.jar`. Source read: `mods/TwilightForest/twilightforest/block/TFBlocks.java` (registration, lines 146-451), the block classes beside it (`BlockTFCastleBlock`, `BlockTFMazestone`, `BlockTFUnderBrick`, `BlockTFDeadrock`, `BlockTFTowerWood`, `BlockTFNagastone*`, `BlockTFAurora*`, `BlockTFDoor`, `BlockTFHedge`, `BlockTFThorns`, `BlockTFFireflyJar`, `BlockTFCompressed`), names from `mods/TwilightForest/assets/twilightforest/lang/en_US.lang`. Pack config `config/TwilightForest.cfg` has no block toggles. Sheets: `tools/block-contact-sheet/sheets/twilightforest/` (previews in `docs/previews/blocks/twilightforest/`, local only).

## Summary

- **125 registry names, about 264 block+meta entries** (orientation metas collapsed), **about 95 useful** for cities. This is the GTNH fork of TF: it adds eight wood types' doors, trapdoors, chests, stairs and slabs, and the nagastone, spiral-brick and castle sets.
- **Registry names carry the `tile.` prefix.** TF registers each block under `block.getUnlocalizedName()` (`TFBlocks.java` lines 457-471), so the names are `TwilightForest:tile.TFPlank`, `TwilightForest:tile.CastleBrick` and so on. The mod ID is case-sensitive `TwilightForest`.
- **Look.** Two genuinely urban sets: **underbrick** (brown-red brick with mossy and cracked variants: a ready-made ruined brick wall) and **castle brick** (pale limestone-like bricks, worn and cracked variants, a near-black roof tile). Mazestone is a gray-green stone brick family of eight. Eight plank colors (twilight oak, canopy, mangrove, darkwood, timewood, transwood, minewood, sortingwood) read as ordinary planks. The rest is fantasy (nagastone, aurora, trollsteinn, magic logs) or dangerous.
- **Hardness trap.** Castle brick, castle rune brick, mazestone and deadrock have **hardness 100** (obsidian is 50), and castle brick and mazestone **wear a pickaxe 16 times per block** unless it is TF's mazebreaker (`BlockTFCastleBlock.harvestBlock`, `BlockTFMazestone.harvestBlock`). Towerwood is hardness 40. Players are meant to move into city buildings, so these must stay accents (a few blocks per building) or be left out; the look is good, the mining is not.
- **Wood doors fill a pack gap.** The pack disables Et Futurum's wood-variant doors ([block-mapping.md](../../00-pack-integration/block-mapping.md)), so TF's eight `TFDoor*` blocks are among the few non-oak wooden doors in the pack. They are a custom `Block` copied from vanilla's door (`BlockTFDoor.java`), metadata assumed vanilla's (unverified), and **drop a vanilla oak door item** when broken (`getItemDropped`, line 329).
- **Dangers.** Hedge meta 0/2 and thorns deal cactus damage on contact (`BlockTFHedge.onEntityCollidedWithBlock`, `BlockTFThorns` line 122; thorns are hardness 50 and spread when broken). Towerwood meta 4 spawns a tower termite when broken. Knightmetal blocks hurt on contact; fiery-metal blocks set entities on fire. Force fields, stronghold shields, castle doors, boss spawners, tower devices and slide traps are progression machinery: never.
- **Tile entities:** chests (`TFChest*`), firefly, cicada, moonworm, boss spawner, trophy, magic-log cores, cinder furnace. None of the useful construction blocks needs one; the firefly jar does not (`BlockTFFireflyJar` extends `Block`, light 15).
- **Light:** firefly jar 15, trollber 15, torchberry plant 8 and mushgloom 3 (`BlockTFPlant` line 44), huge mushgloom 5, fiery block 7, force field 2.
- **Ore rule:** none of these are GT ore or boulder hosts (all are TF-only blocks).
- **Without the mod:** every name resolves to nothing; per D19 each pick needs a vanilla fallback (below).

## Blocks

### Stone and brick

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `TwilightForest:tile.TFUnderBrick:0` | Underbrick | wall-main, wall-accent | yes | urban-brick, ruin | Brown-red brick, hardness 1.5. Fallback `minecraft:brick_block` |
| `TwilightForest:tile.TFUnderBrick:1` | Mossy Underbrick | wall-main, rubble-ruin | yes | ruin, overgrown | The only mossy *red* brick in the pack outside Chisel. Fallback `minecraft:brick_block` |
| `TwilightForest:tile.TFUnderBrick:2` | Cracked Underbrick | wall-main, rubble-ruin | yes | ruin | Fallback `minecraft:brick_block` |
| `TwilightForest:tile.CastleBrick:0` | Castle Brick | wall-accent, trim-pillar | yes | urban-stone | Pale limestone bricks. **Hardness 100, wears pickaxes 16x.** Fallback `minecraft:stonebrick:0` |
| `TwilightForest:tile.CastleBrick:1` | Worn Castle Brick | wall-accent | yes | urban-stone, ruin | As above |
| `TwilightForest:tile.CastleBrick:2` | Cracked Castle Brick | wall-accent, rubble-ruin | yes | ruin | As above. Fallback `minecraft:stonebrick:2` |
| `TwilightForest:tile.CastleBrick:3` | Castle Roof Tile | roof | partly | urban-stone | Near-black tile; a good flat-roof cap. Same hardness trap. Fallback `etfuturum:blackstone:1` then `minecraft:stonebrick` |
| `TwilightForest:tile.CastleMagic:0-3` | Castle Rune Brick (blue, yellow, pink, violet) | off-theme | no | magic | Glowing rune overlay on castle brick; hardness 100 |
| `TwilightForest:tile.TFMazestone:0` | Mazestone | wall-main | partly | urban-stone | Gray-green smooth stone. **Hardness 100, wears pickaxes 16x** |
| `TwilightForest:tile.TFMazestone:1` | Mazestone Brick | wall-main | yes | urban-stone | Gray-green stone brick; same trap |
| `TwilightForest:tile.TFMazestone:2` | Chiseled Mazestone | trim-pillar | yes | urban-stone | Pillar sides |
| `TwilightForest:tile.TFMazestone:3` | Decorative Mazestone Brick | wall-accent | yes | urban-stone | Carved band |
| `TwilightForest:tile.TFMazestone:4` | Cracked Mazestone Brick | rubble-ruin | yes | ruin | |
| `TwilightForest:tile.TFMazestone:5` | Mossy Mazestone Brick | rubble-ruin | yes | ruin, overgrown | |
| `TwilightForest:tile.TFMazestone:6` | Mazestone Mosaic | floor | yes | urban-stone | Mosaic top, brick sides |
| `TwilightForest:tile.TFMazestone:7` | Mazestone Border | floor | yes | urban-stone | Border top, brick sides |
| `TwilightForest:tile.TFDeadrock:0-2` | Weathered, Cracked, Deadrock | rubble-ruin | partly | ruin | Rough gray rock, **hardness 100**; GTNH sets resistance 5 (`BlockTFDeadrock` line 27). Plain stone look; vanilla stone does the same job |
| `TwilightForest:tile.TFTowerStone:0` | Towerwood Planks | wall-main, floor | partly | rustic | Orange planks, **hardness 40** |
| `TwilightForest:tile.TFTowerStone:1` | Encased Towerwood | wall-accent | partly | rustic, industrial | Crate-like frame |
| `TwilightForest:tile.TFTowerStone:2` | Cracked Towerwood | rubble-ruin | partly | ruin | |
| `TwilightForest:tile.TFTowerStone:3` | Mossy Towerwood | rubble-ruin | partly | ruin, overgrown | |
| `TwilightForest:tile.TFTowerStone:4` | Infested Towerwood | off-theme | no | | Spawns a tower termite: same reason as the no-silverfish rule. Never |
| `TwilightForest:tile.TFNagastoneEtched:0-5`, `...EtchedMossy`, `...EtchedWeathered` | Etched Nagastone (plain, mossy, ruined) | wall-accent | partly | ruin, fantasy | Carved serpent pattern; meta is facing (ForgeDirection) |
| `TwilightForest:tile.TFNagastonePillar:0/4/8`, `...PillarMossy`, `...PillarWeathered` | Nagastone Pillar (plain, mossy, ruined) | trim-pillar | partly | ruin | Axis metas like logs; the mossy and ruined columns suit overgrown ruins |
| `TwilightForest:tile.TFNagastoneStairs{,Mossy,Weathered}{Left,Right}:0-7` | Nagastone Stairs | stairs-slab | partly | ruin | Six stair blocks; stair metas |
| `TwilightForest:tile.TFSpiralBricks:0-7` | Spiral Stone Bricks | wall-accent | partly | fantasy | Connected spiral pattern on stone brick; meta is orientation |
| `TwilightForest:tile.TFNagastone`, `tile.TFNagastoneBody`, `tile.TFNagastoneHead` | Nagastone (serpent statue parts) | off-theme | no | fantasy | Statue pieces |
| `TwilightForest:tile.TrollSteinn:0` | Trollsteinn | off-theme | no | fantasy | Lights up near light |
| `TwilightForest:tile.TFAuroraBrick:0-15` | Aurora Block | glass, wall-accent | partly | modern, cold | Packed-ice material, opaque; tinted a pastel rainbow by x and z (`colorMultiplier`). Reads as glazed tile or a lit facade at night, not as glass |
| `TwilightForest:tile.AuroraPillar:0` | Aurora Pillar | trim-pillar | partly | modern, cold | |
| `TwilightForest:tile.AuroraSlab:0`, `tile.AuroraDoubleSlab:0` | Aurora Slab | stairs-slab | partly | modern | |
| `TwilightForest:tile.GiantCobble`, `tile.GiantLog`, `tile.GiantLeaves`, `tile.GiantObsidian` | Giant blocks | off-theme | no | | Render at 4x scale; giant cobble hardness 128 |
| `TwilightForest:tile.FluffyCloud`, `tile.WispyCloud` | Clouds | off-theme | no | | |
| `TwilightForest:tile.KnightmetalBlock`, `tile.IronwoodBlock`, `tile.SteeleafBlock`, `tile.CarminiteBlock`, `tile.FieryBlock`, `tile.ArcticFurBlock` | Storage blocks | off-theme | no | | Loot-value blocks; knightmetal hurts on contact, fiery burns and glows |
| `TwilightForest:tile.UberousSoil` | Uberous Soil | off-theme | no | | Fertilizer soil |
| `TwilightForest:tile.CastleDoor:0-3`, `tile.CastleDoorVanished`, `tile.ForceField:0-4`, `tile.TFShield`, `tile.TFTowerDevice:0-13`, `tile.TFTowerTranslucent:0-7`, `tile.TFFireJet`, `tile.Slider`, `tile.TFBossSpawner:0-7`, `tile.TFTrophy`, `tile.TFTrophyPedestal`, `tile.TFPortal`, `tile.TFExperiment115`, `tile.TFUncraftingTable`, `tile.CinderFurnaceIdle/Lit` | Progression machinery | off-theme | no | | Unbreakable, ticking, damaging, boss-spawning or functional. Never place |

### Wood

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `TwilightForest:tile.TFPlank:0-7` | Twilight Oak, Canopy, Mangrove, Darkwood, Timewood, Transwood, Minewood, Sortingwood Planks | floor, wall-main | yes | rustic, interior | Eight ordinary plank colors from pale (minewood, mangrove) to dark (canopy). Hardness 2. Fallback `minecraft:planks` |
| `TwilightForest:tile.TFWoodSlab:0-7` (+8 top), `tile.TFWoodSlabDouble:0-7` | Wood slabs | stairs-slab, floor | yes | rustic | Fallback `minecraft:wooden_slab` |
| `TwilightForest:tile.TFWoodStairs{Twilight,Canopy,Mangrove,Darkwood,Time,Trans,Mine,Sort}` | Wood stairs | stairs-slab, roof | yes | rustic | Vanilla stair metas (`BlockTFWoodStairs extends BlockStairs`). Fallback `minecraft:oak_stairs` |
| `TwilightForest:tile.TFDoor{Twilight,...,Sort}` | Wood doors | door-trap | yes | rustic, interior | Eight door styles, including a paneled darkwood and a glazed-look transwood. Drops a vanilla oak door. Fallback `minecraft:wooden_door` |
| `TwilightForest:tile.TFTrapDoor{Twilight,...,Sort}` | Wood trapdoors | door-trap, furniture-deco | yes | rustic | `extends BlockTrapDoor`: vanilla metas. Fallback `minecraft:trapdoor` |
| `TwilightForest:tile.TFLog:0-3` (+4/+8 axis, 12 bark) | Twilight Oak, Canopy, Mangrove, Darkwood logs | trim-pillar, foliage | partly | rustic, park-garden | Fallback `minecraft:log` |
| `TwilightForest:tile.TFMagicLog:0-3` | Timewood, Transwood, Minewood, Sortingwood | off-theme | no | magic | Patterned bark |
| `TwilightForest:tile.TFMagicLogSpecial:0-3` | Clock, Heart, Core, Engine | off-theme | no | | Ticking tree cores with tile entities |
| `TwilightForest:tile.CinderLog:0` | Cinder Log | rubble-ruin | partly | ruin | Charred log look for burnt-out buildings |
| `TwilightForest:tile.TFChest*`, `tile.TFChestTrap*` | Wood chests, trapped chests (8 each) | furniture-deco | partly | interior | Tile entity; loot rules apply as for vanilla chests ([loot-and-mobs.md](../../00-pack-integration/loot-and-mobs.md)) |

### Plants, lights, overgrowth

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `TwilightForest:tile.TFFireflyJar:0` | Firefly Jar | light, furniture-deco | yes | interior, park-garden | Glass jar model, light 15, no tile entity. A lantern stand-in. Fallback `etfuturum:lantern:0` |
| `TwilightForest:tile.TFRoots:0` | Roots | rubble-ruin, foliage | yes | overgrown | Root-mass block for roots breaking through basements and walls |
| `TwilightForest:tile.TFRoots:1` | Liveroots | foliage | partly | overgrown | Drops liveroot |
| `TwilightForest:tile.TFPlant:14` | Root Strands | foliage | yes | overgrown | Hanging strands under ceilings |
| `TwilightForest:tile.TFPlant:3` | Moss Patch | foliage | yes | overgrown | Flat moss on floors and roofs; random-ticks |
| `TwilightForest:tile.TFPlant:8, 9, 10, 11, 13` | Fiddlehead, Mushgloom, Forest Grass, Dried Bush, Torchberry Plant | foliage | partly | overgrown, park-garden | Mushgloom light 3, torchberry hangs and glows |
| `TwilightForest:tile.TFHedge:0` | Hedge | foliage, fence-rail | partly | park-garden | Solid trimmed hedge; **hurts on contact** (cactus damage). Park borders only, out of reach. Fallback `minecraft:leaves` |
| `TwilightForest:tile.TFHedge:1` | Darkwood Leaves (hedge) | foliage | partly | park-garden | No contact damage |
| `TwilightForest:tile.TFThorns:0-1`, `tile.TFBurntThorns`, `tile.TFThornRose` | Thorns | off-theme | no | | Contact damage, hardness 50, spreads when broken. Never |
| `TwilightForest:tile.TFLeaves:0-3`, `tile.TFLeaves3:0-1`, `tile.DarkLeaves`, `tile.TFMagicLeaves:0-3` | Leaves | foliage | partly | park-garden | Tinted leaves; rainbow oak (meta 3) is decorative |
| `TwilightForest:tile.TFSapling:0-9` | Saplings | foliage | partly | park-garden | Some grow giant trees; prefer vanilla saplings |
| `TwilightForest:tile.HugeStalk`, `tile.HugeGloomBlock`, `tile.HugeLilyPad`, `tile.HugeWaterLily`, `tile.TrollVidr`, `tile.UnripeTrollBer`, `tile.TrollBer` | Giant plants | off-theme | no | fantasy | |
| `TwilightForest:tile.TFFirefly`, `tile.TFCicada`, `tile.TFMoonworm` | Critters | foliage | partly | park-garden | Tile entities; firefly and moonworm glow. Cicadas are loud |

## Best variety pools

- **Ruined brick walls (`wall-main`, `rubble-ruin`):** underbrick 0, 1, 2. Weight the plain one highest; together they are the best TF addition.
- **Pale stone accents (`wall-accent`, `trim-pillar`):** castle brick 0, 1, 2 and mazestone 1, 2, 3, used sparingly because of hardness.
- **Flat roofs (`roof`):** castle roof tile, sparingly.
- **Wood floors and interiors (`floor`):** the eight TF planks, slabs and stairs.
- **Doors (`door-trap`):** the eight TF wood doors and trapdoors, a real gap in the pack's wood-door set.
- **Lights (`light`):** firefly jar.
- **Overgrowth (`foliage`):** roots, root strands, moss patch, darkwood hedge, mossy nagastone pillars for park follies.

## Worldgen notes

- Write the `tile.`-prefixed names exactly; resolve them at load like every other mod block, with the fallbacks above.
- Keep hardness-100 blocks (castle, mazestone, deadrock) to small accents, or out of anything a player is expected to strip. Never place infested towerwood, thorns, force fields, shields, castle doors, boss spawners or tower devices.
- Hedge meta 0 damages on contact; use meta 1 at reachable heights.
- TF doors are two blocks like vanilla doors; write both halves.
