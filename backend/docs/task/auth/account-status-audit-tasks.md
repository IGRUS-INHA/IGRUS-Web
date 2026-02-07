# Tasks: 계정 상태 변경 감사 이력 기능 구현 (Issue #219)

**Input**: [auth-spec.md](/docs/feature/auth/auth-spec.md), Issue #219
**Prerequisites**: User 엔티티, UserRoleHistory, UserSuspension, WithdrawalLog (모두 구현 완료)
**상위 이슈**: #131

**Tests**: 각 태스크별 통합 테스트 포함

## Format: `[ID] Description`

## 배경

현재 계정 상태 변경은 각각 별도 엔티티에 기록됨:
- `UserRoleHistory` - 역할 변경 이력
- `UserSuspension` - 정지/해제 이력
- `WithdrawalLog` - 탈퇴 이력

이슈 #219는 이 모든 변경을 **통합 감사 이력**으로 관리하고, 관리자가 한 곳에서 조회할 수 있는 API를 요구함.

## 구현 현황 요약

| Phase | 총 태스크 | 완료 | 미완료 | 완료율 |
|-------|----------|------|--------|--------|
| Phase 1: 엔티티 & 마이그레이션 | 2 | 0 | 2 | 0% |
| Phase 2: 이벤트 기반 이력 저장 | 4 | 0 | 4 | 0% |
| Phase 3: Admin 조회 API | 4 | 0 | 4 | 0% |
| Phase 4: 테스트 | 3 | 0 | 3 | 0% |
| Phase 5: 문서 | 1 | 0 | 1 | 0% |
| **Total** | **14** | **0** | **14** | **0%** |

---

## Phase 1: 엔티티 & DB 마이그레이션

- [ ] **[T1]** `AccountStatusChangeHistory` 엔티티 생성
  - 파일: `user/domain/AccountStatusChangeHistory.java`
  - `BaseEntity` 확장 (createdAt, createdBy 자동 감사)
  - 필드:
    - `user` (`@ManyToOne(fetch = LAZY)`, nullable) - 대상 사용자
    - `changedBy` (`@ManyToOne(fetch = LAZY)`, nullable) - 변경자 (관리자)
    - `changeType` (`AccountChangeType` enum) - 변경 유형
    - `previousValue` (String) - 변경 전 상태
    - `newValue` (String) - 변경 후 상태
    - `reason` (String, nullable) - 변경 사유
  - `AccountChangeType` enum 값: `ROLE_CHANGE`, `SUSPENSION`, `SUSPENSION_LIFT`, `WITHDRAWAL`, `APPROVAL`, `STATUS_CHANGE`
  - 인덱스: `(user)`, `(changedBy)`, `(changeType)`, `(createdAt)`
  - 팩토리 메서드: `create(user, changedBy, changeType, previousValue, newValue, reason)`
  - 감사 목적 영구 보관: soft delete 미적용 (`BaseEntity` 사용, `SoftDeletableEntity` 아님)

- [ ] **[T2]** Flyway 마이그레이션 `V24__create_account_status_change_histories_table.sql` 생성
  - 파일: `resources/db/migration/V24__create_account_status_change_histories_table.sql`
  - 테이블: `account_status_change_histories`
  - 컬럼 네이밍: `account_status_change_histories_{column}` 패턴
  - 인덱스 생성 포함
  - **삭제 불가 정책**: 이 테이블에는 DELETE 권한 없음 (애플리케이션 레벨에서 삭제 메서드 미제공)

## Phase 2: 이벤트 기반 이력 저장

**전략**: 기존 서비스에 직접 의존성을 추가하는 대신, Spring Application Event를 발행하여 이력 저장 서비스가 구독하는 방식. 기존 코드 변경 최소화.

- [ ] **[T3]** `AccountStatusChangeEvent` 도메인 이벤트 생성
  - 파일: `user/event/AccountStatusChangeEvent.java`
  - record 클래스: `(Long userId, Long changedByUserId, AccountChangeType changeType, String previousValue, String newValue, String reason)`

- [ ] **[T4]** `AccountStatusChangeHistoryRepository` 생성
  - 파일: `user/repository/AccountStatusChangeHistoryRepository.java`
  - `JpaRepository<AccountStatusChangeHistory, Long>` 확장
  - 복합 필터 쿼리: `findByFilters(userId, changedByUserId, changeType, startDate, endDate, pageable)`
    - `LEFT JOIN FETCH` (N+1 방지), 별도 `countQuery`
    - 모든 필터 파라미터 optional (`IS NULL OR` 패턴)

- [ ] **[T5]** `RecordAccountStatusChangeService` 이벤트 리스너 생성
  - 파일: `user/service/RecordAccountStatusChangeService.java`
  - `@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)` - 호출자 트랜잭션 내에서 함께 커밋
  - `AccountStatusChangeEvent` 수신 → `AccountStatusChangeHistory` 엔티티 생성 및 저장
  - User, ChangedBy는 `getReferenceById()`로 프록시 참조 (SELECT 방지)

