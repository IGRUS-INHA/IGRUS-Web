package igrus.web.event.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 행사 등록(모집) 상태를 나타내는 Enum.
 * 2축 상태 모델의 축 1: 등록 상태를 관리한다.
 *
 * <p>상태 흐름:</p>
 * NOT_STARTED(등록 시작 전) → OPEN(등록 접수 중) → CLOSED(등록 마감)
 * <p>CLOSED → OPEN 역전이: 정원 마감(CAPACITY_FULL) 후 취소로 자리가 생기거나, 운영자 수동 재오픈</p>
 */
@Getter
@RequiredArgsConstructor
public enum RegistrationStatus {

    NOT_STARTED("등록 시작 전", "신청 시작 전"),
    OPEN("등록 접수 중", "신청 가능"),
    CLOSED("등록 마감", "신청 마감");

    private final String displayName;
    private final String description;

    /**
     * 해당 상태로 전이 가능한지 확인합니다.
     *
     * @param target 전이하려는 상태
     * @return 전이 가능 여부
     */
    public boolean canTransitionTo(RegistrationStatus target) {
        if (this == target) {
            return false;
        }

        return switch (this) {
            case NOT_STARTED -> target == OPEN; // CLOSED: cancel()에서 직접 필드 설정 (canTransitionTo 미사용)
            case OPEN -> target == CLOSED;
            case CLOSED -> target == OPEN; // 자동 재오픈 또는 수동 재오픈
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
}
