package igrus.web.security.auth.common.exception;

import igrus.web.common.exception.ErrorCode;
import lombok.Getter;

@Getter
public enum AuthErrorCode implements ErrorCode {

    // Auth
    INVALID_CREDENTIALS(401, "학번 또는 비밀번호가 올바르지 않습니다"),
    EMAIL_NOT_VERIFIED(401, "이메일 인증이 완료되지 않았습니다"),
    EMAIL_ALREADY_VERIFIED(400, "이미 인증된 이메일입니다"),
    VERIFICATION_CODE_EXPIRED(400, "인증 코드가 만료되었습니다"),
    VERIFICATION_CODE_INVALID(400, "유효하지 않은 인증 코드입니다"),
    VERIFICATION_ATTEMPTS_EXCEEDED(429, "인증 시도 횟수를 초과했습니다"),
    DUPLICATE_STUDENT_ID(409, "이미 가입된 학번입니다"),
    INVALID_PASSWORD_FORMAT(400, "비밀번호는 영문, 숫자를 포함하여 8자 이상이어야 합니다"),
    SAME_PASSWORD(400, "현재 비밀번호와 다른 비밀번호를 입력해주세요"),

    // Account
    PRIVACY_CONSENT_REQUIRED(400, "개인정보 처리방침 동의가 필요합니다"),
    ACCOUNT_SUSPENDED(403, "정지된 계정입니다"),
    ACCOUNT_WITHDRAWN(403, "탈퇴한 계정입니다"),
    ACCOUNT_RECOVERABLE(200, "복구 가능한 탈퇴 계정입니다"),
    ACCOUNT_NOT_RECOVERABLE(400, "복구 기간이 만료된 계정입니다"),
    ACCOUNT_LOCKED(423, "로그인 시도 횟수 초과로 계정이 잠겼습니다"),

    // Token
    REFRESH_TOKEN_INVALID(401, "유효하지 않은 리프레시 토큰입니다"),
    REFRESH_TOKEN_EXPIRED(401, "리프레시 토큰이 만료되었습니다"),
    REFRESH_TOKEN_THEFT_DETECTED(401, "토큰 도용이 감지되어 모든 세션이 종료되었습니다"),
    PASSWORD_RESET_TOKEN_INVALID(400, "유효하지 않은 비밀번호 재설정 토큰입니다"),
    PASSWORD_RESET_TOKEN_EXPIRED(400, "비밀번호 재설정 토큰이 만료되었습니다"),
    SSO_CODE_INVALID(401, "유효하지 않거나 만료된 SSO 코드입니다"),
    SSO_REDIRECT_URI_NOT_ALLOWED(400, "허용되지 않은 redirect_uri 입니다"),

    // Email & Verification
    EMAIL_SEND_FAILED(500, "이메일 발송에 실패했습니다"),
    RECENT_WITHDRAWAL_EXISTS(400, "최근 탈퇴 이력이 있어 재가입이 불가합니다"),
    VERIFICATION_RESEND_RATE_LIMITED(429, "인증 코드 재발송은 1분에 1회만 가능합니다"),
    EMAIL_VERIFICATION_REQUIRED(400, "이메일 인증이 필요합니다"),
    VERIFICATION_TOKEN_INVALID(400, "유효하지 않은 인증 토큰입니다"),

    // Approval
    ADMIN_REQUIRED(403, "관리자 권한이 필요합니다"),
    USER_NOT_ASSOCIATE(400, "해당 사용자는 준회원이 아닙니다"),
    LAST_ADMIN_CANNOT_CHANGE(400, "마지막 관리자는 권한을 변경할 수 없습니다"),
    BULK_APPROVAL_EMPTY(400, "승인할 사용자를 선택해주세요"),
    BULK_REJECTION_EMPTY(400, "거절할 사용자를 선택해주세요"),
    ASSOCIATE_ALREADY_DECIDED(400, "이미 처리된 준회원입니다"),

    // Signup
    INVALID_CUSTOM_FIELD(400, "기타 선택 시 직접 입력 값은 필수입니다");

    private final int status;
    private final String message;

    AuthErrorCode(int status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String getCode() {
        return this.name();
    }
}
