package igrus.web.survey.statistics.dto.response;

import java.math.BigDecimal;
import java.util.Map;

/**
 * SCALE 카테고리(LINEAR_SCALE) 질문의 통계 구조.
 * 평균, 최솟값, 최댓값, 값별 분포를 포함합니다.
 *
 * @param average      평균값 (HALF_UP, scale=1). 응답 0건 시 null
 * @param min          최솟값. 응답 0건 시 null
 * @param max          최댓값. 응답 0건 시 null
 * @param distribution 값별 응답 수 (scaleMin~scaleMax 전체 키 포함, 미선택은 0)
 */
public record ScaleQuestionStatistics(
        BigDecimal average,
        Integer min,
        Integer max,
        Map<Integer, Integer> distribution
) {
}
