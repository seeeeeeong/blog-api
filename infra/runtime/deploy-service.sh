#!/bin/bash
set -euo pipefail

SERVICE="${1:?Usage: deploy-service.sh <blog-api|devlog-archive> <ecr-image>}"
NEW_IMAGE="${2:?Usage: deploy-service.sh <blog-api|devlog-archive> <ecr-image>}"

RUNTIME_DIR="/opt/services"
COMPOSE_FILE="$RUNTIME_DIR/docker-compose.yml"
COMPOSE_ENV="$RUNTIME_DIR/compose.env"
ENV_DIR="$RUNTIME_DIR/env"
AWS_REGION="${SSM_AWS_REGION:-ap-northeast-2}"
BLOG_SSM="${BLOG_API_SSM_PREFIX:-/blog/prod}"
DEVLOG_SSM="${DEVLOG_ARCHIVE_SSM_PREFIX:-/devlog-archive/prod}"

case "$SERVICE" in
  blog-api|devlog-archive) ;;
  *) echo "Unknown service: $SERVICE (allowed: blog-api, devlog-archive)"; exit 1 ;;
esac

# --- Helpers ---
compose() {
  if docker compose version &>/dev/null; then
    docker compose --env-file "$COMPOSE_ENV" -f "$COMPOSE_FILE" "$@"
  else
    docker-compose --env-file "$COMPOSE_ENV" -f "$COMPOSE_FILE" "$@"
  fi
}

ssm_get() { aws ssm get-parameter --region "$AWS_REGION" --name "$1" --with-decryption --query Parameter.Value --output text 2>/dev/null; }
ssm_get_or() { ssm_get "$1" 2>/dev/null || printf '%s' "$2"; }

wait_healthy() {
  local container="$1" retries="${2:-60}"
  for i in $(seq 1 "$retries"); do
    local s; s=$(docker inspect --format='{{if .State.Health}}{{.State.Health.Status}}{{else}}{{if .State.Running}}running{{else}}stopped{{end}}{{end}}' "$container" 2>/dev/null || echo "missing")
    echo "  [$i/$retries] $container=$s"
    [[ "$s" == "healthy" || "$s" == "running" ]] && return 0
    sleep 5
  done
  return 1
}

fail_diagnostics() {
  local c="$1"
  echo "--- $c diagnostics ---"
  docker logs --tail 100 "$c" 2>/dev/null || true
}

write_env() {
  local tmp; tmp=$(mktemp)
  cat > "$tmp"; chmod 600 "$tmp"; mv "$tmp" "$1"
}

# --- Setup ---
echo "=== Deploy: $SERVICE ($NEW_IMAGE) ==="
mkdir -p "$RUNTIME_DIR/bin" "$ENV_DIR" "$RUNTIME_DIR/data/devlog-postgres" "$RUNTIME_DIR/data/caddy" "$RUNTIME_DIR/config/caddy"

REGISTRY="${NEW_IMAGE%%/*}"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY"

CUR_BLOG=$(grep '^BLOG_API_IMAGE=' "$COMPOSE_ENV" 2>/dev/null | cut -d= -f2- || echo "busybox:stable")
CUR_DEVLOG=$(grep '^DEVLOG_ARCHIVE_IMAGE=' "$COMPOSE_ENV" 2>/dev/null | cut -d= -f2- || echo "busybox:stable")
CF_SECRET=$(ssm_get "$BLOG_SSM/CLOUDFRONT_SECRET")

# --- Service deploy ---
case "$SERVICE" in
  blog-api)
    write_env "$ENV_DIR/blog-api.env" <<EOF
SPRING_PROFILES_ACTIVE=prod
DB_HOST=$(ssm_get "$BLOG_SSM/DB_HOST")
DB_PORT=5432
DB_NAME=blog_db
DB_USERNAME=postgres
DB_PASSWORD=$(ssm_get "$BLOG_SSM/DB_PASSWORD")
JWT_SECRET=$(ssm_get "$BLOG_SSM/JWT_SECRET")
CORS_ALLOWED_ORIGINS=$(ssm_get "$BLOG_SSM/CORS_ALLOWED_ORIGINS")
AWS_S3_BUCKET=$(ssm_get "$BLOG_SSM/AWS_S3_BUCKET")
AWS_REGION=$(ssm_get_or "$BLOG_SSM/AWS_REGION" ap-northeast-2)
AWS_CLOUDFRONT_DOMAIN=$(ssm_get "$BLOG_SSM/AWS_CLOUDFRONT_DOMAIN")
SPRING_JPA_HIBERNATE_DDL_AUTO=$(ssm_get_or "$BLOG_SSM/SPRING_JPA_HIBERNATE_DDL_AUTO" update)
JWT_ACCESS_EXPIRATION=$(ssm_get_or "$BLOG_SSM/JWT_ACCESS_EXPIRATION" 3600000)
JWT_REFRESH_EXPIRATION=$(ssm_get_or "$BLOG_SSM/JWT_REFRESH_EXPIRATION" 1209600000)
JAVA_TOOL_OPTIONS=-Xms64m -Xmx300m -XX:MaxMetaspaceSize=150m
EOF
    write_env "$COMPOSE_ENV" <<EOF
BLOG_API_IMAGE=$NEW_IMAGE
DEVLOG_ARCHIVE_IMAGE=$CUR_DEVLOG
CLOUDFRONT_SECRET=$CF_SECRET
EOF
    compose up -d --no-deps --force-recreate blog-api
    wait_healthy blog-api || { fail_diagnostics blog-api; exit 1; }
    ;;

  devlog-archive)
    DA_PW=$(ssm_get "$DEVLOG_SSM/DB_PASSWORD")
    write_env "$ENV_DIR/devlog-db.env" <<EOF
POSTGRES_DB=devlog_archive
POSTGRES_USER=devlog
POSTGRES_PASSWORD=$DA_PW
EOF
    write_env "$ENV_DIR/devlog-archive.env" <<EOF
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://devlog-db:5432/devlog_archive
DB_HOST=devlog-db
DB_PORT=5432
DB_NAME=devlog_archive
DB_USERNAME=devlog
DB_PASSWORD=$DA_PW
OPENAI_API_KEY=$(ssm_get "$DEVLOG_SSM/OPENAI_API_KEY")
OPENAI_EMBEDDING_MODEL=$(ssm_get_or "$DEVLOG_SSM/OPENAI_EMBEDDING_MODEL" text-embedding-3-small)
ALLOWED_ORIGIN=$(ssm_get "$DEVLOG_SSM/ALLOWED_ORIGIN")
ADMIN_API_KEY=$(ssm_get "$DEVLOG_SSM/ADMIN_API_KEY")
EOF
    write_env "$COMPOSE_ENV" <<EOF
BLOG_API_IMAGE=$CUR_BLOG
DEVLOG_ARCHIVE_IMAGE=$NEW_IMAGE
CLOUDFRONT_SECRET=$CF_SECRET
EOF
    compose up -d devlog-db
    wait_healthy devlog-postgres 30 || { fail_diagnostics devlog-postgres; exit 1; }
    compose up -d --no-deps --force-recreate devlog-archive
    wait_healthy devlog-archive || { fail_diagnostics devlog-archive; exit 1; }
    ;;
esac

compose up -d caddy
docker image prune -f &>/dev/null || true
echo "=== $SERVICE deployed ==="
