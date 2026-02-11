package igrus.web.inquiry.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.domain.InquiryStatus;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.request.CreateInquiryMemoRequest;
import igrus.web.inquiry.dto.request.CreateInquiryReplyRequest;
import igrus.web.inquiry.dto.request.CreateMemberInquiryRequest;
import igrus.web.inquiry.dto.request.GuestInquiryLookupRequest;
import igrus.web.inquiry.dto.request.UpdateInquiryReplyRequest;
import igrus.web.inquiry.dto.request.UpdateInquiryStatusRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.repository.InquiryRepository;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import igrus.web.inquiry.service.create.CreateMemberInquiryService;
import igrus.web.inquiry.service.manage.CreateInquiryMemoService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@DisplayName("InquiryController 보안/RBAC 통합 테스트")
class InquiryControllerSecurityTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @Autowired
    private CreateMemberInquiryService createMemberInquiryService;

    @Autowired
    private CreateInquiryMemoService createInquiryMemoService;

    @Autowired
    private InquiryRepository inquiryRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private User associateUser;
    private User memberUser;
    private User operatorUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        setUpBase();
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
        memberUser = createAndSaveUser("20230002", "member@inha.edu", UserRole.MEMBER);
        operatorUser = createAndSaveUser("20230003", "operator@inha.edu", UserRole.OPERATOR);
        adminUser = createAndSaveUser("20230004", "admin@inha.edu", UserRole.ADMIN);
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
                .title("비회원 문의")
                .content("내용")
                .email("guest@test.com")
                .name("홍길동")
                .password("password123")
                .build();
        return createGuestInquiryService.createGuestInquiry(request);
    }

    private InquiryCreateResponse createTestMemberInquiry(User user) {
        CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                .type(InquiryType.EVENT)
                .title("회원 문의")
                .content("내용")
                .build();
        return createMemberInquiryService.createMemberInquiry(request, user.getId());
    }

    // ==================== 2.1 공개 API 접근 ====================

    @Nested
    @DisplayName("공개 API 접근 (비인증 허용)")
    class PublicApiAccessTest {

        @Test
        @DisplayName("INQ-SEC-001: 비인증 사용자의 비회원 문의 생성 - 201 Created")
        void guestInquiryCreate_WithoutAuth_Returns201() throws Exception {
            CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                    .type(InquiryType.JOIN)
                    .title("비회원 문의")
                    .content("내용")
                    .email("guest@test.com")
                    .name("홍길동")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/inquiries/guest")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("INQ-SEC-002: 비인증 사용자의 비회원 문의 조회 - 200 OK")
        void guestInquiryLookup_WithoutAuth_Returns200() throws Exception {
            InquiryCreateResponse createResponse = createTestGuestInquiry();

            GuestInquiryLookupRequest lookupRequest = GuestInquiryLookupRequest.builder()
                    .inquiryNumber(createResponse.getInquiryNumber())
                    .email("guest@test.com")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/inquiries/lookup")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(lookupRequest)))
                    .andExpect(status().isOk());
        }
    }

    // ==================== 2.2 비인증 접근 차단 ====================

    @Nested
    @DisplayName("비인증 접근 차단 (401 Unauthorized)")
    class UnauthenticatedAccessTest {

        @Test
        @DisplayName("INQ-SEC-010: 비인증 - 회원 문의 생성")
        void memberInquiryCreate_WithoutAuth_Returns401() throws Exception {
            mockMvc.perform(post("/api/v1/inquiries/member")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("INQ-SEC-011: 비인증 - 내 문의 목록 조회")
        void myInquiries_WithoutAuth_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries/my"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("INQ-SEC-012: 비인증 - 내 문의 상세 조회")
        void myInquiryDetail_WithoutAuth_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries/my/1"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("INQ-SEC-013: 비인증 - 전체 문의 목록")
        void allInquiries_WithoutAuth_Returns401() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("INQ-SEC-014: 비인증 - 답변 작성")
        void createReply_WithoutAuth_Returns401() throws Exception {
            mockMvc.perform(post("/api/v1/inquiries/1/reply")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("INQ-SEC-015: 비인증 - 문의 삭제")
        void deleteInquiry_WithoutAuth_Returns401() throws Exception {
            mockMvc.perform(delete("/api/v1/inquiries/1")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== 2.3 일반 회원 접근 제한 ====================

    @Nested
    @DisplayName("일반 회원 접근 제한 (403 Forbidden)")
    class MemberAccessRestrictionTest {

        @Test
        @DisplayName("INQ-SEC-020: ASSOCIATE - 전체 문의 목록 조회 차단")
        void allInquiries_AsAssociate_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries")
                            .with(withAuth(associateUser)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INQ-SEC-021: MEMBER - 전체 문의 목록 조회 차단")
        void allInquiries_AsMember_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries")
                            .with(withAuth(memberUser)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INQ-SEC-022: ASSOCIATE - 문의 상세 조회 (관리자) 차단")
        void inquiryDetail_AsAssociate_Returns403() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries/1")
                            .with(withAuth(associateUser)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INQ-SEC-023: MEMBER - 답변 작성 차단")
        void createReply_AsMember_Returns403() throws Exception {
            CreateInquiryReplyRequest request = CreateInquiryReplyRequest.builder()
                    .content("답변")
                    .build();

            mockMvc.perform(post("/api/v1/inquiries/1/reply")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INQ-SEC-024: ASSOCIATE - 답변 수정 차단")
        void updateReply_AsAssociate_Returns403() throws Exception {
            UpdateInquiryReplyRequest request = UpdateInquiryReplyRequest.builder()
                    .content("수정")
                    .build();

            mockMvc.perform(put("/api/v1/inquiries/1/reply")
                            .with(withAuth(associateUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INQ-SEC-025: MEMBER - 상태 변경 차단")
        void updateStatus_AsMember_Returns403() throws Exception {
            UpdateInquiryStatusRequest request = UpdateInquiryStatusRequest.builder()
                    .status(InquiryStatus.IN_PROGRESS)
                    .build();

            mockMvc.perform(put("/api/v1/inquiries/1/status")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INQ-SEC-026: ASSOCIATE - 내부 메모 작성 차단")
        void createMemo_AsAssociate_Returns403() throws Exception {
            CreateInquiryMemoRequest request = CreateInquiryMemoRequest.builder()
                    .content("메모")
                    .build();

            mockMvc.perform(post("/api/v1/inquiries/1/memo")
                            .with(withAuth(associateUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INQ-SEC-027: MEMBER - 문의 삭제 차단")
        void deleteInquiry_AsMember_Returns403() throws Exception {
            mockMvc.perform(delete("/api/v1/inquiries/1")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }

    // ==================== 2.4 관리자 접근 허용 ====================

    @Nested
    @DisplayName("관리자 접근 허용 (OPERATOR/ADMIN)")
    class AdminAccessPermissionTest {

        @Test
        @DisplayName("INQ-SEC-030: OPERATOR - 전체 문의 목록 조회 허용")
        void allInquiries_AsOperator_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries")
                            .with(withAuth(operatorUser)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("INQ-SEC-031: ADMIN - 전체 문의 목록 조회 허용")
        void allInquiries_AsAdmin_Returns200() throws Exception {
            mockMvc.perform(get("/api/v1/inquiries")
                            .with(withAuth(adminUser)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("INQ-SEC-032: OPERATOR - 답변 작성 허용")
        void createReply_AsOperator_Returns201() throws Exception {
            InquiryCreateResponse inquiry = createTestGuestInquiry();

            CreateInquiryReplyRequest request = CreateInquiryReplyRequest.builder()
                    .content("답변 내용입니다.")
                    .build();

            mockMvc.perform(post("/api/v1/inquiries/" + inquiry.getId() + "/reply")
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("INQ-SEC-033: ADMIN - 문의 삭제 허용")
        void deleteInquiry_AsAdmin_Returns204() throws Exception {
            InquiryCreateResponse inquiry = createTestGuestInquiry();

            mockMvc.perform(delete("/api/v1/inquiries/" + inquiry.getId())
                            .with(withAuth(adminUser))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("INQ-SEC-034: OPERATOR - 상태 변경 허용")
        void updateStatus_AsOperator_Returns200() throws Exception {
            InquiryCreateResponse inquiry = createTestGuestInquiry();

            UpdateInquiryStatusRequest request = UpdateInquiryStatusRequest.builder()
                    .status(InquiryStatus.IN_PROGRESS)
                    .build();

            mockMvc.perform(put("/api/v1/inquiries/" + inquiry.getId() + "/status")
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }
    }

    // ==================== 2.5 인증된 사용자 API 접근 ====================

    @Nested
    @DisplayName("인증된 사용자 API 접근")
    class AuthenticatedUserAccessTest {

        @Test
        @DisplayName("INQ-SEC-035: ASSOCIATE - 회원 문의 생성 허용")
        void memberInquiry_AsAssociate_Returns201() throws Exception {
            CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.EVENT)
                    .title("회원 문의")
                    .content("내용")
                    .build();

            mockMvc.perform(post("/api/v1/inquiries/member")
                            .with(withAuth(associateUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("INQ-SEC-036: MEMBER - 회원 문의 생성 허용")
        void memberInquiry_AsMember_Returns201() throws Exception {
            CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.EVENT)
                    .title("회원 문의")
                    .content("내용")
                    .build();

            mockMvc.perform(post("/api/v1/inquiries/member")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("INQ-SEC-037: OPERATOR - 회원 문의 생성 허용")
        void memberInquiry_AsOperator_Returns201() throws Exception {
            CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.EVENT)
                    .title("회원 문의")
                    .content("내용")
                    .build();

            mockMvc.perform(post("/api/v1/inquiries/member")
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("INQ-SEC-038: ADMIN - 회원 문의 생성 허용")
        void memberInquiry_AsAdmin_Returns201() throws Exception {
            CreateMemberInquiryRequest request = CreateMemberInquiryRequest.builder()
                    .type(InquiryType.EVENT)
                    .title("회원 문의")
                    .content("내용")
                    .build();

            mockMvc.perform(post("/api/v1/inquiries/member")
                            .with(withAuth(adminUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    // ==================== 2.6 소유권 검증 ====================

    @Nested
    @DisplayName("소유권 검증 (Ownership Verification)")
    class OwnershipVerificationTest {

        @Test
        @DisplayName("INQ-SEC-040: 회원 A가 본인 문의 상세 조회 - 200 OK")
        void myInquiryDetail_AsOwner_Returns200() throws Exception {
            InquiryCreateResponse inquiry = createTestMemberInquiry(associateUser);

            mockMvc.perform(get("/api/v1/inquiries/my/" + inquiry.getId())
                            .with(withAuth(associateUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(inquiry.getId()));
        }

        @Test
        @DisplayName("INQ-SEC-041: 회원 B가 회원 A의 문의 상세 조회 - 403 (접근 거부)")
        void myInquiryDetail_AsNonOwner_Returns403() throws Exception {
            InquiryCreateResponse inquiry = createTestMemberInquiry(associateUser);

            mockMvc.perform(get("/api/v1/inquiries/my/" + inquiry.getId())
                            .with(withAuth(memberUser)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("INQ-SEC-042: 비회원 이메일 불일치로 조회 - 404 (존재 여부 미노출)")
        void guestLookup_WithWrongEmail_Returns404() throws Exception {
            InquiryCreateResponse inquiry = createTestGuestInquiry();

            GuestInquiryLookupRequest lookupRequest = GuestInquiryLookupRequest.builder()
                    .inquiryNumber(inquiry.getInquiryNumber())
                    .email("wrong@test.com")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/inquiries/lookup")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(lookupRequest)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("INQ-SEC-043: 비회원 비밀번호 불일치로 조회 - 401")
        void guestLookup_WithWrongPassword_Returns401() throws Exception {
            InquiryCreateResponse inquiry = createTestGuestInquiry();

            GuestInquiryLookupRequest lookupRequest = GuestInquiryLookupRequest.builder()
                    .inquiryNumber(inquiry.getInquiryNumber())
                    .email("guest@test.com")
                    .password("wrongpassword")
                    .build();

            mockMvc.perform(post("/api/v1/inquiries/lookup")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(lookupRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("INQ-SEC-044: 회원 A의 문의가 회원 B 목록에 미포함")
        void myInquiries_ExcludesOtherUsersInquiries() throws Exception {
            createTestMemberInquiry(associateUser);
            createTestMemberInquiry(associateUser);
            createTestMemberInquiry(memberUser);

            mockMvc.perform(get("/api/v1/inquiries/my")
                            .with(withAuth(memberUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    // ==================== 2.7 보안 검증 ====================

    @Nested
    @DisplayName("보안 검증 (Security Assurance)")
    class SecurityAssuranceTest {

        @Test
        @DisplayName("INQ-SEC-051: 비회원 조회 응답에 내부 메모 미포함")
        void guestLookup_DoesNotExposeMemos() throws Exception {
            InquiryCreateResponse inquiry = createTestGuestInquiry();

            // 내부 메모 작성
            CreateInquiryMemoRequest memoRequest = CreateInquiryMemoRequest.builder()
                    .content("내부 메모")
                    .build();
            createInquiryMemoService.createMemo(inquiry.getId(), memoRequest, operatorUser.getId());

            GuestInquiryLookupRequest lookupRequest = GuestInquiryLookupRequest.builder()
                    .inquiryNumber(inquiry.getInquiryNumber())
                    .email("guest@test.com")
                    .password("password123")
                    .build();

            mockMvc.perform(post("/api/v1/inquiries/lookup")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(lookupRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memos").doesNotExist());
        }

        @Test
        @DisplayName("INQ-SEC-052: 회원 상세 조회 응답에 내부 메모 미포함")
        void myInquiryDetail_DoesNotExposeMemos() throws Exception {
            InquiryCreateResponse inquiry = createTestMemberInquiry(associateUser);

            // 내부 메모 작성
            CreateInquiryMemoRequest memoRequest = CreateInquiryMemoRequest.builder()
                    .content("내부 메모")
                    .build();
            createInquiryMemoService.createMemo(inquiry.getId(), memoRequest, operatorUser.getId());

            mockMvc.perform(get("/api/v1/inquiries/my/" + inquiry.getId())
                            .with(withAuth(associateUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.memos").doesNotExist());
        }

        @Test
        @DisplayName("INQ-SEC-054: 비인가 접근 시 DB 부작용 없음 (상태 변경)")
        void unauthorizedStatusChange_NoSideEffect() throws Exception {
            InquiryCreateResponse inquiry = createTestGuestInquiry();

            UpdateInquiryStatusRequest request = UpdateInquiryStatusRequest.builder()
                    .status(InquiryStatus.IN_PROGRESS)
                    .build();

            // MEMBER로 상태 변경 시도 (403 예상)
            mockMvc.perform(put("/api/v1/inquiries/" + inquiry.getId() + "/status")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());

            // DB에 상태 변경 없음 확인
            transactionTemplate.execute(status -> {
                var savedInquiry = inquiryRepository.findById(inquiry.getId()).orElseThrow();
                assertThat(savedInquiry.getStatus()).isEqualTo(InquiryStatus.PENDING);
                return null;
            });
        }

        @Test
        @DisplayName("INQ-SEC-055: 비인가 삭제 시도 후 문의 존재 확인")
        void unauthorizedDelete_InquiryStillExists() throws Exception {
            InquiryCreateResponse inquiry = createTestGuestInquiry();

            // ASSOCIATE로 삭제 시도 (403 예상)
            mockMvc.perform(delete("/api/v1/inquiries/" + inquiry.getId())
                            .with(withAuth(associateUser))
                            .with(csrf()))
                    .andExpect(status().isForbidden());

            // 문의 여전히 존재 확인
            assertThat(inquiryRepository.findById(inquiry.getId())).isPresent();
        }
    }
}
