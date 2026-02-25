package igrus.web.survey.dto.request;

import igrus.web.survey.domain.SurveyAccessLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * 설문 수정 요청 DTO.
 * 모든 상태(UNPUBLISHED, PUBLISHED)에서 사용 가능합니다.
 *
 * @param title       설문 제목 (필수, 최대 100자)
 * @param description 설문 설명 (선택, 최대 500자)
 * @param accessLevel 응답 대상 권한 (필수, PUBLIC/ASSOCIATE/MEMBER)
 * @param deadline    설문 마감일 (선택, 경과 시 자동 마감)
 */
public record UpdateSurveyRequest(
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 100, message = "제목은 100자 이내여야 합니다")
        String title,

        @Size(max = 500, message = "설명은 500자 이내여야 합니다")
        String description,

        @NotNull(message = "응답 대상은 필수입니다")
        SurveyAccessLevel accessLevel,

        Instant deadline
) {
}
