package igrus.web.user.semester.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class SemesterMemberNotFoundException extends CustomBaseException {

    public SemesterMemberNotFoundException() {
        super(ErrorCode.SEMESTER_MEMBER_NOT_FOUND);
    }

    public SemesterMemberNotFoundException(String message) {
        super(ErrorCode.SEMESTER_MEMBER_NOT_FOUND, message);
    }
}
