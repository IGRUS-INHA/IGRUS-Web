package igrus.web.survey.question.domain;

import igrus.web.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 설문 질문 선택지 엔티티.
 * 객관식(MULTIPLE_CHOICE), 체크박스(CHECKBOX), 드롭다운(DROPDOWN) 질문의 보기 항목을 관리합니다.
 * 그리드 유형(MULTIPLE_CHOICE_GRID, CHECKBOX_GRID)에서는 열(Column) 역할을 합니다.
 *
 * <p>운영진이 옵션 텍스트를 변경하면 새 옵션이 생성되고 기존 옵션은 archive 처리됩니다.
 * archive된 옵션은 폼 렌더링·신규 응답 검증에서는 제외되지만, 과거 응답·통계에서는 그대로 유지됩니다.</p>
 */
@Entity
@Table(name = "survey_question_options")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "survey_question_options_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "survey_question_options_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "survey_question_options_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "survey_question_options_updated_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyQuestionOption extends BaseEntity {

    /** 선택지 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_question_options_id")
    private Long id;

    /** 소속 질문 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_question_options_question_id", nullable = false)
    private SurveyQuestion question;

    /** 선택지 텍스트 (최대 200자) */
    @Column(name = "survey_question_options_text", nullable = false, length = 200)
    private String text;

    /** 표시 순서 (오름차순 정렬) */
    @Column(name = "survey_question_options_display_order", nullable = false)
    private int displayOrder;

    /** 아카이브 시각 (null이면 활성 옵션, 값이 있으면 폼에서 제외됨) */
    @Column(name = "survey_question_options_archived_at")
    private Instant archivedAt;

    /** 아카이브 수행자 ID */
    @Column(name = "survey_question_options_archived_by")
    private Long archivedBy;

    // === Static factory method ===

    /**
     * 선택지를 생성합니다.
     *
     * @param question     소속 질문
     * @param text         선택지 텍스트
     * @param displayOrder 표시 순서
     * @return 생성된 선택지
     */
    public static SurveyQuestionOption create(SurveyQuestion question, String text, int displayOrder) {
        SurveyQuestionOption option = new SurveyQuestionOption();
        option.question = question;
        option.text = text;
        option.displayOrder = displayOrder;
        return option;
    }

    /**
     * 선택지를 수정합니다. (dirty checking)
     *
     * @param text         선택지 텍스트
     * @param displayOrder 표시 순서
     */
    public void update(String text, int displayOrder) {
        this.text = text;
        this.displayOrder = displayOrder;
    }

    /**
     * 선택지를 아카이브 처리합니다 (응답이 존재할 때 사용).
     * 아카이브된 옵션은 폼·신규 응답 검증에서 제외되지만, 기존 응답과 통계에서는 유지됩니다.
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
