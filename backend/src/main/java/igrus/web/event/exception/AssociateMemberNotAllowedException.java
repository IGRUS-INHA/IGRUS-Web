package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 준회원이 행사에 신청하려 할 때 발생하는 예외.
 */
public class AssociateMemberNotAllowedException extends CustomBaseException {

    public AssociateMemberNotAllowedException() {
        super(ErrorCode.EVENT_ASSOCIATE_NOT_ALLOWED);
    }
}
