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
 * Event 도메인 테스트.
 * 테스트 케이스 문서: docs/test-case/event/event-test-cases.md
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

    @Nested
    @DisplayName("Event.create 정적 팩토리 메서드")
    class CreateEventTest {

        /**
         * EVT-001: 선착순 행사 생성
         */
        @Test
        @DisplayName("[EVT-001] 유효한 정보로 선착순 행사 생성 성공")
        void create_WithValidInfoAutoApprove_ReturnsEvent() {
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

        /**
         * EVT-002: 선발제 행사 생성
         */
        @Test
        @DisplayName("[EVT-002] 유효한 정보로 선발제 행사 생성 성공")
        void create_WithValidInfoManualApprove_ReturnsEvent() {
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

        /**
         * EVT-003: 정원 0 생성 거부
         */
        @Test
        @DisplayName("[EVT-003] 정원이 0일 때 예외 발생")
        void create_WithZeroCapacity_ThrowsException() {
            // given
            User mockUser = createMockUser();

            // when & then
            assertThatThrownBy(() -> Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    0, EventRegistrationType.AUTO_APPROVE))
                    .isInstanceOf(InvalidEventCapacityException.class);
        }

        /**
         * EVT-004: 정원 음수 생성 거부
         */
        @Test
        @DisplayName("[EVT-004] 정원이 음수일 때 예외 발생")
        void create_WithNegativeCapacity_ThrowsException() {
            // given
            User mockUser = createMockUser();

            // when & then
            assertThatThrownBy(() -> Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    -1, EventRegistrationType.AUTO_APPROVE))
                    .isInstanceOf(InvalidEventCapacityException.class);
        }

        /**
         * EVT-005: 정원 null 생성 거부
         */
        @Test
        @DisplayName("[EVT-005] 정원이 null일 때 예외 발생")
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

        /**
         * EVT-010: UPCOMING→OPEN 전이
         */
        @Test
        @DisplayName("[EVT-010] UPCOMING에서 OPEN으로 전이 성공")
        void open_FromUpcoming_Success() {
            // given
            Event event = createTestEvent();
            assertThat(event.getStatus()).isEqualTo(EventStatus.UPCOMING);

            // when
            event.open();

            // then
            assertThat(event.getStatus()).isEqualTo(EventStatus.OPEN);
        }

        /**
         * EVT-012: OPEN→CLOSED 수동 마감
         */
        @Test
        @DisplayName("[EVT-012] OPEN에서 CLOSED로 전이 성공 (수동 마감)")
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

        /**
         * EVT-013: OPEN→CLOSED 기한 만료
         */
        @Test
        @DisplayName("[EVT-013] OPEN에서 CLOSED로 전이 성공 (기한 만료)")
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

        /**
         * EVT-014: CLOSED→OPEN 재오픈
         */
        @Test
        @DisplayName("[EVT-014] CLOSED에서 OPEN으로 전이 성공 (재오픈)")
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

        /**
         * EVT-015: COMPLETED→OPEN 전이 불가
         */
        @Test
        @DisplayName("[EVT-015] COMPLETED에서 OPEN으로 전이 시 예외 발생")
        void open_FromCompleted_ThrowsException() {
            // given
            Event event = createTestEvent();
            event.open();
            event.closeManually();
            Instant afterEventEnd = EVENT_END_AT.plus(1, ChronoUnit.DAYS);
            event.updateStatusIfNeeded(afterEventEnd);
            assertThat(event.getStatus()).isEqualTo(EventStatus.COMPLETED);

            // when & then
            assertThatThrownBy(() -> event.open())
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }

        /**
         * EVT-017: UPCOMING→COMPLETED 직접 전이 불가
         */
        @Test
        @DisplayName("[EVT-017] UPCOMING에서 COMPLETED로 직접 전이 시 예외 발생")
        void complete_FromUpcoming_ThrowsException() {
            // given
            Event event = createTestEvent();

            // when & then
            assertThatThrownBy(() -> event.complete())
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }

        /**
         * EVT-060: CLOSED → ONGOING 자동 전환 (행사 시작일 도래)
         */
        @Test
        @DisplayName("[EVT-060] CLOSED 상태에서 행사 시작일이 지나면 ONGOING으로 자동 전환")
        void updateStatusIfNeeded_FromClosed_ToOngoing() {
            // given
            Event event = createTestEvent();
            event.open();
            event.closeManually();
            assertThat(event.getStatus()).isEqualTo(EventStatus.CLOSED);

            // when
            Instant afterEventStart = EVENT_START_AT.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(afterEventStart);

            // then
            assertThat(event.getStatus()).isEqualTo(EventStatus.ONGOING);
        }

        /**
         * EVT-061: ONGOING → COMPLETED 자동 전환 (행사 종료일 경과)
         */
        @Test
        @DisplayName("[EVT-061] ONGOING 상태에서 행사 종료일이 지나면 COMPLETED로 자동 전환")
        void updateStatusIfNeeded_FromOngoing_ToCompleted() {
            // given
            Event event = createTestEvent();
            event.open();
            event.closeManually();
            Instant afterEventStart = EVENT_START_AT.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(afterEventStart);
            assertThat(event.getStatus()).isEqualTo(EventStatus.ONGOING);

            // when
            Instant afterEventEnd = EVENT_END_AT.plus(1, ChronoUnit.DAYS);
            event.updateStatusIfNeeded(afterEventEnd);

            // then
            assertThat(event.getStatus()).isEqualTo(EventStatus.COMPLETED);
        }
    }

    @Nested
    @DisplayName("신청자 수 관리")
    class CurrentCountManagementTest {

        /**
         * EVT-020: 신청자 수 증가
         */
        @Test
        @DisplayName("[EVT-020] 신청자 수 증가 성공")
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

        /**
         * EVT-021: 정원 초과 시 자동 마감
         */
        @Test
        @DisplayName("[EVT-021] 정원이 차면 자동으로 CLOSED 상태로 변경")
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

        /**
         * EVT-022: 신청자 수 감소
         */
        @Test
        @DisplayName("[EVT-022] 신청자 수 감소 성공")
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

        /**
         * EVT-023: 정원 마감 후 취소 시 재오픈
         */
        @Test
        @DisplayName("[EVT-023] 정원 마감 후 취소 시 자동으로 OPEN 상태로 변경")
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

        /**
         * EVT-024: 신청자 수 0 이하 방지
         */
        @Test
        @DisplayName("[EVT-024] 신청자 수가 0일 때 감소해도 음수가 되지 않음")
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

        /**
         * EVT-030: OPEN+여유 시 신청 가능
         */
        @Test
        @DisplayName("[EVT-030] OPEN 상태이고 정원 여유가 있으면 isRegistrable은 true")
        void isRegistrable_WhenOpenAndNotFull_ReturnsTrue() {
            // given
            Event event = createTestEvent();
            event.open();

            // then
            assertThat(event.isRegistrable()).isTrue();
        }

        /**
         * EVT-031: OPEN+정원 초과 시 신청 불가
         */
        @Test
        @DisplayName("[EVT-031] OPEN 상태이지만 정원이 차면 isRegistrable은 false")
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

        /**
         * EVT-032: UPCOMING 시 신청 불가
         */
        @Test
        @DisplayName("[EVT-032] UPCOMING 상태에서는 isRegistrable은 false")
        void isRegistrable_WhenUpcoming_ReturnsFalse() {
            // given
            Event event = createTestEvent();

            // then
            assertThat(event.isRegistrable()).isFalse();
        }

        /**
         * EVT-033: 남은 자리 수 계산
         */
        @Test
        @DisplayName("[EVT-033] 남은 자리 수 계산")
        void getRemainingCapacity_ReturnsCorrectValue() {
            // given
            Event event = createTestEvent();
            event.open();
            event.incrementCurrentCount();
            event.incrementCurrentCount();

            // then
            assertThat(event.getRemainingCapacity()).isEqualTo(28);
        }

        /**
         * EVT-034: 정원 초과 시 남은 자리 0
         */
        @Test
        @DisplayName("[EVT-034] 정원 초과 시 남은 자리 수는 0")
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

        /**
         * EVT-035: 자동 승인 여부 확인
         */
        @Test
        @DisplayName("[EVT-035] 자동 승인 여부 확인")
        void isAutoApprove_WhenAutoApprove_ReturnsTrue() {
            // given
            Event event = createTestEvent(); // AUTO_APPROVE

            // then
            assertThat(event.isAutoApprove()).isTrue();
            assertThat(event.isManualApprove()).isFalse();
        }

        /**
         * EVT-036: 수동 승인 여부 확인
         */
        @Test
        @DisplayName("[EVT-036] 수동 승인 여부 확인")
        void isManualApprove_WhenManualApprove_ReturnsTrue() {
            // given
            User mockUser = createMockUser();
            Event event = Event.create(mockUser, TITLE, DESCRIPTION, LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                    CAPACITY, EventRegistrationType.MANUAL_APPROVE);

            // then
            assertThat(event.isManualApprove()).isTrue();
            assertThat(event.isAutoApprove()).isFalse();
        }
    }

    @Nested
    @DisplayName("시간 중복 확인")
    class OverlapsTest {

        /**
         * EVT-040: 완전히 겹치는 시간대
         */
        @Test
        @DisplayName("[EVT-040] 완전히 겹치는 시간대 - true 반환")
        void overlaps_WhenCompletelyOverlapping_ReturnsTrue() {
            // given
            Event event = createTestEvent();
            Instant otherStart = EVENT_START_AT;
            Instant otherEnd = EVENT_END_AT;

            // then
            assertThat(event.overlaps(otherStart, otherEnd)).isTrue();
        }

        /**
         * EVT-041: 부분적으로 겹치는 시간대 (앞)
         */
        @Test
        @DisplayName("[EVT-041] 부분적으로 겹치는 시간대 (앞부분) - true 반환")
        void overlaps_WhenPartiallyOverlappingFront_ReturnsTrue() {
            // given
            Event event = createTestEvent();
            Instant otherStart = EVENT_START_AT.minus(6, ChronoUnit.HOURS);
            Instant otherEnd = EVENT_START_AT.plus(6, ChronoUnit.HOURS);

            // then
            assertThat(event.overlaps(otherStart, otherEnd)).isTrue();
        }

        /**
         * EVT-042: 부분적으로 겹치는 시간대 (뒤)
         */
        @Test
        @DisplayName("[EVT-042] 부분적으로 겹치는 시간대 (뒷부분) - true 반환")
        void overlaps_WhenPartiallyOverlappingBack_ReturnsTrue() {
            // given
            Event event = createTestEvent();
            Instant otherStart = EVENT_END_AT.minus(6, ChronoUnit.HOURS);
            Instant otherEnd = EVENT_END_AT.plus(6, ChronoUnit.HOURS);

            // then
            assertThat(event.overlaps(otherStart, otherEnd)).isTrue();
        }

        /**
         * EVT-043: 완전히 포함되는 시간대
         */
        @Test
        @DisplayName("[EVT-043] 완전히 포함되는 시간대 - true 반환")
        void overlaps_WhenContainedWithin_ReturnsTrue() {
            // given
            Event event = createTestEvent();
            Instant otherStart = EVENT_START_AT.plus(1, ChronoUnit.HOURS);
            Instant otherEnd = EVENT_END_AT.minus(1, ChronoUnit.HOURS);

            // then
            assertThat(event.overlaps(otherStart, otherEnd)).isTrue();
        }

        /**
         * EVT-044: 완전히 앞에 있는 시간대
         */
        @Test
        @DisplayName("[EVT-044] 완전히 앞에 있는 시간대 - false 반환")
        void overlaps_WhenCompletelyBefore_ReturnsFalse() {
            // given
            Event event = createTestEvent();
            Instant otherStart = EVENT_START_AT.minus(3, ChronoUnit.DAYS);
            Instant otherEnd = EVENT_START_AT.minus(1, ChronoUnit.DAYS);

            // then
            assertThat(event.overlaps(otherStart, otherEnd)).isFalse();
        }

        /**
         * EVT-045: 완전히 뒤에 있는 시간대
         */
        @Test
        @DisplayName("[EVT-045] 완전히 뒤에 있는 시간대 - false 반환")
        void overlaps_WhenCompletelyAfter_ReturnsFalse() {
            // given
            Event event = createTestEvent();
            Instant otherStart = EVENT_END_AT.plus(1, ChronoUnit.DAYS);
            Instant otherEnd = EVENT_END_AT.plus(2, ChronoUnit.DAYS);

            // then
            assertThat(event.overlaps(otherStart, otherEnd)).isFalse();
        }

        /**
         * EVT-046: 경계에서 끝나는 시간대
         */
        @Test
        @DisplayName("[EVT-046] 경계에서 끝나는 시간대 - false 반환")
        void overlaps_WhenEndingAtStart_ReturnsFalse() {
            // given
            Event event = createTestEvent();
            Instant otherStart = EVENT_START_AT.minus(1, ChronoUnit.DAYS);
            Instant otherEnd = EVENT_START_AT;

            // then
            assertThat(event.overlaps(otherStart, otherEnd)).isFalse();
        }

        /**
         * EVT-047: 경계에서 시작하는 시간대
         */
        @Test
        @DisplayName("[EVT-047] 경계에서 시작하는 시간대 - false 반환")
        void overlaps_WhenStartingAtEnd_ReturnsFalse() {
            // given
            Event event = createTestEvent();
            Instant otherStart = EVENT_END_AT;
            Instant otherEnd = EVENT_END_AT.plus(1, ChronoUnit.DAYS);

            // then
            assertThat(event.overlaps(otherStart, otherEnd)).isFalse();
        }
    }

    @Nested
    @DisplayName("수정 메서드")
    class UpdateMethodTest {

        /**
         * EVT-050: UPCOMING 상태에서 수정
         */
        @Test
        @DisplayName("[EVT-050] UPCOMING 상태에서 수정 성공")
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

        /**
         * EVT-051: OPEN 상태에서 수정
         */
        @Test
        @DisplayName("[EVT-051] OPEN 상태에서 수정 성공")
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

        /**
         * EVT-052: COMPLETED 상태에서 수정 불가
         */
        @Test
        @DisplayName("[EVT-052] COMPLETED 상태에서 수정 시 예외 발생")
        void update_WhenCompleted_ThrowsException() {
            // given
            Event event = createTestEvent();
            event.open();
            event.closeManually();
            Instant afterEventEnd = EVENT_END_AT.plus(1, ChronoUnit.DAYS);
            event.updateStatusIfNeeded(afterEventEnd);
            assertThat(event.getStatus()).isEqualTo(EventStatus.COMPLETED);

            // when & then
            assertThatThrownBy(() -> event.update("새 제목", "새 설명", LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 40))
                    .isInstanceOf(EventNotEditableException.class);
        }

        /**
         * EVT-053: ONGOING 상태에서 수정 불가
         */
        @Test
        @DisplayName("[EVT-053] ONGOING 상태에서 수정 시 예외 발생")
        void update_WhenOngoing_ThrowsException() {
            // given
            Event event = createTestEvent();
            event.open();
            event.closeManually();
            Instant afterEventStart = EVENT_START_AT.plus(1, ChronoUnit.HOURS);
            event.updateStatusIfNeeded(afterEventStart);
            assertThat(event.getStatus()).isEqualTo(EventStatus.ONGOING);

            // when & then
            assertThatThrownBy(() -> event.update("새 제목", "새 설명", LOCATION,
                    EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT, 40))
                    .isInstanceOf(EventNotEditableException.class);
        }

        /**
         * EVT-054: 정원 0으로 수정 불가
         */
        @Test
        @DisplayName("[EVT-054] 정원을 0으로 수정 시 예외 발생")
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
