# Block catalog: Railcraft

Railcraft 9.17.36 (mod id `Railcraft`, jar `Railcraft-9.17.36.jar`) as GTNH 2.9.0 ships and configures it. Registrations were read from the upstream source at tag `9.17.36` (`gtnh-src/source/Railcraft/`, `mods.railcraft.common.blocks.aesthetics.*`, `blocks/machine`, `blocks/frame`, `blocks/anvil`); availability from the pack's `config/railcraft/blocks.cfg` and `modules.cfg`; English names from the jar's `assets/railcraft/lang/en_US.lang`. Sheets: `tools/block-contact-sheet/sheets/railcraft/{stone-bricks,cube,glass,lanterns,posts,walls,structure-parts}.json`, rendered to `docs/previews/blocks/railcraft/` (local only). This catalog feeds palette variety under [decision log](../../../design/decision-log.md) D25; the [D19 block mapping](../../00-pack-integration/block-mapping.md) stays the baseline.

Registry names: FML prefixes Railcraft's tag (the unlocalized name minus `tile.railcraft.`) with the mod id, so the brick block registered as `brick.abyssal` is `Railcraft:brick.abyssal` (`RailcraftRegistry.register`, `MiscTools.cleanTag`; mod id from `Railcraft.MOD_ID`).

## Summary

**252 block variants in 23 registry blocks**, all enabled by the pack: every module is on, and the only disabled sub-blocks are ores, the rock crusher and residual heat, none of them decorative. 156 are plain block+meta. **96 are tile entities:** the 88 slab and stair variants, the 7 multiblock parts and the emblem post.

| Role (a block can have several) | Count |
|---|---|
| wall-main | 18 |
| wall-accent | 24 |
| trim-pillar | 14 |
| floor | 9 |
| ceiling | 0 (any wall-main block works) |
| roof | 44 (slabs and stairs, tile entities) |
| glass | 10 |
| pane | 0 (Railcraft has no pane) |
| street | 3 |
| sidewalk | 4 |
| stairs-slab | 68 of the 88 slab and stair variants (the other 20 are off-theme) |
| rubble-ruin | 27 |
| light | 16 |
| fence-rail | 39 (walls, posts and platforms) |
| door-trap | 0 |
| furniture-deco | 3 (steel anvil, wooden and stone platforms) |
| industrial-dressing | 33 |
| foliage | 0 |
| special | 3 |
| off-theme | 55 |

**Fit with upstream's look:** 107 yes, 89 partly, 56 no (counting each slab and stair material separately). The quarried, bleached bone, abyssal and infernal stone families are the strongest finds: four full brick-to-cobble sets (brick, fitted, block, ornate, etched, cobble) in light gray, cream, near-black and dark gray, with matching walls, slabs, stairs and lanterns.

**Flags**

