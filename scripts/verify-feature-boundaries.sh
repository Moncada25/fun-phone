#!/usr/bin/env bash

set -euo pipefail

repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || {
    printf 'Run this script inside a Git repository.\n' >&2
    exit 1
}

python3 - "$repo_root/app/src/main/java/com/bookverse/contacts" <<'PY'
from pathlib import Path
import sys

source_root = Path(sys.argv[1])
failures = []


def imports_for(path):
    for file in path.rglob("*.kt"):
        for line_number, line in enumerate(file.read_text().splitlines(), start=1):
            if line.startswith("import com.bookverse.contacts."):
                yield file, line_number, line.removeprefix("import com.bookverse.contacts.")


feature_roots = {
    "organize": (
        "ui/screens/notes",
        "ui/screens/expenses",
        "data/notes",
        "data/expenses",
    ),
    "private": (
        "ui/screens/passwords",
        "data/password",
        "security",
    ),
    "leisure": (
        "ui/screens/games",
        "data/hangman",
    ),
}
owned_apis = {
    "organize": ("data.notes", "data.expenses"),
    "private": ("data.password", "security"),
    "leisure": ("data.hangman",),
}

for owner, roots in feature_roots.items():
    for relative_root in roots:
        path = source_root / relative_root
        if not path.exists():
            continue
        for file, line_number, imported in imports_for(path):
            for target, prefixes in owned_apis.items():
                if target == owner:
                    continue
                if any(imported == prefix or imported.startswith(prefix + ".") for prefix in prefixes):
                    failures.append(
                        f"{file.relative_to(source_root)}:{line_number}: "
                        f"{owner} imports {target} internals ({imported})"
                    )

dialer_roots = (
    source_root / "ui/screens/calls",
    source_root / "domain/call",
)
dialer_forbidden = (
    "data.model.Contact",
    "data.repository",
    "repository",
    "ui.viewmodel.ContactsViewModel",
)
for path in dialer_roots:
    for file, line_number, imported in imports_for(path):
        if any(imported == prefix or imported.startswith(prefix + ".") for prefix in dialer_forbidden):
            failures.append(
                f"{file.relative_to(source_root)}:{line_number}: "
                f"dialer bypasses its contact lookup boundary ({imported})"
            )

call_state_banner = source_root / "ui/components/CallStateBanner.kt"
call_state_banner_forbidden = (
    "data.repository.ContactRepository",
    "ContactRepository(",
    "kotlinx.coroutines.Dispatchers",
    "kotlinx.coroutines.withContext",
    "withContext(Dispatchers.IO)",
)
call_state_banner_text = call_state_banner.read_text(encoding="utf-8")
for forbidden in call_state_banner_forbidden:
    if forbidden in call_state_banner_text:
        failures.append(
            f"{call_state_banner.relative_to(source_root)}: "
            f"call banner bypasses its lookup boundary ({forbidden})"
        )

holidays_view_model = source_root / "ui/viewmodel/HolidaysViewModel.kt"
holidays_view_model_forbidden = (
    "android.app.Application",
    "android.content",
    "androidx.core.content.edit",
    "androidx.lifecycle.AndroidViewModel",
    "data.repository.CalendarRepository",
    "data.repository.HolidaysRepository",
    "data.repository.HolidaysRepositoryException",
    "utils.ConnectivityChecker",
    "kotlinx.coroutines.Dispatchers",
    "getSharedPreferences(",
)
holidays_view_model_text = holidays_view_model.read_text(encoding="utf-8")
for forbidden in holidays_view_model_forbidden:
    if forbidden in holidays_view_model_text:
        failures.append(
            f"{holidays_view_model.relative_to(source_root)}: "
            f"holidays ViewModel bypasses its injected ports ({forbidden})"
        )

ui_screens = source_root / "ui/screens"
for file, line_number, imported in imports_for(ui_screens):
    if imported == "data.db.AppDatabase" or imported.startswith("data.db.AppDatabase."):
        failures.append(
            f"{file.relative_to(source_root)}:{line_number}: "
            "UI screen accesses AppDatabase directly; use a feature-owned port"
        )

notes_view_model = source_root / "ui/viewmodel/NotesViewModel.kt"
notes_view_model_forbidden = (
    "data.db",
    "data.notes.NoteDao",
)
for file, line_number, imported in imports_for(notes_view_model.parent):
    if file != notes_view_model:
        continue
    if any(
        imported == prefix or imported.startswith(prefix + ".")
        for prefix in notes_view_model_forbidden
    ):
        failures.append(
            f"{file.relative_to(source_root)}:{line_number}: "
            f"NotesViewModel bypasses its repository boundary ({imported})"
        )

passwords_view_model = source_root / "ui/viewmodel/PasswordsViewModel.kt"
passwords_view_model_forbidden = (
    "data.db",
    "data.password.PasswordDao",
    "data.password.PasswordRepository",
)
for file, line_number, imported in imports_for(passwords_view_model.parent):
    if file != passwords_view_model:
        continue
    if any(
        imported == prefix or imported.startswith(prefix + ".")
        for prefix in passwords_view_model_forbidden
    ):
        failures.append(
            f"{file.relative_to(source_root)}:{line_number}: "
            f"PasswordsViewModel bypasses its store boundary ({imported})"
        )

