package igrus.web.inquiry.event;

import igrus.web.inquiry.domain.InquiryChangeType;

import java.util.Objects;

/**
 * 문의 상태 변경 이벤트.
 * {@link igrus.web.inquiry.service.manage.RecordInquiryStatusChangeService}에서 수신하여
 * 감사 이력을 기록합니다.
 */
public record InquiryStatusChangeEvent(
        Long inquiryId,
        Long changedByUserId,
        InquiryChangeType changeType,
        String previousValue,
        String newValue
) {
    public InquiryStatusChangeEvent {
        Objects.requireNonNull(changeType, "changeType must not be null");
        Objects.requireNonNull(previousValue, "previousValue must not be null");
        Objects.requireNonNull(newValue, "newValue must not be null");
    }
}
