package igrus.web.security.auth.common.controller;

import igrus.web.common.OpenApiValidatorUtil;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.security.auth.common.domain.LoginFailureReason;
import igrus.web.security.auth.common.domain.LoginHistory;
import igrus.web.security.auth.common.service.login.RecordLoginFailureService;
import igrus.web.security.auth.common.service.login.RecordLoginSuccessService;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("AdminLoginHistoryController 통합 테스트")
class AdminLoginHistoryControllerTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RecordLoginSuccessService recordLoginSuccessService;

    @Autowired
    private RecordLoginFailureService recordLoginFailureService;

    private static final String BASE_URL = "/api/v1/admin/login-histories";
    private static final String TEST_IP = "192.168.1.100";
    private static final String TEST_IP_2 = "10.0.0.1";
    private static final String TEST_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

    private User adminUser;
    private User memberUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
        memberUser = createAndSaveUser("20220001", "member@inha.edu", UserRole.MEMBER);
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

    private void createLoginSuccess(User user, String studentId, String ip) {
        transactionTemplate.execute(status -> {
            recordLoginSuccessService.recordSuccess(user, studentId, ip, TEST_USER_AGENT);
            return null;
        });
    }

    private void createLoginFailure(String studentId, String ip, LoginFailureReason reason) {
        transactionTemplate.execute(status -> {
            recordLoginFailureService.recordFailure(studentId, ip, TEST_USER_AGENT, reason);
            return null;
        });
    }

    private void createLoginFailureWithUser(User user, String studentId, String ip, LoginFailureReason reason) {
        transactionTemplate.execute(status -> {
            recordLoginFailureService.recordFailure(user, studentId, ip, TEST_USER_AGENT, reason);
            return null;
        });
    }

    @Nested
    @DisplayName("로그인 이력 조회")
    class GetLoginHistoriesTest {

        @Test
        @DisplayName("ADMIN 권한으로 전체 로그인 이력 조회 성공")
        void getLoginHistories_WithAdminRole_ReturnsOk() throws Exception {
            // given
            createLoginSuccess(adminUser, "20200001", TEST_IP);
            createLoginFailure("20220001", TEST_IP, LoginFailureReason.INVALID_CREDENTIALS);

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(2))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        @Test
        @DisplayName("studentId 필터로 조회")
        void getLoginHistories_FilterByStudentId_ReturnsFiltered() throws Exception {
            // given
            createLoginSuccess(adminUser, "20200001", TEST_IP);
            createLoginFailure("20220001", TEST_IP, LoginFailureReason.INVALID_CREDENTIALS);

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("studentId", "20200001"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].studentId").value("20200001"))
                    .andExpect(jsonPath("$.content[0].success").value(true))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        @Test
        @DisplayName("success 필터로 조회")
        void getLoginHistories_FilterBySuccess_ReturnsFiltered() throws Exception {
            // given
            createLoginSuccess(adminUser, "20200001", TEST_IP);
            createLoginFailure("20220001", TEST_IP, LoginFailureReason.INVALID_CREDENTIALS);

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("success", "false"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].success").value(false))
                    .andExpect(jsonPath("$.content[0].failureReason").value("INVALID_CREDENTIALS"))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        @Test
        @DisplayName("ipAddress 필터로 조회")
        void getLoginHistories_FilterByIpAddress_ReturnsFiltered() throws Exception {
            // given
            createLoginSuccess(adminUser, "20200001", TEST_IP);
            createLoginFailure("20220001", TEST_IP_2, LoginFailureReason.INVALID_CREDENTIALS);

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("ipAddress", TEST_IP_2))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].ipAddress").value(TEST_IP_2))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        @Test
        @DisplayName("날짜 범위 필터로 조회")
        void getLoginHistories_FilterByDateRange_ReturnsFiltered() throws Exception {
            // given
            createLoginSuccess(adminUser, "20200001", TEST_IP);

            // 오래된 이력 생성
            transactionTemplate.execute(status -> {
                LoginHistory oldHistory = LoginHistory.success(adminUser, "20200001", TEST_IP, TEST_USER_AGENT);
                setField(oldHistory, "attemptedAt", Instant.now().minus(30, ChronoUnit.DAYS));
                loginHistoryRepository.save(oldHistory);
                return null;
            });

            Instant startDate = Instant.now().minus(1, ChronoUnit.DAYS);
            Instant endDate = Instant.now().plus(1, ChronoUnit.DAYS);

            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        @Test
        @DisplayName("복합 필터로 조회")
        void getLoginHistories_CombinedFilters_ReturnsFiltered() throws Exception {
            // given
            createLoginSuccess(adminUser, "20200001", TEST_IP);
            createLoginFailure("20200001", TEST_IP, LoginFailureReason.INVALID_CREDENTIALS);
            createLoginFailure("20220001", TEST_IP_2, LoginFailureReason.ACCOUNT_LOCKED);

            // when & then - studentId + success 복합 필터
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("studentId", "20200001")
                            .param("success", "false"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].studentId").value("20200001"))
                    .andExpect(jsonPath("$.content[0].success").value(false))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        @Test
        @DisplayName("페이지네이션 동작 확인")
        void getLoginHistories_Pagination_ReturnsCorrectPage() throws Exception {
            // given - 3개 이력 생성
            createLoginSuccess(adminUser, "20200001", TEST_IP);
            createLoginFailure("20220001", TEST_IP, LoginFailureReason.INVALID_CREDENTIALS);
            createLoginFailure("20220001", TEST_IP_2, LoginFailureReason.ACCOUNT_LOCKED);

            // when & then - size=2로 첫 페이지 조회
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("page", "0")
                            .param("size", "2"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }

        @Test
        @DisplayName("빈 결과 조회")
        void getLoginHistories_NoData_ReturnsEmptyPage() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content").isEmpty())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
        }
    }

    @Nested
    @DisplayName("권한 검증")
    class AuthorizationTest {

        @Test
        @DisplayName("MEMBER 권한으로 조회 시 403 반환")
        void getLoginHistories_WithMemberRole_Returns403() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증되지 않은 사용자 조회 시 401 반환")
        void getLoginHistories_Unauthenticated_Returns401() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }
}
