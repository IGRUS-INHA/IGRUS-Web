package igrus.web.security.jwt;

import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.security.auth.common.service.AccountStatusService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 하이브리드 테스트")
class JwtAuthenticationFilterTest {

    // 실제 객체 사용 (고전파)
    private JwtTokenProvider jwtTokenProvider;
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    private JsonMapper jsonMapper;

    // Mock 유지 (런던파 - Servlet API 의존성 및 외부 서비스)
    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private AccountStatusService accountStatusService;

    // 테스트용 설정 값
    private static final String TEST_SECRET_KEY = "ThisIsATestSecretKeyThatIsLongEnoughForHS256AlgorithmAtLeast256Bits";
    private static final long ACCESS_TOKEN_VALIDITY = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_VALIDITY = 604800000L; // 7일
    private static final String TEST_ISSUER = "igrus-test";
    private static final String TEST_AUDIENCE = "igrus-web-test";

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();

        // 실제 JwtTokenProvider 생성
        jwtTokenProvider = new JwtTokenProvider(
                TEST_SECRET_KEY,
                ACCESS_TOKEN_VALIDITY,
                REFRESH_TOKEN_VALIDITY,
                TEST_ISSUER,
                TEST_AUDIENCE
        );

        jsonMapper = JsonMapper.builder().build();

