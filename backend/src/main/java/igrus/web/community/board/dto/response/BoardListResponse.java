package igrus.web.community.board.dto.response;

import igrus.web.community.board.domain.Board;

/**
 * 게시판 목록 응답 DTO.
 */
public record BoardListResponse(
        String code,

        String name,

        String description,

        boolean canRead,

        boolean canWrite
) {
    public static BoardListResponse of(Board board, boolean canRead, boolean canWrite) {
        return new BoardListResponse(
                board.getCode().name(),
                board.getName(),
                board.getDescription(),
                canRead,
                canWrite
        );
    }
}
