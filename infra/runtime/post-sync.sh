#!/bin/bash
# Post-sync setup: called after runtime files are unpacked to /opt/services
set -euo pipefail

echo "=== Post-sync setup ==="

# Ensure directories
mkdir -p /opt/services/bin /opt/services/env \
  /opt/services/data/{redis,devlog-postgres,caddy,backups/postgres} \
  /opt/services/config/caddy \
  /opt/services/scripts/backup \
  /opt/services/scripts/monitoring

# Move deploy script to bin
mv -f /opt/services/deploy-service.sh /opt/services/bin/deploy-service.sh 2>/dev/null || true
chmod +x /opt/services/bin/deploy-service.sh
chmod +x /opt/services/scripts/backup/*.sh /opt/services/scripts/monitoring/*.sh 2>/dev/null || true
chmod +x /opt/services/post-sync.sh 2>/dev/null || true

# Install/update cron jobs
CRON_TMP=$(mktemp)
(crontab -l 2>/dev/null || true) \
  | grep -v pg_backup_to_s3.sh \
  | grep -v pg_restore_rehearsal.sh \
  | grep -v publish_container_restart_metric.sh > "$CRON_TMP" || true
{
  cat "$CRON_TMP"
  echo "15 3 * * * /opt/services/scripts/backup/pg_backup_to_s3.sh >> /var/log/blog-pg-backup.log 2>&1"
  echo "30 4 * * 0 /opt/services/scripts/backup/pg_restore_rehearsal.sh >> /var/log/blog-pg-restore-rehearsal.log 2>&1"
  echo "*/5 * * * * /opt/services/scripts/monitoring/publish_container_restart_metric.sh >> /var/log/blog-container-restart-metric.log 2>&1"
} | crontab -
rm -f "$CRON_TMP"
echo "Cron jobs updated."

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
  $COMPOSE_CMD --env-file /opt/services/compose.env -f /opt/services/docker-compose.yml up -d caddy redis redis-exporter node-exporter
fi

echo "=== Post-sync completed at $(date -u +%Y-%m-%dT%H:%M:%SZ) ==="
