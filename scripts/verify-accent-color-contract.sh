#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ui_root="$repo_root/app/src/main/java/com/bookverse/contacts/ui"
shopt -s nullglob
ui_roots=("$ui_root")
for module_ui_root in "$repo_root"/core/*/src/main/java/com/bookverse/contacts/ui "$repo_root"/feature/*/src/main/java/com/bookverse/contacts/ui; do
  ui_roots+=("$module_ui_root")
done
shopt -u nullglob

ui_file() {
  local root
  for root in "${ui_roots[@]}"; do
    if [[ -f "$root/$1" ]]; then
      printf '%s\n' "$root/$1"
      return
    fi
  done
  echo "Accent contract source is missing: $1" >&2
  exit 1
}

while IFS= read -r raw_seed_consumer; do
  case "$raw_seed_consumer" in
    */activities/IncomingCallActivity.kt:*) ;;
    *)
      echo "$raw_seed_consumer must consume derived theme roles instead of the raw accent seed."
      exit 1
      ;;
  esac
done < <(grep -rn -F '.accentColor.collectAsStateWithLifecycle()' "${ui_roots[@]}" --include='*.kt' || true)

while IFS= read -r seed_consumer; do
  case "$seed_consumer" in
    */components/MainTopBar.kt | \
    */components/SecondaryTopBar.kt | \
    */components/ConfirmDialog.kt | \
    */screens/MainScreen.kt | \
    */screens/settings/SettingsScreen.kt | \
    */screens/settings/SettingsAppearanceSheets.kt | \
    */screens/settings/AppearancePreview.kt) ;;
    *)
      echo "$seed_consumer must use derived roles; exact seed is reserved for Retro bars and color previews."
      exit 1
      ;;
  esac
done < <(grep -rln -E '(accentPalette|preview)\.seed' "${ui_roots[@]}" --include='*.kt' | grep -v '/theme/' || true)

theme_file=$(ui_file theme/ContactsAppTheme.kt)
palette_file=$(ui_file theme/AccentPalette.kt)
grep -Fq 'surfaceTint = Color.Transparent' "$palette_file" || {
  echo "ContactsAppTheme must keep accent surface tint disabled."
  exit 1
}
grep -Fq 'LocalAccentPalette provides accentPalette' "$theme_file" || {
  echo "ContactsAppTheme must provide the derived accent palette."
  exit 1
}
grep -Fq 'private const val MIN_CONTENT_CONTRAST = 4.5f' "$palette_file" || {
  echo "Accent content roles must retain the 4.5:1 contrast target."
  exit 1
}
settings_screen="$ui_root/screens/settings/SettingsScreen.kt"
grep -Fq 'val accentSeed = MaterialTheme.accentPalette.seed' "$settings_screen" || {
  echo "The accent picker must receive the exact selected seed, not the derived primary role."
  exit 1
}
search_field=$(ui_file components/TopBarSearchField.kt)
search_surface=$(ui_file components/SearchFieldSurface.kt)
glass_surface=$(ui_file components/LiquidGlassSurface.kt)
search_action=$(ui_file components/AppBarSearchAction.kt)
grep -Fq 'contentColor.copy(alpha = 0.15f)' "$search_field" || {
  echo "Minimal and Retro search fields must retain their translucent search container."
  exit 1
}
grep -Fq 'LocalAppChromeStyle.current == AccentBarStyle.MODERN' "$search_field" &&
  grep -Fq 'SearchFieldSurface(' "$search_field" &&
  grep -Fq 'SearchFieldSurface(' "$search_action" &&
  grep -Fq 'LocalAppChromeStyle.current == AccentBarStyle.MODERN' "$search_surface" &&
  grep -Fq 'LiquidGlassSurface(modifier)' "$search_surface" &&
  grep -Fq 'allowBackdrop && LocalBackdropAllowed.current' "$search_surface" &&
  grep -Fq 'modifier.background(legacyColor, CircleShape)' "$search_surface" &&
  grep -Fq 'val glassColor = if (topBar) scheme.background else if (dark) scheme.surfaceContainer else scheme.surfaceContainerHigh' "$glass_surface" &&
  grep -Fq 'color = glassColor' "$glass_surface" &&
  grep -Fq 'MaterialTheme.colorScheme.onSurfaceVariant' "$search_field" &&
  grep -Fq 'MaterialTheme.colorScheme.onSurface else LocalContentColor.current' "$search_field" &&
  grep -Fq 'fieldStyle.copy(color = contentColor)' "$search_field" || {
  echo "Modern search must use neutral semantic surfaces/content while preserving legacy contrast."
  exit 1
}
grep -Fq 'Modifier.heightIn(min = 48.dp)' "$search_field" || {
  echo "Modern search fields must adapt to text size and retain a 48 dp minimum target."
  exit 1
}
grep -Fq 'TopBarSearchField(' "$settings_screen" || {
  echo "SettingsScreen must use the shared top bar search field."
  exit 1
}
settings_components="$ui_root/screens/settings/SettingsComponents.kt"
grep -Fq 'color = MaterialTheme.colorScheme.primaryContainer' "$settings_components" || {
  echo "Settings current-value badges must retain the soft accent container."
  exit 1
}
grep -Fq 'contentColor = MaterialTheme.colorScheme.onPrimaryContainer' "$settings_components" || {
  echo "Settings current-value badges must retain contrast-aware accent content."
  exit 1
}
main_screen="$ui_root/screens/MainScreen.kt"
grep -Fq 'StatusBarProtection(retroStatusBarColor)' "$main_screen" || {
  echo "MainScreen must protect the status bar with the active bar style."
  exit 1
}
system_bar_owner="$ui_root/system/SystemBars.kt"
grep -Fq 'val transparent = android.graphics.Color.TRANSPARENT' "$system_bar_owner" || {
  echo "Activity system-bar policy must keep status bars transparent."
  exit 1
}
if grep -Fq 'val statusBarColor = if (isMinimal)' "$main_screen"; then
  echo "MainScreen must not reintroduce an opaque status-bar strip."
  exit 1
