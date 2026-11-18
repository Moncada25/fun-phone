#!/usr/bin/env python3
"""Validate localized Android string resources against the English baseline."""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Sequence, Set, Tuple


SUPPORTED_LOCALES = ("es", "de", "fr", "pt")
RESOURCE_DIR = Path("app/src/main/res")
BASELINE_RELATIVE_PATH = RESOURCE_DIR / "values" / "strings.xml"
ARRAY_TAGS = {"string-array", "integer-array", "array"}
ALL_LOCALES = frozenset(SUPPORTED_LOCALES)
PLURAL_QUANTITIES_BY_LOCALE = {
    "de": frozenset({"one", "other"}),
    "es": frozenset({"one", "many", "other"}),
    "fr": frozenset({"one", "many", "other"}),
    "pt": frozenset({"one", "many", "other"}),
}

# Exact values are allowed only when the value itself and its locale scope are listed.
# translatable="false" remains the preferred declaration for non-user-facing values.
EXACT_VALUE_ALLOWLIST: Dict[str, Tuple[str, Set[str], str]] = {
    "about_feedback": ("Feedback", ALL_LOCALES, "established UX loanword"),
    "accent_usage_retro": ("Retro", ALL_LOCALES, "style label"),
    "auth_method_pin": ("PIN", ALL_LOCALES, "authentication acronym"),
    "avatar_label_gamer": ("Gamer", ALL_LOCALES, "gaming term"),
    "breakout_title": ("Breakout", ALL_LOCALES, "named game title"),
    "brush_style_normal": ("Normal", ALL_LOCALES, "brush-mode value"),
    "collage_two_horizontal": ("2 horizontal", ALL_LOCALES, "layout value"),
    "collage_two_vertical": ("2 vertical", ALL_LOCALES, "layout value"),
    "dark_style_amoled": ("AMOLED", ALL_LOCALES, "display acronym"),
    "dialog_link_label": ("URL", ALL_LOCALES, "web acronym"),
    "expense_category_freelance": ("Freelance", ALL_LOCALES, "professional term"),
    "facebook": ("Facebook", ALL_LOCALES, "brand name"),
    "password_service_category_streaming": ("Streaming", ALL_LOCALES, "established media loanword"),
    "font_cursive": ("Cursiva", ALL_LOCALES, "font-style value"),
    "font_caveat": ("Caveat", ALL_LOCALES, "font family name"),
    "font_great_vibes": ("Great Vibes", ALL_LOCALES, "font family name"),
    "font_henny_penny": ("Henny Penny", ALL_LOCALES, "font family name"),
    "font_inter": ("Inter", ALL_LOCALES, "font family name"),
    "font_jetbrains_mono": ("JetBrains Mono", ALL_LOCALES, "font family name"),
    "font_lato": ("Lato", ALL_LOCALES, "font family name"),
    "font_lobster": ("Lobster", ALL_LOCALES, "font family name"),
    "font_manrope": ("Manrope", ALL_LOCALES, "font family name"),
    "font_monospace": ("Monospace", ALL_LOCALES, "font-style value"),
    "font_noto_sans": ("Noto Sans", ALL_LOCALES, "font family name"),
    "font_noto_serif": ("Noto Serif", ALL_LOCALES, "font family name"),
    "font_nunito": ("Nunito", ALL_LOCALES, "font family name"),
    "font_pacifico": ("Pacifico", ALL_LOCALES, "font family name"),
    "font_pixelify_sans": ("Pixelify Sans", ALL_LOCALES, "font family name"),
    "font_playfair": ("Playfair Display", ALL_LOCALES, "font family name"),
    "font_playpen": ("Playpen Sans Thai", ALL_LOCALES, "font family name"),
    "font_playwrite": ("Playwrite HU", ALL_LOCALES, "font family name"),
    "font_roboto": ("Roboto", ALL_LOCALES, "font family name"),
    "font_roboto_condensed": ("Roboto Condensed", ALL_LOCALES, "font family name"),
    "font_roboto_serif": ("Roboto Serif", ALL_LOCALES, "font family name"),
    "font_rubik": ("Rubik", ALL_LOCALES, "font family name"),
    "font_serif": ("Serif", ALL_LOCALES, "font-style value"),
    "font_urbanist": ("Urbanist", ALL_LOCALES, "font family name"),
    "font_yellowtail": ("Yellowtail", ALL_LOCALES, "font family name"),
    "fun_bird_title": ("Fun Bird", ALL_LOCALES, "named game title"),
    "instagram": ("Instagram", ALL_LOCALES, "brand name"),
    "linkedin": ("LinkedIn", ALL_LOCALES, "brand name"),
    "list_density_normal": ("Normal", ALL_LOCALES, "list-density value"),
    "memory_pairs_value": ("%1$d / %2$d", ALL_LOCALES, "placeholder-only template"),
    "memory_theme_emoji": ("Emoji \U0001f680", ALL_LOCALES, "technical media term"),
    "menstrual_calendar_cycle_range": ("%1$s - %2$s", ALL_LOCALES, "placeholder-only template"),
    "normal": ("Normal", ALL_LOCALES, "generic settings value"),
    "paint_collage": ("Collage", ALL_LOCALES, "art term"),
    "pomodoro_title": ("Pomodoro", ALL_LOCALES, "named method"),
    "puzzle_title": ("Puzzle", ALL_LOCALES, "named game title"),
    "qr_code_utility": ("QR Code", ALL_LOCALES, "technical acronym"),
    "recent_type_whatsapp": ("WhatsApp", ALL_LOCALES, "brand name"),
    "snake_title": ("Snake", ALL_LOCALES, "named game title"),
    "sudoku_title": ("Sudoku", ALL_LOCALES, "named game title"),
    "tictactoe_player_o": ("O", ALL_LOCALES, "game symbol"),
    "tictactoe_player_x": ("X", ALL_LOCALES, "game symbol"),
    "tictactoe_score_format": ("%1$s: %2$d", ALL_LOCALES, "placeholder-only template"),
    "transition_zoom_blur": ("Zoom", ALL_LOCALES, "interaction term"),
    "url_label": ("URL", ALL_LOCALES, "web acronym"),
    "doto": ("Doto", ALL_LOCALES, "font family name"),
    "font_scale_value": ("%d%%", ALL_LOCALES, "placeholder-only template"),
    "vibration_duration_value": ("%1$d ms", ALL_LOCALES, "unit-only template"),
    "vibration_pattern_double": ("Double", ALL_LOCALES, "vibration pattern value"),
    "vibration_pattern_simple": ("Simple", ALL_LOCALES, "vibration pattern value"),
    "vibration_pattern_triple": ("Triple", ALL_LOCALES, "vibration pattern value"),
    "x_twitter": ("X (Twitter)", ALL_LOCALES, "brand name and platform label"),
}

