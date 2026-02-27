package igrus.web.survey.statistics.service;

import igrus.web.survey.domain.Survey;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.question.repository.SurveyQuestionRepository;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.domain.*;
import igrus.web.survey.response.repository.SurveyAnswerRepository;
import igrus.web.survey.response.repository.SurveyResponseRepository;
import igrus.web.survey.statistics.dto.response.*;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static igrus.web.common.fixture.SurveyTestFixture.*;
import static igrus.web.common.fixture.TestConstants.*;
import static igrus.web.common.fixture.TestEntityIdAssigner.withId;
import static igrus.web.common.fixture.UserTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * SurveyStatisticsService 단위 테스트.
 *
 * <p>런던파(Mockist) 방식으로 외부 의존성(Repository)을 Mock 처리하고,
 * 도메인 객체는 실제 객체를 사용합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SurveyStatisticsService 단위 테스트")
class SurveyStatisticsServiceTest {

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private SurveyResponseRepository surveyResponseRepository;

    @Mock
    private SurveyAnswerRepository surveyAnswerRepository;

    @Mock
    private SurveyQuestionRepository surveyQuestionRepository;

    @InjectMocks
    private SurveyStatisticsService surveyStatisticsService;

    private Survey survey;
    private User operator;

    @BeforeEach
    void setUp() {
        survey = createSurveyWithId();
        operator = createOperatorWithId();
    }

    // ==================== 헬퍼 메서드 ====================

    /**
     * SurveyResponse를 생성하고 ID와 createdAt을 설정합니다.
     */
    private SurveyResponse createResponseWithIdAndCreatedAt(Long id, Instant createdAt) {
        SurveyResponse response = SurveyResponse.create(survey, operator);
        withId(response, id);
        ReflectionTestUtils.setField(response, "createdAt", createdAt);
        return response;
    }

