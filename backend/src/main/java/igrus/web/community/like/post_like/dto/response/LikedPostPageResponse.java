package igrus.web.community.like.post_like.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 좋아요한 게시글 목록 페이징 응답 DTO.
 */
@Schema(description = "좋아요한 게시글 목록 페이징 응답")
public record LikedPostPageResponse(
    @Schema(description = "좋아요한 게시글 목록")
    List<LikedPostResponse> posts,

    @Schema(description = "전체 요소 수", example = "100")
    long totalElements,

    @Schema(description = "전체 페이지 수", example = "5")
    int totalPages,

    @Schema(description = "현재 페이지 번호 (0부터 시작)", example = "0")
    int currentPage,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
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
