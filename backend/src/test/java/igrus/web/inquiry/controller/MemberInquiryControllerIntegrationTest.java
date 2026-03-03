package igrus.web.inquiry.controller;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.service.create.CreateMemberInquiryService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MemberInquiryController 통합 테스트.
 *
 * <p>회원 문의 API의 인증/인가 및 정상 동작을 검증합니다.</p>
 *
 * <p>테스트 케이스:</p>
 * <ul>
 *     <li>비인증 사용자 접근 차단 (401)</li>
 *     <li>인증된 사용자 회원 문의 생성 허용 (201)</li>
 *     <li>인증된 사용자 내 문의 목록 조회 허용 (200)</li>
 *     <li>소유권 검증: 타인 문의 조회 차단 (403)</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("MemberInquiryController 통합 테스트")
class MemberInquiryControllerIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CreateMemberInquiryService createMemberInquiryService;

    private User associateUser;
    private User memberUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
            memberUser = createAndSaveUser("20230002", "member@inha.edu", UserRole.MEMBER);
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

    private InquiryCreateResponse createTestMemberInquiry(User user) {
        CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                .type(InquiryType.EVENT)
                .title("회원 문의")
                .content("문의 내용")
                .build();
        return createMemberInquiryService.createMemberInquiry(request, user.getId());
    }

    // ==================== 비인증 사용자 (401) ====================

    @Nested
    @DisplayName("비인증 사용자 접근 차단 (401)")
    class UnauthenticatedAccessTest {

        @Test
        @DisplayName("비인증 사용자 회원 문의 생성 -> 401")
        void createMemberInquiry_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(post("/api/v1/inquiries/member")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"EVENT\",\"title\":\"문의\",\"content\":\"내용\"}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비인증 사용자 내 문의 목록 조회 -> 401")
        void getMyInquiries_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries/my"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("비인증 사용자 내 문의 상세 조회 -> 401")
        void getMyInquiry_Unauthenticated_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries/my/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 인증된 사용자 접근 허용 ====================

    @Nested
    @DisplayName("인증된 사용자 접근 허용")
    class AuthenticatedUserAccessTest {

        @Test
        @DisplayName("ASSOCIATE 회원 문의 생성 -> 201")
        void createMemberInquiry_AsAssociate_Returns201() throws Exception {
            mockMvc.perform(post("/api/v1/inquiries/member")
                            .with(withAuth(associateUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"type\":\"EVENT\",\"title\":\"회원 문의\",\"content\":\"내용\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.inquiryNumber").exists());
        }

        @Test
        @DisplayName("MEMBER 내 문의 목록 조회 -> 200")
        void getMyInquiries_AsMember_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries/my")
                            .with(withAuth(memberUser)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("본인 문의 상세 조회 -> 200")
        void getMyInquiry_AsOwner_Returns200() throws Exception {
            InquiryCreateResponse inquiry = createTestMemberInquiry(associateUser);

            mockMvc.perform(get("/api/v1/inquiries/my/" + inquiry.getId())
                            .with(withAuth(associateUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(inquiry.getId()));
        }
    }

    // ==================== 소유권 검증 ====================

    @Nested
    @DisplayName("소유권 검증")
    class OwnershipVerificationTest {

        @Test
        @DisplayName("타인 문의 상세 조회 -> 403")
        void getMyInquiry_AsNonOwner_Returns403() throws Exception {
            InquiryCreateResponse inquiry = createTestMemberInquiry(associateUser);

            mockMvc.perform(get("/api/v1/inquiries/my/" + inquiry.getId())
                            .with(withAuth(memberUser)))
                    .andExpect(status().isForbidden());
        }
    }
}
