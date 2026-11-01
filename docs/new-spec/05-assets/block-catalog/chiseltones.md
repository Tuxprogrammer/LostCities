# Block catalog: ChiselTones

ChiselTones 1.2.0-GTNH (`chiseltones-1.2.0-GTNH.jar`, mod ID `chiseltones`), read from the source at the pack's tag (`gtnh-src/source/ChiselTones/`, three classes) and the pack's shipped `config/ChiselTones.cfg`. Part of the decorative block catalog (decision log D25).

## Summary

- **ChiselTones registers no blocks.** The jar holds three classes and `mcmod.info`: no textures, no block registrations. It only adds Chisel carving variations for **Ztones** blocks, so every block it touches is catalogued in [ztones.md](ztones.md). Count: **0 enabled of 0**.
- **What it does in the pack.** `variationAdditionType=1` in the shipped config (the code's default is 0). Mode 1 makes each Ztones set its own Chisel group, so a chisel cycles a placed block through the 16 metas of its set (`ChiselTones.addVariationFast`). Mode 0 would instead have made Ztone Tile chisel into meta 0 of every set.
- **Groups added in the pack** (33, one per set, metas 0-15 each), named as Chisel shows them: Agon, Bitt, Cray, Fort, Iszm, Jelt, Korp, Kryp, Lair, Lave, Mint, Myst, Reds, Roen, Sols, Tank, Vect, Vena, Zech, Zion, Zome, Zone, Zorg, Reed, Sync, Zkul, Azur, Zeta, Zest, Zoea, Zane, Zyth, Ztyl. Each group is added only when its Ztones config flag is on; all are on in the pack. Tinted Glass (`glaxx`) and the single blocks get no group.
- **No contact sheets** for this mod: the blocks are on the Ztones sheets (`tools/block-contact-sheet/sheets/ztones/`).

## Blocks

None. See [ztones.md](ztones.md) for the 551 Ztones block+meta that these groups cover.

## What this means for city generation

- **Nothing to place from ChiselTones**, and no fallback is needed when it is absent: Ztones blocks place the same with or without it.
- **The groups are a ready-made variety key.** At run time, Chisel's carving registry (the `CarvingUtils.chisel` field ChiselTones writes to, keyed by group name) lists which Ztones metas belong together. A palette entry such as "any meta of Lave from this list" maps cleanly onto one group. That is a possible way to express Ztones variety later; it is not needed for a static palette, since every set is one block ID with metas 0-15 anyway.

## Best variety pools

See [ztones.md](ztones.md#best-variety-pools).
