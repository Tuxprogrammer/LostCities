#!/usr/bin/env bash
# lc-sync.sh <slot> <git-rev> [--bundle <file>]
# Create or update the checkout <root>/wt/<slot> at <git-rev> (a branch, tag or sha).
# Source: https://github.com/Tuxprogrammer/LostCities.git, or a git bundle pushed from a laptop
# (--bundle <file>, or LC_BUNDLE=<file>) when https is unavailable or the rev is not pushed.
# Leaves a detached HEAD at the rev, removes untracked files except build/, run/ and .gradle/ caches,
# and deletes build/libs/*.jar so a slot never offers a jar from an older checkout.
set -euo pipefail
LC="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
URL="${LC_REPO_URL:-https://github.com/Tuxprogrammer/LostCities.git}"
slot="${1:?usage: lc-sync.sh <slot> <git-rev> [--bundle <file>]}"; rev="${2:?usage: lc-sync.sh <slot> <git-rev> [--bundle <file>]}"
shift 2
BUNDLE="${LC_BUNDLE:-}"
while [ $# -gt 0 ]; do case "$1" in --bundle) BUNDLE="$2"; shift 2 ;; *) echo "unknown argument: $1" >&2; exit 2 ;; esac; done
[[ "$slot" =~ ^[A-Za-z0-9_.-]+$ ]] || { echo "bad slot name: $slot" >&2; exit 2; }
W="$LC/wt/$slot"
mkdir -p "$LC/wt"
exec 8>"$LC/wt/.$slot.lock"; flock -n 8 || { echo "slot $slot is busy" >&2; exit 3; }
if [ ! -d "$W/.git" ]; then
    git init -q "$W"
    git -C "$W" remote add origin "$URL"
fi
if [ -n "$BUNDLE" ]; then
    git -C "$W" bundle verify -q "$BUNDLE" >/dev/null 2>&1 || { echo "bad bundle: $BUNDLE" >&2; exit 4; }
    git -C "$W" fetch -q "$BUNDLE" '+refs/*:refs/bundle/*'
else
    git -C "$W" fetch -q --tags origin '+refs/heads/*:refs/remotes/origin/*'
fi
sha=""
for cand in "refs/bundle/heads/$rev" "refs/remotes/origin/$rev" "refs/tags/$rev" "$rev"; do
    if sha="$(git -C "$W" rev-parse -q --verify "$cand^{commit}" 2>/dev/null)"; then break; fi
    sha=""
done
[ -n "$sha" ] || { echo "rev not found: $rev" >&2; exit 4; }
git -C "$W" checkout -q --force --detach "$sha"
git -C "$W" clean -q -fdx -e build/ -e run/ -e .gradle/
rm -f "$W"/build/libs/*.jar   # jars in a slot are always from its current checkout
echo "slot=$slot rev=$rev sha=$sha dir=$W"
git -C "$W" log -1 --oneline
