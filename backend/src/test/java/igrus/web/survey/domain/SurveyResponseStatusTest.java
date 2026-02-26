package igrus.web.survey.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SurveyResponseStatus 상태 전이 테스트")
class SurveyResponseStatusTest {

    @DisplayName("NOT_STARTED에서 OPEN으로 전이 가능")
    @Test
    void canTransitionTo_FromNotStarted_ToOpen_ReturnsTrue() {
        assertThat(SurveyResponseStatus.NOT_STARTED.canTransitionTo(SurveyResponseStatus.OPEN)).isTrue();
    }

    @DisplayName("NOT_STARTED에서 CLOSED로 전이 불가")
    @Test
    void canTransitionTo_FromNotStarted_ToClosed_ReturnsFalse() {
        assertThat(SurveyResponseStatus.NOT_STARTED.canTransitionTo(SurveyResponseStatus.CLOSED)).isFalse();
    }

    @DisplayName("OPEN에서 CLOSED로 전이 가능")
    @Test
    void canTransitionTo_FromOpen_ToClosed_ReturnsTrue() {
        assertThat(SurveyResponseStatus.OPEN.canTransitionTo(SurveyResponseStatus.CLOSED)).isTrue();
    }

    @DisplayName("OPEN에서 NOT_STARTED로 전이 불가")
    @Test
    void canTransitionTo_FromOpen_ToNotStarted_ReturnsFalse() {
        assertThat(SurveyResponseStatus.OPEN.canTransitionTo(SurveyResponseStatus.NOT_STARTED)).isFalse();
    }

    @DisplayName("CLOSED에서 OPEN으로 전이 가능 (재개)")
    @Test
    void canTransitionTo_FromClosed_ToOpen_ReturnsTrue() {
        assertThat(SurveyResponseStatus.CLOSED.canTransitionTo(SurveyResponseStatus.OPEN)).isTrue();
    }

    @DisplayName("CLOSED에서 NOT_STARTED로 전이 불가")
    @Test
    void canTransitionTo_FromClosed_ToNotStarted_ReturnsFalse() {
        assertThat(SurveyResponseStatus.CLOSED.canTransitionTo(SurveyResponseStatus.NOT_STARTED)).isFalse();
    }

    @DisplayName("같은 상태로 전이 불가 - NOT_STARTED")
    @Test
    void canTransitionTo_SameState_NotStarted_ReturnsFalse() {
        assertThat(SurveyResponseStatus.NOT_STARTED.canTransitionTo(SurveyResponseStatus.NOT_STARTED)).isFalse();
    }

    @DisplayName("같은 상태로 전이 불가 - OPEN")
    @Test
    void canTransitionTo_SameState_Open_ReturnsFalse() {
        assertThat(SurveyResponseStatus.OPEN.canTransitionTo(SurveyResponseStatus.OPEN)).isFalse();
    }

    @DisplayName("같은 상태로 전이 불가 - CLOSED")
    @Test
    void canTransitionTo_SameState_Closed_ReturnsFalse() {
        assertThat(SurveyResponseStatus.CLOSED.canTransitionTo(SurveyResponseStatus.CLOSED)).isFalse();
    }

    @DisplayName("OPEN만 응답 수집 중")
    @Test
    void isAcceptingResponses_Open_ReturnsTrue() {
        assertThat(SurveyResponseStatus.OPEN.isAcceptingResponses()).isTrue();
    }

    @DisplayName("NOT_STARTED는 응답 수집 아님")
    @Test
    void isAcceptingResponses_NotStarted_ReturnsFalse() {
        assertThat(SurveyResponseStatus.NOT_STARTED.isAcceptingResponses()).isFalse();
    }

    @DisplayName("CLOSED는 응답 수집 아님")
    @Test
    void isAcceptingResponses_Closed_ReturnsFalse() {
        assertThat(SurveyResponseStatus.CLOSED.isAcceptingResponses()).isFalse();
    }
}
