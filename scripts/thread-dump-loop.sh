#!/bin/bash
# 부하테스트 중 5초마다 스레드 덤프를 연속 수집
# 사용법: ./thread-dump-loop.sh [횟수] [간격(초)]
# 예시:  ./thread-dump-loop.sh 20 5   → 5초마다 20회 = 100초간 수집

COUNT=${1:-20}
INTERVAL=${2:-5}

EC2_IP="52.78.111.209"
EC2_KEY="$HOME/.ssh/blog-key.pem"
OUT_DIR="$HOME/Desktop/threaddumps"

mkdir -p "$OUT_DIR"
echo "스레드 덤프 연속 수집 시작 (${COUNT}회, ${INTERVAL}초 간격)"
echo "저장 위치: $OUT_DIR"
echo ""

for i in $(seq 1 "$COUNT"); do
    TIMESTAMP=$(date +"%H%M%S")
    OUT_FILE="$OUT_DIR/dump_${i}_${TIMESTAMP}.txt"

    printf "[%2d/%d] %s 수집 중..." "$i" "$COUNT" "$TIMESTAMP"

    ssh -i "$EC2_KEY" -o ConnectTimeout=5 ec2-user@"$EC2_IP" \
        "docker exec blog-api kill -3 1 2>/dev/null; sleep 0.3; docker logs --tail 200 blog-api 2>&1 | grep -A 200 'Full thread dump'" \
        > "$OUT_FILE" 2>/dev/null

    BLOCKED=$(grep -c "java.lang.Thread.State: BLOCKED" "$OUT_FILE" 2>/dev/null || echo 0)
    WAITING=$(grep -c "java.lang.Thread.State: WAITING" "$OUT_FILE" 2>/dev/null || echo 0)
    FILE_IO=$(grep -c "FileAppender\|OutputStreamAppender\|RollingFileAppender" "$OUT_FILE" 2>/dev/null || echo 0)

    printf " BLOCKED=%s WAITING=%s FileIO스레드=%s\n" "$BLOCKED" "$WAITING" "$FILE_IO"

    if [ "$i" -lt "$COUNT" ]; then
        sleep "$INTERVAL"
    fi
done

echo ""
echo "=== 수집 완료 ==="
echo "전체 BLOCKED 스레드 합계:"
grep -c "java.lang.Thread.State: BLOCKED" "$OUT_DIR"/dump_*.txt 2>/dev/null | \
    awk -F: '{sum += $2} END {print sum+0}'

echo ""
echo "FileAppender 관련 스레드 발견된 덤프:"
grep -l "FileAppender\|OutputStreamAppender\|RollingFileAppender" "$OUT_DIR"/dump_*.txt 2>/dev/null || echo "  없음"

echo ""
echo "결과 파일: $OUT_DIR/"
