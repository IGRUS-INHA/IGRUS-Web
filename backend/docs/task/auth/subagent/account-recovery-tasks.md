# Subagent Task: 탈퇴 계정 복구 (US5)

**Branch**: `feat/account-recovery`
**Priority**: P3
**관련 태스크**: T047, T048, T050

## 목표

탈퇴 후 5일 이내 계정 복구 기능을 완성합니다. 탈퇴한 사용자가 로그인 시도 시 복구 가능 여부를 알려주고, 복구를 선택하면 계정을 다시 활성화합니다.

## 구현 완료

### 이미 구현된 것

- ✅ `AccountRecoveryService`: `backend/src/main/java/igrus/web/security/auth/common/service/AccountRecoveryService.java`
  - `checkRecoveryEligibility(String studentId)` - 복구 가능 여부 확인
  - `recoverAccount(String studentId, String password)` - 계정 복구 실행
- ✅ `AccountRecoveryRequest` DTO: `backend/src/main/java/igrus/web/security/auth/common/dto/request/AccountRecoveryRequest.java`
- ✅ `AccountRecoveryResponse` DTO: `backend/src/main/java/igrus/web/security/auth/common/dto/response/AccountRecoveryResponse.java`
- ✅ `RecoveryEligibilityResponse` DTO: `backend/src/main/java/igrus/web/security/auth/common/dto/response/RecoveryEligibilityResponse.java`

### T047: 로그인 시 탈퇴 계정 복구 프롬프트 로직 ✅

**파일**: `backend/src/main/java/igrus/web/security/auth/password/service/PasswordAuthService.java`

구현 완료:
- `AccountRecoverableException` 예외 클래스 활용
- 탈퇴 계정이면서 복구 가능 기간(5일) 내인 경우, `AccountRecoverableException` 발생
- 복구 불가능한 경우 `AccountWithdrawnException` 발생
- 프론트엔드가 이를 감지하여 복구 UI를 표시할 수 있음

### T048: 계정 복구 컨트롤러 엔드포인트 ✅

**파일**: `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthController.java`

추가된 엔드포인트:
- `GET /api/v1/auth/password/account/recovery-check?studentId={studentId}` - 복구 가능 여부 확인
- `POST /api/v1/auth/password/account/recover` - 계정 복구 실행

**Swagger 문서화**: `PasswordAuthControllerApi.java`에 완료

### T050: 컨트롤러 레벨 테스트 ✅

**경로**: `backend/src/test/java/igrus/web/security/auth/password/controller/PasswordAuthControllerAccountRecoveryTest.java`

구현된 테스트 케이스:
1. ✅ 복구 가능한 계정 조회 - 200 OK [REC-001]
2. ✅ 복구 불가능한 계정 조회 (5일 초과) - 200 OK [REC-002]
3. ✅ 탈퇴 상태가 아닌 계정 조회 - 200 OK [REC-003]
4. ✅ 학번 형식 오류 - 400 Bad Request [REC-004]
5. ✅ 학번 누락 - 400 Bad Request [REC-005]
6. ✅ 유효한 요청으로 계정 복구 성공 - 200 OK [REC-010]
7. ✅ 잘못된 비밀번호 - 401 Unauthorized [REC-011]
8. ✅ 존재하지 않는 계정 - 401 Unauthorized [REC-012]
9. ✅ 복구 기간 만료 - 400 Bad Request [REC-013]
10. ✅ 학번 빈 값 - 400 Bad Request [REC-014]
11. ✅ 비밀번호 빈 값 - 400 Bad Request [REC-015]
12. ✅ 학번 형식 오류 - 400 Bad Request [REC-016]

### 기타 테스트 ✅

- `backend/src/test/java/igrus/web/security/auth/common/service/AccountRecoveryServiceTest.java` - 서비스 단위 테스트
- `backend/src/test/java/igrus/web/security/auth/common/integration/AccountRecoveryIntegrationTest.java` - 통합 테스트
- `backend/src/test/java/igrus/web/security/auth/e2e/AuthenticationE2ETest.java` - E2E 테스트 (E2E-005 시나리오)

## 참고 파일

- 기존 컨트롤러: `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthController.java`
- 로그인 서비스: `backend/src/main/java/igrus/web/security/auth/password/service/PasswordAuthService.java`
- 계정 복구 서비스: `backend/src/main/java/igrus/web/security/auth/common/service/AccountRecoveryService.java`
- 단위 테스트: `backend/src/test/java/igrus/web/security/auth/common/service/AccountRecoveryServiceTest.java`
- 예외 처리: `backend/src/main/java/igrus/web/common/exception/GlobalExceptionHandler.java`
- 테스트 픽스쳐: `backend/src/test/java/igrus/web/security/auth/password/controller/fixture/PasswordAuthTestFixture.java`

## 완료 조건

- [x] PasswordAuthService에 복구 프롬프트 로직 추가 (AccountRecoverableException 활용)
- [x] PasswordAuthController에 복구 관련 2개 엔드포인트 추가
- [x] PasswordAuthControllerApi에 Swagger 문서화
- [x] 컨트롤러 테스트 작성 및 통과
- [x] 통합 테스트 작성 및 통과
- [x] E2E 테스트 작성 및 통과
- [x] `./gradlew test` 전체 테스트 통과

## 주의사항

- `PasswordAuthController.java`는 다른 worktree(password-reset)에서도 수정할 수 있으므로, 새로운 메서드만 추가하고 기존 코드는 수정하지 말 것
- `PasswordAuthService.java`의 `login()` 메서드 수정 시 기존 로직을 해치지 않도록 주의
- 복구 가능 여부 체크 API는 인증 없이 접근 가능해야 함 (SecurityConfig 확인 필요)

## 수정된 파일 목록

### 신규 파일
- `backend/src/main/java/igrus/web/security/auth/approval/dto/response/BulkApprovalResultResponse.java`
- `backend/src/test/java/igrus/web/security/auth/password/controller/PasswordAuthControllerAccountRecoveryTest.java`

### 수정된 파일
- `backend/src/main/java/igrus/web/security/auth/approval/controller/AdminMemberController.java` (불필요한 @Override 제거)
- `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthController.java` (@Validated 추가, @Valid 인터페이스로 이동)
- `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthControllerApi.java` (@Valid 어노테이션 추가)
- `backend/src/test/java/igrus/web/security/auth/password/controller/fixture/PasswordAuthTestFixture.java` (복구 관련 픽스쳐 추가)
- `backend/src/test/java/igrus/web/security/auth/password/controller/PasswordAuthControllerLoginTest.java` (PasswordResetService MockBean 추가)
- `backend/src/test/java/igrus/web/security/auth/password/controller/PasswordAuthControllerSignupTest.java` (PasswordResetService MockBean 추가)
- `backend/src/test/java/igrus/web/security/auth/password/controller/PasswordAuthControllerTokenTest.java` (PasswordResetService MockBean 추가)
- `backend/src/test/java/igrus/web/security/auth/password/controller/PasswordAuthControllerVerificationTest.java` (PasswordResetService MockBean 추가)
- `backend/src/test/java/igrus/web/security/auth/e2e/AuthenticationE2ETest.java` (deleted 필드 설정 추가, 트랜잭션 처리)
