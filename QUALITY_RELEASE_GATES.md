# Quality release gates

**Roadmap IDs:** FP-106, FP-201–FP-206
**Applies to:** Community and Play distributions

This file is the stable, evidence-based release checklist for UI architecture, critical flows,
accessibility, performance, and adaptive layouts. Automated checks may prove deterministic
contracts; they do not replace the explicitly manual device and assistive-technology rows.

## Localization quality gate (LOC-01)

Each module with strings (`core/ui` and every `feature/*`) has its own canonical English baseline in
`src/main/res/values/strings.xml`, and its supported translations are `values-es`, `values-de`,
`values-fr`, and `values-pt` in the same module. The gate checks every module against its own
baseline and fails when a string name is declared in more than one module.

Run the reproducible gate from the repository root:

```bash
./scripts/verify-localization.sh
```

The gate parses each XML file with the Python standard library and fails with a file, key, and line
reference when it finds any of the following:

- missing or extra resource names, duplicate names, or resource type mismatches;
- missing or extra plural quantities;
- changed string-array/integer-array lengths;
- changed format placeholder positions or conversion types;
- U+FFFD replacement characters; or
- an identical English user-facing value in a translation.

Identical values are permitted only for `translatable="false"` resources or the explicit
`EXACT_VALUE_ALLOWLIST` in `scripts/verify-localization.py`. Each allowlist entry binds an expected
value to specific locales and states its brand, acronym, or technical-value reason. Do not add a
whole locale or prefix to the allowlist to hide untranslated copy. LOC-01 is part of
`scripts/verify-quality-contract.sh` and is a release-blocking automated check.

## UI state and effects contract (FP-106)

Every route with asynchronous or durable business data must:

1. expose one immutable `*UiState` from their controller or ViewModel;
2. represent loading, empty, content, and recoverable error states explicitly when the screen reads
   data;
3. expose transient navigation, permission, and message actions as a one-shot `SharedFlow`/channel,
   not as a Boolean stored in UI state;
4. keep Android/provider adapters outside the state holder;
5. avoid owning the same durable value in both `remember` and a ViewModel.

Pure presentation state—expanded menus, dialog visibility, animation progress, text-field cursor
position, and in-memory game boards—may remain local when it has no second durable owner and cannot
load or fail. In that case immutable parameters plus explicit callbacks are the route state/effect
boundary; inventing a ViewModel or fake loading/error states would add ownership rather than remove
it. A route is not compliant merely because it uses `StateFlow`.

The guarded examples cover calls, QR history, tags, expenses, vault, duplicate contacts,
onboarding, Phone Status, navigation, dialer editing/actions, note-folder filtering, history
projection, blocked-number settings, contact-detail identity, and holiday formatting. The release
contract is complete; future changed screens must retain it.

## High-risk file ratchet (FP-201)

The seven original risk files may only shrink. `scripts/verify-quality-contract.sh` fails if any
exceeds its recorded ceiling. Each file now has a behavior-preserving controller, model, policy,
dialog, or component extraction with focused tests. Continued decomposition is normal maintenance,
not an open FP-201 acceptance criterion.

| File | Ceiling | First extraction target |
| --- | ---: | --- |
| SettingsScreen | 2,080 | remaining settings sections |
| ContactsViewModel | 1,714 | import/export, block policy, QR/share operations |
| ContactDetailsScreen | 1,604 | edit fields and action dialogs |
| NoteListScreen | 1,372 | selection toolbar |
| DialerScreen | 1,127 | input/editor and action dispatch |
| HistoryScreen | 1,109 | selection controller and provider actions |
| HolidaysScreen | 1,352 | country selector and holiday list |

## Critical domain suite (FP-202)

