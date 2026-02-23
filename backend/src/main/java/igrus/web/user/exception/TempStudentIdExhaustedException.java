package igrus.web.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class TempStudentIdExhaustedException extends CustomBaseException {

    public TempStudentIdExhaustedException() {
        super(UserErrorCode.TEMP_STUDENT_ID_EXHAUSTED);
    }
}
