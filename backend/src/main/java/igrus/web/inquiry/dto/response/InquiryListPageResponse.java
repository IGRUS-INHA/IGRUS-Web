package igrus.web.inquiry.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 문의 목록 페이징 응답 DTO.
 */
@Schema(description = "문의 목록 페이징 응답")
public record InquiryListPageResponse(
    @Schema(description = "문의 목록")
    List<InquiryListResponse> inquiries,

    @Schema(description = "전체 요소 수", example = "100")
    long totalElements,

    @Schema(description = "전체 페이지 수", example = "5")
    int totalPages,

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    int currentPage,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
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
