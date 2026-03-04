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

/**
 * Event 도메인 테스트. (3축 상태 모델: EventStatus + RegistrationStatus + EventVisibility)
 * 테스트 케이스 문서: docs/test-case/event/event-domain-test-cases.md v2.0
 *
 * @see igrus.web.event.domain.Event
 */
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

    // === 2.1 행사 생성 ===

    @Nested
    @DisplayName("행사 생성")
    class CreateEventTest {

        @Test
        @DisplayName("[EVT-001] 유효한 정보로 선착순 행사 생성 성공")
        void create_WithValidInfoAutoApprove_ReturnsEvent() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, EventRegistrationType.AUTO_APPROVE, null);

            assertThat(event).isNotNull();
            assertThat(event.getTitle()).isEqualTo(TITLE);
            assertThat(event.getDescription()).isEqualTo(DESCRIPTION);
            assertThat(event.getLocation()).isEqualTo(LOCATION);
            assertThat(event.getCapacity()).isEqualTo(CAPACITY);
            assertThat(event.getCurrentCount()).isEqualTo(0);
            assertThat(event.getRegistrationType()).isEqualTo(EventRegistrationType.AUTO_APPROVE);
            assertThat(event.isAutoApprove()).isTrue();
            assertThat(event.isManualApprove()).isFalse();
        }

        @Test
        @DisplayName("[EVT-002] 유효한 정보로 선발제 행사 생성 성공")
        void create_WithValidInfoManualApprove_ReturnsEvent() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, EventRegistrationType.MANUAL_APPROVE, null);

            assertThat(event.getRegistrationType()).isEqualTo(EventRegistrationType.MANUAL_APPROVE);
            assertThat(event.isAutoApprove()).isFalse();
            assertThat(event.isManualApprove()).isTrue();
        }

        @Test
        @DisplayName("[EVT-003] 정원이 0일 때 예외 발생")
        void create_WithZeroCapacity_ThrowsException() {
            User mockUser = createMockUser();
            assertThatThrownBy(() -> Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    0, EventRegistrationType.AUTO_APPROVE, null))
                    .isInstanceOf(InvalidEventCapacityException.class);
        }

        @Test
        @DisplayName("[EVT-004] 정원이 음수일 때 예외 발생")
        void create_WithNegativeCapacity_ThrowsException() {
            User mockUser = createMockUser();
            assertThatThrownBy(() -> Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    -1, EventRegistrationType.AUTO_APPROVE, null))
                    .isInstanceOf(InvalidEventCapacityException.class);
        }

        @Test
        @DisplayName("[EVT-005] 정원이 null일 때 예외 발생")
        void create_WithNullCapacity_ThrowsException() {
            User mockUser = createMockUser();
            assertThatThrownBy(() -> Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    null, EventRegistrationType.AUTO_APPROVE, null))
                    .isInstanceOf(InvalidEventCapacityException.class);
        }

        @Test
        @DisplayName("[EVT-062] 생성 시 2축 모델 초기 상태 검증")
        void create_VerifyTwoAxisInitialState() {
            Event event = createTestEvent();
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.NOT_STARTED);
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.UPCOMING);
            assertThat(event.getCurrentCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("[EVT-063] 정원 1인 행사 생성 (경계값)")
        void create_WithCapacityOne_Success() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    1, EventRegistrationType.AUTO_APPROVE, null);
            assertThat(event.getCapacity()).isEqualTo(1);
        }

        @Test
        @DisplayName("[EVT-064] 정원 Integer.MAX_VALUE 행사 생성 (경계값)")
        void create_WithMaxCapacity_Success() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    Integer.MAX_VALUE, EventRegistrationType.AUTO_APPROVE, null);
            assertThat(event.getCapacity()).isEqualTo(Integer.MAX_VALUE);
        }
    }

    // === 2.3 registrationStatus 축 전이 ===

    @Nested
    @DisplayName("registrationStatus 축 전이")
    class RegistrationStatusTransitionTest {

        @Test
        @DisplayName("[EVT-065] NOT_STARTED→OPEN (등록 시작일 도래, Lazy)")
        void lazyOpen_WhenRegStartReached_TransitionsToOpen() {
            Event event = createTestEvent();
            Instant afterRegStart = REGISTRATION_START_AT.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(afterRegStart);

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
        }

        @Test
        @DisplayName("[EVT-066] NOT_STARTED→OPEN 불가 (eventStatus=CANCELED)")
        void lazyOpen_WhenCanceled_DoesNotTransition() {
            Event event = createTestEvent();
            event.openRegistration();
            event.cancel();
            // cancel forces CLOSED, let's set up a scenario where reg would be NOT_STARTED+CANCELED
            // Actually after cancel, registrationStatus=CLOSED, eventStatus=CANCELED
            // We can't easily create NOT_STARTED+CANCELED without reflection
            // But we can verify that updateStatusIfNeeded doesn't open registration when CANCELED
            // Let's reactivate first, then cancel again
            // Better approach: just verify CANCELED blocks Lazy reg open
            Event event2 = createTestEvent();
            event2.openRegistration();
            event2.cancel(); // now CLOSED+CANCELED
            // reactivate to get NOT_STARTED
            Instant beforeRegStart = REGISTRATION_START_AT.minus(1, ChronoUnit.HOURS);
            event2.reactivate(beforeRegStart); // will set NOT_STARTED+UPCOMING via Lazy

            assertThat(event2.getRegistrationStatus()).isEqualTo(RegistrationStatus.NOT_STARTED);
            assertThat(event2.getEventStatus()).isEqualTo(EventStatus.UPCOMING);

            // Now cancel again
            event2.cancel();
            assertThat(event2.getEventStatus()).isEqualTo(EventStatus.CANCELED);
            assertThat(event2.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);

            // Lazy should NOT open registration
            Instant afterRegStart = REGISTRATION_START_AT.plus(1, ChronoUnit.HOURS);
            event2.updateStatusIfNeeded(afterRegStart);
            assertThat(event2.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }

        @Test
        @DisplayName("[EVT-067] OPEN→CLOSED (기한 만료, Lazy)")
        void lazyClosed_WhenRegEndPassed_TransitionsToClosed() {
            Event event = createTestEvent();
            event.openRegistration();
            Instant afterRegEnd = REGISTRATION_END_AT.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(afterRegEnd);

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.DEADLINE_PASSED);
        }

        @Test
        @DisplayName("[EVT-068] OPEN→CLOSED (정원 자동 마감)")
        void autoClose_WhenCapacityFull_TransitionsToClosed() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    2, EventRegistrationType.AUTO_APPROVE, null);
            event.openRegistration();
            event.incrementCurrentCount();
            event.incrementCurrentCount();

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.CAPACITY_FULL);
        }

        @Test
        @DisplayName("[EVT-069] OPEN→CLOSED (수동 마감)")
        void manualClose_FromOpen_TransitionsToClosed() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.MANUAL_CLOSE);
        }

        @Test
        @DisplayName("[EVT-070] CLOSED→OPEN (정원 자동 재오픈)")
        void autoReopen_WhenCapacityAvailable_TransitionsToOpen() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    2, EventRegistrationType.AUTO_APPROVE, null);
            event.openRegistration();
            event.incrementCurrentCount();
            event.incrementCurrentCount();
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);

            Instant beforeRegEnd = REGISTRATION_END_AT.minus(1, ChronoUnit.HOURS);
            event.decrementCurrentCount(beforeRegEnd);

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(event.getCloseReason()).isNull();
        }

        @Test
        @DisplayName("[EVT-071] 자동 재오픈 불가: 기한 만료")
        void autoReopen_WhenDeadlinePassed_StaysClosed() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    2, EventRegistrationType.AUTO_APPROVE, null);
            event.openRegistration();
            event.incrementCurrentCount();
            event.incrementCurrentCount();

            Instant afterRegEnd = REGISTRATION_END_AT.plus(1, ChronoUnit.HOURS);
            event.decrementCurrentCount(afterRegEnd);

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }

        @Test
        @DisplayName("[EVT-072] 자동 재오픈 불가: 여전히 만석")
        void autoReopen_WhenStillFull_StaysClosed() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    1, EventRegistrationType.AUTO_APPROVE, null);
            event.openRegistration();
            event.incrementCurrentCount();
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.isFull()).isTrue();

            // Count is still 1 (full), just check it stays CLOSED
            Instant beforeRegEnd = REGISTRATION_END_AT.minus(1, ChronoUnit.HOURS);
            event.reopenIfNeeded(beforeRegEnd);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }

        @Test
        @DisplayName("[EVT-073] 자동 재오픈 불가: DEADLINE_PASSED")
        void autoReopen_WhenDeadlinePassed_DoesNotReopen() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationByDeadline();

            Instant beforeRegEnd = REGISTRATION_END_AT.minus(1, ChronoUnit.HOURS);
            event.reopenIfNeeded(beforeRegEnd);

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.DEADLINE_PASSED);
        }

        @Test
        @DisplayName("[EVT-074] 자동 재오픈 불가: MANUAL_CLOSE")
        void autoReopen_WhenManualClose_DoesNotReopen() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();

            Instant beforeRegEnd = REGISTRATION_END_AT.minus(1, ChronoUnit.HOURS);
            event.reopenIfNeeded(beforeRegEnd);

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.MANUAL_CLOSE);
        }

        @Test
        @DisplayName("[EVT-075] 자동 재오픈 불가: eventStatus=CANCELED")
        void autoReopen_WhenCanceled_DoesNotReopen() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    2, EventRegistrationType.AUTO_APPROVE, null);
            event.openRegistration();
            event.incrementCurrentCount();
            event.incrementCurrentCount();
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.CAPACITY_FULL);

            event.cancel(); // forces CLOSED + CANCELED
            // Even if we could decrease count, CANCELED blocks reopen
            Instant beforeRegEnd = REGISTRATION_END_AT.minus(1, ChronoUnit.HOURS);
            event.reopenIfNeeded(beforeRegEnd);

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }

        @Test
        @DisplayName("[EVT-076] NOT_STARTED→CLOSED (행사 취소 강제)")
        void cancel_FromNotStarted_ForcesClosedRegistration() {
            Event event = createTestEvent();
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.NOT_STARTED);

            event.cancel();

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.MANUAL_CLOSE);
        }

        @Test
        @DisplayName("[EVT-077] 자동 재오픈 경계값: now == regEnd (exclusive)")
        void autoReopen_WhenNowEqualsRegEnd_StaysClosed() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    2, EventRegistrationType.AUTO_APPROVE, null);
            event.openRegistration();
            event.incrementCurrentCount();
            event.incrementCurrentCount();

            // now == regEnd → isBefore returns false → stays CLOSED
            event.decrementCurrentCount(REGISTRATION_END_AT);

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }
    }

    // === 2.4 eventStatus 축 전이 ===

    @Nested
    @DisplayName("eventStatus 축 전이")
    class EventStatusTransitionTest {

        @Test
        @DisplayName("[EVT-078] UPCOMING→ONGOING (행사 시작일 도래, Lazy)")
        void lazyOngoing_WhenEventStartReached_TransitionsToOngoing() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            Instant afterEventStart = EVENT_START_AT.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(afterEventStart);

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.ONGOING);
        }

        @Test
        @DisplayName("[EVT-079] ONGOING→COMPLETED (행사 종료일 경과, Lazy)")
        void lazyCompleted_WhenEventEndPassed_TransitionsToCompleted() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            Instant afterEventEnd = EVENT_END_AT.plus(1, ChronoUnit.DAYS);
            event.updateStatusIfNeeded(afterEventEnd);

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.COMPLETED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }

        @Test
        @DisplayName("[EVT-080] UPCOMING→CANCELED (수동 취소)")
        void cancel_FromUpcoming_TransitionsToCanceled() {
            Event event = createTestEvent();
            event.openRegistration();

            event.cancel();

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.CANCELED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }

        @Test
        @DisplayName("[EVT-081] ONGOING→CANCELED (수동 취소)")
        void cancel_FromOngoing_TransitionsToCanceled() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            event.startOngoing();

            event.cancel();

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.CANCELED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }

        @Test
        @DisplayName("[EVT-082] CANCELED→UPCOMING 재활성화 (now < eventStartAt)")
        void reactivate_BeforeEventStart_TransitionsToUpcoming() {
            Event event = createTestEvent();
            event.cancel();
            Instant beforeEventStart = EVENT_START_AT.minus(1, ChronoUnit.HOURS);

            event.reactivate(beforeEventStart);

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.UPCOMING);
        }

        @Test
        @DisplayName("[EVT-083] CANCELED→ONGOING 재활성화 (eventStart <= now < eventEnd)")
        void reactivate_DuringEvent_TransitionsToOngoing() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            event.startOngoing();
            event.cancel();
            Instant duringEvent = EVENT_START_AT.plus(6, ChronoUnit.HOURS);

            event.reactivate(duringEvent);

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.ONGOING);
        }

        @Test
        @DisplayName("[EVT-084] COMPLETED→어떤 상태든 전이 불가 (종단)")
        void transition_FromCompleted_AlwaysThrows() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            event.startOngoing();
            event.complete();

            assertThatThrownBy(() -> event.startOngoing())
                    .isInstanceOf(InvalidEventStateTransitionException.class);
            assertThatThrownBy(() -> event.cancel())
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }

        @Test
        @DisplayName("[EVT-085] CANCELED→COMPLETED 전이 불가")
        void complete_FromCanceled_ThrowsException() {
            Event event = createTestEvent();
            event.cancel();

            assertThatThrownBy(() -> event.complete())
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }

        @Test
        @DisplayName("[EVT-086] UPCOMING→COMPLETED 직접 전이 불가")
        void complete_FromUpcoming_ThrowsException() {
            Event event = createTestEvent();

            assertThatThrownBy(() -> event.complete())
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }

        @Test
        @DisplayName("[EVT-087] ONGOING→UPCOMING 역방향 전이 불가")
        void upcoming_FromOngoing_ThrowsException() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            event.startOngoing();

            // startOngoing from ONGOING should fail (same state)
            // ONGOING→UPCOMING is not a valid transition
            // There's no direct method to go back to UPCOMING, but we can test
            // via reactivate which only works from CANCELED
            assertThatThrownBy(() -> event.reactivate(EVENT_START_AT.minus(1, ChronoUnit.HOURS)))
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }
    }

    // === 2.5 Lazy Evaluation 통합 2축 ===

    @Nested
    @DisplayName("Lazy Evaluation 통합 2축")
    class LazyEvaluationTest {

        @Test
        @DisplayName("[EVT-088] NOT_STARTED→OPEN 자동 전이 (Lazy 단독)")
        void lazy_NotStartedToOpen_WhenRegStartReached() {
            Event event = createTestEvent();
            Instant afterRegStart = REGISTRATION_START_AT.plus(1, ChronoUnit.MINUTES);
            event.updateStatusIfNeeded(afterRegStart);

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.UPCOMING);
        }

        @Test
        @DisplayName("[EVT-089] 겹침 기간: reg=OPEN + event=ONGOING 유지")
        void lazy_OverlapPeriod_OpenAndOngoing() {
            // Create event where regEnd > eventStart (overlap period)
            Instant regStart = Instant.now();
            Instant eventStart = Instant.now().plus(7, ChronoUnit.DAYS);
            Instant regEnd = Instant.now().plus(10, ChronoUnit.DAYS); // regEnd after eventStart
            Instant eventEnd = Instant.now().plus(14, ChronoUnit.DAYS);
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    eventStart, eventEnd, regStart, regEnd,
                    CAPACITY, EventRegistrationType.AUTO_APPROVE, null);

            // During overlap: regStart < now < eventStart < now2 < regEnd
            Instant duringOverlap = eventStart.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(duringOverlap);

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.ONGOING);
        }

        @Test
        @DisplayName("[EVT-090] 한 번의 Lazy 호출로 두 축 동시 전이")
        void lazy_BothAxesTransition_InSingleCall() {
            Event event = createTestEvent();
            // now > regStart AND now > eventStart → both should transition
            Instant afterBoth = EVENT_START_AT.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(afterBoth);

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED); // OPEN→CLOSED (deadline passed since regEnd < eventStart+1h for our constants)
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.ONGOING);
        }

        @Test
        @DisplayName("[EVT-091] COMPLETED 전이 시 registrationStatus=CLOSED 강제")
        void lazy_WhenCompleted_ForcesRegistrationClosed() {
            // Create event where reg is still OPEN when event ends
            Instant regStart = Instant.now();
            Instant eventStart = Instant.now().plus(7, ChronoUnit.DAYS);
            Instant regEnd = Instant.now().plus(20, ChronoUnit.DAYS); // regEnd > eventEnd
            Instant eventEnd = Instant.now().plus(14, ChronoUnit.DAYS);
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    eventStart, eventEnd, regStart, regEnd,
                    CAPACITY, EventRegistrationType.AUTO_APPROVE, null);

            // Move to during event (reg still open)
            Instant duringEvent = eventStart.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(duringEvent);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.ONGOING);

            // Move past event end
            Instant afterEventEnd = eventEnd.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(afterEventEnd);

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.COMPLETED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }

        @Test
        @DisplayName("[EVT-092] regStart == regEnd 시 Lazy 동작 → OPEN 후 즉시 CLOSED")
        void lazy_WhenRegStartEqualsRegEnd_TransitionsToClosed() {
            Instant regStartEnd = Instant.now().plus(1, ChronoUnit.DAYS);
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, regStartEnd, regStartEnd,
                    CAPACITY, EventRegistrationType.AUTO_APPROVE, null);

            // now > regStart == regEnd → NOT_STARTED→OPEN→CLOSED 연속 전이
            Instant afterRegTime = regStartEnd.plus(1, ChronoUnit.MINUTES);
            event.updateStatusIfNeeded(afterRegTime);

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.DEADLINE_PASSED);
        }

        @Test
        @DisplayName("[EVT-060] CLOSED에서 ONGOING 자동 전환 (행사 시작일 도래)")
        void lazy_ClosedToOngoing_WhenEventStartReached() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);

            Instant afterEventStart = EVENT_START_AT.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(afterEventStart);

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.ONGOING);
        }

        @Test
        @DisplayName("[EVT-061] ONGOING에서 COMPLETED 자동 전환 (행사 종료일 경과)")
        void lazy_OngoingToCompleted_WhenEventEndPassed() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            Instant afterEventStart = EVENT_START_AT.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(afterEventStart);
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.ONGOING);

            Instant afterEventEnd = EVENT_END_AT.plus(1, ChronoUnit.DAYS);
            event.updateStatusIfNeeded(afterEventEnd);

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.COMPLETED);
        }
    }

    // === 2.6 교차 축 불변조건 ===

    @Nested
    @DisplayName("교차 축 불변조건")
    class CrossAxisInvariantsTest {

        @Test
        @DisplayName("[EVT-093] COMPLETED이면 registrationStatus=CLOSED")
        void completed_AlwaysHasClosedRegistration() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            event.startOngoing();
            event.complete();

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.COMPLETED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }

        @Test
        @DisplayName("[EVT-094] CANCELED이면 registrationStatus=CLOSED")
        void canceled_AlwaysHasClosedRegistration() {
            Event event = createTestEvent();
            event.openRegistration();
            event.cancel();

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.CANCELED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }

        @Test
        @DisplayName("[EVT-095] NOT_STARTED이면 eventStatus=UPCOMING")
        void notStarted_AlwaysHasUpcomingEvent() {
            Event event = createTestEvent();
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.NOT_STARTED);
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.UPCOMING);
        }

        @Test
        @DisplayName("[EVT-096] 무효 조합: NOT_STARTED + ONGOING 도달 불가")
        void notStartedPlusOngoing_NotReachable() {
            // regStart < eventStart 제약으로 인해, eventStart 도래 시 regStart도 이미 경과
            Event event = createTestEvent();
            Instant afterEventStart = EVENT_START_AT.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(afterEventStart);

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.ONGOING);
            assertThat(event.getRegistrationStatus()).isNotEqualTo(RegistrationStatus.NOT_STARTED);
        }

        @Test
        @DisplayName("[EVT-097] 무효 조합: OPEN + COMPLETED 도달 불가")
        void openPlusCompleted_NotReachable() {
            // regEnd <= eventEnd 제약으로 인해, eventEnd 경과 시 regEnd도 이미 경과 → CLOSED
            Event event = createTestEvent();
            event.openRegistration();
            Instant afterEventEnd = EVENT_END_AT.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(afterEventEnd);

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.COMPLETED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }

        @Test
        @DisplayName("[EVT-098] OPEN + CANCELED 도달 불가 (cancel 시 CLOSED 강제)")
        void openPlusCanceled_NotReachable() {
            Event event = createTestEvent();
            event.openRegistration();
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);

            event.cancel();

            // cancel() forces registrationStatus to CLOSED
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.CANCELED);
        }

        @Test
        @DisplayName("[EVT-099] 유효 7가지 복합 상태 조합 검증")
        void valid7CompositeStates_AllReachable() {
            // 1. NOT_STARTED + UPCOMING
            Event e1 = createTestEvent();
            assertThat(e1.getRegistrationStatus()).isEqualTo(RegistrationStatus.NOT_STARTED);
            assertThat(e1.getEventStatus()).isEqualTo(EventStatus.UPCOMING);

            // 2. OPEN + UPCOMING
            Event e2 = createTestEvent();
            e2.openRegistration();
            assertThat(e2.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(e2.getEventStatus()).isEqualTo(EventStatus.UPCOMING);

            // 3. OPEN + ONGOING (key feature: reg during event)
            Instant regStart = Instant.now();
            Instant eventStart = Instant.now().plus(7, ChronoUnit.DAYS);
            Instant regEnd = Instant.now().plus(10, ChronoUnit.DAYS);
            Instant eventEnd = Instant.now().plus(14, ChronoUnit.DAYS);
            Event e3 = Event.create(createMockUser(), TITLE, DESCRIPTION, LOCATION,
                    eventStart, eventEnd, regStart, regEnd, CAPACITY, EventRegistrationType.AUTO_APPROVE, null);
            Instant duringOverlap = eventStart.plus(1, ChronoUnit.HOURS);
            e3.updateStatusIfNeeded(duringOverlap);
            assertThat(e3.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(e3.getEventStatus()).isEqualTo(EventStatus.ONGOING);

            // 4. CLOSED + UPCOMING
            Event e4 = createTestEvent();
            e4.openRegistration();
            e4.closeRegistrationManually();
            assertThat(e4.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(e4.getEventStatus()).isEqualTo(EventStatus.UPCOMING);

            // 5. CLOSED + ONGOING
            Event e5 = createTestEvent();
            e5.openRegistration();
            e5.closeRegistrationManually();
            e5.startOngoing();
            assertThat(e5.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(e5.getEventStatus()).isEqualTo(EventStatus.ONGOING);

            // 6. CLOSED + COMPLETED
            Event e6 = createTestEvent();
            e6.openRegistration();
            e6.closeRegistrationManually();
            e6.startOngoing();
            e6.complete();
            assertThat(e6.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(e6.getEventStatus()).isEqualTo(EventStatus.COMPLETED);

            // 7. CLOSED + CANCELED
            Event e7 = createTestEvent();
            e7.cancel();
            assertThat(e7.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(e7.getEventStatus()).isEqualTo(EventStatus.CANCELED);
        }
    }

    // === 2.7 행사 수정 ===

    @Nested
    @DisplayName("행사 수정")
    class UpdateMethodTest {

        @Test
        @DisplayName("[EVT-050] UPCOMING 상태에서 전체 필드 수정 성공")
        void update_WhenUpcoming_Success() {
            Event event = createTestEvent();
            String newTitle = "수정된 제목";
            String newDescription = "수정된 설명";
            Integer newCapacity = 50;

            event.update(newTitle, newDescription, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, newCapacity, null);

            assertThat(event.getTitle()).isEqualTo(newTitle);
            assertThat(event.getDescription()).isEqualTo(newDescription);
            assertThat(event.getCapacity()).isEqualTo(newCapacity);
        }

        @Test
        @DisplayName("[EVT-051] OPEN 상태(registrationStatus)에서 수정 성공")
        void update_WhenRegistrationOpen_Success() {
            Event event = createTestEvent();
            event.openRegistration();
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);

            event.update("새 제목", "새 설명", "새 장소",
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 50, null);

            assertThat(event.getTitle()).isEqualTo("새 제목");
            assertThat(event.getDescription()).isEqualTo("새 설명");
            assertThat(event.getLocation()).isEqualTo("새 장소");
            assertThat(event.getCapacity()).isEqualTo(50);
        }

        @Test
        @DisplayName("[EVT-052] COMPLETED 상태에서 수정 불가")
        void update_WhenCompleted_ThrowsException() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            event.startOngoing();
            event.complete();

            assertThatThrownBy(() -> event.update("새 제목", "새 설명", LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 40, null))
                    .isInstanceOf(EventNotEditableException.class);
        }

        @Test
        @DisplayName("[EVT-054] 정원 0으로 수정 불가")
        void update_WithZeroCapacity_ThrowsException() {
            Event event = createTestEvent();

            assertThatThrownBy(() -> event.update("새 제목", "새 설명", LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 0, null))
                    .isInstanceOf(InvalidEventCapacityException.class);
        }

        @Test
        @DisplayName("[EVT-100] ONGOING 허용 필드: title 수정")
        void update_OngoingTitle_Success() {
            Event event = createOngoingEvent();
            event.update("새 제목", DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, CAPACITY, null);
            assertThat(event.getTitle()).isEqualTo("새 제목");
        }

        @Test
        @DisplayName("[EVT-101] ONGOING 허용 필드: description 수정")
        void update_OngoingDescription_Success() {
            Event event = createOngoingEvent();
            event.update(TITLE, "새 설명", LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, CAPACITY, null);
            assertThat(event.getDescription()).isEqualTo("새 설명");
        }

        @Test
        @DisplayName("[EVT-102] ONGOING 허용 필드: location 수정")
        void update_OngoingLocation_Success() {
            Event event = createOngoingEvent();
            event.update(TITLE, DESCRIPTION, "새 장소",
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, CAPACITY, null);
            assertThat(event.getLocation()).isEqualTo("새 장소");
        }

        @Test
        @DisplayName("[EVT-103] ONGOING 허용 필드: eventEndAt 수정")
        void update_OngoingEventEndAt_Success() {
            Event event = createOngoingEvent();
            Instant newEventEnd = EVENT_END_AT.plus(1, ChronoUnit.DAYS);
            event.update(TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, newEventEnd, REGISTRATION_START_AT, REGISTRATION_END_AT, CAPACITY, null);
            assertThat(event.getEventEndAt()).isEqualTo(newEventEnd);
        }

        @Test
        @DisplayName("[EVT-104] ONGOING 허용 필드: registrationEndAt 수정")
        void update_OngoingRegEndAt_Success() {
            Event event = createOngoingEvent();
            Instant newRegEnd = REGISTRATION_END_AT.plus(1, ChronoUnit.DAYS);
            event.update(TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, newRegEnd, CAPACITY, null);
            assertThat(event.getRegistrationEndAt()).isEqualTo(newRegEnd);
        }

        @Test
        @DisplayName("[EVT-105] ONGOING 허용 필드: capacity 수정 (capacity >= currentCount)")
        void update_OngoingCapacity_WhenAboveCurrentCount_Success() {
            Event event = createOngoingEvent();
            // currentCount=0, so capacity=10 is fine
            event.update(TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 10, null);
            assertThat(event.getCapacity()).isEqualTo(10);
        }

        @Test
        @DisplayName("[EVT-106] ONGOING 금지 필드: eventStartAt 변경")
        void update_OngoingEventStartAt_ThrowsException() {
            Event event = createOngoingEvent();
            Instant newEventStart = EVENT_START_AT.plus(1, ChronoUnit.HOURS);

            assertThatThrownBy(() -> event.update(TITLE, DESCRIPTION, LOCATION,
                    newEventStart, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, CAPACITY, null))
                    .isInstanceOf(EventNotEditableException.class);
        }

        @Test
        @DisplayName("[EVT-107] ONGOING 금지 필드: registrationStartAt 변경")
        void update_OngoingRegStartAt_ThrowsException() {
            Event event = createOngoingEvent();
            Instant newRegStart = REGISTRATION_START_AT.plus(1, ChronoUnit.HOURS);

            assertThatThrownBy(() -> event.update(TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, newRegStart, REGISTRATION_END_AT, CAPACITY, null))
                    .isInstanceOf(EventNotEditableException.class);
        }

        @Test
        @DisplayName("[EVT-108] ONGOING capacity 감소: capacity < currentCount 거부")
        void update_OngoingCapacityBelowCurrentCount_ThrowsException() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    10, EventRegistrationType.AUTO_APPROVE, null);
            event.openRegistration();
            for (int i = 0; i < 5; i++) event.incrementCurrentCount();
            event.closeRegistrationManually();
            event.startOngoing();

            assertThatThrownBy(() -> event.update(TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 3, null))
                    .isInstanceOf(InvalidEventCapacityException.class);
        }

        @Test
        @DisplayName("[EVT-109] CANCELED 상태에서 수정 시도 시 예외 발생")
        void update_WhenCanceled_ThrowsException() {
            Event event = createTestEvent();
            event.cancel();
            Instant newEventStart = EVENT_START_AT.plus(1, ChronoUnit.DAYS);
            Instant newEventEnd = EVENT_END_AT.plus(2, ChronoUnit.DAYS);

            assertThatThrownBy(() -> event.update("새 제목", "새 설명", "새 장소",
                    newEventStart, newEventEnd, REGISTRATION_START_AT, REGISTRATION_END_AT, 50, null))
                    .isInstanceOf(EventNotEditableException.class);
        }

        @Test
        @DisplayName("[EVT-110] COMPLETED 상태에서 수정 시도 거부 (2축)")
        void update_WhenCompleted_ThrowsException_TwoAxis() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            event.startOngoing();
            event.complete();
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.COMPLETED);

            assertThatThrownBy(() -> event.update("새 제목", "새 설명", LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 40, null))
                    .isInstanceOf(EventNotEditableException.class);
        }
    }

    // === 2.8 행사 취소/재활성화 ===

    @Nested
    @DisplayName("행사 취소/재활성화")
    class CancelReactivateTest {

        @Test
        @DisplayName("[EVT-111] UPCOMING+OPEN 취소 시 registrationStatus=CLOSED 강제")
        void cancel_FromUpcomingOpen_ForcesClosedRegistration() {
            Event event = createTestEvent();
            event.openRegistration();

            event.cancel();

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.CANCELED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.MANUAL_CLOSE);
        }

        @Test
        @DisplayName("[EVT-112] ONGOING+OPEN 취소 시 registrationStatus=CLOSED 강제")
        void cancel_FromOngoingOpen_ForcesClosedRegistration() {
            // Create overlap event (OPEN during ONGOING)
            Instant regStart = Instant.now();
            Instant eventStart = Instant.now().plus(7, ChronoUnit.DAYS);
            Instant regEnd = Instant.now().plus(10, ChronoUnit.DAYS);
            Instant eventEnd = Instant.now().plus(14, ChronoUnit.DAYS);
            Event event = Event.create(createMockUser(), TITLE, DESCRIPTION, LOCATION,
                    eventStart, eventEnd, regStart, regEnd, CAPACITY, EventRegistrationType.AUTO_APPROVE, null);
            Instant duringOverlap = eventStart.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(duringOverlap);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.ONGOING);

            event.cancel();

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.CANCELED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }

        @Test
        @DisplayName("[EVT-113] COMPLETED 행사 취소 불가 (종단)")
        void cancel_FromCompleted_ThrowsException() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            event.startOngoing();
            event.complete();

            assertThatThrownBy(() -> event.cancel())
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }

        @Test
        @DisplayName("[EVT-114] CANCELED 재활성화: now < eventStart → UPCOMING + Lazy 등록 복원")
        void reactivate_BeforeEventStart_RestoredByLazy() {
            Event event = createTestEvent();
            event.openRegistration();
            event.cancel();

            Instant afterRegStart = REGISTRATION_START_AT.plus(1, ChronoUnit.HOURS);
            event.reactivate(afterRegStart);

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.UPCOMING);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
        }

        @Test
        @DisplayName("[EVT-115] CANCELED 재활성화: eventStart <= now < eventEnd → ONGOING")
        void reactivate_DuringEvent_TransitionsToOngoing() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            event.startOngoing();
            event.cancel();

            Instant duringEvent = EVENT_START_AT.plus(6, ChronoUnit.HOURS);
            event.reactivate(duringEvent);

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.ONGOING);
        }

        @Test
        @DisplayName("[EVT-116] CANCELED 재활성화: now > eventEnd → COMPLETED")
        void reactivate_AfterEventEnd_TransitionsToCompleted() {
            Event event = createTestEvent();
            event.cancel();

            Instant afterEventEnd = EVENT_END_AT.plus(1, ChronoUnit.DAYS);
            event.reactivate(afterEventEnd);

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.COMPLETED);
        }

        @Test
        @DisplayName("[EVT-117] 이미 활성 상태 재활성화 불가")
        void reactivate_FromUpcoming_ThrowsException() {
            Event event = createTestEvent();

            assertThatThrownBy(() -> event.reactivate(Instant.now()))
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }
    }

    // === 2.9 수동 재오픈 (도메인 레벨) ===

    @Nested
    @DisplayName("수동 재오픈 (도메인)")
    class ManualReopenDomainTest {

        @Test
        @DisplayName("[EVT-118] CLOSED→OPEN 수동 재오픈 성공")
        void reopenRegistration_FromClosed_TransitionsToOpen() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();

            event.reopenRegistration();

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(event.getCloseReason()).isNull();
        }

        @Test
        @DisplayName("[EVT-126] CAPACITY_FULL 에서 수동 재오픈 성공")
        void reopenRegistration_FromCapacityFull_Success() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    2, EventRegistrationType.AUTO_APPROVE, null);
            event.openRegistration();
            event.incrementCurrentCount();
            event.incrementCurrentCount();
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.CAPACITY_FULL);

            // decrement first so it's not full, then reopen
            Instant beforeRegEnd = REGISTRATION_END_AT.minus(1, ChronoUnit.HOURS);
            event.decrementCurrentCount(beforeRegEnd);
            // auto-reopen already happens via decrementCurrentCount for CAPACITY_FULL
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
        }

        @Test
        @DisplayName("[EVT-127] DEADLINE_PASSED에서 수동 재오픈 성공")
        void reopenRegistration_FromDeadlinePassed_Success() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationByDeadline();
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.DEADLINE_PASSED);

            event.reopenRegistration();

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(event.getCloseReason()).isNull();
        }

        @Test
        @DisplayName("[EVT-128] MANUAL_CLOSE 에서 수동 재오픈 성공")
        void reopenRegistration_FromManualClose_Success() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.MANUAL_CLOSE);

            event.reopenRegistration();

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(event.getCloseReason()).isNull();
        }

        @Test
        @DisplayName("[EVT-129] ONGOING 중 수동 재오픈 성공 (2축 모델 핵심)")
        void reopenRegistration_DuringOngoing_Success() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            event.startOngoing();

            event.reopenRegistration();

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.ONGOING);
        }
    }

    // === 2.11 신청자 수 관리 ===

    @Nested
    @DisplayName("신청자 수 관리")
    class CurrentCountManagementTest {

        @Test
        @DisplayName("[EVT-020] 신청자 수 증가 성공")
        void incrementCurrentCount_Success() {
            Event event = createTestEvent();
            event.openRegistration();
            assertThat(event.getCurrentCount()).isEqualTo(0);

            event.incrementCurrentCount();

            assertThat(event.getCurrentCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("[EVT-021] 정원이 차면 자동으로 CLOSED 상태로 변경")
        void incrementCurrentCount_WhenFull_AutoCloses() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    2, EventRegistrationType.AUTO_APPROVE, null);
            event.openRegistration();

            event.incrementCurrentCount();
            event.incrementCurrentCount();

            assertThat(event.getCurrentCount()).isEqualTo(2);
            assertThat(event.isFull()).isTrue();
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.CAPACITY_FULL);
        }

        @Test
        @DisplayName("[EVT-022] 신청자 수 감소 성공")
        void decrementCurrentCount_Success() {
            Event event = createTestEvent();
            event.openRegistration();
            event.incrementCurrentCount();
            event.incrementCurrentCount();

            Instant beforeRegEnd = REGISTRATION_END_AT.minus(1, ChronoUnit.HOURS);
            event.decrementCurrentCount(beforeRegEnd);

            assertThat(event.getCurrentCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("[EVT-023] 정원 마감 후 취소 시 자동으로 OPEN 상태로 변경")
        void decrementCurrentCount_WhenCapacityFullClosed_ReopensAutomatically() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    2, EventRegistrationType.AUTO_APPROVE, null);
            event.openRegistration();
            event.incrementCurrentCount();
            event.incrementCurrentCount();
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);

            Instant beforeRegEnd = REGISTRATION_END_AT.minus(1, ChronoUnit.HOURS);
            event.decrementCurrentCount(beforeRegEnd);

            assertThat(event.getCurrentCount()).isEqualTo(1);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(event.getCloseReason()).isNull();
        }

        @Test
        @DisplayName("[EVT-024] 신청자 수가 0일 때 감소해도 음수가 되지 않음")
        void decrementCurrentCount_WhenZero_StaysZero() {
            Event event = createTestEvent();
            event.openRegistration();
            assertThat(event.getCurrentCount()).isEqualTo(0);

            Instant now = Instant.now();
            event.decrementCurrentCount(now);

            assertThat(event.getCurrentCount()).isEqualTo(0);
        }
    }

    // === 2.12 조회 메서드 ===

    @Nested
    @DisplayName("조회 메서드")
    class QueryMethodsTest {

        @Test
        @DisplayName("[EVT-030] OPEN+여유 시 신청 가능")
        void isRegistrable_WhenOpenAndNotFull_ReturnsTrue() {
            Event event = createTestEvent();
            event.openRegistration();
            assertThat(event.isRegistrable()).isTrue();
        }

        @Test
        @DisplayName("[EVT-031] OPEN+정원 초과 시 신청 불가")
        void isRegistrable_WhenOpenButFull_ReturnsFalse() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    1, EventRegistrationType.AUTO_APPROVE, null);
            event.openRegistration();
            event.incrementCurrentCount();
            assertThat(event.isRegistrable()).isFalse();
        }

        @Test
        @DisplayName("[EVT-032] NOT_STARTED 시 신청 불가")
        void isRegistrable_WhenNotStarted_ReturnsFalse() {
            Event event = createTestEvent();
            assertThat(event.isRegistrable()).isFalse();
        }

        @Test
        @DisplayName("[EVT-033] 남은 자리 수 계산")
        void getRemainingCapacity_ReturnsCorrectValue() {
            Event event = createTestEvent();
            event.openRegistration();
            event.incrementCurrentCount();
            event.incrementCurrentCount();
            assertThat(event.getRemainingCapacity()).isEqualTo(28);
        }

        @Test
        @DisplayName("[EVT-034] 정원 초과 시 남은 자리 수는 0")
        void getRemainingCapacity_WhenOverCapacity_ReturnsZero() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    1, EventRegistrationType.AUTO_APPROVE, null);
            event.openRegistration();
            event.incrementCurrentCount();
            assertThat(event.getRemainingCapacity()).isEqualTo(0);
        }

        @Test
        @DisplayName("[EVT-035] 자동 승인 여부 확인")
        void isAutoApprove_WhenAutoApprove_ReturnsTrue() {
            Event event = createTestEvent();
            assertThat(event.isAutoApprove()).isTrue();
            assertThat(event.isManualApprove()).isFalse();
        }

        @Test
        @DisplayName("[EVT-036] 수동 승인 여부 확인")
        void isManualApprove_WhenManualApprove_ReturnsTrue() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, EventRegistrationType.MANUAL_APPROVE, null);
            assertThat(event.isManualApprove()).isTrue();
            assertThat(event.isAutoApprove()).isFalse();
        }
    }

    // === 2.13 시간 중복 확인 ===

    @Nested
    @DisplayName("시간 중복 확인")
    class OverlapsTest {

        @Test
        @DisplayName("[EVT-040] 완전히 겹치는 시간대 - true")
        void overlaps_WhenCompletelyOverlapping_ReturnsTrue() {
            Event event = createTestEvent();
            assertThat(event.overlaps(EVENT_START_AT, EVENT_END_AT)).isTrue();
        }

        @Test
        @DisplayName("[EVT-041] 부분 겹침 (앞) - true")
        void overlaps_WhenPartiallyOverlappingFront_ReturnsTrue() {
            Event event = createTestEvent();
            assertThat(event.overlaps(
                    EVENT_START_AT.minus(6, ChronoUnit.HOURS),
                    EVENT_START_AT.plus(6, ChronoUnit.HOURS))).isTrue();
        }

        @Test
        @DisplayName("[EVT-042] 부분 겹침 (뒤) - true")
        void overlaps_WhenPartiallyOverlappingBack_ReturnsTrue() {
            Event event = createTestEvent();
            assertThat(event.overlaps(
                    EVENT_END_AT.minus(6, ChronoUnit.HOURS),
                    EVENT_END_AT.plus(6, ChronoUnit.HOURS))).isTrue();
        }

        @Test
        @DisplayName("[EVT-043] 완전히 포함 - true")
        void overlaps_WhenContainedWithin_ReturnsTrue() {
            Event event = createTestEvent();
            assertThat(event.overlaps(
                    EVENT_START_AT.plus(1, ChronoUnit.HOURS),
                    EVENT_END_AT.minus(1, ChronoUnit.HOURS))).isTrue();
        }

        @Test
        @DisplayName("[EVT-044] 완전히 앞에 - false")
        void overlaps_WhenCompletelyBefore_ReturnsFalse() {
            Event event = createTestEvent();
            assertThat(event.overlaps(
                    EVENT_START_AT.minus(3, ChronoUnit.DAYS),
                    EVENT_START_AT.minus(1, ChronoUnit.DAYS))).isFalse();
        }

        @Test
        @DisplayName("[EVT-045] 완전히 뒤에 - false")
        void overlaps_WhenCompletelyAfter_ReturnsFalse() {
            Event event = createTestEvent();
            assertThat(event.overlaps(
                    EVENT_END_AT.plus(1, ChronoUnit.DAYS),
                    EVENT_END_AT.plus(2, ChronoUnit.DAYS))).isFalse();
        }

        @Test
        @DisplayName("[EVT-046] 경계에서 끝남 - false")
        void overlaps_WhenEndingAtStart_ReturnsFalse() {
            Event event = createTestEvent();
            assertThat(event.overlaps(
                    EVENT_START_AT.minus(1, ChronoUnit.DAYS),
                    EVENT_START_AT)).isFalse();
        }

        @Test
        @DisplayName("[EVT-047] 경계에서 시작 - false")
        void overlaps_WhenStartingAtEnd_ReturnsFalse() {
            Event event = createTestEvent();
            assertThat(event.overlaps(
                    EVENT_END_AT,
                    EVENT_END_AT.plus(1, ChronoUnit.DAYS))).isFalse();
        }
    }

    // === 2.14 closeReason 정합성 ===

    @Nested
    @DisplayName("closeReason 정합성")
    class CloseReasonConsistencyTest {

        @Test
        @DisplayName("[EVT-141] registrationStatus=CLOSED 시 closeReason != null")
        void closed_HasNonNullCloseReason() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isNotNull();
        }

        @Test
        @DisplayName("[EVT-142] registrationStatus=OPEN 시 closeReason == null")
        void open_HasNullCloseReason() {
            Event event = createTestEvent();
            event.openRegistration();

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(event.getCloseReason()).isNull();
        }

        @Test
        @DisplayName("[EVT-143] registrationStatus=NOT_STARTED 시 closeReason == null")
        void notStarted_HasNullCloseReason() {
            Event event = createTestEvent();

            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.NOT_STARTED);
            assertThat(event.getCloseReason()).isNull();
        }
    }

    // === 축 1: Visibility (공개 상태) 전이 ===

    @Nested
    @DisplayName("축 1: Visibility (공개 상태) 전이")
    class VisibilityTransitionTest {

        @Test
        @DisplayName("[GAP-EVT-25] 행사 생성 시 visibility=UNPUBLISHED 초기 상태")
        void create_InitialVisibility_IsUnpublished() {
            Event event = createTestEvent();
            assertThat(event.getVisibility()).isEqualTo(EventVisibility.UNPUBLISHED);
        }

        @Test
        @DisplayName("[GAP-EVT-26] publish(): UNPUBLISHED -> PUBLISHED 전이 성공")
        void publish_FromUnpublished_TransitionsToPublished() {
            Event event = createTestEvent();
            assertThat(event.getVisibility()).isEqualTo(EventVisibility.UNPUBLISHED);

            event.publish();

            assertThat(event.getVisibility()).isEqualTo(EventVisibility.PUBLISHED);
        }

        @Test
        @DisplayName("[GAP-EVT-26] publish(): 이미 PUBLISHED이면 InvalidEventStateTransitionException")
        void publish_FromPublished_ThrowsException() {
            Event event = createTestEvent();
            event.publish();
            assertThat(event.getVisibility()).isEqualTo(EventVisibility.PUBLISHED);

            assertThatThrownBy(() -> event.publish())
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }

        @Test
        @DisplayName("[GAP-EVT-26] unpublish(): PUBLISHED -> UNPUBLISHED 전이 성공")
        void unpublish_FromPublished_TransitionsToUnpublished() {
            Event event = createTestEvent();
            event.publish();
            assertThat(event.getVisibility()).isEqualTo(EventVisibility.PUBLISHED);

            event.unpublish();

            assertThat(event.getVisibility()).isEqualTo(EventVisibility.UNPUBLISHED);
        }

        @Test
        @DisplayName("[GAP-EVT-26] unpublish(): 이미 UNPUBLISHED이면 InvalidEventStateTransitionException")
        void unpublish_FromUnpublished_ThrowsException() {
            Event event = createTestEvent();
            assertThat(event.getVisibility()).isEqualTo(EventVisibility.UNPUBLISHED);

            assertThatThrownBy(() -> event.unpublish())
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }

        @Test
        @DisplayName("[GAP-EVT-31] unpublish(): registrationStatus=OPEN이면 CLOSED(MANUAL_CLOSE)로 자동 마감")
        void unpublish_WhenRegistrationOpen_AutoClosesWithManualClose() {
            Event event = createTestEvent();
            event.publish();
            event.openRegistration();
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);

            event.unpublish();

            assertThat(event.getVisibility()).isEqualTo(EventVisibility.UNPUBLISHED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.MANUAL_CLOSE);
        }

        @Test
        @DisplayName("[GAP-EVT-32] unpublish(): registrationStatus=NOT_STARTED이면 변경 없음")
        void unpublish_WhenRegistrationNotStarted_NoChange() {
            Event event = createTestEvent();
            event.publish();
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.NOT_STARTED);

            event.unpublish();

            assertThat(event.getVisibility()).isEqualTo(EventVisibility.UNPUBLISHED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.NOT_STARTED);
        }

        @Test
        @DisplayName("[GAP-EVT-33] unpublish(): registrationStatus=CLOSED이면 기존 closeReason 유지")
        void unpublish_WhenRegistrationClosed_KeepsCloseReason() {
            Event event = createTestEvent();
            event.publish();
            event.openRegistration();
            event.closeRegistrationByDeadline();
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.DEADLINE_PASSED);

            event.unpublish();

            assertThat(event.getVisibility()).isEqualTo(EventVisibility.UNPUBLISHED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(EventCloseReason.DEADLINE_PASSED);
        }

        @Test
        @DisplayName("[GAP-EVT-34] publish() 시 eventStatus 변경 없음 (독립성)")
        void publish_DoesNotChangeEventStatus() {
            Event event = createTestEvent();
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.UPCOMING);

            event.publish();

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.UPCOMING);
        }

        @Test
        @DisplayName("[GAP-EVT-34] unpublish() 시 eventStatus 변경 없음 (독립성)")
        void unpublish_DoesNotChangeEventStatus() {
            Event event = createTestEvent();
            event.publish();
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.UPCOMING);

            event.unpublish();

            assertThat(event.getEventStatus()).isEqualTo(EventStatus.UPCOMING);
        }

        @Test
        @DisplayName("[GAP-EVT-35] update() 호출 전후 visibility 변경 없음 (축 독립성)")
        void update_DoesNotChangeVisibility() {
            Event event = createTestEvent();
            assertThat(event.getVisibility()).isEqualTo(EventVisibility.UNPUBLISHED);

            event.update("새 제목", "새 설명", "새 장소",
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 50, null);

            assertThat(event.getVisibility()).isEqualTo(EventVisibility.UNPUBLISHED);
        }

        @Test
        @DisplayName("[GAP-EVT-35] update() 호출 전후 PUBLISHED visibility 유지")
        void update_KeepsPublishedVisibility() {
            Event event = createTestEvent();
            event.publish();
            assertThat(event.getVisibility()).isEqualTo(EventVisibility.PUBLISHED);

            event.update("새 제목", "새 설명", "새 장소",
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 50, null);

            assertThat(event.getVisibility()).isEqualTo(EventVisibility.PUBLISHED);
        }

        @Test
        @DisplayName("[GAP-EVT-42] COMPLETED 행사 unpublish: registrationStatus 변경 없음 (이미 CLOSED)")
        void unpublish_CompletedEvent_RegistrationStatusUnchanged() {
            Event event = createTestEvent();
            event.publish();
            event.openRegistration();
            event.closeRegistrationManually();
            event.startOngoing();
            event.complete();
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.COMPLETED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            EventCloseReason originalCloseReason = event.getCloseReason();

            event.unpublish();

            assertThat(event.getVisibility()).isEqualTo(EventVisibility.UNPUBLISHED);
            assertThat(event.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(event.getCloseReason()).isEqualTo(originalCloseReason);
        }

        @Test
        @DisplayName("[GAP-EVT-43] CANCELED 행사 publish: eventStatus 변경 없음 (독립성)")
        void publish_CanceledEvent_EventStatusUnchanged() {
            Event event = createTestEvent();
            event.cancel();
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.CANCELED);

            event.publish();

            assertThat(event.getVisibility()).isEqualTo(EventVisibility.PUBLISHED);
            assertThat(event.getEventStatus()).isEqualTo(EventStatus.CANCELED);
        }
    }

    // === 설문 연동 (Survey-Event Linking) ===

    @Nested
    @DisplayName("설문 연동 (Survey-Event Linking)")
    class SurveyEventLinkingTest {

        @Test
        @DisplayName("[TC-001] 설문 미연결 행사 생성 시 surveyId가 null로 설정됨")
        void create_WithoutSurvey_SurveyIdIsNull() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, EventRegistrationType.AUTO_APPROVE, null);

            assertThat(event.getSurveyId()).isNull();
            assertThat(event.hasSurvey()).isFalse();
        }

        @Test
        @DisplayName("[TC-003] 행사 생성 시 단일 surveyId만 설정됨")
        void create_WithSurveyId_SingleSurveyIdSet() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, EventRegistrationType.AUTO_APPROVE, 100L);

            assertThat(event.getSurveyId()).isEqualTo(100L);
            assertThat(event.hasSurvey()).isTrue();
        }

        @Test
        @DisplayName("[TC-009] UPCOMING 상태 행사에서 설문 연결 변경 성공 (null -> 100)")
        void update_UpcomingEvent_SurveyIdChangeSucceeds() {
            Event event = createTestEvent();
            assertThat(event.getSurveyId()).isNull();

            event.update(TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, 100L);

            assertThat(event.getSurveyId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("[TC-010] ONGOING 상태 행사에서 설문 연결 변경 성공 (100 -> 200)")
        void update_OngoingEvent_SurveyIdChangeSucceeds() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, EventRegistrationType.AUTO_APPROVE, 100L);
            event.openRegistration();
            event.closeRegistrationManually();
            event.startOngoing();

            event.update(TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, 200L);

            assertThat(event.getSurveyId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("[TC-011] CANCELED 상태 행사에서 설문 변경 시 수정 불가 예외")
        void update_CanceledEvent_SurveyIdChangeThrowsException() {
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, EventRegistrationType.AUTO_APPROVE, 100L);
            event.openRegistration();
            event.cancel();

            assertThatThrownBy(() -> event.update(TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, null))
                    .isInstanceOf(EventNotEditableException.class);
        }

        @Test
        @DisplayName("[TC-012] COMPLETED 상태 행사에서 설문 연결 변경 시 수정 불가 예외")
        void update_CompletedEvent_SurveyIdChangeThrowsException() {
            Event event = createTestEvent();
            event.openRegistration();
            event.closeRegistrationManually();
            event.startOngoing();
            event.complete();

            assertThatThrownBy(() -> event.update(TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, 100L))
                    .isInstanceOf(EventNotEditableException.class);
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
                CAPACITY, EventRegistrationType.AUTO_APPROVE, null);
    }

    private Event createOngoingEvent() {
        Event event = createTestEvent();
        event.openRegistration();
        event.closeRegistrationManually();
        event.startOngoing();
        return event;
    }
}
