package igrus.web.survey.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 설문 질문 선택지 저장 요청 DTO.
 * 선택지 추가 및 수정에 공용으로 사용합니다.
 * MULTIPLE_CHOICE, CHECKBOX, DROPDOWN, 그리드 유형의 열(Column)에 해당합니다.
 *
 * @param text         선택지 텍스트 (필수, 최대 200자)
 * @param displayOrder 표시 순서
 */
public record SaveQuestionOptionRequest(
        @NotBlank(message = "선택지 텍스트는 필수입니다")
        @Size(max = 200, message = "선택지 텍스트는 200자 이내여야 합니다")
        String text,

        int displayOrder
) {
}
