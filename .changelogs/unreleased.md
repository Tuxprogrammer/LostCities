# Unreleased

## Changed
- Faster chunk generation near city links. On the `perf.sites` scenario (RWG2 continents, seed 4242), server generator time drops by about 10 % on `rarecities` (30.6 s to 27.5 s over six sites) and its worst chunk from 2.4 s to 0.63–0.87 s. On the default profile it drops by about 7 % (31.3 s to 29.2 s) and its worst chunk from 0.62 s to 0.40 s. The overlay's own terrain time is down by about two thirds on `rarecities` (3.5 s to 1.1–1.5 s) and by about a quarter on the default profile (3.1 s to 2.2–2.3 s). What changed:
  - Highway routing asks a coarse per-cell "can a link pass here" mask instead of building full landmark zones. It caches each route and each cell's links and bounds its cell scans. The landmark, village and height oracles keep larger caches (the world cache ceiling B5 is raised to 48 MB, D-perf-1).
  - Landmark types a profile ignores (by default Roguelike Dungeons and Witchery) are no longer looked up at all (D-perf2-oracles-1).
  - The landmark oracles are asked once per 16 × 16-chunk super-cell instead of once per 8 × 8-chunk cell. On the slowest site this cuts the queries about sixfold (D-perf2-route-1, D-perf2-route-3).
  - A link that meets a landmark is detoured around the blocked stretch only, with a bounded search, instead of redrawing the whole line up to 30 times (D-perf2-route-2).
  - City links are chosen from geometry: neighbors in a relative-neighborhood graph, topped up so every city has its 2 nearest. Only the links that reach a chunk are routed, and choosing a cell's links no longer routes every link nearby (D-perf2-links-1 to 3).
- On RWG2 worlds, city plans and city-link routes are worked out on a background thread while the server thread waits on terrain or sits idle between ticks. Routes are worked out in bounded steps, so the background thread never holds the planner for more than about 2 ms (D-perf2-worker-1). Fetches that are not safe off-thread still run on the server thread. Turn it off with `-Dlostcitiesnh.planWorker=false`; worlds come out the same either way.
- Diagnostics: `-Dlostcitiesnh.perfCounters=true` logs per-stage timing counters, with a `.worker` suffix for the background thread's share, and `lostcities test perf.sites` (test hooks on) times chunk generation over a row of sites.

## Upgrade notes
- The city-link network is chosen differently, and detours around landmarks take new shapes. In a world made by an earlier version, links in chunks already generated stay as they are. Chunks generated after the upgrade follow the new network, so a highway or railway can end at the edge of the old ground, or a new one can start there. Cities and their own streets are unchanged.

## Known issues
- The first chunks after a teleport next to an unrouted city link can still take about 0.6 s. The background planner works ahead of a walking player but cannot get ahead of a jump, so this chunk is still over the 10 ms per-chunk budget (B4).
