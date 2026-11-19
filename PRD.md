# Product Requirements Document (PRD)
## Fun Phone Android Application

### Document Information
- **Product Name**: Fun Phone
- **Version**: 3.21.1 (Build 270; source: `release.properties`)
- **Date**: November 19, 2026
- **Document Type**: Product Requirements Document
- **Application ID**: `com.bookverse.contacts` (Play) and `com.bookverse.contacts.community` (community)
- **Status**: Active / In Development

---

## 1. Executive Summary

### 1.1 Product Overview
Fun Phone is a private, customizable Android dialer and contacts app with optional tools. Its core
experience covers default-dialer activation, T9 search, favorites, recents, contact management,
call controls, on-device call screening, and permission recovery. Users may opt into Room-backed
notes/reminders and expenses, a Tink-encrypted vault with system autofill, QR and speech utilities,
a Pomodoro timer, cycle tracking, paint, and games without enabling the whole tools area. The
Kotlin-only, Android-only codebase uses Jetpack Compose + Material 3 and MVVM with explicit
repository/data boundaries across feature and core Gradle modules.

### 1.2 Business Objectives
- Deliver a trustworthy default dialer and contact manager that is useful before optional tools.
- Extend retention by blending productivity (notes, reminders, QR flows, expenses, Pomodoro) with delightful extras (voice tools, games, paint).
- Maintain local-first private storage, authenticated vault encryption, and PIN/biometric access gates.
- Support offline and low-permission modes via cached contacts, widgets, and user-held encrypted backups.
- Keep both distributions free of telemetry, Firebase, advertising and developer-operated cloud services.

### 1.3 Current Capabilities (v3.21.x)
- Two distributions: `play` (ML Kit QR scanning and the native In-App Review client) and `community`
  (ZXing QR scanning, no Google libraries). They install side by side and do not share data.
- Two-step onboarding: a concise welcome with contact/call benefits, followed by guided phone setup.
  Each missing requirement is requested only after an explicit tap; users can continue immediately
  in limited mode and recover through Device Status. The welcome also offers optional tool presets
  and custom visibility, with the same choices preserved in Settings.
- Limited-mode guidance links to Device Status and tool customization without automatically
  requesting access or changing tool visibility. Setup is reconciled on return from Android Settings
  and Device Status, including notification channels, exact-alarm access for birthday reminders,
  and all core permissions. Exact-alarm access is optional for the phone surface, so declining it
  does not hide contacts or enter limited mode. Birthday reminders do not open a separate
  permission prompt after onboarding.
- Call features: SIM choice, conference calls, hold/swap, DTMF tones, speaker/Bluetooth/wired
  routing, reply with a message, and on-device call screening with spam filtering and blocked numbers.
- Contacts: Android-account contacts with alphabetical index, tags, favorites, vCard/JSON import and
  export, a duplicates finder, a recycle bin and contact QR codes.
- Notes with rich text, checklists, folders, tags, images, global search, repeating reminders, voice
  recordings, a recycle bin and a Quick Settings tile for new notes.
- Password vault encrypted with Tink and the Android Keystore, unlocked with PIN or biometrics,
  with folders, a recycle bin, generator, TOTP codes, reused-password warnings and system autofill
  (`VaultAutofillService`).
- Expense Manager with income/expense transactions, categories, per-category budgets, fixed
  (recurring) transactions, monthly and yearly views, filters and CSV export.
- Public Holidays explorer powered by the Nager.Date API (opt-in) plus contact birthdays and
  read-only device calendar events.
- Compose game suite (Fun Bird, Snake, Minesweeper, 2048, Puzzle, Tic-Tac-Toe, Hangman, Breakout,
  Sudoku, Memory) with stat tracking, a paint studio, a Pomodoro timer and an on-device menstrual
  calendar.
- Modern, Minimal and Retro visual styles in light and dark, accent colors, 25+ fonts, text size,
  animations, vibration patterns and adaptive glass effects.
- Encrypted full-app backup (passphrase-protected, vault optional) that the user exports and keeps.
- Home screen widgets for favorites, recent calls, notes and expenses.
- Durable reminder delivery, Telecom regression gates, explicit module and feature boundaries, and
  a Baseline Profile with macrobenchmarks.

---

## 2. Product Scope

