package igrus.web.community.comment.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 댓글 좋아요 관련 예외.
 */
public class CommentLikeException extends CustomBaseException {

    public CommentLikeException(ErrorCode errorCode) {
        super(errorCode);
    }

    public CommentLikeException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static CommentLikeException cannotLikeOwnComment() {
        return new CommentLikeException(ErrorCode.CANNOT_LIKE_OWN_COMMENT);
    }

    public static CommentLikeException alreadyLiked() {
        return new CommentLikeException(ErrorCode.ALREADY_LIKED_COMMENT);
    }

    public static CommentLikeException likeNotFound() {
        return new CommentLikeException(ErrorCode.LIKE_NOT_FOUND);
    }
}
