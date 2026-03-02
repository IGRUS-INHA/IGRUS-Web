package igrus.web.community.post.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 게시글 조회 기록 페이징 응답 DTO.
 */
public record PostViewHistoryPageResponse(
    List<PostViewHistoryResponse> viewHistory,

    long totalElements,

    int totalPages,

    int currentPage,

    boolean hasNext
) {
    public static PostViewHistoryPageResponse from(Page<PostViewHistoryResponse> page) {
        return new PostViewHistoryPageResponse(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.hasNext()
        );
    }
}
