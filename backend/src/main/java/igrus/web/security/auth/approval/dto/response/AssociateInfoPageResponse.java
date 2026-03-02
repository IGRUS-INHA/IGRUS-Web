package igrus.web.security.auth.approval.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 준회원 목록 페이징 응답 DTO.
 */
public record AssociateInfoPageResponse(
    List<AssociateInfoResponse> associates,

    long totalElements,

    int totalPages,

    int currentPage,

    boolean hasNext
) {
    public static AssociateInfoPageResponse from(Page<AssociateInfoResponse> page) {
        return new AssociateInfoPageResponse(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.hasNext()
        );
    }
}
