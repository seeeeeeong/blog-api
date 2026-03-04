#!/bin/bash
set -euo pipefail

APP_DIR="/home/ec2-user/app"
ENV_FILE="$APP_DIR/.env.prod"
BACKUP_DIR="${BACKUP_DIR:-/home/ec2-user/backups/postgres}"
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

if [ -z "${AWS_S3_BUCKET:-}" ]; then
  echo "AWS_S3_BUCKET is required"
  exit 1
fi

mkdir -p "$BACKUP_DIR"

TIMESTAMP=$(date -u +%Y%m%dT%H%M%SZ)
DATE_PATH=$(date -u +%Y/%m/%d)
BACKUP_FILE="$BACKUP_DIR/blog_db_${TIMESTAMP}.sql.gz"
S3_KEY="$BACKUP_S3_PREFIX/$DATE_PATH/$(basename "$BACKUP_FILE")"

if ! docker inspect -f '{{.State.Running}}' blog-postgres >/dev/null 2>&1; then
  echo "blog-postgres container is not running"
  exit 1
fi

docker exec blog-postgres pg_dump -U postgres -d blog_db | gzip > "$BACKUP_FILE"
aws s3 cp "$BACKUP_FILE" "s3://$AWS_S3_BUCKET/$S3_KEY" --region "$AWS_REGION"

# 로컬 백업은 14일만 유지
find "$BACKUP_DIR" -type f -name 'blog_db_*.sql.gz' -mtime +14 -delete || true

echo "backup completed: s3://$AWS_S3_BUCKET/$S3_KEY"
