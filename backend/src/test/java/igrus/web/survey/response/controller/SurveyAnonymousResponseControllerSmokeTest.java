package igrus.web.survey.response.controller;

import igrus.web.common.OpenApiValidatorUtil;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.question.domain.TextSurveyQuestion;
import igrus.web.survey.question.domain.SurveyQuestion;
import igrus.web.survey.question.domain.SurveyQuestionType;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
import igrus.web.survey.repository.SurveyRepository;
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
 * SurveyAnonymousResponseController OpenAPI 응답 스키마 스모크 테스트.
 *
 * <p>TC-213-13: POST /api/v1/surveys/{id}/responses/anonymous 응답이
 * OpenAPI 스키마와 일치하는지 검증한다.
 * 비회원 설문 응답 제출 성공 시 응답(201 Created)의 스키마 정합성을 확인한다.</p>
 *
 * <p>비회원 설문 응답 API는 인증 없이 접근 가능한 공개 API이다.
 * PUBLIC accessLevel의 공개 + 응답 수집 중인 설문이 필요하다.</p>
 */
@AutoConfigureMockMvc
@DisplayName("SurveyAnonymousResponseController OpenAPI 스모크 테스트")
class SurveyAnonymousResponseControllerSmokeTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyQuestionRepository surveyQuestionRepository;

    private Survey survey;
    private SurveyQuestion question;

    @BeforeEach
    void setUp() {
        setUpBase();

        survey = Survey.create("익명 응답 스모크 설문", "설명", SurveyAccessLevel.PUBLIC, null);
        survey.publishAndOpen();
        survey = surveyRepository.save(survey);

        question = surveyQuestionRepository.save(
                TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER, "단답형 질문", null, false, 1));
    }

    @DisplayName("POST /api/v1/surveys/{id}/responses/anonymous - 비회원 설문 응답 제출 응답 스키마 검증 (201)")
    @Test
    void submitAnonymousResponse_ReturnsCreatedAndMatchesOpenApiSpec() throws Exception {
        String requestBody = """
                {
                    "answers": [
                        {
                            "questionId": %d,
                            "textValue": "익명 테스트 답변"
                        }
                    ]
                }
                """.formatted(question.getId());

        mockMvc.perform(post("/api/v1/surveys/{surveyId}/responses/anonymous", survey.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
    }
}
