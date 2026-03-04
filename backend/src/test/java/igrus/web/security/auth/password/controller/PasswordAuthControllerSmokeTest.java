package igrus.web.security.auth.password.controller;

import igrus.web.common.OpenApiValidatorUtil;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PasswordAuthController OpenAPI 응답 스키마 스모크 테스트.
 *
 * <p>TC-213-02: POST /api/v1/auth/password/login 응답이 OpenAPI 스키마와 일치하는지 검증한다.
 * 로그인 성공 시 응답 스키마(Login200Response)의 정합성을 확인한다.</p>
 *
 * <p>POST /api/v1/auth/password/** 는 인증 없이 접근 가능한 공개 API이다.</p>
 */
@AutoConfigureMockMvc
@DisplayName("PasswordAuthController OpenAPI 스모크 테스트")
class PasswordAuthControllerSmokeTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private static final String PASSWORD = "Test1234!@";

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @DisplayName("POST /api/v1/auth/password/login - 로그인 성공 응답 스키마 검증 (200)")
    @Test
    void login_WithValidCredentials_ReturnsOkAndMatchesOpenApiSpec() throws Exception {
        // given
        User user = createAndSaveUser("20231001", "smoketest@inha.edu", UserRole.MEMBER);
        createAndSaveCredential(user, PASSWORD);

        String requestBody = """
                {
                    "studentId": "20231001",
                    "password": "%s"
                }
                """.formatted(PASSWORD);

        // when & then
        mockMvc.perform(post("/api/v1/auth/password/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
    }

    @DisplayName("GET /api/v1/auth/password/check-student-id - 학번 중복 확인 응답 스키마 검증 (200)")
    @Test
    void checkStudentIdDuplicate_ReturnsOkAndMatchesOpenApiSpec() throws Exception {
        mockMvc.perform(get("/api/v1/auth/password/check-student-id")
                        .param("studentId", "99999999")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
    }
}
