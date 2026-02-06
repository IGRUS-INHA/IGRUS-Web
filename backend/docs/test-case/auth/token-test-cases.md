# 토큰 갱신 테스트 케이스

**작성일**: 2026-01-23
**버전**: 1.4
**관련 스펙**: [auth-spec.md](../../../../docs/feature/auth/auth-spec.md)
**우선순위**: P2

---

## 1. 개요

토큰 갱신 기능에 대한 테스트 케이스입니다. 로그인한 사용자가 Access Token 만료 시 Refresh Token을 사용하여 새로운 Access Token을 발급받는 과정을 검증합니다.

리프레시 토큰 로테이션 및 탈취 감지 기능이 포함되어 있습니다:
- **토큰 로테이션**: 갱신 시 기존 Refresh Token을 폐기하고 새 Refresh Token을 발급
- **Token Family**: 같은 로그인 세션에서 파생된 토큰 체인을 그룹화
- **탈취 감지**: 이미 폐기된 토큰 재사용 시 해당 Token Family 전체 무효화
- **Grace Period (10초)**: 동시 탭에서의 경쟁 조건을 처리

---

## 2. 테스트 케이스

### 2.1 토큰 갱신 성공 (로테이션 포함)

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| TKN-001 | 유효한 Refresh Token으로 갱신 성공 | Access Token 만료, Refresh Token 유효 | Refresh Token으로 갱신 요청 | 새로운 Access Token + 새로운 Refresh Token 발급 | ✅ |
| TKN-002 | 갱신된 Access Token 5분 유효 | 토큰 갱신 성공 | 새로운 Access Token 만료 시간 확인 | 5분 유효기간 설정 | ✅ |
| TKN-003 | 연쇄 갱신 시 매번 새 Refresh Token 발급 | 토큰 갱신 성공 | 새 Refresh Token으로 다시 갱신 요청 반복 | 매 갱신마다 새 Refresh Token 발급, 이전 토큰 폐기 | ✅ |
| TKN-004 | Access Token 만료 전 갱신 가능 | Access Token 유효한 상태 | Refresh Token으로 갱신 요청 | 새로운 Access Token + 새로운 Refresh Token 발급 | ✅ |
| TKN-005 | 갱신 시 Set-Cookie 헤더에 새 Refresh Token 포함 | 토큰 갱신 요청 | HTTP 응답 헤더 확인 | Set-Cookie 헤더에 새 Refresh Token이 HttpOnly 쿠키로 설정 | ✅ |
| TKN-006 | 갱신 후 기존 Refresh Token 폐기 확인 | 토큰 갱신 성공 | DB에서 기존 토큰 상태 확인 | 기존 토큰 revoked=true, replacedByToken에 새 토큰 기록 | ✅ |

### 2.2 토큰 갱신 실패

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| TKN-010 | 만료된 Refresh Token으로 갱신 시도 | Refresh Token 만료 (3일 경과) | 토큰 갱신 요청 | "토큰이 만료되었습니다" 메시지, 재로그인 필요 | ✅ |
| TKN-011 | 유효하지 않은 Refresh Token으로 갱신 시도 | 잘못된 형식의 Refresh Token | 토큰 갱신 요청 | "유효하지 않은 토큰입니다" 메시지 표시 | ✅ |
| TKN-012 | 변조된 Refresh Token으로 갱신 시도 | Refresh Token 페이로드 변조 | 토큰 갱신 요청 | "유효하지 않은 토큰입니다" 메시지 표시 | ✅ |
| TKN-013 | 빈 Refresh Token으로 갱신 시도 | - | 빈 토큰으로 갱신 요청 | 400 Bad Request 응답 | ✅ |
| TKN-014 | 탈취 감지 시 401 반환 및 쿠키 삭제 | 폐기된 토큰 재사용 (Grace Period 밖) | 탈취된 Refresh Token으로 갱신 요청 | 401 응답, Refresh Token 쿠키 삭제(Set-Cookie maxAge=0) | ✅ |

