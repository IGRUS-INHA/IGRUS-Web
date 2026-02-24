package igrus.web.common.exception;

import lombok.Getter;

@Getter
public enum CommonErrorCode implements ErrorCode {

    INVALID_INPUT_VALUE(400, "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(405, "허용되지 않은 메서드입니다"),
    INTERNAL_SERVER_ERROR(500, "서버 내부 오류가 발생했습니다"),
    INVALID_TYPE_VALUE(400, "잘못된 타입입니다"),
    ACCESS_DENIED(403, "접근이 거부되었습니다"),
    RESOURCE_NOT_FOUND(404, "요청한 리소스를 찾을 수 없습니다");

    private final int status;
    private final String message;

    CommonErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
