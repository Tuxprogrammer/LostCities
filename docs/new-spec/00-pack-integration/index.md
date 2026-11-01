# 00 · Pack integration

How Lost Cities: New Horizons fits into GTNH 2.9.0 across every feature area: the RWG terrain it overlays, the GT ores it must not fight, the blocks it builds with, the loot and mobs it places, the other structures it shares the overworld with, and world spawn. The other new-spec areas build on these sheets.

Status: *Accepted* (spikes S2 and S3; [decision log](../../design/decision-log.md) D18 to D21 and record [0005](../../design/0005-rwg-overlay.md)). Each sheet states its answer to one or more of the questions [OPEN-QUESTIONS.md](../OPEN-QUESTIONS.md) tracked, and the decision that settled it. Facts about other mods are *verified* with a pinned source permalink or a pack config path relative to `.minecraft/`, or marked *unverified* ([new-spec README](../README.md)).

## Sheets

| Sheet | Covers | Question | Answer | Decision |
|---|---|---|---|---|
| [rwg.md](rwg.md) | The overlay hook into RWG2 Continents and classic RWG, the terrain blend, height mapping (spike S2) | Q1, Q2, Q7, Q10 | A delegating chunk provider around RWG's; RWG builds terrain and decoration first, then the city clears RWG's decoration inside its footprint and keeps it outside | Record [0005](../../design/0005-rwg-overlay.md) |
| [ore-veins.md](ore-veins.md) | How GT5-Unofficial generates veins, small ores and boulders, and how cities coexist with them | Q3 | Veins pass through; buildings use no block GT ore replaces; no exclusion and no restore at alpha. A restore pass for boulder hosts only if the alpha screenshots show boulder damage | D18 |
| [block-mapping.md](block-mapping.md) | Every block upstream uses, mapped to a 1.7.10 pack block with a vanilla fallback | Q4 | Et Futurum Requiem first, then Botania and Chisel where the pack disables Et Futurum's block, then vanilla. Infested blocks map to their plain blocks: no silverfish | D19 |
| [loot-and-mobs.md](loot-and-mobs.md) | Chest loot and spawners in the pack, and what fills city chests and spawners | Q5, Q6 | Our own `ChestGenHooks` categories per tier band, Steam to EV, plus rail, filled with pack items at generation; vanilla categories only by opt-in. Vanilla spawners with vanilla mob names, `Blaze` included, at upstream's density; the pack's mob mods vary them | D20 |
| [other-worldgen.md](other-worldgen.md) | Every other overworld structure and feature in the pack, its hook and its conflict risk | Q8 | Four treatments: avoid in the layout (villages included), deny the populate event in city chunks, coexist (AE2 meteorites included), or a compat guard if the alpha screenshots show the conflict | D21 |
| [spawn.md](spawn.md) | How the pack picks world spawn, and upstream's spawn options | Q9 | The pack's spawn is kept, with a city likely a short walk away. Upstream's options are ported as profile settings, off by default, through `WorldEvent.CreateSpawnPosition`, searching the layout rather than the world; `forceSpawnInBuilding` applies to the first join only | D21 |

## Pack mods this mod touches

Versions, mod IDs and source tags come from the reference pack's `MOD-INDEX.md` ([Reference pack](#reference-pack)). "Touch" means one of: **blocks** (named in assets, resolved at load), **items** (loot contents), **generation** (our generator reacts to, avoids or yields to theirs), **behavior** (their behavior shapes ours, with no code contact). Whether each is a hard or optional dependency, and how it is guarded, is settled in `01-core`; every touch below works by registry name or Forge event unless the sheet says otherwise.

