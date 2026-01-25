package igrus.web.security.auth.password.controller;

import tools.jackson.databind.json.JsonMapper;
import igrus.web.common.exception.ErrorCode;
import igrus.web.common.exception.GlobalExceptionHandler;
import igrus.web.security.auth.common.exception.token.RefreshTokenExpiredException;
import igrus.web.security.auth.common.exception.token.RefreshTokenInvalidException;
import igrus.web.security.auth.common.service.AccountRecoveryService;
import igrus.web.security.auth.common.service.AccountStatusService;
import igrus.web.security.auth.password.dto.request.TokenRefreshRequest;
import igrus.web.security.auth.password.dto.response.TokenRefreshResponse;
import igrus.web.security.auth.password.service.PasswordAuthService;
import igrus.web.security.auth.password.service.PasswordResetService;
import igrus.web.security.auth.password.service.PasswordSignupService;
import igrus.web.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
@DisplayName("PasswordAuthController 토큰 갱신 테스트")
class PasswordAuthControllerTokenTest {

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @MockitoBean
    private PasswordAuthService passwordAuthService;

    @MockitoBean
    private PasswordSignupService passwordSignupService;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AccountRecoveryService accountRecoveryService;

    @MockitoBean
    private AccountStatusService accountStatusService;

    private static final String VALID_REFRESH_TOKEN = "valid.refresh.token";
    private static final String EXPIRED_REFRESH_TOKEN = "expired.refresh.token";
    private static final String INVALID_REFRESH_TOKEN = "invalid.refresh.token";
    private static final String FORGED_REFRESH_TOKEN = "forged.malicious.token";
    private static final String REVOKED_REFRESH_TOKEN = "revoked.logout.token";
    private static final long EXPIRES_IN = 3600000L; // 1 hour

    @Nested
    @DisplayName("토큰 갱신 성공")
    class TokenRefreshSuccessTest {

        @Test
        @DisplayName("유효한 Refresh Token으로 갱신 시 새 Access Token 반환 [TKN-001]")
        void refreshToken_withValidToken_returns200() throws Exception {
            // given
            TokenRefreshRequest request = new TokenRefreshRequest(VALID_REFRESH_TOKEN);
            TokenRefreshResponse response = TokenRefreshResponse.of("new.access.token", EXPIRES_IN);
            given(passwordAuthService.refreshToken(any(TokenRefreshRequest.class)))
                .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new.access.token"))
                .andExpect(jsonPath("$.expiresIn").value(EXPIRES_IN));
        }

        @Test
        @DisplayName("새 Access Token 유효기간 확인 [TKN-002]")
        void refreshToken_withValidToken_returnsExpiresInGreaterThanZero() throws Exception {
            // given
            TokenRefreshRequest request = new TokenRefreshRequest(VALID_REFRESH_TOKEN);
            TokenRefreshResponse response = TokenRefreshResponse.of("new.access.token", EXPIRES_IN);
            given(passwordAuthService.refreshToken(any(TokenRefreshRequest.class)))
                .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.expiresIn").value(org.hamcrest.Matchers.greaterThan(0)));
        }

        @Test
        @DisplayName("갱신된 토큰으로 API 호출 가능 [TKN-003]")
        void refreshToken_withValidToken_returnsValidAccessToken() throws Exception {
            // given
            String newAccessToken = "valid.new.access.token.for.api.calls";
            TokenRefreshRequest request = new TokenRefreshRequest(VALID_REFRESH_TOKEN);
            TokenRefreshResponse response = TokenRefreshResponse.of(newAccessToken, EXPIRES_IN);
            given(passwordAuthService.refreshToken(any(TokenRefreshRequest.class)))
                .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(newAccessToken))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
        }
    }

    @Nested
    @DisplayName("토큰 갱신 실패")
    class TokenRefreshFailureTest {

        @Test
        @DisplayName("만료된 Refresh Token으로 갱신 시도 시 401 반환 [TKN-010]")
        void refreshToken_withExpiredToken_returns401() throws Exception {
            // given
            TokenRefreshRequest request = new TokenRefreshRequest(EXPIRED_REFRESH_TOKEN);
            given(passwordAuthService.refreshToken(any(TokenRefreshRequest.class)))
                .willThrow(new RefreshTokenExpiredException());

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_EXPIRED.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.REFRESH_TOKEN_EXPIRED.getMessage()));
        }

        @Test
        @DisplayName("유효하지 않은 Refresh Token으로 갱신 시도 시 401 반환 [TKN-011]")
        void refreshToken_withInvalidToken_returns401() throws Exception {
            // given
            TokenRefreshRequest request = new TokenRefreshRequest(INVALID_REFRESH_TOKEN);
            given(passwordAuthService.refreshToken(any(TokenRefreshRequest.class)))
                .willThrow(new RefreshTokenInvalidException());

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.REFRESH_TOKEN_INVALID.getMessage()));
        }

        @Test
        @DisplayName("위조된 Refresh Token으로 갱신 시도 시 401 반환 [TKN-012]")
        void refreshToken_withForgedToken_returns401() throws Exception {
            // given
            TokenRefreshRequest request = new TokenRefreshRequest(FORGED_REFRESH_TOKEN);
            given(passwordAuthService.refreshToken(any(TokenRefreshRequest.class)))
                .willThrow(new RefreshTokenInvalidException());

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.REFRESH_TOKEN_INVALID.getMessage()));
        }

        @Test
        @DisplayName("빈 Refresh Token으로 갱신 시도 시 400 반환 [TKN-013]")
        void refreshToken_withEmptyToken_returns400() throws Exception {
            // given
            TokenRefreshRequest request = new TokenRefreshRequest("");

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
        @DisplayName("null Refresh Token으로 갱신 시도 시 400 반환 [TKN-013-2]")
        void refreshToken_withNullToken_returns400() throws Exception {
            // given - null token is serialized as {"refreshToken":null}
            String requestBody = "{\"refreshToken\":null}";

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
        @DisplayName("공백만 있는 Refresh Token으로 갱신 시도 시 400 반환 [TKN-013-3]")
        void refreshToken_withBlankToken_returns400() throws Exception {
            // given
            TokenRefreshRequest request = new TokenRefreshRequest("   ");

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
        @DisplayName("로그아웃된 (무효화된) Refresh Token으로 갱신 시도 시 401 반환 [TKN-014]")
        void refreshToken_withRevokedToken_returns401() throws Exception {
            // given
            TokenRefreshRequest request = new TokenRefreshRequest(REVOKED_REFRESH_TOKEN);
            given(passwordAuthService.refreshToken(any(TokenRefreshRequest.class)))
                .willThrow(new RefreshTokenInvalidException());

            // when & then
            mockMvc.perform(post("/api/v1/auth/password/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.REFRESH_TOKEN_INVALID.getCode()))
                .andExpect(jsonPath("$.message").value(ErrorCode.REFRESH_TOKEN_INVALID.getMessage()));
        }
    }
}