| ID | Contract | Automated evidence |
| --- | --- | --- |
| DOM-01 | ordered permission/limited-mode policy | `PhoneStatusRequirementTest`, `ReminderPermissionActionTest` |
| DOM-02 | dialer role and packaged Telecom contract | `TelecomManifestContractTest`, telecom source guard |
| DOM-03 | call actions and state/route mapping | `TelecomCallActionsTest`, `InCallViewModelTest`, `UiCallEqualityTest` |
| DOM-04 | contact search ranking and tag filtering | `ContactSearchPolicyTest` |
| DOM-05 | contact import validation and normalization | `ContactImportPolicyTest` |
| DOM-06 | blocked-number identity across formatted/country variants | `BlockedNumberMatchPolicyTest`; provider mutation remains device-covered |
| DOM-07 | Room migrations | exported-schema 2→29 through 7→29 and representative 28→29 preservation in `AppDatabaseMigrationTest` |
| DOM-08 | durable reminder decisions and receiver enqueue | reminder JVM and instrumentation suites |
| DOM-09 | vault state and authenticated-encryption boundary | `PasswordsViewModelTest`, device `CryptoManagerTest` |

## Core UI flow matrix (FP-203)

| ID | Flow | Deterministic check | Device evidence required |
| --- | --- | --- | --- |
| UI-01 | onboarding and limited mode | two-step and setup-controller tests; skip, denial, exact-alarm recovery, saved-state, contrast and large-text Compose tests | first-install system permission/role/exact-alarm deny and retry |
| UI-02 | main tabs and tools visibility | single-state/saved-state tests; selected-tab/click/hidden-tools Compose semantics | physical rotation and process-death |
| UI-03 | dialer search and input | search/index/limit plus edit/action policy tests; native paste/selection replacement; ContactsProvider-backed result UI dispatches the exact number through an explicit call seam | physical system call-role handoff |
| UI-04 | contact create/edit/delete | repository/ViewModel tests; emulator-only Compose + real ContactsProvider create/edit/delete with scoped cleanup | physical permission and UI regression with owner-managed data |
| UI-05 | call history search/delete/block | history projection/search tests; emulator-only local Room search/selection/delete with scoped cleanup; blocked-list empty/confirm UI tests | real CallLog provider rows and empty state |
| UI-06 | tools presets | `ToolsOrderConfigTest`, `ToolsPresetSelectorTest` at compact width/200% font | Settings and limited-mode tool-customization entry points |
| UI-07 | protected-section unlock | vault ViewModel tests; PIN success/invalid/cancel and biometric state success/cancel/lockout UI tests; biometric terminal-error policy JVM tests | real system biometric prompt success/cancel and lockout |
| UI-08 | neutral-first accent hierarchy | palette contrast tests across every configured seed and four surface schemes; source guard prevents raw seed collection and prop drilling through shared UI | representative light/AMOLED/dark-gray/dim screenshots with light, yellow, orange, dark-purple and blue-gray accents |

Run `connectedCommunityDebugAndroidTest` on the reference Android 15 device and Android 17 emulator
before release. Record device model, build fingerprint, test report, and sanitized screenshots.

## Accessibility matrix (FP-204)

For each main route—Favorites, Dialer, Contacts, History, Phone Status, Tools, and Settings—verify:

- every actionable icon has a localized label and stateful controls announce role/state;
- traversal order follows the visual reading order;
- touch targets are at least 48 dp;
- content is usable at 200% font scale without clipped actions;
- light/dark contrast passes automated inspection and status is never color-only;
- TalkBack can complete the route's primary action without an external keyboard.

Release evidence must include one TalkBack pass by a screen-reader user or accessibility tester.
Lint/static checks are supporting evidence only.

## Performance budgets (FP-205)

Measure a release-like Community build on a mid-tier physical device after a reboot and thermal
stabilization. Capture the median of five runs and the worst observed run.

