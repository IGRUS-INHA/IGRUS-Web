package igrus.web.survey.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 설문 공개 상태를 나타내는 Enum.
 * 2축 상태 모델의 축 1: 공개 상태를 관리한다.
 *
 * <p>상태 흐름:</p>
 * DRAFT(비공개) → PUBLISHED(공개)
 * PUBLISHED → DRAFT 역전이 불가
 */
@Getter
@RequiredArgsConstructor
public enum SurveyVisibility {

    DRAFT("비공개", "작성 중"),
    PUBLISHED("공개", "응답자에게 노출");

    private final String displayName;
    private final String description;

    /**
     * 해당 상태로 전이 가능한지 확인합니다.
     * DRAFT → PUBLISHED만 허용, PUBLISHED → DRAFT 금지
     *
     * @param target 전이하려는 상태
     * @return 전이 가능 여부
     */
    public boolean canTransitionTo(SurveyVisibility target) {
        if (this == target) {
            return false;
        }

        return switch (this) {
            case DRAFT -> target == PUBLISHED;
            case PUBLISHED -> false;
        };
    }
}
