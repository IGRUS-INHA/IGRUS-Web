package igrus.web.user.domain;

public enum AccountChangeType {
    ROLE_CHANGE,       // 역할 변경
    SUSPENSION,        // 계정 정지
    SUSPENSION_LIFT,   // 정지 해제
    WITHDRAWAL,        // 탈퇴
    APPROVAL           // 준회원 승인
}
