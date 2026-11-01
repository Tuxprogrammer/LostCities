# Biomes O' Plenty

Family D. Mod ID `BiomesOPlenty`, jar `BiomesOPlenty-1.7.10-2.1.0.2308-universal.jar`. Source read: `mods/BiomesOPlenty/biomesoplenty/common/core/BOPBlocks.java` (every registration), `mods/BiomesOPlenty/biomesoplenty/common/blocks/*.java` (metadata arrays, light, collision effects, ticking), `mods/BiomesOPlenty/assets/biomesoplenty/lang/en_US.lang` (names), `config/biomesoplenty/*.cfg` (biome IDs and terrain only: no block toggles). Sheets: `tools/block-contact-sheet/sheets/biomesoplenty/` (previews in `docs/previews/blocks/biomesoplenty/`, local only).

## Summary

- **83 registry names, about 280 block+meta entries** (orientation and growth bits collapsed), of which about **140 are useful** to cities: 15 plank colors with their stairs and slabs, 16 logs, 6 sedimentary stones (3 raw, 3 polished), mud bricks with stairs and slab, earth blocks, and a large set of flowers, grasses, vines, leaf piles and leaves for overgrown ruins and parks.
- Registry names are the unlocalized name minus `tile.`, prefixed with `BiomesOPlenty:` (`BOPBlocks.registerBlock`, `block.getUnlocalizedName().replace("tile.", "")`). Mixed case matters: `BiomesOPlenty:mudBricks`, `BiomesOPlenty:logs1`.
- **The pack enables every block.** BoP 2.1.0 has no block toggles; `config/biomesoplenty/` only sets biome IDs, weights and terrain.
- **This BoP version has no fences, fence gates, doors, trapdoors, buttons or pressure plates** (checked: no fence or door class in the source). Natura supplies those for its own woods.
- **Look:** the planks are flat, strongly saturated colors (cherry red, magic blue, ethereal teal, willow green, jacaranda pink) beside a few natural tones (fir, pine, palm, redwood, mahogany, hellbark). The natural tones read as ordinary 20th-century floors and cladding; the saturated ones read as fantasy. The stones are the real surprise: **polished shale is a dark charcoal slab-like block with a border, and polished siltstone a tan smooth block**, both convincing as urban dressed stone or concrete. Mud bricks are small gray-brown bricks, good for ruins and poorer districts.
- **No tile entities** anywhere in the placeable set (the gravestone and bone segments are model blocks without a TE).
- **Light:** Celestial Crystal 15; Glowflower (`flowers:3`) 9; Burning Blossom (`flowers2:2`) 9; Glowing Coral (`coral1:15`) 10; Glowshroom (`mushrooms:3`) 6; Enderlotus (`flowers:11`) 5.
- **Gravity:** none. Hardened Sand is `Material.sand` but a plain block, so it does not fall.
- **Dangerous or annoying blocks** (never in walkable city spaces): Quicksand (`mud:1`, traps like a cobweb), Mud (`mud:0`, slows to 10 %), Ash Block (slows to 40 %), Flesh (slows to 90 %), Smoldering Grass (`bopGrass:1`, sets fire), Thorns and Tiny Cactus (`plants:5`, `plants:12`, 1 damage per touch without leather boots and leggings), Deathbloom (`flowers:2`, Wither for 10 s), Burning Blossom (`flowers2:2`, sets players on fire), Poison Ivy (`foliage:7`, Poison for 5 s), and the fluids `poison`, `hell_blood`, `honey`.
- **Ticking:** leaves decay (bit rules below); vines (`willow`, `ivy`, `treeMoss`, `flowerVine`, `wisteria`, `moss`) extend `BlockVine` and spread on random ticks like vanilla vines; `newBopGrass`, `originGrass`, `longGrass`, `bopGrass` and `overgrownNetherrack` tick (grass spread or decay).
- **Ore and boulder hosts:** none. No BoP block overrides `isReplaceableOreGen`, so GT veins and boulders never replace BoP blocks (`ore-veins.md` host list).
- **Optional-dependency handling:** every BoP entry in a palette needs a vanilla fallback (D19 chain). Suggested fallbacks are in the tables; a server without BoP loses all of these and falls back cleanly because nothing is a TE.