| Mod (mod ID) | 2.9.0 version | Touch | How | Sheet |
|---|---|---|---|---|
| Realistic World Gen 2 (`rwg2`) | 2.0.6 | generation | The default overworld (RWG2 Continents) that cities overlay; the layout avoids its volcanoes, lava caves and river junctions through its chunk manager | [rwg.md](rwg.md), [other-worldgen.md](other-worldgen.md) |
| Realistic World Gen (`RWG`) | alpha-1.5.3 | generation | The classic RWG overworld cities also overlay | [rwg.md](rwg.md), [other-worldgen.md](other-worldgen.md) |
| DefaultWorldGenerator (`defaultworldgenerator`) | 0.5.0 | behavior | Makes RWG2 Continents the default world type and whitelists `flat`, `RWG`, `RWG2_CONTINENTS` | [rwg.md](rwg.md) |
| GT5-Unofficial (`gregtech` and others) | 5.09.54.265 | generation, items, behavior | Its ore veins, small ores and boulders run after everything else and are never suppressed; loot draws GT components and materials by tier; its `harderMobSpawner` makes spawners very hard to break | [ore-veins.md](ore-veins.md), [loot-and-mobs.md](loot-and-mobs.md) |
| Et Futurum Requiem (`etfuturum`) | 2.6.65-GTNH | blocks, generation | Most modern blocks (concrete, deepslate bricks, blackstone, copper, chain, lantern, barrel, smooth stone and more); its deepslate and tuff are ore hosts and never building blocks; its mineshafts and geodes coexist | [block-mapping.md](block-mapping.md), [other-worldgen.md](other-worldgen.md) |
| Botania (`Botania`) | 1.13.42-GTNH | blocks | Sea lantern and the diorite family, which the pack's Et Futurum config disables | [block-mapping.md](block-mapping.md) |
| Chisel (`chisel`) | 2.17.34-GTNH | blocks | Second-choice polished diorite and andesite; the catalog for GTNH-themed buildings after beta | [block-mapping.md](block-mapping.md) |
| Biomes O' Plenty (`BiomesOPlenty`) | 2.1.0.2308 | behavior, generation | Replaces the overworld provider for every world type, so it judges spawn; replaces the village generator before VillageNames does; supplies RWG2's volcano biome | [spawn.md](spawn.md), [other-worldgen.md](other-worldgen.md) |
| VillageNames (`VillageNames`) | 4.5.21-GTNH | generation | Owns village generation; the layout avoids its villages as upstream's `avoidVillages` does | [other-worldgen.md](other-worldgen.md) |
| NewHorizonsCoreMod (`dreamcraft`) | 2.9.99 | generation | Its oil spheres and fountains and its Railcraft quarry and geode populators are denied in city chunks | [other-worldgen.md](other-worldgen.md) |
| Railcraft (`Railcraft`) | 9.17.36 | items, generation | Rail parts for rail-dungeon loot; quarry and geode populators denied in city chunks | [loot-and-mobs.md](loot-and-mobs.md), [other-worldgen.md](other-worldgen.md) |
| TooMuchLoot (`TML`) | 4.3.0-GTNH | behavior | Replaces 16 vanilla loot categories from XML at server start; server owners can retune our categories the same way | [loot-and-mobs.md](loot-and-mobs.md) |
| EnhancedLootBags (`enhancedlootbags`) | 1.3.5 | items | Tiered loot bags (Steam to EV groups) in city loot | [loot-and-mobs.md](loot-and-mobs.md) |
| Pam's HarvestCraft (`harvestcraft`) | 1.3.15-GTNH | items | Food in low-tier loot | [loot-and-mobs.md](loot-and-mobs.md) |
| Special Mobs (`SpecialMobs`), Infernal Mobs (`InfernalMobs`) | 3.7.10, 1.10.7-GTNH | behavior | Transform whatever a spawner spawns, so vanilla mob names give GTNH-style variety | [loot-and-mobs.md](loot-and-mobs.md) |
| EnderZoo (`EnderZoo`) | 1.3.6 | behavior | Optional overworld mobs for `hardmobs` spawners, off by default | [loot-and-mobs.md](loot-and-mobs.md) |
| EnderIO (`EnderIO`) | 2.10.51 | behavior | Every broken spawner drops a Broken Spawner, which makes city spawners a progression resource | [loot-and-mobs.md](loot-and-mobs.md) |
| Roguelike Dungeons (`Roguelike`), Witchery (`witchery`) | 1.6.6-GTNH, 0.24.1 | generation | Large surface structures with no event to deny; an optional compat guard only if alpha shows them in cities | [other-worldgen.md](other-worldgen.md) |
| LootGames (`lootgames`), Applied Energistics 2 (`appliedenergistics2`) | 2.2.14, rv3-beta-1095-GTNH | generation | Seed-determined puzzle dungeons the layout avoids, and meteorites that may crater a city | [other-worldgen.md](other-worldgen.md) |
| Thaumcraft (`Thaumcraft`), Forestry (`Forestry`), Tinkers' Construct (`TConstruct`) and the small decorators | 4.2.3.5, 4.11.40, 1.14.123-GTNH | generation | Nodes, small structures, hives, slime islands and plants coexist with cities | [other-worldgen.md](other-worldgen.md) |
| Hodgepodge (`hodgepodge`) | 2.7.221 | behavior | Its cascading-worldgen warning is what the headless worldgen scenario fails on | [CONTRIBUTING § World generation rules](../../../CONTRIBUTING.md#world-generation-rules) |
| GTNHLib (`gtnhlib`) | 0.11.54 | behavior | Annotation config for mod options | `01-core` |
| PersonalSpace (`personalspace`) | 1.0.41 | none yet | Cities in personal dimensions after beta ([0003](../../design/0003-world-presence.md)) | |

## Reference pack

The research behind these sheets reads the pack as players get it:

- **Source:** the official GTNH 2.9.0 MultiMC client zip (`downloads.gtnewhorizons.com/Multi_mc_downloads/GT_New_Horizons_2.9.0_Java_17-26.zip`) and the DreamAssemblerXXL release manifest `releases/manifests/2.9.0.json`.
- **Location:** `reference/gtnh/2.9.0/` in the main checkout (git-ignored), shared by every worktree. Its `.minecraft/config/` is the "pack config" every *verified* config citation refers to.
- **Index:** `reference/gtnh/2.9.0/MOD-INDEX.md` lists all 243 mods in the manifest (242 jars; JourneyMap Server is server-side only) with version, jar, mod IDs, license, source repo and the tag matching the pack.
- **Helper:** [tools/gtnh-reference/](../../../tools/gtnh-reference/README.md) fetches and indexes the pack, finds a class's jar, clones a mod's source at its pack tag into `reference/src/`, and decompiles as a last resort.

## What surprised us in 2.9.0

Each is detailed and cited in its sheet.

- **Et Futurum is half off.** The pack disables Et Futurum's granite, andesite and diorite, prismarine and sea lantern, iron trapdoor and wood-variant doors ([block-mapping.md](block-mapping.md)).
- **Name traps.** 1.7.10's `minecraft:stone_stairs` is cobblestone stairs, and its `minecraft:stone_slab` is the smooth stone slab ([block-mapping.md](block-mapping.md)).
- **GT runs last and late.** A GT mixin forces its world generator to the end of the list, and its ore work is queued and can land during a later chunk's population. Its boulders replace cobblestone and colored terracotta as well as stone ([ore-veins.md](ore-veins.md)).
- **GT's vein choice depends on generation order**, so ore placement can't be compared between two worlds in real-pack checks ([ore-veins.md](ore-veins.md)).
- **TooMuchLoot rewrites vanilla loot** at server start, after the spawn area has already generated, so spawn-area chests miss it ([loot-and-mobs.md](loot-and-mobs.md)).
- **Spawners are nearly unbreakable** (GT's hardness 500) and break into EnderIO Broken Spawners ([loot-and-mobs.md](loot-and-mobs.md)).
- **Spawn is a random walk.** RWG gives vanilla no spawn biome, and Biomes O' Plenty's provider, installed for every world type, judges the walk ([spawn.md](spawn.md)).
- **RWG2 runs on its defaults** (the pack ships no `rwg2.cfg`), and the shipped `config/RTG/` folder is read by no jar ([other-worldgen.md](other-worldgen.md)).
- **Some pack generators break the rules this mod follows:** Witchery places structures with `world.rand`, Roguelike Dungeons writes 40 to 100 blocks from the chunk it populates, and AE2 meteorites land a tick after population ([other-worldgen.md](other-worldgen.md)).

## Decisions

Every question these sheets raised is settled. Q3 to Q6, Q8 and Q9 are rows D18 to D21 of the [decision log](../../design/decision-log.md); Q1, Q2, Q7 and Q10 are settled by record [0005](../../design/0005-rwg-overlay.md). Each spike S3 sheet ends with a Decisions section that lists what was decided. Anything left to tune is on the [beta balance list](../beta-balance-list.md).
