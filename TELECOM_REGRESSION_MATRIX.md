# Default-dialer compatibility and telecom regression matrix

**Roadmap ID:** FP-101
**Last reviewed:** 2026-07-24
**Status:** FP-101 complete; this matrix remains a recurring release gate
**Supported app range:** Android 10+ (`minSdk 29`)

This is the release gate for call-critical changes. A build or unit test alone cannot prove Telecom,
carrier, lock-screen, Bluetooth, or OEM behavior, so automated checks protect deterministic app
contracts while the device matrix records real calls. Do not mark a device row passed from unrelated
app testing.

## Compatibility decision

- Fun Phone supports Android 10 and newer when the device exposes Android Telecom/default-dialer
  roles and telephony capability.
- The reference automated environment is the latest stable/target AOSP or Pixel emulator.
- Release candidates also require one current physical Pixel/AOSP-class device and one non-Pixel
  OEM. Samsung is the priority additional OEM because its phone/lock-screen behavior is distinct;
  Motorola is the currently available non-Pixel smoke device.
- Android 14+ exercises the `CallEndpoint` audio path. Android 10–13 exercises the legacy
  `CallAudioState` path. Both are release-critical.
- Dual-SIM, hold/swap, carrier features, Bluetooth, and full-screen incoming UI always require
  physical-device evidence. Emulator-only success is insufficient.
- If an OEM blocks a documented Android capability, record the exact device/build, fallback, and
  user recovery path instead of silently weakening the default-dialer contract.

## Automated coverage map

| ID | Contract | Automated evidence | Current status |
| --- | --- | --- | --- |
| TEL-A01 | Dial/CALL/tel intent filters, Telecom service binding, ringing/UI metadata, locked incoming activity, phone-call FGS declarations | `scripts/verify-telecom-contract.sh`; packaged assertions in `TelecomManifestContractTest` | `DONE` locally on Android 15/17; CI workflow execution pending |
| TEL-A02 | Default-dialer role is the first activation requirement and permissions recover one at a time | `PhoneStatusRequirementTest` | `DONE` |
| TEL-A03 | Call repository state reaches ringing/active/ended without losing mute, duration, or route state | `InCallViewModelTest`, `UiCallEqualityTest` | `DONE` |
| TEL-A04 | System mute-state callbacks do not fight the preferred route | `CallControllerMuteTest` | `DONE` |
| TEL-A05 | Missed-call notification is immediate, bounded, and caller-thread disk-safe | `MissedCallDisplayNameTest`, `RuntimeIoStrictModeTest` | `DONE` on Android 15/17 |
| TEL-A06 | Answer/reject/end/hold/unhold/DTMF/swap invoke the intended platform `Call` operations | `TelecomCallActionsTest` exercises the production seam, including legacy reject fallback and ordered DTMF | `DONE` |
| TEL-A07 | Android 14+ endpoint selection and legacy audio-route fallback preserve the selected route | `TelecomCallActionsTest` covers every legacy route and modern endpoint mapping; hardware behavior remains TEL-M07 | `DONE` |
| TEL-A08 | Incoming notification appears and Telecom cleans up a cancelled synthetic call | `scripts/smoke-emulator-incoming-call.sh` refuses physical devices, temporarily assigns the role, simulates GSM, asserts notification/cleanup, and restores the previous dialer | `DONE` on Android 17 emulator; full-screen policy/actions remain device-only TEL-M08/M09 |
| TEL-A09 | The app never forces a default route: system routing (car/Android Auto, Bluetooth, wired) wins, and an in-app pick is retried at most once until the system confirms it | `AudioRouteRequestPolicyTest` | `DONE` |

## Manual scenario matrix

Record `PASS`, `FAIL`, `BLOCKED`, or `N/A`, plus evidence in the device table. Every supported release
must cover TEL-M01–TEL-M10 on the primary physical device. TEL-M05, TEL-M06, and TEL-M07 cannot be
signed off without the matching hardware/carrier setup.

