# Product Requirements Document (PRD)
## Fun Phone Android Application

### Document Information
- **Product Name**: Fun Phone
- **Version**: 3.20.2 (Build 262; source: `release.properties`)
- **Date**: November 8, 2026
- **Document Type**: Product Requirements Document
- **Application ID**: com.bookverse.contacts
- **Status**: Active / In Development

---

## 1. Executive Summary

### 1.1 Product Overview
Fun Phone is a private, customizable Android dialer and contacts app with optional tools. Its core
experience covers default-dialer activation, T9 search, favorites, recents, contact management,
call controls, and permission recovery. Users may opt into Room-backed notes/reminders and expenses,
a Tink-encrypted vault, QR and speech utilities, wellness, paint, and games without enabling the
whole tools area. The Kotlin-only codebase uses Jetpack Compose + Material 3 and MVVM with explicit
repository/data boundaries.

### 1.2 Business Objectives
- Deliver a trustworthy default dialer and contact manager that is useful before optional tools.
- Extend retention by blending productivity (notes, reminders, QR flows) with delightful extras (voice tools, games, paint).
- Maintain local-first private storage, authenticated vault encryption, and PIN/biometric access gates.
- Support offline and low-permission modes via cached contacts, widgets, and JSON backups.
- Keep both distributions free of telemetry, Firebase, advertising and developer-operated cloud services.

### 1.3 Release Highlights (v3.16.x)
- Community and Play distributions without Firebase; only Play contains the native In-App Review client.
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
- Durable reminder delivery, Telecom regression gates, and explicit architecture boundaries.
- Compose performance hardening: lifecycle-aware state collection (`collectAsStateWithLifecycle`), `@Immutable` annotations on all domain data classes, stable LazyList keys, semantic color tokens, and elimination of `runBlocking`/uncontrolled coroutine launches inside Composables.
- ViewModel consolidation: `NavigationViewModel` and `MemoryGameViewModel` migrated from `mutableStateOf` to consolidated `StateFlow<UiState>` pattern; `ContactsViewModel` lifecycle cleanup with `onCleared()`.
- WorkManager-driven contact cache plus “limited mode” onboarding so the app remains useful even before permissions are granted.
- Phone Status diagnostics that verify default dialer consent, required permissions, and offer single-tap remediation.
- Extras hub refresh with speech utilities (dictation + multi-voice TTS), QR scanner/generator, paint studio, password manager entry, and shortcut to notes/games.
- Public Holidays explorer powered by the Nager.Date API with year/country pickers and optional CalendarContract overlays.
- Hardened password vault leveraging Tink encryption, biometric or PIN unlock, password generator, and JSON share/export helpers.
- Local JSON/QR contact import/export with FileProvider photo sharing and gallery-based QR decoding.
- Compose game suite (Fun Bird, Snake, Minesweeper, 2048, Puzzle, Tic-Tac-Toe, Hangman, Breakout, Sudoku, Memory) with stat tracking plus enhanced paint canvas import/share tools.
- Lifestyle upgrades featuring an on-device Menstrual Calendar (cycle predictions, wellness tips) and the Expense Manager (budgets, categories, fixed transactions, export/import, insights) available directly from Extras and Settings quick actions.

---

## 2. Product Scope

### 2.1 Target Platform
- **Platform**: Android phones and large-screen devices.
- **Minimum SDK**: 29 (Android 10).
- **Target/Compile SDK**: 37.
- **Languages**: Spanish (default) and English, extensible via localized string packs.
- **Architecture**: MVVM with repository/data/use-case separation in a single `app/` module.
- **UI**: Jetpack Compose + Material 3, Navigation Compose, and custom components.

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
- Call recording, spam detection, or server-side contact deduplication (future ideas only).

---

## 3. Functional Requirements

### 3.1 Contacts & Dialer (CD)
- **CD-001**: Create/read/update/delete contacts with multi-field support (phones, emails, birthdays, notes, social links, custom ringtones, avatars).
- **CD-002**: Normalize search across all stored fields with <500 ms response for 10k contacts.
- **CD-003**: Manage favorites grid (custom columns/count, drag reorder, pinned profile) and keep widget/data store state in sync.
- **CD-004**: Provide call history merging Room `RecentOperation` entries with CallLog data, including swipe actions (call, block, share) and smart filters.
- **CD-005**: Deliver Compose incoming/in-call surfaces with DTMF keypad, mute, hold, swap, speaker/Bluetooth/wired routing, and ProximitySensor-based screen control.
- **CD-006**: Support contact export/import through vCard intents, local JSON files with local photo references, and QR codes; enable batch share flows.
- **CD-007**: Allow blocking/unblocking phone numbers via AndroidX Contacts blocked-number APIs with conflict detection and undo feedback.
- **CD-008**: Operate offline through Room-based caches (populated by `ContactSyncWorker`) and degraded “limited mode” when contacts permission is missing.
- **CD-009**: Surface Phone Status diagnostics including default dialer request, permission checks, and quick links to OS settings.

