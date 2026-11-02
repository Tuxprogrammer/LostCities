# Block catalog coverage

Which of GTNH 2.9.0's 243 mods register placeable blocks, and which catalog family covers each one (decision log D25). The catalog judges every decorative and construction block in the pack against upstream's look, so palettes can draw weighted variety for each role. The D19 mapping in [block-mapping.md](../../00-pack-integration/block-mapping.md) stays the baseline and the fallback.

Status: *Proposed* (catalog work for D25). The rows below were counted on 2026-11-01.

## Families

| Family | Mods | Scope |
|---|---|---|
| A | 1 | Chisel |
| B | 5 | ChiselTones, Ztones, Et Futurum Requiem, Botania, Thaumcraft, plus vanilla 1.7.10 (not a pack mod, so not in the table) |
| C | 11 | GT5-Unofficial (with bartworks, GT++ and its other bundled mod IDs), IndustrialCraft 2, Railcraft, NewHorizonsCoreMod, ProjectRed, Applied Energistics 2, EnderIO, Tinkers' Construct, Extra Utilities, Galacticraft, GalaxySpace |
| D | 99 | Every other mod that registers placeable blocks: 45 have catalog rows (32 files below), 54 were checked and have nothing useful |
| none | 127 | No placeable blocks: libraries, client-only mods, tweaks, and debug or test blocks that the pack's config leaves off |

All 243 mods are assigned. No mod with placeable blocks is left without a family. Two edge cases:

- **Amun-Ra** is a Galacticraft add-on but a separate mod. Family C covers only Galacticraft and GalaxySpace, so Amun-Ra is in D ([amunra.md](amunra.md)).
- **ChiselTones registers no blocks of its own.** It adds Ztones' blocks to Chisel's carving groups (`mods/chiseltones/tehnut/chiseltones/ChiselTones.java` L30-L44), so its 0 counts are correct. Family B lists it.

## How the counts were made

- **Block textures:** PNG files under `assets/<domain>/textures/block/` or `textures/blocks/` in the mod's jar in `reference/gtnh/2.9.0/.minecraft/mods/`. This shows how much block art a mod ships. It is not a block count: some textures belong to other mods' domains (Et Futurum ships `minecraft:` overrides), and some mods keep model textures elsewhere (BiblioCraft, CookingForBlockheads).
- **registerBlock sites:** occurrences of `registerBlock(` in the mod's source in `gtnh-src/mods/<dir>/`. These are call sites, not blocks. A loop registers many blocks from one site (GT, Natura, HarvestCraft), and annotation-based registrars show 0 (OpenBlocks through OpenModsLib, VendingMachine).
- **Family D's registry-name and "useful" counts** in the verdict column come from each catalog file, where the source was read block by block. Counts with `~` are estimates.

## Catalog files (family D)

| File | Mods | Sheets in `tools/block-contact-sheet/sheets/` |
|---|---|---|
| [biomesoplenty.md](biomesoplenty.md) | Biomes O' Plenty | `biomesoplenty/` (5) |
| [natura.md](natura.md) | Natura | `natura/` (3) |
| [forestry.md](forestry.md) | Forestry | `forestry/` (3) |
| [binnie.md](binnie.md) | ExtraTrees, Botany, ExtraBees, Genetics, BinnieCore | `binnie/` (4) |
| [harvestcraft.md](harvestcraft.md) | Pam's HarvestCraft | `harvestcraft/` (1) |
| [bibliocraft.md](bibliocraft.md) | BiblioCraft and the three BiblioWoods editions | `bibliocraft/` (1) |
| [carpentersblocks.md](carpentersblocks.md) | Carpenter's Blocks | `carpentersblocks/` (1) |
| [cookingforblockheads.md](cookingforblockheads.md) | CookingForBlockheads | `cookingforblockheads/` (1) |
| [storagedrawers.md](storagedrawers.md) | Storage Drawers and its wood packs | `storagedrawers/` (3) |
| [ironchest.md](ironchest.md), [jabba.md](jabba.md) | Iron Chests, JABBA | `ironchest/`, `jabba/`; also `irontanks/` |
| [malisisdoors.md](malisisdoors.md) | MalisisDoors | `malisisdoors/` (1) |
| [catwalks.md](catwalks.md) | Catwalks 2 | `catwalks/` (1) |
| [modernmarkings.md](modernmarkings.md) | ModernMarkings | `modernmarkings/` (3) |
| [floodlights.md](floodlights.md) | FloodLights | `floodlights/` (1) |
| [architecturecraft.md](architecturecraft.md) | ArchitectureCraft | none: shapes take their texture from a material named in NBT |
| [littletiles.md](littletiles.md) | LittleTiles | `littletiles/` (1) |
| [villagenames.md](villagenames.md) | VillageNames | `villagenames/` (1) |
| [vendingmachine.md](vendingmachine.md) | VendingMachine | `vendingmachine/` (1) |
| [battlegear2.md](battlegear2.md) | Battlegear 2 | `battlegear2/` (1) |
| [openblocks.md](openblocks.md) | OpenBlocks | `openblocks/` (1) |
| [opencomputers.md](opencomputers.md) | OpenComputers, Computronics, OpenSecurity, OpenPrinter, OpenGlasses | `opencomputers/` (1) |
| [openmodularturrets.md](openmodularturrets.md) | OpenModularTurrets | `openmodularturrets/` (1) |
| [nuclearcontrol.md](nuclearcontrol.md) | Nuclear Control | `nuclearcontrol/` (1) |
| [amunra.md](amunra.md) | Amun-Ra | `amunra/` (1) |
| [buildcraft.md](buildcraft.md) | BuildCraft | `buildcraft/` (1) |
| [fether.md](fether.md) | Fether | `fether/` (1) |
| [twilightforest.md](twilightforest.md) | Twilight Forest | `twilightforest/` (2) |
| [witchery.md](witchery.md) | Witchery | `witchery/` (1) |
| [hardcoreenderexpansion.md](hardcoreenderexpansion.md) | Hardcore Ender Expansion | `hardcoreenderexpansion/` (1) |
| [bloodmagic.md](bloodmagic.md) | Blood Magic, Blood Arsenal | `bloodmagic/` (1) |
| [thaumic-addons.md](thaumic-addons.md) | Every Thaumcraft add-on: Thaumic Bases, Thaumic Tinkerer, Salis Arcana, Automagy, Thaumic Insurgence, Thaumic Horizons, Thaumic Exploration, Witching Gadgets, Forbidden Magic, Tainted Magic, and one-line verdicts for the rest | `thaumic-addons/` (2), `thaumicbases/`, `thaumictinkerer/`, `forbiddenmagic/`, `taintedmagic/`, `thaumichorizons/`, `thaumcraft-addons/` (1 each) |