## Blocks

### Planks, stairs and slabs (sheet `woods`)

Slabs: `woodenSingleSlab1` metas 0-7 are sacred oak, cherry, dark, fir, ethereal, magic, mangrove, palm; `woodenSingleSlab2` metas 0-5 are redwood, willow, pine, hellbark, jacaranda, mahogany; +8 is the top half; `woodenDoubleSlab1`/`2` are the double slabs with the same metas. Stairs are one block per wood (vanilla stair metas). Bamboo Thatching has no stairs or slab.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `BiomesOPlenty:planks:0` (+ `sacredoakStairs`, `woodenSingleSlab1:0`) | Sacred Oak Wood Planks | off-theme | no | fantasy | Tan planks with leafy green flecks; reads overgrown, but the flecks are baked in. Fallback `minecraft:planks:0` |
| `BiomesOPlenty:planks:1` (+ `cherryStairs`, `woodenSingleSlab1:1`) | Cherry Wood Planks | floor, wall-accent, stairs-slab | partly | interior, rustic | Saturated red; usable as a painted-wood accent or a red-stained floor. Fallback `minecraft:planks:4` |
| `BiomesOPlenty:planks:2` (+ `darkStairs`, `woodenSingleSlab1:2`) | Dark Wood Planks | floor, wall-accent, stairs-slab | yes | interior, urban-brick | Near-black purple-brown; an ebonized floor or trim. Fallback `minecraft:planks:5` |
| `BiomesOPlenty:planks:3` (+ `firStairs`, `woodenSingleSlab1:3`) | Fir Wood Planks | floor, wall-main, stairs-slab | yes | rustic, interior | Pale gray-tan; like weathered spruce. Fallback `minecraft:planks:1` |
| `BiomesOPlenty:planks:4` (+ `etherealStairs`, `woodenSingleSlab1:4`) | Ethereal Wood Planks | off-theme | no | fantasy | Teal. Fallback `minecraft:planks:0` |
| `BiomesOPlenty:planks:5` (+ `magicStairs`, `woodenSingleSlab1:5`) | Magic Wood Planks | off-theme | no | fantasy | Bright blue; could pass as painted cladding in a seaside style at most |
| `BiomesOPlenty:planks:6` (+ `mangroveStairs`, `woodenSingleSlab1:6`) | Mangrove Wood Planks | floor, wall-main, stairs-slab | yes | interior, modern | Off-white, almost bleached; a pale floor or painted siding. Fallback `minecraft:planks:2` |
| `BiomesOPlenty:planks:7` (+ `palmStairs`, `woodenSingleSlab1:7`) | Palm Wood Planks | floor, stairs-slab | yes | interior, desert | Warm orange; a parquet look, good in desert towns. Fallback `minecraft:planks:4` |
| `BiomesOPlenty:planks:8` (+ `redwoodStairs`, `woodenSingleSlab2:0`) | Redwood Wood Planks | floor, wall-main, stairs-slab | yes | rustic, interior | Rust-orange; reads as stained wood. Fallback `minecraft:planks:4` |
| `BiomesOPlenty:planks:9` (+ `willowStairs`, `woodenSingleSlab2:1`) | Willow Wood Planks | off-theme | no | fantasy | Olive green |
| `BiomesOPlenty:planks:10` | Bamboo Thatching | roof, wall-accent | partly | rustic, park-garden | Yellow-green woven pattern; a thatch or bamboo screen for park kiosks. No stairs or slab. Fallback `minecraft:hay_block` |
| `BiomesOPlenty:planks:11` (+ `pineStairs`, `woodenSingleSlab2:2`) | Pine Wood Planks | floor, wall-main, stairs-slab | yes | rustic, interior | Mid brown, plain; a neutral second oak. Fallback `minecraft:planks:0` |
| `BiomesOPlenty:planks:12` (+ `hellBarkStairs`, `woodenSingleSlab2:3`) | Hellbark Wood Planks | floor, stairs-slab | yes | interior | Warm brown, close to oak. Fallback `minecraft:planks:0` |
| `BiomesOPlenty:planks:13` (+ `jacarandaStairs`, `woodenSingleSlab2:4`) | Jacaranda Wood Planks | floor, wall-accent | partly | interior | Pale pinkish white; a whitewashed floor. Fallback `minecraft:planks:2` |
| `BiomesOPlenty:planks:14` (+ `mahoganyStairs`, `woodenSingleSlab2:5`) | Mahogany Wood Planks | floor, wall-accent, stairs-slab | yes | interior | Dusty rose-brown; an office or library floor. Fallback `minecraft:planks:3` |