### 2.1 Target Platform
- **Platform**: Android phones and large-screen devices. Android only: no KMP, iOS, web or desktop targets.
- **Minimum SDK**: 29 (Android 10).
- **Target/Compile SDK**: 37.
- **Languages**: English (baseline), Spanish, German, French and Portuguese, kept in sync for every visible string.
- **Architecture**: MVVM with repository/data/use-case separation across `:app`, `:core:*` and `:feature:*` modules.
- **UI**: Jetpack Compose + Material 3 with `NavigationViewModel`-driven navigation and custom components.

### 2.2 Key Stakeholders
- **Product Management** – roadmap ownership, prioritization, inbound feedback.
- **Engineering** – Android developers for features, tooling, performance, and reliability.
- **Design & Research** – UX for Compose surfaces, accessibility, theming.
- **Security & Compliance** – vault encryption, PIN/biometric flows and platform permission boundaries.
- **Product quality** – reviews reproducible local checks and owner-provided feedback without usage telemetry.
- **Support & QA** – test plans, regression triage, documentation of known issues.

### 2.3 Primary Personas
- **Everyday Organizer** – depends on dependable contacts, reminders, and quick note capture.
- **Power Communicator** – heavy dialer caller needing widgets, favorites, call history accuracy.
- **Privacy-Conscious User** – stores passwords and locked notes, expects biometric/PIN gates.
- **Creative Break Seeker** – uses mini games and paint for short entertainment bursts.
- **On-the-Go Pro** – leverages QR tools, voice dictation, and holiday insights while traveling.

### 2.4 Out of Scope
- iOS/web/desktop companions.
- Real-time collaborative editing or shared vaults (future roadmap).
- Ads, subscriptions, or other monetization levers.
- Developer-operated contact sync, remote photo storage, analytics and cloud backups.
- Call recording or server-side contact deduplication (future ideas only).

---

## 3. Functional Requirements

### 3.1 Contacts & Dialer (CD)
- **CD-001**: Create/read/update/delete contacts with multi-field support (phones, emails, birthdays, notes, social links, custom ringtones, avatars, tags).
- **CD-002**: Normalize search across all stored fields with <500 ms response for 10k contacts.
- **CD-003**: Manage favorites grid (custom columns/count, drag reorder, pinned profile) and keep widget/data store state in sync.
- **CD-004**: Provide call history merging Room `RecentOperation` entries with CallLog data, including swipe actions (call, block, share) and smart filters.
- **CD-005**: Deliver Compose incoming/in-call surfaces with DTMF keypad, mute, hold, swap, conference, SIM choice, speaker/Bluetooth/wired routing, reply with a message, and proximity-based screen control.
- **CD-006**: Support contact export/import through vCard intents, local JSON files with local photo references, and QR codes; enable batch share flows.
- **CD-007**: Allow blocking/unblocking phone numbers via the platform blocked-number APIs with conflict detection and undo feedback, and screen incoming calls on the device (`FunPhoneCallScreeningService`) with user-configured spam filtering.
- **CD-008**: Operate offline through Room-based caches (populated by `ContactSyncWorker`) and degraded “limited mode” when contacts permission is missing.
- **CD-009**: Surface Phone Status diagnostics including default dialer request, permission checks, and quick links to OS settings.
- **CD-010**: Find duplicate contacts locally (with ignorable pairs) and keep deleted contacts in a recoverable recycle bin.

### 3.2 Notes & Reminders (NR)
- **NR-001**: Compose editor with rich text, headings, locked mode, checklist support, images, folders, tags, global search, and archived states.
- **NR-002**: Reminder scheduling via `NotificationService` + AlarmManager supporting one-off, periodic, and advance notifications with exact-alarm requests when required.
- **NR-003**: Audio recordings per note captured in `RecordingData`, with playback, rename, delete, and share flows.
- **NR-004**: Import/export pipelines using `NotesBackupUtils` (HTML, JSON, or ZIP, including folders and tags) with share intents and validation of unsupported formats.
- **NR-005**: Provide locking/unlock-all controls that respect biometric or PIN requirements configured in settings.
- **NR-006**: Keep deleted notes in a recycle bin and offer a Quick Settings tile and widget for fast note capture.

