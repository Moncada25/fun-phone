#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
quality_file="$repo_root/QUALITY_RELEASE_GATES.md"

if [[ ! -f "$quality_file" ]]; then
  echo "Missing QUALITY_RELEASE_GATES.md"
  exit 1
fi

required_ids=(
  DOM-01 DOM-02 DOM-03 DOM-04 DOM-05 DOM-06 DOM-07 DOM-08 DOM-09
  UI-01 UI-02 UI-03 UI-04 UI-05 UI-06 UI-07 UI-08
  PERF-01 PERF-02 PERF-03 PERF-04 PERF-05 PERF-06
  LOC-01
)
for id in "${required_ids[@]}"; do
  grep -Fq "$id" "$quality_file" || {
    echo "Quality release matrix is missing $id"
    exit 1
  }
done

check_line_ceiling() {
  local relative_path="$1"
  local ceiling="$2"
  local actual
  actual="$(wc -l < "$repo_root/$relative_path" | tr -d ' ')"
  if (( actual > ceiling )); then
    echo "$relative_path grew to $actual lines (ceiling: $ceiling). Extract a cohesive component."
    exit 1
  fi
}

check_line_ceiling "feature/settings/src/main/java/com/bookverse/contacts/ui/screens/settings/SettingsScreen.kt" 2080
check_line_ceiling "feature/contacts/src/main/java/com/bookverse/contacts/ui/viewmodel/ContactsViewModel.kt" 1714
check_line_ceiling "feature/contacts/src/main/java/com/bookverse/contacts/ui/screens/contacts/ContactDetailsScreen.kt" 1604
check_line_ceiling "feature/notes/src/main/java/com/bookverse/contacts/ui/screens/notes/NoteListScreen.kt" 1372
check_line_ceiling "feature/dialer/src/main/java/com/bookverse/contacts/ui/screens/dialer/DialerScreen.kt" 1127
check_line_ceiling "feature/dialer/src/main/java/com/bookverse/contacts/ui/screens/history/HistoryScreen.kt" 1109
check_line_ceiling "feature/tools/src/main/java/com/bookverse/contacts/ui/screens/tools/HolidaysScreen.kt" 1352

speed_dial_sheet="$repo_root/feature/settings/src/main/java/com/bookverse/contacts/ui/screens/settings/SpeedDialSettingsDialog.kt"
for forbidden_view_model in AppSettingsViewModel ContactsViewModel; do
  if grep -Fq "$forbidden_view_model" "$speed_dial_sheet"; then
    echo "SpeedDialConfigSheet must consume immutable values and callbacks, not $forbidden_view_model."
    exit 1
  fi
done

pure_settings_components=(
  "$repo_root/feature/settings/src/main/java/com/bookverse/contacts/ui/screens/settings/SettingsComponents.kt"
  "$repo_root/feature/settings/src/main/java/com/bookverse/contacts/ui/screens/settings/SettingsSections.kt"
  "$repo_root/core/ui/src/main/java/com/bookverse/contacts/ui/screens/settings/SettingsSheetComponents.kt"
  "$repo_root/feature/settings/src/main/java/com/bookverse/contacts/ui/screens/settings/SettingsAppearanceSheets.kt"
  "$repo_root/feature/settings/src/main/java/com/bookverse/contacts/ui/screens/settings/SettingsPreferenceSheets.kt"
  "$repo_root/core/ui/src/main/java/com/bookverse/contacts/ui/components/SelectBottomSheet.kt"
  "$repo_root/core/designsystem/src/main/java/com/bookverse/contacts/ui/components/SettingSwitch.kt"
)
for component in "${pure_settings_components[@]}"; do
  if grep -Fq "AppSettingsViewModel" "$component"; then
    echo "${component#$repo_root/} must consume immutable values and callbacks, not AppSettingsViewModel."
    exit 1
  fi
done

