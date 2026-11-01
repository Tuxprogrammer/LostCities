# Block contact sheets

Cheap review renders for the decorative block catalog (decision log D25). Each sheet is a grid of small isometric cubes made from a block's textures, read straight from the mod jars in the reference pack, labeled with registry name and metadata. They judge look and feel against upstream Lost Cities before a block goes into a palette. They are not in-game renders: no lighting, connected textures or models.

Sheets are local review material. The PNGs go to `docs/previews/blocks/`, which is git-ignored. The JSON entry lists that produce them may be committed, so a sheet can be rebuilt.

## Usage

```sh
python3 -I tools/block-contact-sheet/contact_sheet.py tools/block-contact-sheet/sheets/chisel/marble.json \
    --out docs/previews/blocks/chisel/marble.png [--cube 96] [--cols 8]
python3 -I tools/block-contact-sheet/test_contact_sheet.py    # offline tests
```

A sheet file:

```json
{"title": "Chisel: marble",
 "entries": [
   {"label": "chisel:marble:0", "name": "Marble", "all": "chisel-2.17.34-GTNH.jar!assets/chisel/textures/blocks/marble/raw.png"},
   {"label": "chisel:marble:1", "top": "<jar>!<path>", "side": "<jar>!<path>", "note": "pillar"}
 ]}
```

- Texture references are `<jar file name>!<path inside the jar>`, resolved against `--mods` (default `reference/gtnh/2.9.0/.minecraft/mods/` in this checkout), or a plain file path. The jar name `minecraft.jar` means the vanilla 1.7.10 client jar: `$LC_MC_CLIENT_JAR` if set, else RetroFuturaGradle's cache (`~/.gradle/caches/retro_futura_gradle/mc-vanilla/1.7.10/client.jar`, present after any build of this repo).
- **Icon atlases** (one PNG holding several square icons in a row or grid, such as Railcraft's `TextureAtlasSheet` strips): append `#<n>` to the reference to take the n-th square tile, counted row-major from 0, with the tile side equal to the image's shorter edge (`Railcraft-9.17.36.jar!assets/railcraft/textures/blocks/brick.abyssal.png#2`).
- `all` sets every face; `top` and `side` set them separately.
- `tint` (`"#rrggbb"`) multiplies every face by a color, for textures the game colors at runtime (GT material blocks and glass, Railcraft strengthened glass). It approximates the in-game color.
- Animated strips use their first frame. A missing texture renders as a magenta checker, and the run lists it.
- **Connected-texture atlases** (Chisel's `*-ctm.png` and similar) are tiled sheets, not faces. Point the entry at the block's plain face texture instead, which is usually the non-`ctm` file next to it.

Jars and textures are untrusted data: the tool only reads them as zip archives and decodes them as images.
