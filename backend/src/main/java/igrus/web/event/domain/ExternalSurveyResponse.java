package igrus.web.event.domain;

import igrus.web.survey.domain.Survey;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 외부인 설문 응답 엔티티. (DECISION-04: 옵션 B - 별도 테이블)
 * 외부인의 설문 응답을 별도 테이블에 저장합니다.
 * BaseEntity를 상속하지 않습니다 (createdBy가 항상 null이므로 불필요).
 */
@Entity
@Table(name = "external_survey_responses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExternalSurveyResponse {

    /** 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "external_survey_responses_id")
    private Long id;

    /** 설문 (surveys FK) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_survey_responses_survey_id", nullable = false)
    private Survey survey;

    /** 행사 신청 (event_registrations FK) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_survey_responses_registration_id", nullable = false)
    private EventRegistration registration;

    /** 외부인 학번 */
    @Column(name = "external_survey_responses_student_id", nullable = false, length = 20)
    private String studentId;

    /** 설문 응답 데이터 (JSON) */
    @Column(name = "external_survey_responses_answers", nullable = false, columnDefinition = "JSON")
    private String answers;

    /** 생성 시각 */
    @Column(name = "external_survey_responses_created_at", nullable = false)
    private Instant createdAt;

    // === 정적 팩토리 메서드 ===

    /**
     * 외부인 설문 응답을 생성합니다.
     *
     * @param survey       설문
     * @param registration 행사 신청
     * @param studentId    외부인 학번
     * @param answers      설문 응답 JSON 문자열
     * @return 생성된 ExternalSurveyResponse
     */
    public static ExternalSurveyResponse create(Survey survey, EventRegistration registration,
                                                 String studentId, String answers) {
        ExternalSurveyResponse response = new ExternalSurveyResponse();
        response.survey = survey;
        response.registration = registration;
        response.studentId = studentId;
        response.answers = answers;
        response.createdAt = Instant.now();
        return response;
    }

    /**
     * 연결된 설문의 ID를 반환합니다.
     *
     * @return 설문 ID
     */
    public Long getSurveyId() {
        return survey != null ? survey.getId() : null;
    }

    /**
     * 연결된 행사 신청의 ID를 반환합니다.
     *
     * @return 행사 신청 ID
     */
    public Long getRegistrationId() {
        return registration != null ? registration.getId() : null;
    }
}
