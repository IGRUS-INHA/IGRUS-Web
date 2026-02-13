package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class TempStudentIdNotAvailableException extends CustomBaseException {

    public TempStudentIdNotAvailableException() {
        super(ErrorCode.TEMP_STUDENT_ID_NOT_AVAILABLE);
    }
}
