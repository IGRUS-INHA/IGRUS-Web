package igrus.web.security.auth.common.controller;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PrivacyConsentController 통합 테스트.
 *
 * <p>개인정보 동의 API의 인증/인가 및 정상 동작을 검증합니다.</p>
 *
 * <p>테스트 케이스:</p>
 * <ul>
 *     <li>비인증 사용자 접근 차단 (401)</li>
 *     <li>인증된 사용자 동의 이력 조회 허용 (200)</li>
 *     <li>인증된 사용자 최신 동의 조회 허용 (200/404)</li>
 *     <li>인증된 사용자 동의 여부 확인 허용 (200)</li>
 *     <li>인증된 사용자 재동의 필요 여부 확인 허용 (200)</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("PrivacyConsentController 통합 테스트")
class PrivacyConsentControllerIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private User associateUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
            return null;
        });
    }

    private RequestPostProcessor withAuth(User user) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getStudentId(),
                user.getRole().name()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        return authentication(auth);
    }

    private void createTestConsent(User user, String policyVersion) {
        transactionTemplate.execute(status -> {
            PrivacyConsent consent = PrivacyConsent.create(user, policyVersion);
            privacyConsentRepository.save(consent);
            return null;
        });
    }

    // ==================== 비인증 사용자 (401) ====================

    @Nested
    @DisplayName("비인증 사용자 접근 차단 (401)")
    class UnauthenticatedAccessTest {

        @Test
        @DisplayName("비인증 사용자 동의 이력 조회 -> 401")
        void getConsentHistory_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/privacy/consent/history"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비인증 사용자 최신 동의 조회 -> 401")
        void getLatestConsent_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/privacy/consent/latest"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비인증 사용자 동의 여부 확인 -> 401")
        void checkHasConsent_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/privacy/consent/check"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비인증 사용자 버전별 동의 여부 확인 -> 401")
        void checkConsentedToVersion_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/privacy/consent/check-version")
                            .param("version", "v1.0"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비인증 사용자 재동의 필요 여부 확인 -> 401")
        void checkNeedsReConsent_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/privacy/consent/needs-reconsent")
                            .param("currentVersion", "v1.0"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 인증된 사용자 접근 허용 ====================

    @Nested
    @DisplayName("인증된 사용자 접근 허용")
    class AuthenticatedUserAccessTest {

        @Test
        @DisplayName("인증된 사용자 동의 이력 조회 -> 200")
        void getConsentHistory_Authenticated_Returns200() throws Exception {
            createTestConsent(associateUser, "v1.0");

            mockMvc.perform(get("/api/v1/privacy/consent/history")
                            .with(withAuth(associateUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(associateUser.getId()))
                    .andExpect(jsonPath("$.totalCount").value(1));
        }

        @Test
        @DisplayName("인증된 사용자 최신 동의 조회 (동의 있음) -> 200")
        void getLatestConsent_WithConsent_Returns200() throws Exception {
            createTestConsent(associateUser, "v1.0");

            mockMvc.perform(get("/api/v1/privacy/consent/latest")
                            .with(withAuth(associateUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.policyVersion").value("v1.0"))
                    .andExpect(jsonPath("$.consentGiven").value(true));
        }

        @Test
        @DisplayName("인증된 사용자 최신 동의 조회 (동의 없음) -> 404")
        void getLatestConsent_WithoutConsent_Returns404() throws Exception {
            mockMvc.perform(get("/api/v1/privacy/consent/latest")
                            .with(withAuth(associateUser)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("인증된 사용자 동의 여부 확인 -> 200")
        void checkHasConsent_Authenticated_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/privacy/consent/check")
                            .with(withAuth(associateUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").exists());
        }

        @Test
        @DisplayName("인증된 사용자 재동의 필요 여부 확인 -> 200")
        void checkNeedsReConsent_Authenticated_Returns200() throws Exception {
            createTestConsent(associateUser, "v1.0");

            mockMvc.perform(get("/api/v1/privacy/consent/needs-reconsent")
                            .param("currentVersion", "v1.0")
                            .with(withAuth(associateUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.result").exists());
        }
    }
}