# These are valid identical words in the listed locale, not blanket exemptions for a key.
EXACT_VALUE_ALLOWLIST.update(
    {
        "avatar_label_doctor": ("Doctor", {"es"}, "Spanish lexical form"),
        "cyan": ("Cyan", {"de", "fr"}, "shared palette value"),
        "error_title": ("Error", {"es"}, "Spanish technical status term"),
        "expense_category_color_label": ("Color", {"es"}, "Spanish palette label"),
        "expense_summary_balance": ("Balance", {"es"}, "Spanish financial term"),
        "expense_total": ("Total", {"es", "fr", "pt"}, "shared financial term"),
        "general_section": ("General", {"es"}, "Spanish section label"),
        "indigo": ("Indigo", {"de", "fr"}, "shared palette value"),
        "national": ("National", {"de", "fr"}, "shared classification label"),
        "note_folders_color_label": ("Color", {"es"}, "Spanish palette label"),
        "pink": ("Pink", {"de"}, "shared palette value"),
        "reset_label": ("Reset", {"de"}, "technical action term"),
        "tag_color": ("Color", {"es"}, "Spanish palette label"),
        "theme_system": ("System", {"de"}, "technical platform value"),
        "website": ("Website", {"de"}, "web term"),
        "websites": ("Websites", {"de"}, "web term"),
    }
)

