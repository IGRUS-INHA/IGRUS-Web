package igrus.web.community.pinnedpost.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 이미 고정된 게시글을 다시 고정하려 할 때 발생하는 예외.
 */
public class PinnedPostAlreadyExistsException extends CustomBaseException {

    public PinnedPostAlreadyExistsException(Long postId) {
        super(ErrorCode.PINNED_POST_ALREADY_EXISTS, "이미 고정된 게시글입니다: " + postId);
    }
}