| ID | Scenario | Initial budget | Current evidence |
| --- | --- | ---: | --- |
| PERF-01 | cold start to first interactive core route | median ≤ 1,500 ms; worst ≤ 2,500 ms | Benchmark 359 ms median/388 ms worst; stabilization pending |
| PERF-02 | contact-list 90th percentile frame time | ≤ 24 ms | provisional Android 15 release-like run: 6.359 ms p90/14.348 ms worst across 118 frames with 2,000 isolated synthetic contacts; stabilization pending |
| PERF-03 | dialer query update with 2,000 synthetic contacts | ≤ 100 ms | JVM p90 0.129 ms; physical UI trace pending |
| PERF-04 | favorites widget refresh | ≤ 500 ms | Glance migration: `FavoritesWidgetRenderingPerformanceTest` measures avatar preparation for 12 synthetic favorites; launcher/provider trace pending |
| PERF-05 | reminder scheduling call | ≤ 100 ms caller time | Android 15 p90 11.562 ms over 20 cleaned samples |
| PERF-06 | idle app private dirty memory after core navigation | ≤ 180 MB | Benchmark 27,564 KB; stabilization pending |

The budgets are release gates after the first approved physical-device baseline. Do not relax a
budget to hide a regression; document the cause and owner decision. Run
`scripts/measure-core-performance.sh` against the installed Community debug package for the
repeatable provisional cold-start/memory evidence. For comparable evidence, install the minified,
non-debuggable, parallel-installable variant with `./gradlew installCommunityBenchmark`, complete
onboarding using synthetic/empty data, then run the collector against
`com.bookverse.contacts.community.benchmark`. The report separates total PSS from private dirty,
calculates median/worst startup, evaluates the numerical budgets, rejects debuggable packages when
`RELEASE_LIKE=true`, and is release-gate eligible only when both `RELEASE_LIKE=true` and
`DEVICE_STABILIZED=true` are explicitly supplied. Set `ANDROID_SERIAL` when multiple devices are
connected. For a user-prepared Contacts route, set
`MEASURE_CONTACT_SCROLL=true CONTACT_SCROLL_READY=true`. The preferred reproducible path is
`PREPARE_SYNTHETIC_CONTACTS=true MEASURE_CONTACT_SCROLL=true`: a benchmark-only Activity protected
by the platform `DUMP` permission writes deterministic contacts solely to that parallel package's
Room cache, completes onboarding in limited mode, and never requests or reads Contacts Provider
access. The collector performs alternating swipes, parses completed application frames by header
name, reports p90/worst duration, discards the raw dump, and marks PERF-02 evidence eligible only
for a stabilized release-like run within the 24 ms budget. Dialer and full widget traces still
require their scenario-specific profiler evidence. Never publish the benchmark APK.

### Baseline Profile

`app` ships `androidx.profileinstaller` and a generated Baseline Profile plus startup profile in
`app/src/main/generated/baselineProfiles/`, so release installs AOT-compile cold start, the
Contacts list scroll, Favorites, History and the dialer instead of waiting for JIT. The
`:baselineprofile` module (`androidx.baselineprofile` + Macrobenchmark) regenerates them. It clears
the target Community package's data, sets its app language to `en-US` so the journeys find their
labels on any device language, and seeds 2,000 synthetic contacts through the benchmark setup
Activity, so it refuses physical devices unless
`-Pandroid.testInstrumentationRunnerArguments.allowPhysicalDevice=true` is passed for a dedicated
test phone. Regenerate after significant UI changes and commit the updated files:

```bash
ANDROID_SERIAL=emulator-5554 ./gradlew :app:generateBaselineProfile
```

`CoreBenchmarks` compares cold start and Contacts frame timing with and without the profile:

```bash
ANDROID_SERIAL=<test-device> ./gradlew :baselineprofile:connectedCommunityBenchmarkReleaseAndroidTest
```

Emulator results only check that the journeys run; comparable numbers need a stabilized physical
test device.

## Adaptive layout matrix (FP-206)

Test every core route in:

- compact portrait and landscape;
- split screen at the platform minimum width;
- medium width (foldable/tablet, 600–839 dp);
- expanded width (840 dp or wider);
- font scales 1.0, 1.3, and 2.0;
- light/dark mode and English/Spanish, plus one long-string locale.

