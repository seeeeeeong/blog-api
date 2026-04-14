#!/bin/bash
# 공용 배포 스크립트
# 사용법: deploy-service.sh <service-name> <ecr-image>
# 허용 서비스: blog-api, devlog-archive
set -euo pipefail

SERVICE="${1:?Usage: deploy-service.sh <blog-api|devlog-archive> <ecr-image>}"
NEW_IMAGE="${2:?Usage: deploy-service.sh <blog-api|devlog-archive> <ecr-image>}"

RUNTIME_DIR="/opt/services"
COMPOSE_FILE="$RUNTIME_DIR/docker-compose.yml"
COMPOSE_ENV_FILE="$RUNTIME_DIR/compose.env"
ENV_DIR="$RUNTIME_DIR/env"

HEALTH_CHECK_RETRIES="${HEALTH_CHECK_RETRIES:-60}"
HEALTH_CHECK_INTERVAL_SECONDS="${HEALTH_CHECK_INTERVAL_SECONDS:-5}"
SSM_AWS_REGION="${SSM_AWS_REGION:-ap-northeast-2}"

# SSM prefix per service (overridable via env)
BLOG_API_SSM_PREFIX="${BLOG_API_SSM_PREFIX:-/blog/prod}"
DEVLOG_ARCHIVE_SSM_PREFIX="${DEVLOG_ARCHIVE_SSM_PREFIX:-/devlog-archive/prod}"

# =============================================
# Compose command detection
# =============================================
if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE")
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE")
else
  echo "docker compose command not found"
  exit 1
fi

# =============================================
# Helper functions
# =============================================
fetch_ssm_parameter() {
  local name="$1"
  local value=""

  if ! value=$(aws ssm get-parameter \
    --region "$SSM_AWS_REGION" \
    --name "$name" \
    --with-decryption \
    --query 'Parameter.Value' \
    --output text 2>/dev/null); then
    return 1
  fi

  if [ "$value" = "None" ] || [ -z "$value" ]; then
    return 1
  fi

  printf '%s' "$value"
}

fetch_ssm_or_default() {
  local name="$1"
  local default_value="$2"
  fetch_ssm_parameter "$name" 2>/dev/null || printf '%s' "$default_value"
}

wait_for_health() {
  local container="$1"
  local retries="${2:-$HEALTH_CHECK_RETRIES}"
  local interval="${3:-$HEALTH_CHECK_INTERVAL_SECONDS}"

  for i in $(seq 1 "$retries"); do
    local status
    status=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}{{if .State.Running}}running{{else}}stopped{{end}}{{end}}' "$container" 2>/dev/null || echo "missing")
    echo "  [$i/$retries] $container status=$status"
    if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then
      return 0
    fi
    sleep "$interval"
  done

  return 1
}

write_atomic() {
  local target="$1"
  local tmp
  tmp=$(mktemp)
  cat > "$tmp"
  chmod 600 "$tmp"
  mv "$tmp" "$target"
}

update_compose_env() {
  local blog_api_image="$1"
  local devlog_archive_image="$2"
  local cloudfront_secret="$3"

  write_atomic "$COMPOSE_ENV_FILE" <<EOF
BLOG_API_IMAGE=$blog_api_image
DEVLOG_ARCHIVE_IMAGE=$devlog_archive_image
CLOUDFRONT_SECRET=$cloudfront_secret
EOF
}

print_failure_diagnostics() {
  local container="$1"
  echo "----- Deployment Diagnostics: $container -----"
  docker ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Image}}'
  echo "----- $container inspect (state) -----"
  docker inspect "$container" --format '{{json .State}}' 2>/dev/null || true
  echo "----- $container inspect (restart count) -----"
  docker inspect "$container" --format '{{.RestartCount}}' 2>/dev/null || true
  echo "----- $container logs (tail 200) -----"
  docker logs --tail 200 "$container" 2>/dev/null || true
  echo "----- $container previous logs (tail 200) -----"
  docker logs --previous --tail 200 "$container" 2>/dev/null || true
}

