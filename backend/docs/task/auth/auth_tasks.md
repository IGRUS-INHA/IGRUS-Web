# Tasks: 로그인/회원가입 백엔드 구현

**Input**: [auth-spec.md](/docs/feature/auth/auth-spec.md), [user-entity-design.md](/docs/feature/auth/user-entity-design.md)
**Prerequisites**: User 도메인 엔티티 (구현 완료), JWT 인프라 (구현 완료)

**Tests**: 백엔드 CLAUDE.md의 개발 워크플로우에 따라 각 User Story별 테스트 포함

**Organization**: User Story 기반으로 태스크를 구성하여 독립적인 구현 및 테스트 가능

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 실행 가능 (다른 파일, 의존성 없음)
- **[Story]**: 해당 태스크가 속한 User Story (예: US1, US2, US3)
- 설명에 정확한 파일 경로 포함

## ⚠️ 구현 경로 변경 안내

**실제 구현 경로**: 태스크 문서에서 명시된 `igrus/web/auth/` 경로가 아닌 `igrus/web/security/auth/` 경로에 구현되었습니다.
- 원본: `backend/src/main/java/igrus/web/auth/`
- 실제: `backend/src/main/java/igrus/web/security/auth/`

## 구현 현황 요약 (2026-01-25 기준)

| Phase | 총 태스크 | 완료 | 미완료 | 완료율 |
|-------|----------|------|--------|--------|
| Phase 1: Setup | 5 | 5 | 0 | 100% |
| Phase 2: Foundational | 11 | 11 | 0 | 100% |
| Phase 3: US1 회원가입 | 8 | 8 | 0 | 100% |
| Phase 4: US2 로그인 | 7 | 7 | 0 | 100% |
| Phase 5: US3 토큰 갱신 | 6 | 6 | 0 | 100% |
| Phase 6: US4 비밀번호 재설정 | 6 | 6 | 0 | 100% |
| Phase 7: US5 탈퇴 계정 복구 | 7 | 7 | 0 | 100% |
| Phase 8: US6 준회원 승인 | 7 | 7 | 0 | 100% |
| Phase 9: Polish | 10 | 10 | 0 | 100% |
| **Total** | **67** | **67** | **0** | **100%** |

### MVP 구현 현황 (Phase 1-4)
- **완료율: 100% (31/31 태스크)** ✅

## 기존 구현 현황

### 구현 완료
- User, PasswordCredential, Position, UserPosition 엔티티
- UserRoleHistory, UserSuspension 엔티티
- UserRole, UserStatus Enum
- JwtTokenProvider (토큰 생성/검증)
- JwtAuthenticationFilter (인증 필터)
- SecurityConfig → ApiSecurityConfig, PublicResourceSecurityConfig로 분리
- BCryptPasswordEncoder

### 추가 구현 완료 (이 태스크에서)
- ✅ Auth Controller (PasswordAuthController), Service (PasswordAuthService, SignupService, VerifyEmailService, ResendVerificationService)
- ✅ EmailVerification 엔티티 및 기능
- ✅ RefreshToken 엔티티 및 관리
- ✅ PrivacyConsent 엔티티 및 서비스 (PrivacyConsentService)
- ✅ 비밀번호 재설정 서비스 (PasswordResetService) 및 컨트롤러
- ✅ 계정 복구 서비스 (AccountRecoveryService) 및 컨트롤러
- ✅ 준회원 승인 서비스 (MemberApprovalService) 및 AdminMemberController
- ✅ 인증 관련 DTO, Exception (27개 커스텀 예외), ErrorCode
- ✅ 이메일 재시도 로직 (Spring Retry + Async, 지수 백오프 1분→3분→9분)
- ✅ Brute Force 방지 (LoginAttemptService, LoginAttempt 엔티티)
- ✅ AuthenticatedUser 도메인 클래스 (인증된 사용자 정보 캡슐화)

---

## Phase 1: Setup (공통 인프라)

**Purpose**: 인증 기능 구현을 위한 기반 구조 설정

