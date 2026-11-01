# Mine & Blade Battlegear 2 (flag pole)

Family D. Mod ID `battlegear2`, jar `battlegear2-1.6.8-backhand.jar`. Source read: `mods/battlegear2/mods/battlegear2/utils/BattlegearConfig.java` (L60-L61, L198-L200), `heraldry/BlockFlagPole.java` (L52-L56, L60, L142-L146), `heraldry/TileEntityFlagPole.java` (L82, L128); pack config `config/battlegear2.cfg` (no items disabled). Sheets: `tools/block-contact-sheet/sheets/battlegear2/flagpole.json` (pole materials only; previews in `docs/previews/blocks/battlegear2/`, local only).

## Summary

- **1 registry name, 7 entries**: `battlegear2:flagpole`, meta 0-3 oak, spruce, birch, jungle (log texture), 4 iron (iron-block texture), 5 acacia, 6 dark oak (`BlockFlagPole.java` L142-L146).
- A **thin pole** (not a full block) with a TE (`TileEntityFlagPole`) that stores `orientation` (vertical, or sticking out of a wall) and a list of flags (heraldry banners). With no flags it is a bare pole.
- **Iron flag poles** on building roofs, plazas, schools and city halls are a strong civic detail; a wall-mounted pole over a shop door also reads well. The heraldry banners are medieval in style, so leave poles bare or check the banner patterns before using them.
- No light, no gravity. **Fallback when the mod is absent**: `minecraft:cobblestone_wall` (iron look: `minecraft:iron_bars`), or air.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `battlegear2:flagpole:4` | Iron Flag Pole | furniture-deco, trim-pillar | yes | urban-stone, modern | stack 3-6 high on roofs and plazas |
| `battlegear2:flagpole:0-3`, `:5-6` | Wooden Flag Poles | furniture-deco | partly | rustic | log-textured poles |

## Best variety pools

- **furniture-deco (civic buildings, rooftops)**: iron flag pole, vertical, 4 high.

## Worldgen notes

- One non-ticking TE per segment (unverified that it never ticks); write NBT `orientation` for wall-mounted poles, nothing for vertical ones (default 0, assumed vertical; unverified).
