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
    INVALID_EMAIL_FORMAT(400, "U006", "유효하지 않은 이메일 형식입니다"),

    // Inquiry
    INQUIRY_NOT_FOUND(404, "I001", "문의를 찾을 수 없습니다"),
    INQUIRY_ACCESS_DENIED(403, "I002", "문의에 대한 접근 권한이 없습니다"),
    INQUIRY_ALREADY_REPLIED(409, "I003", "이미 답변이 작성된 문의입니다"),
    INQUIRY_INVALID_PASSWORD(401, "I004", "문의 비밀번호가 일치하지 않습니다"),
    INQUIRY_MAX_ATTACHMENTS_EXCEEDED(400, "I005", "첨부파일은 최대 3개까지 가능합니다"),
    INQUIRY_NUMBER_GENERATION_FAILED(500, "I006", "문의 번호 생성에 실패했습니다"),
    GUEST_INQUIRY_EMAIL_REQUIRED(400, "I007", "비회원 문의 시 이메일은 필수입니다"),
    GUEST_INQUIRY_NAME_REQUIRED(400, "I008", "비회원 문의 시 이름은 필수입니다"),
    GUEST_INQUIRY_PASSWORD_REQUIRED(400, "I009", "비회원 문의 시 비밀번호는 필수입니다"),
    INQUIRY_REPLY_NOT_FOUND(404, "I010", "답변을 찾을 수 없습니다"),
    INVALID_STATUS_TRANSITION(400, "I011", "허용되지 않은 상태 변경입니다");

    private final int status;
    private final String code;
    private final String message;
}
