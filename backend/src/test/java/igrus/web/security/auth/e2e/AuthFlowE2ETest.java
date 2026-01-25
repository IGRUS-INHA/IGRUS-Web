package igrus.web.security.auth.e2e;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.dto.request.PasswordLoginRequest;
import igrus.web.security.auth.password.dto.request.PasswordLogoutRequest;
import igrus.web.security.auth.password.dto.request.PasswordSignupRequest;
import igrus.web.security.auth.password.dto.request.TokenRefreshRequest;
import igrus.web.security.auth.password.service.PasswordAuthService;
import igrus.web.security.auth.password.service.PasswordSignupService;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 인증 플로우 HTTP E2E 테스트 (T066)
 *
 * <p>HTTP 레벨에서 전체 인증 플로우를 시나리오 기반으로 테스트합니다.</p>
 *
 * <p>테스트 시나리오:</p>
 * <ul>
 *     <li>시나리오 1: 완전한 회원가입 → 이메일 인증 → 로그인 플로우</li>
 *     <li>시나리오 2: 토큰 갱신 플로우</li>
 *     <li>시나리오 3: 로그아웃 플로우</li>
 *     <li>시나리오 4: 다중 디바이스 세션 관리</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("인증 플로우 HTTP E2E 테스트")
class AuthFlowE2ETest extends ServiceIntegrationTestBase {

    private static final String API_BASE_PATH = "/api/v1/auth/password";

    private static final long ACCESS_TOKEN_VALIDITY = 3600000L; // 1시간
    private static final long REFRESH_TOKEN_VALIDITY = 604800000L; // 7일
    private static final long VERIFICATION_CODE_EXPIRY = 600000L; // 10분

    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_NAME = "홍길동";
    private static final String TEST_EMAIL = "test@inha.edu";
    private static final String TEST_PASSWORD = "TestPass1!@";
    private static final String TEST_PHONE = "010-1234-5678";
    private static final String TEST_DEPARTMENT = "컴퓨터공학과";
    private static final String TEST_MOTIVATION = "동아리 활동을 열심히 하고 싶습니다.";

    @Autowired
    private MockMvc mockMvc;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Autowired
    private PasswordAuthService passwordAuthService;

    @Autowired
    private PasswordSignupService passwordSignupService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private AuthEmailService authEmailService;

