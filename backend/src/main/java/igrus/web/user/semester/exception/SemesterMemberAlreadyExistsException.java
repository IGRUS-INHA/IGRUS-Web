package igrus.web.user.semester.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.user.exception.UserErrorCode;

public class SemesterMemberAlreadyExistsException extends CustomBaseException {

    public SemesterMemberAlreadyExistsException() {
        super(UserErrorCode.SEMESTER_MEMBER_ALREADY_EXISTS);
    }

    public SemesterMemberAlreadyExistsException(String message) {
        super(UserErrorCode.SEMESTER_MEMBER_ALREADY_EXISTS, message);
    }
}
