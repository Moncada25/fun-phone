# Política de Seguridad / Security Policy

## Versiones soportadas / Supported Versions

| Versión | Soportada |
|---------|-----------|
| 3.x.x  | ✅ Sí     |
| < 3.0   | ❌ No     |

---

## Español

### Reportar una vulnerabilidad

Si descubres una vulnerabilidad de seguridad en Fun Phone, por favor repórtala de forma responsable.

**⚠️ NO abras un issue público para vulnerabilidades de seguridad.**

#### Cómo reportar

1. Envía un email a: **santiago.moncada.dev@gmail.com**
2. Incluye:
   - Descripción detallada de la vulnerabilidad
   - Pasos para reproducirla
   - Impacto potencial
   - Versión de Fun Phone afectada
   - Tu información de contacto (opcional)

#### Qué esperar

- **Confirmación**: Recibirás una confirmación dentro de **48 horas**.
- **Evaluación**: Evaluaremos la vulnerabilidad dentro de **7 días**.
- **Resolución**: Trabajaremos en una corrección y la publicaremos en un release de seguridad.
- **Crédito**: Si lo deseas, te daremos crédito en el CHANGELOG y release notes.

#### Alcance

Las siguientes áreas son de especial interés:

| Área | Prioridad |
|------|-----------|
| **Vault de contraseñas** (Tink encryption) | 🔴 Crítica |
| **Autenticación biométrica** | 🔴 Crítica |
| **Backups cifrados y bloqueo de la app** | 🔴 Crítica |
| **Almacenamiento de datos sensibles** | 🟡 Alta |
| **Permisos de Android** | 🟡 Alta |
| **Comunicaciones de red** | 🟡 Alta |
| **Servicios de llamadas (InCallService)** | 🟠 Media |

#### Fuera de alcance

- Vulnerabilidades en dependencias de terceros (reportar directamente al proveedor)
- Ataques que requieren acceso físico al dispositivo desbloqueado
- Ataques de ingeniería social
- Denegación de servicio (DoS)

---

## English

### Reporting a Vulnerability

If you discover a security vulnerability in Fun Phone, please report it responsibly.

**⚠️ DO NOT open a public issue for security vulnerabilities.**

#### How to report

1. Send an email to: **santiago.moncada.dev@gmail.com**
2. Include:
   - Detailed description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Affected Fun Phone version
   - Your contact information (optional)

#### What to expect

- **Acknowledgment**: You will receive confirmation within **48 hours**.
- **Assessment**: We will evaluate the vulnerability within **7 days**.
- **Resolution**: We will work on a fix and publish it in a security release.
- **Credit**: If desired, we will credit you in the CHANGELOG and release notes.

#### Scope

The following areas are of special interest:

| Area | Priority |
|------|----------|
| **Password vault** (Tink encryption) | 🔴 Critical |
| **Biometric authentication** | 🔴 Critical |
| **Encrypted backups and app lock** | 🔴 Critical |
| **Sensitive data storage** | 🟡 High |
| **Android permissions** | 🟡 High |
| **Network communications** | 🟡 High |
| **Call services (InCallService)** | 🟠 Medium |

#### Out of scope

- Vulnerabilities in third-party dependencies (report directly to the vendor)
- Attacks requiring physical access to an unlocked device
- Social engineering attacks
- Denial of service (DoS)