# =============================================
# Validate service name
# =============================================
case "$SERVICE" in
  blog-api|devlog-archive) ;;
  *)
    echo "Unsupported service: $SERVICE"
    echo "Allowed: blog-api, devlog-archive"
    exit 1
    ;;
esac

echo "=== Deploy: $SERVICE ==="
echo "Image: $NEW_IMAGE"

# =============================================
# Ensure directories exist
# =============================================
mkdir -p "$RUNTIME_DIR/bin" "$ENV_DIR" \
  "$RUNTIME_DIR/data/devlog-postgres" \
  "$RUNTIME_DIR/data/caddy" \
  "$RUNTIME_DIR/config/caddy"

# =============================================
# ECR login
# =============================================
REGISTRY="$(echo "$NEW_IMAGE" | cut -d/ -f1)"
aws ecr get-login-password --region "$SSM_AWS_REGION" \
  | docker login --username AWS --password-stdin "$REGISTRY"

# =============================================
# Read current compose.env (preserve other service's image)
# =============================================
CURRENT_BLOG_API_IMAGE="$(grep '^BLOG_API_IMAGE=' "$COMPOSE_ENV_FILE" 2>/dev/null | cut -d= -f2- || true)"
CURRENT_DEVLOG_ARCHIVE_IMAGE="$(grep '^DEVLOG_ARCHIVE_IMAGE=' "$COMPOSE_ENV_FILE" 2>/dev/null | cut -d= -f2- || true)"
: "${CURRENT_BLOG_API_IMAGE:=public.ecr.aws/docker/library/busybox:stable}"
: "${CURRENT_DEVLOG_ARCHIVE_IMAGE:=public.ecr.aws/docker/library/busybox:stable}"

# =============================================
# Shared: CloudFront secret (used by Caddy)
# =============================================
CLOUDFRONT_SECRET="$(fetch_ssm_parameter "$BLOG_API_SSM_PREFIX/CLOUDFRONT_SECRET")"

# =============================================
# Service-specific logic
# =============================================
case "$SERVICE" in
  blog-api)
    P="$BLOG_API_SSM_PREFIX"

    # Fetch required SSM parameters
    DB_HOST="$(fetch_ssm_parameter "$P/DB_HOST")"
    DB_PASSWORD="$(fetch_ssm_parameter "$P/DB_PASSWORD")"
    JWT_SECRET="$(fetch_ssm_parameter "$P/JWT_SECRET")"
    CORS_ALLOWED_ORIGINS="$(fetch_ssm_parameter "$P/CORS_ALLOWED_ORIGINS")"
    AWS_S3_BUCKET="$(fetch_ssm_parameter "$P/AWS_S3_BUCKET")"
    AWS_CLOUDFRONT_DOMAIN="$(fetch_ssm_parameter "$P/AWS_CLOUDFRONT_DOMAIN")"

    # Fetch optional SSM parameters with defaults
    AWS_REGION="$(fetch_ssm_or_default "$P/AWS_REGION" ap-northeast-2)"
    SPRING_JPA_HIBERNATE_DDL_AUTO="$(fetch_ssm_or_default "$P/SPRING_JPA_HIBERNATE_DDL_AUTO" update)"
    JWT_ACCESS_EXPIRATION="$(fetch_ssm_or_default "$P/JWT_ACCESS_EXPIRATION" 3600000)"
    JWT_REFRESH_EXPIRATION="$(fetch_ssm_or_default "$P/JWT_REFRESH_EXPIRATION" 1209600000)"

    # Write service env file
    write_atomic "$ENV_DIR/blog-api.env" <<EOF