### 3.3 Password Vault (PV)
- **PV-001**: Encrypt credentials (`PasswordEntity`) with Tink + Android Keystore; decrypted values never persist outside memory.
- **PV-002**: Gate access with biometric or PIN auth (configurable), track enablement flags, and enforce re-auth on resume.
- **PV-003**: Offer password generator presets (length, symbols) plus strength hints and reused-password warnings before saving.
- **PV-004**: Support search/filter across decrypted data with diacritic-insensitive matching and throttled updates.
- **PV-005**: Allow secure JSON share/export with FileProvider (no plaintext storage on disk).
- **PV-006**: Organize entries in folders with a recycle bin, generate TOTP two-step codes, and fill credentials through Android autofill after authentication.

### 3.4 Voice & Audio Utilities (VA)
- **VA-001**: Provide speech-to-text through the user's configured Android speech provider with partial results, auto-stop timers, and permission rationale flows.
- **VA-002**: Surface text-to-speech controls (voice picker, speed, pitch, clipboard, share intents) with error handling for unavailable engines.
- **VA-003**: Generate shareable transcripts, save snippets back into notes, and respect microphone permission state.

### 3.5 QR & Sharing Tools (QR)
- **QR-001**: CameraX-powered scanner (ML Kit in `play`, ZXing in `community`) with pinch-to-zoom, tap-to-focus, torch toggle, and overlay instructions.
- **QR-002**: Decode Wi-Fi payloads, URLs, plain text, vCard/JSON contact exports, and convert to actionable flows (create contact, open link, copy).
- **QR-003**: Import QR images from the gallery and process offline.
- **QR-004**: Generator supporting contact, Wi-Fi, URL, and free-form text payloads with Compose previews, color accents, and save/share actions.
- **QR-005**: Keep a local QR history (`QRHistoryEntity`) that the user can review and clear.

### 3.6 Holiday Insights & Calendar (HI)
- **HI-001**: Fetch holidays over HTTPS from the Nager.Date API after disclosure and approval with per-country/year caching and TTL-based invalidation.
- **HI-002**: Allow users to pick countries (flag selector) and years, with horizontal pager per month and scroll-to-today behavior.
- **HI-003**: Optional read-only CalendarContract integration that lists events from selected calendars, respecting READ_CALENDAR access.
- **HI-004**: Present contextual errors (client, server, offline) and retry flows.
- **HI-005**: Project contact birthdays into the monthly agenda on their exact date without creating duplicate calendar events.

### 3.7 Games, Paint & Entertainment (GE)
- **GE-001**: Games hub grid listing the 10 Compose games with hero art, quick launch, and stats modal.
- **GE-002**: Persist per-game stats (high scores, streaks, difficulty) via DataStore (`DataStoreGameStatsRepository`) for achievements sheets.
- **GE-003**: Paint studio that supports multi-brush drawing, image import, text overlay, color pickers, undo/redo, gallery save, and share intents.
- **GE-004**: Pomodoro timer backed by a foreground `PomodoroService` so sessions survive leaving the screen.

### 3.8 Personalization & Settings (PS)
- **PS-001**: Settings allow selecting the visual style (Modern, Minimal, Retro), accent colors, fonts, text size, theme mode (dark/light/system), accent bar style, glass effects, and animation speed.
- **PS-002**: Let users configure transition types (slide, fade, elastic, etc.) and vibration duration/pattern used throughout the UI.
- **PS-003**: Manage password manager enablement, required auth method (biometric vs. PIN), and saved PIN hash.
- **PS-004**: Store default account for new contacts, favorites column count/order, tool visibility/order, home shortcuts, default dialer consent flag, and call screening preferences via DataStore.
- **PS-005**: Provide privacy toggles (note locks reset, clear cached contacts, limited mode), the app lock, encrypted backup/restore, and links to support/about.

### 3.9 Widgets & Shortcuts (WS)
- **WS-001**: Glance widgets for favorites, recent calls, notes and expenses display cached data with quick actions and update when their data changes.
- **WS-002**: Support app shortcuts (favorites, contact detail, recents, dialer, notes) that deep-link into the Compose navigation stack.
- **WS-003**: Handle widget/shortcut-deep links securely, even when the app is locked or limited mode is enabled.