### Logs (sheet `woods`)

Meta = type (`& 3`) plus axis: +4 east-west, +8 north-south, +12 all bark (`BlockBOPLog`). Flammable except hellbark (`logs4:1`).

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `BiomesOPlenty:logs1:0` | Sacred Oak Wood | off-theme | no | fantasy | Bark with leafy flecks |
| `BiomesOPlenty:logs1:1` | Cherry Wood | trim-pillar | partly | rustic | Red-brown bark |
| `BiomesOPlenty:logs1:2` | Dark Wood | trim-pillar | partly | rustic, ruin | Dark purple-gray bark |
| `BiomesOPlenty:logs1:3` | Fir Wood | trim-pillar | yes | rustic | Gray-brown bark, plain |
| `BiomesOPlenty:logs2:0` | Ethereal Wood | off-theme | no | fantasy | Green-teal |
| `BiomesOPlenty:logs2:1` | Magic Wood | off-theme | no | fantasy | Blue |
| `BiomesOPlenty:logs2:2` | Mangrove Wood | trim-pillar | partly | park-garden | Pale birch-like bark |
| `BiomesOPlenty:logs2:3` | Palm Wood | trim-pillar | partly | desert, park-garden | Ringed tan trunk; pergola posts |
| `BiomesOPlenty:logs3:0` | Redwood Wood | trim-pillar | yes | rustic | Red-brown furrowed bark |
| `BiomesOPlenty:logs3:1` | Willow Wood | trim-pillar | partly | park-garden | Greenish bark |
| `BiomesOPlenty:logs3:2` | Dead Wood | rubble-ruin, trim-pillar | yes | ruin | Gray dead bark; dead trees and charred beams in ruins |
| `BiomesOPlenty:logs3:3` | Giant Flower Stem | off-theme | no | fantasy | Bright green stem |
| `BiomesOPlenty:logs4:0` | Pine Wood | trim-pillar | yes | rustic | Plain brown bark |
| `BiomesOPlenty:logs4:1` | Hellbark Wood | trim-pillar | partly | rustic | Not flammable |
| `BiomesOPlenty:logs4:2` | Jacaranda Wood | trim-pillar | partly | park-garden | Pale bark |
| `BiomesOPlenty:logs4:3` | Mahogany Wood | trim-pillar | partly | rustic | Pinkish bark |

