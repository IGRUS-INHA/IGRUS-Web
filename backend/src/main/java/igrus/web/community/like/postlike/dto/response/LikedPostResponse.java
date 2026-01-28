package igrus.web.community.like.postlike.dto.response;

import igrus.web.community.like.postlike.domain.PostLike;
import igrus.web.community.post.domain.Post;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 좋아요한 게시글 목록 조회 응답 DTO.
 */
@Schema(description = "좋아요한 게시글 응답")
public record LikedPostResponse(
    @Schema(description = "게시글 ID", example = "1")
    Long postId,

    @Schema(description = "게시글 제목", example = "안녕하세요")
    String title,

    @Schema(description = "게시판 코드", example = "GENERAL")
    String boardCode,

    @Schema(description = "게시판 이름", example = "자유게시판")
    String boardName,

    @Schema(description = "작성자 이름", example = "홍길동")
    String authorName,

    @Schema(description = "좋아요 수", example = "42")
    int likeCount,

    @Schema(description = "게시글 작성일")
    Instant createdAt,

    @Schema(description = "삭제 여부", example = "false")
    boolean isDeleted,

    @Schema(description = "삭제된 경우 메시지", example = "삭제된 게시글입니다")
    String deletedMessage
) {
    public static LikedPostResponse from(PostLike postLike) {
        Post post = postLike.getPost();
        boolean isDeleted = post.isDeleted();

        return new LikedPostResponse(
            post.getId(),
            isDeleted ? null : post.getTitle(),
            post.getBoard().getCode().name(),
            post.getBoard().getName(),
            isDeleted ? null : (post.isAnonymous() ? "익명" : post.getAuthor().getName()),
            post.getLikeCount(),
            post.getCreatedAt(),
            isDeleted,
            isDeleted ? "삭제된 게시글입니다" : null
        );
    }
}
