# OpenModularTurrets

Family D. Mod ID `openmodularturrets`, jar `OpenModularTurrets-2.4.8.jar`. Source read: `mods/OpenModularTurrets/openmodularturrets/blocks/Blocks.java`, `.../blocks/misc/BlockHardWallTier*.java`, `.../blocks/misc/BlockFenceTier*.java`, `.../reference/Names.java` (lines 11-50), `assets/openmodularturrets/lang/en_US.lang`, `config/openmodularturrets.cfg`. Sheets: `tools/block-contact-sheet/sheets/openmodularturrets/` (previews in `docs/previews/blocks/openmodularturrets/`, local only).

## Summary

- **Counts.** 36 registry names, all meta 0 apart from orientation: 5 hard walls, 5 fences, 5 turret bases, 10 expanders, 10 turrets (each gated by a `B:Enabled` flag; all are `true` in `config/openmodularturrets.cfg`), 1 lever. The addons are items. **Useful: 6** (hard walls tiers 1-4, fences tiers 1-2). Hard wall tier 5 is harder than obsidian and fences 3-5 hurt on contact: never placed.
- **Look.** The hard walls are plain, slightly blue-gray metal panels; tiers 3 to 5 add rivets and get darker, tier 5 nearly black. They read as sheet steel, industrial cladding or a bank-vault lining. The fences are thin wire mesh panes like chain-link or security fencing; tiers 3-5 carry colored wires.
- **No tile entities** for walls and fences. Turret bases, turrets and expanders are TEs.
- **Hardness.** Walls and fences are very hard: hardness and blast resistance 10, 20, 30, 50, 80 for tiers 1 to 5 (obsidian is 50), harvest level 2 (iron pickaxe) for tier 1. Players who take over a building will find tiers 3-5 tedious to remove. Prefer tiers 1-2 for ordinary walls and tier 4 for vaults and bunkers where that is the point. Tier 5 (hardness 80) is never placed (D28 rule 7).
- **Damage.** Fences of tiers 3, 4 and 5 hurt any non-item entity that touches them: 3, 4 and 5 points of cactus damage (`BlockFenceTier{Three,Four,Five}.onEntityCollidedWithBlock`). Tiers 1-2 are harmless.
- **Turrets shoot.** Turret blocks on a powered base attack players who are not on the owner's list. A worldgen turret with no owner data is unpredictable; never place turrets, bases or expanders.
- **Optional dependency.** A server without OpenModularTurrets loses these. Fallbacks: hard walls to `minecraft:iron_block` (tiers 1-3) or `etfuturum:concrete:7` / `minecraft:stained_hardened_clay:7`; fences to `minecraft:iron_bars`.

## Blocks

### Walls and fences

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `openmodularturrets:hardWallTierOne:0` | Hard Wall (Tier 1) | `wall-main`, `wall-accent`, `floor` | yes | `industrial`, `modern` | Plain pale steel panel, no rivets. Hardness 10 |
| `openmodularturrets:hardWallTierTwo:0` | Hard Wall (Tier 2) | `wall-main`, `wall-accent` | yes | `industrial`, `modern` | Slightly darker steel. Hardness 20 |
| `openmodularturrets:hardWallTierThree:0` | Hard Wall (Tier 3) | `wall-accent`, `trim-pillar` | yes | `industrial` | Riveted steel plate. Hardness 30 |
| `openmodularturrets:hardWallTierFour:0` | Hard Wall (Tier 4) | `wall-accent`, `special` | partly | `industrial` | Dark riveted plate. Hardness 50: vault or bunker only |
| `openmodularturrets:hardWallTierFive:0` | Hard Wall (Tier 5) | `special` | partly | `industrial` | Near-black riveted plate. Hardness 80: never placed (D28 rule 7) |
| `openmodularturrets:fenceTierOne:0` | Fence (Tier 1) | `fence-rail`, `pane` | yes | `industrial`, `urban-stone` | Pane-shaped (`BlockAbstractMiscPane`), connects like iron bars. Fine gray wire mesh: chain-link fencing around lots, rooftops and yards |
| `openmodularturrets:fenceTierTwo:0` | Fence (Tier 2) | `fence-rail`, `pane` | yes | `industrial` | As tier 1, slightly heavier mesh. Harmless |
| `openmodularturrets:fenceTierThree:0` | Fence (Tier 3) | `fence-rail` | partly | `industrial` | Yellow-wired mesh. **3 damage on contact**: never placed |
| `openmodularturrets:fenceTierFour:0` | Fence (Tier 4) | `fence-rail` | partly | `industrial` | Blue-wired mesh. **4 damage on contact**: never placed |
| `openmodularturrets:fenceTierFive:0` | Fence (Tier 5) | `fence-rail` | partly | `industrial` | **5 damage on contact**, hardness 80: never placed |

### Turrets and accessories (off-theme)

| registry name:meta | English name | roles | fits upstream? | styles | notes |
|---|---|---|---|---|---|
| `openmodularturrets:baseTierWood:0`, `baseTierOneBlock:0`, `baseTierTwoBlock:0`, `baseTierThreeBlock:0`, `baseTierFourBlock:0` | Turret Base (Tier 1-5) | `off-theme` (weapon platform) | no | | TEs with owner and energy |
| `openmodularturrets:expanderPowerTier*`, `expanderInvTier*` (10) | Expanders | `off-theme` | no | | TEs |
| turrets (`disposeItemTurret`, `machineGunTurret`, `incendiaryTurret`, `grenadeTurret`, `rocketTurret`, `laserTurret`, `railGunTurret`, `teleporterTurret`, `potatoCannonTurret`, `relativisticTurret`) | Turrets | `off-theme` (they shoot players) | no | | TEs, models |
| `openmodularturrets:leverBlock:0` | Lever (crank) | `off-theme` | no | | Power crank for a base |

## Best variety pools

- **Industrial and warehouse walls:** `openmodularturrets:hardWallTierOne`, `hardWallTierTwo`, with `hardWallTierThree` as a riveted band or pillar.
- **Vault, bank or bunker lining:** `hardWallTierFour` (hardness 50, the D28 ceiling).
- **Chain-link fencing:** `openmodularturrets:fenceTierOne`, `fenceTierTwo` (with `minecraft:iron_bars` for variety).

Never placed: `hardWallTierFive` (hardness 80, D28 rule 7) and fences tiers 3 to 5 (contact damage).

## Worldgen notes

- Walls and fences are plain blocks: no TE, no ticking, no light.
- Keep wall tiers 3 and 4 out of ordinary buildings because of hardness; never place wall tier 5 or the damaging fences.
- Never place turrets, bases, expanders or the lever.
