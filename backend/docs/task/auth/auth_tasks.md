# Tasks: 로그인/회원가입 백엔드 구현

**Input**: [auth-spec.md](/docs/feature/auth/auth-spec.md), [user-entity-design.md](/docs/feature/auth/user-entity-design.md)
**Prerequisites**: User 도메인 엔티티 (구현 완료), JWT 인프라 (구현 완료)

**Tests**: 백엔드 CLAUDE.md의 개발 워크플로우에 따라 각 User Story별 테스트 포함

**Organization**: User Story 기반으로 태스크를 구성하여 독립적인 구현 및 테스트 가능

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 해당 태스크가 속한 User Story (예: US1, US2, US3)
- 설명에 정확한 파일 경로 포함

## 기존 구현 현황

### 구현 완료
- User, PasswordCredential, Position, UserPosition 엔티티
- UserRoleHistory, UserSuspension 엔티티
- UserRole, UserStatus Enum
- JwtTokenProvider (토큰 생성/검증)
- JwtAuthenticationFilter (인증 필터)
- SecurityConfig (기본 설정)
- BCryptPasswordEncoder

### 미구현 (이 태스크에서 구현)
- Auth Controller, Service
- EmailVerification 엔티티 및 기능
- RefreshToken 엔티티 및 관리
- PrivacyConsent 엔티티
- 비밀번호 재설정 기능
- 계정 복구 기능
- 준회원 승인 기능
- 인증 관련 DTO, Exception, ErrorCode

---

## Phase 1: Setup (공통 인프라)

**Purpose**: 인증 기능 구현을 위한 기반 구조 설정

- [ ] T001 [P] 인증 관련 ErrorCode 추가 in `backend/src/main/java/igrus/web/common/exception/ErrorCode.java`
- [ ] T002 [P] 인증 관련 커스텀 예외 클래스 생성 in `backend/src/main/java/igrus/web/auth/exception/`
- [ ] T003 [P] 이메일 발송 설정 추가 in `backend/src/main/resources/application.yml`
- [ ] T004 [P] 이메일 발송 서비스 인터페이스 정의 in `backend/src/main/java/igrus/web/auth/service/EmailService.java`
- [ ] T005 [P] SMTP 이메일 발송 구현체 생성 in `backend/src/main/java/igrus/web/auth/service/SmtpEmailService.java`

---

## Phase 2: Foundational (핵심 엔티티 및 기반 기능)

**Purpose**: 모든 User Story에서 필요한 핵심 엔티티 및 기반 기능

**⚠️ CRITICAL**: User Story 작업 전 반드시 완료 필요

### 엔티티 및 마이그레이션

- [ ] T006 [P] EmailVerification 엔티티 생성 in `backend/src/main/java/igrus/web/auth/domain/EmailVerification.java`
- [ ] T007 [P] RefreshToken 엔티티 생성 in `backend/src/main/java/igrus/web/auth/domain/RefreshToken.java`
- [ ] T008 [P] PrivacyConsent 엔티티 생성 in `backend/src/main/java/igrus/web/auth/domain/PrivacyConsent.java`
- [ ] T009 [P] PasswordResetToken 엔티티 생성 in `backend/src/main/java/igrus/web/auth/domain/PasswordResetToken.java`
- [ ] T010 Flyway 마이그레이션 V7 생성 (auth 테이블) in `backend/src/main/resources/db/migration/V7__add_auth_tables.sql`

### Repository

- [ ] T011 [P] EmailVerificationRepository 생성 in `backend/src/main/java/igrus/web/auth/repository/EmailVerificationRepository.java`
- [ ] T012 [P] RefreshTokenRepository 생성 in `backend/src/main/java/igrus/web/auth/repository/RefreshTokenRepository.java`
- [ ] T013 [P] PrivacyConsentRepository 생성 in `backend/src/main/java/igrus/web/auth/repository/PrivacyConsentRepository.java`
- [ ] T014 [P] PasswordResetTokenRepository 생성 in `backend/src/main/java/igrus/web/auth/repository/PasswordResetTokenRepository.java`

### Repository 확장 (User 도메인)

