package igrus.web.admin.dashboard.controller;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("AdminDashboardController 통합 테스트")
class AdminDashboardControllerTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private static final String BASE_URL = "/api/v1/admin/dashboard";

    private User adminUser;
    private User operatorUser;
    private User memberUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
        operatorUser = createAndSaveUser("20210001", "operator@inha.edu", UserRole.OPERATOR);
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

    @Nested
    @DisplayName("대시보드 통계 조회")
    class GetDashboardStatsTest {

        @Test
        @DisplayName("ADMIN 권한으로 대시보드 통계 조회 성공")
        void getDashboardStats_WithAdminRole_ReturnsOk() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.todayPostCount").exists())
                    .andExpect(jsonPath("$.todayCommentCount").exists())
                    .andExpect(jsonPath("$.weeklyApprovedMemberCount").exists())
                    .andExpect(jsonPath("$.pendingInquiryCount").exists())
                    .andExpect(jsonPath("$.pendingAssociateCount").exists());
        }

        @Test
        @DisplayName("OPERATOR 권한으로 대시보드 조회 시 403 반환 (ADMIN 전용)")
        void getDashboardStats_WithOperatorRole_Returns403() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(operatorUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("MEMBER 권한으로 대시보드 조회 시 403 반환")
        void getDashboardStats_WithMemberRole_Returns403() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증되지 않은 사용자 대시보드 조회 시 401 반환")
        void getDashboardStats_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }
}
