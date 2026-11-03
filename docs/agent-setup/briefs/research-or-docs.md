<!-- Brief template: research, cataloging, spec writing, audits, "apply decision X to docs". Spawn with model: sonnet, effort: medium. -->

**Read-only research** (or: docs pass limited to `<paths>`) for <project>.

## Question / task
<one or two sentences. For catalog work, give the exact output table columns.>

## Scope
- Sources: `<paths or repo@tag>`. Find with the semantic search tool or `rg -l`, then read only matching files by line range.
- Output: `<one file path>`, at most <N> lines. Facts with citations (repo@tag:path:line); no prose beyond what the table needs.

## Limits (hard)
- At most 80 calls; at 150k context, write what you have and STOP.
- No Gradle, no Minecraft, no edits outside the output file.
- Do not re-read CLAUDE.md; do not `cat` whole docs; never `Read` a spilled tool-results file whole.

## Report (at most 10 lines)
Output path · rows or claims added · open questions (at most 5) · anything you could not verify.