    /**
     * 공통 Mock 설정: survey, responses, questions, answers
     */
    private void setUpMocks(List<SurveyResponse> responses,
                            List<SurveyQuestion> questions,
                            List<SurveyAnswer> answers) {
        given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID))
                .willReturn(Optional.of(survey));
        given(surveyResponseRepository.findBySurveyIdAndDeletedFalseOrderByCreatedAtAsc(DEFAULT_SURVEY_ID))
                .willReturn(responses);
        given(surveyQuestionRepository.findAllBySurveyIdWithOptions(DEFAULT_SURVEY_ID))
                .willReturn(questions);
        given(surveyQuestionRepository.findAllBySurveyIdWithRows(DEFAULT_SURVEY_ID))
                .willReturn(questions);
        given(surveyAnswerRepository.findValidAnswersBySurveyId(DEFAULT_SURVEY_ID))
                .willReturn(answers);
    }

    // ==================== TEXT 카테고리 테스트 ====================

    @Nested
    @DisplayName("TEXT 카테고리 통계")
    class TextCategoryStatistics {

        @DisplayName("TC-STAT-010: SHORT_ANSWER 3건 응답 시 responseCount=3")
        @Test
        void getSurveyStatistics_WithShortAnswerResponses_ReturnsCorrectResponseCount() {
            // given
            TextSurveyQuestion question = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.SHORT_ANSWER, "단답형 질문", null, true, 1);
            withId(question, 100L);

            Instant time1 = Instant.parse("2026-02-01T00:00:00Z");
            Instant time2 = Instant.parse("2026-02-02T00:00:00Z");
            Instant time3 = Instant.parse("2026-02-03T00:00:00Z");

            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, time1);
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, time2);
            SurveyResponse r3 = createResponseWithIdAndCreatedAt(3L, time3);

            List<SurveyAnswer> answers = List.of(
                    TextSurveyAnswer.create(r1, question, "답변1"),
                    TextSurveyAnswer.create(r2, question, "답변2"),
                    TextSurveyAnswer.create(r3, question, "답변3")
            );

            setUpMocks(List.of(r1, r2, r3), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            QuestionStatisticsResponse qStat = result.questionStatistics().getFirst();
            assertThat(qStat.responseCount()).isEqualTo(3);
            assertThat(qStat.questionType()).isEqualTo(SurveyQuestionType.SHORT_ANSWER);
        }

        @DisplayName("TC-STAT-011: 텍스트 응답 목록 반환 + createdAt 오름차순 정렬")
        @Test
        void getSurveyStatistics_WithParagraphResponses_ReturnsTextListInCreatedAtOrder() {
            // given
            TextSurveyQuestion question = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.PARAGRAPH, "서술형 질문", null, true, 1);
            withId(question, 100L);

            Instant time1 = Instant.parse("2026-02-01T00:00:00Z");
            Instant time2 = Instant.parse("2026-02-02T00:00:00Z");
            Instant time3 = Instant.parse("2026-02-03T00:00:00Z");

            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, time1);
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, time2);
            SurveyResponse r3 = createResponseWithIdAndCreatedAt(3L, time3);

            // 의도적으로 순서를 섞어서 답변 목록 생성
            List<SurveyAnswer> answers = List.of(
                    TextSurveyAnswer.create(r3, question, "세번째 응답"),
                    TextSurveyAnswer.create(r1, question, "첫번째 응답"),
                    TextSurveyAnswer.create(r2, question, "두번째 응답")
            );

            setUpMocks(List.of(r1, r2, r3), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            TextQuestionStatistics textStats = result.questionStatistics().getFirst().textStatistics();
            assertThat(textStats).isNotNull();
            assertThat(textStats.textResponses()).hasSize(3);
            assertThat(textStats.textResponses()).containsExactly(
                    "첫번째 응답", "두번째 응답", "세번째 응답");
        }

        @DisplayName("TC-STAT-012: DATE 유형도 TEXT 카테고리 통계 구조 적용")
        @Test
        void getSurveyStatistics_WithDateResponses_ReturnsTextCategoryStructure() {
            // given
            TextSurveyQuestion question = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.DATE, "날짜 질문", null, true, 1);
            withId(question, 100L);

            Instant time1 = Instant.parse("2026-02-01T00:00:00Z");
            Instant time2 = Instant.parse("2026-02-02T00:00:00Z");

            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, time1);
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, time2);

            List<SurveyAnswer> answers = List.of(
                    TextSurveyAnswer.create(r1, question, "2026-02-01"),
                    TextSurveyAnswer.create(r2, question, "2026-02-15")
            );

            setUpMocks(List.of(r1, r2), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            QuestionStatisticsResponse qStat = result.questionStatistics().getFirst();
            assertThat(qStat.responseCount()).isEqualTo(2);
            assertThat(qStat.questionType()).isEqualTo(SurveyQuestionType.DATE);
            assertThat(qStat.textStatistics()).isNotNull();
            assertThat(qStat.textStatistics().textResponses()).hasSize(2);
            assertThat(qStat.textStatistics().textResponses()).containsExactly(
                    "2026-02-01", "2026-02-15");
            // TEXT 구조이므로 다른 통계 필드는 null
            assertThat(qStat.scaleStatistics()).isNull();
            assertThat(qStat.optionStatistics()).isNull();
            assertThat(qStat.gridStatistics()).isNull();
        }

        @DisplayName("TC-STAT-013: FILE_UPLOAD URL 문자열 목록 반환")
        @Test
        void getSurveyStatistics_WithFileUploadResponses_ReturnsUrlTextList() {
            // given
            TextSurveyQuestion question = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.FILE_UPLOAD, "파일 업로드 질문", null, true, 1);
            withId(question, 100L);

            Instant time1 = Instant.parse("2026-02-01T00:00:00Z");
            Instant time2 = Instant.parse("2026-02-02T00:00:00Z");

            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, time1);
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, time2);

            List<SurveyAnswer> answers = List.of(
                    TextSurveyAnswer.create(r1, question, "https://example.com/file1.pdf"),
                    TextSurveyAnswer.create(r2, question, "https://example.com/file2.pdf")
            );

            setUpMocks(List.of(r1, r2), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            TextQuestionStatistics textStats = result.questionStatistics().getFirst().textStatistics();
            assertThat(textStats).isNotNull();
            assertThat(textStats.textResponses()).containsExactly(
                    "https://example.com/file1.pdf",
                    "https://example.com/file2.pdf"
            );
        }
    }

    // ==================== SCALE 카테고리 테스트 ====================

    @Nested
    @DisplayName("SCALE 카테고리 통계")
    class ScaleCategoryStatistics {

        @DisplayName("TC-STAT-020: LINEAR_SCALE 응답 수 정확성 ([1,2,3,4,5] -> 5)")
        @Test
        void getSurveyStatistics_WithLinearScaleResponses_ReturnsCorrectResponseCount() {
            // given
            LinearScaleSurveyQuestion question = LinearScaleSurveyQuestion.create(
                    survey, SurveyQuestionType.LINEAR_SCALE, "만족도", null, true, 1);
            question.setScaleRange(1, 5);
            withId(question, 100L);

            int[] values = {1, 2, 3, 4, 5};
            List<SurveyResponse> responses = new ArrayList<>();
            List<SurveyAnswer> answers = new ArrayList<>();

            for (int i = 0; i < values.length; i++) {
                SurveyResponse r = createResponseWithIdAndCreatedAt(
                        (long) (i + 1), Instant.parse("2026-02-0" + (i + 1) + "T00:00:00Z"));
                responses.add(r);
                answers.add(NumericSurveyAnswer.create(r, question, values[i]));
            }

            setUpMocks(responses, List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            QuestionStatisticsResponse qStat = result.questionStatistics().getFirst();
            assertThat(qStat.responseCount()).isEqualTo(5);
        }

        @DisplayName("TC-STAT-021: LINEAR_SCALE 평균값 정확성 (나누어 떨어지는 경우, 3.0)")
        @Test
        void getSurveyStatistics_WithLinearScaleResponses_ReturnsCorrectAverage() {
            // given
            LinearScaleSurveyQuestion question = LinearScaleSurveyQuestion.create(
                    survey, SurveyQuestionType.LINEAR_SCALE, "만족도", null, true, 1);
            question.setScaleRange(1, 5);
            withId(question, 100L);

            int[] values = {1, 2, 3, 4, 5};
            List<SurveyResponse> responses = new ArrayList<>();
            List<SurveyAnswer> answers = new ArrayList<>();

            for (int i = 0; i < values.length; i++) {
                SurveyResponse r = createResponseWithIdAndCreatedAt(
                        (long) (i + 1), Instant.parse("2026-02-0" + (i + 1) + "T00:00:00Z"));
                responses.add(r);
                answers.add(NumericSurveyAnswer.create(r, question, values[i]));
            }

            setUpMocks(responses, List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            ScaleQuestionStatistics scaleStats = result.questionStatistics().getFirst().scaleStatistics();
            assertThat(scaleStats).isNotNull();
            assertThat(scaleStats.average()).isEqualByComparingTo(new BigDecimal("3.0"));
        }

        @DisplayName("TC-STAT-022: LINEAR_SCALE 최솟값/최댓값 정확성 ([2,5,8] -> min=2, max=8)")
        @Test
        void getSurveyStatistics_WithLinearScaleResponses_ReturnsCorrectMinMax() {
            // given
            LinearScaleSurveyQuestion question = LinearScaleSurveyQuestion.create(
                    survey, SurveyQuestionType.LINEAR_SCALE, "만족도", null, true, 1);
            question.setScaleRange(1, 10);
            withId(question, 100L);

            int[] values = {2, 5, 8};
            List<SurveyResponse> responses = new ArrayList<>();
            List<SurveyAnswer> answers = new ArrayList<>();

            for (int i = 0; i < values.length; i++) {
                SurveyResponse r = createResponseWithIdAndCreatedAt(
                        (long) (i + 1), Instant.parse("2026-02-0" + (i + 1) + "T00:00:00Z"));
                responses.add(r);
                answers.add(NumericSurveyAnswer.create(r, question, values[i]));
            }

            setUpMocks(responses, List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            ScaleQuestionStatistics scaleStats = result.questionStatistics().getFirst().scaleStatistics();
            assertThat(scaleStats).isNotNull();
            assertThat(scaleStats.min()).isEqualTo(2);
            assertThat(scaleStats.max()).isEqualTo(8);
        }

        @DisplayName("TC-STAT-023: LINEAR_SCALE 값별 분포 히스토그램 ([1,1,2,3,3,3,5] -> {1:2,2:1,3:3,4:0,5:1})")
        @Test
        void getSurveyStatistics_WithLinearScaleResponses_ReturnsCorrectDistribution() {
            // given
            LinearScaleSurveyQuestion question = LinearScaleSurveyQuestion.create(
                    survey, SurveyQuestionType.LINEAR_SCALE, "만족도", null, true, 1);
            question.setScaleRange(1, 5);
            withId(question, 100L);

            int[] values = {1, 1, 2, 3, 3, 3, 5};
            List<SurveyResponse> responses = new ArrayList<>();
            List<SurveyAnswer> answers = new ArrayList<>();

            for (int i = 0; i < values.length; i++) {
                SurveyResponse r = createResponseWithIdAndCreatedAt(
                        (long) (i + 1), Instant.parse("2026-02-0" + (i + 1) + "T00:00:00Z"));
                responses.add(r);
                answers.add(NumericSurveyAnswer.create(r, question, values[i]));
            }

            setUpMocks(responses, List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            ScaleQuestionStatistics scaleStats = result.questionStatistics().getFirst().scaleStatistics();
            assertThat(scaleStats).isNotNull();
            assertThat(scaleStats.distribution()).containsEntry(1, 2);
            assertThat(scaleStats.distribution()).containsEntry(2, 1);
            assertThat(scaleStats.distribution()).containsEntry(3, 3);
            assertThat(scaleStats.distribution()).containsEntry(4, 0);
            assertThat(scaleStats.distribution()).containsEntry(5, 1);
            assertThat(scaleStats.distribution()).hasSize(5);
        }

        @DisplayName("TC-STAT-024: LINEAR_SCALE 응답 1건일 때 average=min=max")
        @Test
        void getSurveyStatistics_WithSingleScaleResponse_ReturnsAverageEqualsMinEqualsMax() {
            // given
            LinearScaleSurveyQuestion question = LinearScaleSurveyQuestion.create(
                    survey, SurveyQuestionType.LINEAR_SCALE, "만족도", null, true, 1);
            question.setScaleRange(1, 5);
            withId(question, 100L);

            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            List<SurveyAnswer> answers = List.of(
                    NumericSurveyAnswer.create(r1, question, 3)
            );

            setUpMocks(List.of(r1), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            ScaleQuestionStatistics scaleStats = result.questionStatistics().getFirst().scaleStatistics();
            assertThat(scaleStats).isNotNull();
            assertThat(scaleStats.average()).isEqualByComparingTo(new BigDecimal("3.0"));
            assertThat(scaleStats.min()).isEqualTo(3);
            assertThat(scaleStats.max()).isEqualTo(3);
            assertThat(scaleStats.distribution()).containsEntry(1, 0);
            assertThat(scaleStats.distribution()).containsEntry(2, 0);
            assertThat(scaleStats.distribution()).containsEntry(3, 1);
            assertThat(scaleStats.distribution()).containsEntry(4, 0);
            assertThat(scaleStats.distribution()).containsEntry(5, 0);
        }

        @DisplayName("TC-STAT-025: LINEAR_SCALE 소수점 반올림 ([1,2,4] -> average=2.3, HALF_UP)")
        @Test
        void getSurveyStatistics_WithNonDivisibleAverage_ReturnsRoundedAverage() {
            // given
            LinearScaleSurveyQuestion question = LinearScaleSurveyQuestion.create(
                    survey, SurveyQuestionType.LINEAR_SCALE, "만족도", null, true, 1);
            question.setScaleRange(1, 5);
            withId(question, 100L);

            int[] values = {1, 2, 4};
            List<SurveyResponse> responses = new ArrayList<>();
            List<SurveyAnswer> answers = new ArrayList<>();

            for (int i = 0; i < values.length; i++) {
                SurveyResponse r = createResponseWithIdAndCreatedAt(
                        (long) (i + 1), Instant.parse("2026-02-0" + (i + 1) + "T00:00:00Z"));
                responses.add(r);
                answers.add(NumericSurveyAnswer.create(r, question, values[i]));
            }

            setUpMocks(responses, List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            ScaleQuestionStatistics scaleStats = result.questionStatistics().getFirst().scaleStatistics();
            assertThat(scaleStats).isNotNull();
            // 7/3 = 2.333... -> HALF_UP -> 2.3
            assertThat(scaleStats.average()).isEqualByComparingTo(new BigDecimal("2.3"));
        }
    }

    // ==================== OPTION 카테고리 테스트 ====================

    @Nested
    @DisplayName("OPTION 카테고리 통계")
    class OptionCategoryStatistics {

        @DisplayName("TC-STAT-030: MULTIPLE_CHOICE 옵션별 선택 수 (A=3, B=2, C=0)")
        @Test
        void getSurveyStatistics_WithMultipleChoiceResponses_ReturnsCorrectOptionCounts() {
            // given
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "객관식 질문", null, true, 1);
            withId(question, 100L);

            SurveyQuestionOption optA = SurveyQuestionOption.create(question, "A", 1);
            withId(optA, 201L);
            SurveyQuestionOption optB = SurveyQuestionOption.create(question, "B", 2);
            withId(optB, 202L);
            SurveyQuestionOption optC = SurveyQuestionOption.create(question, "C", 3);
            withId(optC, 203L);
            question.addOption(optA);
            question.addOption(optB);
            question.addOption(optC);

            // 5명 응답: A=3, B=2, C=0
            List<SurveyResponse> responses = new ArrayList<>();
            List<SurveyAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                SurveyResponse r = createResponseWithIdAndCreatedAt(
                        (long) (i + 1), Instant.parse("2026-02-0" + (i + 1) + "T00:00:00Z"));
                responses.add(r);
            }
            // 응답자1,2,3 -> A
            answers.add(OptionSurveyAnswer.create(responses.get(0), question, optA));
            answers.add(OptionSurveyAnswer.create(responses.get(1), question, optA));
            answers.add(OptionSurveyAnswer.create(responses.get(2), question, optA));
            // 응답자4,5 -> B
            answers.add(OptionSurveyAnswer.create(responses.get(3), question, optB));
            answers.add(OptionSurveyAnswer.create(responses.get(4), question, optB));

            setUpMocks(responses, List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            OptionQuestionStatistics optionStats = result.questionStatistics().getFirst().optionStatistics();
            assertThat(optionStats).isNotNull();
            assertThat(optionStats.options()).hasSize(3);

            OptionStatisticsItem itemA = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(201L)).findFirst().orElseThrow();
            OptionStatisticsItem itemB = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(202L)).findFirst().orElseThrow();
            OptionStatisticsItem itemC = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(203L)).findFirst().orElseThrow();

            assertThat(itemA.count()).isEqualTo(3);
            assertThat(itemB.count()).isEqualTo(2);
            assertThat(itemC.count()).isEqualTo(0);
        }

        @DisplayName("TC-STAT-031: MULTIPLE_CHOICE 옵션별 비율 합계 100%")
        @Test
        void getSurveyStatistics_WithMultipleChoiceResponses_ReturnsPercentageSumOf100() {
            // given
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "객관식 질문", null, true, 1);
            withId(question, 100L);

            SurveyQuestionOption optA = SurveyQuestionOption.create(question, "A", 1);
            withId(optA, 201L);
            SurveyQuestionOption optB = SurveyQuestionOption.create(question, "B", 2);
            withId(optB, 202L);
            SurveyQuestionOption optC = SurveyQuestionOption.create(question, "C", 3);
            withId(optC, 203L);
            question.addOption(optA);
            question.addOption(optB);
            question.addOption(optC);

            // 5명 응답: A=3, B=2, C=0
            List<SurveyResponse> responses = new ArrayList<>();
            List<SurveyAnswer> answers = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                SurveyResponse r = createResponseWithIdAndCreatedAt(
                        (long) (i + 1), Instant.parse("2026-02-0" + (i + 1) + "T00:00:00Z"));
                responses.add(r);
            }
            answers.add(OptionSurveyAnswer.create(responses.get(0), question, optA));
            answers.add(OptionSurveyAnswer.create(responses.get(1), question, optA));
            answers.add(OptionSurveyAnswer.create(responses.get(2), question, optA));
            answers.add(OptionSurveyAnswer.create(responses.get(3), question, optB));
            answers.add(OptionSurveyAnswer.create(responses.get(4), question, optB));

            setUpMocks(responses, List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            OptionQuestionStatistics optionStats = result.questionStatistics().getFirst().optionStatistics();
            assertThat(optionStats).isNotNull();

            OptionStatisticsItem itemA = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(201L)).findFirst().orElseThrow();
            OptionStatisticsItem itemB = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(202L)).findFirst().orElseThrow();
            OptionStatisticsItem itemC = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(203L)).findFirst().orElseThrow();

            // A=60.0%, B=40.0%, C=0.0%
            assertThat(itemA.percentage()).isEqualByComparingTo(new BigDecimal("60.0"));
            assertThat(itemB.percentage()).isEqualByComparingTo(new BigDecimal("40.0"));
            assertThat(itemC.percentage()).isEqualByComparingTo(new BigDecimal("0.0"));

            // 합계 100%
            BigDecimal percentageSum = optionStats.options().stream()
                    .map(OptionStatisticsItem::percentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(percentageSum).isEqualByComparingTo(new BigDecimal("100.0"));
        }

        @DisplayName("TC-STAT-032: DROPDOWN 옵션별 선택 수/비율 정확성")
        @Test
        void getSurveyStatistics_WithDropdownResponses_ReturnsCorrectCountsAndPercentages() {
            // given
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.DROPDOWN, "드롭다운 질문", null, true, 1);
            withId(question, 100L);

            SurveyQuestionOption optX = SurveyQuestionOption.create(question, "X", 1);
            withId(optX, 201L);
            SurveyQuestionOption optY = SurveyQuestionOption.create(question, "Y", 2);
            withId(optY, 202L);
            SurveyQuestionOption optZ = SurveyQuestionOption.create(question, "Z", 3);
            withId(optZ, 203L);
            question.addOption(optX);
            question.addOption(optY);
            question.addOption(optZ);

            // 3명 응답: X=2, Y=1, Z=0
            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-02T00:00:00Z"));
            SurveyResponse r3 = createResponseWithIdAndCreatedAt(3L, Instant.parse("2026-02-03T00:00:00Z"));

            List<SurveyAnswer> answers = List.of(
                    OptionSurveyAnswer.create(r1, question, optX),
                    OptionSurveyAnswer.create(r2, question, optX),
                    OptionSurveyAnswer.create(r3, question, optY)
            );

            setUpMocks(List.of(r1, r2, r3), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            OptionQuestionStatistics optionStats = result.questionStatistics().getFirst().optionStatistics();
            assertThat(optionStats).isNotNull();

            OptionStatisticsItem itemX = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(201L)).findFirst().orElseThrow();
            OptionStatisticsItem itemY = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(202L)).findFirst().orElseThrow();
            OptionStatisticsItem itemZ = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(203L)).findFirst().orElseThrow();

            assertThat(itemX.count()).isEqualTo(2);
            assertThat(itemY.count()).isEqualTo(1);
            assertThat(itemZ.count()).isEqualTo(0);

            // X=66.7%, Y=33.3%, Z=0.0%
            assertThat(itemX.percentage()).isEqualByComparingTo(new BigDecimal("66.7"));
            assertThat(itemY.percentage()).isEqualByComparingTo(new BigDecimal("33.3"));
            assertThat(itemZ.percentage()).isEqualByComparingTo(new BigDecimal("0.0"));
        }

        @DisplayName("TC-STAT-033: 미선택 옵션도 통계에 0으로 포함")
        @Test
        void getSurveyStatistics_WithUnselectedOptions_IncludesZeroCounts() {
            // given
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "객관식 질문", null, true, 1);
            withId(question, 100L);

            SurveyQuestionOption optA = SurveyQuestionOption.create(question, "A", 1);
            withId(optA, 201L);
            SurveyQuestionOption optB = SurveyQuestionOption.create(question, "B", 2);
            withId(optB, 202L);
            SurveyQuestionOption optC = SurveyQuestionOption.create(question, "C", 3);
            withId(optC, 203L);
            question.addOption(optA);
            question.addOption(optB);
            question.addOption(optC);

            // 2명 응답: A=1, B=1, C=0
            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-02T00:00:00Z"));

            List<SurveyAnswer> answers = List.of(
                    OptionSurveyAnswer.create(r1, question, optA),
                    OptionSurveyAnswer.create(r2, question, optB)
            );

            setUpMocks(List.of(r1, r2), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            OptionQuestionStatistics optionStats = result.questionStatistics().getFirst().optionStatistics();
            assertThat(optionStats).isNotNull();
            assertThat(optionStats.options()).hasSize(3);

            OptionStatisticsItem itemC = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(203L)).findFirst().orElseThrow();
            assertThat(itemC.count()).isEqualTo(0);
            assertThat(itemC.percentage()).isEqualByComparingTo(new BigDecimal("0.0"));
        }
    }

    // ==================== CHECKBOX 카테고리 테스트 ====================

    @Nested
    @DisplayName("CHECKBOX 카테고리 통계")
    class CheckboxCategoryStatistics {

        @DisplayName("TC-STAT-040: CHECKBOX 옵션별 선택 수 (A=2, B=2, C=1)")
        @Test
        void getSurveyStatistics_WithCheckboxResponses_ReturnsCorrectOptionCounts() {
            // given
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX, "체크박스 질문", null, true, 1);
            withId(question, 100L);

            SurveyQuestionOption optA = SurveyQuestionOption.create(question, "A", 1);
            withId(optA, 201L);
            SurveyQuestionOption optB = SurveyQuestionOption.create(question, "B", 2);
            withId(optB, 202L);
            SurveyQuestionOption optC = SurveyQuestionOption.create(question, "C", 3);
            withId(optC, 203L);
            question.addOption(optA);
            question.addOption(optB);
            question.addOption(optC);

            // 응답자1: A,B / 응답자2: B,C / 응답자3: A
            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-02T00:00:00Z"));
            SurveyResponse r3 = createResponseWithIdAndCreatedAt(3L, Instant.parse("2026-02-03T00:00:00Z"));

            List<SurveyAnswer> answers = List.of(
                    OptionSurveyAnswer.create(r1, question, optA),
                    OptionSurveyAnswer.create(r1, question, optB),
                    OptionSurveyAnswer.create(r2, question, optB),
                    OptionSurveyAnswer.create(r2, question, optC),
                    OptionSurveyAnswer.create(r3, question, optA)
            );

            setUpMocks(List.of(r1, r2, r3), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            OptionQuestionStatistics optionStats = result.questionStatistics().getFirst().optionStatistics();
            assertThat(optionStats).isNotNull();

            OptionStatisticsItem itemA = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(201L)).findFirst().orElseThrow();
            OptionStatisticsItem itemB = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(202L)).findFirst().orElseThrow();
            OptionStatisticsItem itemC = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(203L)).findFirst().orElseThrow();

            assertThat(itemA.count()).isEqualTo(2);
            assertThat(itemB.count()).isEqualTo(2);
            assertThat(itemC.count()).isEqualTo(1);
        }

        @DisplayName("TC-STAT-041: CHECKBOX 비율 합계 > 100% (A=66.7%, B=66.7%, C=33.3%)")
        @Test
        void getSurveyStatistics_WithCheckboxResponses_ReturnsPercentageSumGreaterThan100() {
            // given
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX, "체크박스 질문", null, true, 1);
            withId(question, 100L);

            SurveyQuestionOption optA = SurveyQuestionOption.create(question, "A", 1);
            withId(optA, 201L);
            SurveyQuestionOption optB = SurveyQuestionOption.create(question, "B", 2);
            withId(optB, 202L);
            SurveyQuestionOption optC = SurveyQuestionOption.create(question, "C", 3);
            withId(optC, 203L);
            question.addOption(optA);
            question.addOption(optB);
            question.addOption(optC);

            // 응답자1: A,B / 응답자2: B,C / 응답자3: A
            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-02T00:00:00Z"));
            SurveyResponse r3 = createResponseWithIdAndCreatedAt(3L, Instant.parse("2026-02-03T00:00:00Z"));

            List<SurveyAnswer> answers = List.of(
                    OptionSurveyAnswer.create(r1, question, optA),
                    OptionSurveyAnswer.create(r1, question, optB),
                    OptionSurveyAnswer.create(r2, question, optB),
                    OptionSurveyAnswer.create(r2, question, optC),
                    OptionSurveyAnswer.create(r3, question, optA)
            );

            setUpMocks(List.of(r1, r2, r3), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            OptionQuestionStatistics optionStats = result.questionStatistics().getFirst().optionStatistics();
            assertThat(optionStats).isNotNull();

            OptionStatisticsItem itemA = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(201L)).findFirst().orElseThrow();
            OptionStatisticsItem itemB = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(202L)).findFirst().orElseThrow();
            OptionStatisticsItem itemC = optionStats.options().stream()
                    .filter(o -> o.optionId().equals(203L)).findFirst().orElseThrow();

            // 비율 = 해당 옵션 선택 수 / 전체 응답자 수(3)
            // A=2/3=66.7%, B=2/3=66.7%, C=1/3=33.3%
            assertThat(itemA.percentage()).isEqualByComparingTo(new BigDecimal("66.7"));
            assertThat(itemB.percentage()).isEqualByComparingTo(new BigDecimal("66.7"));
            assertThat(itemC.percentage()).isEqualByComparingTo(new BigDecimal("33.3"));

            // 합계 > 100%
            BigDecimal percentageSum = optionStats.options().stream()
                    .map(OptionStatisticsItem::percentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(percentageSum).isGreaterThan(new BigDecimal("100"));
        }

        @DisplayName("TC-STAT-042: CHECKBOX 총 응답자 수와 개별 선택 합계 구분 (responseCount=2, 선택 합계=6)")
        @Test
        void getSurveyStatistics_WithCheckboxResponses_DistinguishesResponseCountFromSelectionSum() {
            // given
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX, "체크박스 질문", null, true, 1);
            withId(question, 100L);

            SurveyQuestionOption optA = SurveyQuestionOption.create(question, "A", 1);
            withId(optA, 201L);
            SurveyQuestionOption optB = SurveyQuestionOption.create(question, "B", 2);
            withId(optB, 202L);
            SurveyQuestionOption optC = SurveyQuestionOption.create(question, "C", 3);
            withId(optC, 203L);
            SurveyQuestionOption optD = SurveyQuestionOption.create(question, "D", 4);
            withId(optD, 204L);
            question.addOption(optA);
            question.addOption(optB);
            question.addOption(optC);
            question.addOption(optD);

            // 응답자1: A,B,C / 응답자2: B,C,D -> 총 OptionSurveyAnswer 6건
            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-02T00:00:00Z"));

            List<SurveyAnswer> answers = List.of(
                    OptionSurveyAnswer.create(r1, question, optA),
                    OptionSurveyAnswer.create(r1, question, optB),
                    OptionSurveyAnswer.create(r1, question, optC),
                    OptionSurveyAnswer.create(r2, question, optB),
                    OptionSurveyAnswer.create(r2, question, optC),
                    OptionSurveyAnswer.create(r2, question, optD)
            );

            setUpMocks(List.of(r1, r2), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            QuestionStatisticsResponse qStat = result.questionStatistics().getFirst();
            // responseCount = 고유 응답자 수 = 2 (SurveyResponse 기준)
            assertThat(qStat.responseCount()).isEqualTo(2);

            // 옵션별 count 합계 = 6 (OptionSurveyAnswer 기준)
            int selectionSum = qStat.optionStatistics().options().stream()
                    .mapToInt(OptionStatisticsItem::count)
                    .sum();
            assertThat(selectionSum).isEqualTo(6);

            // responseCount != 선택 합계
            assertThat(qStat.responseCount()).isNotEqualTo(selectionSum);
        }
    }

    // ==================== GRID 카테고리 테스트 ====================

    @Nested
    @DisplayName("GRID 카테고리 통계")
    class GridCategoryStatistics {

        @DisplayName("TC-STAT-050: MC_GRID 행별 옵션 분포 정확성")
        @Test
        void getSurveyStatistics_WithMcGridResponses_ReturnsCorrectRowOptionDistribution() {
            // given
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "그리드 질문", null, true, 1);
            withId(question, 100L);

            SurveyQuestionOption optSatisfied = SurveyQuestionOption.create(question, "만족", 1);
            withId(optSatisfied, 201L);
            SurveyQuestionOption optDissatisfied = SurveyQuestionOption.create(question, "불만족", 2);
            withId(optDissatisfied, 202L);
            question.addOption(optSatisfied);
            question.addOption(optDissatisfied);

            SurveyQuestionRow rowMath = SurveyQuestionRow.create(question, "수학", 1);
            withId(rowMath, 301L);
            SurveyQuestionRow rowEng = SurveyQuestionRow.create(question, "영어", 2);
            withId(rowEng, 302L);
            question.addRow(rowMath);
            question.addRow(rowEng);

            // 응답자1: 수학=만족, 영어=불만족
            // 응답자2: 수학=불만족, 영어=만족
            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-02T00:00:00Z"));

            List<SurveyAnswer> answers = List.of(
                    GridSurveyAnswer.create(r1, question, rowMath, optSatisfied),
                    GridSurveyAnswer.create(r1, question, rowEng, optDissatisfied),
                    GridSurveyAnswer.create(r2, question, rowMath, optDissatisfied),
                    GridSurveyAnswer.create(r2, question, rowEng, optSatisfied)
            );

            setUpMocks(List.of(r1, r2), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            GridQuestionStatistics gridStats = result.questionStatistics().getFirst().gridStatistics();
            assertThat(gridStats).isNotNull();
            assertThat(gridStats.rows()).hasSize(2);

            // 수학: 만족=1, 불만족=1
            GridRowStatistics mathRow = gridStats.rows().stream()
                    .filter(r -> r.rowId().equals(301L)).findFirst().orElseThrow();
            OptionStatisticsItem mathSatisfied = mathRow.options().stream()
                    .filter(o -> o.optionId().equals(201L)).findFirst().orElseThrow();
            OptionStatisticsItem mathDissatisfied = mathRow.options().stream()
                    .filter(o -> o.optionId().equals(202L)).findFirst().orElseThrow();
            assertThat(mathSatisfied.count()).isEqualTo(1);
            assertThat(mathDissatisfied.count()).isEqualTo(1);

            // 영어: 만족=1, 불만족=1
            GridRowStatistics engRow = gridStats.rows().stream()
                    .filter(r -> r.rowId().equals(302L)).findFirst().orElseThrow();
            OptionStatisticsItem engSatisfied = engRow.options().stream()
                    .filter(o -> o.optionId().equals(201L)).findFirst().orElseThrow();
            OptionStatisticsItem engDissatisfied = engRow.options().stream()
                    .filter(o -> o.optionId().equals(202L)).findFirst().orElseThrow();
            assertThat(engSatisfied.count()).isEqualTo(1);
            assertThat(engDissatisfied.count()).isEqualTo(1);
        }

        @DisplayName("TC-STAT-051: MC_GRID 행별 비율 합계 <= 100%")
        @Test
        void getSurveyStatistics_WithMcGridResponses_ReturnsRowPercentageSumLessThanOrEqual100() {
            // given
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "그리드 질문", null, true, 1);
            withId(question, 100L);

            SurveyQuestionOption optA = SurveyQuestionOption.create(question, "A", 1);
            withId(optA, 201L);
            SurveyQuestionOption optB = SurveyQuestionOption.create(question, "B", 2);
            withId(optB, 202L);
            SurveyQuestionOption optC = SurveyQuestionOption.create(question, "C", 3);
            withId(optC, 203L);
            question.addOption(optA);
            question.addOption(optB);
            question.addOption(optC);

            SurveyQuestionRow row1 = SurveyQuestionRow.create(question, "항목1", 1);
            withId(row1, 301L);
            SurveyQuestionRow row2 = SurveyQuestionRow.create(question, "항목2", 2);
            withId(row2, 302L);
            question.addRow(row1);
            question.addRow(row2);

            // 응답자 3명 (MC_GRID = 행당 단일 선택)
            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-02T00:00:00Z"));
            SurveyResponse r3 = createResponseWithIdAndCreatedAt(3L, Instant.parse("2026-02-03T00:00:00Z"));

            List<SurveyAnswer> answers = List.of(
                    // 응답자1: 항목1=A, 항목2=B
                    GridSurveyAnswer.create(r1, question, row1, optA),
                    GridSurveyAnswer.create(r1, question, row2, optB),
                    // 응답자2: 항목1=A, 항목2=C
                    GridSurveyAnswer.create(r2, question, row1, optA),
                    GridSurveyAnswer.create(r2, question, row2, optC),
                    // 응답자3: 항목1=B, 항목2=A
                    GridSurveyAnswer.create(r3, question, row1, optB),
                    GridSurveyAnswer.create(r3, question, row2, optA)
            );

            setUpMocks(List.of(r1, r2, r3), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            GridQuestionStatistics gridStats = result.questionStatistics().getFirst().gridStatistics();
            assertThat(gridStats).isNotNull();

            // 각 행별 비율 합계 <= 100%
            for (GridRowStatistics row : gridStats.rows()) {
                BigDecimal rowPercentageSum = row.options().stream()
                        .map(OptionStatisticsItem::percentage)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                assertThat(rowPercentageSum).isLessThanOrEqualTo(new BigDecimal("100.0"));
            }

            // 비율 분모는 전체 설문 응답자 수 (totalResponseCount=3)
            // 항목1: A=2/3=66.7%, B=1/3=33.3%, C=0/3=0.0%
            GridRowStatistics row1Stats = gridStats.rows().stream()
                    .filter(r -> r.rowId().equals(301L)).findFirst().orElseThrow();
            OptionStatisticsItem row1OptA = row1Stats.options().stream()
                    .filter(o -> o.optionId().equals(201L)).findFirst().orElseThrow();
            assertThat(row1OptA.percentage()).isEqualByComparingTo(new BigDecimal("66.7"));
        }

        @DisplayName("TC-STAT-052: CB_GRID 행별 옵션 분포 (복수 선택)")
        @Test
        void getSurveyStatistics_WithCbGridResponses_ReturnsCorrectRowOptionDistribution() {
            // given
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX_GRID, "체크박스 그리드", null, true, 1);
            withId(question, 100L);

            SurveyQuestionOption optX = SurveyQuestionOption.create(question, "X", 1);
            withId(optX, 201L);
            SurveyQuestionOption optY = SurveyQuestionOption.create(question, "Y", 2);
            withId(optY, 202L);
            SurveyQuestionOption optZ = SurveyQuestionOption.create(question, "Z", 3);
            withId(optZ, 203L);
            question.addOption(optX);
            question.addOption(optY);
            question.addOption(optZ);

            SurveyQuestionRow rowA = SurveyQuestionRow.create(question, "A", 1);
            withId(rowA, 301L);
            SurveyQuestionRow rowB = SurveyQuestionRow.create(question, "B", 2);
            withId(rowB, 302L);
            question.addRow(rowA);
            question.addRow(rowB);

            // 응답자1: A={X,Y}, B={Z}
            // 응답자2: A={X}, B={X,Y}
            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-02T00:00:00Z"));

            List<SurveyAnswer> answers = List.of(
                    GridSurveyAnswer.create(r1, question, rowA, optX),
                    GridSurveyAnswer.create(r1, question, rowA, optY),
                    GridSurveyAnswer.create(r1, question, rowB, optZ),
                    GridSurveyAnswer.create(r2, question, rowA, optX),
                    GridSurveyAnswer.create(r2, question, rowB, optX),
                    GridSurveyAnswer.create(r2, question, rowB, optY)
            );

            setUpMocks(List.of(r1, r2), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            GridQuestionStatistics gridStats = result.questionStatistics().getFirst().gridStatistics();
            assertThat(gridStats).isNotNull();

            // A: {X:2, Y:1, Z:0}
            GridRowStatistics rowAStats = gridStats.rows().stream()
                    .filter(r -> r.rowId().equals(301L)).findFirst().orElseThrow();
            assertThat(rowAStats.options().stream().filter(o -> o.optionId().equals(201L))
                    .findFirst().orElseThrow().count()).isEqualTo(2);
            assertThat(rowAStats.options().stream().filter(o -> o.optionId().equals(202L))
                    .findFirst().orElseThrow().count()).isEqualTo(1);
            assertThat(rowAStats.options().stream().filter(o -> o.optionId().equals(203L))
                    .findFirst().orElseThrow().count()).isEqualTo(0);

            // B: {X:1, Y:1, Z:1}
            GridRowStatistics rowBStats = gridStats.rows().stream()
                    .filter(r -> r.rowId().equals(302L)).findFirst().orElseThrow();
            assertThat(rowBStats.options().stream().filter(o -> o.optionId().equals(201L))
                    .findFirst().orElseThrow().count()).isEqualTo(1);
            assertThat(rowBStats.options().stream().filter(o -> o.optionId().equals(202L))
                    .findFirst().orElseThrow().count()).isEqualTo(1);
            assertThat(rowBStats.options().stream().filter(o -> o.optionId().equals(203L))
                    .findFirst().orElseThrow().count()).isEqualTo(1);
        }

        @DisplayName("TC-STAT-053: CB_GRID 행별 비율 합계 100% 초과 가능")
        @Test
        void getSurveyStatistics_WithCbGridResponses_ReturnsRowPercentageSumGreaterThan100() {
            // given
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX_GRID, "체크박스 그리드", null, true, 1);
            withId(question, 100L);

            SurveyQuestionOption optX = SurveyQuestionOption.create(question, "X", 1);
            withId(optX, 201L);
            SurveyQuestionOption optY = SurveyQuestionOption.create(question, "Y", 2);
            withId(optY, 202L);
            SurveyQuestionOption optZ = SurveyQuestionOption.create(question, "Z", 3);
            withId(optZ, 203L);
            question.addOption(optX);
            question.addOption(optY);
            question.addOption(optZ);

            SurveyQuestionRow rowA = SurveyQuestionRow.create(question, "A", 1);
            withId(rowA, 301L);
            question.addRow(rowA);

            // 응답자1: A={X,Y,Z}, 응답자2: A={X,Y}
            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-02T00:00:00Z"));

            List<SurveyAnswer> answers = List.of(
                    GridSurveyAnswer.create(r1, question, rowA, optX),
                    GridSurveyAnswer.create(r1, question, rowA, optY),
                    GridSurveyAnswer.create(r1, question, rowA, optZ),
                    GridSurveyAnswer.create(r2, question, rowA, optX),
                    GridSurveyAnswer.create(r2, question, rowA, optY)
            );

            setUpMocks(List.of(r1, r2), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            GridQuestionStatistics gridStats = result.questionStatistics().getFirst().gridStatistics();
            assertThat(gridStats).isNotNull();

            GridRowStatistics rowAStats = gridStats.rows().getFirst();
            // A행: X=2/2=100.0%, Y=2/2=100.0%, Z=1/2=50.0%
            OptionStatisticsItem itemX = rowAStats.options().stream()
                    .filter(o -> o.optionId().equals(201L)).findFirst().orElseThrow();
            OptionStatisticsItem itemY = rowAStats.options().stream()
                    .filter(o -> o.optionId().equals(202L)).findFirst().orElseThrow();
            OptionStatisticsItem itemZ = rowAStats.options().stream()
                    .filter(o -> o.optionId().equals(203L)).findFirst().orElseThrow();

            assertThat(itemX.percentage()).isEqualByComparingTo(new BigDecimal("100.0"));
            assertThat(itemY.percentage()).isEqualByComparingTo(new BigDecimal("100.0"));
            assertThat(itemZ.percentage()).isEqualByComparingTo(new BigDecimal("50.0"));

            // 합계 > 100%
            BigDecimal rowPercentageSum = rowAStats.options().stream()
                    .map(OptionStatisticsItem::percentage)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(rowPercentageSum).isGreaterThan(new BigDecimal("100"));
        }

        @DisplayName("TC-STAT-054: 비필수 질문의 미응답 행 (모든 옵션 count=0)")
        @Test
        void getSurveyStatistics_WithUnansweredGridRow_ReturnsZeroCountsForRow() {
            // given
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX_GRID, "체크박스 그리드", null, false, 1);
            withId(question, 100L);

            SurveyQuestionOption optX = SurveyQuestionOption.create(question, "X", 1);
            withId(optX, 201L);
            SurveyQuestionOption optY = SurveyQuestionOption.create(question, "Y", 2);
            withId(optY, 202L);
            question.addOption(optX);
            question.addOption(optY);

            SurveyQuestionRow rowA = SurveyQuestionRow.create(question, "A", 1);
            withId(rowA, 301L);
            SurveyQuestionRow rowB = SurveyQuestionRow.create(question, "B", 2);
            withId(rowB, 302L);
            question.addRow(rowA);
            question.addRow(rowB);

            // 응답자1: A행만 응답 A={X}, B행은 미응답
            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));

            List<SurveyAnswer> answers = List.of(
                    GridSurveyAnswer.create(r1, question, rowA, optX)
            );

            setUpMocks(List.of(r1), List.of(question), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            GridQuestionStatistics gridStats = result.questionStatistics().getFirst().gridStatistics();
            assertThat(gridStats).isNotNull();

            // B행: 모든 옵션 count = 0
            GridRowStatistics rowBStats = gridStats.rows().stream()
                    .filter(r -> r.rowId().equals(302L)).findFirst().orElseThrow();
            for (OptionStatisticsItem option : rowBStats.options()) {
                assertThat(option.count()).isEqualTo(0);
            }
        }
    }

    // ==================== TASK-016: 전체 요약 통계 (TC-STAT-060~063) ====================

    @Nested
    @DisplayName("전체 요약 통계")
    class SummaryStatistics {

        @DisplayName("TC-STAT-060: 총 응답 수 정확성 - 유효 응답 5건이면 totalResponseCount=5")
        @Test
        void getSurveyStatistics_WithFiveValidResponses_ReturnsTotalResponseCountFive() {
            // given
            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-05T00:00:00Z"));
            SurveyResponse r3 = createResponseWithIdAndCreatedAt(3L, Instant.parse("2026-02-10T00:00:00Z"));
            SurveyResponse r4 = createResponseWithIdAndCreatedAt(4L, Instant.parse("2026-02-15T00:00:00Z"));
            SurveyResponse r5 = createResponseWithIdAndCreatedAt(5L, Instant.parse("2026-02-20T00:00:00Z"));

            setUpMocks(List.of(r1, r2, r3, r4, r5), List.of(), List.of());

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result.totalResponseCount()).isEqualTo(5);
        }

        @DisplayName("TC-STAT-061: 삭제된 응답 제외 후 총 응답 수 - 5건 중 2건 deleted -> totalResponseCount=3")
        @Test
        void getSurveyStatistics_WithDeletedResponses_ExcludesDeletedFromCount() {
            // given: Repository mock이 deleted=false인 응답만 반환하므로 3건만 반환
            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-05T00:00:00Z"));
            SurveyResponse r3 = createResponseWithIdAndCreatedAt(3L, Instant.parse("2026-02-10T00:00:00Z"));

            setUpMocks(List.of(r1, r2, r3), List.of(), List.of());

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result.totalResponseCount()).isEqualTo(3);
        }

        @DisplayName("TC-STAT-062: 응답 기간 정확성 - 첫 응답과 마지막 응답의 createdAt 반환")
        @Test
        void getSurveyStatistics_WithMultipleResponses_ReturnsCorrectResponsePeriod() {
            // given
            Instant firstAt = Instant.parse("2026-02-01T00:00:00Z");
            Instant middleAt = Instant.parse("2026-02-10T00:00:00Z");
            Instant lastAt = Instant.parse("2026-02-25T00:00:00Z");

            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, firstAt);
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, middleAt);
            SurveyResponse r3 = createResponseWithIdAndCreatedAt(3L, lastAt);

            setUpMocks(List.of(r1, r2, r3), List.of(), List.of());

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result.responseStartedAt()).isEqualTo(firstAt);
            assertThat(result.responseEndedAt()).isEqualTo(lastAt);
        }

        @DisplayName("TC-STAT-063: 응답 0건일 때 응답 기간 null")
        @Test
        void getSurveyStatistics_WithNoResponses_ReturnsNullResponsePeriod() {
            // given
            setUpMocks(List.of(), List.of(), List.of());

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result.totalResponseCount()).isZero();
            assertThat(result.responseStartedAt()).isNull();
            assertThat(result.responseEndedAt()).isNull();
        }
    }

    // ==================== TASK-017: 경계값/엣지 케이스 (TC-STAT-070~075) ====================

    @Nested
    @DisplayName("경계값/엣지 케이스")
    class EdgeCases {

        @DisplayName("TC-STAT-070: 응답 0건 통계 - 200 OK, division by zero 미발생")
        @Test
        void getSurveyStatistics_WithZeroResponses_ReturnsEmptyStatisticsWithoutError() {
            // given
            OptionSurveyQuestion mcQuestion = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "MC 질문", null, true, 0);
            withId(mcQuestion, 100L);
            SurveyQuestionOption optA = SurveyQuestionOption.create(mcQuestion, "A", 1);
            withId(optA, 101L);
            SurveyQuestionOption optB = SurveyQuestionOption.create(mcQuestion, "B", 2);
            withId(optB, 102L);
            mcQuestion.addOption(optA);
            mcQuestion.addOption(optB);

            LinearScaleSurveyQuestion scaleQuestion = LinearScaleSurveyQuestion.create(
                    survey, SurveyQuestionType.LINEAR_SCALE, "SCALE 질문", null, true, 1);
            withId(scaleQuestion, 200L);
            scaleQuestion.setScaleRange(1, 5);

            setUpMocks(List.of(), List.of(mcQuestion, scaleQuestion), List.of());

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result.totalResponseCount()).isZero();

            // MC 질문: 옵션 percentage = 0.0, division by zero 미발생
            QuestionStatisticsResponse mcStat = result.questionStatistics().get(0);
            assertThat(mcStat.responseCount()).isZero();
            assertThat(mcStat.optionStatistics()).isNotNull();
            for (OptionStatisticsItem item : mcStat.optionStatistics().options()) {
                assertThat(item.count()).isZero();
                assertThat(item.percentage()).isEqualByComparingTo(new BigDecimal("0.0"));
            }

            // SCALE 질문: average = 0.0, min/max = null
            QuestionStatisticsResponse scaleStat = result.questionStatistics().get(1);
            assertThat(scaleStat.responseCount()).isZero();
            assertThat(scaleStat.scaleStatistics()).isNotNull();
            assertThat(scaleStat.scaleStatistics().average()).isEqualByComparingTo(new BigDecimal("0.0"));
            assertThat(scaleStat.scaleStatistics().min()).isNull();
            assertThat(scaleStat.scaleStatistics().max()).isNull();
        }

        @DisplayName("TC-STAT-071: 응답 1건 통계 - 최소 유효 응답 경계값")
        @Test
        void getSurveyStatistics_WithOneResponse_ReturnsCorrectStatistics() {
            // given
            TextSurveyQuestion textQuestion = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.SHORT_ANSWER, "텍스트 질문", null, true, 0);
            withId(textQuestion, 100L);

            OptionSurveyQuestion mcQuestion = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "MC 질문", null, true, 1);
            withId(mcQuestion, 200L);
            SurveyQuestionOption optA = SurveyQuestionOption.create(mcQuestion, "A", 1);
            withId(optA, 201L);
            SurveyQuestionOption optB = SurveyQuestionOption.create(mcQuestion, "B", 2);
            withId(optB, 202L);
            mcQuestion.addOption(optA);
            mcQuestion.addOption(optB);

            Instant responseTime = Instant.parse("2026-02-15T00:00:00Z");
            SurveyResponse response = createResponseWithIdAndCreatedAt(1L, responseTime);

            TextSurveyAnswer textAnswer = TextSurveyAnswer.create(response, textQuestion, "응답1");
            OptionSurveyAnswer mcAnswer = OptionSurveyAnswer.create(response, mcQuestion, optA);

            setUpMocks(List.of(response), List.of(textQuestion, mcQuestion), List.of(textAnswer, mcAnswer));

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result.totalResponseCount()).isEqualTo(1);
            assertThat(result.responseStartedAt()).isEqualTo(responseTime);
            assertThat(result.responseEndedAt()).isEqualTo(responseTime);

            // TEXT 질문
            QuestionStatisticsResponse textStat = result.questionStatistics().get(0);
            assertThat(textStat.responseCount()).isEqualTo(1);
            assertThat(textStat.textStatistics().textResponses()).containsExactly("응답1");

            // MC 질문: A=1 (100.0%), B=0 (0.0%)
            QuestionStatisticsResponse mcStat = result.questionStatistics().get(1);
            assertThat(mcStat.responseCount()).isEqualTo(1);
            OptionStatisticsItem itemA = mcStat.optionStatistics().options().stream()
                    .filter(o -> o.optionId().equals(201L)).findFirst().orElseThrow();
            OptionStatisticsItem itemB = mcStat.optionStatistics().options().stream()
                    .filter(o -> o.optionId().equals(202L)).findFirst().orElseThrow();
            assertThat(itemA.count()).isEqualTo(1);
            assertThat(itemA.percentage()).isEqualByComparingTo(new BigDecimal("100.0"));
            assertThat(itemB.count()).isZero();
            assertThat(itemB.percentage()).isEqualByComparingTo(new BigDecimal("0.0"));
        }

        @DisplayName("TC-STAT-072: 모든 응답이 삭제된 경우 - 유효 0건, 응답 0건과 동일")
        @Test
        void getSurveyStatistics_WithAllResponsesDeleted_ReturnsSameAsZeroResponses() {
            // given: Repository mock이 deleted=false인 응답만 반환하므로 빈 리스트 반환
            TextSurveyQuestion textQuestion = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.SHORT_ANSWER, "텍스트 질문", null, true, 0);
            withId(textQuestion, 100L);

            setUpMocks(List.of(), List.of(textQuestion), List.of());

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result.totalResponseCount()).isZero();
            assertThat(result.responseStartedAt()).isNull();
            assertThat(result.responseEndedAt()).isNull();
            assertThat(result.questionStatistics()).hasSize(1);
            assertThat(result.questionStatistics().getFirst().responseCount()).isZero();
        }

        @DisplayName("TC-STAT-073: 비필수 질문에 일부만 응답 - 5명 중 3명만 응답, responseCount=3")
        @Test
        void getSurveyStatistics_WithOptionalQuestion_CountsOnlyRespondents() {
            // given
            OptionSurveyQuestion mcQuestion = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "비필수 MC", null, false, 0);
            withId(mcQuestion, 100L);
            SurveyQuestionOption optA = SurveyQuestionOption.create(mcQuestion, "A", 1);
            withId(optA, 101L);
            SurveyQuestionOption optB = SurveyQuestionOption.create(mcQuestion, "B", 2);
            withId(optB, 102L);
            SurveyQuestionOption optC = SurveyQuestionOption.create(mcQuestion, "C", 3);
            withId(optC, 103L);
            mcQuestion.addOption(optA);
            mcQuestion.addOption(optB);
            mcQuestion.addOption(optC);

            // 5명 응답, 3명만 해당 질문에 답변
            List<SurveyResponse> responses = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                responses.add(createResponseWithIdAndCreatedAt(
                        (long) i, Instant.parse("2026-02-0" + i + "T00:00:00Z")));
            }

            List<SurveyAnswer> answers = List.of(
                    OptionSurveyAnswer.create(responses.get(0), mcQuestion, optA),
                    OptionSurveyAnswer.create(responses.get(1), mcQuestion, optA),
                    OptionSurveyAnswer.create(responses.get(2), mcQuestion, optB)
            );
            // responses.get(3), responses.get(4)는 이 질문에 답변하지 않음 (비필수)

            setUpMocks(responses, List.of(mcQuestion), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result.totalResponseCount()).isEqualTo(5);

            QuestionStatisticsResponse mcStat = result.questionStatistics().getFirst();
            assertThat(mcStat.responseCount()).isEqualTo(3);
            // 비율은 totalResponseCount(5) 기준: A=2/5=40.0%, B=1/5=20.0%, C=0/5=0.0%
            OptionStatisticsItem itemA = mcStat.optionStatistics().options().stream()
                    .filter(o -> o.optionId().equals(101L)).findFirst().orElseThrow();
            OptionStatisticsItem itemB = mcStat.optionStatistics().options().stream()
                    .filter(o -> o.optionId().equals(102L)).findFirst().orElseThrow();
            OptionStatisticsItem itemC = mcStat.optionStatistics().options().stream()
                    .filter(o -> o.optionId().equals(103L)).findFirst().orElseThrow();

            assertThat(itemA.count()).isEqualTo(2);
            assertThat(itemA.percentage()).isEqualByComparingTo(new BigDecimal("40.0"));
            assertThat(itemB.count()).isEqualTo(1);
            assertThat(itemB.percentage()).isEqualByComparingTo(new BigDecimal("20.0"));
            assertThat(itemC.count()).isZero();
            assertThat(itemC.percentage()).isEqualByComparingTo(new BigDecimal("0.0"));
        }

        @DisplayName("TC-STAT-074: 모든 질문 비필수 + 빈 응답 제출 - totalResponseCount=1, 질문별 0")
        @Test
        void getSurveyStatistics_WithEmptyResponseToOptionalQuestions_ReturnsOneResponseZeroAnswers() {
            // given
            TextSurveyQuestion textQuestion = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.SHORT_ANSWER, "비필수 텍스트", null, false, 0);
            withId(textQuestion, 100L);

            OptionSurveyQuestion mcQuestion = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "비필수 MC", null, false, 1);
            withId(mcQuestion, 200L);
            SurveyQuestionOption optA = SurveyQuestionOption.create(mcQuestion, "A", 1);
            withId(optA, 201L);
            mcQuestion.addOption(optA);

            // 1명이 아무 응답 없이 제출 (SurveyResponse만 존재)
            SurveyResponse response = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-15T00:00:00Z"));

            setUpMocks(List.of(response), List.of(textQuestion, mcQuestion), List.of());

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result.totalResponseCount()).isEqualTo(1);

            // TEXT 질문: responseCount=0, textResponses 빈 리스트
            QuestionStatisticsResponse textStat = result.questionStatistics().get(0);
            assertThat(textStat.responseCount()).isZero();
            assertThat(textStat.textStatistics().textResponses()).isEmpty();

            // MC 질문: responseCount=0, 모든 옵션 count=0
            QuestionStatisticsResponse mcStat = result.questionStatistics().get(1);
            assertThat(mcStat.responseCount()).isZero();
            for (OptionStatisticsItem item : mcStat.optionStatistics().options()) {
                assertThat(item.count()).isZero();
            }
        }

        @DisplayName("TC-STAT-075: 삭제된 SurveyAnswer가 질문별 통계에서 제외")
        @Test
        void getSurveyStatistics_WithDeletedSurveyAnswer_ExcludesFromQuestionStatistics() {
            // given
            OptionSurveyQuestion mcQuestion = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "MC 질문", null, true, 0);
            withId(mcQuestion, 100L);
            SurveyQuestionOption optA = SurveyQuestionOption.create(mcQuestion, "A", 1);
            withId(optA, 101L);
            SurveyQuestionOption optB = SurveyQuestionOption.create(mcQuestion, "B", 2);
            withId(optB, 102L);
            SurveyQuestionOption optC = SurveyQuestionOption.create(mcQuestion, "C", 3);
            withId(optC, 103L);
            mcQuestion.addOption(optA);
            mcQuestion.addOption(optB);
            mcQuestion.addOption(optC);

            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-02T00:00:00Z"));
            SurveyResponse r3 = createResponseWithIdAndCreatedAt(3L, Instant.parse("2026-02-03T00:00:00Z"));

            // 응답자1=A, 응답자2=B, 응답자3=A (응답자3의 SurveyAnswer가 deleted=true)
            OptionSurveyAnswer a1 = OptionSurveyAnswer.create(r1, mcQuestion, optA);
            OptionSurveyAnswer a2 = OptionSurveyAnswer.create(r2, mcQuestion, optB);
            // 응답자3의 answer는 deleted=true이므로 findValidAnswersBySurveyId에서 제외됨

            setUpMocks(List.of(r1, r2, r3), List.of(mcQuestion), List.of(a1, a2));

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result.totalResponseCount()).isEqualTo(3);

            QuestionStatisticsResponse mcStat = result.questionStatistics().getFirst();
            assertThat(mcStat.responseCount()).isEqualTo(2);

            OptionStatisticsItem itemA = mcStat.optionStatistics().options().stream()
                    .filter(o -> o.optionId().equals(101L)).findFirst().orElseThrow();
            OptionStatisticsItem itemB = mcStat.optionStatistics().options().stream()
                    .filter(o -> o.optionId().equals(102L)).findFirst().orElseThrow();
            OptionStatisticsItem itemC = mcStat.optionStatistics().options().stream()
                    .filter(o -> o.optionId().equals(103L)).findFirst().orElseThrow();

            // A=1, B=1, C=0 (응답자3의 A 선택이 제외됨)
            assertThat(itemA.count()).isEqualTo(1);
            assertThat(itemB.count()).isEqualTo(1);
            assertThat(itemC.count()).isZero();
        }
    }

    // ==================== TASK-018: soft delete 통계 (TC-STAT-080~083) ====================

    @Nested
    @DisplayName("soft delete된 질문/선택지/행은 통계에서 제외")
    class SoftDeleteStatistics {

        @DisplayName("TC-STAT-080: soft delete된 질문은 통계에서 제외")
        @Test
        void getSurveyStatistics_WithSoftDeletedQuestion_ExcludedFromStatistics() {
            // given - 삭제된 질문과 활성 질문 각 1개
            OptionSurveyQuestion deletedQuestion = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "삭제된 질문", null, true, 0);
            withId(deletedQuestion, 100L);
            deletedQuestion.delete(DEFAULT_OPERATOR_ID);

            OptionSurveyQuestion activeQuestion = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "활성 질문", null, true, 1);
            withId(activeQuestion, 200L);
            SurveyQuestionOption optA = SurveyQuestionOption.create(activeQuestion, "A", 1);
            withId(optA, 201L);
            activeQuestion.addOption(optA);

            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));

            List<SurveyAnswer> answers = List.of(
                    OptionSurveyAnswer.create(r1, activeQuestion, optA)
            );

            // 삭제된 질문은 Repository에서 이미 필터링되므로 activeQuestion만 전달
            setUpMocks(List.of(r1), List.of(activeQuestion), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then - 삭제된 질문은 통계에 포함되지 않음
            assertThat(result.questionStatistics()).hasSize(1);
            assertThat(result.questionStatistics().getFirst().questionId()).isEqualTo(200L);
        }

        @DisplayName("TC-STAT-081: soft delete된 선택지는 통계에서 제외")
        @Test
        void getSurveyStatistics_WithSoftDeletedOption_ExcludedFromStatistics() {
            // given
            OptionSurveyQuestion mcQuestion = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "MC 질문", null, true, 0);
            withId(mcQuestion, 100L);

            SurveyQuestionOption optA = SurveyQuestionOption.create(mcQuestion, "A", 1);
            withId(optA, 101L);
            SurveyQuestionOption optB = SurveyQuestionOption.create(mcQuestion, "B", 2);
            withId(optB, 102L);
            SurveyQuestionOption optC = SurveyQuestionOption.create(mcQuestion, "C", 3);
            withId(optC, 103L);
            optC.delete(DEFAULT_OPERATOR_ID); // C를 soft delete

            mcQuestion.addOption(optA);
            mcQuestion.addOption(optB);
            mcQuestion.addOption(optC);

            // 응답: A=3, B=1, C=2
            List<SurveyResponse> responses = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                responses.add(createResponseWithIdAndCreatedAt(
                        (long) i, Instant.parse("2026-02-0" + i + "T00:00:00Z")));
            }

            List<SurveyAnswer> answers = List.of(
                    OptionSurveyAnswer.create(responses.get(0), mcQuestion, optA),
                    OptionSurveyAnswer.create(responses.get(1), mcQuestion, optA),
                    OptionSurveyAnswer.create(responses.get(2), mcQuestion, optA),
                    OptionSurveyAnswer.create(responses.get(3), mcQuestion, optB),
                    OptionSurveyAnswer.create(responses.get(4), mcQuestion, optC),
                    OptionSurveyAnswer.create(responses.get(5), mcQuestion, optC)
            );

            setUpMocks(responses, List.of(mcQuestion), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then - 삭제된 선택지 C는 통계에 포함되지 않음
            QuestionStatisticsResponse mcStat = result.questionStatistics().getFirst();
            List<OptionStatisticsItem> options = mcStat.optionStatistics().options();
            assertThat(options).hasSize(2); // A, B만

            assertThat(options.stream().map(OptionStatisticsItem::optionId).toList())
                    .containsExactly(101L, 102L);

            OptionStatisticsItem itemA = options.stream()
                    .filter(o -> o.optionId().equals(101L)).findFirst().orElseThrow();
            OptionStatisticsItem itemB = options.stream()
                    .filter(o -> o.optionId().equals(102L)).findFirst().orElseThrow();
            assertThat(itemA.count()).isEqualTo(3);
            assertThat(itemB.count()).isEqualTo(1);
        }

        @DisplayName("TC-STAT-082: soft delete된 행은 통계에서 제외")
        @Test
        void getSurveyStatistics_WithSoftDeletedRow_ExcludedFromStatistics() {
            // given
            GridSurveyQuestion gridQuestion = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "GRID 질문", null, true, 0);
            withId(gridQuestion, 100L);

            SurveyQuestionOption optSatisfied = SurveyQuestionOption.create(gridQuestion, "만족", 1);
            withId(optSatisfied, 101L);
            SurveyQuestionOption optDissatisfied = SurveyQuestionOption.create(gridQuestion, "불만족", 2);
            withId(optDissatisfied, 102L);
            gridQuestion.addOption(optSatisfied);
            gridQuestion.addOption(optDissatisfied);

            SurveyQuestionRow rowMath = SurveyQuestionRow.create(gridQuestion, "수학", 1);
            withId(rowMath, 201L);
            rowMath.delete(DEFAULT_OPERATOR_ID); // 수학 행 soft delete

            SurveyQuestionRow rowEnglish = SurveyQuestionRow.create(gridQuestion, "영어", 2);
            withId(rowEnglish, 202L);

            gridQuestion.addRow(rowMath);
            gridQuestion.addRow(rowEnglish);

            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-02T00:00:00Z"));

            List<SurveyAnswer> answers = List.of(
                    GridSurveyAnswer.create(r1, gridQuestion, rowMath, optSatisfied),
                    GridSurveyAnswer.create(r2, gridQuestion, rowMath, optDissatisfied),
                    GridSurveyAnswer.create(r1, gridQuestion, rowEnglish, optSatisfied),
                    GridSurveyAnswer.create(r2, gridQuestion, rowEnglish, optSatisfied)
            );

            setUpMocks(List.of(r1, r2), List.of(gridQuestion), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then - 삭제된 수학 행은 통계에서 제외
            GridQuestionStatistics gridStats = result.questionStatistics().getFirst().gridStatistics();
            assertThat(gridStats).isNotNull();
            assertThat(gridStats.rows()).hasSize(1); // 영어만

            GridRowStatistics englishRow = gridStats.rows().getFirst();
            assertThat(englishRow.rowId()).isEqualTo(202L);
            assertThat(englishRow.rowLabel()).isEqualTo("영어");

            OptionStatisticsItem engSatisfied = englishRow.options().stream()
                    .filter(o -> o.optionId().equals(101L)).findFirst().orElseThrow();
            OptionStatisticsItem engDissatisfied = englishRow.options().stream()
                    .filter(o -> o.optionId().equals(102L)).findFirst().orElseThrow();
            assertThat(engSatisfied.count()).isEqualTo(2);
            assertThat(engDissatisfied.count()).isZero();
        }

        @DisplayName("TC-STAT-083: 모든 선택지가 soft delete된 질문 - 빈 옵션 목록")
        @Test
        void getSurveyStatistics_WithAllOptionsSoftDeleted_ReturnsEmptyOptionList() {
            // given
            OptionSurveyQuestion mcQuestion = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "MC 질문", null, true, 0);
            withId(mcQuestion, 100L);

            SurveyQuestionOption optA = SurveyQuestionOption.create(mcQuestion, "A", 1);
            withId(optA, 101L);
            optA.delete(DEFAULT_OPERATOR_ID);
            SurveyQuestionOption optB = SurveyQuestionOption.create(mcQuestion, "B", 2);
            withId(optB, 102L);
            optB.delete(DEFAULT_OPERATOR_ID);

            mcQuestion.addOption(optA);
            mcQuestion.addOption(optB);

            SurveyResponse r1 = createResponseWithIdAndCreatedAt(1L, Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = createResponseWithIdAndCreatedAt(2L, Instant.parse("2026-02-02T00:00:00Z"));

            List<SurveyAnswer> answers = List.of(
                    OptionSurveyAnswer.create(r1, mcQuestion, optA),
                    OptionSurveyAnswer.create(r2, mcQuestion, optB)
            );

            setUpMocks(List.of(r1, r2), List.of(mcQuestion), answers);

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then - 모든 선택지가 삭제되었으므로 빈 옵션 목록
            QuestionStatisticsResponse mcStat = result.questionStatistics().getFirst();
            assertThat(mcStat.optionStatistics().options()).isEmpty();
        }
    }

    // ==================== TASK-019: 설문 상태별 통계 조회 (TC-STAT-090~094) ====================

    @Nested
    @DisplayName("설문 상태별 통계 조회")
    class SurveyStatusStatistics {

        @DisplayName("TC-STAT-090: UNPUBLISHED + NOT_STARTED 설문 통계 조회 -> 200 OK")
        @Test
        void getSurveyStatistics_WithUnpublishedNotStartedSurvey_ReturnsStatistics() {
            // given: setUp()의 survey는 기본 UNPUBLISHED + NOT_STARTED
            setUpMocks(List.of(), List.of(), List.of());

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.totalResponseCount()).isZero();
        }

        @DisplayName("TC-STAT-091: PUBLISHED + OPEN 설문 통계 조회 -> 200 OK (실시간)")
        @Test
        void getSurveyStatistics_WithPublishedOpenSurvey_ReturnsStatistics() {
            // given
            Survey publishedOpenSurvey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);

            SurveyResponse r1 = SurveyResponse.create(publishedOpenSurvey, operator);
            withId(r1, 1L);
            ReflectionTestUtils.setField(r1, "createdAt", Instant.parse("2026-02-01T00:00:00Z"));

            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(publishedOpenSurvey));
            given(surveyResponseRepository.findBySurveyIdAndDeletedFalseOrderByCreatedAtAsc(DEFAULT_SURVEY_ID))
                    .willReturn(List.of(r1));
            given(surveyQuestionRepository.findAllBySurveyIdWithOptions(DEFAULT_SURVEY_ID))
                    .willReturn(List.of());
            given(surveyQuestionRepository.findAllBySurveyIdWithRows(DEFAULT_SURVEY_ID))
                    .willReturn(List.of());
            given(surveyAnswerRepository.findValidAnswersBySurveyId(DEFAULT_SURVEY_ID))
                    .willReturn(List.of());

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.totalResponseCount()).isEqualTo(1);
        }

        @DisplayName("TC-STAT-092: PUBLISHED + CLOSED 설문 통계 조회 -> 200 OK")
        @Test
        void getSurveyStatistics_WithPublishedClosedSurvey_ReturnsStatistics() {
            // given
            Survey closedSurvey = withId(createClosedSurvey(), DEFAULT_SURVEY_ID);

            SurveyResponse r1 = SurveyResponse.create(closedSurvey, operator);
            withId(r1, 1L);
            ReflectionTestUtils.setField(r1, "createdAt", Instant.parse("2026-02-01T00:00:00Z"));
            SurveyResponse r2 = SurveyResponse.create(closedSurvey, operator);
            withId(r2, 2L);
            ReflectionTestUtils.setField(r2, "createdAt", Instant.parse("2026-02-10T00:00:00Z"));

            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(closedSurvey));
            given(surveyResponseRepository.findBySurveyIdAndDeletedFalseOrderByCreatedAtAsc(DEFAULT_SURVEY_ID))
                    .willReturn(List.of(r1, r2));
            given(surveyQuestionRepository.findAllBySurveyIdWithOptions(DEFAULT_SURVEY_ID))
                    .willReturn(List.of());
            given(surveyQuestionRepository.findAllBySurveyIdWithRows(DEFAULT_SURVEY_ID))
                    .willReturn(List.of());
            given(surveyAnswerRepository.findValidAnswersBySurveyId(DEFAULT_SURVEY_ID))
                    .willReturn(List.of());

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.totalResponseCount()).isEqualTo(2);
        }

        @DisplayName("TC-STAT-093: 휴지통 설문 통계 조회 -> 200 OK")
        @Test
        void getSurveyStatistics_WithTrashedSurvey_ReturnsStatistics() {
            // given
            Survey trashedSurvey = withId(createTrashedSurvey(), DEFAULT_SURVEY_ID);

            SurveyResponse r1 = SurveyResponse.create(trashedSurvey, operator);
            withId(r1, 1L);
            ReflectionTestUtils.setField(r1, "createdAt", Instant.parse("2026-02-01T00:00:00Z"));

            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.of(trashedSurvey));
            given(surveyResponseRepository.findBySurveyIdAndDeletedFalseOrderByCreatedAtAsc(DEFAULT_SURVEY_ID))
                    .willReturn(List.of(r1));
            given(surveyQuestionRepository.findAllBySurveyIdWithOptions(DEFAULT_SURVEY_ID))
                    .willReturn(List.of());
            given(surveyQuestionRepository.findAllBySurveyIdWithRows(DEFAULT_SURVEY_ID))
                    .willReturn(List.of());
            given(surveyAnswerRepository.findValidAnswersBySurveyId(DEFAULT_SURVEY_ID))
                    .willReturn(List.of());

            // when
            SurveyStatisticsResponse result = surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID);

            // then
            assertThat(result).isNotNull();
            assertThat(result.totalResponseCount()).isEqualTo(1);
        }

        @DisplayName("TC-STAT-094: 영구 삭제된 설문 -> SurveyNotFoundException")
        @Test
        void getSurveyStatistics_WithPermanentlyDeletedSurvey_ThrowsSurveyNotFoundException() {
            // given: findByIdAndDeletedFalse는 deleted=true인 설문을 반환하지 않음
            given(surveyRepository.findByIdAndDeletedFalse(DEFAULT_SURVEY_ID))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> surveyStatisticsService.getSurveyStatistics(
                    DEFAULT_SURVEY_ID, DEFAULT_OPERATOR_ID))
                    .isInstanceOf(SurveyNotFoundException.class);
        }
    }
}
