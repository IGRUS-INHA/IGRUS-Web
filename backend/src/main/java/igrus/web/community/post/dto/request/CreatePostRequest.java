package igrus.web.community.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 게시글 작성 요청 DTO.
 * 일반 게시판에 새 게시글을 작성할 때 사용합니다.
 *
 * @param title               게시글 제목 (필수, 최대 100자)
 * @param content             게시글 내용 (필수)
 * @param isAnonymous         익명 작성 여부
 * @param isQuestion          질문글 여부
 * @param isVisibleToAssociate 준회원 공개 여부 (공지사항 게시판에서만 사용)
 * @param imageUrls           첨부 이미지 URL 목록 (최대 5개)
 */
public record CreatePostRequest(
    @NotBlank(message = "제목을 입력해 주세요")
    @Size(max = 100, message = "제목은 100자 이내여야 합니다")
    String title,

    @NotBlank(message = "내용을 입력해 주세요")
    String content,

    boolean isAnonymous,
    boolean isQuestion,
    boolean isVisibleToAssociate,

    @Size(max = 5, message = "이미지는 최대 5개까지 첨부 가능합니다")
    List<String> imageUrls
) {
    public CreatePostRequest {
        if (imageUrls == null) {
            imageUrls = List.of();
        }
    }
}