### 3.2 Notes & Reminders (NR)
- **NR-001**: Compose editor with rich text, headings, locked mode, checklist support, photo previews, and archived states.
- **NR-002**: Reminder scheduling via `NotificationService` + AlarmManager supporting one-off, periodic, and advance notifications with exact-alarm requests when required.
- **NR-003**: Audio recordings per note captured in `RecordingData`, with playback, rename, delete, and share flows.
- **NR-004**: Import/export pipelines using `NotesBackupUtils` (HTML, JSON, or ZIP) with share intents and validation of unsupported formats.
- **NR-005**: Provide locking/unlock-all controls that respect biometric or PIN requirements configured in settings.

### 3.3 Password Vault (PV)
- **PV-001**: Encrypt credentials (`PasswordEntity`) with Tink + Android Keystore; decrypted values never persist outside memory.
- **PV-002**: Gate access with biometric or PIN auth (configurable), track enablement flags, and enforce re-auth on resume.
- **PV-003**: Offer password generator presets (length, symbols) plus strength hints before saving.
- **PV-004**: Support search/filter across decrypted data with diacritic-insensitive matching and throttled updates.
- **PV-005**: Allow secure JSON share/export with FileProvider (no plaintext storage on disk).

### 3.4 Voice & Audio Utilities (VA)
- **VA-001**: Provide on-device speech-to-text with partial results, auto-stop timers, and permission rationale flows.
- **VA-002**: Surface text-to-speech controls (voice picker, speed, pitch, clipboard, share intents) with error handling for unavailable engines.
- **VA-003**: Generate shareable transcripts, save snippets back into notes, and respect microphone permission state.

### 3.5 QR & Sharing Tools (QR)
- **QR-001**: CameraX-powered scanner with pinch-to-zoom, tap-to-focus, torch toggle, and overlay instructions.
- **QR-002**: Decode Wi-Fi payloads, URLs, plain text, vCard/JSON contact exports, and convert to actionable flows (create contact, open link, copy).
- **QR-003**: Import QR images from the gallery and process offline.
- **QR-004**: Generator supporting contact, Wi-Fi, URL, and free-form text payloads with Compose previews, color accents, and save/share actions.

### 3.6 Holiday Insights & Calendar (HI)
- **HI-001**: Fetch holidays over HTTPS from the Nager.Date API after disclosure and approval with per-country/year caching and TTL-based invalidation.
- **HI-002**: Allow users to pick countries (flag selector) and years, with horizontal pager per month and scroll-to-today behavior.
- **HI-003**: Optional read-only CalendarContract integration that lists events from selected calendars, respecting READ_CALENDAR access.
- **HI-005**: Project contact birthdays into the monthly agenda on their exact date without creating duplicate calendar events.
- **HI-004**: Present contextual errors (client, server, offline) and retry flows.

### 3.7 Games, Paint & Entertainment (GE)
- **GE-001**: Games hub grid listing the 10+ Compose games with hero art, quick launch, and stats modal.
- **GE-002**: Persist per-game stats (high scores, streaks, difficulty) via SharedPreferences/DataStore for achievements sheets.
- **GE-003**: Paint studio that supports multi-brush drawing, image import, text overlay, color pickers, undo/redo, gallery save, and share intents.

### 3.8 Personalization & Settings (PS)
- **PS-001**: Settings allow selecting accent colors, fonts, theme mode (dark/light/system), accent bar style, and animation speed.
- **PS-002**: Let users configure transition types (slide, fade, elastic, etc.) and vibration duration/pattern used throughout the UI.
- **PS-003**: Manage password manager enablement, required auth method (biometric vs. PIN), and saved PIN hash.
- **PS-004**: Store default account for new contacts, favorites column count/order, default dialer consent flag, and widget refresh cadence via DataStore.
- **PS-005**: Provide privacy toggles (note locks reset, clear cached contacts, limited mode) and link to support/about.

