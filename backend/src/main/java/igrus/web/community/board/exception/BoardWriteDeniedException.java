package igrus.web.community.board.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 게시판 쓰기 권한이 없을 때 발생하는 예외.
 */
public class BoardWriteDeniedException extends CustomBaseException {

    public BoardWriteDeniedException() {
        super(ErrorCode.BOARD_WRITE_DENIED);
    }

    public BoardWriteDeniedException(String message) {
        super(ErrorCode.BOARD_WRITE_DENIED, message);
    }
}
