package igrus.web.community.post.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 게시글의 익명 설정 변경 시도 시 발생하는 예외.
 */
public class PostAnonymousUnchangeableException extends CustomBaseException {

    public PostAnonymousUnchangeableException() {
        super(ErrorCode.POST_ANONYMOUS_UNCHANGEABLE);
    }

    public PostAnonymousUnchangeableException(Long postId) {
        super(ErrorCode.POST_ANONYMOUS_UNCHANGEABLE, "익명 설정은 변경할 수 없습니다: " + postId);
    }
}
