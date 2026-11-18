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
    grep -Ev '^include\(":(app|baselineprofile|(core|feature):[a-z]+)"\)[[:space:]]*$' || true)
if [ -n "$unexpected_includes" ]; then
    fail "settings.gradle.kts includes modules other than :app, :core:* and :feature:* Android libraries and the Android :baselineprofile test module: $unexpected_includes"
fi

library_modules=$(sed -nE 's/^include\(":((core|feature):[a-z]+)"\)[[:space:]]*$/\1/p' settings.gradle.kts)
for library_module in $library_modules; do
    library_build_file="${library_module/://}/build.gradle.kts"
    if [ ! -f "$library_build_file" ]; then
        fail ":$library_module has no $library_build_file"
    elif ! grep -Eq 'libs\.plugins\.funphone\.android\.library' "$library_build_file"; then
        fail ":$library_module must apply the funphone.android.library convention plugin"
    fi
done

kmp_pattern='org\.jetbrains\.kotlin\.multiplatform|kotlin[[:space:]]*\([[:space:]]*"multiplatform"|iosArm64|iosX64|iosSimulatorArm64|iosTarget|macosX64|cocoapods|uikit'
kmp_matches=$(git grep -IlE "$kmp_pattern" -- \
    '*.gradle' '*.gradle.kts' '*.toml' 'build-logic/*.kt' \
    ':!scripts/verify-android-only.sh' \
    ':!FUN_PHONE_EVOLUTION_SPEC.md' || true)
if [ -n "$kmp_matches" ]; then
    fail "KMP/iOS build configuration found in tracked source: $kmp_matches"
fi

if [ "$failures" -ne 0 ]; then
    exit 1
fi

printf 'Android-only guard passed.\n'
