package igrus.web.community.comment.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 댓글 접근이 거부되었을 때 발생하는 예외.
 */
public class CommentAccessDeniedException extends CustomBaseException {

    public CommentAccessDeniedException() {
        super(ErrorCode.COMMENT_ACCESS_DENIED);
    }

    public CommentAccessDeniedException(String message) {
        super(ErrorCode.COMMENT_ACCESS_DENIED, message);
    }
}
