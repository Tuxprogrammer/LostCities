# Witchery

Family D. Mod ID `witchery`, jar `witchery-1.7.10-0.24.1.jar` (decompiled; no upstream source). Source read: `mods/witchery/com/emoniph/witchery/WitcheryBlocks.java` (lines 104-285), `util/BlockUtil.java` (registration strips the `witchery:` prefix from the unlocalized name, lines 21-37), block classes in `blocks/` (`BlockShadedGlass`, `BlockStockade`, `BlockWitchWood`, `BlockWitchLog`, `BlockWitchDoor`, `BlockCandelabra`, `BlockGlowGlobe`, `BlockChalice`, `BlockSpanishMoss`, `BlockBramble`, `BlockEmberMoss`, `BlockBloodRose`), names from `assets/witchery/lang/en_US.lang`. Pack config `config/witchery.cfg` disables no decorative block. WitcheryExtras (`WitcheryExtras-1.4.20.jar`) registers no blocks (mixins only). Sheets: `tools/block-contact-sheet/sheets/witchery/` (previews in `docs/previews/blocks/witchery/`, local only).

## Summary

- **About 120 registry names (121 `setUnlocalizedName` calls in `WitcheryBlocks.java`), about 200 block+meta entries; about 30 useful.** Registry names are `witchery:<name>`, the part after the colon in `setUnlocalizedName("witchery:<name>")` (for example `witchery:witchwood`, `witchery:shadedglass`). Two odd ones: Witchery registers its own `witchery:cactus` and `witchery:lilypad`.
- **Useful:** three plank colors (rowan tan-yellow, **alder salmon-red**, hawthorn pale gray) with logs, stairs and slabs; **shaded glass in 16 colors** (a tinted glass that turns opaque when powered: a second stained-glass palette, darker and smokier than vanilla's); snow and perpetual-ice stairs, slabs, fences and gates for cold-biome roofs and railings; stockades (log palisades in nine woods); spanish moss (hanging vine) for overgrowth; glow globe and candelabra lights.
- **Dangers, never place:** wild bramble (hardness 20, damages and **spreads when broken** unless with a golden axe, `BlockBramble.harvestBlock`), void and ender bramble (**teleport** entities), ember moss (sets entities on fire), blood rose (damages), plant mine (`witchery:plantmine`, explodes), beartrap and wolftrap, brew gas and brew liquids, mirrors (teleport), barriers and force blocks, cursed doors, levers, plates and buttons (`witchery:cwoodendoor`, `witchery:clever` and the rest: cursed redstone), `witchery:wallgen` (village-wall generator block), spirit and torment portals.
- **Rowan door** (`witchery:rowanwooddoor`) is a keyed door that drops nothing (`BlockWitchDoor.getDrops`, line 85); the alder door uses vanilla's door texture. Neither is worth placing over a vanilla door.
- **Tile entities:** candelabra, chalice, garlic garland, coffin, statues, altar, kettle, cauldron, oven, distillery, spinning wheel, poppet shelf, dream catcher, crystal ball, brazier, blood rose, area markers. Candelabra and chalice are the only plausible furniture and need only their default tile entity (no NBT).
- **Light:** candelabra 15, glow globe 14, glint weed 14, ember moss 6.
- **Ore rule:** none of these host GT ores or boulders.
- **Without the mod:** every pick needs a vanilla fallback (below).

## Blocks

### Woods

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `witchery:witchwood:0` | Rowan Planks | floor, wall-main | yes | rustic, interior | Tan-yellow, close to birch. Fallback `minecraft:planks:2` |
| `witchery:witchwood:1` | Alder Planks | floor, wall-main | yes | rustic, interior | Salmon-red: a painted-wood look no other plank has. Fallback `minecraft:planks:4` |
| `witchery:witchwood:2` | Hawthorn Planks | floor, wall-main | yes | rustic, interior | Pale gray, like weathered boards. Burns slowly (stairs fire info 1/1). Fallback `minecraft:planks:0` |
| `witchery:witchwoodslab:0-2` (+8 top), `witchery:witchwooddoubleslab:0-2` | Rowan, Alder, Hawthorn Slab | stairs-slab, floor | yes | rustic | Fallback `minecraft:wooden_slab` |
| `witchery:stairswoodrowan`, `witchery:stairswoodalder`, `witchery:stairswoodhawthorn` | Wood stairs | stairs-slab, roof | yes | rustic | Vanilla stair metas |
| `witchery:witchlog:0-2` (+4/+8 axis) | Rowan, Alder, Hawthorn Wood | trim-pillar, foliage | partly | rustic, park-garden | Hawthorn logs make nearby mobs flee (`BlockWitchLog`, around line 60). Rowan has a dark green-gray bark |
| `witchery:witchleaves:0-2` | Rowan, Alder, Hawthorn Leaves | foliage | partly | park-garden | Berry and flower overlays; decay like leaves |
| `witchery:witchsapling:0-2` | Saplings | foliage | partly | park-garden | |
| `witchery:stockade:0-8` | Oak, Spruce, Birch, Jungle, Rowan, Alder, Hawthorn, Acacia, Dark Oak Stockade | fence-rail | partly | rustic | Pointed log palisade, connects like a wall; hardness 25. For rustic outskirts. Fallback `minecraft:fence` |
| `witchery:icestockade:0` | Ice Stockade | fence-rail | partly | cold | |
| `witchery:rowanwooddoor`, `witchery:alderwooddoor` | Rowan, Alder Door | door-trap | no | | Keyed door that drops nothing; alder looks like vanilla's door |

### Glass

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `witchery:shadedglass:0-15` | Shaded Glass | glass, wall-accent | yes | modern, urban-stone | Tinted, smoky glass, 16 colors, meta in wool order (0 white to 15 black; the icon list is the reversed dye list, `BlockShadedGlass` line 26). A redstone signal swaps it to `witchery:shadedglass_active` (opaque, light opacity 15). Unpowered it reads as tinted office glazing; the gray, black, cyan and blue metas suit office towers. No pane form. Fallback `minecraft:stained_glass` same meta |
| `witchery:shadedglass_active:0-15` | Shaded Glass (active) | wall-accent | partly | modern | The opaque state; reverts if not powered when updated, so do not place directly |

### Snow and ice

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `witchery:snowstairs`, `witchery:snowslab:0` (+8), `witchery:snowdoubleslab` | Snow stairs and slabs | roof, stairs-slab | partly | cold | Snow-block stairs and slabs for snow-laden roofs in cold cities |
| `witchery:perpetualice:0` | Perpetual Ice | glass, rubble-ruin | partly | cold | Ice that never melts and makes no water when broken |
| `witchery:icestairs`, `witchery:iceslab`, `witchery:icedoubleslab`, `witchery:icefence`, `witchery:icefencegate`, `witchery:icedoor`, `witchery:icepressureplate`, `witchery:snowpressureplate` | Ice building set | fence-rail, stairs-slab | partly | cold, fantasy | Slippery. Frozen-city dressing at most |

### Decoration, lights, plants

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `witchery:candelabra:0` | Candelabra | light, furniture-deco | partly | interior | Tile entity (no data), light 15, iron candlestick model. Old-house interiors. Fallback `minecraft:torch` |
| `witchery:glowglobe:0` | Glow Globe | light | partly | interior | Small glowing orb, light 14, no tile entity. Fallback `minecraft:torch` |
| `witchery:chalice:0` | Chalice | furniture-deco | partly | interior | Tile entity (filled state). Table dressing |
| `witchery:spanishmoss:0` | Spanish Moss | foliage | yes | overgrown | Vine subclass: hangs from ceilings and walls, grows downward on random ticks. Fallback `minecraft:vine` |
| `witchery:wickerbundle:0` | Wicker Bundle | rubble-ruin | partly | rustic | Straw-bale look |
| `witchery:wickerbundle:1`, `witchery:bloodedwool` | Bloodied wicker, Blooded Wool | off-theme | no | | Gore |
| `witchery:web` | Web | rubble-ruin | partly | ruin | Witchery's web; prefer `minecraft:web` |
| `witchery:somniancotton`, `witchery:glintweed`, `witchery:grassper`, crops (`belladonna`, `mandrake`, `artichoke`, `snowbell`, `wormwood`, `mindrake`, `wolfsbane`, `garlicplant`) | Plants and crops | off-theme | no | magic | Garden dressing at best; mandrakes spawn mobs when harvested |
| `witchery:garlicgarland`, `witchery:coffinblock`, `witchery:statuegoddess`, `witchery:statueofworship`, `witchery:wolfhead`, `witchery:alluringskull`, `witchery:demonheart`, `witchery:infinityegg` | Ritual decoration | off-theme | no | magic | Tile entities or ritual effects |
| `witchery:altar`, `witchery:kettle`, `witchery:cauldron`, `witchery:witchesovenidle/burning`, `witchery:fumefunnel`, `witchery:filteredfumefunnel`, `witchery:distilleryidle/burning`, `witchery:spinningwheel`, `witchery:poppetshelf`, `witchery:dreamcatcher`, `witchery:crystalball`, `witchery:brazier`, `witchery:daylightcollector`, `witchery:silvervat`, `witchery:bloodcrucible`, `witchery:wolfaltar`, `witchery:scarecrow`, `witchery:trent`, `witchery:witchsladder`, `witchery:decurseteleport`, `witchery:decursedirected`, `witchery:circle`, `witchery:circleglyph*`, `witchery:refillingchest`, `witchery:leechchest`, `witchery:placeditem`, `witchery:light`, `witchery:slurp`, `witchery:pitdirt`, `witchery:pitgrass` | Machines and ritual blocks | off-theme | no | magic | Functional tile entities or invisible helper blocks |
| `witchery:bramble:0-1`, `witchery:voidbramble`, `witchery:embermoss`, `witchery:bloodrose`, `witchery:plantmine`, `witchery:leapinglily`, `witchery:crittersnare`, `witchery:beartrap`, `witchery:wolftrap`, `witchery:brew*`, `witchery:disease`, `witchery:spiritflowing`, `witchery:hollowtears`, `witchery:mirrorblock`, `witchery:mirrorblock2`, `witchery:mirrorwall`, `witchery:force`, `witchery:tormentstone`, `witchery:barrier`, `witchery:wallgen`, `witchery:spiritportal`, `witchery:tormentportal`, `witchery:clever`, `witchery:cwoodendoor`, `witchery:cwoodpressureplate`, `witchery:cstonepressureplate`, `witchery:csnowpressureplate`, `witchery:cbuttonwood`, `witchery:cbuttonstone` | Hazards and mechanisms | off-theme | no | | Damage, teleport, fire, explosion, spreading, cursed redstone, unbreakable barriers. Never |

## Best variety pools

- **Wood floors and walls (`floor`, `wall-main`):** alder planks for a painted-red accent, hawthorn for weathered gray boards, rowan as a birch alternative; matching slabs and stairs.
- **Tinted glazing (`glass`):** shaded glass gray (7), light gray (8), black (15), cyan (9), blue (11) for office towers; brown (12) for older buildings.
- **Cold-city roofs (`roof`):** snow stairs and slabs.
- **Overgrowth (`foliage`):** spanish moss.
- **Interior lights (`light`):** candelabra, glow globe.

## Worldgen notes

- Shaded glass: place the unpowered `witchery:shadedglass` only; it re-checks power when added (`onBlockAdded`), so a building wired with redstone could darken it.
- Spanish moss random-ticks and grows downward like vines; fine for overgrowth.
- Candelabra and chalice create their tile entities on placement with no data to write.
