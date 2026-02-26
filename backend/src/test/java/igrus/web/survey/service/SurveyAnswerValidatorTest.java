package igrus.web.survey.service;

import igrus.web.survey.domain.*;
import igrus.web.survey.dto.request.SubmitAnswerRequest;
import igrus.web.survey.dto.request.SubmitAnswerRequest.GridAnswerRequest;
import igrus.web.survey.exception.SurveyResponseValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static igrus.web.common.fixture.SurveyTestFixture.*;
import static igrus.web.common.fixture.TestEntityIdAssigner.withId;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SurveyAnswerValidator 단위 테스트.
 * 질문 유형별 답변 유효성 검증 로직을 테스트합니다.
 */
@DisplayName("SurveyAnswerValidator 단위 테스트")
class SurveyAnswerValidatorTest {

    private final SurveyAnswerValidator validator = new SurveyAnswerValidator();

    private static final Long QUESTION_ID = 100L;
    private static final Long OPTION_1_ID = 201L;
    private static final Long OPTION_2_ID = 202L;
    private static final Long ROW_1_ID = 301L;
    private static final Long ROW_2_ID = 302L;

    // ==================== 공통 검증 ====================

    @Nested
    @DisplayName("공통 검증")
    class CommonValidation {

        @DisplayName("존재하지 않는 질문에 대한 답변은 거부된다")
        @Test
        void validate_WithNonExistentQuestion_ThrowsException() {
            Survey survey = createSurvey();
            SurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, QUESTION_ID);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    999L, "답변", null, null, null, null);

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("존재하지 않는 질문");
        }

        @DisplayName("필수 질문에 대한 답변이 누락되면 거부된다")
        @Test
        void validate_WithMissingRequiredAnswer_ThrowsException() {
            Survey survey = createSurvey();
            SurveyQuestion question = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.SHORT_ANSWER, "필수 질문", null, true, 1);
            withId(question, QUESTION_ID);

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of()))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("필수 질문");
        }

        @DisplayName("동일 질문에 대한 중복 답변은 거부된다")
        @Test
        void validate_WithDuplicateQuestionId_ThrowsException() {
            Survey survey = createSurvey();
            SurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, QUESTION_ID);

            SubmitAnswerRequest answer1 = new SubmitAnswerRequest(
                    QUESTION_ID, "답변1", null, null, null, null);
            SubmitAnswerRequest answer2 = new SubmitAnswerRequest(
                    QUESTION_ID, "답변2", null, null, null, null);

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer1, answer2)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("중복 답변");
        }

        @DisplayName("선택 질문은 답변 누락을 허용한다")
        @Test
        void validate_WithMissingOptionalAnswer_DoesNotThrow() {
            Survey survey = createSurvey();
            SurveyQuestion question = createShortAnswerQuestion(survey, 1);
            withId(question, QUESTION_ID);

            assertThatNoException()
                    .isThrownBy(() -> validator.validate(List.of(question), List.of()));
        }
    }

    // ==================== 텍스트 유형 (SHORT_ANSWER, PARAGRAPH) ====================

    @Nested
    @DisplayName("텍스트 유형 검증 (SHORT_ANSWER, PARAGRAPH)")
    class TextTypeValidation {

        @DisplayName("필수 단답형 질문에 텍스트 답변을 제출하면 통과한다")
        @Test
        void validate_ShortAnswer_Required_WithText_Success() {
            Survey survey = createSurvey();
            SurveyQuestion question = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.SHORT_ANSWER, "이름", null, true, 1);
            withId(question, QUESTION_ID);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, "홍길동", null, null, null, null);

            assertThatNoException()
                    .isThrownBy(() -> validator.validate(List.of(question), List.of(answer)));
        }

        @DisplayName("필수 단답형 질문에 빈 텍스트를 제출하면 거부된다")
        @Test
        void validate_ShortAnswer_Required_WithBlank_ThrowsException() {
            Survey survey = createSurvey();
            SurveyQuestion question = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.SHORT_ANSWER, "이름", null, true, 1);
            withId(question, QUESTION_ID);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, "  ", null, null, null, null);

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("텍스트 답변이 비어있습니다");
        }
    }

    // ==================== 단일 선택 유형 (MULTIPLE_CHOICE, DROPDOWN) ====================

    @Nested
    @DisplayName("단일 선택 유형 검증 (MULTIPLE_CHOICE, DROPDOWN)")
    class SingleOptionValidation {

        @DisplayName("필수 객관식 질문에 올바른 옵션을 선택하면 통과한다")
        @Test
        void validate_MultipleChoice_Required_WithValidOption_Success() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "선택", null, true, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "옵션1", 1);
            withId(option, OPTION_1_ID);
            question.addOption(option);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, OPTION_1_ID, null, null);

            assertThatNoException()
                    .isThrownBy(() -> validator.validate(List.of(question), List.of(answer)));
        }

        @DisplayName("필수 객관식 질문에 옵션을 선택하지 않으면 거부된다")
        @Test
        void validate_MultipleChoice_Required_WithNoOption_ThrowsException() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "선택", null, true, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "옵션1", 1);
            withId(option, OPTION_1_ID);
            question.addOption(option);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, null, null);

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("선택지가 지정되지 않았습니다");
        }

        @DisplayName("질문에 속하지 않는 옵션을 선택하면 거부된다")
        @Test
        void validate_MultipleChoice_WithInvalidOption_ThrowsException() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE, "선택", null, false, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "옵션1", 1);
            withId(option, OPTION_1_ID);
            question.addOption(option);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, 999L, null, null);

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("속하지 않는 선택지");
        }
    }

    // ==================== 다중 선택 유형 (CHECKBOX) ====================

    @Nested
    @DisplayName("다중 선택 유형 검증 (CHECKBOX)")
    class MultipleOptionValidation {

        @DisplayName("필수 체크박스 질문에 올바른 옵션을 선택하면 통과한다")
        @Test
        void validate_Checkbox_Required_WithValidOptions_Success() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX, "다중선택", null, true, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option1 = SurveyQuestionOption.create(question, "옵션1", 1);
            withId(option1, OPTION_1_ID);
            SurveyQuestionOption option2 = SurveyQuestionOption.create(question, "옵션2", 2);
            withId(option2, OPTION_2_ID);
            question.addOption(option1);
            question.addOption(option2);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, List.of(OPTION_1_ID, OPTION_2_ID), null);

            assertThatNoException()
                    .isThrownBy(() -> validator.validate(List.of(question), List.of(answer)));
        }

        @DisplayName("필수 체크박스 질문에 빈 목록을 제출하면 거부된다")
        @Test
        void validate_Checkbox_Required_WithEmptyList_ThrowsException() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX, "다중선택", null, true, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "옵션1", 1);
            withId(option, OPTION_1_ID);
            question.addOption(option);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, List.of(), null);

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("최소 1개 이상");
        }

        @DisplayName("중복된 선택지가 포함되면 거부된다")
        @Test
        void validate_Checkbox_WithDuplicateOptionIds_ThrowsException() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX, "다중선택", null, false, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option1 = SurveyQuestionOption.create(question, "옵션1", 1);
            withId(option1, OPTION_1_ID);
            question.addOption(option1);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, List.of(OPTION_1_ID, OPTION_1_ID), null);

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("중복된 선택지");
        }

        @DisplayName("질문에 속하지 않는 옵션이 포함되면 거부된다")
        @Test
        void validate_Checkbox_WithInvalidOption_ThrowsException() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX, "다중선택", null, false, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "옵션1", 1);
            withId(option, OPTION_1_ID);
            question.addOption(option);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, List.of(OPTION_1_ID, 999L), null);

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("속하지 않는 선택지");
        }
    }

    // ==================== 숫자 유형 (LINEAR_SCALE) ====================

    @Nested
    @DisplayName("숫자 유형 검증 (LINEAR_SCALE)")
    class NumericValidation {

        @DisplayName("범위 내 값을 제출하면 통과한다")
        @Test
        void validate_LinearScale_WithValueInRange_Success() {
            Survey survey = createSurvey();
            SurveyQuestion question = createLinearScaleQuestion(survey, 1);
            withId(question, QUESTION_ID);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, 3, null, null, null);

            assertThatNoException()
                    .isThrownBy(() -> validator.validate(List.of(question), List.of(answer)));
        }

        @DisplayName("범위 초과 값을 제출하면 거부된다")
        @Test
        void validate_LinearScale_WithValueOutOfRange_ThrowsException() {
            Survey survey = createSurvey();
            SurveyQuestion question = createLinearScaleQuestion(survey, 1);
            withId(question, QUESTION_ID);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, 10, null, null, null);

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("범위");
        }

        @DisplayName("필수 선형 배율에 값이 없으면 거부된다")
        @Test
        void validate_LinearScale_Required_WithNull_ThrowsException() {
            Survey survey = createSurvey();
            LinearScaleSurveyQuestion question = LinearScaleSurveyQuestion.create(
                    survey, SurveyQuestionType.LINEAR_SCALE, "배율", null, true, 1);
            question.setScaleRange(1, 5);
            withId(question, QUESTION_ID);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, null, null);

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("숫자 답변이 지정되지 않았습니다");
        }
    }

    // ==================== 그리드 유형 (MULTIPLE_CHOICE_GRID, CHECKBOX_GRID) ====================

    @Nested
    @DisplayName("그리드 유형 검증")
    class GridValidation {

        @DisplayName("MC 그리드 - 행당 정확히 1개 옵션을 선택하면 통과한다")
        @Test
        void validate_McGrid_WithOneOptionPerRow_Success() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "MC 그리드", null, true, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "열1", 1);
            withId(option, OPTION_1_ID);
            question.addOption(option);
            SurveyQuestionRow row = SurveyQuestionRow.create(question, "행1", 1);
            withId(row, ROW_1_ID);
            question.addRow(row);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, null,
                    List.of(new GridAnswerRequest(ROW_1_ID, List.of(OPTION_1_ID))));

            assertThatNoException()
                    .isThrownBy(() -> validator.validate(List.of(question), List.of(answer)));
        }

        @DisplayName("MC 그리드 - 행당 2개 이상 옵션을 선택하면 거부된다")
        @Test
        void validate_McGrid_WithMultipleOptionsPerRow_ThrowsException() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "MC 그리드", null, false, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option1 = SurveyQuestionOption.create(question, "열1", 1);
            withId(option1, OPTION_1_ID);
            SurveyQuestionOption option2 = SurveyQuestionOption.create(question, "열2", 2);
            withId(option2, OPTION_2_ID);
            question.addOption(option1);
            question.addOption(option2);
            SurveyQuestionRow row = SurveyQuestionRow.create(question, "행1", 1);
            withId(row, ROW_1_ID);
            question.addRow(row);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, null,
                    List.of(new GridAnswerRequest(ROW_1_ID, List.of(OPTION_1_ID, OPTION_2_ID))));

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("1개의 선택지만");
        }

        @DisplayName("CB 그리드 - 행당 복수 옵션을 선택하면 통과한다")
        @Test
        void validate_CbGrid_WithMultipleOptionsPerRow_Success() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX_GRID, "CB 그리드", null, true, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option1 = SurveyQuestionOption.create(question, "열1", 1);
            withId(option1, OPTION_1_ID);
            SurveyQuestionOption option2 = SurveyQuestionOption.create(question, "열2", 2);
            withId(option2, OPTION_2_ID);
            question.addOption(option1);
            question.addOption(option2);
            SurveyQuestionRow row = SurveyQuestionRow.create(question, "행1", 1);
            withId(row, ROW_1_ID);
            question.addRow(row);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, null,
                    List.of(new GridAnswerRequest(ROW_1_ID, List.of(OPTION_1_ID, OPTION_2_ID))));

            assertThatNoException()
                    .isThrownBy(() -> validator.validate(List.of(question), List.of(answer)));
        }

        @DisplayName("필수 그리드 - 행 답변이 누락되면 거부된다")
        @Test
        void validate_Grid_Required_WithMissingRow_ThrowsException() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "MC 그리드", null, true, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "열1", 1);
            withId(option, OPTION_1_ID);
            question.addOption(option);
            SurveyQuestionRow row1 = SurveyQuestionRow.create(question, "행1", 1);
            withId(row1, ROW_1_ID);
            SurveyQuestionRow row2 = SurveyQuestionRow.create(question, "행2", 2);
            withId(row2, ROW_2_ID);
            question.addRow(row1);
            question.addRow(row2);

            // row2에 대한 답변 누락
            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, null,
                    List.of(new GridAnswerRequest(ROW_1_ID, List.of(OPTION_1_ID))));

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("행 '행2'에 대한 답변이 누락");
        }

        @DisplayName("동일 행에 대한 중복 답변은 거부된다")
        @Test
        void validate_Grid_WithDuplicateRowId_ThrowsException() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID, "MC 그리드", null, false, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option1 = SurveyQuestionOption.create(question, "열1", 1);
            withId(option1, OPTION_1_ID);
            SurveyQuestionOption option2 = SurveyQuestionOption.create(question, "열2", 2);
            withId(option2, OPTION_2_ID);
            question.addOption(option1);
            question.addOption(option2);
            SurveyQuestionRow row = SurveyQuestionRow.create(question, "행1", 1);
            withId(row, ROW_1_ID);
            question.addRow(row);

            // 같은 rowId로 2개 GridAnswerRequest 제출 → MC_GRID 단일 선택 우회 시도
            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, null,
                    List.of(
                            new GridAnswerRequest(ROW_1_ID, List.of(OPTION_1_ID)),
                            new GridAnswerRequest(ROW_1_ID, List.of(OPTION_2_ID))
                    ));

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("중복 답변");
        }

        @DisplayName("그리드 행 내 중복 선택지는 거부된다")
        @Test
        void validate_Grid_WithDuplicateOptionInRow_ThrowsException() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX_GRID, "CB 그리드", null, false, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "열1", 1);
            withId(option, OPTION_1_ID);
            question.addOption(option);
            SurveyQuestionRow row = SurveyQuestionRow.create(question, "행1", 1);
            withId(row, ROW_1_ID);
            question.addRow(row);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, null,
                    List.of(new GridAnswerRequest(ROW_1_ID, List.of(OPTION_1_ID, OPTION_1_ID))));

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("중복된 선택지");
        }

        @DisplayName("질문에 속하지 않는 행 ID를 제출하면 거부된다")
        @Test
        void validate_Grid_WithInvalidRowId_ThrowsException() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX_GRID, "CB 그리드", null, false, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "열1", 1);
            withId(option, OPTION_1_ID);
            question.addOption(option);
            SurveyQuestionRow row = SurveyQuestionRow.create(question, "행1", 1);
            withId(row, ROW_1_ID);
            question.addRow(row);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, null,
                    List.of(new GridAnswerRequest(999L, List.of(OPTION_1_ID))));

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("속하지 않는 행");
        }

        @DisplayName("그리드 행에 빈 옵션 목록을 제출하면 거부된다")
        @Test
        void validate_Grid_WithEmptyOptionsForRow_ThrowsException() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(
                    survey, SurveyQuestionType.CHECKBOX_GRID, "CB 그리드", null, false, 1);
            withId(question, QUESTION_ID);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "열1", 1);
            withId(option, OPTION_1_ID);
            question.addOption(option);
            SurveyQuestionRow row = SurveyQuestionRow.create(question, "행1", 1);
            withId(row, ROW_1_ID);
            question.addRow(row);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, null, null, null, null,
                    List.of(new GridAnswerRequest(ROW_1_ID, List.of())));

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("최소 1개 이상");
        }
    }

    // ==================== 날짜/시간 유형 (DATE, TIME) ====================

    @Nested
    @DisplayName("날짜/시간 유형 검증")
    class DateTimeValidation {

        @DisplayName("올바른 날짜 형식(yyyy-MM-dd)을 제출하면 통과한다")
        @Test
        void validate_Date_WithValidFormat_Success() {
            Survey survey = createSurvey();
            SurveyQuestion question = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.DATE, "날짜", null, true, 1);
            withId(question, QUESTION_ID);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, "2026-01-15", null, null, null, null);

            assertThatNoException()
                    .isThrownBy(() -> validator.validate(List.of(question), List.of(answer)));
        }

        @DisplayName("잘못된 날짜 형식을 제출하면 거부된다")
        @Test
        void validate_Date_WithInvalidFormat_ThrowsException() {
            Survey survey = createSurvey();
            SurveyQuestion question = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.DATE, "날짜", null, false, 1);
            withId(question, QUESTION_ID);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, "01/15/2026", null, null, null, null);

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("날짜 형식");
        }

        @DisplayName("올바른 시간 형식(HH:mm)을 제출하면 통과한다")
        @Test
        void validate_Time_WithValidFormat_Success() {
            Survey survey = createSurvey();
            SurveyQuestion question = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.TIME, "시간", null, true, 1);
            withId(question, QUESTION_ID);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, "14:30", null, null, null, null);

            assertThatNoException()
                    .isThrownBy(() -> validator.validate(List.of(question), List.of(answer)));
        }

        @DisplayName("잘못된 시간 형식을 제출하면 거부된다")
        @Test
        void validate_Time_WithInvalidFormat_ThrowsException() {
            Survey survey = createSurvey();
            SurveyQuestion question = TextSurveyQuestion.create(
                    survey, SurveyQuestionType.TIME, "시간", null, false, 1);
            withId(question, QUESTION_ID);

            SubmitAnswerRequest answer = new SubmitAnswerRequest(
                    QUESTION_ID, "2:30 PM", null, null, null, null);

            assertThatThrownBy(() -> validator.validate(List.of(question), List.of(answer)))
                    .isInstanceOf(SurveyResponseValidationException.class)
                    .hasMessageContaining("시간 형식");
        }
    }
}