required_tests=(
  "feature/contacts/src/test/java/com/bookverse/contacts/domain/contacts/ContactSearchPolicyTest.kt"
  "feature/contacts/src/test/java/com/bookverse/contacts/domain/contacts/ContactImportPolicyTest.kt"
  "feature/contacts/src/test/java/com/bookverse/contacts/domain/contacts/BlockedNumberMatchPolicyTest.kt"
  "feature/contacts/src/test/java/com/bookverse/contacts/domain/contacts/ContactInputPolicyTest.kt"
  "feature/contacts/src/test/java/com/bookverse/contacts/domain/contacts/ContactDuplicatePolicyTest.kt"
  "feature/contacts/src/test/java/com/bookverse/contacts/domain/contacts/ContactSortPolicyTest.kt"
  "feature/contacts/src/test/java/com/bookverse/contacts/domain/contacts/ContactRecycleBinPolicyTest.kt"
  "feature/contacts/src/test/java/com/bookverse/contacts/domain/contacts/vcard/VCardCodecTest.kt"
  "core/database/src/test/java/com/bookverse/contacts/data/db/DeletedContactsMigrationSqlTest.kt"
  "app/src/test/java/com/bookverse/contacts/domain/telemetry/FunnelTelemetryTest.kt"
  "app/src/test/java/com/bookverse/contacts/service/TelecomCallActionsTest.kt"
  "feature/notes/src/test/java/com/bookverse/contacts/service/ReminderDeliveryActionTest.kt"
  "feature/dialer/src/test/java/com/bookverse/contacts/ui/screens/history/RecentPhoneMatchTest.kt"
  "feature/contacts/src/test/java/com/bookverse/contacts/ui/screens/contacts/ContactDetailsPhoneTest.kt"
  "feature/dialer/src/test/java/com/bookverse/contacts/ui/screens/history/HistoryProjectionTest.kt"
  "feature/dialer/src/test/java/com/bookverse/contacts/ui/screens/dialer/DialerContactFilterTest.kt"
  "feature/dialer/src/test/java/com/bookverse/contacts/ui/screens/dialer/DialerInputPolicyTest.kt"
  "feature/dialer/src/test/java/com/bookverse/contacts/ui/screens/dialer/DialerSearchPerformanceTest.kt"
  "feature/notes/src/test/java/com/bookverse/contacts/ui/screens/notes/NoteFolderFilterStateTest.kt"
  "feature/settings/src/test/java/com/bookverse/contacts/ui/screens/settings/OnboardingFlowControllerTest.kt"
  "feature/settings/src/test/java/com/bookverse/contacts/ui/screens/settings/PhoneStatusStateTest.kt"
  "feature/settings/src/test/java/com/bookverse/contacts/ui/screens/settings/SpeedDialSettingsPolicyTest.kt"
  "feature/settings/src/test/java/com/bookverse/contacts/ui/screens/settings/SettingsPreferencePolicyTest.kt"
  "feature/tools/src/test/java/com/bookverse/contacts/ui/screens/tools/HolidayDateFormatterTest.kt"
  "app/src/test/java/com/bookverse/contacts/ui/components/CallStateBannerTest.kt"
  "feature/tools/src/test/java/com/bookverse/contacts/ui/viewmodel/HolidaysViewModelTest.kt"
  "app/src/test/java/com/bookverse/contacts/settings/LocaleControllerTest.kt"
  "feature/games/src/test/java/com/bookverse/contacts/data/hangman/HangmanWordsTest.kt"
  "app/src/test/java/com/bookverse/contacts/ui/viewmodel/NavigationViewModelTest.kt"
  "feature/dialer/src/test/java/com/bookverse/contacts/ui/screens/dialer/SpeedDialResolverTest.kt"
  "app/src/test/java/com/bookverse/contacts/ui/viewmodel/PasswordAuthMethodContractTest.kt"
  "core/database/src/androidTest/java/com/bookverse/contacts/data/db/AppDatabaseMigrationTest.kt"
  "core/database/src/main/java/com/bookverse/contacts/data/db/LegacyDatabaseMigrations.kt"
  "core/security/src/androidTest/java/com/bookverse/contacts/security/CryptoManagerTest.kt"
  "app/src/androidTest/java/com/bookverse/contacts/service/ReminderSchedulingPerformanceTest.kt"
  "core/ui/src/androidTest/java/com/bookverse/contacts/ui/auth/PasswordAuthDialogsTest.kt"
  "app/src/androidTest/java/com/bookverse/contacts/ui/navigation/MainTabNavigationBarTest.kt"
  "feature/dialer/src/androidTest/java/com/bookverse/contacts/ui/screens/dialer/DialerNumberEditorTest.kt"
  "feature/settings/src/androidTest/java/com/bookverse/contacts/ui/screens/settings/OnboardingFlowScreenTest.kt"
  "feature/settings/src/androidTest/java/com/bookverse/contacts/ui/screens/settings/BlockedNumbersDialogsTest.kt"
  "app/src/androidTest/java/com/bookverse/contacts/ui/screens/tools/ToolsPresetSelectorTest.kt"
  "app/src/androidTest/java/com/bookverse/contacts/settings/LocaleResourceSelectionTest.kt"
  "app/src/androidTest/java/com/bookverse/contacts/ui/AccessibilityCoreContractTest.kt"
  "feature/widgets/src/androidTest/java/com/bookverse/contacts/widget/FavoritesWidgetRenderingPerformanceTest.kt"
)
for relative_path in "${required_tests[@]}"; do
  [[ -f "$repo_root/$relative_path" ]] || {
    echo "Missing quality contract test: $relative_path"
    exit 1
  }
done

"$repo_root/scripts/verify-localization.sh"
"$repo_root/scripts/verify-accent-color-contract.sh"