### Stone, earth and building blocks (sheet `stone-earth`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `BiomesOPlenty:rocks:0` | Limestone | wall-accent | partly | ruin | Pale mint-green, speckled. Odd color for limestone |
| `BiomesOPlenty:rocks:1` | Polished Limestone | wall-accent | partly | modern | Smooth mint pastel; a 1950s tile accent at most |
| `BiomesOPlenty:rocks:2` | Siltstone | wall-main, sidewalk | yes | urban-stone, desert | Tan cobbled stone; a sandstone-like rubble wall |
| `BiomesOPlenty:rocks:3` | Polished Siltstone | wall-main, floor, sidewalk | yes | urban-stone, desert | Smooth tan block with a fine border; reads as dressed sandstone or buff concrete. Fallback `minecraft:sandstone:2` |
| `BiomesOPlenty:rocks:4` | Shale | wall-main, street | yes | urban-stone, industrial | Dark gray, layered; like rough asphalt or slate |
| `BiomesOPlenty:rocks:5` | Polished Shale | wall-main, floor, street, sidewalk | yes | modern, urban-stone, industrial | Dark charcoal smooth block with a border, very much a smooth concrete or basalt slab; the best BoP find. Fallback `minecraft:double_stone_slab:8` (lighter) or `minecraft:stonebrick:0` |
| `BiomesOPlenty:mudBricks:0` | Mud Bricks | wall-main, rubble-ruin | yes | urban-brick, ruin | Small gray-brown bricks; humbler than vanilla bricks. Fallback `minecraft:brick_block` |
| `BiomesOPlenty:mudBricksStairs` | Mud Bricks Stairs | stairs-slab | yes | urban-brick, ruin | Vanilla stair metas |
| `BiomesOPlenty:stoneSingleSlab:0` (+8 top), `stoneDoubleSlab:0` | Mud Bricks Slab | stairs-slab | yes | urban-brick, ruin | The only stone slab BoP has |
| `BiomesOPlenty:hardDirt:0` | Hardened Dirt | rubble-ruin, street | partly | ruin, desert | Dry cobbled dirt; a cracked dirt lot |
| `BiomesOPlenty:driedDirt:0` | Dried Dirt | rubble-ruin | partly | ruin, desert | Olive-gray crust, breaks instantly |
| `BiomesOPlenty:hardSand:0` | Hardened Sand | street, rubble-ruin | partly | desert | Does not fall |
| `BiomesOPlenty:hardIce:0` | Hardened Ice | special | partly | cold | Opaque pale blue, does not melt; frozen ruins only |
| `BiomesOPlenty:ashStone:0` | Ash Stone | rubble-ruin | partly | ruin | Dark gray cobble; burnt-out rubble |
| `BiomesOPlenty:ash:0` | Ash Block | rubble-ruin | partly | ruin | Black; slows to 40 %; burnt floors only, sparingly |
| `BiomesOPlenty:cragRock:0` | Crag Rock | off-theme | no | alien | Teal-green rock |
| `BiomesOPlenty:mud:0` | Mud | off-theme | no | overgrown | Slows to 10 %: never on walkways |
| `BiomesOPlenty:mud:1` | Quicksand | off-theme | no | desert | Traps like a cobweb: never |
| `BiomesOPlenty:flesh:0` | Flesh | off-theme | no | fantasy | Red flesh; Nether biome block |
| `BiomesOPlenty:crystal:0` | Celestial Crystal | light, special | no | magic | Pink-purple crystal, light 15; drops an item, not itself |
| `BiomesOPlenty:biomeBlock:0` | Biome Essence Ore | off-theme | no | magic | Drops biome essence |
| `BiomesOPlenty:gemOre:1,3,5,7,9,11,13,15` | Block of Amethyst, Ruby, Peridot, Topaz, Tanzanite, Malachite, Sapphire, Amber | special | no | fantasy | Bright gem blocks. Only for the treasure mix (`!`), and only if the beta balance list accepts them |
| `BiomesOPlenty:gemOre:0,2,4,...,14` | gem ores | off-theme | no | | Ores; never place |
| `BiomesOPlenty:honeyBlock:0` | Honey Block | off-theme | no | | Orange; sticky look |
| `BiomesOPlenty:hive:0-3` | Honeycomb, Hive, Empty and Filled Honeycomb | off-theme | no | | Wasp-nest blocks |
| `BiomesOPlenty:newBopDirt:0-5` | Loamy, Coarse Loamy, Sandy, Coarse Sandy, Silty, Coarse Silty Dirt | foliage, rubble-ruin | yes | park-garden, ruin | Even = dirt, odd = coarse (no grass spreads onto coarse). Planters and bare lots. Fallback `minecraft:dirt:0`/`:1` |
| `BiomesOPlenty:newBopGrass:0-2` | Loamy, Sandy, Silty Grass Block | foliage | yes | park-garden | Biome-tinted top; ticks like grass. Fallback `minecraft:grass` |
| `BiomesOPlenty:newBopFarmland:0-2` | Loamy, Sandy, Silty Farmland | foliage | partly | park-garden | Community gardens; dries out (not rendered) |
| `BiomesOPlenty:longGrass:0` | Long Grass Block | foliage | partly | overgrown | Grass-topped block with long grass sides |
| `BiomesOPlenty:originGrass:0` | Origin Grass Block | off-theme | no | fantasy | Beta-style bright grass |
| `BiomesOPlenty:bopGrass:0` | Spectral Moss | off-theme | no | alien | End biome |
| `BiomesOPlenty:bopGrass:1` | Smoldering Grass Block | off-theme | no | | Sets entities on fire |
| `BiomesOPlenty:overgrownNetherrack:0` | Overgrown Netherrack | off-theme | no | | Nether |
| `BiomesOPlenty:bones:0-2` (sideways 3-6) | Small, Medium, Large Bone Segment | rubble-ruin | partly | ruin | Bone pillar models; a wasteland touch at most |
| `BiomesOPlenty:grave:0` | Gravestone | furniture-deco | partly | park-garden, ruin | Two-block stone model; a cemetery plot dressing. Rendered as a cube here |
| `BiomesOPlenty:stoneFormations:0`, `:1` | Stalagmite, Stalactite | rubble-ruin | partly | ruin | Cross models; drip formations in flooded basements and subway tunnels |