        jwtAuthenticationFilter = new JwtAuthenticationFilter(
                jwtTokenProvider,
                accountStatusService,
                jsonMapper
        );
    }

    @Nested
    @DisplayName("공개 경로 처리 테스트")
    class PublicPathTest {

        @ParameterizedTest
        @ValueSource(strings = {
                "/api/v1/auth/password/login",
                "/api/v1/auth/password/signup",
                "/api/v1/auth/password/reset",
                "/api/v1/auth/password/verify"
        })
        @DisplayName("[JWT-FILTER-001] 비밀번호 인증 경로 요청 시 필터 스킵")
        void shouldNotFilter_WhenPasswordAuthPath_ReturnsTrue(String path) {
            // given
            given(request.getRequestURI()).willReturn(path);

            // when
            boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("[JWT-FILTER-002] 개인정보 처리방침 경로 요청 시 필터 스킵")
        void shouldNotFilter_WhenPrivacyPolicyPath_ReturnsTrue() {
            // given
            given(request.getRequestURI()).willReturn("/api/privacy/policy");

            // when
            boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("[JWT-FILTER-003] 비회원 문의 작성 경로 요청 시 필터 스킵")
        void shouldNotFilter_WhenGuestInquiriesPath_ReturnsTrue() {
            // given
            given(request.getRequestURI()).willReturn("/api/v1/inquiries/guest");

            // when
            boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("[JWT-FILTER-004] 비회원 문의 조회 경로 요청 시 필터 스킵")
        void shouldNotFilter_WhenInquiriesLookupPath_ReturnsTrue() {
            // given
            given(request.getRequestURI()).willReturn("/api/v1/inquiries/lookup");

            // when
            boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/api/users",
                "/api/v1/boards",
                "/api/v1/posts",
                "/api/admin/settings"
        })
        @DisplayName("[JWT-FILTER-005] 보호된 경로 요청 시 필터 실행")
        void shouldNotFilter_WhenProtectedPath_ReturnsFalse(String path) {
            // given
            given(request.getRequestURI()).willReturn(path);

            // when
            boolean result = jwtAuthenticationFilter.shouldNotFilter(request);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("인증 성공 테스트")
    class AuthenticationSuccessTest {

        @Test
        @DisplayName("[JWT-FILTER-006] 유효한 Access Token으로 SecurityContext 설정 성공")
        void doFilterInternal_WithValidAccessToken_SetsSecurityContext() throws ServletException, IOException {
            // given
            Long userId = 1L;
            String studentId = "12345678";
            String role = "MEMBER";

            // 실제 토큰 생성
            String validToken = jwtTokenProvider.createAccessToken(userId, studentId, role);

            given(request.getHeader("Authorization")).willReturn("Bearer " + validToken);
            // AccountStatusService가 예외를 던지지 않도록 설정
            willDoNothing().given(accountStatusService).validateAccountStatus(userId);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();
            assertThat(authentication.getPrincipal()).isInstanceOf(AuthenticatedUser.class);

            AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
            assertThat(principal.userId()).isEqualTo(userId);
            assertThat(principal.studentId()).isEqualTo(studentId);
            assertThat(principal.role()).isEqualTo(role);

            then(filterChain).should(times(1)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("인증 없이 통과 테스트")
    class PassWithoutAuthTest {

        @Test
        @DisplayName("[JWT-FILTER-007] Authorization 헤더 없음 - 인증 없이 통과")
        void doFilterInternal_WithoutAuthorizationHeader_PassesWithoutAuth() throws ServletException, IOException {
            // given
            given(request.getHeader("Authorization")).willReturn(null);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            then(filterChain).should(times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("[JWT-FILTER-008] Bearer 접두사 없음 - 인증 없이 통과")
        void doFilterInternal_WithoutBearerPrefix_PassesWithoutAuth() throws ServletException, IOException {
            // given
            given(request.getHeader("Authorization")).willReturn("InvalidPrefix token.value");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            then(filterChain).should(times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("[JWT-FILTER-009] 빈 Authorization 헤더 - 인증 없이 통과")
        void doFilterInternal_WithEmptyAuthorizationHeader_PassesWithoutAuth() throws ServletException, IOException {
            // given
            given(request.getHeader("Authorization")).willReturn("");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            then(filterChain).should(times(1)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("토큰 검증 실패 테스트")
    class TokenValidationFailureTest {

        @Test
        @DisplayName("[JWT-FILTER-010] 만료된 토큰 - 로그 기록 후 통과")
        void doFilterInternal_WithExpiredToken_LogsAndPasses() throws ServletException, IOException {
            // given
            // 만료된 토큰을 시뮬레이션하기 위해 매우 짧은 유효기간으로 JwtTokenProvider 생성
            JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(
                    TEST_SECRET_KEY,
                    1L, // 1ms 유효기간
                    REFRESH_TOKEN_VALIDITY,
                    TEST_ISSUER,
                    TEST_AUDIENCE
            );

            String expiredToken = expiredTokenProvider.createAccessToken(1L, "12345678", "MEMBER");

            // 토큰이 만료될 때까지 잠시 대기
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            given(request.getHeader("Authorization")).willReturn("Bearer " + expiredToken);
            given(request.getRequestURI()).willReturn("/api/users");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            then(filterChain).should(times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("[JWT-FILTER-011] Refresh Token을 Access Token으로 사용 - InvalidTokenTypeException 후 통과")
        void doFilterInternal_WithRefreshTokenAsAccess_LogsAndPasses() throws ServletException, IOException {
            // given
            // Refresh 토큰 생성
            String refreshToken = jwtTokenProvider.createRefreshToken(1L);

            given(request.getHeader("Authorization")).willReturn("Bearer " + refreshToken);
            given(request.getRequestURI()).willReturn("/api/users");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            then(filterChain).should(times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("[JWT-FILTER-012] 유효하지 않은 토큰 - 로그 기록 후 통과")
        void doFilterInternal_WithInvalidToken_LogsAndPasses() throws ServletException, IOException {
            // given
            String invalidToken = "invalid.token.value";

            given(request.getHeader("Authorization")).willReturn("Bearer " + invalidToken);
            given(request.getRequestURI()).willReturn("/api/users");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            then(filterChain).should(times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("[JWT-FILTER-013] 변조된 토큰 - 로그 기록 후 통과")
        void doFilterInternal_WithTamperedToken_LogsAndPasses() throws ServletException, IOException {
            // given
            String validToken = jwtTokenProvider.createAccessToken(1L, "12345678", "MEMBER");
            // 토큰 변조 - 서명 부분의 여러 문자를 변경하여 확실히 무효화
            String[] parts = validToken.split("\\.");
            String tamperedSignature = "TAMPERED" + parts[2].substring(8);
            String tamperedToken = parts[0] + "." + parts[1] + "." + tamperedSignature;

            given(request.getHeader("Authorization")).willReturn("Bearer " + tamperedToken);
            given(request.getRequestURI()).willReturn("/api/users");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            then(filterChain).should(times(1)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("역할별 권한 부여 테스트")
    class RoleAuthorityTest {

        @Test
        @DisplayName("[JWT-FILTER-014] ASSOCIATE 역할에 대한 GrantedAuthority 설정")
        void doFilterInternal_WithAssociateRole_SetsCorrectAuthority() throws ServletException, IOException {
            verifyRoleAuthority("ASSOCIATE");
        }

        @Test
        @DisplayName("[JWT-FILTER-015] MEMBER 역할에 대한 GrantedAuthority 설정")
        void doFilterInternal_WithMemberRole_SetsCorrectAuthority() throws ServletException, IOException {
            verifyRoleAuthority("MEMBER");
        }

        @Test
        @DisplayName("[JWT-FILTER-016] OPERATOR 역할에 대한 GrantedAuthority 설정")
        void doFilterInternal_WithOperatorRole_SetsCorrectAuthority() throws ServletException, IOException {
            verifyRoleAuthority("OPERATOR");
        }

        @Test
        @DisplayName("[JWT-FILTER-017] ADMIN 역할에 대한 GrantedAuthority 설정")
        void doFilterInternal_WithAdminRole_SetsCorrectAuthority() throws ServletException, IOException {
            verifyRoleAuthority("ADMIN");
        }

        private void verifyRoleAuthority(String role) throws ServletException, IOException {
            // given
            Long userId = 1L;
            String studentId = "12345678";

            // 실제 토큰 생성
            String validToken = jwtTokenProvider.createAccessToken(userId, studentId, role);

            given(request.getHeader("Authorization")).willReturn("Bearer " + validToken);
            // AccountStatusService가 예외를 던지지 않도록 설정
            willDoNothing().given(accountStatusService).validateAccountStatus(userId);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            assertThat(authentication).isNotNull();

            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            assertThat(authorities).hasSize(1);
            assertThat(authorities.iterator().next().getAuthority()).isEqualTo("ROLE_" + role);

            then(filterChain).should(times(1)).doFilter(request, response);

            // Clean up for next test
            SecurityContextHolder.clearContext();
        }
    }

    @Nested
    @DisplayName("필터 체인 동작 테스트")
    class FilterChainBehaviorTest {

        @Test
        @DisplayName("[JWT-FILTER-018] 인증 성공 후에도 필터 체인 계속 진행")
        void doFilterInternal_AfterSuccessfulAuth_ContinuesFilterChain() throws ServletException, IOException {
            // given
            Long userId = 1L;
            String studentId = "12345678";
            String role = "MEMBER";

            String validToken = jwtTokenProvider.createAccessToken(userId, studentId, role);
            given(request.getHeader("Authorization")).willReturn("Bearer " + validToken);
            // AccountStatusService가 예외를 던지지 않도록 설정
            willDoNothing().given(accountStatusService).validateAccountStatus(userId);

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            then(filterChain).should(times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("[JWT-FILTER-019] 인증 실패 후에도 필터 체인 계속 진행")
        void doFilterInternal_AfterFailedAuth_ContinuesFilterChain() throws ServletException, IOException {
            // given
            String invalidToken = "invalid.token";
            given(request.getHeader("Authorization")).willReturn("Bearer " + invalidToken);
            given(request.getRequestURI()).willReturn("/api/users");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            then(filterChain).should(times(1)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("토큰 추출 엣지 케이스 테스트")
    class TokenExtractionEdgeCaseTest {

        @Test
        @DisplayName("[JWT-FILTER-020] Bearer 접두사만 있는 경우 - 빈 토큰으로 인증 시도하여 실패")
        void doFilterInternal_WithOnlyBearerPrefix_AttemptsAuthAndFails() throws ServletException, IOException {
            // given
            given(request.getHeader("Authorization")).willReturn("Bearer ");
            given(request.getRequestURI()).willReturn("/api/users");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            then(filterChain).should(times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("[JWT-FILTER-021] Bearer 대소문자 변형 - 인증 없이 통과")
        void doFilterInternal_WithLowercaseBearer_PassesWithoutAuth() throws ServletException, IOException {
            // given
            given(request.getHeader("Authorization")).willReturn("bearer token.value");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            then(filterChain).should(times(1)).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("다른 Secret Key로 서명된 토큰 테스트")
    class DifferentSecretKeyTest {

        @Test
        @DisplayName("[JWT-FILTER-022] 다른 Secret Key로 서명된 토큰 - 인증 실패")
        void doFilterInternal_WithDifferentSecretKey_FailsAuthentication() throws ServletException, IOException {
            // given
            String differentSecretKey = "DifferentSecretKeyThatIsAlsoLongEnoughForHS256AlgorithmAtLeast256Bits";
            JwtTokenProvider differentProvider = new JwtTokenProvider(
                    differentSecretKey,
                    ACCESS_TOKEN_VALIDITY,
                    REFRESH_TOKEN_VALIDITY,
                    TEST_ISSUER,
                    TEST_AUDIENCE
            );

            String tokenFromDifferentProvider = differentProvider.createAccessToken(1L, "12345678", "MEMBER");

            given(request.getHeader("Authorization")).willReturn("Bearer " + tokenFromDifferentProvider);
            given(request.getRequestURI()).willReturn("/api/users");

            // when
            jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

            // then
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            then(filterChain).should(times(1)).doFilter(request, response);
        }
    }
}
