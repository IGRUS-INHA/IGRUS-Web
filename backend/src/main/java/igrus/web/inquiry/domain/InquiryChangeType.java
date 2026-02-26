package igrus.web.inquiry.domain;

/**
 * 문의 상태 변경 유형.
 */
public enum InquiryChangeType {
    /** 관리자에 의한 수동 상태 변경. */
    STATUS_CHANGED,
    /** 답변 작성에 의한 상태 변경 (답변 완료 처리). */
    REPLY_COMPLETED
}
