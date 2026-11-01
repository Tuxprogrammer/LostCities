# Block catalog: NewHorizonsCoreMod (dreamcraft)

GT New Horizons' core mod 2.9.99, mod ID `dreamcraft`, jar `GTNewHorizonsCoreMod-2.9.99.jar`. Source read at tag `2.9.99` in `gtnh-src/source/NewHorizonsCoreMod/`: every block comes from the `com.dreammaster.block.BlockList` enum, registered unconditionally by `BlockList.registerAll()` under its enum name (`dreamcraft:SteelBars`), plus the config-gated Baby Chest (`dreamcraft:BabyChest`, `config/GTNewHorizons/dreamcraft.cfg`, `modules.BabyChest=true`). English names from `assets/dreamcraft/lang/en_US.lang`. Every block is a single block with metadata 0. GT's machine casings that NHCore used to own now live in GT5-Unofficial (`gregtech:gt.blockcasingsNH`) and are catalogued with GregTech. Contact sheets: `tools/block-contact-sheet/sheets/dreamcraft/construction.json`, `bars.json`, `fuel-blocks.json`, `resource-blocks.json`. This catalog serves D25 in the [decision log](../../../design/decision-log.md): variety per palette role, with the [D19 mapping](../../00-pack-integration/block-mapping.md) as baseline.

## Summary

**50 block+meta entries from 50 registry blocks** (the Baby Chest, a tile-entity chest, is not catalogued). Fit with upstream's look: 3 yes, 17 partly, 30 no. NHCore is mostly a store of crafting-material blocks; its building value is the metal bars (a nine-step fence and window-grille family) and the first three plated reinforced stones.

| Role | Count |
|---|---|
| wall-accent | 8 |
| pane | 5 |
| rubble-ruin | 2 |
| fence-rail | 5 |
| industrial-dressing | 14 |
| off-theme | 30 |

Entries carry several roles, so the column sums to more than 50.

Flags:

- **Tile entities:** none (the Baby Chest, left out, has one).
- **Light:** none.
- **Gravity:** `ZincGravelOre` only (`BlockFalling`). The sand-material blocks (`SandClayMix`, `ManaPowder`, `PixieDust`) are plain blocks and do not fall.
- **Very hard to move:** the six plated reinforced stones have hardness 60 (bronze), 150 (steel), 200 (titanium), 250 (tungstensteel), 500 (naquadah) and 750 (neutronium), against obsidian's 50, with harvest levels 3 to 5 and explosion resistance above 300. A building made of them is effectively permanent; use them as accents, never as the main wall of a building meant to be moved into. The diamond frame box has hardness 25; the quintuple compressed fuel blocks up to 30.
- **Value:** the resource blocks (`Ledox`, `Mytryl`, `Quantinum`, `BlackPlutonium`, `MysteriousCrystalBlock`, `CallistoColdIce`) are compressed space-tier materials, the coal, charcoal and coke blocks are compressed fuel (a triple-compressed coal block is 729 coal), the diamond frame box holds diamonds, and the Blood Magic and Botania blocks are compressed magic materials. Placing any of them in quantity hands out late-game or bulk resources; keep them out of worldgen except, at most, single treasure blocks.
- **Tier (record 0004, nothing above EV in the overworld):** within EV: bronze, steel and titanium plated stone; steel, aluminium, stainless steel and titanium bars. Above EV: tungstensteel (IV), naquadah and neutronium plated stone; tungstensteel, iridium, osmium and neutronium bars. Chrome bars sit at the EV/IV boundary (chrome is processed at EV in GTNH) and need an owner call.
- **GT ore or boulder hosts:** none.

## Plated stone and construction blocks

