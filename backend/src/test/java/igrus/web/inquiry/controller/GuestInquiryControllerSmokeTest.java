package igrus.web.inquiry.controller;

import igrus.web.common.OpenApiValidatorUtil;
import igrus.web.common.ServiceIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GuestInquiryController OpenAPI 응답 스키마 스모크 테스트.
 *
 * <p>TC-213-05: POST /api/v1/inquiries/guest 응답이 OpenAPI 스키마와 일치하는지 검증한다.
 * 비회원 문의 생성 성공 시 응답(201 Created)의 스키마 정합성을 확인한다.</p>
 *
 * <p>비회원 문의 API는 인증 없이 접근 가능한 공개 API이다.</p>
 */
@AutoConfigureMockMvc
@DisplayName("GuestInquiryController OpenAPI 스모크 테스트")
class GuestInquiryControllerSmokeTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @DisplayName("POST /api/v1/inquiries/guest - 비회원 문의 생성 응답 스키마 검증 (201)")
    @Test
    void createGuestInquiry_ReturnsCreatedAndMatchesOpenApiSpec() throws Exception {
        String requestBody = """
                {
                    "type": "OTHER",
                    "title": "스모크 테스트 문의",
                    "content": "스모크 테스트용 문의 내용입니다.",
                    "email": "guest@example.com",
                    "name": "테스트방문자",
                    "password": "inquiry1234!"
                }
                """;

        mockMvc.perform(post("/api/v1/inquiries/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
    }
}