**Status**: ✅ 완료 (5/5)

- [x] T001 [P] 인증 관련 ErrorCode 추가 in `backend/src/main/java/igrus/web/common/exception/ErrorCode.java`
  - 실제 구현: Auth (A001-A021), Member Approval (M001-M004) 에러 코드 추가됨
- [x] T002 [P] 인증 관련 커스텀 예외 클래스 생성 in `backend/src/main/java/igrus/web/security/auth/**/exception/`
  - 실제 구현: email/, verification/, token/, account/, signup/, approval/exception 패키지에 분산 구현
- [x] T003 [P] 이메일 발송 설정 추가 in `backend/src/main/resources/application.yml`
  - 실제 구현: app.mail.* 설정 추가됨 (from-address, verification-code-expiry, verification-max-attempts 등)
- [x] T004 [P] 이메일 발송 서비스 인터페이스 정의 in `backend/src/main/java/igrus/web/security/auth/common/service/EmailService.java`
- [x] T005 [P] SMTP 이메일 발송 구현체 생성 in `backend/src/main/java/igrus/web/security/auth/common/service/SmtpEmailService.java`
  - 추가 구현: LoggingEmailService (local, test 프로파일용)

---

## Phase 2: Foundational (핵심 엔티티 및 기반 기능)

**Purpose**: 모든 User Story에서 필요한 핵심 엔티티 및 기반 기능

**Status**: ✅ 완료 (11/11)

### 엔티티 및 마이그레이션

- [x] T006 [P] EmailVerification 엔티티 생성 in `backend/src/main/java/igrus/web/security/auth/common/domain/EmailVerification.java`
- [x] T007 [P] RefreshToken 엔티티 생성 in `backend/src/main/java/igrus/web/security/auth/common/domain/RefreshToken.java`
- [x] T008 [P] PrivacyConsent 엔티티 생성 in `backend/src/main/java/igrus/web/security/auth/common/domain/PrivacyConsent.java`
- [x] T009 [P] PasswordResetToken 엔티티 생성 in `backend/src/main/java/igrus/web/security/auth/password/domain/PasswordResetToken.java`
- [x] T010 Flyway 마이그레이션 생성 (auth 테이블) in `backend/src/main/resources/db/migration/V1__init_schema.sql`
  - 변경: V7 대신 V1에 통합됨 (email_verifications, refresh_tokens, privacy_consents, password_reset_tokens 테이블 포함)

### Repository

- [x] T011 [P] EmailVerificationRepository 생성 in `backend/src/main/java/igrus/web/security/auth/common/repository/EmailVerificationRepository.java`
- [x] T012 [P] RefreshTokenRepository 생성 in `backend/src/main/java/igrus/web/security/auth/common/repository/RefreshTokenRepository.java`
- [x] T013 [P] PrivacyConsentRepository 생성 in `backend/src/main/java/igrus/web/security/auth/common/repository/PrivacyConsentRepository.java`
- [x] T014 [P] PasswordResetTokenRepository 생성 in `backend/src/main/java/igrus/web/security/auth/password/repository/PasswordResetTokenRepository.java`

### Repository 확장 (User 도메인)

- [x] T015 [P] UserRepository에 findByStudentId, findByEmail, existsByStudentId 등 쿼리 메서드 추가 in `backend/src/main/java/igrus/web/user/repository/UserRepository.java`
  - 구현: findByEmail, findByStudentId, findByPhoneNumber, existsByEmail, existsByStudentId, existsByPhoneNumber, findByIdIncludingDeleted, findByEmailIncludingDeleted, findByStudentIdIncludingDeleted
- [x] T016 [P] PasswordCredentialRepository에 findByUserId 등 쿼리 메서드 추가 in `backend/src/main/java/igrus/web/security/auth/password/repository/PasswordCredentialRepository.java`

**Checkpoint**: ✅ Foundation 완료 - User Story 구현 시작 가능

---

