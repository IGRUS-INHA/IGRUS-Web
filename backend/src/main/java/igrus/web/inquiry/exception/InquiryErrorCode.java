package igrus.web.inquiry.exception;

import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum InquiryErrorCode implements ErrorCode {

    INQUIRY_NOT_FOUND(404, "문의를 찾을 수 없습니다"),
    INQUIRY_ACCESS_DENIED(403, "문의에 대한 접근 권한이 없습니다"),
    INQUIRY_ALREADY_REPLIED(409, "이미 답변이 작성된 문의입니다"),
    INQUIRY_INVALID_PASSWORD(401, "문의 비밀번호가 일치하지 않습니다"),
    INQUIRY_MAX_ATTACHMENTS_EXCEEDED(400, "첨부파일은 최대 3개까지 가능합니다"),
    INQUIRY_NUMBER_GENERATION_FAILED(500, "문의 번호 생성에 실패했습니다"),
    GUEST_INQUIRY_EMAIL_REQUIRED(400, "비회원 문의 시 이메일은 필수입니다"),
    GUEST_INQUIRY_NAME_REQUIRED(400, "비회원 문의 시 이름은 필수입니다"),
    GUEST_INQUIRY_PASSWORD_REQUIRED(400, "비회원 문의 시 비밀번호는 필수입니다"),
    INQUIRY_REPLY_NOT_FOUND(404, "답변을 찾을 수 없습니다"),
    INVALID_STATUS_TRANSITION(400, "허용되지 않은 상태 변경입니다");

    private final int status;
    private final String message;

    InquiryErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
