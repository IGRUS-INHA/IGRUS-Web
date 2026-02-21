package igrus.web.security.auth.common.exception.account;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class AccountSuspendedException extends CustomBaseException {
    public AccountSuspendedException() {
        super(AuthErrorCode.ACCOUNT_SUSPENDED);
    }
}
