package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class DuplicatePhoneNumberException extends CustomBaseException {

    public DuplicatePhoneNumberException() {
        super(UserErrorCode.DUPLICATE_PHONE_NUMBER);
    }

    public DuplicatePhoneNumberException(String phoneNumber) {
        super(UserErrorCode.DUPLICATE_PHONE_NUMBER, "이미 등록된 전화번호입니다: " + phoneNumber);
    }
}
