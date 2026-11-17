#!/usr/bin/env python3
"""Export reviewed listing text and an asset index without contacting Play Console."""

import argparse
import json
import re
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
LANGUAGES = {
    "en": "English",
    "es": "Español",
    "de": "Deutsch",
    "fr": "Français",
    "pt": "Português",
}
FIELDS = {"title.txt": 30, "short-description.txt": 80, "full-description.txt": 4000}


def section(document, heading):
    match = re.search(r"^## " + re.escape(heading) + r"\n(.*?)(?=^## |\Z)", document, re.M | re.S)
    if not match:
        raise ValueError("Missing listing section: " + heading)
    return match.group(1)


def listing_fields(document, heading):
    body = section(document, heading)
    title = re.search(r"^Title: (.+)$", body, re.M)
    descriptions = re.split(r"^\*\*[^\n]+\*\*\s*$", body, flags=re.M)
    if not title or len(descriptions) != 3:
        raise ValueError("Expected a title, short description and full description: " + heading)
    # Markdown wraps paragraphs for review; plain text is ready to paste into Play Console.
    paragraphs = []
    for paragraph in descriptions[2].strip().split("\n\n"):
        lines = paragraph.splitlines()
        paragraphs.append("\n".join(lines) if any(line.startswith("• ") for line in lines) else " ".join(lines))
    values = dict(zip(FIELDS, [title.group(1).strip(), descriptions[1].strip(), "\n\n".join(paragraphs)]))
    for name, limit in FIELDS.items():
        value = values[name]
        if not value or len(value) > limit:
            raise ValueError(f"{heading}/{name}: {len(value)} characters, expected 1–{limit}")
        if name != "full-description.txt" and "\n" in value:
            raise ValueError("Unexpected line break: " + heading + "/" + name)
    return values


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Check the committed export instead of writing it")
    parser.add_argument("--source", type=Path, default=ROOT / "PLAY_STORE_LISTING.md")
    args = parser.parse_args()
    document = args.source.read_text(encoding="utf-8")
    campaign = json.loads((ROOT / "screenshots/store-v3/campaign.json").read_text(encoding="utf-8"))
    expected_languages = set(LANGUAGES)
    if set(campaign["export"]["locales"]) != expected_languages:
        raise ValueError("The campaign must cover all five app languages")
    if set(campaign["feature"]["subtitle"]) != expected_languages:
        raise ValueError("The feature graphic needs copy for all five app languages")
    for index, slide in enumerate(campaign["slides"], 1):
        captions = [" ".join(slide["headline"][language]) for language in LANGUAGES]
        row = "| " + str(index) + " | " + " | ".join(captions) + " |"
        if row not in document.splitlines():
            raise ValueError("Screenshot order table differs from campaign copy: " + slide["id"])
    fixtures = json.loads((ROOT / "app/src/benchmark/assets/store-fixtures.json").read_text(encoding="utf-8"))
    fixture_sizes = {"noteTitles": 8, "noteBodies": 8, "noteTags": 4, "checklist": 3, "vaultNames": 8, "expenseCategories": 6}
    for language in LANGUAGES:
        if any(len(fixtures[language].get(key, [])) != count for key, count in fixture_sizes.items()):
            raise ValueError("Incomplete native capture fixtures: " + language)
    capture_ids = {capture["id"] for capture in campaign["captures"]}
    looks = {(capture["style"], capture["dark"]) for capture in campaign["captures"]}
    if {style for style, _ in looks} != {"MODERN", "MINIMAL", "RETRO"} or {dark for _, dark in looks} != {True, False}:
        raise ValueError("The campaign must show the three visual styles in light and dark")
    if len({capture["accent"] for capture in campaign["captures"]}) < 3:
        raise ValueError("The campaign must show several accent colours")
    manifest = {"source": "PLAY_STORE_LISTING.md", "pathsRelativeTo": "repository root", "locales": []}
    expected = {}
    for language, heading in LANGUAGES.items():
        locale = campaign["export"]["playLocales"][language]
        values = listing_fields(document, heading)
        for name, value in values.items():
            expected[Path("play-listing") / locale / name] = value + "\n"
        images = []
        for slide in campaign["slides"]:
            for field in ("headline", "subtitle", "label", "alt"):
                if set(slide[field]) != expected_languages or not slide[field][language]:
                    raise ValueError("Incomplete campaign translation: " + slide["id"] + "/" + field)
            raw_scenes = [slide["scene"]] + ([slide["paired"]] if slide.get("paired") else []) + slide.get("trio", [])
            if any(scene not in capture_ids for scene in raw_scenes):
                raise ValueError("Slide uses an undeclared capture: " + slide["id"])
            for scene in raw_scenes:
                raw = ROOT / "screenshots/store-v3/raw" / language / (scene + ".png")
                if not raw.is_file():
                    raise ValueError("Missing native capture: " + str(raw))
            images.append(f"screenshots/store-v3/{language}/{slide['id']}.jpg")
        feature = f"screenshots/store-v3/{language}/feature-graphic.jpg"
        if len(images) != 8:
            raise ValueError("Each language requires eight phone screenshots")
        for image in images + [feature]:
            if not (ROOT / image).is_file():
                raise ValueError("Missing Play image: " + image)
        manifest["locales"].append({
            "language": language,
            "playLocale": locale,
            "textDirectory": "play-listing/" + locale,
            "phoneScreenshots": images,
            "featureGraphic": feature,
        })
        counts = ", ".join(f"{name}={len(value)}/{FIELDS[name]}" for name, value in values.items())
        print(locale + ": " + counts)
    expected[Path("play-listing/manifest.json")] = json.dumps(manifest, ensure_ascii=False, indent=2) + "\n"
    for relative, content in expected.items():
        target = ROOT / relative
        if args.check:
            if not target.is_file() or target.read_text(encoding="utf-8") != content:
                raise ValueError("Stale Play export; run scripts/export-play-listing.py: " + str(relative))
        else:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(content, encoding="utf-8")
    print("Play listing export " + ("verified" if args.check else "written") + " for all five languages.")


if __name__ == "__main__":
    main()
