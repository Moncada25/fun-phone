#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
parser="$script_dir/parse-gfx-framestats.sh"
collector="$script_dir/measure-core-performance.sh"

bash -n "$parser" "$collector"

actual="$(
  {
    printf '%s\n' \
      '---PROFILEDATA---' \
      'Flags,FrameTimelineVsyncId,IntendedVsync,FrameCompleted,'
    for duration_ms in 1 2 3 4 5 6 7 8 9 10; do
      printf '0,42,1000000000,%s,\n' "$((1000000000 + duration_ms * 1000000))"
    done
    printf '%s\n' '---PROFILEDATA---'
  } | "$parser" -
)"
expected=$'frame_count=10\nframe_p90_ms=9.000\nframe_worst_ms=10.000'
if [[ "$actual" != "$expected" ]]; then
  echo "The gfx framestats parser produced unexpected percentile output."
  exit 1
fi

if {
  printf '%s\n' \
    '---PROFILEDATA---' \
    'Flags,FrameTimelineVsyncId,IntendedVsync,FrameCompleted,' \
    '1,42,1000000000,1010000000,' \
    '---PROFILEDATA---'
} | "$parser" - >/dev/null 2>&1; then
  echo "The gfx framestats parser must reject input without completed app frames."
  exit 1
fi

echo "Performance collector and framestats parser guard passed."
