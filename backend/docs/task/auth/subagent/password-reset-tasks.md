# Subagent Task: 비밀번호 재설정 (US4)

**Branch**: `feat/password-reset`
**Priority**: P2
**관련 태스크**: T038, T039, T041, T043

## 목표

비밀번호를 잊은 사용자가 이메일을 통해 비밀번호를 재설정할 수 있도록 컨트롤러 엔드포인트와 DTO를 구현합니다.

## 이미 구현된 것

- ✅ `PasswordResetToken` 엔티티: `backend/src/main/java/igrus/web/security/auth/password/domain/PasswordResetToken.java`
- ✅ `PasswordResetTokenRepository`: `backend/src/main/java/igrus/web/security/auth/password/repository/PasswordResetTokenRepository.java`
- ✅ `PasswordResetService`: `backend/src/main/java/igrus/web/security/auth/password/service/PasswordResetService.java`
  - `requestPasswordReset(String studentId)` - 재설정 토큰 생성 및 이메일 발송
  - `confirmPasswordReset(String token, String newPassword)` - 토큰 검증 및 비밀번호 변경

## 구현할 태스크

### T038: PasswordResetRequest DTO 생성

**경로**: `backend/src/main/java/igrus/web/security/auth/password/dto/request/PasswordResetRequest.java`

```java
public record PasswordResetRequest(
    @NotBlank(message = "학번은 필수입니다")
    String studentId
) {}
```

### T039: PasswordResetConfirmRequest DTO 생성

**경로**: `backend/src/main/java/igrus/web/security/auth/password/dto/request/PasswordResetConfirmRequest.java`

```java
public record PasswordResetConfirmRequest(
    @NotBlank(message = "토큰은 필수입니다")
    String token,

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
             message = "비밀번호는 영문 대/소문자, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다")
    String newPassword
) {}
```

### T041: 컨트롤러 엔드포인트 추가

**파일**: `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthController.java`

추가할 엔드포인트:
- `POST /api/v1/auth/password/reset-request` - 비밀번호 재설정 요청
- `POST /api/v1/auth/password/reset-confirm` - 새 비밀번호 설정

**Swagger 문서화**: `PasswordAuthControllerApi.java` 인터페이스에도 메서드 시그니처 추가

### T043: 통합 테스트

**경로**: `backend/src/test/java/igrus/web/security/auth/password/controller/PasswordAuthControllerPasswordResetTest.java`

테스트 케이스:
1. 비밀번호 재설정 요청 성공
2. 존재하지 않는 학번으로 요청 시 에러
3. 유효한 토큰으로 비밀번호 변경 성공
4. 만료된 토큰으로 요청 시 에러
5. 잘못된 토큰으로 요청 시 에러
6. 비밀번호 정책 미충족 시 에러

## 참고 파일

- 기존 컨트롤러 패턴: `backend/src/main/java/igrus/web/security/auth/password/controller/PasswordAuthController.java`
- 기존 DTO 패턴: `backend/src/main/java/igrus/web/security/auth/password/dto/request/PasswordLoginRequest.java`
- 서비스: `backend/src/main/java/igrus/web/security/auth/password/service/PasswordResetService.java`
- 단위 테스트 참고: `backend/src/test/java/igrus/web/security/auth/password/service/PasswordResetServiceTest.java`

## 완료 조건

- [ ] PasswordResetRequest DTO 생성
- [ ] PasswordResetConfirmRequest DTO 생성
- [ ] PasswordAuthController에 2개 엔드포인트 추가
- [ ] PasswordAuthControllerApi에 Swagger 문서화
- [ ] 통합 테스트 작성 및 통과
- [ ] `./gradlew test` 전체 테스트 통과

## 주의사항

- `PasswordAuthController.java`는 다른 worktree(account-recovery)에서도 수정할 수 있으므로, 새로운 메서드만 추가하고 기존 코드는 수정하지 말 것
- 에러 코드가 필요하면 `ErrorCode.java`에 추가하되, 기존 코드와 충돌하지 않도록 주의