## Phase 3: User Story 1 - 회원가입 (Priority: P1) 🎯 MVP

**Goal**: 비회원이 필수 정보를 입력하고 이메일 인증을 완료하여 준회원으로 등록

**Status**: ✅ 완료 (8/8)

**Independent Test**: 회원가입 폼 작성 → 이메일 인증 → 로그인 성공 확인

### DTO for User Story 1

- [x] T017 [P] [US1] SignupRequest DTO 생성 in `backend/src/main/java/igrus/web/security/auth/password/dto/request/PasswordSignupRequest.java`
  - 변경: SignupRequest → PasswordSignupRequest (패스워드 기반 인증 명시)
- [x] T018 [P] [US1] EmailVerificationRequest DTO 생성 in `backend/src/main/java/igrus/web/security/auth/common/dto/request/EmailVerificationRequest.java`
- [x] T019 [P] [US1] ResendVerificationRequest DTO 생성 in `backend/src/main/java/igrus/web/security/auth/common/dto/request/ResendVerificationRequest.java`
- [x] T020 [P] [US1] SignupResponse DTO 생성 in `backend/src/main/java/igrus/web/security/auth/password/dto/response/PasswordSignupResponse.java`
  - 변경: SignupResponse → PasswordSignupResponse

### Service for User Story 1

- [x] T021 [US1] SignupService 생성 - 회원가입 비즈니스 로직 in `backend/src/main/java/igrus/web/security/auth/password/service/SignupService, VerifyEmailService, ResendVerificationService.java`
  - ✅ 개인정보 동의 검증
  - ✅ 중복 검증 (학번, 이메일, 전화번호)
  - ✅ 비밀번호 정책 검증 (영문 대/소문자 + 숫자 + 특수문자, 8자 이상)
  - ✅ 임시 사용자 데이터 저장
  - ✅ 인증 코드 생성 및 이메일 발송
  - ✅ 인증 코드 검증 (10분 유효, 5회 제한)
  - ✅ 준회원(ASSOCIATE) 등록

### Controller for User Story 1

- [x] T022 [US1] AuthController - 회원가입 엔드포인트 구현 in `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthController.java`
  - ✅ POST /api/v1/auth/password/signup (회원가입 요청) - 경로 변경
  - ✅ POST /api/v1/auth/password/verify-email (이메일 인증) - 경로 변경
  - ✅ POST /api/v1/auth/password/resend-verification (인증 코드 재발송) - 경로 변경
  - 추가: PasswordAuthControllerApi (Swagger 문서화)

### Test for User Story 1

- [x] T023 [P] [US1] SignupService 단위 테스트 in `backend/src/test/java/igrus/web/security/auth/password/service/SignupService, VerifyEmailService, ResendVerificationServiceTest.java`
- [x] T024 [P] [US1] AuthController 회원가입 통합 테스트 in `backend/src/test/java/igrus/web/security/auth/password/integration/PasswordSignupIntegrationTest.java`

**Checkpoint**: ✅ 회원가입 기능 완료

---

## Phase 4: User Story 2 - 로그인 (Priority: P1) 🎯 MVP

**Goal**: 등록된 사용자가 학번과 비밀번호로 로그인하여 토큰 발급

**Status**: ✅ 완료 (7/7)

**Independent Test**: 등록된 계정으로 로그인 → Access Token + Refresh Token 발급 확인

### DTO for User Story 2

- [x] T025 [P] [US2] LoginRequest DTO 생성 in `backend/src/main/java/igrus/web/security/auth/password/dto/request/PasswordLoginRequest.java`
  - 변경: LoginRequest → PasswordLoginRequest
- [x] T026 [P] [US2] LoginResponse DTO 생성 in `backend/src/main/java/igrus/web/security/auth/password/dto/response/PasswordLoginResponse.java`
  - 변경: LoginResponse → PasswordLoginResponse
- [x] T027 [P] [US2] LogoutRequest DTO 생성 in `backend/src/main/java/igrus/web/security/auth/password/dto/request/PasswordLogoutRequest.java`
  - 변경: LogoutRequest → PasswordLogoutRequest

