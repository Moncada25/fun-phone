# Guía de Contribución / Contributing Guide

¡Gracias por tu interés en contribuir a Fun Phone! 🎉

Thank you for your interest in contributing to Fun Phone! 🎉

---

## 🌐 Idioma / Language

Este proyecto se mantiene en **español** como idioma principal. Los PRs, issues y documentación pueden estar en español o inglés.

This project is maintained in **Spanish** as the primary language. PRs, issues, and documentation can be in Spanish or English.

---

## 🚀 Cómo empezar / Getting Started

### Requisitos previos
- **Android Studio** Ladybug o superior
- **JDK 21**
- **Android SDK 36**
- Dispositivo físico o emulador con **API 29+**

### Configuración del proyecto

1. **Fork** el repositorio en GitHub.
2. Clona tu fork:
   ```bash
   git clone https://github.com/TU_USUARIO/fun-phone.git
   cd fun-phone
   ```
3. Mantén el `gradle.properties` tracked libre de credenciales. Para firma local usa
   `keystore.properties`, `local.properties` o variables de entorno.
4. Usa el build `community` para contribuir: no contiene librerías propietarias. El build `play`
   añade ML Kit para el escáner QR e In-App Review de Google Play (ver [SETUP.md](SETUP.md)).
5. Abre el proyecto en Android Studio.
6. Sincroniza Gradle y compila:
   ```bash
   ./gradlew assembleCommunityDebug
   ```

---

## 📋 Proceso de contribución

### 1. Busca o crea un issue
- Revisa los [issues existentes](../../issues) antes de crear uno nuevo.
- Para features nuevas, abre un issue de discusión primero.
- Para bugs, usa la plantilla de bug report.

### 2. Crea una rama
```bash
git checkout -b feature/nombre-descriptivo
# o
git checkout -b fix/descripcion-del-bug
```

Convención de nombres:
- `feature/` — Nueva funcionalidad
- `fix/` — Corrección de bug
- `docs/` — Documentación
- `refactor/` — Refactorización sin cambio funcional

### 3. Haz tus cambios
- Sigue las [convenciones de código](#-convenciones-de-código).
- Añade cada string nueva al baseline inglés (`values/strings.xml`) y a sus cuatro traducciones:
  `values-es/strings.xml`, `values-de/strings.xml`, `values-fr/strings.xml` y `values-pt/strings.xml`.
- Escribe tests para lógica nueva cuando sea posible.

### 4. Verifica tu trabajo
```bash
./gradlew assembleCommunityDebug          # Compilación sin Firebase
./gradlew testCommunityDebugUnitTest      # Tests unitarios
./gradlew lintCommunityDebug              # Lint
./scripts/verify-lint-ratchet.sh          # Ratchet de deuda conocida
./scripts/verify-android-only.sh           # Guard Android-only
./scripts/verify-localization.sh           # Gate de strings, plurales y arrays
```

### 5. Haz commit
```bash
git commit -m "feat: descripción corta del cambio"
```

Formato de commits (Conventional Commits):
- `feat:` — Nueva funcionalidad
- `fix:` — Corrección de bug
- `docs:` — Documentación
- `refactor:` — Refactorización
- `test:` — Tests
- `chore:` — Mantenimiento

### 6. Envía un Pull Request
- Apunta tu PR a la rama `main`.
- Completa la plantilla del PR.
- Espera la revisión de un maintainer.

---

## 🎨 Convenciones de código

### Kotlin / Compose
- **Indentación**: 4 espacios.
- **Trailing commas** en construcciones multilínea.
- Preferir `val` sobre `var`.
- Funciones Composable en **PascalCase**.
- Operaciones pesadas en `Dispatchers.IO`.
- Usar `collectAsStateWithLifecycle()` (no `collectAsState()`).
- `LazyList` items con parámetro `key`.
- Data classes con `@Immutable` para Compose.
- Usar tokens de color semánticos del tema — no hardcodear `Color(0xFF...)`.

### Localización
- **Todas** las strings visibles al usuario deben ir en:
  - `app/src/main/res/values/strings.xml` (baseline en inglés)
  - `app/src/main/res/values-es/strings.xml` (Español)
  - `app/src/main/res/values-de/strings.xml` (Alemán)
  - `app/src/main/res/values-fr/strings.xml` (Francés)
  - `app/src/main/res/values-pt/strings.xml` (Portugués)
- Nunca hardcodear texto en Kotlin.
- Usar formateadores localizados para fechas, números y monedas.
- Ejecuta `./scripts/verify-localization.sh` antes de abrir el PR. Corrige cada referencia de
  archivo, clave y línea; no ocultes una traducción faltante con una excepción amplia.

### Arquitectura
- **MVVM + Repository Pattern**.
- ViewModels exponen `StateFlow` via `MutableStateFlow` + `.asStateFlow()`.
- Usar `viewModelScope` para coroutines.
- Factories manuales via companion `provideFactory()`.
- No usar Hilt/Dagger — inyección manual.

### Room Database
- Al modificar entidades Room:
  1. Actualizar entity + DAO + `AppDatabase`.
  2. Ejecutar `./gradlew assembleCommunityDebug` para exportar schemas actualizados.
  3. Las auto-migrations manejan la evolución del esquema.

---

## 🔍 Code Review

Los PRs serán revisados considerando:
- ✅ Funcionalidad correcta
- ✅ Adherencia a convenciones de código
- ✅ Localización completa (inglés, español, alemán, francés y portugués)
- ✅ Sin regresiones en funcionalidad existente
- ✅ Uso apropiado de la arquitectura MVVM
- ✅ Componentes Compose reutilizables cuando aplique

---

## 🐛 Reportar bugs

Usa la [plantilla de bug report](../../issues/new?template=bug_report.md) e incluye:
- Versión de Fun Phone
- Versión de Android / dispositivo
- Pasos para reproducir
- Comportamiento esperado vs. actual
- Screenshots o logs si es posible

---

## 💡 Proponer features

Abre un [feature request](../../issues/new?template=feature_request.md) con:
- Descripción clara del feature
- Caso de uso / problema que resuelve
- Mockups o wireframes si aplica

---

## ⚖️ Licencia

Al contribuir, aceptas que tus contribuciones se licenciarán bajo la misma [GPL v3](LICENSE) del proyecto.

---

## 📬 Contacto

- Issues: [GitHub Issues](../../issues)
- Seguridad: Ver [SECURITY.md](SECURITY.md)

¡Gracias por hacer Fun Phone mejor! 🚀
