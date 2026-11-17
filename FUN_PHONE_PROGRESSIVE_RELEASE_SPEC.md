# Fun Phone — Progressive release specification

**Status:** Active source of truth

**Current source metadata:** `3.20.2` / code `262` (`release.properties`), reviewed 2026-11-08.
**Publication evidence:** the historical completed Play baseline below remains `3.17.8` / `238`.
Later source versions do not prove a Play-delivered install or completed smoke test.

**Last updated:** 2026-11-08

## Local candidate 3.20.2 / 262

The owner explicitly requested a patch bump from 3.20.1/261, local release artifacts, release notes
and one local commit without push. This candidate groups the Modern redesign and its navigation,
search, typography, palette and glass controls with the external telephone-link prefill correction.
Preparation does not authorize publication, a tag, a PR or a GitHub Release. Historical Play-delivery
checkboxes below remain unchanged; local builds do not close those gates. Validation evidence and
remaining physical/OEM requirements are recorded in `QUALITY_RELEASE_GATES.md`.

## Unversioned upgrade work package

User-approved app/web work covers social previews, transparent scroll-under status bars, neutral
surface contrast, immediate protected-session locking, compatible PIN verifier migration, encrypted
editor drafts, and the companion static site's privacy/accessibility/performance improvements.
Room remains at **30**, matching the executable database and quality guard. This package does not
increment release metadata, change ciphertext/backup formats, introduce cloud/telemetry, or authorize
publication. Historical Play validation checkboxes remain pending until owner evidence is supplied.
Group the validated work only after reconciling that evidence; do not mark a candidate DONE from a
successful local build or benchmark.

This specification replaces the previous roadmap as the operational plan for Fun Phone work packages
and grouped Play releases. A work package is delivered through a scoped PR; several merged work
packages may be grouped into one Play release. `FUN_PHONE_EVOLUTION_SPEC.md` remains historical
context and must not be used to expand a work package beyond the scope recorded here.

## Status legend

- `DONE`: published and validated from Play.
- `MERGED`: implemented and merged into `main`; intentionally not published alone.
- `IN_REVIEW`: submitted to Play and awaiting approval.
- `NEXT`: the next authorized work package.
- `IN_PROGRESS`: implementation and automated validation are underway.
- `PLANNED`: not started.
- `DEFERRED`: explicitly frozen.
- `CANCELLED`: removed from the authorized roadmap by product or scope decision.

## Current state

- [x] Rollback completed.
- [x] Pull request #61 merged into `main`.
- [x] Version `3.17.8` generated.
- [x] Release submitted for Play review.
- [x] Release approved and published.
- [x] Update installed directly from Play.
- [x] Post-publication smoke test passed.
- [ ] Tag and GitHub Release for `3.17.8` created.
- [x] `CHANGELOG.md` updated with `3.17.8`.
- [x] PR #62 merged into `main` with the Android Lint closure work package.
- [x] Candidate metadata `3.17.9` / code `239` retained for the grouped Play release.
- [x] Grouped scope selected: PR #62, incremental architecture, and call continuity recovery.
- [x] Pull request #63 merged the grouped `3.17.9` candidate into `main`.
- [x] Automated validation completed for the grouped candidate.
- [x] Automated validation repeated after the lock-screen answer hotfix.
- [x] Development-build device testing completed per owner confirmation.
- [ ] Update installed and validated from the Play-delivered `3.17.9` build.
- [x] PR #66 merged into `main` without Unified Search, Post-call Actions, or Call Screening v1
  in the resulting application tree.
- [x] Keep the surviving scope limited to lint/maintenance and explicitly approved fixes.
- [x] Fix and validate `preparePlayStoreArtifacts` with configuration cache, generating the AAB,
  mapping file, and native debug-symbol archive.
- [x] Assign candidate metadata `3.18.0` / code `244` for the current grouped minor release.
- [ ] Complete CI, device, update, and Play-delivered validation for `3.18.0`.

Play publication and validation for `3.17.8` are complete per the owner confirmation. The tag and
GitHub Release remain an explicit administrative follow-up and are not created automatically. Release
`3.17.9` intentionally groups PR #62, the incremental-architecture work, and the blocking
call-continuity correction. Its development-build regression is complete; Play-delivered validation
remains open. Any additional call regression reported during that validation interrupts `3.17.10`
and is handled as a scoped `3.17.9` hotfix before this roadmap resumes.

