package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class UserNotFoundException extends CustomBaseException {

    public UserNotFoundException() {
        super(UserErrorCode.USER_NOT_FOUND);
    }

    public UserNotFoundException(String message) {
        super(UserErrorCode.USER_NOT_FOUND, message);
    }

    public UserNotFoundException(Long userId) {
        super(UserErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다: id=" + userId);
    }
}
