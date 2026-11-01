# BuildCraft

Family D. Mod IDs `BuildCraft|Core`, `BuildCraft|Builders`, `BuildCraft|Factory`, `BuildCraft|Transport`, `BuildCraft|Energy`, `BuildCraft|Silicon`, `BuildCraft|Robotics`, jar `buildcraft-7.1.65.jar`; BuildCraftCompat (`buildcraft-compat-7.1.22.jar`) adds no placeable blocks of its own worth listing. Source read: `mods/buildcraft/buildcraft/core/BCRegistry.java` (lines 32-52), the `registerBlock` calls in `BuildCraftBuilders.java`, `BuildCraftFactory.java`, `BuildCraftCore.java`, `BuildCraftEnergy.java`, `BuildCraftSilicon.java`, `BuildCraftRobotics.java`, `BuildCraftTransport.java` (all under `buildcraft/`), `buildcraft/builders/BlockFrame.java`. Sheets: `tools/block-contact-sheet/sheets/buildcraft/` (previews in `docs/previews/blocks/buildcraft/`, local only).

## Summary

- **Registry names.** `BCRegistry.registerBlock` registers each block under its unlocalized name minus `tile.`, prefixed by the registering module's mod ID: `BuildCraft|Factory:tankBlock`, `BuildCraft|Builders:frameBlock`. Non-forced blocks can be switched off in `config/buildcraft/main.cfg`; the pack's file has no `blocks` category, so all default to on.
- **Counts.** About 32 registry names; with engine and laser-table subtypes about 40 block+meta entries. **Useful: 2** (frame, tank), both marginal. Everything else is a machine, a pipe, a marker or a fluid.
- **Look.** Light gray machine casings with small LED details; wooden-and-iron engines with pistons; the frame is a thin orange-brown lattice (the quarry frame); the tank is a glass cylinder in a red-black frame.
- **Caveats.** All machines are TEs; engines and pumps tick. **Breaking one frame block breaks every connected frame block** (`BlockFrame.removeNeighboringFrames`, lines 50-76): a whole scaffold collapses when a player mines one piece. Fluids (oil, fuel, red plasma) flow.
- **Optional dependency.** A server without BuildCraft loses these. Fallbacks: frame to `minecraft:fence` or air, tank to `minecraft:glass`.

## Blocks

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `BuildCraft\|Builders:frameBlock:0` | Frame | `special`, `rubble-ruin` | partly | `industrial`, `ruin` | No TE. Connected thin lattice. Could suggest an unfinished steel skeleton or a crane mast, but the whole connected frame breaks at once |
| `BuildCraft\|Factory:tankBlock:0` | Tank | `furniture-deco` | partly | `industrial` | TE (holds fluid; empty by default). Stackable glass tank: clutter in a factory or brewery |
| `BuildCraft\|Core:engineBlock:0-2` | Redstone, Stirling, Combustion Engine | `off-theme` (machine) | no | `industrial` | TE models, tick |
| `BuildCraft\|Factory:pumpBlock`, `floodGateBlock`, `refineryBlock`, `miningWellBlock`, `autoWorkbenchBlock`, `blockHopper`, `plainPipeBlock` | Factory machines | `off-theme` (machines) | no | `industrial` | TEs |
| `BuildCraft\|Builders:machineBlock`, `fillerBlock`, `builderBlock`, `architectBlock`, `libraryBlock`, `constructionMarkerBlock` | Quarry and builders | `off-theme` (machines; a quarry would dig) | no | `industrial` | TEs |
| `BuildCraft\|Core:markerBlock`, `pathMarkerBlock`, `springBlock`, `buildToolBlock` | Markers, spring | `off-theme` | no | | |
| `BuildCraft\|Transport:pipeBlock`, `filteredBufferBlock` | Pipes, filtered buffer | `off-theme` (pipe TEs need NBT per pipe type) | no | `industrial` | |
| `BuildCraft\|Silicon:laserBlock`, `laserTableBlock:0-5`, `packagerBlock`; `BuildCraft\|Robotics:zonePlan`, `requester` | Silicon and robotics machines | `off-theme` | no | | TEs |
| `BuildCraft\|Energy:blockOil`, `blockFuel`, `blockRedPlasma` | Fluids | `off-theme` (flowing fluids) | no | | |

## Best variety pools

- **Factory clutter:** `BuildCraft|Factory:tankBlock` stacks of 1 to 3.

## Worldgen notes

- Not worth placing apart from the tank. The frame's cascade break makes it a poor scaffold.
