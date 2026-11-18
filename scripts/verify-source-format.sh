#!/usr/bin/env bash

set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

base_ref=${FORMAT_BASE_REF:-origin/main}
if git rev-parse --verify "$base_ref^{commit}" >/dev/null 2>&1; then
    base_commit=$(git merge-base HEAD "$base_ref")
else
    base_commit=$(git rev-parse HEAD^ 2>/dev/null || git hash-object -t tree /dev/null)
fi

if [ "$base_commit" = "$(git rev-parse HEAD)" ]; then
    base_commit=$(git rev-parse HEAD^ 2>/dev/null || git hash-object -t tree /dev/null)
fi

mapfile_command=(git diff --name-only --diff-filter=ACMR "$base_commit" --)
failures=0

while IFS= read -r file; do
    [ -f "$file" ] || continue
    case "$file" in
        core/database/schemas/*|*.png|*.jpg|*.jpeg|*.webp|*.ttf|*.keystore|*.jks)
            continue
            ;;
        *.kt|*.kts|*.xml|*.md|*.yml|*.yaml|*.json|*.sh|*.properties|*.toml)
            ;;
        *)
            continue
            ;;
    esac

    if LC_ALL=C grep -nE '[[:blank:]]+$' "$file" >/dev/null; then
        printf 'FAIL: trailing whitespace in %s\n' "$file" >&2
        failures=$((failures + 1))
    fi
    if [ -s "$file" ] && [ "$(tail -c 1 "$file" | wc -l | tr -d ' ')" -eq 0 ]; then
        printf 'FAIL: missing final newline in %s\n' "$file" >&2
        failures=$((failures + 1))
    fi
done < <("${mapfile_command[@]}")

if [ "$failures" -gt 0 ]; then
    printf 'Source format guard failed with %d issue(s).\n' "$failures" >&2
    exit 1
fi

printf 'Source format guard passed for files changed since %s.\n' "$base_commit"
