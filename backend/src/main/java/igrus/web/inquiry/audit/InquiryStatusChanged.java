package igrus.web.inquiry.audit;

import igrus.web.inquiry.domain.InquiryChangeType;

import java.util.Objects;

/**
 * 문의 상태 변경 감사 이벤트.
 * {@link igrus.web.inquiry.service.manage.RecordInquiryStatusChangeService}에서 수신하여
 * 감사 이력을 기록합니다.
 */
public record InquiryStatusChanged(
        Long inquiryId,
        Long changedByUserId,
        InquiryChangeType changeType,
        String previousValue,
        String newValue
) {
    public InquiryStatusChanged {
        Objects.requireNonNull(changeType, "changeType must not be null");
        Objects.requireNonNull(previousValue, "previousValue must not be null");
        Objects.requireNonNull(newValue, "newValue must not be null");
    }
}
