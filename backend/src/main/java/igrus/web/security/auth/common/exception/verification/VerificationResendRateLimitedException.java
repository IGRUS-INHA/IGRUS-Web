package igrus.web.security.auth.common.exception.verification;

import igrus.web.common.exception.CustomBaseException;
import igrus.web.common.exception.ErrorCode;

/**
 * 인증 코드 재발송 Rate Limit 초과 시 발생하는 예외.
 *
 * <p>동일 이메일로 5분 내 재발송 요청 시 발생합니다.</p>
 */
public class VerificationResendRateLimitedException extends CustomBaseException {
    public VerificationResendRateLimitedException() {
        super(ErrorCode.VERIFICATION_RESEND_RATE_LIMITED);
    }
}
