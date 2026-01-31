package igrus.web.community.post.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 게시글을 찾을 수 없을 때 발생하는 예외.
 */
public class PostNotFoundException extends CustomBaseException {

    public PostNotFoundException() {
        super(ErrorCode.POST_NOT_FOUND);
    }

    public PostNotFoundException(Long postId) {
        super(ErrorCode.POST_NOT_FOUND, "게시글을 찾을 수 없습니다: " + postId);
    }
}
