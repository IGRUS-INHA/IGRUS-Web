package igrus.web.security.auth.common.exception.verification;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 이메일 재전송 시 해당 이메일로 가입 요청된 계정이 없을 때 발생하는 예외.
 *
 * <p>가입 요청되지 않은 이메일로 인증 코드 재발송을 시도할 경우 발생합니다.</p>
 */
public class VerificationEmailNotFoundException extends CustomBaseException {
    public VerificationEmailNotFoundException() {
        super(ErrorCode.VERIFICATION_EMAIL_NOT_FOUND);
    }
}
