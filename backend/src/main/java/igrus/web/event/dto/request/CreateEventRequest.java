package igrus.web.event.dto.request;

import igrus.web.event.domain.EventRegistrationType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * 행사 생성 요청 DTO.
 * 새로운 행사를 생성할 때 필요한 정보를 담습니다.
 *
 * @param title               행사 제목 (필수, 최대 100자)
 * @param description         행사 설명 (필수)
 * @param location            행사 장소 (필수, 최대 200자)
 * @param eventStartAt        행사 시작일시 (필수)
 * @param eventEndAt          행사 종료일시 (필수)
 * @param registrationStartAt 신청 시작일시 (필수)
 * @param registrationEndAt   신청 마감일시 (필수)
 * @param capacity            정원 (필수, 1명 이상)
 * @param registrationType    신청 방식 (필수, AUTO_APPROVE: 자동 승인/선착순, MANUAL_APPROVE: 수동 승인/선발제)
 */
public record CreateEventRequest(
        @NotBlank(message = "제목은 필수입니다")
        @Size(max = 100, message = "제목은 100자 이내여야 합니다")
        String title,

        @NotBlank(message = "설명은 필수입니다")
        String description,

        @NotBlank(message = "장소는 필수입니다")
        @Size(max = 200, message = "장소는 200자 이내여야 합니다")
        String location,

        @NotNull(message = "행사 시작일은 필수입니다")
        Instant eventStartAt,

        @NotNull(message = "행사 종료일은 필수입니다")
        Instant eventEndAt,

        @NotNull(message = "신청 시작일은 필수입니다")
        Instant registrationStartAt,

        @NotNull(message = "신청 마감일은 필수입니다")
        Instant registrationEndAt,

        @NotNull(message = "정원은 필수입니다")
        @Min(value = 1, message = "정원은 1명 이상이어야 합니다")
        Integer capacity,

        @NotNull(message = "신청 방식은 필수입니다")
        EventRegistrationType registrationType,

        Long surveyId
) {
}
