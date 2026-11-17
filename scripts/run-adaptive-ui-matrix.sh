#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
serial="${ANDROID_SERIAL:-${1:-}}"
app_package="com.bookverse.contacts.community.debug"
test_package="com.bookverse.contacts.community.debug.test"
runner="androidx.test.runner.AndroidJUnitRunner"

if [[ -z "$serial" ]]; then
  connected_devices="$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')"
  device_count="$(printf '%s\n' "$connected_devices" | awk 'NF { count++ } END { print count + 0 }')"
  if [[ "$device_count" -ne 1 ]]; then
    echo "Set ANDROID_SERIAL when zero or multiple Android devices are connected." >&2
    exit 1
  fi
  serial="$connected_devices"
fi

adb_device=(adb -s "$serial")
"${adb_device[@]}" get-state >/dev/null

if [[ "$("${adb_device[@]}" shell getprop ro.kernel.qemu | tr -d '\r')" != "1" ]]; then
  echo "Adaptive matrix changes display settings and is restricted to an emulator." >&2
  exit 1
fi

size_state="$("${adb_device[@]}" shell wm size | tr -d '\r')"
density_state="$("${adb_device[@]}" shell wm density | tr -d '\r')"
font_scale="$("${adb_device[@]}" shell settings get system font_scale | tr -d '\r')"
night_mode="$("${adb_device[@]}" shell cmd uimode night | sed 's/^Night mode: //' | tr -d '\r')"
app_locale_output="$("${adb_device[@]}" shell cmd locale get-app-locales "$app_package" 2>/dev/null | tr -d '\r' || true)"
app_locales="$(sed -n 's/.*\\[\\(.*\\)\\].*/\\1/p' <<<"$app_locale_output")"

restore_device() {
  "${adb_device[@]}" shell wm size reset >/dev/null
  if [[ "$size_state" == *"Override size:"* ]]; then
    "${adb_device[@]}" shell wm size "${size_state##*Override size: }" >/dev/null
  fi

  "${adb_device[@]}" shell wm density reset >/dev/null
  if [[ "$density_state" == *"Override density:"* ]]; then
    "${adb_device[@]}" shell wm density "${density_state##*Override density: }" >/dev/null
  fi

  "${adb_device[@]}" shell settings put system font_scale "$font_scale" >/dev/null
  "${adb_device[@]}" shell cmd uimode night "$night_mode" >/dev/null
  if [[ -n "$app_locales" ]]; then
    "${adb_device[@]}" shell cmd locale set-app-locales "$app_package" \
      --locales "$app_locales" >/dev/null 2>&1 || true
  else
    "${adb_device[@]}" shell cmd locale set-app-locales "$app_package" >/dev/null 2>&1 || true
  fi
}
trap restore_device EXIT

cd "$repo_root"
./gradlew :app:assembleCommunityDebug :app:assembleCommunityDebugAndroidTest

main_apk="app/build/outputs/apk/community/debug/app-community-debug.apk"
test_apk="app/build/outputs/apk/androidTest/community/debug/app-community-debug-androidTest.apk"
"${adb_device[@]}" install -r -t "$main_apk" >/dev/null
"${adb_device[@]}" install -r -t "$test_apk" >/dev/null

test_classes="com.bookverse.contacts.ui.screens.settings.OnboardingFlowScreenTest"
test_classes+=",com.bookverse.contacts.ui.screens.settings.OnboardingRestorationTest"
test_classes+=",com.bookverse.contacts.ui.navigation.MainTabNavigationBarTest"
test_classes+=",com.bookverse.contacts.ui.screens.settings.BlockedNumbersDialogsTest"
test_classes+=",com.bookverse.contacts.ui.screens.tools.ToolsPresetSelectorTest"
test_classes+=",com.bookverse.contacts.ui.AccessibilityCoreContractTest"
test_classes+=",com.bookverse.contacts.ui.auth.PasswordAuthDialogsTest"
test_classes+=",com.bookverse.contacts.ui.screens.contacts.dialer.DialerNumberEditorTest"

scenarios=(
  "split-width-proxy|360|720|1.3|no|es"
  "compact-landscape|640|360|1.3|no|en-US"
  "medium-long-locale|800|1067|1.3|yes|de"
  "expanded-large-text|1000|1280|2.0|yes|en-US"
)

for scenario in "${scenarios[@]}"; do
  IFS='|' read -r name width height scale night locale <<<"$scenario"
  echo "Running $name: ${width}x${height}dp, font=$scale, night=$night, locale=$locale"
  "${adb_device[@]}" shell wm size "${width}dpx${height}dp" >/dev/null
  "${adb_device[@]}" shell settings put system font_scale "$scale" >/dev/null
  "${adb_device[@]}" shell cmd uimode night "$night" >/dev/null
  "${adb_device[@]}" shell cmd locale set-app-locales "$app_package" \
    --locales "$locale" >/dev/null

  instrument_output="$(
    "${adb_device[@]}" shell am instrument -w -r \
      -e class "$test_classes" \
      "$test_package/$runner"
  )"
  if ! grep -q '^OK (' <<<"$instrument_output"; then
    echo "$instrument_output" >&2
    echo "Adaptive scenario failed: $name" >&2
    exit 1
  fi
  grep '^OK (' <<<"$instrument_output"
done

echo "Adaptive UI matrix passed on emulator $serial."
