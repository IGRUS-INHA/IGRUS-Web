package igrus.web.survey.statistics.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * TEXT 카테고리 질문의 개별 텍스트 응답 항목.
 * 텍스트 값과 응답자 정보를 포함합니다.
 *
 * <p>PUBLIC 설문에서는 respondent가 null이며, JSON 직렬화 시 생략됩니다.
 *
 * @param text       텍스트 응답 값
 * @param respondent 응답자 정보 (PUBLIC 설문 시 null)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TextResponseItem(
        String text,
        RespondentInfo respondent
) {
}