No route may lose its primary action, overlap system bars, trap focus off-screen, or require a
fixed portrait orientation. Screenshots must use synthetic contacts/call data.

Run `ANDROID_SERIAL=<emulator> ./scripts/run-adaptive-ui-matrix.sh` for the deterministic proxy
matrix. It installs the Community debug/test APKs directly on the selected emulator and executes
17 focused accessibility, onboarding, navigation, dialog, tools, protected-section, and dialer-editor cases at
360×720 dp/Spanish, 640×360 dp/English, 800×1067 dp/German, and 1000×1280 dp/200% font. The runner
refuses physical devices and restores display, density, font, theme, and app locale on exit. The
360 dp case is an automated width proxy only; it does not replace platform split-window,
hinge/fold posture, route screenshot, or physical tablet evidence.

## Evidence record

| Release | Automated | Android 15 physical | Android 17 emulator | TalkBack | Performance | Large screen |
| --- | --- | --- | --- | --- | --- | --- |
| 3.17.0 candidate | Community build/unit/guards/lint pass | Prior migration suite plus schema 30 password-folder preservation; exported schemas 2–7 and representative 28/29 data reach the current schema without destructive fallback; full UI matrix pending | Deterministic tests include legacy schema preservation, password folder/recycle-bin state, emulator-only synthetic local-contact CRUD, scoped local-history search/delete, and biometric state success/cancel/lockout; the repeatable adaptive proxy covers compact/landscape/medium/expanded, 130–200% font, light/dark, ES/EN/DE scenarios; formal route screenshots and real split/fold posture remain pending | pending | release-like benchmark: prior startup 359 ms median/388 ms worst/private dirty 27,564 KB; new 2,000-contact provisional run 439/461 ms startup, 33,092 KB private dirty and PERF-02 6.359 ms p90/14.348 ms worst; PERF-03/04/05 automated; stabilization/full UI traces pending | partial; deterministic width proxy passes, real split/fold/tablet screenshots pending |

## Upgrade work package — 2026-11-03

The working metadata remains 3.19.6/259; this is not Play-delivery evidence. The upgrade adds JVM
contracts for social URL validation/provider identity, independently verified PBKDF2 output, legacy
PIN migration, durable attempt limiting across authenticator recreation/reboot, locked-vault access,
corrupt-row recovery, neutral surface readability and encrypted draft restoration after re-auth.
The suite now contains 318 passing JVM cases (79 suites), including concurrent attempts through separate
PIN-authenticator instances sharing one process-wide mutex. The encrypted-draft contract covers
latest edits, recreation, corrupt ciphertext and final recorder metadata without revealing locked
fields or reviving discarded drafts. Protected checklist/reminder/recording and expense editors use
that boundary. Editing a protected reminder retains its lock flag.
MainScreen is composed only after settings preload, so a startup shortcut cannot interpret the
initial empty auth state as a request to configure a new PIN. PIN dialogs enforce secure windows
even when opened from otherwise unprotected Settings.
Community Debug assembly, ktlint, Community Debug/Play Release lint and existing Android-test Kotlin
compilation pass. Lint has zero errors and 17/18 warnings respectively, within the unchanged limit
of 20; report timestamps follow the final Kotlin edits. Localization, quality, accent, Telecom,
feature-boundary, source-format, performance-tooling and zero-collection guards pass.

Direct ADB exploration on the isolated benchmark package confirmed that Facebook, Instagram and
LinkedIn home pages render without a username; the modal was closed and the synthetic-contact
editor cancelled without saving. Light and AMOLED Settings captures were refreshed for the web.
This is manual exploration, not an instrumented regression pass. Document-picker
reauthentication, all protected-editor UI transitions, biometric cancellation, four-theme/Retro
coverage and 200% text still need the device UI matrix.

