package igrus.web.event.domain;

import igrus.web.event.exception.EventNotEditableException;
import igrus.web.event.exception.InvalidEventCapacityException;
import igrus.web.event.exception.InvalidEventStateTransitionException;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Event 도메인")
class EventTest {

    private static final String TITLE = "테스트 행사";
    private static final String DESCRIPTION = "행사 설명입니다.";
    private static final String LOCATION = "동아리방";
    private static final Instant REGISTRATION_START_AT = Instant.now();
    private static final Instant REGISTRATION_END_AT = Instant.now().plus(7, ChronoUnit.DAYS);
    private static final Instant EVENT_START_AT = Instant.now().plus(14, ChronoUnit.DAYS);
    private static final Instant EVENT_END_AT = Instant.now().plus(15, ChronoUnit.DAYS);
    private static final Integer CAPACITY = 30;

    @Nested
    @DisplayName("Event.create 정적 팩토리 메서드")
    class CreateEventTest {

        @Test
        @DisplayName("유효한 정보로 선착순 행사 생성 성공")
        void create_WithValidInfoFirstCome_ReturnsEvent() {
            // given
            User mockUser = createMockUser();

            // when
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, EventRegistrationType.AUTO_APPROVE);

            // then
            assertThat(event).isNotNull();
            assertThat(event.getTitle()).isEqualTo(TITLE);
            assertThat(event.getDescription()).isEqualTo(DESCRIPTION);
            assertThat(event.getLocation()).isEqualTo(LOCATION);
            assertThat(event.getCapacity()).isEqualTo(CAPACITY);
            assertThat(event.getCurrentCount()).isEqualTo(0);
            assertThat(event.getStatus()).isEqualTo(EventStatus.UPCOMING);
            assertThat(event.getRegistrationType()).isEqualTo(EventRegistrationType.AUTO_APPROVE);
            assertThat(event.isAutoApprove()).isTrue();
            assertThat(event.isManualApprove()).isFalse();
        }

        @Test
        @DisplayName("유효한 정보로 선발제 행사 생성 성공")
        void create_WithValidInfoSelection_ReturnsEvent() {
            // given
            User mockUser = createMockUser();

            // when
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, EventRegistrationType.MANUAL_APPROVE);