RESOURCE_START_RE = re.compile(
    r"<(?P<tag>string-array|integer-array|plurals|string|array)\b(?P<attributes>[^>]*)>"
)
NAME_ATTRIBUTE_RE = re.compile(r"\bname\s*=\s*(?P<quote>[\"'])(?P<name>.*?)(?P=quote)")
ITEM_START_RE = re.compile(r"<item\b(?P<attributes>[^>]*)>")
FORMAT_TOKEN_RE = re.compile(
    r"%%|%(?:(?P<position>\d+)\$)?[-+#0,(]*(?:\d+)?(?:\.\d+)?(?P<conversion>[a-zA-Z])"
)


@dataclass(frozen=True)
class Issue:
    path: str
    line: int
    key: str
    code: str
    message: str


@dataclass(frozen=True)
class Resource:
    name: str
    kind: str
    element: ET.Element
    line: int
    item_lines: Tuple[int, ...]


@dataclass
class ParsedFile:
    path: Path
    text: str
    root: ET.Element
    resources: Dict[str, Resource]
    duplicate_resources: Tuple[Resource, ...]
    anchor_line: int

    def key_at_line(self, line: int) -> str:
        candidates = [resource for resource in self.resources.values() if resource.line <= line]
        if not candidates:
            return "<resources>"
        return max(candidates, key=lambda resource: resource.line).name


def relative_path(path: Path, root: Path) -> str:
    try:
        return path.relative_to(root).as_posix()
    except ValueError:
        return path.as_posix()


