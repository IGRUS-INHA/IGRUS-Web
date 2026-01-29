package igrus.web.security.auth.common.domain;

/**
 * 로그인 실패 사유를 정의하는 열거형.
 */
public enum LoginFailureReason {

    /** 잘못된 자격 증명 (학번 또는 비밀번호 불일치) */
    INVALID_CREDENTIALS,

    /** 계정 잠금 (로그인 시도 횟수 초과) */
    ACCOUNT_LOCKED,

    /** 계정 정지 */
    ACCOUNT_SUSPENDED,

    /** 계정 탈퇴 (복구 불가) */
    ACCOUNT_WITHDRAWN,

    /** 이메일 미인증 */
    EMAIL_NOT_VERIFIED,

    /** 복구 가능한 탈퇴 계정 */
    ACCOUNT_RECOVERABLE
}
