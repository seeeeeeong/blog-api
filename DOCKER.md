# Docker Compose 가이드

## 로컬 개발 환경 설정

### 1. Docker Compose로 PostgreSQL + Redis 실행

```bash
# Docker Compose 시작
docker-compose -f docker-compose-local.yml up -d

# 로그 확인
docker-compose -f docker-compose-local.yml logs -f

# 상태 확인
docker-compose -f docker-compose-local.yml ps
```

### 2. 서비스 정보

#### PostgreSQL
- **이미지**: `pgvector/pgvector:pg16` (pgvector 사전 설치)
- **컨테이너 이름**: `blog-postgres-local`
- **포트**: `5432`
- **데이터베이스**: `blog_db`
- **사용자**: `postgres`
- **비밀번호**: `postgres`
- **pgvector 확장**: 자동 설치됨

#### Redis
- **이미지**: `redis:7-alpine`
- **컨테이너 이름**: `blog-redis-local`
- **포트**: `6379`

### 3. PostgreSQL 접속

```bash
# psql로 접속
docker exec -it blog-postgres-local psql -U postgres -d blog_db

# pgvector 확장 확인
SELECT * FROM pg_extension WHERE extname = 'vector';

# 종료
\q
```

### 4. Docker Compose 중지 및 제거

```bash
# 중지
docker-compose -f docker-compose-local.yml stop

# 중지 및 컨테이너 제거
docker-compose -f docker-compose-local.yml down

# 볼륨까지 모두 제거 (데이터 삭제)
docker-compose -f docker-compose-local.yml down -v
```

### 5. 애플리케이션 실행

Docker Compose 실행 후:

```bash
# 애플리케이션 시작
./gradlew bootRun
```

### 6. 문제 해결

#### PostgreSQL 연결 실패
```bash
# 컨테이너 로그 확인
docker-compose -f docker-compose-local.yml logs postgres

# 컨테이너 재시작
docker-compose -f docker-compose-local.yml restart postgres
```

#### pgvector 확장 설치 확인
```bash
# PostgreSQL 접속
docker exec -it blog-postgres-local psql -U postgres -d blog_db

# 확장 목록 확인
\dx

# 수동 설치 (필요시)
CREATE EXTENSION IF NOT EXISTS vector;
```

#### 데이터 초기화
```bash
# 기존 볼륨 삭제 및 재생성
docker-compose -f docker-compose-local.yml down -v
docker-compose -f docker-compose-local.yml up -d
```

## 초기화 스크립트

`init-scripts/01-init-pgvector.sql` 파일이 PostgreSQL 컨테이너 시작 시 자동으로 실행되어 pgvector 확장을 설치합니다.

추가 초기화 스크립트가 필요하면 `init-scripts/` 디렉토리에 `.sql` 파일을 추가하세요. (파일명 순서대로 실행됨)
