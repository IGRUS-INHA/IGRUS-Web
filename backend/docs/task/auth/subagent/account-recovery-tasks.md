# Subagent Task: 탈퇴 계정 복구 (US5)

**Branch**: `feat/account-recovery`
**Priority**: P3
**관련 태스크**: T047, T048, T050

## 목표

탈퇴 후 5일 이내 계정 복구 기능을 완성합니다. 탈퇴한 사용자가 로그인 시도 시 복구 가능 여부를 알려주고, 복구를 선택하면 계정을 다시 활성화합니다.

## 이미 구현된 것

- ✅ `AccountRecoveryService`: `backend/src/main/java/igrus/web/security/auth/common/service/AccountRecoveryService.java`
  - `checkRecoveryEligibility(String studentId)` - 복구 가능 여부 확인
  - `recoverAccount(String studentId, String password)` - 계정 복구 실행
- ✅ `AccountRecoveryRequest` DTO: `backend/src/main/java/igrus/web/security/auth/common/dto/request/AccountRecoveryRequest.java`
- ✅ `AccountRecoveryResponse` DTO: `backend/src/main/java/igrus/web/security/auth/common/dto/response/AccountRecoveryResponse.java`
- ✅ `RecoveryEligibilityResponse` DTO: `backend/src/main/java/igrus/web/security/auth/common/dto/response/RecoveryEligibilityResponse.java`

## 구현할 태스크

### T047: 로그인 시 탈퇴 계정 복구 프롬프트 로직

**파일**: `backend/src/main/java/igrus/web/security/auth/password/service/PasswordAuthService.java`

현재 동작:
- 탈퇴 계정 로그인 시 `AccountWithdrawnException` 발생

변경할 동작:
- 탈퇴 계정이면서 복구 가능 기간(5일) 내인 경우, 복구 가능 정보를 포함한 특별한 예외 또는 응답 반환
- 프론트엔드가 이를 감지하여 복구 UI를 표시할 수 있도록 함

구현 방안:
1. `AccountRecoverableException` 생성 (복구 가능한 탈퇴 계정임을 알림)
2. 또는 `GlobalExceptionHandler`에서 `AccountWithdrawnException` 처리 시 복구 가능 여부 체크

### T048: 계정 복구 컨트롤러 엔드포인트

**파일**: `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthController.java`

추가할 엔드포인트:
- `GET /api/v1/auth/password/account/recovery-status?studentId={studentId}` - 복구 가능 여부 확인
- `POST /api/v1/auth/password/account/recover` - 계정 복구 실행

```java
@GetMapping("/account/recovery-status")
public ResponseEntity<RecoveryEligibilityResponse> checkRecoveryStatus(
    @RequestParam String studentId
);

@PostMapping("/account/recover")
public ResponseEntity<AccountRecoveryResponse> recoverAccount(
    @Valid @RequestBody AccountRecoveryRequest request
);
```

**Swagger 문서화**: `PasswordAuthControllerApi.java`에 메서드 시그니처 추가

### T050: 통합 테스트

**경로**: `backend/src/test/java/igrus/web/security/auth/password/controller/PasswordAuthControllerAccountRecoveryTest.java`

테스트 케이스:
1. 탈퇴 계정 로그인 시도 → 복구 가능 응답 반환
2. 복구 가능 여부 조회 성공 (복구 가능)
3. 복구 가능 여부 조회 성공 (복구 불가 - 5일 초과)
4. 계정 복구 성공
5. 잘못된 비밀번호로 복구 시도 시 실패
6. 복구 불가능한 계정 복구 시도 시 실패
7. 복구 후 정상 로그인 성공

## 참고 파일

- 기존 컨트롤러: `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthController.java`
- 로그인 서비스: `backend/src/main/java/igrus/web/security/auth/password/service/PasswordAuthService.java`
- 계정 복구 서비스: `backend/src/main/java/igrus/web/security/auth/common/service/AccountRecoveryService.java`
- 단위 테스트: `backend/src/test/java/igrus/web/security/auth/common/service/AccountRecoveryServiceTest.java`
- 예외 처리: `backend/src/main/java/igrus/web/common/exception/GlobalExceptionHandler.java`

## 완료 조건

- [ ] PasswordAuthService에 복구 프롬프트 로직 추가 (또는 별도 예외 클래스)
- [ ] PasswordAuthController에 복구 관련 2개 엔드포인트 추가
- [ ] PasswordAuthControllerApi에 Swagger 문서화
- [ ] 통합 테스트 작성 및 통과
- [ ] `./gradlew test` 전체 테스트 통과

## 주의사항

- `PasswordAuthController.java`는 다른 worktree(password-reset)에서도 수정할 수 있으므로, 새로운 메서드만 추가하고 기존 코드는 수정하지 말 것
- `PasswordAuthService.java`의 `login()` 메서드 수정 시 기존 로직을 해치지 않도록 주의
- 복구 가능 여부 체크 API는 인증 없이 접근 가능해야 함 (SecurityConfig 확인 필요)