### Leaves and petals (sheet `leaves`)

Leaves: meta `& 3` is the type; **+4 = never decays** (placed by a player), +8 = decay check pending (`BlockBOPLeaves.updateTick` decays only when `meta & 8 != 0 && meta & 4 == 0`). Worldgen must always set bit 4 (meta + 4) so palette leaves without a log nearby do not vanish. Colorized leaves are gray in the jar and tinted by biome in game.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `BiomesOPlenty:leaves1:0` (+4) | Yellow Autumn Leaves | foliage | yes | park-garden, overgrown | Mustard yellow; autumn park trees and hedges |
| `BiomesOPlenty:leaves1:1` (+4) | Bamboo Leaves | foliage | partly | park-garden | Lime green |
| `BiomesOPlenty:leaves1:2` (+4) | Magic Leaves | off-theme | no | fantasy | Blue |
| `BiomesOPlenty:leaves1:3` (+4) | Dark Leaves | off-theme | no | fantasy | Near black |
| `BiomesOPlenty:leaves2:0` (+4) | Dying Leaves | foliage, rubble-ruin | yes | ruin, overgrown | Brown dead leaves; dead hedges in ruins |
| `BiomesOPlenty:leaves2:1` (+4) | Fir Leaves | foliage | yes | park-garden, cold | Dark green conifer |
| `BiomesOPlenty:leaves2:2` (+4) | Ethereal Leaves | off-theme | no | fantasy | Teal |
| `BiomesOPlenty:leaves2:3` (+4) | Orange Autumn Leaves | foliage | yes | park-garden | Rust orange |
| `BiomesOPlenty:leaves3:0` (+4) | Origin Leaves | off-theme | no | | Neon green |
| `BiomesOPlenty:leaves3:1` (+4) | Pink Cherry Leaves | foliage | yes | park-garden | Blossom pink; street trees |
| `BiomesOPlenty:leaves3:2` (+4) | Maple Leaves | foliage | yes | park-garden | Deep red |
| `BiomesOPlenty:leaves3:3` (+4) | White Cherry Leaves | foliage | yes | park-garden | White blossom |
| `BiomesOPlenty:leaves4:0` (+4) | Hellbark Leaves | foliage | partly | | Olive; not flammable |
| `BiomesOPlenty:leaves4:1` (+4) | Jacaranda Leaves | foliage | partly | park-garden | Lavender |
| `BiomesOPlenty:colorizedLeaves1:0-3` (+4) | Sacred Oak, Mangrove, Palm, Redwood Leaves | foliage | yes | park-garden | Biome-tinted green; palm for desert squares |
| `BiomesOPlenty:colorizedLeaves2:0-3` (+4) | Willow, Pine, Mahogany, Flowering Oak Leaves | foliage | yes | park-garden | Biome-tinted; flowering oak has white blossoms |
| `BiomesOPlenty:appleLeaves:0-3` (+4) | Apple Leaves | foliage | partly | park-garden | Metas 0-3 are ripeness; apples drop when ripe |
| `BiomesOPlenty:persimmonLeaves:0-3` (+4) | Persimmon Leaves | foliage | partly | park-garden | As above |
| `BiomesOPlenty:petals:0`, `:1` | Giant Red, Yellow Flower | off-theme | no | fantasy | Solid petal blocks |

