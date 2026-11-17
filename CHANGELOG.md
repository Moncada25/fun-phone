# Changelog

All notable changes to Fun Phone will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Recent calls and Monthly expenses home screen widgets. Recent calls lists the latest calls with
  call-back buttons; Monthly expenses shows this month's spending, income and budget progress, and
  hides amounts when sensitive sections are locked.
- The password manager's Website field has a picker of common services (streaming, email and
  social, shopping and payments, gaming and developer). Picking one fills the address and, when
  empty, the account name. Known services also suggest the entry in their Android apps and related
  sign-in domains when autofilling; only exact matches fill without asking.

### Changed
- The favorites widget now uses Jetpack Glance like the other widgets: it follows Material You
  colors, light and dark mode, and adapts its columns to the widget size.
- All widgets use the system widget corner radius, show a localized name and preview in the widget
  picker, and refresh when their data changes and after the app restarts.
- Personalization options apply instantly again from their own sheets, without the preview and
  Apply step. Theme and accent changes crossfade; font size and list density keep their sliders.

### Fixed
- Widgets and background work failed in release builds because R8 removed the WorkManager input
  merger constructor.
- The notes widget did not refresh after notes changed, and its "+" button opened the notes list
  instead of a new note.
- Creating a new note from a shortcut while another note was open showed an empty editor.

## [3.20.7] - 2026-11-17

### Fixed
- Filling a saved login from the password manager into another app no longer crashes on Android 15+
  or in dark mode: `Theme.Contacts` now extends an AppCompat theme in every configuration.

### Changed
- The profile screen shows a fixed "Your profile" title instead of "Profile of <first name>".
- `RELEASING.md` documents the release process for Google Play, GitHub Releases and F-Droid, and
  `scripts/release.sh` builds and verifies both artifacts from a clean checkout of the release tag.

## [3.20.6] - 2026-11-17

### Added
- Fun Phone is open source under GPL-3.0.
- Signed `community` release APKs for GitHub Releases and Obtainium, using a dedicated
  `COMMUNITY_RELEASE_*` key. Without it the build stays unsigned for F-Droid.
- Fastlane metadata (`fastlane/metadata/android`) generated with the Play listing export.

### Changed
- Release APKs no longer embed the encrypted Google dependency metadata block (the Play bundle
  keeps it), as F-Droid's scanner requires.
- The `community` release reports a plain `versionName` (for example `3.20.6`).
- The `community` build scans QR codes with ZXing and no longer ships ML Kit, Google Play Services,
  Play Core or Firebase classes. The `play` build keeps ML Kit.

### Removed
- The unused ML Kit text recognition dependency.

## [3.20.5] - 2026-11-16

### Added
- Dialer: when the typed digits match exactly one contact, show it as a chip that calls it.
- Ship a Baseline Profile (startup and core journeys) with `profileinstaller`, plus a
  `:baselineprofile` generator and macrobenchmark module.

### Fixed
- History no longer crashes when a local recent and a system call-log entry share the same id;
  selection and deletion now target the right source.
- Contacts re-aggregated by a sync adapter (e.g. WhatsApp) keep working for edit, favorite and
  delete without restarting the app.
- Dialer: the country chip always shows the flag and calling code, opens the country picker and
  preselects the device country; short service numbers are dialed without the calling code.

### Changed
- Raise the minimum supported version to Android 10 (API 29) and remove the Android 9 fallbacks for
  default-dialer requests, call direction, emergency numbers, gallery saves and cursor tinting.
- Add Settings › Glass effects (Automatic/On/Off). Automatic gives full Modern glass to devices that
  declare media performance class 12+, blur without refraction to 6 GB+ RAM devices, and solid
  tonal bars below that.
- Give the Gradle and Kotlin compiler daemons 3 GB heaps so clean CI builds no longer run out of memory.
- Add social-network home previews, validated profile links, load/error/retry states and browser fallback.
- Let core lists and Settings scroll behind transparent protected status bars; strengthen neutral surface contrast.
- Lock protected sessions immediately on background/screen off; defer vault decryption until authentication.
- Migrate legacy PIN verifiers after successful unlock, with salted PBKDF2 and durable attempt limiting.
- Encrypt restorable password/protected-note drafts and mark password clipboard copies sensitive.
- Preserve encrypted checklist, reminder, recording and protected-expense drafts across re-auth;
  resume vault document selection only after authentication, without changing backup formats.
