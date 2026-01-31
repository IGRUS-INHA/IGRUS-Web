package igrus.web.community.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 신고 요청 DTO.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CreateCommentReportRequest {

    @NotBlank(message = "신고 사유를 입력해 주세요")
    private String reason;
}
