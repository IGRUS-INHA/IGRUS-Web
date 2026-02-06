package igrus.web.user.mypage.dto.response;

import igrus.web.community.post.domain.Post;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * 내 게시글 목록 응답 DTO.
 */
@Schema(description = "내 게시글 정보")
public record MyPostResponse(
        @Schema(description = "게시글 ID", example = "1")
        Long id,

        @Schema(description = "게시판 코드", example = "GENERAL")
        String boardCode,

        @Schema(description = "게시판 이름", example = "자유게시판")
        String boardName,

        @Schema(description = "제목", example = "첫 번째 게시글")
        String title,

        @Schema(description = "조회수", example = "100")
        int viewCount,

        @Schema(description = "좋아요 수", example = "10")
        int likeCount,

        @Schema(description = "익명 여부", example = "false")
        boolean isAnonymous,

        @Schema(description = "작성일")
        Instant createdAt
) {
    public static MyPostResponse from(Post post) {
        return new MyPostResponse(
                post.getId(),
                post.getBoard().getCode().name(),
                post.getBoard().getName(),
                post.getTitle(),
                post.getViewCount(),
                post.getLikeCount(),
                post.isAnonymous(),
                post.getCreatedAt()
        );
    }
}