    @BeforeEach
    void setUp() {
        setUpBase();
        ReflectionTestUtils.setField(passwordAuthService, "accessTokenValidity", ACCESS_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(passwordAuthService, "refreshTokenValidity", REFRESH_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(passwordSignupService, "verificationCodeExpiry", VERIFICATION_CODE_EXPIRY);
        ReflectionTestUtils.setField(passwordSignupService, "maxAttempts", 5);
        ReflectionTestUtils.setField(passwordSignupService, "resendRateLimitSeconds", 0L); // 테스트용으로 비활성화
    }

    // ===== 시나리오 1: 완전한 회원가입 → 이메일 인증 → 로그인 플로우 =====

    @Nested
    @DisplayName("[시나리오 1] 완전한 회원가입 → 이메일 인증 → 로그인 플로우")
    class CompleteSignupFlowTest {

        @Test
        @DisplayName("E2E-HTTP-001: 회원가입부터 로그인까지 전체 HTTP 플로우")
        void fullSignupToLoginFlow_viaHttp() throws Exception {
            // === Step 1: POST /signup → 201 Created, 인증 코드 발송 ===
            PasswordSignupRequest signupRequest = new PasswordSignupRequest(
                    TEST_STUDENT_ID, TEST_NAME, TEST_EMAIL, TEST_PASSWORD,
                    TEST_PHONE, TEST_DEPARTMENT, TEST_MOTIVATION, true
            );

            mockMvc.perform(post(API_BASE_PATH + "/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value(TEST_EMAIL))
                    .andExpect(jsonPath("$.requiresVerification").value(true));

            // 이메일 발송 확인
            verify(authEmailService).sendVerificationEmail(eq(TEST_EMAIL), anyString());

            // 사용자 상태 확인 (PENDING_VERIFICATION)
            User pendingUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(pendingUser.getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);

            // === Step 2: POST /verify-email → 200 OK, 준회원 등록 완료 ===
            EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(TEST_EMAIL).orElseThrow();
            EmailVerificationRequest verifyRequest = new EmailVerificationRequest(TEST_EMAIL, verification.getCode());

            mockMvc.perform(post(API_BASE_PATH + "/verify-email")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(verifyRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.requiresVerification").value(false));

            // 사용자 상태 확인 (ACTIVE)
            User activeUser = userRepository.findByEmail(TEST_EMAIL).orElseThrow();
            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(activeUser.getRole()).isEqualTo(UserRole.ASSOCIATE);

            // === Step 3: POST /login → 200 OK, 토큰 발급 ===
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            MvcResult loginResult = mockMvc.perform(post(API_BASE_PATH + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.userId").value(activeUser.getId()))
                    .andExpect(jsonPath("$.studentId").value(TEST_STUDENT_ID))
                    .andExpect(jsonPath("$.name").value(TEST_NAME))
                    .andExpect(jsonPath("$.role").value("ASSOCIATE"))
                    .andReturn();

            // === Step 4: Access Token 검증 ===
            JsonNode responseJson = jsonMapper.readTree(loginResult.getResponse().getContentAsString());
            String accessToken = responseJson.get("accessToken").asText();

            var claims = jwtTokenProvider.validateAccessTokenAndGetClaims(accessToken);
            assertThat(jwtTokenProvider.getUserIdFromClaims(claims)).isEqualTo(activeUser.getId());
            assertThat(jwtTokenProvider.getStudentIdFromClaims(claims)).isEqualTo(TEST_STUDENT_ID);
        }
    }

    // ===== 시나리오 2: 토큰 갱신 플로우 =====

    @Nested
    @DisplayName("[시나리오 2] 토큰 갱신 플로우")
    class TokenRefreshFlowTest {

        @Test
        @DisplayName("E2E-HTTP-002: 로그인 → 토큰 갱신 → 새 토큰으로 검증")
        void tokenRefreshFlow_viaHttp() throws Exception {
            // === Setup: 인증된 사용자 생성 ===
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER);
            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            passwordCredentialRepository.save(credential);

            // === Step 1: POST /login → 토큰 발급 ===
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            MvcResult loginResult = mockMvc.perform(post(API_BASE_PATH + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode loginJson = jsonMapper.readTree(loginResult.getResponse().getContentAsString());
            String originalAccessToken = loginJson.get("accessToken").asText();
            String refreshToken = loginJson.get("refreshToken").asText();

            // === Step 2: POST /refresh → 새 Access Token 발급 ===
            TokenRefreshRequest refreshRequest = new TokenRefreshRequest(refreshToken);

            MvcResult refreshResult = mockMvc.perform(post(API_BASE_PATH + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.expiresIn").value(ACCESS_TOKEN_VALIDITY))
                    .andReturn();

            JsonNode refreshJson = jsonMapper.readTree(refreshResult.getResponse().getContentAsString());
            String newAccessToken = refreshJson.get("accessToken").asText();

            // === Step 3: 새 Access Token이 다른 것 확인 ===
            assertThat(newAccessToken).isNotEqualTo(originalAccessToken);

            // === Step 4: 새 Access Token으로 사용자 정보 검증 ===
            var claims = jwtTokenProvider.validateAccessTokenAndGetClaims(newAccessToken);
            assertThat(jwtTokenProvider.getUserIdFromClaims(claims)).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("E2E-HTTP-003: 여러 번 토큰 갱신해도 항상 새로운 토큰 발급")
        void multipleTokenRefreshes_viaHttp() throws Exception {
            // === Setup ===
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER);
            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            passwordCredentialRepository.save(credential);

            // === Login ===
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            MvcResult loginResult = mockMvc.perform(post(API_BASE_PATH + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode loginJson = jsonMapper.readTree(loginResult.getResponse().getContentAsString());
            String refreshToken = loginJson.get("refreshToken").asText();

            // === 여러 번 갱신 ===
            TokenRefreshRequest refreshRequest = new TokenRefreshRequest(refreshToken);

            MvcResult refresh1 = mockMvc.perform(post(API_BASE_PATH + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            MvcResult refresh2 = mockMvc.perform(post(API_BASE_PATH + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String token1 = jsonMapper.readTree(refresh1.getResponse().getContentAsString()).get("accessToken").asText();
            String token2 = jsonMapper.readTree(refresh2.getResponse().getContentAsString()).get("accessToken").asText();

            // 서로 다른 토큰
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    // ===== 시나리오 3: 로그아웃 플로우 =====

    @Nested
    @DisplayName("[시나리오 3] 로그아웃 플로우")
    class LogoutFlowTest {

        @Test
        @DisplayName("E2E-HTTP-004: 로그인 → 로그아웃 → 이전 토큰으로 갱신 실패")
        void logoutFlow_viaHttp() throws Exception {
            // === Setup ===
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER);
            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            passwordCredentialRepository.save(credential);

            // === Step 1: POST /login → 토큰 발급 ===
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);
            MvcResult loginResult = mockMvc.perform(post(API_BASE_PATH + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode loginJson = jsonMapper.readTree(loginResult.getResponse().getContentAsString());
            String refreshToken = loginJson.get("refreshToken").asText();

            // Refresh Token이 DB에 저장되어 있는지 확인
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)).isPresent();

            // === Step 2: POST /logout → 200 OK ===
            PasswordLogoutRequest logoutRequest = new PasswordLogoutRequest(refreshToken);
            mockMvc.perform(post(API_BASE_PATH + "/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(logoutRequest)))
                    .andExpect(status().isOk());

            // === Step 3: 이전 Refresh Token으로 갱신 시도 → 401 Unauthorized ===
            TokenRefreshRequest refreshRequest = new TokenRefreshRequest(refreshToken);
            mockMvc.perform(post(API_BASE_PATH + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isUnauthorized());

            // 토큰이 무효화되었는지 확인
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)).isEmpty();
        }
    }

    // ===== 시나리오 4: 다중 디바이스 세션 관리 =====

    @Nested
    @DisplayName("[시나리오 4] 다중 디바이스 세션 관리")
    class MultiDeviceSessionTest {

        @Test
        @DisplayName("E2E-HTTP-005: 다중 디바이스 로그인 → 한 기기 로그아웃 → 다른 기기 유지")
        void multiDeviceSession_viaHttp() throws Exception {
            // === Setup ===
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER);
            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            passwordCredentialRepository.save(credential);

            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // === Step 1: Device A 로그인 ===
            MvcResult deviceAResult = mockMvc.perform(post(API_BASE_PATH + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String deviceARefreshToken = jsonMapper.readTree(deviceAResult.getResponse().getContentAsString())
                    .get("refreshToken").asText();

            // === Step 2: Device B 로그인 ===
            MvcResult deviceBResult = mockMvc.perform(post(API_BASE_PATH + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            String deviceBRefreshToken = jsonMapper.readTree(deviceBResult.getResponse().getContentAsString())
                    .get("refreshToken").asText();

            // 서로 다른 토큰인지 확인
            assertThat(deviceARefreshToken).isNotEqualTo(deviceBRefreshToken);

            // === Step 3: Device A 로그아웃 ===
            PasswordLogoutRequest logoutRequest = new PasswordLogoutRequest(deviceARefreshToken);
            mockMvc.perform(post(API_BASE_PATH + "/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(logoutRequest)))
                    .andExpect(status().isOk());

            // === Step 4: Device A 토큰 무효화 확인 ===
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(deviceARefreshToken)).isEmpty();

            // === Step 5: Device B 토큰 유효 확인 ===
            assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(deviceBRefreshToken)).isPresent();

            // Device B로 토큰 갱신 가능
            TokenRefreshRequest refreshRequest = new TokenRefreshRequest(deviceBRefreshToken);
            mockMvc.perform(post(API_BASE_PATH + "/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(refreshRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("E2E-HTTP-006: 동시 로그인 시 사용자 정보 일관성 확인")
        void multiDeviceLogin_userInfoConsistency_viaHttp() throws Exception {
            // === Setup ===
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.OPERATOR);
            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            passwordCredentialRepository.save(credential);

            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            // === 여러 기기에서 로그인 ===
            MvcResult result1 = mockMvc.perform(post(API_BASE_PATH + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            MvcResult result2 = mockMvc.perform(post(API_BASE_PATH + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode json1 = jsonMapper.readTree(result1.getResponse().getContentAsString());
            JsonNode json2 = jsonMapper.readTree(result2.getResponse().getContentAsString());

            // 사용자 정보는 동일해야 함
            assertThat(json1.get("userId").asLong()).isEqualTo(json2.get("userId").asLong());
            assertThat(json1.get("studentId").asText()).isEqualTo(json2.get("studentId").asText());
            assertThat(json1.get("name").asText()).isEqualTo(json2.get("name").asText());
            assertThat(json1.get("role").asText()).isEqualTo(json2.get("role").asText());

            // 토큰은 달라야 함
            assertThat(json1.get("accessToken").asText()).isNotEqualTo(json2.get("accessToken").asText());
            assertThat(json1.get("refreshToken").asText()).isNotEqualTo(json2.get("refreshToken").asText());
        }
    }

    // ===== 시나리오 5: 에러 핸들링 플로우 =====

    @Nested
    @DisplayName("[시나리오 5] 에러 핸들링 플로우")
    class ErrorHandlingFlowTest {

        @Test
        @DisplayName("E2E-HTTP-007: 이메일 미인증 상태에서 로그인 시도 → 401 Unauthorized")
        void loginWithoutEmailVerification_viaHttp() throws Exception {
            // === Setup: 회원가입만 하고 인증 안 함 ===
            PasswordSignupRequest signupRequest = new PasswordSignupRequest(
                    TEST_STUDENT_ID, TEST_NAME, TEST_EMAIL, TEST_PASSWORD,
                    TEST_PHONE, TEST_DEPARTMENT, TEST_MOTIVATION, true
            );

            mockMvc.perform(post(API_BASE_PATH + "/signup")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(signupRequest)))
                    .andExpect(status().isCreated());

            // === 로그인 시도 → 401 Unauthorized (이메일 미인증) ===
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, TEST_PASSWORD);

            mockMvc.perform(post(API_BASE_PATH + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("E2E-HTTP-008: 잘못된 자격 증명으로 로그인 시도 → 401 Unauthorized")
        void loginWithInvalidCredentials_viaHttp() throws Exception {
            // === Setup ===
            User user = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER);
            PasswordCredential credential = PasswordCredential.create(user, passwordEncoder.encode(TEST_PASSWORD));
            credential.verifyEmail();
            passwordCredentialRepository.save(credential);

            // === 잘못된 비밀번호로 로그인 ===
            PasswordLoginRequest loginRequest = new PasswordLoginRequest(TEST_STUDENT_ID, "WrongPassword1!@");

            mockMvc.perform(post(API_BASE_PATH + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }
}
