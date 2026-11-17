# Branch Protection — `main`

> Reglas activas en el ruleset **Protect main** del repositorio. Están pensadas para un único
> mantenedor: todo pasa por Pull Request y CI, sin exigir aprobación de otra persona.

## 🎯 Objetivo

- Nadie puede pushear directamente a `main`, incluido el owner.
- Todos los cambios pasan por Pull Request.
- Las PRs deben pasar `validate` (PR Checks) y `gitleaks` (Secret scanning) antes de mergear.
- No se permiten force pushes ni borrar la rama.

## 📍 Dónde se configura

`https://github.com/Moncada25/fun-phone/settings/rules` → **Protect main**

## ⚙️ Configuración

| Regla | Valor |
|-------|-------|
| **Rama objetivo** | Rama por defecto (`main`) |
| **Bypass** | Nadie |
| **Require a pull request before merging** | ✅ ON |
| → Required approvals | **0** |
| → Require conversation resolution | ✅ ON |
| → Merge methods | Squash, Rebase |
| **Require status checks to pass** | ✅ ON |
| → Require branches to be up to date | ✅ ON |
| → Checks requeridos (GitHub Actions) | `validate`, `gitleaks` |
| **Require linear history** | ✅ ON |
| **Block force pushes** | ✅ ON |
| **Restrict deletions** | ✅ ON |

Ajustes del repositorio que acompañan al ruleset: merge commits deshabilitados y borrado
automático de la rama al mergear.

## 👥 Si se suman mantenedores

Sube **Required approvals** a 1 y activa **Require review from Code Owners** en el mismo ruleset.

## 🔍 Verificación

1. `git push origin main` directamente → debe ser rechazado.
2. Una PR no se puede mergear hasta que `validate` y `gitleaks` pasen.

## 📚 Referencias

- [GitHub: About rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets)
