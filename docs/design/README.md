# Design records

Accepted design decisions for Lost Cities: New Horizons. Larger decisions get a numbered record, from the [template](../templates/decision-record.md). Smaller ones are rows in the [decision log](decision-log.md). When a decision changes, its record is edited in place; git keeps the history.

| Record | Status | Settles |
|---|---|---|
| [0001](0001-port-policy-and-licensing.md) | Accepted | Upstream baseline `1.20` at `f18a094d`; MIT code ported under the ported-code gate; upstream JSON assets reused; our work MIT |
| [0002](0002-target-pack.md) | Accepted | GTNH 2.9.0 is the target; dependency pins; the reference pack |
| [0003](0003-world-presence.md) | Accepted | Cities generate in the GTNH overworld over RWG2 Continents and classic RWG; no Lost City dimension; PersonalSpace after the port |
| [0004](0004-gtnh-layer-and-tier-ceiling.md) | Accepted | Faithful cities with a GTNH layer; nothing above EV in the overworld, weighted low; balance waits for beta; buildings players can move into |
| [decision-log.md](decision-log.md) | Running | Process, spec depth, tooling, code layout and testing and gate decisions (D1 to D8) |