- Exit normally on Back without killing the process or its active call services.
- Align the companion site's privacy disclosures, accessibility, local fonts and dependency security.


## [3.20.2] - 2026-11-08

Local release candidate; Play publication and delivered-build validation remain pending.

### Added
- Modern appearance with a one-time migration that preserves existing preferences.
- Glass navigation and circular Modern action buttons, compact-first headers with reachable actions,
  persistent scrolling search bars, and draft appearance previews.
- Six accent seeds and local variable Inter/Manrope font weights.

### Changed
- Preserve tab/header positions and secondary navigation history, including backward transitions.
- Adapt shared controls to large text and retain solid backdrop fallbacks for protected/limited devices.
- Highlight the selected Modern dock icon and label with the chosen accent, keeping press feedback rounded.

### Fixed
- Prefill the dialer immediately from external telephone links, including cold starts and repeated links.
- Keep route and phone input in one navigation update; retain edits across configuration changes
  without persisting the number in saved-state bundles or placing a call automatically.
- Preserve international `+` prefixes while editing and avoid adding a second country code.

### Play Store notes

#### en-US

• Meet Modern: scrolling titles and search, glass navigation and a new floating action button.
• Personalize with six new colors, improved fonts and a preview before applying changes.
• Enjoy smoother navigation and improved large-text layouts.
• Fixed phone links: the number now appears immediately in the dialer.

#### es-419

• Estrenamos Moderno: títulos y búsqueda que acompañan el scroll, navegación de cristal y un nuevo botón flotante.
• Más personalización: seis colores nuevos, fuentes mejoradas y vista previa antes de aplicar.
• Navegación más fluida y mejor adaptación al tamaño del texto.
• Corregimos los enlaces de llamada: el número aparece directamente en el marcador.

#### de-DE

• Neuer Stil Modern: scrollende Titel und Suche, Glasnavigation und eine neue schwebende Aktionstaste.
• Mehr Personalisierung: sechs neue Farben, verbesserte Schriften und eine Vorschau vor dem Anwenden.
• Flüssigere Navigation und bessere Darstellung großer Texte.
• Telefonlinks korrigiert: Die Nummer erscheint jetzt sofort im Wählfeld.

#### fr-FR

• Découvrez Moderne : titres et recherche au défilement, navigation en verre et nouveau bouton flottant.
• Personnalisez avec six nouvelles couleurs, des polices améliorées et un aperçu avant application.
• Navigation plus fluide et meilleure adaptation aux grands textes.
• Liens téléphoniques corrigés : le numéro apparaît immédiatement dans le composeur.

#### pt-BR

• Conheça Moderno: títulos e busca que acompanham a rolagem, navegação de vidro e novo botão flutuante.
• Personalize com seis novas cores, fontes aprimoradas e prévia antes de aplicar.
• Navegação mais fluida e melhor adaptação a textos grandes.
• Links de chamada corrigidos: o número aparece imediatamente no discador.

## [3.18.6] - 2026-09-04

### Changed
- Refreshed the contact photo, avatar, and incoming-call background selectors to match the current
  Settings sheet design.
- Simplified Paint shape controls so shapes are resized directly with pinch-to-zoom.

### Fixed
- Prevented contact refreshes from deleting an incoming-call background while it is still being
  copied, so valid gallery and camera images can be applied reliably.

### Play Store notes

#### en-US
• Add birthdays to contact profiles and improve holiday and calendar browsing.
• Improve incoming calls, custom backgrounds, contact-photo loading, and update feedback.
• Refresh Settings, Expenses, Paint, Favorites, Pomodoro, Voice, and contact photo selectors.
• Resize Paint shapes directly with pinch-to-zoom through a simpler toolbar.
• Improve sheets, interactions, and local processing of private data.

