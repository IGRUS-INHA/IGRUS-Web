package igrus.web.survey.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 설문 응답 수집 상태를 나타내는 Enum.
 * 2축 상태 모델의 축 2: 응답 수집 상태를 관리한다.
 *
 * <p>상태 흐름:</p>
 * NOT_STARTED(시작 전) → OPEN(응답 수집 중) → CLOSED(응답 마감)
 * CLOSED → OPEN 역전이 가능 (재개)
 * NOT_STARTED → CLOSED 직접 전이 불가 (열지도 않았는데 마감할 수 없음)
 */
@Getter
@RequiredArgsConstructor
public enum SurveyResponseStatus {

    NOT_STARTED("시작 전", "아직 응답 수집을 시작하지 않음"),
    OPEN("응답 수집 중", "응답 수집 중"),
    CLOSED("응답 마감", "응답 받지 않음");

    private final String displayName;
    private final String description;

    /**
     * 해당 상태로 전이 가능한지 확인합니다.
     * NOT_STARTED → OPEN (최초 시작)
     * OPEN → CLOSED (마감)
     * CLOSED → OPEN (재개)
     *
     * @param target 전이하려는 상태
     * @return 전이 가능 여부
     */
    public boolean canTransitionTo(SurveyResponseStatus target) {
        if (this == target) {
            return false;
        }

        return switch (this) {
            case NOT_STARTED -> target == OPEN;
            case OPEN -> target == CLOSED;
            case CLOSED -> target == OPEN;
        };
    }

    /**
     * 응답을 수집 중인 상태인지 확인합니다.
     *
     * @return 응답 수집 중 여부
     */
    public boolean isAcceptingResponses() {
        return this == OPEN;
    }
}
