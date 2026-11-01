# Block catalog: Botania

Botania 1.13.42-GTNH (`Botania-1.13.42-GTNH.jar`, source `GTNewHorizons/Botania` at tag `1.13.42-GTNH`, read in `gtnh-src/source/Botania/`: `common/block/ModBlocks.java`, `ModFluffBlocks.java`, `common/block/decor/**`, `common/lib/LibBlockNames.java`, `assets/botania/lang/en_US.lang`), with the pack's `config/Botania.cfg`. Part of the decorative block catalog (decision log D25). Contact sheets: `tools/block-contact-sheet/sheets/botania/*.json`, rendered to `docs/previews/blocks/botania/`.

## Summary

- **Catalogued: 426 rows, 501 block+meta, all enabled; that is every decorative block Botania registers (501/501).** The only Botania block switch in the pack's config that touches building blocks is `darkQuartz.enabled`, and it is `true`; `18stones.enabled` only switches recipes. Counting rule: a stair block counts once (its meta is a facing), a half slab once (bottom 0, top +8), a double slab once, a quartz pillar once (metas 2-4 are axes); 16-color tinted blocks (petal block, buried petals, unstable cube, floating flower, unstable beacon) are one row each with meta `0-15`, and represent 16 block+meta each.
- **What is in, what is out.** Rows cover every Botania block without a tile entity that is not a machine, plus tile-entity blocks whose only job is decoration. **Excluded** (machines, mana logistics and technical blocks; about 55 registry entries): altars, spreader, pool, pylons, spark tinkerer, mana void/detector/pump/prism/bellows, enchanter, terra plate, brewery, alchemy and conjuration catalysts, open and crafty crates, drums, Eye of the Ancients, Ender Overseer, Starfield Creator, Mana Fluxfield, all red-string and corporea blocks, Force Relay, Luminizers, Spectral Rail, Hovering Hourglass, Incense Plate, Cacophonium, Cocoon of Caprice, Manastorm Charge, Life Imbuer, Elven Gateway Core, Tiny Planet, Cellular Block, the three platforms (camouflage tile entities; Infrangible is unbreakable), Temporary Bifrost (decays), Gaia head, Livingwood Avatar, functional and floating special flowers, and `fakeAir`.
- **Registry names.** Mod ID `Botania`. Full blocks use their `LibBlockNames` name (`Botania:livingrock`, `Botania:stone`, `Botania:customBrick`, `Botania:seaLamp`, `Botania:miniIsland` for floating flowers). Decorative quartz is `Botania:quartzType<Dark|Mana|Blaze|Lavender|Red|Elf|Sunny>` with stairs `quartzStairs<Type>` and slabs `quartzSlab<Type>Half` / `Full`. Other stairs, slabs and walls take the source block's name plus its meta: `livingrock0Stairs`, `customBrick3Slab`, `customBrick3SlabFull`, `stone10Stairs`, `biomeStoneB5Slab`, `prismarine0Wall`. Two walls carry several stones by meta: `Botania:stone0Wall` (0-3 andesite, basalt, diorite, granite) and `Botania:biomeStoneA0Wall` (0-7, the metamorphic cobblestones). Panes: `manaGlassPane`, `elfGlassPane`, `bifrostPermPane`. *Verified* from the registration code (`BlockModStairs`, `BlockModSlab.register`, `BlockModWall`, `BlockVariantWall`, `BlockModPane`); FML prefixes `Botania:`.
- **Gameplay effects.** **Light:** Managlass, Alfglass, Bifrost and all three panes emit **light 15** (glass that lights a room by itself); Sea Lantern, Blaze Lamp, glimmering flowers, floating flowers and Mana Flash 15; glimmering livingwood and dreamwood 12; shimmering mushrooms and buried petals 3. **No mob spawning:** the pack sets `noMobSpawnOnBlocks=true`, so no mob spawns naturally on any Botania full block, slab or tile-entity block; a Botania floor makes a room spawn-proof (upstream's spawners still work). **Spreading:** the six alternate grasses spread onto nearby plain dirt at light 9+. **Tile entities:** only the rows in the last section. **No gravity** blocks. **Transparency:** glass and panes (translucent pass), plants, vines, Unstable Cube (half size), Trodden Dirt (15/16 high under air). **Animated textures:** Sea Lantern, Prismarine, Shimmerrock, Shimmerwood, Bifrost, Enchanted Soil. **No connected textures**; Alfglass picks one of four textures by position.
- **GT hosts.** No Botania block is a GT ore or boulder host; GT matches only vanilla stone, cobblestone, stained hardened clay, netherrack, end stone, its own stones and Et Futurum deepslate and tuff ([ore-veins.md](../../00-pack-integration/ore-veins.md)). Botania's End Stone Bricks are a separate block from `minecraft:end_stone`; its "basalt" is not GT basalt.
- **Fit with upstream's look: 152 yes, 212 partly, 62 no.**
- **Add-ons.** The pack's only Botania add-on, Botanic Horizons 1.12.10-GTNH, registers only multiblock machine controllers (Synthetic Elven Gateway, Mana Fountain, Fountains of Alchemy and Conjuration); it adds no decorative blocks, so it has no section and no sheet.

Rows per role (a block can hold several roles; each 16-color row counts once):

| Role | Blocks+meta |
|---|---|
| `wall-main` | 38 |
| `wall-accent` | 64 |
| `trim-pillar` | 29 |
| `floor` | 38 |
| `ceiling` | 1 |
| `roof` | 6 |
| `glass` | 2 |
| `pane` | 2 |
| `street` | 7 |
| `sidewalk` | 8 |
| `stairs-slab` | 194 |
| `rubble-ruin` | 30 |
| `light` | 21 |
| `fence-rail` | 18 |
| `furniture-deco` | 4 |
| `foliage` | 73 |
| `special` | 6 |
| `off-theme` | 62 |

### Et Futurum gaps Botania fills

The pack switches off Et Futurum's stones, prismarine and sea lantern ([block-mapping.md](../../00-pack-integration/block-mapping.md)). Botania covers each:

| Missing Et Futurum block | Botania block | Coverage |
|---|---|---|
| `sea_lantern` | `Botania:seaLamp` | Same look and light 15 (D19 already uses it) |
| `granite`, `andesite`, `diorite` and polished forms | `Botania:stone` 0-3 raw, 4-7 polished | Full; adds bricks (8-11) and chiseled bricks (12-15) that 1.8 never had |
| their stairs, slabs, walls | `stone<n>Stairs`, `stone<n>Slab` for raw (0-3) and bricks (8-11); `stone0Wall` 0-3 | **No polished stairs or slabs**, as D19 notes; brick stairs are the nearest |
| `prismarine`, `prismarine_bricks`, `dark_prismarine` with stairs, slabs and wall | `Botania:prismarine` 0-2, `prismarine<n>Stairs`, `Slab`, `prismarine0Wall` | Full |
| (not missing) `end_bricks` with stairs, slab and wall | `Botania:endStoneBrick` 0, `endStoneBrick0Stairs`, `Slab` | Et Futurum's end stone bricks are on in the pack (`enableChorusFruit=true`); Botania's are a second set and add chiseled, ender bricks and pillar |

## Blocks

### Livingrock, livingwood, dreamwood, shimmer

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Botania:livingrock:0` | Livingrock | `wall-main`, `floor` | partly | standard, border | pale beige-white mottled stone, like weathered limestone; stairs, slab and wall exist |
| `Botania:livingrock:1` | Livingrock Brick | `wall-main`, `wall-accent` | yes | standard, border, new: old town | pale stone brick; a light alternative to stone brick for `#`; stairs and slab exist |
| `Botania:livingrock:2` | Mossy Livingrock Brick | `rubble-ruin`, `wall-main` | yes | standard, overgrown ruins | pale brick with moss |
| `Botania:livingrock:3` | Cracked Livingrock Brick | `rubble-ruin`, `wall-main` | yes | standard, overgrown ruins | pale cracked brick |
| `Botania:livingrock:4` | Chiseled Livingrock Brick | `wall-accent`, `trim-pillar` | yes | standard, border | pale carved square |
| `Botania:livingwood:0` | Livingwood | `trim-pillar`, `wall-accent` | partly | new: old town | dark red-brown wood, same texture on all faces (no log rings); stairs, slab and wall exist |
| `Botania:livingwood:1` | Livingwood Planks | `floor`, `wall-main` | yes | standard, new: old town | dark red-brown planks, close to dark oak; stairs and slab exist |
| `Botania:livingwood:2` | Mossy Livingwood Planks | `floor`, `rubble-ruin` | yes | standard, overgrown ruins | dark planks with moss |
| `Botania:livingwood:3` | Framed Livingwood | `wall-accent`, `trim-pillar` | yes | standard, new: old town | dark wood panel, framed |
| `Botania:livingwood:4` | Pattern Framed Livingwood | `wall-accent`, `furniture-deco` | partly | new: old town | dark carved panel |
| `Botania:livingwood:5` | Glimmering Livingwood | `light`, `special` | partly | new: old town, overgrown ruins | light 12; dark wood with green sparkles; a hidden light source in a wood wall |
| `Botania:dreamwood:0` | Dreamwood | `trim-pillar` | partly | standard | pale blue-gray wood, all faces alike; stairs, slab and wall exist |
| `Botania:dreamwood:1` | Dreamwood Planks | `floor`, `wall-main` | partly | standard | pale gray-cyan planks; reads as whitewashed or bleached wood; stairs and slab exist |
| `Botania:dreamwood:2` | Mossy Dreamwood Planks | `floor`, `rubble-ruin` | partly | overgrown ruins | pale planks with teal moss |
| `Botania:dreamwood:3` | Framed Dreamwood | `wall-accent` | partly | standard | pale framed panel with glyphs |
| `Botania:dreamwood:4` | Pattern Framed Dreamwood | `off-theme` | no | - | elven glyph panel |
| `Botania:dreamwood:5` | Glimmering Dreamwood | `light`, `off-theme` | no | - | light 12; blue-white sparkle texture, reads as magic |
| `Botania:shimmerrock:0` | Shimmerrock | `off-theme` | no | - | animated rainbow-pastel stone; stairs and slab exist |
| `Botania:shimmerwoodPlanks:0` | Shimmerwood Planks | `off-theme` | no | - | animated rainbow planks; stairs and slab exist |

### 1.8 stones (`Botania:stone`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Botania:stone:0` | Andesite | `wall-main`, `floor`, `rubble-ruin` | yes | standard, border | mid gray speckled raw stone; stairs, slab (`stone0Stairs`, `stone0Slab`) and wall (`stone0Wall:0`) exist; not a GT host (GT keys on its own and vanilla blocks) |
| `Botania:stone:1` | Basalt | `wall-main`, `floor`, `rubble-ruin` | partly | standard, border | near-black raw stone; stairs, slab (`stone1Stairs`, `stone1Slab`) and wall (`stone0Wall:1`) exist; not a GT host (GT keys on its own and vanilla blocks); Botania's basalt is a near-black stone, not GT basalt |
| `Botania:stone:2` | Diorite | `wall-main`, `floor`, `rubble-ruin` | yes | standard, border | white speckled raw stone; stairs, slab (`stone2Stairs`, `stone2Slab`) and wall (`stone0Wall:2`) exist; not a GT host (GT keys on its own and vanilla blocks) |
| `Botania:stone:3` | Granite | `wall-main`, `floor`, `rubble-ruin` | yes | standard, border, desert | salmon pink speckled raw stone; stairs, slab (`stone3Stairs`, `stone3Slab`) and wall (`stone0Wall:3`) exist; not a GT host (GT keys on its own and vanilla blocks) |
| `Botania:stone:4` | Polished Andesite | `wall-main`, `floor`, `wall-accent` | yes | standard, border | mid gray speckled polished, with a thin border; no stairs or slab of its own |
| `Botania:stone:5` | Polished Basalt | `wall-main`, `floor`, `wall-accent` | yes | standard, border | near-black polished, with a thin border; no stairs or slab of its own |
| `Botania:stone:6` | Polished Diorite | `wall-main`, `floor`, `wall-accent` | yes | standard, border | white speckled polished, with a thin border; no stairs or slab of its own; D19's `polished_diorite` |
| `Botania:stone:7` | Polished Granite | `wall-main`, `floor`, `wall-accent` | yes | standard, border, desert | salmon pink speckled polished, with a thin border; no stairs or slab of its own |
| `Botania:stone:8` | Andesite Bricks | `wall-main`, `wall-accent` | yes | standard, border | mid gray speckled brick in the stone-brick pattern; stairs and slab exist (`stone8Stairs`) |
| `Botania:stone:9` | Basalt Bricks | `wall-main`, `wall-accent` | yes | standard, border | near-black brick in the stone-brick pattern; stairs and slab exist (`stone9Stairs`) |
| `Botania:stone:10` | Diorite Bricks | `wall-main`, `wall-accent` | yes | standard, border | white speckled brick in the stone-brick pattern; stairs and slab exist (`stone10Stairs`) |
| `Botania:stone:11` | Granite Bricks | `wall-main`, `wall-accent` | yes | standard, border, desert | salmon pink speckled brick in the stone-brick pattern; stairs and slab exist (`stone11Stairs`) |
| `Botania:stone:12` | Chiseled Andesite Brick | `wall-accent`, `trim-pillar` | yes | standard, border | mid gray speckled concentric-square carving |
| `Botania:stone:13` | Chiseled Basalt Brick | `wall-accent`, `trim-pillar` | yes | standard, border | near-black concentric-square carving |
| `Botania:stone:14` | Chiseled Diorite Brick | `wall-accent`, `trim-pillar` | yes | standard, border | white speckled concentric-square carving |
| `Botania:stone:15` | Chiseled Granite Brick | `wall-accent`, `trim-pillar` | yes | standard, border, desert | salmon pink speckled concentric-square carving |

### Metamorphic stones (`Botania:biomeStoneA`, `biomeStoneB`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Botania:biomeStoneA:0` | Metamorphic Forest Stone | `off-theme` | no | - | dark green smooth stone; only made by the Marimorphosis flower, so rare in play; off-theme: saturated fantasy tint |
| `Botania:biomeStoneA:1` | Metamorphic Plains Stone | `wall-main`, `floor` | partly | new: old town | dark olive smooth stone |
| `Botania:biomeStoneA:2` | Metamorphic Mountain Stone | `wall-main`, `floor` | partly | new: old town | dark brown smooth stone |
| `Botania:biomeStoneA:3` | Metamorphic Fungal Stone | `off-theme` | no | - | dark purple smooth stone; off-theme: saturated fantasy tint |
| `Botania:biomeStoneA:4` | Metamorphic Swamp Stone | `off-theme` | no | - | dark teal-green smooth stone; off-theme: saturated fantasy tint |
| `Botania:biomeStoneA:5` | Metamorphic Desert Stone | `wall-main`, `floor` | yes | desert | ochre smooth stone |
| `Botania:biomeStoneA:6` | Metamorphic Taiga Stone | `wall-main`, `floor` | partly | new: old town | teal smooth stone |
| `Botania:biomeStoneA:7` | Metamorphic Mesa Stone | `wall-main`, `floor` | yes | desert | dark red smooth stone |
| `Botania:biomeStoneA:8` | Metamorphic Forest Cobblestone | `off-theme` | no | - | dark green cobble; wall `biomeStoneA0Wall:0` |
| `Botania:biomeStoneA:9` | Metamorphic Plains Cobblestone | `rubble-ruin` | partly | new: old town | dark olive cobble; wall `biomeStoneA0Wall:1` |
| `Botania:biomeStoneA:10` | Metamorphic Mountain Cobblestone | `rubble-ruin` | partly | new: old town | dark brown cobble; wall `biomeStoneA0Wall:2` |
| `Botania:biomeStoneA:11` | Metamorphic Fungal Cobblestone | `off-theme` | no | - | dark purple cobble; wall `biomeStoneA0Wall:3` |
| `Botania:biomeStoneA:12` | Metamorphic Swamp Cobblestone | `off-theme` | no | - | dark teal-green cobble; wall `biomeStoneA0Wall:4` |
| `Botania:biomeStoneA:13` | Metamorphic Desert Cobblestone | `rubble-ruin` | yes | desert | ochre cobble; wall `biomeStoneA0Wall:5` |
| `Botania:biomeStoneA:14` | Metamorphic Taiga Cobblestone | `rubble-ruin` | partly | new: old town | teal cobble; wall `biomeStoneA0Wall:6` |
| `Botania:biomeStoneA:15` | Metamorphic Mesa Cobblestone | `rubble-ruin` | yes | desert | dark red cobble; wall `biomeStoneA0Wall:7` |
| `Botania:biomeStoneB:0` | Metamorphic Forest Stone Bricks | `off-theme` | no | - | dark green stone brick |
| `Botania:biomeStoneB:1` | Metamorphic Plains Stone Bricks | `wall-main`, `wall-accent` | partly | new: old town | dark olive stone brick |
| `Botania:biomeStoneB:2` | Metamorphic Mountain Stone Bricks | `wall-main`, `wall-accent` | partly | new: old town | dark brown stone brick |
| `Botania:biomeStoneB:3` | Metamorphic Fungal Stone Bricks | `off-theme` | no | - | dark purple stone brick |
| `Botania:biomeStoneB:4` | Metamorphic Swamp Stone Bricks | `off-theme` | no | - | dark teal-green stone brick |
| `Botania:biomeStoneB:5` | Metamorphic Desert Stone Bricks | `wall-main`, `wall-accent` | yes | desert | ochre stone brick |
| `Botania:biomeStoneB:6` | Metamorphic Taiga Stone Bricks | `wall-main`, `wall-accent` | partly | new: old town | teal stone brick |
| `Botania:biomeStoneB:7` | Metamorphic Mesa Stone Bricks | `wall-main`, `wall-accent` | yes | desert | dark red stone brick |
| `Botania:biomeStoneB:8` | Chiseled Metamorphic Forest Stone Brick | `off-theme` | no | - | dark green carved square; no stairs or slab |
| `Botania:biomeStoneB:9` | Chiseled Metamorphic Plains Stone Brick | `wall-accent`, `trim-pillar` | partly | new: old town | dark olive carved square; no stairs or slab |
| `Botania:biomeStoneB:10` | Chiseled Metamorphic Mountain Stone Brick | `wall-accent`, `trim-pillar` | partly | new: old town | dark brown carved square; no stairs or slab |
| `Botania:biomeStoneB:11` | Chiseled Metamorphic Fungal Stone Brick | `off-theme` | no | - | dark purple carved square; no stairs or slab |
| `Botania:biomeStoneB:12` | Chiseled Metamorphic Swamp Stone Brick | `off-theme` | no | - | dark teal-green carved square; no stairs or slab |
| `Botania:biomeStoneB:13` | Chiseled Metamorphic Desert Stone Brick | `wall-accent`, `trim-pillar` | yes | desert | ochre carved square; no stairs or slab |
| `Botania:biomeStoneB:14` | Chiseled Metamorphic Taiga Stone Brick | `wall-accent`, `trim-pillar` | partly | new: old town | teal carved square; no stairs or slab |
| `Botania:biomeStoneB:15` | Chiseled Metamorphic Mesa Stone Brick | `wall-accent`, `trim-pillar` | yes | desert | dark red carved square; no stairs or slab |

### Decorative quartz (`Botania:quartzType<Type>`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Botania:quartzTypeDark:0` | Smokey Quartz | `wall-main`, `wall-accent`, `floor` | yes | standard, border | charcoal-black quartz block; stairs `quartzStairsDark`, slabs `quartzSlabDarkHalf`/`Full`; config `darkQuartz.enabled=true` in the pack |
| `Botania:quartzTypeDark:1` | Chiseled Smokey Quartz | `wall-accent`, `trim-pillar` | yes | standard, border | charcoal-black chiseled; texture differs from vanilla's |
| `Botania:quartzTypeDark:2-4` | Pillar Smokey Quartz | `trim-pillar` | yes | standard, border | charcoal-black pillar; meta 2 vertical, 3 and 4 horizontal (axis), as vanilla quartz |
| `Botania:quartzTypeMana:0` | Mana Quartz | `wall-main`, `wall-accent` | yes | standard | pale ice blue quartz block; stairs `quartzStairsMana`, slabs `quartzSlabManaHalf`/`Full` |
| `Botania:quartzTypeMana:1` | Chiseled Mana Quartz | `wall-accent`, `trim-pillar` | yes | standard | pale ice blue chiseled; texture differs from vanilla's |
| `Botania:quartzTypeMana:2-4` | Pillar Mana Quartz | `trim-pillar` | yes | standard | pale ice blue pillar; meta 2 vertical, 3 and 4 horizontal (axis), as vanilla quartz |
| `Botania:quartzTypeBlaze:0` | Blaze Quartz | `wall-main`, `wall-accent` | partly | standard, desert | pale peach quartz block; stairs `quartzStairsBlaze`, slabs `quartzSlabBlazeHalf`/`Full` |
| `Botania:quartzTypeBlaze:1` | Chiseled Blaze Quartz | `wall-accent`, `trim-pillar` | partly | standard, desert | pale peach chiseled; texture differs from vanilla's |
| `Botania:quartzTypeBlaze:2-4` | Pillar Blaze Quartz | `trim-pillar` | partly | standard, desert | pale peach pillar; meta 2 vertical, 3 and 4 horizontal (axis), as vanilla quartz |
| `Botania:quartzTypeLavender:0` | Lavender Quartz | `wall-accent` | partly | standard | pale lavender quartz block; stairs `quartzStairsLavender`, slabs `quartzSlabLavenderHalf`/`Full` |
| `Botania:quartzTypeLavender:1` | Chiseled Lavender Quartz | `wall-accent`, `trim-pillar` | partly | standard | pale lavender chiseled; texture differs from vanilla's |
| `Botania:quartzTypeLavender:2-4` | Pillar Lavender Quartz | `trim-pillar` | partly | standard | pale lavender pillar; meta 2 vertical, 3 and 4 horizontal (axis), as vanilla quartz |
| `Botania:quartzTypeRed:0` | Redquartz | `wall-accent` | partly | standard | pale pink quartz block; stairs `quartzStairsRed`, slabs `quartzSlabRedHalf`/`Full` |
| `Botania:quartzTypeRed:1` | Chiseled Redquartz | `wall-accent`, `trim-pillar` | partly | standard | pale pink chiseled; texture differs from vanilla's |
| `Botania:quartzTypeRed:2-4` | Pillar Redquartz | `trim-pillar` | partly | standard | pale pink pillar; meta 2 vertical, 3 and 4 horizontal (axis), as vanilla quartz |
| `Botania:quartzTypeElf:0` | Elven Quartz | `wall-accent` | partly | standard | pale mint green quartz block; stairs `quartzStairsElf`, slabs `quartzSlabElfHalf`/`Full` |
| `Botania:quartzTypeElf:1` | Chiseled Elven Quartz | `wall-accent`, `trim-pillar` | partly | standard | pale mint green chiseled; texture differs from vanilla's |
| `Botania:quartzTypeElf:2-4` | Pillar Elven Quartz | `trim-pillar` | partly | standard | pale mint green pillar; meta 2 vertical, 3 and 4 horizontal (axis), as vanilla quartz |
| `Botania:quartzTypeSunny:0` | Sunny Quartz | `wall-main`, `wall-accent` | yes | standard, desert | cream quartz block; stairs `quartzStairsSunny`, slabs `quartzSlabSunnyHalf`/`Full` |
| `Botania:quartzTypeSunny:1` | Chiseled Sunny Quartz | `wall-accent`, `trim-pillar` | yes | standard, desert | cream chiseled; texture differs from vanilla's |
| `Botania:quartzTypeSunny:2-4` | Pillar Sunny Quartz | `trim-pillar` | yes | standard, desert | cream pillar; meta 2 vertical, 3 and 4 horizontal (axis), as vanilla quartz |

### Custom bricks, azulejo, end stone bricks, prismarine, pavement

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Botania:customBrick:0` | Hellish Brick | `wall-main`, `wall-accent` | partly | standard, new: old town | dark red brick, between nether brick and red nether brick; stairs and slab exist |
| `Botania:customBrick:1` | Soul Brick | `wall-main`, `wall-accent` | partly | new: old town | dark brown brick; stairs and slab exist |
| `Botania:customBrick:2` | Frosty Brick | `wall-main`, `wall-accent` | partly | standard | white-ice brick, like whitewashed or glazed brick; stairs and slab exist |
| `Botania:customBrick:3` | Roof Tile | `roof`, `wall-accent` | yes | standard, desert, new: old town, new: Mediterranean quarter | terracotta-red overlapping tiles; stairs and slab make proper pitched roofs, which upstream lacks; not a boulder host, unlike terracotta |
| `Botania:customBrick:4` | Azulejo 0 | `wall-accent`, `floor` | partly | new: Mediterranean quarter | blue-on-white glazed tile, 12 patterns; Portuguese tile facades, kitchen and bath floors; no stairs or slabs |
| `Botania:customBrick:5` | Azulejo 1 | `wall-accent`, `floor` | partly | new: Mediterranean quarter | blue-on-white glazed tile pattern |
| `Botania:customBrick:6` | Azulejo 2 | `wall-accent`, `floor` | partly | new: Mediterranean quarter | blue-on-white glazed tile pattern |
| `Botania:customBrick:7` | Azulejo 3 | `wall-accent`, `floor` | partly | new: Mediterranean quarter | blue-on-white glazed tile pattern |
| `Botania:customBrick:8` | Azulejo 4 | `wall-accent`, `floor` | partly | new: Mediterranean quarter | blue-on-white glazed tile pattern |
| `Botania:customBrick:9` | Azulejo 5 | `wall-accent`, `floor` | partly | new: Mediterranean quarter | blue-on-white glazed tile pattern |
| `Botania:customBrick:10` | Azulejo 6 | `wall-accent`, `floor` | partly | new: Mediterranean quarter | blue-on-white glazed tile pattern |
| `Botania:customBrick:11` | Azulejo 7 | `wall-accent`, `floor` | partly | new: Mediterranean quarter | blue-on-white glazed tile pattern |
| `Botania:customBrick:12` | Azulejo 8 | `wall-accent`, `floor` | partly | new: Mediterranean quarter | blue-on-white glazed tile pattern |
| `Botania:customBrick:13` | Azulejo 9 | `wall-accent`, `floor` | partly | new: Mediterranean quarter | blue-on-white glazed tile pattern |
| `Botania:customBrick:14` | Azulejo 10 | `wall-accent`, `floor` | partly | new: Mediterranean quarter | blue-on-white glazed tile pattern |
| `Botania:customBrick:15` | Azulejo 11 | `wall-accent`, `floor` | partly | new: Mediterranean quarter | blue-on-white glazed tile pattern |
| `Botania:endStoneBrick:0` | End Stone Bricks | `wall-main`, `wall-accent` | partly | desert | pale yellow brick; a sandstone-brick stand-in for the desert style; stairs and slab exist; a different block from `minecraft:end_stone`, so not a host |
| `Botania:endStoneBrick:1` | Chiseled End Stone Brick | `wall-accent`, `trim-pillar` | partly | desert | pale yellow carved |
| `Botania:endStoneBrick:2` | Ender Bricks | `wall-accent` | partly | standard | mauve-purple brick, like 1.9 purpur; stairs and slab exist |
| `Botania:endStoneBrick:3` | Pillar Ender Bricks | `trim-pillar` | partly | standard | mauve pillar, not rotatable |
| `Botania:prismarine:0` | Prismarine | `wall-main`, `wall-accent` | partly | new: waterfront | animated teal-green mottled stone (interpolated); stairs, slab and wall exist; fills Et Futurum's switched-off prismarine |
| `Botania:prismarine:1` | Prismarine Bricks | `wall-main`, `wall-accent` | partly | new: waterfront | teal brick; stairs and slab exist |
| `Botania:prismarine:2` | Dark Prismarine | `wall-accent`, `floor` | partly | new: waterfront, standard | dark green-slate tiles; stairs and slab exist |
| `Botania:pavement:0` | White Portuguese Pavement | `sidewalk`, `street`, `floor` | yes | standard, border, new: Mediterranean quarter | cream setts, gray grout; 4x4 small-sett paving; stairs `pavement0Stairs` and slab `pavement0Slab` exist; the best sidewalk block in this family |
| `Botania:pavement:1` | Black Portuguese Pavement | `sidewalk`, `street`, `floor` | yes | standard, border | dark gray setts; 4x4 small-sett paving; stairs `pavement1Stairs` and slab `pavement1Slab` exist |
| `Botania:pavement:2` | Blue Portuguese Pavement | `sidewalk`, `street`, `floor` | partly | standard, new: Mediterranean quarter | blue setts; 4x4 small-sett paving; stairs `pavement2Stairs` and slab `pavement2Slab` exist |
| `Botania:pavement:3` | Red Portuguese Pavement | `sidewalk`, `street`, `floor` | partly | standard, new: Mediterranean quarter, desert | red setts; 4x4 small-sett paving; stairs `pavement3Stairs` and slab `pavement3Slab` exist |
| `Botania:pavement:4` | Yellow Portuguese Pavement | `sidewalk`, `street`, `floor` | partly | standard, desert | ochre setts; 4x4 small-sett paving; stairs `pavement4Stairs` and slab `pavement4Slab` exist |
| `Botania:pavement:5` | Green Portuguese Pavement | `sidewalk`, `street`, `floor` | partly | standard, park | green setts; 4x4 small-sett paving; stairs `pavement5Stairs` and slab `pavement5Slab` exist |

### Glass, panes and lamps

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Botania:manaGlass:0` | Managlass | `glass` | yes | standard, border | nearly clear (20% alpha, faint lavender), frameless; **light 15**; translucent render pass; reads as modern plate glass |
| `Botania:elfGlass:0` | Alfglass | `glass` | partly | standard | nearly clear, faint cyan hexagons; **light 15**; texture picked at random per position from 4; elven item (Alfheim trade) |
| `Botania:bifrostPerm:0` | Bifrost Block | `off-theme` | no | - | animated rainbow glass, **light 15**; random sparkle particles |
| `Botania:manaGlassPane:0` | Managlass Pane | `pane` | yes | standard, border | pane of Managlass; **light 15** |
| `Botania:elfGlassPane:0` | Alfglass Pane | `pane` | partly | standard | pane of Alfglass; **light 15** |
| `Botania:bifrostPermPane:0` | Bifrost Pane | `off-theme` | no | - | rainbow pane; **light 15** |
| `Botania:seaLamp:0` | Sea Lantern | `light`, `ceiling` | yes | standard, border, new: waterfront | light 15, animated pale-teal glow block; D19's `sea_lantern` (Et Futurum's is off) |
| `Botania:blazeBlock:0` | Blaze Lamp | `light`, `special` | partly | new: industrial, desert | light 15; woven orange-gold block; also a furnace fuel |

