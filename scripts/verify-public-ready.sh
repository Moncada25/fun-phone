#!/usr/bin/env bash

set -euo pipefail

repo_root=$(git rev-parse --show-toplevel 2>/dev/null) || {
    printf 'Run this script inside a Git repository.\n' >&2
    exit 1
}
cd "$repo_root"

current_only=false
if [ "${1:-}" = "--current-only" ]; then
    current_only=true
elif [ "$#" -gt 0 ]; then
    printf 'Usage: %s [--current-only]\n' "$0" >&2
    exit 2
fi

failures=0
warnings=0

ok() {
    printf 'OK: %s\n' "$1"
}

fail() {
    printf 'FAIL: %s\n' "$1"
    failures=$((failures + 1))
}

warn() {
    printf 'WARN: %s\n' "$1"
    warnings=$((warnings + 1))
}

for file in LICENSE README.md CONTRIBUTING.md CODE_OF_CONDUCT.md SECURITY.md \
    PRIVACY.md SETUP.md CHANGELOG.md .gitignore; do
    if [ -s "$file" ]; then
        ok "$file is present"
    else
        fail "$file is missing or empty"
    fi
done

if grep -qF "GNU General Public License" LICENSE && grep -qF "version 3" LICENSE; then
    ok "LICENSE contains GPL v3 text"
else
    fail "LICENSE does not contain the expected GPL v3 text"
fi

for pattern in "app/google-services.json" "gradle.properties.private" "local.properties" \
    "*.jks" "*.keystore" "keystore.properties"; do
    if grep -qF "$pattern" .gitignore; then
        ok ".gitignore covers $pattern"
    else
        fail ".gitignore does not cover $pattern"
    fi
done

tracked_local_config=$(git ls-files -- app/google-services.json)
if [ -n "$tracked_local_config" ]; then
    fail "local credential-bearing configuration is tracked"
else
    ok "local credential-bearing configuration is not tracked"
fi

sensitive_paths=$(git ls-files | grep -E '(^|/)(local|keystore)\.properties$|\.jks$|\.keystore$|\.pem$|\.p12$' || true)
if [ -n "$sensitive_paths" ]; then
    fail "sensitive file types are tracked"
else
    ok "no sensitive file types are tracked"
fi

scan_current() {
    local pattern=$1
    local label=$2
    local matches
    matches=$(git grep --untracked -IlE "$pattern" -- . \
        ':!scripts/verify-public-ready.sh' \
        ':!.gitleaks.toml' \
        ':!app/google-services.json.example' || true)
    if [ -n "$matches" ]; then
        fail "$label detected in current publishable files"
    else
        ok "no $label detected in current publishable files"
    fi
}

firebase_pattern='AIz''a[0-9A-Za-z_-]{30,}'
github_pattern='g''h[pousr]_[0-9A-Za-z]{20,}'
stripe_pattern='[srp]''k_(live|test)_[0-9A-Za-z]{12,}'
private_key_pattern='BEGIN[[:space:]]+(RSA|EC|OPENSSH|DSA)[[:space:]]+PRIVATE[[:space:]]+KEY'

scan_current "$firebase_pattern" "Firebase API key"
scan_current "$github_pattern" "GitHub token"
scan_current "$stripe_pattern" "Stripe key"
scan_current "$private_key_pattern" "private key"
signing_pattern='RELEASE_(STORE_PASSWORD|KEY_PASSWORD)[[:space:]]*=[[:space:]]*[^[:space:]<{][^[:space:]]{4,}'
if git grep -IqE "$signing_pattern" -- gradle.properties; then
    fail "release signing password detected in tracked gradle.properties"
else
    ok "tracked gradle.properties contains only safe build settings"
fi

if ! "$current_only"; then
    scan_history() {
        local pattern=$1
        local label=$2
        local commits
        commits=$(git log --all -G"$pattern" --format='%H' -- . \
            ':!scripts/verify-public-ready.sh' \
            ':!.gitleaks.toml' | sort -u | wc -l | tr -d ' ')
        if [ "$commits" -gt 0 ]; then
            fail "$label appears in $commits historical commit(s); rotate credentials and clean or replace history manually"
        else
            ok "no $label detected in Git history"
        fi
    }

    scan_history "$firebase_pattern" "Firebase API key"
    scan_history "$github_pattern" "GitHub token"
    scan_history "$stripe_pattern" "Stripe key"
    scan_history "$private_key_pattern" "private key"
    signing_commits=$(git log --all -G"$signing_pattern" --format='%H' -- \
        gradle.properties | sort -u | wc -l | tr -d ' ')
    if [ "$signing_commits" -gt 0 ]; then
        fail "release signing password appears in $signing_commits historical commit(s)"
    else
        ok "no release signing password detected in Gradle history"
    fi

    historical_config_count=$(git log --all --format= --name-only -- \
        app/google-services.json | sed '/^$/d' | wc -l | tr -d ' ')
    if [ "$historical_config_count" -gt 0 ]; then
        fail "credential-bearing configuration appears in Git history; history cleanup remains manual and blocking"
    else
        ok "credential-bearing configuration is absent from Git history"
    fi
else
    warn "historical scan skipped by --current-only"
fi

if ./scripts/verify-android-only.sh >/dev/null; then
    ok "Android-only source guard passes"
else
    fail "Android-only source guard fails"
fi

if [ "$failures" -gt 0 ]; then
    printf 'Result: %d failure(s), %d warning(s). Not public-ready.\n' "$failures" "$warnings"
    exit 1
fi

printf 'Result: current tree passes with %d warning(s).\n' "$warnings"