- **Tile entities.** `Railcraft:slab` and `Railcraft:stair` store their material in a tile entity (`TileSlab`, `TileStair`); block metadata is only the orientation. Placing one in worldgen means writing the tile entity's NBT as well. The emblem post (`Railcraft:post:3`) and every `machine.*` block (coke oven, tanks) are tile entities. Everything else here is plain block+meta: the bricks, `cube`, glass, lanterns, posts, platforms and walls.
- **Light.** All 16 lanterns emit light 15 (`setLightLevel(0.9375F)`), with no power or tile entity, and render a flame and smoke particles.
- **Gravity.** Crushed obsidian (`Railcraft:cube:4`) falls like sand. The steel anvil falls like a vanilla anvil.
- **Hardness and blast resistance.** The themed bricks are hardness 5 and resistance 15, about three times slower to mine than stone bricks. That is noticeable but not painful. Lanterns are 5/15. The steel anvil has resistance 2000. Metal posts need an iron pickaxe (harvest level 2).
- **No mob spawning.** The pack sets `cannotMobsSpawnOnDecorBlocks=true` (`config/railcraft/railcraft.cfg`), so no mob spawns on the themed bricks, the `cube` blocks or Railcraft slabs. A building floored with these blocks has no natural spawns. That matters for upstream's spawner rooms and dark interiors: spawners still work, natural spawns don't.
- **Ore and boulder hosts.** None. GT's ore hosts and boulder hosts are fixed lists of vanilla, GT and Et Futurum blocks ([ore-veins.md](../../00-pack-integration/ore-veins.md)). No Railcraft block is on either, including the raw abyssal and quarried stones.
- **Tier.** Only the multiblock parts are machine-like: the coke oven, the advanced coke oven, the water tank and the iron and steel tanks are all steam age, far below EV. The GTNH-added `machine.zeta`/`machine.eta` tanks (aluminium up to neutronium) are not catalogued: tungstensteel and above pass the EV ceiling of [record 0004](../../../design/0004-gtnh-layer-and-tier-ceiling.md), and the rest add nothing the steel tank doesn't.
- **Not catalogued:** tracks (tile entities, and upstream's rails map to vanilla), signals and signal boxes, detectors, loaders, engines, boilers, anchors and other machines, plus ores and fluids.

## Themed stone bricks

Sheet `stone-bricks.json`. Each theme is one block, `Railcraft:brick.<theme>`, with metas 0 brick, 1 fitted, 2 block, 3 ornate, 4 etched, 5 cobblestone (`BrickVariant`). The texture is a six-icon strip (`brick.<theme>.png`). Hardness 5, resistance 15, no mob spawns. Nether meta 0 is never handed out (vanilla nether brick stands in for it) and is left out of the count. Ornate has a concentric-square inlay. Etched has a carved pattern that reads as a creeper face at block scale, so it is an accent at most.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Railcraft:brick.quarried:0` | Quarried Brick | wall-main | yes | urban, ruined, industrial | Light gray, almost white running bond: the cleanest stone-brick alternative in the pack |
| `Railcraft:brick.quarried:1` | Fitted Quarried Stone | wall-main, floor | yes | urban, ruined | Large ashlar blocks |
| `Railcraft:brick.quarried:2` | Quarried Block | floor, sidewalk, trim-pillar, wall-main | yes | urban, industrial | Smooth light gray, bevelled edge: reads as smooth stone or concrete |
| `Railcraft:brick.quarried:3` | Ornate Quarried Stone | wall-accent, trim-pillar | partly: decorative inlay, use as a band | urban | |
| `Railcraft:brick.quarried:4` | Etched Quarried Stone | wall-accent | partly: creeper-face motif | urban | Sparingly |
| `Railcraft:brick.quarried:5` | Quarried Cobblestone | rubble-ruin | yes | ruined, overgrown | Light cobble |
| `Railcraft:brick.bleachedbone:0` | Bleached Bone Brick | wall-main | yes | urban, desert | Cream limestone brick, like 1920s civic buildings |
| `Railcraft:brick.bleachedbone:1` | Fitted Bleached Bone Stone | wall-main | yes | urban, desert | |
| `Railcraft:brick.bleachedbone:2` | Bleached Bone Block | wall-main, floor, trim-pillar | yes | urban, desert | Smooth cream: alternative to smooth quartz and cut sandstone |
| `Railcraft:brick.bleachedbone:3` | Ornate Bleached Bone Stone | wall-accent, trim-pillar | partly: decorative inlay | urban, desert | |
| `Railcraft:brick.bleachedbone:4` | Etched Bleached Bone Stone | wall-accent | partly: creeper-face motif | desert | |
| `Railcraft:brick.bleachedbone:5` | Bleached Bone Cobblestone | rubble-ruin | yes | ruined, desert | |
| `Railcraft:brick.abyssal:0` | Abyssal Brick | wall-main, wall-accent | yes | urban, industrial | Near-black brick: like polished blackstone bricks (upstream `building7`) |
| `Railcraft:brick.abyssal:1` | Fitted Abyssal Stone | wall-main, floor | yes | urban, industrial | |
| `Railcraft:brick.abyssal:2` | Abyssal Block | floor, trim-pillar, wall-accent | yes | urban, industrial | Smooth black, bevelled |
| `Railcraft:brick.abyssal:3` | Ornate Abyssal Stone | wall-accent | partly: decorative inlay | industrial | |
| `Railcraft:brick.abyssal:4` | Etched Abyssal Stone | wall-accent | partly: creeper-face motif | industrial | |
| `Railcraft:brick.abyssal:5` | Abyssal Cobblestone | rubble-ruin | yes | ruined, industrial | |
| `Railcraft:brick.infernal:0` | Infernal Brick | wall-main | yes | urban, industrial, ruined | Dark gray-brown brick; reads as sooty factory brick |
| `Railcraft:brick.infernal:1` | Fitted Infernal Stone | wall-main | yes | industrial | |
| `Railcraft:brick.infernal:2` | Infernal Block | floor, trim-pillar | yes | industrial | Smooth dark gray |
| `Railcraft:brick.infernal:3` | Ornate Infernal Stone | wall-accent | partly: decorative inlay | industrial | |
| `Railcraft:brick.infernal:4` | Etched Infernal Stone | wall-accent | partly: creeper-face motif | industrial | |
| `Railcraft:brick.infernal:5` | Infernal Cobblestone | rubble-ruin | yes | ruined, industrial | |
| `Railcraft:brick.bloodstained:0` | Blood Stained Brick | wall-accent | partly: saturated crimson, stronger than red terracotta | urban | Red-brick accent, a band or one facade in a block |
| `Railcraft:brick.bloodstained:1` | Fitted Blood Stained Stone | wall-accent | partly: saturated crimson | urban | |
| `Railcraft:brick.bloodstained:2` | Blood Stained Block | wall-accent | partly: flat crimson | urban | |
| `Railcraft:brick.bloodstained:3` | Ornate Blood Stained Stone | off-theme | no | none | |
| `Railcraft:brick.bloodstained:4` | Etched Blood Stained Stone | off-theme | no | none | |
| `Railcraft:brick.bloodstained:5` | Blood Stained Cobblestone | rubble-ruin | partly: red rubble only next to red walls | ruined | |
| `Railcraft:brick.sandy:0` | Sandy Brick | wall-main | yes | desert | Tan brick; desert variety next to sandstone |
| `Railcraft:brick.sandy:1` | Fitted Sandy Stone | wall-main | yes | desert | |
| `Railcraft:brick.sandy:2` | Sandy Block | floor, trim-pillar, wall-main | yes | desert | |
| `Railcraft:brick.sandy:3` | Ornate Sandy Stone | wall-accent | partly: decorative inlay | desert | |
| `Railcraft:brick.sandy:4` | Etched Sandy Stone | wall-accent | partly: creeper-face motif | desert | |
| `Railcraft:brick.sandy:5` | Sandy Cobblestone | rubble-ruin | yes | desert, ruined | |
| `Railcraft:brick.nether:1` | Fitted Nether Stone | wall-accent | partly: nether palette | desert | Dark maroon; next to red nether bricks (desert `B`) |
| `Railcraft:brick.nether:2` | Nether Block | wall-accent, trim-pillar | partly: nether palette | desert | |
| `Railcraft:brick.nether:3` | Ornate Nether Stone | off-theme | no | none | |
| `Railcraft:brick.nether:4` | Etched Nether Stone | off-theme | no | none | |
| `Railcraft:brick.nether:5` | Nether Cobblestone | off-theme | no | none | |
| `Railcraft:brick.frostbound:0` | Frost Bound Brick | wall-accent | partly: blue glazed brick, loud | urban | One accent band at most |
| `Railcraft:brick.frostbound:1` | Fitted Frost Bound Stone | wall-accent | partly: loud blue | urban | |
| `Railcraft:brick.frostbound:2` | Frost Bound Block | wall-accent | partly: loud blue | urban | Flat steel blue |
| `Railcraft:brick.frostbound:3` | Ornate Frost Bound Stone | off-theme | no | none | |
| `Railcraft:brick.frostbound:4` | Etched Frost Bound Stone | off-theme | no | none | |
| `Railcraft:brick.frostbound:5` | Frost Bound Cobblestone | off-theme | no | none | |

## Cube blocks

Sheet `cube.json`. `Railcraft:cube`, meta = `EnumCube` ordinal. Hardness and resistance per meta (`EnumCube`; effective blast resistance is the listed value × 3/5). No mob spawns.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Railcraft:cube:0` | Block of Coal Coke | industrial-dressing | partly: dark lumpy gray, reads as stockpile | industrial | Flammable, burns as fuel; hardness 2 |
| `Railcraft:cube:1` | Block of Concrete | wall-main, floor, street, sidewalk | yes | urban, industrial, ruined | Plain mid-gray concrete; hardness 3. Strong upstream fit for streets and plain facades |
| `Railcraft:cube:2` | Block of Steel | industrial-dressing, wall-accent | partly: metal plate with a beveled frame | industrial | Hardness 5. Treasure-block candidate (upstream `!`) |
| `Railcraft:cube:3` | Infernal Brick (obsolete) | off-theme | no | none | Deprecated `ReplacerCube`: converts to `Railcraft:brick.infernal`. Do not place |
| `Railcraft:cube:4` | Crushed Obsidian | special, rubble-ruin | partly: purple-black gravel | ruined | **Falls like sand**; never allows mob spawns |
| `Railcraft:cube:5` | Sandy Brick (obsolete) | off-theme | no | none | Deprecated `ReplacerCube`. Do not place |
| `Railcraft:cube:6` | Abyssal Stone | wall-main, rubble-ruin | partly: raw dark stone | ruined, industrial | Not a GT host |
| `Railcraft:cube:7` | Quarried Stone | wall-main, rubble-ruin | partly: raw pale stone | ruined | Not a GT host |
| `Railcraft:cube:8` | Creosote Wood Block | floor, wall-accent, industrial-dressing | yes | industrial, urban | Dark tarred wood (uses the wooden-post texture), like railroad ties; **flammable** (`FlammableCube(5, 300)`) |
| `Railcraft:cube:9` | Block of Copper | industrial-dressing, wall-accent | partly: bright orange metal | industrial | Treasure candidate |
| `Railcraft:cube:10` | Block of Tin | industrial-dressing | partly: near-white metal | industrial | |
| `Railcraft:cube:11` | Block of Lead | industrial-dressing, wall-accent | partly: dull blue-gray plate | industrial | Reads as a zinc or lead roof |

## Strengthened glass

Sheet `glass.json`. `Railcraft:glass`, metas 0-15 in dye order (meta 0 white … 15 black: the color is `EnumColor.fromId(15 - meta)`). Hardness 1, resistance 5. It is clear glass with a light-gray frame at the corners, and it **connects vertically** with same-colored neighbors: the frame disappears between stacked blocks, so a column reads as one tall pane. **The color is a runtime tint** (`colorMultiplier`) of one white texture, so the sheet shows all 16 untinted; in game the frame and any opaque pixels take the dye color.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Railcraft:glass:0` | White Strengthened Glass | glass | yes | urban, industrial | Framed window; tall windows when stacked. Pairs with upstream `glass_full_white` |
| `Railcraft:glass:1` | Orange Strengthened Glass | glass | partly: tinted frame | industrial | |
| `Railcraft:glass:2` | Magenta Strengthened Glass | off-theme | no | none | |
| `Railcraft:glass:3` | Light Blue Strengthened Glass | glass | yes | urban | Upstream `glass_full_light_blue` family |
| `Railcraft:glass:4` | Yellow Strengthened Glass | glass | partly | industrial | |
| `Railcraft:glass:5` | Lime Strengthened Glass | off-theme | no | none | |
| `Railcraft:glass:6` | Pink Strengthened Glass | off-theme | no | none | |
| `Railcraft:glass:7` | Gray Strengthened Glass | glass | yes | urban, industrial | Upstream `glass_full_gray` family |
| `Railcraft:glass:8` | Light Gray Strengthened Glass | glass | yes | urban | |
| `Railcraft:glass:9` | Cyan Strengthened Glass | glass | partly | urban | |
| `Railcraft:glass:10` | Purple Strengthened Glass | off-theme | no | none | |
| `Railcraft:glass:11` | Blue Strengthened Glass | glass | yes | urban | Upstream `glass_full_blue` family |
| `Railcraft:glass:12` | Brown Strengthened Glass | glass | partly | industrial | |
| `Railcraft:glass:13` | Green Strengthened Glass | off-theme | no | none | |
| `Railcraft:glass:14` | Red Strengthened Glass | off-theme | no | none | |
| `Railcraft:glass:15` | Black Strengthened Glass | glass | yes | urban, industrial | Oil rig `I` (black stained glass) alternative |

## Lanterns

Sheet `lanterns.json`. `Railcraft:lantern.stone` (metas 0-9) and `Railcraft:lantern.metal` (metas 0-5): a candle in a small cage with caps of the source material. Plain blocks (no tile entity), **light 15**, flame and smoke particles, hardness 5, resistance 15. The sheet approximates the model: top = the cap texture, side = the candle texture.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Railcraft:lantern.stone:0` | Abyssal Stone Lantern | light | yes | urban, industrial | Black caps: street-lamp or doorway light |
| `Railcraft:lantern.stone:1` | Bleached Bone Stone Lantern | light | yes | urban, desert | |
| `Railcraft:lantern.stone:2` | Blood Stained Stone Lantern | light | partly: red caps | urban | |
| `Railcraft:lantern.stone:3` | Frostbound Stone Lantern | light | partly: blue caps | urban | |
| `Railcraft:lantern.stone:4` | Infernal Stone Lantern | light | yes | industrial | |
| `Railcraft:lantern.stone:5` | Nether Stone Lantern | light | partly | desert | |
| `Railcraft:lantern.stone:6` | Quarried Stone Lantern | light | yes | urban | |
| `Railcraft:lantern.stone:7` | Sandy Stone Lantern | light | yes | desert | |
| `Railcraft:lantern.stone:8` | Sandstone Lantern | light | yes | desert | Vanilla sandstone-slab caps |
| `Railcraft:lantern.stone:9` | Stone Lantern | light | yes | urban, ruined | Vanilla stone-slab caps |
| `Railcraft:lantern.metal:0` | Iron Lantern | light | yes | urban, industrial | Alternative to the Et Futurum lantern |
| `Railcraft:lantern.metal:1` | Gold Lantern | light | partly: gold caps | urban | Lobby or hotel entrance |
| `Railcraft:lantern.metal:2` | Copper Lantern | light | yes | urban, industrial | |
| `Railcraft:lantern.metal:3` | Tin Lantern | light | yes | urban | |
| `Railcraft:lantern.metal:4` | Lead Lantern | light | yes | industrial | |
| `Railcraft:lantern.metal:5` | Steel Lantern | light | yes | urban, industrial | |

## Posts and platforms

Sheet `posts.json`. Thin posts that connect to each other, to fences and to signals; a "platform" is a post with a full-size top slab. The cube in the sheet shows the texture only. `Railcraft:post` metas 0-6 (`EnumPost`); `Railcraft:post.metal` and `Railcraft:post.metal.platform` metas 0-15 in Railcraft's `EnumColor` order, which is **reverse dye order** (0 black, 1 red, 2 green, 3 brown, 4 blue, 5 purple, 6 cyan, 7 light gray, 8 gray, 9 pink, 10 lime, 11 yellow, 12 light blue, 13 magenta, 14 orange, 15 white). Hardness 3, resistance 15; metal posts need an iron pickaxe. Only the emblem post has a tile entity.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Railcraft:post:0` | Wooden Post | fence-rail, trim-pillar | yes | urban, ruined, overgrown | Creosote-dark wood: utility pole, pier piling |
| `Railcraft:post:1` | Stone Post | fence-rail, trim-pillar | yes | urban | Concrete bollard |
| `Railcraft:post:2` | Metal Post | fence-rail, trim-pillar | yes | urban, industrial, ruined | Rust-orange steel: street-light or sign pole, topped with a lantern |
| `Railcraft:post:3` | Emblem Post | special | no | none | **Tile entity** (`TilePostEmblem`); needs an emblem to mean anything |
| `Railcraft:post:4` | Wooden Platform | fence-rail, furniture-deco | partly: post with a slab top | urban, overgrown | Pier or table |
| `Railcraft:post:5` | Stone Platform | fence-rail, furniture-deco | partly | urban | Bench-like |
| `Railcraft:post:6` | Metal Platform | industrial-dressing | partly | industrial | |
| `Railcraft:post.metal:0` | Black Metal Post | fence-rail, trim-pillar | yes | urban, industrial | Lamp post |
| `Railcraft:post.metal:1` | Red Metal Post | fence-rail | partly: bright red | urban | Hydrant or signal pole |
| `Railcraft:post.metal:2` | Green Metal Post | fence-rail | yes | urban | Classic painted street-lamp green |
| `Railcraft:post.metal:3` | Brown Metal Post | fence-rail | partly | urban | |
| `Railcraft:post.metal:4` | Blue Metal Post | fence-rail | partly | urban | |
| `Railcraft:post.metal:5` | Purple Metal Post | off-theme | no | none | |
| `Railcraft:post.metal:6` | Cyan Metal Post | fence-rail | partly | urban | |
| `Railcraft:post.metal:7` | Light Gray Metal Post | fence-rail, trim-pillar | yes | urban, industrial | Galvanized pole |
| `Railcraft:post.metal:8` | Gray Metal Post | fence-rail, trim-pillar | yes | urban, industrial | |
| `Railcraft:post.metal:9` | Pink Metal Post | off-theme | no | none | |
| `Railcraft:post.metal:10` | Lime Metal Post | off-theme | no | none | |
| `Railcraft:post.metal:11` | Yellow Metal Post | fence-rail, industrial-dressing | yes | industrial, urban | Safety-yellow bollard |
| `Railcraft:post.metal:12` | Light Blue Metal Post | off-theme | no | none | |
| `Railcraft:post.metal:13` | Magenta Metal Post | off-theme | no | none | |
| `Railcraft:post.metal:14` | Orange Metal Post | fence-rail, industrial-dressing | partly | industrial | Safety orange |
| `Railcraft:post.metal:15` | White Metal Post | fence-rail | yes | urban | |
| `Railcraft:post.metal.platform:0` | Black Metal Platform | fence-rail, industrial-dressing | partly | industrial, urban | |
| `Railcraft:post.metal.platform:1` | Red Metal Platform | industrial-dressing | partly | industrial | |
| `Railcraft:post.metal.platform:2` | Green Metal Platform | fence-rail | partly | urban | |
| `Railcraft:post.metal.platform:3` | Brown Metal Platform | fence-rail | partly | urban | |
| `Railcraft:post.metal.platform:4` | Blue Metal Platform | off-theme | no | none | |
| `Railcraft:post.metal.platform:5` | Purple Metal Platform | off-theme | no | none | |
| `Railcraft:post.metal.platform:6` | Cyan Metal Platform | off-theme | no | none | |
| `Railcraft:post.metal.platform:7` | Light Gray Metal Platform | industrial-dressing | partly | industrial | Catwalk support |
| `Railcraft:post.metal.platform:8` | Gray Metal Platform | industrial-dressing | partly | industrial | |
| `Railcraft:post.metal.platform:9` | Pink Metal Platform | off-theme | no | none | |
| `Railcraft:post.metal.platform:10` | Lime Metal Platform | off-theme | no | none | |
| `Railcraft:post.metal.platform:11` | Yellow Metal Platform | industrial-dressing | partly | industrial | |
| `Railcraft:post.metal.platform:12` | Light Blue Metal Platform | off-theme | no | none | |
| `Railcraft:post.metal.platform:13` | Magenta Metal Platform | off-theme | no | none | |
| `Railcraft:post.metal.platform:14` | Orange Metal Platform | off-theme | no | none | |
| `Railcraft:post.metal.platform:15` | White Metal Platform | fence-rail | partly | urban | |

## Walls

Sheet `walls.json`. Wall-shaped blocks (like `cobblestone_wall`), plain metadata, no tile entity. `Railcraft:wall.alpha` metas 0-15, `Railcraft:wall.beta` metas 0-8 (`EnumWallAlpha`, `EnumWallBeta`). Hardness and resistance follow the source block. The sheet shows the source texture on a full cube. These give upstream's `w` (cobblestone wall) many alternatives.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Railcraft:wall.alpha:0` | Infernal Brick Wall | fence-rail | yes | industrial, urban | |
| `Railcraft:wall.alpha:1` | Sandy Brick Wall | fence-rail | yes | desert | |
| `Railcraft:wall.alpha:2` | Concrete Wall | fence-rail | yes | urban, industrial | Jersey-barrier look on highways |
| `Railcraft:wall.alpha:3` | Snow Wall | off-theme | no | none | |
| `Railcraft:wall.alpha:4` | Ice Wall | off-theme | no | none | Melts near light |
| `Railcraft:wall.alpha:5` | Stone Brick Wall | fence-rail | yes | urban, ruined | Vanilla-texture wall that 1.7.10 lacks |
| `Railcraft:wall.alpha:6` | Mossy Stone Brick Wall | fence-rail, rubble-ruin | yes | overgrown, ruined | |
| `Railcraft:wall.alpha:7` | Cracked Stone Brick Wall | fence-rail, rubble-ruin | yes | ruined | |
| `Railcraft:wall.alpha:8` | Chiseled Stone Brick Wall | fence-rail | partly: busy pattern | urban | |
| `Railcraft:wall.alpha:9` | Nether Brick Wall | fence-rail | partly | desert | |
| `Railcraft:wall.alpha:10` | Brick Wall | fence-rail | yes | urban | Garden wall; also Et Futurum's |
| `Railcraft:wall.alpha:11` | Sandstone Wall | fence-rail | yes | desert | |
| `Railcraft:wall.alpha:12` | Chiseled Sandstone Wall | fence-rail | partly | desert | |
| `Railcraft:wall.alpha:13` | Smooth Sandstone Wall | fence-rail | yes | desert | |
| `Railcraft:wall.alpha:14` | Obsidian Wall | off-theme | no | none | Hardness 50 |
| `Railcraft:wall.alpha:15` | Frost Bound Brick Wall | off-theme | no | none | |
| `Railcraft:wall.beta:0` | Quartz Wall | fence-rail | yes | urban | Balustrade for quartz buildings |
| `Railcraft:wall.beta:1` | Chiseled Quartz Wall | fence-rail | partly | urban | |
| `Railcraft:wall.beta:2` | Iron Wall | fence-rail, industrial-dressing | partly | industrial | |
| `Railcraft:wall.beta:3` | Gold Wall | off-theme | no | none | Lootable value |
| `Railcraft:wall.beta:4` | Diamond Wall | off-theme | no | none | Lootable value |
| `Railcraft:wall.beta:5` | Abyssal Brick Wall | fence-rail | yes | urban, industrial | |
| `Railcraft:wall.beta:6` | Quarried Brick Wall | fence-rail | yes | urban | |
| `Railcraft:wall.beta:7` | Blood Stained Brick Wall | fence-rail | partly | urban | |
| `Railcraft:wall.beta:8` | Bleached Bone Brick Wall | fence-rail | yes | urban, desert | |

## Slabs and stairs (tile entities)

No sheet: each variant uses the texture of its material, already on the sheets above. `Railcraft:slab` and `Railcraft:stair` hold one of 44 materials (`EnumBlockMaterial`) in a tile entity; the item damage is the material's ordinal, and the block metadata is only the orientation. All 88 are enabled (`subblocks` in `blocks.cfg`). Hardness and resistance follow the material. Placing them needs the tile entity written with the material, which the asset model does not do yet. Vanilla, Et Futurum and Chisel stairs and slabs are plain metadata.

| registry name (TE material) | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Railcraft:slab`, `Railcraft:stair` (QUARRIED_BRICK, _FITTED, _BLOCK, _COBBLE) | Quarried Brick/Fitted/Block/Cobble Slab and Stairs | stairs-slab, roof, rubble-ruin | yes | urban, ruined | 8 variants; the best match for upstream's stone-brick stairs and slabs |
| same (BLEACHEDBONE_*) | Bleached Bone … Slab and Stairs | stairs-slab, roof | yes | urban, desert | 8 |
| same (ABYSSAL_*) | Abyssal … Slab and Stairs | stairs-slab, roof, rubble-ruin | yes | urban, industrial | 8; blackstone-rubble stand-in |
| same (INFERNAL_*) | Infernal … Slab and Stairs | stairs-slab, roof | yes | industrial | 8 |
| same (SANDY_*) | Sandy … Slab and Stairs | stairs-slab | yes | desert | 8 |
| same (BLOODSTAINED_*) | Blood Stained … Slab and Stairs | stairs-slab | partly: crimson | urban | 8 |
| same (FROSTBOUND_*) | Frost Bound … Slab and Stairs | off-theme | no | none | 8 |
| same (NETHER_FITTED, _BLOCK, _COBBLE) | Nether … Slab and Stairs | stairs-slab | partly | desert | 6 |
| same (CONCRETE) | Concrete Slab and Stairs | stairs-slab, street, sidewalk | yes | urban, industrial | 2; curbs and steps |
| same (CREOSOTE) | Creosote Block Slab and Stairs | stairs-slab, roof | yes | industrial, urban | 2; flammable |
| same (IRON, STEEL, COPPER, TIN, LEAD) | metal Slab and Stairs | stairs-slab, roof, industrial-dressing | partly: metal roofs | industrial | 10; lead and steel read as sheet-metal roofing |
| same (GOLD, DIAMOND) | Gold, Diamond Slab and Stairs | off-theme | no | none | 4; lootable value |
| same (OBSIDIAN) | Obsidian Slab and Stairs | off-theme | no | none | 2; hardness 50 |
| same (SNOW, ICE, PACKED_ICE) | Snow, Ice, Packed Ice Slab and Stairs | off-theme | no | none | 6; ice melts near light |

## Multiblock parts and other blocks

Sheet `structure-parts.json`. The `machine.*` blocks are **tile entities** (Railcraft multiblock parts). Placed alone they stay inert single blocks that try to form a multiblock when neighbors change, and they cost a tile entity each. Whether they may be used as set dressing is an owner decision (below). Tier: all steam age, well under EV.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `Railcraft:machine.alpha:7` | Coke Oven Brick | industrial-dressing, wall-main | partly: tile entity | industrial | Tan brick; GTNH marks it deprecated |
| `Railcraft:machine.alpha:12` | Advanced Coke Oven Brick | industrial-dressing, wall-main | partly: tile entity | industrial | Dark gray firebrick; a good factory wall if TE cost is accepted |
| `Railcraft:machine.alpha:14` | Water Tank Siding | industrial-dressing, special | partly: tile entity | urban, industrial | Banded wooden staves: **the rooftop water tower** of 20th-century cities. Upstream has none |
| `Railcraft:machine.beta:0` | Iron Tank Wall | industrial-dressing | partly: tile entity | industrial | Riveted plate; tank farms |
| `Railcraft:machine.beta:1` | Iron Tank Gauge | industrial-dressing | partly: tile entity | industrial | Glass gauge strip |
| `Railcraft:machine.beta:13` | Steel Tank Wall | industrial-dressing | partly: tile entity | industrial | |
| `Railcraft:machine.beta:14` | Steel Tank Gauge | industrial-dressing | partly: tile entity | industrial | |
| `Railcraft:frame:0` | Wire Support Frame | industrial-dressing | partly: open rusty frame | industrial, ruined | No tile entity; hardness 5. Reads as a steel truss segment |
| `Railcraft:anvil:0` | Steel Anvil | furniture-deco | yes | urban, industrial | Upstream `F` furniture (anvil) variety. **Falls**; resistance 2000; metas encode facing and damage like the vanilla anvil |

## Best variety pools

Weights are suggestions out of the role's total, beside the D19 baseline block.

- **`bricks_standard` `#` (main wall, stone brick):** quarried brick (`brick.quarried:0`) 3, fitted quarried (`:1`) 2, bleached bone brick (`brick.bleachedbone:0`) 2, infernal brick (`brick.infernal:0`) 1, beside vanilla stone bricks. For the `stonebrick` variant's cracked and mossy slots, Railcraft has nothing; use the matching cobble (`:5`) in rubble only.
- **Dark-brick buildings (`blackstone` and `deepslate` variants, `building6`/`building7`):** abyssal brick (`brick.abyssal:0`) 3, fitted abyssal (`:1`) 2, abyssal block (`:2`) 1, infernal brick (`brick.infernal:0`) 2.
- **`quartzbricks_border` / `quartz` (light civic buildings):** bleached bone block (`brick.bleachedbone:2`) 3, quarried block (`brick.quarried:2`) 2, fitted bleached bone (`:1`) 1.
- **`X` accent and `$` band:** ornate quarried/bleached bone/abyssal (`:3`) as one-row bands; blood stained brick (`brick.bloodstained:0`) as the red-brick accent; abyssal block (`brick.abyssal:2`) as a dark band.
- **`}` rubble:** the cobble metas (`:5`) of the wall's theme, plus Railcraft slabs and stairs of the same theme once tile-entity placement exists; crushed obsidian sparingly (it falls).
- **Desert bricks (`bricks_desert*`):** sandy brick, fitted and block (`brick.sandy:0-2`), bleached bone brick for the light desert family, nether block (`brick.nether:2`) next to red nether bricks (`B`).
- **`glass_*`:** strengthened glass white, gray, light gray, light blue, blue, black (`Railcraft:glass:0,7,8,3,11,15`), 1 each beside the stained-glass baseline. Stacked columns read as tall framed windows.
- **Street and sidewalk `S`/`u`:** concrete (`Railcraft:cube:1`) 2, quarried block (`brick.quarried:2`) 1, beside smooth stone.
- **Pillars `v`:** quarried block, abyssal block, infernal block (`:2`), concrete.
- **Highway `R`:** concrete (`cube:1`) for a gray-highway variant; concrete wall (`wall.alpha:2`) as a barrier.
- **Light `h`:** iron, steel and stone lanterns (`lantern.metal:0,5`, `lantern.stone:9,6,0`) for street lights on a metal or wooden post (`post:2`, `post.metal:0,2`); bleached bone and sandy lanterns in deserts.
- **Iron bars `:` and fence `.`:** metal posts (`post:2`, `post.metal:0,2,7,8`), wooden post (`post:0`), and the brick and stone walls.
- **Furniture `F`:** steel anvil.
- **Oil rig / industrial style:** creosote block (`cube:8`) decks, steel and lead blocks, metal platforms, yellow metal posts, and (owner decision) tank walls and the water tower siding.

## Owner decisions

1. **Railcraft slabs and stairs need tile-entity NBT.** Use them (asset model gains a TE-material field) or rely on vanilla, Et Futurum and Chisel plain-metadata stairs and slabs only.
2. **Multiblock parts as set dressing.** The water tank siding (rooftop water towers), tank walls and gauges (tank farms) and the advanced coke oven brick are distinctive but are tile entities that try to form multiblocks.
3. **No-spawn floors.** With `cannotMobsSpawnOnDecorBlocks=true`, buildings floored in Railcraft bricks or cubes get no natural spawns. Either accept that, or keep Railcraft blocks off floors where upstream expects mobs.
