#!/bin/bash
set -euo pipefail

# =============================================================
# Flyway Migration File Integrity Validator
# Stage 1: Checks file naming, version duplicates, and
#           detects modifications to existing migrations.
# =============================================================

MIGRATION_DIR="backend/src/main/resources/db/migration"
BASE_BRANCH="${1:-origin/main}"
EXIT_CODE=0
ERRORS=()

echo "============================================"
echo " Flyway Migration File Integrity Check"
echo " Base branch: $BASE_BRANCH"
echo "============================================"
echo ""

# -------------------------------------------------------
# Check 1: Filename format validation
# Pattern: V{number}__{AlphanumericAndUnderscores}.sql
# -------------------------------------------------------
echo "[1/3] Checking filename format..."

if ls "$MIGRATION_DIR"/V*.sql 1>/dev/null 2>&1; then
  for file in "$MIGRATION_DIR"/V*.sql; do
    basename=$(basename "$file")
    if [[ ! "$basename" =~ ^V[0-9]+__[A-Za-z0-9_]+\.sql$ ]]; then
      ERRORS+=("FILENAME_FORMAT: Invalid filename - $basename (expected pattern: V{N}__{Description}.sql)")
      EXIT_CODE=1
    fi
  done
else
  echo "  No migration files found in $MIGRATION_DIR"
fi

if ls "$MIGRATION_DIR"/*.sql 1>/dev/null 2>&1; then
  for file in "$MIGRATION_DIR"/*.sql; do
    basename=$(basename "$file")
    if [[ ! "$basename" =~ ^V ]]; then
      ERRORS+=("FILENAME_FORMAT: Non-versioned SQL file found - $basename")
      EXIT_CODE=1
    fi
  done
fi

if [[ $EXIT_CODE -eq 0 ]]; then
  echo "  OK - All filenames match expected pattern"
fi

# -------------------------------------------------------
# Check 2: Version number duplicate detection
# -------------------------------------------------------
echo "[2/3] Checking for duplicate version numbers..."

if ls "$MIGRATION_DIR"/V*.sql 1>/dev/null 2>&1; then
  duplicates=$(
    for file in "$MIGRATION_DIR"/V*.sql; do
      basename "$file" | sed 's/^V\([0-9]*\)__.*/\1/'
    done | sort -n | uniq -d
  )

  if [[ -n "$duplicates" ]]; then
    for dup in $duplicates; do
      matching_files=$(ls "$MIGRATION_DIR"/V${dup}__*.sql 2>/dev/null | xargs -I{} basename {})
      ERRORS+=("DUPLICATE_VERSION: Version $dup has multiple files: $matching_files")
    done
    EXIT_CODE=1
  else
    echo "  OK - No duplicate version numbers"
  fi
fi

# -------------------------------------------------------
# Check 3: Existing migration modification detection
# Compares against base branch to detect changes to
# migrations that already exist in the base branch.
# -------------------------------------------------------
echo "[3/3] Checking for modifications to existing migrations..."

if ! git rev-parse "$BASE_BRANCH" >/dev/null 2>&1; then
  echo "  WARNING: Cannot resolve $BASE_BRANCH - skipping modification check"
  echo "  (This may happen on first run or if fetch-depth is insufficient)"
else
  changed_files=$(git diff --name-only "$BASE_BRANCH" -- "$MIGRATION_DIR" 2>/dev/null || true)

  if [[ -n "$changed_files" ]]; then
    while IFS= read -r f; do
      if git show "$BASE_BRANCH:$f" >/dev/null 2>&1; then
        ERRORS+=("MODIFIED_MIGRATION: Existing migration modified - $(basename "$f")")
        EXIT_CODE=1
      fi
    done <<< "$changed_files"
  fi

  has_modified_error=false
  for err in "${ERRORS[@]+"${ERRORS[@]}"}"; do
    if [[ "$err" == MODIFIED_MIGRATION* ]]; then
      has_modified_error=true
      break
    fi
  done

  if [[ "$has_modified_error" == false ]]; then
    echo "  OK - No existing migrations modified"
  fi
fi

# -------------------------------------------------------
# Summary
# -------------------------------------------------------
echo ""
echo "============================================"
if [[ $EXIT_CODE -ne 0 ]]; then
  echo " FAILED - ${#ERRORS[@]} error(s) found:"
  echo "============================================"
  for err in "${ERRORS[@]}"; do
    echo "  ERROR: $err"
  done
else
  echo " PASSED - All checks passed"
  echo "============================================"
fi

exit $EXIT_CODE