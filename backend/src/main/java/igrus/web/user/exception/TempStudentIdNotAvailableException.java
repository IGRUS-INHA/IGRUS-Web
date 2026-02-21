package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class TempStudentIdNotAvailableException extends CustomBaseException {

    public TempStudentIdNotAvailableException() {
        super(UserErrorCode.TEMP_STUDENT_ID_NOT_AVAILABLE);
    }
}
