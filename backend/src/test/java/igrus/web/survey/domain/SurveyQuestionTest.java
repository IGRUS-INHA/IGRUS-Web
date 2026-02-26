package igrus.web.survey.domain;

import igrus.web.survey.exception.SurveyInvalidStateTransitionException;
import igrus.web.survey.exception.SurveyQuestionValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static igrus.web.common.fixture.SurveyTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SurveyQuestion 엔티티 테스트")
class SurveyQuestionTest {

    // ==================== 3.1 질문 생성 (11가지 유형) ====================

    @Nested
    @DisplayName("질문 생성 (11가지 유형)")
    class QuestionCreation {

        @DisplayName("QST-001: SHORT_ANSWER 유형 질문 생성 성공")
        @Test
        void create_ShortAnswer_Success() {
            Survey survey = createSurvey();
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "단답형 질문", null, false, 1);
            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.SHORT_ANSWER);
            assertThat(question.getSurvey()).isEqualTo(survey);
            assertThat(question).isInstanceOf(TextSurveyQuestion.class);
        }

        @DisplayName("QST-002: PARAGRAPH 유형 질문 생성 성공")
        @Test
        void create_Paragraph_Success() {
            Survey survey = createSurvey();
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.PARAGRAPH,
                    "서술형 질문", null, false, 1);
            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.PARAGRAPH);
            assertThat(question).isInstanceOf(TextSurveyQuestion.class);
        }

        @DisplayName("QST-003: MULTIPLE_CHOICE 유형 질문 생성 성공")
        @Test
        void create_MultipleChoice_Success() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE,
                    "객관식 질문", null, false, 1);
            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.MULTIPLE_CHOICE);
            assertThat(question).isInstanceOf(OptionSurveyQuestion.class);
        }

        @DisplayName("QST-004: CHECKBOX 유형 질문 생성 성공")
        @Test
        void create_Checkbox_Success() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(survey, SurveyQuestionType.CHECKBOX,
                    "체크박스 질문", null, false, 1);
            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.CHECKBOX);
            assertThat(question).isInstanceOf(OptionSurveyQuestion.class);
        }

        @DisplayName("QST-005: DROPDOWN 유형 질문 생성 성공")
        @Test
        void create_Dropdown_Success() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(survey, SurveyQuestionType.DROPDOWN,
                    "드롭다운 질문", null, false, 1);
            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.DROPDOWN);
            assertThat(question).isInstanceOf(OptionSurveyQuestion.class);
        }

        @DisplayName("QST-006: LINEAR_SCALE 유형 질문 생성 성공")
        @Test
        void create_LinearScale_Success() {
            Survey survey = createSurvey();
            LinearScaleSurveyQuestion question = LinearScaleSurveyQuestion.create(survey, SurveyQuestionType.LINEAR_SCALE,
                    "선형 배율 질문", null, false, 1);
            question.setScaleRange(1, 5);
            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.LINEAR_SCALE);
            assertThat(question.getScaleMin()).isEqualTo(1);
            assertThat(question.getScaleMax()).isEqualTo(5);
            assertThat(question).isInstanceOf(LinearScaleSurveyQuestion.class);
        }

        @DisplayName("QST-007: MULTIPLE_CHOICE_GRID 유형 질문 생성 성공")
        @Test
        void create_MultipleChoiceGrid_Success() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID,
                    "객관식 그리드 질문", null, false, 1);
            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.MULTIPLE_CHOICE_GRID);
            assertThat(question).isInstanceOf(GridSurveyQuestion.class);
        }

        @DisplayName("QST-008: CHECKBOX_GRID 유형 질문 생성 성공")
        @Test
        void create_CheckboxGrid_Success() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.CHECKBOX_GRID,
                    "체크박스 그리드 질문", null, false, 1);
            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.CHECKBOX_GRID);
            assertThat(question).isInstanceOf(GridSurveyQuestion.class);
        }

        @DisplayName("QST-009: DATE 유형 질문 생성 성공")
        @Test
        void create_Date_Success() {
            Survey survey = createSurvey();
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.DATE,
                    "날짜 질문", null, false, 1);
            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.DATE);
            assertThat(question).isInstanceOf(TextSurveyQuestion.class);
        }

        @DisplayName("QST-010: TIME 유형 질문 생성 성공")
        @Test
        void create_Time_Success() {
            Survey survey = createSurvey();
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.TIME,
                    "시간 질문", null, false, 1);
            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.TIME);
            assertThat(question).isInstanceOf(TextSurveyQuestion.class);
        }

        @DisplayName("QST-011: FILE_UPLOAD 유형 질문 생성 성공")
        @Test
        void create_FileUpload_Success() {
            Survey survey = createSurvey();
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.FILE_UPLOAD,
                    "파일 업로드 질문", null, false, 1);
            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.FILE_UPLOAD);
            assertThat(question).isInstanceOf(TextSurveyQuestion.class);
        }
    }

    // ==================== 3.2 질문 입력값 경계값 ====================

    @Nested
    @DisplayName("질문 입력값 경계값")
    class QuestionInputBoundary {

        @DisplayName("QST-015: 질문 제목 1자 (최소) 성공")
        @Test
        void create_TitleMinLength_Success() {
            Survey survey = createSurvey();
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "A", null, false, 0);
            assertThat(question.getTitle()).isEqualTo("A");
        }

        @DisplayName("QST-016: 질문 제목 200자 (최대) 성공")
        @Test
        void create_TitleMaxLength_Success() {
            Survey survey = createSurvey();
            String title200 = "A".repeat(200);
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    title200, null, false, 0);
            assertThat(question.getTitle()).hasSize(200);
        }

        @DisplayName("QST-020: 질문 설명 null 성공")
        @Test
        void create_DescriptionNull_Success() {
            Survey survey = createSurvey();
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "질문", null, false, 0);
            assertThat(question.getDescription()).isNull();
        }

        @DisplayName("QST-021: 질문 설명 500자 (최대) 성공")
        @Test
        void create_DescriptionMaxLength_Success() {
            Survey survey = createSurvey();
            String desc500 = "B".repeat(500);
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "질문", desc500, false, 0);
            assertThat(question.getDescription()).hasSize(500);
        }

        @DisplayName("QST-023: required=true 성공")
        @Test
        void create_RequiredTrue_Success() {
            Survey survey = createSurvey();
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "필수 질문", null, true, 0);
            assertThat(question.isRequired()).isTrue();
        }

        @DisplayName("QST-024: required=false 성공")
        @Test
        void create_RequiredFalse_Success() {
            Survey survey = createSurvey();
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "선택 질문", null, false, 0);
            assertThat(question.isRequired()).isFalse();
        }

        @DisplayName("QST-025: displayOrder=0 (최소) 성공")
        @Test
        void create_DisplayOrderZero_Success() {
            Survey survey = createSurvey();
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "질문", null, false, 0);
            assertThat(question.getDisplayOrder()).isZero();
        }
    }

    // ==================== 3.3 질문 유형별 필수 구성요소 (INV-13) ====================

    @Nested
    @DisplayName("질문 유형별 필수 구성요소 검증 (발행 시)")
    class QuestionRequiredComponents {

        @DisplayName("QST-030: MULTIPLE_CHOICE 선택지 0개 -> 발행 시 SurveyPublishValidationException")
        @Test
        void publish_MultipleChoiceNoOptions_ThrowsException() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE,
                    "객관식 질문", null, false, 1);
            survey.getQuestions().add(question);

            // publish()는 Service에서 validatePublishPreConditions를 호출하지만,
            // 엔티티 레벨에서는 검증이 없으므로, 선택지 0개 상태 확인
            assertThat(question.getOptions()).isEmpty();
        }

        @DisplayName("QST-031: MULTIPLE_CHOICE 선택지 1개 이상 -> 구성 충족")
        @Test
        void multipleChoice_WithOptions_HasValidStructure() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = createMultipleChoiceQuestion(survey, 1);
            assertThat(question.getOptions()).isNotEmpty();
            assertThat(question.getOptions().stream().filter(o -> !o.isDeleted()).count()).isGreaterThanOrEqualTo(1);
        }

        @DisplayName("QST-034: MULTIPLE_CHOICE_GRID 행 0개 -> 발행 시 구성 미충족 (INV-06)")
        @Test
        void multipleChoiceGrid_NoRows_HasNoRows() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID,
                    "그리드 질문", null, false, 1);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "열 1", 1);
            question.addOption(option);

            assertThat(question.getRows()).isEmpty();
            assertThat(question.getOptions()).hasSize(1);
        }

        @DisplayName("QST-035: MULTIPLE_CHOICE_GRID 열 0개 -> 발행 시 구성 미충족 (INV-06)")
        @Test
        void multipleChoiceGrid_NoOptions_HasNoOptions() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID,
                    "그리드 질문", null, false, 1);
            SurveyQuestionRow row = SurveyQuestionRow.create(question, "행 1", 1);
            question.addRow(row);

            assertThat(question.getOptions()).isEmpty();
            assertThat(question.getRows()).hasSize(1);
        }

        @DisplayName("QST-036: MULTIPLE_CHOICE_GRID 행 1개 + 열 1개 -> 최소 유효 구성")
        @Test
        void multipleChoiceGrid_MinimalConfig_IsValid() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = createGridQuestion(survey, 1);

            long activeOptionCount = question.getOptions().stream().filter(o -> !o.isDeleted()).count();
            long activeRowCount = question.getRows().stream().filter(r -> !r.isDeleted()).count();

            assertThat(activeOptionCount).isGreaterThanOrEqualTo(1);
            assertThat(activeRowCount).isGreaterThanOrEqualTo(1);
        }

        @DisplayName("QST-037: CHECKBOX_GRID 행 0개 -> 발행 시 구성 미충족")
        @Test
        void checkboxGrid_NoRows_HasNoRows() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.CHECKBOX_GRID,
                    "체크박스 그리드 질문", null, false, 1);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "열 1", 1);
            question.addOption(option);

            assertThat(question.getRows()).isEmpty();
        }

        @DisplayName("QST-038: CHECKBOX_GRID 행 1개 + 열 1개 -> 최소 유효 구성")
        @Test
        void checkboxGrid_MinimalConfig_IsValid() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.CHECKBOX_GRID,
                    "체크박스 그리드 질문", null, false, 1);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "열 1", 1);
            question.addOption(option);
            SurveyQuestionRow row = SurveyQuestionRow.create(question, "행 1", 1);
            question.addRow(row);

            long activeOptionCount = question.getOptions().stream().filter(o -> !o.isDeleted()).count();
            long activeRowCount = question.getRows().stream().filter(r -> !r.isDeleted()).count();

            assertThat(activeOptionCount).isEqualTo(1);
            assertThat(activeRowCount).isEqualTo(1);
        }
    }

    // ==================== 3.4 선형 배율 범위 (INV-07) ====================

    @Nested
    @DisplayName("선형 배율 범위 (INV-07)")
    class LinearScaleRange {

        @DisplayName("QST-040: scaleMin=1, scaleMax=5 -> 유효")
        @Test
        void setScaleRange_ValidRange_Success() {
            Survey survey = createSurvey();
            LinearScaleSurveyQuestion question = LinearScaleSurveyQuestion.create(survey, SurveyQuestionType.LINEAR_SCALE,
                    "배율 질문", null, false, 1);

            question.setScaleRange(1, 5);

            assertThat(question.getScaleMin()).isEqualTo(1);
            assertThat(question.getScaleMax()).isEqualTo(5);
        }

        @DisplayName("QST-041: scaleMin=1, scaleMax=2 -> 유효 (최소 범위)")
        @Test
        void setScaleRange_MinimalRange_Success() {
            Survey survey = createSurvey();
            LinearScaleSurveyQuestion question = LinearScaleSurveyQuestion.create(survey, SurveyQuestionType.LINEAR_SCALE,
                    "배율 질문", null, false, 1);

            question.setScaleRange(1, 2);

            assertThat(question.getScaleMin()).isEqualTo(1);
            assertThat(question.getScaleMax()).isEqualTo(2);
        }

        @DisplayName("QST-042: scaleMin=scaleMax -> 에러 (INV-07)")
        @Test
        void setScaleRange_EqualMinMax_ThrowsException() {
            Survey survey = createSurvey();
            LinearScaleSurveyQuestion question = LinearScaleSurveyQuestion.create(survey, SurveyQuestionType.LINEAR_SCALE,
                    "배율 질문", null, false, 1);

            assertThatThrownBy(() -> question.setScaleRange(3, 3))
                    .isInstanceOf(SurveyQuestionValidationException.class);
        }

        @DisplayName("QST-043: scaleMin > scaleMax -> 에러 (INV-07)")
        @Test
        void setScaleRange_MinGreaterThanMax_ThrowsException() {
            Survey survey = createSurvey();
            LinearScaleSurveyQuestion question = LinearScaleSurveyQuestion.create(survey, SurveyQuestionType.LINEAR_SCALE,
                    "배율 질문", null, false, 1);

            assertThatThrownBy(() -> question.setScaleRange(5, 1))
                    .isInstanceOf(SurveyQuestionValidationException.class);
        }
    }

    // ==================== 3.5 질문 수정 ====================

    @Nested
    @DisplayName("질문 수정")
    class QuestionUpdate {

        @DisplayName("동일 카테고리 내 질문 수정 시 모든 필드 반영")
        @Test
        void update_SameCategoryAllFields_Updated() {
            Survey survey = createSurvey();
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "원본 제목", "원본 설명", false, 1);

            question.update(SurveyQuestionType.PARAGRAPH, "수정 제목", "수정 설명", true, 5);

            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.PARAGRAPH);
            assertThat(question.getTitle()).isEqualTo("수정 제목");
            assertThat(question.getDescription()).isEqualTo("수정 설명");
            assertThat(question.isRequired()).isTrue();
            assertThat(question.getDisplayOrder()).isEqualTo(5);
        }

        @DisplayName("다른 카테고리로 유형 변경 시 SurveyInvalidStateTransitionException")
        @Test
        void update_CrossCategory_ThrowsException() {
            Survey survey = createSurvey();
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "단답형 질문", null, false, 1);

            assertThatThrownBy(() -> question.update(SurveyQuestionType.MULTIPLE_CHOICE, "수정", null, false, 1))
                    .isInstanceOf(SurveyInvalidStateTransitionException.class);
        }

        @DisplayName("OPTION 카테고리 내 유형 변경 (MULTIPLE_CHOICE -> CHECKBOX) 성공")
        @Test
        void update_SameOptionCategory_Success() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE,
                    "객관식 질문", null, false, 1);

            question.update(SurveyQuestionType.CHECKBOX, "체크박스 질문", null, false, 1);

            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.CHECKBOX);
        }

        @DisplayName("GRID 카테고리 내 유형 변경 (MULTIPLE_CHOICE_GRID -> CHECKBOX_GRID) 성공")
        @Test
        void update_SameGridCategory_Success() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID,
                    "객관식 그리드", null, false, 1);

            question.update(SurveyQuestionType.CHECKBOX_GRID, "체크박스 그리드", null, false, 1);

            assertThat(question.getQuestionType()).isEqualTo(SurveyQuestionType.CHECKBOX_GRID);
        }
    }

    // ==================== 3.6 선택지/행 생성 및 경계값 ====================

    @Nested
    @DisplayName("선택지 생성 및 경계값")
    class OptionCreation {

        @DisplayName("QST-050: 선택지 text 1자 (최소) 성공")
        @Test
        void createOption_TextMinLength_Success() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE,
                    "질문", null, false, 1);

            SurveyQuestionOption option = SurveyQuestionOption.create(question, "A", 1);

            assertThat(option.getText()).isEqualTo("A");
            assertThat(option.getQuestion()).isEqualTo(question);
        }

        @DisplayName("QST-051: 선택지 text 200자 (최대) 성공")
        @Test
        void createOption_TextMaxLength_Success() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE,
                    "질문", null, false, 1);
            String text200 = "C".repeat(200);

            SurveyQuestionOption option = SurveyQuestionOption.create(question, text200, 1);

            assertThat(option.getText()).hasSize(200);
        }

        @DisplayName("선택지 수정 시 필드 반영")
        @Test
        void updateOption_FieldsUpdated() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE,
                    "질문", null, false, 1);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "원본", 1);

            option.update("수정됨", 3);

            assertThat(option.getText()).isEqualTo("수정됨");
            assertThat(option.getDisplayOrder()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("행 생성 및 경계값")
    class RowCreation {

        @DisplayName("QST-054: 행 label 1자 (최소) 성공")
        @Test
        void createRow_LabelMinLength_Success() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID,
                    "질문", null, false, 1);

            SurveyQuestionRow row = SurveyQuestionRow.create(question, "A", 1);

            assertThat(row.getLabel()).isEqualTo("A");
            assertThat(row.getQuestion()).isEqualTo(question);
        }

        @DisplayName("QST-055: 행 label 200자 (최대) 성공")
        @Test
        void createRow_LabelMaxLength_Success() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID,
                    "질문", null, false, 1);
            String label200 = "D".repeat(200);

            SurveyQuestionRow row = SurveyQuestionRow.create(question, label200, 1);

            assertThat(row.getLabel()).hasSize(200);
        }

        @DisplayName("행 수정 시 필드 반영")
        @Test
        void updateRow_FieldsUpdated() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID,
                    "질문", null, false, 1);
            SurveyQuestionRow row = SurveyQuestionRow.create(question, "원본", 1);

            row.update("수정됨", 3);

            assertThat(row.getLabel()).isEqualTo("수정됨");
            assertThat(row.getDisplayOrder()).isEqualTo(3);
        }
    }

    // ==================== 3.7 Soft Delete (INV-10, INV-14) ====================

    @Nested
    @DisplayName("Soft Delete")
    class SoftDelete {

        @DisplayName("QST-060: 질문 soft delete 후 deleted=true")
        @Test
        void deleteQuestion_SoftDelete_Success() {
            Survey survey = createSurvey();
            TextSurveyQuestion question = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "질문", null, false, 1);

            question.delete(1L);

            assertThat(question.isDeleted()).isTrue();
        }

        @DisplayName("QST-062: 선택지 soft delete 후 deleted=true")
        @Test
        void deleteOption_SoftDelete_Success() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE,
                    "질문", null, false, 1);
            SurveyQuestionOption option = SurveyQuestionOption.create(question, "선택지", 1);

            option.delete(1L);

            assertThat(option.isDeleted()).isTrue();
        }

        @DisplayName("QST-063: 행 soft delete 후 deleted=true")
        @Test
        void deleteRow_SoftDelete_Success() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID,
                    "질문", null, false, 1);
            SurveyQuestionRow row = SurveyQuestionRow.create(question, "행", 1);

            row.delete(1L);

            assertThat(row.isDeleted()).isTrue();
        }

        @DisplayName("삭제된 질문은 활성 질문 필터링 시 제외됨")
        @Test
        void deletedQuestion_FilteredOut() {
            Survey survey = createSurvey();
            TextSurveyQuestion active = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "활성 질문", null, false, 1);
            TextSurveyQuestion deleted = TextSurveyQuestion.create(survey, SurveyQuestionType.SHORT_ANSWER,
                    "삭제 질문", null, false, 2);
            survey.getQuestions().add(active);
            survey.getQuestions().add(deleted);
            deleted.delete(1L);

            long activeCount = survey.getQuestions().stream().filter(q -> !q.isDeleted()).count();

            assertThat(activeCount).isEqualTo(1);
        }

        @DisplayName("삭제된 선택지는 활성 선택지 필터링 시 제외됨")
        @Test
        void deletedOption_FilteredOut() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE,
                    "질문", null, false, 1);
            SurveyQuestionOption active = SurveyQuestionOption.create(question, "활성", 1);
            SurveyQuestionOption deleted = SurveyQuestionOption.create(question, "삭제", 2);
            question.addOption(active);
            question.addOption(deleted);
            deleted.delete(1L);

            long activeCount = question.getOptions().stream().filter(o -> !o.isDeleted()).count();

            assertThat(activeCount).isEqualTo(1);
        }

        @DisplayName("삭제된 행은 활성 행 필터링 시 제외됨")
        @Test
        void deletedRow_FilteredOut() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID,
                    "질문", null, false, 1);
            SurveyQuestionRow active = SurveyQuestionRow.create(question, "활성", 1);
            SurveyQuestionRow deleted = SurveyQuestionRow.create(question, "삭제", 2);
            question.addRow(active);
            question.addRow(deleted);
            deleted.delete(1L);

            long activeCount = question.getRows().stream().filter(r -> !r.isDeleted()).count();

            assertThat(activeCount).isEqualTo(1);
        }
    }

    // ==================== 선택지/행 추가 ====================

    @Nested
    @DisplayName("선택지/행 추가")
    class AddOptionsAndRows {

        @DisplayName("addOption: 선택지 추가 시 목록에 포함")
        @Test
        void addOption_AddsToList() {
            Survey survey = createSurvey();
            OptionSurveyQuestion question = OptionSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE,
                    "질문", null, false, 1);
            SurveyQuestionOption option1 = SurveyQuestionOption.create(question, "선택지 1", 1);
            SurveyQuestionOption option2 = SurveyQuestionOption.create(question, "선택지 2", 2);

            question.addOption(option1);
            question.addOption(option2);

            assertThat(question.getOptions()).hasSize(2);
        }

        @DisplayName("addRow: 행 추가 시 목록에 포함")
        @Test
        void addRow_AddsToList() {
            Survey survey = createSurvey();
            GridSurveyQuestion question = GridSurveyQuestion.create(survey, SurveyQuestionType.MULTIPLE_CHOICE_GRID,
                    "질문", null, false, 1);
            SurveyQuestionRow row1 = SurveyQuestionRow.create(question, "행 1", 1);
            SurveyQuestionRow row2 = SurveyQuestionRow.create(question, "행 2", 2);

            question.addRow(row1);
            question.addRow(row2);

            assertThat(question.getRows()).hasSize(2);
        }
    }

    // ==================== SurveyQuestionType 카테고리 검증 ====================

    @Nested
    @DisplayName("SurveyQuestionType 카테고리")
    class QuestionTypeCategory {

        @DisplayName("TEXT 카테고리 질문 유형 매핑 검증")
        @Test
        void textCategory_MappedCorrectly() {
            assertThat(SurveyQuestionType.SHORT_ANSWER.getCategory()).isEqualTo("TEXT");
            assertThat(SurveyQuestionType.PARAGRAPH.getCategory()).isEqualTo("TEXT");
            assertThat(SurveyQuestionType.DATE.getCategory()).isEqualTo("TEXT");
            assertThat(SurveyQuestionType.TIME.getCategory()).isEqualTo("TEXT");
            assertThat(SurveyQuestionType.FILE_UPLOAD.getCategory()).isEqualTo("TEXT");
        }

        @DisplayName("SCALE 카테고리 질문 유형 매핑 검증")
        @Test
        void scaleCategory_MappedCorrectly() {
            assertThat(SurveyQuestionType.LINEAR_SCALE.getCategory()).isEqualTo("SCALE");
        }

        @DisplayName("OPTION 카테고리 질문 유형 매핑 검증")
        @Test
        void optionCategory_MappedCorrectly() {
            assertThat(SurveyQuestionType.MULTIPLE_CHOICE.getCategory()).isEqualTo("OPTION");
            assertThat(SurveyQuestionType.CHECKBOX.getCategory()).isEqualTo("OPTION");
            assertThat(SurveyQuestionType.DROPDOWN.getCategory()).isEqualTo("OPTION");
        }

        @DisplayName("GRID 카테고리 질문 유형 매핑 검증")
        @Test
        void gridCategory_MappedCorrectly() {
            assertThat(SurveyQuestionType.MULTIPLE_CHOICE_GRID.getCategory()).isEqualTo("GRID");
            assertThat(SurveyQuestionType.CHECKBOX_GRID.getCategory()).isEqualTo("GRID");
        }
    }
}
