#!/usr/bin/env bash
# contact-sheet.sh - tile a directory of screenshots into labelled contact sheets so an agent reads
# one ~1500-token image instead of N full-size shots. Needs ImageMagick (`montage`), which is easy
# to install on the build server; run it there and fetch only the sheets.
#
# Usage: contact-sheet.sh <shot-dir> [out-dir=<shot-dir>/sheets] [tile=3x2] [width=400]
set -eu
dir="$1"; out="${2:-$1/sheets}"; tile="${3:-3x2}"; w="${4:-400}"
mkdir -p "$out"
per=$(( ${tile%x*} * ${tile#*x} ))
mapfile -t shots < <(find "$dir" -maxdepth 1 -type f \( -name '*.png' -o -name '*.jpg' \) | sort)
n=${#shots[@]}; i=0; k=0
while [ $i -lt $n ]; do
  montage -label '%t' "${shots[@]:i:per}" -tile "$tile" -geometry "${w}x+4+4" -pointsize 14 \
    "$out/sheet-$(printf %02d $k).jpg"
  i=$((i + per)); k=$((k + 1))
done
echo "contact-sheet: $n shots -> $k sheets in $out (tile $tile, ${w}px)"