- [ ] T015 [P] UserRepository에 findByStudentId, findByEmail, existsByStudentId 등 쿼리 메서드 추가 in `backend/src/main/java/igrus/web/user/repository/UserRepository.java`
- [ ] T016 [P] PasswordCredentialRepository에 findByUserId 등 쿼리 메서드 추가 in `backend/src/main/java/igrus/web/user/repository/PasswordCredentialRepository.java`

**Checkpoint**: Foundation 완료 - User Story 구현 시작 가능

---

## Phase 3: User Story 1 - 회원가입 (Priority: P1) 🎯 MVP

**Goal**: 비회원이 필수 정보를 입력하고 이메일 인증을 완료하여 준회원으로 등록

**Independent Test**: 회원가입 폼 작성 → 이메일 인증 → 로그인 성공 확인

### DTO for User Story 1

- [ ] T017 [P] [US1] SignupRequest DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/request/SignupRequest.java`
- [ ] T018 [P] [US1] EmailVerificationRequest DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/request/EmailVerificationRequest.java`
- [ ] T019 [P] [US1] ResendVerificationRequest DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/request/ResendVerificationRequest.java`
- [ ] T020 [P] [US1] SignupResponse DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/response/SignupResponse.java`

### Service for User Story 1

- [ ] T021 [US1] SignupService 생성 - 회원가입 비즈니스 로직 in `backend/src/main/java/igrus/web/auth/service/SignupService.java`
  - 개인정보 동의 검증
  - 중복 검증 (학번, 이메일, 전화번호)
  - 비밀번호 정책 검증 (영문 대/소문자 + 숫자 + 특수문자, 8자 이상)
  - 임시 사용자 데이터 저장
  - 인증 코드 생성 및 이메일 발송
  - 인증 코드 검증 (10분 유효, 5회 제한)
  - 준회원(ASSOCIATE) 등록

### Controller for User Story 1

- [ ] T022 [US1] AuthController - 회원가입 엔드포인트 구현 in `backend/src/main/java/igrus/web/auth/controller/AuthController.java`
  - POST /api/v1/auth/signup (회원가입 요청)
  - POST /api/v1/auth/verify-email (이메일 인증)
  - POST /api/v1/auth/resend-verification (인증 코드 재발송)

### Test for User Story 1

- [ ] T023 [P] [US1] SignupService 단위 테스트 in `backend/src/test/java/igrus/web/auth/service/SignupServiceTest.java`
- [ ] T024 [P] [US1] AuthController 회원가입 통합 테스트 in `backend/src/test/java/igrus/web/auth/controller/AuthControllerSignupTest.java`

**Checkpoint**: 회원가입 기능 완료 - 독립적으로 테스트 가능

---

## Phase 4: User Story 2 - 로그인 (Priority: P1) 🎯 MVP

**Goal**: 등록된 사용자가 학번과 비밀번호로 로그인하여 토큰 발급

**Independent Test**: 등록된 계정으로 로그인 → Access Token + Refresh Token 발급 확인

### DTO for User Story 2

