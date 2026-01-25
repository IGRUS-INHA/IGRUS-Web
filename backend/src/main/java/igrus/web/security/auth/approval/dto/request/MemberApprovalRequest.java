package igrus.web.security.auth.approval.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "개별 준회원 승인 요청")
public record MemberApprovalRequest(
    @Schema(description = "승인 사유 (선택)", example = "회비 납부 확인 완료")
    @Size(max = 255, message = "승인 사유는 255자 이내여야 합니다")
    String reason
) {}