expenses_view_model = source_root / "ui/viewmodel/ExpensesViewModel.kt"
expenses_view_model_forbidden = (
    "data.db",
    "data.expenses.ExpenseDao",
    "data.expenses.ExpensesRepository",
    "utils.FixedTransactionsBackupUtils",
)
for file, line_number, imported in imports_for(expenses_view_model.parent):
    if file != expenses_view_model:
        continue
    if any(
        imported == prefix or imported.startswith(prefix + ".")
        for prefix in expenses_view_model_forbidden
    ):
        failures.append(
            f"{file.relative_to(source_root)}:{line_number}: "
            f"ExpensesViewModel bypasses its store boundary ({imported})"
        )

contact_tag_view_model = source_root / "ui/viewmodel/ContactTagViewModel.kt"
contact_tag_view_model_forbidden = (
    "data.db",
    "data.model.ContactTagCrossRef",
    "integration.RoomContactTagStore",
)
for file, line_number, imported in imports_for(contact_tag_view_model.parent):
    if file != contact_tag_view_model:
        continue
    if any(
        imported == prefix or imported.startswith(prefix + ".")
        for prefix in contact_tag_view_model_forbidden
    ):
        failures.append(
            f"{file.relative_to(source_root)}:{line_number}: "
            f"ContactTagViewModel bypasses its store boundary ({imported})"
        )

duplicate_contacts_view_model = source_root / "ui/viewmodel/DuplicateContactsViewModel.kt"
duplicate_contacts_view_model_forbidden = (
    "android.app",
    "android.content",
    "android.provider",
    "androidx.lifecycle.AndroidViewModel",
    "contacts.core",
    "data.db",
    "data.model.IgnoredDuplicatePair",
    "integration.AndroidContactMergeGateway",
    "integration.RoomIgnoredDuplicateStore",
)
for file, line_number, imported in imports_for(duplicate_contacts_view_model.parent):
    if file != duplicate_contacts_view_model:
        continue
    if any(
        imported == prefix or imported.startswith(prefix + ".")
        for prefix in duplicate_contacts_view_model_forbidden
    ):
        failures.append(
            f"{file.relative_to(source_root)}:{line_number}: "
            f"DuplicateContactsViewModel bypasses its contacts boundary ({imported})"
        )

qr_history_view_model = source_root / "ui/viewmodel/QRHistoryViewModel.kt"
qr_history_view_model_forbidden = (
    "data.db",
    "data.qr.QRHistoryDao",
    "data.qr.QRHistoryRepository",
    "integration.QRHistoryViewModelFactory",
)
for file, line_number, imported in imports_for(qr_history_view_model.parent):
    if file != qr_history_view_model:
        continue
    if any(
        imported == prefix or imported.startswith(prefix + ".")
        for prefix in qr_history_view_model_forbidden
    ):
        failures.append(
            f"{file.relative_to(source_root)}:{line_number}: "
            f"QRHistoryViewModel bypasses its store boundary ({imported})"
        )

legacy_qr_factory = source_root / "ui/viewmodel/QRHistoryViewModelFactory.kt"
if legacy_qr_factory.exists():
    failures.append(
        f"{legacy_qr_factory.relative_to(source_root)}: "
        "QR-history Room composition must remain outside the UI package"
    )

di_guard_files = {
    source_root / "ui/viewmodel/ContactsViewModel.kt": (
        "ContactRepository(application",
        "ProfileRepository(application",
        "ContactCacheRepository(application",
        "RecentOperationsRepository(application",
        "ContactImportRepository(application",
        "ContactUsageRepository(application",
    ),
    source_root / "service/CallNotificationManager.kt": (
        "ContactRepository(",
    ),
    source_root / "ui/viewmodel/calls/IncomingCallViewModel.kt": (
        "DefaultCallRepository",
    ),
    source_root / "ui/viewmodel/calls/InCallViewModel.kt": (
        "DefaultCallRepository",
    ),
}
for file, forbidden_fragments in di_guard_files.items():
    text = file.read_text(encoding="utf-8")
    for forbidden in forbidden_fragments:
        if forbidden in text:
            failures.append(
                f"{file.relative_to(source_root)}: "
                f"bypasses AppContainer dependency injection ({forbidden})"
            )

settings_view_model = source_root / "ui/viewmodel/AppSettingsViewModel.kt"
settings_direct_accesses = settings_view_model.read_text(encoding="utf-8").count(
    "AppDataStore.getDataStore(context)"
)
if settings_direct_accesses > 1:
    failures.append(
        f"{settings_view_model.relative_to(source_root)}: "
        "settings persistence bypasses the injected AppSettingsStore"
    )

if failures:
    for failure in failures:
        print(f"FAIL: {failure}", file=sys.stderr)
    sys.exit(1)

print("Feature boundary guard passed.")
PY