### Service for User Story 2

- [x] T028 [US2] AuthService 생성 - 로그인/로그아웃 비즈니스 로직 in `backend/src/main/java/igrus/web/security/auth/password/service/PasswordAuthService.java`
  - ✅ 학번/비밀번호 인증
  - ✅ 이메일 인증 완료 여부 확인
  - ✅ 계정 상태 확인 (ACTIVE, SUSPENDED, WITHDRAWN)
  - ✅ Access Token (1시간) + Refresh Token (7일) 발급
  - ✅ Refresh Token DB 저장
  - ✅ 로그아웃 시 토큰 무효화

### Controller for User Story 2

- [x] T029 [US2] AuthController - 로그인/로그아웃 엔드포인트 추가 in `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthController.java`
  - ✅ POST /api/v1/auth/password/login (로그인) - 경로 변경
  - ✅ POST /api/v1/auth/password/logout (로그아웃) - 경로 변경

### Test for User Story 2

- [x] T030 [P] [US2] AuthService 로그인 단위 테스트 in `backend/src/test/java/igrus/web/security/auth/password/service/PasswordAuthServiceLoginTest.java`
- [x] T031 [P] [US2] AuthController 로그인 통합 테스트 in `backend/src/test/java/igrus/web/security/auth/password/integration/PasswordLoginIntegrationTest.java`

**Checkpoint**: ✅ 로그인/로그아웃 기능 완료

---

## Phase 5: User Story 3 - 토큰 갱신 (Priority: P2)

**Goal**: Access Token 만료 시 Refresh Token으로 새 Access Token 발급

**Status**: ✅ 완료 (6/6)

**Independent Test**: 만료된 Access Token 상태에서 Refresh Token으로 갱신 성공 확인

### DTO for User Story 3

- [x] T032 [P] [US3] TokenRefreshRequest DTO 생성 in `backend/src/main/java/igrus/web/security/auth/password/dto/request/TokenRefreshRequest.java`
- [x] T033 [P] [US3] TokenRefreshResponse DTO 생성 in `backend/src/main/java/igrus/web/security/auth/password/dto/response/TokenRefreshResponse.java`

### Service for User Story 3

- [x] T034 [US3] TokenService 생성 - 토큰 갱신 비즈니스 로직 in `backend/src/main/java/igrus/web/security/auth/password/service/PasswordAuthService.java`
  - 변경: 별도 TokenService가 아닌 PasswordAuthService.refreshToken() 메서드로 구현
  - ✅ Refresh Token 유효성 검증
  - ✅ DB 저장 토큰과 비교
  - ✅ 새 Access Token 발급
  - ❌ (선택) Refresh Token Rotation - 미구현

### Controller for User Story 3

- [x] T035 [US3] AuthController - 토큰 갱신 엔드포인트 추가 in `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthController.java`
  - ✅ POST /api/v1/auth/password/refresh (토큰 갱신) - 경로 변경

### Test for User Story 3

- [x] T036 [P] [US3] TokenService 단위 테스트 in `backend/src/test/java/igrus/web/security/auth/password/service/PasswordAuthServiceTokenTest.java`
- [x] T037 [P] [US3] AuthController 토큰 갱신 통합 테스트 in `backend/src/test/java/igrus/web/security/auth/password/integration/TokenRefreshIntegrationTest.java`

**Checkpoint**: ✅ 토큰 갱신 기능 완료

---

## Phase 6: User Story 4 - 비밀번호 재설정 (Priority: P2)

**Goal**: 비밀번호를 잊은 사용자가 이메일을 통해 비밀번호 재설정

**Status**: ✅ 완료 (6/6)

**Independent Test**: 비밀번호 재설정 요청 → 이메일 링크 → 새 비밀번호 설정 → 로그인 성공

### DTO for User Story 4