Sheets kept for reference without a catalog file: `adventurebackpack/`, `forgerelocation/`, `irontanks/`, `lootgames/`, `solarpanels/` (Advanced Solar Panel and supersolarpanels). Render any sheet with `python3 -I tools/block-contact-sheet/contact_sheet.py <sheet.json> --mods <pack mods dir> --out docs/previews/blocks/<mod>/<group>.png` ([README](../../../../tools/block-contact-sheet/README.md)).

## Family D: best additions

The 15 additions from family D that add the most variety to upstream's urban look, in rough order of value. Details, metadata and caveats are in each file.

| # | Blocks | Role | Why | Main caveat |
|---|---|---|---|---|
| 1 | ModernMarkings floor markings: lane lines, crossings, arrows, parking squares, hazard stripes, `EXIT`, `NO STEP` ([modernmarkings.md](modernmarkings.md)) | street, floor | Street paint upstream never had. Plain blocks with no tile entity, 7 colors | Flat overlays with no collision; they pop off without a solid block under them |
| 2 | ModernMarkings wall signs: exit, first aid, fire extinguisher, hazard symbols, arrows, letters and digits | furniture-deco | Cheap signage for offices, stairwells and garages | Placed by side; the GT voltage signs are off-theme |
| 3 | Botany ceramic bricks and tiles (`Botany:ceramicBrick`, [binnie.md](binnie.md)) | wall-main, wall-accent, floor | Running-bond, large, striped and vertical bricks, square tiles and checkerboards in two of 36 colors: about ten brick colors vanilla lacks, plus bathroom tile and diner checker floors | Every block is a tile entity holding the design (`meta` NBT); use for accents and a minority of buildings |
| 4 | Storage Drawers trims (`StorageDrawers:trim` and the pack trims, [storagedrawers.md](storagedrawers.md)) | trim-pillar, wall-accent | 83 framed-plank blocks with no tile entity: wainscoting, paneled walls, door surrounds and columns in every wood in the pack | The Misc pack also registers woods for mods the pack lacks |
| 5 | CookingForBlockheads kitchen floors and kitchen furniture ([cookingforblockheads.md](cookingforblockheads.md)) | floor, furniture-deco | 16 checkerboard floors with no tile entity (diners, bathrooms, shops); counters, fridges, ovens and sinks with facing in metadata | The furniture blocks have tile entities with inventories |
| 6 | BiblioCraft furniture with the BiblioWoods editions ([bibliocraft.md](bibliocraft.md)) | furniture-deco, light | Bookcases, desks, chairs, tables, display cases, typewriters, clocks, lamps and lanterns (light 15, no power) in about 60 woods: the strongest lived-in interior set in the pack | Facing and contents live in TE NBT; the models are costly to render; a "filled" bookcase starts empty under worldgen |
| 7 | Catwalks 2 catwalks, caged ladders, scaffolds and support columns ([catwalks.md](catwalks.md)) | fence-rail, special, light | Fire escapes, rooftop walkways, factory mezzanines. No tile entity; lit variants give light 12 | Catwalk metadata encodes which sides are open |
| 8 | MalisisDoors doors, hatches and curtains ([malisisdoors.md](malisisdoors.md)) | door-trap, furniture-deco | Glass sliding doors, garage, jail, lab and factory doors, a rusty ladder, 16 curtain colors, an iron trapdoor and wood-variant doors, which D19 found missing | Every door is a tile entity, and the mod replaces the vanilla doors, trapdoor and fence gate (`modifyVanillaDoors=true`) |
| 9 | Natura doors, bookshelves, workbenches, trapdoors, fences and gates ([natura.md](natura.md)) | door-trap, furniture-deco, fence-rail | Seven wood doors, 13 bookshelves and 13 workbenches with no tile entity: variety for upstream's `L` and `F` characters at no cost | Natura's soul glass has no collision and its heat glass burns; neither is a window |
| 10 | Hardcore Ender Expansion ravaged brick ([hardcoreenderexpansion.md](hardcoreenderexpansion.md)) | wall-main, rubble-ruin, stairs-slab, fence-rail | A complete ruined-brick kit: plain, cracked and four damaged states, smooth, slab, stairs and fence | Crimson; keep it to a minority of brick buildings |
| 11 | OpenModularTurrets hard walls tiers 1-3 and fences tiers 1-2 ([openmodularturrets.md](openmodularturrets.md)) | wall-main, wall-accent, fence-rail | Plain and riveted steel cladding and chain-link mesh with no tile entity: the best industrial set in family D | Tier 3-5 fences damage on contact; tier 4-5 walls are very hard |
| 12 | OpenComputers cases, screens, keyboards and racks, OpenPrinter's file cabinet, printer and shredder ([opencomputers.md](opencomputers.md)) | furniture-deco | Convincing offices and server rooms. Empty, unpowered default tile entities already look right in a dead city | Tile entities; facing NBT keys not yet verified |
| 13 | Biomes O' Plenty polished shale and siltstone, mud bricks ([biomesoplenty.md](biomesoplenty.md)) | wall-main, floor, sidewalk | Dark charcoal and buff dressed stone that read as concrete; a full mud-brick set for poorer districts and ruins | None found; no gravity, no tile entity |
| 14 | ArchitectureCraft shapes ([architecturecraft.md](architecturecraft.md)) | trim-pillar, roof, special | Cornices, balustrades, columns with capitals, mullions, arches and true roof slopes in any material | Shape and material live in TE NBT; palettes need a new "shape of material" entry kind first |
| 15 | HarvestCraft colored candles (`harvestcraft:pamcandleDeco1`-`16`, [harvestcraft.md](harvestcraft.md)) | light | Light 14 with no tile entity in 16 colors: a cheap interior light | Small; reads as a candle, not a lamp |

Close runners-up: Twilight Forest underbrick and its wood doors and trapdoors; Thaumic Bases' beta-era old bricks and cobblestone; Thaumic Tinkerer's smokey quartz set; Amun-Ra's weathered concrete (Urbolite) and basalt bricks; Forestry's fireproof dark woods and ExtraTrees' wood-variant doors (both tile entities); Computronics' colorful lamp and Nuclear Control's panel lamps (the lamps go dark without power); Witchery's shaded glass; OpenComputers' chamelium (16 colors, no tile entity).

## Cross-cutting findings

