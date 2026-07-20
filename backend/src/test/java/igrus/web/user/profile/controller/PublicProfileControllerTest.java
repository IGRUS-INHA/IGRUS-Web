package igrus.web.user.profile.controller;

import igrus.web.common.OpenApiValidatorUtil;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.user.domain.ProfileLink;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PublicProfileController 통합 테스트 — 인증 없이 공개 프로필을 조회한다.
 * 닉네임이 있으면 실명이 응답에 노출되지 않아야 한다 (서버단 폴백).
 */
@AutoConfigureMockMvc
@DisplayName("PublicProfileController 통합 테스트")
class PublicProfileControllerTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    private User member;

    @BeforeEach
    void setUp() {
        setUpBase();
        member = createAndSaveUser("20230001", "member@inha.edu", UserRole.MEMBER);
    }

    @DisplayName("GET /api/v1/users/{studentId}/public-profile - 닉네임이 있으면 닉네임만 노출되고 실명은 응답에 없다 (200)")
    @Test
    void getPublicProfile_WithNickname_HidesRealName() throws Exception {
        member.updatePublicProfile("닉네임임", "소개글", List.of(new ProfileLink("github", "https://github.com/u")));
        userRepository.save(member);

        mockMvc.perform(get("/api/v1/users/{studentId}/public-profile", member.getStudentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("닉네임임"))
                .andExpect(jsonPath("$.introduction").value("소개글"))
                .andExpect(jsonPath("$.links[0].label").value("github"))
                .andExpect(content().string(not(containsString(member.getName()))))
                .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
    }

    @DisplayName("GET /api/v1/users/{studentId}/public-profile - 닉네임이 없으면 이름으로 폴백된다 (200)")
    @Test
    void getPublicProfile_WithoutNickname_FallsBackToName() throws Exception {
        mockMvc.perform(get("/api/v1/users/{studentId}/public-profile", member.getStudentId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value(member.getName()))
                .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
    }

    @DisplayName("GET /api/v1/users/{studentId}/public-profile - 없는 학번이면 404")
    @Test
    void getPublicProfile_WhenUserNotFound_Returns404() throws Exception {
        mockMvc.perform(get("/api/v1/users/{studentId}/public-profile", "99999999"))
                .andExpect(status().isNotFound());
    }
}
