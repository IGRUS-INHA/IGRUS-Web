package igrus.web.community.post.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 게시글 제목이 너무 길 때 발생하는 예외.
 */
@Getter
public class PostTitleTooLongException extends CustomBaseException {

    private final int actualLength;

    public PostTitleTooLongException(int actualLength) {
        super(ErrorCode.POST_TITLE_TOO_LONG, "게시글 제목이 너무 깁니다. 현재 길이: " + actualLength);
        this.actualLength = actualLength;
    }
}
