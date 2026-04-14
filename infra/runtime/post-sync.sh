#!/bin/bash
# Post-sync setup: called after runtime files are unpacked to /opt/services
set -euo pipefail

echo "=== Post-sync setup ==="

# Ensure directories
mkdir -p /opt/services/bin /opt/services/env \
  /opt/services/data/{devlog-postgres,caddy} \
  /opt/services/config/caddy

# Move deploy script to bin
mv -f /opt/services/deploy-service.sh /opt/services/bin/deploy-service.sh 2>/dev/null || true
chmod +x /opt/services/bin/deploy-service.sh
chmod +x /opt/services/post-sync.sh 2>/dev/null || true

# Ensure placeholder env files exist
for f in /opt/services/env/blog-api.env /opt/services/env/devlog-archive.env /opt/services/env/devlog-db.env; do
  [ -f "$f" ] || touch "$f"
done

# Detect compose command
if docker compose version >/dev/null 2>&1; then
  COMPOSE_CMD="docker compose"
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE_CMD="docker-compose"
else
  echo "No compose command found"
  exit 1
fi
echo "Using: $COMPOSE_CMD"

# Reload Caddy if running
if docker inspect blog-caddy >/dev/null 2>&1; then
  echo "Reloading Caddy..."
  docker exec blog-caddy caddy reload --config /etc/caddy/Caddyfile || true
fi

# Reconcile shared services only (app services are managed by deploy-service.sh)
if [ -f /opt/services/compose.env ]; then
  echo "Reconciling shared services..."
  $COMPOSE_CMD --env-file /opt/services/compose.env -f /opt/services/docker-compose.yml up -d caddy node-exporter
fi

echo "=== Post-sync completed at $(date -u +%Y-%m-%dT%H:%M:%SZ) ==="
