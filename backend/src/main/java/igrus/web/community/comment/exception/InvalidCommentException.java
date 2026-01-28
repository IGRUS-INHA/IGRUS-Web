package igrus.web.community.comment.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 잘못된 댓글 요청일 때 발생하는 예외.
 */
public class InvalidCommentException extends CustomBaseException {

    public InvalidCommentException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InvalidCommentException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static InvalidCommentException contentTooLong() {
        return new InvalidCommentException(ErrorCode.COMMENT_CONTENT_TOO_LONG);
    }

    public static InvalidCommentException contentEmpty() {
        return new InvalidCommentException(ErrorCode.COMMENT_CONTENT_EMPTY);
    }

    public static InvalidCommentException replyToReplyNotAllowed() {
        return new InvalidCommentException(ErrorCode.REPLY_TO_REPLY_NOT_ALLOWED);
    }

    public static InvalidCommentException postDeletedCannotComment() {
        return new InvalidCommentException(ErrorCode.POST_DELETED_CANNOT_COMMENT);
    }

    public static InvalidCommentException anonymousNotAllowed() {
        return new InvalidCommentException(ErrorCode.ANONYMOUS_NOT_ALLOWED);
    }
}
