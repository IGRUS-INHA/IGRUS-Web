# Tasks: 학기별 회원 명단 관리

**Input**: `docs/feature/member-list/member-list-spec.md`
**Prerequisites**: member-list-spec.md (기능 명세서)
**Tech Stack**: Java 21, Spring Boot 4.0.1, Spring Data JPA, MySQL 8.x, Flyway

**Tests**: 테스트 코드 작성은 별도 요청 시 진행

**Organization**: User Story 기반으로 구성하여 독립적 구현 및 테스트 가능

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 해당 태스크가 속한 User Story (US1, US2, US3)
- 모든 태스크에 정확한 파일 경로 포함

## Path Conventions

- **Backend**: `backend/src/main/java/igrus/web/`
- **Semester Module**: `backend/src/main/java/igrus/web/user/semester/`
- **Migrations**: `backend/src/main/resources/db/migration/`

---

## Phase 1: Setup (프로젝트 구조 설정) ✅

**Purpose**: 학기별 회원 관리 도메인의 패키지 구조 설정

- [x] T001 학기별 회원 관리 도메인 패키지 구조 생성: `domain/`, `repository/`, `service/`, `controller/`, `dto/request/`, `dto/response/`, `exception/` in `backend/src/main/java/igrus/web/user/semester/`

---

## Phase 2: Foundational (핵심 인프라 - 모든 User Story 선행 조건) ✅

**Purpose**: 익명화 제거 및 SemesterMember 핵심 엔티티/인프라 구축

### 2.1 익명화 제거

- [x] T002 Flyway 마이그레이션 V14 작성: `users_anonymized` 컬럼 제거 in `backend/src/main/resources/db/migration/V14__remove_user_anonymized_column.sql`
  - `ALTER TABLE users DROP COLUMN users_anonymized`
  - 주의: 이미 익명화된 사용자의 데이터는 복구 불가 (기존 상태 유지)

- [x] T003 User 엔티티에서 익명화 관련 코드 제거 in `backend/src/main/java/igrus/web/user/domain/User.java`
  - `anonymized` 필드 제거
  - `anonymize(String hash)` 메서드 제거
  - `isAnonymized()` 메서드 제거
  - `isWithdrawnOrAnonymized()` → `isWithdrawn()` 으로 대체 (메서드 제거 후 호출부 변경)
  - `getDisplayName()`: `isWithdrawnOrAnonymized()` → `isWithdrawn()` 으로 변경
  - `getDisplayId()`: `isWithdrawnOrAnonymized()` → `isWithdrawn()` 으로 변경

- [x] T004 `isWithdrawnOrAnonymized()` 호출부 전체 검색 및 `isWithdrawn()` 으로 변경 in 프로젝트 전체
  - 커뮤니티 모듈(Post, Comment 등)에서 `isWithdrawnOrAnonymized()` 참조 검색
  - 모든 참조를 `isWithdrawn()` 으로 변경

- [x] T005 WithdrawnUserCleanupService에서 익명화 로직 제거 in `backend/src/main/java/igrus/web/security/auth/common/service/WithdrawnUserCleanupService.java`
  - `anonymizeExpiredWithdrawnUsers()` 메서드에서 `user.anonymize(hash)` 호출 제거
  - 인증 데이터 hard delete 로직(PasswordCredential, PrivacyConsent, EmailVerification, RefreshToken)은 기존과 동일하게 유지
  - 메서드명을 `cleanupExpiredWithdrawnUsers()` 등으로 변경 고려

- [x] T006 WithdrawnUserCleanupScheduler 업데이트 in `backend/src/main/java/igrus/web/security/auth/common/scheduler/WithdrawnUserCleanupScheduler.java`
  - 익명화 관련 로그 메시지 수정
  - 서비스 메서드명 변경 시 호출부 동기화

- [x] T007 UserRepository에서 익명화 관련 쿼리 정리 in `backend/src/main/java/igrus/web/user/repository/UserRepository.java`
  - `findWithdrawnUsersBeforeAndNotAnonymized()` 메서드의 `users_anonymized = false` 조건 제거
  - 메서드명을 `findWithdrawnUsersBefore(Instant cutoffTime)` 으로 변경

### 2.2 SemesterMember 엔티티 및 인프라