### 3.10 Cloud, Sync & Backup (CB)
- **CB-001**: Keep startup offline; no remote account, App Check or Firebase runtime.
- **CB-002**: Keep photos and exports local; cloud backup is disabled. Preserve Android-authorized local device transfer rules.
- **CB-003**: Provide a passphrase-encrypted full-app backup (`AppBackupManager`, vault optional) plus manual exports for contacts, passwords, notes and expenses.
- **CB-004**: Schedule `ContactSyncWorker` to refresh cached contacts every six hours (plus manual triggers) when permissions allow.
- **CB-005**: Persist settings via DataStore and maintain boot receivers to reschedule reminders and widgets after device restarts.

### 3.11 Expense Manager (EM)
- **EM-001**: Capture expenses and incomes with category, date, optional note and a fixed (recurring) flag; persist in Room for offline mode.
- **EM-002**: Provide monthly and yearly views, category breakdowns and filters.
- **EM-003**: Support per-category budgets with progress indicators.
- **EM-004**: Respect sensitive sections lock—optionally require biometric/PIN before opening and after backgrounding.
- **EM-005**: Offer CSV export with formula-injection protection, include expenses in the encrypted app backup, and keep the default currency as a setting.

### 3.12 Wellness & Cycle Tracking (WC)
- **WC-001**: Menstrual Calendar stores cycle length, luteal phase, and first-day entries locally with adjustable presets.
- **WC-002**: Visualize past/present/future cycles via timeline + calendar grid, highlight fertile windows, ovulation estimate, and next period prediction.
- **WC-003**: Surface contextual tips per cycle phase.
- **WC-004**: Allow manual edits and reset while keeping all data offline; include it only in the user-held encrypted backup.
- **WC-005**: Ensure accessibility (colorblind-friendly palettes, TalkBack descriptions) and privacy (no sync, optional locking under sensitive sections).

---

## 4. Technical Requirements

### 4.1 Module Layout
- `:app`: navigation, the `AppContainer` composition root, in-call screens, Telecom services, contact detail and autofill activities, and the manifest.
- `:core:designsystem` (theme and string-free components), `:core:database` (Room), `:core:datastore` (the single settings DataStore), `:core:security` (`CryptoManager`, PIN, app lock), `:core:ui` (shared strings, resources and components) and `:core:common` (diagnostics, `SecondaryScreen`, `AppEntryPoints`).
- `:feature:contacts`, `:feature:dialer`, `:feature:settings`, `:feature:widgets`, `:feature:games`, `:feature:expenses`, `:feature:notes`, `:feature:qr`, `:feature:vault` and `:feature:tools`.
- `:baselineprofile`: Baseline Profile generator and macrobenchmarks.
- Shared Gradle configuration lives in the `build-logic` convention plugins. `AGENTS.md` is the canonical description.

### 4.2 Architecture
- MVVM layers with `ViewModel`s (manual DI via `AppContainer` and `ViewModelProvider.Factory`, no Hilt) exposing `StateFlow`, Compose screens collecting state with `collectAsStateWithLifecycle()`, and repositories mapping to system APIs (Contacts Provider, CalendarContract, WorkManager and bounded Android adapters).
- Domain data classes annotated with `@Immutable` for Compose compiler stability guarantees; semantic color tokens defined in `:core:designsystem` (`ContactsAppTheme`).
- Navigation managed by `NavigationViewModel` with consolidated `NavigationUiState` StateFlow; main tab pager (Favorites, Contacts, Recents, Extras) plus `SecondaryScreen` enum for detail flows.
- Room + WorkManager for offline caches; DataStore for global settings and game statistics; existing SharedPreferences integrations remain limited.
- `:app` hosts the call services (`FunPhoneInCallService`, `IncomingCallService`, `OngoingCallService`, `FunPhoneCallScreeningService`) and helpers (`CallController`, `CallNotificationManager`).
- `ContactsViewModel` implements `onCleared()` for actor channel cleanup and photo cache eviction.

### 4.3 Key Libraries & Integrations
- Versions live in `gradle/libs.versions.toml`; this document does not duplicate them.
- AndroidX Contacts, CameraX, ZXing (both flavors for generation, `community` for scanning) and ML Kit barcode scanning (`play` only).
- No Firebase, remote authentication, analytics, advertising or crash-reporting SDKs.
- Compose BOM, Material 3, Lifecycle Runtime Compose, Glance for app widgets, and Haze for glass effects.
- Coil (base + SVG), Konfetti Compose, Lucide icons, RichEditor Compose, uCrop.
- Gson, Guava, AndroidX WorkManager and AlarmManager; HTTPS holiday lookup and a user-requested social/web preview are the only direct network boundaries.
- Android SpeechRecognizer/TextToSpeech, Tink, AndroidX Security Crypto and Biometric APIs.
- Room (KSP), Coroutines, DataStore, ProfileInstaller and Macrobenchmark.

