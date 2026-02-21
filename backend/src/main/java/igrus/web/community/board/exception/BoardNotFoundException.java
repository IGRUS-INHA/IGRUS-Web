package igrus.web.community.board.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.community.exception.CommunityErrorCode;

/**
 * 게시판을 찾을 수 없을 때 발생하는 예외.
 */
public class BoardNotFoundException extends CustomBaseException {

    public BoardNotFoundException() {
        super(CommunityErrorCode.BOARD_NOT_FOUND);
    }

    public BoardNotFoundException(String boardCode) {
        super(CommunityErrorCode.BOARD_NOT_FOUND, "게시판을 찾을 수 없습니다: " + boardCode);
    }
}