- [x] T008 Flyway 마이그레이션 V15 작성: `semester_members` 테이블 생성 in `backend/src/main/resources/db/migration/V15__create_semester_members_table.sql`
  - 컬럼: `semester_members_id` (PK, AUTO_INCREMENT), `semester_members_user_id` (FK → users.users_id), `semester_members_year` (INT, NOT NULL), `semester_members_semester` (INT, NOT NULL, 1 또는 2), `semester_members_role` (VARCHAR(20), NOT NULL), `semester_members_created_at`, `semester_members_updated_at`, `semester_members_created_by`, `semester_members_updated_by`
  - UNIQUE 제약조건: `(semester_members_user_id, semester_members_year, semester_members_semester)`
  - INDEX: `idx_semester_members_year_semester` on `(semester_members_year, semester_members_semester)`

- [x] T009 SemesterMember 엔티티 구현 in `backend/src/main/java/igrus/web/user/semester/domain/SemesterMember.java`
  - BaseEntity 상속
  - 필드: `user` (ManyToOne, FetchType.LAZY → User), `year` (Integer), `semester` (Integer, 1 또는 2), `role` (UserRole, 등록 시점의 역할)
  - `@Table(uniqueConstraints = @UniqueConstraint(columns = {"semester_members_user_id", "semester_members_year", "semester_members_semester"}))`
  - 정적 팩토리 메서드: `create(User user, int year, int semester, UserRole role)`
  - `@AttributeOverrides` 적용 (기존 엔티티 패턴 따름)

- [x] T010 [P] SemesterMemberRepository 인터페이스 구현 in `backend/src/main/java/igrus/web/user/semester/repository/SemesterMemberRepository.java`
  - `findByYearAndSemester(int year, int semester, Pageable pageable)`: Page<SemesterMember>
  - `findByYearAndSemester(int year, int semester)`: List<SemesterMember>
  - `findByUser(User user)`: List<SemesterMember>
  - `existsByUserAndYearAndSemester(User user, int year, int semester)`: boolean
  - `deleteByUserAndYearAndSemester(User user, int year, int semester)`: void
  - `countByYearAndSemester(int year, int semester)`: long
  - Native 쿼리: `findAllWithUserIncludingDeleted(int year, int semester)` - User soft delete 필터 우회 JOIN 쿼리
  - Native 쿼리: `findDistinctSemestersWithCount()` - 학기 목록과 회원 수 조회

- [x] T011 [P] ErrorCode에 학기별 회원 관련 에러 코드 추가 in `backend/src/main/java/igrus/web/common/exception/ErrorCode.java`
  - `SEMESTER_MEMBER_NOT_FOUND(404, "해당 학기에 등록된 회원을 찾을 수 없습니다")`
  - `SEMESTER_MEMBER_ALREADY_EXISTS(409, "이미 해당 학기에 등록된 회원입니다")`
  - `SEMESTER_INVALID_SEMESTER(400, "학기는 1 또는 2만 가능합니다")`
  - `SEMESTER_INVALID_YEAR(400, "유효하지 않은 연도입니다")`

- [x] T012 [P] 학기별 회원 예외 클래스 구현 in `backend/src/main/java/igrus/web/user/semester/exception/`
  - `SemesterMemberAlreadyExistsException` extends CustomBaseException
  - `SemesterMemberNotFoundException` extends CustomBaseException
  - `InvalidSemesterException` extends CustomBaseException

**Checkpoint**: ✅ Foundation 완료 - User Story 구현 시작 가능

---

## Phase 3: User Story 1 - 학기별 회원 등록 (Priority: P1) ✅ MVP

**Goal**: 관리자가 특정 학기에 현재 회원(ASSOCIATE 이상)을 선택하여 등록할 수 있다

**Independent Test**: 관리자가 학기를 선택하고 회원을 등록하면 해당 학기 멤버십 기록이 생성되며, 등록 시점의 역할이 함께 저장된다

### Acceptance Criteria

1. 관리자가 학기 선택 후 회원 등록 메뉴에 접속하면 ASSOCIATE 이상 회원 목록이 등록 여부와 함께 표시
2. 선택된 회원의 등록 시점 역할이 함께 기록
3. 이미 등록된 회원은 "이미 등록됨" 표시와 함께 중복 등록 방지
4. 전체 선택 시 미등록 회원만 등록

### Implementation for User Story 1

- [x] T013 [P] [US1] CandidateMemberResponse DTO 구현 in `backend/src/main/java/igrus/web/user/semester/dto/response/CandidateMemberResponse.java`
  - 필드: `userId`, `studentId`, `name`, `department`, `role`, `alreadyRegistered` (해당 학기 등록 여부)
  - `from(User user, boolean alreadyRegistered)` 정적 팩토리 메서드

