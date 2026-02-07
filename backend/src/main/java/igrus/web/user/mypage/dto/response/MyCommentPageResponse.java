package igrus.web.user.mypage.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 내 댓글 목록 페이징 응답 DTO.
 */
@Schema(description = "내 댓글 목록 페이징 응답")
public record MyCommentPageResponse(
    @Schema(description = "댓글 목록")
    List<MyCommentResponse> comments,

    @Schema(description = "전체 요소 수", example = "100")
    long totalElements,

    @Schema(description = "전체 페이지 수", example = "5")
    int totalPages,

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    int currentPage,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
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
