package igrus.web.event.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * 외부인 신청 시 동일 학번으로 가입된 회원이 존재할 때 발생하는 예외.
 */
public class RegisteredMemberExistsException extends CustomBaseException {

    public RegisteredMemberExistsException() {
        super(EventErrorCode.REGISTERED_MEMBER_EXISTS);
    }

    public RegisteredMemberExistsException(String message) {
        super(EventErrorCode.REGISTERED_MEMBER_EXISTS, message);
    }
}