## Decisions after PR #66

The following call-adjacent and cross-surface features are withdrawn from the roadmap. They add
complexity and new decision points to the call flow without enough user benefit to justify their
maintenance and validation cost.

- [x] Do not implement Unified Search v1; keep search entry points and navigation focused on the
  existing feature surfaces.
- [x] Do not implement Post-call Actions v1; keep call completion independent from contact/blocking
  workflows.
- [x] Do not implement Call Screening v1; keep incoming-call behavior and notification handling
  unchanged by optional screening policies.
- [x] Do not schedule these three features in a grouped Play candidate.

## Release workflow

Each work package starts from current `main`, uses its own branch, and ends in one scoped PR. A merged
PR is not a Play release. Do not increment `versionCode`, generate a signed AAB, or publish to Play
for every PR. Accumulate several validated work packages, then freeze and publish one grouped
candidate. Do not mix dependency updates, Room changes, backup changes, or unrelated features inside
one work package.

- [ ] Define the next scoped work package and its acceptance tests.
- [ ] Implement the marked work package on its own branch.
- [ ] Run automated checks, manual checks relevant to the work package, and review the complete diff.
- [ ] Merge the PR into `main`.
- [ ] Repeat for additional work packages without changing the Play version metadata.
- [ ] Freeze the grouped Play candidate and assign its final version metadata.
- [ ] Generate the signed AAB, publish directly to 100% in Play, and validate the delivered build.
- [ ] Update this specification and `CHANGELOG.md` after the grouped Play release.

There is no staged percentage rollout while the user base remains small. If a published build has a
critical regression, stop this roadmap and create a hotfix from the previous stable tag with a new
`versionCode`.

## Release 3.17.8 / 238 — Stable rollback

**Status:** `DONE`

When Play publishes the release:

- [x] Update without uninstalling the existing app.
- [x] Confirm the launcher icon and splash artwork.
- [x] Test PIN authentication.
- [x] Test biometric success, cancellation, and retry.
- [x] Confirm that passphrase authentication is never offered.
- [x] Verify Favorites, Contacts, and History.
- [x] Confirm immediate keyboard focus in contact and history search.
- [x] Place, receive, answer, and end a call.
- [x] Open existing notes, passwords, expenses, and history.
- [x] Restore an older backup on a secondary device.
- [x] Review Android Vitals after the physical smoke test.
- [ ] Create the `3.17.8` tag and GitHub Release.
- [x] Change this release to `DONE` and authorize `3.17.9`.

## Work Package 3.17.9 / PR #62 — Complete Android Lint closure

**Status:** `MERGED`

**Estimated duration:** 5–7 days

**Goal:** resolve all 94 current Android Lint errors in one cohesive work package and make zero
errors a permanent release gate.

PR #62 is merged into `main`. This work package is complete and its changes are retained for the next
grouped Play candidate; do not publish `3.17.9` as a standalone user update.

### Day 1 — Baseline and frozen-contract protection

- [x] Generate a fresh lint report from the published `main` baseline.
- [x] Confirm the five correctness errors and 89 Compose resource-access errors.
- [x] Add tests or guards that keep Room at version 29.
- [x] Add a regression contract that allows only PIN and biometric authentication.
- [x] Keep legacy backup fixtures as compatibility evidence.
- [x] Correct stale Room, Firebase, Dependabot, Tink, and lint documentation.

### Days 2–3 — Complete correction

- [x] Resolve `MissingPermission` without requesting privileged platform permissions.
- [x] Add an API 28–30 fallback for `PhoneNumberUtils.areSamePhoneNumber`.
- [x] Resolve both `SuspiciousIndentation` findings without changing behavior.
- [x] Replace `android:tint` with `app:tint` in the affected widget layout.
- [x] Resolve all `LocalContextGetResourceValueCall` findings.
- [x] Fix warnings only when the change is unambiguous; never bulk-delete resources.

### Day 4 — Permanent gate

