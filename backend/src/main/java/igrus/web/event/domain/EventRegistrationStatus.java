package igrus.web.event.domain;

/**
 * 행사 신청 상태.
 */
public enum EventRegistrationStatus {

    /** 선착순 - 신청 완료 */
    REGISTERED,

    /** 선발제 - 승인 대기 중 */
    WAITING,

    /** 선발제 - 승인됨 */
    APPROVED,

    /** 선발제 - 거절됨 */
    REJECTED,

    /** 공통 - 사용자가 취소 */
    CANCELED
}
