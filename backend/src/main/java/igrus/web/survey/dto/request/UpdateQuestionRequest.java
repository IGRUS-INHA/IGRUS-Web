package igrus.web.survey.dto.request;

import igrus.web.survey.domain.SurveyQuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 설문 질문 수정 요청 DTO.
 * 기존 질문의 정보를 수정할 때 사용합니다.
 * 모든 상태(DRAFT, PUBLISHED, CLOSED)에서 사용 가능합니다.
 *
 * @param questionType 질문 유형 (필수)
 * @param title        질문 제목 (필수, 최대 200자)
 * @param description  질문 설명 (선택, 최대 500자)
 * @param required     필수 응답 여부
 * @param displayOrder 표시 순서
 */
public record UpdateQuestionRequest(
        @NotNull(message = "질문 유형은 필수입니다")
        SurveyQuestionType questionType,

        @NotBlank(message = "질문 제목은 필수입니다")
        @Size(max = 200, message = "질문 제목은 200자 이내여야 합니다")
        String title,

        @Size(max = 500, message = "질문 설명은 500자 이내여야 합니다")
        String description,

        boolean required,

        int displayOrder
) {
}