### 3.9 Widgets & Shortcuts (WS)
- **WS-001**: Favorites AppWidget displays cached contacts with quick actions; updates automatically when favorites change or via manual refresh.
- **WS-002**: Support app shortcuts (favorites, contact detail, recents, dialer, notes) that deep-link into the Compose navigation stack.
- **WS-003**: Handle widget/shortcut-deep links securely, even when the app is locked or limited mode is enabled.

### 3.10 Cloud, Sync & Backup (CB)
- **CB-001**: Keep startup offline; no remote account, App Check or Firebase runtime.
- **CB-002**: Keep photos and exports local; cloud backup is disabled. Preserve Android-authorized local device transfer rules.
- **CB-003**: Provide manual JSON exports/imports for contacts and passwords plus zipped HTML/JSON bundles for notes.
- **CB-004**: Schedule `ContactSyncWorker` to refresh cached contacts every six hours (plus manual triggers) when permissions allow.
- **CB-005**: Persist settings via DataStore and maintain BootReceiver hooks to reschedule reminders and widgets after device restarts.

### 3.11 Expense Manager (EM)
- **EM-001**: Capture expenses and incomes with categories, tags, payment method, currency, and optional note/attachment metadata; persist in Room for offline mode.
- **EM-002**: Provide analytics (category share pie chart, trend lines, average spend) with adjustable time ranges and shareable summaries.
- **EM-003**: Support budgets per category or global totals with threshold alerts and progress bars.
- **EM-004**: Respect sensitive sections lock—optionally require biometric/PIN before opening and after backgrounding.
- **EM-005**: Offer CSV/JSON export and import with validation, plus DataStore toggles for default currency and rounding preferences.

### 3.12 Wellness & Cycle Tracking (WC)
- **WC-001**: Menstrual Calendar stores cycle length, luteal phase, and first-day entries locally with adjustable presets.
- **WC-002**: Visualize past/present/future cycles via timeline + calendar grid, highlight fertile windows, ovulation estimate, and next period prediction.
- **WC-003**: Surface contextual tips per phase along with optional reminders for medication, hydration, or rest days.
- **WC-004**: Allow manual edits and reset while keeping all data offline; export anonymized summary on demand.
- **WC-005**: Ensure accessibility (colorblind-friendly palettes, TalkBack descriptions) and privacy (no sync, optional locking under sensitive sections).

---

### 4.2 Architecture & Modules
- Single `app/` module organized by domain (`data`, `service`, `security`, `settings`, `ui`, `utils`, `widget`).
- MVVM layers with `ViewModel`s (manual DI via `AndroidViewModel` + companion factory pattern, no Hilt) exposing `StateFlow`, Compose screens collecting state with `collectAsStateWithLifecycle()`, and repositories mapping to system APIs (Contacts Provider, CalendarContract, WorkManager and bounded Android adapters).
- Domain data classes annotated with `@Immutable` for Compose compiler stability guarantees; semantic color tokens defined in `ui/theme/ContactsAppTheme.kt`.
- Navigation managed by `NavigationViewModel` with consolidated `NavigationUiState` StateFlow; main tab pager (Favorites, Contacts, History, Extras) plus `SecondaryScreen` enum for detail flows.
- Room + WorkManager for offline caches; DataStore for global settings; DataStore for game statistics and settings; existing SharedPreferences integrations remain limited.
- Service layer hosts call services (`FunPhoneInCallService`, `IncomingCallService`, `OngoingCallService`) and helpers (CallController, MissedCallSuppressor).
- `ContactsViewModel` implements `onCleared()` for actor channel cleanup and photo cache eviction.

### 4.3 Key Libraries & Integrations
- AndroidX Contacts, CameraX 1.5.0, ML Kit barcode + text recognition, ZXing 3.5.3.
- No Firebase, remote authentication, analytics, advertising or crash-reporting SDKs.
- Compose BOM 2025.04.00, Lifecycle Runtime Compose 2.8.7 (for `collectAsStateWithLifecycle`).
- Coil (base + SVG), Konfetti Compose, lucide icon set, RichEditor Compose, uCrop.
- Gson, Guava, AndroidX WorkManager and AlarmManager; HTTPS holiday lookup and a user-requested social/web preview are the only direct network boundaries.
- Android SpeechRecognizer/TextToSpeech, Tink 1.18.0, AndroidX Security Crypto & Biometric APIs.
- Room (KSP), Coroutines, and DataStore.