- [ ] **[T6]** 기존 서비스에 이벤트 발행 추가
  - `ApplicationEventPublisher` 주입 후 `publishEvent()` 호출
  - 대상 서비스 및 발행 위치:
    1. `ApproveAssociateService` - 승인 시 `APPROVAL` (ASSOCIATE → MEMBER)
    2. `BulkApproveAssociatesService` - 일괄 승인 시 각 건마다 `APPROVAL`
    3. `ChangeUserRoleService` - 역할 변경 시 `ROLE_CHANGE`
    4. `ChangeUserStatusService.suspendUser()` - 정지 시 `SUSPENSION`
    5. `ChangeUserStatusService.liftSuspension()` - 정지 해제 시 `SUSPENSION_LIFT`
    6. `WithdrawService` - 탈퇴 시 `WITHDRAWAL`

## Phase 3: Admin 조회 API

- [ ] **[T7]** `AccountStatusChangeHistoryResponse` DTO 생성
  - 파일: `user/dto/response/AccountStatusChangeHistoryResponse.java`
  - record 클래스, `@Schema` 어노테이션
  - 필드: id, userId, userStudentId, changedByUserId, changedByStudentId, changeType, previousValue, newValue, reason, createdAt
  - `static from(AccountStatusChangeHistory)` 팩토리 메서드

- [ ] **[T8]** `GetAccountStatusChangeHistoryService` 서비스 생성
  - 파일: `user/service/GetAccountStatusChangeHistoryService.java`
  - `@Transactional(readOnly = true)`
  - `repository.findByFilters(...).map(AccountStatusChangeHistoryResponse::from)` 반환

- [ ] **[T9]** `AdminAccountStatusChangeHistoryController` 생성
  - 파일: `user/controller/AdminAccountStatusChangeHistoryController.java`
  - `GET /api/v1/admin/account-status-histories`
  - `@PreAuthorize("hasRole('ADMIN')")`
  - 선택적 필터: userId, changedByUserId, changeType, startDate, endDate
  - `@PageableDefault(size = 20, sort = "createdAt", direction = DESC)`
  - Swagger 어노테이션 포함

- [ ] **[T10]** `ApiSecurityConfig`에 admin 경로 추가
  - 파일: `security/config/ApiSecurityConfig.java`
  - `.requestMatchers("/api/v1/admin/account-status-histories/**").hasRole("ADMIN")` 추가

## Phase 4: 테스트

- [ ] **[T11]** `RecordAccountStatusChangeServiceTest` 통합 테스트
  - 파일: `(test) user/service/RecordAccountStatusChangeServiceTest.java`
  - `ServiceIntegrationTestBase` 확장
  - 테스트 케이스: 각 changeType별 이력 저장 확인, 이벤트 발행 → 이력 생성 검증

- [ ] **[T12]** `GetAccountStatusChangeHistoryServiceTest` 통합 테스트
  - 파일: `(test) user/service/GetAccountStatusChangeHistoryServiceTest.java`
  - `ServiceIntegrationTestBase` 확장
  - 테스트 케이스: 필터별 조회, 복합 필터, 페이지네이션, 빈 결과

- [ ] **[T13]** `AdminAccountStatusChangeHistoryControllerTest` 통합 테스트
  - 파일: `(test) user/controller/AdminAccountStatusChangeHistoryControllerTest.java`
  - `ControllerIntegrationTestBase` 확장
  - 테스트 케이스: ADMIN 조회 성공, 각 필터 동작, 403(MEMBER/OPERATOR), 401(미인증), 빈 결과

## Phase 5: 문서 업데이트

- [ ] **[T14]** `auth-spec.md` 업데이트
  - 파일: `docs/feature/auth/auth-spec.md`
  - 계정 상태 변경 감사 이력 관련 FR 추가
  - 관리자 조회 API 엔드포인트 명세 추가
  - 영구 보관 정책 명시

---

## 설계 결정 사항

### 이벤트 기반 vs 직접 호출
- **선택: 이벤트 기반** (`@TransactionalEventListener`)
- **이유**: 기존 서비스 코드 변경 최소화, 감사 로직과 비즈니스 로직 분리, 향후 다른 상태 변경에도 쉽게 확장 가능

### 통합 이력 vs 기존 엔티티 활용
- **선택: 새 통합 엔티티** (`AccountStatusChangeHistory`)
- **이유**: 기존 `UserRoleHistory`, `UserSuspension`, `WithdrawalLog`는 각각 다른 스키마를 가지며 통합 조회가 어려움. 새 엔티티로 일관된 감사 이력 제공. 기존 엔티티는 각자의 도메인 로직에서 그대로 유지.

### TransactionPhase
- **선택: `BEFORE_COMMIT`**
- **이유**: 감사 이력은 원본 변경과 원자적으로 저장되어야 함. 상태 변경 트랜잭션이 롤백되면 감사 이력도 함께 롤백.
