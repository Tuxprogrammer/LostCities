# Hardcore Ender Expansion

Family D. Mod ID `HardcoreEnderExpansion`, jar `HardcoreEnderExpansion-1.12.29-GTNH.jar`. Source read: `mods/HardcoreEnderExpansion/chylex/hee/init/BlockList.java` (lines 182-425; registry name is the `register` key, via `system/util/GameRegistryUtil.java` lines 16-22), `block/BlockRavagedBrick.java`, `block/BlockObsidianSpecial.java`, `block/BlockLaboratoryGlass.java`, `block/BlockSpookyLog.java`. Pack config `config/HardcoreEnderExpansion.cfg` has no block toggles. Sheets: `tools/block-contact-sheet/sheets/hardcoreenderexpansion/` (previews in `docs/previews/blocks/hardcoreenderexpansion/`, local only).

## Summary

- **49 registry names (`register(` calls in `BlockList.java`), about 75 block+meta entries; 16 useful.** Names are `HardcoreEnderExpansion:<key>`, for example `HardcoreEnderExpansion:ravaged_brick`.
- **The find is ravaged brick:** a crimson red brick with six metas (plain, cracked, and four "damaged" states with chunks knocked out of the face), plus smooth, slab, stairs and a brick fence. It is a complete ruined-brick kit, darker and redder than vanilla brick. Hardness 3.5 (the client adds hardness near many HEE spawners, but only in the End, `BlockRavagedBrick.getBlockHardness`).
- **Dark modern stone:** smooth, chiseled and pillar obsidian (`obsidian_special`, hardness 28, near-black blue) and the laboratory floor and glass (navy, hardness 30 to 40). They suit a sleek dark facade but are slow to mine; accents only.
- **Never:** `obsidian_end` (falling obsidian), `dungeon_puzzle`, `transport_beacon`, `biome_core` (unbreakable), `ender_goo` (fluid that fights water, hardness 150), `corrupted_energy_*`, `energy_cluster` (explodes when broken), `enhanced_tnt`, `custom_spawner`, `temple_end_portal`, `laser_beam`, `block_special_effects`, `spooky_log` (ticks, spawns Forest Ghosts, `BlockSpookyLog.updateTick`), the ores.
- **Tile entities:** essence altar, enhanced brewing stand and TNT, void chest, the four tables, spawner, beacon. None useful.
- **Light:** `ravaged_brick_glow` and `obsidian_special_glow` 15.
- **Ore rule:** `end_powder_ore`, `endium_ore`, `stardust_ore`, `igneous_rock_ore`, `instability_orb_ore`, `sphalerite` are ores; never as building blocks. The building blocks are not GT hosts.
- **Without the mod:** vanilla fallbacks below.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `HardcoreEnderExpansion:ravaged_brick:0` | Ravaged Brick | wall-main | yes | urban-brick | Crimson brick. Fallback `minecraft:brick_block` |
| `HardcoreEnderExpansion:ravaged_brick:1` | Ravaged Brick (cracked) | wall-main, rubble-ruin | yes | ruin | Fallback `minecraft:brick_block` |
| `HardcoreEnderExpansion:ravaged_brick:2-5` | Ravaged Brick (damaged 1 to 4) | rubble-ruin | yes | ruin | Faces with bricks knocked out; top and bottom stay whole. The best "damaged wall" texture set found in this slice |
| `HardcoreEnderExpansion:ravaged_brick_smooth:0` | Smooth Ravaged Brick | wall-accent, trim-pillar | yes | urban-brick | Framed tile; good banding |
| `HardcoreEnderExpansion:ravaged_brick_slab:0` (+8 top) | Ravaged Brick Slab | stairs-slab, roof | yes | urban-brick | |
| `HardcoreEnderExpansion:ravaged_brick_stairs` | Ravaged Brick Stairs | stairs-slab | yes | urban-brick | Vanilla stair metas |
| `HardcoreEnderExpansion:ravaged_brick_fence` | Ravaged Brick Fence | fence-rail | yes | urban-brick | A brick fence (vanilla `BlockFence`): low brick railings and parapet posts. Fallback `minecraft:nether_brick_fence` |
| `HardcoreEnderExpansion:ravaged_brick_glow:0` | Glowing Ravaged Brick | light | partly | fantasy | Glowing rune square, light 15 |
| `HardcoreEnderExpansion:obsidian_special:0` | Smooth Obsidian | wall-accent | partly | modern | Near-black; hardness 28. Fallback `minecraft:obsidian` |
| `HardcoreEnderExpansion:obsidian_special:1` | Chiseled Obsidian | wall-accent | partly | modern | |
| `HardcoreEnderExpansion:obsidian_special:2-4` | Obsidian Pillar (Y, N-S, E-W) | trim-pillar | partly | modern | |
| `HardcoreEnderExpansion:obsidian_special:5-6`, `obsidian_special_glow:0-6` | Particle and glowing obsidian | off-theme | no | magic | Particle effects; glow 15 |
| `HardcoreEnderExpansion:obsidian_stairs` | Obsidian Stairs | stairs-slab | partly | modern | Hardness 25 |
| `HardcoreEnderExpansion:laboratory_floor:0` | Laboratory Floor | floor | partly | modern | Navy tile, hardness 30 |
| `HardcoreEnderExpansion:laboratory_stairs` | Laboratory Stairs | stairs-slab | partly | modern | |
| `HardcoreEnderExpansion:laboratory_obsidian:0` | Laboratory Obsidian | wall-accent | partly | modern | Hardness 40 |
| `HardcoreEnderExpansion:laboratory_glass:0` | Laboratory Glass | glass | partly | modern | Dark-tinted connected glass, light opacity 5, hardness 5 |
| `HardcoreEnderExpansion:cinder:0` | Cinder | off-theme | no | | Speckled volcanic block |
| `HardcoreEnderExpansion:persegrit:0-15` | Persegrit | off-theme | no | alien | Purple bordered block |
| `HardcoreEnderExpansion:end_stone_terrain:0-2` | Infested, Burned, Enchanted End Stone | off-theme | no | alien | End terrain; meta 0 is named "infested" |
| `HardcoreEnderExpansion:endium_block:0` | Endium Block | off-theme | no | | Storage block |
| `HardcoreEnderExpansion:spooky_log`, `spooky_leaves` | Spooky Log, Leaves | off-theme | no | | Log spawns ghosts |
| `HardcoreEnderExpansion:crossed_decoration:*`, `death_flower`, `death_flower_pot` | End plants | off-theme | no | alien | |
| `HardcoreEnderExpansion:obsidian_end`, `dungeon_puzzle`, `transport_beacon`, `biome_core`, `ender_goo`, `corrupted_energy_high`, `corrupted_energy_low`, `energy_cluster`, `enhanced_tnt`, `custom_spawner`, `temple_end_portal`, `laser_beam`, `block_special_effects`, `essence_altar`, `enhanced_brewing_stand_block`, `void_chest`, `decomposition_table`, `experience_table`, `accumulation_table`, `energy_extraction_table`, ores | Mechanics, hazards, ores | off-theme | no | | Falling, unbreakable, explosive, fluid, spawner, machine or ore |

## Best variety pools

- **Ruined red brick (`wall-main`, `rubble-ruin`):** ravaged brick 0 (high weight), 1, 2-5 (low weight, more as the building decays). Pair with Twilight Forest underbrick for two red-brick shades with damage states.
- **Brick trim and caps (`wall-accent`, `stairs-slab`, `fence-rail`):** smooth ravaged brick, its slab and stairs, the brick fence.
- **Dark modern accents (`trim-pillar`, `wall-accent`):** obsidian pillar and chiseled obsidian, sparingly.

## Worldgen notes

- All useful blocks are plain blocks: no tile entity, no ticking.
- Keep the hardness-25-plus obsidian and laboratory blocks to accents.
