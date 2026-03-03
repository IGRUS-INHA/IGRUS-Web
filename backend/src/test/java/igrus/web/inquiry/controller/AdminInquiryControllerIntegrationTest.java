package igrus.web.inquiry.controller;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AdminInquiryController 통합 테스트.
 *
 * <p>관리자 문의 API의 인증/인가 및 정상 동작을 검증합니다.</p>
 *
 * <p>테스트 케이스:</p>
 * <ul>
 *     <li>비인증 사용자 접근 차단 (401)</li>
 *     <li>일반 회원 접근 제한 (403)</li>
 *     <li>관리자 접근 허용 (OPERATOR/ADMIN)</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("AdminInquiryController 통합 테스트")
class AdminInquiryControllerIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    private User associateUser;
    private User memberUser;
    private User operatorUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
            memberUser = createAndSaveUser("20230002", "member@inha.edu", UserRole.MEMBER);
            operatorUser = createAndSaveUser("20230003", "operator@inha.edu", UserRole.OPERATOR);
            adminUser = createAndSaveUser("20230004", "admin@inha.edu", UserRole.ADMIN);
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

    private InquiryCreateResponse createTestGuestInquiry() {
        CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                .type(InquiryType.JOIN)
                .title("테스트 문의")
                .content("문의 내용")
                .email("guest@test.com")
                .name("홍길동")
                .password("password123")
                .build();
        return createGuestInquiryService.createGuestInquiry(request);
    }

    // ==================== 비인증 사용자 (401) ====================

    @Nested
    @DisplayName("비인증 사용자 접근 차단 (401)")
    class UnauthenticatedAccessTest {

        @Test
        @DisplayName("비인증 사용자 전체 문의 목록 조회 -> 401")
        void getAllInquiries_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비인증 사용자 문의 상세 조회 -> 401")
        void getInquiryDetail_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비인증 사용자 문의 상태 변경 -> 401")
        void updateInquiryStatus_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(put("/api/v1/inquiries/1/status")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"IN_PROGRESS\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비인증 사용자 답변 작성 -> 401")
        void createReply_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(post("/api/v1/inquiries/1/reply")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"답변\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비인증 사용자 문의 삭제 -> 401")
        void deleteInquiry_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(delete("/api/v1/inquiries/1")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비인증 사용자 내부 메모 작성 -> 401")
        void createMemo_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(post("/api/v1/inquiries/1/memo")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"메모\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 일반 회원 접근 제한 (403) ====================

    @Nested
    @DisplayName("일반 회원 접근 제한 (403)")
    class MemberAccessRestrictionTest {

        @Test
        @DisplayName("ASSOCIATE 전체 문의 목록 조회 -> 403")
        void getAllInquiries_AsAssociate_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries")
                            .with(withAuth(associateUser)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("MEMBER 전체 문의 목록 조회 -> 403")
        void getAllInquiries_AsMember_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries")
                            .with(withAuth(memberUser)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("MEMBER 답변 작성 -> 403")
        void createReply_AsMember_Returns403() throws Exception {
            mockMvc.perform(post("/api/v1/inquiries/1/reply")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"답변\"}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("ASSOCIATE 문의 삭제 -> 403")
        void deleteInquiry_AsAssociate_Returns403() throws Exception {
            mockMvc.perform(delete("/api/v1/inquiries/1")
                            .with(withAuth(associateUser))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("MEMBER 내부 메모 작성 -> 403")
        void createMemo_AsMember_Returns403() throws Exception {
            mockMvc.perform(post("/api/v1/inquiries/1/memo")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"메모\"}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== 관리자 접근 허용 ====================

    @Nested
    @DisplayName("관리자 접근 허용 (OPERATOR/ADMIN)")
    class AdminAccessPermissionTest {

        @Test
        @DisplayName("OPERATOR 전체 문의 목록 조회 -> 200")
        void getAllInquiries_AsOperator_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries")
                            .with(withAuth(operatorUser)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("ADMIN 전체 문의 목록 조회 -> 200")
        void getAllInquiries_AsAdmin_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries")
                            .with(withAuth(adminUser)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("OPERATOR 답변 작성 -> 201")
        void createReply_AsOperator_Returns201() throws Exception {
            InquiryCreateResponse inquiry = createTestGuestInquiry();

            mockMvc.perform(post("/api/v1/inquiries/" + inquiry.getId() + "/reply")
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"content\":\"답변 내용입니다.\"}"))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("ADMIN 문의 삭제 -> 204")
        void deleteInquiry_AsAdmin_Returns204() throws Exception {
            InquiryCreateResponse inquiry = createTestGuestInquiry();

            mockMvc.perform(delete("/api/v1/inquiries/" + inquiry.getId())
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }
}
