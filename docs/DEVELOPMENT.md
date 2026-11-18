# Development

Everything you need to build, test and ship Fun Phone. For the contribution process, see
[CONTRIBUTING.md](../CONTRIBUTING.md).

## Requirements

| Tool | Version |
|------|---------|
| JDK | 25 |
| Android SDK | API 37 (`compileSdk` and `targetSdk`) |
| Android Studio | A recent stable release that supports AGP 9.3 |
| Device or emulator | Android 10 (API 29) or newer |
| Gradle | Use the included wrapper |

Release signing is optional for development and described in [SETUP.md](../SETUP.md).

## Architecture

- **Modules:** `:app` (the application, package `com.bookverse.contacts`), the Android libraries
  `:core:designsystem` (theme and generic Compose components) and `:core:database` (Room), and
  `:baselineprofile` (Baseline Profile generator and macrobenchmarks). Shared Gradle configuration
  lives in the convention plugins under `build-logic/`.
- **UI:** 100% Jetpack Compose and Material 3. No XML layouts or fragments.
- **Pattern:** MVVM with repositories and manual dependency injection. ViewModels expose `StateFlow`,
  collected with `collectAsStateWithLifecycle()`.
- **Data:** Room (KSP, exported schemas in `core/database/schemas/`) and DataStore for settings.
- **Async:** Kotlin coroutines and Flow, WorkManager and AlarmManager for reminders.
- **Telephony:** `InCallService` based default dialer with foreground call services.
- **Security:** Google Tink with the Android Keystore, AndroidX Biometric with PIN fallback.
- **Images and widgets:** Coil, and Glance for the notes widget.

### Distribution flavors

| Flavor | Package | Differences |
|--------|---------|-------------|
| `community` | `com.bookverse.contacts.community` | No Google libraries. QR scanning with ZXing. Published on GitHub Releases; F-Droid listing planned. |
| `play` | `com.bookverse.contacts` | QR scanning with ML Kit and Google Play In-App Review. Published on Google Play. |

Neither flavor contains Firebase, ads or analytics. `scripts/verify-zero-collection.sh` enforces it.

<details>
<summary>Source layout</summary>

```
app/src/
├── main/java/com/bookverse/contacts/
│   ├── data/          # Room database, DAOs, entities and repositories
│   ├── domain/        # Use cases and domain models
│   ├── integration/   # App container and flavor seams (QR decoder, review prompt)
│   ├── security/      # CryptoManager, app lock and the vault autofill service
│   ├── service/       # In-call, incoming and ongoing call services
│   ├── settings/      # App settings, fonts, locales
│   ├── ui/            # screens, components, navigation, theme, viewmodels
│   ├── utils/         # ContactUtils, NoteShareUtils, NotesBackupUtils, ...
│   └── widget/        # Notes Glance widget
├── community/         # community-only implementations
├── play/              # play-only implementations
└── test/, testCommunity/, androidTest/
```

</details>

## Common tasks

| Command | Purpose |
|---------|---------|
| `./gradlew assembleCommunityDebug` | Build the community debug APK. |
| `./gradlew assemblePlayDebug` | Build the Play debug APK. |
| `./gradlew testCommunityDebugUnitTest` | Run JVM unit tests. |
| `./gradlew connectedCommunityDebugAndroidTest` | Run instrumented tests on a connected device. |
| `./gradlew ktlintCheck` / `ktlintFormat` | Check or fix Kotlin style. |
| `./gradlew lintCommunityDebug lintPlayRelease` | Android lint for both flavors. |
| `./scripts/verify-lint-ratchet.sh communityDebug playRelease` | Zero lint errors and a warning ceiling. |
| `./scripts/verify-localization.sh` | All five locales match the English baseline. |
| `./scripts/verify-zero-collection.sh` | Only the approved network paths; no Firebase, analytics or `AD_ID`. |
| `./scripts/verify-telecom-contract.sh` | Protects the default dialer and `InCallService` manifest contract. |
| `./scripts/verify-feature-boundaries.sh` | Prevents direct dependencies between isolated features. |
| `./scripts/verify-quality-contract.sh` | Protects quality matrices, critical tests and file size limits. |
| `./scripts/verify-android-only.sh` | Rejects Kotlin Multiplatform or iOS targets. |
| `./scripts/smoke-emulator-incoming-call.sh <emulator>` | Simulates an incoming call on an emulator. |
| `python3 scripts/export-play-listing.py` | Exports the Play listing and the fastlane metadata from `PLAY_STORE_LISTING.md`. |
| `./scripts/release.sh vX.Y.Z` | Builds and verifies the Play AAB and community APK from a clean checkout of a tag. |

