# Fun Phone Development Instructions

## Project Overview
Fun Phone v3.18.0 is an Android-first personal hub that unifies a full-featured default dialer,
offline-ready contact management, Room-backed productivity surfaces, a Tink-encrypted password vault,
QR and speech utilities, and a curated catalog of Compose mini games.

- **Package**: `com.bookverse.contacts`
- **Min SDK**: 29 (Android 10)
- **Target/Compile SDK**: 37
- **Architecture**: MVVM with repository/data/use-case separation
- **UI**: 100% Jetpack Compose + Material 3
- **Languages**: English baseline with Spanish, German, French, and Portuguese translations

## Code Style Requirements

### CRITICAL: No Comments in Code
**IMPORTANTE**: No agregues comentarios en el código generado a menos que el usuario lo solicite explícitamente.
NO REALIZAR COMMIT AUTOMÁTICO!!!

### Kotlin Style Guide
- Use 4-space indentation
- Prefer `val` over `var`
- Use trailing commas in multiline constructs
- Composable functions use PascalCase
- Keep related classes in their feature package

### Compose Best Practices
- Only use Jetpack Compose (no XML layouts or classic fragments)
- Reuse existing components from `ui/components` and themes from `ui/theme`
- Use reactive flows with coroutines + Flow/StateFlow
- Avoid blocking APIs on main thread
- Hoist state into ViewModels when practical

## Architecture Patterns

### ViewModels
- Located in `ui/viewmodel` package
- Must expose immutable `StateFlow` for UI state
- Collect state in Compose using `collectAsStateWithLifecycle()`
- Use `viewModelScope` for coroutine operations
- Handle business logic and coordinate with repositories

Example:
```kotlin
class MyViewModel(
    private val repository: MyRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyUiState())
    val uiState: StateFlow<MyUiState> = _uiState.asStateFlow()

    fun performAction() {
        viewModelScope.launch {
            repository.doSomething()
        }
    }
}
```

### Repository Pattern
- Heavy operations (IO, network, encryption) must use `Dispatchers.IO`
- Repositories abstract data sources and provide Flow-based reactive streams
- Reuse existing repositories before creating new ones
- Follow existing patterns in `data/` package

### Navigation
- Managed through destinations in `ui/navigation`
- Register new screens in `SecondaryScreen`/`MainTabsScaffold`
- Handle deep links for app shortcuts and widget taps
- Back handler prioritizes secondary stack before exiting

### Data Persistence
- Room Database (schema version 29 with retained legacy migrations)
- Update entities + DAO + `AppDatabase` for new tables
- Sync schema JSONs in `core/database/schemas/` after migrations
- DataStore for app settings via `AppSettingsViewModel`
- SharedPreferences only for game stats (legacy)

## Key Features & Components

### Main Features
- **Contacts & Dialer**: CRUD operations, favorites, call history, blocking
- **Notes & Reminders**: Rich text editor, checklists, audio recordings, reminders via AlarmManager
- **Password Vault**: Tink encryption, biometric/PIN auth, password generator
- **Voice Utilities**: Speech-to-text, TTS with voice picker
- **QR Tools**: Scanner with CameraX + ML Kit, generator with ZXing
- **Public Holidays**: Bounded HTTPS request to Nager.Date API
- **Social/Web Preview**: User-requested preview through the approved WebView exception
- **Games Suite**: 10+ Compose mini games with stats tracking
- **Paint Studio**: Multi-brush drawing, image import, share intents
- **Expense Manager**: Track expenses with categories, budgets, analytics
- **Menstrual Calendar**: Cycle tracking with predictions and tips

### Security & Authentication
- Tink + Android Keystore encryption for sensitive data
- No Firebase, analytics, advertising, cloud backup, or telemetry services
- Biometric/PIN gates for vault and locked notes (`PasswordAuthState`)
- Respect `AppSettingsViewModel.passwordManagerEnabled` toggle

### Permissions & System Integration
- Android Contacts Provider for CRUD operations
- Telecom APIs for call management (`FunPhoneInCallService`, `CallController`)
- WorkManager for contact sync (6-hour intervals)
- NotificationManager + AlarmManager for reminders
- CalendarContract for calendar integration

## Localization
- All new strings must go to `app/src/main/res/values/strings.xml`
- Provide translations in `values-en/strings.xml`
- Never hardcode text in Kotlin except for temporary debugging
- Use localized formatters for dates, numbers, and currencies

## Performance & Quality

### Performance Targets
- Cold start < 2.0s, warm start < 1.0s
- Contact list scroll >60 fps with 10k contacts
- Voice utilities first transcript <400ms
- Games render ≥55fps on mid-tier devices

### Testing
- Unit tests for repositories and utilities
- Compose UI tests for key user flows
- Target ≥70% line coverage for core modules
- Run `./gradlew lint testDebugUnitTest` before committing

### Error Handling
- Handle offline/network errors gracefully
- Show localized error messages via Snackbar/Dialog
- Respect limited mode when permissions are denied
- Never log sensitive data in release builds

## Privacy Architecture
- Contacts, notes, passwords, and backups remain on the device.
- The optional holidays tool and user-requested social/web preview are the only approved network paths.
- Do not add Firebase, analytics, advertising, cloud backup, or telemetry.

## Common Utilities (Reuse These)
- `ContactUtils` - Contact operations and formatting
- `NoteShareUtils` - Note export/import helpers
- `PermissionUtils` - Permission request flows
- `SpeechUtils` - Voice recognition and TTS
- `VibrationUtils` - Haptic feedback patterns
- `NotesBackupUtils` - Backup serialization
- `CryptoManager` - Tink encryption wrapper

## Response Guidelines
1. Respond in the same language as the user's request
2. Return only requested code unless explanation is needed
3. Ensure code is self-contained with correct imports
4. Ask minimal clarifications if request is ambiguous
5. For multi-step tasks (ViewModel + UI + Repository), deliver all coordinated changes

## When in Doubt
- Consult `AGENTS.md`, `PRD.md`, and `README.md` for scope and requirements
- Prefer improving shared components over duplicating them
- Document new utilities briefly outside of code
- Respect release guard rails for Play Store builds
- Follow existing patterns in similar features

## Build & Deployment
- Android Gradle Plugin 9.3.1 with Kotlin 2.3.21
- Compose BOM 2026.05.01
- Room 2.8.4 with KSP 2.3.9
- Tink 1.23.0 and AndroidX Security Crypto compatibility
- Release signing via env vars or `local.properties`
- Key Gradle tasks: `assembleCommunityDebug`, `lintCommunityDebug`, `testCommunityDebugUnitTest`,
  `preparePlayStoreArtifacts`

Follow these guidelines to deliver production-ready solutions aligned with Fun Phone's architecture and best practices.