- [x] T014 [P] [US1] RegisterSemesterMembersRequest DTO 구현 in `backend/src/main/java/igrus/web/user/semester/dto/request/RegisterSemesterMembersRequest.java`
  - 필드: `List<Long> userIds` (@NotEmpty, 등록할 회원 ID 목록)
  - Bean Validation 적용

- [x] T015 [P] [US1] RegisterSemesterMembersResponse DTO 구현 in `backend/src/main/java/igrus/web/user/semester/dto/response/RegisterSemesterMembersResponse.java`
  - 필드: `registeredCount`, `skippedCount` (이미 등록된 회원), `totalRequested`

- [x] T016 [US1] SemesterMemberService - 등록 관련 메서드 구현 in `backend/src/main/java/igrus/web/user/semester/service/SemesterMemberService.java`
  - `getCandidateMembers(int year, int semester)`: List<CandidateMemberResponse> - ASSOCIATE 이상 + ACTIVE 상태 회원 목록 (등록 여부 포함)
  - `registerMembers(int year, int semester, List<Long> userIds, Long adminId)`: RegisterSemesterMembersResponse - 선택된 회원 일괄 등록
  - 연도/학기 유효성 검증 (semester는 1 또는 2)
  - 중복 등록 방지 (existsByUserAndYearAndSemester 체크)
  - 등록 시 User의 현재 role 스냅샷 저장

- [x] T017 [US1] AdminSemesterMemberController 구현 - 등록 엔드포인트 in `backend/src/main/java/igrus/web/user/semester/controller/AdminSemesterMemberController.java`
  - `@RestController`, `@RequestMapping("/api/v1/admin/semesters")`, `@PreAuthorize("hasRole('ADMIN')")`
  - `GET /{year}/{semester}/candidates` - 등록 후보 회원 목록 조회
  - `POST /{year}/{semester}/members` - 회원 일괄 등록
  - Swagger 어노테이션 (`@Operation`, `@ApiResponse`, `@Tag`) 추가

- [x] T018 [US1] SecurityConfig에 학기별 회원 관리 API 경로 권한 설정 in `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java`
  - `/api/v1/admin/semesters/**` → ADMIN only (`hasRole('ADMIN')`)
  - `/api/v1/semesters/**` → OPERATOR, ADMIN (`hasAnyRole('OPERATOR', 'ADMIN')`)

**Checkpoint**: ✅ User Story 1 완료 - 학기별 회원 등록 기능 독립적으로 테스트 가능

---

## Phase 4: User Story 2 - 학기별 회원 제외 (Priority: P1) ✅

**Goal**: 관리자가 특정 학기에서 잘못 등록된 회원을 제외(삭제)할 수 있다

**Independent Test**: 관리자가 특정 학기 회원을 선택하여 제외하면 멤버십 기록이 삭제되고, 해당 학기 명단에서 제거된다

### Acceptance Criteria

1. 관리자가 특정 학기 회원을 선택하고 제외 실행 시 멤버십 기록 삭제
2. 제외 후 해당 학기 명단 재조회 시 제외된 회원 미표시

### Implementation for User Story 2

- [x] T019 [P] [US2] RemoveSemesterMembersRequest DTO 구현 in `backend/src/main/java/igrus/web/user/semester/dto/request/RemoveSemesterMembersRequest.java`
  - 필드: `List<Long> userIds` (@NotEmpty, 제외할 회원 ID 목록)

- [x] T020 [US2] SemesterMemberService - 제외 관련 메서드 구현 in `backend/src/main/java/igrus/web/user/semester/service/SemesterMemberService.java`
  - `removeMembers(int year, int semester, List<Long> userIds, Long adminId)`: int (제외된 회원 수)
  - 존재하지 않는 멤버십 기록에 대한 예외 처리

- [x] T021 [US2] AdminSemesterMemberController에 제외 엔드포인트 추가 in `backend/src/main/java/igrus/web/user/semester/controller/AdminSemesterMemberController.java`
  - `DELETE /{year}/{semester}/members` - 회원 일괄 제외
  - RequestBody로 제외할 회원 ID 목록 수신
  - Swagger 어노테이션 추가

**Checkpoint**: ✅ User Story 2 완료 - 학기별 회원 제외 기능 독립적으로 테스트 가능

---

## Phase 5: User Story 3 - 학기별 명단 조회 (Priority: P1) ✅

**Goal**: 운영진(OPERATOR) 이상 권한의 사용자가 학기별 회원 명단을 조회할 수 있다 (탈퇴 회원 포함)

**Independent Test**: 운영진이 학기 목록을 조회하고, 특정 학기를 선택하여 해당 학기의 전체 회원 명단(탈퇴자 포함)을 확인할 수 있다