| ID | Scenario | Required observation |
| --- | --- | --- |
| TEL-M01 | Default-dialer role grant, revoke, and re-grant | Phone Status reflects each transition; outgoing and incoming handling recover after re-grant |
| TEL-M02 | Outgoing call and remote hang-up | Correct number/state, in-call UI, duration, recents entry, and cleanup |
| TEL-M03 | Incoming answer from unlocked and locked screen, including volume up when enabled | Ringing UI/notification appears once; answer reaches active audio call |
| TEL-M04 | Incoming reject, including volume down when enabled, and unanswered/missed call | Reject does not answer; volume keys only act while ringing and only when their setting is on; missed notification and recents are correct with no duplicate system alert |
| TEL-M05 | Hold/unhold and two-call swap | Active/held states and controls match the carrier; swap changes the audible call |
| TEL-M06 | DTMF against a real IVR | Digits `0-9`, `*`, and `#` are received once and in order |
| TEL-M07 | Earpiece, speaker, wired, and Bluetooth routing | Selected route becomes audible, survives mute changes, and falls back safely on disconnect; with a car/Android Auto connected the call stays on the car and a route picked on the car screen sticks |
| TEL-M08 | Full-screen incoming call while locked/backgrounded | UI appears under allowed system policy; otherwise notification fallback is actionable |
| TEL-M09 | Notification answer/reject/end actions | Each action affects only the intended call and clears stale notifications |
| TEL-M10 | Recents, contact name, missed-call fallback, and process return | Call log is correct; unavailable contact lookup falls back to number; reopening shows no ghost call |

## Required OS/OEM release rows

| Tier | Device / OS | Why | Required scenarios | Current evidence |
| --- | --- | --- | --- | --- |
| Legacy | AOSP emulator, Android 10/API 29 | Minimum SDK and legacy Telecom/audio path | TEL-M01–M04, M06, M08–M10 | `PLANNED` |
| Permission transition | AOSP emulator, Android 12 or 13 | Legacy audio plus modern role/notification restrictions | TEL-M01–M04, M06, M08–M10 | `PLANNED` |
| Modern reference | Pixel/AOSP, latest stable target | `CallEndpoint`, current full-screen/FGS policy | TEL-M01–M10 | `PLANNED` |
| Forward target | Pixel emulator, Android 17/API 37 | Target-SDK compatibility before release | TEL-M01–M04, M06, M08–M10 | TEL-A01 packaged contract `PASS`; manual calls `PLANNED` |
| Non-Pixel physical | Motorola Android 15 | Available OEM smoke and real carrier/audio hardware | TEL-M01–M10 | TEL-A01 packaged contract `PASS`; manual calls `PLANNED` |
| Priority OEM | Samsung current supported Android | OEM lock screen, notifications, Telecom, Bluetooth | TEL-M01–M10 | `BLOCKED` until device access |

## Evidence record

For each run, append one row. Never include real phone numbers, contact names, screenshots containing
PII, or carrier-account identifiers.

| Date | App version/commit | Device/build | SIM/audio setup | Scenarios | Result | Sanitized evidence / issue |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-07-24 | 3.16.5 / working tree | Pixel 9 Pro emulator, Android 17 | No SIM/audio | TEL-A01 | `PASS` | Two packaged manifest-contract assertions passed |
| 2026-07-24 | 3.16.5 / working tree | Moto g34 5G, Android 15 | No call placed | TEL-A01 | `PASS` | Two packaged manifest-contract assertions passed |
| 2026-07-24 | 3.16.5 / working tree | Pixel 9 Pro emulator, Android 17 | Synthetic GSM `5550101` | TEL-A08 | `PASS` | Incoming notification observed, call cancelled, Telecom cleaned up, prior dialer restored |
| 2026-11-05 | 3.20.1 / working tree | Test Automation emulator, Android 15, Play debug | Synthetic GSM | Boot recovery, TEL-A01, TEL-A08 | `PASS` | Reminder recovery completed after reboot. An incoming notification appeared, the service stopped on cancellation, and the prior dialer was restored. |
| 2026-11-08 | 3.20.2 / local candidate | Moto g34 5G and isolated emulator, Android 15 | Synthetic telephone links; no call placed | TEL-A01; external-link prefill and editing | `PASS` | Packaged Telecom assertions pass. Direct ADB verifies cold/warm DIAL/VIEW/CALL, browser manifest resolution, repeated and empty links, international/service numbers, keypad editing and FAB return; emulator also preserves edits through rotation. Evidence: `app/build/reports/release-3.20.2/`. Carrier scenarios TEL-M01–M10 remain pending. |

## Release rule

FP-101 is complete because TEL-A01–TEL-A08 are automated and carrier/hardware/policy behavior is
explicitly assigned to stable manual scenarios across the supported OS/OEM tiers. The manual rows
are recurring release evidence, not claims already passed. Any change to `CallController`,
`FunPhoneInCallService`, incoming/ongoing call services, notifications, role handling, or call UI
must run the automated suite and the affected manual rows; a release cannot ship with a required
row missing or failed without a documented disposition.
