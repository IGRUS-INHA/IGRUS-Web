# 백엔드 인증 로직 코드 리뷰

> 작성일: 2025-01-23
> 최종 수정일: 2026-01-25
> 리뷰 범위: `backend/src/main/java/igrus/web/security/**`
> 총 테스트 파일: 35개 | 총 테스트 케이스: 550개

## 목차

1. [리뷰 개요](#1-리뷰-개요)
2. [Critical 이슈](#2-critical-이슈-즉시-조치-필요)
3. [High 이슈](#3-high-이슈-빠른-조치-권장)
4. [Medium 이슈](#4-medium-이슈-계획된-조치)
5. [잘 작성된 부분](#5-잘-작성된-부분)
6. [테스트 커버리지 분석](#6-테스트-커버리지-분석)
7. [조치 권고 사항](#7-조치-권고-사항)
8. [종합 평가](#8-종합-평가)

---

## 1. 리뷰 개요

### 1.1 리뷰 대상 파일

| 영역 | 주요 파일 |
|------|----------|
| JWT | `JwtTokenProvider.java`, `JwtAuthenticationFilter.java`, `jwt/exception/*.java` |
| Security Config | `ApiSecurityConfig.java`, `PublicResourceSecurityConfig.java`, `SecurityConfigUtil.java`, `SecurityPaths.java` |
| Password Auth | `PasswordAuthService.java`, `PasswordSignupService.java`, `PasswordAuthController.java` |
| Brute Force 방어 | `LoginAttempt.java`, `LoginAttemptService.java`, `LoginAttemptRepository.java` |
| 계정 상태 검증 | `AccountStatusService.java`, `account/exception/*.java` |
| 도메인/공통 서비스 | `EmailVerification.java`, `RefreshToken.java`, `PrivacyConsent.java`, `SmtpEmailService.java`, `RetryConfig.java`, `AsyncConfig.java` 등 |
| 테스트 | `JwtTokenProviderTest.java`, `JwtAuthenticationFilterTest.java`, `JwtAuthenticationFilterAccountStatusTest.java`, `AccountStatusServiceTest.java`, `PasswordAuthServiceLoginTest.java`, `PasswordSignupServiceTest.java`, `LoginAttemptServiceTest.java`, `SmtpEmailServiceRetryTest.java` 등 |

### 1.2 리뷰 관점

- 보안 취약점 (OWASP Top 10)
- Spring Security 설정 적절성
- JWT 표준 준수 여부
- 코드 품질 및 유지보수성
- 테스트 커버리지

---

## 2. Critical 이슈 (즉시 조치 필요)

### 2.1 CORS 설정 과도하게 허용

**파일**: `SecurityConfigUtil.java` (39-42행)

```java
configuration.setAllowedOriginPatterns(Collections.singletonList("*"));
configuration.setAllowedMethods(Collections.singletonList("*"));
configuration.setAllowCredentials(true);
```

**상태**: ⚠️ **미해결**

**문제점**:
- 모든 Origin, 모든 Method를 허용하면서 `allowCredentials(true)` 설정
- CSRF 공격에 취약하며, 악성 사이트에서 쿠키를 포함한 요청 가능
- 프로덕션 환경에서 심각한 보안 취약점

**권고사항**:
```java
// 프로덕션 환경
configuration.setAllowedOriginPatterns(List.of("https://igrus.inha.ac.kr"));
configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
configuration.setAllowedHeaders(List.of("Content-Type", "Authorization"));
configuration.setMaxAge(3600L);

// 개발 환경
configuration.setAllowedOriginPatterns(List.of("http://localhost:3000", "http://localhost:5173"));
```

---

### 2.2 JWT 표준 클레임 누락

**파일**: `JwtTokenProvider.java`

**상태**: ✅ **해결됨** - 2026-01-25

**해결 내용**:
- `issuer("igrus-api")` 클레임 추가
- `audience().add("igrus-web").and()` 클레임 추가
- 토큰 검증 시 `requireIssuer()`, `requireAudience()` 검증 추가
- `application.yml`에 설정 외부화 (`app.jwt.issuer`, `app.jwt.audience`)

```java
return Jwts.builder()
        .issuer(issuer)
        .audience().add(audience).and()
        .subject(userId.toString())
        .claim("studentId", studentId)
        .claim("role", role)
        .claim("type", "access")
        .issuedAt(now)
        .expiration(expiry)
        .signWith(secretKey)
        .compact();
```

---

### 2.3 Timing Attack 취약점

**파일**: `PasswordSignupService.java`

**상태**: ✅ **해결됨** - 2026-01-25

**해결 내용**:
- `String.equals()` → `MessageDigest.isEqual()` 변경
- 상수 시간(constant-time) 비교로 타이밍 공격 방지

```java
if (!MessageDigest.isEqual(
    verification.getCode().getBytes(StandardCharsets.UTF_8),
    request.code().getBytes(StandardCharsets.UTF_8))) {
    verification.incrementAttempts();
    emailVerificationRepository.save(verification);
    throw new VerificationCodeInvalidException();
}
```

---

## 3. High 이슈 (빠른 조치 권장)

### 3.1 Brute Force 방어 부재

**파일**: `PasswordAuthService.java`

**상태**: ✅ **해결됨** - 2026-01-25

**해결 내용**:
- `LoginAttempt` 엔티티 생성 (DB 기반 로그인 시도 추적)
- `LoginAttemptService` 구현
- 5회 실패 시 30분 계정 잠금
- 로그인 성공 시 시도 횟수 초기화
- `AccountLockedException` 예외 추가 (HTTP 423)

**구현 파일**:
- `LoginAttempt.java` - 엔티티
- `LoginAttemptRepository.java` - 리포지토리
- `LoginAttemptService.java` - 서비스
- `AccountLockedException.java` - 예외
- `V2__add_login_attempts.sql` - DB 마이그레이션
- `LoginAttemptServiceTest.java` - 테스트

**설정** (`application.yml`):
```yaml
app:
  security:
    login-attempts-max: 5
    login-lockout-minutes: 30
```

---

### 3.2 PUBLIC_PATHS 이중 관리

**파일**: `ApiSecurityConfig.java`, `JwtAuthenticationFilter.java`

**상태**: ✅ **해결됨** - 2026-01-25

**해결 내용**:
- `SecurityPaths.java` 공통 상수 클래스 생성
- `ApiSecurityConfig.java`에서 `SecurityPaths.PUBLIC_PATHS` 참조
- `JwtAuthenticationFilter.java`에서 `SecurityPaths.PUBLIC_PATHS` 참조

```java
// SecurityPaths.java
public final class SecurityPaths {
    private SecurityPaths() {}

    public static final String[] PUBLIC_PATHS = {
        "/api/v1/auth/password/**",
        "/api/privacy/policy",
        "/api/inquiries",
        "/api/inquiries/lookup"
    };
}
```

---

### 3.3 보안 헤더 설정 누락

**파일**: `ApiSecurityConfig.java`, `SecurityConfigUtil.java`

**상태**: ✅ **해결됨** - 2026-01-25

**해결 내용**:
- `SecurityConfigUtil.configSecurityHeaders()` 메서드 추가
- XSS Protection 헤더
- X-Frame-Options: DENY (클릭재킹 방지)
- X-Content-Type-Options: nosniff (MIME 스니핑 방지)
- Content-Security-Policy
- Referrer-Policy: STRICT_ORIGIN_WHEN_CROSS_ORIGIN

```java
public void configSecurityHeaders(HttpSecurity http) throws Exception {
    http.headers(headers -> headers
        .xssProtection(xss -> xss.headerValue(
            XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
        .frameOptions(frame -> frame.deny())
        .contentTypeOptions(contentType -> {})
        .contentSecurityPolicy(csp -> csp.policyDirectives(
            "default-src 'self'; frame-ancestors 'none'; form-action 'self'"))
        .referrerPolicy(referrer -> referrer.policy(
            ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
    );
}
```

---

## 4. Medium 이슈 (계획된 조치)

### 4.1 JwtAuthenticationFilter 테스트 전무

**상태**: ✅ **해결됨** - 2026-01-25

**해결 내용**:
- `JwtAuthenticationFilterTest.java` 생성
- 21개 테스트 케이스 구현

**테스트 케이스**:
- 공개 경로 요청 시 필터 스킵 (6개)
- 유효한 토큰으로 SecurityContext 설정 성공
- Authorization 헤더 없음 → 인증 없이 통과
- Bearer 접두사 없음 → 인증 없이 통과
- 만료된 토큰 → 로그 후 통과
- Refresh Token 사용 → InvalidTokenTypeException
- 역할별 권한 부여 (4개: ASSOCIATE, MEMBER, OPERATOR, ADMIN)
- 필터 체인 동작 검증 (2개)
- 토큰 추출 엣지 케이스 (2개)

---

### 4.2 레거시 JWT 메서드 정리 필요

**파일**: `JwtTokenProvider.java`

**상태**: ✅ **해결됨** - 2026-01-25

**해결 내용**:
다음 메서드들에 `@Deprecated(since = "1.0", forRemoval = true)` 추가:
- `validateToken(String token)`
- `getUserId(String token)`
- `getStudentId(String token)`
- `getRole(String token)`
- `getTokenType(String token)`
- `isTokenExpired(String token)`

각 메서드에 대체 메서드를 안내하는 Javadoc 추가됨.

---

## 5. 잘 작성된 부분

### 5.1 JWT 구현

| 항목 | 설명 |
|------|------|
| 토큰 타입 분리 | Access/Refresh Token을 `type` 클레임으로 구분하여 토큰 혼용 공격 방지 |
| JJWT 최신 API | `parseSignedClaims()`, `verifyWith()` 등 0.12.x API 올바르게 사용 |
| 불변 SecretKey | `final`로 선언하여 변조 방지 |
| 예외 분리 | 만료/무효/타입오류를 각각 다른 예외로 분리 |
| 비밀키 검증 | 최소 32바이트 검증으로 HMAC-SHA256 요구사항 충족 ✅ |
| 토큰 파싱 최적화 | `validateAccessTokenAndGetClaims()` 메서드로 재파싱 최소화 ✅ |
| 표준 클레임 | `iss`, `aud` 클레임 포함 및 검증 ✅ |

### 5.2 Security Config

| 항목 | 설명 |
|------|------|
| Stateless 세션 | JWT 인증에 적합한 `STATELESS` 세션 정책 |
| JWT 필터 위치 | `UsernamePasswordAuthenticationFilter` 앞에 적절히 배치 |
| 불필요한 인증 비활성화 | Form Login, HTTP Basic 비활성화로 공격 표면 감소 |
| Config 분리 | API/정적 리소스 별도 SecurityFilterChain ✅ |
| 경로 중앙 관리 | `SecurityPaths` 클래스로 PUBLIC_PATHS 통합 관리 ✅ |
| 보안 헤더 | XSS, 클릭재킹, CSP 등 보안 헤더 적용 ✅ |
| 계정 상태 검증 | JWT 필터에서 토큰 검증 후 계정 상태 실시간 확인 (ACTIVE/SUSPENDED/WITHDRAWN/PENDING_VERIFICATION) ✅ |

### 5.3 Password Auth

| 항목 | 설명 |
|------|------|
| BCryptPasswordEncoder | 업계 표준 암호화 알고리즘 사용 |
| Bean Validation | 입력 검증에 어노테이션 활용 |
| 비밀번호 복잡도 | 정규식으로 대/소문자, 숫자, 특수문자 요구 |
| 비밀번호 최대 길이 | 72자 제한으로 BCrypt 한계 준수 ✅ |
| SecureRandom | 예측 불가능한 인증 코드 생성 |
| Rate Limiting | 인증 코드 재발송 5분 제한 ✅ |
| Brute Force 방어 | 5회 실패 시 30분 계정 잠금 ✅ |
| Timing Attack 방지 | MessageDigest.isEqual() 사용 ✅ |

### 5.4 도메인 설계

| 항목 | 설명 |
|------|------|
| 정적 팩토리 메서드 | 객체 생성 로직 캡슐화 (`create()`) |
| 비즈니스 로직 캡슐화 | `isExpired()`, `canAttempt()`, `isLocked()` 등 도메인 내부 구현 |
| Refresh Token 저장 | DB 저장으로 revoke 기능 지원 |
| 환경별 구현체 분리 | Profile 기반 EmailService 전략 패턴 |
| 시간 타입 통일 | 모든 엔티티에서 `Instant` 사용 ✅ |
| Login Attempt 추적 | DB 기반 로그인 시도 추적 ✅ |

### 5.5 운영 기능

| 항목 | 설명 |
|------|------|
| 미인증 사용자 정리 | 24시간 후 자동 삭제 배치 스케줄러 ✅ |
| SMTP 발신자 설정 | 명시적 from 주소 설정 ✅ |
| 이메일 재시도 로직 | Spring Retry + Async로 지수 백오프 재시도 (1분 → 3분 → 9분, 최대 4회) ✅ |

---

## 6. 테스트 커버리지 분석

### 6.1 현재 테스트 현황

**총 테스트 파일: 36개 | 총 테스트 케이스: 567개**

#### JWT/Security 테스트

| 파일 | 테스트 유형 | 테스트 수 | 상태 |
|------|------------|---------|------|
| `JwtTokenProviderTest.java` | 단위 테스트 | 14개 | ✅ 양호 |
| `JwtAuthenticationFilterTest.java` | 단위 테스트 | 21개 | ✅ 양호 |
| `JwtAuthenticationFilterAccountStatusTest.java` | 단위 테스트 | 7개 | ✅ 양호 |

#### 서비스 테스트

| 파일 | 테스트 유형 | 테스트 수 | 상태 |
|------|------------|---------|------|
| `PasswordSignupServiceTest.java` | 서비스 Mock | 20개 | ✅ 양호 |
| `PasswordAuthServiceLoginTest.java` | 서비스 Mock | 22개 | ✅ 양호 |
| `PasswordAuthServiceTokenTest.java` | 서비스 Mock | 11개 | ✅ 양호 |
| `LoginAttemptServiceTest.java` | 서비스 Mock | 10개 | ✅ 양호 |
| `AccountStatusServiceTest.java` | 서비스 Mock | 6개 | ✅ 양호 |
| `PrivacyConsentServiceTest.java` | 서비스 Mock | 10개 | ✅ 양호 |
| `SmtpEmailServiceRetryTest.java` | 서비스 단위 | 17개 | ✅ 양호 |

#### 도메인 테스트

| 파일 | 테스트 유형 | 테스트 수 | 상태 |
|------|------------|---------|------|
| `RefreshTokenTest.java` | 도메인 단위 | 11개 | ✅ 양호 |
| `EmailVerificationTest.java` | 도메인 단위 | 16개 | ✅ 양호 |
| `PrivacyConsentTest.java` | 도메인 단위 | 5개 | ✅ 양호 |
| `AuthenticatedUserTest.java` | 단위 테스트 | 2개 | ⚠️ 최소한 |

#### 통합 테스트

| 파일 | 테스트 유형 | 테스트 수 | 상태 |
|------|------------|---------|------|
| `PasswordSignupIntegrationTest.java` | 통합 테스트 | 37개 | ✅ 양호 |
| `PasswordLoginIntegrationTest.java` | 통합 테스트 | 21개 | ✅ 양호 |
| `TokenRefreshIntegrationTest.java` | 통합 테스트 | 18개 | ✅ 양호 |
| `PasswordResetIntegrationTest.java` | 통합 테스트 | 20개 | ✅ 양호 |
| `AccountRecoveryIntegrationTest.java` | 통합 테스트 | 25개 | ✅ 양호 |
| `PrivacyConsentRepositoryTest.java` | 통합 테스트 | 12개 | ✅ 양호 |

#### E2E 테스트

| 파일 | 테스트 유형 | 테스트 수 | 상태 |
|------|------------|---------|------|
| `AuthenticationE2ETest.java` | E2E (서비스) | 12개 | ✅ 양호 |
| `AuthFlowE2ETest.java` | E2E (HTTP) | 8개 | ✅ 양호 |

### 6.2 테스트 보완 필요

#### 도메인 테스트 보완 필요
- `LoginAttempt` 도메인 테스트 (서비스 테스트만 존재, 도메인 테스트 없음)

---

## 7. 조치 권고 사항

### 7.1 1단계: 프로덕션 배포 전 필수

- [ ] CORS Origin 제한 (프로덕션용 도메인만 허용)

### 7.2 2단계: 테스트 보완

- [ ] `LoginAttempt` 도메인 테스트 작성

### 7.3 완료된 항목

- [x] 인가 규칙 순서 수정 - **2026-01-24 완료**
- [x] SMTP 발신자 주소 설정 - **2026-01-24 완료**
- [x] JWT 비밀키 길이 검증 추가 - **2026-01-24 완료**
- [x] 토큰 파싱 최적화 (Claims 재사용) - **2026-01-24 완료**
- [x] 시간 타입 통일 (`Instant`) - **2026-01-24 완료**
- [x] 미인증 사용자 정리 배치 작업 구현 - **2026-01-24 완료**
- [x] 인증 코드 재발송 Rate Limiting (5분 제한) - **2026-01-24 완료**
- [x] 비밀번호 최대 길이 제한 (72자) - **2026-01-24 완료**
- [x] PrivacyConsentRepository N+1 문제 해결 - **2026-01-24 완료**
- [x] `PrivacyConsentRepositoryTest`에서 `@Transactional` 제거 - **2026-01-24 완료**
- [x] `PasswordAuthService` 테스트 작성 (33 케이스) - **2026-01-24 완료**
- [x] `PasswordSignupService` 테스트 작성 (20 케이스) - **2026-01-24 완료**
- [x] JWT 표준 클레임 추가 (issuer, audience) - **2026-01-25 완료**
- [x] Timing Attack 완화 (MessageDigest.isEqual 적용) - **2026-01-25 완료**
- [x] Brute Force 방어 구현 (DB 기반 로그인 실패 추적) - **2026-01-25 완료**
- [x] PUBLIC_PATHS 상수 중앙화 - **2026-01-25 완료**
- [x] 보안 헤더 설정 추가 - **2026-01-25 완료**
- [x] 레거시 JWT 메서드 @Deprecated 처리 - **2026-01-25 완료**
- [x] `JwtAuthenticationFilter` 테스트 작성 (21 케이스) - **2026-01-25 완료**
- [x] `RefreshToken` 도메인 테스트 작성 (11 케이스) - **2026-01-25 완료**
- [x] `EmailVerification` 도메인 테스트 작성 (16 케이스) - **2026-01-25 완료**
- [x] 통합 테스트 추가 (회원가입, 로그인, 토큰 갱신, 비밀번호 초기화, 계정 복구) - **2026-01-25 완료**
- [x] E2E 인증 플로우 테스트 추가 (서비스 레벨 + HTTP 레벨) - **2026-01-25 완료**
- [x] T062: JwtAuthenticationFilter 계정 상태 검증 추가 - **2026-01-25 완료**
- [x] T062: AccountStatusService 구현 (ACTIVE/SUSPENDED/WITHDRAWN/PENDING_VERIFICATION 상태 검증) - **2026-01-25 완료**
- [x] T062: 계정 상태 테스트 작성 (AccountStatusServiceTest 6개, JwtAuthenticationFilterAccountStatusTest 7개) - **2026-01-25 완료**

---

## 8. 종합 평가

### 8.1 영역별 점수

| 영역 | 점수 | 비고 |
|-----|------|------|
| JWT 구현 | 9/10 | 표준 클레임 추가, 레거시 메서드 정리 완료 |
| Security Config | 8/10 | 보안 헤더 추가, 경로 중앙화 완료, CORS만 미해결 |
| Password Auth | 9/10 | Brute Force 방어, Timing Attack 방지 완료 |
| 도메인/서비스 | 9/10 | 시간 타입 통일, 배치 스케줄러 완료 |
| 테스트 커버리지 | 9.5/10 | 550개 테스트, 도메인/통합/E2E 완비, LoginAttempt 도메인 테스트만 부족 |

### 8.2 전체 보안 점수

**8.9 / 10** (이전: 8.5 → 6.8)

### 8.3 OWASP Top 10 매핑

| OWASP 순위 | 취약점 | 현황 | 심각도 |
|----------|--------|------|--------|
| A01:2021 - Broken Access Control | PUBLIC_PATHS 이중 관리 | ✅ 해결 | - |
| A02:2021 - Cryptographic Failures | CORS + allowCredentials | ⚠️ 미해결 | **CRITICAL** |
| A02:2021 - Cryptographic Failures | JWT 표준 클레임 | ✅ 해결 | - |
| A07:2021 - CSRF | JWT 기반 + CSRF 비활성화 | ✅ 해결 | - |
| A07:2021 - Auth Failures | Brute Force 방어 | ✅ 해결 | - |
| A03:2021 - Injection | Timing Attack | ✅ 해결 | - |

### 8.4 강점

- JWT 라이브러리(JJWT)의 최신 API 올바르게 사용
- Access/Refresh Token 분리 및 타입 검증으로 토큰 혼용 공격 방지
- 도메인 주도 설계(DDD) 원칙 준수
- 팩토리 메서드 패턴으로 객체 생성 로직 캡슐화
- 예외 처리가 체계적으로 구성됨
- 핵심 서비스 및 필터 테스트 커버리지 확보
- 시간 타입 일관성 확보 (Instant)
- JWT 표준 클레임(iss, aud) 포함 및 검증
- Brute Force 방어 메커니즘 구현
- Timing Attack 방지 (상수 시간 비교)
- 보안 헤더 적용 (XSS, 클릭재킹, CSP)
- JWT 필터 계정 상태 실시간 검증 (ACTIVE/SUSPENDED/WITHDRAWN/PENDING_VERIFICATION)
- 550개의 테스트 케이스로 높은 테스트 커버리지 확보
- 통합 테스트 (회원가입, 로그인, 토큰 갱신, 비밀번호 초기화, 계정 복구)
- E2E 테스트 (서비스 레벨 + HTTP 레벨) 완비

### 8.5 개선 필요

- CORS 설정이 과도하게 허용됨 (CRITICAL) - 프로덕션 배포 전 필수 수정
- `LoginAttempt` 도메인 테스트 부재 (LOW) - 서비스 테스트로 커버되지만 도메인 테스트 권장

### 8.6 프로덕션 배포 준비 상태

**조건부 가능** - 1개의 CRITICAL 이슈 해결 필요:
1. CORS 설정 개선 (환경별 Origin 제한)

---

## 변경 이력

| 날짜 | 작성자 | 내용 |
|------|-------|------|
| 2025-01-23 | Claude | 초기 코드 리뷰 작성 |
| 2026-01-24 | Claude | 이슈 2.2, 2.3, 2.5, 3.4, 3.5, 4.1, 4.2, 4.3, 4.6, 4.7 수정 완료 |
| 2026-01-24 | Claude | 2차 리뷰: 테스트 커버리지 개선 확인, 미해결 이슈 재평가, 종합 점수 업데이트 |
| 2026-01-25 | Claude | 3차 리팩토링: 이슈 2.2(JWT 클레임), 2.3(Timing Attack), 3.1(Brute Force), 3.2(PUBLIC_PATHS), 3.3(보안 헤더), 4.1(필터 테스트), 4.2(레거시 메서드) 해결 완료, 보안 점수 8.5/10으로 상향 |
| 2026-01-25 | Claude | 4차 리뷰: 도메인 테스트(RefreshToken 11개, EmailVerification 16개), 통합 테스트(8개 파일, 170개+), E2E 테스트(2개 파일, 20개) 완료 확인, 총 537개 테스트 케이스, 보안 점수 8.9/10으로 상향 |
| 2026-01-25 | Claude | T062 완료: JwtAuthenticationFilter 계정 상태 검증 추가, AccountStatusService 구현 (ACTIVE/SUSPENDED/WITHDRAWN/PENDING_VERIFICATION 상태 검증), 테스트 13개 추가 (AccountStatusServiceTest 6개, JwtAuthenticationFilterAccountStatusTest 7개), 총 550개 테스트 케이스 |
| 2026-01-25 | Claude | T061 완료: 이메일 재시도 로직 구현 (Spring Retry + Async), RetryConfig/AsyncConfig 생성, EmailService 인터페이스 WithRetry 메서드 추가, SmtpEmailService/LoggingEmailService 수정, SmtpEmailServiceRetryTest 17개 테스트 추가, 총 567개 테스트 케이스 |
