package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class InvalidGradeException extends CustomBaseException {

    public InvalidGradeException() {
        super(ErrorCode.INVALID_GRADE);
    }

    public InvalidGradeException(int grade) {
        super(ErrorCode.INVALID_GRADE, "학년은 1 이상이어야 합니다: " + grade);
    }
}