### 2.3 토큰 탈취 감지 및 Grace Period

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| TKN-040 | 폐기된 토큰 재사용 시 탈취 감지 | 갱신 후 10초 경과 | 이전(폐기된) Refresh Token으로 갱신 요청 | "토큰 도용이 감지되어 모든 세션이 종료되었습니다" 메시지, 401 응답 | ✅ |
| TKN-041 | 탈취 감지 시 같은 패밀리의 모든 토큰 무효화 | 폐기된 토큰 재사용 감지 | DB에서 동일 Token Family 토큰 상태 확인 | 같은 Token Family의 모든 토큰 revoked=true | ✅ |
| TKN-042 | Grace Period 내 폐기된 토큰 사용 시 성공 | 갱신 직후 (10초 이내) | 이전(폐기된) Refresh Token으로 갱신 요청 | 현재 활성 토큰 기반으로 새 Access Token 발급 (탈취로 간주하지 않음) | ✅ |
| TKN-043 | Grace Period 내이지만 활성 토큰 없는 경우 | 폐기 후 10초 이내, 활성 토큰 없음 | 이전 Refresh Token으로 갱신 요청 | "유효하지 않은 토큰입니다" 메시지 표시 | ✅ |
| TKN-044 | Grace Period 내 응답 시 Set-Cookie 미포함 | Grace Period 내 폐기된 토큰 사용 | 이전 Refresh Token으로 갱신 요청 | Access Token만 갱신, Set-Cookie 헤더 미포함 (Refresh Token 쿠키 유지) | ✅ |
| TKN-045 | 폐기되고 만료된 토큰은 Grace Period 내여도 실패 | 토큰 폐기 + 만료 | 만료된 폐기 토큰으로 갱신 요청 | RefreshTokenExpiredException 발생 (만료 체크가 Grace Period보다 우선) | ✅ |

### 2.4 계정 상태 변경 시 토큰 처리

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| TKN-020 | 계정 정지 시 토큰 즉시 무효화 | 유효한 Access/Refresh Token 보유 | 계정이 SUSPENDED로 변경 | 모든 활성 토큰 즉시 무효화 | ✅ |
| TKN-021 | 계정 탈퇴 시 토큰 즉시 무효화 | 유효한 Access/Refresh Token 보유 | 계정이 WITHDRAWN으로 변경 | 모든 활성 토큰 즉시 무효화 | ✅ |
| TKN-022 | 정지된 계정으로 토큰 갱신 시도 | 계정 상태 SUSPENDED | Refresh Token으로 갱신 요청 | "계정이 정지되었습니다" 메시지 표시 | ✅ |
| TKN-023 | 탈퇴한 계정으로 토큰 갱신 시도 | 계정 상태 WITHDRAWN | Refresh Token으로 갱신 요청 | "유효하지 않은 토큰입니다" 메시지 표시 | ✅ |
| TKN-024 | 비밀번호 재설정 시 모든 토큰 무효화 | 비밀번호 재설정 완료 | 기존 Refresh Token으로 갱신 요청 | 모든 기존 토큰 무효화, 재로그인 필요 | ✅ |

### 2.5 토큰 보안

| ID | 테스트 케이스 | 사전 조건 | 테스트 단계 | 예상 결과 | 상태 |
|----|-------------|----------|-----------|----------|------|
| TKN-030 | Access Token 서명 검증 | 유효한 Access Token | 서명을 변조한 토큰으로 API 호출 | 401 Unauthorized 응답 | ✅ |
| TKN-031 | Refresh Token 서명 검증 | 유효한 Refresh Token | 서명을 변조한 토큰으로 갱신 요청 | "유효하지 않은 토큰입니다" 응답 | ✅ |
| TKN-032 | Access Token에 역할 정보 포함 | 로그인 성공 | Access Token 디코딩 | 사용자 역할 정보 포함 | ✅ |

---

## 3. 관련 Functional Requirements

| ID | 요구사항 | 관련 테스트 케이스 |
|----|---------|------------------|
| FR-013 | Access Token(5분), Refresh Token(3일) 발급 | TKN-001, TKN-002, TKN-010 |
| FR-015 | Refresh Token 로테이션으로 Access Token + 새 Refresh Token 재발급 | TKN-001 ~ TKN-006 |
| FR-015a | 토큰 탈취 감지 (Token Family 기반) | TKN-014, TKN-040 ~ TKN-045 |
| FR-019 | 비밀번호 재설정 시 모든 토큰 무효화 | TKN-024 |
| FR-022 | 계정 정지/탈퇴 시 모든 토큰 즉시 무효화 | TKN-020 ~ TKN-023 |

---

## 4. 구현된 테스트 클래스

