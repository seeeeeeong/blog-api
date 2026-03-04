#!/bin/bash
set -euo pipefail

# Usage:
#   bash scripts/ops/seed_ssm_parameters.sh /path/to/env-file [prefix] [region]
# Example:
#   bash scripts/ops/seed_ssm_parameters.sh .env.prod /blog/prod ap-northeast-2

ENV_FILE="${1:-}"
SSM_PREFIX="${2:-/blog/prod}"
AWS_REGION="${3:-ap-northeast-2}"

if [ -z "$ENV_FILE" ] || [ ! -f "$ENV_FILE" ]; then
  echo "Usage: $0 <env-file> [prefix] [region]"
  echo "env-file not found: $ENV_FILE"
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

required_keys=(
  DB_PASSWORD
  JWT_SECRET
  GITHUB_CLIENT_ID
  GITHUB_CLIENT_SECRET
  CORS_ALLOWED_ORIGINS
  OAUTH_REDIRECT_URL
  OAUTH_CALLBACK_URL
  AWS_S3_BUCKET
  AWS_CLOUDFRONT_DOMAIN
  CLOUDFRONT_SECRET
)

optional_keys=(
  SPRING_JPA_HIBERNATE_DDL_AUTO
  JWT_ACCESS_EXPIRATION
  JWT_REFRESH_EXPIRATION
  AWS_REGION
  BACKUP_S3_PREFIX
)

put_param() {
  local key=$1
  local value=${!key:-}

  if [ -z "$value" ]; then
    echo "skip: $key (empty)"
    return 0
  fi

  aws ssm put-parameter \
    --region "$AWS_REGION" \
    --name "$SSM_PREFIX/$key" \
    --type SecureString \
    --value "$value" \
    --overwrite >/dev/null

  echo "upserted: $SSM_PREFIX/$key"
}

for key in "${required_keys[@]}"; do
  if [ -z "${!key:-}" ]; then
    echo "missing required key in env file: $key"
    exit 1
  fi
  put_param "$key"
done

for key in "${optional_keys[@]}"; do
  put_param "$key"
done

echo "done: seeded SSM parameters under $SSM_PREFIX"
