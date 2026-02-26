package igrus.web.survey.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import igrus.web.survey.exception.SurveyInvalidStateTransitionException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DiscriminatorFormula;

import java.util.ArrayList;
import java.util.List;

/**
 * 설문 질문 엔티티 (추상 부모 클래스).
 * Single Table Inheritance 전략을 사용하여 질문 유형별 서브클래스로 분리합니다.
 *
 * <ul>
 *   <li>{@link TextSurveyQuestion} - SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD</li>
 *   <li>{@link LinearScaleSurveyQuestion} - LINEAR_SCALE</li>
 *   <li>{@link OptionSurveyQuestion} - MULTIPLE_CHOICE, CHECKBOX, DROPDOWN</li>
 *   <li>{@link GridSurveyQuestion} - MULTIPLE_CHOICE_GRID, CHECKBOX_GRID</li>
 * </ul>
 */
@Entity
@Table(name = "survey_questions")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorFormula("CASE " +
        "WHEN survey_questions_type IN ('SHORT_ANSWER','PARAGRAPH','DATE','TIME','FILE_UPLOAD') THEN 'TEXT' " +
        "WHEN survey_questions_type = 'LINEAR_SCALE' THEN 'SCALE' " +
        "WHEN survey_questions_type IN ('MULTIPLE_CHOICE','CHECKBOX','DROPDOWN') THEN 'OPTION' " +
        "WHEN survey_questions_type IN ('MULTIPLE_CHOICE_GRID','CHECKBOX_GRID') THEN 'GRID' " +
        "END")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "survey_questions_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "survey_questions_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "survey_questions_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "survey_questions_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "survey_questions_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "survey_questions_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "survey_questions_deleted_by"))
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class SurveyQuestion extends SoftDeletableEntity {

    /** 질문 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_questions_id")
    @Getter
    private Long id;

    /** 소속 설문 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_questions_survey_id", nullable = false)
    @Getter
    private Survey survey;

    /** 질문 유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "survey_questions_type", nullable = false, length = 30)
    @Getter
    private SurveyQuestionType questionType;

    /** 질문 제목 (필수, 최대 200자) */
    @Column(name = "survey_questions_title", nullable = false, length = 200)
    @Getter
    private String title;

    /** 질문 설명 (선택, 최대 500자) */
    @Column(name = "survey_questions_description", length = 500)
    @Getter
    private String description;

    /** 필수 응답 여부 */
    @Column(name = "survey_questions_required", nullable = false)
    @Getter
    private boolean required;

    /** 표시 순서 (오름차순 정렬) */
    @Column(name = "survey_questions_display_order", nullable = false)
    @Getter
    private int displayOrder;

    // === 서브클래스 전용 필드 (JPA 매핑용, getter는 서브클래스에서만 노출) ===

    /** 선형 배율 최솟값 (LINEAR_SCALE 유형에서만 사용) */
    @Column(name = "survey_questions_scale_min")
    protected Integer scaleMin;

    /** 선형 배율 최댓값 (LINEAR_SCALE 유형에서만 사용) */
    @Column(name = "survey_questions_scale_max")
    protected Integer scaleMax;

    /** 선택지 목록 (MULTIPLE_CHOICE, CHECKBOX, DROPDOWN, 그리드의 열, soft delete 대상이므로 orphanRemoval 미사용) */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    protected List<SurveyQuestionOption> options = new ArrayList<>();

    /** 그리드 행 목록 (MULTIPLE_CHOICE_GRID, CHECKBOX_GRID에서만 사용, soft delete 대상이므로 orphanRemoval 미사용) */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    protected List<SurveyQuestionRow> rows = new ArrayList<>();

    // === Protected constructor (서브클래스에서 사용) ===

    protected SurveyQuestion(Survey survey, SurveyQuestionType questionType,
                              String title, String description,
                              boolean required, int displayOrder) {
        this.survey = survey;
        this.questionType = questionType;
        this.title = title;
        this.description = description;
        this.required = required;
        this.displayOrder = displayOrder;
    }

    // === Business methods ===

    /**
     * 질문 정보를 수정합니다. 모든 상태에서 호출 가능합니다.
     * 동일 카테고리 내에서만 유형 변경이 가능합니다.
     *
     * @param questionType 질문 유형
     * @param title        질문 제목
     * @param description  질문 설명
     * @param required     필수 응답 여부
     * @param displayOrder 표시 순서
     * @throws SurveyInvalidStateTransitionException 다른 카테고리로 유형 변경 시
     */
    public void update(SurveyQuestionType questionType, String title, String description,
                       boolean required, int displayOrder) {
        if (!this.questionType.getCategory().equals(questionType.getCategory())) {
            throw new SurveyInvalidStateTransitionException(
                    "다른 카테고리의 질문 유형으로 변경할 수 없습니다.");
        }
        this.questionType = questionType;
        this.title = title;
        this.description = description;
        this.required = required;
        this.displayOrder = displayOrder;
    }
}