### Acceptance Criteria

1. 학기 목록 조회 시 멤버십 기록이 존재하는 학기가 회원 수와 함께 최신순으로 표시
2. 특정 학기 선택 시 회원 목록(학번, 본명, 학과, 역할, 이메일, 전화번호) 표시
3. 탈퇴 회원의 정보도 정상 표시 (탈퇴 여부 표시 포함)
4. MEMBER 권한으로 접근 시 403 Forbidden

### Implementation for User Story 3

- [x] T022 [P] [US3] SemesterSummaryResponse DTO 구현 in `backend/src/main/java/igrus/web/user/semester/dto/response/SemesterSummaryResponse.java`
  - 필드: `year`, `semester`, `memberCount`, `displayName` (예: "2026년 1학기")

- [x] T023 [P] [US3] SemesterMemberListResponse DTO 구현 in `backend/src/main/java/igrus/web/user/semester/dto/response/SemesterMemberListResponse.java`
  - 필드: `userId`, `studentId`, `name`, `department`, `email`, `phoneNumber`, `role` (등록 시점 역할), `isWithdrawn` (탈퇴 여부)

- [x] T024 [US3] SemesterMemberRepository에 Native 쿼리 구현 in `backend/src/main/java/igrus/web/user/semester/repository/SemesterMemberRepository.java`
  - `findAllWithUserIncludingDeleted(int year, int semester)`: User `@SQLRestriction` 필터를 우회하여 탈퇴 회원 포함 조회
  - Native SQL로 `semester_members sm JOIN users u ON sm.semester_members_user_id = u.users_id` (WHERE 절에 `users_deleted = false` 조건 없음)
  - `findDistinctSemestersWithCount()`: `SELECT year, semester, COUNT(*) ... GROUP BY year, semester ORDER BY year DESC, semester DESC`

- [x] T025 [US3] SemesterMemberService - 조회 관련 메서드 구현 in `backend/src/main/java/igrus/web/user/semester/service/SemesterMemberService.java`
  - `getSemesterList()`: List<SemesterSummaryResponse> - 학기 목록 (회원 수 포함, 최신순)
  - `getMemberList(int year, int semester, String keyword)`: List<SemesterMemberListResponse> - 학기별 회원 명단 (탈퇴자 포함)
  - 검색 기능: keyword로 학번, 본명 검색 지원
  - Native 쿼리 결과를 DTO로 매핑

- [x] T026 [US3] SemesterMemberController 구현 - 조회 엔드포인트 in `backend/src/main/java/igrus/web/user/semester/controller/SemesterMemberController.java`
  - `@RestController`, `@RequestMapping("/api/v1/semesters")`, `@PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")`
  - `GET /` - 학기 목록 조회 (회원 수 포함)
  - `GET /{year}/{semester}/members` - 학기별 회원 명단 조회 (검색 파라미터: `keyword`)
  - Swagger 어노테이션 (`@Operation`, `@ApiResponse`, `@Tag`) 추가

**Checkpoint**: ✅ User Story 3 완료 - 학기별 명단 조회 기능 독립적으로 테스트 가능 (탈퇴 회원 포함)

---

## Phase 6: Polish & Cross-Cutting Concerns ✅

**Purpose**: 전체 기능에 걸친 개선 및 품질 향상

- [x] T027 [P] Swagger API 문서 검토 및 보완 in `backend/src/main/java/igrus/web/user/semester/controller/`
  - 모든 엔드포인트에 `@Operation`, `@ApiResponse` 어노테이션 확인
  - 요청/응답 DTO에 `@Schema` 어노테이션 확인
  - API 그룹 `@Tag(name = "Semester Member")` 설정

- [x] T028 [P] 기존 코드 영향 범위 최종 검증
  - `isWithdrawnOrAnonymized()` 참조가 모두 `isWithdrawn()` 으로 변경되었는지 확인
  - 익명화 관련 코드(anonymize, anonymized)가 완전히 제거되었는지 확인
  - WithdrawnUserCleanupService가 정상 동작하는지 확인 (hard delete만 수행)

