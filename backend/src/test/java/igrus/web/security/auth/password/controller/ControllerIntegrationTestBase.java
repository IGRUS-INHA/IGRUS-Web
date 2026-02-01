package igrus.web.security.auth.password.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.service.AuthEmailService;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.service.PasswordAuthService;
import igrus.web.security.auth.password.service.PasswordSignupService;
import igrus.web.security.jwt.JwtTokenProvider;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * 컨트롤러 통합 테스트를 위한 기반 클래스.
 *
 * <p>ServiceIntegrationTestBase를 확장하여 MockMvc 지원을 추가합니다.</p>
 *
 * <p>주요 기능:
 * <ul>
 *     <li>MockMvc를 통한 HTTP 요청/응답 테스트</li>
 *     <li>JSON 직렬화/역직렬화 지원</li>
 *     <li>ServiceIntegrationTestBase의 DB 클린업, 헬퍼 메서드 재사용</li>
 *     <li>HTTP 요청 헬퍼 메서드 제공</li>
 * </ul>
 * </p>
 */
@AutoConfigureMockMvc
public abstract class ControllerIntegrationTestBase extends ServiceIntegrationTestBase {

    protected static final String API_BASE_PATH = "/api/v1/auth/password";

    protected static final long ACCESS_TOKEN_VALIDITY = 3600000L; // 1시간
    protected static final long REFRESH_TOKEN_VALIDITY = 604800000L; // 7일
    protected static final long VERIFICATION_CODE_EXPIRY = 600000L; // 10분
    protected static final int MAX_VERIFICATION_ATTEMPTS = 5;
    protected static final long RESEND_RATE_LIMIT_SECONDS = 60L;

    protected static final String TEST_STUDENT_ID = "12345678";
    protected static final String TEST_PASSWORD = "TestPass1!@";
    protected static final String TEST_NAME = "홍길동";
    protected static final String TEST_EMAIL = "test@inha.edu";
    protected static final String TEST_PHONE = "010-1234-5678";
    protected static final String TEST_DEPARTMENT = "컴퓨터공학과";
    protected static final String TEST_MOTIVATION = "동아리 활동을 열심히 하고 싶습니다.";
    protected static final String TEST_IP_ADDRESS = "192.168.1.100";
    protected static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    protected PasswordAuthService passwordAuthService;

    @Autowired
    protected PasswordSignupService passwordSignupService;

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    protected AuthEmailService authEmailService;

    /**
     * 컨트롤러 테스트를 위한 초기화를 수행합니다.
     * 서브클래스에서 @BeforeEach 메서드에서 호출해야 합니다.
     */
    protected void setUpControllerTest() {
        setUpBase();
        configureServiceProperties();
    }

    /**
     * 서비스 속성을 설정합니다.
     */
    protected void configureServiceProperties() {
        ReflectionTestUtils.setField(passwordAuthService, "accessTokenValidity", ACCESS_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(passwordAuthService, "refreshTokenValidity", REFRESH_TOKEN_VALIDITY);
        ReflectionTestUtils.setField(passwordSignupService, "verificationCodeExpiry", VERIFICATION_CODE_EXPIRY);
        ReflectionTestUtils.setField(passwordSignupService, "maxAttempts", MAX_VERIFICATION_ATTEMPTS);
        ReflectionTestUtils.setField(passwordSignupService, "resendRateLimitSeconds", RESEND_RATE_LIMIT_SECONDS);
    }

    // ==================== HTTP Request Helper Methods ====================

    /**
     * POST 요청을 수행합니다.
     */
    protected ResultActions performPost(String endpoint, Object requestBody) throws Exception {
        return mockMvc.perform(post(API_BASE_PATH + endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)));
    }

    /**
     * GET 요청을 수행합니다.
     */
    protected ResultActions performGet(String endpoint) throws Exception {
        return mockMvc.perform(get(API_BASE_PATH + endpoint));
    }

