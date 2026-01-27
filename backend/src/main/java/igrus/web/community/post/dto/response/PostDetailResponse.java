package igrus.web.community.post.dto.response;

import igrus.web.community.post.domain.Post;
import igrus.web.community.post.domain.PostImage;
import java.time.Instant;
import java.util.List;

/**
 * 게시글 상세 조회 응답 DTO.
 * 상세 화면에서 보여줄 게시글 정보를 담습니다.
 */
public record PostDetailResponse(
    Long postId,
    String boardCode,
    String title,
    String content,
    Long authorId,
    String authorName,
    boolean isAnonymous,
    boolean isQuestion,
    int viewCount,
    int likeCount,
    int commentCount,
    List<String> imageUrls,
    Instant createdAt,
    Instant updatedAt,
    boolean isAuthor
) {
    /**
     * Post 엔티티로부터 PostDetailResponse를 생성합니다.
     * 익명 게시글의 경우 작성자 ID는 null, 이름은 "익명"으로 표시합니다.
     *
     * @param post              게시글 엔티티
     * @param isCurrentUserAuthor 현재 사용자가 작성자인지 여부
     * @return PostDetailResponse
     */
    public static PostDetailResponse from(Post post, boolean isCurrentUserAuthor) {
        List<String> imageUrls = post.getImages().stream()
            .sorted((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()))
            .map(PostImage::getImageUrl)
            .toList();

        return new PostDetailResponse(
            post.getId(),
            post.getBoard().getCode().name(),
            post.getTitle(),
            post.getContent(),
            post.isAnonymous() ? null : post.getAuthor().getId(),
            post.isAnonymous() ? "익명" : post.getAuthor().getName(),
            post.isAnonymous(),
            post.isQuestion(),
            post.getViewCount(),
            0,  // likeCount - 추후 구현
            0,  // commentCount - 추후 구현
            imageUrls,
            post.getCreatedAt(),
            post.getUpdatedAt(),
            isCurrentUserAuthor
        );
    }
}
