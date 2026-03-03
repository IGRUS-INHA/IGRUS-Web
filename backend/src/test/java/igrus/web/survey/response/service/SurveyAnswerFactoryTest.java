package igrus.web.survey.response.service;

import igrus.web.survey.domain.Survey;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.response.domain.SurveyResponse;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static igrus.web.common.fixture.SurveyTestFixture.*;
import static igrus.web.common.fixture.TestConstants.DEFAULT_SURVEY_ID;
import static igrus.web.common.fixture.TestEntityIdAssigner.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SurveyAnswerFactory 단위 테스트.
 *
 * <p>고전파(Classical) 방식으로 실제 도메인 객체를 사용합니다.
 * 각 질문 카테고리(TEXT, OPTION, SCALE, GRID)에 대한 답변 엔티티 생성을 검증합니다.
 */
@DisplayName("SurveyAnswerFactory 단위 테스트")
class SurveyAnswerFactoryTest {

    private SurveyAnswerFactory factory;

    @BeforeEach
    void setUp() {
        factory = new SurveyAnswerFactory();
    }

    // ==================== TEXT 유형 ====================

    @Test
    @DisplayName("TEXT 카테고리 질문 → TextSurveyAnswer 생성")
    void createAnswers_TextCategory_CreatesTextSurveyAnswer() {
        // given
        Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
        TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
        withId(question, 100L);
        survey.getQuestions().add(question);

        SurveyResponse response = createMockResponse(survey);

        List<SubmitAnswerRequest> answers = List.of(
                new SubmitAnswerRequest(100L, "텍스트 답변", null, null, null)
        );

        // when
        factory.createAnswers(response, survey, answers);

        // then
        assertThat(response.getAnswers()).hasSize(1);
        assertThat(response.getAnswers().get(0))
                .isInstanceOf(igrus.web.survey.response.domain.TextSurveyAnswer.class);
    }

    // ==================== OPTION 유형 ====================

    @Test
    @DisplayName("OPTION 카테고리 질문 → OptionSurveyAnswer 생성")
    void createAnswers_OptionCategory_CreatesOptionSurveyAnswer() {
        // given
        Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
        OptionSurveyQuestion question = createMultipleChoiceQuestion(survey, 1);
        withId(question, 200L);
        // 선택지에 ID 설정
        SurveyQuestionOption option = question.getOptions().get(0);
        withId(option, 301L);
        survey.getQuestions().add(question);

        SurveyResponse response = createMockResponse(survey);

        List<SubmitAnswerRequest> answers = List.of(
                new SubmitAnswerRequest(200L, null, List.of(301L), null, null)
        );

        // when
        factory.createAnswers(response, survey, answers);

        // then
        assertThat(response.getAnswers()).hasSize(1);
        assertThat(response.getAnswers().get(0))
                .isInstanceOf(igrus.web.survey.response.domain.OptionSurveyAnswer.class);
    }

    // ==================== SCALE 유형 ====================

    @Test
    @DisplayName("SCALE 카테고리 질문 → NumericSurveyAnswer 생성")
    void createAnswers_ScaleCategory_CreatesNumericSurveyAnswer() {
        // given
        Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
        LinearScaleSurveyQuestion question = createLinearScaleQuestion(survey, 1);
        withId(question, 300L);
        survey.getQuestions().add(question);

        SurveyResponse response = createMockResponse(survey);

        List<SubmitAnswerRequest> answers = List.of(
                new SubmitAnswerRequest(300L, null, null, 4, null)
        );

        // when
        factory.createAnswers(response, survey, answers);

        // then
        assertThat(response.getAnswers()).hasSize(1);
        assertThat(response.getAnswers().get(0))
                .isInstanceOf(igrus.web.survey.response.domain.NumericSurveyAnswer.class);
    }

    // ==================== GRID 유형 ====================

    @Test
    @DisplayName("GRID 카테고리 질문 → GridSurveyAnswer 생성")
    void createAnswers_GridCategory_CreatesGridSurveyAnswer() {
        // given
        Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
        GridSurveyQuestion question = createGridQuestion(survey, 1);
        withId(question, 400L);
        // 선택지와 행에 ID 설정
        SurveyQuestionOption option = question.getOptions().get(0);
        withId(option, 401L);
        SurveyQuestionRow row = question.getRows().get(0);
        withId(row, 501L);
        survey.getQuestions().add(question);

        SurveyResponse response = createMockResponse(survey);

        List<SubmitAnswerRequest> answers = List.of(
                new SubmitAnswerRequest(400L, null, null, null,
                        List.of(new SubmitAnswerRequest.GridAnswerRequest(501L, List.of(401L))))
        );

        // when
        factory.createAnswers(response, survey, answers);

        // then
        assertThat(response.getAnswers()).hasSize(1);
        assertThat(response.getAnswers().get(0))
                .isInstanceOf(igrus.web.survey.response.domain.GridSurveyAnswer.class);
    }

    // ==================== 엣지 케이스 ====================

    @Test
    @DisplayName("존재하지 않는 questionId → 건너뛰기 (답변 생성 안됨)")
    void createAnswers_UnknownQuestionId_SkipsAnswer() {
        // given
        Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
        TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
        withId(question, 100L);
        survey.getQuestions().add(question);

        SurveyResponse response = createMockResponse(survey);

        List<SubmitAnswerRequest> answers = List.of(
                new SubmitAnswerRequest(999L, "답변", null, null, null) // 존재하지 않는 ID
        );

        // when
        factory.createAnswers(response, survey, answers);

        // then
        assertThat(response.getAnswers()).isEmpty();
    }

    @Test
    @DisplayName("SCALE 질문에 numericValue가 null이면 답변 생성 안됨")
    void createAnswers_ScaleWithNullNumeric_SkipsAnswer() {
        // given
        Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
        LinearScaleSurveyQuestion question = createLinearScaleQuestion(survey, 1);
        withId(question, 300L);
        survey.getQuestions().add(question);

        SurveyResponse response = createMockResponse(survey);

        List<SubmitAnswerRequest> answers = List.of(
                new SubmitAnswerRequest(300L, null, null, null, null) // numericValue 없음
        );

        // when
        factory.createAnswers(response, survey, answers);

        // then
        assertThat(response.getAnswers()).isEmpty();
    }

    @Test
    @DisplayName("삭제된 질문은 questionMap에서 제외됨")
    void createAnswers_DeletedQuestion_Excluded() {
        // given
        Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
        TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
        withId(question, 100L);
        ReflectionTestUtils.setField(question, "deleted", true); // 질문 삭제
        survey.getQuestions().add(question);

        SurveyResponse response = createMockResponse(survey);

        List<SubmitAnswerRequest> answers = List.of(
                new SubmitAnswerRequest(100L, "답변", null, null, null)
        );

        // when
        factory.createAnswers(response, survey, answers);

        // then
        assertThat(response.getAnswers()).isEmpty();
    }

    // ==================== 헬퍼 메서드 ====================

    private SurveyResponse createMockResponse(Survey survey) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        return SurveyResponse.create(survey, user);
    }
}