Physical provisional collection on moto g34 5G / API 35, non-debuggable Community benchmark,
2,000 isolated synthetic contacts, five cold starts: median **458 ms**, worst **476 ms**;
private dirty **32,752 KB**, total PSS **53,158 KB**; contact-list frame p90 **12.330 ms**,
worst **17.579 ms**, 118 frames. The setup now explicitly selects Contacts, so the collector cannot
silently measure the small Favorites grid. All numeric budgets measured here pass.
`DEVICE_STABILIZED=false`: no reboot/thermal stabilization was claimed, and the report correctly
keeps release-gate eligibility false. Sanitized collector output is under
`app/build/reports/performance/upgrade-2026-11-03-contacts/core-startup.txt`.

A later benchmark rebuild/install succeeded, but its collection was interrupted by Android's input
service (`Broken pipe (32)`) and the benchmark no longer held the foreground. ADB remained
authorized and the device was awake; a controlled launch still did not retain foreground. The
partial `upgrade-2026-11-03-final` report is discarded and is not a replacement baseline. No device
restart, global input-service change, real-contact access or interaction with another app followed.
The collector now checks foreground ownership before every swipe and before accepting frames;
synthetic shell-policy checks accept its own Activity and reject other/missing Activities. Bash
syntax, the framestats parser and performance-tooling guards pass.

No new device-interaction tests were authored in that work package. JVM tests, static guards and
direct ADB benchmark collection are separate evidence.
Real biometric/lock-screen behavior, carrier/OEM call continuity, TalkBack by an accessibility tester,
API 28/37 emulator coverage, update-with-data preservation, stabilized profiling and the outstanding
Play-delivered validation remain required before publication.

## Modern redesign work package — 2026-11-08

This is implementation evidence, not a Play release approval. Exploration used Appium MCP and
direct ADB on the only visible target: `emulator-5554`, Android 15 / API 35, 1280×2856, density 480,
60 Hz. All captured content comes from the parallel benchmark package's synthetic Room cache;
2,000 contacts were seeded without writing to Contacts Provider or placing calls.

The JVM suite passes 329 cases, including one-time appearance migration, subsequent style choices,
backup round trips, the six accent seeds, secondary back-stack restoration and Telecom return policy.
Community Debug and benchmark builds, ktlint, Community Debug/Play Release lint and PR workflow
guards pass. Lint reports zero errors/fatal issues and 18/19 warnings, within the unchanged limit
of 20. Localization, accent, quality, feature boundaries, source format, performance tooling, Play
listing and privacy guards pass. No Room schema, release metadata or publishing gate was changed.

The focused final device suite passes 34 cases: shared headers, rapid scrolling, compact forms,
search, saved header state, pager tap/swipe and interrupted taps, accessible dock targets, all three
styles/four themes, appearance Apply/Cancel, labelled creation actions with motion disabled,
onboarding restoration/limited mode and dialer layout/editor contracts. An additional shared-chrome
adaptive matrix passes 70 executions (14 cases in five scenarios): 400×800 dp/EN/100%,
360×720 dp/ES/200%, 640×360 dp/DE/130%, 800×1067 dp/FR/dark/130%, and
1000×1280 dp/PT/dark/200%. These are focused component/route tests, not exhaustive screenshots of
every tool at every setting. Display, font, locale, power-save and accessibility settings were restored.

Appium exploration confirmed contact search, selection, detail/edit cancellation, keyboard-first
Back using Gboard, Settings draft preview/cancel, Notes creation/cancel, Pomodoro, Snake's setup
dialog, Paint, expenses, dialer, voice tools and QR generation. No real call, camera capture, voice
recording or sensitive credential was created. A TalkBack smoke check confirmed the bound service,
touch exploration and visible focus in the app; comprehensive spoken-feedback review remains pending.

Performance diagnostics used eight alternating 400 ms swipes within the contact-list bounds with
compilation idle. Haze uses automatic input downsampling; header and dock share one content source.
The fallback was exercised through Android battery saver, which also changes system power policy.

