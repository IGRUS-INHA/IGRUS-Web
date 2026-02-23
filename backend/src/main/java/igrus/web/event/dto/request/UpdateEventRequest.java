package igrus.web.event.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * 행사 수정 요청 DTO.
 *
 * @param title               행사 제목 (필수, 최대 100자)
 * @param description         행사 설명 (필수)
 * @param location            행사 장소 (필수, 최대 200자)
 * @param eventStartAt        행사 시작일시 (필수)
 * @param eventEndAt          행사 종료일시 (필수)
 * @param registrationStartAt 신청 시작일 (필수)
 * @param registrationEndAt   신청 마감일 (필수)
 * @param capacity            정원 (필수, 1 이상)
 */
public record UpdateEventRequest(
        @NotBlank(message = "행사 제목을 입력해 주세요")
        @Size(max = 100, message = "행사 제목은 100자 이내여야 합니다")
        String title,

        @NotBlank(message = "행사 설명을 입력해 주세요")
        String description,

        @NotBlank(message = "행사 장소를 입력해 주세요")
        @Size(max = 200, message = "행사 장소는 200자 이내여야 합니다")
        String location,

        @NotNull(message = "행사 시작일을 입력해 주세요")
        Instant eventStartAt,

        @NotNull(message = "행사 종료일을 입력해 주세요")
        Instant eventEndAt,

        @NotNull(message = "신청 시작일을 입력해 주세요")
        Instant registrationStartAt,

        @NotNull(message = "신청 마감일을 입력해 주세요")
        Instant registrationEndAt,

        @NotNull(message = "정원을 입력해 주세요")
        @Min(value = 1, message = "정원은 1명 이상이어야 합니다")
        Integer capacity
) {
}
