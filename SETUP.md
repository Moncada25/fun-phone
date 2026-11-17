# Guía de Configuración / Setup Guide

Esta guía te ayudará a configurar Fun Phone para desarrollo local.

This guide will help you set up Fun Phone for local development.

---

## Requisitos previos / Prerequisites

| Herramienta          | Versión mínima              |
|----------------------|-----------------------------|
| Android Studio       | Ladybug (2024.2) o superior |
| JDK                  | 21                          |
| Android SDK          | API 37 (compileSdk)         |
| Dispositivo/Emulador | API 29+ (minSdk)            |
| Git                  | 2.x                         |

---

## 1. Clonar el repositorio / Clone the repository

```bash
git clone https://github.com/TU_USUARIO/fun-phone.git
cd fun-phone
```

---

## 2. Configurar Gradle Properties

El repositorio incluye un `gradle.properties` seguro con memoria y opciones ordinarias. No agregues
credenciales a ese archivo. Usa `gradle.properties.private`, `keystore.properties`,
`local.properties` o variables de entorno para firma release.

```bash
cp gradle.properties.example gradle.properties.private
```

Para desarrollo (debug builds), los valores por defecto funcionan. Para release builds, configura
el keystore en el `gradle.properties.private` ignorado, `keystore.properties` o `local.properties`:

```properties
RELEASE_STORE_FILE=path/to/your-release-key.jks
RELEASE_KEY_ALIAS=your-key-alias
RELEASE_STORE_PASSWORD=your-store-password
RELEASE_KEY_PASSWORD=your-key-password
```

### Crear un keystore de desarrollo (opcional)

```bash
keytool -genkey -v -keystore debug-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias debug-alias
```

---

## 3. Elegir distribución

`community` es la distribución predeterminada para contribuciones y se instala como
`com.bookverse.contacts.community.debug`. No requiere una cuenta o backend externo.

```bash
./gradlew assembleCommunityDebug
```

`play` conserva el camino oficial de Google Play y añade únicamente el cliente de In-App Review.
Tampoco usa Firebase, analítica, publicidad, almacenamiento remoto ni permiso de Internet. No se
necesita `google-services.json`. Para un AAB de producción solo necesitas la configuración de firma
release descrita arriba.

Verifica ambas distribuciones:

```bash
./gradlew assembleCommunityDebug assemblePlayDebug
./gradlew :app:dependencies --configuration playDebugRuntimeClasspath
```

La segunda salida no debe contener Firebase, Analytics, Auth, Storage, App Check, Crashlytics o SDKs
publicitarios. Los archivos Firebase locales heredados están ignorados y no participan del build.

---

## 4. Compilar y ejecutar / Build and Run

### Community debug

```bash
./gradlew assembleCommunityDebug
```

### Instalar Community

```bash
./gradlew installCommunityDebug
```

Community usa nombre y `applicationId` distintos, por lo que puede convivir con Play.

### Ejecutar tests

```bash
./gradlew testCommunityDebugUnitTest   # Tests unitarios
./gradlew connectedCommunityDebugAndroidTest  # Tests instrumentados
```

### Medición de rendimiento local

La variante `communityBenchmark` replica la optimización/minificación de release, usa un
`applicationId` separado, firma debug únicamente para instalación local y permite profiling desde
ADB. No reemplaza la app instalada y **nunca debe publicarse**.

```bash
./gradlew installCommunityBenchmark
# Completa onboarding con datos vacíos/sintéticos y llega a la ruta principal.
RELEASE_LIKE=true DEVICE_STABILIZED=true \
  ./scripts/measure-core-performance.sh com.bookverse.contacts.community.benchmark
```

Declara `DEVICE_STABILIZED=true` solo después de reiniciar el dispositivo y estabilizarlo
térmicamente. El colector rechaza paquetes depurables como evidencia release-like.

### Verificar estilo

```bash
./gradlew lintCommunityDebug           # Android Lint: cero errores
./scripts/verify-lint-ratchet.sh       # Cero errores y máximo de 393 warnings
./scripts/verify-android-only.sh        # Guard Android-only
```

---

## 5. Configurar como Dialer predeterminado (opcional)

Para probar funciones de llamada:

1. Instala la app en el dispositivo.
2. Ve a **Ajustes > Apps > Apps predeterminadas > App de teléfono**.
3. Selecciona **Fun Phone**.

> Esto es necesario para: gestión de llamadas, InCallService, notificaciones de llamadas perdidas.

---

## 6. Permisos necesarios

La app solicita permisos de forma contextual. Para testing completo, concede todos los permisos
desde ajustes del dispositivo:

- Contactos
- Teléfono
- Cámara
- Micrófono
- Calendario
- Notificaciones
- Almacenamiento (para backups)

---

## 7. Estructura del proyecto

```
fun-phone/
├── app/
│   ├── src/main/java/com/bookverse/contacts/
│   │   ├── App.kt                    # Application class
│   │   ├── MainActivity.kt           # Entry point
│   │   ├── data/                     # Data layer (Room, models, repos)
│   │   ├── domain/                   # Domain models
│   │   ├── repository/               # Repository implementations
│   │   ├── security/                 # CryptoManager (Tink)
│   │   ├── service/                  # Android services (calls, notifications)
│   │   ├── settings/                 # Fonts, themes, transitions
│   │   ├── ui/                       # Compose UI layer
│   │   │   ├── screens/              # Feature screens
│   │   │   ├── components/           # Reusable components
│   │   │   ├── navigation/           # Navigation logic
│   │   │   ├── viewmodel/            # ViewModels
│   │   │   └── theme/                # Material 3 theming
│   │   ├── utils/                    # Utility classes
│   │   └── widget/                   # App widgets
│   ├── src/main/res/                 # Resources
│   └── schemas/                      # Room schema exports
├── gradle/libs.versions.toml         # Version catalog
├── CONTRIBUTING.md                   # Contribution guide
├── LICENSE                           # GPL v3
└── README.md                         # Project overview
```

---

## Solución de problemas / Troubleshooting

### Aparece una referencia heredada a `google-services.json`

Fun Phone 3.18.0+ no usa ese archivo. Verifica que estés en el código actual, limpia outputs con
`./gradlew clean` y vuelve a compilar. No copies una configuración Firebase al proyecto.

### Error: "Release signing not configured"

Solo necesitas signing para release builds. Debug builds funcionan sin configuración.

### Error: Room schema mismatch

```bash
./gradlew clean assembleCommunityDebug
```

Esto regenera los schemas de Room automáticamente.

### Build lento

Agrega a `gradle.properties`:

```properties
org.gradle.parallel=true
org.gradle.daemon=true
org.gradle.caching=true
```

---

## ¿Necesitas ayuda? / Need help?

- 📖 Lee [CONTRIBUTING.md](CONTRIBUTING.md) para la guía de contribución
- 🐛 Abre un [issue](../../issues) para reportar problemas
- 🔒 Lee [SECURITY.md](SECURITY.md) para vulnerabilidades