#### es-419
• Añade cumpleaños a los perfiles y mejora la navegación de festivos y calendarios.
• Mejora las llamadas entrantes, los fondos personalizados, la carga de fotos y la confirmación al actualizar contactos.
• Renueva Ajustes, Gastos, Pintura, Favoritos, Pomodoro, Voz y los selectores de fotos y avatares.
• Ajusta las formas en Pintura con el gesto de pellizcar y una barra más simple.
• Mejora las hojas, las interacciones y el procesamiento local de datos privados.

#### de-DE
• Geburtstage in Kontaktprofilen und bessere Navigation durch Feiertage und Kalender.
• Verbesserte eingehende Anrufe, Anrufhintergründe, Fotoladevorgänge und Rückmeldungen bei Kontaktupdates.
• Überarbeitete Ansichten für Einstellungen, Ausgaben, Paint, Favoriten, Pomodoro, Sprache und Fotoauswahl.
• Formen in Paint lassen sich über eine einfachere Leiste direkt per Zwei-Finger-Zoom skalieren.
• Verbesserte Sheets, Interaktionen und lokale Verarbeitung privater Daten.

#### fr-FR
• Ajout des anniversaires aux profils et meilleure navigation dans les jours fériés et calendriers.
• Amélioration des appels entrants, arrière-plans, photos et confirmations de mise à jour des contacts.
• Nouvelles interfaces pour les réglages, dépenses, peinture, favoris, Pomodoro, voix et sélecteurs de photos.
• Redimensionnement des formes dans Peinture par pincement avec une barre simplifiée.
• Meilleures feuilles, interactions et traitement local des données privées.

#### pt-BR
• Adicione aniversários aos perfis e melhore a navegação por feriados e calendários.
• Melhore chamadas recebidas, fundos personalizados, fotos e confirmações ao atualizar contatos.
• Renove Configurações, Despesas, Paint, Favoritos, Pomodoro, Voz e os seletores de fotos.
• Redimensione formas no Paint com gesto de pinça e uma barra mais simples.
• Melhore folhas, interações e o processamento local de dados privados.

## [3.18.5] - 2026-09-03

### Added
- Dedicated checklist item editing with copy and completion controls.
- Month-based holiday and selected-calendar event browsing.
- Round or square card styles for Favorites.
- User-requested social and website previews through the approved HTTPS WebView path.

### Changed
- Refreshed Settings, contact editing, photo tools, Expenses, Pomodoro, and Voice screens.
- Added bounded image processing for imported contact photos and Paint content.
- Improved incoming-call background handling, contact-photo loading, and contact update feedback.
- Made holiday and calendar browsing read-only while adding birthday details to contact profiles.

### Play Store notes

#### en-US
• Add birthdays to contact profiles and improve holiday and calendar browsing.
• Improve incoming-call handling, contact-photo loading, and contact update feedback.
• Refresh Settings, Expenses, Paint, Favorites, Pomodoro, and Voice screens.
• Improve sheets, interactions, and local processing of private data.

#### es-419
• Añade cumpleaños a los perfiles y mejora la navegación de festivos y calendarios.
• Mejora las llamadas entrantes, la carga de fotos y la confirmación al actualizar contactos.
• Renueva Ajustes, Gastos, Pintura, Favoritos, Pomodoro y Voz.
• Mejora las hojas, las interacciones y el procesamiento local de datos privados.

#### de-DE
• Geburtstage in Kontaktprofilen und eine bessere Navigation durch Feiertage und Kalender.
• Verbesserte eingehende Anrufe, Fotoladevorgänge und Rückmeldungen beim Aktualisieren von Kontakten.
• Überarbeitete Ansichten für Einstellungen, Ausgaben, Paint, Favoriten, Pomodoro und Sprache.
• Verbesserte Sheets, Interaktionen und lokale Verarbeitung privater Daten.

#### fr-FR
• Ajout des anniversaires dans les profils et meilleure navigation dans les jours fériés et calendriers.
• Amélioration des appels entrants, du chargement des photos et du retour après mise à jour des contacts.
• Nouvelles interfaces pour les réglages, dépenses, peinture, favoris, Pomodoro et voix.
• Meilleures feuilles, interactions et traitement local des données privées.

