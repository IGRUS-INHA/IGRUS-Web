package igrus.web.security.auth.common.exception.account;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

import java.time.Instant;

/**
 * 복구 가능한 탈퇴 계정 로그인 시 발생하는 예외.
 * <p>
 * 탈퇴 후 5일 이내에 로그인을 시도하면 이 예외가 발생하며,
 * 클라이언트는 이 예외를 받으면 복구 플로우로 안내합니다.
 */
@Getter
public class AccountRecoverableException extends CustomBaseException {

    private final String studentId;
    private final Instant recoveryDeadline;

    public AccountRecoverableException(String studentId, Instant recoveryDeadline) {
        super(ErrorCode.ACCOUNT_RECOVERABLE);
        this.studentId = studentId;
        this.recoveryDeadline = recoveryDeadline;
    }
}
