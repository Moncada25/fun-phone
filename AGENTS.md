# Manual operativo para agentes de desarrollo

Este archivo define cómo trabajar de forma segura y consistente en Fun Phone. Es un contrato
operativo: úsalo para decidir cómo explorar, cambiar, validar y entregar trabajo en este
repositorio. Evita duplicar aquí inventarios de producto o versiones que ya tienen una fuente de
verdad ejecutable.

## 1. Reglas no negociables

1. **No realizar commits ni pushes automáticos.** Solo hacerlo si el usuario lo solicita de forma
   explícita.
2. **Preservar el trabajo existente.** Revisar `git status` antes de editar y no revertir, formatear
   ni sobrescribir cambios ajenos.
3. **Mantener el alcance.** No convertir una corrección puntual en una migración arquitectónica o
   actualización masiva de dependencias.
4. **Compose es la UI.** No introducir layouts XML, Fragments o Views clásicas salvo que una API del
   sistema o librería existente lo requiera y el proyecto ya use ese bridge.
5. **La inyección es manual.** Usar `AppContainer` y factories existentes; no añadir Hilt, Dagger ni
   otro contenedor DI sin una decisión explícita de arquitectura.
6. **Privacidad local-first.** No añadir Firebase, analítica, publicidad, tracking, Crashlytics,
   backup cloud o rutas de red nuevas sin aprobación explícita y revisión de privacidad.
7. **Proteger datos sensibles.** No hardcodear secretos ni registrar contactos, números,
   contraseñas, notas, audio, tokens o contenido personal.
8. **No debilitar seguridad ni persistencia.** Mantener Tink/Keystore, las puertas biométricas/PIN,
   las migraciones Room y la ausencia de fallback destructivo.
9. **Localizar toda UI.** El inglés es baseline; español, alemán, francés y portugués deben quedar
   sincronizados.
10. **Validar proporcionalmente.** Ejecutar los checks correspondientes al riesgo del cambio y
    declarar con claridad cualquier validación omitida.

## 2. Fuentes de verdad

Cuando dos documentos difieran, usa la fuente más cercana al comportamiento ejecutable y corrige
la inconsistencia si está dentro del alcance.

| Tema | Fuente canónica |
| --- | --- |
| versión, `minSdk` y `targetSdk` | `release.properties` |
| `compileSdk`, flavors, build types, signing y tareas | `app/build.gradle.kts` |
| versiones de dependencias y plugins | `gradle/libs.versions.toml` |
| módulos y repositorios Gradle | `settings.gradle.kts` y convention plugins en `build-logic/` |
| permisos, components y entry points Android | `app/src/main/AndroidManifest.xml` |
| esquema Room y migraciones activas | `AppDatabase.kt` y `core/database/schemas/` (`:core:database`) |
| navegación principal y secundaria | `NavigationItem`, `SecondaryScreen` y `MainTabsScaffold` |
| UI/UX, componentes y accesibilidad | [DESIGN.md](DESIGN.md) |
| contratos de calidad y evidencia | [QUALITY_RELEASE_GATES.md](QUALITY_RELEASE_GATES.md) |
| secuencia de releases | [FUN_PHONE_PROGRESSIVE_RELEASE_SPEC.md](FUN_PHONE_PROGRESSIVE_RELEASE_SPEC.md) |
| producto y comportamiento esperado | [PRD.md](PRD.md) |
| setup, signing y comandos operativos | [SETUP.md](SETUP.md) y [CHEATSHEET.md](CHEATSHEET.md) |
| privacidad y límites de datos | [PRIVACY_DATA_FLOW.md](PRIVACY_DATA_FLOW.md) y [TELEMETRY.md](TELEMETRY.md) |

No iniciar una release progresiva nueva hasta que la validación Play de su predecesora esté marcada
como completa en la especificación de releases.

## 3. Contexto técnico estable

Fun Phone es una aplicación Android single-module (`app/`) con package
`com.bookverse.contacts`. Combina marcador predeterminado, contactos, llamadas, productividad,
herramientas y juegos en una experiencia local-first.

Stack y límites principales:

- Kotlin, Jetpack Compose y Material 3.
- MVVM con estado unidireccional, repositorios, adaptadores de plataforma y casos de uso donde
  aportan una frontera real.
- Coroutines, `Flow` y `StateFlow` para trabajo asíncrono y estado observable.
- DI manual mediante `AppContainer` y `ViewModelProvider.Factory`.
- Room con schemas exportados, DataStore para preferencias y SharedPreferences en integraciones o
  áreas legacy ya existentes; no ampliar este último sin una razón concreta.
