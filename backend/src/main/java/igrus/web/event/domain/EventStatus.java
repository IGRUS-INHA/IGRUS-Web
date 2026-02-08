package igrus.web.event.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 행사 상태를 나타내는 Enum.
 *
 * <p>상태 흐름:</p>
 * UPCOMING(예정) → OPEN(모집 중) ↔ CLOSED(마감) → ONGOING(진행 중) → COMPLETED(완료)
 * <p>CLOSED → OPEN 역전이: 정원 마감(CAPACITY_FULL) 후 취소로 자리가 생기면 다시 OPEN</p>
 */
@Getter
@RequiredArgsConstructor
public enum EventStatus {

    UPCOMING("예정", "신청 시작 전"),
    OPEN("모집 중", "신청 가능"),
    CLOSED("마감", "신청 마감"),
    ONGOING("진행 중", "행사 진행 중"),
    COMPLETED("완료", "행사 종료");

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
            return false;
        }

        return switch (this) {
            case UPCOMING -> target == OPEN;
            case OPEN -> target == CLOSED;
            case CLOSED -> target == OPEN || target == ONGOING;
            case ONGOING -> target == COMPLETED;
            case COMPLETED -> false;
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
     * 행사가 최종 종료된 상태인지 확인합니다.
     *
     * @return 종료 여부 (COMPLETED)
     */
    public boolean isTerminal() {
        return this == COMPLETED;
    }

    /**
     * 신청 마감 이후 상태인지 확인합니다.
     * CLOSED, ONGOING, COMPLETED 모두 해당합니다.
     *
     * @return 신청 마감 이후 여부
     */
    public boolean isFinished() {
        return this == CLOSED || this == ONGOING || this == COMPLETED;
    }

    /**
     * 행사 정보 수정이 가능한 상태인지 확인합니다.
     * ONGOING, COMPLETED 상태에서는 수정 불가.
     *
     * @return 수정 가능 여부
     */
    public boolean isEditable() {
        return this == UPCOMING || this == OPEN || this == CLOSED;
    }
}
