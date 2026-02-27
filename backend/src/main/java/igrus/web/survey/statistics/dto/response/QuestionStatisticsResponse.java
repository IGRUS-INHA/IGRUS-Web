package igrus.web.survey.statistics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import igrus.web.survey.question.domain.SurveyQuestionType;

/**
 * 질문 하나의 통계를 나타내는 DTO.
 * 질문 유형(TEXT/SCALE/OPTION/CHECKBOX/GRID)에 따라 해당하는 상세 통계 필드만 non-null이 됩니다.
 *
 * @param questionId      질문 ID
 * @param questionTitle   질문 제목
 * @param questionType    질문 유형
 * @param responseCount   해당 질문의 유효 답변 수
 * @param textStatistics  TEXT 카테고리 상세 (TEXT 유형일 때만 non-null)
 * @param scaleStatistics SCALE 카테고리 상세 (SCALE 유형일 때만 non-null)
 * @param optionStatistics OPTION/CHECKBOX 카테고리 상세 (OPTION/CHECKBOX 유형일 때만 non-null)
 * @param gridStatistics  GRID 카테고리 상세 (GRID 유형일 때만 non-null)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionStatisticsResponse(
        Long questionId,
        String questionTitle,
        SurveyQuestionType questionType,
        int responseCount,
        TextQuestionStatistics textStatistics,
        ScaleQuestionStatistics scaleStatistics,
        OptionQuestionStatistics optionStatistics,
        GridQuestionStatistics gridStatistics
) {
}
