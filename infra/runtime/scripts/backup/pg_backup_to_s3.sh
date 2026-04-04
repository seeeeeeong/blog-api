#!/bin/bash
set -euo pipefail

ENV_FILE="/opt/services/env/blog-api.env"
BACKUP_DIR="${BACKUP_DIR:-/opt/services/data/backups/postgres}"
AWS_REGION="${AWS_REGION:-ap-northeast-2}"
BACKUP_S3_PREFIX="${BACKUP_S3_PREFIX:-backups/postgres}"

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
DB_NAME="${DB_NAME:-blog_db}"
DB_USERNAME="${DB_USERNAME:-postgres}"

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date -u +%Y%m%dT%H%M%SZ)
DATE_PATH=$(date -u +%Y/%m/%d)
BACKUP_FILE="$BACKUP_DIR/blog_db_${TIMESTAMP}.sql.gz"
S3_KEY="$BACKUP_S3_PREFIX/$DATE_PATH/$(basename "$BACKUP_FILE")"

PGPASSWORD="$DB_PASSWORD" pg_dump \
  -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USERNAME" -d "$DB_NAME" \
  | gzip > "$BACKUP_FILE"

aws s3 cp "$BACKUP_FILE" "s3://$AWS_S3_BUCKET/$S3_KEY" --region "$AWS_REGION"

# 로컬 백업은 14일만 유지
find "$BACKUP_DIR" -type f -name 'blog_db_*.sql.gz' -mtime +14 -delete || true

echo "backup completed: s3://$AWS_S3_BUCKET/$S3_KEY"
