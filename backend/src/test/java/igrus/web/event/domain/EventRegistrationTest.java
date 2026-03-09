package igrus.web.event.domain;

import igrus.web.event.exception.InvalidRegistrationStatusException;
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
 * EventRegistration 도메인 테스트.
 * 테스트 케이스 문서: docs/test-case/event/event-test-cases.md
 *
 * @see igrus.web.event.domain.EventRegistration
 */
@DisplayName("EventRegistration 도메인")
class EventRegistrationTest {

    private static final String TITLE = "테스트 행사";
    private static final String DESCRIPTION = "행사 설명입니다.";
    private static final String LOCATION = "동아리방";
    private static final Instant REGISTRATION_START_AT = Instant.now();
    private static final Instant REGISTRATION_END_AT = Instant.now().plus(7, ChronoUnit.DAYS);
    private static final Instant EVENT_START_AT = Instant.now().plus(14, ChronoUnit.DAYS);
    private static final Instant EVENT_END_AT = Instant.now().plus(15, ChronoUnit.DAYS);
    private static final Integer CAPACITY = 30;

    @Nested
    @DisplayName("EventRegistration.create 정적 팩토리 메서드")
    class CreateRegistrationTest {

        /**
         * REG-001: 선착순 행사 신청
         */
        @Test
        @DisplayName("[REG-001] 선착순 행사 신청 시 REGISTERED 상태로 생성")
        void create_WithAutoApproveEvent_ReturnsRegisteredStatus() {
            // given
            Event event = createEvent(EventRegistrationType.AUTO_APPROVE);
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

        /**
         * REG-002: 선발제 행사 신청
         */
        @Test
        @DisplayName("[REG-002] 선발제 행사 신청 시 WAITING 상태로 생성")
        void create_WithManualApproveEvent_ReturnsWaitingStatus() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");

            // when
            EventRegistration registration = EventRegistration.create(event, user);

            // then
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.WAITING);
            assertThat(registration.isWaiting()).isTrue();
            assertThat(registration.isActive()).isFalse();
        }

        /**
         * REG-003: 신청 시 registeredAt 설정
         */
        @Test
        @DisplayName("[REG-003] 신청 시 registeredAt이 설정됨")
        void create_SetsRegisteredAt() {
            // given
            Event event = createEvent(EventRegistrationType.AUTO_APPROVE);
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

        /**
         * REG-010: WAITING→APPROVED 승인
         */
        @Test
        @DisplayName("[REG-010] WAITING 상태에서 approve 호출 시 APPROVED로 변경")
        void approve_FromWaiting_ChangesToApproved() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
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

        /**
         * REG-011: WAITING→REJECTED 거절
         */
        @Test
        @DisplayName("[REG-011] WAITING 상태에서 reject 호출 시 REJECTED로 변경")
        void reject_FromWaiting_ChangesToRejected() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);

            // when
            registration.reject();