- **Registry names with `tile.`.** Several mods register a block under its unlocalized name, which `Block.getUnlocalizedName()` prefixes with `tile.` (`minecraft/net/minecraft/block/Block.java` L1594-L1596). ModernMarkings (`modernmarkings:tile.floor_marking_white_lane`), Twilight Forest (`TwilightForest:tile.TFPlank`), Amun-Ra (`GalacticraftAmunRa:tile.baseBlockRock`), Thaumic Insurgence and, *unverified*, VendingMachine's casing do this. Some Natura names contain spaces or dots (`Natura:Rare Tree`, `Natura:Natura.fence`), and four Blood Magic names are concatenated (`AWWayofTime:AlchemicalWizardrybloodRune`). The block-name map must keep every name verbatim.
- **MalisisDoors changes the D19 baseline.** With `config.modifyVanillaDoors=true` (`config/malisisdoors.cfg` L16) it replaces `minecraft:wooden_door`, `iron_door`, `trapdoor` and `fence_gate` with tile-entity versions, so upstream's doors gain tile entities in this pack. It also supplies the wood-variant doors and the iron trapdoor that D19 found missing.
- **Wood is cheap or expensive by mod.** Natura, Biomes O' Plenty, Twilight Forest, Witchery and the Storage Drawers trims store wood type in metadata. Every Forestry wood block (planks, logs, stairs, slabs and fences, fireproof included) and every decorative Binnie block is a tile entity, with the type in NBT (`WT` for Forestry, `meta` for Binnie). A Forestry block without its NBT turns into larch, and one without its tile entity drops nothing. Prefer the metadata woods for walls and floors.
- **Furniture is tile entities.** BiblioCraft, Carpenter's Blocks, ArchitectureCraft, MalisisDoors, the OpenComputers family, Iron Chests and JABBA all need tile entities, mostly with facing or material in NBT. Natura's bookshelves and workbenches, HarvestCraft's candles, CookingForBlockheads' floors and the Catwalks blocks do not. The furniture files list, per block, what worldgen must write.
- **Hazards to keep out of cities:** OpenSecurity's security door (hardness 400, cannot be opened by hand), OpenModularTurrets' tier 3-5 fences (contact damage) and turrets, OpenBlocks elevators (same-colored blocks in a column become a lift) and its scaffolding (decays on random ticks), Natura's fusewood logs (explode when harvested) and heat glass, Witchery's brambles (spread or teleport), Blood Magic's spectral block (vanishes), Thaumic Tinkerer's travel slabs (speed boost), Twilight Forest's infested towerwood (termite, the same rule as no silverfish) and its hardness-100 castle brick, mazestone and deadrock, Biomes O' Plenty's quicksand, poison ivy, thorns and similar blocks, and LootGames' unbreakable dungeon floor.
- **Ore and boulder hosts.** No family D block was found to be a GT ore host or boulder host. Thaumic Bases' old cobblestone is ore-dictionaried as cobblestone but is a separate block, and GT's boulders test block identity against `minecraft:cobblestone` (`WorldgenStone.java` L223-L229 with the vanilla `isReplaceableOreGen`), so they ignore it.
- **Absent mods.** Every family D block is a pack mod's block, so each palette entry needs a vanilla fallback per D19. Each file names one.

## All mods

