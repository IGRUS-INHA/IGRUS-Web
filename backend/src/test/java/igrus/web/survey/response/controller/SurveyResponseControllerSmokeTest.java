package igrus.web.survey.response.controller;

import igrus.web.common.OpenApiValidatorUtil;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.question.domain.TextSurveyQuestion;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SurveyResponseController OpenAPI 응답 스키마 스모크 테스트.
 *
 * <p>TC-213-12: POST /api/v1/surveys/{id}/responses 응답이 OpenAPI 스키마와 일치하는지 검증한다.
 * 설문 응답 제출 성공 시 응답(201 Created)의 스키마 정합성을 확인한다.</p>
 *
 * <p>설문 응답 제출을 위해 공개 + 응답 수집 중인 설문과 단답형 질문을 사전 생성한다.</p>
 */
@AutoConfigureMockMvc
@DisplayName("SurveyResponseController OpenAPI 스모크 테스트")
class SurveyResponseControllerSmokeTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private SurveyQuestionRepository surveyQuestionRepository;

    private User member;
    private Survey survey;
    private SurveyQuestion question;

    @BeforeEach
    void setUp() {
        setUpBase();
        member = createAndSaveUser("20230001", "member@inha.edu", UserRole.MEMBER);

        survey = Survey.create("응답 스모크 설문", "설명", SurveyAccessLevel.ASSOCIATE, null);
        survey.publishAndOpen();
        survey = surveyRepository.save(survey);

        question = surveyQuestionRepository.save(
                TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER, "단답형 질문", null, false, 1));
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

    @DisplayName("POST /api/v1/surveys/{id}/responses - 설문 응답 제출 응답 스키마 검증 (201)")
    @Test
    void submitResponse_ReturnsCreatedAndMatchesOpenApiSpec() throws Exception {
        String requestBody = """
                {
                    "answers": [
                        {
                            "questionId": %d,
                            "textValue": "테스트 답변"
                        }
                    ]
                }
                """.formatted(question.getId());

        mockMvc.perform(post("/api/v1/surveys/{surveyId}/responses", survey.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(withAuth(member))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(OpenApiValidatorUtil.matchesOpenApiSpec());
    }
}