#### pt-BR
• Adicione aniversários aos perfis e melhore a navegação por feriados e calendários.
• Melhore chamadas recebidas, carregamento de fotos e confirmação ao atualizar contatos.
• Renove Configurações, Despesas, Pintura, Favoritos, Pomodoro e Voz.
• Melhore folhas, interações e o processamento local de dados privados.

## [3.17.10] - Unreleased

### Added
- A typed diagnostic-event facade and regression guards that reject direct Android logging and
  stack-trace printing outside the sanitized implementation.

### Changed
- Debug diagnostics now emit content-free event identifiers, while release builds discard them;
  names, numbers, URIs, note content, call handles, credentials, and exception messages are no
  longer written by production logging paths.
- Moved speed-dial configuration and contact-picker presentation out of the oversized Settings
  screen; the extracted UI now consumes immutable values and explicit callbacks instead of feature
  ViewModels.
- Decoupled shared selection, theme, color, and switch components from settings state ownership;
  preference dialogs now use one explicit route and immutable presentation inputs.

## [3.17.9] - Unreleased

### Changed
- Closed the Android Lint correctness debt and made zero lint errors a release gate.
- Kept Room schema version 29, legacy migration fixtures, local backup formats, and PIN/biometric
  authentication contracts unchanged.
- Merged the lint closure work package through PR #62 without creating a standalone Play update.
- Moved call-banner contact lookup and holiday dependencies behind injected domain boundaries.
- Made call notification permission and channels part of dialer onboarding and Phone Status readiness.

### Fixed
- Removed the unavailable Telecom missed-call cancellation API and added the API 28–30 phone-number
  comparison fallback.
- Replaced Compose resource reads tied to `LocalContext.current` with configuration-aware resources.
- Restored reliable access to active calls through incoming and ongoing `CallStyle` notifications,
  exact in-call navigation, foreground restoration, and persistent in-app controls.
- Unified the ongoing-call foreground-service lifecycle and refreshed mute and speaker actions from
  the current Telecom state.
- Prevented a late ongoing `CallStyle` refresh from crashing after answering on the lock screen while
  Fun Phone transitions back to its in-call UI.
- Deferred ongoing-service shutdown until a pending foreground start completes, preventing Android's
  foreground-service timeout during the same lock-screen transition.

## [3.17.8] - 2026-08-14

### Added
- A focused ktlint guard that reports unused Kotlin imports locally and in CI.

### Changed
- Restored the stable pre-refactor application baseline while preserving the existing Room,
  backup, PIN, and biometric contracts.
- Aligned history search with the contacts search field and focused it immediately when opened.
- Dependency updates return to deliberate manual review instead of automated Dependabot pull
  requests.

### Fixed
- Removed unused Kotlin imports and restored source-format compliance for the release branch.

## [3.17.6] - 2026-08-03

### Added
- A one-time, localized network disclosure before the public-holidays tool contacts Nager.Date.

### Changed
- Restored the optional HTTPS public-holiday lookup with bounded responses, timeouts and an
  in-memory country/year cache. Contacts, calls and calendar entries are never included.

### Fixed
- Restored the previous launcher artwork so the Android splash screen no longer shows the
  unintended generic phone icon.
- The holidays tool no longer crashes when checking connectivity without
  `ACCESS_NETWORK_STATE`; stale or mis-merged builds now show the offline state instead.

## [3.17.5] - 2026-08-01

### Added
- Respectful Play In-App Review eligibility after three connected calls on three distinct days,
  only while Fun Phone is the default dialer, with a 180-day local cooldown.
- Unit coverage for Android 9 call-direction fallback and review eligibility boundaries.

### Changed
- Both Play and Community builds now use a zero-collection architecture with no Internet
  permission, Firebase, analytics, advertising, remote photo storage, or embedded web previews.
- Contact photos share as local `content://` files through Android Sharesheet.

### Fixed
- Android 9 no longer calls the API 29-only `Call.Details.getCallDirection()` method when
  classifying disconnected calls.