### 4.1 RefreshTokenService 통합 테스트
- **파일**: `backend/src/test/java/igrus/web/security/auth/password/service/auth/RefreshTokenServiceTest.java`
- **테스트 범위**: TKN-001 ~ TKN-008, TKN-010 ~ TKN-013, TKN-030 ~ TKN-032, TKN-040 ~ TKN-045 (로테이션, 탈취 감지, Grace Period, 만료 체크)

### 4.2 Controller 단위 테스트
- **파일**: `backend/src/test/java/igrus/web/security/auth/password/controller/PasswordAuthControllerTokenTest.java`
- **테스트 범위**: TKN-001 ~ TKN-014, TKN-005 Set-Cookie, TKN-033 Grace Period 쿠키, TKN-044 (HTTP 레이어, TokenRotationResult 반환)

```java
@WebMvcTest(PasswordAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("PasswordAuthController 토큰 갱신 테스트")
class PasswordAuthControllerTokenTest {

    @Nested
    @DisplayName("토큰 갱신 성공")
    class TokenRefreshSuccessTest {
        // TKN-001 ~ TKN-003
    }

    @Nested
    @DisplayName("토큰 갱신 실패")
    class TokenRefreshFailureTest {
        // TKN-010 ~ TKN-014 (탈취 감지 시 쿠키 삭제 검증 포함)
    }

    @Nested
    @DisplayName("토큰 갱신 - 쿠키 동작")
    class TokenRefreshCookieTest {
        // TKN-005 Set-Cookie 존재 검증, TKN-033/044 Grace Period 시 Set-Cookie 미포함
    }
}
```

### 4.3 Controller 통합 테스트
- **파일**: `backend/src/test/java/igrus/web/security/auth/password/controller/PasswordAuthControllerTokenIntegrationTest.java`
- **테스트 범위**: TKN-001 ~ TKN-006 (실제 DB + HTTP 레이어, 연쇄 갱신 검증)

### 4.4 서비스 통합 테스트
- **파일**: `backend/src/test/java/igrus/web/security/auth/password/integration/TokenRefreshIntegrationTest.java`
- **테스트 범위**: TKN-001 ~ TKN-043 (서비스 통합 테스트)

```java
@DisplayName("토큰 갱신 통합 테스트")
class TokenRefreshIntegrationTest extends ServiceIntegrationTestBase {
    @Nested class TokenRefreshSuccessTest { /* TKN-001 ~ TKN-006 */ }
    @Nested class TokenRefreshFailureTest { /* TKN-010 ~ TKN-014 */ }
    @Nested class AccountStatusTokenTest { /* TKN-020 ~ TKN-024 */ }
    @Nested class TokenSecurityTest { /* TKN-030 ~ TKN-032 */ }
    @Nested class TokenTheftDetectionTest { /* TKN-040 ~ TKN-043 */ }
}
```

### 4.5 E2E 테스트
- **파일**: `backend/src/test/java/igrus/web/security/auth/e2e/AuthFlowE2ETest.java`
- **테스트 범위**: TKN-001, TKN-003, TKN-005 (HTTP 레이어 E2E, Set-Cookie 검증, 연쇄 갱신)
- **파일**: `backend/src/test/java/igrus/web/security/auth/e2e/AuthenticationE2ETest.java`
- **테스트 범위**: TKN-001, TKN-003 (서비스 레이어 E2E, TokenRotationResult 연쇄 갱신)

---

## 5. 변경 이력

| 버전 | 날짜 | 작성자 | 변경 내용 |
|------|------|--------|----------|
| 1.0 | 2026-01-23 | - | 최초 작성 |
| 1.1 | 2026-01-24 | - | 컨트롤러 레벨 테스트 구현 정보 추가 |
| 1.2 | 2026-01-25 | - | 서비스 테스트 및 통합 테스트(TokenRefreshIntegrationTest) 구현 정보 추가 |
| 1.3 | 2026-02-06 | Claude | 리프레시 토큰 로테이션 및 탈취 감지 반영: TKN-005~006, TKN-040~043 추가, 유효기간 변경(5분/3일), E2E 테스트 클래스 정보 추가 |
| 1.4 | 2026-02-06 | Claude | PR 리뷰 리팩토링: TKN-014 탈취감지+쿠키삭제로 변경, TKN-044~045 추가, 섹션번호 수정(2.4→2.5), 테스트 클래스 경로 수정 |
