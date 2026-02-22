package igrus.web.survey.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 설문 그리드 질문의 행 엔티티.
 * 객관식 그리드(MULTIPLE_CHOICE_GRID)와 체크박스 그리드(CHECKBOX_GRID) 유형에서만 사용됩니다.
 * 각 행은 하나의 평가 대상 항목을 나타내며, 응답자는 행마다 열(Option)에서 선택합니다.
 */
@Entity
@Table(name = "survey_question_rows")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "survey_question_rows_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "survey_question_rows_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "survey_question_rows_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "survey_question_rows_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "survey_question_rows_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "survey_question_rows_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "survey_question_rows_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyQuestionRow extends SoftDeletableEntity {

    /** 행 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_question_rows_id")
    private Long id;

    /** 소속 질문 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_question_rows_question_id", nullable = false)
    private SurveyQuestion question;

    /** 행 라벨 텍스트 (최대 200자) */
    @Column(name = "survey_question_rows_label", nullable = false, length = 200)
    private String label;

    /** 표시 순서 (오름차순 정렬) */
    @Column(name = "survey_question_rows_display_order", nullable = false)
    private int displayOrder;

    // === Static factory method ===

    /**
     * 그리드 행을 생성합니다.
     *
     * @param question     소속 질문
     * @param label        행 라벨 텍스트
     * @param displayOrder 표시 순서
     * @return 생성된 행
     */
    public static SurveyQuestionRow create(SurveyQuestion question, String label, int displayOrder) {
        SurveyQuestionRow row = new SurveyQuestionRow();
        row.question = question;
        row.label = label;
        row.displayOrder = displayOrder;
        return row;
    }
}
