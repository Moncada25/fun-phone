#!/usr/bin/env bash
set -euo pipefail

package_name="${1:-com.bookverse.contacts.community.debug}"
activity_name="${2:-com.bookverse.contacts.MainActivity}"
iterations="${ITERATIONS:-5}"
report_dir="${REPORT_DIR:-app/build/reports/performance}"
release_like="${RELEASE_LIKE:-false}"
device_stabilized="${DEVICE_STABILIZED:-false}"
measure_contact_scroll="${MEASURE_CONTACT_SCROLL:-false}"
contact_scroll_ready="${CONTACT_SCROLL_READY:-false}"
scroll_swipes="${SCROLL_SWIPES:-12}"
scroll_duration_ms="${SCROLL_DURATION_MS:-400}"
frame_budget_ms="${FRAME_BUDGET_MS:-24}"
prepare_synthetic_contacts="${PREPARE_SYNTHETIC_CONTACTS:-false}"
synthetic_contact_count="${SYNTHETIC_CONTACT_COUNT:-2000}"
benchmark_setup_activity="${BENCHMARK_SETUP_ACTIVITY:-com.bookverse.contacts.benchmark.BenchmarkSetupActivity}"
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

command -v adb >/dev/null || { echo "adb is required"; exit 1; }
adb_command=(adb)
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  adb_command+=(-s "$ANDROID_SERIAL")
  device_state="$("${adb_command[@]}" get-state 2>/dev/null || true)"
  if [[ "$device_state" != "device" ]]; then
    echo "ANDROID_SERIAL must identify one connected, authorized Android device."
    exit 1
  fi
else
  device_count="$(adb devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
  if [[ "$device_count" -ne 1 ]]; then
    echo "Connect exactly one authorized physical Android device or set ANDROID_SERIAL."
    exit 1
  fi
fi
if [[ "$("${adb_command[@]}" shell getprop ro.kernel.qemu | tr -d '\r')" == "1" ]]; then
  echo "FP-205 baselines require a physical mid-tier device, not an emulator."
  exit 1
fi
if ! "${adb_command[@]}" shell pm path "$package_name" >/dev/null 2>&1; then
  echo "Package $package_name is not installed."
  exit 1
fi
package_flags="$(
  "${adb_command[@]}" shell dumpsys package "$package_name" |
    awk '/^[[:space:]]*flags=[[]/ { print; exit }'
)"
package_debuggable=false
if [[ "$package_flags" == *"DEBUGGABLE"* ]]; then
  package_debuggable=true
fi
if ! [[ "$iterations" =~ ^[1-9][0-9]*$ ]]; then
  echo "ITERATIONS must be a positive integer."
  exit 1
fi
if [[ "$release_like" != "true" && "$release_like" != "false" ]]; then
  echo "RELEASE_LIKE must be true or false."
  exit 1
fi
if [[ "$device_stabilized" != "true" && "$device_stabilized" != "false" ]]; then
  echo "DEVICE_STABILIZED must be true or false."
  exit 1
fi
if [[ "$measure_contact_scroll" != "true" && "$measure_contact_scroll" != "false" ]]; then
  echo "MEASURE_CONTACT_SCROLL must be true or false."
  exit 1
fi
if [[ "$contact_scroll_ready" != "true" && "$contact_scroll_ready" != "false" ]]; then
  echo "CONTACT_SCROLL_READY must be true or false."
  exit 1
fi
if [[ "$prepare_synthetic_contacts" != "true" && "$prepare_synthetic_contacts" != "false" ]]; then
  echo "PREPARE_SYNTHETIC_CONTACTS must be true or false."
  exit 1
fi
if [[ "$measure_contact_scroll" == "true" &&
      "$contact_scroll_ready" != "true" &&
      "$prepare_synthetic_contacts" != "true" ]]; then
  echo "Set CONTACT_SCROLL_READY=true only after onboarding is complete and the Contacts route has approved data."
  exit 1
fi
if ! [[ "$synthetic_contact_count" =~ ^[1-9][0-9]*$ ]] ||
  (( synthetic_contact_count < 100 || synthetic_contact_count > 10000 )); then
  echo "SYNTHETIC_CONTACT_COUNT must be between 100 and 10000."
  exit 1
fi
if [[ "$prepare_synthetic_contacts" == "true" && "$package_name" != *.benchmark ]]; then
  echo "Synthetic contact preparation is available only to an isolated benchmark package."
  exit 1
