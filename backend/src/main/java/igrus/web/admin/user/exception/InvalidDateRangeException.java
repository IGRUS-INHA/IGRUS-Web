package igrus.web.admin.user.exception;

import igrus.web.common.exception.CustomBaseException;

public class InvalidDateRangeException extends CustomBaseException {

    public InvalidDateRangeException() {
        super(AdminErrorCode.INVALID_DATE_RANGE);
    }
}
