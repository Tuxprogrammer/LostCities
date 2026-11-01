# FloodLights

Family D. Mod ID `FloodLights`, jar `FloodLights-1.5.6.jar`. Source read: `mods/FloodLights/de/keridos/floodlights/init/ModBlocks.java` (L48-L59), `reference/Names.java`, `block/*.java`, `tileentity/TileEntitySmallFloodlight.java`, `tileentity/TileEntityPhantomLight.java`; pack config `config/FloodLights.cfg` (all lights enabled). Sheets: `tools/block-contact-sheet/sheets/floodlights/lights.json` (previews in `docs/previews/blocks/floodlights/`, local only).

## Summary

- **7 registry names, 8 placeable entries**: `electricFloodlight`, `carbonFloodlight`, `uvFloodlight`, `smallElectricFloodlightMetaBlock` (0 small fluorescent strip, 1 square fluorescent panel), `growLight`, plus the internal `tilePhantomLight` and `tileUVLight`.
- **Every fixture is a TE that is dark unless powered.** Electric lights need RF or EU in their TE storage and drain it each tick (`TileEntitySmallFloodlight.java` L85-L111); the carbon floodlight burns fuel. Light comes from invisible `tilePhantomLight` blocks (light 15) the fixture places in a cone or line; a phantom light removes itself when no source remains (`TileEntityPhantomLight.java` L38-L46). So a generated fixture is a **dead fixture**: the right look for an abandoned city, but not a light source.
- **Look**: white fluorescent ceiling strips and square panels (office and store ceilings), gray floodlight boxes (stadiums, warehouses, building facades).
- **Fallback when the mod is absent**: dead fixtures to air or `minecraft:stone_slab` (top half) as a ceiling patch.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `FloodLights:smallElectricFloodlightMetaBlock:0` | Small Electric Light (fluorescent strip) | ceiling, light | yes | modern, interior | dead unless powered; facing (ceiling, wall, floor) in TE |
| `FloodLights:smallElectricFloodlightMetaBlock:1` | Small Square Electric Light (fluorescent panel) | ceiling, light | yes | modern, interior | the office drop-ceiling panel |
| `FloodLights:electricFloodlight` | Electric Floodlight | light, special | yes | industrial | stadiums, rooftops, warehouse yards; dead unless powered |
| `FloodLights:carbonFloodlight` | Carbon Floodlight | light, special | partly | industrial | fuel-burning |
| `FloodLights:uvFloodlight` | UV Floodlight | off-theme | no | | damages mobs when on |
| `FloodLights:growLight` | Grow Light | special | partly | industrial | greenhouse fixture |
| `FloodLights:tilePhantomLight`, `tileUVLight` | Phantom light blocks | special | no | | internal; self-removing without a source |

## Best variety pools

- **ceiling (offices, stores, hospitals)**: square fluorescent panels on a grid in drop ceilings, strips in corridors; pair with a real light source elsewhere (glowstone behind glass, lit catwalks, BiblioCraft lamps) because these stay dark.
- **special (stadium, rooftops, yards)**: electric floodlights on poles or roof edges.

## Worldgen notes

- One non-trivial TE per fixture (ticks to check power). Unpowered fixtures do almost nothing per tick, but the count adds up: keep to ceiling panels in a few rooms per building.
- Do not try to pre-charge them through NBT for lighting: they drain and go dark, and the phantom blocks they spawn are extra block updates.
- Facing lives in the TE as byte `teDirection` (a `ForgeDirection` ordinal, default south; `TileEntityFL.java` L26, L93-L95, `Names.java` L58), so ceiling-mounted panels need NBT or they face the default direction.
