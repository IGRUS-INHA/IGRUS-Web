package igrus.web.survey.statistics.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * 설문 통계 API의 최상위 응답 DTO.
 * 총 응답 수, 응답 기간, 질문별 통계 목록을 포함합니다.
 *
 * @param totalResponseCount 유효 응답 수 (deleted=false 기준)
 * @param responseStartedAt  첫 응답 시각 (응답 0건 시 null)
 * @param responseEndedAt    마지막 응답 시각 (응답 0건 시 null)
 * @param questionStatistics 질문별 통계 목록 (displayOrder 오름차순)
 */
public record SurveyStatisticsResponse(
        int totalResponseCount,
        Instant responseStartedAt,
        Instant responseEndedAt,
        List<QuestionStatisticsResponse> questionStatistics
) {
}
