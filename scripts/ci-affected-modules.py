#!/usr/bin/env python3

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

FULL_RUN_FILES = {
    ".editorconfig",
    ".github/workflows/pr-checks.yml",
    "build.gradle.kts",
    "gradle.properties",
    "gradlew",
    "gradlew.bat",
    "release.properties",
    "scripts/ci-affected-modules.py",
    "scripts/verify-lint-ratchet.sh",
    "settings.gradle.kts",
}

FULL_RUN_PREFIXES = (
    "build-logic/",
    "gradle/",
)

NON_GRADLE_FILES = {
    ".gitignore",
    "LICENSE",
    "gradle.properties.example",
    "setup-keystore.sh",
}

NON_GRADLE_PREFIXES = (
    ".github/",
    "docs/",
    "fastlane/",
    "play-listing/",
    "screenshots/",
    "scripts/",
)

APP_MODULE = ":app"
APP_LINT_VARIANTS = ("communityDebug", "playRelease")


def git(repo: Path, *args: str) -> str:
    return subprocess.run(
        ["git", *args],
        cwd=repo,
        check=True,
        capture_output=True,
        text=True,
    ).stdout


def read_modules(repo: Path) -> dict[str, str]:
    settings = (repo / "settings.gradle.kts").read_text(encoding="utf-8")
    modules = {}
    for path in re.findall(r'include\(\s*"(:[^"]+)"\s*\)', settings):
        modules[path] = path.lstrip(":").replace(":", "/")
    if not modules:
        raise SystemExit("No Gradle modules found in settings.gradle.kts")
    return modules


def build_file(repo: Path, module_dir: str) -> str:
    return (repo / module_dir / "build.gradle.kts").read_text(encoding="utf-8")


def read_dependents(repo: Path, modules: dict[str, str]) -> dict[str, set[str]]:
    dependents: dict[str, set[str]] = {module: set() for module in modules}
    for module, module_dir in modules.items():
        for dependency in re.findall(r'project\(\s*"(:[^"]+)"\s*\)', build_file(repo, module_dir)):
            if dependency not in modules:
                raise SystemExit(f"{module} depends on unknown module {dependency}")
            dependents[dependency].add(module)
    return dependents


def owning_module(path: str, modules: dict[str, str]) -> str | None:
    matches = [module for module, module_dir in modules.items() if path.startswith(module_dir + "/")]
    return max(matches, key=lambda module: len(modules[module]), default=None)


def classify(paths: list[str], modules: dict[str, str]) -> tuple[set[str], list[str]]:
    changed_modules: set[str] = set()
    full_run_reasons: list[str] = []
    for path in paths:
        if path in FULL_RUN_FILES or path.startswith(FULL_RUN_PREFIXES):
            full_run_reasons.append(path)
            continue
        module = owning_module(path, modules)
        if module is not None:
            changed_modules.add(module)
            continue
        if "/" not in path and path.endswith(".md"):
            continue
        if path in NON_GRADLE_FILES or path.startswith(NON_GRADLE_PREFIXES):
            continue
        full_run_reasons.append(path)
    return changed_modules, full_run_reasons


def with_dependents(changed: set[str], dependents: dict[str, set[str]]) -> set[str]:
    affected = set(changed)
    pending = list(changed)
    while pending:
        for dependent in dependents[pending.pop()]:
            if dependent not in affected:
                affected.add(dependent)
                pending.append(dependent)
    return affected


def unit_test_task(repo: Path, module: str, module_dir: str) -> str | None:
    source = build_file(repo, module_dir)
    if "libs.plugins.android.test" in source:
        return None
    if "productFlavors" in source:
        return f"{module}:testCommunityDebugUnitTest"
    return f"{module}:testDebugUnitTest"


def gradle_tasks(repo: Path, affected: list[str], modules: dict[str, str]) -> list[str]:
    tasks = [f"{module}:ktlintCheck" for module in affected]
    if APP_MODULE in affected:
        tasks.append(f"{APP_MODULE}:assembleCommunityDebug")
    for module in affected:
        task = unit_test_task(repo, module, modules[module])
        if task is not None:
            tasks.append(task)
    if APP_MODULE in affected:
        tasks.extend(f"{APP_MODULE}:lint{variant[0].upper()}{variant[1:]}" for variant in APP_LINT_VARIANTS)
    return tasks


def changed_paths(repo: Path, base: str) -> list[str]:
    merge_base = git(repo, "merge-base", base, "HEAD").strip()
    output = git(repo, "diff", "--name-only", "--no-renames", merge_base, "HEAD")
    return [line for line in output.splitlines() if line]


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Select the Gradle checks a change needs: full, affected modules only, or none.",
    )
    parser.add_argument("--base", help="Base revision to diff against. Omit to plan a full run.")
    parser.add_argument("--full", action="store_true", help="Plan a full run regardless of the diff.")
    parser.add_argument("--repo", default=".", help="Repository root.")
    args = parser.parse_args()

    repo = Path(args.repo).resolve()
    modules = read_modules(repo)
    dependents = read_dependents(repo, modules)
    order = list(modules)

    if args.full or not args.base:
        mode = "full"
        reason = "full run requested"
        affected = order
    else:
        paths = changed_paths(repo, args.base)
        changed, full_run_reasons = classify(paths, modules)
        if full_run_reasons:
            mode = "full"
            reason = "build-wide or unmapped change: " + ", ".join(full_run_reasons[:5])
            affected = order
        elif changed:
            mode = "affected"
            reason = "changed modules: " + " ".join(module for module in order if module in changed)
            selected = with_dependents(changed, dependents)
            affected = [module for module in order if module in selected]
        else:
            mode = "none"
            reason = "no Gradle module changed"
            affected = []

    tasks = gradle_tasks(repo, affected, modules)
    lint_variants = list(APP_LINT_VARIANTS) if APP_MODULE in affected else []

    print(f"Gradle plan: {mode} ({reason})", file=sys.stderr)
    print(f"Modules: {' '.join(affected) or '-'}", file=sys.stderr)
    print(f"mode={mode}")
    print(f"modules={' '.join(affected)}")
    print(f"tasks={' '.join(tasks)}")
    print(f"lint_variants={' '.join(lint_variants)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
