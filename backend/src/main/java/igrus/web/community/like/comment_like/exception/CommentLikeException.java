package igrus.web.community.like.comment_like.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;
import igrus.web.community.exception.CommunityErrorCode;

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
        return new CommentLikeException(CommunityErrorCode.CANNOT_LIKE_OWN_COMMENT);
    }

    public static CommentLikeException alreadyLiked() {
        return new CommentLikeException(CommunityErrorCode.ALREADY_LIKED_COMMENT);
    }

    public static CommentLikeException likeNotFound() {
        return new CommentLikeException(CommunityErrorCode.LIKE_NOT_FOUND);
    }
}
