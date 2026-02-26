package igrus.web.survey.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SurveyVisibility 상태 전이 테스트")
class SurveyVisibilityTest {

    @DisplayName("UNPUBLISHED에서 PUBLISHED로 전이 가능")
    @Test
    void canTransitionTo_FromUnpublished_ToPublished_ReturnsTrue() {
        assertThat(SurveyVisibility.UNPUBLISHED.canTransitionTo(SurveyVisibility.PUBLISHED)).isTrue();
    }

    @DisplayName("PUBLISHED에서 UNPUBLISHED로 전이 가능")
    @Test
    void canTransitionTo_FromPublished_ToUnpublished_ReturnsTrue() {
        assertThat(SurveyVisibility.PUBLISHED.canTransitionTo(SurveyVisibility.UNPUBLISHED)).isTrue();
    }

    @DisplayName("같은 상태로 전이 불가 - UNPUBLISHED")
    @Test
    void canTransitionTo_SameState_Unpublished_ReturnsFalse() {
        assertThat(SurveyVisibility.UNPUBLISHED.canTransitionTo(SurveyVisibility.UNPUBLISHED)).isFalse();
    }

    @DisplayName("같은 상태로 전이 불가 - PUBLISHED")
    @Test
    void canTransitionTo_SameState_Published_ReturnsFalse() {
        assertThat(SurveyVisibility.PUBLISHED.canTransitionTo(SurveyVisibility.PUBLISHED)).isFalse();
    }
}
