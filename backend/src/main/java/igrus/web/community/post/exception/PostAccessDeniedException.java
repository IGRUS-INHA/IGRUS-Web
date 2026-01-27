package igrus.web.community.post.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 게시글 접근이 거부되었을 때 발생하는 예외.
 */
public class PostAccessDeniedException extends CustomBaseException {

    public PostAccessDeniedException() {
        super(ErrorCode.POST_ACCESS_DENIED);
    }

    public PostAccessDeniedException(String message) {
        super(ErrorCode.POST_ACCESS_DENIED, message);
    }
}
