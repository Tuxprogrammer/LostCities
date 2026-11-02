/**
 * The JSON asset system: loading, validation and the in-memory model of buildings, parts, palettes and styles. Pure
 * Java, like {@code layout}: {@code LayoutPurityTest} keeps Minecraft types out (CONTRIBUTING § Architecture, D6).
 *
 * <p>
 * {@link com.tuxprogrammer.lostcities.asset.AssetLoader} reads the thirteen asset types from the jar and the config
 * folder into an immutable {@link com.tuxprogrammer.lostcities.asset.AssetSet}, resolving every name and every block at
 * load. Subpackages: {@code block} (the block-name map, decorative metals, the never-place list, variety pools and the
 * resolver), {@code model} (one class per asset type), {@code palette} (layered palette compilation), {@code match}
 * (biome, block and name matchers, weighted picks), {@code transform} (part transforms and metadata rotation) and
 * {@code json} (typed field reads). The format is docs/new-spec/05-assets/asset-format.md; the glue that runs the
 * loader against Forge's registry is {@code core.assets}.
 *
 * <p>
 * Side: both.
 */
package com.tuxprogrammer.lostcities.asset;