- [x] Reach zero errors and zero fatal findings.
- [x] Set Android Lint `abortOnError = true`.
- [x] Set the lint error ratchet to zero.
- [x] Keep warnings at or below the resulting verified ceiling (`393`).
- [x] Retain `ktlintCheck` and `ktlintFormat` for unused imports.
- [x] Run every CI guard.

### Work package validation

- [x] Build Community and Play debug variants.
- [x] Run unit and relevant instrumentation tests.
- [x] Manually validate the changed Compose surfaces on connected devices.
- [x] Review the update/data-preservation path against the installed `3.17.8` baseline.
- [x] Include this work package in the grouped `3.17.9` Play candidate.

**User benefit:** fewer permission/API crashes and a CI gate that prevents known correctness errors
from returning.

## Architecture work package — Included in 3.17.9

**Status:** `MERGED`

**Estimated duration:** 4–6 days

- [x] Make `CallStateBanner` consume the existing `CallContactLookup`.
- [x] Remove `ContactRepository` construction and IO dispatcher ownership from the Composable.
- [x] Introduce `HolidaySource`, `CalendarEventSource`, and `HolidayPreferences`.
- [x] Make `HolidaysViewModel` independent of concrete repositories, Context, and preferences.
- [x] Wire both seams from `AppContainer` and feature factories.
- [x] Add boundary guards that reject restoration of the old dependencies.
- [x] Add focused unit tests with fakes.
- [x] Test the call banner with known and unknown callers.
- [x] Test holidays online, offline, and with calendar account selection.
- [x] Include this work package in the grouped `3.17.9` Play candidate.

**User benefit:** fewer lifecycle-related failures, fewer repeated UI queries, and safer future changes.

## Grouped Play Candidate 3.17.9 / 239 — Architecture and call continuity

**Status:** `IN_REVIEW`

**Frozen scope:** Android Lint closure from PR #62, incremental architecture listed above, and the
call-continuity fix prompted by Play feedback on `3.17.8`. Do not add unrelated functionality.

- [x] Keep `CallStateBanner` on the injected `CallContactLookup` boundary.
- [x] Treat call notifications and both call channels as required dialer readiness.
- [x] Request notification permission during onboarding on Android 13 and newer.
- [x] Expose notification permission/channel recovery in Phone Status.
- [x] Restore the exact active call when Fun Phone returns to the foreground.
- [x] Provide incoming `CallStyle` answer/reject actions and ongoing return, mute, speaker, and end actions.
- [x] Use one lifecycle owner for the ongoing foreground-service notification and remove it on return or disconnect.
- [x] Ignore late `CallStyle` refreshes after the foreground service stops during lock-screen answer transitions.
- [x] Defer stop requests until every pending ongoing foreground-service start reaches `startForeground`.
- [x] Cover notification availability, foreground/background call surfaces, action toggles, and return intents.
- [x] Run the complete automated release gate.
- [x] Re-run unit tests, lint, Community/Play builds, and call instrumentation after the lock-screen hotfix.
- [ ] Re-test `locked screen → answer → InCallScreen → end` on the physical API 35 device.
- [ ] Run the physical incoming/outgoing Telecom matrix on API 35, prioritizing HMD Fusion X1.
- [ ] Repeat compatibility smoke on API 29.
- [x] Freeze, generate, and submit the signed candidate for Play review per owner confirmation.
- [ ] Install and validate the Play-delivered candidate without clearing data.

**User benefit:** every active call has an explicit route back to its controls when call notifications
are available, while setup and Phone Status clearly identify and recover disabled notification access.

## Work Package 3.17.10 / 240 — Private diagnostic logging (previous candidate)

**Status:** `IN_PROGRESS`

**Estimated duration:** 5–7 days

**Frozen scope:** private diagnostic logging, speed-dial settings extraction, and pure preference
presentation. Version `3.17.10` / code `240` was prepared as a previous candidate and must not be
published until the pending
physical and Play-delivered validation gates pass.