- [ ] T025 [P] [US2] LoginRequest DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/request/LoginRequest.java`
- [ ] T026 [P] [US2] LoginResponse DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/response/LoginResponse.java`
- [ ] T027 [P] [US2] LogoutRequest DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/request/LogoutRequest.java`

### Service for User Story 2

- [ ] T028 [US2] AuthService 생성 - 로그인/로그아웃 비즈니스 로직 in `backend/src/main/java/igrus/web/auth/service/AuthService.java`
  - 학번/비밀번호 인증
  - 이메일 인증 완료 여부 확인
  - 계정 상태 확인 (ACTIVE, SUSPENDED, WITHDRAWN)
  - Access Token (1시간) + Refresh Token (7일) 발급
  - Refresh Token DB 저장
  - 로그아웃 시 토큰 무효화

### Controller for User Story 2

- [ ] T029 [US2] AuthController - 로그인/로그아웃 엔드포인트 추가 in `backend/src/main/java/igrus/web/auth/controller/AuthController.java`
  - POST /api/v1/auth/login (로그인)
  - POST /api/v1/auth/logout (로그아웃)

### Test for User Story 2

- [ ] T030 [P] [US2] AuthService 로그인 단위 테스트 in `backend/src/test/java/igrus/web/auth/service/AuthServiceLoginTest.java`
- [ ] T031 [P] [US2] AuthController 로그인 통합 테스트 in `backend/src/test/java/igrus/web/auth/controller/AuthControllerLoginTest.java`

**Checkpoint**: 로그인/로그아웃 기능 완료 - 독립적으로 테스트 가능

---

## Phase 5: User Story 3 - 토큰 갱신 (Priority: P2)

**Goal**: Access Token 만료 시 Refresh Token으로 새 Access Token 발급

**Independent Test**: 만료된 Access Token 상태에서 Refresh Token으로 갱신 성공 확인

### DTO for User Story 3

- [ ] T032 [P] [US3] TokenRefreshRequest DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/request/TokenRefreshRequest.java`
- [ ] T033 [P] [US3] TokenRefreshResponse DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/response/TokenRefreshResponse.java`

### Service for User Story 3

- [ ] T034 [US3] TokenService 생성 - 토큰 갱신 비즈니스 로직 in `backend/src/main/java/igrus/web/auth/service/TokenService.java`
  - Refresh Token 유효성 검증
  - DB 저장 토큰과 비교
  - 새 Access Token 발급
  - (선택) Refresh Token Rotation

### Controller for User Story 3

- [ ] T035 [US3] AuthController - 토큰 갱신 엔드포인트 추가 in `backend/src/main/java/igrus/web/auth/controller/AuthController.java`
  - POST /api/v1/auth/refresh (토큰 갱신)

### Test for User Story 3

- [ ] T036 [P] [US3] TokenService 단위 테스트 in `backend/src/test/java/igrus/web/auth/service/TokenServiceTest.java`
- [ ] T037 [P] [US3] AuthController 토큰 갱신 통합 테스트 in `backend/src/test/java/igrus/web/auth/controller/AuthControllerTokenTest.java`

**Checkpoint**: 토큰 갱신 기능 완료 - 독립적으로 테스트 가능

---

## Phase 6: User Story 4 - 비밀번호 재설정 (Priority: P2)

**Goal**: 비밀번호를 잊은 사용자가 이메일을 통해 비밀번호 재설정

**Independent Test**: 비밀번호 재설정 요청 → 이메일 링크 → 새 비밀번호 설정 → 로그인 성공

### DTO for User Story 4

- [ ] T038 [P] [US4] PasswordResetRequest DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/request/PasswordResetRequest.java`
- [ ] T039 [P] [US4] PasswordResetConfirmRequest DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/request/PasswordResetConfirmRequest.java`

### Service for User Story 4

- [ ] T040 [US4] PasswordResetService 생성 in `backend/src/main/java/igrus/web/auth/service/PasswordResetService.java`
  - 학번으로 사용자 조회
  - 재설정 토큰 생성 (30분 유효)
  - 이메일로 재설정 링크 발송
  - 토큰 검증 및 비밀번호 변경
  - 모든 기존 Refresh Token 무효화

### Controller for User Story 4

- [ ] T041 [US4] AuthController - 비밀번호 재설정 엔드포인트 추가 in `backend/src/main/java/igrus/web/auth/controller/AuthController.java`
  - POST /api/v1/auth/password/reset-request (재설정 요청)
  - POST /api/v1/auth/password/reset-confirm (새 비밀번호 설정)

### Test for User Story 4

- [ ] T042 [P] [US4] PasswordResetService 단위 테스트 in `backend/src/test/java/igrus/web/auth/service/PasswordResetServiceTest.java`
- [ ] T043 [P] [US4] AuthController 비밀번호 재설정 통합 테스트 in `backend/src/test/java/igrus/web/auth/controller/AuthControllerPasswordResetTest.java`

**Checkpoint**: 비밀번호 재설정 기능 완료 - 독립적으로 테스트 가능

---

## Phase 7: User Story 5 - 탈퇴 계정 복구 (Priority: P3)

**Goal**: 탈퇴 후 5일 이내 계정 복구 기능 제공

**Independent Test**: 탈퇴 → 5일 이내 로그인 시도 → 복구 선택 → 계정 활성화

### DTO for User Story 5

- [ ] T044 [P] [US5] AccountRecoveryResponse DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/response/AccountRecoveryResponse.java`
- [ ] T045 [P] [US5] AccountRecoveryConfirmRequest DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/request/AccountRecoveryConfirmRequest.java`

### Service for User Story 5

- [ ] T046 [US5] AccountRecoveryService 생성 in `backend/src/main/java/igrus/web/auth/service/AccountRecoveryService.java`
  - 탈퇴 상태 및 복구 가능 기간 확인 (5일)
  - 계정 상태 ACTIVE로 전환
  - 탈퇴 후 5일 이내 재가입 차단 로직

### Service for User Story 5 (추가)

- [ ] T047 [US5] AuthService에 탈퇴 계정 로그인 시 복구 프롬프트 로직 추가 in `backend/src/main/java/igrus/web/auth/service/AuthService.java`

### Controller for User Story 5

- [ ] T048 [US5] AuthController - 계정 복구 엔드포인트 추가 in `backend/src/main/java/igrus/web/auth/controller/AuthController.java`
  - POST /api/v1/auth/account/recover (계정 복구)

### Test for User Story 5

- [ ] T049 [P] [US5] AccountRecoveryService 단위 테스트 in `backend/src/test/java/igrus/web/auth/service/AccountRecoveryServiceTest.java`
- [ ] T050 [P] [US5] AuthController 계정 복구 통합 테스트 in `backend/src/test/java/igrus/web/auth/controller/AuthControllerRecoveryTest.java`

**Checkpoint**: 탈퇴 계정 복구 기능 완료 - 독립적으로 테스트 가능

---

## Phase 8: User Story 6 - 준회원 승인 (Priority: P2)

**Goal**: 관리자가 준회원을 정회원으로 승인

**Independent Test**: 관리자 로그인 → 준회원 목록 조회 → 승인 → 역할 변경 확인

### DTO for User Story 6

- [ ] T051 [P] [US6] AssociateMemberResponse DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/response/AssociateMemberResponse.java`
- [ ] T052 [P] [US6] MemberApprovalRequest DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/request/MemberApprovalRequest.java`
- [ ] T053 [P] [US6] BulkApprovalRequest DTO 생성 in `backend/src/main/java/igrus/web/auth/dto/request/BulkApprovalRequest.java`

### Service for User Story 6

- [ ] T054 [US6] MemberApprovalService 생성 in `backend/src/main/java/igrus/web/auth/service/MemberApprovalService.java`
  - 준회원 목록 조회 (학번, 본명, 학과, 가입 동기)
  - 개별 승인 (ASSOCIATE → MEMBER)
  - 일괄 승인
  - 역할 변경 이력 기록

### Controller for User Story 6

- [ ] T055 [US6] AdminMemberController 생성 in `backend/src/main/java/igrus/web/auth/controller/AdminMemberController.java`
  - GET /api/v1/admin/members/pending (준회원 목록)
  - POST /api/v1/admin/members/{id}/approve (개별 승인)
  - POST /api/v1/admin/members/approve/bulk (일괄 승인)

### Test for User Story 6

- [ ] T056 [P] [US6] MemberApprovalService 단위 테스트 in `backend/src/test/java/igrus/web/auth/service/MemberApprovalServiceTest.java`
- [ ] T057 [P] [US6] AdminMemberController 통합 테스트 in `backend/src/test/java/igrus/web/auth/controller/AdminMemberControllerTest.java`

**Checkpoint**: 준회원 승인 기능 완료 - 독립적으로 테스트 가능

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: 여러 User Story에 걸친 개선사항

### 스케줄링 및 정리 작업

- [ ] T058 인증 미완료 임시 데이터 24시간 후 삭제 스케줄러 in `backend/src/main/java/igrus/web/auth/scheduler/AuthCleanupScheduler.java`
- [ ] T059 만료된 Refresh Token 정리 스케줄러 in `backend/src/main/java/igrus/web/auth/scheduler/AuthCleanupScheduler.java`
- [ ] T060 탈퇴 후 5일 경과 개인정보 영구 삭제 스케줄러 in `backend/src/main/java/igrus/web/auth/scheduler/AuthCleanupScheduler.java`

### 이메일 재시도 로직

- [ ] T061 이메일 발송 실패 시 재시도 로직 구현 (1분, 5분, 15분) in `backend/src/main/java/igrus/web/auth/service/SmtpEmailService.java`

### 보안 강화

- [ ] T062 JwtAuthenticationFilter 계정 상태 검증 추가 in `backend/src/main/java/igrus/web/security/jwt/JwtAuthenticationFilter.java`
- [ ] T063 SecurityConfig URL 패턴 최종 업데이트 in `backend/src/main/java/igrus/web/security/config/SecurityConfig.java`

### API 문서화

- [ ] T064 [P] AuthController Swagger 어노테이션 추가 in `backend/src/main/java/igrus/web/auth/controller/AuthController.java`
- [ ] T065 [P] AdminMemberController Swagger 어노테이션 추가 in `backend/src/main/java/igrus/web/auth/controller/AdminMemberController.java`

### 통합 테스트

- [ ] T066 전체 인증 플로우 E2E 테스트 in `backend/src/test/java/igrus/web/auth/AuthFlowIntegrationTest.java`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 의존성 없음 - 즉시 시작 가능
- **Foundational (Phase 2)**: Setup 완료 후 - 모든 User Story 차단
- **User Stories (Phase 3-8)**: Foundational 완료 후 시작 가능
  - US1 (회원가입), US2 (로그인)은 MVP로 우선 구현
  - 이후 US3-US6 순차 또는 병렬 구현 가능
- **Polish (Phase 9)**: 원하는 User Story 완료 후 진행

### User Story Dependencies

```
Phase 1 (Setup) ──────────────────────────┐
                                          ▼
