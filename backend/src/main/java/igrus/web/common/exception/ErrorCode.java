package igrus.web.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(405, "C002", "허용되지 않은 메서드입니다"),
    INTERNAL_SERVER_ERROR(500, "C003", "서버 내부 오류가 발생했습니다"),
    INVALID_TYPE_VALUE(400, "C004", "잘못된 타입입니다"),
    ACCESS_DENIED(403, "C005", "접근이 거부되었습니다"),

    // User
    USER_NOT_FOUND(404, "U001", "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(409, "U002", "이미 존재하는 이메일입니다"),
    INVALID_PASSWORD(401, "U003", "비밀번호가 일치하지 않습니다"),
    SAME_ROLE_CHANGE(400, "U004", "이전 역할과 새 역할이 동일합니다"),
    INVALID_STUDENT_ID(400, "U005", "학번은 8자리 숫자여야 합니다"),
    INVALID_EMAIL_FORMAT(400, "U006", "유효하지 않은 이메일 형식입니다");

    private final int status;
    private final String code;
    private final String message;
}
