package igrus.web.event.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 행사 신청 방식.
 */
@Getter
@RequiredArgsConstructor
public enum EventRegistrationType {

    /** 자동 승인 (선착순) - 신청하면 바로 확정 */
    AUTO_APPROVE("자동 승인", "신청 즉시 확정됩니다"),

    /** 수동 승인 (선발제) - 운영진 승인 필요 */
    MANUAL_APPROVE("수동 승인", "운영자 승인 후 확정됩니다");

    private final String displayName;
    private final String description;
}