Phase 2 (Foundational) ───────────────────┤
                                          │
    ┌─────────────────────────────────────┴─────────────────────────────────────┐
    │                                                                            │
    ▼                      ▼                     ▼                      ▼        │
US1 (회원가입) ───────► US2 (로그인) ───────► US3 (토큰 갱신)          │        │
    P1 MVP                 P1 MVP              P2                       │        │
                              │                                         │        │
                              │         ┌───────────────────────────────┘        │
                              │         │                                        │
                              ▼         ▼                      ▼                 │
                        US4 (비밀번호 재설정)           US5 (탈퇴 복구)     US6 (승인)
                              P2                        P3               P2
                                                                                 │
                                          ▼                                      │
                                 Phase 9 (Polish) ◄──────────────────────────────┘
```

### 의존성 상세

- **US1 (회원가입)**: Foundational 완료 후 독립 실행 가능
- **US2 (로그인)**: US1 완료 필요 (테스트를 위해 회원가입 필요)
- **US3 (토큰 갱신)**: US2 완료 필요 (로그인 후 Refresh Token 발급 필요)
- **US4 (비밀번호 재설정)**: US2 완료 필요 (로그인된 계정 필요)
- **US5 (탈퇴 복구)**: US2 완료 필요 (탈퇴 상태 확인 로직이 로그인 시 동작)
- **US6 (준회원 승인)**: US1 완료 필요 (승인 대상 준회원 필요)

### Within Each User Story

1. DTO 먼저 생성
2. Service 구현
3. Controller 구현
4. 테스트 작성 및 검증

### Parallel Opportunities

**Setup Phase (Phase 1)**:
```
T001, T002, T003, T004, T005 - 모두 병렬 실행 가능
```

**Foundational Phase (Phase 2)**:
```
T006, T007, T008, T009 - 엔티티 병렬 생성
T010 - 위 엔티티 완료 후 마이그레이션
T011, T012, T013, T014 - Repository 병렬 생성
T015, T016 - Repository 확장 병렬 실행
```

**User Story 1 (Phase 3)**:
```
T017, T018, T019, T020 - DTO 병렬 생성
T021 - Service (DTO 완료 후)
T022 - Controller (Service 완료 후)
T023, T024 - 테스트 병렬 실행
```

---

## Parallel Example: User Story 1

```bash
# Phase 3: DTO 병렬 생성
Task: "SignupRequest DTO 생성 in backend/src/main/java/igrus/web/auth/dto/request/SignupRequest.java"
Task: "EmailVerificationRequest DTO 생성 in backend/src/main/java/igrus/web/auth/dto/request/EmailVerificationRequest.java"
Task: "ResendVerificationRequest DTO 생성 in backend/src/main/java/igrus/web/auth/dto/request/ResendVerificationRequest.java"
Task: "SignupResponse DTO 생성 in backend/src/main/java/igrus/web/auth/dto/response/SignupResponse.java"

