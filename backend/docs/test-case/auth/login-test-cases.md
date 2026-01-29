# 로그인 테스트 케이스

**작성일**: 2026-01-23
**버전**: 1.0
**관련 스펙**: [auth-spec.md](../../../../docs/feature/auth/auth-spec.md)
**우선순위**: P1

---

## 1. 개요

로그인 기능에 대한 테스트 케이스입니다. 등록된 사용자가 학번과 비밀번호로 시스템에 로그인하여 서비스를 이용하는 과정을 검증합니다.

---

## 2. 테스트 케이스

### 2.1 로그인 성공

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| LOG-001 | 준회원 로그인 성공 | 이메일 인증 완료된 준회원(ASSOCIATE) | 올바른 학번과 비밀번호 입력 | 로그인 성공, Access Token과 Refresh Token 발급 | ✅ |
| LOG-002 | 정회원 로그인 성공 | 정회원(MEMBER) 계정 | 올바른 학번과 비밀번호 입력 | 로그인 성공, 역할 정보 MEMBER 반환 | ✅ |
| LOG-003 | 운영진 로그인 성공 | 운영진(OPERATOR) 계정 | 올바른 학번과 비밀번호 입력 | 로그인 성공, 역할 정보 OPERATOR 반환 | ✅ |
| LOG-004 | 관리자 로그인 성공 | 관리자(ADMIN) 계정 | 올바른 학번과 비밀번호 입력 | 로그인 성공, 역할 정보 ADMIN 반환 | ✅ |
| LOG-005 | Access Token 1시간 유효 | 로그인 성공 | Access Token 발급 확인 | Access Token 만료 시간이 1시간 | ✅ |
| LOG-006 | Refresh Token 7일 유효 | 로그인 성공 | Refresh Token 발급 확인 | Refresh Token 만료 시간이 7일 | ✅ |
| LOG-007 | 로그인 시 사용자 역할 정보 반환 | 로그인 성공 | 응답 데이터 확인 | 사용자 역할(ASSOCIATE/MEMBER/OPERATOR/ADMIN) 정보 포함 | ✅ |

### 2.2 로그인 실패

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| LOG-010 | 잘못된 학번으로 로그인 시도 | 존재하지 않는 학번 | 잘못된 학번과 비밀번호 입력 | "학번 또는 비밀번호가 일치하지 않습니다" 메시지 표시 | ✅ |
| LOG-011 | 잘못된 비밀번호로 로그인 시도 | 유효한 학번 존재 | 올바른 학번과 잘못된 비밀번호 입력 | "학번 또는 비밀번호가 일치하지 않습니다" 메시지 표시 | ✅ |
| LOG-012 | 이메일 미인증 사용자 로그인 시도 | 이메일 인증 미완료 계정 | 학번과 비밀번호 입력 | "이메일 인증이 완료되지 않았습니다" 메시지 표시 | ✅ |
| LOG-013 | 학번 빈 값으로 로그인 시도 | - | 학번 필드 비워두고 로그인 시도 | 입력 검증 오류 메시지 표시 | ✅ |
| LOG-014 | 비밀번호 빈 값으로 로그인 시도 | - | 비밀번호 필드 비워두고 로그인 시도 | 입력 검증 오류 메시지 표시 | ✅ |

### 2.3 계정 상태별 로그인 제한

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| LOG-020 | 정지된 계정 로그인 시도 | 계정 상태 SUSPENDED | 올바른 학번과 비밀번호 입력 | "계정이 정지되었습니다"와 정지 해제 일시 표시 | ✅ |
| LOG-021 | 탈퇴한 계정 로그인 시도 (5일 이내) | 5일 이내 탈퇴한 계정 (WITHDRAWN) | 올바른 학번과 비밀번호 입력 | "탈퇴한 계정입니다. 복구하시겠습니까?" 메시지와 복구 가능 기한 표시 | ✅ |
| LOG-022 | 탈퇴한 계정 로그인 시도 (5일 초과) | 5일 초과 탈퇴한 계정 | 학번과 비밀번호 입력 | "학번 또는 비밀번호가 일치하지 않습니다" 메시지 표시 (개인정보 파기 완료) | ✅ |

