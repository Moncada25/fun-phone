# 🚀 Plan FOSS: Hacer Fun Phone Open Source

> Documento histórico. El tracker único y vigente es
> [`FUN_PHONE_PROGRESSIVE_RELEASE_SPEC.md`](FUN_PHONE_PROGRESSIVE_RELEASE_SPEC.md). Las decisiones de variante única,
> Firebase obligatorio y readiness que aparecen abajo han sido reemplazadas por los builds
> `community` y `play`; se conserva el contenido como contexto, no como estado operativo.
>
> **Actualización noviembre 2026:** la exclusión de F-Droid (§ 0.2) ya no aplica. Firebase salió
> de la app y el build `community` escanea QR con ZXing, sin ML Kit, Google Play Services, Play Core
> ni Firebase. `community` es candidato para IzzyOnDroid y F-Droid; `play` conserva ML Kit e
> In-App Review.

---

## 📌 0. Directrices actualizadas (v2)

> Referencia histórica; no usar esta sección como estado operativo actual.

Estas directrices están vigentes a partir de junio 2026 y **no se negocian** salvo decisión explícita
del mantenedor principal.

### 0.1 Pilares innegociables

| # | Directriz | Razón |
|---|-----------|-------|
| 1 | **Licencia GPL v3 confirmada** | Máxima protección del código: cualquier fork que se distribuya debe publicar sus cambios bajo la misma licencia. |
| 2 | **Firebase y todo el stack actual se mantienen intactos** | El proyecto no introduce `productFlavors`, no elimina dependencias de Firebase, ML Kit, Google Play Services, Tink u otras. La app es una sola variante de build. |
| 3 | **Código base único** | No hay rama paralela "foss" ni "no-foss". El mismo APK se publica en GitHub Releases y en Google Play Store. |
| 4 | **Distribución: GitHub + Google Play Store** | F-Droid queda explícitamente fuera de alcance (ver § 0.2). |
| 5 | **Identidad del proyecto "Fun Phone"** | Protección paralela a la licencia: el nombre y los assets gráficos siguen siendo propiedad de Bookverse (ver `TRADEMARK.md`). |

### 0.2 Por qué F-Droid queda fuera de alcance

F-Droid exige que el binario distribuido sea **compilable de forma reproducible a partir de código
fuente 100% libre**, sin dependencias propietarias activas en tiempo de ejecución. Fun Phone depende
de varios servicios de Google en runtime (Firebase Auth, App Check, Analytics, Crashlytics, ML Kit,
Play Integrity) y un build sin ellos implicaría un fork con pérdida significativa de funcionalidad.

Aceptamos conscientemente este trade-off: **preferimos un único build coherente y completo a una
versión reducida y bifurcada**. Si en el futuro la comunidad quiere un fork sin Firebase, la GPL v3
lo permite; pero el proyecto principal no lo mantiene.

### 0.3 Compatibilidad GPL v3 ↔ Firebase (aclaración)

La GPL v3 **aplica al código fuente que se distribuye**. Es perfectamente compatible con que la
app consuma servicios backend propietarios en runtime. Los SDKs cliente de Firebase son open
source (mayormente Apache 2.0) y los Términos de Servicio de Firebase no imponen restricciones
sobre la licencia del código que los integra. No hay conflicto legal ni filosófico en mantener
esta arquitectura.

### 0.4 Lo que sí puede cambiar un contributor (bajo GPL v3)

- ✅ Crear un fork público del código
- ✅ Modificar la app y publicar el código fuente resultante
- ✅ Compilar binarios y distribuirlos **bajo otro nombre** (no "Fun Phone" — ver `TRADEMARK.md`)
- ✅ Enviar PRs upstream que se integren al proyecto principal

### 0.5 Lo que NO puede hacer un fork

- ❌ Llamarse "Fun Phone" ni usar el logo
- ❌ Distribuir binarios modificados sin publicar el código fuente
- ❌ Cambiar la licencia del código a algo más permisivo
- ❌ Reclamar autoría original del código base

---

## 📊 1. Análisis de Ventajas y Desventajas

### ✅ Ventajas

