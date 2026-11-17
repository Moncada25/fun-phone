#!/usr/bin/env bash

set -euo pipefail

repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || {
    printf 'Run this script inside a Git repository.\n' >&2
    exit 1
}

python3 - "$repo_root/app/src/main/AndroidManifest.xml" <<'PY'
import sys
import xml.etree.ElementTree as ET

manifest_path = sys.argv[1]
android = "{http://schemas.android.com/apk/res/android}"
root = ET.parse(manifest_path).getroot()
application = root.find("application")

failures = []


def require(condition, message):
    if not condition:
        failures.append(message)


permissions = {
    element.get(android + "name")
    for element in root.findall("uses-permission")
}
required_permissions = {
    "android.permission.ANSWER_PHONE_CALLS",
    "android.permission.CALL_PHONE",
    "android.permission.FOREGROUND_SERVICE_PHONE_CALL",
    "android.permission.MANAGE_OWN_CALLS",
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.READ_CALL_LOG",
    "android.permission.READ_PHONE_STATE",
    "android.permission.USE_FULL_SCREEN_INTENT",
}
for permission in sorted(required_permissions):
    require(permission in permissions, f"missing telecom permission: {permission}")


def component(tag, name):
    for element in application.findall(tag):
        if element.get(android + "name") == name:
            return element
    return None


main_activity = component("activity", ".MainActivity")
require(main_activity is not None, "MainActivity declaration is missing")
if main_activity is not None:
    actions = {
        action.get(android + "name")
        for intent_filter in main_activity.findall("intent-filter")
        for action in intent_filter.findall("action")
    }
    for action in (
        "android.intent.action.CALL",
        "android.intent.action.CALL_BUTTON",
        "android.intent.action.DIAL",
        "android.intent.action.VIEW",
    ):
        require(action in actions, f"MainActivity is missing {action}")
    tel_schemes = {
        data.get(android + "scheme")
        for intent_filter in main_activity.findall("intent-filter")
        for data in intent_filter.findall("data")
    }
    require("tel" in tel_schemes, "MainActivity is missing the tel scheme")

in_call_service = component("service", ".service.FunPhoneInCallService")
require(in_call_service is not None, "FunPhoneInCallService declaration is missing")
if in_call_service is not None:
    require(
        in_call_service.get(android + "permission") == "android.permission.BIND_INCALL_SERVICE",
        "FunPhoneInCallService must require BIND_INCALL_SERVICE",
    )
    require(
        in_call_service.get(android + "exported") == "true",
        "FunPhoneInCallService must be exported to Telecom",
    )
    service_actions = {
        action.get(android + "name")
        for intent_filter in in_call_service.findall("intent-filter")
        for action in intent_filter.findall("action")
    }
    require(
        "android.telecom.InCallService" in service_actions,
        "FunPhoneInCallService is missing the Telecom intent action",
    )
    metadata = {
        element.get(android + "name"): element.get(android + "value")
        for element in in_call_service.findall("meta-data")
    }
    require(
        metadata.get("android.telecom.IN_CALL_SERVICE_UI") == "true",
        "FunPhoneInCallService must declare the in-call UI",
    )
    require(
        metadata.get("android.telecom.IN_CALL_SERVICE_RINGING") == "true",
        "FunPhoneInCallService must declare ringing support",
    )

screening_service = component("service", ".service.FunPhoneCallScreeningService")
require(screening_service is not None, "FunPhoneCallScreeningService declaration is missing")
if screening_service is not None:
    require(
        screening_service.get(android + "permission") == "android.permission.BIND_SCREENING_SERVICE",
        "FunPhoneCallScreeningService must require BIND_SCREENING_SERVICE",
    )
    require(
        screening_service.get(android + "exported") == "true",
        "FunPhoneCallScreeningService must be exported to Telecom",
    )
    screening_actions = {
        action.get(android + "name")
        for intent_filter in screening_service.findall("intent-filter")
        for action in intent_filter.findall("action")
    }
    require(
        "android.telecom.CallScreeningService" in screening_actions,
        "FunPhoneCallScreeningService is missing the Telecom screening action",
    )

incoming_activity = component("activity", ".ui.activities.IncomingCallActivity")
require(incoming_activity is not None, "IncomingCallActivity declaration is missing")
if incoming_activity is not None:
    for attribute, expected in (
        ("exported", "false"),
        ("excludeFromRecents", "true"),
        ("showWhenLocked", "true"),
        ("turnScreenOn", "true"),
    ):
        require(
            incoming_activity.get(android + attribute) == expected,
            f"IncomingCallActivity must set android:{attribute}={expected}",
        )

for service_name in (
    ".service.IncomingCallService",
    ".service.OngoingCallService",
):
    service = component("service", service_name)
    require(service is not None, f"{service_name} declaration is missing")
    if service is not None:
        require(
            service.get(android + "exported") == "false",
            f"{service_name} must not be exported",
        )
        require(
            service.get(android + "foregroundServiceType") == "phoneCall",
            f"{service_name} must use the phoneCall foreground-service type",
        )

if failures:
    for failure in failures:
        print(f"FAIL: {failure}", file=sys.stderr)
    sys.exit(1)

print("Telecom manifest contract guard passed.")
PY
