package igrus.web.security.auth.common.exception.account;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 로그인 시도 횟수 초과로 계정이 잠긴 경우 발생하는 예외.
 */
public class AccountLockedException extends CustomBaseException {
    public AccountLockedException() {
        super(ErrorCode.ACCOUNT_LOCKED);
    }
}
