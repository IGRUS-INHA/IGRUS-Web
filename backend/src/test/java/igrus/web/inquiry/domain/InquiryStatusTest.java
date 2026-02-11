package igrus.web.inquiry.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InquiryStatus FSM")
class InquiryStatusTest {

    @Nested
    @DisplayName("PENDING 상태 전이")
    class PendingTransitionTest {

        @Test
        @DisplayName("INQ-D-020: PENDING → IN_PROGRESS 전이 가능")
        void canTransitionTo_InProgress_ReturnsTrue() {
            assertThat(InquiryStatus.PENDING.canTransitionTo(InquiryStatus.IN_PROGRESS)).isTrue();
        }

        @Test
        @DisplayName("INQ-D-021: PENDING → COMPLETED 전이 가능")
        void canTransitionTo_Completed_ReturnsTrue() {
            assertThat(InquiryStatus.PENDING.canTransitionTo(InquiryStatus.COMPLETED)).isTrue();
        }

        @Test
        @DisplayName("INQ-D-022: PENDING → PENDING 동일 상태 허용 (멱등성)")
        void canTransitionTo_Pending_ReturnsTrue() {
            assertThat(InquiryStatus.PENDING.canTransitionTo(InquiryStatus.PENDING)).isTrue();
        }
    }

    @Nested
    @DisplayName("IN_PROGRESS 상태 전이")
    class InProgressTransitionTest {

        @Test
        @DisplayName("INQ-D-023: IN_PROGRESS → PENDING 전이 가능 (GAP-INQ-05)")
        void canTransitionTo_Pending_ReturnsTrue() {
            assertThat(InquiryStatus.IN_PROGRESS.canTransitionTo(InquiryStatus.PENDING)).isTrue();
        }

        @Test
        @DisplayName("INQ-D-024: IN_PROGRESS → COMPLETED 전이 가능 (GAP-INQ-05)")
        void canTransitionTo_Completed_ReturnsTrue() {
            assertThat(InquiryStatus.IN_PROGRESS.canTransitionTo(InquiryStatus.COMPLETED)).isTrue();
        }

        @Test
        @DisplayName("INQ-D-025: IN_PROGRESS → IN_PROGRESS 동일 상태 허용 (멱등성)")
        void canTransitionTo_InProgress_ReturnsTrue() {
            assertThat(InquiryStatus.IN_PROGRESS.canTransitionTo(InquiryStatus.IN_PROGRESS)).isTrue();
        }
    }

    @Nested
    @DisplayName("COMPLETED 종단 상태 (GAP-INQ-01)")
    class CompletedTransitionTest {

        @Test
        @DisplayName("INQ-D-026: COMPLETED → COMPLETED 동일 상태 허용 (멱등성)")
        void canTransitionTo_Completed_ReturnsTrue() {
            assertThat(InquiryStatus.COMPLETED.canTransitionTo(InquiryStatus.COMPLETED)).isTrue();
        }

        @Test
        @DisplayName("INQ-D-027: COMPLETED → PENDING 전이 불가 (INQ-INV-07)")
        void canTransitionTo_Pending_ReturnsFalse() {
            assertThat(InquiryStatus.COMPLETED.canTransitionTo(InquiryStatus.PENDING)).isFalse();
        }

        @Test
        @DisplayName("INQ-D-028: COMPLETED → IN_PROGRESS 전이 불가 (INQ-INV-07)")
        void canTransitionTo_InProgress_ReturnsFalse() {
            assertThat(InquiryStatus.COMPLETED.canTransitionTo(InquiryStatus.IN_PROGRESS)).isFalse();
        }
    }
}