### Removed
- Firebase Auth, Storage, Analytics, App Check, Google Services and the obsolete online holiday
  lookup from release runtime dependencies.

## [3.17.1] - 2026-07-27

### Added
- Search and scoped deletion for local call history.
- Non-destructive Room migrations from every exported legacy schema 2–7.
- Broader adaptive-layout, accessibility, contact CRUD, dialer-result, and biometric-state coverage.

### Changed
- Tool personalization now shows a Custom state and uses the configured accent color.
- Contact setup requests the complete permission contract needed to create and edit local contacts.
- Onboarding, authentication dialogs, and blocked-number management are safer at large font sizes and compact heights.

### Fixed
- Local contacts no longer use an unregistered Android sync-account sentinel.
- Biometric cancellation, failed attempts, and terminal lockout now produce distinct state transitions.

### Upgrade notes
- Version code 231 preserves the `com.bookverse.contacts` Play application ID and Room schema 29.
- Updating from 3.17.0 does not introduce a schema change or remove additional user data.

## [3.17.0] - 2026-07-24

### Added
- Community and Play distribution boundaries with explicit Firebase adapters.
- Simplified onboarding, limited mode recovery, Phone Status diagnostics, and configurable tool presets.
- Focused UI, domain, migration, reminder, widget, and benchmark coverage.

### Changed
- Calls and contacts now lead the product hierarchy across onboarding, listing, and release documentation.
- Notification, receiver, widget, navigation, and high-risk UI paths were made more deterministic and testable.
- Database upgrades no longer use a destructive fallback.

### Removed
- Academic Management UI, navigation, persistence logic, resources, dependency, and Room tables.

### Upgrade notes
- Version code 230 preserves the `com.bookverse.contacts` Play application ID.
- The verified 28→29 migration removes only Academic Management tables and preserves representative notes and expense data.
- Explicit non-destructive migrations now preserve available notes, checklists, history, favorites,
  reminders, and recordings when upgrading directly from exported database schemas 2–7.
- Users upgrading from 3.16.5 keep app preferences and non-academic Room data; Android Contacts Provider data remains outside the app database.

---

### Added
- 🎉 Fun Phone adoptó GPL v3 y preparó su publicación FOSS reproducible.
- CI/CD pipelines with GitHub Actions
- Automated release builds with keystore security
- PR validation with code quality checks
- `CONTRIBUTING.md` — Guía completa de contribución para la comunidad
- `CODE_OF_CONDUCT.md` — Código de conducta (Contributor Covenant v2.1)
- `SECURITY.md` — Política de reporte responsable de vulnerabilidades
- `PRIVACY.md` — Política de privacidad detallada (datos locales vs. Firebase)
- `TRADEMARK.md` — Política de uso de marca "Fun Phone" y assets gráficos
- `SETUP.md` — Guía paso a paso para configurar el entorno de desarrollo
- `.github/ISSUE_TEMPLATE/` — Templates para bug reports y feature requests
- `.github/PULL_REQUEST_TEMPLATE.md` — Template de PR con checklist
- `.github/FUNDING.yml` — Configuración de GitHub Sponsors
- `.github/CODEOWNERS` — Asignación automática de reviewers
- `google-services.json.example` — Plantilla de Firebase para contribuidores
- `gradle.properties.example` — Plantilla de Gradle sin credenciales

### Changed
- Licencia cambiada de MIT a **GPL v3** para mayor protección del código
- `README.md` actualizado con badges FOSS, documentación de comunidad y guía de setup
- `.gitignore` actualizado para excluir `google-services.json` y `gradle.properties`
- Sección de "Primeros Pasos" en README actualizada (sin credenciales expuestas)

### Security
- Removidas credenciales placeholder de `gradle.properties` del repositorio
- `google-services.json` excluido del control de versiones
- Archivos `.example` creados como plantillas seguras para contribuidores

