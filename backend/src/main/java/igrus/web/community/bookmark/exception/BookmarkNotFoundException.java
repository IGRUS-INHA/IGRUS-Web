package igrus.web.community.bookmark.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 북마크를 찾을 수 없을 때 발생하는 예외.
 */
public class BookmarkNotFoundException extends CustomBaseException {

    public BookmarkNotFoundException() {
        super(ErrorCode.BOOKMARK_NOT_FOUND);
    }

    public BookmarkNotFoundException(String message) {
        super(ErrorCode.BOOKMARK_NOT_FOUND, message);
    }
}
