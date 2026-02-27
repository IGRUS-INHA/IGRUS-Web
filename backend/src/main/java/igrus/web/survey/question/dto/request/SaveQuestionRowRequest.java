package igrus.web.survey.question.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 설문 그리드 행 저장 요청 DTO.
 * 행 추가 및 수정에 공용으로 사용합니다.
 * MULTIPLE_CHOICE_GRID, CHECKBOX_GRID 유형에서만 사용합니다.
 *
 * @param label        행 라벨 텍스트 (필수, 최대 200자)
 * @param displayOrder 표시 순서
 */
public record SaveQuestionRowRequest(
        @NotBlank(message = "행 라벨은 필수입니다")
        @Size(max = 200, message = "행 라벨은 200자 이내여야 합니다")
        String label,

        int displayOrder
) {
}
