package igrus.web.survey.dto.response;

import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyAccessLevel;
import igrus.web.survey.domain.SurveyResponseStatus;
import igrus.web.survey.domain.SurveyVisibility;

import java.time.Instant;

/**
 * 설문 목록 응답 DTO.
 * 목록 조회 시 사용하며, 질문 구조는 포함하지 않습니다.
 *
 * @param id             설문 ID
 * @param title          설문 제목
 * @param visibility     공개 상태 (UNPUBLISHED / PUBLISHED)
 * @param responseStatus 응답 수집 상태 (NOT_STARTED / OPEN / CLOSED)
 * @param accessLevel    응답 대상 권한
 * @param deadline       설문 마감일
 * @param createdAt      생성 시각
 */
public record SurveyListResponse(
        Long id,
        String title,
        SurveyVisibility visibility,
        SurveyResponseStatus responseStatus,
        SurveyAccessLevel accessLevel,
        Instant deadline,
        Instant createdAt
) {
    /**
     * Survey 엔티티로부터 SurveyListResponse를 생성합니다.
     *
     * @param survey 설문 엔티티
     * @return SurveyListResponse
     */
    public static SurveyListResponse from(Survey survey) {
        return new SurveyListResponse(
                survey.getId(),
                survey.getTitle(),
                survey.getVisibility(),
                survey.getResponseStatus(),
                survey.getAccessLevel(),
                survey.getDeadline(),
                survey.getCreatedAt()
        );
    }
}
