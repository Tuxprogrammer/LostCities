# Loot and mobs

How upstream fills chests and spawners, how GTNH 2.9.0 handles chest loot and spawner mobs, and what this mod does with them. Covers open questions Q5 (loot) and Q6 (mobs). Tier rules come from [0004](../../design/0004-gtnh-layer-and-tier-ceiling.md): nothing above **EV**, weighted toward the lower tiers, no balance pass before beta.

Status words follow the [new-spec README](../README.md). *Verified* facts cite a pinned permalink or a pack config path relative to `.minecraft/` in the official GTNH 2.9.0 download. *Unverified* facts must be checked before code.

## Summary

- Upstream has two chest pools (`lostcitychest`, `raildungeonchest`), one chest condition (`chestloot`) and two spawner conditions (`easymobs`, `hardmobs`). Every block that carries loot or a mob is in one palette, `common.json`.
- In the pack, all structure loot goes through Forge's `ChestGenHooks` categories, and **TooMuchLoot replaces the contents of 16 categories from XML at server start**. GT's own loot additions to those categories do not survive it.
- The pack's own tier-labeled loot is **EnhancedLootBags**: one item, `enhancedlootbags:lootbag`, whose metadata is a loot group, with groups named Steam, LV, MV, HV, EV and higher. Quests hand out about 2,000 of them.
- Spawner mobs in the pack are vanilla entity names on a vanilla `mob_spawner`. Special Mobs and Infernal Mobs then transform whatever a spawner spawns, so plain vanilla names already produce GTNH-style mobs.
- Two pack settings matter for "livable" buildings: GT makes mob spawners hardness 500 and blast resistance 6,000,000, and EnderIO drops a Broken Spawner every time one is broken.

## Proposed answers to Q5 and Q6

| Question | Proposed answer | Confidence |
|---|---|---|
| **Q5** Loot | Register our own `ChestGenHooks` categories, one per tier band (Steam, LV, MV, HV, EV) plus a rail category, with defaults built from pack items: GT components and materials by tier, EnhancedLootBags bags of the matching tier, pack food and building supplies. Upstream's `chestloot` condition keeps its floor and part tests and chooses among these categories, so deeper cellars and higher floors roll the higher bands with smaller factors. The EV ceiling is enforced by a tier map that the dev self-check reads. Server owners retune any category with TooMuchLoot XML, without our config. | High for the mechanism (verified); medium for the item categories (owner and beta balance) |
| **Q6** Mobs | Map upstream's mob IDs to 1.7.10 entity names on vanilla spawners (`Zombie`, `Skeleton`, `Spider`; `Blaze`, `Witch`). Add no pack mobs by default: Special Mobs and Infernal Mobs already vary what vanilla spawners spawn. Offer EnderZoo overworld mobs as an optional `hardmobs` set. Ask the owner whether `Blaze` stays in the overworld. | High for the mechanism (verified); medium for the mob lists |

## Upstream behavior

*Verified* at upstream `1.20` `f18a094d`.

