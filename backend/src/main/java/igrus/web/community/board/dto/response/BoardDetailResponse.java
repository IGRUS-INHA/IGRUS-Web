package igrus.web.community.board.dto.response;

import igrus.web.community.board.domain.Board;

/**
 * 게시판 상세 응답 DTO.
 */
public record BoardDetailResponse(
        String code,

        String name,

        String description,

        boolean allowsAnonymous,

        boolean allowsQuestionTag,

        boolean canRead,

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
