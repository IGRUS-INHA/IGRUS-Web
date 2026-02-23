package igrus.web.community.bookmark.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.community.exception.CommunityErrorCode;

/**
 * 이미 북마크가 존재할 때 발생하는 예외.
 */
public class BookmarkAlreadyExistsException extends CustomBaseException {

    public BookmarkAlreadyExistsException() {
        super(CommunityErrorCode.BOOKMARK_ALREADY_EXISTS);
    }

    public BookmarkAlreadyExistsException(String message) {
        super(CommunityErrorCode.BOOKMARK_ALREADY_EXISTS, message);
    }
}
