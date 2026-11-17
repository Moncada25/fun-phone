#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
dependency_report="$(mktemp)"
trap 'rm -f "$dependency_report"' EXIT

cd "$repo_root"

./gradlew --max-workers=1 :app:processPlayReleaseMainManifest
./gradlew --max-workers=1 :app:dependencies \
  --configuration playReleaseRuntimeClasspath >"$dependency_report"

merged_manifest="$(
  find app/build/intermediates/merged_manifest/playRelease \
    -name AndroidManifest.xml -type f -print -quit 2>/dev/null || true
)"

if [[ -z "$merged_manifest" ]]; then
  echo "Could not find the merged Play release manifest."
  exit 1
fi

for permission in android.permission.INTERNET android.permission.ACCESS_NETWORK_STATE; do
  if ! grep -Fq "$permission" "$merged_manifest"; then
    echo "Play release is missing required network permission: $permission"
    exit 1
  fi
done

if grep -Eq 'com.google.android.gms.permission.AD_ID|android.permission.ACCESS_ADSERVICES_AD_ID' \
  "$merged_manifest"; then
  echo "Play release requests an advertising identifier permission."
  exit 1
fi

if grep -Eqi 'firebase-(analytics|auth|storage|appcheck|crashlytics)|google-services' \
  "$dependency_report"; then
  echo "Play release runtime contains a prohibited Firebase/Google Services dependency."
  exit 1
fi

if grep -Eqi 'logging-interceptor' "$dependency_report"; then
  echo "Play release runtime contains an HTTP logging interceptor."
  exit 1
fi

network_sources="$(
  grep -RlE 'HttpsURLConnection|HttpURLConnection|Retrofit\.Builder|OkHttpClient|WebView' \
    app/src/main/java 2>/dev/null | sort || true
)"
expected_network_sources=$'app/src/main/java/com/bookverse/contacts/data/repository/HolidaysRepository.kt\napp/src/main/java/com/bookverse/contacts/ui/screens/contacts/SocialMediaSearchDialog.kt'
if [[ "$network_sources" != "$expected_network_sources" ]]; then
  echo "Unexpected direct-network source detected:"
  echo "$network_sources"
  exit 1
fi

social_network_source="app/src/main/java/com/bookverse/contacts/ui/screens/contacts/SocialMediaSearchDialog.kt"
if ! grep -Fq 'WebView' "$social_network_source"; then
  echo "The approved social-media network boundary is missing its WebView implementation."
  exit 1
fi

expected_network_source="app/src/main/java/com/bookverse/contacts/data/repository/HolidaysRepository.kt"
if ! grep -Eq 'https://date\.nager\.at/api/v3' "$expected_network_source"; then
  echo "The holidays network boundary is not pinned to date.nager.at."
  exit 1
fi

echo "Privacy-boundary guard passed: approved holidays and social-media preview network access, no Firebase, ads, or telemetry."
