package igrus.web.security.auth.approval.domain;

public enum AssociateDecisionType {
    APPROVED,  // 승인
    REJECTED,  // 거절
    DEMOTED    // 강등 (정회원 → 준회원)
}
