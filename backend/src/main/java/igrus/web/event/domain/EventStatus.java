package igrus.web.event.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

/**
 * 행사 상태를 나타내는 Enum.
 */
@Getter
@RequiredArgsConstructor
public enum EventStatus {

    UPCOMING("예정", "신청 시작 전"),
    OPEN("모집 중", "신청 가능"),
    CLOSED("마감", "신청 마감"),
    COMPLETED("완료", "행사 종료"),
    CANCELED("취소", "행사 취소됨");

    private final String displayName;
    private final String description;

    /**
     * 해당 상태로 전이 가능한지 확인합니다.
     *
     * @param target 전이하려는 상태
     * @return 전이 가능 여부
     */
    public boolean canTransitionTo(EventStatus target) {
        if (this == target) {
            return false; // 같은 상태로 전이 불가
        }

        return switch (this) {
            case UPCOMING -> target == OPEN || target == CANCELED;
            case OPEN -> target == CLOSED || target == COMPLETED || target == CANCELED;
            case CLOSED -> target == OPEN || target == COMPLETED || target == CANCELED;
            case COMPLETED, CANCELED -> false; // 종료 상태에서는 전이 불가
        };
    }

    /**
     * 신청 가능한 상태인지 확인합니다.
     *
     * @return 신청 가능 여부
     */
    public boolean isRegistrable() {
        return this == OPEN;
    }

    /**
     * 행사가 종료된 상태인지 확인합니다.
     *
     * @return 종료 여부 (COMPLETED, CANCELED)
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELED;
    }

    /**
     * 행사가 종료된 상태인지 확인합니다.
     *
     * @return 종료 여부 (CLOSED, COMPLETED, CANCELED)
     */
    public boolean isFinished() {
        return this == CLOSED || this == COMPLETED || this == CANCELED;
    }

    /**
     * 행사 정보 수정이 가능한 상태인지 확인합니다.
     * COMPLETED, CANCELED 상태에서는 수정 불가.
     *
     * @return 수정 가능 여부
     */
    public boolean isEditable() {
        return this == UPCOMING || this == OPEN || this == CLOSED;
    }
}
