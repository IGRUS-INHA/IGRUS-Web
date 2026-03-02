package igrus.web.admin.user.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 관리자용 회원 목록 페이징 응답 DTO.
 */
public record UserListPageResponse(
    List<UserListResponse> users,

    long totalElements,

    int totalPages,

    int currentPage,

    boolean hasNext
) {
    public static UserListPageResponse from(Page<UserListResponse> page) {
        return new UserListPageResponse(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.hasNext()
        );
    }
}
