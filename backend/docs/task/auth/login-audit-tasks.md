# Tasks: 로그인 감사 이력 기능 구현 (Issue #222)

**Input**: [auth-spec.md](/docs/feature/auth/auth-spec.md), Issue #222
**Prerequisites**: LoginHistory 엔티티 (구현 완료), RecordLoginSuccessService/RecordLoginFailureService (구현 완료)

**Tests**: 각 태스크별 통합 테스트 포함

## Format: `[ID] Description`

## 구현 현황 요약

| Phase | 총 태스크 | 완료 | 미완료 | 완료율 |
|-------|----------|------|--------|--------|
| Phase 1: 버그 수정 | 2 | 0 | 2 | 0% |
| Phase 2: Admin 조회 API | 5 | 0 | 5 | 0% |
| Phase 3: 영구 보관 정책 | 2 | 0 | 2 | 0% |
| Phase 4: 테스트 | 2 | 0 | 2 | 0% |
| Phase 5: 문서 | 1 | 0 | 1 | 0% |
| **Total** | **12** | **0** | **12** | **0%** |

---

## Phase 1: 트랜잭션 롤백 버그 수정 (Critical)

**배경**: `LoginService.login()`이 `@Transactional`이며, 실패 경로에서 예외를 던지면 `RecordFailedAttemptService`와 `RecordLoginFailureService`의 save가 트랜잭션 롤백으로 소실됨. 브루트포스 방지(LoginAttempt 카운트)와 실패 이력 저장이 모두 작동하지 않는 상태.

**선례**: `EmailVerificationAttemptService.incrementAttempts()` - 동일 패턴으로 `Propagation.REQUIRES_NEW` 사용 중.

- [ ] **[T1]** `RecordFailedAttemptService.recordFailedAttempt()`에 `@Transactional(propagation = Propagation.REQUIRES_NEW)` 적용
  - 파일: `security/auth/common/service/login/RecordFailedAttemptService.java`
  - 별도 트랜잭션으로 실행되어 호출자 트랜잭션 롤백 시에도 카운트 유지

- [ ] **[T2]** `RecordLoginFailureService`의 두 `recordFailure()` 오버로드 메서드에 `@Transactional(propagation = Propagation.REQUIRES_NEW)` 적용
  - 파일: `security/auth/common/service/login/RecordLoginFailureService.java`
  - 별도 트랜잭션으로 실행되어 실패 이력이 항상 저장됨

## Phase 2: Admin 로그인 이력 조회 API

- [ ] **[T3]** `LoginHistoryResponse` DTO 생성
  - 파일: `security/auth/common/dto/response/LoginHistoryResponse.java`
  - `AssociateInfoResponse` 패턴의 record 클래스
  - 필드: id, userId(nullable), studentId, ipAddress, userAgent, success, failureReason, attemptedAt
  - `@Schema` 어노테이션, `static from(LoginHistory)` 팩토리 메서드

- [ ] **[T4]** `LoginHistoryRepository`에 복합 필터 쿼리 추가
  - 파일: `security/auth/common/repository/LoginHistoryRepository.java`
  - `findByFilters(studentId, success, ipAddress, startDate, endDate, pageable)` JPQL 쿼리
  - `LEFT JOIN FETCH lh.user` (N+1 방지), 별도 `countQuery` 지정
  - 모든 필터 파라미터 optional (`IS NULL OR` 패턴)

- [ ] **[T5]** `GetLoginHistoryForAdminService` 서비스 생성
  - 파일: `security/auth/common/service/login/GetLoginHistoryForAdminService.java`
  - `GetLoginHistoryByStudentIdService` 패턴
  - `@Transactional(readOnly = true)`, `repository.findByFilters().map(LoginHistoryResponse::from)`

- [ ] **[T6]** `AdminLoginHistoryController` 생성
  - 파일: `security/auth/common/controller/AdminLoginHistoryController.java`
  - `AdminMemberController` 패턴
  - `GET /api/v1/admin/login-histories`, `@PreAuthorize("hasRole('ADMIN')")`
  - 선택적 필터: studentId, success, ipAddress, startDate, endDate
  - `@PageableDefault(size = 20, sort = "attemptedAt", direction = DESC)`
  - Swagger 어노테이션 포함

- [ ] **[T7]** `ApiSecurityConfig`에 admin 경로 추가
  - 파일: `security/config/ApiSecurityConfig.java`
  - `.requestMatchers("/api/v1/admin/login-histories/**").hasRole("ADMIN")` 추가

## Phase 3: 영구 보관 정책 적용

**배경**: 이슈 요구사항 "보안 감사 목적으로 영구 보관 (삭제 불가)"와 기존 1년 후 자동 삭제 정책(FR-035/036)이 충돌.

- [ ] **[T8]** `LoginHistoryRepository`에서 `deleteByAttemptedAtBefore` 메서드 제거
  - 파일: `security/auth/common/repository/LoginHistoryRepository.java`

- [ ] **[T9]** 삭제 인프라 제거
  - `DeleteOldLoginHistoriesService.java` 삭제
  - `LoginHistoryCleanupScheduler.java` 삭제
  - `DeleteOldLoginHistoriesServiceTest.java` 삭제

## Phase 4: 테스트

- [ ] **[T10]** `AdminLoginHistoryControllerTest` 통합 테스트 작성
  - 파일: `(test) security/auth/common/controller/AdminLoginHistoryControllerTest.java`
  - `ServiceIntegrationTestBase` 확장, `@AutoConfigureMockMvc`
  - 테스트 케이스: ADMIN 조회 성공, 각 필터 동작, 복합 필터, 페이지네이션, 403(MEMBER), 401(미인증), 빈 결과

- [ ] **[T11]** `GetLoginHistoryForAdminServiceTest` 통합 테스트 작성
  - 파일: `(test) security/auth/common/service/login/GetLoginHistoryForAdminServiceTest.java`
  - `ServiceIntegrationTestBase` 확장
  - 테스트 케이스: 필터별 조회, 복합 필터, 빈 결과

## Phase 5: 문서 업데이트

- [ ] **[T12]** `auth-spec.md` 업데이트
  - 파일: `docs/feature/auth/auth-spec.md`
  - FR-035 수정: "1년간 보관" → "영구 보관 (보안 감사 목적, 삭제 불가)"
  - FR-036 제거 (자동 삭제 기능 제거)
  - 관리자 로그인 이력 조회 API 관련 FR 추가
