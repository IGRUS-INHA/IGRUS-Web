package igrus.web.survey.response.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import igrus.web.survey.question.domain.SurveyQuestion;
import igrus.web.survey.question.domain.SurveyQuestionOption;
import igrus.web.survey.question.domain.SurveyQuestionRow;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 설문 개별 답변 엔티티 (추상 부모 클래스).
 * Single Table Inheritance 전략을 사용하여 답변 유형별 서브클래스로 분리합니다.
 *
 * <ul>
 *   <li>{@link TextSurveyAnswer} - SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD</li>
 *   <li>{@link OptionSurveyAnswer} - MULTIPLE_CHOICE, CHECKBOX, DROPDOWN</li>
 *   <li>{@link NumericSurveyAnswer} - LINEAR_SCALE</li>
 *   <li>{@link GridSurveyAnswer} - MULTIPLE_CHOICE_GRID, CHECKBOX_GRID</li>
 * </ul>
 */
@Entity
@Table(name = "survey_answers")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "survey_answers_answer_type", length = 10)
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "survey_answers_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "survey_answers_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "survey_answers_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "survey_answers_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "survey_answers_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "survey_answers_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "survey_answers_deleted_by"))
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class SurveyAnswer extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_answers_id")
    @Getter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_answers_response_id", nullable = false)
    @Getter
    private SurveyResponse response;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_answers_question_id", nullable = false)
    @Getter
    private SurveyQuestion question;

    // === 서브클래스 전용 필드 (JPA 매핑용, getter는 서브클래스에서만 노출) ===

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_answers_option_id")
    protected SurveyQuestionOption selectedOption;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_answers_row_id")
    protected SurveyQuestionRow selectedRow;

    @Column(name = "survey_answers_text_value", columnDefinition = "TEXT")
    protected String textValue;

    @Column(name = "survey_answers_numeric_value")
    protected Integer numericValue;

    protected SurveyAnswer(SurveyResponse response, SurveyQuestion question) {
        this.response = response;
        this.question = question;
    }
}
