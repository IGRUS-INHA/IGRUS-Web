# Tasks: 권한 변경 감사 이력 기능 구현 (Issue #220)

**Input**: [auth-spec.md](/docs/feature/auth/auth-spec.md), Issue #220
**Prerequisites**: UserRoleHistory 엔티티 (구현 완료), ChangeUserRoleService/ChangeAdminRoleService/ApproveAssociateService (구현 완료)

**Tests**: 각 태스크별 통합 테스트 포함

## Format: `[ID] Description`

## 구현 현황 요약

### 이미 구현된 요구사항

| 요구사항 | 상태 | 구현 위치 |
|----------|------|-----------|
| 권한 변경 시 자동 이력 저장 | ✅ 완료 | `ChangeUserRoleService`, `ChangeAdminRoleService`, `ApproveAssociateService`, `BulkApproveAssociatesService` |
| 변경자 (관리자 ID) 기록 | ✅ 완료 | `BaseEntity.createdBy` (Spring Data Auditing) |
| 대상자 (사용자 ID) 기록 | ✅ 완료 | `UserRoleHistory.user` (ManyToOne) |
| 변경 전/후 역할 기록 | ✅ 완료 | `UserRoleHistory.previousRole`, `UserRoleHistory.newRole` |
| 변경 일시 기록 | ✅ 완료 | `BaseEntity.createdAt` |
| 영구 보관 (삭제 불가) | ✅ 완료 | `UserRoleHistory`는 `BaseEntity` 확장 (soft delete 없음), 삭제 쿼리 없음 |
| 권한 변경 시 리프레시 토큰 만료 (Issue #277) | ✅ 완료 | `ChangeUserRoleService`, `ApproveAssociateService`, `BulkApproveAssociatesService`에서 `refreshTokenRepository.revokeAllByUserId()` 호출 |

### 태스크 현황

| Phase | 총 태스크 | 완료 | 미완료 | 완료율 |
|-------|----------|------|--------|--------|
| Phase 1: Response DTO | 1 | 1 | 0 | 100% |
| Phase 2: Repository 필터 쿼리 | 1 | 1 | 0 | 100% |
| Phase 3: Service | 1 | 1 | 0 | 100% |
| Phase 4: Controller | 1 | 1 | 0 | 100% |
| Phase 5: 테스트 | 2 | 2 | 0 | 100% |
| Phase 6: 문서 | 1 | 1 | 0 | 100% |
| **Total** | **7** | **7** | **0** | **100%** |

---

## Phase 1: Response DTO

- [x] **[T1]** `UserRoleHistoryResponse` record 생성
  - 파일: `admin/user/dto/UserRoleHistoryResponse.java`
  - `UserDetailResponse` 패턴의 record 클래스
  - 필드:
    - `id` (Long) - 이력 ID
    - `userId` (Long) - 대상 사용자 ID
    - `userName` (String) - 대상 사용자 이름
    - `studentId` (String) - 대상 사용자 학번
    - `previousRole` (UserRole) - 변경 전 역할
    - `newRole` (UserRole) - 변경 후 역할
    - `reason` (String, nullable) - 변경 사유
    - `changedBy` (Long, nullable) - 변경자 ID (`BaseEntity.createdBy`)
    - `changedAt` (Instant) - 변경 일시 (`BaseEntity.createdAt`)
  - `@Schema` 어노테이션 포함
  - `static from(UserRoleHistory)` 팩토리 메서드
  - user 필드의 null 처리 (탈퇴한 사용자 고려)

## Phase 2: Repository 필터 쿼리

- [x] **[T2]** `UserRoleHistoryRepository`에 복합 필터 쿼리 추가
  - 파일: `user/repository/UserRoleHistoryRepository.java`
  - `findByFilters(userId, previousRole, newRole, changedBy, startDate, endDate, pageable)` JPQL 쿼리
  - `LEFT JOIN FETCH h.user` (N+1 방지)
  - 별도 `countQuery` 지정 (FETCH JOIN은 count 쿼리에서 제외)
  - 모든 필터 파라미터 optional (`IS NULL OR` 패턴, `UserRepository.findByFilters` 참고)
  - 정렬: `createdAt DESC` 기본

## Phase 3: Service

- [x] **[T3]** `GetUserRoleHistoryService` 서비스 생성
  - 파일: `admin/user/service/GetUserRoleHistoryService.java`
  - `GetUserListService` 패턴 (단순 repository 위임)
  - `@Transactional(readOnly = true)`, `repository.findByFilters().map(UserRoleHistoryResponse::from)`

## Phase 4: Controller

- [x] **[T4]** `AdminUserController`에 권한 변경 이력 조회 엔드포인트 추가
  - 파일: `admin/user/controller/AdminUserController.java`
  - `GET /api/v1/admin/users/role-histories`
  - `@PreAuthorize("hasRole('ADMIN')")` (ADMIN 전용 - 감사 데이터 보호)
  - 선택적 필터 파라미터:
    - `userId` (Long) - 특정 사용자 필터
    - `previousRole` (UserRole) - 변경 전 역할 필터
    - `newRole` (UserRole) - 변경 후 역할 필터
    - `changedBy` (Long) - 변경자 필터
    - `startDate` (Instant) - 시작 일시
    - `endDate` (Instant) - 종료 일시
  - `@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)`
  - Swagger 어노테이션 포함 (`@Operation`, `@ApiResponses`, `@Parameter`)
  - **Security 경로**: `/api/v1/admin/users/**`는 이미 `hasAnyRole("OPERATOR", "ADMIN")` 설정 → `ApiSecurityConfig` 변경 불필요. 메서드 레벨 `@PreAuthorize`로 ADMIN 전용 추가 보호

## Phase 5: 테스트

- [x] **[T5]** `AdminUserControllerTest`에 role-histories 테스트 추가
  - 파일: `(test) admin/user/controller/AdminUserControllerTest.java`
  - 기존 테스트 클래스에 `@Nested` 클래스 추가
  - 테스트 케이스:
    - ADMIN 권한으로 이력 조회 성공
    - userId 필터 동작
    - previousRole/newRole 필터 동작
    - 날짜 범위 필터 동작
    - 복합 필터 조합 동작
    - 페이지네이션 동작
    - 403: OPERATOR 권한으로 접근 시 거부
    - 403: MEMBER 권한으로 접근 시 거부
    - 401: 미인증 접근 시 거부
    - 빈 결과 처리

- [x] **[T6]** `GetUserRoleHistoryServiceTest` 통합 테스트 작성
  - 파일: `(test) admin/user/service/GetUserRoleHistoryServiceTest.java`
  - `ServiceIntegrationTestBase` 확장
  - 테스트 케이스:
    - 전체 이력 조회 (필터 없음)
    - 각 필터별 조회 (userId, previousRole, newRole, changedBy, 날짜 범위)
    - 복합 필터 조합
    - 빈 결과 처리
    - 페이지네이션 동작

## Phase 6: 문서 업데이트

- [x] **[T7]** 관련 스펙 문서 업데이트
  - 파일: `docs/feature/auth/auth-spec.md`
  - 관리자 권한 변경 이력 조회 API 관련 FR 추가
  - 영구 보관 정책 명시
