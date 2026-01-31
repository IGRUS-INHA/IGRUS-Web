package igrus.web.event.domain;

import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("EventRegistration 도메인")
class EventRegistrationTest {

    private static final String TITLE = "테스트 행사";
    private static final String DESCRIPTION = "행사 설명입니다.";
    private static final Instant START_AT = Instant.now();
    private static final Instant END_AT = Instant.now().plus(7, ChronoUnit.DAYS);
    private static final Integer CAPACITY = 30;

    @Nested
    @DisplayName("EventRegistration.create 정적 팩토리 메서드")
    class CreateRegistrationTest {

        @Test
        @DisplayName("선착순 행사 신청 시 REGISTERED 상태로 생성")
        void create_WithFirstComeEvent_ReturnsRegisteredStatus() {
            // given
            Event event = createEvent(EventRegistrationType.FIRST_COME);
            User user = createMockUser(2L, "신청자");

            // when
            EventRegistration registration = EventRegistration.create(event, user);

            // then
            assertThat(registration).isNotNull();
            assertThat(registration.getEvent()).isEqualTo(event);
            assertThat(registration.getUser()).isEqualTo(user);
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.REGISTERED);
            assertThat(registration.isRegistered()).isTrue();
            assertThat(registration.isActive()).isTrue();
        }

        @Test
        @DisplayName("선발제 행사 신청 시 WAITING 상태로 생성")
        void create_WithSelectionEvent_ReturnsWaitingStatus() {
            // given
            Event event = createEvent(EventRegistrationType.SELECTION);
            User user = createMockUser(2L, "신청자");

            // when
            EventRegistration registration = EventRegistration.create(event, user);

            // then
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.WAITING);
            assertThat(registration.isWaiting()).isTrue();
            assertThat(registration.isActive()).isFalse();
        }

        @Test
        @DisplayName("신청 시 registeredAt이 설정됨")
        void create_SetsRegisteredAt() {
            // given
            Event event = createEvent(EventRegistrationType.FIRST_COME);
            User user = createMockUser(2L, "신청자");
            Instant before = Instant.now();

            // when
            EventRegistration registration = EventRegistration.create(event, user);

            // then
            assertThat(registration.getRegisteredAt()).isNotNull();
            assertThat(registration.getRegisteredAt()).isAfterOrEqualTo(before);
        }
    }

    @Nested
    @DisplayName("상태 변경 메서드")
    class StatusChangeTest {

        @Test
        @DisplayName("WAITING 상태에서 approve 호출 시 APPROVED로 변경")
        void approve_FromWaiting_ChangesToApproved() {
            // given
            Event event = createEvent(EventRegistrationType.SELECTION);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            assertThat(registration.isWaiting()).isTrue();

            // when
            registration.approve();

            // then
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.APPROVED);
            assertThat(registration.isApproved()).isTrue();
            assertThat(registration.isActive()).isTrue();
        }

        @Test
        @DisplayName("WAITING 상태에서 reject 호출 시 REJECTED로 변경")
        void reject_FromWaiting_ChangesToRejected() {
            // given
            Event event = createEvent(EventRegistrationType.SELECTION);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);

            // when
            registration.reject();

            // then
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.REJECTED);
            assertThat(registration.isActive()).isFalse();
        }

        @Test
        @DisplayName("REGISTERED 상태에서 cancel 호출 시 CANCELED로 변경")
        void cancel_FromRegistered_ChangesToCanceled() {
            // given
            Event event = createEvent(EventRegistrationType.FIRST_COME);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            assertThat(registration.isRegistered()).isTrue();

            // when
            registration.cancel();

            // then
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.CANCELED);
            assertThat(registration.isCanceled()).isTrue();
            assertThat(registration.isActive()).isFalse();
        }

        @Test
        @DisplayName("APPROVED 상태에서 cancel 호출 시 CANCELED로 변경")
        void cancel_FromApproved_ChangesToCanceled() {
            // given
            Event event = createEvent(EventRegistrationType.SELECTION);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.approve();

            // when
            registration.cancel();

            // then
            assertThat(registration.isCanceled()).isTrue();
        }

        @Test
        @DisplayName("WAITING 상태에서 cancel 호출 시 CANCELED로 변경")
        void cancel_FromWaiting_ChangesToCanceled() {
            // given
            Event event = createEvent(EventRegistrationType.SELECTION);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);

            // when
            registration.cancel();

            // then
            assertThat(registration.isCanceled()).isTrue();
        }
    }

    @Nested
    @DisplayName("조회 메서드")
    class QueryMethodsTest {

        @Test
        @DisplayName("REGISTERED 상태는 isActive가 true")
        void isActive_WhenRegistered_ReturnsTrue() {
            // given
            Event event = createEvent(EventRegistrationType.FIRST_COME);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);

            // then
            assertThat(registration.isActive()).isTrue();
        }

        @Test
        @DisplayName("APPROVED 상태는 isActive가 true")
        void isActive_WhenApproved_ReturnsTrue() {
            // given
            Event event = createEvent(EventRegistrationType.SELECTION);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.approve();

            // then
            assertThat(registration.isActive()).isTrue();
        }

        @Test
        @DisplayName("WAITING 상태는 isActive가 false")
        void isActive_WhenWaiting_ReturnsFalse() {
            // given
            Event event = createEvent(EventRegistrationType.SELECTION);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);

            // then
            assertThat(registration.isActive()).isFalse();
        }

        @Test
        @DisplayName("REJECTED 상태는 isActive가 false")
        void isActive_WhenRejected_ReturnsFalse() {
            // given
            Event event = createEvent(EventRegistrationType.SELECTION);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.reject();

            // then
            assertThat(registration.isActive()).isFalse();
        }

        @Test
        @DisplayName("CANCELED 상태는 isActive가 false")
        void isActive_WhenCanceled_ReturnsFalse() {
            // given
            Event event = createEvent(EventRegistrationType.FIRST_COME);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.cancel();

            // then
            assertThat(registration.isActive()).isFalse();
        }
    }

    // === Helper Methods ===

    private User createMockUser(Long id, String name) {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(id);
        when(mockUser.getName()).thenReturn(name);
        return mockUser;
    }

    private Event createEvent(EventRegistrationType type) {
        User operator = createMockUser(1L, "운영자");
        return Event.create(operator, TITLE, DESCRIPTION, START_AT, END_AT, CAPACITY, type);
    }
}
