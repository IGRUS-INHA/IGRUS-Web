package igrus.web.survey.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 설문 질문 엔티티.
 * 설문에 포함되는 개별 질문의 정보를 관리합니다.
 * 질문 유형에 따라 선택지(options)와 그리드 행(rows)을 가질 수 있습니다.
 */
@Entity
@Table(name = "survey_questions")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "survey_questions_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "survey_questions_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "survey_questions_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "survey_questions_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "survey_questions_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "survey_questions_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "survey_questions_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyQuestion extends SoftDeletableEntity {

    /** 질문 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_questions_id")
    private Long id;

    /** 소속 설문 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_questions_survey_id", nullable = false)
    private Survey survey;

    /** 질문 유형 */
    @Enumerated(EnumType.STRING)
    @Column(name = "survey_questions_type", nullable = false, length = 30)
    private SurveyQuestionType questionType;

    /** 질문 제목 (필수, 최대 200자) */
    @Column(name = "survey_questions_title", nullable = false, length = 200)
    private String title;

    /** 질문 설명 (선택, 최대 500자) */
    @Column(name = "survey_questions_description", length = 500)
    private String description;

    /** 필수 응답 여부 */
    @Column(name = "survey_questions_required", nullable = false)
    private boolean required;

    /** 표시 순서 (오름차순 정렬) */
    @Column(name = "survey_questions_display_order", nullable = false)
    private int displayOrder;

    /** 선형 배율 최솟값 (LINEAR_SCALE 유형에서만 사용) */
    @Column(name = "survey_questions_scale_min")
    private Integer scaleMin;

    /** 선형 배율 최댓값 (LINEAR_SCALE 유형에서만 사용) */
    @Column(name = "survey_questions_scale_max")
    private Integer scaleMax;

    /** 선택지 목록 (MULTIPLE_CHOICE, CHECKBOX, DROPDOWN, 그리드의 열, soft delete 대상이므로 orphanRemoval 미사용) */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    private List<SurveyQuestionOption> options = new ArrayList<>();

    /** 그리드 행 목록 (MULTIPLE_CHOICE_GRID, CHECKBOX_GRID에서만 사용, soft delete 대상이므로 orphanRemoval 미사용) */
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    @OrderBy("displayOrder ASC")
    private List<SurveyQuestionRow> rows = new ArrayList<>();

    // === Static factory method ===

    /**
     * 설문 질문을 생성합니다.
     *
     * @param survey       소속 설문
     * @param questionType 질문 유형
     * @param title        질문 제목
     * @param description  질문 설명 (null 가능)
     * @param required     필수 응답 여부
     * @param displayOrder 표시 순서
     * @return 생성된 질문
     */
    public static SurveyQuestion create(Survey survey, SurveyQuestionType questionType,
                                        String title, String description,
                                        boolean required, int displayOrder) {
        SurveyQuestion question = new SurveyQuestion();
        question.survey = survey;
        question.questionType = questionType;
        question.title = title;
        question.description = description;
        question.required = required;
        question.displayOrder = displayOrder;
        return question;
    }

    // === Business methods ===

    /**
     * 선형 배율 범위를 설정합니다. LINEAR_SCALE 유형에서만 사용합니다.
     *
     * @param min 최솟값
     * @param max 최댓값
     * @throws IllegalStateException LINEAR_SCALE 유형이 아닌 경우
     * @throws IllegalArgumentException min이 max 이상인 경우
     */
    public void setScaleRange(int min, int max) {
        if (this.questionType != SurveyQuestionType.LINEAR_SCALE) {
            throw new IllegalStateException("LINEAR_SCALE 유형에서만 배율 범위를 설정할 수 있습니다.");
        }
        if (min >= max) {
            throw new IllegalArgumentException("최솟값은 최댓값보다 작아야 합니다.");
        }
        this.scaleMin = min;
        this.scaleMax = max;
    }

    /**
     * 선택지를 추가합니다.
     * MULTIPLE_CHOICE, CHECKBOX, DROPDOWN, 그리드 유형에서 사용합니다.
     *
     * @param option 추가할 선택지
     */
    public void addOption(SurveyQuestionOption option) {
        this.options.add(option);
    }

    /**
     * 그리드 행을 추가합니다.
     * MULTIPLE_CHOICE_GRID, CHECKBOX_GRID 유형에서만 사용합니다.
     *
     * @param row 추가할 행
     */
    public void addRow(SurveyQuestionRow row) {
        this.rows.add(row);
    }
}
