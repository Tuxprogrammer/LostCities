# Blood Magic and Blood Arsenal

Family D. Blood Magic: mod ID `AWWayofTime`, jar `BloodMagic-1.9.16.jar`. Blood Arsenal: mod ID `BloodArsenal`, jar `BloodArsenal-1.5.12.jar`. Source read: `mods/BloodMagic/WayofTime/alchemicalWizardry/ModBlocks.java` (lines 164-232), `common/block/BloodStoneBrick.java`, `LargeBloodStoneBrick.java`, `BlockCrystal.java`, `BlockBloodLightSource.java`, `SpectralBlock.java`; `mods/BloodArsenal/com/arc/bloodarsenal/common/block/ModBlocks.java` (lines 46-112) and the block classes beside it. Pack config: `config/BloodArsenal.cfg` disables no blocks (empty "Disable blocks here" list); `config/BloodMagic/` holds only meteor settings. Sheets: `tools/block-contact-sheet/sheets/bloodmagic/` (previews in `docs/previews/blocks/bloodmagic/`, local only).

## Summary

- **Blood Magic: 39 registry names, about 60 block+meta entries; 3 partly useful. Blood Arsenal: 18 registry names, about 22 entries; 2 partly useful.** Nearly everything is ritual or altar machinery in saturated crimson and purple.
- **Registry names** are the literal strings: `AWWayofTime:bloodStoneBrick`, `AWWayofTime:largeBloodStoneBrick`. Four Blood Magic blocks are registered under a concatenated name: `AWWayofTime:AlchemicalWizardrybloodRune` (the unlocalized name minus `tile.`) and `AWWayofTime:AlchemicalWizardrytile.blockSpellParadigm` and its three siblings (with `tile.` kept), `ModBlocks.java` lines 167-222.
- **Bloodstone bricks** are glossy saturated red bricks (hardness 2, plain blocks, no tile entity). They read as glazed red brick from a distance but are far brighter than any upstream brick: accent at most.
- **Dangers:** `AWWayofTime:spectralBlock` is a tile-entity block that reverts to air after a timeout (never place); `AWWayofTime:blockTeleposer` swaps block regions; `AWWayofTime:demonPortalMain`, `AWWayofTime:blockDemonChest` belong to demon invasions; `AWWayofTime:blockMimic` imitates other blocks via its tile entity; `BloodArsenal:blood_tnt` explodes.
- **Light:** `AWWayofTime:bloodLight` 15 (non-opaque, particle block placed by a sigil, no visible model), `BloodArsenal:blood_lamp` 15, `BloodArsenal:blood_infused_glowstone` 15, `BloodArsenal:blood_torch` 14.
- **Ore rule:** none are GT hosts.
- **Without the mod:** fallbacks below.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `AWWayofTime:bloodStoneBrick:0` | Bloodstone Brick | wall-accent | partly | urban-brick | Saturated glazed red brick. Fallback `minecraft:brick_block` |
| `AWWayofTime:largeBloodStoneBrick:0` | Large Bloodstone Brick | wall-accent | partly | urban-brick | Larger courses. Fallback `minecraft:brick_block` |
| `AWWayofTime:blockCrystal:0-1` | Crystal Cluster, Crystal Cluster Brick | off-theme | no | magic | Purple crystal |
| `AWWayofTime:bloodLight:0` | Blood Light | light | partly | | Invisible light source with red particles. Usable as a hidden light only if red particles are acceptable |
| `AWWayofTime:Altar`, `AWWayofTime:AlchemicalWizardrybloodRune:0-*`, `speedRune`, `efficiencyRune`, `runeOfSacrifice`, `runeOfSelfSacrifice`, `ritualStone`, `imperfectRitualStone`, `masterStone`, `blockPedestal`, `blockPlinth`, `blockStabilityGlyph`, `blockEnchantmentGlyph`, `blockWritingTable`, `blockAlchemicCalcinator`, `blockReagentConduit`, `blockCrystalBelljar`, `emptySocket`, `bloodSocket`, `armourForge`, `blockHomHeart`, `blockConduit`, `AlchemicalWizardrytile.blockSpell*` (4), `blockCrucible`, `blockTeleposer`, `spectralBlock`, `spectralContainer`, `lifeEssence`, `demonPortalMain`, `blockDemonChest`, `blockSchemSaver`, `blockMimic` | Altar, ritual, spell and utility blocks | off-theme | no | magic | Tile entities, fluids, hazards |
| `BloodArsenal:blood_stained_glass:0` | Blood-Stained Glass | glass | partly | | Dark red translucent glass. Fallback `minecraft:stained_glass:14` |
| `BloodArsenal:blood_infused_planks:0` | Blood Infused Planks | wall-accent | partly | | Bright red planks: a painted-red plank, too saturated for most uses. Fallback `minecraft:planks:0` |
| `BloodArsenal:blood_infused_wood:0` | Blood Infused Wood | off-theme | no | magic | |
| `BloodArsenal:blood_stone:0-4` | Blood Stone | off-theme | no | magic | Dark stone with red glyphs |
| `BloodArsenal:blood_stained_ice`, `blood_stained_ice_packed`, `blood_infused_iron_block`, `blood_infused_diamond_block`, `blood_infused_glowstone`, `blood_lamp`, `blood_torch`, `blood_cake`, `block_burned_string`, `blood_tnt`, `portable_altar`, `life_infuser`, `compacter`, `lp_materializer`, `compacted_mrs` | Storage, lights, machines | off-theme | no | magic | Pink ice, red lamps, machines; TNT explodes |

## Best variety pools

- **Red brick accent (`wall-accent`):** bloodstone brick and large bloodstone brick at low weight in a red-brick palette, if the alpha screenshot review accepts the saturation. Otherwise nothing from these mods.

## Worldgen notes

- Never place `spectralBlock` (vanishes), `blockTeleposer`, `blockMimic`, demon-portal blocks or `blood_tnt`.