- [x] T029 feature spec 문서 상태를 Draft → In Progress 로 업데이트 in `docs/feature/member-list/member-list-spec.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: 의존성 없음 - 즉시 시작 가능
- **Phase 2 (Foundational)**: Phase 1 완료 필요 - 모든 User Story 블로킹
  - 2.1 익명화 제거: 독립 실행 가능
  - 2.2 SemesterMember 인프라: T008(마이그레이션) → T009(엔티티) → T010(리포지토리) 순서
  - 2.1과 2.2는 병렬 진행 가능
- **Phase 3 (US1)**: Phase 2 완료 필요 - MVP
- **Phase 4 (US2)**: Phase 2 완료 필요, Phase 3와 병렬 가능
- **Phase 5 (US3)**: Phase 2 완료 필요, Phase 3/4와 병렬 가능
- **Phase 6 (Polish)**: 모든 User Story 완료 필요

### User Story Dependencies

- **User Story 1 (P1)**: Phase 2 완료 후 시작 - MVP. 다른 스토리에 대한 의존성 없음
- **User Story 2 (P1)**: Phase 2 완료 후 시작 - US1과 병렬 가능 (같은 Controller 파일 주의)
- **User Story 3 (P1)**: Phase 2 완료 후 시작 - US1/US2와 독립적 (별도 Controller)

### Within Each User Story

- DTO → Service → Controller 순서
- 핵심 기능 → 부가 기능 순서

### Parallel Opportunities

- Phase 2의 T002~T007 (익명화 제거)와 T008~T012 (SemesterMember 인프라) 병렬 가능
- Phase 2의 T010, T011, T012 병렬 실행 가능
- Phase 3의 T013, T014, T015 DTO 병렬 구현 가능
- Phase 5의 T022, T023 DTO 병렬 구현 가능
- US1과 US3는 별도 Controller이므로 완전 병렬 가능
- US1과 US2는 같은 AdminSemesterMemberController를 수정하므로 순차 진행 권장

---

## Parallel Example: Phase 2 Foundation

```bash
# 익명화 제거와 SemesterMember 인프라 병렬 진행
# 스트림 A: 익명화 제거
Task: "Flyway V14 마이그레이션 작성"
Task: "User 엔티티 익명화 코드 제거"
Task: "호출부 변경"
Task: "CleanupService/Scheduler 수정"

# 스트림 B: SemesterMember 인프라
Task: "Flyway V15 마이그레이션 작성"
Task: "SemesterMember 엔티티 구현"
Task: "SemesterMemberRepository 구현"
Task: "ErrorCode 추가"
Task: "예외 클래스 구현"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup 완료
2. Phase 2: Foundational 완료 (CRITICAL - 익명화 제거 + 엔티티 인프라)
3. Phase 3: User Story 1 완료
4. **STOP and VALIDATE**: 학기별 회원 등록 기능 독립 테스트
5. 배포/데모 가능

### Incremental Delivery

1. Setup + Foundational → Foundation 완료
2. User Story 1 → 테스트 → 배포/데모 (MVP!)
3. User Story 2 → 테스트 → 배포/데모 (등록 + 제외)
4. User Story 3 → 테스트 → 배포/데모 (조회 기능 추가)
5. 각 스토리가 이전 스토리를 깨뜨리지 않고 가치 추가

### 주의사항

- 익명화 제거는 기존 코드에 광범위한 영향이 있으므로 `isWithdrawnOrAnonymized()` 참조를 빠짐없이 변경해야 한다
- 이미 익명화된 사용자 데이터는 복구 불가하므로 마이그레이션에서 별도 처리 불필요
- SemesterMember의 Native 쿼리는 User `@SQLRestriction`을 우회하여 탈퇴 회원도 포함해야 한다
- 학기(semester) 값은 1 또는 2만 허용하도록 서비스 레벨에서 검증

---

## Notes

- [P] 태스크 = 다른 파일, 의존성 없음
- [Story] 라벨로 User Story 추적 가능
- 각 User Story는 독립적으로 완료 및 테스트 가능
- 태스크 또는 논리적 그룹 완료 후 커밋
- Checkpoint에서 스토리 독립 검증 가능
- 모호한 태스크, 같은 파일 충돌, 독립성 깨는 크로스-스토리 의존성 지양

---

## Summary

| Phase | Task Count | Completed | Status |
|-------|------------|-----------|--------|
| Phase 1: Setup | 1 | 1 | ✅ |
| Phase 2: Foundational | 11 | 11 | ✅ |
| Phase 3: User Story 1 (MVP) | 6 | 6 | ✅ |
| Phase 4: User Story 2 | 3 | 3 | ✅ |
| Phase 5: User Story 3 | 5 | 5 | ✅ |
| Phase 6: Polish | 3 | 3 | ✅ |
| **Total** | **29** | **29** | **✅ 100%** |

**Status**: 모든 태스크 구현 완료 (테스트 코드 작성은 별도 요청 시 진행)
