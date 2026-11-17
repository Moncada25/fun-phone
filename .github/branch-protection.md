# Branch Protection — `main`

> Especificación de las reglas de protección de la rama `main` que debes aplicar
> en GitHub UI antes de hacer público el repositorio.

## 🎯 Objetivo

- **Nadie** puede pushear directamente a `main` (incluido el owner).
- Todos los cambios pasan por Pull Request.
- Cada PR requiere al menos 1 aprobación de un Code Owner.
- Las PRs deben pasar el workflow de `Android CI` antes de mergear.
- No se permiten force pushes ni borrado de la rama.

## 📍 Dónde configurarlo

`https://github.com/Moncada25/fun-phone/settings/branches` → **Add rule**

## ⚙️ Configuración

| Campo | Valor |
|-------|-------|
| **Branch name pattern** | `main` |
| **Require a pull request before merging** | ✅ ON |
| → Require approvals | **1** |
| → Dismiss stale approvals on new commits | ✅ |
| → Require review from Code Owners | ✅ |
| → Require approval of the most recent reviewable push | ✅ |
| **Require status checks to pass before merging** | ✅ ON |
| → Require branches to be up to date | ✅ |
| → Status checks required | `Android CI` |
| **Require conversation resolution** | ✅ ON |
| **Require signed commits** | ⚪ OFF (opcional) |
| **Require linear history** | ✅ ON (squash o rebase) |
| **Include administrators** | ✅ ON |
| **Allow force pushes** | ❌ OFF |
| → Allow force pushes to specific people | ❌ — |
| **Allow deletions** | ❌ OFF |

## 🔍 Verificación

Después de configurarlo:

1. Intenta `git push origin main` directamente → debe ser rechazado.
2. Abre una PR de prueba → debe pedir aprobación.
3. Verifica que el badge "Branch protection" aparece en la home del repo.

## 🚨 Si algo sale mal

- **PR mergeada sin tu approval**: revisa que "Include administrators" esté ON.
- **PR mergeada con CI fallando**: revisa que `Android CI` esté en "Status checks required".
- **No puedes mergear tu propia PR**: pide review a otro maintainer.

## 📚 Referencias

- [GitHub: About protected branches](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- [GitHub: Managing a branch protection rule](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/managing-a-branch-protection-rule)