| Ventaja                            | Impacto | Detalle                                                                                                                                         |
|------------------------------------|---------|-------------------------------------------------------------------------------------------------------------------------------------------------|
| **Confianza del usuario**          | ALTO    | Los usuarios pueden verificar que no hay spyware, malware ni telemetría oculta. Diferenciador clave frente a apps de contactos/dialer cerradas. |
| **Contribuciones de la comunidad** | ALTO    | Desarrolladores pueden enviar PRs con mejoras, traducciones, corrección de bugs.                                                                |
| **Reputación profesional**         | ALTO    | Demostrar capacidad técnica públicamente. Portfolio vivo y verificable.                                                                         |
| **Auditoría de seguridad**         | ALTO    | La comunidad puede auditar el código de encriptación (Tink), vault de contraseñas, etc.                                                         |
| **Longevidad del proyecto**        | MEDIO   | Si dejas de mantener, otros pueden continuar (fork bajo GPL v3, pero con otro nombre).                                                          |
| **Marketing orgánico**             | MEDIO   | Aparición en listas de apps open source, blogs de privacidad, Reddit r/androiddev, etc.                                                        |
| **Transparencia**                  | ALTO    | Alineado con la filosofía privacy-first del PRD.                                                                                                |
| **Aprendizaje**                    | MEDIO   | Feedback técnico de desarrolladores experimentados.                                                                                             |

### ❌ Desventajas y Riesgos

| Desventaja                         | Impacto | Mitigación                                                              |
|------------------------------------|---------|-------------------------------------------------------------------------|
| **Clones/forks competidores**      | MEDIO   | Protección mediante GPL v3 + identidad del proyecto + branding fuerte.        |
| **Soporte no remunerado**          | MEDIO   | Establecer límites claros en `CONTRIBUTING.md` y templates de issues.   |
| **Exposición de arquitectura**     | BAJO    | El código ya usa patrones estándar; no hay "salsa secreta" algorítmica. |
| **Complejidad de gestión**         | MEDIO   | Definir governance model claro desde el inicio.                         |
| **Uso comercial sin atribución**   | BAJO    | Mitigado con licencia GPL v3.                                           |
| **Contribuciones de baja calidad** | BAJO    | CI/CD + code review + templates de PR.                                  |
| **Vulnerabilidades expuestas**     | BAJO    | Mitigado con `SECURITY.md` + responsible disclosure.                    |
| **Exclusión de F-Droid**           | BAJO    | Trade-off aceptado (ver § 0.2). Cobertura: GitHub Releases + Play Store. |

### 🎯 Veredicto

**Las ventajas superan ampliamente las desventajas.** Fun Phone es una app ideal para FOSS por su
filosofía privacy-first y su stack moderno. El trade-off de F-Droid es consciente y queda
documentado en § 0.2.

---

## 📜 2. Selección de Licencia

### Opciones evaluadas

| Licencia       | Permite uso comercial | Obliga a compartir cambios | Protege contra clones | Comentario |
|----------------|-----------------------|----------------------------|-----------------------|------------|
| **MIT**        | ✅ Sí                  | ❌ No                       | ❌ Mínima              | Demasiado permisiva para nuestro objetivo. |
| **Apache 2.0** | ✅ Sí                  | ❌ No                       | ⚠️ Patentes           | Buena pero no copyleft. |
| **GPL v3**     | ✅ Sí                  | ✅ Sí (copyleft fuerte)     | ✅ Alta                | **Elegida.** Ver § 0.1. |
| **AGPL v3**    | ✅ Sí                  | ✅ Sí (incluso SaaS)        | ✅ Muy alta            | Innecesaria para app móvil. |

### 🏆 Decisión: **GPL v3**

- Cualquiera que modifique y distribuya Fun Phone **debe** publicar su código fuente.
- Impide que alguien tome la app, la renombre y la venda sin compartir cambios.
- Es la licencia de apps open source exitosas como Signal, NewPipe, K-9 Mail.
- **Compatible con Firebase**: la GPL v3 regula el código fuente, no los servicios backend que la
  app consume. Los SDKs cliente de Firebase son Apache 2.0 (compatible con GPL v3).

