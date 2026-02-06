# 리프레시 토큰 로테이션 및 탈취 감지

## 배경

기존 구현에서는 Access Token 재발급 시 동일한 Refresh Token이 만료(7일)까지 재사용되는 구조였다. 이 경우 Refresh Token이 탈취되면 만료 전까지 공격자가 자유롭게 사용할 수 있으며, 탈취 사실을 감지할 수단이 없었다.

보안을 강화하기 위해 Refresh Token 로테이션(매 갱신마다 새 토큰 발급)과 Token Family 기반 탈취 감지 메커니즘 도입이 필요했다.

## 선택지

1. **Refresh Token 로테이션 없이 유효기간만 단축**: 단순히 Refresh Token 유효기간을 줄여 탈취 위험을 낮춘다. 구현이 간단하지만 탈취 자체를 감지할 수 없다.
2. **Refresh Token 로테이션 + Token Family 기반 탈취 감지**: 매 갱신마다 토큰을 교체하고, 이미 폐기된 토큰이 재사용되면 탈취로 간주하여 해당 Token Family 전체를 무효화한다. Grace Period를 두어 동시 탭에서의 경쟁 조건을 처리한다.
3. **Refresh Token을 Redis에 저장**: 서버 사이드 세션처럼 Redis에 토큰 상태를 저장하여 즉시 무효화를 지원한다. 인프라 복잡도가 증가하고 현재 스택(RDB 기반)과의 정합성 문제가 있다.

## 결정

- **선택지 2: Refresh Token 로테이션 + Token Family 기반 탈취 감지** 채택

## 결정 이유

- **탈취 감지 가능**: 폐기된 토큰의 재사용을 탐지하여 공격자와 정당한 사용자 모두를 보호할 수 있다
- **OWASP 권장 사항**: OWASP에서 권장하는 Refresh Token 로테이션 패턴을 따른다
- **Grace Period로 실용성 확보**: 동시 탭(다중 탭 열어둔 상태)에서의 경쟁 조건을 10초 Grace Period로 처리하여 사용자 경험을 해치지 않는다
- **기존 인프라 활용**: 추가 인프라(Redis 등) 없이 기존 RDB 기반으로 구현 가능하다
- **유효기간 단축과 시너지**: Access Token 5분 + Refresh Token 3일로 단축하여 토큰 노출 시간을 최소화한다

## 적용 범위

- `RefreshToken` 엔티티: `tokenFamily`, `replacedByToken`, `revokedAt` 필드 추가
- `RefreshTokenService.refreshToken()`: 로테이션 + 탈취 감지 로직
- `PasswordAuthController`: 갱신 응답에 새 Refresh Token을 Set-Cookie 헤더로 포함
- `LoginService`, `RecoverAccountService`: 로그인/복구 시 `createInitial()`로 새 Token Family 생성
- `application.yml`: Access Token 5분, Refresh Token 3일, Grace Period 10초

## 결과

### 구현 완료 항목

- DB 마이그레이션 V20: `token_family`, `replaced_by_token`, `revoked_at` 컬럼 추가
- 엔티티 변경: `RefreshToken.createInitial()`, `rotateWith()`, `isWithinGracePeriod()`
- 레포지토리 변경: `revokeAllByTokenFamily()`, `findByTokenFamilyAndRevokedFalse()`
- 서비스 로직: `@Transactional(noRollbackFor = RefreshTokenTheftException.class)` 적용
- 컨트롤러: `TokenRotationResult` 반환, Set-Cookie 헤더에 새 Refresh Token 설정
- 전체 테스트 통과 (1309개)

### 주요 기술적 결정

- **Hibernate 6 JPQL 제약**: `CURRENT_TIMESTAMP`가 `Instant` 필드에 할당 불가 → 파라미터 `@Param("now") Instant now` + default 메서드 패턴으로 해결
- **트랜잭션 롤백 방지**: 탈취 감지 시 Token Family 무효화가 롤백되지 않도록 `noRollbackFor` 적용

### 리팩토링 (2026-02-07)

PR #241 리뷰 반영 사항:

- **`revokeAllByTokenFamily` 반환 타입**: `void` → `int` 변경. 탈취 감지 시 무효화된 토큰 수를 로깅하여 운영 가시성 확보
- **Optimistic lock 예외 로깅**: `ObjectOptimisticLockingFailureException` catch 시 예외 객체(`e`)를 로그에 포함
- **Grace Period 방어적 코딩**: `revokedToken.getUser()` 대신 `activeToken.getUser()`를 사용하여 JWT 발급. 토큰 패밀리 내 사용자 불일치 가능성에 대비
- **`rotateWith()` 검증 강화**: `newToken` null 체크(`Objects.requireNonNull`) + 이미 revoked된 토큰에 대해 `IllegalStateException` throw (프로그래밍 에러 조기 감지). `revoke()`는 idempotent 유지
- **`TokenRotationResult` 검증**: compact constructor에서 `accessToken` null 체크 + `accessTokenValidity > 0` 검증. `gracePeriod()` 팩토리 메서드 추가
- **쿠키 정리 확장**: `PasswordAuthController`에서 `RefreshTokenExpiredException` 발생 시에도 만료된 쿠키를 삭제 (기존에는 `RefreshTokenTheftException`만 처리)
- 전체 테스트 통과 (1317개, +8개 추가)

## 후속 조치

- [ ] Refresh Token 정리 스케줄러: 만료된 토큰 체인(Token Family) 주기적 삭제
- [ ] 프론트엔드: 갱신 응답의 Set-Cookie에서 새 Refresh Token을 자동으로 사용하도록 확인
- [ ] 모니터링: 탈취 감지 이벤트 로깅 및 알림 체계 구축
