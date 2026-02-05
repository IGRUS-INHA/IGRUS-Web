package igrus.web.event.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 행사 마감 사유를 나타내는 Enum.
 * EventStatus가 CLOSED일 때만 값이 설정됩니다.
 */
@Getter
@RequiredArgsConstructor
public enum EventCloseReason {

    CAPACITY_FULL("정원 마감", "정원이 모두 찼습니다"),
    DEADLINE_PASSED("기한 마감", "신청 마감일이 지났습니다"),
    MANUAL_CLOSE("수동 마감", "운영자가 조기 마감했습니다");

    private final String displayName;
    private final String description;
}
