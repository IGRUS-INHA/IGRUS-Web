package igrus.web.inquiry.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public enum InquiryStatus {
    PENDING("대기 중"),
    IN_PROGRESS("처리 중"),
    COMPLETED("완료");

    private final String description;

    /**
     * 현재 상태에서 새로운 상태로의 전이가 허용되는지 확인합니다.
     *
     * 허용된 상태 전이:
     * - PENDING → IN_PROGRESS, COMPLETED
     * - IN_PROGRESS → PENDING, COMPLETED
     * - COMPLETED → (변경 불가)
     *
     * @param newStatus 전환하려는 새 상태
     * @return 전이가 허용되면 true, 그렇지 않으면 false
     */
    public boolean canTransitionTo(InquiryStatus newStatus) {
        if (this == newStatus) {
            return true; // 동일 상태로의 전이는 허용 (idempotent)
        }

        return switch (this) {
            case PENDING -> Set.of(IN_PROGRESS, COMPLETED).contains(newStatus);
            case IN_PROGRESS -> Set.of(PENDING, COMPLETED).contains(newStatus);
            case COMPLETED -> false; // 완료 상태에서는 변경 불가
        };
    }
}