---

## 🛡️ 3. Protección Intelectual

### 3.1 Protección Legal

| Mecanismo            | Qué protege                | Cómo implementar                          | Costo         |
|----------------------|----------------------------|-------------------------------------------|---------------|
| **Identidad del proyecto** | Nombre "Fun Phone" y logo  | Registrar en IMPI (México) o USPTO (EEUU) | ~$100-500 USD |
| **Licencia GPL v3**  | Código fuente              | ✅ Implementado (LICENSE)                  | Gratis        |
| **Copyright notice** | Autoría original           | ✅ Implementado (© Bookverse 2026)         | Gratis        |
| **CLA**              | Contribuciones de terceros | Opcional — evaluar tras volumen alto      | Gratis        |

### 3.2 Protección Técnica

| Mecanismo                | Qué protege                                           | Estado                        |
|--------------------------|-------------------------------------------------------|-------------------------------|
| **Application ID único** | Identidad en Play Store (`com.bookverse.contacts`)    | ✅ Implementado                |
| **Firma de release**     | APKs legítimos (solo tú tienes el keystore)           | ✅ Implementado                |
| **Firebase project**     | Backend separado (forks necesitan su propio proyecto) | ✅ Implementado                |
| **Branding assets**      | Identidad visual (iconos/avatares protegidos)         | ✅ Documentado en TRADEMARK.md |
| **Play Store listing**   | Presencia comercial (historial, reviews, descargas)   | ✅ Implementado                |

### 3.3 Protección de Marca

**CLAVE**: La licencia open source protege el **CÓDIGO**. La identidad del proyecto protege la **IDENTIDAD**.

**Ejemplo real**: Firefox es open source (MPL), pero "Firefox" y el logo son marcas registradas de
Mozilla. Cualquiera puede hacer un fork del código, pero NO puede llamarlo "Firefox". Debian
recompila Firefox sin marca → se llama "Iceweasel".

### 3.4 ¿Qué pasa si alguien hace un fork?

| Escenario                 | Con GPL v3 + Marca                      |
|---------------------------|-----------------------------------------|
| Fork y mejora open source | ✅ Permitido (con otro nombre)           |
| Fork y vende cerrado      | ❌ Ilegal + violación de marca           |
| Fork con tu nombre/logo   | ❌ Violación de identidad del proyecto         |
| Fork mejora algo útil     | ✅ Deben compartir código (te beneficia) |

**Conclusión: GPL v3 + Identidad del Proyecto = Máxima protección posible en open source.**

---

## 📢 4. Estrategia de Comunicación

### Canales de lanzamiento

| Canal                   | Audiencia                  | Formato                                                       |
|-------------------------|----------------------------|---------------------------------------------------------------|
| **GitHub Release**      | Desarrolladores            | Release notes detalladas + APK                                |
| **Reddit r/androiddev** | Desarrolladores Android    | Post técnico sobre el stack                                   |
| **Reddit r/Android**    | Usuarios general           | Post de presentación                                          |
| **Hacker News**         | Tech community             | "Show HN: Fun Phone – Open Source Android Dialer + Productivity Hub" |
| **Twitter/X**           | Público general            | Thread con features + link                                    |
| **Mastodon/Fediverse**  | Comunidad open source      | Post con hashtags #OpenSource #Android #GPL                   |
| **Google Play Store**   | Usuarios finales           | Listing público                                               |
| **AlternativeTo.net**   | Buscadores de alternativas | Listar como alternativa a Google Phone, Truecaller            |
| **Product Hunt**        | Early adopters             | Lanzamiento con assets visuales                               |

### Plantilla de anuncio

