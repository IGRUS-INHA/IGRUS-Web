package igrus.web.user.domain;

public enum UserRole {
    ASSOCIATE,   // 준회원 (가입 완료, 승인 대기)
    MEMBER,      // 정회원 (승인 완료)
    OPERATOR,    // 운영진
    ADMIN        // 관리자
}
