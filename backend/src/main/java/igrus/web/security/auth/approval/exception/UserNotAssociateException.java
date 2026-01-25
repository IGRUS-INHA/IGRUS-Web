package igrus.web.security.auth.approval.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class UserNotAssociateException extends CustomBaseException {

    public UserNotAssociateException() {
        super(ErrorCode.USER_NOT_ASSOCIATE);
    }

    public UserNotAssociateException(Long userId) {
        super(ErrorCode.USER_NOT_ASSOCIATE, "해당 사용자는 준회원이 아닙니다: id=" + userId);
    }
}