# DTO 완료 후 Service 구현
Task: "SignupService 생성 in backend/src/main/java/igrus/web/auth/service/SignupService.java"

# Service 완료 후 Controller 구현
Task: "AuthController 회원가입 엔드포인트 구현 in backend/src/main/java/igrus/web/auth/controller/AuthController.java"

# Controller 완료 후 테스트 병렬 실행
Task: "SignupService 단위 테스트 in backend/src/test/java/igrus/web/auth/service/SignupServiceTest.java"
Task: "AuthController 회원가입 통합 테스트 in backend/src/test/java/igrus/web/auth/controller/AuthControllerSignupTest.java"
```

---

## Implementation Strategy

### MVP First (User Story 1 + 2)

1. Phase 1: Setup 완료
2. Phase 2: Foundational 완료
3. Phase 3: User Story 1 (회원가입) 완료
4. Phase 4: User Story 2 (로그인) 완료
5. **STOP and VALIDATE**: 회원가입 → 로그인 플로우 테스트
6. Deploy/Demo (MVP 완료!)

### Incremental Delivery

1. Setup + Foundational → 기반 완료
2. US1 (회원가입) → 테스트 → Deploy
3. US2 (로그인) → 테스트 → Deploy (MVP!)
4. US3 (토큰 갱신) → 테스트 → Deploy
5. US4 (비밀번호 재설정) → 테스트 → Deploy
6. US5 (탈퇴 복구) → 테스트 → Deploy
7. US6 (준회원 승인) → 테스트 → Deploy
8. Polish → 최종 테스트 → Release

### Parallel Team Strategy

개발자 2명 이상인 경우:

1. 팀 전체: Setup + Foundational 완료
2. Foundational 완료 후:
   - 개발자 A: US1 (회원가입) → US2 (로그인) → US3 (토큰 갱신)
   - 개발자 B: US4 (비밀번호 재설정) → US5 (탈퇴 복구) → US6 (준회원 승인)
3. 각 Story 완료 후 통합 및 리뷰

---

## Summary

| Phase | 태스크 수 | 병렬 가능 | 설명 |
|-------|----------|----------|------|
| Phase 1: Setup | 5 | 5 | 공통 인프라 |
| Phase 2: Foundational | 11 | 10 | 핵심 엔티티/Repository |
| Phase 3: US1 회원가입 | 8 | 6 | MVP |
| Phase 4: US2 로그인 | 7 | 5 | MVP |
| Phase 5: US3 토큰 갱신 | 6 | 4 | P2 |
| Phase 6: US4 비밀번호 재설정 | 6 | 4 | P2 |
| Phase 7: US5 탈퇴 복구 | 7 | 4 | P3 |
| Phase 8: US6 준회원 승인 | 7 | 5 | P2 |
| Phase 9: Polish | 9 | 2 | 정리 및 개선 |
| **Total** | **66** | **45** | |

### Suggested MVP Scope

- Phase 1 (Setup): 5 tasks
- Phase 2 (Foundational): 11 tasks
- Phase 3 (US1 회원가입): 8 tasks
- Phase 4 (US2 로그인): 7 tasks
- **MVP Total: 31 tasks**

---

## Notes

- [P] 태스크 = 다른 파일, 의존성 없음
- [Story] 라벨은 해당 User Story에 태스크 매핑
- 각 User Story는 독립적으로 완료 및 테스트 가능해야 함
- 태스크 완료 후 또는 논리적 그룹 단위로 커밋
- 체크포인트에서 Story 독립 검증 가능
- 피해야 할 것: 모호한 태스크, 같은 파일 충돌, Story 간 독립성 파괴하는 의존성
