# Pack-side changes

<!-- Every change this mod needs outside its own jar.
     Paths are relative to the pack's .minecraft/ folder; take them from the real pack's config/, not the other mod's defaults.
     This mod never writes another mod's config at runtime. -->

Target pack version: GTNH 2.9.0 ([0002](design/0002-target-pack.md)).

This mod is standalone: nothing in it depends on a change to the pack. This list records what a server owner may want to change, and what the pack would change if it ever adopted the mod.

| File | Change | Why | Delivery | Spec sheet |
|---|---|---|---|---|
| `config/TooMuchLoot/loot/<name>.xml`, one per category (`lostcitiesnh:city_supplies`, `city_lv`, `city_mv`, `city_hv`, `city_ev`, `rail_dungeon`) | Optional: a `LootGroup` with `category="lostcitiesnh:city_lv"` and `loading_mode="OVERRIDE"` (or `ADD`, `REMOVE`) retunes that category's contents | City loot is this mod's `ChestGenHooks` categories, filled from built-in defaults; TooMuchLoot is how the pack already tunes loot, so this mod adds no loot config of its own. The file applies at server start: chests generated before it, the spawn area of a new world, keep the defaults. An override is not checked against the EV ceiling | Documented server-owner step | [loot-and-mobs.md](new-spec/00-pack-integration/loot-and-mobs.md) |

## Pack-wide issues found

Shipped values that break something for everyone, not just this mod. Report separately.

| File | Value | Effect |
|---|---|---|

None yet.