| Final emulator diagnostic | Parsed frames | Frame p90 | Worst | Janky / rendered | Missed vsync |
| --- | --- | --- | --- | --- | --- |
| Modern blur | 119 | 64.191 ms | 73.290 ms | 24 / 213 (11.27%) | 3 |
| Solid fallback | 120 | 33.116 ms | 38.912 ms | 10 / 162 (6.17%) | 0 |

The 24 ms budget is **not met by these emulator measurements** and the physical performance gate
remains open. The initial pre-update diagnostic (75.075 ms p90) used an older installed package,
concurrent compilation and a different swipe path; it is not a controlled before/after baseline.
The production collector's physical-device/stabilization requirements were not bypassed or weakened.
Physical 60/120 Hz profiling, Android 10–11 fallback, OEM/carrier calls, lock-screen/biometric behavior,
real split/fold/tablet layouts, comprehensive TalkBack and outstanding Play validation remain required.

Local evidence is under `app/build/reports/modern-redesign/` (ignored build output):
`modern-walkthrough.mp4`, expanded/compact header and appearance screenshots, `talkback-focus.png`,
`final-instrumented.txt`, `matrix-*.txt`, Gradle logs, and `final-shared-*-framestats.txt` with parsed
metrics. The walkthrough records the final benchmark candidate using synthetic data only.

## Compact headers and glass dock iteration — 2026-11-08

All exploration and device validation in this iteration used direct ADB. The selected physical
target is moto g34 5G, Android 15 / API 35, 720×1600 at density 280. The parallel Community
benchmark package contains 2,000 synthetic Room contacts; Contacts Provider and real calls were
not used. The existing debug installation was updated without clearing its data.

Modern headers now open compact, expand only when pulling down at the start of the content, and
collapse when scrolling forward. Dragging the toolbar itself does not expand it. Saved offsets
remain independent per destination and style. Physical exploration verified compact launch,
expansion at the start, no expansion mid-list, tab taps/swipes, and Light/AMOLED appearance.
The dock uses neutral selection, backdrop blur, a lit rim and API 33+ edge refraction; its labels
and icons remain outside the optical layer. Battery saver switches it to a solid surface.

The final focused physical suite passes **15 tests**. The same suite passes **75 executions** on
the isolated emulator across the five size/language/font scenarios documented above, including
200% text. Coverage adds compact initial state, pull-at-start expansion, mid-list scroll and
pixel comparison proving dock selection is independent of the accent seed. The pull test begins
inside the list, below the overlaid header. JVM tests pass **329 cases**. Community Debug and
benchmark assembly, ktlint, Community Debug/Play Release lint and the executable PR workflow
guards pass; lint remains at zero errors/fatal issues and 18/19 warnings. Localization, accent,
privacy and Telecom guards pass. No gates or thresholds were relaxed.

The physical production collector used five cold starts and the same 2,000-contact setup before
and after the iteration. Its frame p90 changed from **9.132 ms** to **13.828 ms** (119 parsed frames
each); final worst frame was **21.452 ms**. Final cold-start median/worst are **477/544 ms**, total
PSS **64,593 KB**, private dirty **47,116 KB**. These measured numeric budgets pass. This is a
small provisional comparison, not evidence that the added glass effect improves rendering cost.

Additional physical diagnostics used twelve alternating 400 ms swipes wholly inside the list.
Android reported the requested 60/120 Hz render rates. The parser samples the available frame
history; jank/deadline counters cover the full collection and therefore have different totals.

| Physical diagnostic | Parsed frames | Frame p90 | Worst | Janky / rendered | Missed vsync | Missed deadlines |
| --- | --- | --- | --- | --- | --- | --- |
| Glass, 60 Hz | 119 | 12.632 ms | 18.175 ms | 1 / 372 (0.27%) | 0 | 1 |
| Glass, 120 Hz | 119 | 9.344 ms | 10.934 ms | 4 / 713 (0.56%) | 0 | 4 |
| Solid fallback, 60 Hz | 119 | 8.115 ms | 10.484 ms | 1 / 365 (0.27%) | 0 | 1 |

