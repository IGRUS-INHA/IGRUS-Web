package igrus.web.survey.statistics.dto.response;

import java.util.List;

/**
 * TEXT 카테고리(SHORT_ANSWER, PARAGRAPH, DATE, TIME, FILE_UPLOAD) 질문의 통계 구조.
 * 텍스트 응답 항목 목록을 응답 제출 시각(SurveyResponse.createdAt) 오름차순으로 반환합니다.
 *
 * @param textResponses 텍스트 응답 항목 목록 (createdAt 오름차순)
 */
public record TextQuestionStatistics(
        List<TextResponseItem> textResponses
) {
}
