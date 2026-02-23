package igrus.web.survey.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 설문 개별 답변 엔티티.
 * 응답자가 각 질문에 대해 제출한 답변 데이터를 저장합니다.
 * 질문 유형에 따라 사용되는 필드가 다릅니다.
 *
 * <ul>
 *   <li>SHORT_ANSWER, PARAGRAPH → textValue</li>
 *   <li>MULTIPLE_CHOICE, DROPDOWN → selectedOption</li>
 *   <li>CHECKBOX → selectedOption (선택지당 1 row)</li>
 *   <li>LINEAR_SCALE → numericValue</li>
 *   <li>MULTIPLE_CHOICE_GRID → selectedRow + selectedOption (행마다 1 row)</li>
 *   <li>CHECKBOX_GRID → selectedRow + selectedOption (행×선택지마다 1 row)</li>
 *   <li>DATE, TIME → textValue</li>
 *   <li>FILE_UPLOAD → textValue (파일 URL)</li>
 * </ul>
 */
@Entity
@Table(name = "survey_answers")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "survey_answers_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "survey_answers_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "survey_answers_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "survey_answers_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "survey_answers_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "survey_answers_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "survey_answers_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyAnswer extends SoftDeletableEntity {

    /** 답변 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_answers_id")
    private Long id;

    /** 소속 응답 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_answers_response_id", nullable = false)
    private SurveyResponse response;

    /** 답변 대상 질문 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_answers_question_id", nullable = false)
    private SurveyQuestion question;

    /** 선택된 선택지 (MULTIPLE_CHOICE, CHECKBOX, DROPDOWN, 그리드 유형에서 사용) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_answers_option_id")
    private SurveyQuestionOption selectedOption;

    /** 선택된 그리드 행 (MULTIPLE_CHOICE_GRID, CHECKBOX_GRID에서 사용) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_answers_row_id")
    private SurveyQuestionRow selectedRow;

    /** 텍스트 답변 (SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD에서 사용) */
    @Column(name = "survey_answers_text_value", columnDefinition = "TEXT")
    private String textValue;

    /** 숫자 답변 (LINEAR_SCALE에서 사용) */
    @Column(name = "survey_answers_numeric_value")
    private Integer numericValue;

    // === Static factory methods ===

    /**
     * 텍스트 답변을 생성합니다. (SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD)
     *
     * @param response  소속 응답
     * @param question  답변 대상 질문
     * @param textValue 텍스트 값
     * @return 생성된 답변
     */
    public static SurveyAnswer ofText(SurveyResponse response, SurveyQuestion question, String textValue) {
        SurveyAnswer answer = new SurveyAnswer();
        answer.response = response;
        answer.question = question;
        answer.textValue = textValue;
        return answer;
    }

    /**
     * 선택지 답변을 생성합니다. (MULTIPLE_CHOICE, CHECKBOX, DROPDOWN)
     *
     * @param response       소속 응답
     * @param question       답변 대상 질문
     * @param selectedOption 선택한 선택지
     * @return 생성된 답변
     */
    public static SurveyAnswer ofOption(SurveyResponse response, SurveyQuestion question,
                                        SurveyQuestionOption selectedOption) {
        SurveyAnswer answer = new SurveyAnswer();
        answer.response = response;
        answer.question = question;
        answer.selectedOption = selectedOption;
        return answer;
    }

    /**
     * 숫자 답변을 생성합니다. (LINEAR_SCALE)
     *
     * @param response     소속 응답
     * @param question     답변 대상 질문
     * @param numericValue 숫자 값
     * @return 생성된 답변
     */
    public static SurveyAnswer ofNumeric(SurveyResponse response, SurveyQuestion question, int numericValue) {
        SurveyAnswer answer = new SurveyAnswer();
        answer.response = response;
        answer.question = question;
        answer.numericValue = numericValue;
        return answer;
    }

    /**
     * 그리드 답변을 생성합니다. (MULTIPLE_CHOICE_GRID, CHECKBOX_GRID)
     *
     * @param response       소속 응답
     * @param question       답변 대상 질문
     * @param selectedRow    선택한 행
     * @param selectedOption 선택한 열(선택지)
     * @return 생성된 답변
     */
    public static SurveyAnswer ofGrid(SurveyResponse response, SurveyQuestion question,
                                      SurveyQuestionRow selectedRow, SurveyQuestionOption selectedOption) {
        SurveyAnswer answer = new SurveyAnswer();
        answer.response = response;
        answer.question = question;
        answer.selectedRow = selectedRow;
        answer.selectedOption = selectedOption;
        return answer;
    }
}
