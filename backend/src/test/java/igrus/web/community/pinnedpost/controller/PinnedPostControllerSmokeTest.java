package igrus.web.community.pinnedpost.controller;

import igrus.web.common.OpenApiValidatorUtil;
import igrus.web.common.ServiceIntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PinnedPostController OpenAPI 응답 스키마 스모크 테스트.
 *
 * <p>TC-213-01: GET /api/v1/pinned-posts 응답이 OpenAPI 스키마와 일치하는지 검증한다.
 * 고정 게시글이 없는 상태에서 빈 배열 응답(200 OK)의 스키마 정합성을 확인한다.</p>
 *
 * <p>GET /api/v1/pinned-posts는 인증 없이 접근 가능한 공개 API이다.</p>
 */
@AutoConfigureMockMvc
@DisplayName("PinnedPostController OpenAPI 스모크 테스트")
class PinnedPostControllerSmokeTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @DisplayName("GET /api/v1/pinned-posts - 빈 고정 게시글 목록 응답 스키마 검증 (200)")
    @Test
    void getPinnedPostList_WhenEmpty_ReturnsOkAndMatchesOpenApiSpec() throws Exception {
        mockMvc.perform(get("/api/v1/pinned-posts")
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
    }
}
