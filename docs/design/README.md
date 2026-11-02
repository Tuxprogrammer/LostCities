# Design records

Accepted design decisions for Lost Cities: New Horizons. Larger decisions get a numbered record, from the [template](../templates/decision-record.md). Smaller ones are rows in the [decision log](decision-log.md). When a decision changes, its record is edited in place; git keeps the history.

| Record | Status | Settles |
|---|---|---|
| [0001](0001-port-policy-and-licensing.md) | Accepted | Upstream baseline `1.20` at `f18a094d`; MIT code ported under the ported-code gate; upstream JSON assets reused; our work MIT |
| [0002](0002-target-pack.md) | Accepted | GTNH 2.9.0 is the target; dependency pins; the reference pack |
| [0003](0003-world-presence.md) | Accepted | Cities generate in the GTNH overworld over RWG2 Continents and classic RWG; no Lost City dimension; PersonalSpace after the port |
| [0004](0004-gtnh-layer-and-tier-ceiling.md) | Accepted | Faithful cities with a GTNH layer; nothing above EV in the overworld, weighted low; balance waits for beta; buildings players can move into |
| [0005](0005-rwg-overlay.md) | Accepted | The RWG overlay: a chunk-provider wrapper installed at world creation; RWG's landscape first, the city set into it and its footprint cleared; height oracle; profile pinning; the time budget |
| [0006](0006-build-around-other-worldgen.md) | Accepted | Cities build around other mods' structures: pure structure oracles, landmark cores the city never writes in, edge rings of parks and plazas, soft edges, villages sewn in as old towns; deep railways may pass under a core (D30) and shallow ones dip under it (D31) |
| [0007](0007-loot-tier-map.md) | Accepted | City loot: six `ChestGenHooks` categories in tier bands, Steam to EV plus rail; one bundled file is the defaults and the tier map; the ceiling checked by a unit test and the dev self-check; upstream's `chestloot` value converted to a band |
| [decision-log.md](decision-log.md) | Running | Process, spec depth, tooling, code layout and testing, gate, scope, asset, loot, worldgen, licensing, coexistence and block decisions (D1 to D143) |
