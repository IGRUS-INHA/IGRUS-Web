package igrus.web.admin.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.admin.user.dto.ChangeUserRoleRequest;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.common.exception.ErrorCode;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
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
                    .andExpect(jsonPath("$.content").isArray())
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
                    .andExpect(jsonPath("$.content").isArray());
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
}
