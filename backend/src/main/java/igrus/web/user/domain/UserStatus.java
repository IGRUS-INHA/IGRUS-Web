package igrus.web.user.domain;

public enum UserStatus {
    ACTIVE,      // 정상 이용
    INACTIVE,    // 명단 미포함, 로그인 불가
    SUSPENDED,   // 관리자 정지
    WITHDRAWN    // 본인 탈퇴
}