def line_number(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def text_value(element: ET.Element) -> str:
    return "".join(element.itertext())


def parse_file(path: Path, root: Path, issues: List[Issue]) -> Optional[ParsedFile]:
    display_path = relative_path(path, root)
    try:
        text = path.read_text(encoding="utf-8")
    except OSError as error:
        issues.append(Issue(display_path, 1, "<file>", "LOC-IO", f"cannot read file: {error}"))
        return None

    replacement_lines = [
        (line, content.count("\ufffd"))
        for line, content in enumerate(text.splitlines(), 1)
        if "\ufffd" in content
    ]

    try:
        xml_root = ET.fromstring(text)
    except ET.ParseError as error:
        error_line = getattr(error, "position", (1, 0))[0]
        issues.append(Issue(display_path, error_line, "<resources>", "LOC-XML", f"invalid XML: {error}"))
        return None

    resources: List[Resource] = []
    matches = list(RESOURCE_START_RE.finditer(text))
    direct_children = list(xml_root)
    for index, element in enumerate(direct_children):
        if index >= len(matches):
            break
        match = matches[index]
        name_match = NAME_ATTRIBUTE_RE.search(match.group("attributes"))
        if name_match is None:
            continue
        next_start = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        item_matches = list(ITEM_START_RE.finditer(text, match.end(), next_start))
        item_lines = tuple(line_number(text, item.start()) for item in item_matches)
        resources.append(
            Resource(
                name=name_match.group("name"),
                kind=element.tag,
                element=element,
                line=line_number(text, match.start()),
                item_lines=item_lines,
            )
        )

    if len(resources) != len(direct_children):
        issues.append(
            Issue(
                display_path,
                1,
                "<resources>",
                "LOC-XML",
                "could not map every direct resource element to a named string resource",
            )
        )

    resource_map: Dict[str, Resource] = {}
    duplicates: List[Resource] = []
    for resource in resources:
        if resource.name in resource_map:
            duplicates.append(resource)
        else:
            resource_map[resource.name] = resource

    root_match = re.search(r"<resources\b", text)
    anchor_line = line_number(text, root_match.start()) if root_match else 1
    parsed = ParsedFile(path, text, xml_root, resource_map, tuple(duplicates), anchor_line)

    for line, count in replacement_lines:
        issues.append(
            Issue(
                display_path,
                line,
                parsed.key_at_line(line),
                "LOC-REPLACEMENT",
                f"contains {count} U+FFFD replacement character(s); restore the original text",
            )
        )

    for duplicate in parsed.duplicate_resources:
        issues.append(
            Issue(
                display_path,
                duplicate.line,
                duplicate.name,
                "LOC-DUPLICATE",
                "resource name is declared more than once in this file",
            )
        )

    return parsed


def placeholder_signature(value: str) -> Tuple[str, ...]:
    signature: List[str] = []
    implicit_position = 1
    for match in FORMAT_TOKEN_RE.finditer(value):
        if match.group(0) == "%%":
            signature.append("%%")
            continue
        position = match.group("position")
        if position is None:
            position = str(implicit_position)
            implicit_position += 1
        signature.append(f"{position}${match.group('conversion')}")
    return tuple(signature)


def signature_text(signature: Sequence[str]) -> str:
    return ", ".join(signature) if signature else "none"


def allowlist_reason(locale: str, key: str, value: str) -> Optional[str]:
    entry = EXACT_VALUE_ALLOWLIST.get(key)
    if entry is None:
        return None
    expected_value, locales, reason = entry
    if value == expected_value and locale in locales:
        return reason
    return None


def add_issue(
    issues: List[Issue],
    parsed: ParsedFile,
    root: Path,
    line: int,
    key: str,
    code: str,
    message: str,
) -> None:
    issues.append(Issue(relative_path(parsed.path, root), line, key, code, message))


def item_line(resource: Resource, index: int) -> int:
    if index < len(resource.item_lines):
        return resource.item_lines[index]
    return resource.line


def check_value(
    baseline: Resource,
    translated: Resource,
    baseline_file: ParsedFile,
    baseline_value: str,
    translated_value: str,
    locale: str,
    root: Path,
    translated_file: ParsedFile,
    issues: List[Issue],
    key: str,
    line: int,
) -> None:
    expected_signature = placeholder_signature(baseline_value)
    actual_signature = placeholder_signature(translated_value)
    if expected_signature != actual_signature:
        add_issue(
            issues,
            translated_file,
            root,
            line,
            key,
            "LOC-FORMAT",
            "format placeholders differ from English baseline "
            f"(expected: {signature_text(expected_signature)}; "
            f"found: {signature_text(actual_signature)}; "
            f"baseline: {relative_path(baseline_file.path, root)}:{baseline.line})",
        )

    if (
        baseline.element.attrib.get("translatable") != "false"
        and baseline_value
        and translated_value == baseline_value
        and allowlist_reason(locale, baseline.name, baseline_value) is None
    ):
        add_issue(
            issues,
            translated_file,
            root,
            line,
            key,
            "LOC-EXACT",
            "value is identical to the English baseline; translate it or add the exact "
            "value, locale, and reason to EXACT_VALUE_ALLOWLIST",
        )


def compare_resources(
    baseline: ParsedFile,
    translated: ParsedFile,
    locale: str,
    root: Path,
    issues: List[Issue],
) -> None:
    baseline_names = set(baseline.resources)
    translated_names = set(translated.resources)
    display_baseline = relative_path(baseline.path, root)

    for name in sorted(baseline_names - translated_names):
        resource = baseline.resources[name]
        if resource.element.attrib.get("translatable") == "false":
            continue
        add_issue(
            issues,
            translated,
            root,
            translated.anchor_line,
            name,
            "LOC-MISSING",
            f"resource is missing; add it to this locale (English baseline: "
            f"{display_baseline}:{resource.line})",
        )

    for name in sorted(translated_names - baseline_names):
        resource = translated.resources[name]
        add_issue(
            issues,
            translated,
            root,
            resource.line,
            name,
            "LOC-EXTRA",
            f"resource is not declared in the English baseline ({display_baseline})",
        )

    for name in sorted(baseline_names & translated_names):
        expected = baseline.resources[name]
        actual = translated.resources[name]
        if expected.kind != actual.kind:
            add_issue(
                issues,
                translated,
                root,
                actual.line,
                name,
                "LOC-TYPE",
                f"resource type is {actual.kind!r}; expected {expected.kind!r} "
                f"({display_baseline}:{expected.line})",
            )
            continue

        if expected.kind == "plurals":
            compare_plurals(expected, actual, baseline, translated, locale, root, issues)
        elif expected.kind in ARRAY_TAGS:
            compare_array(expected, actual, baseline, translated, locale, root, issues)
        elif expected.kind == "string":
            check_value(
                expected,
                actual,
                baseline,
                text_value(expected.element),
                text_value(actual.element),
                locale,
                root,
                translated,
                issues,
                name,
                actual.line,
            )


def compare_plurals(
    expected: Resource,
    actual: Resource,
    baseline: ParsedFile,
    translated: ParsedFile,
    locale: str,
    root: Path,
    issues: List[Issue],
) -> None:
    expected_items = list(expected.element)
    actual_items = list(actual.element)
    expected_quantities = [item.attrib.get("quantity", "<missing>") for item in expected_items]
    actual_quantities = [item.attrib.get("quantity", "<missing>") for item in actual_items]
    expected_map = {quantity: item for quantity, item in zip(expected_quantities, expected_items)}
    actual_map = {quantity: item for quantity, item in zip(actual_quantities, actual_items)}
    display_baseline = relative_path(baseline.path, root)

    for quantity, count in Counter(expected_quantities).items():
        if count > 1:
            add_issue(
                issues,
                baseline,
                root,
                expected.line,
                f"{expected.name}[{quantity}]",
                "LOC-DUPLICATE",
                "plural quantity is declared more than once in the English baseline",
            )
    for quantity, count in Counter(actual_quantities).items():
        if count > 1:
            index = actual_quantities.index(quantity)
            add_issue(
                issues,
                translated,
                root,
                item_line(actual, index),
                f"{actual.name}[{quantity}]",
                "LOC-DUPLICATE",
                "plural quantity is declared more than once",
            )

    actual_set = set(actual_map)
    required_set = PLURAL_QUANTITIES_BY_LOCALE[locale]
    for quantity in sorted(required_set - actual_set):
        add_issue(
            issues,
            translated,
            root,
            actual.line,
            f"{actual.name}[{quantity}]",
            "LOC-QUANTITY",
            f"plural quantity required for locale {locale!r} is missing "
            f"(English baseline: {display_baseline}:{expected.line})",
        )
    for quantity in sorted(actual_set - required_set):
        index = actual_quantities.index(quantity)
        add_issue(
            issues,
            translated,
            root,
            item_line(actual, index),
            f"{actual.name}[{quantity}]",
            "LOC-QUANTITY",
            f"plural quantity is not valid for locale {locale!r}",
        )

    baseline_fallback = expected_map.get("other")
    for quantity in sorted(required_set & actual_set):
        expected_item = expected_map.get(quantity, baseline_fallback)
        if expected_item is None:
            add_issue(
                issues,
                baseline,
                root,
                expected.line,
                f"{expected.name}[other]",
                "LOC-QUANTITY",
                "English baseline must define the plural fallback quantity 'other'",
            )
            continue
        actual_item = actual_map[quantity]
        actual_index = actual_items.index(actual_item)
        check_value(
            expected,
            actual,
            baseline,
            text_value(expected_item),
            text_value(actual_item),
            locale,
            root,
            translated,
            issues,
            f"{actual.name}[{quantity}]",
            item_line(actual, actual_index),
        )


def compare_array(
    expected: Resource,
    actual: Resource,
    baseline: ParsedFile,
    translated: ParsedFile,
    locale: str,
    root: Path,
    issues: List[Issue],
) -> None:
    expected_items = list(expected.element)
    actual_items = list(actual.element)
    display_baseline = relative_path(baseline.path, root)
    if len(expected_items) != len(actual_items):
        add_issue(
            issues,
            translated,
            root,
            actual.line,
            actual.name,
            "LOC-ARRAY",
            f"array has {len(actual_items)} item(s); expected {len(expected_items)} "
            f"({display_baseline}:{expected.line})",
        )

    for index, (expected_item, actual_item) in enumerate(zip(expected_items, actual_items)):
        check_value(
            expected,
            actual,
            baseline,
            text_value(expected_item),
            text_value(actual_item),
            locale,
            root,
            translated,
            issues,
            f"{actual.name}[{index}]",
            item_line(actual, index),
        )


def discover_translation_files(resource_dir: Path) -> Dict[str, Path]:
    discovered: Dict[str, Path] = {}
    for path in sorted(resource_dir.glob("values-*/strings.xml")):
        directory_name = path.parent.name
        locale = directory_name[len("values-") :]
        if locale in SUPPORTED_LOCALES:
            discovered[locale] = path
    return discovered


def sort_issues(issues: Iterable[Issue]) -> List[Issue]:
    return sorted(issues, key=lambda issue: (issue.path, issue.line, issue.code, issue.key, issue.message))


def main(argv: Sequence[str]) -> int:
    root = Path(__file__).resolve().parent.parent
    if len(argv) == 2 and argv[0] == "--root":
        root = Path(argv[1]).resolve()
    elif argv:
        print("Usage: verify-localization.py [--root REPOSITORY_ROOT]", file=sys.stderr)
        return 2

    issues: List[Issue] = []
    baseline_path = root / BASELINE_RELATIVE_PATH
    resource_dir = root / RESOURCE_DIR
    if not baseline_path.is_file():
        issues.append(
            Issue(
                relative_path(baseline_path, root),
                1,
                "<file>",
                "LOC-BASELINE",
                "English baseline is missing",
            )
        )
        parsed_baseline = None
    else:
        parsed_baseline = parse_file(baseline_path, root, issues)

    translation_paths = discover_translation_files(resource_dir) if resource_dir.is_dir() else {}
    for locale in SUPPORTED_LOCALES:
        if locale not in translation_paths:
            issues.append(
                Issue(
                    relative_path(resource_dir / f"values-{locale}" / "strings.xml", root),
                    1,
                    "<file>",
                    "LOC-LOCALE",
                    f"supported locale file is missing; expected values-{locale}/strings.xml",
                )
            )

    parsed_translations: Dict[str, ParsedFile] = {}
    for locale in SUPPORTED_LOCALES:
        path = translation_paths.get(locale)
        if path is not None:
            parsed = parse_file(path, root, issues)
            if parsed is not None:
                parsed_translations[locale] = parsed

    if parsed_baseline is not None:
        for locale in SUPPORTED_LOCALES:
            translated = parsed_translations.get(locale)
            if translated is not None:
                compare_resources(parsed_baseline, translated, locale, root, issues)

    ordered_issues = sort_issues(issues)
    baseline_display = relative_path(baseline_path, root)
    print(
        f"Localization quality gate: baseline {baseline_display}; "
        f"supported translations {', '.join(SUPPORTED_LOCALES)}"
    )
    if ordered_issues:
        print(f"FAIL: {len(ordered_issues)} issue(s).")
        for issue in ordered_issues:
            print(f"{issue.path}:{issue.line}: {issue.code}: {issue.key}: {issue.message}")
        print("Fix each referenced resource, then rerun ./scripts/verify-localization.sh.")
        return 1

    print(f"PASS: {len(parsed_translations)} translation file(s) match the English baseline.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main(sys.argv[1:]))