### 4.4 Performance & Reliability Targets
- **PR-001**: Cold start < 2.0 s on Pixel 5 (API 33); warm start < 1.0 s.
- **PR-002**: Contact list scroll >60 fps with 10k contacts and cached thumbnails.
- **PR-003**: Voice utilities deliver first partial transcript <400 ms and maintain <150 ms TTS latency after initialization.
- **PR-004**: Games render ≥55 fps on mid-tier devices; paint canvas responds within 16 ms per stroke.
- **PR-005**: WorkManager contact sync completes within 30 s and retries gracefully when offline; reminders fire within ±10 s of scheduled time.

### 4.5 Compatibility & Accessibility
- Responsive Compose layouts for phones, tablets, foldables, and landscape orientation.
- Light/dark themes, dynamic color support, and high-contrast accent palette.
- WCAG 2.1 AA adherence: TalkBack labels, focus order, scalable fonts, haptic affordances.
- RTL mirroring on supported locales; tests for bi-directional text in notes and QR surfaces.

### 4.6 Observability & Tooling
- Private local diagnostic event codes; never log personal content or credentials.
- No remote telemetry. Quality evidence comes from local tests, sanitized benchmarks and manual device verification.
- Debug logging guarded by `BuildConfig.DEBUG`; sensitive data never logged in release.
- Core Gradle tasks: `assembleDebug`, `getApk`, `lint`, `testDebugUnitTest`, `connectedDebugAndroidTest`, `preparePlayStoreArtifacts`.
- Guarded release tasks validate keystore presence and env vars before producing artifacts.

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
- Localization maintained for es/en (strings, fonts, pluralization) with fallback to English.

### 5.3 Reliability & Quality
- Maintain ≥99.5% crash-free sessions; ANR rate <0.1%.
- Keep provider synchronization and durable reminders retryable; offer explicit retry/external-browser recovery for network previews.
- Reminder scheduling verifies AlarmManager capabilities (exact alarm permission on API 31+).
- Room migrations covered by exported schemas and regression tests; DataStore migrations validated during startup.

### 5.4 Scalability & Performance
- Support at least 15k contacts, 5k notes, hundreds of passwords without noticeable lag.
- Efficient lazy lists/grids and caching for favorites widget updates.
- Manage storage footprint with cleanup jobs (recordings, QR temp files, paint exports).
- WorkManager jobs respect battery/charging constraints for heavy tasks (contact sync, backups).

---

## 6. User Interface Requirements

### 6.1 Design Principles
- Material 3 foundation with customizable accent bar styles and wallpapers.
- Consistent card-based grouping in Extras, settings, and secondary sheets.
- Motion tuned via user-selected animation speed/transition; provide reduced motion mode.
- Adaptive layouts with responsive typography scales.

### 6.2 Key Screens & States
- **Main Tabs**: Bottom nav + pager for Favorites, Contacts, History, Extras.
- **Favorites Grid**: Configurable columns, reorder drag handles, widget sync indicator.
- **Contact List & Detail**: Alphabet scrollbar, action chips (call, message, QR, share, block), photo controls via Coil/uCrop.
- **Dialer & Call Flow**: Compose incoming/in-call UI, keypad, banners, ongoing notification handling, phone status view.
- **Notes Hub**: List with lock badges, note editor, checklist checklist, recording modal, reminder sheet.
- **Password Vault**: Search bar, list, create/edit sheet, generator, masked toggles, copy/share feedback.
- **Voice Utilities**: Dictation waveform, transcripts, voice picker dropdown, clipboard/share actions.
- **QR Tools**: Scanner overlay, gallery import button, parsed result cards, generator preview.
- **Public Holidays**: Country selector, yearly timeline, month pager, account selection dialog, error/empty placeholders.
- **Games & Paint**: Grid of games, leaderboard sheet, in-game overlays, paint toolbar and options dialogs.
- **Settings & About**: Accordions for customization, security, privacy, support content.

### 6.3 Navigation Structure
- Bottom navigation for main tabs; HorizontalPager ensures smooth swipes with state retention.
- Secondary navigation stack via `NavigationViewModel` for settings, notes, games, QR, voice, holidays, paint, passwords, dialer, onboarding, about.
- Deep link handling for app shortcuts, widget taps, tel: intents, and share actions.
- Back handler prioritizes secondary stack dismissal before exiting app; limited mode prompts before closing.

---

## 7. Integration Requirements

