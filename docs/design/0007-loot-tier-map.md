# 0007 · City loot: tier bands and the tier map

- Status: Accepted
- Gate: M08 exit (loot at EV or below)
- Amends: none; implements [0004](0004-gtnh-layer-and-tier-ceiling.md) §2 and §3 and decision log D20
- Date: 2026-11-02

## Context

[0004](0004-gtnh-layer-and-tier-ceiling.md) sets the ceiling: nothing generated in the overworld is above EV, and the distribution leans to the lower tiers. D20 ([loot-and-mobs.md](../new-spec/00-pack-integration/loot-and-mobs.md)) chose the mechanism: this mod's own `ChestGenHooks` categories, one per tier band, filled from pack items, chosen by upstream's `chestloot` condition, with a tier map the dev self-check reads. Upstream's tables (diamonds, emeralds, diamond tools) are dropped ([08-loot-mobs-blocks](../new-spec/08-loot-mobs-blocks/index.md)).

The facts that shaped the build:

- The pack's own tier labels are EnhancedLootBags' groups (`config/EnhancedLootBags/LootBags.xml`); two groups in the allowed families hold items far above their label (`slow_food` rolls Avaritia's end-game food) or untiered machinery (`transportation_1_class`).
- GT's components are `ItemList` constants whose damage values are internal; GT's materials and circuits are ore-dictionary names (`ingotSteel`, `circuitLV`; GT 5.09.54.265 registers circuits under `OrePrefixes.circuit` with the voltage materials, [LoaderGTOreDictionary.java#L263-L268](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/loaders/preload/LoaderGTOreDictionary.java#L263-L268)).
- Upstream's `chestloot.json` is a bundled file the asset converter copies byte for byte from upstream, with documented text patches.
- TooMuchLoot rewrites categories from XML at server start, after the spawn area of a new world has generated.

## Decision

1. **Categories.** Six `ChestGenHooks` categories, registered at `postInit`, 2 to 4 stacks a chest:

   | Category | Band | Holds |
   |---|---|---|
   | `lostcitiesnh:city_supplies` | Steam | Vanilla supplies and tools, food, tin, copper, bronze, iron, wrought iron, coke; loot bags 1, 2, 28, 35, 36, 51 |
   | `lostcitiesnh:city_lv` | LV | LV components (motor, pump, conveyor, piston, robot arm, sensor, emitter), LV circuits, steel, tin cable; bags 4, 45, 46 |
   | `lostcitiesnh:city_mv` | MV | MV components and circuits, aluminium, copper cable; bag 5 |
   | `lostcitiesnh:city_hv` | HV | HV components and circuits, stainless steel, gold cable; bag 6 |
   | `lostcitiesnh:city_ev` | EV | EV components and circuits, titanium, aluminium cable; bag 7 |
   | `lostcitiesnh:rail_dungeon` | LV | The pack's `railcraft:workshop` list; bags 32, 33 |

2. **One file is the defaults and the tier map.** `data/lostcitiesnh/loot/categories.json` lists every entry with its source (registry name, ore names, `ItemList` constant, loot bag group), stack range, weight and tier. The registered stacks keep their entry's tier; that is the tier map.
3. **The ceiling is checked twice.** A unit test holds the file to its rules: no entry above EV or above its band, only the allowed loot-bag groups at their pack tiers, a vanilla entry in every category. The dev self-check walks every registered category in the running pack and fails on an unmapped item or one above EV.
4. **Weighting.** Upstream's `chestloot` stays upstream's file. The value its floor and part tests pick is converted: `simple_dungeon` (every floor, factor 30) becomes `city_supplies` or `city_lv`, 2 to 1; `lostcitychest` (floors 4 and up, the third cellar down, factor 8) becomes `city_mv`, `city_hv` or `city_ev`, 10 to 5 to 1; `raildungeonchest` becomes `rail_dungeon`. So about one chest in 60 on a high floor is EV, and none on the lower floors.
5. **Vanilla categories** only through the profile option `vanillaLootCategories` (off), filtered before the condition's draw.
6. **Integration without compile dependencies:** `ItemList` by reflection on its constant names; ore names through Forge's ore dictionary, GregTech's stack preferred; registry names through the item registry. An absent entry is skipped with one log line.

## Consequences

- Code: `asset.loot` (pure: tiers, loot-bag groups, the defaults reader), `compat.gregtech.GtItemList`, `worldgen.overlay.CityLoot` (registration, tier map), `worldgen.city.LootRules` (value to band), `core.selfcheck.LootCheck`; `gtnh.layer` checks a fresh city's chests in the real pack.
- Spec: the `chestloot` rows of [08-loot-mobs-blocks](../new-spec/08-loot-mobs-blocks/index.md) and [loot-and-mobs.md](../new-spec/00-pack-integration/loot-and-mobs.md) describe the conversion; [PACK-CHANGES.md](../PACK-CHANGES.md) documents TooMuchLoot overrides as an optional server-owner step.
- Balance: every weight and stack range is on the [beta balance list](../new-spec/beta-balance-list.md).

## Alternatives considered

| Alternative | Why not |
|---|---|
| Convert `chestloot.json` itself to name the bands | A new text patch in the asset converter, which the Assets lane is editing; the conversion in code keeps upstream's file diffable and also converts asset overrides that name upstream's tables |
| Draw from the pack's `chest1` to `chest4` (LootGames' graded loot) | They belong to LootGames and stop at MV; the pack can change them for LootGames' sake |
| Tier map by hand in code, separate from the defaults | Two lists that drift; one file keeps them the same by construction |
| A compile dependency on GT5-Unofficial for `ItemList` | A large dependency for six constant names per band; reflection on the names is as exact |
| Pick a value, then drop it if it cannot be drawn | Leaves chests empty with a share that depends on how many vanilla tables a condition lists |

## Still open

- Weights, stack sizes and the band split (beta balance).
- Whether the high bands should hold more than components and materials (machine hulls, batteries) once the beta review plays through a city.