Sheet `construction.json`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `dreamcraft:BronzePlatedReinforcedStone` | Bronze Plated Reinforced Stone | wall-accent, industrial-dressing | partly: riveted orange plate in a dark frame | industrial | Hardness 60, harvest level 4. Steam/LV-era material |
| `dreamcraft:SteelPlatedReinforcedStone` | Steel Plated Reinforced Stone | wall-accent, industrial-dressing | partly: riveted light-gray plate, reads as a steel-clad panel | industrial | Hardness 150, harvest level 3. LV |
| `dreamcraft:TitaniumPlatedReinforcedStone` | Titanium Plated Reinforced Stone | wall-accent, industrial-dressing | partly: lavender plate | industrial | Hardness 200, harvest level 4. EV |
| `dreamcraft:TungstensteelPlatedReinforcedStone` | Tungstensteel Plated Reinforced Stone | off-theme | no: above EV | industrial | Hardness 250. IV |
| `dreamcraft:NaquadahPlatedReinforcedStone` | Naquadah Plated Reinforced Stone | off-theme | no: above EV | industrial | Hardness 500, harvest level 5 |
| `dreamcraft:NeutroniumPlatedReinforcedStone` | Neutronium Plated Reinforced Stone | off-theme | no: above EV | industrial | Hardness 750, harvest level 5 |
| `dreamcraft:PistonBlock` | Piston Block | rubble-ruin, wall-accent | partly: rough gray stone with dark cracks, a cobble look | ruined, industrial | Hardness 0.5. A crafting ingredient for pistons |
| `dreamcraft:DiamondFrameBox` | Diamond Frame Box | off-theme | no: cyan see-through lattice, made of diamond | none | Not opaque, hardness 25. Valuable |
| `dreamcraft:CompressedGraphite` | Compressed Graphite | industrial-dressing, wall-accent | partly: black slatted block | industrial | Hardness 5 |
| `dreamcraft:SandClayMix` | Sand Clay Mix | off-theme | no: speckled light blue | none | Sand material, shovel, does not fall |
| `dreamcraft:ZincGravelOre` | Zinc Gravel Ore | off-theme | no: an ore | none | Falls (gravity). Ore |

## Metal bars

Sheet `bars.json`. All are panes (`BlockPane`, connecting like iron bars), hardness 5, blast resistance 10. The sheet draws the pane texture on a cube; in game they are thin bars like `minecraft:iron_bars`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `dreamcraft:SteelBars` | Steel Bars | fence-rail, pane | yes: a gray grille with a mesh, a drop-in for iron bars | urban, ruined, industrial | LV |
| `dreamcraft:AluminiumBars` | Aluminium Bars | fence-rail, pane | yes: light blue-gray vertical bars, like a modern railing | urban, industrial | MV |
| `dreamcraft:StainlessSteelBars` | Stainless Steel Bars | fence-rail, pane | yes: fine chain-link mesh, reads as a security fence | urban, industrial | HV |
| `dreamcraft:TitaniumBars` | Titanium Bars | fence-rail, pane | partly: violet chain pattern | industrial | EV |
| `dreamcraft:ChromeBars` | Chrome Bars | fence-rail, pane | partly: pale pink cross-braced grille | industrial | EV/IV boundary: owner decision |
| `dreamcraft:TungstenSteelBars` | Tungstensteel Bars | off-theme | no: above EV | industrial | IV |
| `dreamcraft:IridiumBars` | Iridium Bars | off-theme | no: above EV | industrial | LuV |
| `dreamcraft:OsmiumBars` | Osmium Bars | off-theme | no: above EV | industrial | LuV |
| `dreamcraft:NeutroniumBars` | Neutronium Bars | off-theme | no: above EV | industrial | UV and beyond |

## Charcoal, coal and coke blocks