- WorkManager y AlarmManager para trabajo durable; servicios/receivers Android para Telecom,
  notificaciones y reinicios.
- Tink + Android Keystore para cifrado; biometría/PIN para superficies protegidas.
- Contacts Provider, Telecom, CalendarContract, CameraX, ML Kit, ZXing y servicios de voz como
  integraciones de plataforma.
- Dos rutas de red directas y opcionales: festivos públicos mediante HTTPS hacia Nager.Date y el
  preview social/web solicitado por el usuario.
- Flavors `community` y `play`; build types `debug`, `release` y `benchmark`.

## 4. Arquitectura y flujo de datos

```text
Compose UI
  -> ViewModel / controller / policy
    -> domain interface o repository contract
      -> repository / Android adapter
        -> Room, DataStore, Provider, service o API acotada
```

### UI y presentación

- Las pantallas viven en `ui/screens/<feature>/`; los elementos compartidos en `ui/components/`
  (genéricos sin strings de la app en `:core:designsystem`, el resto en `:app`).
- Aplicar [DESIGN.md](DESIGN.md): tokens semánticos, componentes existentes, estados completos,
  accesibilidad y layouts adaptables.
- Recoger flujos en Compose con `collectAsStateWithLifecycle()`.
- Elevar estado durable o compartido; mantener local solo estado puramente visual y efímero.
- Una ruta con datos asíncronos expone un `*UiState` inmutable con los estados aplicables.
- Navegación, permisos y mensajes de una sola vez usan efectos transitorios, no Boolean durables.
- Los efectos de composición deben respetar ownership y lifecycle; liberar sensores, listeners,
  cámaras y reconocimiento cuando la superficie sale de composición.

### ViewModels, dominio y políticas

- Los ViewModels coordinan estado y casos de uso; no conservan referencias a `Activity`/`View` ni
  renderizan UI. Encapsular dependencias de plataforma tras los adapters existentes cuando aplique.
- Exponer `StateFlow` inmutable y usar `viewModelScope` para coroutines ligadas al ciclo de vida.
- Extraer lógica determinista a políticas o funciones puras cuando permita pruebas JVM rápidas.
- No crear una capa o use case vacío que solo reenvíe una llamada.
- Mantener interfaces de dominio en fronteras con plataforma, red, cifrado o persistencia cuando
  aporten sustitución verificable.

### Repositorios e integraciones

- Reutilizar repositorios y adaptadores antes de crear acceso directo desde UI o ViewModels.
- Ejecutar I/O, cifrado, providers y serialización pesada fuera del main thread.
- Hacer cancelables las operaciones suspendidas y no usar scopes globales para trabajo de UI.
- Mapear fallos técnicos a errores de dominio o UI recuperables sin exponer secretos.
- Las integraciones Android verifican permisos y disponibilidad antes de operar.

### Persistencia y trabajo durable

- Room es la fuente para datos relacionales de la app; DataStore para preferencias reactivas.
- No usar `fallbackToDestructiveMigration`.
- Los workers deben ser idempotentes, usar unique work cuando corresponda y tolerar reintentos,
  reinicio y cambios de zona horaria.
- AlarmManager entrega eventos temporales; WorkManager persiste/reconcilia trabajo durable. No
  intercambiar sus responsabilidades sin revisar recordatorios y boot recovery.

## 5. Convenciones de implementación

### Kotlin

- Cuatro espacios, trailing commas en construcciones multilínea y `val` por defecto.
- Composables en PascalCase; clases en PascalCase; funciones y propiedades en camelCase; constantes
  en `UPPER_SNAKE_CASE`.
- Preferir nullabilidad explícita, sealed types y data classes inmutables frente a flags ambiguos.
- Prefijar con `_` únicamente variables intencionalmente no usadas.
- Añadir comentarios solo para explicar decisiones, invariantes o límites no evidentes; no narrar
  código obvio.
- Mantener imports limpios y archivos dentro del package de su feature.

### Compose

- Reutilizar `MaterialTheme`, `ContactsAppTheme` y `ui/components/`; no hardcodear colores o copy.
- El acento sigue una jerarquía neutral-first. Usar `primary`/`onPrimary` solo para acciones
  principales, foco y anclajes pequeños; usar `primaryContainer`/`onPrimaryContainer` para selección
  y estados activos. Cards, headers, iconos decorativos, listas, acciones secundarias e inactivos
  usan roles neutros de superficie. Conservar los colores semánticos de error, advertencia o éxito.
