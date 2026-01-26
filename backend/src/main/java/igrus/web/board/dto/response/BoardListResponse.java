package igrus.web.board.dto.response;

import igrus.web.board.domain.Board;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 게시판 목록 응답 DTO.
 */
@Schema(description = "게시판 목록 응답")
public record BoardListResponse(
        @Schema(description = "게시판 코드", example = "general")
        String code,

        @Schema(description = "게시판 이름", example = "자유게시판")
        String name,

        @Schema(description = "게시판 설명", example = "자유롭게 이야기를 나눌 수 있는 공간입니다.")
        String description,

        @Schema(description = "읽기 권한 여부", example = "true")
        boolean canRead,

        @Schema(description = "쓰기 권한 여부", example = "true")
        boolean canWrite
) {
    public static BoardListResponse of(Board board, boolean canRead, boolean canWrite) {
        return new BoardListResponse(
                board.getCode(),
                board.getName(),
                board.getDescription(),
                canRead,
                canWrite
        );
    }
}