### 7.1 System Integrations
- Android Contacts Provider for CRUD, account retrieval, blocked numbers, vCard sharing.
- Telecom APIs (`FunPhoneInCallService`, `CallController`) for answering, rejecting, audio routing, and notifications.
- NotificationManager + AlarmManager for reminders and ongoing call alerts.
- MediaStore/FileProvider for paint exports, QR saves, audio recordings, and JSON backups.
- CalendarContract for event overlays in Holidays screen.
- WorkManager + BootReceiver for contact sync scheduling.
- ShortcutManager/AppWidgetManager for shortcuts and favorites widget.

### 7.2 Third-Party & Cloud Services
- No developer cloud, Firebase, remote accounts or telemetry.
- Nager.Date HTTPS API for authorized public holidays; HTTPS social/web sites only inside the requested preview.
- CameraX + ML Kit barcode/text recognition and ZXing for encoding.
- Android Speech Services (SpeechRecognizer + TextToSpeech).
- Coil for image loading/caching, uCrop for photo editing.

---

## 8. Data Requirements

### 8.1 Data Model
- **Contact** (`Contact`, `ContactPhone`, `ContactAccount`): runtime models mirrored by cached entities.
- **CachedContactEntity**: Room entity storing offline copies plus profile flag/timestamps.
- **RecentOperation**: Room history entries for call/operation timeline.
- **Note/ChecklistItem/ReminderData/RecordingData**: Room entities for notes domain.
- **PasswordEntity**: Encrypted credentials, timestamps, optional notes.
- **PublicHoliday**: local API model for holiday metadata.
- **CalendarEvent/CalendarAttendee**: Data classes for CalendarContract projections used to display events and attendees.
- **GameStats**: SharedPreferences entries keyed per game.
- **SettingsStore**: DataStore keys for theme, accent, transitions, vibration, favorites, password options.

### 8.2 Data Storage & Sync
- Room (`AppDatabase` v31) with exported schemas under `core/database/schemas/` and auto-migrations.
- DataStore Preferences (`AppSettingsViewModel`) for global settings; DataStore for game statistics and settings; existing SharedPreferences integrations remain limited.
- Private local photo files and user-selected exports; no remote storage paths.
- JSON/ZIP exports saved via FileProvider; `NotesBackupUtils` handles serialization/deserialization.
- WorkManager (`ContactSyncWorker`) plus AlarmManager for reminders ensures background updates.
- All structured app data resides on-device unless explicitly exported through Android system sharing.

### 8.3 Data Governance
- Form validation for contacts, notes, passwords with realtime error messaging.
- User-initiated export/import flows require explicit confirmation and clean up temp files afterward.
- Provide limited-mode flows that avoid touching contact storage when permissions absent.
- Respect delete/clear-cache actions (recent operations, cached contacts, encrypted tables).
- Document data handling for GDPR-style requests (local deletion and revoking platform permissions).

---

## 9. Testing Requirements

### 9.1 Testing Strategy
- Unit tests for repositories and utilities: `ContactRepository`, `ContactCacheRepository`, `RecentOperationsRepository`, `PasswordRepository`, `HolidaysRepository`, `SpeechUtils`, `CallController`.
- Room DAO tests validating migrations and CRUD for notes, passwords, cached contacts.
- Compose UI tests for Favorites tab, Extras hub navigation, notes editor locking, password vault flows, voice utilities, and QR scanner states (with fake camera feed).
- Instrumented Espresso/UIAutomator tests for QR scanning, password unlock, reminder scheduling, widget interactions.
- Integration tests with fake holiday sources for holidays and simulated WorkManager contact sync.
- Performance/profiling checks for contact list scrolling, speech latency, and paint strokes.

### 9.2 Coverage Targets & Tooling
- Target ≥70% line coverage for core modules (`contacts`, `notes`, `password`, `service`, `utils`, `widget`) with emphasis on repositories and business logic.
- Snapshot/golden tests for key Compose screens (Favorites grid, Extras list, Phone Status) to guard against regressions.
- CI executes `./gradlew lint testDebugUnitTest`; `connectedDebugAndroidTest` runs on demand or before release candidate sign-off.
- Testing checklist includes localization (es/en), RTL mirroring, TalkBack, offline/limited mode, and permission denial handling.

---

## 10. Deployment & Release Management

