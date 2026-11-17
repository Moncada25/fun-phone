#!/usr/bin/env bash
set -euo pipefail

listing="${1:-PLAY_STORE_LISTING.md}"
locales=(English Español Deutsch Français Português)

if [[ ! -f "$listing" ]]; then
  echo "Missing Play listing source: $listing"
  exit 1
fi

for locale in "${locales[@]}"; do
  if ! grep -Fxq "## $locale" "$listing"; then
    echo "Missing Play listing locale section: $locale"
    exit 1
  fi
  if ! grep -Fxq "### $locale" "$listing"; then
    echo "Missing What's new locale section: $locale"
    exit 1
  fi

  title="$(
    awk -v locale="## $locale" '
      $0 == locale { in_locale = 1; next }
      in_locale && /^## / { exit }
      in_locale && /^Title: / { sub(/^Title: /, ""); print; exit }
    ' "$listing"
  )"
  if [[ -z "$title" ]] || (( ${#title} > 30 )); then
    echo "Missing title or title exceeds Play's 30-character limit for $locale."
    exit 1
  fi

  short_description="$(
    awk -v locale="## $locale" '
      $0 == locale { in_locale = 1; next }
      in_locale && /^## / { exit }
      in_locale && /^\*\*/ { read_description = 1; next }
      read_description && NF { print; exit }
    ' "$listing"
  )"
  if [[ -z "$short_description" ]]; then
    echo "Missing short description for $locale."
    exit 1
  fi
  if (( ${#short_description} > 80 )); then
    echo "Short description for $locale exceeds Play's 80-character limit."
    exit 1
  fi

  long_description="$(
    awk -v locale="## $locale" '
      $0 == locale { in_locale = 1; next }
      in_locale && /^## / { exit }
      in_locale && /^\*\*/ {
        label_count++
        if (label_count == 2) {
          read_description = 1
        }
        next
      }
      read_description { print }
    ' "$listing"
  )"
  if [[ -z "${long_description//[[:space:]]/}" ]]; then
    echo "Missing long description for $locale."
    exit 1
  fi
  if (( ${#long_description} > 4000 )); then
    echo "Long description for $locale exceeds Play's 4,000-character limit."
    exit 1
  fi

  release_notes="$(
    awk -v locale="### $locale" '
      $0 == locale { in_locale = 1; next }
      in_locale && /^(##|###) / { exit }
      in_locale { print }
    ' "$listing"
  )"
  if [[ -z "${release_notes//[[:space:]]/}" ]]; then
    echo "Missing release notes for $locale."
    exit 1
  fi
  if (( ${#release_notes} > 500 )); then
    echo "Release notes for $locale exceed Play's 500-character limit."
    exit 1
  fi
done

screenshot_rows="$(
  awk -F'|' \
    '$2 ~ /^[[:space:]]*[1-8][[:space:]]*$/ { count++ } END { print count + 0 }' \
    "$listing"
)"
if [[ "$screenshot_rows" -ne 8 ]]; then
  echo "Expected eight localized screenshot-caption rows."
  exit 1
fi

echo "Play listing localization guard passed for ${#locales[@]} locales."

python3 "$(dirname "$0")/export-play-listing.py" --check --source "$listing"