- `ContactsAppTheme` deriva los roles desde la semilla elegida. Las pantallas no deben recolectar ni
  propagar `accentColor`, crear containers con `primary.copy(alpha = ...)` ni depender de los roles
  `secondary`/`tertiary`. La semilla exacta se reserva para el selector y la barra Retro.
- Hoist state cuando tenga más de un consumidor, deba sobrevivir al flujo o represente datos
  durables.
- Usar `rememberSaveable` para estado de presentación que deba sobrevivir recreación y tenga un
  saver seguro.
- Proporcionar keys estables en listas/grids de entidades.
- Mantener targets de 48 dp, labels localizados y semántica para controles accionables.
- No realizar I/O bloqueante ni mutaciones de estado no controladas durante composición.

### Coroutines y Flow

- Respetar structured concurrency y propagación de cancelación.
- Elegir dispatcher en la frontera que conoce el tipo de trabajo; no dispersar `Dispatchers.IO` por
  UI.
- Evitar colecciones duplicadas del mismo flujo y conversiones `stateIn` sin ownership/lifecycle
  claro.
- En tests, inyectar dispatcher, clock o adapter cuando el comportamiento dependa de ellos.

### Recursos y localización

- Añadir cada string visible al baseline `app/src/main/res/values/strings.xml` y a `values-es`,
  `values-de`, `values-fr` y `values-pt`.
- Conservar nombres, placeholders, plurales y arrays exactamente alineados.
- Usar formatters localizados para fecha, hora, números y moneda.
- No ampliar allowlists de localización para ocultar traducciones ausentes.

## 6. Playbooks por tipo de cambio

### UI o componente Compose

1. Revisar `DESIGN.md`, el componente compartido más cercano y una pantalla equivalente.
2. Definir ownership, estados, eventos y comportamiento Back antes de editar.
3. Implementar con tokens semánticos, recursos localizados y semántica accesible.
4. Cubrir lógica determinista con unit tests y flujos críticos con Compose UI tests.
5. Verificar claro/oscuro, texto grande y tamaños compact/medium/expanded según el riesgo.

### Navegación, shortcut o widget

1. Actualizar de forma coordinada `NavigationViewModel`, `NavigationItem` o `SecondaryScreen` y
   `MainTabsScaffold`/renderer correspondiente.
2. Mantener una sola fuente de estado y restaurar destino, tab y scroll correctamente.
3. Revisar extras de `MainActivity`, manifest, PendingIntents y comportamiento Back si la entrada es
   externa.
4. Añadir prueba de selección, deep link o dispatch según el punto de entrada modificado.

### Room, entidad o DAO

1. Cambiar entidad, DAO y `AppDatabase` de forma atómica.
2. Incrementar versión y añadir auto-migration o migración manual explícita.
3. Compilar para exportar el nuevo schema en `core/database/schemas/`.
4. Añadir pruebas que migren desde las versiones relevantes y preserven datos.
5. Revisar backup/import/export y workers que lean las tablas afectadas.

### Permisos e integración Android

1. Solicitar permisos en contexto mediante `PermissionUtils` o el patrón existente.
2. Modelar granted, denied, rationale, permanently denied y capability unavailable.
3. Actualizar manifest solo si la capacidad lo exige y revisar Play policy/data safety.
4. Probar modo limitado, retry, recreación y retorno desde Settings.

### Telecom y llamadas

1. Preservar el contrato de default dialer, servicios, receivers y notification channels.
2. Revisar `CallController`, `DefaultCallRepository`, restore target, audio routing y lock screen.
3. Ejecutar tests de políticas JVM, guard del manifest y la matriz de regresión Telecom.
4. Exigir validación manual en dispositivo para comportamiento que dependa del carrier/OEM.

### Seguridad, vault o contenido protegido

1. Mantener cifrado autenticado con Tink/Keystore y gates biometría/PIN.
2. No exponer contenido antes de autenticar ni conservarlo en logs, screenshots o estado global.
3. Respetar `passwordManagerEnabled` y cancelación/lockout del prompt.
4. Añadir tests de éxito, fallo, cancelación, datos corruptos y migración cuando apliquen.

### Worker, reminder o receiver

1. Separar decisión determinista de scheduling/IO para probarla en JVM.
2. Usar unique work, restricciones y política de retry coherentes con el caso.
3. Verificar reinicio, actualización, cambio de hora/zona y permisos de alarmas/notificaciones.
4. Añadir instrumentación cuando la garantía dependa de Android framework.

### Release o build logic