- [x] Introduce `DiagnosticEvent` and one central logging facade.
- [x] Accept typed event identifiers instead of arbitrary messages.
- [x] Use sanitized Logcat events in debug and a no-op implementation in release.
- [x] Migrate direct logging in contacts and URI handling.
- [x] Migrate direct logging in Telecom, calls, audio, and proximity handling.
- [x] Migrate direct logging in notes, QR, recordings, settings, and tools.
- [x] Remove every `printStackTrace` call from production code.
- [x] Remove names, numbers, URIs, note content, call handles, passwords, and credentials from logs.
- [x] Reject `android.util.Log` outside the logging implementation.
- [x] Reject `printStackTrace` in production sources.
- [x] Inspect Logcat in debug and a release-equivalent benchmark build.
- [x] Run unit tests for Community and Play, instrumentation on API 35 emulator and physical device,
  Android Lint, ktlint, the lint ratchet, and Community/Play debug builds.
- [x] Re-run the frozen candidate gate and 40 instrumented cases on both the API 35 emulator and
  Moto g34 5G; the physical run skips the two emulator-isolated provider mutation cases.
- [x] Generate the signed AAB, mapping, and native-symbol archive through
  `preparePlayStoreArtifacts`.
- [ ] Run the physical Telecom regression matrix.
- [x] Keep `3.17.9` / code `239` metadata during development; assign `3.17.10` / code `240` only
  when the candidate is frozen.
- [ ] Publish only after Play-delivered `3.17.9` validation passes or after any required hotfix is
  incorporated and revalidated.

**User benefit:** less personal data exposed through device diagnostics and less logging work in
release builds.

### 3.17.10 sub-block — Speed-dial settings extraction

**Status:** `COMPLETE`

- [x] Move speed-dial and contact-picker presentation out of `SettingsScreen`.
- [x] Make the extracted UI consume immutable contacts/assignments and explicit callbacks instead
  of `AppSettingsViewModel` or `ContactsViewModel`.
- [x] Preserve voicemail reservation, assignment removal, contact search, ordering, photos, and
  existing localized copy.
- [x] Add focused unit tests for contact filtering and assigned-contact resolution.
- [x] Lower the enforced `SettingsScreen` line ceiling without changing settings behavior.
- [x] Run unit tests, ktlint, Android Lint, the quality contract, and Community/Play debug builds.

**User benefit:** speed-dial settings become safer to change and the main settings screen owns less
feature-specific presentation logic.

### 3.17.10 sub-block — Pure preference presentation

**Status:** `COMPLETE`

- [x] Remove `AppSettingsViewModel` ownership from shared selection, theme, color, and switch UI.
- [x] Replace thirteen independent preference-dialog flags with one explicit dialog route.
- [x] Move preference-dialog presentation and label policies out of `SettingsScreen`.
- [x] Preserve all existing options, stored-value fallbacks, localized labels, and apply behavior.
- [x] Add focused unit tests for label resolution and font-scale formatting.
- [x] Lower the enforced `SettingsScreen` line ceiling from 2,270 to 2,080 lines.
- [x] Run Community/Play unit tests, lint and builds plus API 35 emulator/physical instrumentation.
- [ ] Manually smoke-test every extracted preference dialog on the physical device.

**User benefit:** personalization screens have predictable state ownership and reusable UI no longer
depends on a feature ViewModel, reducing regression risk across Settings, Notes, Expenses, and Tools.

## Grouped Play Candidate 3.18.0 / 244 — UI and tools refinement

**Status:** `IN_PROGRESS`

**Frozen scope:** checklist item editing, sheet-based Settings, Expenses navigation, holiday/calendar
month browsing, bounded image processing, contact/social form refinements, Favorites card shapes, and
Pomodoro/Voice UI updates. The candidate also retains the existing Room, backup, PIN/biometric,
Telecom, and local-first contracts.

- [x] Set `release.properties` to `3.18.0` / code `244`.
- [x] Align current README, privacy, Data Safety, setup, agent, changelog, and Play listing metadata.
- [x] Keep the lint warning ceiling at 20 and remove branch-introduced warnings.
- [x] Allow only the approved holidays and user-requested social/web preview network boundaries.
- [ ] Run every CI guard, unit test, relevant instrumentation test, and manual regression matrix.
- [ ] Update over the currently installed Play build without clearing data.
- [ ] Publish the grouped candidate and validate the Play-delivered build.

## Withdrawn scope — 3.18.1 / 3.18.2

**Status:** `CANCELLED`

