package igrus.web.security.auth.common.exception.account;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

public class RecentWithdrawalExistsException extends CustomBaseException {
    public RecentWithdrawalExistsException() {
        super(ErrorCode.RECENT_WITHDRAWAL_EXISTS);
    }
}
