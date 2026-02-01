package igrus.web.user.semester.exception;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class SemesterMemberAlreadyExistsException extends CustomBaseException {

    public SemesterMemberAlreadyExistsException() {
        super(ErrorCode.SEMESTER_MEMBER_ALREADY_EXISTS);
    }

    public SemesterMemberAlreadyExistsException(String message) {
        super(ErrorCode.SEMESTER_MEMBER_ALREADY_EXISTS, message);
    }
}