`Block textures` and `registerBlock sites` are defined in [How the counts were made](#how-the-counts-were-made). An empty cell means the jar or its source was not available to count (JourneyMap Server is not in the client `mods/` folder; skipped jars have no source tree).

| Mod | Jar | Family | Block textures | registerBlock sites | Verdict |
|---|---|---|---|---|---|
| Chisel | `chisel-2.17.34-GTNH.jar` | A | 3589 | 37 | Family A (Chisel) |
| Botania | `Botania-1.13.42-GTNH.jar` | B | 575 | 42 | Family B |
| ChiselTones | `chiseltones-1.2.0-GTNH.jar` | B | 0 | 0 | Family B |
| Et-Futurum-Requiem | `etfuturum-2.6.65-GTNH.jar` | B | 785 | 2 | Family B |
| Thaumcraft | `Thaumcraft-1.7.10-4.2.3.5.jar` | B | 248 | 45 | Family B |
| Ztones | `Ztones-1.7.10-2.2.2.jar` | B | 552 | 42 | Family B |
| Applied-Energistics-2-Unofficial | `appliedenergistics2-rv3-beta-1095-GTNH.jar` | C | 621 | 13 | Family C |
| EnderIO | `EnderIO-2.10.51.jar` | C | 396 | 25 | Family C |
| Extra Utilities | `extrautilities-1.2.12.jar` | C | 329 | 72 | Family C |
| Galacticraft | `Galacticraft-3.4.37-GTNH.jar` | C | 268 | 74 | Family C |
| Galaxy-Space-GTNH | `GalaxySpace-1.1.144-GTNH.jar` | C | 226 | 71 | Family C |
| GT5-Unofficial | `gregtech-5.09.54.265.jar` | C | 4008 | 138 | Family C |
| Industrial Craft 2 | `industrialcraft-2-2.2.828-experimental.jar` | C | 423 | 5 | Family C |
| NewHorizonsCoreMod | `GTNewHorizonsCoreMod-2.9.99.jar` | C | 56 | 4 | Family C |
| ProjectRed | `ProjRed-4.12.50-GTNH.jar` | C | 600 | 3 | Family C |
| Railcraft | `Railcraft-9.17.36.jar` | C | 238 | 32 | Family C |
| TinkersConstruct | `TConstruct-1.14.123-GTNH.jar` | C | 735 | 62 | Family C |
| Advanced Solar Panel For 1.7.10 (Unofficial) | `AdvancedSolarPanel-1.7.10-3.5.1.jar` | D | 15 | 2 | Nothing useful: Ticking generators; free high-tier loot against the EV ceiling; only meta 0 is arguable (sheet `solarpanels/`) |
| AdventureBackpack2 | `adventurebackpack-1.4.28-GTNH.jar` | D | 14 | 3 | Nothing useful: Placed backpack, sleeping bag, campfire (TEs); the campfire is a marginal ruin prop (sheet `adventurebackpack/`) |
| AE2FluidCraft-Rework | `ae2fc-1.5.116-gtnh.jar` | D | 106 | 12 | Nothing useful: AE2 fluid machines |
| AFSU | `AFSU-1.3.2-GTNH.jar` | D | 27 | 1 | Nothing useful: One energy-storage machine |
| AlchemyGrate | `alchgrate-1.3.2-GTNH.jar` | D | 1 | 1 | Nothing useful: One machine block |
| Amazing-Trophies | `amazingtrophies-1.4.10.jar` | D | 1 | 1 | Nothing useful: Trophy pedestal TE that needs a trophy ID |
| amunra | `AmunRa-GC-0.8.14.jar` | D | 77 | 10 | [amunra.md](amunra.md): ~43 registry names, 14 useful. Basalt bricks, smooth basalt, weathered concrete, aluminum box; `tile.` names |
| ArchitectureCraft | `ArchitectureCraft-1.12.19.jar` | D | 2 | 5 | [architecturecraft.md](architecturecraft.md): 3 registry names, ~70 shapes useful. Cornices, balustrades, columns, arches, roof slopes; shape and material in TE NBT |
| Automagy-GTNH | `Automagy-0.29.8-GTNH.jar` | D | 95 | 13 | [thaumic-addons.md](thaumic-addons.md): 53 registry names, 6 rows (3 yes, 1 partly). Glowing amber (light 15) and two bookshelves |
| Avaritia | `Avaritia-1.100.jar` | D | 24 | 9 | Nothing useful: Machines and compressed blocks |
| Avaritiaddons | `Avaritiaddons-1.9.6-GTNH.jar` | D | 3 | 3 | Nothing useful: Chests and a crafter |
| Battlegear2-for-Backhand | `battlegear2-1.6.8-backhand.jar` | D | 0 | 1 | [battlegear2.md](battlegear2.md): 1 registry names, 1-7 useful. Flag pole in 7 woods and metals |
| BetterQuesting | `BetterQuesting-3.8.92-GTNH.jar` | D | 5 | 6 | Nothing useful: Quest stations |
| BiblioCraft | `BiblioCraft[v1.11.7][MC1.7.10].jar` | D | 1 | 34 | [bibliocraft.md](bibliocraft.md): 34 registry names, ~25 types useful. The furniture set: bookcases, desks, chairs, tables, cases, lamps; TE facing and contents |
| BiblioCraft: BiblioWoods Biomes O'Plenty Edition | `BiblioWoods[BiomesOPlenty][v1.9].jar` | D | 0 | 18 | [bibliocraft.md](bibliocraft.md): 18 registry names, wood variety useful. BiblioCraft furniture in 14 BoP woods |
| BiblioCraft: BiblioWoods Forestry Edition | `BiblioWoods[Forestry][v1.7].jar` | D | 0 | 36 | [bibliocraft.md](bibliocraft.md): 36 registry names, wood variety useful. BiblioCraft furniture in 29 Forestry woods |
| BiblioCraft: BiblioWoods Natura Edition | `BiblioWoods[Natura][v1.5].jar` | D | 0 | 18 | [bibliocraft.md](bibliocraft.md): 18 registry names, wood variety useful. BiblioCraft furniture in 13 Natura woods |
| Binnie | `binnie-mods-2.6.51.jar` | D | 2149 | 27 | [binnie.md](binnie.md): 31 registry names, ~200 useful. Botany ceramic bricks and tiles (36 colors), ExtraTrees doors and stained glass; all tile entities |
| Biomes O' Plenty | `BiomesOPlenty-1.7.10-2.1.0.2308-universal.jar` | D | 405 | 85 | [biomesoplenty.md](biomesoplenty.md): 83 registry names, ~140 useful. Woods (9 new plank tones), polished shale and siltstone, mud bricks, overgrowth and flowers |
| BloodArsenal | `BloodArsenal-1.5.12.jar` | D | 32 | 23 | [bloodmagic.md](bloodmagic.md): 18 registry names, 2 useful. Partly fitting accents |
| BloodMagic | `BloodMagic-1.9.16.jar` | D | 58 | 39 | [bloodmagic.md](bloodmagic.md): 39 registry names, 3 useful. Bloodstone bricks, partly fitting |
| Botanic-horizons | `BotanicHorizons-1.12.10-GTNH.jar` | D | 6 | 1 | Nothing useful: Multiblock mana pools and a portal |
| BuildCraft | `buildcraft-7.1.65.jar` | D | 277 | 37 | [buildcraft.md](buildcraft.md): ~32 registry names, 2 useful. Marginal: tanks and frames as industrial clutter |
| BuildCraftCompat | `buildcraft-compat-7.1.22.jar` | D | 8 | 0 | Nothing useful: No decorative blocks of its own |
| CarpentersBlocks | `CarpentersBlocks-3.7.3-GTNH.jar` | D | 81 | 18 | [carpentersblocks.md](carpentersblocks.md): 18 registry names, ~8 useful. Shapes, doors, beds, flower pots; cover block in TE NBT (writable by name) |
| Catwalks-2 | `catwalks-2.4.2-GTNH.jar` | D | 63 | 8 | [catwalks.md](catwalks.md): 46 registry names, ~44 useful. Catwalks, caged ladders, scaffolds, support columns; no TE; lit variants |
| Compact Kinetic Generators | `CompactKineticGenerators-1.7.10-1.0.jar` | D | 22 | 1 | Nothing useful: Machines |
| Computronics | `Computronics-1.9.13-GTNH.jar` | D | 56 | 7 | [opencomputers.md](opencomputers.md): 16 registry names, 3 useful. Colorful lamp (light 15, color in TE NBT `clc`) |
| CookingForBlockheads | `CookingForBlockheads-1.4.14-GTNH.jar` | D | 19 | 11 | [cookingforblockheads.md](cookingforblockheads.md): 26 registry names, 25 useful. 16 checkerboard kitchen floors (no TE), counters, fridges, ovens, sinks |
| CropsNH | `cropsnh-2.0.140.jar` | D | 722 | 38 | Nothing useful: Crop sticks are genetics TEs; vanilla or HarvestCraft crops do the job more simply |
| Draconic-Evolution | `Draconic-Evolution-1.5.35-GTNH.jar` | D | 79 | 4 | Nothing useful: Machines, portals, draconium |
| Electro-Magic-Tools | `EMT-1.7.25.jar` | D | 74 | 7 | Nothing useful: Machines and solar panels |
| EnderStorage | `EnderStorage-1.8.9.jar` | D | 0 | 1 | Nothing useful: Ender chest and tank only |
| EnderZoo | `EnderZoo-1.3.6.jar` | D | 13 | 1 | Nothing useful: Explosive charges |
| Fether | `fether-2.0.9.jar` | D | 55 | 12 | [fether.md](fether.md): 12 registry names, 3 useful. Harvest the Nether port: crimson wood, glow flower |
| FloodLights | `FloodLights-1.5.6.jar` | D | 19 | 7 | [floodlights.md](floodlights.md): 7 registry names, 3 useful. Unpowered fixtures as dead ceiling panels only |
| ForbiddenMagic | `Forbidden.Magic-0.9.18-GTNH.jar` | D | 25 | 10 | Nothing useful: Purple tainted wood and stone, a wrath-cage spawner |
| ForestryMC | `Forestry-4.11.40.jar` | D | 337 | 36 | [forestry.md](forestry.md): 33 registry names, ~150 useful. 29 woods incl. fireproof; every wood block is a tile entity (`WT` NBT); mailbox |
| ForgeMultipart | `ForgeMultipart-1.7.18.jar` | D | 0 | 1 | Nothing useful: One microblock container block; contents in NBT, too complex for palettes |
| ForgeRelocation | `ForgeRelocation-0.3.7.jar` | D | 6 | 0 | Nothing useful: Wooden support frame and a motor; marginal (sheet `forgerelocation/`) |
| Gadomancy | `gadomancy-1.5.17.jar` | D | 12 | 19 | Nothing useful: Machines |
| gendustry | `gendustry-1.9.13-GTNH.jar` | D | 39 | 1 | Nothing useful: Bee machines |
| Gravitation-Suite-old | `GraviSuite-1.7.10-2.0.3.jar` | D | 1 | 1 | Nothing useful: One portal block |
| Hardcore-Ender-Expansion | `HardcoreEnderExpansion-1.12.29-GTNH.jar` | D | 138 | 5 | [hardcoreenderexpansion.md](hardcoreenderexpansion.md): 49 registry names, 16 useful. Ravaged brick: a full ruined-brick kit with damaged states, slab, stairs, fence |
| harvestcraft | `harvestcraft-1.3.15-GTNH.jar` | D | 525 | 188 | [harvestcraft.md](harvestcraft.md): 187 registry names, ~45 useful. 16 colored candles (light 14, no TE), kitchen and shop dressing, wild gardens |
| HydroEnergy | `hydroenergy-1.4.26.jar` | D | 7 | 1 | Nothing useful: Water blocks for its dam only |
| IguanaTweaksTConstruct | `IguanaTweaksTConstruct-2.7.15.jar` | D | 0 | 1 | Nothing useful: Mob-head skull TE |
| ironchest | `IronChest-6.1.13.jar` | D | 30 | 1 | [ironchest.md](ironchest.md): 1 registry names, 4 useful. Metal chests as storeroom dressing |
| Irontanks | `irontanks-1.4.7.jar` | D | 30 | 10 | Nothing useful: Tank TEs; marginal industrial props (sheet `irontanks/`) |
| Jabba | `JABBA-1.5.24.jar` | D | 27 | 3 | [jabba.md](jabba.md): 1 registry names, 1 useful. Barrel as storeroom dressing |
| LittleTiles | `littletiles-1.6.63.jar` | D | 11 | 4 | [littletiles.md](littletiles.md): 3 registry names, 10 useful. Colored plaster, tile and brick blocks with no hardness |
| LogisticsPipes | `logisticspipes-1.5.39-GTNH.jar` | D | 210 | 2 | Nothing useful: Pipes and machines |
| LootGames | `lootgames-2.2.14.jar` | D | 19 | 5 | Nothing useful: Dungeon bricks need a level-4 pickaxe, shielded floor is unbreakable, the rest are puzzle TEs (sheet `lootgames/`) |
| MagicBees | `magicbees-2.10.13-GTNH.jar` | D | 26 | 7 | Nothing useful: Hives, effect jar, bee machines |
| MalisisDoors | `malisisdoors-1.19.13-GTNH.jar` | D | 162 | 16 | [malisisdoors.md](malisisdoors.md): ~56 registry names, ~40 useful. Glass sliding, jail, lab, factory, garage doors; curtains; iron trapdoor; replaces vanilla doors |
| ModernMarkings | `modernmarkings-0.3.13-1.7.10.jar` | D | 147 | 1 | [modernmarkings.md](modernmarkings.md): 170 registry names, ~150 useful. Road and floor markings, wall signs, letters and digits; no TE |
| Natura | `Natura-2.8.24.jar` | D | 371 | 137 | [natura.md](natura.md): 117 registry names, ~185 useful. 7 wood doors, 13 bookshelves, workbenches, trapdoors, fences, gates; topiary grass |
| NotEnoughEnergistics | `NotEnoughEnergistics-1.7.50.jar` | D | 1 | 1 | Nothing useful: One machine |
| Nuclear-Control | `IC2NuclearControl-2.7.17.jar` | D | 528 | 4 | [nuclearcontrol.md](nuclearcontrol.md): 3 registry names, ~7 useful. White and orange panel lamps (no TE), alarms, panels |
| OCGlasses | `OpenGlasses-1.6.9-GTNH.jar` | D | 3 | 1 | Nothing useful: One machine |
| OpenBlocks | `OpenBlocks-1.12.22-GTNH.jar` | D | 127 | 0 | [openblocks.md](openblocks.md): 36 registry names, ~7 useful. Canvas, elevator colors, ladder, flag, guide; scaffolding decays |
| OpenComputers | `OpenComputers-1.12.68-GTNH.jar` | D | 145 | 9 | [opencomputers.md](opencomputers.md): 38 registry names, 9 useful. Cases, screens, keyboards, racks as office and server-room dressing; chamelium |
| OpenModularTurrets | `OpenModularTurrets-2.4.8.jar` | D | 41 | 36 | [openmodularturrets.md](openmodularturrets.md): 36 registry names, 6 useful. Steel hard walls tiers 1-3, chain-link fences tiers 1-2; no TE |
| OpenPrinter | `openprinter-0.3.1-GTNH.jar` | D | 6 | 3 | [opencomputers.md](opencomputers.md): 3 registry names, 3 useful. File cabinet, printer, shredder |
| OpenSecurity | `opensecurity-1.2.2-GTNH.jar` | D | 26 | 15 | [opencomputers.md](opencomputers.md): 14 registry names, 2 useful. Office dressing; the security door seals rooms |
| PersonalSpace | `personalspace-1.0.41.jar` | D | 0 | 4 | Nothing useful: Portal blocks |
| ProjectBlue | `ProjectBlue-1.2.11-GTNH.jar` | D | 14 | 1 | Nothing useful: Integration blocks only; probably none registered without MineFactory Reloaded (unverified) |
| Random-Things | `RandomThings-2.7.13.jar` | D | 46 | 7 | Nothing useful: Mostly machines; its lapis lamp lights only on the client, so mobs still spawn |
| RemoteIO | `RemoteIO-2.7.11.jar` | D | 32 | 6 | Nothing useful: Machines; the skylight is transparent only when powered |
| Roguelike-Dungeons | `roguelike-1.6.6-GTNH.jar` | D | 0 | 0 | Nothing useful: Registers no blocks (places vanilla blocks); checked |
| Salis Arcana | `salisarcana-v2.7.0.jar` | D | 0 | 1 | [thaumic-addons.md](thaumic-addons.md): 1 registry name, 2 rows (1 yes, 1 partly). Greatwood and silverwood planks |
| SC2 | `StevesCarts-2.3.15.jar` | D | 82 | 1 | Nothing useful: Cart machines, rails, metal storage blocks |
| SGCraft | `SGCraft-1.4.14-GTNH.jar` | D | 26 | 1 | Nothing useful: Stargate and naquadah blocks |
| SleepingBags | `sleepingbag-0.3.1.jar` | D | 6 | 1 | Nothing useful: Temporary sleeping-bag block |
| Steve-s-Factory-Manager | `StevesFactoryManager-1.3.13-GTNH.jar` | D | 28 | 12 | Nothing useful: Cables and the manager |
| StevesAddons | `StevesAddons-0.15.5.jar` | D | 5 | 2 | Nothing useful: Cables |
| StorageDrawers | `StorageDrawers-2.2.31-GTNH.jar` | D | 1024 | 97 | [storagedrawers.md](storagedrawers.md): 51 registry names, ~90 useful. 83 plain framed-wood trim blocks (no TE); drawers as storeroom dressing |
| StructureLib | `structurelib-1.4.47.jar` | D | 16 | 1 | Nothing useful: One hint block for multiblock previews |
| supersolarpanels | `supersolarpanel-1.1.5.jar` | D | 12 | 4 | Nothing useful: Ticking generators; free high-tier loot against the EV ceiling (sheet `solarpanels/`) |
| Tainted-Magic | `Tainted-Magic-7.7.8-GTNH.jar` | D | 8 | 6 | Nothing useful: Purple warpwood, shadowmetal, an ore |
| Thaumic_Exploration | `Thaumic-Exploration-1.5.29-GTNH.jar` | D | 54 | 14 | [thaumic-addons.md](thaumic-addons.md): 11 registry names, 1 row (partly). Floating candles only partly fit |
| ThaumicBases | `Thaumic-Based-1.9.22.jar` | D | 219 | 53 | [thaumic-addons.md](thaumic-addons.md): 53 registry names, 37 rows (4 yes, 17 partly). Beta-era ancient bricks, cobblestone, mossy cobblestone, gravel |
| ThaumicBoots | `thaumicboots-1.5.13.jar` | D | 64 | 0 | Nothing useful: Ships 64 block textures but registers no blocks |
| ThaumicEnergistics | `thaumicenergistics-1.7.68-GTNH.jar` | D | 42 | 1 | Nothing useful: Machines |
| ThaumicHorizons | `ThaumicHorizons-1.8.28.jar` | D | 90 | 40 | [thaumic-addons.md](thaumic-addons.md): 40 registry names, 34 rows (7 yes, 11 partly). Frosted crystal glass in 16 colors |
| thaumicinsurgence | `thaumicinsurgence-0.4.1.jar` | D | 60 | 6 | [thaumic-addons.md](thaumic-addons.md): 6 registry names, 2 rows (2 yes). Arcane marble; `tile.` names |
| ThaumicMachina | `ThaumicMachina-0.2.6-GTNH.jar` | D | 2 | 2 | Nothing useful: Near-black brick and a rose; nothing fits |
| ThaumicTinkerer | `ThaumicTinkerer-2.12.34.jar` | D | 91 | 6 | [thaumic-addons.md](thaumic-addons.md): about 40 registry names, 9 rows (5 yes, 1 partly). Smokey (black) quartz set |
| Tinkers-Defense | `tinkersdefense-1.3.4.jar` | D | 12 | 9 | Nothing useful: Stations and metal blocks |
| TinkersMechworks | `TMechworks-0.4.2.jar` | D | 37 | 5 | Nothing useful: Machines |
| Translocators | `Translocator-1.4.5.jar` | D | 1 | 2 | Nothing useful: Machine and crafting grid |
| twilightforest | `TwilightForest-2.7.45.jar` | D | 366 | 5 | [twilightforest.md](twilightforest.md): 125 registry names, ~95 useful. Underbrick, 8 woods with doors; castle brick and mazestone are hardness 100 and never placed |
| VendingMachine | `vendingmachine-0.4.103.jar` | D | 15 | 0 | [vendingmachine.md](vendingmachine.md): 2 registry names, 1 useful. A GT multiblock vending machine; depends on family C's GT-machine approach |
| VillageNames | `VillageNames-4.5.21-GTNH.jar` | D | 72 | 11 | [villagenames.md](villagenames.md): 3 registry names, 1-2 useful. Lunarin bricks only; its other blocks are off with Et Futurum present |
| WarpTheory | `WarpTheory-1.5.8-GTNH.jar` | D | 1 | 1 | Nothing useful: One vanishing block |
| WirelessRedstone-CBE | `WR-CBE-1.7.12.jar` | D | 4 | 0 | Nothing useful: Multipart parts, no blocks |
| Witchery | `witchery-1.7.10-0.24.1.jar` | D | 213 | 36 | [witchery.md](witchery.md): ~120 registry names, ~30 useful. Rowan, alder, hawthorn woods; shaded glass; spreading and teleporting brambles flagged |
| WitcheryExtras | `WitcheryExtras-1.4.20.jar` | D | 0 | 0 | Nothing useful: Mixins only |
| WitchingGadgets | `WitchingGadgets-1.8.53-GTNH.jar` | D | 47 | 7 | Nothing useful: Machines, a mirror portal, a rose vine that damages on contact |
| AkashicTome | `akashictome-1.2.7.jar` | - | 0 | 0 | No placeable blocks |
| Angelica | `angelica-2.2.43.jar` | - | 3 |  | No placeable blocks: client renderer; textures only |
| AngerMod | `AngerMod-1.0.7.jar` | - | 0 | 0 | No placeable blocks |
| AppleCore | `AppleCore-3.3.13.jar` | - | 0 | 0 | No placeable blocks |
| Archaicfix | `archaicfix-0.8.2.jar` | - | 0 | 0 | No placeable blocks |
| AsieLib | `AsieLib-0.7.2.jar` | - | 0 | 0 | No placeable blocks |
| AspectRecipeIndex | `aspectrecipeindex-1.1.6.jar` | - | 0 | 0 | No placeable blocks |
| Backhand | `backhand-1.8.18.jar` | - | 0 | 0 | No placeable blocks |
| Baubles-Expanded | `BaublesExpanded-2.2.27-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| bdlib | `bdlib-1.11.1-GTNH.jar` | - | 0 | 2 | No placeable blocks: library; registers only for dependent mods |
| BeeBetterAtBees-GTNH | `beebetteratbees-0.4.8-GTNH.jar` | - | 0 |  | No placeable blocks |
| BetterAchievements | `BetterAchievements-0.3.3.jar` | - | 0 |  | No placeable blocks |
| BetterBuildersWands | `BetterBuildersWands-0.13.12-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| BetterCrashes | `bettercrashes-1.4.7-GTNH.jar` | - | 0 |  | No placeable blocks |
| BetterLoadingScreen | `betterloadingscreen-1.7.19-GTNH.jar` | - | 0 |  | No placeable blocks |
| BetterP2P | `betterp2p-1.4.8.jar` | - | 0 | 0 | No placeable blocks |
| BlockLimiter | `BlockLimiter-0.7.0.jar` | - | 0 | 0 | No placeable blocks |
| BlockRenderer6343 | `blockrenderer6343-1.4.25.jar` | - | 0 | 0 | No placeable blocks |
| Bug-Torch | `bugtorch-1.3.1.jar` | - | 3 | 0 | No placeable blocks: patches vanilla blocks; adds none |
| BuildCraftOilTweak | `BuildCraftOilTweak-1.1.3.jar` | - | 0 | 0 | No placeable blocks |
| ChromaticTooltips | `chromatictooltips-1.0.36-GTNH.jar` | - | 0 |  | No placeable blocks |
| ChromaticTooltipsCompat | `chromatictooltipscompat-1.0.40-GTNH.jar` | - | 0 |  | No placeable blocks |
| Chunk API | `chunkapi-mc1.7.10-0.8.4.jar` | - | 0 | 0 | No placeable blocks |
| CodeChickenCore | `CodeChickenCore-1.4.22.jar` | - | 0 | 0 | No placeable blocks |
| CoFH Core | `CoFHCore-[1.7.10]3.1.4-329.jar` | - | 0 | 0 | No placeable blocks |
| Controlling | `controlling-2.1.10.jar` | - | 0 |  | No placeable blocks |
| CoreTweaks | `coretweaks-0.3.4.7-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| CosmeticArmorReworked | `cosmeticarmorreworked-1.0.7-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| Craft-Presence | `CraftPresence-2.7.1+1.7.10-forge.jar` | - | 0 |  | No placeable blocks |
| CraftTweaker | `CraftTweaker-3.4.8.jar` | - | 0 | 0 | No placeable blocks |
| Custom-Main-Menu | `custommainmenu-1.14.1.jar` | - | 0 |  | No placeable blocks |
| Darkerer | `darkerer-1.1.0.jar` | - | 0 | 0 | No placeable blocks |
| Default-Configs | `defaultkeys-1.3.4.jar` | - | 0 |  | No placeable blocks |
| DefaultServerList | `defaultserverlist-1.7.4.jar` | - | 0 |  | No placeable blocks |
| DefaultWorldGenerator | `defaultworldgenerator-0.5.0.jar` | - | 0 |  | No placeable blocks |
| DummyCore | `DummyCore-1.21.0.jar` | - | 0 | 3 | No placeable blocks: library |
| DuraDisplay | `duradisplay-1.4.7.jar` | - | 0 |  | No placeable blocks |
| EnderCore | `endercore-0.5.15.jar` | - | 0 | 4 | No placeable blocks: library |
| EndlessIDs | `endlessids-mc1.7.10-1.7.4.jar` | - | 0 | 0 | No placeable blocks |
| EnhancedLootBags | `EnhancedLootBags-1.3.5.jar` | - | 0 | 0 | No placeable blocks |
| Eternal-Singularity | `eternalsingularity-1.4.3.jar` | - | 0 | 0 | No placeable blocks |
| Extreme Sound Muffler | `esm-legacy-1.7.10-1.1.1.jar` | - | 0 |  | No placeable blocks |
| FalsepatternLib | `falsepatternlib-mc1.7.10-1.12.2.jar` | - | 0 | 0 | No placeable blocks |
| FindIt | `findit-1.4.8.jar` | - | 0 | 0 | No placeable blocks |
| Forgelin | `forgelin-2.0.3-GTNH.jar` | - | 0 |  | No placeable blocks |
| ForgeRelocationFMP | `ForgeRelocationFMP-0.2.0.jar` | - | 0 | 0 | No placeable blocks |
| Freecam | `freecam-gtnh-1.0.12.jar` | - | 0 | 0 | No placeable blocks |
| Gravitation-Suite-Neo | `gravisuiteneo-1.3.16.jar` | - | 0 | 0 | No placeable blocks |
| GregoryTweaksForCrafting | `craftingtweaks-1.0.3.jar` | - | 0 | 0 | No placeable blocks |
| GTNH-Credits | `gtnhcredits-1.4.0.jar` | - | 0 |  | No placeable blocks |
| GTNH-TC-Wands | `GTNH-TC-Wands-1.4.14.jar` | - | 0 | 0 | No placeable blocks |
| GTNHExtLib | `gtnhextlib-1.0.5.jar` | - | 0 | 0 | No placeable blocks |
| GTNHLib | `gtnhlib-0.11.54.jar` | - | 10 | 7 | No placeable blocks: test blocks behind `enableTestBlocks`, off |
| GuideNH | `guidenh-1.3.55.jar` | - | 1 | 0 | No placeable blocks: no blocks |
| Healer | `Healer-1.2.1.jar` | - | 0 | 0 | No placeable blocks |
| HelpFixer | `HelpFixer-1.3.2.jar` | - | 0 | 0 | No placeable blocks |
| Hodgepodge | `hodgepodge-2.7.221.jar` | - | 0 | 1 | No placeable blocks: mixins; patches registration, adds nothing |
| HoloInventory | `holoinventory-2.5.18-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| Hunger-Overhaul | `HungerOverhaul-1.7.10-1.0.0.jenkins104.jar` | - | 0 | 0 | No placeable blocks |
| IFU | `ifu-1.12.4.jar` | - | 0 | 0 | No placeable blocks |
| Infernal-Mobs | `InfernalMobs-1.10.7-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| InGame-Info-XML | `InGameInfoXML-2.9.8.jar` | - | 0 | 0 | No placeable blocks |
| InventoryBogoSorter | `bogosorter-1.3.55-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| IronChestMinecarts | `IronChestMinecarts-1.2.1.jar` | - | 0 | 0 | No placeable blocks |
| IronTankMinecarts | `irontankminecarts-1.0.12.jar` | - | 0 | 0 | No placeable blocks |
| JarJar | `jarjar-0.3.7-beta.jar` | - | 0 | 0 | No placeable blocks |
| JourneyMap | `journeymap-1.7.10-5.2.23-fairplay.jar` | - | 0 |  | No placeable blocks |
| JourneyMap Server | `(not in mods/)` | - |  |  | No placeable blocks |
| LunatriusCore | `LunatriusCore-1.2.1-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| lwjgl3ify | `lwjgl3ify-3.0.42.jar` | - | 0 | 0 | No placeable blocks |
| Mantle | `Mantle-0.5.4.jar` | - | 0 | 0 | No placeable blocks |
| MatterManipulator | `matter-manipulator-0.1.75-GTNH.jar` | - | 3 | 0 | No placeable blocks: item textures only |
| Minecraft-Backpack-Mod | `backpack-2.6.18-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| Minetweaker-Gregtech-5-Addon | `GTTweaker-2.3.4.jar` | - | 0 | 0 | No placeable blocks |
| Mobs Info | `mobsinfo-0.6.2.jar` | - | 0 | 0 | No placeable blocks |
| ModTweaker | `ModTweaker2-0.14.0.jar` | - | 0 | 0 | No placeable blocks |
| ModularUI | `modularui-1.3.4.jar` | - | 0 | 1 | No placeable blocks: test block in dev or with `forceEnableDebugBlock`, off |
| ModularUI2 | `modularui2-2.3.94-1.7.10.jar` | - | 0 | 1 | No placeable blocks: test block in its test handler only |
| MouseTweaks | `MouseTweaks-2.5.4-GTNH.jar` | - | 0 |  | No placeable blocks |
| MrTJPCore | `MrTJPCore-1.3.7.jar` | - | 0 | 2 | No placeable blocks: library |
| NaturesCompass | `naturescompass-1.5.2-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| Navigator | `navigator-1.1.10.jar` | - | 0 | 0 | No placeable blocks |
| nei-custom-diagram | `NEICustomDiagram-1.8.36.jar` | - | 0 | 0 | No placeable blocks |
| NEI-Integration | `NEIIntegration-1.5.3.jar` | - | 0 | 0 | No placeable blocks |
| neiaddons | `NEIAddons-1.18.8.jar` | - | 0 | 0 | No placeable blocks |
| NetherPortalFix | `netherportalfix-1.4.0.jar` | - | 0 | 0 | No placeable blocks |
| Nodal-Mechanics | `NodalMechanics-1.3.4-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| NoHotbarNeeded | `nohotbarneeded-0.0.6.jar` | - | 0 | 0 | No placeable blocks |
| NotEnoughItems | `NotEnoughItems-2.8.171-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| Nutrition | `nutrition-1.0.1.jar` | - | 0 | 0 | No placeable blocks |
| oauth | `oauth-1.3.4-GTNH.jar` | - | 0 |  | No placeable blocks |
| OpenModsLib | `OpenModsLibs-0.10.15.jar` | - | 0 | 2 | No placeable blocks: library |
| Opis | `Opis-1.4.12-mapless.jar` | - | 0 | 2 | No placeable blocks: debug blocks only when `laggenerator_id` is set, off |
| OverloadedArmorBar | `overloadedarmorbar-1.1.1.jar` | - | 0 |  | No placeable blocks |
| Player-API | `PlayerAPI-1.5.0.jar` | - | 0 | 0 | No placeable blocks |
| Postea | `postea-1.2.7.jar` | - | 0 | 2 | No placeable blocks: example code only |
| RandomBoubles | `randomboubles-1.1.7.jar` | - | 0 | 0 | No placeable blocks |
| Realistic-World-Gen | `RWG-alpha-1.5.3.jar` | - | 0 | 0 | No placeable blocks |
| Realistic-World-Gen-2 | `rwg2-2.0.6.jar` | - | 0 | 0 | No placeable blocks |
| Schematica | `Schematica-1.12.6-GTNH.jar` | - | 0 |  | No placeable blocks |
| ServerUtilities | `ServerUtilities-2.4.22.jar` | - | 1 | 0 | No placeable blocks: no blocks |
| Share-Where-I-am | `sharewhereiam-2.1.11.jar` | - | 0 | 0 | No placeable blocks |
| SimpleSkinBackport | `simpleskinbackport-1.0.5-GTNH.jar` | - | 0 |  | No placeable blocks |
| SpecialMobs | `SpecialMobs-3.7.10.jar` | - | 0 | 0 | No placeable blocks |
| SpiceOfLife | `SpiceOfLife-2.2.12-carrot.jar` | - | 0 | 0 | No placeable blocks |
| StructureCompat | `structurecompat-0.7.5.jar` | - | 0 | 0 | No placeable blocks |
| Super-TiC | `SuperTic-1.5.6.jar` | - | 0 | 0 | No placeable blocks |
| TC-4-Tweaks | `Thaumcraft4Tweaks-1.5.48.jar` | - | 0 | 0 | No placeable blocks |
| TCNodeTracker | `tcnodetracker-1.4.6.jar` | - | 0 |  | No placeable blocks |
| thaumcraft-research-tweaks | `ThaumcraftResearchTweaks-1.4.1.jar` | - | 0 | 0 | No placeable blocks |
| ThaumcraftMobAspects | `ThaumcraftMobAspects-1.2.1-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| TiC-Tooltips | `TiCTooltips-1.4.2.jar` | - | 0 |  | No placeable blocks |
| Tinkers-Gregworks | `TGregworks-1.7.10-GTNH-1.0.34.jar` | - | 0 | 0 | No placeable blocks |
| TooMuchLoot | `TooMuchLoot-4.3.0-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| ToroHealth | `torohealth-1.2.4.jar` | - | 0 |  | No placeable blocks |
| TX-Loader | `txloader-1.9.1.jar` | - | 0 |  | No placeable blocks |
| UniLib | `UniLib-1.2.1+1.7.10-forge.jar` | - | 0 |  | No placeable blocks |
| UniMixins | `+unimixins-all-1.7.10-0.3.1.jar` | - | 0 |  | No placeable blocks |
| Universal-Singularities | `Universal-Singularities-8.15.4.jar` | - | 0 | 0 | No placeable blocks |
| VisualProspecting | `visualprospecting-1.5.42.jar` | - | 0 | 0 | No placeable blocks |
| waila | `Waila-1.19.34.jar` | - | 0 | 0 | No placeable blocks |
| WailaHarvestability | `WailaHarvestability-1.3.6-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| WAILAPlugins | `WAILAPlugins-0.7.2.jar` | - | 0 | 0 | No placeable blocks |
| WanionLib | `WanionLib-1.10.1.jar` | - | 0 | 0 | No placeable blocks |
| WAWLA | `Wawla-1.3.4-GTNH.jar` | - | 0 | 0 | No placeable blocks |
| WirelessCraftingTerminal | `WirelessCraftingTerminal-1.12.17.jar` | - | 0 | 0 | No placeable blocks |
| Yamcl | `YAMCore-0.7.5.jar` | - | 0 | 2 | No placeable blocks: library; fluids for dependent mods |
