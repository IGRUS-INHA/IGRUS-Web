package igrus.web.common.exception;

import lombok.Getter;

@Getter
public abstract class CustomBaseException extends RuntimeException {

    private final ErrorCode errorCode;

    protected CustomBaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    protected CustomBaseException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    protected CustomBaseException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
