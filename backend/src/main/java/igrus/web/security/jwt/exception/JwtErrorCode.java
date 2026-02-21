package igrus.web.security.jwt.exception;

import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum JwtErrorCode implements ErrorCode {

    JWT_SECRET_KEY_TOO_SHORT(500, "JWT 비밀키 길이가 최소 요구사항을 충족하지 않습니다"),
    ACCESS_TOKEN_INVALID(401, "유효하지 않은 액세스 토큰입니다"),
    ACCESS_TOKEN_EXPIRED(401, "액세스 토큰이 만료되었습니다"),
    INVALID_TOKEN_TYPE(401, "올바르지 않은 토큰 타입입니다");

    private final int status;
    private final String message;

    JwtErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
