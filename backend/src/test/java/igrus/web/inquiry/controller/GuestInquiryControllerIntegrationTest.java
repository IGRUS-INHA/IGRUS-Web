package igrus.web.inquiry.controller;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.inquiry.domain.InquiryType;
import igrus.web.inquiry.dto.request.CreateGuestInquiryRequest;
import igrus.web.inquiry.dto.response.InquiryCreateResponse;
import igrus.web.inquiry.service.create.CreateGuestInquiryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GuestInquiryController 통합 테스트.
 *
 * <p>비회원 문의 API의 공개 접근 및 정상 동작을 검증합니다.</p>
 *
 * <p>테스트 케이스:</p>
 * <ul>
 *     <li>비인증 사용자 비회원 문의 생성 허용 (201)</li>
 *     <li>비인증 사용자 비회원 문의 조회 허용 (200)</li>
 *     <li>잘못된 비밀번호로 조회 시 인증 실패 (401)</li>
 * </ul>
 */
@AutoConfigureMockMvc
@DisplayName("GuestInquiryController 통합 테스트")
class GuestInquiryControllerIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CreateGuestInquiryService createGuestInquiryService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private InquiryCreateResponse createTestGuestInquiry() {
        CreateGuestInquiryRequest request = CreateGuestInquiryRequest.builder()
                .type(InquiryType.JOIN)
                .title("비회원 문의")
                .content("문의 내용입니다")
                .email("guest@test.com")
                .name("홍길동")
                .password("password123")
                .build();
        return createGuestInquiryService.createGuestInquiry(request);
    }

    // ==================== 공개 API 접근 ====================

    @Nested
    @DisplayName("공개 API 접근 (비인증 허용)")
    class PublicApiAccessTest {

        @Test
        @DisplayName("비인증 사용자 비회원 문의 생성 -> 201")
        void createGuestInquiry_WithoutAuth_Returns201() throws Exception {
            String requestBody = """
                    {
                        "type": "JOIN",
                        "title": "비회원 문의",
                        "content": "문의 내용입니다",
                        "email": "guest@test.com",
                        "name": "홍길동",
                        "password": "password123"
                    }
                    """;

            mockMvc.perform(post("/api/v1/inquiries/guest")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.inquiryNumber").exists());
        }

        @Test
        @DisplayName("비인증 사용자 비회원 문의 조회 -> 200")
        void lookupGuestInquiry_WithoutAuth_Returns200() throws Exception {
            InquiryCreateResponse inquiry = createTestGuestInquiry();

            String requestBody = String.format("""
                    {
                        "inquiryNumber": "%s",
                        "email": "guest@test.com",
                        "password": "password123"
                    }
                    """, inquiry.getInquiryNumber());

            mockMvc.perform(post("/api/v1/inquiries/lookup")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(inquiry.getId()))
                    .andExpect(jsonPath("$.title").value("비회원 문의"));
        }
    }

    // ==================== 인증 실패 ====================

    @Nested
    @DisplayName("비회원 문의 조회 인증 실패")
    class GuestLookupAuthFailureTest {

        @Test
        @DisplayName("잘못된 비밀번호로 비회원 문의 조회 -> 401")
        void lookupGuestInquiry_WrongPassword_Returns401() throws Exception {
            InquiryCreateResponse inquiry = createTestGuestInquiry();

            String requestBody = String.format("""
                    {
                        "inquiryNumber": "%s",
                        "email": "guest@test.com",
                        "password": "wrongpassword"
                    }
                    """, inquiry.getInquiryNumber());

            mockMvc.perform(post("/api/v1/inquiries/lookup")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("잘못된 이메일로 비회원 문의 조회 -> 404")
        void lookupGuestInquiry_WrongEmail_Returns404() throws Exception {
            InquiryCreateResponse inquiry = createTestGuestInquiry();

            String requestBody = String.format("""
                    {
                        "inquiryNumber": "%s",
                        "email": "wrong@test.com",
                        "password": "password123"
                    }
                    """, inquiry.getInquiryNumber());

            mockMvc.perform(post("/api/v1/inquiries/lookup")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound());
        }
    }
}
