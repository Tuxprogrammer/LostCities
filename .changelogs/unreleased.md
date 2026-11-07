# Unreleased

## Changed
- Link highways that cross now meet at the same height and join in a crossing or T instead of one passing over the other on a bridge; only crossings too close to both cities to ramp stay stacked.
- Cities now build over other mods' structures instead of leaving holes for them, except villages (and RWG2 volcanoes, lava caves and river junctions, unchanged). LootGames dungeons and AE2 meteorites no longer reserve any chunks; Witchery structures and Roguelike Dungeons towers no longer do either, and are skipped where they would stand on a city (Witchery) or steered off it (Roguelike). A profile can opt one back in with `landmarkTreatment` entries such as `witchery=BUILD_AROUND`.
- Cities cut into hills instead of stopping at them: a chunk inside a city is no longer dropped for rising too high, so no more missing blocks at the rim. `cityMaxRise` still caps how high the city's streets climb; the terrain above is cut away.
- Furnishing: GT machines now turn up in shops, offices, residential and kitchen rooms as well as industrial floors, with fewer in basements (same per-chunk tile-entity cap); the district plaza monument is a pillar of stone brick instead of GT casings.

## Fixed
- District plazas are no longer cut in half by a highway or a change of street level across them: a big city's 2 x 2 plaza is drawn only where all four chunks fit, otherwise a one-chunk plaza with its own monument; and a subway station under a plaza puts its entrance in a neighboring chunk instead of breaking into the plaza (the station stays underneath; no entrance if no neighbor is free) (D-plaza-road-1, D-plaza-station-1).
- City link network: a link blocked by a landmark now detours around it instead of being dropped, so cities are no longer leaves with one highway; a highway whose end plaza missed the city runs on as a spur to the first city chunk; a landmark inside a city moves its beltway ring instead of ruling the beltway out (D390 to D392).

## Upgrade notes
- Existing worlds change at the border of newly generated chunks: a city around an old LootGames, meteorite, Witchery or Roguelike lot fills that lot in new chunks, so a cut may show where old and new chunks meet. Chunks already generated are not touched.
