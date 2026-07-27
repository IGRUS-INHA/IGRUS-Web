package igrus.web.security.auth.sso;

import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.password.controller.ControllerIntegrationTestBase;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static igrus.web.common.OpenApiValidatorUtil.matchesOpenApiSpec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SSO 핸드오프 통합 테스트
 *
 * <p>authorize(일회용 코드 발급 리다이렉트)와 token(코드 → 토큰 교환) 흐름을 검증합니다.</p>
 */
@DisplayName("SSO 핸드오프 통합 테스트")
class SsoAuthIntegrationTest extends ControllerIntegrationTestBase {

    private static final String AUTHORIZE_PATH = "/api/v1/auth/sso/authorize";
    private static final String TOKEN_PATH = "/api/v1/auth/sso/token";
    private static final String ALLOWED_REDIRECT = "https://play.test";

    @Autowired
    private SsoService ssoService;

    @Autowired
    private SsoCodeStore ssoCodeStore;

    @Autowired
    private Clock clock;

    @BeforeEach
    void setUp() {
        setUpControllerTest();
        Mockito.reset(clock);
        when(clock.instant()).thenReturn(Instant.now());
        ReflectionTestUtils.setField(ssoService, "accessTokenValidity", ACCESS_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(ssoService, "refreshTokenValidity", REFRESH_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(ssoService, "allowedRedirectOrigins", List.of(ALLOWED_REDIRECT));
    }

    private User createActiveUser() {
        return createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER, UserStatus.ACTIVE);
    }

    private String createAndSaveRefreshToken(User user) {
        String token = jwtTokenProvider.createRefreshToken(user.getId());
        refreshTokenRepository.save(RefreshToken.createInitial(user, token, REFRESH_TOKEN_VALIDITY));
        return token;
    }

    private String extractQueryParam(String location, String name) {
        return java.util.Arrays.stream(location.substring(location.indexOf('?') + 1).split("&"))
                .filter(p -> p.startsWith(name + "="))
                .map(p -> p.substring(name.length() + 1))
                .findFirst()
                .orElse(null);
    }

    @Nested
    @DisplayName("authorize - 일회용 코드 발급")
    class AuthorizeTest {

        @Test
        @DisplayName("유효한 refresh 쿠키로 요청 시 sso_code를 붙여 리다이렉트한다")
        void ssoAuthorize_withValidRefreshCookie_redirectsWithCode() throws Exception {
            User user = createActiveUser();
            String refreshToken = createAndSaveRefreshToken(user);

            MvcResult result = mockMvc.perform(get(AUTHORIZE_PATH)
                            .param("redirect_uri", ALLOWED_REDIRECT + "/")
                            .cookie(new Cookie("refreshToken", refreshToken)))
                    .andExpect(status().isFound())
                    .andReturn();

            String location = result.getResponse().getHeader("Location");
            assertThat(location).startsWith(ALLOWED_REDIRECT + "/?sso_code=");
        }

        @Test
        @DisplayName("refresh 쿠키가 없으면 sso=none으로 리다이렉트한다")
        void ssoAuthorize_withoutCookie_redirectsWithNone() throws Exception {
            mockMvc.perform(get(AUTHORIZE_PATH).param("redirect_uri", ALLOWED_REDIRECT + "/"))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", ALLOWED_REDIRECT + "/?sso=none"));
        }

        @Test
        @DisplayName("무효한 refresh 쿠키면 sso=none으로 리다이렉트한다")
        void ssoAuthorize_withInvalidCookie_redirectsWithNone() throws Exception {
            mockMvc.perform(get(AUTHORIZE_PATH)
                            .param("redirect_uri", ALLOWED_REDIRECT + "/")
                            .cookie(new Cookie("refreshToken", "invalid-token")))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", ALLOWED_REDIRECT + "/?sso=none"));
        }

        @Test
        @DisplayName("허용 목록에 없는 redirect_uri면 400을 반환한다")
        void ssoAuthorize_withDisallowedRedirectUri_returns400() throws Exception {
            mockMvc.perform(get(AUTHORIZE_PATH).param("redirect_uri", "https://evil.example/"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("기존 쿼리가 있는 redirect_uri에는 &로 코드를 붙인다")
        void ssoAuthorize_withQueryInRedirectUri_appendsWithAmpersand() throws Exception {
            User user = createActiveUser();
            String refreshToken = createAndSaveRefreshToken(user);

            MvcResult result = mockMvc.perform(get(AUTHORIZE_PATH)
                            .param("redirect_uri", ALLOWED_REDIRECT + "/?page=2")
                            .cookie(new Cookie("refreshToken", refreshToken)))
                    .andExpect(status().isFound())
                    .andReturn();

            assertThat(result.getResponse().getHeader("Location"))
                    .startsWith(ALLOWED_REDIRECT + "/?page=2&sso_code=");
        }
    }

    @Nested
    @DisplayName("token - 코드 교환")
    class TokenExchangeTest {

        private String issueCodeViaAuthorize(String refreshToken) throws Exception {
            MvcResult result = mockMvc.perform(get(AUTHORIZE_PATH)
                            .param("redirect_uri", ALLOWED_REDIRECT + "/")
                            .cookie(new Cookie("refreshToken", refreshToken)))
                    .andExpect(status().isFound())
                    .andReturn();
            return extractQueryParam(result.getResponse().getHeader("Location"), "sso_code");
        }

        @Test
        @DisplayName("유효한 코드로 액세스 토큰과 새 리프레시 토큰을 발급받는다")
        void ssoIssueToken_withValidCode_returnsTokens() throws Exception {
            User user = createActiveUser();
            String code = issueCodeViaAuthorize(createAndSaveRefreshToken(user));

            MvcResult result = mockMvc.perform(post(TOKEN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("code", code))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.expiresIn").value(ACCESS_TOKEN_VALIDITY))
                    .andExpect(jsonPath("$.refreshExpiresIn").value(REFRESH_TOKEN_VALIDITY))
                    .andExpect(matchesOpenApiSpec())
                    .andReturn();

            // 발급된 리프레시 토큰이 새 패밀리로 저장되었는지 확인
            String newRefreshToken = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("refreshToken").asText();
            assertThat(refreshTokenRepository.findByToken(newRefreshToken)).isPresent();
        }

        @Test
        @DisplayName("같은 코드는 두 번 사용할 수 없다")
        void ssoIssueToken_withReusedCode_returns401() throws Exception {
            User user = createActiveUser();
            String code = issueCodeViaAuthorize(createAndSaveRefreshToken(user));

            mockMvc.perform(post(TOKEN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("code", code))))
                    .andExpect(status().isOk());

            mockMvc.perform(post(TOKEN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("code", code))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("만료된 코드는 401을 반환한다")
        void ssoIssueToken_withExpiredCode_returns401() throws Exception {
            User user = createActiveUser();
            Instant issuedAt = Instant.now();
            when(clock.instant()).thenReturn(issuedAt);
            String code = issueCodeViaAuthorize(createAndSaveRefreshToken(user));

            when(clock.instant()).thenReturn(issuedAt.plus(SsoCodeStore.CODE_TTL).plusSeconds(1));

            mockMvc.perform(post(TOKEN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("code", code))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("존재하지 않는 코드는 401을 반환한다")
        void ssoIssueToken_withUnknownCode_returns401() throws Exception {
            mockMvc.perform(post(TOKEN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("code", "unknown-code"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("정지된 사용자의 코드는 401을 반환한다")
        void ssoIssueToken_withSuspendedUser_returns401() throws Exception {
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER, UserStatus.SUSPENDED);
            String code = ssoCodeStore.issue(user.getId());

            mockMvc.perform(post(TOKEN_PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("code", code))))
                    .andExpect(status().isUnauthorized());
        }
    }
}