### 4.4 Performance & Reliability Targets
- **PR-001**: Cold start < 2.0 s on Pixel 5 (API 33); warm start < 1.0 s.
- **PR-002**: Contact list scroll >60 fps with 10k contacts and cached thumbnails.
- **PR-003**: Voice utilities deliver first partial transcript <400 ms and maintain <150 ms TTS latency after initialization.
- **PR-004**: Games render ≥55 fps on mid-tier devices; paint canvas responds within 16 ms per stroke.
- **PR-005**: WorkManager contact sync completes within 30 s and retries gracefully when offline; reminders fire within ±10 s of scheduled time.

### 4.5 Compatibility & Accessibility
- Responsive Compose layouts for phones, tablets, foldables, and landscape orientation.
- Light/dark themes, three visual styles, and high-contrast accent palette.
- WCAG 2.1 AA adherence: TalkBack labels, focus order, scalable fonts, haptic affordances.
- RTL mirroring on supported locales; tests for bi-directional text in notes and QR surfaces.

### 4.6 Observability & Tooling
- Private local diagnostic event codes; never log personal content or credentials.
- No remote telemetry. Quality evidence comes from local tests, sanitized benchmarks and manual device verification.
- Debug logging guarded by `BuildConfig.DEBUG`; sensitive data never logged in release.
- Core Gradle tasks: `assembleCommunityDebug`, `getApk`, `lintCommunityDebug`, `testCommunityDebugUnitTest`, `testDebugUnitTest`, `connectedCommunityDebugAndroidTest`, `preparePlayStoreArtifacts`.
- Guarded release tasks validate keystore presence and signing credentials before producing artifacts.

---

## 5. Non-Functional Requirements

### 5.1 Security Requirements
- Keep network traffic limited to approved holidays and explicit HTTPS social/web previews; attach no app database or contacts.
- Preserve AES-GCM vault encryption, Keystore-wrapped keys and legacy Tink compatibility. Ordinary notes/settings use private local storage; a UI lock does not imply database encryption.
- Require PIN/biometric access to protected surfaces and lock immediately on background or screen off. Start vault decryption only after authentication; save password/note drafts as ciphertext.
- Validate HTTPS URLs, block local-file/content access and mixed content in previews, and never bypass certificate errors.
- Provide in-app privacy controls (clear caches, reset PIN, delete exported files) and respect scoped storage policies.

### 5.2 Usability & Accessibility
- Compose surfaces must pass design audits for spacing, hierarchy, and motion; animations respect user-selected speed and transition styles.
- Onboarding explains tabs, permissions, limited mode, and personalization.
- All key actions reachable within thumb zone; support hardware keyboards where possible.
- Localization maintained for en/es/de/fr/pt (strings, plurals and arrays) with English as fallback; `scripts/verify-localization.sh` enforces parity.

### 5.3 Reliability & Quality
- Maintain ≥99.5% crash-free sessions; ANR rate <0.1%.
- Keep provider synchronization and durable reminders retryable; offer explicit retry/external-browser recovery for network previews.
- Reminder scheduling verifies AlarmManager capabilities (exact alarm permission on API 31+).
- Room migrations covered by exported schemas and regression tests; no destructive fallback. DataStore migrations validated during startup.

### 5.4 Scalability & Performance
- Support at least 15k contacts, 5k notes, hundreds of passwords without noticeable lag.
- Efficient lazy lists/grids and caching for widget updates.
- Manage storage footprint with cleanup jobs (recordings, QR temp files, paint exports).
- WorkManager jobs respect battery/charging constraints for heavy tasks (contact sync, backups).

---

## 6. User Interface Requirements

### 6.1 Design Principles
- Material 3 foundation with Modern, Minimal and Retro styles and customizable accent bar styles. [DESIGN.md](DESIGN.md) is the canonical design reference.
- Neutral-first accent hierarchy: `primary` only for main actions, focus and small anchors.
- Consistent card-based grouping in Extras, settings, and secondary sheets.
- Motion tuned via user-selected animation speed/transition; provide reduced motion mode.
- Adaptive layouts with responsive typography scales.

