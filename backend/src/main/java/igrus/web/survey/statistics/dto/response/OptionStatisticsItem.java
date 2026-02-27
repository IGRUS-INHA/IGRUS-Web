package igrus.web.survey.statistics.dto.response;

import java.math.BigDecimal;

/**
 * 개별 선택지의 통계 항목.
 * OPTION/CHECKBOX/GRID 카테고리에서 공통으로 사용합니다.
 *
 * @param optionId   선택지 ID
 * @param optionText 선택지 텍스트
 * @param count      선택 수
 * @param percentage 비율 (HALF_UP, scale=1)
 */
public record OptionStatisticsItem(
        Long optionId,
        String optionText,
        int count,
        BigDecimal percentage
) {
}
