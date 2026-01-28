package igrus.web.community.comment.dto.request;

import igrus.web.community.comment.domain.ReportStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 신고 상태 업데이트 요청 DTO.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UpdateReportStatusRequest {

    @NotNull(message = "신고 처리 상태를 선택해 주세요")
    private ReportStatus status;
}
