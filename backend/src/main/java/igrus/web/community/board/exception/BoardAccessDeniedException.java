package igrus.web.community.board.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.community.exception.CommunityErrorCode;

/**
 * 게시판 접근이 거부되었을 때 발생하는 예외.
 */
public class BoardAccessDeniedException extends CustomBaseException {

    public BoardAccessDeniedException() {
        super(CommunityErrorCode.BOARD_ACCESS_DENIED);
    }

    public BoardAccessDeniedException(String message) {
        super(CommunityErrorCode.BOARD_ACCESS_DENIED, message);
    }
}
