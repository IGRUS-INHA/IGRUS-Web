package igrus.web.survey.statistics.dto.response;

import java.util.List;

/**
 * GRID 카테고리(MULTIPLE_CHOICE_GRID, CHECKBOX_GRID) 질문의 통계 구조.
 * 행별 옵션 분포를 포함합니다.
 *
 * @param rows 행별 통계 목록
 */
public record GridQuestionStatistics(
        List<GridRowStatistics> rows
) {
}
