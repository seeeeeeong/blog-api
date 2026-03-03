#!/bin/bash
# Blue/Green 무중단 배포 스크립트 (EC2에서 실행)
# 사용법: bash /home/ec2-user/app/deploy.sh <ECR_IMAGE>
set -euo pipefail

NEW_IMAGE=$1
APP_DIR=/home/ec2-user/app
ACTIVE_FILE=$APP_DIR/.active_color
COMPOSE_FILE=docker-compose-prod.yml

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

is_container_running() {
  local name=$1
  [ "$(docker inspect -f '{{.State.Running}}' "$name" 2>/dev/null || echo "false")" = "true" ]
}

if [ ! -f "$ACTIVE_FILE" ] \
  || ! is_container_running blog-postgres \
  || ! is_container_running blog-redis \
  || ! is_container_running blog-caddy; then
  echo "Bootstrap: ensuring postgres/redis/caddy are running..."
  "${COMPOSE[@]}" up -d postgres redis
  "${COMPOSE[@]}" up -d --no-deps caddy
fi

# 비활성 슬롯에 새 이미지 배포
if [ "$NEXT" = "blue" ]; then
  ECR_IMAGE_BLUE="$NEW_IMAGE" ECR_IMAGE_GREEN="${ECR_IMAGE_GREEN:-scratch}" \
    "${COMPOSE[@]}" up -d --no-deps app-blue
else
  ECR_IMAGE_GREEN="$NEW_IMAGE" ECR_IMAGE_BLUE="${ECR_IMAGE_BLUE:-scratch}" \
    "${COMPOSE[@]}" up -d --no-deps app-green
fi

# Health check 대기 (최대 60초, 5초 간격 × 12회)
echo "Waiting for app-$NEXT to be healthy..."
for i in $(seq 1 12); do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' "blog-api-$NEXT" 2>/dev/null || echo "unknown")
  echo "  [$i/12] status=$STATUS"
  if [ "$STATUS" = "healthy" ]; then
    echo "Health check passed."
    break
  fi
  if [ "$i" = "12" ]; then
    echo "Health check timed out. Rolling back app-$NEXT."
    "${COMPOSE[@]}" stop "app-$NEXT" || true
    exit 1
  fi
  sleep 5
done

# 이전 슬롯 중지
"${COMPOSE[@]}" stop "app-$ACTIVE" 2>/dev/null || true
echo "$NEXT" > "$ACTIVE_FILE"

# 오래된 이미지 정리
docker image prune -f

echo "=== Deployed to app-$NEXT successfully ==="
