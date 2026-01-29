package igrus.web.community.post.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

/**
 * 게시글 이미지 개수가 제한을 초과했을 때 발생하는 예외.
 */
@Getter
public class PostImageLimitExceededException extends CustomBaseException {

    private final int maxAllowed;
    private final int actualCount;

    public PostImageLimitExceededException(int maxAllowed, int actualCount) {
        super(ErrorCode.POST_IMAGE_LIMIT_EXCEEDED,
                "게시글 이미지 개수가 제한을 초과했습니다. 최대: " + maxAllowed + ", 현재: " + actualCount);
        this.maxAllowed = maxAllowed;
        this.actualCount = actualCount;
    }
}