fi

theme_entry_points=(
  "$repo_root/app/src/main/java/com/bookverse/contacts/MainActivity.kt"
  "$ui_root/screens/MainScreen.kt"
  "$ui_root/activities/IncomingCallActivity.kt"
  "$repo_root/app/src/main/java/com/bookverse/contacts/widget/ContactDetailActivity.kt"
)
for entry_point in "${theme_entry_points[@]}"; do
  grep -Fq 'accentSeed = ' "$entry_point" || {
    echo "$entry_point must pass the selected accent seed to ContactsAppTheme."
    exit 1
  }
done

theme_owned_components=(
  "components/ButtonSheetScaffold.kt"
  "components/FeaturesGroup.kt"
  "components/GroupItem.kt"
  "components/InfoGroupItem.kt"
  "components/PermissionBanner.kt"
  "components/SelectionButton.kt"
  "components/SettingSwitch.kt"
  "navigation/MainTabNavigationBar.kt"
  "screens/settings/SettingsComponents.kt"
  "screens/settings/SettingsSheetComponents.kt"
  "screens/tools/ToolsPresetSelector.kt"
)
for relative_path in "${theme_owned_components[@]}"; do
  source_file=$(ui_file "$relative_path")
  if grep -Fq 'accentColor: Color' "$source_file"; then
    echo "$relative_path must consume accent roles from MaterialTheme."
    exit 1
  fi
done

theme_owned_view_model_free_components=(
  "components/CountrySelectDialog.kt"
  "components/DatePickerDialog.kt"
  "components/IncomingCallBackgroundSelector.kt"
  "components/LoaderDialog.kt"
  "components/MessageDialog.kt"
  "components/NoteItem.kt"
  "components/OptionsDialog.kt"
  "components/PhoneNumbersField.kt"
  "components/PinInputDialog.kt"
  "components/RetroGameInstructionsDialog.kt"
  "components/RetroGameOptionsDialog.kt"
  "components/TimePickerDialog.kt"
  "components/TextField.kt"
  "screens/contacts/AddFavoritesDialog.kt"
  "screens/contacts/SocialMediaSearchDialog.kt"
)
for relative_path in "${theme_owned_view_model_free_components[@]}"; do
  source_file=$(ui_file "$relative_path")
  if grep -Fq 'appSettingsViewModel: AppSettingsViewModel' "$source_file"; then
    echo "$relative_path must read accent roles from MaterialTheme without settings prop drilling."
    exit 1
  fi
done

echo "Neutral-first accent color contract passes."
