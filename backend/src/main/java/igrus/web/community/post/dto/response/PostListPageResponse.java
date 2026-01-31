package igrus.web.community.post.dto.response;

import igrus.web.community.post.domain.Post;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * 게시글 목록 페이징 응답 DTO.
 * 페이징된 게시글 목록과 페이징 정보를 담습니다.
 */
public record PostListPageResponse(
    List<PostListResponse> posts,
    long totalElements,
    int totalPages,
    int currentPage,
    boolean hasNext
) {
    /**
     * Page<Post>로부터 PostListPageResponse를 생성합니다.
     *
     * @param page 페이징된 게시글 엔티티
     * @return PostListPageResponse
     */
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