### 10.1 Build & Packaging
- Android Gradle Plugin 8.13.0 with Kotlin Gradle plugin 2.1.21; Compose compiler driven by BOM 2025.04.00; JVM target 21.
- Build variants: `debug` (dev suffix, non-minified) and `release` (minify + shrink resources) and isolated, non-publishable `benchmark`.
- Release signing credentials supplied via env vars or `local.properties`; guard tasks block builds when placeholders detected.
- Gradle helpers: `getApk`, `preparePlayStoreArtifacts`, `packageReleaseNativeSymbols` for Play Console uploads.
- Proguard/R8 rules maintained for Compose, ML Kit and legacy Tink compatibility.

### 10.2 Release Strategy
- Internal QA (dogfood) → closed beta → staged rollout on Play Console.
- Owner reviews Play-delivered smoke tests and Android Vitals; preserve a scoped hotfix path for call/permission regressions.
- Feature highlights (extras hub, games, vault) updated in Play listing; changelog pulled from README.
- Post-release instrumentation monitors dialer adoption, widget usage, voice tool engagement; support triage loop handles feedback.

---

## 11. Success Metrics

### 11.1 Product KPIs
- Monthly Active Users (MAU) and 30-day retention, especially after onboarding/limited mode.
- Average daily session length (+15% vs v3.7 baseline).
- Feature adoption: % of users creating notes, unlocking vault, using voice utilities, launching games, scanning QR codes.
- Conversion rate for default dialer consent and favorites widget installs.
- Store rating (target ≥4.6) and qualitative feedback sentiment.

### 11.2 Technical KPIs
- Crash-free sessions ≥99.5%; ANR rate <0.1%.
- ContactSyncWorker success ≥98% (no unhandled failures) and reminder schedule success ≥99%.
- Voice utility latency <500 ms for first transcript and <150 ms for successive TTS segments.
- Widget/shortcut update success (no stale entries) ≥99%.
- Accessibility issue resolution within one release cycle.

---

## 12. Future Enhancements

### 12.1 Planned Features & Experiments
- Collaborative notes/reminders with selective sharing.
- User-authorized local device transfer; no cloud synchronization is approved.
- AI-assisted contact deduplication, enrichment, and smart suggestions.
- Voice command support for key actions (call, send note, launch QR).
- Downloadable theme or game packs and configurable widget layouts.
- Holiday-aware reminders and context-aware call availability insights.

### 12.2 Technical Roadmap
- Adopt Baseline Profiles + Macrobenchmark tests for faster startup and scroll performance.
- Expand offline-first diffing for contacts/notes to prep for future sync.
- Harden vault security (Class 3 biometrics, hardware-backed keys, integrity monitoring).
- Incrementally modularize features for potential Kotlin Multiplatform reuse.
- Automate schema, privacy-boundary, localization and release metadata checks in CI.

---

## Appendices

### A. Permissions
- READ_CONTACTS, WRITE_CONTACTS, GET_ACCOUNTS
- CALL_PHONE, ANSWER_PHONE_CALLS, MANAGE_OWN_CALLS, READ_CALL_LOG, WRITE_CALL_LOG
- READ_PHONE_STATE, POST_NOTIFICATIONS, FOREGROUND_SERVICE, FOREGROUND_SERVICE_PHONE_CALL
- POINT-IN-TIME perms: RECORD_AUDIO, CAMERA, READ_CALENDAR, SCHEDULE_EXACT_ALARM, RECEIVE_BOOT_COMPLETED
- INTERNET, ACCESS_NETWORK_STATE, VIBRATE, WAKE_LOCK, DISABLE_KEYGUARD, SYSTEM_ALERT_WINDOW
- READ_EXTERNAL_STORAGE (≤API 32) for QR/image imports; FileProvider manages scoped storage.
- USE_FULL_SCREEN_INTENT for incoming call UI, com.google.android.gms.permission.AD_ID for analytics attribution.

### B. Third-Party Libraries & Services
- Jetpack Compose, Material 3, Navigation Compose.
- Room, DataStore, WorkManager, AlarmManager helpers, Kotlin Coroutines.
- Play-only native In-App Review; no Firebase, analytics, advertisements or crash-reporting SDKs.
- CameraX, ML Kit barcode & text recognition, ZXing.
- Bounded HTTPS platform adapters, Gson and Guava.
- Coil (base + SVG), uCrop, Contacts-Android, Konfetti, Lucide icons, RichEditor Compose.
- Tink, AndroidX Security Crypto, Biometric API.
- Nager.Date public holiday API, Android Speech Services.

---

*This PRD reflects Fun Phone v3.18.5 (build 249) and consolidates the current functional, technical, and experience requirements across the core dialer/contacts product and optional extensions.*
