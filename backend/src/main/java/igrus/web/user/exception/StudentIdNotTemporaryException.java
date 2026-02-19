package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class StudentIdNotTemporaryException extends CustomBaseException {

    public StudentIdNotTemporaryException() {
        super(ErrorCode.STUDENT_ID_NOT_TEMPORARY);
    }
}