### Plants, flowers and foliage (sheet `plants-flowers`)

All are cross or flat models (rendered as cubes on the sheet); they need a soil block beneath. Metas are the array index.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `BiomesOPlenty:plants:0` | Dead Grass | foliage, rubble-ruin | yes | ruin, overgrown | Brown dead tufts; abandoned lots |
| `BiomesOPlenty:plants:1`, `:2`, `:3` | Desert Grass, Desert Sprouts, Dune Grass | foliage | yes | desert, overgrown | Desert lots and cracks |
| `BiomesOPlenty:plants:4` | Spectral Fern | off-theme | no | alien | |
| `BiomesOPlenty:plants:5` | Thorns | off-theme | no | | 1 damage per touch |
| `BiomesOPlenty:plants:6` | Wild Rice | foliage | partly | overgrown | Wet ground |
| `BiomesOPlenty:plants:7`, `:9`, `:10` | Cattail (single, top, bottom) | foliage | yes | park-garden, overgrown | Pond edges; 9/10 are the two-block halves |
| `BiomesOPlenty:plants:8` | River Cane | foliage | partly | overgrown | Tall cane |
| `BiomesOPlenty:plants:11` | Wild Carrot | foliage | yes | overgrown | Weedy flower |
| `BiomesOPlenty:plants:12` | Tiny Cactus | foliage | partly | desert | 1 damage per touch |
| `BiomesOPlenty:plants:13` | Wither Wart | off-theme | no | | |
| `BiomesOPlenty:plants:14` | Reed | foliage | partly | park-garden | Pond plant |
| `BiomesOPlenty:plants:15` | Root | foliage, rubble-ruin | yes | overgrown, ruin | Hangs from ceilings; roots in collapsed basements |
| `BiomesOPlenty:flowers:0` | Clover | foliage | yes | park-garden | |
| `BiomesOPlenty:flowers:1` | Swampflower | foliage | partly | overgrown | |
| `BiomesOPlenty:flowers:2` | Deathbloom | off-theme | no | | Wither 10 s |
| `BiomesOPlenty:flowers:3` | Glowflower | light, foliage | partly | park-garden | Light 9; glowing planter |
| `BiomesOPlenty:flowers:4`-`:9` | Blue Hydrangea, Orange Cosmos, Pink Daffodil, Wildflower, Violet, White Anemone | foliage | yes | park-garden, overgrown | The core flower-bed set; adds color variety to upstream's `p` and `K` |
| `BiomesOPlenty:flowers:10` | Waterlily | foliage | yes | park-garden | Pond |
| `BiomesOPlenty:flowers:11` | Enderlotus | off-theme | no | | Light 5; End plant |
| `BiomesOPlenty:flowers:12` | Bromeliad | foliage | partly | park-garden | Tropical |
| `BiomesOPlenty:flowers:13`, `:14` | Eyebulb (bottom, top) | off-theme | no | fantasy | |
| `BiomesOPlenty:flowers:15` | Dandelion Puff | foliage | yes | overgrown | Seeded dandelion |
| `BiomesOPlenty:flowers2:0`, `:1`, `:3`-`:8` | Pink Hibiscus, Lily of the Valley, Lavender, Goldenrod, Bluebells, Miner's Delight, Icy Iris, Rose | foliage | yes | park-garden, overgrown | More flower-bed color; rose and lavender read as planted beds |
| `BiomesOPlenty:flowers2:2` | Burning Blossom | off-theme | no | | Light 9; sets players on fire |
| `BiomesOPlenty:foliage:0` | Duckweed | foliage | yes | overgrown | On water; flooded streets and ponds |
| `BiomesOPlenty:foliage:1`, `:2`, `:10`, `:11` | Short, Medium, Wheat, Damp Grass | foliage | yes | overgrown | Tinted tufts; weeds through pavement |
| `BiomesOPlenty:foliage:3`, `:6` | Flax (bottom, top) | foliage | partly | park-garden | |
| `BiomesOPlenty:foliage:4` | Bush | foliage | yes | park-garden, overgrown | Small round bush |
| `BiomesOPlenty:foliage:5` | Sprout | foliage | yes | overgrown | |
| `BiomesOPlenty:foliage:7` | Poison Ivy | off-theme | no | | Poison 5 s without boots and leggings |
| `BiomesOPlenty:foliage:8` | Berry Bush | foliage | partly | park-garden | Gives berries on use |
| `BiomesOPlenty:foliage:9` | Shrub | foliage | yes | overgrown, ruin | Dry twiggy shrub |
| `BiomesOPlenty:foliage:12` | Koru | foliage | partly | park-garden | Fern fronds |
| `BiomesOPlenty:foliage:13` | Clover Patch | foliage | yes | park-garden | Flat ground cover |
| `BiomesOPlenty:foliage:14`, `:15` | Leaf Pile, Dead Leaf Pile | foliage, rubble-ruin | yes | ruin, overgrown | Flat leaf litter; the best "abandoned" floor dressing in the mod |

