#!/usr/bin/env bash

set -euo pipefail

serial=${1:-}
app_id=${2:-com.bookverse.contacts.community.debug}
call_number=5550101

if [ -z "$serial" ]; then
    printf 'Usage: %s <emulator-serial> [application-id]\n' "$0" >&2
    exit 2
fi

if [ "$(adb -s "$serial" shell getprop ro.kernel.qemu | tr -d '\r')" != "1" ]; then
    printf 'Refusing to simulate a call on non-emulator device %s.\n' "$serial" >&2
    exit 1
fi

if ! adb -s "$serial" shell pm path "$app_id" >/dev/null 2>&1; then
    printf '%s is not installed on %s.\n' "$app_id" "$serial" >&2
    exit 1
fi

previous_dialer=$(adb -s "$serial" shell cmd role get-role-holders \
    --user 0 android.app.role.DIALER | tr -d '\r' | head -1)

restore_state() {
    adb -s "$serial" emu gsm cancel "$call_number" >/dev/null 2>&1 || true
    adb -s "$serial" shell cmd role remove-role-holder \
        --user 0 android.app.role.DIALER "$app_id" >/dev/null 2>&1 || true
    if [ -n "$previous_dialer" ]; then
        adb -s "$serial" shell cmd role add-role-holder \
            --user 0 android.app.role.DIALER "$previous_dialer" >/dev/null 2>&1 || true
    fi
}
trap restore_state EXIT

adb -s "$serial" shell cmd role add-role-holder \
    --user 0 android.app.role.DIALER "$app_id" >/dev/null
adb -s "$serial" emu gsm call "$call_number" >/dev/null
sleep 2

if ! adb -s "$serial" shell dumpsys notification --noredact |
    grep -q "pkg=$app_id"; then
    printf 'Incoming-call notification was not observed for %s.\n' "$app_id" >&2
    exit 1
fi

adb -s "$serial" emu gsm cancel "$call_number" >/dev/null
sleep 1

if adb -s "$serial" shell dumpsys telecom | grep -q "$call_number"; then
    printf 'Synthetic call remained in Telecom after cancellation.\n' >&2
    exit 1
fi

printf 'Synthetic incoming-call smoke passed on %s; previous dialer restored.\n' "$serial"