Unified Search v1, Post-call Actions v1, and Call Screening v1 are intentionally not release work
packages. Their implementation tasks are removed from the active checklist; do not revive them by
merging an old branch or by adding them to a grouped Play candidate.

## Later functional evolution

- [ ] **3.18.3:** incremental contact synchronization without new Room tables.
- [ ] **3.19.0:** contacts/detail two-pane layout and adaptive validation.
- [ ] Select the next work package from current `main`, excluding the cancelled scope above; select
  a grouped Play candidate only after enough merged work packages have accumulated.

## Grouped Play release gate

No PR is published to Play by itself. Use this gate after several work packages have been merged and
validated on `main`:

- [x] Select the accumulated work packages and freeze the grouped scope.
- [x] Assign the final Play version name and increment `versionCode` once for the grouped candidate.
- [x] Move the accumulated entries from `Unreleased` to the final `CHANGELOG.md` version.
- [ ] Run every CI guard, unit test, relevant instrumentation test, and manual regression matrix.
- [ ] Update over the currently installed Play build without clearing data.
- [x] Generate the signed AAB through `preparePlayStoreArtifacts`.
- [ ] Publish the grouped candidate directly to 100% in Play.
- [ ] Install the Play-delivered build and complete the smoke test plus Android Vitals review.
- [ ] Create the final tag and GitHub Release, then mark the grouped candidate `DONE`.

## Frozen contracts

- [ ] Room remains at version 31 (current executable baseline; 31 adds the contacts recycle bin table).
- [ ] Do not change entities, DAOs, exported schemas, or migrations.
- [ ] PIN and biometrics remain the only protected-section authentication methods.
- [ ] Do not introduce passphrase authentication.
- [ ] Do not change backup formats or cloud/device-transfer rules.
- [ ] Do not remove Tink or `security-crypto` compatibility dependencies.
- [ ] Do not introduce Hilt.
- [ ] Do not create a Gradle multi-module architecture.
- [ ] Do not add telemetry, cloud sync, or new mini-apps.
- [ ] Do not start exhaustive performance work without a measured regression.

## Deferred changes: user benefit and reason to wait

| Deferred change | Possible user benefit | Reason to wait |
| --- | --- | --- |
| Hilt | Fewer indirect lifecycle and wiring errors; easier dependency replacement in tests. | It adds no visible feature and would touch Activities, ViewModels, Workers, and Services while `AppContainer` already covers current needs. |
| Gradle multi-module split | Stronger feature isolation and potentially faster incremental developer builds. | It does not automatically speed up the installed app; package interfaces provide the main safety benefit with less risk. |
| Direct Tink adoption | Harder-to-misuse cryptographic primitives and better keyset rotation support. | Changing encryption formats requires migrating existing keys and ciphertext. |
| Tink removal | A slightly smaller dependency surface. | Tink preserves the legacy `EncryptedSharedPreferences` path used to recover older keys; premature removal could lock users out of existing data. |
| New encrypted backup format | Safer and more portable recovery between devices. | A format or key migration error could make existing backups unusable. |
| Room migrations | FTS, merge journals, advanced tags, and encryption of additional entities. | Database migration is the highest-risk path for Play updates and retained user data. |
| Access passphrase | More entropy than a short PIN. | It adds unwanted friction and conflicts with the chosen PIN/biometric experience. |
| Baseline Profiles and exhaustive performance work | Faster startup and less jank on slower devices. | Current measurements are already comfortably within budget; only measured regressions justify this work. |
| Vault 2.0 and Notes FTS | Autofill, TOTP, and richer private search. | These features require currently frozen encryption, authentication, or Room changes. |

## Grouped Play acceptance criteria

- [ ] CI is completely green.
- [x] No unexpected Room or backup changes exist in the diff.
- [ ] PIN and biometric authentication pass on a physical device.
- [ ] The app updates without clearing data.
- [ ] Older backups remain recognized.
- [ ] Physical incoming and outgoing call smoke tests pass.
- [x] The signed AAB is generated through the official release pipeline.
- [ ] The production release is published to 100%.
- [ ] The Play-delivered build is installed and tested.
- [x] `CHANGELOG.md` and this specification are updated before closing the release.
