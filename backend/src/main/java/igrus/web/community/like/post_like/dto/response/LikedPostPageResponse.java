package igrus.web.community.like.post_like.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 좋아요한 게시글 목록 페이징 응답 DTO.
 */
public record LikedPostPageResponse(
    List<LikedPostResponse> posts,

    long totalElements,

    int totalPages,

    int currentPage,

    boolean hasNext
) {
    public static LikedPostPageResponse from(Page<LikedPostResponse> page) {
        return new LikedPostPageResponse(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.hasNext()
        );
    }
}