### 6.2 Key Screens & States
- **Main Tabs**: Bottom nav + pager for Favorites, Contacts, Recents, Extras.
- **Favorites Grid**: Configurable columns, reorder drag handles, widget sync indicator.
- **Contact List & Detail**: Alphabet scrollbar, action chips (call, message, QR, share, block), tags, duplicates and recycle bin, photo controls via Coil/uCrop.
- **Dialer & Call Flow**: Compose incoming/in-call UI, keypad, banners, ongoing notification handling, phone status view.
- **Notes Hub**: List with lock badges, folders, tags, note editor, checklist, recording modal, reminder sheet, recycle bin.
- **Password Vault**: Search bar, folders, list, create/edit sheet, generator, TOTP codes, masked toggles, copy/share feedback, recycle bin.
- **Voice Utilities**: Dictation waveform, transcripts, voice picker dropdown, clipboard/share actions.
- **QR Tools**: Scanner overlay, gallery import button, parsed result cards, generator preview, history.
- **Public Holidays**: Country selector, yearly timeline, month pager, account selection dialog, error/empty placeholders.
- **Expenses**: Monthly/yearly views, filters, budgets, categories, CSV export.
- **Games, Paint & Pomodoro**: Grid of games, leaderboard sheet, in-game overlays, paint toolbar and options dialogs, Pomodoro timer.
- **Settings & About**: Accordions for customization, security, privacy, backup and support content.

### 6.3 Navigation Structure
- Bottom navigation for main tabs; HorizontalPager ensures smooth swipes with state retention.
- Secondary navigation via `NavigationViewModel` and the `SecondaryScreen` enum for settings, notes, games, QR, voice, holidays, paint, passwords, expenses, Pomodoro, dialer, onboarding, about.
- Deep link handling for app shortcuts, widget taps, tel: intents, and share actions.
- Back handler prioritizes secondary stack dismissal before exiting app; limited mode prompts before closing.

---

## 7. Integration Requirements

### 7.1 System Integrations
- Android Contacts Provider for CRUD, account retrieval, blocked numbers, vCard sharing.
- Telecom APIs (`FunPhoneInCallService`, `FunPhoneCallScreeningService`, `CallController`) for answering, rejecting, screening, audio routing, and notifications.
- NotificationManager + AlarmManager for reminders and ongoing call alerts.
- MediaStore/FileProvider for paint exports, QR saves, audio recordings, and backups.
- CalendarContract for event overlays in Holidays screen.
- WorkManager + boot receivers for contact sync and reminder scheduling.
- ShortcutManager, Glance/AppWidgetManager, Quick Settings tiles and the Android autofill framework.

### 7.2 Third-Party & Cloud Services
- No developer cloud, Firebase, remote accounts or telemetry.
- Nager.Date HTTPS API for authorized public holidays; HTTPS social/web sites only inside the requested preview.
- CameraX, ML Kit barcode scanning (`play`) and ZXing.
- Android Speech Services (SpeechRecognizer + TextToSpeech).
- Coil for image loading/caching, uCrop for photo editing.
- Google Play In-App Review (`play` only).

---

## 8. Data Requirements

### 8.1 Data Model
- **Contact** (`Contact`, `ContactPhone`, `ContactAccount`): runtime models mirrored by cached entities.
- **CachedContactEntity / DeletedContactEntity / ContactUsageStats**: Room entities for offline copies, the contact recycle bin and local usage ordering.
- **ContactTag / ContactTagCrossRef / IgnoredDuplicatePair**: tags and dismissed duplicate pairs.
- **RecentOperation**: Room history entries for call/operation timeline.
- **Note / ChecklistItem / ReminderData / RecordingData / NoteFolder / NoteTag / NoteTagCrossRef / NoteImage**: Room entities for the notes domain.
- **PasswordEntity / PasswordFolder**: Encrypted credentials, folders, timestamps, optional notes.
- **ExpenseCategoryEntity / ExpenseTransactionEntity / ExpenseBudgetEntity**: Room entities for expenses.
- **QRHistoryEntity**: local QR history.
- **PublicHoliday**: local API model for holiday metadata.
- **CalendarEvent/CalendarAttendee**: Data classes for CalendarContract projections used to display events and attendees.
- **GameStats**: DataStore entries keyed per game.
- **SettingsStore**: DataStore keys for theme, style, accent, transitions, vibration, favorites, tools, call screening and password options.