The fallback was exercised using battery saver, which also affects system power policy. Refresh
limits, battery simulation, battery saver, benchmark theme and emulator display/font/locale were
restored. `DEVICE_STABILIZED=false` remains explicit: release-gate and PERF-02 eligibility are
false. Stabilized profiling, Android 10–12 device coverage, OEM/carrier calls, lock-screen/biometric
behavior, comprehensive TalkBack and outstanding Play validation remain open.

Local evidence is under `app/build/reports/glass-iteration/`: `glass-walkthrough.mp4`, compact,
expanded, mid-scroll and Light/AMOLED captures, `physical-compose.txt`, `matrix-*.txt`, before/after
`core-startup.txt` reports and `glass-60hz`, `glass-120hz`, `solid-60hz` framestats/metrics. These
ignored artifacts contain only synthetic application data.

## Reachable actions and persistent search — 2026-11-08

This follow-up replaces Modern's two-row Material header with one coordinated title/action row.
Title, count, Back and other actions descend together on pull-at-start expansion. Searchable
destinations use a full-width search affordance below that row instead of an isolated magnifier.
The active search compacts/pins the header and restores its previous expansion when closed.
Contacts/History temporarily omit the limited-mode setup banner during Modern search, preserving
space for results and restoring the banner afterwards. Minimal and Retro keep their icon entry.

Direct ADB exploration on the same physical Motorola preceded the new interaction tests. It
confirmed title/action centers moving together by 154 px at density 280, the persistent search
bar, query entry, and restoration after close. Gboard was temporarily selected to verify real IME
behavior: first Back hides the keyboard while retaining the query, second Back closes search.
The original input method was restored. No user contacts or credentials were used.

The final physical component suite passes **17 cases**; its adaptive emulator matrix passes
**85 executions** across the same five scenarios, including 200% text. The added cases check
coordinated title/actions/search movement, equal search-field height before/after activation,
focus, pinning, clear/close behavior and restored expansion. The isolated emulator's existing
History search/selection/deletion regression also passes and cleans up its synthetic rows.
JVM tests pass **329 cases**. Debug/benchmark assembly, ktlint, Community Debug/Play Release lint,
localization, quality, privacy, Telecom and executable PR guards pass. Lint remains at 18/19
warnings with zero errors/fatal issues. The accent contract now checks Modern's semantic search
surface/content and adaptive 48 dp minimum in addition to the retained legacy translucent style.

The final production collector reports median/worst startup **476/494 ms**, private dirty
**48,912 KB**, total PSS **64,956 KB**, contact-scroll p90 **14.645 ms**, worst **19.889 ms** over
118 parsed frames. Additional physical diagnostics use twelve 400 ms swipes from y=1100 to 1260
and back, inside the list even with the taller search header. This narrower path differs from the
preceding iteration, so these supplemental numbers are not a controlled before/after comparison.

| Final physical diagnostic | Parsed frames | Frame p90 | Worst | Janky / rendered | Missed vsync | Missed deadlines |
| --- | --- | --- | --- | --- | --- | --- |
| Glass, 60 Hz | 119 | 13.315 ms | 19.391 ms | 16 / 366 (4.37%) | 0 | 16 |
| Glass, 120 Hz | 119 | 11.335 ms | 14.621 ms | 13 / 734 (1.77%) | 0 | 13 |
| Solid fallback, 60 Hz | 118 | 9.894 ms | 11.811 ms | 4 / 363 (1.10%) | 0 | 4 |

