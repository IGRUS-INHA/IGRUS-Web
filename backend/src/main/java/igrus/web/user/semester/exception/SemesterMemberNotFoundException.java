package igrus.web.user.semester.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.user.exception.UserErrorCode;

public class SemesterMemberNotFoundException extends CustomBaseException {

    public SemesterMemberNotFoundException() {
        super(UserErrorCode.SEMESTER_MEMBER_NOT_FOUND);
    }

    public SemesterMemberNotFoundException(String message) {
        super(UserErrorCode.SEMESTER_MEMBER_NOT_FOUND, message);
    }
}
