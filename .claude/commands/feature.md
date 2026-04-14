다음 기능을 구현해줘: $ARGUMENTS

## 구현 순서
1. 기존 코드 분석 — 관련 도메인 파악
2. domain/ 패키지에 커맨드 객체 + 도메인 모델 설계
3. Entity + Repository 작성
4. Service 로직 구현 (@Transactional 적용)
5. Controller + Request DTO (nested class, toCommand()) + Response DTO (dto/ 패키지, companion from())
6. 테스트 작성
7. ./gradlew test 실행해서 전체 통과 확인

## 반드시 지켜야 할 것
- CLAUDE.md의 코드 컨벤션 전부 준수
- trailing comma 사용
- 모든 API 응답은 ApiResponse<T>로 래핑
- 에러는 CoreException(ErrorType.XXX) throw
- 기존 코드 스타일과 일관성 유지
