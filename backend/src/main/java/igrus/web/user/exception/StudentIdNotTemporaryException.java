package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class StudentIdNotTemporaryException extends CustomBaseException {

    public StudentIdNotTemporaryException() {
        super(UserErrorCode.STUDENT_ID_NOT_TEMPORARY);
    }
}
