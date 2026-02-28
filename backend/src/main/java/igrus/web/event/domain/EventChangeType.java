package igrus.web.event.domain;

/**
 * 행사 상태 변경 유형.
 * 사용자가 직접 수행한 상태 변경만 기록합니다.
 * 시간 기반 자동 전이(Lazy Evaluation)는 기록하지 않습니다.
 */
public enum EventChangeType {
    /** 행사 취소. */
    EVENT_CANCELED,
    /** 취소된 행사 재활성화. */
    EVENT_REACTIVATED,
    /** 관리자에 의한 수동 등록 마감. */
    REGISTRATION_CLOSED_MANUAL,
    /** 등록 마감 후 재오픈. */
    REGISTRATION_REOPENED,
    /** 행사 공개. */
    EVENT_PUBLISHED,
    /** 행사 비공개. */
    EVENT_UNPUBLISHED
}
