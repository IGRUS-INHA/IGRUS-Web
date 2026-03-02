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
- `docs/feature/storage/` - 스토리지
- `docs/feature/openapi-contract-first/` - OpenAPI Contract-First 마이그레이션
- `docs/feature/survey-event-registration/` - 설문 연동 행사 신청

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
- 최신 버전: V45 (2026-03-02 기준, create_file_metadata_table)
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

### OpenAPI Contract-First 마이그레이션 패턴
- openapi-generator 설정: `useTags: 'true'` -> 태그별 인터페이스 생성
- 생성 패키지: `igrus.web.generated.api` (인터페이스), `igrus.web.generated.model` (DTO)
- 마이그레이션 완료 레퍼런스: StorageController, HealthController
- 핵심 변환: @AuthenticationPrincipal -> SecurityUtils, @ParameterObject Pageable -> PageableUtils
- 주의: Inquiry 태그 3개 컨트롤러 공유 문제 (태그 분리 완료 - TASK-003)
- SpringDoc 의존성: 마이그레이션 완료 후 제거 예정
- CI: compileJava -> openApiGenerate 의존, openapi/ 변경 시 CI 트리거 필요

### OpenAPI 런타임 응답 검증 패턴
- 라이브러리: Atlassian swagger-request-validator (2.46.0)
  - `swagger-request-validator-mockmvc`: MockMvc 통합 테스트용 (`testImplementation`)
  - `swagger-request-validator-spring-webmvc`: Servlet Filter 런타임 검증용 (`implementation`)
- 기존 TASK 번호: TASK-010~111, 런타임 검증: TASK-200번대
- 작업 계획 위치: `docs/feature/openapi-contract-first/runtime-validation-task-plan.md`
- 스펙 파일: 멀티파일(`openapi/openapi.yaml`) + 번들(`openapi.bundled.yaml`, gitignored)
- 테스트 유틸: `OpenApiValidatorUtil` (static 싱글턴 validator)
- Filter 설정: `@Profile({"dev","test"})` 조건부, prod에서 비활성화
- 테스트 컨텍스트 캐싱 주의: Filter 활성화 시 모든 테스트에 적용됨

### 기존 컨트롤러 테스트 보유 현황 (14개)
- Board, Post, Comment, PostLike, CommentLike, Bookmark, CommentReport
- AdminDashboard, AdminUser, AdminMember, AdminLoginHistory, AdminAccountStatusChangeHistory
- Event, EventRegistration
- 미존재: PinnedPost, PasswordAuth, AdminInquiry, Guest/MemberInquiry, MyPage, Survey*, Privacy, Semester*

### Event-Survey 연동 관련 코드 구조
- `Event.java`: 2축 상태 모델 (registrationStatus + eventStatus), SoftDeletableEntity 상속
- `Event.create()`: 정적 팩토리, surveyId 필드 아직 없음 (추가 필요)
- `Event.update()`: COMPLETED 차단, ONGOING 부분 수정 제한
- `EventRegistrationService.registerEvent()`: 검증 순서 — 권한 > Lazy Eval > 중복(재신청) > OPEN > 기간 > 시간겹침 > 정원
- `EventRegistrationService.handleReRegistration()`: 취소 상태만 재신청 가능
- `SurveyResponseService.submitResponse()`: AuthenticatedUser 파라미터, accessLevel 검증 포함
- `SurveyAnswerValidator`: 설문 응답 유효성 검증 (질문 유형별)
- `SurveyResponseRepository.existsBySurveyIdAndUserId()`: 기존 응답 존재 확인 메서드 이미 존재
- `Survey.isAcceptingResponses()`: PUBLISHED + OPEN 상태 확인 메서드

### 출력 형식 규칙
- 파일 경로 출력: 마지막 줄에 `생성된 파일: {절대 경로}` 또는 `수정된 파일: {절대 경로}`
- 이 줄 이후에는 어떠한 텍스트도 출력하지 말 것
