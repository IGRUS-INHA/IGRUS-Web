package igrus.web.security.auth.common.exception.account;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class AccountNotRecoverableException extends CustomBaseException {
    public AccountNotRecoverableException() {
        super(AuthErrorCode.ACCOUNT_NOT_RECOVERABLE);
    }
}
