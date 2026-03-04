package igrus.web.user.semester.controller;

import igrus.web.common.OpenApiValidatorUtil;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SemesterMemberController OpenAPI 응답 스키마 스모크 테스트.
 *
 * <p>TC-213-15a: GET /api/v1/semesters 응답이 OpenAPI 스키마와 일치하는지 검증한다.
 * 학기 데이터가 없는 상태에서 빈 배열 응답(200 OK)의 스키마 정합성을 확인한다.</p>
 *
 * <p>OPERATOR 이상 권한이 필요한 API이다.</p>
 */
@AutoConfigureMockMvc
@DisplayName("SemesterMemberController OpenAPI 스모크 테스트")
class SemesterMemberControllerSmokeTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private User operator;

    @BeforeEach
    void setUp() {
        setUpBase();
        operator = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);
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

    @DisplayName("GET /api/v1/semesters - 빈 학기 목록 응답 스키마 검증 (200)")
    @Test
    void getSemesterList_WhenEmpty_ReturnsOkAndMatchesOpenApiSpec() throws Exception {
        mockMvc.perform(get("/api/v1/semesters")
                        .with(withAuth(operator))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
    }
}