            // then
            assertThat(event.getRegistrationType()).isEqualTo(EventRegistrationType.MANUAL_APPROVE);
            assertThat(event.isAutoApprove()).isFalse();
            assertThat(event.isManualApprove()).isTrue();
        }

        @Test
        @DisplayName("정원이 0일 때 예외 발생")
        void create_WithZeroCapacity_ThrowsException() {
            // given
            User mockUser = createMockUser();

            // when & then
            assertThatThrownBy(() -> Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    0, EventRegistrationType.AUTO_APPROVE))
                    .isInstanceOf(InvalidEventCapacityException.class);
        }

        @Test
        @DisplayName("정원이 음수일 때 예외 발생")
        void create_WithNegativeCapacity_ThrowsException() {
            // given
            User mockUser = createMockUser();

            // when & then
            assertThatThrownBy(() -> Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    -1, EventRegistrationType.AUTO_APPROVE))
                    .isInstanceOf(InvalidEventCapacityException.class);
        }

        @Test
        @DisplayName("정원이 null일 때 예외 발생")
        void create_WithNullCapacity_ThrowsException() {
            // given
            User mockUser = createMockUser();

            // when & then
            assertThatThrownBy(() -> Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    null, EventRegistrationType.AUTO_APPROVE))
                    .isInstanceOf(InvalidEventCapacityException.class);
        }
    }

    @Nested
    @DisplayName("상태 전이")
    class StatusTransitionTest {

        @Test
        @DisplayName("UPCOMING에서 OPEN으로 전이 성공")
        void open_FromUpcoming_Success() {
            // given
            Event event = createTestEvent();
            assertThat(event.getStatus()).isEqualTo(EventStatus.UPCOMING);

            // when
            event.open();

            // then
            assertThat(event.getStatus()).isEqualTo(EventStatus.OPEN);
        }

        @Test
        @DisplayName("UPCOMING에서 CANCELED로 전이 성공")
        void cancel_FromUpcoming_Success() {
            // given
            Event event = createTestEvent();

            // when
            event.cancel();

            // then
            assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELED);
        }

        @Test
        @DisplayName("OPEN에서 CLOSED로 전이 성공 (수동 마감)")
        void closeManually_FromOpen_Success() {
            // given
            Event event = createTestEvent();
            event.open();

            // when
            event.closeManually();

            // then
            assertThat(event.getStatus()).isEqualTo(EventStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.MANUAL_CLOSE);
        }

        @Test
        @DisplayName("OPEN에서 CLOSED로 전이 성공 (기한 만료)")
        void closeByDeadline_FromOpen_Success() {
            // given
            Event event = createTestEvent();
            event.open();

            // when
            event.closeByDeadline();

            // then
            assertThat(event.getStatus()).isEqualTo(EventStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.DEADLINE_PASSED);
        }

        @Test
        @DisplayName("CLOSED에서 OPEN으로 전이 성공 (재오픈)")
        void open_FromClosed_Success() {
            // given
            Event event = createTestEvent();
            event.open();
            event.closeManually();

            // when
            event.open();

            // then
            assertThat(event.getStatus()).isEqualTo(EventStatus.OPEN);
            assertThat(event.getCloseReason()).isNull();
        }

        @Test
        @DisplayName("COMPLETED에서 OPEN으로 전이 시 예외 발생")
        void open_FromCompleted_ThrowsException() {
            // given
            Event event = createTestEvent();
            event.open();
            event.complete();

            // when & then
            assertThatThrownBy(() -> event.open())
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }

        @Test
        @DisplayName("CANCELED에서 OPEN으로 전이 시 예외 발생")
        void open_FromCanceled_ThrowsException() {
            // given
            Event event = createTestEvent();
            event.cancel();

            // when & then
            assertThatThrownBy(() -> event.open())
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }

        @Test
        @DisplayName("UPCOMING에서 COMPLETED로 직접 전이 시 예외 발생")
        void complete_FromUpcoming_ThrowsException() {
            // given
            Event event = createTestEvent();

            // when & then
            assertThatThrownBy(() -> event.complete())
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }
    }

    @Nested
    @DisplayName("신청자 수 관리")
    class CurrentCountManagementTest {

        @Test
        @DisplayName("신청자 수 증가 성공")
        void incrementCurrentCount_Success() {
            // given
            Event event = createTestEvent();
            event.open();
            assertThat(event.getCurrentCount()).isEqualTo(0);

            // when
            event.incrementCurrentCount();

            // then
            assertThat(event.getCurrentCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("정원이 차면 자동으로 CLOSED 상태로 변경")
        void incrementCurrentCount_WhenFull_AutoCloses() {
            // given
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    2, EventRegistrationType.AUTO_APPROVE);
            event.open();

            // when
            event.incrementCurrentCount();
            event.incrementCurrentCount();

            // then
            assertThat(event.getCurrentCount()).isEqualTo(2);
            assertThat(event.isFull()).isTrue();
            assertThat(event.getStatus()).isEqualTo(EventStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.CAPACITY_FULL);
        }

        @Test
        @DisplayName("신청자 수 감소 성공")
        void decrementCurrentCount_Success() {
            // given
            Event event = createTestEvent();
            event.open();
            event.incrementCurrentCount();
            event.incrementCurrentCount();

            // when
            event.decrementCurrentCount();

            // then
            assertThat(event.getCurrentCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("정원 마감 후 취소 시 자동으로 OPEN 상태로 변경")
        void decrementCurrentCount_WhenCapacityFullClosed_ReopensAutomatically() {
            // given
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    2, EventRegistrationType.AUTO_APPROVE);
            event.open();
            event.incrementCurrentCount();
            event.incrementCurrentCount();
            assertThat(event.getStatus()).isEqualTo(EventStatus.CLOSED);

            // when
            event.decrementCurrentCount();

            // then
            assertThat(event.getCurrentCount()).isEqualTo(1);
            assertThat(event.getStatus()).isEqualTo(EventStatus.OPEN);
            assertThat(event.getCloseReason()).isNull();
        }

        @Test
        @DisplayName("신청자 수가 0일 때 감소해도 음수가 되지 않음")
        void decrementCurrentCount_WhenZero_StaysZero() {
            // given
            Event event = createTestEvent();
            event.open();
            assertThat(event.getCurrentCount()).isEqualTo(0);

            // when
            event.decrementCurrentCount();

            // then
            assertThat(event.getCurrentCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("조회 메서드")
    class QueryMethodsTest {

        @Test
        @DisplayName("OPEN 상태이고 정원 여유가 있으면 isRegistrable은 true")
        void isRegistrable_WhenOpenAndNotFull_ReturnsTrue() {
            // given
            Event event = createTestEvent();
            event.open();

            // then
            assertThat(event.isRegistrable()).isTrue();
        }

        @Test
        @DisplayName("OPEN 상태이지만 정원이 차면 isRegistrable은 false")
        void isRegistrable_WhenOpenButFull_ReturnsFalse() {
            // given
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    1, EventRegistrationType.AUTO_APPROVE);
            event.open();
            event.incrementCurrentCount();

            // then
            assertThat(event.isRegistrable()).isFalse();
        }

        @Test
        @DisplayName("UPCOMING 상태에서는 isRegistrable은 false")
        void isRegistrable_WhenUpcoming_ReturnsFalse() {
            // given
            Event event = createTestEvent();

            // then
            assertThat(event.isRegistrable()).isFalse();
        }

        @Test
        @DisplayName("남은 자리 수 계산")
        void getRemainingCapacity_ReturnsCorrectValue() {
            // given
            Event event = createTestEvent();
            event.open();
            event.incrementCurrentCount();
            event.incrementCurrentCount();

            // then
            assertThat(event.getRemainingCapacity()).isEqualTo(28);
        }

        @Test
        @DisplayName("정원 초과 시 남은 자리 수는 0")
        void getRemainingCapacity_WhenOverCapacity_ReturnsZero() {
            // given
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    1, EventRegistrationType.AUTO_APPROVE);
            event.open();
            event.incrementCurrentCount();

            // then
            assertThat(event.getRemainingCapacity()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("수정 메서드")
    class UpdateMethodTest {

        @Test
        @DisplayName("UPCOMING 상태에서 수정 성공")
        void update_WhenUpcoming_Success() {
            // given
            Event event = createTestEvent();
            String newTitle = "수정된 제목";
            String newDescription = "수정된 설명";
            Integer newCapacity = 50;

            // when
            event.update(newTitle, newDescription, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, newCapacity);

            // then
            assertThat(event.getTitle()).isEqualTo(newTitle);
            assertThat(event.getDescription()).isEqualTo(newDescription);
            assertThat(event.getCapacity()).isEqualTo(newCapacity);
        }

        @Test
        @DisplayName("OPEN 상태에서 수정 성공")
        void update_WhenOpen_Success() {
            // given
            Event event = createTestEvent();
            event.open();

            // when
            event.update("새 제목", "새 설명", LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 40);

            // then
            assertThat(event.getTitle()).isEqualTo("새 제목");
        }

        @Test
        @DisplayName("COMPLETED 상태에서 수정 시 예외 발생")
        void update_WhenCompleted_ThrowsException() {
            // given
            Event event = createTestEvent();
            event.open();
            event.complete();

            // when & then
            assertThatThrownBy(() -> event.update("새 제목", "새 설명", LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 40))
                    .isInstanceOf(EventNotEditableException.class);
        }

        @Test
        @DisplayName("CANCELED 상태에서 수정 시 예외 발생")
        void update_WhenCanceled_ThrowsException() {
            // given
            Event event = createTestEvent();
            event.cancel();

            // when & then
            assertThatThrownBy(() -> event.update("새 제목", "새 설명", LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 40))
                    .isInstanceOf(EventNotEditableException.class);
        }

        @Test
        @DisplayName("정원을 0으로 수정 시 예외 발생")
        void update_WithZeroCapacity_ThrowsException() {
            // given
            Event event = createTestEvent();

            // when & then
            assertThatThrownBy(() -> event.update("새 제목", "새 설명", LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 0))
                    .isInstanceOf(InvalidEventCapacityException.class);
        }
    }

    // === Helper Methods ===

    private User createMockUser() {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getName()).thenReturn("테스트 운영자");
        return mockUser;
    }

    private Event createTestEvent() {
        return Event.create(createMockUser(), TITLE, DESCRIPTION, LOCATION,
                EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                CAPACITY, EventRegistrationType.AUTO_APPROVE);
    }
}
