# Unreleased

## Changed
- Faster chunk generation near city links. Highway routing now asks a coarse per-cell "can a link pass here" mask instead of building full landmark zones, caches each route and each cell's links, and bounds its cell scans. The landmark, village and height oracles keep larger caches (world cache ceiling B5 raised to 48 MB, D-perf-1). On the `perf.sites` scenario (RWG2 continents, seed 4242), `rarecities` uses 8–11 % less server generator time, and its worst chunk drops from 2.4 s to 0.7 s; the default profile uses about 7 % less, and its worst chunk drops from 0.62 s to 0.44 s.
- On RWG2 worlds, city plans are now worked out on a background thread while the server thread waits on terrain or sits idle between ticks. Fetches that are not safe off-thread still run on the server thread. Turn it off with `-Dlostcitiesnh.planWorker=false`.
- Diagnostics: `-Dlostcitiesnh.perfCounters=true` logs per-stage timing counters, and `lostcities test perf.sites` (test hooks on) times chunk generation over a row of sites.
