package igrus.web.event.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 행사 상태 변경 사유 요청 DTO.
 * 등록 수동 마감, 행사 취소, 행사 재활성화, 등록 수동 재오픈 시 공통 사용.
 *
 * @param reason 상태 변경 사유 (필수)
 */
public record EventStatusChangeReasonRequest(
        @NotBlank(message = "상태 변경 사유는 필수입니다")
        String reason
) {
}
