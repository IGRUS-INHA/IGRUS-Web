package igrus.web.user.domain;

public enum UserStatus {
    PENDING_VERIFICATION, // 이메일 인증 대기
    ACTIVE,               // 정상
    SUSPENDED,            // 정지
    WITHDRAWN             // 탈퇴
}
