package igrus.web.user.controller;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.AccountChangeType;
import igrus.web.user.domain.AccountStatusChangeHistory;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.repository.AccountStatusChangeHistoryRepository;
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
@DisplayName("AdminAccountStatusChangeHistoryController 통합 테스트")
class AdminAccountStatusChangeHistoryControllerTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountStatusChangeHistoryRepository accountStatusChangeHistoryRepository;

    private static final String BASE_URL = "/api/v1/admin/account-status-histories";

    private User adminUser;
    private User operatorUser;
    private User memberUser;
    private User targetUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
            operatorUser = createAndSaveUser("20210001", "operator@inha.edu", UserRole.OPERATOR);
            memberUser = createAndSaveUser("20220001", "member@inha.edu", UserRole.MEMBER);
            targetUser = createAndSaveUser("20230001", "target@inha.edu", UserRole.MEMBER);
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

    private void createHistory(User user, User changedBy, AccountChangeType changeType,
                               String previousValue, String newValue, String reason) {
        transactionTemplate.execute(status -> {
            AccountStatusChangeHistory history = AccountStatusChangeHistory.create(
                    user.getId(), user.getStudentId(),
                    changedBy.getId(), changedBy.getStudentId(),
                    changeType, previousValue, newValue, reason
            );
            accountStatusChangeHistoryRepository.save(history);
            return null;
        });
    }

    @Nested
    @DisplayName("ADMIN 조회")
    class AdminAccessTests {

        @Test
        @DisplayName("ADMIN 권한으로 감사 이력 조회 성공")
        void getHistories_WithAdminRole_ReturnsOk() throws Exception {
            createHistory(targetUser, adminUser, AccountChangeType.APPROVAL,
                    "ASSOCIATE", "MEMBER", "관리자 승인");

            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("page", "0")
                            .param("size", "20"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].changeType").value("APPROVAL"))
                    .andExpect(jsonPath("$.content[0].previousValue").value("ASSOCIATE"))
                    .andExpect(jsonPath("$.content[0].newValue").value("MEMBER"));
        }

        @Test
        @DisplayName("userId 필터로 감사 이력 조회 성공")
        void getHistories_WithUserIdFilter_ReturnsFiltered() throws Exception {
            createHistory(targetUser, adminUser, AccountChangeType.APPROVAL,
                    "ASSOCIATE", "MEMBER", "승인");
            createHistory(memberUser, adminUser, AccountChangeType.ROLE_CHANGE,
                    "MEMBER", "OPERATOR", "변경");

            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("userId", targetUser.getId().toString()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("changeType 필터로 감사 이력 조회 성공")
        void getHistories_WithChangeTypeFilter_ReturnsFiltered() throws Exception {
            createHistory(targetUser, adminUser, AccountChangeType.APPROVAL,
                    "ASSOCIATE", "MEMBER", "승인");
            createHistory(targetUser, adminUser, AccountChangeType.SUSPENSION,
                    "ACTIVE", "SUSPENDED", "규정 위반");

            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("changeType", "SUSPENSION"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].changeType").value("SUSPENSION"));
        }

        @Test
        @DisplayName("이력이 없을 때 빈 결과 반환")
        void getHistories_NoHistories_ReturnsEmptyPage() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    @Nested
    @DisplayName("권한 제한")
    class AuthorizationTests {

        @Test
        @DisplayName("OPERATOR 권한으로 조회 시 403 반환")
        void getHistories_WithOperatorRole_Returns403() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(operatorUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("MEMBER 권한으로 조회 시 403 반환")
        void getHistories_WithMemberRole_Returns403() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증되지 않은 사용자 조회 시 401 반환")
        void getHistories_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }
}
