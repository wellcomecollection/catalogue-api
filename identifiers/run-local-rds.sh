#!/usr/bin/env bash
# Start the Identifiers API locally against the REAL Aurora ID Registry
# (identifiers-v2-serverless-test) via the RDS Data API.
#
# READ-ONLY: this backend only issues SELECTs and never writes or seeds the DB.
#
# Usage:
#   ./run-local-rds.sh                 # uses .env.rds.local, port 8000
#   PORT=8731 ./run-local-rds.sh       # pick a port
#   ./run-local-rds.sh path/to.env     # use a different env file
set -euo pipefail
cd "$(dirname "$0")"

ENV_FILE="${1:-.env.rds.local}"
if [[ ! -f "$ENV_FILE" ]]; then
  echo "Missing $ENV_FILE — copy .env.rds.local.example and fill it in:" >&2
  echo "  cp .env.rds.local.example .env.rds.local" >&2
  exit 1
fi

# Load the backend config (AWS profile/region + RDS ARNs + backend selection).
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

# Force the RDS backend regardless of what the env file says.
export IDENTIFIERS_BACKEND=rds

# A stray VIRTUAL_ENV (e.g. a repo-root .venv) can steer uv to the wrong
# environment; drop it so uv resolves this project's own env.
unset VIRTUAL_ENV || true

echo "Starting Identifiers API against RDS (read-only): $RDS_RESOURCE_ARN"
exec uv run python src/adapters/run_local.py
