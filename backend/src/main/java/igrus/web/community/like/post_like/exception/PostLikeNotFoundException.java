package igrus.web.community.like.post_like.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 게시글 좋아요를 찾을 수 없을 때 발생하는 예외.
 */
public class PostLikeNotFoundException extends CustomBaseException {

    public PostLikeNotFoundException() {
        super(ErrorCode.POST_LIKE_NOT_FOUND);
    }

    public PostLikeNotFoundException(String message) {
        super(ErrorCode.POST_LIKE_NOT_FOUND, message);
    }
}
