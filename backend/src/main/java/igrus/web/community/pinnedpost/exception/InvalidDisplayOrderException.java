package igrus.web.community.pinnedpost.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.community.exception.CommunityErrorCode;

/**
 * 유효하지 않은 표시 순서 값이 입력되었을 때 발생하는 예외.
 */
public class InvalidDisplayOrderException extends CustomBaseException {

    public InvalidDisplayOrderException(Integer order) {
        super(CommunityErrorCode.INVALID_DISPLAY_ORDER, "표시 순서는 1 이상이어야 합니다. 입력된 값: " + order);
    }
}