- [x] T038 [P] [US4] PasswordResetRequest DTO 생성 in `backend/src/main/java/igrus/web/security/auth/password/dto/request/PasswordResetRequest.java`
- [x] T039 [P] [US4] PasswordResetConfirmRequest DTO 생성 in `backend/src/main/java/igrus/web/security/auth/password/dto/request/PasswordResetConfirmRequest.java`

### Service for User Story 4

- [x] T040 [US4] PasswordResetService 생성 in `backend/src/main/java/igrus/web/security/auth/password/service/PasswordResetService.java`
  - ✅ 학번으로 사용자 조회
  - ✅ 재설정 토큰 생성 (30분 유효)
  - ✅ 이메일로 재설정 링크 발송
  - ✅ 토큰 검증 및 비밀번호 변경
  - ✅ 모든 기존 Refresh Token 무효화

### Controller for User Story 4

- [x] T041 [US4] AuthController - 비밀번호 재설정 엔드포인트 추가 in `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthController.java`
  - ✅ POST /api/v1/auth/password/reset-request (재설정 요청)
  - ✅ POST /api/v1/auth/password/reset-confirm (새 비밀번호 설정)
  - ✅ GET /api/v1/auth/password/reset-validate (토큰 유효성 검증)

### Test for User Story 4

- [x] T042 [P] [US4] PasswordResetService 단위 테스트 in `backend/src/test/java/igrus/web/security/auth/password/service/PasswordResetServiceTest.java`
- [x] T043 [P] [US4] AuthController 비밀번호 재설정 통합 테스트 in `backend/src/test/java/igrus/web/security/auth/password/integration/PasswordResetIntegrationTest.java`

**Checkpoint**: ✅ 비밀번호 재설정 기능 완료

---

## Phase 7: User Story 5 - 탈퇴 계정 복구 (Priority: P3)

**Goal**: 탈퇴 후 5일 이내 계정 복구 기능 제공

**Status**: ✅ 완료 (7/7)

**Independent Test**: 탈퇴 → 5일 이내 로그인 시도 → 복구 선택 → 계정 활성화

### DTO for User Story 5

- [x] T044 [P] [US5] AccountRecoveryResponse DTO 생성 in `backend/src/main/java/igrus/web/security/auth/common/dto/response/AccountRecoveryResponse.java`
  - 추가 구현: RecoveryEligibilityResponse (복구 가능 여부 확인용)
- [x] T045 [P] [US5] AccountRecoveryConfirmRequest DTO 생성 in `backend/src/main/java/igrus/web/security/auth/common/dto/request/AccountRecoveryRequest.java`
  - 변경: AccountRecoveryConfirmRequest → AccountRecoveryRequest

### Service for User Story 5

- [x] T046 [US5] AccountRecoveryService 생성 in `backend/src/main/java/igrus/web/security/auth/common/service/AccountRecoveryService.java`
  - ✅ 탈퇴 상태 및 복구 가능 기간 확인 (5일)
  - ✅ 계정 상태 ACTIVE로 전환
  - ✅ 탈퇴 후 5일 이내 재가입 차단 로직

### Service for User Story 5 (추가)

- [x] T047 [US5] AuthService에 탈퇴 계정 로그인 시 복구 프롬프트 로직 추가
  - ✅ AccountRecoverableException 예외 활용
  - ✅ 탈퇴 계정이면서 복구 가능 기간(5일) 내인 경우 AccountRecoverableException 발생

### Controller for User Story 5

- [x] T048 [US5] AuthController - 계정 복구 엔드포인트 추가 in `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthController.java`
  - ✅ GET /api/v1/auth/password/account/recovery-check (복구 가능 여부 확인)
  - ✅ POST /api/v1/auth/password/account/recover (계정 복구)

### Test for User Story 5

- [x] T049 [P] [US5] AccountRecoveryService 단위 테스트 in `backend/src/test/java/igrus/web/security/auth/common/service/AccountRecoveryServiceTest.java`
- [x] T050 [P] [US5] AuthController 계정 복구 통합 테스트 in `backend/src/test/java/igrus/web/security/auth/common/integration/AccountRecoveryIntegrationTest.java`

