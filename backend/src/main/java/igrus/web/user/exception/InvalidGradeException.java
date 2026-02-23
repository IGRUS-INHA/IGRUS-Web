package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class InvalidGradeException extends CustomBaseException {

    public InvalidGradeException() {
        super(UserErrorCode.INVALID_GRADE);
    }

    public InvalidGradeException(int grade) {
        super(UserErrorCode.INVALID_GRADE, "학년은 1 이상이어야 합니다: " + grade);
    }
}
