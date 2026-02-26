# Task Planner Agent Memory

## 프로젝트 구조

### docs/feature 하위 폴더 구조
- `docs/feature/auth/` - 인증/인가 관련 (auth-spec.md, user-entity-design.md)
- `docs/feature/common/` - 공통 문서 (PRD V1/V2, README)
- `docs/feature/community/` - 커뮤니티 (board/post/comment/like-bookmark spec)
- `docs/feature/member-list/` - 회원 목록
- `docs/feature/devops/staging/` - 인프라/배포
- `docs/feature/design-agent/` - 디자인 에이전트
- `docs/feature/monitoring/` - 모니터링
- `docs/feature/tech-research/` - 기술 조사
- `docs/feature/storage/` - 스토리지 (신규 생성)

### 검증 기준/테스트 케이스 문서 위치
- 검증 기준: `docs/criteria/{도메인}/`
- 테스트 케이스: `docs/test-case/{도메인}/`

### 기존 문서 스타일
- Feature spec: `# Feature Specification: {기능명}` 헤더, Created/Updated/Status 메타, User Story 기반
- 작업 계획(task-plan): 이 프로젝트에서 처음 작성. 마크다운 체크리스트 + 의존성 그래프 형식 채택

## 백엔드 구조

### 패키지 네이밍: `igrus.web.{도메인}`
- `igrus.web.common` - 공통 (BaseEntity, SoftDeletableEntity, ErrorCode, GlobalExceptionHandler)
- `igrus.web.security` - 보안 (auth, jwt, config)
- `igrus.web.user` - 사용자
- `igrus.web.community` - 커뮤니티 (board, post, comment, bookmark, like, pinnedpost)
- `igrus.web.event` - 행사
- `igrus.web.inquiry` - 문의
- `igrus.web.admin` - 관리자

### 엔티티 기반 클래스
- `BaseEntity`: createdAt, updatedAt, createdBy, updatedBy (Instant 타입, JPA Auditing)
- `SoftDeletableEntity extends BaseEntity`: deleted, deletedAt, deletedBy

### 예외 처리 패턴
- `ErrorCode` 인터페이스 -> 도메인별 `{Domain}ErrorCode` enum
- `CustomBaseException` -> 도메인별 커스텀 예외

### Flyway 마이그레이션
- 최신 버전: V40 (2026-02-26 기준)
- 형식: `V{N}__{description}.sql`

### 빌드 의존성 (build.gradle)
- Spring Boot 3.5.9 (CLAUDE.md에는 4.0.1로 기재되나 build.gradle은 3.5.9)
- 이미 포함: spring-cloud-aws-starter-secrets-manager:3.4.0
- S3 SDK 미포함 (신규 추가 필요)

## 프론트엔드 구조
- Orval 자동 생성: `frontend/src/api/model/{도메인}/` 및 `frontend/src/api/model/models/`
- API 클라이언트: `frontend/src/api/client.ts`, `query-client.ts`

## 작업 계획 작성 패턴

### 반복 작업 유형
1. Flyway 마이그레이션 스크립트
2. JPA 엔티티 + enum 구현
3. Repository 인터페이스
4. ErrorCode + 커스텀 예외
5. 서비스 클래스 (비즈니스 로직)
6. DTO (요청/응답, Bean Validation)
7. Controller (Swagger 어노테이션 포함)
8. Spring Security 경로 설정
9. 단위 테스트 / 서비스 통합 테스트 / Controller 통합 테스트
10. Orval API 클라이언트 생성
11. 프론트엔드 커스텀 훅 / 컴포넌트

### 출력 형식 규칙
- 파일 경로 출력: 마지막 줄에 `생성된 파일: {절대 경로}` 또는 `수정된 파일: {절대 경로}`
- 이 줄 이후에는 어떠한 텍스트도 출력하지 말 것
