package igrus.web.event.service;

import igrus.web.event.domain.*;
import igrus.web.event.domain.RegistrationStatus;
import igrus.web.event.dto.response.MyRegistrationResponse;
import igrus.web.event.dto.response.RegistrationListResponse;
import igrus.web.event.dto.response.RegistrationResponse;
import igrus.web.event.exception.*;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EventRegistrationService 테스트.
 * 테스트 케이스 문서: docs/test-case/event/event-test-cases.md
 *
 * @see igrus.web.event.service.EventRegistrationService
 */
@DisplayName("EventRegistrationService")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventRegistrationServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventRegistrationService eventRegistrationService;

    // 테스트 데이터
    private static final Long EVENT_ID = 1L;
    private static final Long USER_ID = 2L;
    private static final Long OPERATOR_ID = 3L;
    private static final Long REGISTRATION_ID = 10L;

    private User regularMember;
    private User associateMember;
    private User operator;
    private Event autoApproveEvent;
    private Event manualApproveEvent;

    @BeforeEach
    void setUp() {
        // 일반 회원 Mock
        regularMember = mock(User.class);
        when(regularMember.getId()).thenReturn(USER_ID);
        when(regularMember.isAssociate()).thenReturn(false);
        when(regularMember.isOperatorOrAbove()).thenReturn(false);

        // 준회원 Mock
        associateMember = mock(User.class);
        when(associateMember.getId()).thenReturn(4L);
        when(associateMember.isAssociate()).thenReturn(true);

        // 운영진 Mock
        operator = mock(User.class);
        when(operator.getId()).thenReturn(OPERATOR_ID);
        when(operator.isAssociate()).thenReturn(false);
        when(operator.isOperatorOrAbove()).thenReturn(true);

        // 자동 승인 행사 Mock
        autoApproveEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE);

        // 수동 승인 행사 Mock
        manualApproveEvent = createMockEvent(EventRegistrationType.MANUAL_APPROVE);
    }

    private Event createMockEvent(EventRegistrationType type) {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn(EVENT_ID);
        when(event.getRegistrationStatus()).thenReturn(RegistrationStatus.OPEN);
        when(event.getEventStatus()).thenReturn(EventStatus.UPCOMING);
        when(event.getVisibility()).thenReturn(EventVisibility.PUBLISHED);
        when(event.getRegistrationStartAt()).thenReturn(Instant.now().minus(1, ChronoUnit.DAYS));
        when(event.getRegistrationEndAt()).thenReturn(Instant.now().plus(7, ChronoUnit.DAYS));
        when(event.isAutoApprove()).thenReturn(type == EventRegistrationType.AUTO_APPROVE);
        when(event.isManualApprove()).thenReturn(type == EventRegistrationType.MANUAL_APPROVE);
        when(event.isFull()).thenReturn(false);
        return event;
    }

    @Nested
    @DisplayName("registerEvent - 행사 신청")
    class RegisterEventTest {

        /**
         * SVC-001: 정회원 선착순 행사 신청
         */
        @Test
        @DisplayName("[SVC-001] 정회원이 자동 승인 행사에 신청하면 REGISTERED 상태로 신청됨")
        void registerEvent_MemberToAutoApprove_ReturnsRegisteredStatus() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);

            EventRegistration savedRegistration = mock(EventRegistration.class);
            when(savedRegistration.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
            when(savedRegistration.getRegisteredAt()).thenReturn(Instant.now());
            when(savedRegistration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(savedRegistration);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(eventRepository).incrementCurrentCountIfAvailable(EVENT_ID);
            verify(eventRegistrationRepository).save(any(EventRegistration.class));
        }

        /**
         * SVC-002: 정회원 선발제 행사 신청
         */
        @Test
        @DisplayName("[SVC-002] 정회원이 수동 승인 행사에 신청하면 WAITING 상태로 신청됨")
        void registerEvent_MemberToManualApprove_ReturnsWaitingStatus() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            EventRegistration savedRegistration = mock(EventRegistration.class);
            when(savedRegistration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);
            when(savedRegistration.getRegisteredAt()).thenReturn(Instant.now());
            when(savedRegistration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(savedRegistration);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(eventRepository, never()).incrementCurrentCountIfAvailable(any());
            verify(eventRegistrationRepository).save(any(EventRegistration.class));
        }

        /**
         * SVC-003: 준회원 신청 거부
         */
        @Test
        @DisplayName("[SVC-003] 준회원이 신청하면 AssociateMemberNotAllowedException 발생")
        void registerEvent_AssociateMember_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(4L)).thenReturn(Optional.of(associateMember));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, 4L))
                    .isInstanceOf(AssociateMemberNotAllowedException.class);
        }

        /**
         * SVC-004: 존재하지 않는 행사 신청
         */
        @Test
        @DisplayName("[SVC-004] 존재하지 않는 행사에 신청하면 EventNotFoundException 발생")
        void registerEvent_EventNotFound_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(999L, USER_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        /**
         * SVC-005: 존재하지 않는 사용자 신청
         */
        @Test
        @DisplayName("[SVC-005] 존재하지 않는 사용자가 신청하면 UserNotFoundException 발생")
        void registerEvent_UserNotFound_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, 999L))
                    .isInstanceOf(UserNotFoundException.class);
        }

        /**
         * SVC-006: 중복 신청 거부
         */
        @Test
        @DisplayName("[SVC-006] 이미 신청한 사용자가 재신청하면 AlreadyRegisteredException 발생")
        void registerEvent_AlreadyRegistered_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            EventRegistration existingRegistration = mock(EventRegistration.class);
            when(existingRegistration.isCanceled()).thenReturn(false);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(existingRegistration));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID))
                    .isInstanceOf(AlreadyRegisteredException.class);
        }

        /**
         * SVC-007: OPEN 아닌 행사 신청 거부
         */
        @Test
        @DisplayName("[SVC-007] 행사가 OPEN 상태가 아니면 EventNotOpenException 발생")
        void registerEvent_EventNotOpen_ThrowsException() {
            // given
            when(autoApproveEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.NOT_STARTED);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID))
                    .isInstanceOf(EventNotOpenException.class);
        }

        /**
         * SVC-008: 신청 기간 전 신청 거부
         */
        @Test
        @DisplayName("[SVC-008] 신청 기간 전에 신청하면 EventNotInRegistrationPeriodException 발생")
        void registerEvent_BeforeRegistrationPeriod_ThrowsException() {
            // given
            when(autoApproveEvent.getRegistrationStartAt()).thenReturn(Instant.now().plus(1, ChronoUnit.DAYS));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID))
                    .isInstanceOf(EventNotInRegistrationPeriodException.class);
        }

        /**
         * SVC-009: 신청 기간 후 신청 거부
         */
        @Test
        @DisplayName("[SVC-009] 신청 기간 후에 신청하면 EventNotInRegistrationPeriodException 발생")
        void registerEvent_AfterRegistrationPeriod_ThrowsException() {
            // given
            when(autoApproveEvent.getRegistrationEndAt()).thenReturn(Instant.now().minus(1, ChronoUnit.DAYS));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID))
                    .isInstanceOf(EventNotInRegistrationPeriodException.class);
        }

        /**
         * SVC-010: 정원 초과 선착순 신청 거부 (원자적 UPDATE 실패)
         */
        @Test
        @DisplayName("[SVC-010] 정원이 찬 선착순 행사에 신청하면 EventCapacityFullException 발생")
        void registerEvent_CapacityFull_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(0); // 원자적 UPDATE 실패

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID))
                    .isInstanceOf(EventCapacityFullException.class);
        }

        /**
         * SVC-011: 취소된 신청 재신청
         */
        @Test
        @DisplayName("[SVC-011] 취소된 신청이 있으면 재신청 처리됨")
        void registerEvent_CanceledRegistrationExists_ReRegisters() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);

            EventRegistration canceledRegistration = mock(EventRegistration.class);
            when(canceledRegistration.isCanceled()).thenReturn(true);
            when(canceledRegistration.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
            when(canceledRegistration.getRegisteredAt()).thenReturn(Instant.now());
            when(canceledRegistration.getId()).thenReturn(REGISTRATION_ID);
            when(canceledRegistration.getUser()).thenReturn(regularMember);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(canceledRegistration));
            when(eventRegistrationRepository.existsOverlappingRegistration(eq(USER_ID), any(), any(), any()))
                    .thenReturn(false);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(canceledRegistration).reRegister();
            verify(eventRepository).incrementCurrentCountIfAvailable(EVENT_ID);
        }
    }

    @Nested
    @DisplayName("cancelRegistration - 신청 취소")
    class CancelRegistrationTest {

        /**
         * SVC-020: REGISTERED 상태 취소
         */
        @Test
        @DisplayName("[SVC-020] REGISTERED 상태의 신청을 취소하면 카운트 감소")
        void cancelRegistration_FromRegistered_DecrementsCount() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(eventRepository.decrementCurrentCount(EVENT_ID)).thenReturn(1);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.isCanceled()).thenReturn(false);
            when(registration.isActive()).thenReturn(true);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.CANCELED);
            when(registration.getRegisteredAt()).thenReturn(Instant.now());
            when(registration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(registration));

            // when
            RegistrationResponse response = eventRegistrationService.cancelRegistration(EVENT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(registration).cancel();
            verify(eventRepository).decrementCurrentCount(EVENT_ID);
        }

        /**
         * SVC-021: WAITING 상태 취소
         */
        @Test
        @DisplayName("[SVC-021] WAITING 상태의 신청을 취소하면 카운트 감소 없음")
        void cancelRegistration_FromWaiting_NoCountChange() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.isCanceled()).thenReturn(false);
            when(registration.isActive()).thenReturn(false); // WAITING은 isActive() = false
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.CANCELED);
            when(registration.getRegisteredAt()).thenReturn(Instant.now());
            when(registration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(registration));

            // when
            RegistrationResponse response = eventRegistrationService.cancelRegistration(EVENT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(registration).cancel();
            verify(eventRepository, never()).decrementCurrentCount(any());
        }

        /**
         * SVC-022: 이미 취소된 신청 취소
         */
        @Test
        @DisplayName("[SVC-022] 이미 취소된 신청을 취소하면 AlreadyCanceledException 발생")
        void cancelRegistration_AlreadyCanceled_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.isCanceled()).thenReturn(true);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(registration));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.cancelRegistration(EVENT_ID, USER_ID))
                    .isInstanceOf(AlreadyCanceledException.class);
        }

        /**
         * SVC-023: 존재하지 않는 신청 취소
         */
        @Test
        @DisplayName("[SVC-023] 존재하지 않는 신청을 취소하면 EventRegistrationNotFoundException 발생")
        void cancelRegistration_NotFound_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.cancelRegistration(EVENT_ID, USER_ID))
                    .isInstanceOf(EventRegistrationNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getMyRegistrations - 내 신청 목록 조회")
    class GetMyRegistrationsTest {

        /**
         * SVC-040: 내 신청 목록 조회
         */
        @Test
        @DisplayName("[SVC-040] 사용자의 신청 목록을 반환함")
        void getMyRegistrations_ReturnsUserRegistrations() {
            // given
            EventRegistration reg1 = mock(EventRegistration.class);
            EventRegistration reg2 = mock(EventRegistration.class);

            Event event1 = mock(Event.class);
            when(event1.getId()).thenReturn(1L);
            when(event1.getTitle()).thenReturn("행사1");
            when(event1.getEventStartAt()).thenReturn(Instant.now().plus(7, ChronoUnit.DAYS));
            when(event1.getEventEndAt()).thenReturn(Instant.now().plus(8, ChronoUnit.DAYS));
            when(event1.getLocation()).thenReturn("장소1");
            when(event1.getRegistrationStatus()).thenReturn(RegistrationStatus.OPEN);

            Event event2 = mock(Event.class);
            when(event2.getId()).thenReturn(2L);
            when(event2.getTitle()).thenReturn("행사2");
            when(event2.getEventStartAt()).thenReturn(Instant.now().plus(14, ChronoUnit.DAYS));
            when(event2.getEventEndAt()).thenReturn(Instant.now().plus(15, ChronoUnit.DAYS));
            when(event2.getLocation()).thenReturn("장소2");
            when(event2.getEventStatus()).thenReturn(EventStatus.UPCOMING);

            when(reg1.getEvent()).thenReturn(event1);
            when(reg1.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
            when(reg1.getRegisteredAt()).thenReturn(Instant.now());

            when(reg2.getEvent()).thenReturn(event2);
            when(reg2.getStatus()).thenReturn(EventRegistrationStatus.WAITING);
            when(reg2.getRegisteredAt()).thenReturn(Instant.now());

            when(eventRegistrationRepository.findByUserId(USER_ID)).thenReturn(List.of(reg1, reg2));

            // when
            List<MyRegistrationResponse> responses = eventRegistrationService.getMyRegistrations(USER_ID);

            // then
            assertThat(responses).hasSize(2);
        }

        /**
         * SVC-041: 신청 내역 없을 때 빈 목록
         */
        @Test
        @DisplayName("[SVC-041] 신청 내역이 없으면 빈 목록 반환")
        void getMyRegistrations_NoRegistrations_ReturnsEmptyList() {
            // given
            when(eventRegistrationRepository.findByUserId(USER_ID)).thenReturn(List.of());

            // when
            List<MyRegistrationResponse> responses = eventRegistrationService.getMyRegistrations(USER_ID);

            // then
            assertThat(responses).isEmpty();
        }
    }

    @Nested
    @DisplayName("getRegistrationList - 신청자 목록 조회")
    class GetRegistrationListTest {

        /**
         * SVC-042: 운영진 신청자 목록 조회
         */
        @Test
        @DisplayName("[SVC-042] 운영진이 조회하면 신청자 목록 반환")
        void getRegistrationList_ByOperator_ReturnsList() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            EventRegistration reg = mock(EventRegistration.class);
            User regUser = mock(User.class);
            when(regUser.getId()).thenReturn(USER_ID);
            when(regUser.getName()).thenReturn("신청자");
            when(regUser.getStudentId()).thenReturn("12345678");
            when(reg.getId()).thenReturn(REGISTRATION_ID);
            when(reg.getUser()).thenReturn(regUser);
            when(reg.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
            when(reg.getRegisteredAt()).thenReturn(Instant.now());

            Page<EventRegistration> page = new PageImpl<>(List.of(reg));
            when(eventRegistrationRepository.findByEventId(eq(EVENT_ID), any(Pageable.class))).thenReturn(page);

            // when
            Page<RegistrationListResponse> responses = eventRegistrationService.getRegistrationList(EVENT_ID, OPERATOR_ID, Pageable.unpaged());

            // then
            assertThat(responses.getContent()).hasSize(1);
        }

        /**
         * SVC-043: 일반 회원 신청자 목록 조회 거부
         */
        @Test
        @DisplayName("[SVC-043] 일반 회원이 조회하면 OperatorPermissionRequiredException 발생")
        void getRegistrationList_ByRegularMember_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.getRegistrationList(EVENT_ID, USER_ID, Pageable.unpaged()))
                    .isInstanceOf(OperatorPermissionRequiredException.class);
        }
    }

    @Nested
    @DisplayName("approveRegistration - 신청 승인")
    class ApproveRegistrationTest {

        /**
         * SVC-030: 운영진 WAITING 승인
         */
        @Test
        @DisplayName("[SVC-030] 운영진이 WAITING 상태 신청을 승인하면 APPROVED로 변경")
        void approveRegistration_ValidCase_ChangesToApproved() {
            // given
            User applicant = mock(User.class);
            when(applicant.getId()).thenReturn(USER_ID);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getId()).thenReturn(REGISTRATION_ID);
            when(registration.getEvent()).thenReturn(manualApproveEvent);
            when(registration.getUser()).thenReturn(applicant);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);
            when(registration.getRegisteredAt()).thenReturn(Instant.now());

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRepository.incrementCurrentCountForApproval(EVENT_ID)).thenReturn(1);
            when(eventRegistrationRepository.existsOverlappingRegistration(eq(USER_ID), any(), any(), any()))
                    .thenReturn(false);

            // Mock approve 후 상태 변경
            doAnswer(invocation -> {
                when(registration.getStatus()).thenReturn(EventRegistrationStatus.APPROVED);
                return null;
            }).when(registration).approve();

            // when
            RegistrationResponse response = eventRegistrationService.approveRegistration(REGISTRATION_ID, OPERATOR_ID);

            // then
            assertThat(response).isNotNull();
            verify(registration).approve();
            verify(eventRepository).incrementCurrentCountForApproval(EVENT_ID);
            verify(eventRegistrationRepository).existsOverlappingRegistration(eq(USER_ID), any(), any(), any());
        }

        /**
         * SVC-030-2: 승인 시 시간 겹침 검증
         */
        @Test
        @DisplayName("[SVC-030-2] 승인 시 시간이 겹치는 확정 신청이 있으면 EventTimeOverlapException 발생")
        void approveRegistration_TimeOverlap_ThrowsException() {
            // given
            User applicant = mock(User.class);
            when(applicant.getId()).thenReturn(USER_ID);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(manualApproveEvent);
            when(registration.getUser()).thenReturn(applicant);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRegistrationRepository.existsOverlappingRegistration(eq(USER_ID), any(), any(), any()))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.approveRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(EventTimeOverlapException.class);
        }

        /**
         * SVC-031: 자동 승인 행사 승인 거부
         */
        @Test
        @DisplayName("[SVC-031] 자동 승인 행사에서 승인하면 NotManualApproveEventException 발생")
        void approveRegistration_AutoApproveEvent_ThrowsException() {
            // given
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(autoApproveEvent);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.approveRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(NotManualApproveEventException.class);
        }

        /**
         * SVC-032: WAITING 아닌 상태 승인 거부
         */
        @Test
        @DisplayName("[SVC-032] WAITING이 아닌 상태를 승인하면 InvalidRegistrationStatusException 발생")
        void approveRegistration_NotWaitingStatus_ThrowsException() {
            // given
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(manualApproveEvent);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.APPROVED); // 이미 승인됨

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.approveRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(InvalidRegistrationStatusException.class);
        }

        /**
         * SVC-033: 정원 초과 상태에서 승인 거부 (원자적 UPDATE 실패)
         */
        @Test
        @DisplayName("[SVC-033] 정원이 찬 상태에서 승인하면 EventCapacityFullException 발생")
        void approveRegistration_CapacityFull_ThrowsException() {
            // given
            User applicant = mock(User.class);
            when(applicant.getId()).thenReturn(USER_ID);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(manualApproveEvent);
            when(registration.getUser()).thenReturn(applicant);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRegistrationRepository.existsOverlappingRegistration(eq(USER_ID), any(), any(), any()))
                    .thenReturn(false);
            when(eventRepository.incrementCurrentCountForApproval(EVENT_ID)).thenReturn(0); // 원자적 UPDATE 실패

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.approveRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(EventCapacityFullException.class);
        }

        /**
         * SVC-034: 일반 회원 승인 거부
         */
        @Test
        @DisplayName("[SVC-034] 일반 회원이 승인하면 OperatorPermissionRequiredException 발생")
        void approveRegistration_ByRegularMember_ThrowsException() {
            // given
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(manualApproveEvent);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.approveRegistration(REGISTRATION_ID, USER_ID))
                    .isInstanceOf(OperatorPermissionRequiredException.class);
        }
    }

    @Nested
    @DisplayName("rejectRegistration - 신청 거절")
    class RejectRegistrationTest {

        /**
         * SVC-035: 운영진 WAITING 거절
         */
        @Test
        @DisplayName("[SVC-035] 운영진이 WAITING 상태 신청을 거절하면 REJECTED로 변경")
        void rejectRegistration_ValidCase_ChangesToRejected() {
            // given
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getId()).thenReturn(REGISTRATION_ID);
            when(registration.getEvent()).thenReturn(manualApproveEvent);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);
            when(registration.getRegisteredAt()).thenReturn(Instant.now());

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // Mock reject 후 상태 변경
            doAnswer(invocation -> {
                when(registration.getStatus()).thenReturn(EventRegistrationStatus.REJECTED);
                return null;
            }).when(registration).reject();

            // when
            RegistrationResponse response = eventRegistrationService.rejectRegistration(REGISTRATION_ID, OPERATOR_ID);

            // then
            assertThat(response).isNotNull();
            verify(registration).reject();
            verify(manualApproveEvent, never()).decrementCurrentCount(any(Instant.class)); // 거절은 카운트 변경 없음
        }

        /**
         * SVC-036: 자동 승인 행사 거절 거부
         */
        @Test
        @DisplayName("[SVC-036] 자동 승인 행사에서 거절하면 NotManualApproveEventException 발생")
        void rejectRegistration_AutoApproveEvent_ThrowsException() {
            // given
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(autoApproveEvent);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.rejectRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(NotManualApproveEventException.class);
        }

        /**
         * SVC-037: WAITING 아닌 상태 거절 거부
         */
        @Test
        @DisplayName("[SVC-037] WAITING이 아닌 상태를 거절하면 InvalidRegistrationStatusException 발생")
        void rejectRegistration_NotWaitingStatus_ThrowsException() {
            // given
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(manualApproveEvent);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.CANCELED);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.rejectRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(InvalidRegistrationStatusException.class);
        }

        /**
         * SVC-038: 일반 회원 거절 거부
         */
        @Test
        @DisplayName("[SVC-038] 일반 회원이 거절하면 OperatorPermissionRequiredException 발생")
        void rejectRegistration_ByRegularMember_ThrowsException() {
            // given
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(manualApproveEvent);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.rejectRegistration(REGISTRATION_ID, USER_ID))
                    .isInstanceOf(OperatorPermissionRequiredException.class);
        }
    }

    @Nested
    @DisplayName("revertRegistration - 승인/거절 되돌리기")
    class RevertRegistrationTest {

        /**
         * SVC-050: APPROVED 상태 되돌리기 (카운트 감소)
         */
        @Test
        @DisplayName("[SVC-050] APPROVED 상태 신청을 되돌리면 WAITING으로 변경되고 카운트 감소")
        void revertRegistration_FromApproved_RevertsToWaitingAndDecrementsCount() {
            // given
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getId()).thenReturn(REGISTRATION_ID);
            when(registration.getEvent()).thenReturn(manualApproveEvent);
            when(registration.isApproved()).thenReturn(true);
            when(registration.isRejected()).thenReturn(false);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);
            when(registration.getRegisteredAt()).thenReturn(Instant.now());

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRepository.decrementCurrentCount(EVENT_ID)).thenReturn(1);

            // when
            RegistrationResponse response = eventRegistrationService.revertRegistration(REGISTRATION_ID, OPERATOR_ID);

            // then
            assertThat(response).isNotNull();
            verify(registration).revertToWaiting();
            verify(eventRegistrationRepository).saveAndFlush(registration);
            verify(eventRepository).decrementCurrentCount(EVENT_ID);
        }

        /**
         * SVC-051: REJECTED 상태 되돌리기 (카운트 변경 없음)
         */
        @Test
        @DisplayName("[SVC-051] REJECTED 상태 신청을 되돌리면 WAITING으로 변경되고 카운트 변경 없음")
        void revertRegistration_FromRejected_RevertsToWaitingWithoutCountChange() {
            // given
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getId()).thenReturn(REGISTRATION_ID);
            when(registration.getEvent()).thenReturn(manualApproveEvent);
            when(registration.isApproved()).thenReturn(false);
            when(registration.isRejected()).thenReturn(true);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);
            when(registration.getRegisteredAt()).thenReturn(Instant.now());

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when
            RegistrationResponse response = eventRegistrationService.revertRegistration(REGISTRATION_ID, OPERATOR_ID);

            // then
            assertThat(response).isNotNull();
            verify(registration).revertToWaiting();
            verify(eventRepository, never()).decrementCurrentCount(any());
        }

        /**
         * SVC-052: WAITING/REGISTERED/CANCELED 상태 되돌리기 거부
         */
        @Test
        @DisplayName("[SVC-052] APPROVED/REJECTED가 아닌 상태를 되돌리면 InvalidRegistrationStatusException 발생")
        void revertRegistration_InvalidStatus_ThrowsException() {
            // given
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(manualApproveEvent);
            when(registration.isApproved()).thenReturn(false);
            when(registration.isRejected()).thenReturn(false);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.revertRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(InvalidRegistrationStatusException.class);
        }

        /**
         * SVC-055: ONGOING 행사 되돌리기 거부
         */
        @Test
        @DisplayName("[SVC-055] ONGOING 상태 행사에서 되돌리면 EventNotEditableException 발생")
        void revertRegistration_OngoingEvent_ThrowsException() {
            // given
            Event ongoingEvent = mock(Event.class);
            when(ongoingEvent.getId()).thenReturn(EVENT_ID);
            when(ongoingEvent.getEventStatus()).thenReturn(EventStatus.ONGOING);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(ongoingEvent);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(ongoingEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.revertRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(EventNotEditableException.class);
        }

        /**
         * SVC-056: COMPLETED 행사 되돌리기 거부
         */
        @Test
        @DisplayName("[SVC-056] COMPLETED 상태 행사에서 되돌리면 EventNotEditableException 발생")
        void revertRegistration_CompletedEvent_ThrowsException() {
            // given
            Event completedEvent = mock(Event.class);
            when(completedEvent.getId()).thenReturn(EVENT_ID);
            when(completedEvent.getEventStatus()).thenReturn(EventStatus.COMPLETED);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(completedEvent);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(completedEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.revertRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(EventNotEditableException.class);
        }

        /**
         * SVC-053: 자동 승인 행사 되돌리기 거부
         */
        @Test
        @DisplayName("[SVC-053] 자동 승인 행사에서 되돌리면 NotManualApproveEventException 발생")
        void revertRegistration_AutoApproveEvent_ThrowsException() {
            // given
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(autoApproveEvent);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.revertRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(NotManualApproveEventException.class);
        }

        /**
         * SVC-054: 일반 회원 되돌리기 거부
         */
        @Test
        @DisplayName("[SVC-054] 일반 회원이 되돌리면 OperatorPermissionRequiredException 발생")
        void revertRegistration_ByRegularMember_ThrowsException() {
            // given
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(manualApproveEvent);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.revertRegistration(REGISTRATION_ID, USER_ID))
                    .isInstanceOf(OperatorPermissionRequiredException.class);
        }
    }

    @Nested
    @DisplayName("시간 겹침 검증")
    class TimeOverlapValidationTest {

        /**
         * SVC-060: 시간 겹치는 행사 신청 거부
         */
        @Test
        @DisplayName("[SVC-060] 시간이 겹치는 다른 행사에 신청하면 EventTimeOverlapException 발생")
        void registerEvent_TimeOverlap_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsOverlappingRegistration(eq(USER_ID), any(), any(), any()))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID))
                    .isInstanceOf(EventTimeOverlapException.class);
        }

        /**
         * SVC-061: 시간 안 겹치는 행사 신청 성공
         */
        @Test
        @DisplayName("[SVC-061] 시간이 겹치지 않으면 정상 신청")
        void registerEvent_NoTimeOverlap_Succeeds() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsOverlappingRegistration(eq(USER_ID), any(), any(), any()))
                    .thenReturn(false);
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);

            EventRegistration savedRegistration = mock(EventRegistration.class);
            when(savedRegistration.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
            when(savedRegistration.getRegisteredAt()).thenReturn(Instant.now());
            when(savedRegistration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(savedRegistration);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(eventRegistrationRepository).existsOverlappingRegistration(eq(USER_ID), any(), any(), any());
        }

        /**
         * SVC-062: 재신청 시 시간 겹침 거부
         */
        @Test
        @DisplayName("[SVC-062] 재신청 시 시간이 겹치면 EventTimeOverlapException 발생")
        void reRegister_TimeOverlap_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            EventRegistration canceledRegistration = mock(EventRegistration.class);
            when(canceledRegistration.isCanceled()).thenReturn(true);
            when(canceledRegistration.getUser()).thenReturn(regularMember);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(canceledRegistration));
            when(eventRegistrationRepository.existsOverlappingRegistration(eq(USER_ID), any(), any(), any()))
                    .thenReturn(true);

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID))
                    .isInstanceOf(EventTimeOverlapException.class);
        }
    }

    @Nested
    @DisplayName("2축 모델 연동 — 서비스 테스트")
    class TwoAxisModelIntegrationTest {

        /**
         * SVC-REG-063: registrationStatus=OPEN, eventStatus=ONGOING에서 신청 성공
         */
        @Test
        @DisplayName("[SVC-REG-063] registrationStatus=OPEN, eventStatus=ONGOING에서 신청 성공")
        void registerEvent_OpenAndOngoing_Succeeds() {
            // given: 겹침 기간 (reg=OPEN, event=ONGOING)
            Event ongoingOpenEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE);
            when(ongoingOpenEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.OPEN);
            when(ongoingOpenEvent.getEventStatus()).thenReturn(EventStatus.ONGOING);

            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(ongoingOpenEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsOverlappingRegistration(eq(USER_ID), any(), any(), any()))
                    .thenReturn(false);
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);

            EventRegistration savedRegistration = mock(EventRegistration.class);
            when(savedRegistration.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
            when(savedRegistration.getRegisteredAt()).thenReturn(Instant.now());
            when(savedRegistration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(savedRegistration);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(eventRepository).incrementCurrentCountIfAvailable(EVENT_ID);
            verify(eventRegistrationRepository).save(any(EventRegistration.class));
        }

        /**
         * SVC-REG-064: eventStatus=CANCELED 행사 신청 거부 (REG-INV-13)
         */
        @Test
        @DisplayName("[SVC-REG-064] eventStatus=CANCELED 행사에 신청하면 EventNotOpenException 발생")
        void registerEvent_CanceledEvent_ThrowsEventNotOpenException() {
            // given: eventStatus=CANCELED → registrationStatus=CLOSED
            Event canceledEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE);
            when(canceledEvent.getEventStatus()).thenReturn(EventStatus.CANCELED);
            when(canceledEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.CLOSED);

            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(canceledEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID))
                    .isInstanceOf(EventNotOpenException.class);
        }

        /**
         * SVC-REG-065: eventStatus=CANCELED 행사 재신청 거부 (REG-INV-13)
         */
        @Test
        @DisplayName("[SVC-REG-065] eventStatus=CANCELED 행사에 재신청하면 EventNotOpenException 발생")
        void registerEvent_CanceledEventReRegister_ThrowsEventNotOpenException() {
            // given: eventStatus=CANCELED, 취소된 신청 존재
            Event canceledEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE);
            when(canceledEvent.getEventStatus()).thenReturn(EventStatus.CANCELED);
            when(canceledEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.CLOSED);

            EventRegistration canceledRegistration = mock(EventRegistration.class);
            when(canceledRegistration.isCanceled()).thenReturn(true);
            when(canceledRegistration.getUser()).thenReturn(regularMember);

            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(canceledEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(canceledRegistration));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID))
                    .isInstanceOf(EventNotOpenException.class);
        }

        /**
         * SVC-REG-066: eventStatus=COMPLETED 행사 승인 거부 (REG-INV-14)
         */
        @Test
        @DisplayName("[SVC-REG-066] eventStatus=COMPLETED 행사에서 승인하면 EventNotEditableException 발생")
        void approveRegistration_CompletedEvent_ThrowsEventNotEditableException() {
            // given
            Event completedEvent = mock(Event.class);
            when(completedEvent.getId()).thenReturn(EVENT_ID);
            when(completedEvent.getEventStatus()).thenReturn(EventStatus.COMPLETED);
            when(completedEvent.isManualApprove()).thenReturn(true);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(completedEvent);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(completedEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.approveRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(EventNotEditableException.class);
        }

        /**
         * SVC-REG-067: eventStatus=CANCELED 행사 승인 거부 (REG-INV-14)
         */
        @Test
        @DisplayName("[SVC-REG-067] eventStatus=CANCELED 행사에서 승인하면 EventNotEditableException 발생")
        void approveRegistration_CanceledEvent_ThrowsEventNotEditableException() {
            // given
            Event canceledEvent = mock(Event.class);
            when(canceledEvent.getId()).thenReturn(EVENT_ID);
            when(canceledEvent.getEventStatus()).thenReturn(EventStatus.CANCELED);
            when(canceledEvent.isManualApprove()).thenReturn(true);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(canceledEvent);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(canceledEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.approveRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(EventNotEditableException.class);
        }

        /**
         * SVC-REG-068: eventStatus=COMPLETED 행사 거절 거부 (REG-INV-14)
         */
        @Test
        @DisplayName("[SVC-REG-068] eventStatus=COMPLETED 행사에서 거절하면 EventNotEditableException 발생")
        void rejectRegistration_CompletedEvent_ThrowsEventNotEditableException() {
            // given
            Event completedEvent = mock(Event.class);
            when(completedEvent.getId()).thenReturn(EVENT_ID);
            when(completedEvent.getEventStatus()).thenReturn(EventStatus.COMPLETED);
            when(completedEvent.isManualApprove()).thenReturn(true);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(completedEvent);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(completedEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.rejectRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(EventNotEditableException.class);
        }

        /**
         * SVC-REG-069: eventStatus=CANCELED 행사 거절 거부 (REG-INV-14)
         */
        @Test
        @DisplayName("[SVC-REG-069] eventStatus=CANCELED 행사에서 거절하면 EventNotEditableException 발생")
        void rejectRegistration_CanceledEvent_ThrowsEventNotEditableException() {
            // given
            Event canceledEvent = mock(Event.class);
            when(canceledEvent.getId()).thenReturn(EVENT_ID);
            when(canceledEvent.getEventStatus()).thenReturn(EventStatus.CANCELED);
            when(canceledEvent.isManualApprove()).thenReturn(true);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(canceledEvent);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(canceledEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.rejectRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(EventNotEditableException.class);
        }

        /**
         * SVC-REG-070: eventStatus=CANCELED 행사 되돌리기 거부 (REG-INV-10)
         */
        @Test
        @DisplayName("[SVC-REG-070] eventStatus=CANCELED 행사에서 되돌리면 EventNotEditableException 발생")
        void revertRegistration_CanceledEvent_ThrowsEventNotEditableException() {
            // given
            Event canceledEvent = mock(Event.class);
            when(canceledEvent.getId()).thenReturn(EVENT_ID);
            when(canceledEvent.getEventStatus()).thenReturn(EventStatus.CANCELED);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(canceledEvent);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(canceledEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.revertRegistration(REGISTRATION_ID, OPERATOR_ID))
                    .isInstanceOf(EventNotEditableException.class);
        }
    }

    @Nested
    @DisplayName("신청자 수 변경 매트릭스")
    class CountMatrixTest {

        /**
         * SVC-REG-071: 겹침 기간 정원 마감 → 취소 → 자동 재오픈
         */
        @Test
        @DisplayName("[SVC-REG-071] 겹침 기간 정원 마감 후 취소하면 registrationStatus=OPEN 복원")
        void cancelRegistration_CapacityFullDuringOverlapPeriod_ReopensRegistration() {
            // given: reg=OPEN, event=ONGOING, REGISTERED 상태 (isActive=true)
            Event ongoingEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE);
            when(ongoingEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.OPEN);
            when(ongoingEvent.getEventStatus()).thenReturn(EventStatus.ONGOING);

            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(ongoingEvent));
            when(eventRepository.decrementCurrentCount(EVENT_ID)).thenReturn(1);

            // 정원 마감 후 취소된 신청 (REGISTERED → CANCELED, isActive=true로 카운트 감소 필요)
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.isCanceled()).thenReturn(false);
            when(registration.isActive()).thenReturn(true);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.CANCELED);
            when(registration.getRegisteredAt()).thenReturn(Instant.now());
            when(registration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(registration));

            // when
            RegistrationResponse response = eventRegistrationService.cancelRegistration(EVENT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(registration).cancel();
            verify(eventRepository).decrementCurrentCount(EVENT_ID);
            // updateEventStatusAfterDecrement에서 reopenIfNeeded 호출 확인
            verify(ongoingEvent).reopenIfNeeded(any(Instant.class));
        }

        /**
         * SVC-REG-072: CANCELED 행사에서 자동 재오픈 차단
         */
        @Test
        @DisplayName("[SVC-REG-072] CANCELED 행사에서 취소 후 자동 재오픈 차단 (CLOSED 유지)")
        void cancelRegistration_CanceledEvent_NoReopen() {
            // given: eventStatus=CANCELED, CAPACITY_FULL 상태
            Event canceledEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE);
            when(canceledEvent.getEventStatus()).thenReturn(EventStatus.CANCELED);
            when(canceledEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.CLOSED);

            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(canceledEvent));
            when(eventRepository.decrementCurrentCount(EVENT_ID)).thenReturn(1);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.isCanceled()).thenReturn(false);
            when(registration.isActive()).thenReturn(true);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.CANCELED);
            when(registration.getRegisteredAt()).thenReturn(Instant.now());
            when(registration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(registration));

            // when
            RegistrationResponse response = eventRegistrationService.cancelRegistration(EVENT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(eventRepository).decrementCurrentCount(EVENT_ID);
            // reopenIfNeeded가 호출되지만, 내부에서 eventStatus=CANCELED 조건으로 CLOSED 유지
            verify(canceledEvent).reopenIfNeeded(any(Instant.class));
            // registrationStatus가 여전히 CLOSED인지 확인
            assertThat(canceledEvent.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
        }

        /**
         * SVC-REG-073: APPROVED 상태 취소 서비스 (선발제)
         */
        @Test
        @DisplayName("[SVC-REG-073] APPROVED 상태 신청을 취소하면 카운트 감소")
        void cancelRegistration_FromApproved_DecrementsCount() {
            // given: APPROVED, MANUAL_APPROVE
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(eventRepository.decrementCurrentCount(EVENT_ID)).thenReturn(1);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.isCanceled()).thenReturn(false);
            when(registration.isActive()).thenReturn(true); // APPROVED는 isActive=true
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.CANCELED);
            when(registration.getRegisteredAt()).thenReturn(Instant.now());
            when(registration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(registration));

            // when
            RegistrationResponse response = eventRegistrationService.cancelRegistration(EVENT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(registration).cancel();
            verify(eventRepository).decrementCurrentCount(EVENT_ID);
        }

        /**
         * SVC-REG-074: 선발제 재신청 서비스 (CANCELED→WAITING)
         */
        @Test
        @DisplayName("[SVC-REG-074] 선발제 재신청 시 WAITING으로 복원되고 incrementCurrentCountIfAvailable 미호출")
        void registerEvent_ManualApproveReRegister_NoCountIncrement() {
            // given: CANCELED, MANUAL_APPROVE
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            EventRegistration canceledRegistration = mock(EventRegistration.class);
            when(canceledRegistration.isCanceled()).thenReturn(true);
            when(canceledRegistration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);
            when(canceledRegistration.getRegisteredAt()).thenReturn(Instant.now());
            when(canceledRegistration.getId()).thenReturn(REGISTRATION_ID);
            when(canceledRegistration.getUser()).thenReturn(regularMember);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(canceledRegistration));
            when(eventRegistrationRepository.existsOverlappingRegistration(eq(USER_ID), any(), any(), any()))
                    .thenReturn(false);
            when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(canceledRegistration);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID);

            // then
            assertThat(response).isNotNull();
            verify(canceledRegistration).reRegister();
            verify(eventRepository, never()).incrementCurrentCountIfAvailable(any());
        }

        /**
         * SVC-REG-075: registrationStatus=CLOSED 후 선발제 승인 가능
         */
        @Test
        @DisplayName("[SVC-REG-075] registrationStatus=CLOSED 상태에서도 선발제 승인 성공")
        void approveRegistration_ClosedRegistrationStatus_Succeeds() {
            // given: registrationStatus=CLOSED, eventStatus=UPCOMING, WAITING 존재
            Event closedEvent = mock(Event.class);
            when(closedEvent.getId()).thenReturn(EVENT_ID);
            when(closedEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.CLOSED);
            when(closedEvent.getEventStatus()).thenReturn(EventStatus.UPCOMING);
            when(closedEvent.isManualApprove()).thenReturn(true);

            User applicant = mock(User.class);
            when(applicant.getId()).thenReturn(USER_ID);

            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getId()).thenReturn(REGISTRATION_ID);
            when(registration.getEvent()).thenReturn(closedEvent);
            when(registration.getUser()).thenReturn(applicant);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);
            when(registration.getRegisteredAt()).thenReturn(Instant.now());

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(closedEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRepository.incrementCurrentCountForApproval(EVENT_ID)).thenReturn(1);
            when(eventRegistrationRepository.existsOverlappingRegistration(eq(USER_ID), any(), any(), any()))
                    .thenReturn(false);

            // Mock approve 후 상태 변경
            doAnswer(invocation -> {
                when(registration.getStatus()).thenReturn(EventRegistrationStatus.APPROVED);
                return null;
            }).when(registration).approve();

            // when
            RegistrationResponse response = eventRegistrationService.approveRegistration(REGISTRATION_ID, OPERATOR_ID);

            // then
            assertThat(response).isNotNull();
            verify(registration).approve();
            verify(eventRepository).incrementCurrentCountForApproval(EVENT_ID);
        }

        /**
         * SVC-REG-076: 비인가 접근 시 DB 상태 변경 없음
         */
        @Test
        @DisplayName("[SVC-REG-076] 일반 회원이 승인/거절/되돌리기 시도 시 예외 발생하고 DB 상태 변경 없음")
        void operatorActions_ByRegularMember_NoDbChanges() {
            // given
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.getEvent()).thenReturn(manualApproveEvent);
            when(registration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registration));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            // when & then: 승인 시도
            assertThatThrownBy(() -> eventRegistrationService.approveRegistration(REGISTRATION_ID, USER_ID))
                    .isInstanceOf(OperatorPermissionRequiredException.class);

            // then: DB 변경 없음 확인
            verify(registration, never()).approve();
            verify(registration, never()).reject();
            verify(registration, never()).revertToWaiting();
            verify(eventRepository, never()).incrementCurrentCountForApproval(any());
            verify(eventRepository, never()).incrementCurrentCountIfAvailable(any());
            verify(eventRepository, never()).decrementCurrentCount(any());
        }

        /**
         * SVC-REG-077: incrementCurrentCountIfAvailable SQL 조건 변경 검증
         */
        @Test
        @DisplayName("[SVC-REG-077] registrationStatus=CLOSED에서 선착순 신청 시 incrementCurrentCountIfAvailable 반환 0")
        void registerEvent_ClosedRegistrationStatus_IncrementReturnsZero() {
            // given: registrationStatus=CLOSED에서 선착순 행사 신청 시도
            // incrementCurrentCountIfAvailable SQL에는 registrationStatus = 'OPEN' 조건이 있음
            Event closedAutoEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE);
            when(closedAutoEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.CLOSED);

            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(closedAutoEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            // when & then: registrationStatus=CLOSED이므로 EventNotOpenException 발생
            // (validateEventIsOpen에서 차단되어 incrementCurrentCountIfAvailable까지 도달하지 않음)
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID))
                    .isInstanceOf(EventNotOpenException.class);

            // SQL 레벨에서도 registrationStatus='OPEN' 조건이 있으므로 0을 반환하는 것을 검증
            // 이 테스트는 서비스 레벨에서 이중으로 보호됨을 확인
            verify(eventRepository, never()).incrementCurrentCountIfAvailable(any());
        }
    }

    // ========== registerEvent - UNPUBLISHED 행사 신청 차단 ==========

    @Nested
    @DisplayName("registerEvent - UNPUBLISHED 행사 신청 차단")
    class RegisterEventVisibilityTest {

        /**
         * GAP-EVT-45: UNPUBLISHED 행사에 대한 registerEvent() 호출 시 EventNotFoundException
         */
        @Test
        @DisplayName("[GAP-EVT-45] UNPUBLISHED 행사에 신청하면 EventNotFoundException 발생 (정보 은폐)")
        void registerEvent_UnpublishedEvent_ThrowsEventNotFoundException() {
            // given: UNPUBLISHED 행사 Mock
            Event unpublishedEvent = mock(Event.class);
            when(unpublishedEvent.getId()).thenReturn(EVENT_ID);
            when(unpublishedEvent.getVisibility()).thenReturn(EventVisibility.UNPUBLISHED);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(unpublishedEvent));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        /**
         * GAP-EVT-45: PUBLISHED 행사에 대한 registerEvent()는 visibility 차단 없이 진행
         */
        @Test
        @DisplayName("[GAP-EVT-45] PUBLISHED 행사 신청은 visibility 차단 없이 정상 진행된다")
        void registerEvent_PublishedEvent_PassesVisibilityCheck() {
            // given: PUBLISHED 행사 (autoApproveEvent는 setUp에서 PUBLISHED로 설정됨)
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);

            EventRegistration savedRegistration = mock(EventRegistration.class);
            when(savedRegistration.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
            when(savedRegistration.getRegisteredAt()).thenReturn(Instant.now());
            when(savedRegistration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(savedRegistration);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID);

            // then: visibility 차단 없이 정상 처리됨
            assertThat(response).isNotNull();
            verify(eventRegistrationRepository).save(any(EventRegistration.class));
        }

        /**
         * GAP-EVT-45: UNPUBLISHED 행사에서 신청 시 EventNotFoundException이 발생하므로
         * 사용자 조회까지 도달하지 않음 (정보 은폐 원칙)
         */
        @Test
        @DisplayName("[GAP-EVT-45] UNPUBLISHED 행사 신청 시 사용자 조회까지 도달하지 않는다")
        void registerEvent_UnpublishedEvent_DoesNotReachUserLookup() {
            // given
            Event unpublishedEvent = mock(Event.class);
            when(unpublishedEvent.getId()).thenReturn(EVENT_ID);
            when(unpublishedEvent.getVisibility()).thenReturn(EventVisibility.UNPUBLISHED);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(unpublishedEvent));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID))
                    .isInstanceOf(EventNotFoundException.class);

            // 사용자 조회는 호출되지 않아야 함
            verify(userRepository, never()).findById(any());
        }

        /**
         * TASK-021 항목 3: PUBLISHED 행사에서 신청 후 unpublish한 경우,
         * 기존 신청에 대한 cancelRegistration()은 visibility와 무관하게 정상 동작해야 한다.
         */
        @Test
        @DisplayName("UNPUBLISHED 행사에서 기존 신청에 대한 cancelRegistration()이 정상 동작한다")
        void cancelRegistration_UnpublishedEvent_WorksNormally() {
            // given: UNPUBLISHED 상태의 행사 (publish 후 unpublish된 경우)
            Event unpublishedEvent = mock(Event.class);
            when(unpublishedEvent.getId()).thenReturn(EVENT_ID);
            when(unpublishedEvent.getVisibility()).thenReturn(EventVisibility.UNPUBLISHED);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(unpublishedEvent));

            // 기존 신청이 존재 (PUBLISHED 시절에 신청한 것, cancel 전 REGISTERED 상태)
            EventRegistration registration = mock(EventRegistration.class);
            when(registration.isCanceled()).thenReturn(false);
            when(registration.isActive()).thenReturn(true);
            when(registration.getStatus())
                    .thenReturn(EventRegistrationStatus.REGISTERED)  // cancel 전
                    .thenReturn(EventRegistrationStatus.CANCELED);   // cancel 후 (RegistrationResponse.from용)
            when(registration.getRegisteredAt()).thenReturn(Instant.now());
            when(registration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(registration));
            when(eventRepository.decrementCurrentCount(EVENT_ID)).thenReturn(1);

            // when: visibility와 무관하게 취소 가능
            RegistrationResponse response = eventRegistrationService.cancelRegistration(EVENT_ID, USER_ID);

            // then: 정상 취소 처리
            assertThat(response).isNotNull();
            verify(registration).cancel();
            verify(eventRepository).decrementCurrentCount(EVENT_ID);
        }
    }
}
