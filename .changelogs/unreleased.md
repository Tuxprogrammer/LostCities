# Unreleased

## Changed
- Cities now build over other mods' structures instead of leaving holes for them, except villages (and RWG2 volcanoes, lava caves and river junctions, unchanged). LootGames dungeons and AE2 meteorites no longer reserve any chunks; Witchery structures and Roguelike Dungeons towers no longer do either, and are skipped where they would stand on a city (Witchery) or steered off it (Roguelike). A profile can opt one back in with `landmarkTreatment` entries such as `witchery=BUILD_AROUND`.
- Cities cut into hills instead of stopping at them: a chunk inside a city is no longer dropped for rising too high, so no more missing blocks at the rim. `cityMaxRise` still caps how high the city's streets climb; the terrain above is cut away.
- Furnishing: GT machines now turn up in shops, offices, residential and kitchen rooms as well as industrial floors, with fewer in basements (same per-chunk tile-entity cap); the district plaza monument is a pillar of stone brick instead of GT casings.

## Fixed
- Buildings near explosion damage no longer get holes in their floors, and their spawners no longer end up as pig spawners: the floating-fragment cleanup leaves a building's floors, walls and furniture where they were built.
- No more pig spawners in cities: a spawner, chest or GT machine that explosion debris knocked off its floor is still given its mob, loot or machine, or becomes air; and furnished GT machines show their proper name and model at once instead of a nameless ".name" block until the world is reloaded.
- City link network: a link blocked by a landmark now detours around it instead of being dropped, so cities are no longer leaves with one highway; a highway whose end plaza missed the city runs on as a spur to the first city chunk; a landmark inside a city moves its beltway ring instead of ruling the beltway out (D390 to D392).

## Upgrade notes
- Existing worlds change at the border of newly generated chunks: a city around an old LootGames, meteorite, Witchery or Roguelike lot fills that lot in new chunks, so a cut may show where old and new chunks meet. Chunks already generated are not touched.
