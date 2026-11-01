# Block catalog: GalaxySpace

GalaxySpace 1.1.144-GTNH, mod ID `GalaxySpace`, jar `GalaxySpace-1.1.144-GTNH.jar`. GTNH ships a private fork; the public checkout in `gtnh-src/source/GalaxySpace/` is the original author's divergent line, so registrations were read from the decompile of the shipped jar (`gtnh-src/mods/GalaxySpace/galaxyspace/core/register/GSBlocks.java`, the per-planet block classes, `ACBlocks`, `BRBlocks`, `TCetiBlocks`, `VegaBlocks`) and English names from the jar's `en_US.lang`. Registry names are the literal strings passed to `GameRegistry.registerBlock` (mixed case, no `tile.` prefix: `GalaxySpace:barnardaCplanks`). Pack config checked: `config/GalaxySpace/core.conf` (all star systems enabled; GalaxySpace's own ore generation off). Contact sheets: `tools/block-contact-sheet/sheets/galaxyspace/metal-blocks.json`, `future-glass.json`, `dungeon-and-light.json`, `planet-stone.json`. This catalog serves D25 in the [decision log](../../../design/decision-log.md): variety per palette role, with the [D19 mapping](../../00-pack-integration/block-mapping.md) as baseline.

## Summary

**108 block+meta entries from 43 registry blocks.** Ores, machines, treasure chests, geysers, crystals, fluids and plants are left out ([Not catalogued](#not-catalogued)). Fit with upstream's look: 13 yes, 43 partly, 52 no. Most of the "no" are planet terrain; GalaxySpace's real contribution is decorative metal, a 17-color glass family, five glowstones and a handful of dungeon bricks.

| Role | Count |
|---|---|
| wall-main | 17 |
| wall-accent | 13 |
| trim-pillar | 1 |
| floor | 5 |
| ceiling | 1 |
| glass | 17 |
| street | 2 |
| sidewalk | 1 |
| rubble-ruin | 3 |
| light | 5 |
| industrial-dressing | 9 |
| foliage | 2 |
| special | 2 |
| off-theme | 50 |

Entries carry several roles, so the column sums to more than 108.

Flags:

- **Tile entities:** none among the catalogued blocks.
- **Light:** the five space glowstones emit 15 (`setLightLevel(1.0F)`, glass material, hardness 1).
- **Gravity:** none.
- **Hard to move:** nothing unusual. Planet blocks have hardness 2 to 3 and harvest levels 1 to 3 (Vega B, Barnarda E and F, Alpha Centauri Bb need level 3, a diamond-level pick).
- **Hazards:** Ceres Web withers and Io Web burns whoever touches it.
- **GT ore hosts, in every dimension including the overworld:** 33 planet stones. GT's `StoneType` registers each GalaxySpace body's main and extra stones (`StoneBuilder.galaxySpace(name, main, extras…)`) without a dimension restriction, and `OreManager` places ore into any stone type valid in the dimension ([StoneType.java#L98-L127](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/StoneType.java#L98-L127), [#L462-L474](https://github.com/GTNewHorizons/GT5-Unofficial/blob/5.09.54.265/src/main/java/gregtech/api/enums/StoneType.java#L462-L474)). They are marked in the planet table and must never be building blocks. The overworld host table in [ore-veins.md](../../00-pack-integration/ore-veins.md) lists only overworld-native stones; these are additional hosts any city could create by placing them. The surface blocks ("grunt", meta 0 or the plain `…grunt` block; except `haumeablocks`, Haumea's only block, which is its main stone), dungeon bricks, dungeon floors and tops, metals, glass and glowstones are not hosts. Nothing here is a GT boulder host. GalacticGreg (inside GT5-Unofficial) generates the space ores into these stones; its own stone blocks are ore hosts by definition and are not catalogued.
- **Tier:** nothing machine-like. The decorative metal blocks are named after space-tier materials (adamantite, mithril, oriharukon, platinum, tungsten) but contain no material: they are crafted decoration and carry no value, so the EV ceiling of record 0004 does not apply.

## Decorative metal blocks

Sheet `metal-blocks.json`. All ten share one pattern: a chevron-plated panel with a quartered top, tinted per metal. Hardness 2, harvest level 2.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `GalaxySpace:metalsblock:0` | Decorative Lead Block | wall-accent, industrial-dressing | partly: busy sci-fi plating | industrial, space | Blue-gray |
| `GalaxySpace:metalsblock:1` | Decorative Adamantite Block | wall-accent, industrial-dressing | partly | industrial, space | Olive |
| `GalaxySpace:metalsblock:2` | Decorative Cobalt Block | wall-accent, industrial-dressing | partly | industrial, space | Teal |
| `GalaxySpace:metalsblock:3` | Decorative Magnesium Block | wall-accent, industrial-dressing | partly | industrial, space | Mauve |
| `GalaxySpace:metalsblock:4` | Decorative Mithril Block | off-theme | no: pink | space | |
| `GalaxySpace:metalsblock:5` | Decorative Nickel Block | wall-accent, industrial-dressing | partly | industrial, space | Khaki |
| `GalaxySpace:metalsblock:6` | Decorative Oriharukon Block | wall-accent, industrial-dressing | partly | industrial, space | Pale teal |
| `GalaxySpace:metalsblock:7` | Decorative Platinum Block | wall-accent, industrial-dressing | partly | industrial, space | Yellow-olive |
| `GalaxySpace:metalsblock:8` | Decorative Tungsten Block | wall-accent, industrial-dressing | partly: the most neutral of the set | industrial, space | Gray |
| `GalaxySpace:metalsblock:9` | Decorative Copper Block | wall-accent, industrial-dressing | partly | industrial, space | Copper |

## Future glass

Sheet `future-glass.json`. A hexagon-leaded glass, translucent (render pass 1), hardness 2, `blockGlass` in the ore dictionary. **Metadata is not dye order:** 0 is black and 15 is white, the reverse of vanilla stained glass.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `GalaxySpace:futureglass` | Future Glass | glass | yes: clear glass with a dark hex lead, a modern curtain-wall look | urban, industrial, space | |
| `GalaxySpace:futureglasses:0` | Black Future Glass | glass | yes: dark tinted glass, like upstream's oil-rig black glass | urban, industrial | |
| `GalaxySpace:futureglasses:1` | Red Future Glass | glass | partly: strong color | space | |
| `GalaxySpace:futureglasses:2` | Green Future Glass | glass | partly | space | |
| `GalaxySpace:futureglasses:3` | Brown Future Glass | glass | partly: smoked glass | urban | |
| `GalaxySpace:futureglasses:4` | Blue Future Glass | glass | yes: matches upstream's blue glass palette | urban | |
| `GalaxySpace:futureglasses:5` | Purple Future Glass | glass | partly | space | |
| `GalaxySpace:futureglasses:6` | Cyan Future Glass | glass | yes: a pale blue-gray close to upstream's light-blue glass | urban | Renders paler than vanilla cyan |
| `GalaxySpace:futureglasses:7` | Light Gray Future Glass | glass | yes | urban | |
| `GalaxySpace:futureglasses:8` | Gray Future Glass | glass | yes: matches upstream's gray glass | urban | |
| `GalaxySpace:futureglasses:9` | Pink Future Glass | glass | partly | space | |
| `GalaxySpace:futureglasses:10` | Lime Future Glass | glass | partly | space | |
| `GalaxySpace:futureglasses:11` | Yellow Future Glass | glass | partly | space | |
| `GalaxySpace:futureglasses:12` | Light Blue Future Glass | glass | yes: matches upstream's light-blue glass | urban | |
| `GalaxySpace:futureglasses:13` | Magenta Future Glass | glass | partly | space | |
| `GalaxySpace:futureglasses:14` | Orange Future Glass | glass | partly | space | |
| `GalaxySpace:futureglasses:15` | White Future Glass | glass | yes: matches upstream's white glass (`Y`) | urban | |

## Dungeon bricks, glowstones and Barnarda C wood

Sheet `dungeon-and-light.json`.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `GalaxySpace:ceresblocks:2` | Ceres Dungeon Bricks | wall-main, wall-accent | partly: pale teal-gray brick | urban, space | Not a host. Hardness 3 |
| `GalaxySpace:ceresblocks:4` | Ceres Dungeon Floor | floor | partly: teal tile with dark grout | industrial, space | Not a host |
| `GalaxySpace:ceresblocks:5` | Ceres Dungeon Top | floor, ceiling | partly: dark slate tile | urban, industrial | Not a host |
| `GalaxySpace:ioblocks:3` | Io Dungeon Bricks | off-theme | no: black brick with glowing orange mortar | space | Not a host. No light emission |
| `GalaxySpace:ioblocks:7` | Io Dungeon Top | wall-main, floor | yes: red-brown square tile, reads as brick or quarry tile | urban, desert | Not a host |
| `GalaxySpace:ioblocks:8` | Io Dungeon Floor | floor | yes: brown paneled floor, reads as wood parquet | urban | Not a host. Stone material and sound despite the look |
| `GalaxySpace:enceladusblocks:2` | Enceladus Dungeon Bricks | wall-main, wall-accent | yes: white brick with pale blue mortar, like glazed or painted brick | urban | Not a host. Snow step sound, hardness 0.5 |
| `GalaxySpace:proteusblocks:3` | Proteus Dungeon Bricks | wall-main, rubble-ruin | yes: large dark-gray bricks | urban, ruined | Not a host. Hardness 3 |
| `GalaxySpace:plutoblocks:6` | Pluto Dungeon Bricks | wall-accent | partly: dark red brick with blue mortar | urban, space | Not a host |
| `GalaxySpace:ceresglowstone` | Ceres Glowstone | light | partly: teal glowstone | space | Light 15 |
| `GalaxySpace:ioglowstone` | Io Glowstone | light | partly: red-orange glowstone | space | Light 15 |
| `GalaxySpace:enceladusglowstone` | Enceladus Glowstone | light | partly: icy blue-white glowstone | urban, space | Light 15 |
| `GalaxySpace:proteusglowstone` | Proteus Glowstone | light | yes: black-and-white speckle, a neutral lamp block | urban, industrial | Light 15 |
| `GalaxySpace:plutoglowstone` | Pluto Glowstone | light | partly: orange and blue | space | Light 15 |
| `GalaxySpace:barnardaClog` | Barnarda C Wood | trim-pillar, wall-accent | partly: dark navy log | space | `logWood`. Metadata carries the log axis |
| `GalaxySpace:barnardaCplanks` | Barnarda C Wood Planks | floor, wall-main | partly: navy planks, a dark-stained wood | urban, space | `plankWood`, axe |
| `GalaxySpace:barnardaCleaves` | Barnarda C Leaves | foliage | no: violet | space | Leaves; may decay away from a log |
| `GalaxySpace:barnardaCgrass` | Barnarda C Grass | foliage | no: purple turf | space | |
| `GalaxySpace:barnardaCdirt` | Barnarda C Dirt | off-theme | no | space | |
| `GalaxySpace:ceresweb` | Ceres Web | rubble-ruin, special | partly: a cobweb that withers | ruined | Applies wither for 80 ticks on contact |
| `GalaxySpace:ioweb` | Io Web | rubble-ruin, special | partly: a red cobweb that burns | ruined | Fire damage on contact |

## Planet and moon terrain

Sheet `planet-stone.json`. Every entry is a body's surface, subsurface or stone block, hardness 2 to 3. "Surface" blocks (except Haumea's) are not GT hosts and a few have flat colors usable as plain stone; everything marked as a host must never be placed.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `GalaxySpace:mercuryblocks:0` | Mercury Surface Block | wall-main | partly: flat sandy tan, a sandstone stand-in | desert, space | Not a host. |
| `GalaxySpace:mercuryblocks:1` | Mercury Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:mercuryblocks:2` | Mercury Core | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:venusblocks:0` | Venus Surface Block | off-theme | no: near-black, dull | space | Not a host. |
| `GalaxySpace:venusblocks:1` | Venus Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:ceresblocks:0` | Ceres Surface Block | off-theme | no: mottled blue-gray | space | Not a host. |
| `GalaxySpace:ceresblocks:1` | Ceres Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:phobosblocks:0` | Phobos Surface Block | wall-main | partly: flat gray-brown stone | urban, space | Not a host. |
| `GalaxySpace:phobosblocks:1` | Phobos Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:phobosblocks:2` | Phobos Stone | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:deimosblocks:0` | Deimos Surface Block | off-theme | no: mottled pale stone | space | Not a host. |
| `GalaxySpace:deimosblocks:1` | Deimos Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:ioblocks:0` | Io Surface Block | wall-main | partly: pale yellow, reads as sandstone | desert, space | Not a host. |
| `GalaxySpace:ioblocks:1` | Io Ash | off-theme | no: black ash | space | Not a host. |
| `GalaxySpace:ioblocks:2` | Io Stone | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:iodualstone` | Io Dual Surface Block | off-theme | no: split black and yellow faces | space | Not a host. Faces differ by side |
| `GalaxySpace:europagrunt:0` | Europa Surface Block | off-theme | no: mottled tan | space | Not a host. |
| `GalaxySpace:europagrunt:1` | Europa Ice | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:ganymedeblocks:0` | Ganymede Surface Block | wall-main | partly: flat gray-tan stone | urban, space | Not a host. |
| `GalaxySpace:ganymedeblocks:1` | Ganymede Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:callistoblocks:0` | Callisto Surface Block | street | partly: flat black, an asphalt stand-in | urban, space | Not a host. |
| `GalaxySpace:callistoblocks:1` | Callisto Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:enceladusblocks:0` | Enceladus Snow | off-theme | no: snow | space | Not a host. Snow step sound, hardness 0.5 |
| `GalaxySpace:enceladusblocks:1` | Enceladus Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:enceladusblocks:3` | Enceladus Ice | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:titanblocks:0` | Titan Surface Block | off-theme | no: saturated orange | space | Not a host. |
| `GalaxySpace:titanblocks:1` | Titan Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:titanblocks:2` | Titan Stone | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:mirandablocks:0` | Miranda Surface Block | wall-main | partly: flat dark brown | urban, space | Not a host. |
| `GalaxySpace:mirandablocks:1` | Miranda Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:mirandablocks:2` | Miranda Stone | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:oberonblocks:0` | Oberon Surface Block | wall-main | partly: flat terracotta brown | urban, desert, space | Not a host. A brown terracotta look that is not a GT boulder host |
| `GalaxySpace:oberonblocks:1` | Oberon Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:oberonblocks:2` | Oberon Stone | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:proteusblocks:0` | Proteus Surface Block | wall-main, street | partly: flat near-black | urban, space | Not a host. |
| `GalaxySpace:proteusblocks:1` | Proteus Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:proteusblocks:2` | Proteus Stone | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:tritonblocks:0` | Triton Surface Block | wall-main, sidewalk | partly: flat light gray, reads as concrete | urban, space | Not a host. |
| `GalaxySpace:tritonblocks:1` | Triton Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:tritonblocks:2` | Triton Stone | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:plutoblocks:0` | Pluto Surface Ice | wall-main | partly: flat tan | desert, space | Not a host. |
| `GalaxySpace:plutoblocks:1` | Pluto Nitrogen Ice | wall-main | partly: flat cream, reads as plaster or limestone | urban, space | Not a host. |
| `GalaxySpace:plutoblocks:2` | Pluto Tholin Ice | off-theme | no: streaked white | space | Not a host. |
| `GalaxySpace:plutoblocks:3` | Pluto Methane Ice | off-theme | no: streaked orange | space | Not a host. |
| `GalaxySpace:plutoblocks:4` | Pluto Subsurface Ice | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:plutoblocks:5` | Pluto Stone | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:haumeablocks` | Haumea Surface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:makemakegrunt:0` | Makemake Surface Block | off-theme | no: dark red-brown | space | Not a host. |
| `GalaxySpace:makemakegrunt:1` | Makemake Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:acentauribbgrunt` | Alpha Centauri Bb Surface Block | off-theme | no: lava-veined | space | Not a host. |
| `GalaxySpace:acentauribbsubgrunt` | Alpha Centauri Bb Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:barnardaEgrunt` | Barnarda E Surface Block | off-theme | no: rusty | space | Not a host. |
| `GalaxySpace:barnardaEsubgrunt` | Barnarda E Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:barnardaFgrunt` | Barnarda F Surface Block | wall-main | partly: flat orange-tan | desert, space | Not a host. |
| `GalaxySpace:barnardaFsubgrunt` | Barnarda F Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:vegabgrunt` | Vega B Surface Block | wall-main | partly: flat slate blue | urban, space | Not a host. |
| `GalaxySpace:vegabsubgrunt` | Vega B Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:tcetieblocks:0` | T Ceti E Surface Block | off-theme | no: mottled | space | Not a host. |
| `GalaxySpace:tcetieblocks:1` | T Ceti E Subsurface Block | off-theme | no | space | **GT ore host anywhere** |
| `GalaxySpace:tcetieblocks:2` | T Ceti E Stone Block | off-theme | no | space | **GT ore host anywhere** |

## Not catalogued

- Ores: `mercuryblocks:3`–`:5`, `venusblocks:2`–`:3`, `leadore`, `ceresblocks:3`, `phobosblocks:3`–`:4`, `deimosblocks:2`–`:3`, `ioblocks:4`–`:6`, `ganymedeblocks:2`–`:3`, `mirandablocks:3`, `oberonblocks:3`, `proteusblocks:4`–`:5`, `tritonblocks:3`, `barnardaEoreiron`, `barnardaEoregold`. GalaxySpace's ore generation is off in the pack (`enableOresGeneration=false`).
- Machines (`geothermalgenerator`, `fuelgenerator`, `solarwindpanel`, `assemblymachine`, `convertersurface`, `storagemoduleT3`, `oxstoragemoduleT2`, `solarPanel`), treasure chests T4 to T8, `europageyser`, `europaunderwatergeyser`, `enceladuscrystal`, `barnardaCcrystal` (tile entities), saplings, dandelions, reeds, seaweed, liquid methane, biofuel and unknown water.
- The `futurelamp` and `duraluminumWire` textures in the jar have no registered block in this version.

## Best variety pools

| Upstream role or palette family | GalaxySpace candidates |
|---|---|
| `bricks_standard` `#` main wall | `proteusblocks:3` (dark brick), `enceladusblocks:2` (white brick), `ioblocks:7` (red quarry tile) |
| `X` accent | `ceresblocks:2`, `plutoblocks:6`, `metalsblock:8` (industrial) |
| `}` rubble | `proteusblocks:3`, `ceresweb`, `ioweb` (ruined variants only, they hurt) |
| `bricks_cyan`, `bricks_gray`, `bricks_silver` (terracotta colors) | `oberonblocks:0` (brown), `tritonblocks:0` (light gray), `vegabgrunt` (slate blue), `phobosblocks:0` (gray-brown): flat colors that are neither ore nor boulder hosts |
| `glass_*` (`+`, `a`) | `futureglass`, `futureglasses:15` (white), `:8` (gray), `:12` (light blue), `:4` (blue), `:0` (black) |
| `Z` city-sphere glass | `futureglasses:4`, `:6`, `:8`, `:12` |
| Floors and ceilings | `ioblocks:8` (parquet look), `ceresblocks:5`, `barnardaCplanks` |
| Street `S` / highway `R` | `callistoblocks:0`, `proteusblocks:0` (flat black) |
| Light `h` | `proteusglowstone` (neutral), `enceladusglowstone` |
| Oil rig | `futureglasses:0` for `I` (black glass), `metalsblock:8` |
| Desert bricks | `ioblocks:0`, `mercuryblocks:0`, `barnardaFgrunt`, `ioblocks:7` |
