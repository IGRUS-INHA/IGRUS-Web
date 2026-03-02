package igrus.web.community.post.dto.response;

import igrus.web.community.post.domain.Post;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * 게시글 목록 페이징 응답 DTO.
 */
public record PostListPageResponse(
    List<PostListResponse> posts,

    long totalElements,

    int totalPages,

    int currentPage,

    boolean hasNext
) {
    public static PostListPageResponse from(Page<Post> page) {
        List<PostListResponse> posts = page.getContent().stream()
            .map(PostListResponse::from)
            .toList();

        return new PostListPageResponse(
            posts,
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.hasNext()
        );
    }
}
