package igrus.web.community.post.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.community.exception.CommunityErrorCode;

/**
 * 삭제된 게시글에 접근할 때 발생하는 예외.
 */
public class PostDeletedException extends CustomBaseException {

    public PostDeletedException() {
        super(CommunityErrorCode.POST_DELETED);
    }

    public PostDeletedException(Long postId) {
        super(CommunityErrorCode.POST_DELETED, "삭제된 게시글입니다: " + postId);
    }
}
