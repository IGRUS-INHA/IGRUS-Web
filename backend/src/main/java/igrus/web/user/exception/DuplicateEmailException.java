package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class DuplicateEmailException extends CustomBaseException {

    public DuplicateEmailException() {
        super(UserErrorCode.DUPLICATE_EMAIL);
    }

    public DuplicateEmailException(String email) {
        super(UserErrorCode.DUPLICATE_EMAIL, "이미 존재하는 이메일입니다: " + email);
    }
}