### 2.4 로그아웃

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| LOG-030 | 로그아웃 요청 시 토큰 무효화 | 로그인된 상태 | 로그아웃 요청 | 현재 세션의 토큰이 무효화됨 | ✅ |
| LOG-031 | 로그아웃 후 이전 토큰 사용 불가 | 로그아웃 완료 | 이전 Access Token으로 API 요청 | 401 Unauthorized 응답 | ✅ |
| LOG-032 | 로그아웃 후 Refresh Token 사용 불가 | 로그아웃 완료 | 이전 Refresh Token으로 토큰 갱신 요청 | 401 Unauthorized 응답 | ✅ |

### 2.5 다중 디바이스 로그인

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| LOG-040 | 여러 기기 동시 로그인 | 기기 A에서 로그인 완료 | 기기 B에서 동일 계정 로그인 | 각각 독립된 토큰 발급, 두 기기 모두 사용 가능 | ✅ |
| LOG-041 | 한 기기 로그아웃 시 다른 기기 유지 | 기기 A, B에서 로그인 완료 | 기기 A에서 로그아웃 | 기기 A 토큰만 무효화, 기기 B 정상 사용 가능 | ✅ |

---

## 3. 관련 Functional Requirements

| ID | 요구사항 | 관련 테스트 케이스 |
|----|---------|------------------|
| FR-011 | 학번과 비밀번호로 인증 | LOG-001 ~ LOG-004, LOG-010 ~ LOG-014 |
| FR-012 | 이메일 인증 완료 사용자만 로그인 허용 | LOG-012 |
| FR-013 | Access Token(1시간), Refresh Token(7일) 발급 | LOG-005, LOG-006 |
| FR-014 | 로그인 시 사용자 역할 정보 반환 | LOG-007 |
| FR-016 | 로그아웃 시 토큰 무효화 | LOG-030 ~ LOG-032 |
| FR-020 | 계정 상태(ACTIVE, SUSPENDED, WITHDRAWN) 관리 | LOG-020 ~ LOG-022 |
| FR-021 | 정지/탈퇴 상태 사용자 로그인 차단 | LOG-020 ~ LOG-022 |

---

## 4. 구현된 테스트 클래스

### 4.1 Service 테스트
- **파일**: `backend/src/test/java/igrus/web/security/auth/password/service/PasswordAuthServiceLoginTest.java`
- **테스트 범위**: LOG-001 ~ LOG-041 (비즈니스 로직)

### 4.2 Controller 테스트
- **파일**: `backend/src/test/java/igrus/web/security/auth/password/controller/PasswordAuthControllerLoginTest.java`
- **테스트 범위**: LOG-001 ~ LOG-032 (HTTP 레이어)

```java
@WebMvcTest(PasswordAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("PasswordAuthController 로그인/로그아웃 테스트")
class PasswordAuthControllerLoginTest {

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTest {
        @Nested class LoginSuccessTest { /* LOG-001 ~ LOG-007 */ }
        @Nested class LoginAuthenticationFailureTest { /* LOG-010 ~ LOG-012 */ }
        @Nested class LoginValidationFailureTest { /* LOG-013 ~ LOG-014 */ }
        @Nested class LoginAccountStatusFailureTest { /* LOG-020 ~ LOG-021 */ }
    }

    @Nested
    @DisplayName("로그아웃 테스트")
    class LogoutTest {
        @Nested class LogoutSuccessTest { /* LOG-030 */ }
        @Nested class LogoutFailureTest { /* LOG-031 ~ LOG-032 */ }
    }
}
```

### 4.3 통합 테스트
- **파일**: `backend/src/test/java/igrus/web/security/auth/password/integration/PasswordLoginIntegrationTest.java`
- **테스트 범위**: LOG-001 ~ LOG-041 (서비스 통합 테스트)
- **테스트 수**: 21개

```java
@DisplayName("로그인 통합 테스트")
class PasswordLoginIntegrationTest extends ServiceIntegrationTestBase {
    @Nested class LoginSuccessTest { /* LOG-001 ~ LOG-007 */ }
    @Nested class LoginFailureTest { /* LOG-010 ~ LOG-014 */ }
    @Nested class AccountStatusLoginTest { /* LOG-020 ~ LOG-022 */ }
    @Nested class LogoutTest { /* LOG-030 ~ LOG-032 */ }
    @Nested class MultiDeviceLoginTest { /* LOG-040 ~ LOG-041 */ }
}
```

---

## 5. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-23 | - | 최초 작성 |
| 1.1 | 2026-01-24 | - | 컨트롤러 레벨 테스트 구현 정보 추가 |
| 1.2 | 2026-01-25 | - | 통합 테스트(PasswordLoginIntegrationTest) 구현 정보 추가 |
