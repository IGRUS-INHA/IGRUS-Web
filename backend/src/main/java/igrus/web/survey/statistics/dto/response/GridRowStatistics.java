package igrus.web.survey.statistics.dto.response;

import java.util.List;

/**
 * GRID 카테고리의 개별 행 통계.
 * 행별 옵션 분포를 포함하며, 비율 분모는 전체 설문 응답자 수입니다.
 *
 * @param rowId   행 ID
 * @param rowLabel 행 라벨
 * @param options 옵션별 선택 수/비율
 */
public record GridRowStatistics(
        Long rowId,
        String rowLabel,
        List<OptionStatisticsItem> options
) {
}
