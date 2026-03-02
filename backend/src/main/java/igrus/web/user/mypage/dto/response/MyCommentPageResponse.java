package igrus.web.user.mypage.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 내 댓글 목록 페이징 응답 DTO.
 */
public record MyCommentPageResponse(
    List<MyCommentResponse> comments,

    long totalElements,

    int totalPages,

    int currentPage,

    boolean hasNext
) {
    public static MyCommentPageResponse from(Page<MyCommentResponse> page) {
        return new MyCommentPageResponse(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.hasNext()
        );
    }
}