### 8.2 Data Storage & Sync
- Room (`AppDatabase` v32) with exported schemas under `core/database/schemas/` and auto-migrations.
- DataStore Preferences (`AppSettingsViewModel`) for global settings; DataStore for game statistics; existing SharedPreferences integrations remain limited.
- Private local photo files and user-selected exports; no remote storage paths.
- JSON/ZIP exports saved via FileProvider; `NotesBackupUtils` handles notes serialization and `AppBackupManager` the encrypted full backup.
- WorkManager (`ContactSyncWorker`) plus AlarmManager for reminders ensures background updates.
- All structured app data resides on-device unless explicitly exported through Android system sharing.

### 8.3 Data Governance
- Form validation for contacts, notes, passwords with realtime error messaging.
- User-initiated export/import flows require explicit confirmation and clean up temp files afterward.
- Provide limited-mode flows that avoid touching contact storage when permissions absent.
- Respect delete/clear-cache actions (recent operations, cached contacts, encrypted tables).
- Document data handling for GDPR-style requests (local deletion and revoking platform permissions) in [PRIVACY_DATA_FLOW.md](PRIVACY_DATA_FLOW.md).

---

## 9. Testing Requirements

### 9.1 Testing Strategy
- JVM unit tests for policies, repositories and utilities (for example `ContactRepository`, `ContactCacheRepository`, `RecentOperationsRepository`, `PasswordRepository`, `HolidaysRepository`, `SpeechUtils`, `CallController`, `CallScreeningPolicy`).
- Room migration tests from the relevant schema versions, preserving data.
- Compose UI tests for critical flows (Favorites tab, Extras hub navigation, notes editor locking, password vault flows, voice utilities, QR scanner states).
- Instrumented tests for QR scanning, password unlock, reminder scheduling and widget interactions.
- Integration tests with fake holiday sources for holidays and simulated WorkManager contact sync.
- Baseline Profile generation and macrobenchmarks for startup, contact list scrolling and other critical journeys.
- Telecom changes follow `scripts/verify-telecom-contract.sh` and [TELECOM_REGRESSION_MATRIX.md](TELECOM_REGRESSION_MATRIX.md).

### 9.2 Quality Gates & Tooling
- [QUALITY_RELEASE_GATES.md](QUALITY_RELEASE_GATES.md) is the canonical source for quality contracts and evidence.
- CI (`.github/workflows/pr-checks.yml`) runs the Android-only, Telecom, feature-boundary, source-format, quality-contract, performance-tool, Play listing and public-readiness guards, then the Gradle build, unit tests and lint for the affected modules, with the lint ratchet (zero errors and a warning ceiling).
- `connectedCommunityDebugAndroidTest` runs on demand or before release candidate sign-off.
- Testing checklist includes localization (en/es/de/fr/pt), RTL mirroring, TalkBack, offline/limited mode, and permission denial handling.

---

## 10. Deployment & Release Management

### 10.1 Build & Packaging
- Toolchain (AGP, Kotlin, Compose BOM) pinned in `gradle/libs.versions.toml`; JVM 25 and `compileSdk` set by the `build-logic` convention plugins.
- Flavors `community` (`.community` suffix) and `play`; build types `debug` (`.debug` suffix, non-minified), `release` (minify + shrink resources) and isolated, non-publishable `benchmark`.
- Release signing credentials resolved from env vars, `keystore.properties`, `local.properties` or `gradle.properties`; guard tasks fail immediately when signing is incomplete.
- Gradle helpers: `getApk`, `preparePlayStoreArtifacts`, `packagePlayReleaseNativeSymbols` for Play Console uploads.
- Proguard/R8 rules maintained for Compose, ML Kit and legacy Tink compatibility.

