package igrus.web.event.domain;

/**
 * 행사 신청 방식.
 */
public enum EventRegistrationType {

    /** 선착순 - 신청하면 바로 확정 */
    FIRST_COME,

    /** 선발제 - 운영진 승인 필요 */
    SELECTION
}
