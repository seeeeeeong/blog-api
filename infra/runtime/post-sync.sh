#!/bin/bash
set -euo pipefail

echo "=== Post-sync ==="

mkdir -p /opt/services/{bin,env,data/devlog-postgres,data/caddy,config/caddy}

mv -f /opt/services/deploy-service.sh /opt/services/bin/deploy-service.sh 2>/dev/null || true
chmod +x /opt/services/bin/deploy-service.sh /opt/services/post-sync.sh 2>/dev/null || true

for f in blog-api.env devlog-archive.env devlog-db.env; do
  [ -f "/opt/services/env/$f" ] || touch "/opt/services/env/$f"
done

# Reload Caddy config if running
docker exec blog-caddy caddy reload --config /etc/caddy/Caddyfile 2>/dev/null || true

# Reconcile shared services
if [ -f /opt/services/compose.env ]; then
  if docker compose version &>/dev/null; then
    docker compose --env-file /opt/services/compose.env -f /opt/services/docker-compose.yml up -d caddy node-exporter
  else
    docker-compose --env-file /opt/services/compose.env -f /opt/services/docker-compose.yml up -d caddy node-exporter
  fi
fi

echo "=== Post-sync done ==="