```
🎉 Fun Phone es ahora Open Source bajo GPL v3!

Fun Phone es un hub personal para Android que combina un dialer completo,
gestión de contactos, notas con recordatorios, vault de contraseñas encriptado,
herramientas QR, utilidades de voz, y 12+ mini juegos — todo en una sola app.

¿Por qué open source?
• Transparencia total: puedes auditar cada línea de código
• Tu vault de contraseñas usa Tink encryption — verificable
• Sin telemetría oculta más allá de la reportada en PRIVACY.md
• Contribuciones bienvenidas

Stack técnico:
• 100% Jetpack Compose + Material 3
• MVVM + Repository Pattern
• Room DB con auto-migrations
• Kotlin Coroutines + Flow
• Firebase (Auth, Storage, AppCheck, Analytics, Crashlytics, ML Kit)
• Tink encryption para el password vault

🔗 GitHub: [link]
📱 Play Store: [link]
📜 Licencia: GPL v3
🛡️ Marca: "Fun Phone" y el logo son identificadores del proyecto Bookverse (registro formal no verificado)

¡PRs, issues y feedback son bienvenidos!
```

### Timeline de comunicación

| Fase                    | Acción                                        |
|-------------------------|-----------------------------------------------|
| **Pre-lanzamiento**     | Limpiar repo, crear docs, preparar anuncio    |
| **Día del lanzamiento** | Publicar en GitHub, Reddit, Twitter           |
| **Semana 1**            | Responder issues/PRs iniciales, iterar docs   |
| **Mes 1**               | Blog post técnico, retrospectiva              |
| **Mes 3**               | Primer release community, evaluar roadmap     |

---

## ✅ 5. Estado de Implementación

### Fase 1: Limpieza y seguridad

| Tarea                       | Estado    | Detalle                                                      |
|-----------------------------|-----------|--------------------------------------------------------------|
| Actualizar `.gitignore`     | ✅ Done    | `google-services.json` y `gradle.properties` excluidos       |
| Crear archivos `.example`   | ✅ Done    | `google-services.json.example` + `gradle.properties.example` |
| Limpiar historial de Git    | ⚠️ Manual | Ejecutar antes de hacer público (ver § 6.2)                  |
| Regenerar API keys Firebase | ⚠️ Manual | Ir a Firebase Console y regenerar todas las keys             |

### Fase 2: Licencia y protección legal

| Tarea                    | Estado | Detalle                                              |
|--------------------------|--------|------------------------------------------------------|
| Cambiar LICENSE a GPL v3 | ✅ Done | Copyright (C) 2026 Bookverse + texto completo GPL v3 |
| Crear `TRADEMARK.md`     | ✅ Done | Política de marca bilingüe (ES/EN)                   |
| Crear `PRIVACY.md`       | ✅ Done | Datos locales vs Firebase, permisos, servicios       |

### Fase 3: Documentación de comunidad

| Tarea                      | Estado | Detalle                                                 |
|----------------------------|--------|---------------------------------------------------------|
| Crear `CONTRIBUTING.md`    | ✅ Done | Guía completa bilingüe: setup, convenciones, PR process |
| Crear `CODE_OF_CONDUCT.md` | ✅ Done | Contributor Covenant v2.1 (ES/EN)                       |
| Crear `SECURITY.md`        | ✅ Done | Política de vulnerabilidades con alcance y prioridades  |
| Crear `SETUP.md`           | ✅ Done | Guía paso a paso: Firebase, signing, estructura         |
| Actualizar `README.md`     | ✅ Done | Badges open source, links a docs, credenciales removidas |

### Fase 4: Templates de GitHub

| Tarea           | Estado | Detalle                                |
|-----------------|--------|----------------------------------------|
| Issue templates | ✅ Done | `bug_report.md` + `feature_request.md` |
| PR template     | ✅ Done | Checklist con validaciones             |
| `FUNDING.yml`   | ⏸️ Retirado | Se agrega cuando exista un perfil de Sponsors |
| `CODEOWNERS`    | ✅ Done | Santiago como reviewer principal       |

### Fase 5: Changelog y release

| Tarea                     | Estado | Detalle                           |
|---------------------------|--------|-----------------------------------|
| Actualizar `CHANGELOG.md` | ✅ Done | Entrada completa del release FOSS |
| Preparar release          | ✅ Done | Documentación lista para publicar |

### Fase 6: F-Droid

| Tarea                                | Estado                    | Detalle                              |
|--------------------------------------|---------------------------|--------------------------------------|
| Submit a F-Droid                     | 🟡 Desbloqueado           | `community` sin librerías propietarias (nov 2026). Pendiente: repo público y release firmada. |