fi
if ! [[ "$scroll_swipes" =~ ^[1-9][0-9]*$ ]]; then
  echo "SCROLL_SWIPES must be a positive integer."
  exit 1
fi
if ! [[ "$scroll_duration_ms" =~ ^[1-9][0-9]*$ ]]; then
  echo "SCROLL_DURATION_MS must be a positive integer."
  exit 1
fi
if ! [[ "$frame_budget_ms" =~ ^[1-9][0-9]*([.][0-9]+)?$ ]]; then
  echo "FRAME_BUDGET_MS must be a positive number."
  exit 1
fi
if [[ "$release_like" == "true" && "$package_debuggable" == "true" ]]; then
  echo "RELEASE_LIKE=true requires a non-debuggable package."
  exit 1
fi

mkdir -p "$report_dir"
report="$report_dir/core-startup.txt"
: > "$report"

if [[ "$prepare_synthetic_contacts" == "true" ]]; then
  "${adb_command[@]}" shell am force-stop "$package_name"
  setup_result="$(
    "${adb_command[@]}" shell am start -W \
      -n "${package_name}/${benchmark_setup_activity}" \
      --ei contact_count "$synthetic_contact_count"
  )"
  if ! grep -Eq '^Status: (ok|warning)$' <<< "$setup_result"; then
    echo "Benchmark synthetic-contact setup did not launch successfully."
    exit 1
  fi
  setup_ready=false
  for _ in $(seq 1 30); do
    top_activity="$(
      "${adb_command[@]}" shell dumpsys activity activities |
        awk '/topResumedActivity=/ && !seen { print; seen=1 }'
    )"
    if [[ "$top_activity" == *"${package_name}/com.bookverse.contacts.MainActivity"* ]]; then
      setup_ready=true
      break
    fi
    sleep 1
  done
  if [[ "$setup_ready" != "true" ]]; then
    echo "Benchmark setup did not finish within 30 seconds."
    exit 1
  fi
fi

{
  echo "package=$package_name"
  echo "device=$("${adb_command[@]}" shell getprop ro.product.model | tr -d '\r')"
  echo "sdk=$("${adb_command[@]}" shell getprop ro.build.version.sdk | tr -d '\r')"
  echo "iterations=$iterations"
  echo "release_like=$release_like"
  echo "device_stabilized=$device_stabilized"
  echo "package_debuggable=$package_debuggable"
  echo "contact_scroll_requested=$measure_contact_scroll"
  echo "synthetic_contacts_prepared=$prepare_synthetic_contacts"
  if [[ "$prepare_synthetic_contacts" == "true" ]]; then
    echo "synthetic_contact_count=$synthetic_contact_count"
  fi
} >> "$report"

total_times=()
for iteration in $(seq 1 "$iterations"); do
  "${adb_command[@]}" shell am force-stop "$package_name"
  result="$("${adb_command[@]}" shell am start -W -n "${package_name}/${activity_name}")"
  total_time="$(awk -F: '/TotalTime:/ { gsub(/ /, "", $2); print $2 }' <<< "$result")"
  wait_time="$(awk -F: '/WaitTime:/ { gsub(/ /, "", $2); print $2 }' <<< "$result")"
  if [[ -z "$total_time" || -z "$wait_time" ]]; then
    echo "Could not read startup timing on iteration $iteration."
    exit 1
  fi
  total_times+=("$total_time")
  echo "run_${iteration}_total_ms=$total_time wait_ms=$wait_time" >> "$report"
done

sorted_times="$(printf '%s\n' "${total_times[@]}" | sort -n)"
median_line=$(( (iterations + 1) / 2 ))
startup_median_ms="$(sed -n "${median_line}p" <<< "$sorted_times")"
startup_worst_ms="$(tail -1 <<< "$sorted_times")"
startup_budget_met=false
if (( startup_median_ms <= 1500 && startup_worst_ms <= 2500 )); then
  startup_budget_met=true
fi

memory_line="$(
  "${adb_command[@]}" shell dumpsys meminfo "$package_name" |
    awk '$1 == "TOTAL" && $2 ~ /^[0-9]+$/ { print; exit }'
)"
total_pss_kb="$(awk '{ print $2 }' <<< "$memory_line")"
private_dirty_kb="$(awk '{ print $3 }' <<< "$memory_line")"
private_dirty_budget_met=false
if [[ -n "$private_dirty_kb" ]] && (( private_dirty_kb <= 184320 )); then
  private_dirty_budget_met=true
fi

