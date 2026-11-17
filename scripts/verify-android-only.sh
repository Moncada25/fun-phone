#!/usr/bin/env bash

set -euo pipefail

repo_root=$(git rev-parse --show-toplevel)
cd "$repo_root"

failures=0

fail() {
    printf 'FAIL: %s\n' "$1"
    failures=$((failures + 1))
}

if [ -e shared ]; then
    fail "shared/ exists; Fun Phone is Android-only"
fi

if [ -e iosApp ]; then
    fail "iosApp/ exists; Fun Phone is Android-only"
fi

ios_project_paths=$(find . -maxdepth 4 \
    \( -name '*.xcodeproj' -o -name '*.xcworkspace' -o -name Podfile \) \
    -not -path './.git/*' -print)
if [ -n "$ios_project_paths" ]; then
    fail "iOS project metadata exists: $ios_project_paths"
fi

if ! grep -Eq '^include\(":app"\)[[:space:]]*$' settings.gradle.kts; then
    fail "settings.gradle.kts must include the Android app module"
fi

unexpected_includes=$(grep -E '^[[:space:]]*include\(' settings.gradle.kts |
    grep -Ev '^include\(":(app|baselineprofile)"\)[[:space:]]*$' || true)
if [ -n "$unexpected_includes" ]; then
    fail "settings.gradle.kts includes modules other than :app and the Android :baselineprofile test module: $unexpected_includes"
fi

kmp_pattern='org\.jetbrains\.kotlin\.multiplatform|kotlin[[:space:]]*\([[:space:]]*"multiplatform"|iosArm64|iosX64|iosSimulatorArm64|iosTarget|macosX64|cocoapods|uikit'
kmp_matches=$(git grep -IlE "$kmp_pattern" -- \
    '*.gradle' '*.gradle.kts' '*.toml' \
    ':!scripts/verify-android-only.sh' \
    ':!FUN_PHONE_EVOLUTION_SPEC.md' \
    ':!FOSS_PLAN.md' \
    ':!PRE_PUBLISH_CHECKLIST.md' || true)
if [ -n "$kmp_matches" ]; then
    fail "KMP/iOS build configuration found in tracked source: $kmp_matches"
fi

if [ "$failures" -ne 0 ]; then
    exit 1
fi

printf 'Android-only guard passed.\n'
