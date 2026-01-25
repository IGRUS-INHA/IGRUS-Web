package igrus.web.security.auth.common.exception.account;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class AccountWithdrawnException extends CustomBaseException {
    public AccountWithdrawnException() {
        super(ErrorCode.ACCOUNT_WITHDRAWN);
    }
}