**Checkpoint**: ✅ 탈퇴 계정 복구 기능 완료

---

## Phase 8: User Story 6 - 준회원 승인 (Priority: P2)

**Goal**: 관리자가 준회원을 정회원으로 승인

**Status**: ✅ 완료 (7/7)

**Independent Test**: 관리자 로그인 → 준회원 목록 조회 → 승인 → 역할 변경 확인

### DTO for User Story 6

- [x] T051 [P] [US6] AssociateMemberResponse DTO 생성 in `backend/src/main/java/igrus/web/security/auth/approval/dto/response/AssociateInfoResponse.java`
  - 변경: AssociateMemberResponse → AssociateInfoResponse
- [x] T052 [P] [US6] MemberApprovalRequest DTO 생성 in `backend/src/main/java/igrus/web/security/auth/approval/dto/request/MemberApprovalRequest.java`
- [x] T053 [P] [US6] BulkApprovalRequest DTO 생성 in `backend/src/main/java/igrus/web/security/auth/approval/dto/request/BulkApprovalRequest.java`

### Service for User Story 6

- [x] T054 [US6] MemberApprovalService 생성 in `backend/src/main/java/igrus/web/security/auth/approval/service/MemberApprovalService.java`
  - ✅ 준회원 목록 조회 (학번, 본명, 학과, 가입 동기)
  - ✅ 개별 승인 (ASSOCIATE → MEMBER)
  - ✅ 일괄 승인
  - ✅ 역할 변경 이력 기록

### Controller for User Story 6

- [x] T055 [US6] AdminMemberController 생성 in `backend/src/main/java/igrus/web/security/auth/approval/controller/AdminMemberController.java`
  - ✅ GET /api/v1/admin/members/pending (준회원 목록)
  - ✅ POST /api/v1/admin/members/{id}/approve (개별 승인)
  - ✅ POST /api/v1/admin/members/approve/bulk (일괄 승인)
  - ✅ Swagger 문서화 (컨트롤러에 직접 어노테이션 추가)

### Test for User Story 6

- [x] T056 [P] [US6] MemberApprovalService 단위 테스트 in `backend/src/test/java/igrus/web/security/auth/approval/service/MemberApprovalServiceTest.java`
- [x] T057 [P] [US6] AdminMemberController 통합 테스트 in `backend/src/test/java/igrus/web/security/auth/approval/controller/AdminMemberControllerTest.java`

**Checkpoint**: ✅ 준회원 승인 기능 완료

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: 여러 User Story에 걸친 개선사항

**Status**: ✅ 완료 (10/10)

### 스케줄링 및 정리 작업

- [x] T058 인증 미완료 임시 데이터 24시간 후 삭제 스케줄러 in `backend/src/main/java/igrus/web/security/auth/common/scheduler/UnverifiedUserCleanupScheduler.java`
  - ✅ 매일 새벽 3시 실행
  - ✅ 24시간 경과한 미인증 EmailVerification 및 관련 사용자 데이터 삭제
- [x] T059 만료된 Refresh Token 정리 스케줄러 in `backend/src/main/java/igrus/web/security/auth/common/scheduler/RefreshTokenCleanupScheduler.java`
  - ✅ 매일 새벽 4시 실행
  - ✅ 만료된 Refresh Token 자동 삭제
  - ✅ RefreshTokenCleanupService (`backend/src/main/java/igrus/web/security/auth/common/service/RefreshTokenCleanupService.java`)
  - ✅ RefreshTokenRepository.deleteByExpiresAtBefore 메서드 추가
  - ✅ 단위 테스트 5개 케이스 (`RefreshTokenCleanupServiceTest.java`)
