package igrus.web.survey.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 설문 질문 선택지 엔티티.
 * 객관식(MULTIPLE_CHOICE), 체크박스(CHECKBOX), 드롭다운(DROPDOWN) 질문의 보기 항목을 관리합니다.
 * 그리드 유형(MULTIPLE_CHOICE_GRID, CHECKBOX_GRID)에서는 열(Column) 역할을 합니다.
 */
@Entity
@Table(name = "survey_question_options")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "survey_question_options_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "survey_question_options_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "survey_question_options_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "survey_question_options_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "survey_question_options_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "survey_question_options_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "survey_question_options_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyQuestionOption extends SoftDeletableEntity {

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
}
