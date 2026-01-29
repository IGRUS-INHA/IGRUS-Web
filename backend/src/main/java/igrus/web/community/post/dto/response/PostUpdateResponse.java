package igrus.web.community.post.dto.response;

import igrus.web.community.post.domain.Post;
import java.time.Instant;

/**
 * 게시글 수정 응답 DTO.
 * 게시글 수정 완료 후 반환되는 정보를 담습니다.
 *
 * @param postId    수정된 게시글 ID
 * @param boardCode 게시판 코드
 * @param title     수정된 게시글 제목
 * @param updatedAt 수정 일시
 */
public record PostUpdateResponse(
    Long postId,
    String boardCode,
    String title,
    Instant updatedAt
) {
    public static PostUpdateResponse from(Post post) {
        return new PostUpdateResponse(
            post.getId(),
            post.getBoard().getCode().name(),
            post.getTitle(),
            post.getUpdatedAt()
        );
    }
}
