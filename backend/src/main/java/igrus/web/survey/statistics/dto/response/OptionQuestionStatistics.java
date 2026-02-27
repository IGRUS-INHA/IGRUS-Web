package igrus.web.survey.statistics.dto.response;

import java.util.List;

/**
 * OPTION/CHECKBOX 카테고리(MULTIPLE_CHOICE, DROPDOWN, CHECKBOX) 질문의 통계 구조.
 * 옵션별 선택 수와 비율을 포함합니다.
 *
 * @param options 옵션별 통계 목록
 */
public record OptionQuestionStatistics(
        List<OptionStatisticsItem> options
) {
}
