package igrus.web.community.like.postlike.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 이미 게시글 좋아요가 존재할 때 발생하는 예외.
 */
public class PostLikeAlreadyExistsException extends CustomBaseException {

    public PostLikeAlreadyExistsException() {
        super(ErrorCode.POST_LIKE_ALREADY_EXISTS);
    }

    public PostLikeAlreadyExistsException(String message) {
        super(ErrorCode.POST_LIKE_ALREADY_EXISTS, message);
    }
}
