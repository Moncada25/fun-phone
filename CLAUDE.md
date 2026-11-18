# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Fun Phone is an Android app (package `com.bookverse.contacts`) — a personal hub combining a default dialer, contact management, notes with rich text/reminders, encrypted password vault, QR tools, voice utilities, expense tracker, and 12+ Compose mini games.

- **Min SDK 29** | **Target/Compile SDK 37** | **JVM Target 25**
- **UI**: 100% Jetpack Compose + Material 3 — no XML layouts or fragments
- **Architecture**: MVVM + Repository, manual DI (no Hilt)
- **Modules**: `:app`, the Android libraries `:core:designsystem` (theme and generic Compose components),
  `:core:database` (Room), `:core:datastore` (the single app settings DataStore), `:core:security` (`CryptoManager`, PIN and
  app lock), `:core:ui` (shared strings, drawables, fonts, `AppSettingsViewModel`, shared
  components and the protected-action auth UI) and `:core:common`
  (diagnostics, `SecondaryScreen` and generic utilities), plus the `:baselineprofile` test module (Baseline Profile generator and
  macrobenchmarks). Feature modules
  (`:feature:contacts`, `:feature:dialer`, `:feature:games`, `:feature:expenses`, `:feature:notes`, `:feature:qr`, `:feature:vault`,
  `:feature:tools`) hold a feature's screens, ViewModels, domain and data, and depend on `:core:ui`;
  `:app` keeps navigation, the `AppContainer` composition root, the in-call screens and telecom
  services, settings, widgets and the manifest. `:feature:dialer` holds the keypad and call history. Shared Gradle setup lives in the `build-logic` convention plugins
  (`funphone.android.library`, `funphone.android.library.compose`, `funphone.android.room`)
- **Localization**: English baseline in `:core:ui` `values/`, with Spanish, German, French, and Portuguese
  translations in `values-es/`, `values-de/`, `values-fr/`, and `values-pt/`

## Build Commands

```bash
./gradlew assembleDebug                # Debug APK
./gradlew assembleRelease              # Release APK (requires signing credentials)
./gradlew testDebugUnitTest            # Unit tests
./gradlew testDebugUnitTest --tests "com.bookverse.contacts.SomeTest"  # Single test
./gradlew connectedDebugAndroidTest    # Instrumented tests
./gradlew ktlintCheck                  # Kotlin style check
./gradlew ktlintFormat                 # Auto-format
./gradlew lintCommunityDebug           # Android lint (zero errors)
./scripts/verify-lint-ratchet.sh       # Zero errors and warning ceiling
./gradlew installDebug                 # Install on device
```

## Architecture

**Data flow**: Compose UI → ViewModel (StateFlow) → Repository → Room or local/platform APIs

### Key Packages (`app/src/main/java/com/bookverse/contacts/`)

- **`ui/viewmodel/`** — ViewModels extend `AndroidViewModel`, expose `StateFlow` via `MutableStateFlow` + `.asStateFlow()`. Use `viewModelScope` for coroutines. Manual factories via companion `provideFactory()`. Collect in Compose with `collectAsStateWithLifecycle()`.
- **`ui/screens/`** — Feature screens organized by domain (contacts, calls, notes, games, qr, passwords, expenses, tools, settings)
- **`ui/components/`** — reusable Compose components. Generic ones that need no strings live in
  `:core:designsystem`; shared ones that use strings or settings live in `:core:ui`; components
  owned by a single feature stay with that feature
- **`ui/navigation/`** — `SecondaryScreen` enum + `SecondaryScreenContent` composable for routing. `NavigationViewModel` manages navigation state via `NavigationUiState` StateFlow.
- **`ui/theme/`** (`:core:designsystem`) — `ContactsAppTheme`, `ThemeMode`, `DarkModeStyle`, `AccentBarStyle` with light/dark schemes and semantic color tokens (`CallAcceptColor`, `CallRejectColor`, `BadgeColor`, `BudgetOkColor`, etc.)
- **`data/db/`** (`:core:database`) — Room `AppDatabase` with auto-migrations, DAOs, converters and the
  entities of every feature. Schemas exported to `core/database/schemas/`
