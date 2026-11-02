# Integration opportunities

This sheet covers GTNH 2.9.0 mods that this mod does not yet integrate with, what each integration would give players, and where it fits the roadmap. The goal is the owner's "seamless and tight": abandoned cities, highways and railways in the RWG overworld that look lived-in, hold pack loot at EV or below, and can become a player's base ([0003](../../design/0003-world-presence.md), [0004](../../design/0004-gtnh-layer-and-tier-ceiling.md)).

Status: a survey. The five rows the owner decided in decision log D28 are *Accepted* ([Owner decisions](#owner-decisions)). The Slot column says what is built, what M06 still decides, and what waits until after beta; no other row is on the roadmap before beta ([decision log](../../design/decision-log.md), beta spec audit). Facts about other mods are *verified* with a permalink pinned to the pack's tag or a pack config path relative to `.minecraft/`, or marked *unverified* ([new-spec README](../README.md)).

## Scope

Already integrated, and not repeated here: RWG2 and classic RWG ([rwg.md](rwg.md)); GT ore veins ([ore-veins.md](ore-veins.md)); the D19 block mapping through Et Futurum Requiem, Botania and Chisel ([block-mapping.md](block-mapping.md)); loot through this mod's `ChestGenHooks` categories with EnhancedLootBags, HarvestCraft and Railcraft parts, and vanilla spawners varied by Special Mobs, Infernal Mobs and EnderZoo ([loot-and-mobs.md](loot-and-mobs.md)); other worldgen and spawn ([other-worldgen.md](other-worldgen.md), [spawn.md](spawn.md)).

Covered elsewhere:

- **Blocks for palette variety.** Spike S4 catalogs every decorative and construction block in the pack, with contact sheets and roles, in `docs/new-spec/05-assets/block-catalog/` (decision log D25). This sheet names a block only where an integration depends on it: a door that restores upstream's look, a road that changes travel, a lamp that matters under the pack's darkness. For anything else about blocks, see the catalog.
- **Coexistence with other mods' structures** (villages, Roguelike Dungeons, Witchery, meteorites, LootGames; decision log D24) is a separate research task. [Notes for the coexistence research](#notes-for-the-coexistence-research) lists what this survey found along the way.

## How to read

- **Value** is what a player notices: in the look, in loot, in travel, or in moving in.
- **Effort**: **S** is a few days inside one milestone; **M** is a milestone task of its own, or new shared machinery; **L** is several weeks, or work that depends on M-sized prerequisites.
- **Risk** covers the kind of dependency (every integration here is optional, guarded under `compat`), version churn, internal rather than public API, and mixins.
- **Kind**:
  - *Faithful* restores or keeps upstream's content.
  - *GTNH layer* is an addition beside upstream's content, which record 0004 allows later.
  - *Meta* sits outside world generation: maps, quests, NEI, the HUD.
- **Slot** is where the row stands on the [ROADMAP](../../ROADMAP.md): built (and in which milestone), M06 (still landing), beta, or after beta.

## Ranked recommendations

Top recommendations first. The ranking weighs value against effort and risk, and puts first what keeps faithful content faithful.

| # | Opportunity | Mods | Value | Effort | Risk | Kind | Slot |
|---|---|---|---|---|---|---|---|
| 1 | [Wood-variant doors and the iron trapdoor](#doors-malisisdoors) that the D19 mapping dropped | MalisisDoors | Upstream's random door woods come back; two recorded deviations disappear | S | Low: blocks by name and metadata, TE created automatically | Faithful | Built, M02 (D28 rule 5) |
| 2 | [Loot shown in NEI](#loot-extensions) through NEI Custom Diagram's worldgen-loot diagrams | NEI Custom Diagram | Players can look up what each city chest band holds, as they do for every other `ChestGenHooks` category | S (lang keys only) | Low: automatic, reads Forge's map | Meta | After beta |
| 3 | [Furniture pools](#furniture-and-fittings) for upstream's random-furniture and fitting characters (`F`, `L`, `T`, `h`, `p`) | Et Futurum Requiem, CookingForBlockheads, Chisel, Railcraft, AE2 | Interiors read as offices, kitchens and workshops rather than vanilla-only rooms; usable workstations for a new base | S to M | Low for name-and-metadata blocks; M for blocks that keep facing in a TE | Faithful (same characters, wider pools) | Built for `F`, `L` and `p`, M11 ([0007](../../design/0007-palette-variety.md)); `T` and `h` after beta |
| 4 | [City names](#names-signs-and-discovery) from VillageNames' name generator, with entrance signs | VillageNames (optional) | Every city has a name the player can tell others, put on a map or read on a sign; the anchor for maps, quests, notes and the HUD | S to M | Medium: names depend on the pack's syllable config; GPL-3 API call only | GTNH layer | After beta |
| 5 | [Highways and streets as asphalt](#streets-and-highways) with lane markings | Chisel (`road`, `road_line`), GT concrete | Highways become fast roads (a 1.4× walking-speed block), and their surface stops being a GT boulder host | S | Low; changes upstream's cyan-terracotta look | GTNH layer | M06 (D28 rule 3) |
| 6 | [Discovered cities on the map](#maps) through a Navigator layer on JourneyMap | Navigator, JourneyMap | Outlines and names of visited cities on the minimap and the full map, like VisualProspecting's veins and ServerUtilities' claims | M (needs a server-to-client city sync) | Medium: client mixin library, GTNH-maintained | Meta | After beta |
| 7 | [Railway and station dressing](#railways-and-stations): buffer stops, platform posts and lanterns, signals, abandoned chest minecarts | Railcraft, vanilla, IronChestMinecarts | Railways read as a rail network, not a vanilla minecart line; carts carry rail loot | M | Medium: Railcraft tracks need their TE set explicitly | GTNH layer | M06 for the parts; cart loot after beta |
| 8 | [Achievements and an in-game guide](#quests-achievements-and-the-guide) | Amazing-Trophies, GuideNH | "Discover a lost city" sits on the pack's own achievement page; a guide explains cities, loot bands and moving in | S each | Low: public APIs, jar-shipped resources | Meta | Beta |
| 9 | [Overgrowth](#overgrowth-and-gardens): ivy and moss in the ruin pass, weathered and overgrown road | Biomes O' Plenty, Chisel | The abandoned look; ruins that read as years old | S | Low; vines spread as vanilla vines do | GTNH layer | After beta (the catalog's overgrowth pool has no variety group) |
| 10 | [Lighting for the pack's darkness](#lighting-and-atmosphere) | Darkerer (behavior), Chisel, Railcraft, AE2, Twilight Forest | Under Darkerer's pitch-black nights, a lit city is a landmark and a refuge | S (pool choice) | Low | Faithful plus variety | Built for ceiling lights, M11 ([0007](../../design/0007-palette-variety.md) §6); the rest after beta |
| 11 | [Move-in kit](#moving-in): beds, an elevator column in tall towers, sleeping bags in loot | Et Futurum Requiem, vanilla, OpenBlocks, SleepingBags | A tower becomes livable: respawn point, fast travel between floors | M | Medium: changes or overlays upstream's parts | GTNH layer | After beta (D28 rule 8) |
| 12 | [A quest line from the jar](#quests-achievements-and-the-guide) through BetterQuesting's scoreboard task | BetterQuesting | Discovering and looting cities is acknowledged in the questbook, with loot-bag rewards | M | Medium: first jar-shipped quest line in the pack; no code dependency on BetterQuesting | Meta | After beta (D28 rule 8) |
| 13 | ["Claim this building" command](#moving-in) | ServerUtilities | On servers with claims on, one command claims a building's chunks within the player's limits | S | Low; the pack ships claims off | Meta | After beta |
| 14 | [Lore notes in chests](#loot-extensions) | OpenPrinter | Printed pages naming the city and its last days; storytelling without new blocks | S (code) plus writing | Low | GTNH layer | After beta |
| 15 | [Roof gardens and parks with pack crops](#overgrowth-and-gardens) | HarvestCraft, Et Futurum Requiem | A food source for a new base | S | Low; gardens spread, up to a limit | GTNH layer | After beta |
| 16 | [Abandoned GT machine hulls and cables](#abandoned-industry) up to EV | GT5-Unofficial (bartworks precedent) | Salvageable industry: the strongest "GTNH city" signal, and real early-game value | M to L | High: GT internals, machine-update thread, balance | GTNH layer | Hulls never; casings after beta (D28 rule 4) |
| 17 | [Office computers](#abandoned-industry): cases, screens, keyboards, inactive | OpenComputers | Offices that look like offices; salvage | M | Medium: placing OpenComputers blocks during population is untested | GTNH layer | After beta |
| 18 | [Current city on the HUD and in Waila](#names-signs-and-discovery) | Waila, InGame-Info-XML | "You are in Ashford, library" at a glance | S once the sync exists | Low; InGame-Info-XML needs a pack-side XML line | Meta | After beta |
| 19 | [Coke ovens and blast furnaces](#railways-and-stations) as intact multiblocks in industrial buildings | Railcraft | A working steam-age facility to move into | M | Medium: forming by itself is untested | GTNH layer | After beta (GTNH-themed buildings) |
| 20 | [Storage blocks](#moving-in): drawers, iron chests, barrels | StorageDrawers, ironchest, Jabba | Warehouse look; empty storage for a new base | M (facing lives in the TE) | Low | GTNH layer | After beta |

Not recommended, with reasons: [Not recommended or note only](#not-recommended-or-note-only).

## Owner decisions

Decision log D28 settles the rows that needed the owner.

| Row | Decision |
|---|---|
| 1, MalisisDoors' wood doors and iron trapdoor | Adopted (D28 rule 5): the pack already replaces vanilla doors with MalisisDoors' tile-entity doors, and the [block mapping](block-mapping.md) uses them with the vanilla door and trapdoor as fallbacks |
| 5, asphalt highways | Speed blocks (Chisel concrete and road, GT concrete, the speeding Tinkers' brownstone) are walked on only on highways and primary city roads, as a weighted option judged at the M06 screenshots; elsewhere they may sit in walls and trims, never where players walk (D28 rule 3) |
| 11, beds and elevator columns | After beta, as an overlay pass with its own toggle, never by editing upstream's part files (D28 rule 8) |
| 12, a quest line in the jar | Achievements and an in-game guide at beta; a BetterQuesting quest line in the jar behind a hidden config key after beta, as in GregGuns (D28 rule 8) |
| 16, abandoned GT industry | Machine hulls are never placed. GT casings ULV to EV wait for the GTNH-themed buildings after beta (D28 rule 4). Cables are decided with those buildings |

## Shared prerequisites

Two pieces of machinery unlock most of the rows above. Building them once, early, keeps each integration small.

1. **Block-entity templates in palettes.** Many pack blocks keep their facing, color or type in the TE rather than in metadata: CookingForBlockheads color, StorageDrawers and iron-chest facing, BiblioCraft rotation, Railcraft track kind, GT machine IDs. The asset system already builds TE data at placement for chests, spawners and furnaces ([block-mapping.md](block-mapping.md), "Block-entity data"). Generalizing that into a per-palette-entry NBT template (with facing derived from the part's rotation) makes each of those blocks a palette entry instead of code. M02 owns the palette format; the template is an extension of it.
2. **A city registry with a server-to-client sync.** Maps, the HUD, Waila, quests and achievements all need "which city is this, what is it called, has this player been here". On a multiplayer client the seed is unknown, so the client cannot compute city centers ([Names, signs and discovery](#names-signs-and-discovery)). The server holds per-player discovered cities in world saved data and sends `{name, center, radius}` for discovered cities, plus the current city, to each client (`networking-side-safety`). M10 (the API) is the natural owner.

## Doors (MalisisDoors)

*Verified.* MalisisDoors 1.19.13-GTNH registers `door_spruce`, `door_birch`, `door_jungle`, `door_acacia` and `door_dark_oak` ([Registers.java#L152-L174](https://github.com/GTNewHorizons/MalisisDoors/blob/1.19.13-GTNH/src/main/java/net/malisis/doors/Registers.java#L152-L174)) and an `iron_trapdoor` ([#L233-L247](https://github.com/GTNewHorizons/MalisisDoors/blob/1.19.13-GTNH/src/main/java/net/malisis/doors/Registers.java#L233-L247)). These are exactly what the D19 mapping found missing, because the pack disables Et Futurum's wood doors and iron trapdoor. Every wooden door therefore became oak, and the iron trapdoor a wooden one ([block-mapping.md](block-mapping.md), "Deviations from upstream this mapping causes").

- **Placement.** The door's descriptor is set when its TE is created ([Door.java#L372-L386](https://github.com/GTNewHorizons/MalisisDoors/blob/1.19.13-GTNH/src/main/java/net/malisis/doors/door/block/Door.java#L372-L386)), and `world.setBlock` creates the TE, so a door is complete with name and metadata. Metadata follows vanilla's door bits; the top half has no TE.
- **The pack already replaces vanilla doors.** `config/malisisdoors.cfg` line 16 sets `modifyVanillaDoors=true`, which makes MalisisDoors re-register vanilla doors, trapdoors and fence gates as its own animated blocks ([Registers.java#L84-L89](https://github.com/GTNewHorizons/MalisisDoors/blob/1.19.13-GTNH/src/main/java/net/malisis/doors/Registers.java#L84-L89)). The `minecraft:wooden_door` that D19 places is already a Malisis block with a TE in this pack.
- **Beyond faithful.** Glass sliding doors (`iron_sliding_door`, `wood_sliding_door`), `garage_door`, `jail_door`, `laboratory_door`, `factory_door`, `shoji_door`, `saloon` and curtains ([#L176-L221](https://github.com/GTNewHorizons/MalisisDoors/blob/1.19.13-GTNH/src/main/java/net/malisis/doors/Registers.java#L176-L221), [#L357-L366](https://github.com/GTNewHorizons/MalisisDoors/blob/1.19.13-GTNH/src/main/java/net/malisis/doors/Registers.java#L357-L366)) suit shopping malls, the gas station and GTNH-themed buildings. These are block choices for the S4 catalog.
- **Avoid.** `carriage_door`, `medieval_door` and `rustyHatch` build their multiblock in `onBlockPlacedBy`, which needs a placing entity ([BigDoor.java#L147-L166](https://github.com/GTNewHorizons/MalisisDoors/blob/1.19.13-GTNH/src/main/java/net/malisis/doors/door/block/BigDoor.java#L147-L166)).

Effort S: mapping rows plus the fallback. Slot: M02; adopted by D28 rule 5.

## Furniture and fittings

Upstream already has slots for furniture. Palette `common` defines `F` as random furniture (furnace, crafting table, brewing stand, anvil, cauldron, enchanting table, or a cobweb), `L` bookshelf, `T` torch, `h` glowstone, `g` lit redstone torch and `p` random flowers and saplings ([catalog-palettes.md](../../original-spec/05-assets/catalog-palettes.md#character-conventions)). Widening those pools with pack blocks changes no part file, keeps every building's layout, and is how the GTNH layer reaches interiors cheaply. The S4 catalog chooses the blocks; this section lists the families that make the pools work.

| Family | What it adds | Placement model | Evidence |
|---|---|---|---|
| Et Futurum Requiem workstations | `smoker`, `blast_furnace`, `smithing_table`, `stonecutter`, `fletching_table`, `cartography_table`, `loom`: usable blocks for a new base | Plain blocks; facing metadata *unverified* | [ModBlocks.java#L484-L494](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L484-L494); all on in `config/etfuturum/blocksitems.cfg` (lines 213, 119, 209, 219, 144, 125, 156). `composter` is off (line 138). |
| CookingForBlockheads | Kitchens: `cookingtable`, `cookingoven`, `counter`, `countercorner`, `cabinet`, `fridge`, `sink`, `toolrack`, `toaster`, colored kitchen floors | Facing is metadata 2 to 5; color and the flipped flag live in the TE and default to white and unflipped, so a palette needs no NBT for white kitchens | [CommonProxy.java#L51-L104](https://github.com/GTNewHorizons/CookingForBlockheads/blob/1.4.14-GTNH/src/main/java/net/blay09/mods/cookingforblockheads/CommonProxy.java#L51-L104), [BlockCounter.java#L101-L118](https://github.com/GTNewHorizons/CookingForBlockheads/blob/1.4.14-GTNH/src/main/java/net/blay09/mods/cookingforblockheads/block/BlockCounter.java#L101-L118). The pack ships no config for it; blocks register unconditionally. |
| Light fittings for `T`, `h`, `g` | Lanterns, quartz fixtures, firefly jars; see [Lighting](#lighting-and-atmosphere) | Plain blocks | S4 catalog |
| Bookcases for `L` | BiblioCraft bookcases would suit the library multibuilding | Rotation lives in the TE, and BiblioCraft is All Rights Reserved with no source, so the NBT contract would come from bytecode. Not recommended before the TE-template prerequisite, and then only with a `// Decompiled:` hedge | Jar class list and `config/BiblioCraft.cfg` lines 10 to 50 (all enabled) |

Two rules carry over from the existing sheets:

- **Ore rule.** No pool member is a block GT ore or GT's boulders replace ([ore-veins.md](ore-veins.md)).
- **Loot rule.** Any pool member with an inventory (barrels, the cooking table's storage) follows the loot rules: it is either empty or filled from this mod's categories ([loot-and-mobs.md](loot-and-mobs.md)).

Effort S for the name-and-metadata families, M once TE templates exist. Built for `F` (furniture), `L` (bookshelves) and `p` (planters) as the palette variety (M11); `T` and `h` keep their baseline until after beta.

## Streets and highways

Upstream's highways are surfaced with cyan terracotta (palette character `R`), and its streets with the smooth stone double slab (`S`) ([catalog-palettes.md](../../original-spec/05-assets/catalog-palettes.md#character-conventions)). Cyan terracotta is `minecraft:stained_hardened_clay`, which GT's boulders can replace ([ore-veins.md](ore-veins.md), D18).

The pack has real road blocks:

- **Chisel `road` and `road2`.** Asphalt with painted markings: crosswalk, dashed white and yellow, lane edges, full white and yellow, weathered and overgrown asphalt ([Features.java#L5250-L5290](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java#L5250-L5290)). They are on by default (absent from `config/chisel.cfg`, so on per [Configurations.java#L305-L311](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/config/Configurations.java#L305-L311)). They are concrete-family blocks, which speed up walking. *Verified* by the helper research; the exact speed factor comes from `chisel.cfg` and is *unverified* here.
- **Chisel `road_line`.** A paint overlay 1/256 of a block tall (white, double white, yellow, double yellow, warning). It breaks off when the block under it goes ([Features.java#L3459-L3464](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/Features.java#L3459-L3464), [BlockRoadLine.java#L54-L69](https://github.com/GTNewHorizons/Chisel/blob/2.17.34-GTNH/src/main/java/team/chisel/block/BlockRoadLine.java#L54-L69)).
- **ModernMarkings.** Floor markings (lane, line, crossing, turn, arrow, hazard line, letters) in six colors, and wall signs (exit, first aid, hazard symbols, voltage tiers) ([ModBlocks.java#L22-L202](https://github.com/GTNewHorizons/ModernMarkings/blob/0.3.13-1.7.10/src/main/java/modernmarkings/init/ModBlocks.java#L22-L202)). A floor marking sits in the air block above a solid top, with rotation in metadata 0 to 3 and no TE ([MarkingFloor.java#L41-L90](https://github.com/GTNewHorizons/ModernMarkings/blob/0.3.13-1.7.10/src/main/java/modernmarkings/blocks/MarkingFloor.java#L41-L90)). Registry names carry a `tile.` prefix, for example `modernmarkings:tile.floor_marking_white_lane` ([#L205-L209](https://github.com/GTNewHorizons/ModernMarkings/blob/0.3.13-1.7.10/src/main/java/modernmarkings/init/ModBlocks.java#L205-L209)).
- **GT concrete** (`gregtech:gt.blockconcretes`, dark and light, eight shapes each) multiplies horizontal motion by 1.4 for a player walking on it ([BlockConcretes.java#L50-L56](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/blocks/BlockConcretes.java#L50-L56)). GT5-Unofficial at this tag has no asphalt block of its own.

The integration:

- Highway and wide-street surfaces draw from asphalt.
- Lane markings come from `road` variants (full blocks, robust) rather than overlays where a part has a marking row. Upstream's `street_large` parts already paint lanes with smooth quartz ([catalog.md](../../original-spec/05-assets/catalog.md)).
- ModernMarkings floor signs mark station entrances, gas-station pumps and parking.

Player value is travel: a city-to-city highway becomes the fastest road in the overworld.

One caution: upstream's `clearRailing` finds highway railings by `Blocks.IRON_BARS` identity ([highway-parts.md](../../original-spec/06-highways-railways/highway-parts.md)), so railing blocks stay iron bars whatever the surface becomes.

Effort S. Slot: M06, a weighted option judged at the M06 screenshots (D28 rule 3, [Owner decisions](#owner-decisions)).

## Railways and stations

Upstream's railways place vanilla `rail` and `powered_rail`, with stations, rail dungeons and staircases ([railway-parts.md](../../original-spec/06-highways-railways/railway-parts.md)). Railcraft 9.17.36 is the pack's rail mod, and all its blocks named below are on in `config/railcraft/blocks.cfg` (lines 12 to 46).

- **Tracks need their TE set explicitly.**
  - A Railcraft track is `Railcraft:track`, with the rail shape in metadata and the track kind in the TE (`trackTag`) ([TileTrack.java#L41-L62](https://github.com/GTNewHorizons/Railcraft/blob/9.17.36/src/main/java/mods/railcraft/common/blocks/tracks/TileTrack.java#L41-L62)).
  - `setBlock` alone leaves a track with no TE. The supported path is `TrackTools.placeTrack`, which sets the block and then the TE ([TrackTools.java#L143-L148](https://github.com/GTNewHorizons/Railcraft/blob/9.17.36/src/main/java/mods/railcraft/common/blocks/tracks/TrackTools.java#L143-L148)).
  - Railcraft's own village workshop does exactly this during worldgen, falling back to vanilla rail when a track kind is disabled ([ComponentWorkshop.java#L216-L230](https://github.com/GTNewHorizons/Railcraft/blob/9.17.36/src/main/java/mods/railcraft/common/worldgen/ComponentWorkshop.java#L216-L230)). That is the precedent and the fallback pattern to copy.
- **There is no abandoned track.** Railcraft 9.17.36 has no abandoned track kind ([EnumTrack.java#L38-L87](https://github.com/GTNewHorizons/Railcraft/blob/9.17.36/src/main/java/mods/railcraft/common/blocks/tracks/EnumTrack.java#L38-L87)) and does not replace mineshaft rails; it only adds loot to the mineshaft category ([LootPlugin.java#L25](https://github.com/GTNewHorizons/Railcraft/blob/9.17.36/src/main/java/mods/railcraft/common/plugins/forge/LootPlugin.java#L25)). Upstream's main lines stay vanilla rail (faithful). Railcraft tracks go only where they add something: buffer stops at `rails_horizontal_end`, and a few special tracks in stations.
- **Station furniture.**
  - Posts and platforms: `Railcraft:post` metadata 0 to 6 (wood, stone, metal, emblem, three platforms; only the emblem has a TE) ([EnumPost.java#L19-L25](https://github.com/GTNewHorizons/Railcraft/blob/9.17.36/src/main/java/mods/railcraft/common/blocks/aesthetics/post/EnumPost.java#L19-L25)), plus colored `post.metal`.
  - Lanterns: `Railcraft:lantern.stone` and `lantern.metal`, no TE ([BlockFactoryLantern.java#L21-L39](https://github.com/GTNewHorizons/Railcraft/blob/9.17.36/src/main/java/mods/railcraft/common/blocks/aesthetics/lantern/BlockFactoryLantern.java#L21-L39)). A post with a lantern on top is a platform lamp.
  - Signals: `Railcraft:signal`, with the TE created from metadata ([EnumSignal.java#L24-L37](https://github.com/GTNewHorizons/Railcraft/blob/9.17.36/src/main/java/mods/railcraft/common/blocks/signals/EnumSignal.java#L24-L37)). Their facing lives in the TE (*unverified*), so they wait for TE templates.
- **Abandoned chest minecarts.**
  - Vanilla 1.7.10 mineshaft corridors place a rail, spawn an `EntityMinecartChest` and fill it from the mineshaft loot category during population. *Decompiled:* vanilla 1.7.10 `StructureMineshaftPieces.Corridor`, read from the decompiled game jar (Mojang code, no published source).
  - The same pattern puts derelict carts on railway parts and in rail dungeons, filled from `lostcitiesnh:rail_dungeon`, spawned in the populate step so they never cascade.
  - IronChestMinecarts 1.2.1 offers iron-chest variants that subclass the vanilla cart ([ItemMinecartIronChest.java](https://github.com/GTNewHorizons/IronChestMinecarts/blob/1.2.1/src/main/java/ganymedes01/ironchestminecarts/minecarts/ItemMinecartIronChest.java); its factory method is *unverified*). Vanilla carts are enough.
  - Entities in worldgen are new territory for this mod: the golden-chunk tests compare blocks, so carts need their own determinism check.
- **Coke ovens and blast furnaces.** `Railcraft:machine.alpha` metadata 7 (coke oven) and 12 (blast furnace) ([EnumMachineAlpha.java#L44-L49](https://github.com/GTNewHorizons/Railcraft/blob/9.17.36/src/main/java/mods/railcraft/common/blocks/machine/alpha/EnumMachineAlpha.java#L44-L49)) are multiblocks that re-test their pattern on server ticks ([TileMultiBlock.java#L141-L160](https://github.com/GTNewHorizons/Railcraft/blob/9.17.36/src/main/java/mods/railcraft/common/blocks/machine/TileMultiBlock.java#L141-L160)). A complete one placed by generation should form by itself (*unverified*, not run). They fit GTNH-themed industrial buildings after beta, not upstream's.

Effort M. Slot: M06 for the parts; the cart loot after beta. Railcraft stays an optional dependency, with vanilla rail as the fallback.

## Abandoned industry

The strongest "this is a GTNH city" signal is industry left behind. Everything here is a GTNH-layer addition for after beta, inside GTNH-themed buildings; upstream's buildings have no machine rooms. D28 rule 4 rules out machine hulls: the bartworks recipe below is recorded as research, not a plan.

- **GT machine hulls and cables: bartworks is the precedent.** Bartworks' ruins on Ross128b place GT machines and cables at a random tier from high-pressure steam to EV, the same ceiling as ours ([MapGenRuins.java#L228-L232](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/bartworks/system/worldgen/MapGenRuins.java#L228-L232)). The recipe, from [MapGenRuins.java#L106-L125](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/bartworks/system/worldgen/MapGenRuins.java#L106-L125), [#L182-L200](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/bartworks/system/worldgen/MapGenRuins.java#L182-L200):
  1. Disable GT's machine-update runnable while placing; the code throws otherwise.
  2. Place `gregtech:gt.blockmachines` with the metadata the meta-tile-entity asks for.
  3. Initialize the TE with the meta-tile-entity ID.
  4. Set the owner and front facing.
  5. For cables and pipes, set the connection bits on both sides.

  Hull IDs are 11 to 14 for LV to EV ([MetaTileEntityIDs.java#L17-L25](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/MetaTileEntityIDs.java#L17-L25)). There is no public helper, so this couples to GT internals that change between pack versions. The bartworks ruins seed their tier from `SecureRandom` ([loot-and-mobs.md](loot-and-mobs.md)), which is not a pattern to copy.
- **Casings without machines.** `gregtech:gt.blockcasings` metadata 0 to 4 are ULV to EV machine casings, plain blocks with no TE. They give the industrial look without salvage value beyond the casing itself. They are a block choice for the S4 catalog; *verified* by the helper research from GT's `en_US.lang` at the tag.
- **Office computers.**
  - OpenComputers `case1` to `case3`, `screen1` to `screen3` and `keyboard` get their TE from `setBlock`. Tier is in the block name, and facing is TE NBT (`oc:yaw`). *Verified* by the helper research.
  - Empty, unpowered computers are set dressing and salvage. Their behavior when placed during chunk population (network join) is untested.
  - Tier 1 parts are within the ceiling (EnhancedLootBags' `tier_1_computers` group is already allowed, [loot-and-mobs.md](loot-and-mobs.md)).
- **IC2 decorative blocks.** GT swaps IC2's reinforced stone, reinforced glass and iron fence for its own blocks through Postea transformers (`gregtech/loaders/postload/PosteaTransformers.java`, *verified* by the helper research). If any of those looks are wanted, use the GT blocks.

## Lighting and atmosphere

- **The pack's nights are pitch black.** Darkerer 1.1.0 lowers the lightmap minimum to zero ([MixinEntityRenderer.java#L27-L43](https://github.com/GTNewHorizons/Darkerer/blob/1.1.0/src/main/java/glowredman/darkerer/mixins/MixinEntityRenderer.java#L27-L43)), and the pack sets `mode=EVERYWHERE` (`config/darkerer.cfg` line 24). Away from block light, nights and unlit interiors are fully dark. The effect is client-side and visual; mob-spawn light levels are unchanged.
  - Upstream's lights (torches, glowstone, redstone torches, sea lanterns) are therefore landmarks: a lit city can be seen across RWG terrain at night.
  - Dark interiors are where spawners and their mobs wait. That is atmosphere worth keeping.
- **Light pools.** The S4 catalog picks the blocks. From this survey: Chisel's full-block lanterns (light 15), Railcraft lanterns, AE2's quartz fixture (no TE, cheap), Twilight Forest's firefly jar (light 15, no TE), Et Futurum's soul lantern and end rod. All are plain blocks, so nothing special is needed for Angelica or shader packs.
- **Lights that don't work as set dressing:**
  - FloodLights only light when powered or fueled, and keep their beam direction in the TE ([BlockFL.java#L122-L133](https://github.com/GTNewHorizons/FloodLights/blob/1.5.6/src/main/java/de/keridos/floodlights/block/BlockFL.java#L122-L133)).
  - ProjectRed's cage, fixture and lantern lights are ForgeMultipart parts, not placeable by `setBlock` ([proxies.scala#L16-L25](https://github.com/GTNewHorizons/ProjectRed/blob/4.12.50-GTNH/src/main/scala/mrtjp/projectred/illumination/proxies.scala#L16-L25)). Its plain lamps are usable but need metadata above 15 for the always-lit inverted kind ([blocks.scala#L370-L377](https://github.com/GTNewHorizons/ProjectRed/blob/4.12.50-GTNH/src/main/scala/mrtjp/projectred/illumination/blocks.scala#L370-L377)).
- **No ambient sound mod** is in the pack. City ambience would mean new sound assets of our own, a client feature for after beta at the earliest.

## Overgrowth and gardens

Upstream already puts vines on ruins: `ChunkFixer` places `minecraft:vine` ([block-inventory.md](../../original-spec/05-assets/block-inventory.md)).

- **Overgrowth.** The ruin and damage pass can draw from a wider pool for the abandoned look:
  - Biomes O' Plenty's `ivy`, `moss`, `willow` and `flowerVine` all extend vanilla `BlockVine`, so they spread and never decay as vanilla vines do. `moss` places only against wood or `minecraft:stone`; its survival elsewhere is *unverified*. The Biomes O' Plenty source is the 1.7.10 branch head, which is assumed to match the pack's build 2308.
  - Chisel's weathered and overgrown `road2` variants for streets.
  - Chisel `moss` and `mossCarpet`.
- **Plants to avoid.** Natura's thornvines damage entities ([ThornVines.java#L51-L56](https://github.com/GTNewHorizons/Natura/blob/2.8.24/src/main/java/mods/natura/blocks/crops/ThornVines.java#L51-L56)). Biomes O' Plenty leaves decay unless placed with the no-decay bit.
- **Roof gardens and parks.**
  - Upstream's `p` (random flowers and saplings) and `G` (grass) already make roof gardens.
  - HarvestCraft's wild gardens (`harvestcraft:herbgarden`, `berrygarden` and ten others) drop food and seeds when harvested, but they spread on random ticks (`config/harvestcraft.cfg` line 89 `enablegardenSpread=true`, limit 5 per line 101). Its crops need farmland. *Verified* from the pack jar and config; its repository is not public.
  - Et Futurum's sweet berry bush is on (`config/etfuturum/blocksitems.cfg` line 451).
  - A few gardens on roof soil give a new base a food source; their spread is a reason to keep them to roofs and parks.

Effort S. The catalog has the pools (S4); none has a variety group, so overgrowth and gardens wait until after beta.

## Names, signs and discovery

Upstream has no city names. A name is the GTNH-layer anchor for every meta integration below.

- **VillageNames' generator is callable with our own seeded `Random`.**
  - The call is `NameGenerator.newRandomName(String nameType, Random random)` ([NameGenerator.java#L45](https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/name/NameGenerator.java#L45)). The types include village, temple, fortress, mineshaft, alien and alienvillage, and types combine with hyphens ([#L73-L553](https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/name/NameGenerator.java#L73-L553)).
  - VillageNames itself seeds from the world seed and the position ([WellDecorateEvent.java#L222-L224](https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/handler/WellDecorateEvent.java#L222-L224)). A city's name can be a function of the city seed (D10).
- **Caveats.**
  - Names depend on the pack's syllable files (`config/VillageNames4/namepieces/*.cfg`), so they change if that config changes. The name is stored in world saved data the first time it is computed, and golden-chunk tests run with a fixed config or leave sign text out of the hash.
  - The call throws after 50 failed attempts on a bad config ([#L925-L956](https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/name/NameGenerator.java#L925-L956)), so it is wrapped.
  - The `custom` type is set to produce only "Notch" in the pack and is never used.
  - Without VillageNames, a small name generator of our own is the fallback.
- **Signs.** VillageNames writes a standing sign with the village name next to each well (`config/VillageNames4/general.cfg` line 5 `"Name Sign"=true`; [WellDecorateEvent.java#L486-L499](https://github.com/GTNewHorizons/VillageNames/blob/4.5.21-GTNH/src/main/java/astrotibs/villagenames/handler/WellDecorateEvent.java#L486-L499)).
  - The same convention fits cities: a vanilla sign at highway entrances and at the city center, written in the populate step. Et Futurum's wood signs (`config/etfuturum/blocksitems.cfg` lines 182 and 228) give wood variety.
  - Players in this pack already read village names this way, so a city sign reads as part of the pack.
- **Waila and the HUD.**
  - Waila's server-side data runs only for blocks with a TE ([Message0x01TERequest.java#L89-L91](https://github.com/GTNewHorizons/waila/blob/1.19.34/src/main/java/mcp/mobius/waila/network/Message0x01TERequest.java#L89-L91)). A tooltip line like "Lost City: Ashford, library" therefore comes from the client's synced current city, through a body provider registered on `Block.class` as Waila does for its own lines ([ModuleRegistrar.java#L300-L311](https://github.com/GTNewHorizons/waila/blob/1.19.34/src/main/java/mcp/mobius/waila/api/impl/ModuleRegistrar.java#L300-L311)).
  - InGame-Info-XML has no plugin hook, but its tag registry is public ([TagRegistry.java#L42-L48](https://github.com/GTNewHorizons/InGame-Info-XML/blob/2.9.8/src/main/java/com/github/lunatrius/ingameinfo/tag/registry/TagRegistry.java#L42-L48)). A `lostcity` tag works only once a player or the pack adds it to `config/InGameInfoXML/InGameInfo.xml`, so it is a [PACK-CHANGES](../../PACK-CHANGES.md) row, not something the jar can do.
  - Low value on its own; small once the [city registry and sync](#shared-prerequisites) exist.
- **ServerUtilities warps.** Warps are a public field (`ServerUtilitiesUniverseData.WARPS`), but the pack ships `warp=false` and `home=false` (`serverutilities/serverutilities.cfg` lines 219 and 162). Not worth doing.

## Maps

*Verified.*

- **Navigator is the route.** The JourneyMap build in the pack (5.2.23-fairplay) has no public client API package. The GTNH route is Navigator 1.1.10, a client-side map-layer library that draws on JourneyMap's full map and minimap ([README](https://github.com/GTNewHorizons/Navigator/blob/1.1.10/README.md), [API.md](https://github.com/GTNewHorizons/Navigator/blob/1.1.10/docs/API.md), [NavigatorApi.java#L50](https://github.com/GTNewHorizons/Navigator/blob/1.1.10/src/main/java/com/gtnewhorizons/navigator/api/NavigatorApi.java#L50)).
- **Two GTNH precedents.**
  - VisualProspecting draws ore veins from a client cache filled by server packets ([OreVeinLayerManager.java#L43-L75](https://github.com/GTNewHorizons/VisualProspecting/blob/1.5.42/src/main/java/com/sinthoras/visualprospecting/integration/model/layers/OreVeinLayerManager.java#L43-L75), [ProspectingNotification.java#L56-L61](https://github.com/GTNewHorizons/VisualProspecting/blob/1.5.42/src/main/java/com/sinthoras/visualprospecting/network/ProspectingNotification.java#L56-L61)).
  - ServerUtilities draws claimed chunks as filled, bordered areas ([ClaimsLayerManager.java#L37-L45](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.22/src/main/java/serverutils/integration/navigator/ClaimsLayerManager.java#L37-L45)). That is the look a city outline wants.
- **The integration.** A "Lost Cities" layer button shows outlines and names of cities this player has discovered (or every city, as an operator option). It reads the [city registry sync](#shared-prerequisites).
  - Only discovered cities appear, so the map rewards exploration rather than spoiling it.
  - Highways could follow as lines between discovered cities.
- **Risk.** Navigator mixes into JourneyMap. It is GTNH-maintained and pinned with the pack, but a client-only compat module must never load on a dedicated server (`networking-side-safety`).

Effort M, mostly the sync. After beta.

## Moving in

Record 0004 says buildings are meant to be lived in. Today that means "ordinary breakable blocks". These integrations make it easier.

- **Beds and respawn.** Upstream places no beds ([block-inventory.md](../../original-spec/05-assets/block-inventory.md)). Vanilla `minecraft:bed` and Et Futurum's 15 dyed beds (`enableDyedBeds`, `config/etfuturum/blocksitems.cfg` line 141) are plain `BlockBed`s with no TE ([ModBlocks.java#L840-L854](https://github.com/GTNewHorizons/Et-Futurum-Requiem/blob/2.6.65-GTNH/src/main/java/ganymedes01/etfuturum/ModBlocks.java#L840-L854)). A bed sets the player's respawn point, which is the first act of moving in.
  - Placing beds means adding to apartment floors, an overlay on upstream's parts, after beta with its own toggle (D28 rule 8).
  - SleepingBags' sleeping bag in `city_supplies` loot is the no-overlay alternative. It returns to the inventory on waking ([BlockSleepingBag.java#L70-L91](https://github.com/GTNewHorizons/SleepingBags/blob/0.3.1/src/main/java/ru/npo6ka/sleepingbag/blocks/BlockSleepingBag.java#L70-L91)), and a loot entry is S effort.
- **Elevators in towers.** OpenBlocks' `elevator` is a plain block with the color in metadata, and it is enabled ([BlockElevator.java#L30-L73](https://github.com/GTNewHorizons/OpenBlocks/blob/1.12.22-GTNH/src/main/java/openblocks/common/block/BlockElevator.java#L30-L73); `config/OpenBlocks.cfg` line 23). It reaches the next same-colored elevator within 20 blocks (line 97), more than the 6-block floor height. One elevator column per tower turns a 10-floor building into one players can live in.
  - Choosing the column for every building family is the hard part (after beta, D28 rule 8).
  - Avoid OpenBlocks' `scaffolding`, which removes itself on random ticks ([BlockScaffolding.java#L31-L36](https://github.com/GTNewHorizons/OpenBlocks/blob/1.12.22-GTNH/src/main/java/openblocks/common/block/BlockScaffolding.java#L31-L36)).
- **Claim this building.** Generation never claims land (0004), but a player can ask to claim.
  - ServerUtilities exposes `ClaimedChunks.instance.claimChunk(ForgePlayer, ChunkDimPos)` with the player's limits checked and a cancellable claim event ([ClaimedChunks.java#L309-L343](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.22/src/main/java/serverutils/data/ClaimedChunks.java#L309-L343), [ChunkModifiedEvent.java#L30-L60](https://github.com/GTNewHorizons/ServerUtilities/blob/2.4.22/src/main/java/serverutils/events/chunks/ChunkModifiedEvent.java#L30-L60)).
  - A `/lostcities claim` command claims the chunks of the building or multibuilding the player stands in, always with limits checked.
  - The pack ships `chunk_claiming=false` (`serverutilities/serverutilities.cfg` line 439), so the command only does something on servers that turn claims on, and says so otherwise.
  - Effort S; after beta.
- **Storage.**
  - StorageDrawers, iron chests and Jabba barrels keep their facing in the TE, defaulting to down or unknown, so they wait for TE templates ([TileEntityDrawers.java#L771-L787](https://github.com/GTNewHorizons/StorageDrawers/blob/2.2.31-GTNH/src/main/java/com/jaquadro/minecraft/storagedrawers/block/tile/TileEntityDrawers.java#L771-L787), [BlockIronChest.java#L141-L162](https://github.com/GTNewHorizons/ironchest/blob/6.1.13/src/main/java/cpw/mods/ironchest/BlockIronChest.java#L141-L162)).
  - They fit warehouses in GTNH-themed buildings.
  - An iron chest in a town-hall treasure room would hold more loot, but that is balance, so it goes on the beta list if wanted.

## Quests, achievements and the guide

- **Achievements: Amazing-Trophies (S).** It is the pack-native achievement system.
  - Another mod can register achievements and condition handlers ([AmazingTrophiesAPI.java#L277-L337](https://github.com/GTNewHorizons/Amazing-Trophies/blob/1.4.10/src/main/java/glowredman/amazingtrophies/api/AmazingTrophiesAPI.java#L277-L337)), and NewHorizonsCoreMod already awards achievements from code onto the pack's own page (`config/amazingtrophies/achievements/infinity_armor.json` line 3).
  - "Discover a lost city", "Loot a rail dungeon" and "Sleep in a city" fit there.
  - Forge's own `AchievementPage` is the fallback without the mod ([AchievementPage.java#L43-L50](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/src/main/java/net/minecraftforge/common/AchievementPage.java#L43-L50), 1.7.10 branch head).
- **Guide: GuideNH (S).** Markdown pages under `assets/<modid>/guidenh/_<lang>/` in the jar are found automatically ([Getting-Started.md#L48-L56](https://github.com/GTNewHorizons/GuideNH/blob/1.3.55/wiki/Getting-Started.md#L48-L56)). Pages can cover what cities are, the loot bands and their tiers, moving in, and the profile options for server owners. Loading from our jar is *unverified at runtime*.
- **Quest line: BetterQuesting (M).** No pack jar ships its own task type or quest database today; this would be the first.
  - The low-coupling route needs no BetterQuesting code dependency. This mod keeps a scoreboard objective (cities visited, rail dungeons looted), and quests use bq_standard's scoreboard task ([TaskScoreboard.java#L101-L118](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.92-GTNH/src/main/java/bq_standard/tasks/TaskScoreboard.java#L101-L118)).
  - The quest line ships from the jar behind a hidden key, with stable IDs and an idempotent upsert (`in-pack-content-delivery`). Rewards are loot bags of the tiers the pack already uses.
  - bq_standard's location task cannot express "inside a lost city": its `structure` field only checks that such a structure exists somewhere ([TaskLocation.java#L152-L161](https://github.com/GTNewHorizons/BetterQuesting/blob/3.8.92-GTNH/src/main/java/bq_standard/tasks/TaskLocation.java#L152-L161)).

## Loot extensions

- **NEI already shows our loot.**
  - NEI Custom Diagram 1.8.36 builds a diagram for every `ChestGenHooks` category by reading Forge's category map ([ForgeWorldgenLoot.java#L88-L110](https://github.com/GTNewHorizons/nei-custom-diagram/blob/1.8.36/src/main/java/com/github/dcysteine/neicustomdiagram/generators/forge/worldgenloot/ForgeWorldgenLoot.java#L88-L110)), shown by default ([#L56](https://github.com/GTNewHorizons/nei-custom-diagram/blob/1.8.36/src/main/java/com/github/dcysteine/neicustomdiagram/generators/forge/worldgenloot/ForgeWorldgenLoot.java#L56)).
  - The `lostcitiesnh:city_*` categories from D20 appear with no code. A lang key `neicustomdiagram.generators.forge.worldgenloot.forge.loot.<category>` in our own lang file gives each a readable name ([Lang.java#L23](https://github.com/GTNewHorizons/nei-custom-diagram/blob/1.8.36/src/main/java/com/github/dcysteine/neicustomdiagram/main/Lang.java#L23)).
  - Whether the diagram is built after our postInit registration is *unverified* (it reads the live map, so likely yes). Effort S; the lang keys wait until after beta.
- **Lore notes.**
  - An OpenPrinter printed page is an item stack with a title and `line<n>` NBT ([PaperGUI.java#L75](https://github.com/GTNewHorizons/OpenPrinter/blob/0.3.1-GTNH/src/main/java/pcl/openprinter/gui/PaperGUI.java#L75)), so a chest can hold a note naming its city: an eviction notice, a shift log at the rail depot, a library overdue slip.
  - The code is S; the writing is the work. Text comes from our lang files.
  - Vanilla written books are the fallback.
- **Backpacks and travel gear.** Minecraft-Backpack-Mod and AdventureBackpack2 items belong in `city_supplies` as early-game convenience. Their item IDs are *unverified*.
- **Food variety.** SpiceOfLife (carrot edition) rewards eating many different foods. HarvestCraft entries in `city_supplies` should be many single meals rather than stacks of one. Beta balance.
- **Carts.** See [Railways and stations](#railways-and-stations).

## Mobs

Nothing new to integrate. Special Mobs, Infernal Mobs and EnderZoo already vary what city spawners spawn ([loot-and-mobs.md](loot-and-mobs.md)), and Darkerer makes dark interiors matter ([Lighting](#lighting-and-atmosphere)).

- Thaumcraft's wisps and taint, Witchery's mobs and Twilight Forest's mobs belong to their own progressions or dimensions.
- OpenModularTurrets as abandoned defenses would shoot players with no counterplay; no.

## Not recommended or note only

| Mod | Why not, or not yet |
|---|---|
| BiblioCraft and BiblioWoods | All Rights Reserved, no source. Every block keeps its rotation in the TE, so the NBT contract would come from bytecode. After TE templates, possibly bookcases for the library, with a `// Decompiled:` hedge |
| Carpenter's Blocks, LittleTiles, ArchitectureCraft | All shape and material state is TE NBT; nothing a palette can express ([TEBase.java#L33-L56](https://github.com/GTNewHorizons/CarpentersBlocks/blob/3.7.3-GTNH/src/main/java/com/carpentersblocks/tileentity/TEBase.java#L33-L56)) |
| ProjectRed fixtures and cage lights | ForgeMultipart parts ([Lighting](#lighting-and-atmosphere)) |
| FloodLights | Dark unless powered or fueled; beam direction in the TE |
| VendingMachine | A GT multiblock whose trades unlock through questbook progress, paid in coins ([MTEVendingMachine.java#L93-L106](https://github.com/GTNewHorizons/VendingMachine/blob/0.4.103/src/main/java/com/cubefury/vendingmachine/blocks/MTEVendingMachine.java#L93-L106)). A working one in a mall would bypass that gating; an empty casing is only decoration |
| Steve's Carts 2 | Modular cart NBT (`Modules`, `Data<i>`); the chest-module inventory format is *unverified*. Vanilla chest carts do the job |
| Forestry and Binnie ExtraTrees wood blocks | Wood type lives in the TE for planks, slabs, fences, stairs and doors. Natura and Biomes O' Plenty woods are plain metadata and are the S4 catalog's choice for extra woods |
| Catwalks | Useful for the oil rig and radio tower, but its source repository is not reachable (the MOD-INDEX link returns 404), so ids come from the jar only. Note for the S4 catalog |
| Thaumcraft, Botania magic blocks, Witchery, Blood Magic | Magic progression, excluded from loot by default (D20). Thaumcraft's paving stone of travel would be a highway speed block, but Chisel's asphalt does the same without magic. Note only |
| Galacticraft, Hardcore Ender Expansion, Twilight Forest content | Other dimensions; this mod is overworld-only (0003). Twilight's firefly jar is the one block worth having, as a light |
| ServerUtilities warps and homes | Off in the pack ([Names, signs and discovery](#names-signs-and-discovery)) |
| IC2 decorative blocks | GT swaps them for its own ([Abandoned industry](#abandoned-industry)) |

## Notes for the coexistence research

Opportunities this survey touched that belong to the structure-coexistence work (D24):

- **Shared village names.** A village sewn into a city could lend the city its VillageNames name, or the city could name the district. Either way the name generator above is the same call.
- **Railcraft workshops.** Railcraft's village workshop is a village piece that places Railcraft tracks ([ComponentWorkshop.java#L216-L230](https://github.com/GTNewHorizons/Railcraft/blob/9.17.36/src/main/java/mods/railcraft/common/worldgen/ComponentWorkshop.java#L216-L230)). A railway that reaches a village could end at its workshop.
- **LootGames dungeons.** A puzzle dungeon the layout leaves room for (T1 in [other-worldgen.md](other-worldgen.md)) could be framed as a city landmark: a plaza with a sign rather than an empty lot.

## What surprised us

- **MalisisDoors restores what D19 gave up.** The pack has every wood door and an iron trapdoor after all. With `modifyVanillaDoors=true`, even vanilla doors in this pack are Malisis blocks with TEs.
- **NEI already displays any `ChestGenHooks` category,** so the D20 loot bands are visible in NEI for the price of a lang key.
- **Railcraft has no abandoned track,** and a Railcraft track placed with `setBlock` alone has no TE. Its own village workshop shows the correct worldgen call.
- **GT5-Unofficial at this tag has no asphalt.** The pack's asphalt is Chisel's, and GT's concrete is the GT block that speeds up walking (1.4×).
- **The pack ships ServerUtilities claims, homes and warps switched off.**
- **The JourneyMap build has no public API.** Navigator, GTNH's own layer library, is how VisualProspecting and ServerUtilities draw on it.
- **No pack mod ships its own BetterQuesting task type or quest line.** A quest line from this jar would be the first.
- **Nights are pitch black** (Darkerer, mode `EVERYWHERE`), which makes upstream's city lights a gameplay feature, not just decoration.
- **VendingMachine is a GT multiblock gated by quest progress,** not a decorative vending machine.
- **Et Futurum Requiem 2.6.65 has no campfire, bell, lectern, grindstone or scaffolding.**