### Vines, mushrooms, coral, saplings and misc (sheet `vines-saplings-misc`)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `BiomesOPlenty:ivy:0-15` | Ivy | foliage | yes | overgrown, ruin | Vine side bits as vanilla; tinted green; spreads like vines. Best facade overgrowth. Fallback `minecraft:vine` |
| `BiomesOPlenty:moss:0-15` | Moss | foliage | yes | overgrown, ruin | Vine-like sheet of moss; damp walls and basements |
| `BiomesOPlenty:willow:0-15` | Willow | foliage | partly | overgrown | Hanging willow strands (tinted) |
| `BiomesOPlenty:treeMoss:0-15` | Tree Moss | foliage | partly | overgrown | Dark hanging moss |
| `BiomesOPlenty:flowerVine:0-15` | Flowering Vines | foliage | yes | overgrown, park-garden | Vines with white flowers; arbors and facades |
| `BiomesOPlenty:wisteria:0-15` | Wisteria | foliage | yes | park-garden | Lilac hanging flowers; pergolas |
| `BiomesOPlenty:mushrooms:0`-`:2`, `:4` | Toadstool, Portobello, Blue Milk Cap, Flat Mushroom | foliage, rubble-ruin | partly | ruin | Damp basements |
| `BiomesOPlenty:mushrooms:3` | Glowshroom | light | partly | ruin | Light 6; dim glow in tunnels |
| `BiomesOPlenty:mushrooms:5` | Shadow Shroom | off-theme | no | | |
| `BiomesOPlenty:coral1:8-11` | Kelp (bottom, middle, top, single) | foliage | partly | overgrown | Underwater only; flooded ruins |
| `BiomesOPlenty:coral1:12-14` | Pink, Orange, Blue Coral | off-theme | no | | Reef only |
| `BiomesOPlenty:coral1:15` | Glowing Coral | off-theme | no | | Light 10 |
| `BiomesOPlenty:coral2:8` | Algae | foliage | partly | overgrown | Underwater |
| `BiomesOPlenty:lilyBop:0-2` | Medium, Small, Tiny Lily Pad | foliage | yes | park-garden | On water |
| `BiomesOPlenty:saplings:0-15` | 16 saplings (apple to persimmon) | foliage | partly | park-garden | +8 = growth stage; they grow into trees on random ticks, which can break a planter |
| `BiomesOPlenty:colorizedSaplings:0-7` | 8 saplings (sacred oak to flowering oak) | foliage | partly | park-garden | As above |
| `BiomesOPlenty:bamboo:0` | Bamboo | foliage, fence-rail | partly | park-garden | Thin post plant, grows upward |
| `BiomesOPlenty:fruitBop:0-4` | Apple, Persimmon, Peach, Pear, Pinecone | off-theme | no | | Hanging fruit; needs leaves above |
| `BiomesOPlenty:turnip:0-7` | Turnip | foliage | partly | park-garden | Crop stages |
| `BiomesOPlenty:poison`, `hell_blood`, `honey` | Poison, Blood, Honey (fluids) | off-theme | no | | Never place |

