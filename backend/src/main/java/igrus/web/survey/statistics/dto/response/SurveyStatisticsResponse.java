package igrus.web.survey.statistics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * 설문 통계 API의 최상위 응답 DTO.
 * 총 응답 수, 응답 기간, 응답자 정보 목록, 질문별 통계 목록을 포함합니다.
 *
 * <p>PUBLIC 설문에서는 respondents가 null이며, JSON 직렬화 시 생략됩니다.
 *
 * @param totalResponseCount 유효 응답 수 (deleted=false 기준)
 * @param responseStartedAt  첫 응답 시각 (응답 0건 시 null)
 * @param responseEndedAt    마지막 응답 시각 (응답 0건 시 null)
 * @param respondents        응답자 정보 목록 (PUBLIC 설문 시 null)
 * @param questionStatistics 질문별 통계 목록 (displayOrder 오름차순)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SurveyStatisticsResponse(
        int totalResponseCount,
        Instant responseStartedAt,
        Instant responseEndedAt,
        List<RespondentInfo> respondents,
        List<QuestionStatisticsResponse> questionStatistics
) {
}
