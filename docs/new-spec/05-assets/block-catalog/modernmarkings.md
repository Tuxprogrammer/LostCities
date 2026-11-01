# ModernMarkings

Family D. Mod ID `modernmarkings`, jar `modernmarkings-0.3.13-1.7.10.jar`. Source read: `mods/modernmarkings/modernmarkings/init/ModBlocks.java`, `blocks/BlockBase.java`, `blocks/MarkingFloor.java`, `blocks/MarkingWall.java`. Sheets: `tools/block-contact-sheet/sheets/modernmarkings/` (`floor`, `wall`, `letters-numbers`; previews in `docs/previews/blocks/modernmarkings/`, local only).

## Summary

- **170 registry names, one block each**: 87 floor markings (61 lines, symbols and colors plus 26 letters) and 83 wall markings (57 signs, numbers and symbols plus 26 letters). No config switches them off. All useful except a handful of GT-voltage and mod-energy signs, which are pack jokes rather than city signage.
- **Registry names carry a `tile.` prefix.** Every block is registered with `block.getUnlocalizedName()` (`ModBlocks.java` L205-L207), which returns `tile.<name>`, so the names are `modernmarkings:tile.floor_marking_<x>` and `modernmarkings:tile.wall_marking_<x>`. The asset map must spell them that way.
- **No tile entities, no light, no gravity.** `Material.carpet`, non-opaque, no collision box (`getCollisionBoundingBoxFromPool` returns an empty box in both classes).
- **Floor markings are a 0.0001-high overlay** (`MarkingFloor.java` L20) that must sit on a block with a solid top: `onNeighborBlockChange` drops it as an item when the block below stops being solid (L84-L91). Place it after the street surface, never on slabs' bottom halves or on air. Meta 0-3 is rotation (set from player yaw on placement, L44-L67).
- **Wall markings are flat signs** placed on a side; meta is the `ForgeDirection` of the face (2 north, 3 south, 4 east, 5 west; `MarkingWall.java` L31-L41). Like the floor markings it needs support: `onNeighborBlockChange` drops it as an item unless the block on the side opposite its meta direction has a solid face (L157-L169). Its `canPlaceBlockAt` calls `world.getClosestPlayer` and dereferences the result (L145-L153, L44-L46), so worldgen must use a plain `setBlock` and never the placement checks: with no player nearby that path throws.
- **The look is exactly upstream's missing street layer**: white and yellow lane lines, crossings, turn arrows, stop-zone hatching, parking squares, hazard stripes; wall signs for exits, first aid, fire extinguishers and hazard pictograms. Textures are clean flat paint on transparent backgrounds.
- **Fallback when the mod is absent**: nothing (leave the street surface plain). These are pure overlays, so the fallback is air in the overlay layer, not a substitute block.

## Blocks

### Floor markings (`floor` sheet)

All `modernmarkings:tile.floor_marking_<x>:0-3`, rotation in meta.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `…floor_marking_white_lane`, `_white_singleline`, `_white_sideline`, `_white_end` :0-3 | White lane line, single line, side line, line end | street | yes | urban-brick, urban-stone, modern | the core road paint; the dashed lane is the best default |
| `…floor_marking_white_lane_cross` :0-3 | White lane crossing | street | yes | modern | zebra-like crossing piece |
| `…floor_marking_white_crossing`, `_white_t_crossing`, `_white_turn` :0-3 | White junction pieces | street | yes | modern | intersections |
| `…floor_marking_white_arrow` :0-3 | White arrow | street | yes | modern | turn lanes |
| `…floor_marking_white_square` :0-3 | White square | street, sidewalk | yes | modern | parking bays |
| `…floor_marking_whiteblack_hazardline` :0-3 | White/black hazard line | street | yes | industrial | |
| `…floor_marking_yellow_lane`, `_yellow_singleline`, `_yellow_sideline`, `_yellow_end` :0-3 | Yellow line set | street | yes | modern, industrial | center lines, no-parking curbs |
| `…floor_marking_yellow_crossing`, `_yellow_t_crossing`, `_yellow_turn`, `_yellow_arrow`, `_yellow_square` :0-3 | Yellow junction, arrow, square | street | yes | modern, industrial | |
| `…floor_marking_yellow_stripes` :0-3 | Yellow diagonal stripes | street | yes | industrial | keep-clear boxes, loading bays |
| `…floor_marking_yellow_hazardline`, `_danger_yellow` :0-3 | Yellow hazard line, yellow danger triangle | street, floor | yes | industrial | factory floors |
| `…floor_marking_{red,green,blue,orange}_{singleline,sideline,end,crossing,t_crossing,turn,square,hazardline}` :0-3 (32 blocks) | Colored line sets | floor, street | partly | industrial, interior | colored lines read as warehouse, parking-garage or sports-court paint, not public roads |
| `…floor_marking_black_singleline` :0-3 | Black line | street | partly | modern | invisible on dark asphalt; useful on concrete |
| `…floor_marking_danger_red`, `_redx`, `_noentry` :0-3 | Red danger triangle, red X, no-entry | street, floor | yes | industrial | |
| `…floor_marking_exit`, `_nostep` :0-3 | EXIT, NO STEP stencils | floor | yes | industrial, interior | |
| `…floor_marking_letter_a` … `_letter_z` :0-3 (26) | Stenciled letters | street, floor | yes | modern | "BUS", "STOP", parking numbers (no floor digits exist) |

