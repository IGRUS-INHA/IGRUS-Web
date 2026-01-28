package igrus.web.community.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 댓글 작성 요청 DTO.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "내용을 입력해 주세요")
    @Size(max = 500, message = "댓글은 500자 이내여야 합니다")
    private String content;

    private boolean isAnonymous;
}