### Storage blocks and other solids

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Botania:storage:0` | Block of Manasteel | `special` | partly | standard | blue metal; treasure for the `!` precious-block mix only, not a wall; Botania progression material |
| `Botania:storage:1` | Block of Terrasteel | `special` | partly | standard | green metal; treasure for the `!` precious-block mix only, not a wall; Botania progression material |
| `Botania:storage:2` | Block of Elementium | `special` | partly | standard | pink-purple metal; treasure for the `!` precious-block mix only, not a wall; Botania progression material |
| `Botania:storage:3` | Block of Mana Diamond | `off-theme` | no | standard | pale cyan gem; treasure for the `!` precious-block mix only, not a wall; Botania progression material |
| `Botania:storage:4` | Block of Dragonstone | `off-theme` | no | standard | pink gem; treasure for the `!` precious-block mix only, not a wall; Botania progression material |
| `Botania:reedBlock:0` | Reed Block | `foliage`, `fence-rail` | partly | park | green bamboo-like pillar (rotatable axis in meta, `BlockRotatedPillar`); stairs, slab and wall exist; park screens and fences |
| `Botania:thatch:0` | Thatch | `roof` | partly | new: old town | golden straw; Material.grass; stairs and slab exist; rural cottage roofs, off the urban look |
| `Botania:petalBlock:0-15` | Petal Block (16 colors) | `foliage` | partly | park | tinted leaf-like block in the 16 dye colors (tint not shown on the sheet); flowering hedges in parks |
| `Botania:unstableBlock:0-15` | Unstable Cube (16 colors) | `off-theme` | no | - | half-size floating cube with lightning particles, tinted per color |

### Soils, grass, plants

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Botania:dirtPath:0` | Trodden Dirt | `sidewalk`, `street` | yes | standard, park, overgrown ruins | dirt path; 15/16 high when air is above (like 1.9 grass path); slab `dirtPath0Slab` exists; park and back-lot paths |
| `Botania:enchantedSoil:0` | Enchanted Soil | `foliage` | partly | park | bright animated grass top; drops dirt |
| `Botania:altGrass:0` | Dry Grass | `foliage` | yes | desert, overgrown ruins | yellowed grass; abandoned lots and desert; **spreads** onto plain dirt nearby at light 9+ (random tick) |
| `Botania:altGrass:1` | Golden Grass | `foliage` | partly | desert | golden-orange grass; spreads like meta 0 |
| `Botania:altGrass:2` | Vivid Grass | `foliage` | yes | park | deep green grass; spreads like meta 0 |
| `Botania:altGrass:3` | Scorched Grass | `foliage` | partly | overgrown ruins | burnt red grass; ruins after fire; spreads like meta 0 |
| `Botania:altGrass:4` | Infused Grass | `off-theme` | no | - | teal fantasy grass; spreads like meta 0 |
| `Botania:altGrass:5` | Mutated Grass | `off-theme` | no | - | purple fantasy grass; spreads like meta 0 |
| `Botania:solidVine:0` | Solid Vine | `foliage`, `rubble-ruin` | yes | overgrown ruins | vine that never spreads or decays; placement meta is attachment sides as vanilla vines; the overgrowth block for ruined facades |
| `Botania:root:0` | Living Root | `off-theme` | no | - | dark root block (Material.plants) |
| `Botania:felPumpkin:0` | Fel Pumpkin | `furniture-deco` | partly | overgrown ruins | carved pumpkin with a different face; no light; on an iron-golem frame it summons a blaze, so never place it on iron blocks |
| `Botania:buriedPetals:0-15` | Buried Petals (16 colors) | `foliage` | partly | park, overgrown ruins | flat petal layer on the ground; **light 3**; tinted (not shown on the sheet) |
| `Botania:flower:0` | Mystical White Flower | `foliage` | yes | standard, park, overgrown ruins | cross plant; needs grass or dirt below; also generates in the wild |
| `Botania:flower:1` | Mystical Orange Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:2` | Mystical Magenta Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:3` | Mystical Light Blue Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:4` | Mystical Yellow Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:5` | Mystical Lime Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:6` | Mystical Pink Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:7` | Mystical Gray Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:8` | Mystical Light Gray Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:9` | Mystical Cyan Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:10` | Mystical Purple Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:11` | Mystical Blue Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:12` | Mystical Brown Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:13` | Mystical Green Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:14` | Mystical Red Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:flower:15` | Mystical Black Flower | `foliage` | yes | standard, park, overgrown ruins |  |
| `Botania:shinyFlower:0` | Glimmering White Flower | `foliage`, `light` | partly | park | flower with **light 15**; lit flower beds and planters |
| `Botania:shinyFlower:1` | Glimmering Orange Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:2` | Glimmering Magenta Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:3` | Glimmering Light Blue Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:4` | Glimmering Yellow Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:5` | Glimmering Lime Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:6` | Glimmering Pink Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:7` | Glimmering Gray Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:8` | Glimmering Light Gray Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:9` | Glimmering Cyan Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:10` | Glimmering Purple Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:11` | Glimmering Blue Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:12` | Glimmering Brown Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:13` | Glimmering Green Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:14` | Glimmering Red Flower | `foliage`, `light` | partly | park |  |
| `Botania:shinyFlower:15` | Glimmering Black Flower | `foliage`, `light` | partly | park |  |
| `Botania:mushroom:0` | White Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins | small mushroom, **light 3**; damp basements and ruins |
| `Botania:mushroom:1` | Orange Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:2` | Magenta Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:3` | Light Blue Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:4` | Yellow Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:5` | Lime Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:6` | Pink Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:7` | Gray Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:8` | Light Gray Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:9` | Cyan Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:10` | Purple Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:11` | Blue Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:12` | Brown Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:13` | Green Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:14` | Red Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:mushroom:15` | Black Shimmering Mushroom | `foliage`, `rubble-ruin` | partly | overgrown ruins |  |
| `Botania:doubleFlower1:0` | Tall Mystical White Flower | `foliage` | yes | park, overgrown ruins | two-block plant: lower half meta 0-7, upper half meta 8 (as vanilla double plants) |
| `Botania:doubleFlower1:1` | Tall Mystical Orange Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower1:2` | Tall Mystical Magenta Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower1:3` | Tall Mystical Light Blue Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower1:4` | Tall Mystical Yellow Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower1:5` | Tall Mystical Lime Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower1:6` | Tall Mystical Pink Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower1:7` | Tall Mystical Gray Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower2:0` | Tall Mystical Light Gray Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower2:1` | Tall Mystical Cyan Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower2:2` | Tall Mystical Purple Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower2:3` | Tall Mystical Blue Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower2:4` | Tall Mystical Brown Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower2:5` | Tall Mystical Green Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower2:6` | Tall Mystical Red Flower | `foliage` | yes | park, overgrown ruins |  |
| `Botania:doubleFlower2:7` | Tall Mystical Black Flower | `foliage` | yes | park, overgrown ruins |  |