### Wall markings (`wall` and `letters-numbers` sheets)

All `modernmarkings:tile.wall_marking_<x>`, meta 2-5 = facing.

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `…wall_marking_exit` | EXIT sign | furniture-deco | yes | interior, modern | stairwells, lobbies |
| `…wall_marking_first_aid`, `_fire_extinguisher` | First aid, fire extinguisher signs | furniture-deco | yes | interior | offices, corridors |
| `…wall_marking_unit` | UNIT label | furniture-deco | yes | industrial | |
| `…wall_marking_arrowleft`, `_arrowright` | Direction arrows | furniture-deco | yes | interior | |
| `…wall_marking_hazardline_left`, `_hazardline_right`, `_hazard_diamond`, `_warning_danger` | Hazard stripes, NFPA diamond, danger triangle | wall-accent | yes | industrial | |
| `…wall_marking_symbol_{chemical,electric,fire,fluid,frost,gears,laser,magic,mob,pinch,production,radiation,radiation_2,storage}` (14) | Hazard and room pictograms | furniture-deco | partly | industrial | `magic` and `mob` are game jokes; skip in cities |
| `…wall_marking_number_0` … `_9`, `_no`, `_numero`, `_dash`, `_comma`, `_period` (15) | Digits and punctuation | furniture-deco | yes | modern, interior | apartment and floor numbers |
| `…wall_marking_letter_a` … `_z` (26) | Letters | furniture-deco | yes | modern | store names, block letters |
| `…wall_marking_voltage_{lv,mv,hv,ev,iv,luv,zpm,uv,uhv,uev,uiv,umv,uxv,max,eu,rf,fe,pert}` (18) | GT voltage tier and energy-unit labels | off-theme | no | industrial | pack-machine labels; out of place in a 20th-century city. LV-EV only, if at all, inside GT-themed buildings later |

## Best variety pools

- **street (lane paint)**: white lane, white singleline, white sideline, yellow lane, yellow singleline at high weight; white arrow, white crossing, white lane cross, yellow stripes at low weight. This is the single biggest win for streets: upstream's streets are plain slabs.
- **street (parking lots, garages)**: white square, yellow square, letters, colored singlelines in garages.
- **floor (factories, warehouses)**: yellow hazardline, whiteblack hazardline, yellow stripes, danger_yellow, nostep, exit, colored lines.
- **furniture-deco (interiors)**: exit, first aid, fire extinguisher, arrows, digits for door numbers.
- **wall-accent (industrial facades)**: hazardline left and right, hazard diamond, warning danger.

## Worldgen notes

- Cheap: plain blocks, no TE, no ticking. One overlay per road block costs one block in the column above the surface.
- Order matters: place the surface first, then the marking; a marking generated before its support pops off when the chunk's neighbor updates run.
- Ruin damage should remove markings with their support, and a ruined street should drop most of them (worn paint).
- Rotation: compute meta from the street axis (0-3); wall markings from the wall's outward face (2-5).
- Dedicated server and clients without the mod lose the blocks; the overlay layer is simply skipped (no vanilla fallback needed).
