#!/usr/bin/env bash
set -euo pipefail

input="${1:--}"
durations_file="$(mktemp)"
trap 'rm -f "$durations_file"' EXIT

awk -F, '
  /^Flags,/ {
    flags_column = 1
    intended_vsync_column = 0
    frame_completed_column = 0
    for (column = 1; column <= NF; column++) {
      if ($column == "IntendedVsync") {
        intended_vsync_column = column
      } else if ($column == "FrameCompleted") {
        frame_completed_column = column
      }
    }
    reading_frames = intended_vsync_column > 0 && frame_completed_column > 0
    next
  }
  /^---PROFILEDATA---/ {
    reading_frames = 0
    next
  }
  reading_frames && $flags_column == "0" {
    intended_vsync = $intended_vsync_column + 0
    frame_completed = $frame_completed_column + 0
    duration_ms = (frame_completed - intended_vsync) / 1000000
    if (duration_ms > 0 && duration_ms < 10000) {
      printf "%.3f\n", duration_ms
    }
  }
' "$input" > "$durations_file"

frame_count="$(wc -l < "$durations_file" | tr -d ' ')"
if [[ "$frame_count" -eq 0 ]]; then
  echo "No completed application frames were found in the gfxinfo framestats input." >&2
  exit 1
fi

sorted_durations="$(sort -n "$durations_file")"
p90_line=$(( (9 * frame_count + 9) / 10 ))
frame_p90_ms="$(sed -n "${p90_line}p" <<< "$sorted_durations")"
frame_worst_ms="$(tail -1 <<< "$sorted_durations")"

echo "frame_count=$frame_count"
echo "frame_p90_ms=$frame_p90_ms"
echo "frame_worst_ms=$frame_worst_ms"
