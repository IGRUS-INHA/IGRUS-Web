package igrus.web.security.auth.common.exception.account;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.security.auth.common.exception.AuthErrorCode;

public class RecentWithdrawalExistsException extends CustomBaseException {
    public RecentWithdrawalExistsException() {
        super(AuthErrorCode.RECENT_WITHDRAWAL_EXISTS);
    }
}