## Best variety pools

- **wall-main / floor (urban stone):** `rocks:5` polished shale (dark smooth concrete), `rocks:3` polished siltstone (buff dressed stone), `rocks:4` shale and `rocks:2` siltstone for rougher walls. These four add two new stone colors (charcoal and buff) that the vanilla mapping lacks.
- **wall-main (brick, ruin):** `mudBricks:0` with `mudBricksStairs` and `stoneSingleSlab:0`: a full brick set in a muddier tone than vanilla bricks.
- **floor (interior wood):** planks 2 (dark), 3 (fir), 6 (mangrove), 7 (palm), 8 (redwood), 11 (pine), 12 (hellbark), 13 (jacaranda), 14 (mahogany), each with matching stairs and slabs. Nine natural wood tones, double vanilla's six.
- **street / sidewalk:** `rocks:5` and `rocks:4` as dark paving; `hardDirt:0` and `hardSand:0` for unpaved desert lots.
- **foliage (overgrown ruins):** `ivy`, `moss`, `flowerVine`, `foliage:14`/`:15` leaf piles, `plants:0` dead grass, `foliage:1`/`:2` short and medium grass, `foliage:9` shrub, `plants:15` root, `leaves2:0` dying leaves, `logs3:2` dead wood.
- **foliage (parks):** flowers `flowers:4`-`:9`, `flowers2:0`, `:1`, `:3`-`:8`; leaves `leaves3:1`/`:2`/`:3` (cherry and maple), `leaves1:0`, `leaves2:3` (autumn), `wisteria`; `lilyBop`, `flowers:10`, `plants:7` cattails for ponds; `newBopDirt` and `newBopGrass` for planters.
- **rubble-ruin:** `ashStone:0`, `ash:0` (sparingly), `hardDirt:0`, `driedDirt:0`, `logs3:2`, `stoneFormations` in flooded underground spaces.
- **roof:** `planks:10` bamboo thatching for park pavilions; otherwise the wood slabs above.
- **light:** only the glow plants (`flowers:3` light 9, `mushrooms:3` light 6); no lamp blocks.

## Worldgen notes

- **No tile entities:** every block is placeable with `setBlock` and metadata only.
- **Leaves:** always place with bit 4 set (meta + 4), or they decay within minutes when no log is within 4 blocks.
- **Vines** need a solid neighbor on the side their bits name, as vanilla; they also spread on random ticks, so a facade covered at generation slowly fills in further. Acceptable for an overgrown look.
- **Plants and flowers** pop off as items if the block beneath is not soil (`canBlockStay`), which happens when ruin damage removes the ground. The damage pass should remove plants with their soil.
- **Saplings** grow into full trees on random ticks; use flowers or bushes, not saplings, in planters that must keep their shape.
- **Grass blocks** (`newBopGrass`, `longGrass`) spread and decay like vanilla grass under a solid block.
- **Hazards:** never use `mud:0`, `mud:1`, `ash:0` on walkways, nor `plants:5`, `plants:12`, `flowers:2`, `flowers2:2`, `foliage:7`, `bopGrass:1`, or the three fluids anywhere players walk.
- **Ore and boulder hosts:** none; safe as building blocks under the D18 rule.
- **Fire:** planks, slabs, stairs, logs (except hellbark), leaves and vines are flammable (`BOPBlocks.setFireInfo`); burning ruins spread fire like vanilla wood.
- **Without BoP:** palette entries fall back to the vanilla blocks named in the tables (`minecraft:planks:*`, `minecraft:sandstone:2`, `minecraft:double_stone_slab:8`, `minecraft:brick_block`, `minecraft:dirt`, `minecraft:vine`, `minecraft:red_flower`, `minecraft:leaves`). Nothing needs NBT, so the fallback is a straight block swap.
