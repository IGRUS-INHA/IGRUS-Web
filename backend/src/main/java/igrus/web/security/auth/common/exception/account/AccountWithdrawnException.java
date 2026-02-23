package igrus.web.security.auth.common.exception.account;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class AccountWithdrawnException extends CustomBaseException {
    public AccountWithdrawnException() {
        super(AuthErrorCode.ACCOUNT_WITHDRAWN);
    }
}
