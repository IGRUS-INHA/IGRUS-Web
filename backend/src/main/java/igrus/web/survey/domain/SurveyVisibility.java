package igrus.web.survey.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 설문 공개 상태를 나타내는 Enum.
 * 2축 상태 모델의 축 1: 공개 상태를 관리한다.
 *
 * <p>상태 흐름:</p>
 * UNPUBLISHED(비공개) ⇄ PUBLISHED(공개) 양방향 전이 가능
 */
@Getter
@RequiredArgsConstructor
public enum SurveyVisibility {

    UNPUBLISHED("비공개", "응답자에게 노출되지 않음"),
    PUBLISHED("공개", "응답자에게 노출");

    private final String displayName;
    private final String description;

    /**
     * 해당 상태로 전이 가능한지 확인합니다.
     * UNPUBLISHED ⇄ PUBLISHED 양방향 모두 허용
     *
     * @param target 전이하려는 상태
     * @return 전이 가능 여부
     */
    public boolean canTransitionTo(SurveyVisibility target) {
        return this != target;
    }
}