- [x] T060 탈퇴 후 5일 경과 개인정보 영구 삭제 스케줄러 in `backend/src/main/java/igrus/web/security/auth/common/scheduler/WithdrawnUserCleanupScheduler.java`
  - ✅ 매일 새벽 5시 실행
  - ✅ 탈퇴 후 5일 경과한 사용자 개인정보 익명화
  - ✅ 연관 데이터 삭제 (PasswordCredential, PrivacyConsent, EmailVerification, RefreshToken)
  - ✅ User 엔티티에 anonymized 필드 및 anonymize() 메서드 추가
  - ✅ 단위 테스트 작성 (WithdrawnUserCleanupServiceTest)

### 이메일 재시도 로직

- [x] T061 이메일 발송 실패 시 재시도 로직 구현 (1분 → 3분 → 9분) - **2026-01-25 완료**
  - ✅ Spring Retry 의존성 추가 (`spring-retry`, `spring-aspects`)
  - ✅ RetryConfig 생성 (`@EnableRetry`)
  - ✅ AsyncConfig 생성 (`@EnableAsync`, `emailTaskExecutor` 스레드 풀)
  - ✅ EmailService 인터페이스에 `WithRetry` 메서드 추가
  - ✅ SmtpEmailService에 `@Retryable`, `@Async` 적용 (최대 4회 시도, 지수 백오프)
  - ✅ LoggingEmailService 동일하게 수정 (테스트용)
  - ✅ 기존 서비스(SignupService, VerifyEmailService, ResendVerificationService, PasswordResetService)에서 `WithRetry` 메서드 호출로 변경
  - ✅ 단위 테스트 작성 (SmtpEmailServiceRetryTest)

### 보안 강화

- [x] T062 JwtAuthenticationFilter 계정 상태 검증 추가 in `backend/src/main/java/igrus/web/security/jwt/JwtAuthenticationFilter.java`
  - ✅ AccountStatusService 생성 (`backend/src/main/java/igrus/web/security/auth/common/service/AccountStatusService.java`)
  - ✅ JwtAuthenticationFilter에서 토큰 유효성 검증 후 계정 상태 (SUSPENDED, WITHDRAWN) 검증
  - ✅ 단위 테스트 6개 케이스 (`AccountStatusServiceTest.java`)
  - ✅ 통합 테스트 7개 케이스 (`JwtAuthenticationFilterAccountStatusTest.java`)
- [x] T062-1 [추가] Brute Force 공격 방지 구현
  - ✅ LoginAttempt 엔티티 생성 (`backend/src/main/java/igrus/web/security/auth/common/domain/LoginAttempt.java`)
  - ✅ LoginAttemptRepository 생성 (`backend/src/main/java/igrus/web/security/auth/common/repository/LoginAttemptRepository.java`)
  - ✅ LoginAttemptService 생성 (`backend/src/main/java/igrus/web/security/auth/common/service/LoginAttemptService.java`)
  - ✅ 로그인 실패 횟수 추적 및 계정 잠금 (기본 30분)
  - ✅ 단위 테스트 (`LoginAttemptServiceTest.java`)
