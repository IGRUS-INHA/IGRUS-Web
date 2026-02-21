package igrus.web.security.jwt.exception;

import igrus.web.common.exception.CustomBaseException;

/**
 * JWT 비밀키 길이가 최소 요구사항을 충족하지 않을 때 발생하는 예외.
 */
public class JwtSecretKeyTooShortException extends CustomBaseException {

    public JwtSecretKeyTooShortException() {
        super(JwtErrorCode.JWT_SECRET_KEY_TOO_SHORT);
    }

    public JwtSecretKeyTooShortException(int minimumLength) {
        super(JwtErrorCode.JWT_SECRET_KEY_TOO_SHORT,
                "JWT 비밀키는 최소 " + minimumLength + "바이트 필요합니다");
    }
}