1. Leer `SETUP.md`, la especificación progresiva y las guardas de signing.
2. No usar ni modificar keystores reales dentro del repositorio.
3. Mantener separadas las distribuciones `community` y `play` y el benchmark no publicable.
4. No relajar lint, privacidad, metadata o budgets para hacer pasar una release.

## 7. Matriz de validación

Ejecuta desde la raíz y selecciona como mínimo las filas que correspondan al cambio.

| Cambio | Validación mínima |
| --- | --- |
| documentación | `git diff --check` y revisión de enlaces/rutas |
| Kotlin o lógica de dominio | `./gradlew testCommunityDebugUnitTest` |
| UI/Compose | unit tests aplicables, `./gradlew assembleCommunityDebug` y Compose tests relevantes |
| strings/recursos localizados | `./scripts/verify-localization.sh` |
| arquitectura o archivos de alto riesgo | `./scripts/verify-quality-contract.sh` |
| formato Kotlin | `./gradlew ktlintCheck` o `./scripts/verify-source-format.sh` según el alcance |
| Android/manifest/feature boundaries | guards específicos de `scripts/` y `./gradlew lintCommunityDebug` |
| Room | build que exporte schemas + tests de migración instrumentados |
| Telecom | `./scripts/verify-telecom-contract.sh`, unit tests y `TELECOM_REGRESSION_MATRIX.md` |
| privacidad o dependencias | `./scripts/verify-zero-collection.sh` y revisión de `PRIVACY_DATA_FLOW.md` |
| PR amplio | misma secuencia que `.github/workflows/pr-checks.yml` |
| release Play | gates de la especificación progresiva y tareas release guardadas |

Comandos de referencia habituales:

```bash
./gradlew assembleCommunityDebug
./gradlew testCommunityDebugUnitTest
./gradlew lintCommunityDebug
./gradlew connectedCommunityDebugAndroidTest
./gradlew ktlintCheck
./scripts/verify-localization.sh
./scripts/verify-quality-contract.sh
./scripts/verify-lint-ratchet.sh
```

No ejecutar `ktlintFormat`, formatters globales, `clean` ni una suite instrumentada completa si
pueden alterar o bloquear trabajo ajeno sin necesidad. Usar la validación más enfocada que demuestre
el cambio y ampliar según el riesgo.

## 8. Flujo de trabajo del agente

### Antes de editar

1. Leer este manual, `README.md` y el brief específico.
2. Ejecutar `git status --short` y registrar los archivos ya modificados.
3. Explorar con `rg`/`rg --files`, identificar entry points, tests y fuentes de verdad.
4. Definir un plan para trabajo multistep y mantener una sola etapa activa.
5. Resolver hechos inspeccionando el repositorio; preguntar solo por intención o tradeoffs reales.

### Durante el cambio

- Hacer edits mínimos y coordinados; no reescribir archivos no relacionados.
- No revertir cambios del usuario ni asumir que un worktree sucio es desechable.
- Mantener código y documentación sincronizados si cambia un contrato público o de arquitectura.
- Añadir pruebas junto con lógica nueva, priorizando tests JVM para comportamiento determinista.
- No crear archivos temporales tracked, experimentos comentados ni TODOs sin owner/contexto.

### Antes de entregar

1. Revisar `git diff --check`, `git diff --stat` y el diff de cada archivo propio.
2. Confirmar que no entraron secretos, artefactos, schemas inesperados o cambios ajenos.
3. Ejecutar la matriz de validación proporcional.
4. Verificar localización, accesibilidad, seguridad y migraciones cuando correspondan.
5. No hacer commit salvo petición explícita.

## 9. Definición de terminado

Un cambio está terminado cuando:

- cumple el comportamiento solicitado sin ampliar el alcance;
- respeta arquitectura, privacidad, diseño y convenciones del repositorio;
- incluye pruebas proporcionales para lógica o flujos modificados;
- conserva schemas, recursos y documentación sincronizados;
- pasa los checks relevantes o documenta por qué no pudieron ejecutarse;
- no altera trabajo ajeno ni introduce secretos/artefactos;
- deja una entrega clara y sin commit automático.

La respuesta final debe resumir:

- qué cambió y dónde;
- qué se validó y con qué resultado;
- qué no se ejecutó y por qué;
- riesgos, límites o verificación manual pendiente.

## 10. Agente especializado opcional

El repositorio incluye `.github/agents/FunPhoneDev.agent.md` para entornos compatibles. Es una ayuda
especializada, no una fuente de verdad superior a este manual, al código o a los documentos
canónicos listados arriba.
