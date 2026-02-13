package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class TempStudentIdExhaustedException extends CustomBaseException {

    public TempStudentIdExhaustedException() {
        super(ErrorCode.TEMP_STUDENT_ID_EXHAUSTED);
    }
}
