#!/usr/bin/env bash
set -Eeuo pipefail

: "${ANDROID_SERIAL:?Select the authorized synthetic-data device with ANDROID_SERIAL}"
exec python3 "$(dirname "$0")/capture-store-previews.py" "$@"
