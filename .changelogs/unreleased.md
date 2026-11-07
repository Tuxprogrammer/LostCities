# Unreleased

## Changed
- Cities now build over other mods' structures instead of leaving holes for them, except villages (and RWG2 volcanoes, lava caves and river junctions, unchanged). LootGames dungeons and AE2 meteorites no longer reserve any chunks; Witchery structures and Roguelike Dungeons towers no longer do either, and are skipped where they would stand on a city (Witchery) or steered off it (Roguelike). A profile can opt one back in with `landmarkTreatment` entries such as `witchery=BUILD_AROUND`.
- Cities cut into hills instead of stopping at them: a chunk inside a city is no longer dropped for rising too high, so no more missing blocks at the rim. `cityMaxRise` still caps how high the city's streets climb; the terrain above is cut away.
- Furnishing: GT machines now turn up in shops, offices, residential and kitchen rooms as well as industrial floors, with fewer in basements (same per-chunk tile-entity cap); the district plaza monument is a pillar of stone brick instead of GT casings.
- The mod's own houses now have 3-block-high rooms (5-block floors), and the workshop a 5-block-high bay under 3-block-high upper floors; towers and upstream buildings keep the world's floor pitch.

## Fixed
- City link network: a link blocked by a landmark now detours around it instead of being dropped, so cities are no longer leaves with one highway; a highway whose end plaza missed the city runs on as a spur to the first city chunk; a landmark inside a city moves its beltway ring instead of ruling the beltway out (D390 to D392).

## Upgrade notes
- Existing worlds change at the border of newly generated chunks: a city around an old LootGames, meteorite, Witchery or Roguelike lot fills that lot in new chunks, so a cut may show where old and new chunks meet. Chunks already generated are not touched.
