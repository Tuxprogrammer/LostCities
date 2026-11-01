# 0004 · Faithful cities with a GTNH layer; EV ceiling

- Status: Accepted
- Gate: none (foundation)
- Amends: none
- Date: 2026-11-01

## Context

The point of the port is to give a GTNH overworld a lived-in look and feel. Players should come across abandoned cities, and be able to take over a complex of buildings and build their base into it. The owner wants upstream's cities kept, with tight GTNH integration on top. Balance and progression checks come later.

## Decision

1. **Faithful cities.** Upstream's layouts, buildings, parts, styles and city styles carry over as they are. Where a 1.20 block has no 1.7.10 vanilla equivalent, it maps to a pack block (for example from Et Futurum Requiem or Chisel). The new spec records each mapping.
2. **A GTNH layer on top:**
   - pack materials and blocks where they belong;
   - loot drawn from pack items;
   - mobs and spawners by pack conventions (vanilla spawners with NBT);
   - nothing that fights GT ore generation.

   GTNH-themed buildings and styles come later, as additions next to upstream's, never as replacements.
3. **Tier ceiling.** Nothing generated in the overworld (loot, placed machines, blocks, ores) is above **EV**, and the distribution is weighted toward the lower tiers.
4. **No balance pass yet.** Within the ceiling, quantities, rarity and progression effects are not restricted during alpha. Balance is a beta-phase pass, and removed or loosened caps are listed for it.
5. **Buildings are meant to be lived in.** They stay ordinary blocks that players can break, reuse and build into. Generation never places unbreakable or protected blocks, and never claims land. A structure is sound enough to move into: no floating debris inside, and consistent floors and walls.

## Consequences

- The new spec gets a block-mapping table (1.20 block to 1.7.10 pack block) and a loot-and-spawner sheet with every pool's tier.
- The dev self-check fails on any generated item above EV once loot is implemented.
- Upstream's damage and explosion features (ruined buildings, rubble) stay, because they are part of the abandoned look. They still have to leave a structure a player can take over.

## Alternatives considered

- **Faithful look only:** too thin for the GTNH-first goal.
- **Full GTNH reskin:** loses what makes Lost Cities recognizable, and front-loads asset work before the RWG problem is solved.

## Still open

- Which pack mods supply mapped blocks, loot and spawner mobs (new spec, after the pack research spike).
