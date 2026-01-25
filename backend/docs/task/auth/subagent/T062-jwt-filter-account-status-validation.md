# T062: JwtAuthenticationFilter 계정 상태 검증 추가

## 개요

현재 JwtAuthenticationFilter는 JWT 토큰의 유효성만 검증하고 있습니다. 토큰은 유효하지만 계정이 정지(SUSPENDED)되었거나 탈퇴(WITHDRAWN)된 경우에도 인증이 성공하는 문제가 있습니다.

## 현재 상태

```java
// JwtAuthenticationFilter.java - 현재 구현
Claims claims = jwtTokenProvider.validateAccessTokenAndGetClaims(token);
Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
// ... 바로 인증 성공 처리 (DB 조회 없음)
```

## 문제점

1. **정지된 계정**: 관리자가 계정을 정지해도 기존 발급된 토큰으로 계속 접근 가능
2. **탈퇴된 계정**: 탈퇴 후에도 토큰 만료 전까지 접근 가능
3. **보안 취약점**: 계정 상태 변경이 즉시 반영되지 않음

## 구현 목표

- 모든 인증된 요청에서 계정 상태 확인
- 정지/탈퇴 계정은 인증 거부
- 성능 고려 (캐싱 옵션)

## 구현 방식 선택

### Option A: 매 요청마다 DB 조회 (단순)
- 장점: 구현 간단, 실시간 반영
- 단점: DB 부하 증가

### Option B: 캐시 활용 (권장)
- 장점: DB 부하 감소, 빠른 응답
- 단점: 캐시 무효화 로직 필요

### Option C: 블랙리스트 방식
- 장점: 정상 사용자에게 오버헤드 없음
- 단점: 블랙리스트 관리 복잡

**선택: Option A (DB 조회)** - 초기 구현으로 단순함 우선, 추후 Option B로 최적화 가능

## 구현 상세

### 1. 계정 상태 조회 서비스 생성

**파일 경로**: `backend/src/main/java/igrus/web/security/auth/common/service/AccountStatusService.java`

```java
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountStatusService {

    private final UserRepository userRepository;

    /**
     * 사용자 계정 상태가 활성 상태인지 확인
     * @param userId 사용자 ID
     * @return 활성 상태이면 true
     * @throws AccountSuspendedException 계정이 정지된 경우
     * @throws AccountWithdrawnException 계정이 탈퇴된 경우
     */
    public boolean validateAccountStatus(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> {
                log.warn("JWT에 포함된 userId로 사용자를 찾을 수 없음: {}", userId);
                return new AccessTokenInvalidException(ErrorCode.ACCESS_TOKEN_INVALID);
            });

        switch (user.getStatus()) {
            case ACTIVE:
                return true;
            case SUSPENDED:
                log.warn("정지된 계정으로 접근 시도: userId={}", userId);
                throw new AccountSuspendedException(ErrorCode.ACCOUNT_SUSPENDED);
            case WITHDRAWN:
                log.warn("탈퇴된 계정으로 접근 시도: userId={}", userId);
                throw new AccountWithdrawnException(ErrorCode.ACCOUNT_WITHDRAWN);
            default:
                log.error("알 수 없는 계정 상태: userId={}, status={}", userId, user.getStatus());
                throw new AccessTokenInvalidException(ErrorCode.ACCESS_TOKEN_INVALID);
        }
    }
}
```

### 2. JwtAuthenticationFilter 수정