The measured p90 budget passes, but the full-collection counters still include missed deadlines.
No zero-jank or stabilized release claim is made. `DEVICE_STABILIZED=false` and release/PERF-02
eligibility remain false. Refresh, battery saver/simulation, IME and emulator display/font/locale
settings were restored. The outstanding OEM/carrier, older Android, comprehensive TalkBack and
Play gates listed above remain open. Evidence is in `app/build/reports/reachable-chrome/`, including
`reachable-chrome.mp4`, screenshots, keyboard/Back XML snapshots, `physical-compose.txt`,
`matrix-*.txt`, `history-flow.txt`, collector output and raw/parsed 60/120 Hz framestats.

## Local 3.20.2 / 262 candidate — 2026-11-08

The final candidate adds a 64 dp circular glass FAB exclusive to Modern. Selected dock icons and
labels now use the theme-derived accent, interpolate with pager movement and retain a neutral
glass capsule. Each tab clips its press/focus feedback to its rounded shape. This supersedes the
earlier iteration's accent-independent foreground contract; Minimal and Retro keep their designs.

External DIAL/VIEW/CALL phone links now update the route and input atomically, including before
settings finish loading. Configuration changes preserve edits, subsequent links replace the
input, and an empty DIAL intent or the FAB opens an empty editor. Programmatic text replacement
keeps the caret at the end without emitting a spurious selection update. International `+`
prefixes survive editing and bypass an already-selected country prefix. No link places a call.

Direct ADB exploration preceded the interaction regressions. The Motorola g34 5G (Android 15)
passes **31 focused instrumented cases** and **10 external-link scenarios** using synthetic data.
The isolated Android 15 emulator passes **19 focused cases**, **12 link scenarios** (including
rotation), and **95 adaptive executions** across compact English, Spanish at 200%, German
landscape, medium French dark mode and expanded Portuguese dark mode at 200%. Pixel assertions
verify that the accent changes only selected dock content and press feedback leaves rounded
corners untouched while remaining visible. No carrier call or Contacts Provider write occurred.

**341 JVM tests pass.** Community Debug/benchmark builds, focused test APK assembly, ktlint,
Community Debug/Play Release lint, release metadata, listing/localization, accent, privacy,
Telecom and executable PR guards pass. Lint reports zero errors/fatal issues and 18/19 warnings.
The signed Play AAB, mapping and native symbols are prepared locally for 3.20.2/262; no publication,
push, tag or GitHub release is implied. Play notes are synchronized in five languages.

The physical production collector with 2,000 synthetic contacts reports startup median/worst
**446/468 ms**, total PSS **69,481 KB**, private dirty **46,904 KB**, frame p90 **14.784 ms** and
worst **19.923 ms** over 118 parsed frames. Its full collection reports 23 janky frames out of 706,
five missed vsyncs and 23 missed deadlines. Supplemental twelve-swipe diagnostics report:

| Final 3.20.2 diagnostic | Parsed frames | Frame p90 | Worst | Janky / rendered | Missed vsync | Missed deadlines |
| --- | --- | --- | --- | --- | --- | --- |
| Glass, 60 Hz | 118 | 13.163 ms | 16.023 ms | 10 / 365 (2.74%) | 0 | 10 |
| Glass, 120 Hz | 118 | 12.806 ms | 16.716 ms | 14 / 725 (1.93%) | 0 | 14 |
| Solid fallback, 60 Hz | 119 | 10.080 ms | 13.171 ms | 2 / 362 (0.55%) | 0 | 2 |

Refresh limits, live battery state, power saver and emulator display/font/locale settings were
restored. The measured budgets pass, but `DEVICE_STABILIZED=false`; PERF-02/release eligibility
remains false. Older-Android coverage, comprehensive TalkBack, OEM/carrier calls and outstanding
Play-delivery validation remain open. Evidence is under `app/build/reports/release-3.20.2/`,
including `physical-compose.txt`, `emulator-focused.txt`, `matrix-*.txt`, link assertions,
screenshots, `modern-and-phone-links.mp4`, production collector output and refresh-rate diagnostics.