SPRING_PROFILES_ACTIVE=prod
DB_HOST=$DB_HOST
DB_PORT=5432
DB_NAME=blog_db
DB_USERNAME=postgres
DB_PASSWORD=$DB_PASSWORD
JWT_SECRET=$JWT_SECRET
CORS_ALLOWED_ORIGINS=$CORS_ALLOWED_ORIGINS
AWS_S3_BUCKET=$AWS_S3_BUCKET
AWS_REGION=$AWS_REGION
AWS_CLOUDFRONT_DOMAIN=$AWS_CLOUDFRONT_DOMAIN
SPRING_JPA_HIBERNATE_DDL_AUTO=$SPRING_JPA_HIBERNATE_DDL_AUTO
JWT_ACCESS_EXPIRATION=$JWT_ACCESS_EXPIRATION
JWT_REFRESH_EXPIRATION=$JWT_REFRESH_EXPIRATION
JAVA_TOOL_OPTIONS=-Xms64m -Xmx300m -XX:MaxMetaspaceSize=150m
EOF

    # Update compose.env with new blog-api image
    update_compose_env "$NEW_IMAGE" "$CURRENT_DEVLOG_ARCHIVE_IMAGE" "$CLOUDFRONT_SECRET"

    # Deploy blog-api
    echo "Deploying blog-api..."
    "${COMPOSE[@]}" up -d --no-deps --force-recreate blog-api
    if ! wait_for_health blog-api; then
      print_failure_diagnostics blog-api
      exit 1
    fi
    ;;

  devlog-archive)
    P="$DEVLOG_ARCHIVE_SSM_PREFIX"

    # Fetch required SSM parameters
    DA_DB_PASSWORD="$(fetch_ssm_parameter "$P/DB_PASSWORD")"
    OPENAI_API_KEY="$(fetch_ssm_parameter "$P/OPENAI_API_KEY")"
    ALLOWED_ORIGIN="$(fetch_ssm_parameter "$P/ALLOWED_ORIGIN")"
    ADMIN_API_KEY="$(fetch_ssm_parameter "$P/ADMIN_API_KEY")"

    # Fetch optional SSM parameters with defaults
    OPENAI_EMBEDDING_MODEL="$(fetch_ssm_or_default "$P/OPENAI_EMBEDDING_MODEL" text-embedding-3-small)"

    # Write database env file
    write_atomic "$ENV_DIR/devlog-db.env" <<EOF
POSTGRES_DB=devlog_archive
POSTGRES_USER=devlog
POSTGRES_PASSWORD=$DA_DB_PASSWORD
EOF

    # Write service env file
    write_atomic "$ENV_DIR/devlog-archive.env" <<EOF
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://devlog-db:5432/devlog_archive
DB_HOST=devlog-db
DB_PORT=5432
DB_NAME=devlog_archive
DB_USERNAME=devlog
DB_PASSWORD=$DA_DB_PASSWORD
OPENAI_API_KEY=$OPENAI_API_KEY
OPENAI_EMBEDDING_MODEL=$OPENAI_EMBEDDING_MODEL
ALLOWED_ORIGIN=$ALLOWED_ORIGIN
ADMIN_API_KEY=$ADMIN_API_KEY
EOF

    # Update compose.env with new devlog-archive image
    update_compose_env "$CURRENT_BLOG_API_IMAGE" "$NEW_IMAGE" "$CLOUDFRONT_SECRET"

    # Ensure dependency is up
    echo "Ensuring devlog-db is running..."
    "${COMPOSE[@]}" up -d devlog-db
    wait_for_health devlog-postgres 30 3

    # Deploy devlog-archive
    echo "Deploying devlog-archive..."
    "${COMPOSE[@]}" up -d --no-deps --force-recreate devlog-archive
    if ! wait_for_health devlog-archive; then
      print_failure_diagnostics devlog-archive
      exit 1
    fi
    ;;
esac

# =============================================
# Ensure Caddy is running (shared reverse proxy)
# =============================================
echo "Ensuring caddy is running..."
"${COMPOSE[@]}" up -d caddy

# =============================================
# Cleanup old images
# =============================================
docker image prune -f >/dev/null 2>&1 || true

echo "=== $SERVICE deployed successfully ==="
