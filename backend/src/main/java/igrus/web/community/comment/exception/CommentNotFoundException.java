package igrus.web.community.comment.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 댓글을 찾을 수 없을 때 발생하는 예외.
 */
public class CommentNotFoundException extends CustomBaseException {

    public CommentNotFoundException() {
        super(ErrorCode.COMMENT_NOT_FOUND);
    }

    public CommentNotFoundException(Long commentId) {
        super(ErrorCode.COMMENT_NOT_FOUND, "댓글을 찾을 수 없습니다: " + commentId);
    }
}
