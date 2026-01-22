package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class InvalidEmailException extends CustomBaseException {

    public InvalidEmailException() {
        super(ErrorCode.INVALID_EMAIL_FORMAT);
    }

    public InvalidEmailException(String email) {
        super(ErrorCode.INVALID_EMAIL_FORMAT, "유효하지 않은 이메일 형식입니다: " + email);
    }
}
