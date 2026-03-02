package igrus.web.inquiry.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 문의 목록 페이징 응답 DTO.
 */
public record InquiryListPageResponse(
    List<InquiryListResponse> inquiries,

    long totalElements,

    int totalPages,

    int currentPage,

    boolean hasNext
) {
    public static InquiryListPageResponse from(Page<InquiryListResponse> page) {
        return new InquiryListPageResponse(
            page.getContent(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber(),
            page.hasNext()
        );
    }
}
