#!/usr/bin/env bash

set -Eeuo pipefail

readonly max_errors=0
readonly max_warnings=${LINT_MAX_WARNINGS:-20}
readonly lint_run_started_at=${LINT_RUN_STARTED_AT:-}
readonly report_dir="app/build/reports"

if [[ ! "$max_warnings" =~ ^[0-9]+$ ]]; then
    printf 'LINT_MAX_WARNINGS must be a non-negative integer: %s\n' "$max_warnings" >&2
    exit 2
fi

if [[ -n "$lint_run_started_at" && ! "$lint_run_started_at" =~ ^[0-9]+$ ]]; then
    printf 'LINT_RUN_STARTED_AT must be a Unix timestamp: %s\n' "$lint_run_started_at" >&2
    exit 2
fi

report_modified_at() {
    local -r report=$1

    if stat -f '%m' "$report" >/dev/null 2>&1; then
        stat -f '%m' "$report"
    else
        stat -c '%Y' "$report"
    fi
}

verify_variant() {
    local -r variant=$1
    local -r report="$report_dir/lint-results-$variant.xml"

    if [[ ! "$variant" =~ ^[A-Za-z][A-Za-z0-9]*$ ]]; then
        printf 'Invalid lint variant: %s\n' "$variant" >&2
        return 2
    fi

    if [[ ! -f "$report" ]]; then
        printf 'Lint report for %s not found: %s\n' "$variant" "$report" >&2
        return 1
    fi

    if [[ -n "$lint_run_started_at" ]]; then
        local modified_at
        modified_at=$(report_modified_at "$report")
        if (( modified_at < lint_run_started_at )); then
            printf 'Lint report for %s is stale: %s predates this lint run.\n' \
                "$variant" "$report" >&2
            return 1
        fi
    fi

    local errors
    local fatals
    local warnings
    errors=$(grep -c 'severity="Error"' "$report" || true)
    fatals=$(grep -c 'severity="Fatal"' "$report" || true)
    warnings=$(grep -c 'severity="Warning"' "$report" || true)

    printf 'Lint ratchet (%s): %d errors, %d fatal, %d warnings; limits are %d errors and %d warnings.\n' \
        "$variant" "$errors" "$fatals" "$warnings" "$max_errors" "$max_warnings"

    if (( fatals > 0 || errors > max_errors || warnings > max_warnings )); then
        return 1
    fi
}

if (( $# == 0 )); then
    variants=(communityDebug)
else
    variants=("$@")
fi
for variant in "${variants[@]}"; do
    verify_variant "$variant"
done
