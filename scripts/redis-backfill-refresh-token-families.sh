#!/usr/bin/env bash
set -euo pipefail

REDIS_HOST="${REDIS_HOST:-localhost}"
REDIS_PORT="${REDIS_PORT:-6379}"
REDIS_DB="${REDIS_DB:-0}"
REDIS_PASSWORD="${REDIS_PASSWORD:-}"
REDIS_CLI_OPTS="${REDIS_CLI_OPTS:-}"

redis_cmd=(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -n "$REDIS_DB")
if [ -n "$REDIS_PASSWORD" ]; then
  redis_cmd+=(-a "$REDIS_PASSWORD")
fi
if [ -n "$REDIS_CLI_OPTS" ]; then
  # shellcheck disable=SC2206
  redis_cmd+=($REDIS_CLI_OPTS)
fi

processed=0

while read -r key; do
  if [ -z "$key" ]; then
    continue
  fi

  json=$("${redis_cmd[@]}" get "$key")
  if [ -z "$json" ]; then
    continue
  fi

  read -r family_id user_id < <(printf '%s' "$json" | python3 - <<'PY'
import json
import sys

try:
    data = json.load(sys.stdin)
except Exception:
    print("", "")
    sys.exit(0)

family_id = data.get("familyId", "") or ""
user_id = data.get("userId", "") or ""
print(family_id, user_id)
PY
)

  if [ -z "$family_id" ]; then
    continue
  fi

  token_id=${key#token:}
  family_tokens_key="family:${family_id}:tokens"
  "${redis_cmd[@]}" sadd "$family_tokens_key" "$token_id" >/dev/null

  ttl=$("${redis_cmd[@]}" ttl "$key" || echo -2)
  if [ "$ttl" -gt 0 ]; then
    new_ttl=$((ttl + 86400))
    current_ttl=$("${redis_cmd[@]}" ttl "$family_tokens_key" || echo -2)
    if [ "$current_ttl" -lt "$new_ttl" ]; then
      "${redis_cmd[@]}" expire "$family_tokens_key" "$new_ttl" >/dev/null
    fi
  fi

  if [ -n "$user_id" ]; then
    user_key="user:${user_id}:families"
    "${redis_cmd[@]}" sadd "$user_key" "$family_id" >/dev/null
    if [ "$ttl" -gt 0 ]; then
      current_ttl=$("${redis_cmd[@]}" ttl "$user_key" || echo -2)
      if [ "$current_ttl" -lt "$new_ttl" ]; then
        "${redis_cmd[@]}" expire "$user_key" "$new_ttl" >/dev/null
      fi
    fi
  fi

  processed=$((processed + 1))
done < <("${redis_cmd[@]}" --scan --pattern 'token:*')

echo "Processed ${processed} token keys."
