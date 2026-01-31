package igrus.web.security.auth.approval.controller;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.common.exception.ErrorCode;
import igrus.web.security.auth.approval.dto.request.BulkApprovalRequest;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.security.jwt.JwtTokenProvider;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("AdminMemberController 통합 테스트")
class AdminMemberControllerTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE_URL = "/api/v1/admin/members";

    private User adminUser;
    private User memberUser;
    private User associateUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        adminUser = createAndSaveUser("20200001", "admin@inha.edu", UserRole.ADMIN);
        memberUser = createAndSaveUser("20220001", "member@inha.edu", UserRole.MEMBER);
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
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
    @DisplayName("준회원 목록 조회")
    class GetPendingAssociatesTest {

        @Test
        @DisplayName("관리자 권한으로 준회원 목록 조회 성공")
        void getPendingAssociates_WithAdminRole_ReturnsOk() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL + "/pending")
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .param("page", "0")
                            .param("size", "10"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content[0].studentId").value("20230001"))
                    .andExpect(jsonPath("$.content[0].name").value("테스트유저"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("일반 사용자 권한으로 목록 조회 시 403 반환")
        void getPendingAssociates_WithMemberRole_Returns403() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL + "/pending")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증되지 않은 사용자 목록 조회 시 403 반환")
        void getPendingAssociates_Unauthenticated_Returns403() throws Exception {
            // when & then
            // Note: 인증 없이 CSRF 토큰도 없으면 CSRF 필터에서 403 반환
            mockMvc.perform(get(BASE_URL + "/pending"))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("개별 승인")
    class ApproveAssociateTest {

        @Test
        @DisplayName("관리자 권한으로 개별 승인 성공")
        void approveAssociate_WithAdminRole_ReturnsOk() throws Exception {
            // when & then
            mockMvc.perform(post(BASE_URL + "/" + associateUser.getId() + "/approve")
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk());

            // verify - 역할이 MEMBER로 변경되었는지 확인
            User updatedUser = userRepository.findById(associateUser.getId()).orElseThrow();
            assert updatedUser.getRole() == UserRole.MEMBER;
        }

        @Test
        @DisplayName("존재하지 않는 사용자 승인 시 404 반환")
        void approveAssociate_UserNotFound_Returns404() throws Exception {
            // given
            Long nonExistentUserId = 999L;

            // when & then
            mockMvc.perform(post(BASE_URL + "/" + nonExistentUserId + "/approve")
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_FOUND.getCode()));
        }

        @Test
        @DisplayName("이미 정회원인 사용자 승인 시 400 반환")
        void approveAssociate_AlreadyMember_Returns400() throws Exception {
            // when & then
            mockMvc.perform(post(BASE_URL + "/" + memberUser.getId() + "/approve")
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.USER_NOT_ASSOCIATE.getCode()));
        }

        @Test
        @DisplayName("일반 사용자 권한으로 승인 시도 시 403 반환")
        void approveAssociate_WithMemberRole_Returns403() throws Exception {
            // when & then
            mockMvc.perform(post(BASE_URL + "/" + associateUser.getId() + "/approve")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("일괄 승인")
    class BulkApprovalTest {

        @Test
        @DisplayName("관리자 권한으로 일괄 승인 성공")
        void approveBulk_WithAdminRole_ReturnsOk() throws Exception {
            // given
            User associate2 = createAndSaveUser("20230002", "a2@inha.edu", UserRole.ASSOCIATE);
            User associate3 = createAndSaveUser("20230003", "a3@inha.edu", UserRole.ASSOCIATE);
            List<Long> userIds = List.of(associateUser.getId(), associate2.getId(), associate3.getId());
            BulkApprovalRequest request = new BulkApprovalRequest(userIds, "2026년 1월 일괄 승인");

            // when & then
            mockMvc.perform(post(BASE_URL + "/approve/bulk")
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.approvedCount").value(3))
                    .andExpect(jsonPath("$.failedCount").value(0))
                    .andExpect(jsonPath("$.totalRequested").value(3));

            // verify - 모든 사용자가 MEMBER로 변경되었는지 확인
            assert userRepository.findById(associateUser.getId()).orElseThrow().getRole() == UserRole.MEMBER;
            assert userRepository.findById(associate2.getId()).orElseThrow().getRole() == UserRole.MEMBER;
            assert userRepository.findById(associate3.getId()).orElseThrow().getRole() == UserRole.MEMBER;
        }

        @Test
        @DisplayName("일부 사용자 승인 실패 시 부분 성공 응답")
        void approveBulk_PartialSuccess_ReturnsPartialResult() throws Exception {
            // given
            User associate2 = createAndSaveUser("20230002", "a2@inha.edu", UserRole.ASSOCIATE);
            Long nonExistentUserId = 999L;
            List<Long> userIds = List.of(associateUser.getId(), associate2.getId(), nonExistentUserId);
            BulkApprovalRequest request = new BulkApprovalRequest(userIds, null);

            // when & then
            mockMvc.perform(post(BASE_URL + "/approve/bulk")
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.approvedCount").value(2))
                    .andExpect(jsonPath("$.failedCount").value(1))
                    .andExpect(jsonPath("$.totalRequested").value(3));
        }

        @Test
        @DisplayName("빈 목록으로 일괄 승인 시 400 반환 (Validation)")
        void approveBulk_EmptyList_Returns400() throws Exception {
            // given
            String requestBody = "{\"userIds\": [], \"reason\": null}";

            // when & then
            mockMvc.perform(post(BASE_URL + "/approve/bulk")
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
        @DisplayName("userIds가 null인 경우 400 반환 (Validation)")
        void approveBulk_NullUserIds_Returns400() throws Exception {
            // given
            String requestBody = "{\"reason\": \"test\"}";

            // when & then
            mockMvc.perform(post(BASE_URL + "/approve/bulk")
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ErrorCode.INVALID_INPUT_VALUE.getCode()));
        }

        @Test
        @DisplayName("일반 사용자 권한으로 일괄 승인 시도 시 403 반환")
        void approveBulk_WithMemberRole_Returns403() throws Exception {
            // given
            List<Long> userIds = List.of(associateUser.getId());
            BulkApprovalRequest request = new BulkApprovalRequest(userIds, null);

            // when & then
            mockMvc.perform(post(BASE_URL + "/approve/bulk")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증되지 않은 사용자 일괄 승인 시도 시 403 반환")
        void approveBulk_Unauthenticated_Returns403() throws Exception {
            // given
            List<Long> userIds = List.of(associateUser.getId());
            BulkApprovalRequest request = new BulkApprovalRequest(userIds, null);

            // when & then
            // Note: 인증 없이 CSRF 토큰도 없으면 CSRF 필터에서 403 반환
            mockMvc.perform(post(BASE_URL + "/approve/bulk")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }
}
