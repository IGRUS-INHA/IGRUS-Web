package igrus.web.admin.user.exception;

import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum AdminErrorCode implements ErrorCode {

    SELF_ROLE_CHANGE_NOT_ALLOWED(400, "자기 자신의 권한은 변경할 수 없습니다"),
    SELF_STATUS_CHANGE_NOT_ALLOWED(400, "자기 자신의 상태는 변경할 수 없습니다"),
    INVALID_DATE_RANGE(400, "종료 일시는 시작 일시 이후여야 합니다"),
    USER_NOT_PENDING_VERIFICATION(400, "해당 사용자는 이메일 인증 대기 상태가 아닙니다"),
    LAST_ADMIN_CANNOT_WITHDRAW(400, "마지막 관리자는 강제 탈퇴할 수 없습니다");

    private final int status;
    private final String message;

    AdminErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
