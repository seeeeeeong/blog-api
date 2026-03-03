#!/bin/bash
# Terraform Remote State 부트스트랩 (1회만 실행)
# 사전 조건: AWS CLI 설정 완료, 충분한 IAM 권한
set -euo pipefail

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION="ap-northeast-2"
BUCKET="blog-terraform-state-${ACCOUNT_ID}"
LOCK_TABLE="terraform-state-lock"
SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
TERRAFORM_DIR="${SCRIPT_DIR}/../terraform"
BACKEND_FILE="${TERRAFORM_DIR}/backend.hcl"

echo "=== Terraform Remote State Bootstrap ==="
echo "Account : $ACCOUNT_ID"
echo "Bucket  : $BUCKET"
echo "Table   : $LOCK_TABLE"
echo "Region  : $REGION"
echo ""

# S3 버킷 생성
if aws s3api head-bucket --bucket "$BUCKET" 2>/dev/null; then
  echo "[SKIP] S3 bucket already exists: $BUCKET"
else
  aws s3api create-bucket \
    --bucket "$BUCKET" \
    --region "$REGION" \
    --create-bucket-configuration LocationConstraint="$REGION"
  echo "[OK] Created S3 bucket: $BUCKET"
fi

# 버전 관리 활성화
aws s3api put-bucket-versioning \
  --bucket "$BUCKET" \
  --versioning-configuration Status=Enabled
echo "[OK] Enabled versioning"

# 서버 사이드 암호화
aws s3api put-bucket-encryption \
  --bucket "$BUCKET" \
  --server-side-encryption-configuration '{
    "Rules": [{
      "ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "AES256"}
    }]
  }'
echo "[OK] Enabled AES256 encryption"

# 퍼블릭 접근 차단
aws s3api put-public-access-block \
  --bucket "$BUCKET" \
  --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
echo "[OK] Blocked public access"

# DynamoDB 락 테이블 생성
if aws dynamodb describe-table --table-name "$LOCK_TABLE" --region "$REGION" 2>/dev/null; then
  echo "[SKIP] DynamoDB table already exists: $LOCK_TABLE"
else
  aws dynamodb create-table \
    --table-name "$LOCK_TABLE" \
    --attribute-definitions AttributeName=LockID,AttributeType=S \
    --key-schema AttributeName=LockID,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --region "$REGION"
  echo "[OK] Created DynamoDB table: $LOCK_TABLE"
fi

cat > "$BACKEND_FILE" <<EOF
bucket         = "$BUCKET"
key            = "blog/terraform.tfstate"
region         = "$REGION"
dynamodb_table = "$LOCK_TABLE"
encrypt        = true
EOF
echo "[OK] Wrote backend config: $BACKEND_FILE"

echo ""
echo "=== Bootstrap complete ==="
echo "다음 명령으로 remote backend를 초기화하세요:"
echo "terraform -chdir=${TERRAFORM_DIR} init -reconfigure -backend-config=backend.hcl -migrate-state"
echo "terraform -chdir=${TERRAFORM_DIR} plan"