| Upstream asset | What it does | Source |
|---|---|---|
| Palette `common`, char `1` | `minecraft:spawner` with `"mob": "easymobs"` | [palettes/common.json#L3-L7](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/lostcities/palettes/common.json#L3-L7) |
| Palette `common`, char `4` | `minecraft:spawner` with `"mob": "hardmobs"` | [palettes/common.json#L8-L12](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/lostcities/palettes/common.json#L8-L12) |
| Palette `common`, char `C` | `minecraft:chest[facing=north]` with `"loot": "chestloot"` | [palettes/common.json#L213-L217](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/lostcities/palettes/common.json#L213-L217) |
| Palette `default`, char `;` | `minecraft:furnace` with a block-entity `tag` holding 10 coal | [palettes/default.json#L8-L21](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/lostcities/palettes/default.json#L8-L21) |
| Condition `easymobs` | `minecraft:zombie`, `minecraft:skeleton`, `minecraft:spider`, factor 1 each | [conditions/easymobs.json](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/lostcities/conditions/easymobs.json) |
| Condition `hardmobs` | `minecraft:blaze`, `minecraft:witch`, factor 1 each | [conditions/hardmobs.json](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/lostcities/conditions/hardmobs.json) |
| Condition `chestloot` | `lostcities:chests/lostcitychest` factor 8 on floors 4 to 100 and on floors -100 to -3; `raildungeonchest` factor 20 in parts `rail_dungeon1` and `rail_dungeon2`; `simple_dungeon` factor 30; 32 other vanilla tables factor 1 | [conditions/chestloot.json](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/lostcities/conditions/chestloot.json) |
| Loot table `lostcitychest` | 2 to 4 rolls of diamond sword (weight 2), gold ingot, diamond, emerald (weight 5 each) | [loot_tables/chests/lostcitychest.json](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/loot_tables/chests/lostcitychest.json) |
| Loot table `raildungeonchest` | 2 to 4 rolls of diamond axe (weight 2), gold ingot, iron ingot, diamond, emerald (weight 5 each) | [loot_tables/chests/raildungeonchest.json](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/resources/data/lostcities/loot_tables/chests/raildungeonchest.json) |

How the code uses them:

- A condition `range` tests the building **floor** (0 is the ground floor, negative is a cellar), both ends inclusive ([ConditionContext.java#L252-L254](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/lost/cityassets/ConditionContext.java#L252-L254)).
- Spawners: when the profile's `generateSpawners` is on and the building has loot, the mob is drawn from the condition and written as spawner block-entity data at generation time; otherwise the spawner becomes air ([LostCityTerrainFeature.java#L1925-L1946](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java#L1925-L1946), [#L2012-L2036](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java#L2012-L2036)).
- Chests: a post-generation step sets a **loot table reference**, so contents are rolled when a player first opens the chest. `chestWithoutLootChance` (0.2) leaves a chest empty; `buildingWithoutLootChance` (0.2) removes all loot and spawners from a building ([#L1948-L1958](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java#L1948-L1959), [#L2049-L2080](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/worldgen/LostCityTerrainFeature.java#L2049-L2080), [LostCityProfile.java#L180-L181](https://github.com/McJtyMods/LostCities/blob/f18a094d/src/main/java/mcjty/lostcities/config/LostCityProfile.java#L180-L181)).

1.7.10 has no loot tables. Chests are filled when generated, so the lazy roll-on-open behavior is a deviation unless this mod rebuilds it (see [Fill timing](#fill-timing)).

## Chest loot in the pack

### ChestGenHooks

*Verified.* Forge 1.7.10 keeps structure loot in named `ChestGenHooks` categories. `getInfo(category)` creates an empty category when it does not exist, so any mod can register its own ([ChestGenHooks.java#L24-L33, #L89-L94](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/src/main/java/net/minecraftforge/common/ChestGenHooks.java#L24-L94), Forge `1.7.10` branch head). A generator fills a chest with `WeightedRandomChestContent.generateChestContents(rand, ChestGenHooks.getItems(category, rand), inventory, ChestGenHooks.getCount(category, rand))`; bartworks does exactly that ([MapGenRuins.java#L310-L314](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/bartworks/system/worldgen/MapGenRuins.java#L310-L314)).

### TooMuchLoot rewrites the categories

*Verified.* TooMuchLoot 4.3.0-GTNH reads every XML file in `config/TooMuchLoot/loot/` at `FMLServerStartingEvent`. A file in `OVERRIDE` mode replaces the whole category with its list; `ADD` and `REMOVE` edit a category item by item ([TooMuchLoot.java#L71-L105](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/TooMuchLoot.java#L71-L105), [ChestLootLoader.java#L117-L175](https://github.com/GTNewHorizons/TooMuchLoot/blob/4.3.0-GTNH/src/main/java/dmillerw/tml/data/chest/ChestLootLoader.java#L117-L175)).

The pack ships 16 files, all `OVERRIDE` (`config/TooMuchLoot/loot/*.xml`):

| Category | Stacks per chest | What the pack put in it | Used by |
|---|---|---|---|
| `dungeonChest` | 8 | Vanilla dungeon loot, Railcraft steel gear, Blood Magic catalysts, HEE notes, a Tinkers' heart canister, OpenComputers parts, EMT Tainted Mjolnir | Vanilla dungeons; Roguelike Dungeons (*unverified* which levels) |
| `mineshaftCorridor` | 4 to 9 | Mineshaft and early-game supplies | Mineshafts, GalacticGreg space loot |
| `strongholdCorridor`, `strongholdCrossing`, `strongholdLibrary` | 2 to 4, 1 to 5, 1 to 5 | Stronghold loot | Strongholds |
| `villageBlacksmith` | 4 to 11 | Blacksmith loot | Villages |
| `pyramidDesertyChest`, `pyramidJungleChest`, `pyramidJungleDispenser` | 2 to 7, 2 to 7, 2 | Temple loot | Temples; bartworks ruins on Ross128b |
| `bonusChest` | 10 | Bonus-chest supplies | World creation |
| `naturalistChest` | 0 | Empty | Forestry |
| `railcraft:workshop` | 10 to 16 | Railcraft tools, rail parts, carts, coke, creosote | Railcraft's village workshop |
| `chest1` to `chest4` | 2 to 4 … 5 to 10 | Graded technical loot: `chest1` rails, ladders, GT small dusts and nuggets; `chest2` GT basic machine blocks and Railcraft machines; `chest3` LV GT components (motors 32600, pumps 32610, conveyors 32630 …); `chest4` MV GT components (32601, 32611, 32631, 32641), EnderIO conduits, Forestry chipsets | LootGames stages 1 to 4 (`config/lootgames/rewards.cfg`, `default_loot_table=chest1` … `chest4`) |

Consequences:

- **The pack decides what a category contains, not the mod that registered it.** GT adds silver, lead, steel, bronze and other ingots to `dungeonChest` and other categories in postInit ([BookAndLootLoader.java#L20-L95](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/postload/BookAndLootLoader.java#L20-L95)); in the pack these additions are replaced unless the XML repeats them.
- **None of the vanilla categories is tier-graded.** `dungeonChest` mixes trivial items with strong ones (a 25-weight Tainted Mjolnir). Drawing city loot from them would put untiered items in cities.
- `chest1` to `chest4` are the closest thing to graded chest loot the pack has, and their contents stay at MV or below (GT component IDs per [IDMetaItem01.java#L298-L386](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/items/IDMetaItem01.java#L298-L386); item damage is 32000 plus the ID). They belong to LootGames, and the pack can change them for LootGames' sake.

### Spawn-area chunks generate before TooMuchLoot loads

*Verified.* On a dedicated server, the worlds load, and the spawn area generates, before `handleServerStarting` fires ([DedicatedServer.java.patch#L35-L48](https://github.com/MinecraftForge/MinecraftForge/blob/9274e4fe435cb415099a8216c1b42235f185443e/fml/patches/minecraft/net/minecraft/server/dedicated/DedicatedServer.java.patch#L35-L48): `func_71247_a` is `loadAllWorlds`). TooMuchLoot applies its XML in `FMLServerStartingEvent`, so chests generated in the spawn area of a new world are filled from the categories as mods registered them, not as the pack configured them. That matters for a city at spawn (Q9).

### EnhancedLootBags

*Verified.*

- One item, `enhancedlootbags:lootbag`, registered in [LootGroupsHandler.java#L308-L309](https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/core/LootGroupsHandler.java#L308-L309). The item damage is the loot group ID, and the contents are rolled on the server when the player opens the bag ([ItemLootBag.java#L121-L134](https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/core/items/ItemLootBag.java#L121-L134)).
- Some drops have a per-player limit, tracked per player and group ([LootGroupsHandler.java#L92-L142](https://github.com/GTNewHorizons/EnhancedLootBags/blob/1.3.5/src/main/java/eu/usrv/enhancedlootbags/core/LootGroupsHandler.java#L92-L142)). Extra bags found in cities share the counters with quest bags; once a limited drop is used up, a bag rolls the group's other drops.
- The groups are defined in `config/EnhancedLootBags/LootBags.xml`. The ones relevant to cities:

| Group ID (item damage) | Group | Tier label | Allowed in the overworld |
|---|---|---|---|
| 0 | trash | none | Yes |
| 1 | basic_stone_age | Stone | Yes |
| 2 | steam_age | Steam | Yes |
| 4 | tier_1_lv | LV | Yes |
| 5 | tier_2_mv | MV | Yes |
| 6 | tier_3_hv | HV | Yes |
| 7 | tier_4_ev | EV | Yes, rarely |
| 8, 41 to 44 | tier_5_iv, tier_6_luv … tier_9_uhv | IV and above | **No** |
| 28 to 31 | fast_food, slow_food, haute_cuisine, dessert | food, rising rarity | Yes |
| 32 to 34 | transportation_3_class … transportation_1_class | rail, rising rarity | Yes (rail dungeons, stations) |
| 35, 36, 51 | seeds, gardens, crops_farmhand | farming | Yes |
| 45, 46 | computer_floppies, tier_1_computers | OpenComputers | Yes |
| 9 to 14, 16 to 18, 52 to 56 | magic, Blood Magic, Witchery | magic progression | Owner's call |
| 22 to 24, 57, 58 | space, Hardcore Ender Expansion | other dimensions | No |

- Quests reward bags heavily: about 2,000 bag rewards in `config/betterquesting/DefaultQuests/Quests/`, most often groups 4 to 8 (LV to IV). Bags are the pack's standard currency for "some tiered loot".
- A bag's tier is the pack's label. The EV group includes Quantum armor parts, which players otherwise reach later; that is the pack's judgment, and the tier map follows the label.

### Other loot sources

| Source | Finding | Status |
|---|---|---|
| NewHorizonsCoreMod 2.9.99 | No `ChestGenHooks` use found by code search; `config/GTNewHorizons/dreamcraft.cfg` has no loot settings. It owns the loot bag group names (`dreamcraft.lootbag.group.*`) and mob drops (`config/GTNewHorizons/CustomDrops.xml`). | *Verified* config; code search covers the default branch only |
| BetterQuesting 3.8.92-GTNH | Quest rewards are `bq_standard:item` (4,267), `choice` (977), `questcompletion` (333) and `xp` (2). No quest uses loot-chest rewards. | *Verified* (`config/betterquesting/DefaultQuests/`) |
| LootGames 2.2.14 | Puzzle rewards come from TooMuchLoot `chest1` to `chest4`. | *Verified* (`config/lootgames/rewards.cfg`) |
| Roguelike Dungeons 1.6.6-GTNH | Generates in dimension 0 only (`dimensionWL=0`), with its own JSON loot settings in `config/roguelike_dungeons/settings/`. | *Verified* config |
| bartworks ruins | Places GT machines up to a configured tier and a `pyramidJungleChest` chest, but only on Ross128b, and seeds its tier from `SecureRandom` ([MapGenRuins.java#L202-L228](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/bartworks/system/worldgen/MapGenRuins.java#L202-L228)). A precedent for tier-capped machines in ruins; its non-seeded randomness is not one to copy. | *Verified* |

## Proposed loot design (Q5)

### Categories

This mod registers its own categories in postInit, with defaults built from pack items, and fills city chests from them. The bands are tier bands, not balance: what goes in each band follows the tier sources below, and quantities wait for beta.

| Category | Band | Item categories |
|---|---|---|
| `lostcitiesnh:city_supplies` | Stone and Steam | Pack food (Pam's HarvestCraft meals, food loot bags 28 and 29), torches, building blocks, vanilla tools, coal and coke, bronze, iron and wrought iron ingots and plates, steam-age loot bag (2) |
| `lostcitiesnh:city_lv` | LV | LV components (motor, pump, conveyor, piston, robot arm), basic circuits, steel ingots and plates, tin and copper cable, LV loot bag (4), OpenComputers floppies (45) |
| `lostcitiesnh:city_mv` | MV | MV components, good circuits, aluminum ingots and plates, MV loot bag (5) |
| `lostcitiesnh:city_hv` | HV | HV components, advanced circuits, stainless steel, HV loot bag (6) |
| `lostcitiesnh:city_ev` | EV | EV components, EV circuits, titanium, EV loot bag (7); rare |
| `lostcitiesnh:rail_dungeon` | Steam to MV | Railcraft rails, ties, carts and tools (the pack's `railcraft:workshop` list), transportation loot bags (32 to 34) |

Tier sources, best first:

1. **The pack's own labels.** EnhancedLootBags group names (`config/EnhancedLootBags/LootBags.xml`). *Verified.*
2. **GT's tiered items.** Components carry their tier in the name (`Electric_Motor_LV` … `_EV`, [IDMetaItem01.java#L298-L386](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/common/items/IDMetaItem01.java#L298-L386)). Code uses `ItemList` constants, never raw damage values. *Verified.*
3. **Material tiers** (steel LV, aluminum MV, stainless steel HV, titanium EV), from the `gt-first-design` skill. *Unverified* against 2.9.0 progression; the beta pass confirms them.
4. **Circuit ore names by tier** (`circuitBasic` LV, `circuitGood` MV, `circuitAdvanced` HV, `circuitData` EV). *Unverified.*

### How the EV ceiling and the weighting are expressed

- **Conditions choose the band.** Upstream's `chestloot` condition already distinguishes high floors, deep cellars and rail-dungeon parts. Its converted form names our categories instead of 1.20 loot tables: ordinary floors draw mostly `city_supplies` and `city_lv`, higher floors and deeper cellars add `city_mv` and `city_hv`, and `city_ev` appears with the smallest factor. Factor values are beta balance.
- **The ceiling is a check, not a convention.** A tier map (item to tier, loot bag group to tier) lives with the loot defaults. The dev self-check walks every category this mod registers and fails on any item above EV or missing from the map ([0004](../../design/0004-gtnh-layer-and-tier-ceiling.md) consequences).
- **Owner overrides.** A server owner can replace or edit any of our categories with a TooMuchLoot XML file using the same category name. That is TooMuchLoot's job, so this mod adds no loot config of its own.
- **Absent mods.** An item whose mod is absent is skipped with one log line; a band never becomes empty because the GT and vanilla entries always resolve.

### Upstream's vanilla-table entries

Upstream's `chestloot` lists 33 vanilla 1.20 tables, mostly at factor 1. The ones with a 1.7.10 category map to it; the rest have no counterpart and are dropped.

| 1.20 loot table | 1.7.10 category | Note |
|---|---|---|
| `simple_dungeon` (factor 30) | `dungeonChest` | Pack-overridden, untiered. Proposed: replace with `city_supplies` |
| `desert_pyramid` | `pyramidDesertyChest` | |
| `jungle_temple` | `pyramidJungleChest` | |
| `jungle_temple_dispenser` | `pyramidJungleDispenser` | Only fits a dispenser |
| `stronghold_corridor`, `stronghold_crossing`, `stronghold_library` | `strongholdCorridor`, `strongholdCrossing`, `strongholdLibrary` | |
| `spawn_bonus_chest` | `bonusChest` | |
| `village/village_armorer`, `village_toolsmith`, `village_weaponsmith` | `villageBlacksmith` | |
| All other village tables, `buried_treasure`, `end_city_treasure`, `igloo_chest`, `pillager_outpost`, `shipwreck_*`, `underwater_ruin_*`, `woodland_mansion` | none | Dropped |
| `lostcities:chests/lostcitychest` | `lostcitiesnh:city_*` by floor | |
| `lostcities:chests/raildungeonchest` | `lostcitiesnh:rail_dungeon` | |

Proposed default: city chests draw only from this mod's categories, and the vanilla categories are an opt-in profile option, because they are untiered in the pack.

### Fill timing

1.7.10 chests are filled when they are written. Two consequences:

- **Spawn-area chests use registration defaults.** Chests generated before `FMLServerStartingEvent` get this mod's defaults even when a TooMuchLoot file overrides the category. Proposed: accept it and document it; a server owner who overrides our categories should know the spawn area keeps defaults. A deferred fill (record chest positions in world saved data and fill them on first open) would restore upstream's lazy behavior, at the cost of saved-data state; it waits for the owner.
- **Determinism.** Chest contents depend on the pack's loot configuration as well as the seed. Golden-chunk tests compare blocks and the chosen category per chest, not item stacks, or they run with a fixed loot configuration.

### Other block-entity data

Upstream's furnace `tag` names items as strings (`"id": "minecraft:coal"`). A 1.7.10 item stack in NBT stores a numeric `id` (*unverified* here; vanilla 1.7.10 has no public source), so the asset system resolves item names through the registry and builds the stack, never copying the tag as raw NBT.

## Mobs in the pack

### Spawner conventions

- **Vanilla spawner, vanilla names.** Roguelike Dungeons places `minecraft:mob_spawner`, sets the entity with `MobSpawnerBaseLogic.setEntityName(String)` using 1.7.10 entity names (`Zombie`, `Skeleton`, `Spider`, `CaveSpider`, `Blaze`, `Witch`, `Creeper`, `LavaSlime`, `PigZombie` …), and, with `rogueSpawners=true` in the pack, adds a weighted spawn entry with potion effects scaled by dungeon level ([Spawner.java#L32-L63, #L65-L120](https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/worldgen/Spawner.java#L32-L120); `config/roguelike_dungeons/roguelike.cfg`). *Verified.* Its common spawners are zombie, skeleton and spider ([#L32](https://github.com/GTNewHorizons/Roguelike-Dungeons/blob/1.6.6-GTNH/src/main/java/greymerk/roguelike/worldgen/Spawner.java#L32)); whether any overworld room places blaze or witch spawners was not checked (*unverified*).
- **Special Mobs replaces spawner mobs too.** It replaces vanilla mobs on `EntityJoinWorldEvent` in every dimension not blacklisted, with no exception for spawners ([EventHandler.java#L45-L88](https://github.com/GTNewHorizons/SpecialMobs/blob/3.7.10/src/main/java/toast/specialMobs/EventHandler.java#L45-L88)). The pack blacklists no dimension and keeps vanilla at weight 3 against 6 for the special zombies (`config/SpecialMobs.cfg`, `_general`, `zombie_rates`). *Verified.*
- **Infernal Mobs upgrades spawner mobs too.** Every living entity that joins the world has a 1-in-20 chance to become at least elite (`eliteRarity=20`), with no spawner exception; only dimension 50 is blacklisted ([EntityEventHandler.java#L69-L80](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/events/EntityEventHandler.java#L69-L80), [InfernalMobsCore.java#L382-L386](https://github.com/GTNewHorizons/Infernal-Mobs/blob/1.10.7-GTNH/src/main/java/atomicstryker/infernalmobs/common/InfernalMobsCore.java#L382-L386), `config/InfernalMobs.cfg`). *Verified.* Its anti-farm threshold (`mobFarmDamageThreshold=150.0`) applies to spawner farms in cities like anywhere else.
- **Spawners are very hard to break.** GT sets `mob_spawner` hardness to 500 and blast resistance to 6,000,000 when `harderMobSpawner` is on ([GTPreLoad.java#L369-L372](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/preload/GTPreLoad.java#L369-L372)), and the pack turns it on (`config/GregTech/GregTech.cfg`, `harderMobSpawner=true`). *Verified.* A diamond pickaxe still breaks one, in roughly 90 seconds by the vanilla formula (*unverified* estimate). Explosions never remove them.
- **Broken spawners are progression items.** EnderIO drops a Broken Spawner every time a spawner is broken (`config/enderio/EnderIO.cfg`, `brokenSpawnerDropChance=1.0`), and the Soul Binder turns it into a Powered Spawner input (`soulBinderBrokenSpawnerLevels=30`). Every city spawner is therefore a typed Broken Spawner for whoever breaks it. *Verified* config; the Powered Spawner's tier in 2.9.0 is *unverified*.

### Pack mobs that could fill spawners

| Mod | Overworld mobs and 1.7.10 entity names | Fit for city spawners | Status |
|---|---|---|---|
| Vanilla | `Zombie`, `Skeleton`, `Spider`, `CaveSpider`, `Creeper`, `Witch`, `Blaze`, `Silverfish`, `Enderman`, `Slime`, `LavaSlime`, `PigZombie` | Default. Special Mobs and Infernal Mobs add the GTNH variety | *Verified* (names as used in Roguelike's `Spawner.java`) |
| EnderZoo 1.3.6 | `enderzoo.FallenKnight`, `enderzoo.ConcussionCreeper`, `enderzoo.Enderminy`, `enderzoo.WitherWitch`, `enderzoo.DireWolf` spawn naturally in the overworld (`config/enderzoo/SpawnConfig_Core.xml`; all enabled in `config/enderzoo/EnderZoo.cfg`) | Good optional `hardmobs` set: overworld mobs, mid difficulty | *Verified* names for `FallenKnight` ([EntityFallenKnight.java#L39](https://github.com/GTNewHorizons/EnderZoo/blob/1.3.6/src/main/java/crazypants/enderzoo/entity/EntityFallenKnight.java#L39), registered under that name in [EnderZoo.java#L116-L127](https://github.com/GTNewHorizons/EnderZoo/blob/1.3.6/src/main/java/crazypants/enderzoo/EnderZoo.java#L116-L127)); the others from the spawn config |
| Special Mobs 3.7.10 | Variants of vanilla mobs | Not placed directly; they arrive through replacement | *Verified* |
| Infernal Mobs 1.10.7-GTNH | Modifiers on any mob | Not placed directly | *Verified* |
| Thaumcraft 4.2.3.5 | Its own mobs appear around its structures and taint | Not by default: magic progression and taint belong to Thaumcraft | *Unverified* names |
| Witchery 0.24.1 | Ritual and night mobs | Not by default | *Unverified* |
| Twilight Forest 2.7.45 | Twilight Forest dimension mobs | No: not overworld mobs | *Unverified* |
| Hardcore Ender Expansion | End mobs | No: End progression | *Unverified* |

## Proposed mob design (Q6)

| Upstream ID | 1.7.10 spawner entity name | Fallback |
|---|---|---|
| `minecraft:zombie` | `Zombie` | none needed |
| `minecraft:skeleton` | `Skeleton` | none needed |
| `minecraft:spider` | `Spider` | none needed |
| `minecraft:witch` | `Witch` | none needed |
| `minecraft:blaze` | `Blaze` (faithful) or `enderzoo.FallenKnight` (proposed GTNH default) | `Skeleton` when EnderZoo is absent |

- Spawners are vanilla `minecraft:mob_spawner` blocks with the entity name set through `MobSpawnerBaseLogic`, as Roguelike does. No spawner block of our own ([CONTRIBUTING § Loot, spawners and items](../../../CONTRIBUTING.md#gtnh-integration)).
- Conditions keep upstream's structure, so a profile or asset override can add pack mobs by name. A mob whose mod is absent falls back as in the table, and the dev self-check reports it.
- Upstream's `generateSpawners` and `buildingWithoutLootChance` stay; they are how a server owner keeps buildings free of spawners.

## Questions for the owner

| Question | Recommendation | Trade-off |
|---|---|---|
| Do Blaze spawners stay in overworld cities (`hardmobs`)? | Replace `Blaze` with EnderZoo's Fallen Knight by default; keep `Witch`. | Faithfulness against an early overworld source of blaze rods and of Broken Blaze Spawners for EnderIO. |
| Spawners in buildings meant to be lived in: GT makes them hardness 500, and each one breaks into an EnderIO Broken Spawner. Keep upstream's spawner density? | Keep spawners, faithful to upstream, and list "spawner count and mob types" for the beta balance pass. | Players moving in must clear spawners with slow mining or a Dislocation focus, and each city becomes a stock of Broken Spawners. |
| Should city chests ever draw from the vanilla categories (`dungeonChest` and others)? | No by default; an opt-in profile option. | The pack's vanilla categories are untiered and contain strong items; leaving them out loses some of upstream's variety. |
| Fill chests at generation, or defer to first open? | Fill at generation; document that spawn-area chests keep this mod's defaults. | Deferring restores upstream's roll-on-open behavior and honors TooMuchLoot overrides everywhere, but adds per-world saved state. |
| Which loot bag families belong in cities beyond tech, food, rail and farming (magic, Blood Magic, Witchery)? | Tech, food, rail, farming and computers only. | Magic bags would give cities a reason to matter to magic players, but they skip that progression's own gates. |

## Pack changes

None needed. A server owner may retune this mod's categories with TooMuchLoot files in `config/TooMuchLoot/loot/`; that goes in [PACK-CHANGES.md](../../PACK-CHANGES.md) when loot is implemented (M08).
