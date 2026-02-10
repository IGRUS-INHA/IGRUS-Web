package igrus.web.community.pinnedpost.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 고정 게시글을 찾을 수 없을 때 발생하는 예외.
 */
public class PinnedPostNotFoundException extends CustomBaseException {

    public PinnedPostNotFoundException() {
        super(ErrorCode.PINNED_POST_NOT_FOUND);
    }

    public PinnedPostNotFoundException(Long id) {
        super(ErrorCode.PINNED_POST_NOT_FOUND, "고정 게시글을 찾을 수 없습니다: " + id);
    }
}
