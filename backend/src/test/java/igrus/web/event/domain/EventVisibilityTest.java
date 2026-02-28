package igrus.web.event.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EventVisibility 상태 전이 테스트.
 * SurveyVisibilityTest 패턴 참조.
 * 관련 검증 기준: EVT-INV-17 (Visibility 양방향 전이)
 * 관련 테스트 케이스: GAP-EVT-26
 *
 * @see igrus.web.event.domain.EventVisibility
 */
@DisplayName("EventVisibility 상태 전이 테스트")
class EventVisibilityTest {

    @DisplayName("[GAP-EVT-26] UNPUBLISHED에서 PUBLISHED로 전이 가능")
    @Test
    void canTransitionTo_FromUnpublished_ToPublished_ReturnsTrue() {
        assertThat(EventVisibility.UNPUBLISHED.canTransitionTo(EventVisibility.PUBLISHED)).isTrue();
    }

    @DisplayName("[GAP-EVT-26] PUBLISHED에서 UNPUBLISHED로 전이 가능")
    @Test
    void canTransitionTo_FromPublished_ToUnpublished_ReturnsTrue() {
        assertThat(EventVisibility.PUBLISHED.canTransitionTo(EventVisibility.UNPUBLISHED)).isTrue();
    }

    @DisplayName("[GAP-EVT-26] 같은 상태로 전이 불가 - UNPUBLISHED")
    @Test
    void canTransitionTo_SameState_Unpublished_ReturnsFalse() {
        assertThat(EventVisibility.UNPUBLISHED.canTransitionTo(EventVisibility.UNPUBLISHED)).isFalse();
    }

    @DisplayName("[GAP-EVT-26] 같은 상태로 전이 불가 - PUBLISHED")
    @Test
    void canTransitionTo_SameState_Published_ReturnsFalse() {
        assertThat(EventVisibility.PUBLISHED.canTransitionTo(EventVisibility.PUBLISHED)).isFalse();
    }

    @DisplayName("UNPUBLISHED의 displayName과 description 값 검증")
    @Test
    void unpublished_HasCorrectDisplayNameAndDescription() {
        assertThat(EventVisibility.UNPUBLISHED.getDisplayName()).isEqualTo("비공개");
        assertThat(EventVisibility.UNPUBLISHED.getDescription()).isEqualTo("일반 사용자에게 노출되지 않음");
    }

    @DisplayName("PUBLISHED의 displayName과 description 값 검증")
    @Test
    void published_HasCorrectDisplayNameAndDescription() {
        assertThat(EventVisibility.PUBLISHED.getDisplayName()).isEqualTo("공개");
        assertThat(EventVisibility.PUBLISHED.getDescription()).isEqualTo("일반 사용자에게 노출");
    }
}