benchmark_manifest="app/src/benchmark/AndroidManifest.xml"
benchmark_setup_activity=\
"app/src/benchmark/java/com/bookverse/contacts/benchmark/BenchmarkSetupActivity.kt"
[[ -f "$repo_root/$benchmark_manifest" ]] || {
  echo "Missing release-like benchmark manifest: $benchmark_manifest"
  exit 1
}
[[ -f "$repo_root/$benchmark_setup_activity" ]] || {
  echo "Missing isolated synthetic-data benchmark setup: $benchmark_setup_activity"
  exit 1
}
grep -Fq 'create("benchmark")' "$repo_root/app/build.gradle.kts" || {
  echo "Missing benchmark build type."
  exit 1
}
grep -Fq 'applicationIdSuffix = ".benchmark"' "$repo_root/app/build.gradle.kts" || {
  echo "Benchmark build must remain parallel-installable."
  exit 1
}
grep -Fq '<profileable android:shell="true" />' "$repo_root/$benchmark_manifest" || {
  echo "Benchmark build must remain profileable by shell."
  exit 1
}
grep -Fq 'android:permission="android.permission.DUMP"' "$repo_root/$benchmark_manifest" || {
  echo "Benchmark setup must remain callable only by privileged development tooling."
  exit 1
}
grep -Fq 'DEVICE_STABILIZED' "$repo_root/scripts/measure-core-performance.sh" || {
  echo "Performance evidence must distinguish stabilized device runs."
  exit 1
}
grep -Fq 'PREPARE_SYNTHETIC_CONTACTS' "$repo_root/scripts/measure-core-performance.sh" || {
  echo "PERF-02 must support isolated synthetic contacts."
  exit 1
}
grep -Fq 'ALL_MANUAL_MIGRATIONS' \
  "$repo_root/core/database/src/main/java/com/bookverse/contacts/data/db/AppDatabase.kt" || {
  echo "The production database builder must register every manual migration."
  exit 1
}

room_database="$repo_root/core/database/src/main/java/com/bookverse/contacts/data/db/AppDatabase.kt"
grep -Fq 'version = 32' "$room_database" || {
  echo "Room database version must remain frozen at 32."
  exit 1
}
if grep -Eq 'version[[:space:]]*=[[:space:]]*(3[3-9]|[4-9][0-9])' "$room_database"; then
  echo "Room database version must not advance beyond 32 in this release."
  exit 1
fi
grep -Fq 'migrateEveryLegacySchema2Through7To29PreservesAvailableUserData' \
  "$repo_root/core/database/src/androidTest/java/com/bookverse/contacts/data/db/AppDatabaseMigrationTest.kt" || {
  echo "Legacy Room migration fixtures must remain as compatibility evidence."
  exit 1
}
grep -Fq 'migrateRealSchema29To30PreservesEncryptedPasswordsAndAddsFolders' \
  "$repo_root/core/database/src/androidTest/java/com/bookverse/contacts/data/db/AppDatabaseMigrationTest.kt" || {
  echo "Password folder migration evidence must preserve encrypted credential data."
  exit 1
}
grep -Fq 'migrateRealSchema30To31AddsContactRecycleBinAndPreservesUserData' \
  "$repo_root/core/database/src/androidTest/java/com/bookverse/contacts/data/db/AppDatabaseMigrationTest.kt" || {
  echo "Contact recycle bin migration evidence must preserve existing user data."
  exit 1
}
[[ -f "$repo_root/core/database/schemas/com.bookverse.contacts.data.db.AppDatabase/31.json" ]] || {
  echo "Missing exported Room schema 31."
  exit 1
}
grep -Fq 'migrateRealSchema31To32PreservesVaultAndReminderDataAndAddsNoteTables' \
  "$repo_root/core/database/src/androidTest/java/com/bookverse/contacts/data/db/AppDatabaseMigrationTest.kt" || {
  echo "Vault and notes migration evidence must preserve encrypted credentials and reminders."
  exit 1
}
[[ -f "$repo_root/core/database/schemas/com.bookverse.contacts.data.db.AppDatabase/32.json" ]] || {
  echo "Missing exported Room schema 32."
  exit 1
}

adaptive_matrix="$repo_root/scripts/run-adaptive-ui-matrix.sh"
[[ -x "$adaptive_matrix" ]] || {
  echo "Missing executable adaptive UI matrix runner."
  exit 1
}
bash -n "$adaptive_matrix"
grep -Fq 'ro.kernel.qemu' "$adaptive_matrix" || {
  echo "Adaptive display overrides must remain restricted to an emulator."
  exit 1
}
for scenario in split-width-proxy compact-landscape medium-long-locale expanded-large-text; do
  grep -Fq "$scenario" "$adaptive_matrix" || {
    echo "Adaptive UI matrix is missing $scenario."
    exit 1
  }
done

localization_guard="$repo_root/scripts/verify-localization.sh"
[[ -x "$localization_guard" ]] || {
  echo "Missing executable localization quality gate."
  exit 1
}
"$localization_guard"

echo "Quality contract and maintainability ratchets pass."
