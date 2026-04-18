#!/bin/bash
set -euo pipefail

SERVICE="${1:?Usage: deploy-service.sh <blog-api> <ecr-image>}"
NEW_IMAGE="${2:?Usage: deploy-service.sh <blog-api> <ecr-image>}"

RUNTIME_DIR="/opt/services"
COMPOSE_FILE="$RUNTIME_DIR/docker-compose.yml"
COMPOSE_ENV="$RUNTIME_DIR/compose.env"
ENV_DIR="$RUNTIME_DIR/env"
AWS_REGION="${SSM_AWS_REGION:-ap-northeast-2}"
BLOG_SSM="${BLOG_API_SSM_PREFIX:-/blog/prod}"

case "$SERVICE" in
  blog-api) ;;
  *) echo "Unknown service: $SERVICE (allowed: blog-api)"; exit 1 ;;
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
mkdir -p "$RUNTIME_DIR/bin" "$ENV_DIR" "$RUNTIME_DIR/data/caddy" "$RUNTIME_DIR/config/caddy"

REGISTRY="${NEW_IMAGE%%/*}"
aws ecr get-login-password --region "$AWS_REGION" | docker login --username AWS --password-stdin "$REGISTRY"

CF_SECRET=$(ssm_get "$BLOG_SSM/CLOUDFRONT_SECRET")

# --- blog-api deploy ---
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
SENTRY_DSN=$(ssm_get_or "$BLOG_SSM/SENTRY_DSN" "")
SENTRY_ENVIRONMENT=$(ssm_get_or "$BLOG_SSM/SENTRY_ENVIRONMENT" prod)
APP_VERSION=$(ssm_get_or "$BLOG_SSM/APP_VERSION" unknown)
BLOG_AI_BASE_URL=$(ssm_get_or "$BLOG_SSM/BLOG_AI_BASE_URL" "http://blog-ai:8081")
BLOG_AI_INTERNAL_KEY=$(ssm_get "$BLOG_SSM/BLOG_AI_INTERNAL_KEY")
JAVA_TOOL_OPTIONS=-Xms64m -Xmx300m -XX:MaxMetaspaceSize=150m
EOF

write_env "$COMPOSE_ENV" <<EOF
BLOG_API_IMAGE=$NEW_IMAGE
CLOUDFRONT_SECRET=$CF_SECRET
EOF

compose up -d --no-deps --force-recreate blog-api
wait_healthy blog-api || { fail_diagnostics blog-api; exit 1; }

compose up -d caddy
docker image prune -f &>/dev/null || true
echo "=== $SERVICE deployed ==="
