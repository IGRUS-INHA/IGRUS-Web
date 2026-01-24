# 백엔드 인증 로직 코드 리뷰

> 작성일: 2025-01-23
> 최종 수정일: 2026-01-24
> 리뷰 범위: `backend/src/main/java/igrus/web/security/**`

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
| Security Config | `ApiSecurityConfig.java`, `PublicResourceSecurityConfig.java`, `SecurityConfigUtil.java` |
| Password Auth | `PasswordAuthService.java`, `PasswordSignupService.java`, `PasswordAuthController.java` |
| 도메인/공통 서비스 | `EmailVerification.java`, `RefreshToken.java`, `PrivacyConsent.java`, `SmtpEmailService.java` 등 |
| 테스트 | `JwtTokenProviderTest.java`, `PasswordAuthServiceLoginTest.java`, `PasswordSignupServiceTest.java` 등 |

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

**파일**: `JwtTokenProvider.java` (42-54행)

```java
return Jwts.builder()
        .subject(userId.toString())
        .claim("studentId", studentId)
        .claim("role", role)
        .claim("type", "access")
        .issuedAt(now)
        .expiration(expiry)
        .signWith(secretKey)
        .compact();
```

**상태**: ⚠️ **미해결**

**문제점**:
- JWT 표준 클레임 누락: `iss` (Issuer), `aud` (Audience)
- 토큰 생성 환경/버전 추적 불가
- 토큰 용도 검증 불충분

**권고사항**:
```java
return Jwts.builder()
        .subject(userId.toString())
        .issuer("igrus-api")
        .audience().add("igrus-web").and()
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

**파일**: `PasswordSignupService.java` (136행)

```java
if (!verification.getCode().equals(request.code())) {
    verification.incrementAttempts();
    emailVerificationRepository.save(verification);
    throw new VerificationCodeInvalidException();
}
```

**상태**: ⚠️ **미해결**

**문제점**:
- `String.equals()`는 첫 번째 불일치 문자에서 즉시 반환
- 응답 시간 차이로 인증 코드 유추 가능
- 6자리 코드는 충분히 추측 가능 범위

**권고사항**:
```java
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

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

**상태**: ⚠️ **미해결**

**문제점**:
- 로그인 실패 횟수 제한 없음
- IP 기반 Rate Limiting 없음
- 계정 잠금 기능 없음

**권고사항**:
```java
// Redis를 사용한 로그인 실패 추적
@Service
public class LoginAttemptService {
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCK_TIME_MINUTES = 15;

    private final RedisTemplate<String, Integer> redisTemplate;

    public void recordFailedLogin(String studentId) {
        String key = "login_attempts:" + studentId;
        Integer attempts = redisTemplate.opsForValue().get(key);

        if (attempts == null) {
            redisTemplate.opsForValue().set(key, 1, Duration.ofMinutes(LOCK_TIME_MINUTES));
        } else {
            redisTemplate.opsForValue().increment(key);
        }
    }

    public boolean isAccountLocked(String studentId) {
        String key = "login_attempts:" + studentId;
        Integer attempts = redisTemplate.opsForValue().get(key);
        return attempts != null && attempts >= MAX_ATTEMPTS;
    }
}
```

---

### 3.2 PUBLIC_PATHS 이중 관리

**파일**: `ApiSecurityConfig.java`, `JwtAuthenticationFilter.java`

```java
// ApiSecurityConfig.java (32-40행)
.requestMatchers(
    "/api/v1/auth/password/**",
    "/api/privacy/policy",
    "/api/inquiries",
    "/api/inquiries/lookup"
).permitAll()

// JwtAuthenticationFilter.java (34-39행)
private static final Set<String> PUBLIC_PATHS = Set.of(
    "/api/v1/auth/password/**",
    "/api/privacy/policy",
    "/api/inquiries",
    "/api/inquiries/lookup"
);
```

**상태**: ⚠️ **미해결**

**문제점**:
- 동일한 경로를 두 곳에서 관리
- 변경 시 불일치 발생 위험

