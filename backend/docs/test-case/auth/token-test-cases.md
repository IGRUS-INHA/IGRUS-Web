# 토큰 갱신 테스트 케이스

**작성일**: 2026-01-23
**버전**: 1.0
**관련 스펙**: [auth-spec.md](../../../../docs/feature/auth/auth-spec.md)
**우선순위**: P2

---

## 1. 개요

토큰 갱신 기능에 대한 테스트 케이스입니다. 로그인한 사용자가 Access Token 만료 시 Refresh Token을 사용하여 새로운 Access Token을 발급받는 과정을 검증합니다.

---

## 2. 테스트 케이스

### 2.1 토큰 갱신 성공

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| TKN-001 | 유효한 Refresh Token으로 갱신 성공 | Access Token 만료, Refresh Token 유효 | Refresh Token으로 갱신 요청 | 새로운 Access Token 발급 | ✅ |
| TKN-002 | 갱신된 Access Token 1시간 유효 | 토큰 갱신 성공 | 새로운 Access Token 만료 시간 확인 | 1시간 유효기간 설정 | ✅ |
| TKN-003 | 갱신된 Access Token으로 API 호출 성공 | 토큰 갱신 성공 | 새 Access Token으로 보호된 API 호출 | 정상 응답 | ✅ |
| TKN-004 | Access Token 만료 전 갱신 가능 | Access Token 유효한 상태 | Refresh Token으로 갱신 요청 | 새로운 Access Token 발급 | ✅ |

### 2.2 토큰 갱신 실패

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| TKN-010 | 만료된 Refresh Token으로 갱신 시도 | Refresh Token 만료 (7일 경과) | 토큰 갱신 요청 | "토큰이 만료되었습니다" 메시지, 재로그인 필요 | ✅ |
| TKN-011 | 유효하지 않은 Refresh Token으로 갱신 시도 | 잘못된 형식의 Refresh Token | 토큰 갱신 요청 | "유효하지 않은 토큰입니다" 메시지 표시 | ✅ |
| TKN-012 | 변조된 Refresh Token으로 갱신 시도 | Refresh Token 페이로드 변조 | 토큰 갱신 요청 | "유효하지 않은 토큰입니다" 메시지 표시 | ✅ |
| TKN-013 | 빈 Refresh Token으로 갱신 시도 | - | 빈 토큰으로 갱신 요청 | 400 Bad Request 응답 | ✅ |
| TKN-014 | 로그아웃된 Refresh Token으로 갱신 시도 | 로그아웃 후 토큰 무효화 | 이전 Refresh Token으로 갱신 요청 | "유효하지 않은 토큰입니다" 메시지 표시 | ✅ |

### 2.3 계정 상태 변경 시 토큰 처리

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| TKN-020 | 계정 정지 시 토큰 즉시 무효화 | 유효한 Access/Refresh Token 보유 | 계정이 SUSPENDED로 변경 | 모든 활성 토큰 즉시 무효화 | ✅ |
| TKN-021 | 계정 탈퇴 시 토큰 즉시 무효화 | 유효한 Access/Refresh Token 보유 | 계정이 WITHDRAWN으로 변경 | 모든 활성 토큰 즉시 무효화 | ✅ |
| TKN-022 | 정지된 계정으로 토큰 갱신 시도 | 계정 상태 SUSPENDED | Refresh Token으로 갱신 요청 | "계정이 정지되었습니다" 메시지 표시 | ✅ |
| TKN-023 | 탈퇴한 계정으로 토큰 갱신 시도 | 계정 상태 WITHDRAWN | Refresh Token으로 갱신 요청 | "유효하지 않은 토큰입니다" 메시지 표시 | ✅ |
| TKN-024 | 비밀번호 재설정 시 모든 토큰 무효화 | 비밀번호 재설정 완료 | 기존 Refresh Token으로 갱신 요청 | 모든 기존 토큰 무효화, 재로그인 필요 | ✅ |

### 2.4 토큰 보안

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| TKN-030 | Access Token 서명 검증 | 유효한 Access Token | 서명을 변조한 토큰으로 API 호출 | 401 Unauthorized 응답 | ✅ |
| TKN-031 | Refresh Token 서명 검증 | 유효한 Refresh Token | 서명을 변조한 토큰으로 갱신 요청 | "유효하지 않은 토큰입니다" 응답 | ✅ |
| TKN-032 | Access Token에 역할 정보 포함 | 로그인 성공 | Access Token 디코딩 | 사용자 역할 정보 포함 | ✅ |

---

## 3. 관련 Functional Requirements

| ID | 요구사항 | 관련 테스트 케이스 |
|----|---------|------------------|
| FR-013 | Access Token(1시간), Refresh Token(7일) 발급 | TKN-001, TKN-002, TKN-010 |
| FR-015 | Refresh Token으로 Access Token 재발급 | TKN-001 ~ TKN-004 |
| FR-019 | 비밀번호 재설정 시 모든 토큰 무효화 | TKN-024 |
| FR-022 | 계정 정지/탈퇴 시 모든 토큰 즉시 무효화 | TKN-020 ~ TKN-023 |

---

## 4. 구현된 테스트 클래스

### 4.1 Service 테스트
- **파일**: `backend/src/test/java/igrus/web/security/auth/password/service/PasswordAuthServiceTokenTest.java`
- **테스트 범위**: TKN-001 ~ TKN-024 (비즈니스 로직)

### 4.2 Controller 테스트
- **파일**: `backend/src/test/java/igrus/web/security/auth/password/controller/PasswordAuthControllerTokenTest.java`
- **테스트 범위**: TKN-001 ~ TKN-014 (HTTP 레이어)

```java
@WebMvcTest(PasswordAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("PasswordAuthController 토큰 갱신 테스트")
class PasswordAuthControllerTokenTest {

    @Nested
    @DisplayName("토큰 갱신 성공")
    class TokenRefreshSuccessTest {
        // TKN-001 ~ TKN-004
    }

    @Nested
    @DisplayName("토큰 갱신 실패")
    class TokenRefreshFailureTest {
        // TKN-010 ~ TKN-014
    }
}
```

### 4.3 통합 테스트
- **파일**: `backend/src/test/java/igrus/web/security/auth/password/integration/TokenRefreshIntegrationTest.java`
- **테스트 범위**: TKN-001 ~ TKN-032 (서비스 통합 테스트)
- **테스트 수**: 18개

```java
@DisplayName("토큰 갱신 통합 테스트")
class TokenRefreshIntegrationTest extends ServiceIntegrationTestBase {
    @Nested class TokenRefreshSuccessTest { /* TKN-001 ~ TKN-004 */ }
    @Nested class TokenRefreshFailureTest { /* TKN-010 ~ TKN-014 */ }
    @Nested class AccountStatusTokenTest { /* TKN-020 ~ TKN-024 */ }
    @Nested class TokenSecurityTest { /* TKN-030 ~ TKN-032 */ }
}
```

---

## 5. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-23 | - | 최초 작성 |
| 1.1 | 2026-01-24 | - | 컨트롤러 레벨 테스트 구현 정보 추가 |
| 1.2 | 2026-01-25 | - | 서비스 테스트 및 통합 테스트(TokenRefreshIntegrationTest) 구현 정보 추가 |
