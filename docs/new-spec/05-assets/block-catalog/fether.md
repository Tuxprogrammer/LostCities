# Fether

Family D. Mod ID `fether`, jar `fether-2.0.9.jar`. "Food from the Nether": a GTNH port of Pam's Harvest the Nether (`mcmod.info`). Source read: `mods/fether/glowredman/fether/FetherBlocks.java` (lines 39-80), `.../blocks/BlockNetherLog.java`, `.../blocks/BlockGlowFlower.java`, `config/fether.cfg`. Sheets: `tools/block-contact-sheet/sheets/fether/` (previews in `docs/previews/blocks/fether/`, local only).

## Summary

- **Counts.** 12 registry names (`fether:nether_log`, `nether_leaves`, `nether_planks`, `nether_sapling`, `nether_bed`, `nether_garden`, `ignis_fruit`, `glow_flower`, and 4 crops). About 30 block+meta entries counting crop stages and log axes. **Useful: 3** (planks, stripped log, glow flower), all as accents.
- **Look.** Crimson-magenta wood with dark red bark, like 1.16's crimson stems. Strongly colored, so an accent at most in a 20th-century city.
- **Caveats.** No TEs. The glow flower and glow-flower crop emit light 15 (`setLightLevel(1.0F)`). Log meta bit 1 is the "infested" variant, which has special drops (`BlockNetherLog.getDrops`); use metas 0 and 2 only. Nether gardens spread on random ticks (`gardenSpreadRate=100`).
- **Optional dependency.** A server without Fether loses these. Fallbacks: planks to `minecraft:planks:5` (dark oak), log to `minecraft:log2:1`, glow flower to `minecraft:red_flower`.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `fether:nether_planks:0` | Nether Planks | `floor`, `wall-accent` | partly | `interior`, `fantasy` | Deep crimson planks. A mahogany-like floor if used sparingly |
| `fether:nether_log:0` (+axis bits) | Nether Log | `trim-pillar` | partly | `fantasy` | Dark red bark |
| `fether:nether_log:2` (+axis bits) | Stripped Nether Log | `trim-pillar` | partly | `interior` | Smooth crimson |
| `fether:nether_log:1`, `:3` | Infested Nether Log (and stripped) | none | no | | Special drops; avoid |
| `fether:nether_leaves:0` | Nether Leaves | `foliage` | no | `fantasy` | Dark maroon |
| `fether:glow_flower:0` | Glowflower | `foliage`, `light` | partly | `park-garden` | Cross plant, light 15. A glowing flowerbed for parks at night |
| `fether:nether_sapling:0`, `nether_garden:0`, `ignis_fruit`, crops | Plants | `foliage` | no | `fantasy` | Nether-themed; gardens spread |
| `fether:nether_bed` | Nether Bed | `furniture-deco` | partly | `interior` | Two-block bed (head and foot metas) in purple-gray; no item block of its own |

## Best variety pools

- **Accent wood (sparingly):** `fether:nether_planks:0`.
- **Park lighting plants:** `fether:glow_flower:0`.

## Worldgen notes

- Plain blocks. Keep the infested log metas and spreading gardens out.