### 10.2 Release Strategy
- Progressive releases follow [FUN_PHONE_PROGRESSIVE_RELEASE_SPEC.md](FUN_PHONE_PROGRESSIVE_RELEASE_SPEC.md) and [RELEASING.md](RELEASING.md): a new release starts only after its predecessor's Play validation is complete.
- Play builds go through testing tracks and staged rollout; signed community APKs are published on GitHub Releases. F-Droid distribution is planned.
- Owner reviews Play-delivered smoke tests and Android Vitals; preserve a scoped hotfix path for call/permission regressions.
- Feature highlights updated in the Play listing ([PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md)); release notes recorded in [CHANGELOG.md](CHANGELOG.md).
- Feedback comes from store reviews, GitHub issues and direct reports; there is no in-app usage tracking.

---

## 11. Success Metrics

Fun Phone collects no usage telemetry. Metrics come only from aggregate data that Google Play
Console and Android Vitals already provide, store reviews, GitHub activity, and local test or
benchmark evidence.

### 11.1 Product KPIs
- Installs, active devices and retention as reported in aggregate by Play Console.
- Store rating (target ≥4.6) and qualitative feedback sentiment from reviews and issues.
- Recurring feature requests and support themes, used to prioritize the roadmap.

### 11.2 Technical KPIs
- Crash-free sessions ≥99.5%; ANR rate <0.1% (Android Vitals).
- No regressions in the Telecom regression matrix before each release.
- Startup and scrolling macrobenchmarks within the targets in section 4.4.
- Zero lint errors and no growth beyond the lint warning ceiling.
- Accessibility issue resolution within one release cycle.

---

## 12. Future Enhancements

### 12.1 Planned Features & Experiments
- Collaborative notes/reminders with selective sharing.
- User-authorized local device transfer; no cloud synchronization is approved.
- Smarter on-device contact deduplication, enrichment, and suggestions.
- Voice command support for key actions (call, send note, launch QR).
- Downloadable theme or game packs and configurable widget layouts.
- Holiday-aware reminders and context-aware call availability insights.
- F-Droid distribution of the community build.

### 12.2 Technical Roadmap
- Expand Baseline Profile and macrobenchmark coverage to more critical journeys.
- Expand offline-first diffing for contacts/notes to prep for local device transfer.
- Harden vault security (Class 3 biometrics, hardware-backed keys, integrity monitoring).
- Keep modularizing only where it brings Android build or boundary benefits; no Kotlin Multiplatform.
- Extend the CI guards for schemas, privacy boundaries, localization and release metadata.

---

## Appendices

### A. Permissions
Source of truth: `app/src/main/AndroidManifest.xml`.
- Contacts and accounts: READ_CONTACTS, WRITE_CONTACTS, GET_ACCOUNTS.
- Calls: CALL_PHONE, ANSWER_PHONE_CALLS, MANAGE_OWN_CALLS, READ_PHONE_STATE, READ_CALL_LOG, WRITE_CALL_LOG.
- Runtime, requested in context: RECORD_AUDIO (note recordings and speech), CAMERA (QR), READ_CALENDAR, POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM.
- Services and call UI: FOREGROUND_SERVICE, FOREGROUND_SERVICE_PHONE_CALL, FOREGROUND_SERVICE_SPECIAL_USE, USE_FULL_SCREEN_INTENT, SYSTEM_ALERT_WINDOW, WAKE_LOCK, DISABLE_KEYGUARD.
- Other: INTERNET, ACCESS_NETWORK_STATE, VIBRATE, RECEIVE_BOOT_COMPLETED, READ_EXTERNAL_STORAGE (≤API 32) for QR/image imports; FileProvider manages scoped storage.
- Bound services: BIND_INCALL_SERVICE, BIND_SCREENING_SERVICE, BIND_QUICK_SETTINGS_TILE, BIND_AUTOFILL_SERVICE.

### B. Third-Party Libraries & Services
- Jetpack Compose, Material 3, Glance.
- Room, DataStore, WorkManager, AlarmManager helpers, Kotlin Coroutines.
- Play-only ML Kit barcode scanning and native In-App Review; no Firebase, analytics, advertisements or crash-reporting SDKs.
- CameraX and ZXing.
- Bounded HTTPS platform adapters, Gson and Guava.
- Coil (base + SVG), uCrop, Contacts-Android, Konfetti, Lucide icons, RichEditor Compose, Haze.
- Tink, AndroidX Security Crypto, Biometric API.
- Nager.Date public holiday API, Android Speech Services.

---

*This PRD reflects Fun Phone v3.21.1 (build 270) and consolidates the current functional, technical, and experience requirements across the core dialer/contacts product and optional extensions.*