contact_scroll_measured=false
contact_scroll_budget_met=false
contact_scroll_frame_count=""
contact_scroll_p90_ms=""
contact_scroll_worst_ms=""
if [[ "$measure_contact_scroll" == "true" ]]; then
  require_scroll_foreground() {
    local top_activity
    top_activity="$(
      "${adb_command[@]}" shell dumpsys activity activities |
        awk '/topResumedActivity=/ && !seen { print; seen=1 }'
    )"
    if [[ "$top_activity" != *"${package_name}/${activity_name}"* ]]; then
      printf 'Contact scroll aborted: benchmark lost foreground; refusing further input.\n' >&2
      return 1
    fi
  }
  require_scroll_foreground
  display_size="$("${adb_command[@]}" shell wm size | awk -F: '/Physical size:/ { gsub(/ /, "", $2); print $2; exit }')"
  display_width="${display_size%x*}"
  display_height="${display_size#*x}"
  if ! [[ "$display_width" =~ ^[1-9][0-9]*$ && "$display_height" =~ ^[1-9][0-9]*$ ]]; then
    echo "Could not determine the physical display size."
    exit 1
  fi

  scroll_x=$(( display_width / 2 ))
  scroll_top=$(( display_height * 3 / 10 ))
  scroll_bottom=$(( display_height * 8 / 10 ))
  framestats_file="$(mktemp)"
  trap 'rm -f "$framestats_file"' EXIT
  "${adb_command[@]}" shell dumpsys gfxinfo "$package_name" reset >/dev/null
  for swipe in $(seq 1 "$scroll_swipes"); do
    require_scroll_foreground
    if (( swipe % 2 == 1 )); then
      "${adb_command[@]}" shell input swipe \
        "$scroll_x" "$scroll_bottom" "$scroll_x" "$scroll_top" "$scroll_duration_ms"
    else
      "${adb_command[@]}" shell input swipe \
        "$scroll_x" "$scroll_top" "$scroll_x" "$scroll_bottom" "$scroll_duration_ms"
    fi
  done
  require_scroll_foreground
  "${adb_command[@]}" shell dumpsys gfxinfo "$package_name" framestats > "$framestats_file"
  parsed_framestats="$("$script_dir/parse-gfx-framestats.sh" "$framestats_file")"
  contact_scroll_frame_count="$(awk -F= '$1 == "frame_count" { print $2 }' <<< "$parsed_framestats")"
  contact_scroll_p90_ms="$(awk -F= '$1 == "frame_p90_ms" { print $2 }' <<< "$parsed_framestats")"
  contact_scroll_worst_ms="$(awk -F= '$1 == "frame_worst_ms" { print $2 }' <<< "$parsed_framestats")"
  contact_scroll_measured=true
  if awk -v observed="$contact_scroll_p90_ms" -v budget="$frame_budget_ms" \
    'BEGIN { exit !(observed <= budget) }'; then
    contact_scroll_budget_met=true
  fi
fi

{
  echo "startup_median_ms=$startup_median_ms"
  echo "startup_worst_ms=$startup_worst_ms"
  echo "startup_budget_met=$startup_budget_met"
  if [[ -n "$total_pss_kb" ]]; then
    echo "total_pss_kb=$total_pss_kb"
  fi
  if [[ -n "$private_dirty_kb" ]]; then
    echo "private_dirty_kb=$private_dirty_kb"
    echo "private_dirty_budget_met=$private_dirty_budget_met"
  fi
  echo "contact_scroll_measured=$contact_scroll_measured"
  if [[ "$contact_scroll_measured" == "true" ]]; then
    echo "contact_scroll_frame_count=$contact_scroll_frame_count"
    echo "contact_scroll_p90_ms=$contact_scroll_p90_ms"
    echo "contact_scroll_worst_ms=$contact_scroll_worst_ms"
    echo "contact_scroll_budget_ms=$frame_budget_ms"
    echo "contact_scroll_budget_met=$contact_scroll_budget_met"
  fi
  if [[ "$release_like" == "true" && "$device_stabilized" == "true" ]]; then
    echo "release_gate_eligible=true"
  else
    echo "release_gate_eligible=false"
  fi
  if [[ "$release_like" == "true" &&
        "$device_stabilized" == "true" &&
        "$contact_scroll_measured" == "true" &&
        "$contact_scroll_budget_met" == "true" ]]; then
    echo "perf_02_evidence_eligible=true"
  else
    echo "perf_02_evidence_eligible=false"
  fi
} >> "$report"

echo "Sanitized performance evidence written to $report"
