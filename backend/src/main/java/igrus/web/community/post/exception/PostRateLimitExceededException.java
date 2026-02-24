package igrus.web.community.post.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.community.exception.CommunityErrorCode;

/**
 * 게시글 작성 속도 제한을 초과했을 때 발생하는 예외.
 */
public class PostRateLimitExceededException extends CustomBaseException {

    public PostRateLimitExceededException() {
        super(CommunityErrorCode.POST_RATE_LIMIT_EXCEEDED);
    }

    public PostRateLimitExceededException(String message) {
        super(CommunityErrorCode.POST_RATE_LIMIT_EXCEEDED, message);
    }
}