### Decorative blocks with a tile entity

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Botania:miniIsland:0-15` | Floating Flower (16 colors) | `off-theme` | no | - | tile entity; floating grass island with a flower, **light 15**; magic look |
| `Botania:tinyPotato:0` | Tiny Potato | `furniture-deco` | partly | - | tile entity (stores a custom name); a desk easter egg at most |
| `Botania:teruTeruBozu:0` | Teru Teru Bozu | `furniture-deco` | partly | - | tile entity; hanging cloth charm; can change the weather when activated, so place sparingly |
| `Botania:manaFlame:0` | Mana Flash | `light`, `special` | partly | - | tile entity; an invisible, intangible light source (light 15, colored, with particles); upstream has no such block |
| `Botania:manaBeacon:0-15` | Unstable Beacon (16 colors) | `off-theme` | no | - | tile entity; particle beam |

### Stairs, slabs and walls

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Botania:livingrock0Stairs` | Livingrock Stairs | `stairs-slab` | yes | standard, border | meta = facing, as vanilla stairs |
| `Botania:livingrock0Slab` | Livingrock Slab | `stairs-slab` | yes | standard, border | 0 bottom, 8 top |
| `Botania:livingrock0SlabFull` | Livingrock Double Slab | `stairs-slab` | yes | standard, border | double slab; looks like the block |
| `Botania:livingrock1Stairs` | Livingrock Brick Stairs | `stairs-slab` | yes | standard, border | meta = facing, as vanilla stairs |
| `Botania:livingrock1Slab` | Livingrock Brick Slab | `stairs-slab` | yes | standard, border | 0 bottom, 8 top |
| `Botania:livingrock1SlabFull` | Livingrock Brick Double Slab | `stairs-slab` | yes | standard, border | double slab; looks like the block |
| `Botania:livingwood0Stairs` | Livingwood Stairs | `stairs-slab` | partly | new: old town | meta = facing, as vanilla stairs |
| `Botania:livingwood0Slab` | Livingwood Slab | `stairs-slab` | partly | new: old town | 0 bottom, 8 top |
| `Botania:livingwood0SlabFull` | Livingwood Double Slab | `stairs-slab` | partly | new: old town | double slab; looks like the block |
| `Botania:livingwood1Stairs` | Livingwood Plank Stairs | `stairs-slab` | yes | standard, new: old town | meta = facing, as vanilla stairs |
| `Botania:livingwood1Slab` | Livingwood Plank Slab | `stairs-slab` | yes | standard, new: old town | 0 bottom, 8 top |
| `Botania:livingwood1SlabFull` | Livingwood Plank Double Slab | `stairs-slab` | yes | standard, new: old town | double slab; looks like the block |
| `Botania:dreamwood0Stairs` | Dreamwood Stairs | `stairs-slab` | partly | standard | meta = facing, as vanilla stairs |
| `Botania:dreamwood0Slab` | Dreamwood Slab | `stairs-slab` | partly | standard | 0 bottom, 8 top |
| `Botania:dreamwood0SlabFull` | Dreamwood Double Slab | `stairs-slab` | partly | standard | double slab; looks like the block |
| `Botania:dreamwood1Stairs` | Dreamwood Plank Stairs | `stairs-slab` | partly | standard | meta = facing, as vanilla stairs |
| `Botania:dreamwood1Slab` | Dreamwood Plank Slab | `stairs-slab` | partly | standard | 0 bottom, 8 top |
| `Botania:dreamwood1SlabFull` | Dreamwood Plank Double Slab | `stairs-slab` | partly | standard | double slab; looks like the block |
| `Botania:prismarine0Stairs` | Prismarine Stairs | `stairs-slab` | partly | new: waterfront | meta = facing, as vanilla stairs |
| `Botania:prismarine0Slab` | Prismarine Slab | `stairs-slab` | partly | new: waterfront | 0 bottom, 8 top |
| `Botania:prismarine0SlabFull` | Prismarine Double Slab | `stairs-slab` | partly | new: waterfront | double slab; looks like the block |
| `Botania:prismarine1Stairs` | Prismarine Brick Stairs | `stairs-slab` | partly | new: waterfront | meta = facing, as vanilla stairs |
| `Botania:prismarine1Slab` | Prismarine Brick Slab | `stairs-slab` | partly | new: waterfront | 0 bottom, 8 top |
| `Botania:prismarine1SlabFull` | Prismarine Brick Double Slab | `stairs-slab` | partly | new: waterfront | double slab; looks like the block |
| `Botania:prismarine2Stairs` | Dark Prismarine Stairs | `stairs-slab` | partly | new: waterfront | meta = facing, as vanilla stairs |
| `Botania:prismarine2Slab` | Dark Prismarine Slab | `stairs-slab` | partly | new: waterfront | 0 bottom, 8 top |
| `Botania:prismarine2SlabFull` | Dark Prismarine Double Slab | `stairs-slab` | partly | new: waterfront | double slab; looks like the block |
| `Botania:reedBlock0Stairs` | Reed Stairs | `stairs-slab` | partly | park | meta = facing, as vanilla stairs |
| `Botania:reedBlock0Slab` | Reed Slab | `stairs-slab` | partly | park | 0 bottom, 8 top |
| `Botania:reedBlock0SlabFull` | Reed Double Slab | `stairs-slab` | partly | park | double slab; looks like the block |
| `Botania:thatch0Stairs` | Thatch Stairs | `stairs-slab`, `roof` | partly | new: old town | meta = facing, as vanilla stairs |
| `Botania:thatch0Slab` | Thatch Slab | `stairs-slab`, `roof` | partly | new: old town | 0 bottom, 8 top |
| `Botania:thatch0SlabFull` | Thatch Double Slab | `stairs-slab` | partly | new: old town | double slab; looks like the block |
| `Botania:customBrick0Stairs` | Hellish Brick Stairs | `stairs-slab` | partly | new: old town | meta = facing, as vanilla stairs |
| `Botania:customBrick0Slab` | Hellish Brick Slab | `stairs-slab` | partly | new: old town | 0 bottom, 8 top |
| `Botania:customBrick0SlabFull` | Hellish Brick Double Slab | `stairs-slab` | partly | new: old town | double slab; looks like the block |
| `Botania:customBrick1Stairs` | Soul Brick Stairs | `stairs-slab` | partly | new: old town | meta = facing, as vanilla stairs |
| `Botania:customBrick1Slab` | Soul Brick Slab | `stairs-slab` | partly | new: old town | 0 bottom, 8 top |
| `Botania:customBrick1SlabFull` | Soul Brick Double Slab | `stairs-slab` | partly | new: old town | double slab; looks like the block |
| `Botania:customBrick2Stairs` | Frosty Brick Stairs | `stairs-slab` | partly | standard | meta = facing, as vanilla stairs |
| `Botania:customBrick2Slab` | Frosty Brick Slab | `stairs-slab` | partly | standard | 0 bottom, 8 top |
| `Botania:customBrick2SlabFull` | Frosty Brick Double Slab | `stairs-slab` | partly | standard | double slab; looks like the block |
| `Botania:customBrick3Stairs` | Roof Tile Stairs | `stairs-slab`, `roof` | yes | standard, desert, new: old town, new: Mediterranean quarter | meta = facing, as vanilla stairs |
| `Botania:customBrick3Slab` | Roof Tile Slab | `stairs-slab`, `roof` | yes | standard, desert, new: old town, new: Mediterranean quarter | 0 bottom, 8 top |
| `Botania:customBrick3SlabFull` | Roof Tile Double Slab | `stairs-slab` | yes | standard, desert, new: old town, new: Mediterranean quarter | double slab; looks like the block |
| `Botania:endStoneBrick0Stairs` | End Stone Brick Stairs | `stairs-slab` | partly | desert | meta = facing, as vanilla stairs |
| `Botania:endStoneBrick0Slab` | End Stone Brick Slab | `stairs-slab` | partly | desert | 0 bottom, 8 top |
| `Botania:endStoneBrick0SlabFull` | End Stone Brick Double Slab | `stairs-slab` | partly | desert | double slab; looks like the block |
| `Botania:endStoneBrick2Stairs` | Ender Brick Stairs | `stairs-slab` | partly | standard | meta = facing, as vanilla stairs |
| `Botania:endStoneBrick2Slab` | Ender Brick Slab | `stairs-slab` | partly | standard | 0 bottom, 8 top |
| `Botania:endStoneBrick2SlabFull` | Ender Brick Double Slab | `stairs-slab` | partly | standard | double slab; looks like the block |
| `Botania:shimmerrock0Stairs` | Shimmerrock Stairs | `stairs-slab`, `off-theme` | no | - | meta = facing, as vanilla stairs |
| `Botania:shimmerrock0Slab` | Shimmerrock Slab | `stairs-slab`, `off-theme` | no | - | 0 bottom, 8 top |
| `Botania:shimmerrock0SlabFull` | Shimmerrock Double Slab | `stairs-slab`, `off-theme` | no | - | double slab; looks like the block |
| `Botania:shimmerwoodPlanks0Stairs` | Shimmerwood Plank Stairs | `stairs-slab`, `off-theme` | no | - | meta = facing, as vanilla stairs |
| `Botania:shimmerwoodPlanks0Slab` | Shimmerwood Plank Slab | `stairs-slab`, `off-theme` | no | - | 0 bottom, 8 top |
| `Botania:shimmerwoodPlanks0SlabFull` | Shimmerwood Plank Double Slab | `stairs-slab`, `off-theme` | no | - | double slab; looks like the block |
| `Botania:quartzStairsDark` | Smokey Quartz Stairs | `stairs-slab` | yes | standard, border | meta = facing, as vanilla stairs |
| `Botania:quartzSlabDarkHalf` | Smokey Quartz Slab | `stairs-slab` | yes | standard, border | 0 bottom, 8 top |
| `Botania:quartzSlabDarkFull` | Smokey Quartz Double Slab | `stairs-slab` | yes | standard, border | double slab; looks like the block |
| `Botania:quartzStairsMana` | Mana Quartz Stairs | `stairs-slab` | yes | standard | meta = facing, as vanilla stairs |
| `Botania:quartzSlabManaHalf` | Mana Quartz Slab | `stairs-slab` | yes | standard | 0 bottom, 8 top |
| `Botania:quartzSlabManaFull` | Mana Quartz Double Slab | `stairs-slab` | yes | standard | double slab; looks like the block |
| `Botania:quartzStairsBlaze` | Blaze Quartz Stairs | `stairs-slab` | partly | standard, desert | meta = facing, as vanilla stairs |
| `Botania:quartzSlabBlazeHalf` | Blaze Quartz Slab | `stairs-slab` | partly | standard, desert | 0 bottom, 8 top |
| `Botania:quartzSlabBlazeFull` | Blaze Quartz Double Slab | `stairs-slab` | partly | standard, desert | double slab; looks like the block |
| `Botania:quartzStairsLavender` | Lavender Quartz Stairs | `stairs-slab` | partly | standard | meta = facing, as vanilla stairs |
| `Botania:quartzSlabLavenderHalf` | Lavender Quartz Slab | `stairs-slab` | partly | standard | 0 bottom, 8 top |
| `Botania:quartzSlabLavenderFull` | Lavender Quartz Double Slab | `stairs-slab` | partly | standard | double slab; looks like the block |
| `Botania:quartzStairsRed` | Redquartz Stairs | `stairs-slab` | partly | standard | meta = facing, as vanilla stairs |
| `Botania:quartzSlabRedHalf` | Redquartz Slab | `stairs-slab` | partly | standard | 0 bottom, 8 top |
| `Botania:quartzSlabRedFull` | Redquartz Double Slab | `stairs-slab` | partly | standard | double slab; looks like the block |
| `Botania:quartzStairsElf` | Elven Quartz Stairs | `stairs-slab` | partly | standard | meta = facing, as vanilla stairs |
| `Botania:quartzSlabElfHalf` | Elven Quartz Slab | `stairs-slab` | partly | standard | 0 bottom, 8 top |
| `Botania:quartzSlabElfFull` | Elven Quartz Double Slab | `stairs-slab` | partly | standard | double slab; looks like the block |
| `Botania:quartzStairsSunny` | Sunny Quartz Stairs | `stairs-slab` | yes | standard, desert | meta = facing, as vanilla stairs |
| `Botania:quartzSlabSunnyHalf` | Sunny Quartz Slab | `stairs-slab` | yes | standard, desert | 0 bottom, 8 top |
| `Botania:quartzSlabSunnyFull` | Sunny Quartz Double Slab | `stairs-slab` | yes | standard, desert | double slab; looks like the block |
| `Botania:stone0Stairs` | Andesite Stairs | `stairs-slab` | yes | standard, border | meta = facing, as vanilla stairs |
| `Botania:stone0Slab` | Andesite Slab | `stairs-slab` | yes | standard, border | 0 bottom, 8 top |
| `Botania:stone0SlabFull` | Andesite Double Slab | `stairs-slab` | yes | standard, border | double slab; looks like the block |
| `Botania:stone1Stairs` | Basalt Stairs | `stairs-slab` | partly | standard, border | meta = facing, as vanilla stairs |
| `Botania:stone1Slab` | Basalt Slab | `stairs-slab` | partly | standard, border | 0 bottom, 8 top |
| `Botania:stone1SlabFull` | Basalt Double Slab | `stairs-slab` | partly | standard, border | double slab; looks like the block |
| `Botania:stone2Stairs` | Diorite Stairs | `stairs-slab` | yes | standard, border | meta = facing, as vanilla stairs |
| `Botania:stone2Slab` | Diorite Slab | `stairs-slab` | yes | standard, border | 0 bottom, 8 top |
| `Botania:stone2SlabFull` | Diorite Double Slab | `stairs-slab` | yes | standard, border | double slab; looks like the block |
| `Botania:stone3Stairs` | Granite Stairs | `stairs-slab` | yes | standard, border | meta = facing, as vanilla stairs |
| `Botania:stone3Slab` | Granite Slab | `stairs-slab` | yes | standard, border | 0 bottom, 8 top |
| `Botania:stone3SlabFull` | Granite Double Slab | `stairs-slab` | yes | standard, border | double slab; looks like the block |
| `Botania:stone8Stairs` | Andesite Brick Stairs | `stairs-slab` | yes | standard, border | meta = facing, as vanilla stairs |
| `Botania:stone8Slab` | Andesite Brick Slab | `stairs-slab` | yes | standard, border | 0 bottom, 8 top |
| `Botania:stone8SlabFull` | Andesite Brick Double Slab | `stairs-slab` | yes | standard, border | double slab; looks like the block |
| `Botania:stone9Stairs` | Basalt Brick Stairs | `stairs-slab` | partly | standard, border | meta = facing, as vanilla stairs |
| `Botania:stone9Slab` | Basalt Brick Slab | `stairs-slab` | partly | standard, border | 0 bottom, 8 top |
| `Botania:stone9SlabFull` | Basalt Brick Double Slab | `stairs-slab` | partly | standard, border | double slab; looks like the block |
| `Botania:stone10Stairs` | Diorite Brick Stairs | `stairs-slab` | yes | standard, border | meta = facing, as vanilla stairs |
| `Botania:stone10Slab` | Diorite Brick Slab | `stairs-slab` | yes | standard, border | 0 bottom, 8 top |
| `Botania:stone10SlabFull` | Diorite Brick Double Slab | `stairs-slab` | yes | standard, border | double slab; looks like the block |
| `Botania:stone11Stairs` | Granite Brick Stairs | `stairs-slab` | yes | standard, border | meta = facing, as vanilla stairs |
| `Botania:stone11Slab` | Granite Brick Slab | `stairs-slab` | yes | standard, border | 0 bottom, 8 top |
| `Botania:stone11SlabFull` | Granite Brick Double Slab | `stairs-slab` | yes | standard, border | double slab; looks like the block |
| `Botania:pavement0Stairs` | White Portuguese Pavement Stairs | `stairs-slab` | yes | standard, border, new: Mediterranean quarter | meta = facing, as vanilla stairs |
| `Botania:pavement0Slab` | White Portuguese Pavement Slab | `stairs-slab` | yes | standard, border, new: Mediterranean quarter | 0 bottom, 8 top |
| `Botania:pavement0SlabFull` | White Portuguese Pavement Double Slab | `stairs-slab` | yes | standard, border, new: Mediterranean quarter | double slab; looks like the block |
| `Botania:pavement1Stairs` | Black Portuguese Pavement Stairs | `stairs-slab` | yes | standard, border | meta = facing, as vanilla stairs |
| `Botania:pavement1Slab` | Black Portuguese Pavement Slab | `stairs-slab` | yes | standard, border | 0 bottom, 8 top |
| `Botania:pavement1SlabFull` | Black Portuguese Pavement Double Slab | `stairs-slab` | yes | standard, border | double slab; looks like the block |
| `Botania:pavement2Stairs` | Blue Portuguese Pavement Stairs | `stairs-slab` | partly | standard, new: Mediterranean quarter | meta = facing, as vanilla stairs |
| `Botania:pavement2Slab` | Blue Portuguese Pavement Slab | `stairs-slab` | partly | standard, new: Mediterranean quarter | 0 bottom, 8 top |
| `Botania:pavement2SlabFull` | Blue Portuguese Pavement Double Slab | `stairs-slab` | partly | standard, new: Mediterranean quarter | double slab; looks like the block |
| `Botania:pavement3Stairs` | Red Portuguese Pavement Stairs | `stairs-slab` | partly | standard, new: Mediterranean quarter, desert | meta = facing, as vanilla stairs |
| `Botania:pavement3Slab` | Red Portuguese Pavement Slab | `stairs-slab` | partly | standard, new: Mediterranean quarter, desert | 0 bottom, 8 top |
| `Botania:pavement3SlabFull` | Red Portuguese Pavement Double Slab | `stairs-slab` | partly | standard, new: Mediterranean quarter, desert | double slab; looks like the block |
| `Botania:pavement4Stairs` | Yellow Portuguese Pavement Stairs | `stairs-slab` | partly | standard, desert | meta = facing, as vanilla stairs |
| `Botania:pavement4Slab` | Yellow Portuguese Pavement Slab | `stairs-slab` | partly | standard, desert | 0 bottom, 8 top |
| `Botania:pavement4SlabFull` | Yellow Portuguese Pavement Double Slab | `stairs-slab` | partly | standard, desert | double slab; looks like the block |
| `Botania:pavement5Stairs` | Green Portuguese Pavement Stairs | `stairs-slab` | partly | standard, park | meta = facing, as vanilla stairs |
| `Botania:pavement5Slab` | Green Portuguese Pavement Slab | `stairs-slab` | partly | standard, park | 0 bottom, 8 top |
| `Botania:pavement5SlabFull` | Green Portuguese Pavement Double Slab | `stairs-slab` | partly | standard, park | double slab; looks like the block |
| `Botania:biomeStoneA0Stairs` | Metamorphic Forest Stone Stairs | `stairs-slab`, `off-theme` | no | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA0Slab` | Metamorphic Forest Stone Slab | `stairs-slab`, `off-theme` | no | - | 0 bottom, 8 top |
| `Botania:biomeStoneA0SlabFull` | Metamorphic Forest Stone Double Slab | `stairs-slab`, `off-theme` | no | - | double slab; looks like the block |
| `Botania:biomeStoneA1Stairs` | Metamorphic Plains Stone Stairs | `stairs-slab` | partly | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA1Slab` | Metamorphic Plains Stone Slab | `stairs-slab` | partly | - | 0 bottom, 8 top |
| `Botania:biomeStoneA1SlabFull` | Metamorphic Plains Stone Double Slab | `stairs-slab` | partly | - | double slab; looks like the block |
| `Botania:biomeStoneA2Stairs` | Metamorphic Mountain Stone Stairs | `stairs-slab` | partly | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA2Slab` | Metamorphic Mountain Stone Slab | `stairs-slab` | partly | - | 0 bottom, 8 top |
| `Botania:biomeStoneA2SlabFull` | Metamorphic Mountain Stone Double Slab | `stairs-slab` | partly | - | double slab; looks like the block |
| `Botania:biomeStoneA3Stairs` | Metamorphic Fungal Stone Stairs | `stairs-slab`, `off-theme` | no | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA3Slab` | Metamorphic Fungal Stone Slab | `stairs-slab`, `off-theme` | no | - | 0 bottom, 8 top |
| `Botania:biomeStoneA3SlabFull` | Metamorphic Fungal Stone Double Slab | `stairs-slab`, `off-theme` | no | - | double slab; looks like the block |
| `Botania:biomeStoneA4Stairs` | Metamorphic Swamp Stone Stairs | `stairs-slab`, `off-theme` | no | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA4Slab` | Metamorphic Swamp Stone Slab | `stairs-slab`, `off-theme` | no | - | 0 bottom, 8 top |
| `Botania:biomeStoneA4SlabFull` | Metamorphic Swamp Stone Double Slab | `stairs-slab`, `off-theme` | no | - | double slab; looks like the block |
| `Botania:biomeStoneA5Stairs` | Metamorphic Desert Stone Stairs | `stairs-slab` | yes | desert | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA5Slab` | Metamorphic Desert Stone Slab | `stairs-slab` | yes | desert | 0 bottom, 8 top |
| `Botania:biomeStoneA5SlabFull` | Metamorphic Desert Stone Double Slab | `stairs-slab` | yes | desert | double slab; looks like the block |
| `Botania:biomeStoneA6Stairs` | Metamorphic Taiga Stone Stairs | `stairs-slab` | partly | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA6Slab` | Metamorphic Taiga Stone Slab | `stairs-slab` | partly | - | 0 bottom, 8 top |
| `Botania:biomeStoneA6SlabFull` | Metamorphic Taiga Stone Double Slab | `stairs-slab` | partly | - | double slab; looks like the block |
| `Botania:biomeStoneA7Stairs` | Metamorphic Mesa Stone Stairs | `stairs-slab` | yes | desert | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA7Slab` | Metamorphic Mesa Stone Slab | `stairs-slab` | yes | desert | 0 bottom, 8 top |
| `Botania:biomeStoneA7SlabFull` | Metamorphic Mesa Stone Double Slab | `stairs-slab` | yes | desert | double slab; looks like the block |
| `Botania:biomeStoneA8Stairs` | Metamorphic Forest Cobblestone Stairs | `stairs-slab`, `off-theme` | no | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA8Slab` | Metamorphic Forest Cobblestone Slab | `stairs-slab`, `off-theme` | no | - | 0 bottom, 8 top |
| `Botania:biomeStoneA8SlabFull` | Metamorphic Forest Cobblestone Double Slab | `stairs-slab`, `off-theme` | no | - | double slab; looks like the block |
| `Botania:biomeStoneA9Stairs` | Metamorphic Plains Cobblestone Stairs | `stairs-slab` | partly | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA9Slab` | Metamorphic Plains Cobblestone Slab | `stairs-slab` | partly | - | 0 bottom, 8 top |
| `Botania:biomeStoneA9SlabFull` | Metamorphic Plains Cobblestone Double Slab | `stairs-slab` | partly | - | double slab; looks like the block |
| `Botania:biomeStoneA10Stairs` | Metamorphic Mountain Cobblestone Stairs | `stairs-slab` | partly | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA10Slab` | Metamorphic Mountain Cobblestone Slab | `stairs-slab` | partly | - | 0 bottom, 8 top |
| `Botania:biomeStoneA10SlabFull` | Metamorphic Mountain Cobblestone Double Slab | `stairs-slab` | partly | - | double slab; looks like the block |
| `Botania:biomeStoneA11Stairs` | Metamorphic Fungal Cobblestone Stairs | `stairs-slab`, `off-theme` | no | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA11Slab` | Metamorphic Fungal Cobblestone Slab | `stairs-slab`, `off-theme` | no | - | 0 bottom, 8 top |
| `Botania:biomeStoneA11SlabFull` | Metamorphic Fungal Cobblestone Double Slab | `stairs-slab`, `off-theme` | no | - | double slab; looks like the block |
| `Botania:biomeStoneA12Stairs` | Metamorphic Swamp Cobblestone Stairs | `stairs-slab`, `off-theme` | no | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA12Slab` | Metamorphic Swamp Cobblestone Slab | `stairs-slab`, `off-theme` | no | - | 0 bottom, 8 top |
| `Botania:biomeStoneA12SlabFull` | Metamorphic Swamp Cobblestone Double Slab | `stairs-slab`, `off-theme` | no | - | double slab; looks like the block |
| `Botania:biomeStoneA13Stairs` | Metamorphic Desert Cobblestone Stairs | `stairs-slab` | yes | desert | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA13Slab` | Metamorphic Desert Cobblestone Slab | `stairs-slab` | yes | desert | 0 bottom, 8 top |
| `Botania:biomeStoneA13SlabFull` | Metamorphic Desert Cobblestone Double Slab | `stairs-slab` | yes | desert | double slab; looks like the block |
| `Botania:biomeStoneA14Stairs` | Metamorphic Taiga Cobblestone Stairs | `stairs-slab` | partly | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA14Slab` | Metamorphic Taiga Cobblestone Slab | `stairs-slab` | partly | - | 0 bottom, 8 top |
| `Botania:biomeStoneA14SlabFull` | Metamorphic Taiga Cobblestone Double Slab | `stairs-slab` | partly | - | double slab; looks like the block |
| `Botania:biomeStoneA15Stairs` | Metamorphic Mesa Cobblestone Stairs | `stairs-slab` | yes | desert | meta = facing, as vanilla stairs |
| `Botania:biomeStoneA15Slab` | Metamorphic Mesa Cobblestone Slab | `stairs-slab` | yes | desert | 0 bottom, 8 top |
| `Botania:biomeStoneA15SlabFull` | Metamorphic Mesa Cobblestone Double Slab | `stairs-slab` | yes | desert | double slab; looks like the block |
| `Botania:biomeStoneB0Stairs` | Metamorphic Forest Stone Brick Stairs | `stairs-slab`, `off-theme` | no | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneB0Slab` | Metamorphic Forest Stone Brick Slab | `stairs-slab`, `off-theme` | no | - | 0 bottom, 8 top |
| `Botania:biomeStoneB0SlabFull` | Metamorphic Forest Stone Brick Double Slab | `stairs-slab`, `off-theme` | no | - | double slab; looks like the block |
| `Botania:biomeStoneB1Stairs` | Metamorphic Plains Stone Brick Stairs | `stairs-slab` | partly | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneB1Slab` | Metamorphic Plains Stone Brick Slab | `stairs-slab` | partly | - | 0 bottom, 8 top |
| `Botania:biomeStoneB1SlabFull` | Metamorphic Plains Stone Brick Double Slab | `stairs-slab` | partly | - | double slab; looks like the block |
| `Botania:biomeStoneB2Stairs` | Metamorphic Mountain Stone Brick Stairs | `stairs-slab` | partly | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneB2Slab` | Metamorphic Mountain Stone Brick Slab | `stairs-slab` | partly | - | 0 bottom, 8 top |
| `Botania:biomeStoneB2SlabFull` | Metamorphic Mountain Stone Brick Double Slab | `stairs-slab` | partly | - | double slab; looks like the block |
| `Botania:biomeStoneB3Stairs` | Metamorphic Fungal Stone Brick Stairs | `stairs-slab`, `off-theme` | no | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneB3Slab` | Metamorphic Fungal Stone Brick Slab | `stairs-slab`, `off-theme` | no | - | 0 bottom, 8 top |
| `Botania:biomeStoneB3SlabFull` | Metamorphic Fungal Stone Brick Double Slab | `stairs-slab`, `off-theme` | no | - | double slab; looks like the block |
| `Botania:biomeStoneB4Stairs` | Metamorphic Swamp Stone Brick Stairs | `stairs-slab`, `off-theme` | no | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneB4Slab` | Metamorphic Swamp Stone Brick Slab | `stairs-slab`, `off-theme` | no | - | 0 bottom, 8 top |
| `Botania:biomeStoneB4SlabFull` | Metamorphic Swamp Stone Brick Double Slab | `stairs-slab`, `off-theme` | no | - | double slab; looks like the block |
| `Botania:biomeStoneB5Stairs` | Metamorphic Desert Stone Brick Stairs | `stairs-slab` | yes | desert | meta = facing, as vanilla stairs |
| `Botania:biomeStoneB5Slab` | Metamorphic Desert Stone Brick Slab | `stairs-slab` | yes | desert | 0 bottom, 8 top |
| `Botania:biomeStoneB5SlabFull` | Metamorphic Desert Stone Brick Double Slab | `stairs-slab` | yes | desert | double slab; looks like the block |
| `Botania:biomeStoneB6Stairs` | Metamorphic Taiga Stone Brick Stairs | `stairs-slab` | partly | - | meta = facing, as vanilla stairs |
| `Botania:biomeStoneB6Slab` | Metamorphic Taiga Stone Brick Slab | `stairs-slab` | partly | - | 0 bottom, 8 top |
| `Botania:biomeStoneB6SlabFull` | Metamorphic Taiga Stone Brick Double Slab | `stairs-slab` | partly | - | double slab; looks like the block |
| `Botania:biomeStoneB7Stairs` | Metamorphic Mesa Stone Brick Stairs | `stairs-slab` | yes | desert | meta = facing, as vanilla stairs |
| `Botania:biomeStoneB7Slab` | Metamorphic Mesa Stone Brick Slab | `stairs-slab` | yes | desert | 0 bottom, 8 top |
| `Botania:biomeStoneB7SlabFull` | Metamorphic Mesa Stone Brick Double Slab | `stairs-slab` | yes | desert | double slab; looks like the block |
| `Botania:dirtPath0Slab` | Trodden Dirt Slab | `stairs-slab`, `sidewalk` | yes | park | 0 bottom, 8 top; no stairs |
| `Botania:dirtPath0SlabFull` | Trodden Dirt Double Slab | `stairs-slab` | yes | park |  |
| `Botania:livingrock0Wall:0` | Livingrock Wall | `fence-rail` | yes | standard, border | connects like a cobblestone wall |
| `Botania:livingwood0Wall:0` | Livingwood Wall | `fence-rail` | partly | new: old town | connects like a cobblestone wall |
| `Botania:dreamwood0Wall:0` | Dreamwood Wall | `fence-rail` | partly | standard | connects like a cobblestone wall |
| `Botania:prismarine0Wall:0` | Prismarine Wall | `fence-rail` | partly | new: waterfront | connects like a cobblestone wall |
| `Botania:reedBlock0Wall:0` | Reed Block Wall | `fence-rail` | partly | park | connects like a cobblestone wall |
| `Botania:stone0Wall:0` | Andesite Wall | `fence-rail` | yes | standard, border | one wall block, metas 0-3; D19's `diorite_wall` is meta 2 |
| `Botania:stone0Wall:1` | Basalt Wall | `fence-rail` | partly | standard, border |  |
| `Botania:stone0Wall:2` | Diorite Wall | `fence-rail` | yes | standard, border |  |
| `Botania:stone0Wall:3` | Granite Wall | `fence-rail` | yes | standard, border |  |
| `Botania:biomeStoneA0Wall:0` | Metamorphic Forest Cobblestone Wall | `fence-rail`, `off-theme` | no | - | one wall block, metas 0-7, cobblestone texture |
| `Botania:biomeStoneA0Wall:1` | Metamorphic Plains Cobblestone Wall | `fence-rail` | partly | - |  |
| `Botania:biomeStoneA0Wall:2` | Metamorphic Mountain Cobblestone Wall | `fence-rail` | partly | - |  |
| `Botania:biomeStoneA0Wall:3` | Metamorphic Fungal Cobblestone Wall | `fence-rail`, `off-theme` | no | - |  |
| `Botania:biomeStoneA0Wall:4` | Metamorphic Swamp Cobblestone Wall | `fence-rail`, `off-theme` | no | - |  |
| `Botania:biomeStoneA0Wall:5` | Metamorphic Desert Cobblestone Wall | `fence-rail` | yes | desert |  |
| `Botania:biomeStoneA0Wall:6` | Metamorphic Taiga Cobblestone Wall | `fence-rail` | partly | - |  |
| `Botania:biomeStoneA0Wall:7` | Metamorphic Mesa Cobblestone Wall | `fence-rail` | yes | desert |  |

