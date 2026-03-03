package igrus.web.survey.question.controller;

import igrus.web.common.OpenApiValidatorUtil;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.question.domain.GridSurveyQuestion;
import igrus.web.survey.question.domain.SurveyQuestion;
import igrus.web.survey.question.domain.SurveyQuestionType;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
import igrus.web.survey.repository.SurveyRepository;
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
 * SurveyQuestionRowController OpenAPI 응답 스키마 스모크 테스트.
 *
 * <p>TC-213-11: GET /api/v1/surveys/{surveyId}/questions/{questionId}/rows 응답이
 * OpenAPI 스키마와 일치하는지 검증한다.
 * 행이 없는 그리드 질문에서 빈 배열 응답(200 OK)의 스키마 정합성을 확인한다.</p>
 */
@AutoConfigureMockMvc
@DisplayName("SurveyQuestionRowController OpenAPI 스모크 테스트")
class SurveyQuestionRowControllerSmokeTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyQuestionRepository surveyQuestionRepository;

    private User operator;
    private Survey survey;
    private SurveyQuestion gridQuestion;

    @BeforeEach
    void setUp() {
        setUpBase();
        operator = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);
        survey = surveyRepository.save(Survey.create("테스트 설문", "설명", SurveyAccessLevel.PUBLIC, null));
        gridQuestion = surveyQuestionRepository.save(
                GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "그리드 질문", null, false, 1));
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

    @DisplayName("GET /api/v1/surveys/{surveyId}/questions/{questionId}/rows - 빈 행 목록 응답 스키마 검증 (200)")
    @Test
    void getRowList_WhenEmpty_ReturnsOkAndMatchesOpenApiSpec() throws Exception {
        mockMvc.perform(get("/api/v1/surveys/{surveyId}/questions/{questionId}/rows",
                        survey.getId(), gridQuestion.getId())
                        .with(withAuth(operator))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
    }
}
