package igrus.web.security.auth.common.exception.account;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class AccountSuspendedException extends CustomBaseException {
    public AccountSuspendedException() {
        super(ErrorCode.ACCOUNT_SUSPENDED);
    }
}