## Conventions

- Kotlin official style: 4 spaces and trailing commas in multiline constructs.
- No comments in code unless they are needed to explain something non-obvious.
- Every user-visible string goes into the five locales (`values`, `values-es`, `values-de`,
  `values-fr`, `values-pt`). Never hardcode text in Kotlin.
- LazyList `items()` use stable keys. Data classes passed to composables are `@Immutable`.
- Use semantic color tokens from the theme instead of hardcoded colors.
- After changing Room entities, build once to export the updated schema.

More detail lives in [AGENTS.md](../AGENTS.md), [DESIGN.md](../DESIGN.md) and
[CHEATSHEET.md](../CHEATSHEET.md).

## CI

| Workflow | Trigger | What it does |
|----------|---------|--------------|
| [`pr-checks.yml`](../.github/workflows/pr-checks.yml) (`validate`) | Pull requests to `main` | Guards, ktlint, build, unit tests, lint and the lint ratchet. |
| [`secrets.yml`](../.github/workflows/secrets.yml) (`gitleaks`) | Pushes and pull requests to `main` | Scans for leaked secrets. |
| [`android.yml`](../.github/workflows/android.yml) | Pushes to `main` | Full community build, tests, lint and release metadata checks. |

`main` is protected: changes land through pull requests, `validate` and `gitleaks` must pass, and
pull requests are squash-merged.

## Releases

Every version ships to Google Play (AAB) and GitHub Releases (signed, reproducible community APK),
and F-Droid builds it from the tag. Follow [RELEASING.md](../RELEASING.md).

## Project documents

| Document | Contents |
|----------|----------|
| [SETUP.md](../SETUP.md) | SDK, signing and build setup. |
| [RELEASING.md](../RELEASING.md) | Release process for Google Play, GitHub Releases and F-Droid. |
| [DESIGN.md](../DESIGN.md) | UI and UX design system. |
| [AGENTS.md](../AGENTS.md) | Working agreement for coding agents. |
| [PRD.md](../PRD.md) | Product requirements. |
| [QUALITY_RELEASE_GATES.md](../QUALITY_RELEASE_GATES.md) | UI state, testing, accessibility and performance gates. |
| [TELECOM_REGRESSION_MATRIX.md](../TELECOM_REGRESSION_MATRIX.md) | Default dialer regression matrix. |
| [FUN_PHONE_PROGRESSIVE_RELEASE_SPEC.md](../FUN_PHONE_PROGRESSIVE_RELEASE_SPEC.md) | Progressive release tracking. |
| [PRIVACY.md](../PRIVACY.md), [PRIVACY_DATA_FLOW.md](../PRIVACY_DATA_FLOW.md), [TELEMETRY.md](../TELEMETRY.md) | Privacy policy, data flows and the zero-telemetry policy. |
| [PLAY_STORE_LISTING.md](../PLAY_STORE_LISTING.md), [PLAY_DATA_SAFETY.md](../PLAY_DATA_SAFETY.md) | Play listing source and Data Safety worksheet. |
| [CHANGELOG.md](../CHANGELOG.md) | Version history. |
