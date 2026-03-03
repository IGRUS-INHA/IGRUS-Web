package igrus.web.user.mypage.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 내 게시글 목록 페이징 응답 DTO.
 */
public record MyPostPageResponse(
    List<MyPostResponse> posts,

    long totalElements,

    int totalPages,

    int currentPage,

    boolean hasNext
) {
    public static MyPostPageResponse from(Page<MyPostResponse> page) {
        return new MyPostPageResponse(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.hasNext()
        );
    }
}
