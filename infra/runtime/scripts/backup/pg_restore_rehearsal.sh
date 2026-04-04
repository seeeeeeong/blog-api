#!/bin/bash
set -euo pipefail

ENV_FILE="/opt/services/env/blog-api.env"
TMP_DIR="${TMP_DIR:-/tmp/blog-restore-rehearsal}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
BACKUP_S3_PREFIX="${BACKUP_S3_PREFIX:-backups/postgres}"
TEST_DB="blog_restore_rehearsal"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing env file: $ENV_FILE"
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

: "${DB_HOST:?DB_HOST is required}"
: "${DB_PASSWORD:?DB_PASSWORD is required}"
: "${AWS_S3_BUCKET:?AWS_S3_BUCKET is required}"

DB_PORT="${DB_PORT:-5432}"
DB_USERNAME="${DB_USERNAME:-postgres}"

mkdir -p "$TMP_DIR"

LATEST_KEY=$(aws s3 ls "s3://$AWS_S3_BUCKET/$BACKUP_S3_PREFIX/" --recursive --region "$AWS_REGION" | awk '{print $4}' | sort | tail -n 1)
if [ -z "$LATEST_KEY" ]; then
  echo "No backup found in s3://$AWS_S3_BUCKET/$BACKUP_S3_PREFIX/"
  exit 1
fi

LATEST_FILE="$TMP_DIR/$(basename "$LATEST_KEY")"
aws s3 cp "s3://$AWS_S3_BUCKET/$LATEST_KEY" "$LATEST_FILE" --region "$AWS_REGION"

PGPASSWORD="$DB_PASSWORD" psql \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d postgres -v ON_ERROR_STOP=1 \
  -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = '$TEST_DB';" \
  -c "DROP DATABASE IF EXISTS $TEST_DB;" \
  -c "CREATE DATABASE $TEST_DB;"

gunzip -c "$LATEST_FILE" | PGPASSWORD="$DB_PASSWORD" psql \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$TEST_DB" -v ON_ERROR_STOP=1 >/dev/null

TABLE_COUNT=$(PGPASSWORD="$DB_PASSWORD" psql \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$TEST_DB" -Atc \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public';")

if [ "${TABLE_COUNT:-0}" -le 0 ]; then
  echo "Restore rehearsal failed: restored DB has no public tables"
  exit 1
fi

PGPASSWORD="$DB_PASSWORD" psql \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d postgres -v ON_ERROR_STOP=1 \
  -c "DROP DATABASE IF EXISTS $TEST_DB;" >/dev/null

rm -f "$LATEST_FILE"

echo "restore rehearsal completed from s3://$AWS_S3_BUCKET/$LATEST_KEY (public tables=$TABLE_COUNT)"
