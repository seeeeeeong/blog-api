#!/bin/bash
set -euo pipefail

AWS_REGION="${AWS_REGION:-ap-northeast-2}"
STATE_FILE="/var/tmp/blog-container-restart-state"
NAMESPACE="Blog/Infra"
METRIC_NAME="ContainerRestartCount"
CONTAINERS=(blog-api-blue blog-api-green blog-postgres blog-redis blog-caddy)

get_instance_id() {
  local token
  token=$(curl -fsS -X PUT "http://169.254.169.254/latest/api/token" \
    -H "X-aws-ec2-metadata-token-ttl-seconds: 21600")
  curl -fsS -H "X-aws-ec2-metadata-token: $token" \
    "http://169.254.169.254/latest/meta-data/instance-id"
}

declare -A prev current
if [ -f "$STATE_FILE" ]; then
  while read -r name count; do
    prev["$name"]="$count"
  done < "$STATE_FILE"
fi

delta=0
for container in "${CONTAINERS[@]}"; do
  if docker inspect "$container" >/dev/null 2>&1; then
    restart_count=$(docker inspect -f '{{.RestartCount}}' "$container" 2>/dev/null || echo 0)
  else
    restart_count=0
  fi

  current["$container"]="$restart_count"
  prev_count="${prev[$container]:-0}"

  if [ "$restart_count" -gt "$prev_count" ]; then
    delta=$((delta + restart_count - prev_count))
  fi
done

{
  for container in "${CONTAINERS[@]}"; do
    echo "$container ${current[$container]}"
  done
} > "$STATE_FILE"

INSTANCE_ID=$(get_instance_id)
aws cloudwatch put-metric-data \
  --region "$AWS_REGION" \
  --namespace "$NAMESPACE" \
  --metric-data "MetricName=$METRIC_NAME,Dimensions=[{Name=InstanceId,Value=$INSTANCE_ID}],Unit=Count,Value=$delta"

echo "published restart delta metric: instance=$INSTANCE_ID value=$delta"
