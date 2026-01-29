package igrus.web.community.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 게시글 수정 요청 DTO.
 * 기존 게시글을 수정할 때 사용합니다.
 * 익명 여부는 수정할 수 없습니다.
 *
 * @param title      게시글 제목 (필수, 최대 100자)
 * @param content    게시글 내용 (필수)
 * @param isQuestion 질문글 여부
 * @param imageUrls  첨부 이미지 URL 목록 (최대 5개)
 */
public record UpdatePostRequest(
    @NotBlank(message = "제목을 입력해 주세요")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다")
    String title,

    @NotBlank(message = "내용을 입력해 주세요")
    String content,

    boolean isQuestion,

    @Size(max = 5, message = "이미지는 최대 5개까지 첨부 가능합니다")
    List<String> imageUrls
) {
    public UpdatePostRequest {
        if (imageUrls == null) {
            imageUrls = List.of();
        }
    }
}
