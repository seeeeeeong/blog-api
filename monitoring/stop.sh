#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "모니터링 종료 중..."

docker compose -f "$SCRIPT_DIR/docker-compose.local.yml" down

pkill -f "19090:localhost:8080" 2>/dev/null || true
pkill -f "19092:localhost:9100" 2>/dev/null || true

echo "완료"
