package igrus.web.survey.domain;

import igrus.web.common.domain.SoftDeletableEntity;
import igrus.web.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 설문 응답 엔티티.
 * 응답자가 설문에 제출한 응답 세션을 나타냅니다.
 * 회원은 설문당 1회만 응답 가능하며, 비회원(PUBLIC 설문)은 user가 null입니다.
 */
@Entity
@Table(name = "survey_responses", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_survey_responses_survey_user",
                columnNames = {"survey_responses_survey_id", "survey_responses_user_id"}
        )
})
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "survey_responses_created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "survey_responses_updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "survey_responses_created_by", updatable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "survey_responses_updated_by")),
        @AttributeOverride(name = "deleted", column = @Column(name = "survey_responses_deleted", nullable = false)),
        @AttributeOverride(name = "deletedAt", column = @Column(name = "survey_responses_deleted_at")),
        @AttributeOverride(name = "deletedBy", column = @Column(name = "survey_responses_deleted_by"))
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SurveyResponse extends SoftDeletableEntity {

    /** 응답 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "survey_responses_id")
    private Long id;

    /** 응답 대상 설문 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_responses_survey_id", nullable = false)
    private Survey survey;

    /** 응답자 (비회원 응답 시 null) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_responses_user_id")
    private User user;

    /** 개별 답변 목록 */
    @OneToMany(mappedBy = "response", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SurveyAnswer> answers = new ArrayList<>();

    // === Static factory methods ===

    /**
     * 회원 응답을 생성합니다.
     *
     * @param survey 응답 대상 설문
     * @param user   응답자
     * @return 생성된 응답
     */
    public static SurveyResponse create(Survey survey, User user) {
        SurveyResponse response = new SurveyResponse();
        response.survey = survey;
        response.user = user;
        return response;
    }

    /**
     * 비회원(익명) 응답을 생성합니다. PUBLIC 설문에서만 사용합니다.
     *
     * @param survey 응답 대상 설문
     * @return 생성된 익명 응답
     */
    public static SurveyResponse createAnonymous(Survey survey) {
        SurveyResponse response = new SurveyResponse();
        response.survey = survey;
        response.user = null;
        return response;
    }

    /**
     * 답변을 추가합니다.
     *
     * @param answer 추가할 답변
     */
    public void addAnswer(SurveyAnswer answer) {
        this.answers.add(answer);
    }
}