**파일 경로**: `backend/src/main/java/igrus/web/security/jwt/JwtAuthenticationFilter.java`

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final AccountStatusService accountStatusService;  // 추가
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        extractToken(request).ifPresent(token -> {
            try {
                // 1. Access Token 유효성 검증 및 Claims 추출
                Claims claims = jwtTokenProvider.validateAccessTokenAndGetClaims(token);

                Long userId = jwtTokenProvider.getUserIdFromClaims(claims);
                String studentId = jwtTokenProvider.getStudentIdFromClaims(claims);
                String role = jwtTokenProvider.getRoleFromClaims(claims);

                // 2. 계정 상태 검증 (NEW)
                accountStatusService.validateAccountStatus(userId);

                // 3. 인증 객체 생성
                AuthenticatedUser principal = new AuthenticatedUser(userId, studentId, role);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("인증 성공: userId={}, studentId={}", userId, studentId);

            } catch (AccountSuspendedException e) {
                log.warn("계정 정지로 인증 거부: path={}", request.getRequestURI());
                // SecurityContext에 인증 정보 설정하지 않음
            } catch (AccountWithdrawnException e) {
                log.warn("계정 탈퇴로 인증 거부: path={}", request.getRequestURI());
                // SecurityContext에 인증 정보 설정하지 않음
            } catch (CustomBaseException e) {
                log.warn("인증 실패 - {}: path={}", e.getMessage(), request.getRequestURI());
            } catch (Exception e) {
                log.error("인증 처리 중 예외 발생: path={}, error={}", request.getRequestURI(), e.getMessage());
            }
        });

        filterChain.doFilter(request, response);
    }

    // ... 기존 메서드 유지
}
```

### 3. 예외 응답 처리 (선택사항)

계정 상태 관련 예외를 명확한 HTTP 응답으로 변환하려면 `JwtAuthenticationEntryPoint` 또는 별도 필터에서 처리:

```java
// GlobalExceptionHandler에서 처리 (이미 있을 수 있음)
@ExceptionHandler(AccountSuspendedException.class)
public ResponseEntity<ErrorResponse> handleAccountSuspended(AccountSuspendedException e) {
    return ResponseEntity
        .status(HttpStatus.FORBIDDEN)
        .body(ErrorResponse.of(e.getErrorCode()));
}
```

## 성능 최적화 (향후)

초기 구현 후 성능 이슈 발생 시 캐시 적용:

```java
@Cacheable(value = "userStatus", key = "#userId", unless = "#result == null")
public UserStatus getUserStatus(Long userId) {
    return userRepository.findStatusById(userId);
}

// 계정 상태 변경 시 캐시 무효화
@CacheEvict(value = "userStatus", key = "#userId")
public void evictUserStatusCache(Long userId) {}
```

## 테스트 계획

### 단위 테스트

**파일 경로**: `backend/src/test/java/igrus/web/security/auth/common/service/AccountStatusServiceTest.java`

| 테스트 케이스 | 설명 |
|-------------|------|
| ACTIVE 계정 | 정상적으로 true 반환 |
| SUSPENDED 계정 | AccountSuspendedException 발생 |
| WITHDRAWN 계정 | AccountWithdrawnException 발생 |
| 존재하지 않는 userId | AccessTokenInvalidException 발생 |

### 통합 테스트

**파일 경로**: `backend/src/test/java/igrus/web/security/jwt/JwtAuthenticationFilterAccountStatusTest.java`

| 테스트 케이스 | 설명 |
|-------------|------|
| 활성 계정 + 유효 토큰 | 인증 성공, 요청 처리 |
| 정지 계정 + 유효 토큰 | 인증 실패, 403 반환 |
| 탈퇴 계정 + 유효 토큰 | 인증 실패, 403 반환 |
| 실시간 상태 변경 반영 | 토큰 발급 후 계정 정지 → 다음 요청 거부 |

## 고려사항

1. **순환 참조 방지**: AccountStatusService가 UserRepository만 의존하도록 설계
2. **DB 부하**: 인증된 모든 요청에서 DB 조회 발생 → 모니터링 필요
3. **응답 일관성**: 401 vs 403 응답 구분 (토큰 무효 vs 계정 상태 문제)

## 체크리스트

- [ ] AccountStatusService 생성
- [ ] JwtAuthenticationFilter에 계정 상태 검증 로직 추가
- [ ] AccountStatusService 단위 테스트 작성
- [ ] JwtAuthenticationFilter 통합 테스트 작성
- [ ] 계정 정지/탈퇴 시나리오 E2E 테스트
- [ ] 성능 모니터링 및 필요시 캐시 적용