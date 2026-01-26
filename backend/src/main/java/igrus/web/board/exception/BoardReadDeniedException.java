package igrus.web.board.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 게시판 읽기 권한이 없을 때 발생하는 예외.
 */
public class BoardReadDeniedException extends CustomBaseException {

    public BoardReadDeniedException() {
        super(ErrorCode.BOARD_READ_DENIED);
    }

    public BoardReadDeniedException(String message) {
        super(ErrorCode.BOARD_READ_DENIED, message);
    }
}
