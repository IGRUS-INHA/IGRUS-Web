package igrus.web.security.auth.common.exception.account;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class AccountNotRecoverableException extends CustomBaseException {
    public AccountNotRecoverableException() {
        super(ErrorCode.ACCOUNT_NOT_RECOVERABLE);
    }
}
