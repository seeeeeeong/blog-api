#!/bin/bash
# Blue/Green 무중단 배포 스크립트 (EC2에서 실행)
# 사용법: bash /home/ec2-user/app/deploy.sh <ECR_IMAGE>
set -euo pipefail

NEW_IMAGE=$1
APP_DIR=/home/ec2-user/app
ACTIVE_FILE=$APP_DIR/.active_color
COMPOSE_FILE=docker-compose-prod.yml
ENV_FILE=$APP_DIR/.env.prod
HEALTH_CHECK_RETRIES=24
HEALTH_CHECK_INTERVAL_SECONDS=5

if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose -f "$COMPOSE_FILE")
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose -f "$COMPOSE_FILE")
else
  echo "docker compose command not found."
  exit 1
fi

ACTIVE=$(cat "$ACTIVE_FILE" 2>/dev/null || echo "blue")
[ "$ACTIVE" = "blue" ] && NEXT="green" || NEXT="blue"

echo "=== Blue/Green Deploy ==="
echo "Active slot : $ACTIVE"
echo "Next slot   : $NEXT"
echo "New image   : $NEW_IMAGE"

# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 \
  | docker login --username AWS --password-stdin "$(echo "$NEW_IMAGE" | cut -d/ -f1)"

cd "$APP_DIR"

if [ ! -f "$ENV_FILE" ]; then
  echo "Missing env file: $ENV_FILE"
  echo "Please provide deployment env vars before running deploy."
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

required_vars=(
  DB_PASSWORD
  JWT_SECRET
  GITHUB_CLIENT_ID
  GITHUB_CLIENT_SECRET
  CORS_ALLOWED_ORIGINS
  OAUTH_REDIRECT_URL
  OAUTH_CALLBACK_URL
  AWS_S3_BUCKET
  AWS_CLOUDFRONT_DOMAIN
)
missing_vars=()

for var_name in "${required_vars[@]}"; do
  if [ -z "${!var_name:-}" ]; then
    missing_vars+=("$var_name")
  fi
done

if [ "${#missing_vars[@]}" -gt 0 ]; then
  echo "Missing required env vars in $ENV_FILE: ${missing_vars[*]}"
  exit 1
fi

is_container_running() {
  local name=$1
  [ "$(docker inspect -f '{{.State.Running}}' "$name" 2>/dev/null || echo "false")" = "true" ]
}

print_failure_diagnostics() {
  local container_name=$1
  echo "----- Deployment Diagnostics: ${container_name} -----"
  docker ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Image}}'
  echo "----- ${container_name} inspect(state/health) -----"
  docker inspect "$container_name" --format '{{json .State}}' 2>/dev/null || true
  echo "----- ${container_name} inspect(restart-count) -----"
  docker inspect "$container_name" --format '{{.RestartCount}}' 2>/dev/null || true
  echo "----- ${container_name} logs (tail 200) -----"
  docker logs --tail 200 "$container_name" 2>/dev/null || true
  echo "----- ${container_name} previous logs (tail 200) -----"
  docker logs --previous --tail 200 "$container_name" 2>/dev/null || true
  echo "----- ${container_name} /app/logs/blog-api-error.log (tail 200) -----"
  docker exec "$container_name" sh -lc 'test -f /app/logs/blog-api-error.log && tail -n 200 /app/logs/blog-api-error.log || echo "no /app/logs/blog-api-error.log"' 2>/dev/null || true
  echo "----- ${container_name} /app/logs/blog-api-prod.log (tail 200) -----"
  docker exec "$container_name" sh -lc 'test -f /app/logs/blog-api-prod.log && tail -n 200 /app/logs/blog-api-prod.log || echo "no /app/logs/blog-api-prod.log"' 2>/dev/null || true
  echo "----- blog-postgres logs (tail 80) -----"
  docker logs --tail 80 blog-postgres 2>/dev/null || true
  echo "----- blog-redis logs (tail 80) -----"
  docker logs --tail 80 blog-redis 2>/dev/null || true
}

if [ ! -f "$ACTIVE_FILE" ] \
  || ! is_container_running blog-postgres \
  || ! is_container_running blog-redis \
  || ! is_container_running blog-caddy; then
  echo "Bootstrap: ensuring postgres/redis/caddy are running..."
  "${COMPOSE[@]}" up -d postgres redis >/tmp/deploy-bootstrap.log 2>&1 || {
    tail -n 200 /tmp/deploy-bootstrap.log || true
    exit 1
  }
  "${COMPOSE[@]}" up -d --no-deps caddy >/tmp/deploy-caddy.log 2>&1 || {
    tail -n 200 /tmp/deploy-caddy.log || true
    exit 1
  }
fi

# 비활성 슬롯에 새 이미지 배포
APP_UP_LOG=/tmp/deploy-app-up.log
if [ "$NEXT" = "blue" ]; then
  set +e
  ECR_IMAGE_BLUE="$NEW_IMAGE" ECR_IMAGE_GREEN="${ECR_IMAGE_GREEN:-scratch}" \
    "${COMPOSE[@]}" up -d --no-deps app-blue >"$APP_UP_LOG" 2>&1
  APP_UP_EXIT=$?
  set -e
else
  set +e
  ECR_IMAGE_GREEN="$NEW_IMAGE" ECR_IMAGE_BLUE="${ECR_IMAGE_BLUE:-scratch}" \
    "${COMPOSE[@]}" up -d --no-deps app-green >"$APP_UP_LOG" 2>&1
  APP_UP_EXIT=$?
  set -e
fi

if [ "$APP_UP_EXIT" -ne 0 ]; then
  echo "Failed to start app-$NEXT via docker compose."
  tail -n 200 "$APP_UP_LOG" || true
  exit 1
fi

# Health check 대기 (기본 120초, 5초 간격 × 24회)
echo "Waiting for app-$NEXT to be healthy..."
for i in $(seq 1 "$HEALTH_CHECK_RETRIES"); do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' "blog-api-$NEXT" 2>/dev/null || echo "unknown")
  echo "  [$i/$HEALTH_CHECK_RETRIES] status=$STATUS"
  if [ "$STATUS" = "healthy" ]; then
    echo "Health check passed."
    break
  fi
  if [ "$i" = "$HEALTH_CHECK_RETRIES" ]; then
    echo "Health check timed out. Rolling back app-$NEXT."
    print_failure_diagnostics "blog-api-$NEXT"
    "${COMPOSE[@]}" stop "app-$NEXT" || true
    exit 1
  fi
  sleep "$HEALTH_CHECK_INTERVAL_SECONDS"
done

# 이전 슬롯 중지
"${COMPOSE[@]}" stop "app-$ACTIVE" 2>/dev/null || true
echo "$NEXT" > "$ACTIVE_FILE"

# 오래된 이미지 정리
docker image prune -f

echo "=== Deployed to app-$NEXT successfully ==="
