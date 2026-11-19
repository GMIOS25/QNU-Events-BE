#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SQL_FILE="$SCRIPT_DIR/reset-demo-data.sql"

if [ ! -f "$SQL_FILE" ]; then
  echo "[ERROR] SQL file not found: $SQL_FILE" >&2
  exit 1
fi

if [ -f "$REPO_ROOT/.env" ]; then
  while IFS='=' read -r key value; do
    case "$key" in
      DB_USERNAME) [ -z "${DB_USERNAME:-}" ] && DB_USERNAME="${value%$'\r'}" ;;
      DB_NAME) [ -z "${DB_NAME:-}" ] && DB_NAME="${value%$'\r'}" ;;
    esac
  done < "$REPO_ROOT/.env"
fi

DB_USER="${DB_USERNAME:-qnu_events}"
DB_TARGET="${DB_NAME:-qnu_events}"

echo "=============================================="
echo " QNU-Events: Reset Demo Database Data"
echo "=============================================="

(cd "$REPO_ROOT" && docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "$DB_USER" -d "$DB_TARGET" < "$SQL_FILE")

echo "[SUCCESS] Demo transaction data has been reset cleanly."
echo "Accounts (owner01, owner02, sv001, sv002, sv003, admin01) and classes remain intact."
