#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
EC2_IP="52.78.111.209"
EC2_KEY="$HOME/.ssh/blog-key.pem"

# 1. 기존 SSH 터널 종료
echo "[1/4] 기존 SSH 터널 정리 중..."
pkill -f "19090:localhost:8080" 2>/dev/null || true
pkill -f "19092:localhost:9100" 2>/dev/null || true
sleep 1

# 2. SSH 터널 백그라운드 시작
echo "[2/4] SSH 터널 연결 중 (EC2: $EC2_IP)..."
ssh -i "$EC2_KEY" \
    -L 19090:localhost:8080 \
    -L 19092:localhost:9100 \
    -N -f \
    -o StrictHostKeyChecking=no \
    -o ExitOnForwardFailure=yes \
    ec2-user@"$EC2_IP"
echo "      SSH 터널 연결 완료"

# 3. 모니터링 스택 시작
echo "[3/4] 모니터링 스택 시작 중..."
docker compose -f "$SCRIPT_DIR/docker-compose.local.yml" down 2>/dev/null || true
docker compose -f "$SCRIPT_DIR/docker-compose.local.yml" up -d

echo "      Grafana 준비 대기 중..."
until curl -sf http://localhost:13000/api/health > /dev/null 2>&1; do
    printf "."
    sleep 2
done
echo " 준비 완료"

# 4. Grafana 대시보드 자동 import
echo "[4/4] Grafana 대시보드 import 중..."
python3 << 'PYTHON'
import json, urllib.request, urllib.error, base64

GRAFANA_URL = "http://localhost:13000"
CREDENTIALS = base64.b64encode(b"admin:admin").decode()

def import_dashboard(dashboard_id, name):
    print(f"  - {name} (ID: {dashboard_id}) ...", end="", flush=True)
    try:
        url = f"https://grafana.com/api/dashboards/{dashboard_id}/revisions/latest/download"
        dashboard = json.loads(urllib.request.urlopen(url).read())

        payload = json.dumps({
            "dashboard": dashboard,
            "overwrite": True,
            "inputs": [{
                "name": "DS_PROMETHEUS",
                "type": "datasource",
                "pluginId": "prometheus",
                "value": "Prometheus"
            }]
        }).encode()

        req = urllib.request.Request(
            f"{GRAFANA_URL}/api/dashboards/import",
            data=payload,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Basic {CREDENTIALS}"
            }
        )
        urllib.request.urlopen(req)
        print(" 완료")
    except Exception as e:
        print(f" 실패: {e}")

import_dashboard(1860, "Node Exporter Full (시스템 메모리/CPU)")
import_dashboard(12900, "JVM Micrometer (힙/GC)")
PYTHON

echo ""
echo "=========================================="
echo "  모니터링 시작 완료"
echo "=========================================="
echo "  Prometheus : http://localhost:19091"
echo "  Grafana    : http://localhost:13000"
echo "               (admin / admin)"
echo "  Alerts     : http://localhost:19091/alerts"
echo "=========================================="
