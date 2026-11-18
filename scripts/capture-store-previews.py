#!/usr/bin/env python3
"""Capture the store-preview scenes declared in screenshots/store-v3/campaign.json.

Drives only the isolated benchmark capture host on the device named by ANDROID_SERIAL.
"""

import html
import json
import os
import re
import subprocess
import sys
import time
import unicodedata
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CAMPAIGN = json.loads((ROOT / "screenshots/store-v3/campaign.json").read_text(encoding="utf-8"))
FIXTURES = json.loads((ROOT / "app/src/benchmark/assets/store-fixtures.json").read_text(encoding="utf-8"))
PACKAGE = CAMPAIGN["capture"]["package"]
ACTIVITY = CAMPAIGN["capture"]["activity"]
SERIAL = os.environ.get("ANDROID_SERIAL") or sys.exit("Select the authorized synthetic-data device with ANDROID_SERIAL")
OUTPUT = Path(os.environ.get("STORE_CAPTURE_DIR", ROOT / "screenshots/store-v3/raw"))
DEVICE_XML = "/data/local/tmp/fun-phone-store-preview.xml"
NODE = re.compile(r'<node[^>]*?text="([^"]*)"[^>]*?content-desc="([^"]*)"[^>]*?bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"')


def adb(*args, binary=False):
    result = subprocess.run(["adb", "-s", SERIAL, *args], check=True, capture_output=True)
    return result.stdout if binary else result.stdout.decode("utf-8", "replace")


def shell(*args):
    return adb("shell", *args)


def foreground():
    lines = [line for line in shell("dumpsys", "activity", "activities").splitlines() if "topResumedActivity=" in line]
    return bool(lines) and f"{PACKAGE}/{ACTIVITY}" in lines[0]


def hierarchy():
    shell("uiautomator", "dump", DEVICE_XML)
    return shell("cat", DEVICE_XML)


def app_string(language, name):
    folder = "values" if language == "en" else "values-" + language
    pattern = r'<string name="' + re.escape(name) + r'"[^>]*>(.*?)</string>'
    for module in ("core/*", "feature/*"):
        for path in sorted(ROOT.glob(f"{module}/src/main/res/{folder}/strings.xml")):
            match = re.search(pattern, path.read_text(encoding="utf-8"), re.S)
            if match:
                return html.unescape(match.group(1)).replace("\\'", "'")
    raise SystemExit(f"Missing string resource {name} for {language}")


def pattern_for(value, language):
    if value.startswith("string:"):
        return "^" + re.escape(app_string(language, value[len("string:"):])) + "$"
    return value


def tap(action, language, size):
    """Dynamic first (text or content description), verified coordinates as the fallback."""
    if "fixtureFirst" in action:
        # The list is alphabetical, so the first fixture name is the row at the top of the screen.
        first = min(FIXTURES[language][action["fixtureFirst"]], key=lambda name: unicodedata.normalize("NFD", name).casefold())
        field, pattern = "text", "^" + re.escape(first) + "$"
    elif "desc" in action:
        field, pattern = "desc", pattern_for(action["desc"], language)
    else:
        field, pattern = "text", pattern_for(action["text"], language)
    for _attempt in range(8):
        for text, desc, left, top, right, bottom in NODE.findall(hierarchy()):
            if re.search(pattern, html.unescape(desc if field == "desc" else text)):
                shell("input", "tap", str((int(left) + int(right)) // 2), str((int(top) + int(bottom)) // 2))
                return
        time.sleep(0.3)
    if "at" not in action:
        raise SystemExit(f"Capture aborted: no element matches {pattern!r} ({language})")
    shell("input", "tap", str(round(action["at"][0] * size[0])), str(round(action["at"][1] * size[1])))


def main():
    languages = sys.argv[1:] or CAMPAIGN["export"]["locales"]
    for language in languages:
        if language not in CAMPAIGN["export"]["locales"]:
            sys.exit("Unsupported capture language: " + language)
    # Personal phones are never a capture target unless the owner opts in explicitly.
    if shell("getprop", "ro.kernel.qemu").strip() != "1" and os.environ.get("STORE_CAPTURE_ALLOW_PHYSICAL") != "1":
        sys.exit("Capture aborted: ANDROID_SERIAL is not an emulator (set STORE_CAPTURE_ALLOW_PHYSICAL=1 to override).")
    only = set(filter(None, os.environ.get("STORE_CAPTURE_ONLY", "").split(",")))
    size = tuple(int(value) for value in re.search(r"(\d+)x(\d+)", shell("wm", "size")).groups())
    shell("settings", "put", "secure", "immersive_mode_confirmations", "confirmed")
    try:
        for language in languages:
            (OUTPUT / language).mkdir(parents=True, exist_ok=True)
            for capture in CAMPAIGN["captures"]:
                if only and capture["id"] not in only:
                    continue
                shell("am", "force-stop", PACKAGE)
                shell(
                    "am", "start", "-W", "-n", f"{PACKAGE}/{ACTIVITY}",
                    "--es", "scene", capture["scene"], "--es", "language", language,
                    "--es", "style", capture["style"], "--ez", "dark", str(capture["dark"]).lower(),
                    "--es", "darkStyle", capture["darkStyle"], "--es", "accent", capture["accent"],
                    "--es", "number", capture.get("number", "555"),
                )
                marker = f"store-preview:{capture['scene']}:{language}"
                for _attempt in range(30):
                    if foreground() and marker in hierarchy():
                        break
                    time.sleep(0.3)
                else:
                    sys.exit(f"Capture aborted: fixture scene {language}/{capture['id']} is not ready in foreground.")
                for action in capture.get("actions", []):
                    tap(action, language, size)
                    time.sleep(0.9)
                if capture.get("totp"):
                    # Avoid the last seconds of a 30 s window, when the code turns to its expiring state.
                    while time.time() % 30 > 20:
                        time.sleep(0.5)
                time.sleep(1.2)
                if not foreground():
                    sys.exit(f"Capture aborted: {language}/{capture['id']} left the foreground.")
                if re.search(r"android\.permission\.READ_(CONTACTS|CALL_LOG): granted=true", shell("dumpsys", "package", PACKAGE)):
                    sys.exit("Capture aborted: benchmark has personal-provider permissions.")
                png = adb("exec-out", "screencap", "-p", binary=True)
                if not png.startswith(b"\x89PNG"):
                    sys.exit(f"Capture aborted: invalid screenshot for {language}/{capture['id']}")
                (OUTPUT / language / f"{capture['id']}.png").write_bytes(png)
                print(f"Captured {language}/{capture['id']}", flush=True)
    finally:
        shell("rm", "-f", DEVICE_XML)
        shell("am", "force-stop", PACKAGE)


if __name__ == "__main__":
    main()
