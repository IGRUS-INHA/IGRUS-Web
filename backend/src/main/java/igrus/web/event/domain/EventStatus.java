package igrus.web.event.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 행사 진행 상태를 나타내는 Enum.
 * 2축 상태 모델의 축 2: 행사 진행 상태를 관리한다.
 *
 * <p>상태 흐름:</p>
 * UPCOMING(예정) → ONGOING(진행 중) → COMPLETED(완료, 종단)
 * UPCOMING/ONGOING → CANCELED(취소, 재활성화 가능)
 * CANCELED → UPCOMING/ONGOING(재활성화)
 */
@Getter
@RequiredArgsConstructor
public enum EventStatus {

    UPCOMING("예정", "행사 시작 전"),
    ONGOING("진행 중", "행사 진행 중"),
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
            return false;
        }

        return switch (this) {
            case UPCOMING -> target == ONGOING || target == CANCELED;
            case ONGOING -> target == COMPLETED || target == CANCELED;
            case COMPLETED -> false; // 종단 상태
            case CANCELED -> target == UPCOMING || target == ONGOING; // 재활성화
        };
    }

    /**
     * 행사가 최종 종료된 상태인지 확인합니다.
     *
     * @return 종료 여부 (COMPLETED)
     */
    public boolean isTerminal() {
        return this == COMPLETED;
    }
}
