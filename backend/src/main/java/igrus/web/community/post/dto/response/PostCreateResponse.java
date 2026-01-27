package igrus.web.community.post.dto.response;

import igrus.web.community.post.domain.Post;
import java.time.Instant;

/**
 * 게시글 작성 응답 DTO.
 * 게시글 작성 완료 후 반환되는 정보를 담습니다.
 *
 * @param postId    생성된 게시글 ID
 * @param boardCode 게시판 코드
 * @param title     게시글 제목
 * @param createdAt 작성 일시
 */
public record PostCreateResponse(
    Long postId,
    String boardCode,
    String title,
    Instant createdAt
) {
    public static PostCreateResponse from(Post post) {
        return new PostCreateResponse(
            post.getId(),
            post.getBoard().getCode().name(),
            post.getTitle(),
            post.getCreatedAt()
        );
    }
}