## Best variety pools

Pools list what fits each upstream role, beside the D19 baseline block. Weights are set later.

| Upstream role (palette char) | Baseline (D19) | Botania variety |
|---|---|---|
| Stone-brick wall `#` (`bricks_standard`, border) | `minecraft:stonebrick` variants | `stone` 8 and 10 (andesite and diorite bricks); `livingrock` 1; `quartzTypeDark` 0 for dark modern blocks |
| Accent `X`, band `$` | stone brick, smooth stone double slab | `stone` 4, 6, 12, 14 (polished, chiseled); `livingrock` 4; `quartzTypeDark` 1 and 2-4 |
| Rubble `}` | `stonebrick_rubble` variant | `livingrock` 2, 3; `stone` 0, 2; `biomeStoneA` 13, 15 (desert) |
| Quartz `Q`, quartz stairs `(` `)` | `minecraft:quartz_block`, `quartz_stairs` | `quartzTypeSunny`, `quartzTypeMana`, `quartzTypeDark` with their stairs and slabs; pastel types for accents only |
| Desert walls and bands (`bricks_desert*`, `$`) | cut sandstone, red sandstone double slab, terracotta | `stone` 3, 7, 11 (granite); `biomeStoneA` 5, `biomeStoneB` 5, 7 (desert and mesa); `endStoneBrick` 0, 1; `quartzTypeSunny`, `quartzTypeBlaze` |
| Glass `+` | `minecraft:glass`, stained glass | `manaGlass` (clear, frameless; lights the room) |
| Pane `` ` `` | `minecraft:glass_pane`, stained panes | `manaGlassPane`; `elfGlassPane` |
| Light `h` | `minecraft:glowstone` | `seaLamp`; `blazeBlock`; `manaFlame` for hidden lighting; managlass windows light interiors anyway |
| Street `S` and sidewalks | smooth stone double slab | `pavement` 0 and 1 (+ slabs); `dirtPath` for parks and back lots |
| Roof (new) | (flat roofs in upstream) | `customBrick` 3 Roof Tile with its stairs and slabs; `quartzTypeDark` stairs for flat modern parapets; `thatch` for an old-town style |
| Wood floors and trims | `minecraft:planks` | `livingwood` 1 (dark), `dreamwood` 1 (bleached); `livingwood` 3 framed |
| Flowers `p`, `K` | vanilla flowers and saplings | `flower` 0-15, `doubleFlower1`/`2`; `shinyFlower` for lit beds |
| Overgrowth on ruins | (leaves in upstream's `generalblocks`) | `solidVine` (never spreads); `altGrass` 0 dry grass (but it spreads); `buriedPetals`; `mushroom` in basements |
| Walls `w` (cobblestone wall) | `minecraft:cobblestone_wall` | `stone0Wall` 0, 2; `livingrock0Wall` |
| Treasure `!` | gold, iron, diamond, emerald blocks | `storage` 0-2 (Manasteel, Terrasteel, Elementium), sparingly; 3-4 are high-value |

### New styles Botania suggests

- **Mediterranean quarter**: Azulejo facades (`customBrick` 4-15), Roof Tile pitched roofs, white and colored Portuguese Pavement, Frosty Brick and Sunny Quartz walls.
- **Waterfront**: Prismarine family and Sea Lanterns for docks and harbor buildings.
- **Overgrown ruins**: Solid Vines, mossy and cracked livingrock, dry and scorched grass, buried petals, shimmering mushrooms.
