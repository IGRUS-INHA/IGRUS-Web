package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class DuplicatePhoneNumberException extends CustomBaseException {

    public DuplicatePhoneNumberException() {
        super(ErrorCode.DUPLICATE_PHONE_NUMBER);
    }

    public DuplicatePhoneNumberException(String phoneNumber) {
        super(ErrorCode.DUPLICATE_PHONE_NUMBER, "이미 등록된 전화번호입니다: " + phoneNumber);
    }
}
