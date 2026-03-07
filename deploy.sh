#!/bin/bash
# 배포 스크립트 (EC2에서 실행)
# 사용법: bash /home/ec2-user/app/deploy.sh <ECR_IMAGE>
set -euo pipefail

NEW_IMAGE=$1
APP_DIR=/home/ec2-user/app
COMPOSE_FILE=docker-compose-prod.yml
ENV_FILE=$APP_DIR/.env.prod
HEALTH_CHECK_RETRIES="${HEALTH_CHECK_RETRIES:-48}"
HEALTH_CHECK_INTERVAL_SECONDS="${HEALTH_CHECK_INTERVAL_SECONDS:-5}"
SSM_PARAMETER_PREFIX="${SSM_PARAMETER_PREFIX:-/blog/prod}"
SSM_AWS_REGION="${SSM_AWS_REGION:-ap-northeast-2}"
CW_AGENT_CONFIG_PATH="/opt/aws/amazon-cloudwatch-agent/etc/amazon-cloudwatch-agent.json"

required_vars=(
  DB_HOST
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

optional_vars=(
  SPRING_JPA_HIBERNATE_DDL_AUTO
  JWT_ACCESS_EXPIRATION
  JWT_REFRESH_EXPIRATION
  AWS_REGION
  BACKUP_S3_PREFIX
)

if docker compose version >/dev/null 2>&1; then
  COMPOSE=(docker compose -f "$COMPOSE_FILE")
elif command -v docker-compose >/dev/null 2>&1; then
  COMPOSE=(docker-compose -f "$COMPOSE_FILE")
else
  echo "docker compose command not found."
  exit 1
fi

fetch_ssm_parameter() {
  local name=$1
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

resolve_env_value() {
  local key=$1
  local param_name="$SSM_PARAMETER_PREFIX/$key"
  local value=""

  if [ -n "${!key:-}" ]; then
    return 0
  fi

  if value=$(fetch_ssm_parameter "$param_name"); then
    export "$key=$value"
    return 0
  fi

  return 1
}

load_runtime_env() {
  local missing_vars=()
  local var_name

  for var_name in "${required_vars[@]}"; do
    if ! resolve_env_value "$var_name"; then
      missing_vars+=("$var_name")
    fi
  done

  for var_name in "${optional_vars[@]}"; do
    resolve_env_value "$var_name" || true
  done

  if [ "${#missing_vars[@]}" -gt 0 ]; then
    echo "Missing required env vars from SSM path [$SSM_PARAMETER_PREFIX]: ${missing_vars[*]}"
    exit 1
  fi

  : "${SPRING_JPA_HIBERNATE_DDL_AUTO:=update}"
  : "${JWT_ACCESS_EXPIRATION:=3600000}"
  : "${JWT_REFRESH_EXPIRATION:=1209600000}"
  : "${AWS_REGION:=ap-northeast-2}"
  : "${BACKUP_S3_PREFIX:=backups/postgres}"

  export SPRING_JPA_HIBERNATE_DDL_AUTO JWT_ACCESS_EXPIRATION JWT_REFRESH_EXPIRATION AWS_REGION BACKUP_S3_PREFIX
}

write_env_file() {
  cat > "$ENV_FILE" <<EOV
DB_HOST=${DB_HOST}
DB_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
SPRING_JPA_HIBERNATE_DDL_AUTO=${SPRING_JPA_HIBERNATE_DDL_AUTO}
JWT_ACCESS_EXPIRATION=${JWT_ACCESS_EXPIRATION}
JWT_REFRESH_EXPIRATION=${JWT_REFRESH_EXPIRATION}
GITHUB_CLIENT_ID=${GITHUB_CLIENT_ID}
GITHUB_CLIENT_SECRET=${GITHUB_CLIENT_SECRET}
CORS_ALLOWED_ORIGINS=${CORS_ALLOWED_ORIGINS}
OAUTH_REDIRECT_URL=${OAUTH_REDIRECT_URL}
OAUTH_CALLBACK_URL=${OAUTH_CALLBACK_URL}
AWS_S3_BUCKET=${AWS_S3_BUCKET}
AWS_REGION=${AWS_REGION}
AWS_CLOUDFRONT_DOMAIN=${AWS_CLOUDFRONT_DOMAIN}
CLOUDFRONT_SECRET=${CLOUDFRONT_SECRET}
BACKUP_S3_PREFIX=${BACKUP_S3_PREFIX}
EOV

  chmod 600 "$ENV_FILE"
  chown ec2-user:ec2-user "$ENV_FILE" 2>/dev/null || true

  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
}

configure_cloudwatch_agent() {
  if [ ! -x /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl ]; then
    if command -v dnf >/dev/null 2>&1; then
      dnf install -y amazon-cloudwatch-agent >/tmp/deploy-cwagent-install.log 2>&1 || return 1
    else
      return 1
    fi
  fi

  cat > "$CW_AGENT_CONFIG_PATH" <<'JSON'
{
  "agent": {
    "metrics_collection_interval": 60,
    "run_as_user": "root"
  },
  "metrics": {
    "namespace": "CWAgent",
    "append_dimensions": {
      "InstanceId": "${aws:InstanceId}"
    },
    "aggregation_dimensions": [["InstanceId"]],
    "metrics_collected": {
      "mem": {
        "measurement": ["mem_used_percent"]
      },
      "disk": {
        "measurement": ["used_percent"],
        "resources": ["/"],
        "ignore_file_system_types": ["sysfs", "devtmpfs", "tmpfs", "squashfs", "overlay", "proc", "devfs"]
      }
    }
  }
}
JSON

  /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl \
    -a fetch-config -m ec2 -c file:"$CW_AGENT_CONFIG_PATH" -s >/tmp/deploy-cwagent.log 2>&1
}

install_or_update_cron_jobs() {
  local backup_script="$APP_DIR/scripts/backup/pg_backup_to_s3.sh"
  local restore_script="$APP_DIR/scripts/backup/pg_restore_rehearsal.sh"
  local restart_metric_script="$APP_DIR/scripts/monitoring/publish_container_restart_metric.sh"
  local cron_tmp

  for script_path in "$backup_script" "$restore_script" "$restart_metric_script"; do
    if [ ! -f "$script_path" ]; then
      echo "Missing automation script: $script_path"
      exit 1
    fi
    chmod +x "$script_path"
  done

  if ! command -v crontab >/dev/null 2>&1; then
    if command -v dnf >/dev/null 2>&1; then
      dnf install -y cronie >/tmp/deploy-cronie-install.log 2>&1 || true
    fi
  fi

  if ! command -v crontab >/dev/null 2>&1; then
    echo "crontab command not found; backup/restore automation setup skipped."
    return 0
  fi

  if command -v systemctl >/dev/null 2>&1; then
    systemctl enable crond >/dev/null 2>&1 || true
    systemctl start crond >/dev/null 2>&1 || true
  fi

  cron_tmp=$(mktemp)
  (crontab -l 2>/dev/null || true) \
    | grep -v 'pg_backup_to_s3.sh' \
    | grep -v 'pg_restore_rehearsal.sh' \
    | grep -v 'publish_container_restart_metric.sh' > "$cron_tmp" || true

  {
    cat "$cron_tmp"
    echo "15 3 * * * AWS_REGION=${AWS_REGION} BACKUP_S3_PREFIX=${BACKUP_S3_PREFIX} $backup_script >> /var/log/blog-pg-backup.log 2>&1"
    echo "30 4 * * 0 AWS_REGION=${AWS_REGION} BACKUP_S3_PREFIX=${BACKUP_S3_PREFIX} $restore_script >> /var/log/blog-pg-restore-rehearsal.log 2>&1"
    echo "*/5 * * * * AWS_REGION=${AWS_REGION} $restart_metric_script >> /var/log/blog-container-restart-metric.log 2>&1"
  } | crontab -

  rm -f "$cron_tmp"
}

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
  echo "----- blog-redis logs (tail 80) -----"
  docker logs --tail 80 blog-redis 2>/dev/null || true
}

echo "=== Deploy ==="
echo "New image: $NEW_IMAGE"

# ECR 로그인
aws ecr get-login-password --region ap-northeast-2 \
  | docker login --username AWS --password-stdin "$(echo "$NEW_IMAGE" | cut -d/ -f1)"

cd "$APP_DIR"

load_runtime_env
write_env_file

if ! configure_cloudwatch_agent; then
  echo "Warning: failed to configure CloudWatch agent. Memory/Disk alarms may remain in INSUFFICIENT_DATA."
fi

install_or_update_cron_jobs

if ! is_container_running blog-redis \
  || ! is_container_running blog-caddy; then
  echo "Bootstrap: ensuring redis/caddy are running..."
  "${COMPOSE[@]}" up -d redis >/tmp/deploy-bootstrap.log 2>&1 || {
    tail -n 200 /tmp/deploy-bootstrap.log || true
    exit 1
  }
  "${COMPOSE[@]}" up -d --no-deps caddy >/tmp/deploy-caddy.log 2>&1 || {
    tail -n 200 /tmp/deploy-caddy.log || true
    exit 1
  }
fi

# 새 이미지로 앱 재시작
APP_UP_LOG=/tmp/deploy-app-up.log
set +e
ECR_IMAGE="$NEW_IMAGE" "${COMPOSE[@]}" up -d --no-deps app >"$APP_UP_LOG" 2>&1
APP_UP_EXIT=$?
set -e

if [ "$APP_UP_EXIT" -ne 0 ]; then
  echo "Failed to start app."
  tail -n 200 "$APP_UP_LOG" || true
  exit 1
fi

# Health check 대기
echo "Waiting for app to be healthy..."
for i in $(seq 1 "$HEALTH_CHECK_RETRIES"); do
  STATUS=$(docker inspect --format='{{.State.Health.Status}}' "blog-api" 2>/dev/null || echo "unknown")
  echo "  [$i/$HEALTH_CHECK_RETRIES] status=$STATUS"
  if [ "$STATUS" = "healthy" ]; then
    echo "Health check passed."
    break
  fi
  if [ "$i" = "$HEALTH_CHECK_RETRIES" ]; then
    echo "Health check timed out."
    print_failure_diagnostics "blog-api"
    exit 1
  fi
  sleep "$HEALTH_CHECK_INTERVAL_SECONDS"
done

# 오래된 이미지 정리
docker image prune -f

echo "=== Deployed successfully ==="