            // then
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.REJECTED);
            assertThat(registration.isActive()).isFalse();
        }

        /**
         * REG-012: REGISTERED→CANCELED 취소
         */
        @Test
        @DisplayName("[REG-012] REGISTERED 상태에서 cancel 호출 시 CANCELED로 변경")
        void cancel_FromRegistered_ChangesToCanceled() {
            // given
            Event event = createEvent(EventRegistrationType.AUTO_APPROVE);
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

        /**
         * REG-013: APPROVED→CANCELED 취소
         */
        @Test
        @DisplayName("[REG-013] APPROVED 상태에서 cancel 호출 시 CANCELED로 변경")
        void cancel_FromApproved_ChangesToCanceled() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.approve();

            // when
            registration.cancel();

            // then
            assertThat(registration.isCanceled()).isTrue();
        }

        /**
         * REG-014: WAITING→CANCELED 취소
         */
        @Test
        @DisplayName("[REG-014] WAITING 상태에서 cancel 호출 시 CANCELED로 변경")
        void cancel_FromWaiting_ChangesToCanceled() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);

            // when
            registration.cancel();

            // then
            assertThat(registration.isCanceled()).isTrue();
        }

        /**
         * REG-045: REJECTED→CANCELED 취소
         */
        @Test
        @DisplayName("[REG-045] REJECTED 상태에서 cancel 호출 시 CANCELED로 변경")
        void cancel_FromRejected_ChangesToCanceled() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.reject();

            // when
            registration.cancel();

            // then
            assertThat(registration.isCanceled()).isTrue();
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.CANCELED);
        }

        /**
         * REG-046: CANCELED에서 cancel() 불가
         */
        @Test
        @DisplayName("[REG-046] CANCELED 상태에서 cancel 호출 시 InvalidRegistrationStatusException 발생")
        void cancel_FromCanceled_ThrowsException() {
            // given
            Event event = createEvent(EventRegistrationType.AUTO_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.cancel();

            // when & then
            assertThatThrownBy(() -> registration.cancel())
                    .isInstanceOf(InvalidRegistrationStatusException.class);
        }
    }

    @Nested
    @DisplayName("재신청 메서드")
    class ReRegisterTest {

        /**
         * REG-020: 선착순 행사 재신청
         */
        @Test
        @DisplayName("[REG-020] 선착순 행사 - 취소 후 재신청 시 REGISTERED 상태로 복원")
        void reRegister_AutoApproveAfterCancel_ChangesToRegistered() {
            // given
            Event event = createEvent(EventRegistrationType.AUTO_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.cancel();
            assertThat(registration.isCanceled()).isTrue();

            Instant canceledAt = registration.getRegisteredAt();

            // when
            registration.reRegister();

            // then
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.REGISTERED);
            assertThat(registration.isRegistered()).isTrue();
            assertThat(registration.isActive()).isTrue();
            assertThat(registration.getRegisteredAt()).isAfterOrEqualTo(canceledAt);
        }

        /**
         * REG-021: 선발제 행사 재신청
         */
        @Test
        @DisplayName("[REG-021] 선발제 행사 - 취소 후 재신청 시 WAITING 상태로 복원")
        void reRegister_ManualApproveAfterCancel_ChangesToWaiting() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.cancel();

            // when
            registration.reRegister();

            // then
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.WAITING);
            assertThat(registration.isWaiting()).isTrue();
            assertThat(registration.isActive()).isFalse();
        }

        /**
         * REG-022: REGISTERED 상태에서 재신청 불가
         */
        @Test
        @DisplayName("[REG-022] REGISTERED 상태에서 재신청 시 예외 발생")
        void reRegister_FromRegistered_ThrowsException() {
            // given
            Event event = createEvent(EventRegistrationType.AUTO_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            assertThat(registration.isRegistered()).isTrue();

            // when & then
            assertThatThrownBy(() -> registration.reRegister())
                    .isInstanceOf(InvalidRegistrationStatusException.class);
        }

        /**
         * REG-023: WAITING 상태에서 재신청 불가
         */
        @Test
        @DisplayName("[REG-023] WAITING 상태에서 재신청 시 예외 발생")
        void reRegister_FromWaiting_ThrowsException() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            assertThat(registration.isWaiting()).isTrue();

            // when & then
            assertThatThrownBy(() -> registration.reRegister())
                    .isInstanceOf(InvalidRegistrationStatusException.class);
        }

        /**
         * REG-024: APPROVED 상태에서 재신청 불가
         */
        @Test
        @DisplayName("[REG-024] APPROVED 상태에서 재신청 시 예외 발생")
        void reRegister_FromApproved_ThrowsException() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.approve();

            // when & then
            assertThatThrownBy(() -> registration.reRegister())
                    .isInstanceOf(InvalidRegistrationStatusException.class);
        }

        /**
         * REG-025: REJECTED 상태에서 재신청 불가
         */
        @Test
        @DisplayName("[REG-025] REJECTED 상태에서 재신청 시 예외 발생")
        void reRegister_FromRejected_ThrowsException() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.reject();

            // when & then
            assertThatThrownBy(() -> registration.reRegister())
                    .isInstanceOf(InvalidRegistrationStatusException.class);
        }

        /**
         * REG-026: 재신청 시 registeredAt 갱신
         */
        @Test
        @DisplayName("[REG-026] 재신청 시 registeredAt이 갱신됨")
        void reRegister_UpdatesRegisteredAt() throws InterruptedException {
            // given
            Event event = createEvent(EventRegistrationType.AUTO_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            Instant originalRegisteredAt = registration.getRegisteredAt();
            registration.cancel();

            Thread.sleep(10); // 시간 차이를 위해 대기

            // when
            registration.reRegister();

            // then
            assertThat(registration.getRegisteredAt()).isAfter(originalRegisteredAt);
        }
    }

    @Nested
    @DisplayName("되돌리기 메서드")
    class RevertToWaitingTest {

        /**
         * REG-040: APPROVED→WAITING 되돌리기 성공
         */
        @Test
        @DisplayName("[REG-040] APPROVED 상태에서 revertToWaiting 호출 시 WAITING으로 변경")
        void revertToWaiting_FromApproved_ChangesToWaiting() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.approve();
            assertThat(registration.isApproved()).isTrue();

            // when
            registration.revertToWaiting();

            // then
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.WAITING);
            assertThat(registration.isWaiting()).isTrue();
        }

        /**
         * REG-041: REJECTED→WAITING 되돌리기 성공
         */
        @Test
        @DisplayName("[REG-041] REJECTED 상태에서 revertToWaiting 호출 시 WAITING으로 변경")
        void revertToWaiting_FromRejected_ChangesToWaiting() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.reject();
            assertThat(registration.isRejected()).isTrue();

            // when
            registration.revertToWaiting();

            // then
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.WAITING);
        }

        /**
         * REG-042: REGISTERED→WAITING 되돌리기 불가
         */
        @Test
        @DisplayName("[REG-042] REGISTERED 상태에서 revertToWaiting 호출 시 예외 발생")
        void revertToWaiting_FromRegistered_ThrowsException() {
            // given
            Event event = createEvent(EventRegistrationType.AUTO_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            assertThat(registration.isRegistered()).isTrue();

            // when & then
            assertThatThrownBy(() -> registration.revertToWaiting())
                    .isInstanceOf(InvalidRegistrationStatusException.class);
        }

        /**
         * REG-043: WAITING→WAITING 되돌리기 불가
         */
        @Test
        @DisplayName("[REG-043] WAITING 상태에서 revertToWaiting 호출 시 예외 발생")
        void revertToWaiting_FromWaiting_ThrowsException() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            assertThat(registration.isWaiting()).isTrue();

            // when & then
            assertThatThrownBy(() -> registration.revertToWaiting())
                    .isInstanceOf(InvalidRegistrationStatusException.class);
        }

        /**
         * REG-044: CANCELED→WAITING 되돌리기 불가
         */
        @Test
        @DisplayName("[REG-044] CANCELED 상태에서 revertToWaiting 호출 시 예외 발생")
        void revertToWaiting_FromCanceled_ThrowsException() {
            // given
            Event event = createEvent(EventRegistrationType.AUTO_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.cancel();
            assertThat(registration.isCanceled()).isTrue();

            // when & then
            assertThatThrownBy(() -> registration.revertToWaiting())
                    .isInstanceOf(InvalidRegistrationStatusException.class);
        }
    }

    @Nested
    @DisplayName("조회 메서드")
    class QueryMethodsTest {

        /**
         * REG-030: REGISTERED는 isActive true
         */
        @Test
        @DisplayName("[REG-030] REGISTERED 상태는 isActive가 true")
        void isActive_WhenRegistered_ReturnsTrue() {
            // given
            Event event = createEvent(EventRegistrationType.AUTO_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);

            // then
            assertThat(registration.isActive()).isTrue();
        }

        /**
         * REG-031: APPROVED는 isActive true
         */
        @Test
        @DisplayName("[REG-031] APPROVED 상태는 isActive가 true")
        void isActive_WhenApproved_ReturnsTrue() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.approve();

            // then
            assertThat(registration.isActive()).isTrue();
        }

        /**
         * REG-032: WAITING은 isActive false
         */
        @Test
        @DisplayName("[REG-032] WAITING 상태는 isActive가 false")
        void isActive_WhenWaiting_ReturnsFalse() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);

            // then
            assertThat(registration.isActive()).isFalse();
        }

        /**
         * REG-033: REJECTED는 isActive false
         */
        @Test
        @DisplayName("[REG-033] REJECTED 상태는 isActive가 false")
        void isActive_WhenRejected_ReturnsFalse() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.reject();

            // then
            assertThat(registration.isActive()).isFalse();
        }

        /**
         * REG-035: REJECTED는 isRejected true
         */
        @Test
        @DisplayName("[REG-035] REJECTED 상태는 isRejected가 true")
        void isRejected_WhenRejected_ReturnsTrue() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.reject();

            // then
            assertThat(registration.isRejected()).isTrue();
        }

        /**
         * REG-034: CANCELED는 isActive false
         */
        @Test
        @DisplayName("[REG-034] CANCELED 상태는 isActive가 false")
        void isActive_WhenCanceled_ReturnsFalse() {
            // given
            Event event = createEvent(EventRegistrationType.AUTO_APPROVE);
            User user = createMockUser(2L, "신청자");
            EventRegistration registration = EventRegistration.create(event, user);
            registration.cancel();

            // then
            assertThat(registration.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("EventRegistration.createExternal 정적 팩토리 메서드")
    class CreateExternalRegistrationTest {

        /**
         * TC-031: 선착순 행사에 외부인 신청 시 즉시 REGISTERED 상태
         */
        @Test
        @DisplayName("[TC-031] 선착순 행사에 외부인 신청 시 REGISTERED 상태로 생성")
        void createExternal_WithAutoApproveEvent_ReturnsRegisteredStatus() {
            // given
            Event event = createEvent(EventRegistrationType.AUTO_APPROVE);

            // when
            EventRegistration registration = EventRegistration.createExternal(
                    event, "홍길동", "12345678", "01012345678", "컴퓨터공학과");

            // then
            assertThat(registration).isNotNull();
            assertThat(registration.getEvent()).isEqualTo(event);
            assertThat(registration.getUser()).isNull();
            assertThat(registration.getIsExternal()).isTrue();
            assertThat(registration.getExternalName()).isEqualTo("홍길동");
            assertThat(registration.getExternalStudentId()).isEqualTo("12345678");
            assertThat(registration.getExternalPhone()).isEqualTo("01012345678");
            assertThat(registration.getExternalDepartment()).isEqualTo("컴퓨터공학과");
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.REGISTERED);
            assertThat(registration.isRegistered()).isTrue();
            assertThat(registration.isActive()).isTrue();
            assertThat(registration.getRegisteredAt()).isNotNull();
        }

        /**
         * TC-033: 선발제 행사에 외부인 신청 시 WAITING 상태
         */
        @Test
        @DisplayName("[TC-033] 선발제 행사에 외부인 신청 시 WAITING 상태로 생성")
        void createExternal_WithManualApproveEvent_ReturnsWaitingStatus() {
            // given
            Event event = createEvent(EventRegistrationType.MANUAL_APPROVE);

            // when
            EventRegistration registration = EventRegistration.createExternal(
                    event, "홍길동", "12345678", "01012345678", "컴퓨터공학과");

            // then
            assertThat(registration).isNotNull();
            assertThat(registration.getEvent()).isEqualTo(event);
            assertThat(registration.getUser()).isNull();
            assertThat(registration.getIsExternal()).isTrue();
            assertThat(registration.getStatus()).isEqualTo(EventRegistrationStatus.WAITING);
            assertThat(registration.isWaiting()).isTrue();
            assertThat(registration.isActive()).isFalse();
        }

        /**
         * 회원 신청(create)과 외부인 신청(createExternal) isExternal 차이 검증
         */
        @Test
        @DisplayName("회원 신청 create()는 isExternal=false, 외부인 createExternal()는 isExternal=true")
        void create_vs_createExternal_IsExternalDifference() {
            // given
            Event event = createEvent(EventRegistrationType.AUTO_APPROVE);
            User user = createMockUser(2L, "회원");

            // when
            EventRegistration memberReg = EventRegistration.create(event, user);
            EventRegistration externalReg = EventRegistration.createExternal(
                    event, "외부인", "99999999", "01099999999", "경영학과");

            // then
            assertThat(memberReg.getIsExternal()).isFalse();
            assertThat(memberReg.getUser()).isEqualTo(user);
            assertThat(externalReg.getIsExternal()).isTrue();
            assertThat(externalReg.getUser()).isNull();
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
        return Event.create(operator, TITLE, DESCRIPTION, LOCATION,
                EVENT_START_AT, EVENT_END_AT, REGISTRATION_START_AT, REGISTRATION_END_AT,
                CAPACITY, type, (igrus.web.survey.domain.Survey) null);
    }
}
