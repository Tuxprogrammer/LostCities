# Bundled Lost Cities assets

Every folder here (buildings, citystyles, conditions, multibuildings, palettes, parts, scattered, stuff, styles, variants, worldstyles) is upstream Lost Cities' JSON asset data, by McJty, MIT licensed: https://github.com/McJtyMods/LostCities at branch 1.20, commit f18a094d. The license text is in THIRD-PARTY-NOTICES.md at the root of the source repository.

The files are copied file for file by tools/asset-convert/convert.py, which applies a few documented conversions to the 1.7.10 vocabulary and adds two rubble variants of this mod's own (variants/sandstone_rubble.json and variants/red_sandstone_rubble.json). Do not edit them by hand: change the script and rerun it. See docs/new-spec/05-assets/asset-format.md, "Bundled assets", and docs/assets/CREDITS.md.
