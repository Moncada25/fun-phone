#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
release_file="$repo_root/release.properties"
manifest_file="$repo_root/app/build/reports/release/release-manifest.json"

if [[ ! -f "$release_file" ]]; then
  echo "Missing release.properties"
  exit 1
fi

read_property() {
  local key="$1"
  sed -n "s/^${key}=//p" "$release_file"
}

version_name="$(read_property VERSION_NAME)"
version_code="$(read_property VERSION_CODE)"
min_sdk="$(read_property MIN_SDK)"
target_sdk="$(read_property TARGET_SDK)"

for value_name in version_name version_code min_sdk target_sdk; do
  if [[ -z "${!value_name}" ]]; then
    echo "Missing ${value_name} in release.properties"
    exit 1
  fi
done

if [[ ! -f "$manifest_file" ]]; then
  echo "Missing generated release manifest. Run ./gradlew :app:generateReleaseManifest."
  exit 1
fi

python3 - "$manifest_file" "$version_name" "$version_code" "$min_sdk" "$target_sdk" <<'PY'
import json
import sys

path, version_name, version_code, min_sdk, target_sdk = sys.argv[1:]
with open(path, encoding="utf-8") as source:
    manifest = json.load(source)

expected = {
    "versionName": version_name,
    "versionCode": int(version_code),
    "minSdk": int(min_sdk),
    "targetSdk": int(target_sdk),
}
for key, value in expected.items():
    if manifest.get(key) != value:
        raise SystemExit(f"Release manifest {key}={manifest.get(key)!r}; expected {value!r}")

distributions = manifest.get("distributions", {})
if distributions.get("community", {}).get("firebase") is not False:
    raise SystemExit("Community release metadata must declare firebase=false")
if distributions.get("play", {}).get("firebase") is not False:
    raise SystemExit("Play release metadata must declare firebase=false")
PY

echo "Release metadata is synchronized (${version_name}, code ${version_code})."