- [x] T063 SecurityConfig URL 패턴 최종 업데이트 in `backend/src/main/java/igrus/web/security/config/ApiSecurityConfig.java`
  - 변경: SecurityConfig → ApiSecurityConfig + PublicResourceSecurityConfig로 분리
  - ✅ /api/v1/auth/password/** 허용
  - ✅ /api/admin/** ADMIN 역할 필요
  - ✅ 운영진 (OPERATOR, ADMIN) 경로 설정

### API 문서화

- [x] T064 [P] AuthController Swagger 어노테이션 추가
  - 변경: Swagger 어노테이션을 컨트롤러에 직접 추가하는 방식으로 변경됨 (인터페이스 분리 방식 폐기)
- [x] T065 [P] AdminMemberController Swagger 어노테이션 추가 in `backend/src/main/java/igrus/web/security/auth/approval/controller/AdminMemberController.java`
  - 컨트롤러에 직접 Swagger 어노테이션 포함

### 통합 테스트

- [x] T066 전체 인증 플로우 E2E 테스트 in `backend/src/test/java/igrus/web/security/auth/e2e/AuthenticationE2ETest.java`

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

| Phase | 태스크 수 | 완료 | 미완료 | 완료율 | 설명 |
|-------|----------|------|--------|--------|------|
| Phase 1: Setup | 5 | 5 | 0 | 100% | 공통 인프라 ✅ |
| Phase 2: Foundational | 11 | 11 | 0 | 100% | 핵심 엔티티/Repository ✅ |
| Phase 3: US1 회원가입 | 8 | 8 | 0 | 100% | MVP ✅ |
| Phase 4: US2 로그인 | 7 | 7 | 0 | 100% | MVP ✅ |
| Phase 5: US3 토큰 갱신 | 6 | 6 | 0 | 100% | P2 ✅ |
| Phase 6: US4 비밀번호 재설정 | 6 | 6 | 0 | 100% | P2 ✅ |
| Phase 7: US5 탈퇴 복구 | 7 | 7 | 0 | 100% | P3 ✅ |
| Phase 8: US6 준회원 승인 | 7 | 7 | 0 | 100% | P2 ✅ |
| Phase 9: Polish | 10 | 10 | 0 | 100% | 정리 및 개선 ✅ |
| **Total** | **67** | **67** | **0** | **100%** | |

### MVP Scope 현황

- Phase 1 (Setup): 5/5 tasks ✅
- Phase 2 (Foundational): 11/11 tasks ✅
- Phase 3 (US1 회원가입): 8/8 tasks ✅
- Phase 4 (US2 로그인): 7/7 tasks ✅
- **MVP Total: 31/31 tasks (100%)** ✅

### 완료된 태스크 목록

모든 67개 태스크가 완료되었습니다. ✅

---

## 테스트 현황

### 단위/통합 테스트 (35개 파일)

**도메인 테스트:**
- `AuthenticatedUserTest.java`
- `EmailVerificationTest.java`
- `PrivacyConsentTest.java`
- `RefreshTokenTest.java`
- `PasswordCredentialTest.java`
- `PasswordResetTokenTest.java`

**서비스 테스트:**
- `LoginAttemptServiceTest.java`
- `PrivacyConsentServiceTest.java`
- `AccountRecoveryServiceTest.java`
- `AccountStatusServiceTest.java`
- `PasswordAuthServiceLoginTest.java`
- `PasswordAuthServiceTokenTest.java`
- `SignupService, VerifyEmailService, ResendVerificationServiceTest.java`
- `PasswordResetServiceTest.java`
- `RefreshTokenCleanupServiceTest.java`
- `WithdrawnUserCleanupServiceTest.java`
- `SmtpEmailServiceRetryTest.java`
- `UnverifiedUserCleanupServiceTest.java`
- `MemberApprovalServiceTest.java`

**컨트롤러/통합 테스트:**
- `PasswordAuthControllerLoginTest.java`
- `PasswordAuthControllerSignupTest.java`
- `PasswordAuthControllerTokenTest.java`
- `PasswordAuthControllerVerificationTest.java`
- `PasswordAuthControllerAccountRecoveryTest.java`
- `AdminMemberControllerTest.java`
- `PasswordSignupIntegrationTest.java`
- `PasswordLoginIntegrationTest.java`
- `TokenRefreshIntegrationTest.java`
- `PasswordResetIntegrationTest.java`
- `AccountRecoveryIntegrationTest.java`
- `JwtAuthenticationFilterAccountStatusTest.java`

**E2E 테스트:**
- `AuthenticationE2ETest.java`
- `AuthFlowE2ETest.java`

---

## Notes

- [P] 태스크 = 다른 파일, 의존성 없음
- [Story] 라벨은 해당 User Story에 태스크 매핑
- 각 User Story는 독립적으로 완료 및 테스트 가능해야 함
- 태스크 완료 후 또는 논리적 그룹 단위로 커밋
- 체크포인트에서 Story 독립 검증 가능
- 피해야 할 것: 모호한 태스크, 같은 파일 충돌, Story 간 독립성 파괴하는 의존성
