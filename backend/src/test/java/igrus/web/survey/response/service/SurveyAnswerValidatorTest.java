package igrus.web.survey.response.service;

import igrus.web.survey.domain.*;
import igrus.web.survey.question.domain.*;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import igrus.web.survey.response.exception.SurveyResponseValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import static igrus.web.common.fixture.SurveyTestFixture.*;
import static igrus.web.common.fixture.TestConstants.DEFAULT_SURVEY_ID;
import static igrus.web.common.fixture.TestEntityIdAssigner.withId;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SurveyAnswerValidator 단위 테스트.
 *
 * <p>고전파(Classical) 방식으로 실제 도메인 객체를 사용합니다.
 */
@DisplayName("SurveyAnswerValidator 단위 테스트")
class SurveyAnswerValidatorTest {

    private SurveyAnswerValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SurveyAnswerValidator();
    }

    // ==================== TEXT 유형 ====================

    @Nested
    @DisplayName("텍스트(TEXT) 유형 검증")
    class TextValidation {

        @DisplayName("필수 텍스트 질문에 값이 있으면 통과")
        @Test
        void validate_RequiredTextWithValue_Success() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            makeRequired(question);
            withId(question, 100L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, "답변 텍스트", null, null, null)
            );

            assertThatCode(() -> validator.validate(survey, answers))
                    .doesNotThrowAnyException();
        }

        @DisplayName("필수 텍스트 질문에 빈 값이면 실패")
        @Test
        void validate_RequiredTextWithBlank_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            makeRequired(question);
            withId(question, 100L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, "", null, null, null)
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }

        @DisplayName("필수 텍스트 질문에 null 값이면 실패")
        @Test
        void validate_RequiredTextWithNull_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            makeRequired(question);
            withId(question, 100L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, null, null)
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }

        @DisplayName("선택적 텍스트 질문에 빈 값이면 통과")
        @Test
        void validate_OptionalTextWithBlank_Success() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, 100L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, "", null, null, null)
            );

            assertThatCode(() -> validator.validate(survey, answers))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== OPTION 유형 ====================

    @Nested
    @DisplayName("선택지(OPTION) 유형 검증")
    class OptionValidation {

        @DisplayName("객관식 질문에 유효한 옵션 1개 선택 시 통과")
        @Test
        void validate_MultipleChoiceWithValidOption_Success() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            OptionSurveyQuestion question = createMultipleChoiceQuestion(survey, 1);
            withId(question, 100L);
            withId(question.getOptions().getFirst(), 200L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, List.of(200L), null, null)
            );

            assertThatCode(() -> validator.validate(survey, answers))
                    .doesNotThrowAnyException();
        }

        @DisplayName("객관식 질문에 2개 이상 선택 시 실패")
        @Test
        void validate_MultipleChoiceWithMultipleOptions_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            OptionSurveyQuestion question = createMultipleChoiceQuestion(survey, 1);
            withId(question, 100L);
            withId(question.getOptions().getFirst(), 200L);
            // 두 번째 옵션 추가
            SurveyQuestionOption option2 = SurveyQuestionOption.create(question, "선택지 2", 2);
            question.addOption(option2);
            withId(option2, 201L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, List.of(200L, 201L), null, null)
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }

        @DisplayName("객관식 질문에 유효하지 않은 옵션 ID 시 실패")
        @Test
        void validate_MultipleChoiceWithInvalidOptionId_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            OptionSurveyQuestion question = createMultipleChoiceQuestion(survey, 1);
            withId(question, 100L);
            withId(question.getOptions().getFirst(), 200L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, List.of(999L), null, null)
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }

        @DisplayName("체크박스 질문에 여러 옵션 선택 시 통과")
        @Test
        void validate_CheckboxWithMultipleOptions_Success() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX, "체크박스", null, false, 1);
            withId(question, 100L);
            SurveyQuestionOption opt1 = SurveyQuestionOption.create(question, "A", 1);
            SurveyQuestionOption opt2 = SurveyQuestionOption.create(question, "B", 2);
            question.addOption(opt1);
            question.addOption(opt2);
            withId(opt1, 200L);
            withId(opt2, 201L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, List.of(200L, 201L), null, null)
            );

            assertThatCode(() -> validator.validate(survey, answers))
                    .doesNotThrowAnyException();
        }

        @DisplayName("필수 체크박스 질문에 빈 선택 시 실패")
        @Test
        void validate_RequiredCheckboxWithEmpty_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX, "체크박스", null, true, 1);
            withId(question, 100L);
            SurveyQuestionOption opt1 = SurveyQuestionOption.create(question, "A", 1);
            question.addOption(opt1);
            withId(opt1, 200L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, List.of(), null, null)
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }

        @DisplayName("archived 옵션 ID로 응답 시 실패")
        @Test
        void validate_ArchivedOptionId_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            OptionSurveyQuestion question = createMultipleChoiceQuestion(survey, 1);
            withId(question, 100L);
            SurveyQuestionOption option = question.getOptions().getFirst();
            withId(option, 200L);
            option.archive(1L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, List.of(200L), null, null)
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }
    }

    // ==================== SCALE 유형 ====================

    @Nested
    @DisplayName("선형 배율(SCALE) 유형 검증")
    class ScaleValidation {

        @DisplayName("범위 내 값이면 통과")
        @Test
        void validate_ScaleWithinRange_Success() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            LinearScaleSurveyQuestion question = createLinearScaleQuestion(survey, 1);
            withId(question, 100L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, 3, null)
            );

            assertThatCode(() -> validator.validate(survey, answers))
                    .doesNotThrowAnyException();
        }

        @DisplayName("범위 초과 시 실패")
        @Test
        void validate_ScaleAboveMax_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            LinearScaleSurveyQuestion question = createLinearScaleQuestion(survey, 1);
            withId(question, 100L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, 6, null)
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }

        @DisplayName("범위 미만 시 실패")
        @Test
        void validate_ScaleBelowMin_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            LinearScaleSurveyQuestion question = createLinearScaleQuestion(survey, 1);
            withId(question, 100L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, 0, null)
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }

        @DisplayName("필수 선형 배율에 null 값이면 실패")
        @Test
        void validate_RequiredScaleWithNull_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            LinearScaleSurveyQuestion question = createLinearScaleQuestion(survey, 1);
            makeRequired(question);
            withId(question, 100L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, null, null)
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }

        @DisplayName("선택적 선형 배율에 null 값이면 통과")
        @Test
        void validate_OptionalScaleWithNull_Success() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            LinearScaleSurveyQuestion question = createLinearScaleQuestion(survey, 1);
            withId(question, 100L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, null, null)
            );

            assertThatCode(() -> validator.validate(survey, answers))
                    .doesNotThrowAnyException();
        }
    }

    // ==================== GRID 유형 ====================

    @Nested
    @DisplayName("그리드(GRID) 유형 검증")
    class GridValidation {

        @DisplayName("객관식 그리드에서 행당 1개 선택 시 통과")
        @Test
        void validate_McGridWithOnePerRow_Success() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            GridSurveyQuestion question = createGridQuestion(survey, 1);
            withId(question, 100L);
            withId(question.getOptions().getFirst(), 200L);
            withId(question.getRows().getFirst(), 300L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, null, List.of(
                            new SubmitAnswerRequest.GridAnswerRequest(300L, List.of(200L))
                    ))
            );

            assertThatCode(() -> validator.validate(survey, answers))
                    .doesNotThrowAnyException();
        }

        @DisplayName("객관식 그리드에서 행당 2개 이상 선택 시 실패")
        @Test
        void validate_McGridWithMultiplePerRow_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            GridSurveyQuestion question = createGridQuestion(survey, 1);
            withId(question, 100L);
            SurveyQuestionOption opt2 = SurveyQuestionOption.create(question, "열 2", 2);
            question.addOption(opt2);
            withId(question.getOptions().get(0), 200L);
            withId(opt2, 201L);
            withId(question.getRows().getFirst(), 300L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, null, List.of(
                            new SubmitAnswerRequest.GridAnswerRequest(300L, List.of(200L, 201L))
                    ))
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }

        @DisplayName("체크박스 그리드에서 행당 여러 개 선택 시 통과")
        @Test
        void validate_CbGridWithMultiplePerRow_Success() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX_GRID, "CB 그리드", null, false, 1);
            SurveyQuestionOption opt1 = SurveyQuestionOption.create(question, "열 1", 1);
            SurveyQuestionOption opt2 = SurveyQuestionOption.create(question, "열 2", 2);
            question.addOption(opt1);
            question.addOption(opt2);
            SurveyQuestionRow row = SurveyQuestionRow.create(question, "행 1", 1);
            question.addRow(row);
            withId(question, 100L);
            withId(opt1, 200L);
            withId(opt2, 201L);
            withId(row, 300L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, null, List.of(
                            new SubmitAnswerRequest.GridAnswerRequest(300L, List.of(200L, 201L))
                    ))
            );

            assertThatCode(() -> validator.validate(survey, answers))
                    .doesNotThrowAnyException();
        }

        @DisplayName("유효하지 않은 행 ID 시 실패")
        @Test
        void validate_GridWithInvalidRowId_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            GridSurveyQuestion question = createGridQuestion(survey, 1);
            withId(question, 100L);
            withId(question.getOptions().getFirst(), 200L);
            withId(question.getRows().getFirst(), 300L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, null, List.of(
                            new SubmitAnswerRequest.GridAnswerRequest(999L, List.of(200L))
                    ))
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }

        @DisplayName("유효하지 않은 옵션 ID 시 실패")
        @Test
        void validate_GridWithInvalidOptionId_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            GridSurveyQuestion question = createGridQuestion(survey, 1);
            withId(question, 100L);
            withId(question.getOptions().getFirst(), 200L);
            withId(question.getRows().getFirst(), 300L);
            survey.getQuestions().add(question);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, null, List.of(
                            new SubmitAnswerRequest.GridAnswerRequest(300L, List.of(999L))
                    ))
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }

        @DisplayName("필수 그리드에서 행 응답 누락 시 실패")
        @Test
        void validate_RequiredGridMissingRow_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            GridSurveyQuestion question = createGridQuestion(survey, 1);
            makeRequired(question);
            // 두 번째 행 추가
            SurveyQuestionRow row2 = SurveyQuestionRow.create(question, "행 2", 2);
            question.addRow(row2);
            withId(question, 100L);
            withId(question.getOptions().getFirst(), 200L);
            withId(question.getRows().get(0), 300L);
            withId(row2, 301L);
            survey.getQuestions().add(question);

            // 행 1개만 응답 (행 2 누락)
            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, null, List.of(
                            new SubmitAnswerRequest.GridAnswerRequest(300L, List.of(200L))
                    ))
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class);
        }
    }

    // ==================== 공통 검증 ====================

    @Nested
    @DisplayName("공통 검증")
    class CommonValidation {

        @DisplayName("필수 질문 응답 누락 시 실패")
        @Test
        void validate_MissingRequiredQuestion_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            makeRequired(question);
            withId(question, 100L);
            survey.getQuestions().add(question);

            // 빈 답변 목록
            List<SubmitAnswerRequest> answers = List.of();

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("필수 질문");
        }

        @DisplayName("존재하지 않는 질문 ID에 응답 시 실패")
        @Test
        void validate_NonExistentQuestionId_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(999L, "답변", null, null, null)
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("존재하지 않는 질문");
        }

        @DisplayName("선택적 질문에 응답 없이 통과")
        @Test
        void validate_OptionalQuestionWithNoAnswer_Success() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, 100L);
            survey.getQuestions().add(question);

            // 선택적 질문은 답변 안 해도 됨
            List<SubmitAnswerRequest> answers = List.of();

            assertThatCode(() -> validator.validate(survey, answers))
                    .doesNotThrowAnyException();
        }

        @DisplayName("archived 질문에 대한 응답은 무시됨")
        @Test
        void validate_ArchivedQuestionIgnored_Success() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            makeRequired(question);
            withId(question, 100L);
            question.archive(1L);
            survey.getQuestions().add(question);

            // archived 질문이므로 필수여도 응답 안 해도 됨
            List<SubmitAnswerRequest> answers = List.of();

            assertThatCode(() -> validator.validate(survey, answers))
                    .doesNotThrowAnyException();
        }

        @DisplayName("archived 질문 ID로 답변 제출 시 실패 (R2-007)")
        @Test
        void validate_ArchivedQuestionIdAnswer_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, 100L);
            question.archive(1L);
            survey.getQuestions().add(question);

            // archived 질문의 ID로 답변을 제출하면 활성 질문 맵에 없으므로 "존재하지 않는 질문" 예외
            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, "답변", null, null, null)
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("존재하지 않는 질문");
        }
    }

    // ==================== 중복 검증 ====================

    @Nested
    @DisplayName("중복 검증")
    class DuplicateValidation {

        @DisplayName("동일 questionId 중복 답변 시 실패")
        @Test
        void validate_DuplicateQuestionId_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            TextSurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, 100L);
            survey.getQuestions().add(question);

            // 같은 questionId로 2개 답변 제출
            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, "답변1", null, null, null),
                    new SubmitAnswerRequest(100L, "답변2", null, null, null)
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("중복 응답");
        }

        @DisplayName("CHECKBOX 동일 optionId 중복 선택 시 실패")
        @Test
        void validate_CheckboxDuplicateOptionId_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX, "체크박스", null, false, 1);
            SurveyQuestionOption opt1 = SurveyQuestionOption.create(question, "A", 1);
            question.addOption(opt1);
            withId(question, 100L);
            withId(opt1, 200L);
            survey.getQuestions().add(question);

            // 같은 optionId를 중복 선택
            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, List.of(200L, 200L), null, null)
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("중복된 선택지");
        }

        @DisplayName("MC_GRID 동일 rowId 중복 응답 시 실패")
        @Test
        void validate_McGridDuplicateRowId_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            GridSurveyQuestion question = createGridQuestion(survey, 1);
            withId(question, 100L);
            withId(question.getOptions().getFirst(), 200L);
            withId(question.getRows().getFirst(), 300L);
            survey.getQuestions().add(question);

            // 같은 rowId로 2번 응답
            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, null, List.of(
                            new SubmitAnswerRequest.GridAnswerRequest(300L, List.of(200L)),
                            new SubmitAnswerRequest.GridAnswerRequest(300L, List.of(200L))
                    ))
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("중복 응답");
        }

        @DisplayName("CB_GRID 행 내 동일 optionId 중복 선택 시 실패")
        @Test
        void validate_CbGridDuplicateOptionIdInRow_ThrowsException() {
            Survey survey = withId(createPublishedAndOpenSurvey(), DEFAULT_SURVEY_ID);
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX_GRID, "CB 그리드", null, false, 1);
            SurveyQuestionOption opt1 = SurveyQuestionOption.create(question, "열 1", 1);
            question.addOption(opt1);
            SurveyQuestionRow row = SurveyQuestionRow.create(question, "행 1", 1);
            question.addRow(row);
            withId(question, 100L);
            withId(opt1, 200L);
            withId(row, 300L);
            survey.getQuestions().add(question);

            // 같은 행에서 같은 optionId를 중복 선택
            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(100L, null, null, null, List.of(
                            new SubmitAnswerRequest.GridAnswerRequest(300L, List.of(200L, 200L))
                    ))
            );

            assertThatThrownBy(() -> validator.validate(survey, answers))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("중복된 선택지");
        }
    }

    // ==================== Helper ====================

    private void makeRequired(SurveyQuestion question) {
        ReflectionTestUtils.setField(question, "required", true);
    }
}
