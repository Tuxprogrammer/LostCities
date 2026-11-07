# Asset credits

Where every asset in the jar comes from, as CONTRIBUTING § Assets and localization requires: source, author and license. The mod ships no textures or sounds.

| Path in the jar | What | Source | Author | License |
|---|---|---|---|---|
| `data/lostcities/lostcities/` (322 files: buildings, multibuildings, parts, palettes, variants, styles, city styles, conditions, world styles, scattered, stuff) | Upstream Lost Cities' JSON asset data, copied file for file by `tools/asset-convert/convert.py`, with the conversions listed in [asset-format.md § Bundled assets](../new-spec/05-assets/asset-format.md#bundled-assets) | [Lost Cities](https://github.com/McJtyMods/LostCities), branch `1.20` at `f18a094d` | McJty | MIT ([THIRD-PARTY-NOTICES.md](../../THIRD-PARTY-NOTICES.md)) |
| `data/lostcities/lostcities/variants/sandstone_rubble.json`, `red_sandstone_rubble.json` | Desert rubble mixes, shaped like upstream's `stonebrick_rubble` (P05-5) | This mod | Lost Cities: New Horizons contributors | MIT |
| `data/lostcities/lostcities/palettes/bricks_desert_rough.json`, `bricks_desert_bleached.json`, `bricks_desert_taupe.json` | Muted desert building palettes, shaped like upstream's `bricks_desert` (AR-16) | This mod | Lost Cities: New Horizons contributors | MIT |
| `data/lostcities/lostcities/buildings/nh_*.json`, `parts/nh_*.json`, `palettes/nh_residential.json` (20 files) | Original low-density buildings (houses and low-rises, city-realism Phase 2d), drawn by `tools/asset-convert/originals.py`: original work, not derived from upstream | This mod | Lost Cities: New Horizons contributors | MIT |
| `data/lostcities/lostcities/parts/nh_highway_tunnel*.json` (4 files) | Upstream's `highway_tunnel*` parts with 16 ceiling blocks turned into glowstone (D-tunnel-light-1), written by `tools/asset-convert/convert.py` | McJty, Lost Cities (`1.20`, f18a094d), with this mod's change | McJty; Lost Cities: New Horizons contributors | MIT |
| `data/lostcitiesnh/blockmap/` (`mapping.json`, `metals.json`, `never_place.json`, `pools.json`) | The block-name map, decorative metals, never-place list and variety pools, from [block-mapping.md](../new-spec/00-pack-integration/block-mapping.md) and the [block catalog](../new-spec/05-assets/block-catalog/index.md) | This mod | Lost Cities: New Horizons contributors | MIT |
| `data/lostcitiesnh/assets.index` | The list of bundled asset files | This mod (generated) | Lost Cities: New Horizons contributors | MIT |
| `assets/lostcitiesnh/lang/en_US.lang` | English lang file | This mod | Lost Cities: New Horizons contributors | MIT |