- **`data/notes/`, `data/expenses/`, `data/password/`, `data/qr/`** — Feature-specific Room entities and domain models annotated with `@Immutable` for Compose stability
- **`data/repository/`** — Repository implementations for contacts and the bounded HTTPS holidays source.
- **`service/`** — Android services: `FunPhoneInCallService`, `IncomingCallService`, `OngoingCallService`, `NotificationService`, `CallNotificationManager`
- **`security/`** (`:core:security`) — `CryptoManager` (Tink + Android Keystore), PIN and app lock; the
  vault autofill service stays in `:app`
- **`utils/`** — `ContactUtils`, `PermissionUtils`, `SpeechUtils`, `VibrationUtils`, `NoteShareUtils`, `NotesBackupUtils`

### Data Persistence
- **Room** (v32, auto/manual migrations) — contacts cache, contacts recycle bin, notes, passwords, expenses, and QR history
- **DataStore** — app settings (theme, fonts, transitions) via `AppSettingsViewModel`; the single
  `app_settings_datastore` instance lives in `:core:datastore`
- **Network** — Optional Nager.Date holidays and user-requested social/web previews are the only
  direct network paths; no Firebase, analytics, advertising, or cloud backup.

## Critical Code Style Rules

- **No comments in code** unless the user explicitly requests them
- **No XML layouts** — Compose only
- 4-space indentation, trailing commas in multiline constructs, prefer `val`
- Composable functions use PascalCase
- Heavy operations must use `Dispatchers.IO`
- Respond in the same language as the user's request

## Compose Patterns

- Use `collectAsStateWithLifecycle()` (not `collectAsState()`) for all StateFlow collection
- LazyList `items()` must include `key` parameter for stable identity
- Data classes used as Composable parameters should have `@Immutable` annotation
- Use semantic color tokens from `ui/theme/ContactsAppTheme.kt` in `:core:designsystem` — avoid hardcoded `Color(0xFF...)` values
- Avoid `runBlocking` and `coroutineScope.launch` inside Composables — use `LaunchedEffect` with appropriate keys
- `LaunchedEffect(Unit)` should only be used for genuinely one-time effects (animations, SharedFlow collectors). For effects that depend on a value, use that value as key.

## Localization

All new user-facing strings must be added to the following resources:
- `core/ui/src/main/res/values/strings.xml` (English baseline)
- `core/ui/src/main/res/values-es/strings.xml`, `values-de/strings.xml`, `values-fr/strings.xml`, and
  `values-pt/strings.xml` (supported translations)

Shared strings, drawables, avatars and fonts live in `:core:ui`; reference them through
`com.bookverse.contacts.core.ui.R`. `:app` keeps only manifest-owned resources (theme, launcher
icon, widget previews and `xml/` configs). Never hardcode text in Kotlin. Use localized formatters for dates, numbers, currencies.
Run `./scripts/verify-localization.sh` after changing localized resources.

## Room Database Changes

When modifying Room entities:
1. Update entity classes + DAO + `AppDatabase` in `:core:database`
2. Run `./gradlew assembleDebug` to export updated schemas to `core/database/schemas/`
3. Auto-migrations handle schema evolution

## Release Signing

Credentials resolved in order: environment variables → `keystore.properties` → `local.properties` → `gradle.properties`. Release tasks fail fast if signing is misconfigured.

## Key Dependencies

Compose BOM 2026.05.01, Kotlin 2.3.21, Room 2.8.4 (KSP), Tink 1.23.0, CameraX 1.6.1 + ML Kit.
Version catalog: `gradle/libs.versions.toml`. No Dependabot configuration is active; dependency
updates are reviewed manually.

## Reusable Utilities

Before creating new helpers, check: `ContactUtils`, `NoteShareUtils`, `NotesBackupUtils`, `PermissionUtils`, `SpeechUtils`, `VibrationUtils`, `CryptoManager`.