**권고사항**:
```java
// SecurityPaths.java (공통 상수 클래스)
public final class SecurityPaths {
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

**파일**: `ApiSecurityConfig.java`

**상태**: ⚠️ **미해결**

**권고사항**:
```java
http.headers(headers -> headers
    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
    .frameOptions(frame -> frame.deny())
    .xssProtection(xss -> xss.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
);
```

---

## 4. Medium 이슈 (계획된 조치)

### 4.1 JwtAuthenticationFilter 테스트 전무

**상태**: ⚠️ **미해결**

**예상 필요 케이스**:
```
- 유효한 토큰으로 인증 성공
- Authorization 헤더 없음 -> 인증 없이 통과
- Bearer 접두사 없음 -> 인증 없이 통과
- 만료된 토큰 -> 로깅 후 통과
- Refresh Token으로 접근 시도 -> InvalidTokenTypeException
- 공개 경로 -> shouldNotFilter() 반환 true
```

---

### 4.2 레거시 JWT 메서드 정리 필요

**파일**: `JwtTokenProvider.java`

**문제점**:
- `validateAccessTokenAndGetClaims()` 추가로 토큰 파싱 최적화됨
- 그러나 레거시 메서드들 (`getUserId()`, `getStudentId()`, `getRole()`)이 여전히 존재
- 다른 코드에서 재파싱 발생 가능

**권고사항**:
- 레거시 메서드들을 `@Deprecated`로 표시
- 점진적으로 제거

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

### 5.2 Security Config

| 항목 | 설명 |
|------|------|
| Stateless 세션 | JWT 인증에 적합한 `STATELESS` 세션 정책 |
| JWT 필터 위치 | `UsernamePasswordAuthenticationFilter` 앞에 적절히 배치 |
| 불필요한 인증 비활성화 | Form Login, HTTP Basic 비활성화로 공격 표면 감소 |
| Config 분리 | API/정적 리소스 별도 SecurityFilterChain ✅ |

### 5.3 Password Auth

| 항목 | 설명 |
|------|------|
| BCryptPasswordEncoder | 업계 표준 암호화 알고리즘 사용 |
| Bean Validation | 입력 검증에 어노테이션 활용 |
| 비밀번호 복잡도 | 정규식으로 대/소문자, 숫자, 특수문자 요구 |
| 비밀번호 최대 길이 | 72자 제한으로 BCrypt 한계 준수 ✅ |
| SecureRandom | 예측 불가능한 인증 코드 생성 |
| Rate Limiting | 인증 코드 재발송 5분 제한 ✅ |

### 5.4 도메인 설계

| 항목 | 설명 |
|------|------|
| 정적 팩토리 메서드 | 객체 생성 로직 캡슐화 (`create()`) |
| 비즈니스 로직 캡슐화 | `isExpired()`, `canAttempt()` 등 도메인 내부 구현 |
| Refresh Token 저장 | DB 저장으로 revoke 기능 지원 |
| 환경별 구현체 분리 | Profile 기반 EmailService 전략 패턴 |
| 시간 타입 통일 | 모든 엔티티에서 `Instant` 사용 ✅ |

### 5.5 운영 기능

| 항목 | 설명 |
|------|------|
| 미인증 사용자 정리 | 24시간 후 자동 삭제 배치 스케줄러 ✅ |
| SMTP 발신자 설정 | 명시적 from 주소 설정 ✅ |

---

## 6. 테스트 커버리지 분석

### 6.1 현재 테스트 현황

| 파일 | 테스트 유형 | 테스트 수 | 상태 |
|------|------------|---------|------|
| `JwtTokenProviderTest.java` | 단위 테스트 | 10개 | ✅ 양호 |
| `PasswordSignupServiceTest.java` | 서비스 Mock | 20개 | ✅ 양호 |
| `PasswordAuthServiceLoginTest.java` | 서비스 Mock | 22개 | ✅ 양호 |
| `PasswordAuthServiceTokenTest.java` | 서비스 Mock | 11개 | ✅ 양호 |
| `PrivacyConsentTest.java` | 도메인 단위 | 5개 | ✅ 양호 |
| `PrivacyConsentServiceTest.java` | 서비스 Mock | 10개 | ✅ 양호 |
| `PrivacyConsentRepositoryTest.java` | 통합 테스트 | 12개 | ✅ 양호 |
| `AuthenticatedUserTest.java` | 단위 테스트 | 2개 | ⚠️ 최소한 |

### 6.2 테스트 전무 컴포넌트

#### JwtAuthenticationFilter (예상 10+ 케이스)
```
- 유효한 토큰 -> SecurityContext 설정 성공
- Authorization 헤더 없음 -> 인증 없이 통과
- Bearer 접두사 없음 -> 인증 없이 통과
- 만료된 토큰 -> 로깅 후 통과
- Refresh Token으로 접근 시도 -> InvalidTokenTypeException
- 공개 경로 -> shouldNotFilter() true
```

#### 도메인 테스트 보완 필요
- `RefreshToken` 도메인 테스트
- `EmailVerification` 도메인 테스트

---

## 7. 조치 권고 사항

### 7.1 1단계: 프로덕션 배포 전 필수

- [ ] CORS Origin 제한 (프로덕션용 도메인만 허용)
- [ ] Timing Attack 완화 (MessageDigest.isEqual 적용)
- [ ] Brute Force 방어 구현 (Redis 기반 로그인 실패 추적)

### 7.2 2단계: 보안 강화

- [ ] PUBLIC_PATHS 상수 중앙화
- [ ] JWT 표준 클레임 추가 (issuer, audience)
- [ ] 보안 헤더 설정 추가

### 7.3 3단계: 테스트 보완

- [ ] `JwtAuthenticationFilter` 테스트 작성 (10+ 케이스)
- [ ] `RefreshToken`, `EmailVerification` 도메인 테스트 작성
- [ ] 통합 테스트 추가 (E2E 인증 플로우)

### 7.4 4단계: 코드 품질

- [ ] 레거시 JWT 메서드 @Deprecated 처리 및 정리

### 7.5 완료된 항목

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

---

## 8. 종합 평가

### 8.1 영역별 점수

| 영역 | 점수 | 비고 |
|-----|------|------|
| JWT 구현 | 8/10 | 비밀키 검증 및 파싱 최적화 완료, 표준 클레임 누락 |
| Security Config | 6/10 | CORS 수정 필요, Config 분리는 양호 |
| Password Auth | 7/10 | Brute Force 방어 추가 필요 |
| 도메인/서비스 | 9/10 | 시간 타입 통일, 배치 스케줄러 완료 |
| 테스트 커버리지 | 7/10 | 핵심 서비스 테스트 완료, 필터 테스트 부족 |

### 8.2 전체 보안 점수

**6.8 / 10** (이전: 6.4)

### 8.3 OWASP Top 10 매핑

| OWASP 순위 | 취약점 | 현황 | 심각도 |
|----------|--------|------|--------|
| A01:2021 - Broken Access Control | PUBLIC_PATHS 이중 관리 | ⚠️ 미해결 | MEDIUM |
| A02:2021 - Cryptographic Failures | CORS + allowCredentials | ⚠️ 미해결 | **CRITICAL** |
| A07:2021 - CSRF | JWT 기반 + CSRF 비활성화 | ✅ 해결 | LOW |
| A07:2021 - Auth Failures | Brute Force 방어 부재 | ⚠️ 미해결 | **HIGH** |
| A03:2021 - Injection | Timing Attack 취약점 | ⚠️ 미해결 | **HIGH** |

### 8.4 강점

- JWT 라이브러리(JJWT)의 최신 API 올바르게 사용
- Access/Refresh Token 분리 및 타입 검증으로 토큰 혼용 공격 방지
- 도메인 주도 설계(DDD) 원칙 준수
- 팩토리 메서드 패턴으로 객체 생성 로직 캡슐화
- 예외 처리가 체계적으로 구성됨
- 핵심 서비스 테스트 커버리지 확보
- 시간 타입 일관성 확보 (Instant)

### 8.5 개선 필요

- CORS 설정이 과도하게 허용됨 (CRITICAL)
- Brute Force 방어 메커니즘 부재 (HIGH)
- Timing Attack 취약점 (HIGH)
- JWT 표준 클레임(`iss`, `aud`) 누락
- JwtAuthenticationFilter 테스트 전무

### 8.6 프로덕션 배포 준비 상태

**불가** - 3개의 CRITICAL/HIGH 이슈 해결 필요:
1. CORS 설정 개선 (예상: 30분)
2. Timing Attack 완화 (예상: 20분)
3. Brute Force 방어 구현 (예상: 2-3시간)

---

## 변경 이력

| 날짜 | 작성자 | 내용 |
|------|-------|------|
| 2025-01-23 | Claude | 초기 코드 리뷰 작성 |
| 2026-01-24 | Claude | 이슈 2.2, 2.3, 2.5, 3.4, 3.5, 4.1, 4.2, 4.3, 4.6, 4.7 수정 완료 |
| 2026-01-24 | Claude | 2차 리뷰: 테스트 커버리지 개선 확인, 미해결 이슈 재평가, 종합 점수 업데이트 |
