package igrus.web.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.password.domain.PasswordCredential;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("JwtAuthenticationFilter 계정 상태 검증 통합 테스트")
@AutoConfigureMockMvc
class JwtAuthenticationFilterAccountStatusTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Admin 엔드포인트 사용 (ADMIN 권한 필요, 인증 필수)
    private static final String PROTECTED_ENDPOINT = "/api/v1/admin/members/pending";
    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_PASSWORD = "TestPass1!@";

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Nested
    @DisplayName("계정 상태별 접근 제어")
    class AccountStatusAccessControlTest {

        @Test
        @DisplayName("ACTIVE 계정 + 유효 토큰 - 200, 요청 정상 처리")
        void activeAccount_WithValidToken_Returns200() throws Exception {
            // given
            User user = createAndSaveUserWithStatus(UserStatus.ACTIVE);
            String accessToken = jwtTokenProvider.createAccessToken(
                    user.getId(), user.getStudentId(), user.getRole().name());

            // when & then
            mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("SUSPENDED 계정 + 유효 토큰 - 403, '정지된 계정입니다' 메시지")
        void suspendedAccount_WithValidToken_Returns403() throws Exception {
            // given
            User user = createAndSaveUserWithStatus(UserStatus.SUSPENDED);
            String accessToken = jwtTokenProvider.createAccessToken(
                    user.getId(), user.getStudentId(), user.getRole().name());

            // when & then
            mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"))
                    .andExpect(jsonPath("$.message").value("정지된 계정입니다"));
        }

        @Test
        @DisplayName("WITHDRAWN 계정 + 유효 토큰 - 403, '탈퇴한 계정입니다' 메시지")
        void withdrawnAccount_WithValidToken_Returns403() throws Exception {
            // given
            User user = createAndSaveUserWithStatus(UserStatus.WITHDRAWN);
            String accessToken = jwtTokenProvider.createAccessToken(
                    user.getId(), user.getStudentId(), user.getRole().name());

            // when & then
            mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCOUNT_WITHDRAWN"))
                    .andExpect(jsonPath("$.message").value("탈퇴한 계정입니다"));
        }

        @Test
        @DisplayName("PENDING_VERIFICATION 계정 + 유효 토큰 - 401, '이메일 인증이 완료되지 않았습니다' 메시지")
        void pendingVerificationAccount_WithValidToken_Returns401() throws Exception {
            // given
            User user = createAndSaveUserWithStatus(UserStatus.PENDING_VERIFICATION);
            String accessToken = jwtTokenProvider.createAccessToken(
                    user.getId(), user.getStudentId(), user.getRole().name());

            // when & then
            mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"))
                    .andExpect(jsonPath("$.message").value("이메일 인증이 완료되지 않았습니다"));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 토큰 생성 - 404")
        void nonExistentUser_WithValidToken_Returns404() throws Exception {
            // given - 존재하지 않는 userId로 토큰 생성
            Long nonExistentUserId = 99999L;
            String accessToken = jwtTokenProvider.createAccessToken(
                    nonExistentUserId, TEST_STUDENT_ID, "ASSOCIATE");

            // when & then
            mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("토큰 발급 후 계정 상태 변경 시나리오")
    class StatusChangeAfterTokenIssuedTest {

        @Test
        @DisplayName("토큰 발급 후 계정 정지 - 다음 요청에서 403 반환")
        void tokenIssuedThenAccountSuspended_NextRequestReturns403() throws Exception {
            // given - 정상 계정으로 토큰 발급
            User user = createAndSaveUserWithStatus(UserStatus.ACTIVE);
            String accessToken = jwtTokenProvider.createAccessToken(
                    user.getId(), user.getStudentId(), user.getRole().name());

            // 첫 번째 요청 - 정상 처리
            mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // 계정 상태를 SUSPENDED로 변경
            transactionTemplate.execute(status -> {
                User foundUser = userRepository.findById(user.getId()).orElseThrow();
                foundUser.suspend();
                return null;
            });

            // when & then - 같은 토큰으로 다시 요청하면 403 반환
            mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCOUNT_SUSPENDED"));
        }

        @Test
        @DisplayName("토큰 발급 후 계정 탈퇴 - 다음 요청에서 403 반환")
        void tokenIssuedThenAccountWithdrawn_NextRequestReturns403() throws Exception {
            // given - 정상 계정으로 토큰 발급
            User user = createAndSaveUserWithStatus(UserStatus.ACTIVE);
            String accessToken = jwtTokenProvider.createAccessToken(
                    user.getId(), user.getStudentId(), user.getRole().name());

            // 첫 번째 요청 - 정상 처리
            mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());

            // 계정 상태를 WITHDRAWN으로 변경
            transactionTemplate.execute(status -> {
                User foundUser = userRepository.findById(user.getId()).orElseThrow();
                foundUser.withdraw();
                return null;
            });

            // when & then - 같은 토큰으로 다시 요청하면 403 반환
            mockMvc.perform(get(PROTECTED_ENDPOINT)
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCOUNT_WITHDRAWN"));
        }
    }

    /**
     * 특정 상태의 테스트용 ADMIN 사용자를 생성하고 저장합니다.
     */
    private User createAndSaveUserWithStatus(UserStatus status) {
        User user = User.create(
                TEST_STUDENT_ID,
                "테스트유저",
                "test@inha.edu",
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기"
        );
        user.changeRole(UserRole.ADMIN);  // ADMIN 권한으로 설정
        applyUserStatus(user, status);

        User savedUser = transactionTemplate.execute(txStatus -> {
            User saved = userRepository.save(user);
            // PasswordCredential도 생성해야 /me 엔드포인트 접근 가능
            PasswordCredential credential = PasswordCredential.create(saved, passwordEncoder.encode(TEST_PASSWORD));
            applyCredentialStatus(credential, status);
            passwordCredentialRepository.save(credential);
            return saved;
        });

        return savedUser;
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
}
