package igrus.web.user.domain;

public enum AccountChangeType {
    ROLE_CHANGE,       // 역할 변경
    SUSPENSION,        // 계정 정지
    SUSPENSION_LIFT,   // 정지 해제
    WITHDRAWAL,        // 탈퇴
    FORCE_WITHDRAWAL,  // 강제 탈퇴
    APPROVAL,          // 준회원 승인
    FORCE_ACTIVATION,  // 관리자 강제 활성화 (이메일 인증 우회)
    ADMIN_INFO_EDIT,   // 관리자 사용자 정보 수정
    STUDENT_ID_UPDATE  // 임시 학번 → 실제 학번 변경
}
