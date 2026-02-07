package igrus.web.admin.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.admin.user.dto.ChangeUserRoleRequest;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.common.exception.ErrorCode;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("AdminUserController 통합 테스트")
class AdminUserControllerTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE_URL = "/api/v1/admin/users";

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
    @DisplayName("회원 목록 조회")
    class GetUserListTest {

        @Test
        @DisplayName("ADMIN 권한으로 회원 목록 조회 성공")
        void getUserList_WithAdminRole_ReturnsOk() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users").isArray())
                    .andExpect(jsonPath("$.totalElements").value(3));
        }

        @Test
        @DisplayName("OPERATOR 권한으로 회원 목록 조회 성공")
        void getUserList_WithOperatorRole_ReturnsOk() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.users").isArray());
        }

        @Test
        @DisplayName("MEMBER 권한으로 회원 목록 조회 시 403 반환")
        void getUserList_WithMemberRole_Returns403() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증되지 않은 사용자 목록 조회 시 401 반환")
        void getUserList_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get(BASE_URL))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("키워드로 회원 검색 성공")
        void getUserList_WithKeyword_ReturnsFilteredResults() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("keyword", "20200001"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("역할 필터로 회원 검색 성공")
        void getUserList_WithRoleFilter_ReturnsFilteredResults() throws Exception {
            mockMvc.perform(get(BASE_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("role", "MEMBER"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    @Nested
    @DisplayName("회원 상세 조회")
    class GetUserDetailTest {

        @Test
        @DisplayName("ADMIN 권한으로 회원 상세 조회 성공")
        void getUserDetail_WithAdminRole_ReturnsOk() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + memberUser.getId())
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(memberUser.getId()))
                    .andExpect(jsonPath("$.studentId").value("20220001"))
                    .andExpect(jsonPath("$.name").value("테스트유저"));
        }

        @Test
        @DisplayName("OPERATOR 권한으로 회원 상세 조회 성공")
        void getUserDetail_WithOperatorRole_ReturnsOk() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + memberUser.getId())
                            .with(withAuth(operatorUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.userId").value(memberUser.getId()));
        }

        @Test
        @DisplayName("MEMBER 권한으로 회원 상세 조회 시 403 반환")
        void getUserDetail_WithMemberRole_Returns403() throws Exception {
            mockMvc.perform(get(BASE_URL + "/" + memberUser.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("존재하지 않는 사용자 상세 조회 시 404 반환")
        void getUserDetail_UserNotFound_Returns404() throws Exception {
            mockMvc.perform(get(BASE_URL + "/999")
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("회원 권한 변경")
    class ChangeUserRoleTest {

        @Test
        @DisplayName("ADMIN 권한으로 회원 권한 변경 성공")
        void changeUserRole_WithAdminRole_ReturnsNoContent() throws Exception {
            ChangeUserRoleRequest request = new ChangeUserRoleRequest(UserRole.OPERATOR);

            mockMvc.perform(put(BASE_URL + "/" + memberUser.getId() + "/role")
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNoContent());

            User updatedUser = userRepository.findById(memberUser.getId()).orElseThrow();
            assertThat(updatedUser.getRole()).isEqualTo(UserRole.OPERATOR);
        }

        @Test
        @DisplayName("OPERATOR 권한으로 회원 권한 변경 시 403 반환 (ADMIN 전용)")
        void changeUserRole_WithOperatorRole_Returns403() throws Exception {
            ChangeUserRoleRequest request = new ChangeUserRoleRequest(UserRole.OPERATOR);

            mockMvc.perform(put(BASE_URL + "/" + memberUser.getId() + "/role")
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("MEMBER 권한으로 회원 권한 변경 시 403 반환")
        void changeUserRole_WithMemberRole_Returns403() throws Exception {
            ChangeUserRoleRequest request = new ChangeUserRoleRequest(UserRole.OPERATOR);

            mockMvc.perform(put(BASE_URL + "/" + memberUser.getId() + "/role")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("자기 자신의 권한 변경 시 400 반환")
        void changeUserRole_SelfChange_Returns400() throws Exception {
            ChangeUserRoleRequest request = new ChangeUserRoleRequest(UserRole.MEMBER);

            mockMvc.perform(put(BASE_URL + "/" + adminUser.getId() + "/role")
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.SELF_ROLE_CHANGE_NOT_ALLOWED.getCode()));
        }

        @Test
        @DisplayName("동일 역할로 변경 시 400 반환")
        void changeUserRole_SameRole_Returns400() throws Exception {
            ChangeUserRoleRequest request = new ChangeUserRoleRequest(UserRole.MEMBER);

            mockMvc.perform(put(BASE_URL + "/" + memberUser.getId() + "/role")
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.SAME_ROLE_CHANGE.getCode()));
        }

        @Test
        @DisplayName("마지막 ADMIN 권한 변경 시 400 반환")
        void changeUserRole_LastAdmin_Returns400() throws Exception {
            // adminUser가 DB에서 유일한 ADMIN
            // callerUser는 DB에서 OPERATOR이지만 SecurityContext에서 ADMIN으로 인증
            // → DB countByRole(ADMIN) = 1 (adminUser만) → 마지막 ADMIN 에러 발생
            User callerUser = createAndSaveUser("20200002", "caller@inha.edu", UserRole.OPERATOR);

            AuthenticatedUser callerAuth = new AuthenticatedUser(
                    callerUser.getId(), callerUser.getStudentId(), "ADMIN"
            );
            Authentication auth = new UsernamePasswordAuthenticationToken(
                    callerAuth, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );

            ChangeUserRoleRequest request = new ChangeUserRoleRequest(UserRole.MEMBER);

            mockMvc.perform(put(BASE_URL + "/" + adminUser.getId() + "/role")
                            .with(authentication(auth))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.LAST_ADMIN_CANNOT_CHANGE.getCode()));
        }

        @Test
        @DisplayName("존재하지 않는 사용자 권한 변경 시 404 반환")
        void changeUserRole_UserNotFound_Returns404() throws Exception {
            ChangeUserRoleRequest request = new ChangeUserRoleRequest(UserRole.OPERATOR);

            mockMvc.perform(put(BASE_URL + "/999/role")
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()));
        }
    }

    @Nested
    @DisplayName("권한 변경 이력 조회")
    class GetRoleHistoriesTest {

        private static final String ROLE_HISTORIES_URL = BASE_URL + "/role-histories";

        private UserRoleHistory saveHistory(User user, UserRole previousRole, UserRole newRole, String reason) {
            UserRoleHistory history = UserRoleHistory.create(user, previousRole, newRole, reason);
            return userRoleHistoryRepository.save(history);
        }

        @Test
        @DisplayName("ADMIN 권한으로 이력 조회 성공")
        void getRoleHistories_WithAdminRole_ReturnsOk() throws Exception {
            saveHistory(memberUser, UserRole.ASSOCIATE, UserRole.MEMBER, "승급");

            mockMvc.perform(get(ROLE_HISTORIES_URL)
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].previousRole").value("ASSOCIATE"))
                    .andExpect(jsonPath("$.content[0].newRole").value("MEMBER"))
                    .andExpect(jsonPath("$.content[0].reason").value("승급"));
        }

        @Test
        @DisplayName("userId 필터 동작")
        void getRoleHistories_WithUserIdFilter_ReturnsFilteredResults() throws Exception {
            saveHistory(memberUser, UserRole.ASSOCIATE, UserRole.MEMBER, null);
            saveHistory(operatorUser, UserRole.MEMBER, UserRole.OPERATOR, null);

            mockMvc.perform(get(ROLE_HISTORIES_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("userId", memberUser.getId().toString()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].userId").value(memberUser.getId()));
        }

        @Test
        @DisplayName("changedBy 필터 동작")
        void getRoleHistories_WithChangedByFilter_ReturnsFilteredResults() throws Exception {
            UserRoleHistory h1 = saveHistory(memberUser, UserRole.ASSOCIATE, UserRole.MEMBER, null);
            UserRoleHistory h2 = saveHistory(operatorUser, UserRole.MEMBER, UserRole.OPERATOR, null);

            transactionTemplate.execute(status -> {
                entityManager.createNativeQuery(
                        "UPDATE user_role_histories SET user_role_histories_created_by = ? WHERE user_role_histories_id = ?")
                        .setParameter(1, adminUser.getId()).setParameter(2, h1.getId()).executeUpdate();
                entityManager.createNativeQuery(
                        "UPDATE user_role_histories SET user_role_histories_created_by = ? WHERE user_role_histories_id = ?")
                        .setParameter(1, operatorUser.getId()).setParameter(2, h2.getId()).executeUpdate();
                return null;
            });

            mockMvc.perform(get(ROLE_HISTORIES_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("changedBy", adminUser.getId().toString()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("previousRole/newRole 필터 동작")
        void getRoleHistories_WithRoleFilter_ReturnsFilteredResults() throws Exception {
            saveHistory(memberUser, UserRole.ASSOCIATE, UserRole.MEMBER, null);
            saveHistory(operatorUser, UserRole.MEMBER, UserRole.OPERATOR, null);

            mockMvc.perform(get(ROLE_HISTORIES_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("previousRole", "ASSOCIATE")
                            .param("newRole", "MEMBER"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("날짜 범위 필터 동작")
        void getRoleHistories_WithDateFilter_ReturnsFilteredResults() throws Exception {
            saveHistory(memberUser, UserRole.ASSOCIATE, UserRole.MEMBER, null);

            Instant startDate = Instant.now().minus(1, ChronoUnit.HOURS);
            Instant endDate = Instant.now().plus(1, ChronoUnit.HOURS);

            mockMvc.perform(get(ROLE_HISTORIES_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("startDate", startDate.toString())
                            .param("endDate", endDate.toString()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("복합 필터 조합 동작")
        void getRoleHistories_WithMultipleFilters_ReturnsFilteredResults() throws Exception {
            saveHistory(memberUser, UserRole.ASSOCIATE, UserRole.MEMBER, null);
            saveHistory(operatorUser, UserRole.MEMBER, UserRole.OPERATOR, null);

            mockMvc.perform(get(ROLE_HISTORIES_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("userId", memberUser.getId().toString())
                            .param("newRole", "MEMBER"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("페이지네이션 동작")
        void getRoleHistories_WithPagination_ReturnsPaginatedResults() throws Exception {
            saveHistory(memberUser, UserRole.ASSOCIATE, UserRole.MEMBER, "1번");
            saveHistory(memberUser, UserRole.MEMBER, UserRole.OPERATOR, "2번");
            saveHistory(memberUser, UserRole.OPERATOR, UserRole.ADMIN, "3번");

            mockMvc.perform(get(ROLE_HISTORIES_URL)
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("page", "0")
                            .param("size", "2"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(3))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.content.length()").value(2));
        }

        @Test
        @DisplayName("OPERATOR 권한으로 접근 시 403 반환")
        void getRoleHistories_WithOperatorRole_Returns403() throws Exception {
            mockMvc.perform(get(ROLE_HISTORIES_URL)
                            .with(withAuth(operatorUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("MEMBER 권한으로 접근 시 403 반환")
        void getRoleHistories_WithMemberRole_Returns403() throws Exception {
            mockMvc.perform(get(ROLE_HISTORIES_URL)
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("미인증 접근 시 401 반환")
        void getRoleHistories_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get(ROLE_HISTORIES_URL))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("빈 결과 처리")
        void getRoleHistories_NoData_ReturnsEmptyPage() throws Exception {
            mockMvc.perform(get(ROLE_HISTORIES_URL)
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("탈퇴한 사용자의 이력 조회 시 사용자 정보가 null/'탈퇴한 사용자'로 표시된다")
        void getRoleHistories_WithWithdrawnUser_ReturnsWithdrawnInfo() throws Exception {
            saveHistory(memberUser, UserRole.ASSOCIATE, UserRole.MEMBER, "승급");

            transactionTemplate.execute(status -> {
                entityManager.createNativeQuery(
                        "UPDATE users SET users_status = 'WITHDRAWN', users_deleted = true, users_deleted_at = NOW() " +
                                "WHERE users_id = :userId")
                        .setParameter("userId", memberUser.getId())
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
                return null;
            });

            mockMvc.perform(get(ROLE_HISTORIES_URL)
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].userId").isEmpty())
                    .andExpect(jsonPath("$.content[0].userName").value("탈퇴한 사용자"))
                    .andExpect(jsonPath("$.content[0].studentId").isEmpty())
                    .andExpect(jsonPath("$.content[0].previousRole").value("ASSOCIATE"))
                    .andExpect(jsonPath("$.content[0].newRole").value("MEMBER"));
        }
    }
}