Sheet `fuel-blocks.json`. The plain charcoal block is a dark red-brown solid; the compressed ones are the same fuel framed by one to five nested square borders, coal black-green, charcoal red-brown, coke steel-blue. Stone material, hardness 5 to 30. All are fuel and carry the value flag above.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `dreamcraft:Charcoal` | Block of Charcoal | industrial-dressing, rubble-ruin | partly: a dark burnt block for a fire-gutted ruin or a coal yard | ruined, industrial | Hardness 5. Fuel |
| `dreamcraft:CompressedCharcoal` | Block of Compressed Charcoal | industrial-dressing, wall-accent | partly: framed dark panel | industrial | Hardness 10. Fuel |
| `dreamcraft:CompressedCoal` | Block of Compressed Coal | industrial-dressing, wall-accent | partly | industrial | Hardness 10. Fuel |
| `dreamcraft:CompressedCoalCoke` | Block of Compressed Coal Coke | industrial-dressing, wall-accent | partly: framed steel-blue panel | industrial | Hardness 10. Fuel |
| `dreamcraft:DoubleCompressedCharcoal`, `DoubleCompressedCoal`, `DoubleCompressedCoalCoke` (3 entries) | Block of Double Compressed Charcoal, Coal, Coal Coke | industrial-dressing | partly | industrial | Hardness 15. Fuel |
| `dreamcraft:TripleCompressedCharcoal`, `TripleCompressedCoal`, `TripleCompressedCoalCoke` (3 entries) | Block of Triple Compressed Charcoal, Coal, Coal Coke | industrial-dressing | partly | industrial | Hardness 20, harvest level 2. Fuel |
| `dreamcraft:QuadrupleCompressedCharcoal`, `QuadrupleCompressedCoal`, `QuadrupleCompressedCoalCoke` (3 entries) | Block of Quadruple Compressed Charcoal, Coal, Coal Coke | off-theme | no: bulk fuel value | industrial | Hardness 25, harvest level 3 |
| `dreamcraft:QuintupleCompressedCharcoal`, `QuintupleCompressedCoal`, `QuintupleCompressedCoalCoke` (3 entries) | Block of Quintuple Compressed Charcoal, Coal, Coal Coke | off-theme | no: bulk fuel value | industrial | Hardness 30, harvest level 2 to 3 |

## Material and magic resource blocks

Sheet `resource-blocks.json`. Compressed materials; none belongs in a city except as a rare treasure block.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `dreamcraft:Ledox` | Block of Ledox | off-theme | no: bright blue metal, space-tier material | none | Hardness 50, harvest level 3 |
| `dreamcraft:Mytryl` | Block of Mytryl | off-theme | no: orange metal | none | Hardness 30 |
| `dreamcraft:Quantinum` | Block of Quantium | off-theme | no: green metal | none | Hardness 50, harvest level 3 |
| `dreamcraft:BlackPlutonium` | Block of Black Plutonium | off-theme | no: late-game material | none | Hardness 50, harvest level 3 |
| `dreamcraft:Sulfur` | Block of Sulfur | off-theme | no: yellow crystal | none | Glass material, hardness 25 |
| `dreamcraft:CallistoColdIce` | Callisto Cold Ice Block | off-theme | no | none | Glass material, hardness 25 |
| `dreamcraft:MysteriousCrystalBlock` | Mysterious Crystal Block | off-theme | no | none | Hardness 50, harvest level 3 |
| `dreamcraft:BloodyThaumium` | Blood-Soaked Thaumium Block | off-theme | no | none | Blood Magic altar block |
| `dreamcraft:BloodyVoid` | Blood-Soaked Void Block | off-theme | no | none | Blood Magic altar block |
| `dreamcraft:BloodyIchorium` | Blood-Soaked Ichorium Block | off-theme | no | none | Blood Magic altar block, hardness 120 |
| `dreamcraft:Gaia` | Gaia Spirit Block | off-theme | no: animated rainbow | none | Harvest level 6 |
| `dreamcraft:ManaPowder` | Mana Powder Block | off-theme | no | none | Sand material, does not fall |
| `dreamcraft:ManaPearl` | Mana Pearl Block | off-theme | no | none | Glass material |
| `dreamcraft:PixieDust` | Pixie Dust Block | off-theme | no: magenta | none | Sand material, does not fall |

## Best variety pools

| Upstream role or palette family | NHCore candidates |
|---|---|
| Iron bars `:` (also the `iron_bars` "damaged to" block) | `SteelBars`, `AluminiumBars`, `StainlessSteelBars` (urban); add `TitaniumBars` in an industrial style |
| Fence `.` and railings | `AluminiumBars`, `StainlessSteelBars` (chain-link look) |
| `X` accent in an industrial style | `SteelPlatedReinforcedStone`, `BronzePlatedReinforcedStone`, `CompressedGraphite` (accents only: very hard) |
| `}` rubble, ruined variants | `PistonBlock`, `Charcoal` (burnt-out ruins, sparingly) |
| Oil rig | `SteelBars` and `StainlessSteelBars` for guard rails |
| Treasure `!` | none recommended; if the owner wants pack-flavored treasure, single `CompressedCoalCoke` or `CompressedCoal` blocks are the cheapest |
