package igrus.web.board.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 게시판 접근이 거부되었을 때 발생하는 예외.
 */
public class BoardAccessDeniedException extends CustomBaseException {

    public BoardAccessDeniedException() {
        super(ErrorCode.BOARD_ACCESS_DENIED);
    }

    public BoardAccessDeniedException(String message) {
        super(ErrorCode.BOARD_ACCESS_DENIED, message);
    }
}