### Removed
- Academic Management UI, navigation, persistence logic, localized resources, and Apache POI dependency.
- Academic Room tables through a targeted 28→29 migration that preserves unrelated app data.
- Backup en la nube de notas y contraseñas (Firebase Storage) — el código no estaba conectado a la UI
- `BackupRepository`, `BackupViewModel`, `BackupModels`, `BackupValidator` y la dependencia `firebase-firestore-ktx` (no se utilizaba en ningún archivo de código)

---

## [3.12.9] - 2024-12-16

### Added
- Comprehensive CI/CD setup with GitHub Actions
- Three workflows: Android CI, PR Checks, and Release builds
- Automated artifact uploads and GitHub Releases
- Security-focused release signing with encrypted secrets
- Helper script for setting up release credentials
- Detailed CI/CD documentation and guides

### Changed
- Updated build configuration to prevent accidental release builds without valid keystore
- Improved Gradle task guards for release variants
- Enhanced security model for CI/CD pipelines

### Fixed
- CI build failures due to missing release credentials
- Workflow errors when building release variants in CI

### Documentation
- Added `.github/CI_CD_GUIDE.md` with complete setup instructions
- Added `.github/CI_CD_OVERVIEW.md` with visual workflow diagrams
- Added `.github/workflows/README.md` with quick reference
- Added `keystore.properties.example` template
- Updated main README with CI/CD badges and setup info

---

## [3.12.0] - 2024-12-01

### Added
- Academic Management module (Gestión Académica)
- Course tracking with CRUD operations
- Assignment management with due dates
- Grade calculator with weighted averages
- Schedule viewer with conflict detection
- Study statistics and analytics
- Material 3 redesign for academic screens

### Changed
- Updated Room database to version 12
- Improved navigation with academic section
- Enhanced Material 3 theming system

### Fixed
- Various UI inconsistencies in dark mode
- Performance improvements in lazy lists

---

## [3.11.0] - 2024-11-15

### Added
- Menstrual calendar with cycle tracking
- Expense manager with budget tracking
- Enhanced paint studio with more brushes
- New mini-games: Memory Match, Tic-Tac-Toe

### Changed
- Migrated all screens to Jetpack Compose
- Updated to Compose BOM 2025.04.00
- Improved app startup performance

### Fixed
- Crash on some devices with ML Kit
- QR scanner reliability improvements

---

## [3.10.0] - 2024-10-20

### Added
- Voice utilities (Speech-to-Text, Text-to-Speech)
- Public holidays API integration
- Calendar integration
- Widget system for favorites

### Changed
- Updated Firebase SDKs to latest versions
- Improved encryption with Tink 1.18
- Enhanced biometric authentication

### Fixed
- Memory leaks in some ViewModels
- Contact sync issues

---

## [3.9.0] - 2024-09-15

### Added
- Password manager (Vault) with encryption
- QR code generator and scanner
- Paint studio for drawing
- Initial game suite (Snake, 2048, Minesweeper)

### Changed
- Complete UI overhaul with Material 3
- Migrated to Jetpack Compose for all new features
- Updated minimum SDK to 28 (Android 9)

### Security
- Implemented Tink encryption for sensitive data
- Added biometric authentication for vault
- Enhanced Firebase App Check integration

---

## [3.8.0] - 2024-08-01

### Added
- Notes with rich text support
- Audio recording in notes
- Reminder system with AlarmManager
- Import/Export functionality

### Changed
- Improved search performance
- Better dark mode support
- Updated to Material 3 components

---

## [3.7.0] - 2024-07-01

### Added
- Contact management with Contacts Provider integration
- Call history and dialer
- Favorites system
- vCard import/export

### Changed
- Initial architecture with MVVM + Repository pattern
- Room database for local storage
- Firebase integration for backups

---

## Version Format

Version numbers follow this pattern: `MAJOR.MINOR.PATCH`

- **MAJOR**: Breaking changes or major new features
- **MINOR**: New features, backwards compatible
- **PATCH**: Bug fixes, minor improvements

Build numbers increment with each release.

---

## Links

- [GitHub Releases](https://github.com/Moncada25/fun-phone/releases)
- [Documentation](.github/CI_CD_GUIDE.md)
- [Contributing Guidelines](CONTRIBUTING.md)