---

## 🔜 6. Siguientes Pasos (Manuales)

> 📋 **Toda la checklist operativa está en [`PRE_PUBLISH_CHECKLIST.md`](PRE_PUBLISH_CHECKLIST.md)** —
> ejecutable paso a paso. Esta sección es el resumen ejecutivo.

### 🚨 Antes de hacer público el repositorio

#### Paso 1: Regenerar API keys en Firebase Console

1. Ir a [Firebase Console](https://console.firebase.google.com/) → Proyecto `funphone-34fdf`.
2. **Project Settings** → **General** → Regenerar API Key.
3. **Authentication** → Revisar OAuth clients.
4. Descargar nuevo `google-services.json` y reemplazar el local.

#### Paso 2: Limpiar historial de Git

Las API keys y credenciales están en el historial de Git. **Esto es BLOQUEANTE** para hacer
público. Verifica con `./scripts/verify-public-ready.sh` que no quedan secretos. Dos opciones
(detalles en `PRE_PUBLISH_CHECKLIST.md` § 1):

**Opción A (Recomendada): Repo nuevo limpio**

```bash
cd ..
mkdir fun-phone-public
cd fun-phone-public
git init
cp -r ../fun-phone/* .
cp -r ../fun-phone/.github .
cp ../fun-phone/.gitignore .
git add .
git commit -m "Initial open source release of Fun Phone

Fun Phone is now FOSS under GPL v3.
See CHANGELOG.md for details."
```

**Opción B: Limpiar historial con git filter-repo**

```bash
pip install git-filter-repo
git filter-repo --path app/google-services.json --invert-paths
git filter-repo --blob-callback '
  if b"password123" in blob.data:
    blob.data = blob.data.replace(b"password123", b"REDACTED")
'
```

#### Paso 3: Registrar marca "Fun Phone"

- **México (IMPI)**: ~$2,500 MXN (~$150 USD) — [https://www.gob.mx/impi](https://www.gob.mx/impi)
- **Estados Unidos (USPTO)**: ~$250-350
  USD — [https://www.uspto.gov/trademarks](https://www.uspto.gov/trademarks)
- **Clase**: 9 (Software de computadora/aplicaciones móviles)
- **Protege**: Nombre "Fun Phone" + logotipo

#### Paso 4: Configurar repositorio en GitHub

> 🔒 **Crítico:** Configurar branch protection en `main` antes de publicar. Ver
> [`.github/branch-protection.md`](.github/branch-protection.md) para la spec completa.

- [ ] Habilitar GitHub Discussions
- [ ] **Configurar branch protection en `main` (require PR reviews, CI passing)**
- [ ] Habilitar GitHub Sponsors
- [ ] Habilitar Secret Scanning + Push Protection
- [ ] Habilitar Dependabot alerts + security updates
- [ ] Habilitar GitHub Sponsors
- [ ] Agregar topics: `android`, `kotlin`, `jetpack-compose`, `open-source`, `gpl-v3`,
  `dialer`, `contacts`, `material3`
- [ ] Configurar releases con APKs firmados
- [ ] Actualizar About: descripción + website + topics

#### Paso 5: Publicar y anunciar

1. Push al repositorio público en GitHub.
2. Crear GitHub Release con APK firmado + release notes.
3. Publicar en Reddit (r/androiddev, r/Android).
4. Publicar en Hacker News: "Show HN: Fun Phone – Open Source Android Dialer + Productivity Hub".
5. Publicar en Twitter/X y Mastodon.
6. Registrar en AlternativeTo.net como alternativa a Google Phone/Truecaller.

---

### 🆕 Archivos operativos (v2)

| Archivo | Propósito |
|---------|-----------|
| [`PRE_PUBLISH_CHECKLIST.md`](PRE_PUBLISH_CHECKLIST.md) | Checklist paso a paso para hacer público el repo. |
| [`scripts/verify-public-ready.sh`](scripts/verify-public-ready.sh) | Script que audita secretos, licencias, .gitignore, etc. |
| [`.github/branch-protection.md`](.github/branch-protection.md) | Spec de branch protection recomendada para `main`. |

---

## 📁 7. Archivos del Proyecto (Resumen)

### Archivos modificados

| Archivo        | Cambio                                                         |
|----------------|----------------------------------------------------------------|
| `.gitignore`   | Agregados `google-services.json` y `gradle.properties`         |
| `LICENSE`      | MIT → GPL v3 (texto completo con copyright Bookverse)          |
| `README.md`    | Badges open source, links a docs de comunidad, credenciales removidas, badges de CI corregidos a `Moncada25/fun-phone` |
| `FOSS_PLAN.md` | Directrices v2: GPL v3 + stack completo, F-Droid fuera de alcance |
| `CHANGELOG.md` | Entrada del release open source                                |

### Archivos creados

| Archivo                                     | Propósito                                          |
|---------------------------------------------|----------------------------------------------------|
| `CONTRIBUTING.md`                           | Guía de contribución bilingüe (ES/EN)              |
| `CODE_OF_CONDUCT.md`                        | Código de conducta Contributor Covenant v2.1       |
| `SECURITY.md`                               | Política de reporte de vulnerabilidades            |
| `PRIVACY.md`                                | Política de privacidad (datos locales vs Firebase) |
| `TRADEMARK.md`                              | Protección de marca y assets gráficos              |
| `SETUP.md`                                  | Guía de configuración para desarrolladores         |
| `FOSS_PLAN.md`                              | Este documento                                     |
| `app/google-services.json.example`          | Plantilla de Firebase para contribuidores          |
| `gradle.properties.example`                 | Plantilla de Gradle sin credenciales               |
| `.github/ISSUE_TEMPLATE/bug_report.md`      | Template de reporte de bugs                        |
| `.github/ISSUE_TEMPLATE/feature_request.md` | Template de solicitud de features                  |
| `.github/PULL_REQUEST_TEMPLATE.md`          | Checklist de Pull Requests                         |
| `.github/FUNDING.yml`                       | Configuración de GitHub Sponsors                   |
| `.github/CODEOWNERS`                        | Asignación automática de reviewers                 |
| `PRE_PUBLISH_CHECKLIST.md`                  | Checklist paso a paso para hacer público el repo   |
| `scripts/verify-public-ready.sh`            | Script de auditoría de secretos/estructura         |
| `.github/branch-protection.md`              | Spec de branch protection recomendada para `main`  |

---

## 📊 8. Readiness Score

| Aspecto                 | Antes            | Después                          | Estado                                  |
|-------------------------|------------------|----------------------------------|-----------------------------------------|
| **Licencia**            | MIT (permisiva)  | GPL v3 (copyleft)                | ✅                                       |
| **Secretos en repo**    | ⚠️ Expuestos     | Excluidos de .gitignore          | ✅                                       |
| **Documentación open source** | ❌ Incompleta | 8 documentos nuevos              | ✅                                       |
| **Protección de marca** | ❌ Sin protección | TRADEMARK.md creado              | ✅ (registro pendiente)                  |
| **GitHub community**    | ⚠️ Parcial       | Templates + funding + codeowners | ✅                                       |
| **Directrices claras**  | ❌ Mixtas (F-Droid vs Firebase) | Directrices v2 con pilares explícitos | ✅                       |
| **F-Droid ready**       | ❌ Firebase      | `community` sin Firebase ni ML Kit | 🟡 Desbloqueado (nov 2026)          |
| **Calidad del código**  | ✅ Excelente      | Sin cambios necesarios           | ✅                                       |
| **CI/CD**               | ✅ GitHub Actions | Sin cambios necesarios           | ✅                                       |

**Readiness Score: 7.5/10 → 9.7/10** (después de limpiar historial Git y regenerar Firebase keys)

> La nota máxima (10/10) requeriría además: registro de marca completado, primer PR externo
> recibido y mergeado, y al menos una release pública en GitHub. Son hitos naturales del proceso
> post-lanzamiento.

---

*Documento generado como parte del plan de transición FOSS de Fun Phone.*
*Última actualización: Junio 2026 — Directrices v2 (GPL v3 + stack completo, F-Droid fuera de alcance).*
