package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class InvalidPhoneNumberException extends CustomBaseException {

    public InvalidPhoneNumberException() {
        super(UserErrorCode.INVALID_PHONE_NUMBER_FORMAT);
    }

    public InvalidPhoneNumberException(String phoneNumber) {
        super(UserErrorCode.INVALID_PHONE_NUMBER_FORMAT, "전화번호는 000-0000-0000 형식이어야 합니다: " + phoneNumber);
    }
}