    /**
     * GET 요청에 파라미터를 추가하여 수행합니다.
     */
    protected ResultActions performGetWithParam(String endpoint, String paramName, String paramValue) throws Exception {
        return mockMvc.perform(get(API_BASE_PATH + endpoint)
                .param(paramName, paramValue));
    }

    /**
     * Authorization Bearer 토큰을 포함한 GET 요청을 수행합니다.
     */
    protected ResultActions performGetWithAuth(String endpoint, String accessToken) throws Exception {
        return mockMvc.perform(get(API_BASE_PATH + endpoint)
                .header("Authorization", "Bearer " + accessToken));
    }

    /**
     * Authorization Bearer 토큰을 포함한 POST 요청을 수행합니다.
     */
    protected ResultActions performPostWithAuth(String endpoint, Object requestBody, String accessToken) throws Exception {
        return mockMvc.perform(post(API_BASE_PATH + endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody))
                .header("Authorization", "Bearer " + accessToken));
    }

    // ==================== Test Data Creation Helper Methods ====================

    /**
     * 테스트용 사용자를 생성하고 저장합니다 (특정 상태로).
     */
    protected User createAndSaveTestUser(String studentId, String email, UserRole role, UserStatus status) {
        User user = User.create(
                studentId,
                TEST_NAME,
                email,
                "010-" + studentId,
                TEST_DEPARTMENT,
                TEST_MOTIVATION,
                Gender.MALE,
                1
        );
        user.changeRole(role);
        applyUserStatus(user, status);
        return userRepository.save(user);
    }

    /**
     * 테스트용 사용자와 비밀번호 자격증명을 생성하고 저장합니다.
     */
    protected User createAndSaveUserWithCredential(String studentId, String email, String password,
                                                    UserRole role, UserStatus status) {
        User user = createAndSaveTestUser(studentId, email, role, status);
        createAndSaveCredential(user, password, status);
        return user;
    }

    /**
     * 기본 테스트 데이터로 사용자와 자격증명을 생성합니다.
     */
    protected User createAndSaveDefaultUserWithCredential() {
        return createAndSaveUserWithCredential(
                TEST_STUDENT_ID, TEST_EMAIL, TEST_PASSWORD,
                UserRole.ASSOCIATE, UserStatus.ACTIVE
        );
    }

    /**
     * 비밀번호 자격증명을 생성하고 저장합니다.
     */
    protected PasswordCredential createAndSaveCredential(User user, String password, UserStatus status) {
        String encodedPassword = passwordEncoder.encode(password);
        PasswordCredential credential = PasswordCredential.create(user, encodedPassword);
        applyCredentialStatus(credential, status);
        return passwordCredentialRepository.save(credential);
    }

    /**
     * 사용자 상태를 적용합니다.
     */
    private void applyUserStatus(User user, UserStatus status) {
        switch (status) {
            case ACTIVE -> user.verifyEmail();
            case SUSPENDED -> {
                user.verifyEmail();
                user.suspend();
            }
            case WITHDRAWN -> {
                user.verifyEmail();
                user.withdraw();
            }
            case PENDING_VERIFICATION -> {
                // 기본 상태로 유지
            }
        }
    }

    /**
     * 자격증명 상태를 적용합니다.
     */
    private void applyCredentialStatus(PasswordCredential credential, UserStatus status) {
        switch (status) {
            case ACTIVE -> credential.verifyEmail();
            case SUSPENDED -> {
                credential.verifyEmail();
                credential.suspend();
            }
            case WITHDRAWN -> {
                credential.verifyEmail();
                credential.withdraw();
            }
            case PENDING_VERIFICATION -> {
                // 기본 상태로 유지
            }
        }
    }

    /**
     * 이메일 인증 코드를 조회합니다.
     */
    protected String getVerificationCode(String email) {
        EmailVerification verification = emailVerificationRepository.findByEmailAndVerifiedFalse(email)
                .orElseThrow(() -> new IllegalStateException("Verification not found for email: " + email));
        return verification.getCode();
    }
}
