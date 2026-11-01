# 0003 · World presence: the GTNH overworld

- Status: Accepted
- Gate: none (foundation)
- Amends: none
- Date: 2026-11-01

## Context

Upstream Lost Cities 1.20 generates in two ways:

- **Overworld profiles**, chosen at world creation, which lay cities over vanilla noise terrain.
- **A dedicated Lost City dimension**, reached by sleeping in a bed placed on a configured block.

GTNH plays differently. The overworld is the main world, and Realistic World Gen is its only generator. GTNH 2.9.0's `defaultworldgenerator.cfg` sets `RWG2_CONTINENTS` (Realistic World Gen 2, 2.0.6) as the default world type and whitelists only `flat`, `RWG` (classic RWG, alpha-1.5.3) and `RWG2_CONTINENTS`. GT ore veins generate into that terrain. Most players later move into their own personal dimension (PersonalSpace 1.0.41).

The owner wants the hard part done first: cities that fit the GTNH overworld and don't break when they meet RWG. They accept that alpha and beta will have rough edges to iron out, as GregGuns did.

## Decision

1. **Cities generate in the overworld of a GTNH world**, over RWG2 Continents terrain first and classic RWG second. Flat worlds count as a supported case only where they come free.
2. **The dedicated Lost City dimension is not ported.** The bed teleporter, the dimension type and the dimension profile list go with it. That is a deviation from upstream, recorded in the new spec.
3. **Cities are an overlay on the pack's terrain, not a replacement world type.** How the overlay hooks into RWG2's generation (chunk provider wrapping, an event, or a mixin) and how city terrain blends into RWG terrain are settled by spike S2 ([ROADMAP](../ROADMAP.md)) in their own design record.
4. **GT ore veins and other pack world generation must coexist with cities.** Whether veins are kept under or around cities, and what happens where they cross a building, is settled in the new spec after research. The rule from the owner's other work holds as a default: a structure may restore itself after pack generation, and losing some ore under a building is acceptable.
5. **PersonalSpace is a stretch goal after the port.** Generating Lost Cities inside a personal dimension (a flat world with cities) comes after beta. Nothing before then depends on it, but the overlay design should not rule it out.

## Consequences

- The overworld is the only target, so the RWG work is the critical path. The roadmap puts an RWG overlay spike before any city milestone.
- Upstream's height range (-64 to 320) does not exist in 1.7.10 (0 to 255), and RWG has its own sea level and height profile. The new spec maps every height-dependent number.
- New worlds are the target. Adding cities to chunks of an existing world is not a goal: the border between old and new chunks is the server owner's concern.

## Alternatives considered

- **Both modes, dimension first:** faithful to upstream and works on existing servers. Rejected: GTNH players live in the RWG overworld and later in personal dimensions, so a separate city dimension serves neither.
- **Dimension only:** the simplest integration, but it avoids the hard problem the owner wants solved.

## Still open

- The overlay mechanism and the terrain blend (spike S2).
- Ore-vein coexistence (new spec, after S2).
