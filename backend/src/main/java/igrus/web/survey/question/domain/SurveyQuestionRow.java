package igrus.web.survey.question.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 설문 그리드 질문의 행 엔티티.
 * 객관식 그리드(MULTIPLE_CHOICE_GRID)와 체크박스 그리드(CHECKBOX_GRID) 유형에서만 사용됩니다.
 * 각 행은 하나의 평가 대상 항목을 나타내며, 응답자는 행마다 열(Option)에서 선택합니다.
 *
 * <p>운영진이 행을 수정·삭제해도 응답이 존재하면 archive 처리되어, 과거 응답·통계에서 그대로 유지됩니다.</p>
 */
@Entity
@Table(name = "survey_question_rows")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "survey_question_rows_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "survey_question_rows_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "survey_question_rows_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "survey_question_rows_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyQuestionRow extends BaseEntity {

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

    /** 아카이브 시각 (null이면 활성, 값이 있으면 폼에서 제외됨) */
    @Column(name = "survey_question_rows_archived_at")
    private Instant archivedAt;

    /** 아카이브 수행자 ID */
    @Column(name = "survey_question_rows_archived_by")
    private Long archivedBy;

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

    /**
     * 행을 수정합니다. (dirty checking)
     *
     * @param label        행 라벨 텍스트
     * @param displayOrder 표시 순서
     */
    public void update(String label, int displayOrder) {
        this.label = label;
        this.displayOrder = displayOrder;
    }

    /**
     * 행을 아카이브 처리합니다 (응답이 존재할 때 사용).
     */
    public void archive(Long userId) {
        this.archivedAt = Instant.now();
        this.archivedBy = userId;
    }

    /** 아카이브 해제 */
    public void unarchive() {
        this.archivedAt = null;
        this.archivedBy = null;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }
}
