package igrus.web.community.board.dto.response;

import igrus.web.community.board.domain.Board;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 게시판 상세 응답 DTO.
 */
@Schema(description = "게시판 상세 응답")
public record BoardDetailResponse(
        @Schema(description = "게시판 코드", example = "GENERAL")
        String code,

        @Schema(description = "게시판 이름", example = "자유게시판")
        String name,

        @Schema(description = "게시판 설명", example = "자유롭게 이야기를 나눌 수 있는 공간입니다.")
        String description,

        @Schema(description = "익명 작성 허용 여부", example = "true")
        boolean allowsAnonymous,

        @Schema(description = "질문 태그 허용 여부", example = "true")
        boolean allowsQuestionTag,

        @Schema(description = "읽기 권한 여부", example = "true")
        boolean canRead,

        @Schema(description = "쓰기 권한 여부", example = "true")
        boolean canWrite
) {
    public static BoardDetailResponse of(Board board, boolean canRead, boolean canWrite) {
        return new BoardDetailResponse(
                board.getCode().name(),
                board.getName(),
                board.getDescription(),
                board.getAllowsAnonymous(),
                board.getAllowsQuestionTag(),
                canRead,
                canWrite
        );
    }
}
