#!/usr/bin/env bash
#
# docs/architecture/components.md claims to catalogue every Java package in the
# library. A catalogue with a hole in it reads as complete, so the next reader
# concludes the package they cannot find does not exist. This makes the hole a
# failing check instead.
#
# It also holds the two dependency rules from docs/architecture/README.md §4:
#   - ch.qos.logback.core never imports ch.qos.logback.classic
#   - android.* is imported only from the packages listed in ANDROID_PACKAGES
#
# What it cannot check is whether the prose beside each name is still true.
# That is review; see .claude/skills/architecture-sync/SKILL.md.
#
# Run: ./scripts/check-architecture-docs.sh

set -euo pipefail

cd "$(dirname "$0")/.."

SRC=logback-android/src/main/java
CATALOGUE=docs/architecture/components.md

# Keep in step with docs/architecture/README.md §4.
ANDROID_PACKAGES="
ch/qos/logback/classic/android
ch/qos/logback/core/android
ch/qos/logback/core/net/ssl
"

status=0

packages=$(find "$SRC" -name '*.java' -exec dirname {} \; | sed "s|^$SRC/||" | sort -u)
catalogued=$(sed -n 's/^### `\([^`]*\)`.*/\1/p' "$CATALOGUE" | sort -u)

missing=$(comm -23 <(echo "$packages") <(echo "$catalogued"))
stale=$(comm -13 <(echo "$packages") <(echo "$catalogued"))

if [ -n "$missing" ]; then
  echo "$CATALOGUE: no entry for these packages (add a '### \`<path>\`' heading):"
  echo "$missing" | sed 's/^/  /'
  status=1
fi

if [ -n "$stale" ]; then
  echo "$CATALOGUE: entries for packages that no longer exist under $SRC:"
  echo "$stale" | sed 's/^/  /'
  status=1
fi

core_to_classic=$(grep -rl 'import ch\.qos\.logback\.classic' "$SRC/ch/qos/logback/core" || true)
if [ -n "$core_to_classic" ]; then
  echo "core must not depend on classic (docs/architecture/README.md §4):"
  echo "$core_to_classic" | sed 's/^/  /'
  status=1
fi

android_users=$({ grep -rlE '^import (static )?android\.' "$SRC" || true; } | xargs -r -n1 dirname | sed "s|^$SRC/||" | sort -u)
unexpected=$(comm -23 <(echo "$android_users") <(echo "$ANDROID_PACKAGES" | sed '/^$/d' | sort -u))
if [ -n "$unexpected" ]; then
  echo "android.* imported outside the packages allowed in docs/architecture/README.md §4:"
  echo "$unexpected" | sed 's/^/  /'
  status=1
fi

if [ "$status" -eq 0 ]; then
  echo "Architecture docs: $(echo "$packages" | wc -l | tr -d ' ') packages catalogued, dependency rules hold."
fi
exit "$status"
