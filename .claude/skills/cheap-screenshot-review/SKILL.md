---
name: cheap-screenshot-review
description: Use when checking screenshots, renders or shot-list output visually - Minecraft shots, UI captures, before/after comparisons - before opening any image file with Read.
---

# Look at screenshots cheaply

A 1600x900 screenshot costs on the order of 1,700 tokens. It is then re-read on every later call, so thirty shots opened early in a long session cost far more than the shots themselves. Image-heavy agents were among the most expensive in the reviews this guidance comes from.

## Rules

1. **Machine checks first.** Decide pass or fail from logs, markers, block counts and hashes where you can. Look at images only for what a check cannot see.
2. **Contact sheet, not single shots.** Tile a run's shots into one image, about 6 tiles at 400 px wide with labels (`tools/agent/contact-sheet.sh`). Read the sheet once.
3. **Zoom only on suspects.** Open a crop or a single shot at full size only for a tile that looks wrong. Write down what you saw, and do not re-open it.
4. **Diff before looking.** For before/after shots, compute a pixel diff or changed-area percentage and look only where it changed.
5. **Never re-read an image you already described.** Your description is in context; the image adds nothing new.
6. **Hand image-heavy review to a separate short agent** (a cheaper model), which reports findings in text. The builder never carries the images.

For what to capture and what breaks under shaders, see `client-rendering-shaders`.
