#!/bin/bash

EC2_IP="52.78.111.209"
EC2_KEY="$HOME/.ssh/blog-key.pem"
REMOTE_PATH="/var/lib/docker/volumes/app_data/_data/heapdump.hprof"
CONTAINER_PATH="/app/heapdump.hprof"
LOCAL_PATH="$HOME/Desktop/heapdump.hprof"

echo "EC2에서 힙 덤프 확인 중..."

# 컨테이너 내부에 힙 덤프가 있는지 확인
EXISTS=$(ssh -i "$EC2_KEY" ec2-user@"$EC2_IP" \
    "docker exec blog-api test -f $CONTAINER_PATH && echo yes || echo no")

if [ "$EXISTS" = "no" ]; then
    echo "힙 덤프 파일이 없습니다. OOM이 아직 발생하지 않았거나 컨테이너가 재시작됐을 수 있어요."
    exit 1
fi

echo "힙 덤프 발견! 로컬로 복사 중..."

# 컨테이너에서 EC2 호스트로 복사
ssh -i "$EC2_KEY" ec2-user@"$EC2_IP" \
    "docker cp blog-api:$CONTAINER_PATH /tmp/heapdump.hprof"

# EC2에서 로컬로 복사
scp -i "$EC2_KEY" ec2-user@"$EC2_IP":/tmp/heapdump.hprof "$LOCAL_PATH"

# EC2 임시 파일 삭제
ssh -i "$EC2_KEY" ec2-user@"$EC2_IP" "rm /tmp/heapdump.hprof"

echo ""
echo "힙 덤프 저장 완료: $LOCAL_PATH"
echo ""
echo "분석 방법:"
echo "  1. VisualVM (무료): https://visualvm.github.io"
echo "     → File → Load → heapdump.hprof"
echo "  2. Eclipse MAT (무료): https://eclipse.dev/mat"
echo "     → File → Open Heap Dump"
