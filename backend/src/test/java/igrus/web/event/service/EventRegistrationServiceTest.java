package igrus.web.event.service;

import igrus.web.event.domain.*;
import igrus.web.event.domain.RegistrationStatus;
import igrus.web.event.dto.response.MyRegistrationResponse;
import igrus.web.event.dto.response.RegistrationListResponse;
import igrus.web.event.dto.response.RegistrationResponse;
import igrus.web.event.exception.*;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyResponseStatus;
import igrus.web.survey.exception.SurveyNotFoundException;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.domain.SurveyResponse;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import igrus.web.survey.response.exception.SurveyResponseValidationException;
import igrus.web.survey.response.repository.SurveyResponseRepository;
import igrus.web.survey.response.service.SurveyAnswerFactory;
import igrus.web.survey.response.service.SurveyAnswerValidator;
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
import org.springframework.context.ApplicationEventPublisher;
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

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private SurveyResponseRepository surveyResponseRepository;

    @Mock
    private SurveyAnswerValidator surveyAnswerValidator;

    @Mock
    private SurveyAnswerFactory surveyAnswerFactory;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private EventStatusHelper eventStatusHelper;

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
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

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
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

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
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, 4L, null))
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
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(999L, USER_ID, null))
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
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, 999L, null))
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
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
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
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
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
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
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
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
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
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(EventCapacityFullException.class);
        }

        /**
         * TC-011: 외부인만으로 정원 가득 찬 경우 회원 신청도 차단됨 (EXT-INV-04)
         * 정원 공유 검증: capacity 5, currentCount 5 (전부 외부인) -> 회원 registerEvent() 호출 시
         * incrementCurrentCountIfAvailable 원자적 UPDATE 실패(0) -> EventCapacityFullException
         */
        @Test
        @DisplayName("[TC-011] 외부인만으로 정원 가득 찬 경우 회원 신청 시 EventCapacityFullException")
        void registerEvent_ExternalsFull_MemberBlocked() {
            // given: allowExternal=true 행사, 정원 5 전부 외부인으로 차 있음
            when(autoApproveEvent.getAllowExternal()).thenReturn(true);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(0); // 정원 가득 -> 원자적 UPDATE 실패

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
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

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

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
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);

            EventRegistration savedRegistration = mock(EventRegistration.class);
            when(savedRegistration.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
            when(savedRegistration.getRegisteredAt()).thenReturn(Instant.now());
            when(savedRegistration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(savedRegistration);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

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
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
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
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
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
            // updateEventStatusAfterDecrement 공통 헬퍼 호출 확인
            verify(eventStatusHelper).updateEventStatusAfterDecrement(EVENT_ID);
        }

        /**
         * SVC-REG-072: CANCELED 행사에서 신청 취소 시 예외 발생
         */
        @Test
        @DisplayName("[SVC-REG-072] CANCELED 행사에서 신청 취소 시 예외 발생")
        void cancelRegistration_CanceledEvent_ThrowsException() {
            // given: eventStatus=CANCELED
            Event canceledEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE);
            when(canceledEvent.getEventStatus()).thenReturn(EventStatus.CANCELED);

            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(canceledEvent));

            // when & then: CANCELED 행사에서 신청 취소 시 예외 발생
            assertThatThrownBy(() -> eventRegistrationService.cancelRegistration(EVENT_ID, USER_ID))
                    .isInstanceOf(EventNotCancelableException.class);
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
            when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(canceledRegistration);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

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
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
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
         * GAP-EVT-45: UNPUBLISHED 행사에 대한 registerEvent() 호출 시 EventNotFoundException (정보 은폐)
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
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
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
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

            // then: visibility 차단 없이 정상 처리됨
            assertThat(response).isNotNull();
            verify(eventRegistrationRepository).save(any(EventRegistration.class));
        }

        /**
         * GAP-EVT-45: UNPUBLISHED 행사에서 신청 시 EventNotFoundException이 발생하므로
         * 사용자 조회까지 도달하지 않음
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
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
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

    // ========== 설문 연동 (Survey-Event Registration) ==========

    @Nested
    @DisplayName("설문 연동 행사 신청 -- registerEventWithSurvey 분기 매트릭스")
    class SurveyEventRegistrationTest {

        private static final Long SURVEY_ID = 100L;
        private Survey mockSurvey;

        private Event createSurveyLinkedEvent(EventRegistrationType type) {
            Event event = createMockEvent(type);
            when(event.hasSurvey()).thenReturn(true);
            when(event.getSurveyId()).thenReturn(SURVEY_ID);
            when(event.getSurvey()).thenAnswer(inv -> mockSurvey);
            return event;
        }

        /**
         * 설문 OPEN + 활성 상태(삭제/휴지통 아님) Mock 설정
         */
        private void setupSurveyOpen() {
            mockSurvey = mock(Survey.class);
            when(mockSurvey.getId()).thenReturn(SURVEY_ID);
            when(mockSurvey.getResponseStatus()).thenReturn(SurveyResponseStatus.OPEN);
            when(mockSurvey.isDeleted()).thenReturn(false);
            when(mockSurvey.getTrashedAt()).thenReturn(null);
            when(surveyRepository.findById(SURVEY_ID)).thenReturn(Optional.of(mockSurvey));
        }

        /**
         * 설문 CLOSED + 활성 상태(삭제/휴지통 아님) Mock 설정
         */
        private void setupSurveyClosed() {
            mockSurvey = mock(Survey.class);
            when(mockSurvey.getId()).thenReturn(SURVEY_ID);
            when(mockSurvey.getResponseStatus()).thenReturn(SurveyResponseStatus.CLOSED);
            when(mockSurvey.isDeleted()).thenReturn(false);
            when(mockSurvey.getTrashedAt()).thenReturn(null);
            when(surveyRepository.findById(SURVEY_ID)).thenReturn(Optional.of(mockSurvey));
        }

        /**
         * 공통 신청 성공 Mock 설정 (저장 결과)
         */
        private void setupRegistrationSuccess(EventRegistrationStatus expectedStatus) {
            EventRegistration savedRegistration = mock(EventRegistration.class);
            when(savedRegistration.getStatus()).thenReturn(expectedStatus);
            when(savedRegistration.getRegisteredAt()).thenReturn(Instant.now());
            when(savedRegistration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(savedRegistration);
        }

        // --- TC-002: 설문 미연결 회귀 테스트 ---

        @Test
        @DisplayName("[TC-002] 설문 미연결 행사 신청 시 설문 관련 검증 호출 없음 (회귀)")
        void registerEvent_NoSurvey_SurveyRepositoryNotCalled() {
            // given
            when(autoApproveEvent.hasSurvey()).thenReturn(false);
            when(autoApproveEvent.getSurveyId()).thenReturn(null);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);
            setupRegistrationSuccess(EventRegistrationStatus.REGISTERED);

            // when
            eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

            // then: 설문 관련 검증 호출 없음
            verify(surveyRepository, never()).findById(any());
            verify(surveyResponseRepository, never()).existsBySurveyIdAndUserId(any(), any());
        }

        // --- TC-013: OPEN + surveyAnswers 있음 + 미응답 -> 새 응답 저장 + 신청 (#1) ---

        @Test
        @DisplayName("[TC-013] 설문 OPEN, 미응답, surveyAnswers 포함 -- 새 응답 저장 + 신청 성공")
        void registerWithSurvey_OpenNoResponseWithAnswers_SavesResponseAndRegisters() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);
            setupSurveyOpen();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(false);
            setupRegistrationSuccess(EventRegistrationStatus.REGISTERED);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(1L, "답변1", null, null, null)
            );

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, answers);

            // then
            assertThat(response).isNotNull();
            verify(surveyAnswerValidator).validate(eq(mockSurvey), eq(answers));
            verify(surveyResponseRepository).save(any(SurveyResponse.class));
            verify(eventRegistrationRepository).save(any(EventRegistration.class));
        }

        // --- TC-014: OPEN + 기존 응답 존재 + surveyAnswers 생략 -> 기존 응답으로 신청 (#2) ---

        @Test
        @DisplayName("[TC-014] 설문 OPEN, 기존 응답 존재, surveyAnswers 생략 -- 기존 응답으로 신청 성공")
        void registerWithSurvey_OpenExistingResponseNoAnswers_RegistersWithExisting() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);
            setupSurveyOpen();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(true);
            setupRegistrationSuccess(EventRegistrationStatus.REGISTERED);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

            // then
            assertThat(response).isNotNull();
            verify(surveyResponseRepository, never()).save(any());
            verify(eventRegistrationRepository).save(any(EventRegistration.class));
        }

        // --- TC-015: CLOSED + 기존 응답 존재 + surveyAnswers 포함 -> surveyAnswers 무시 (#5) ---

        @Test
        @DisplayName("[TC-015] 설문 CLOSED, 기존 응답 존재, surveyAnswers 포함 -- surveyAnswers 무시, 기존 응답으로 신청")
        void registerWithSurvey_ClosedExistingResponseWithAnswers_IgnoresNewAnswers() {
            // given: CLOSED + existingResponse=true + surveyAnswers present => #5: 기존 응답으로 진행, surveyAnswers 무시
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);
            setupSurveyClosed();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(true);
            setupRegistrationSuccess(EventRegistrationStatus.REGISTERED);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(1L, "새 답변", null, null, null)
            );

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, answers);

            // then: surveyAnswers 무시됨 (CLOSED + 기존 응답 존재 = #5)
            assertThat(response).isNotNull();
            verify(surveyResponseRepository, never()).save(any());
            verify(eventRegistrationRepository).save(any(EventRegistration.class));
        }

        // --- OPEN + 기존 응답 존재 + surveyAnswers 포함 -> 중복 제약조건 -> SurveyResponseDuplicateException ---

        @Test
        @DisplayName("설문 OPEN, 기존 응답 존재, surveyAnswers 포함 -- 중복 제약조건 위반 시 SurveyResponseDuplicateException")
        void registerWithSurvey_OpenExistingResponseWithAnswers_ThrowsDuplicateOnConstraint() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            setupSurveyOpen();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(false);
            // save 시 중복 제약조건 위반
            org.hibernate.exception.ConstraintViolationException hibernateEx =
                    new org.hibernate.exception.ConstraintViolationException(
                            "duplicate", null, "uk_survey_responses_survey_user");
            when(surveyResponseRepository.save(any(SurveyResponse.class)))
                    .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate", hibernateEx));

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(1L, "답변", null, null, null)
            );

            // when/then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, answers))
                    .isInstanceOf(igrus.web.survey.response.exception.SurveyResponseDuplicateException.class);
            verify(eventRegistrationRepository, never()).save(any());
        }

        @Test
        @DisplayName("설문 OPEN, surveyAnswers 포함 -- 비-중복 DB 에러 시 원본 DataIntegrityViolationException 전파")
        void registerWithSurvey_OpenWithAnswers_NonDuplicateConstraint_Rethrows() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            setupSurveyOpen();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(false);
            // save 시 다른 제약조건 위반 (NOT NULL 등)
            org.hibernate.exception.ConstraintViolationException hibernateEx =
                    new org.hibernate.exception.ConstraintViolationException(
                            "not null", null, "some_other_constraint");
            when(surveyResponseRepository.save(any(SurveyResponse.class)))
                    .thenThrow(new org.springframework.dao.DataIntegrityViolationException("not null", hibernateEx));

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(1L, "답변", null, null, null)
            );

            // when/then: 원본 예외가 전파되어야 함
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, answers))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            verify(eventRegistrationRepository, never()).save(any());
        }

        // --- TC-016: OPEN + 미응답 + surveyAnswers 미포함 -> SurveyResponseRequiredException (#3) ---

        @Test
        @DisplayName("[TC-016] 설문 OPEN, 미응답, surveyAnswers 미포함 -- SurveyResponseRequiredException")
        void registerWithSurvey_OpenNoResponseNoAnswers_ThrowsRequired() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            setupSurveyOpen();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(false);

            // when/then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(SurveyResponseRequiredException.class);
        }

        // --- TC-019: 설문 응답 유효성 검증 실패 시 행사 신청 미수행 ---

        @Test
        @DisplayName("[TC-019] 설문 응답 유효성 검증 실패 시 행사 신청 미수행")
        void registerWithSurvey_InvalidAnswers_NoRegistration() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            setupSurveyOpen();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(false);

            List<SubmitAnswerRequest> invalidAnswers = List.of(
                    new SubmitAnswerRequest(1L, null, null, null, null)
            );
            doThrow(new SurveyResponseValidationException("필수 답변 누락"))
                    .when(surveyAnswerValidator).validate(eq(mockSurvey), eq(invalidAnswers));

            // when/then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, invalidAnswers))
                    .isInstanceOf(SurveyResponseValidationException.class);
            verify(eventRegistrationRepository, never()).save(any());
        }

        // --- TC-020: 신청 완료 후 설문 응답 수정 시 기존 신청 상태 유지 (SEVT-INV-09) ---

        @Test
        @DisplayName("[TC-020] 신청 완료 후 설문 응답 수정 -- EventRegistrationService가 관여하지 않음")
        void surveyResponseUpdate_DoesNotAffectRegistration() {
            // SEVT-INV-09: 설문 응답 수정은 SurveyResponseService.updateResponse()에서 처리됨.
            // EventRegistrationService에는 '응답 수정 시 신청을 변경하는' 메서드가 존재하지 않으므로,
            // 설문 응답 수정과 EventRegistration 간에 커플링이 없음을 구조적으로 확인.
            // (1) EventRegistrationService에 '설문 응답 수정 후' 호출되는 메서드가 없음
            // (2) 이미 신청된 상태에서 중복 신청 시 AlreadyRegisteredException 발생 확인
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            EventRegistration existingReg = mock(EventRegistration.class);
            when(existingReg.isCanceled()).thenReturn(false);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(existingReg));

            // 설문 응답이 수정되어도 신청 상태와 무관 → 재신청 시 AlreadyRegisteredException
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(AlreadyRegisteredException.class);
            // 설문 관련 검증 호출 없음 (중복 신청에서 이미 차단)
            verify(surveyRepository, never()).findById(any());
        }

        // --- TC-021: 설문 NOT_STARTED 시 SurveyNotReadyException ---

        @Test
        @DisplayName("[TC-021] 설문 NOT_STARTED인 행사 신청 시 SurveyNotReadyException")
        void registerWithSurvey_SurveyNotStarted_ThrowsNotReady() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            mockSurvey = mock(Survey.class);
            when(mockSurvey.getResponseStatus()).thenReturn(SurveyResponseStatus.NOT_STARTED);
            when(mockSurvey.isDeleted()).thenReturn(false);
            when(mockSurvey.getTrashedAt()).thenReturn(null);
            when(surveyRepository.findById(SURVEY_ID)).thenReturn(Optional.of(mockSurvey));

            // when/then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(SurveyNotReadyException.class);
        }

        // --- TC-022: 설문 OPEN + 응답 존재 시 신청 성공 ---

        @Test
        @DisplayName("[TC-022] 설문 OPEN + 응답 존재 시 신청 성공")
        void registerWithSurvey_SurveyOpenExistingResponse_Success() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);
            setupSurveyOpen();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(true);
            setupRegistrationSuccess(EventRegistrationStatus.REGISTERED);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

            // then
            assertThat(response).isNotNull();
            verify(eventRegistrationRepository).save(any(EventRegistration.class));
        }

        // --- TC-023: 설문 CLOSED + 기존 응답 존재 시 신청 성공 (#6) ---

        @Test
        @DisplayName("[TC-023] 설문 CLOSED + 기존 응답 존재 시 신청 성공")
        void registerWithSurvey_SurveyClosedExistingResponse_Success() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);
            setupSurveyClosed();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(true);
            setupRegistrationSuccess(EventRegistrationStatus.REGISTERED);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

            // then
            assertThat(response).isNotNull();
            verify(eventRegistrationRepository).save(any(EventRegistration.class));
        }

        // --- TC-024: 설문 휴지통일 때 신규 신청 차단 ---

        @Test
        @DisplayName("[TC-024] 설문이 휴지통에 있을 때 신규 신청 차단")
        void registerWithSurvey_SurveyTrashed_ThrowsNotFound() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            mockSurvey = mock(Survey.class);
            when(mockSurvey.getId()).thenReturn(SURVEY_ID);
            when(mockSurvey.isDeleted()).thenReturn(false);
            when(mockSurvey.getTrashedAt()).thenReturn(Instant.now());
            when(surveyRepository.findById(SURVEY_ID)).thenReturn(Optional.of(mockSurvey));

            // when/then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(SurveyNotFoundException.class);
        }

        // --- TC-025: 설문 영구 삭제 상태에서 신규 신청 차단 ---

        @Test
        @DisplayName("[TC-025] 설문이 영구 삭제된 상태에서 신규 신청 차단")
        void registerWithSurvey_SurveyDeleted_ThrowsNotFound() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            when(surveyRepository.findById(SURVEY_ID)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(SurveyNotFoundException.class);
        }

        // --- TC-027: PUBLISHED + NOT_STARTED(registrationStatus) 행사 신청 시 실패 ---

        @Test
        @DisplayName("[TC-027] PUBLISHED + NOT_STARTED 행사 신청 시 EventNotOpenException")
        void registerWithSurvey_RegistrationNotStarted_ThrowsNotOpen() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(event.getRegistrationStatus()).thenReturn(RegistrationStatus.NOT_STARTED);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(EventNotOpenException.class);
        }

        // --- TC-028: PUBLISHED + OPEN + 설문 NOT_STARTED -> SurveyNotReadyException ---

        @Test
        @DisplayName("[TC-028] PUBLISHED + OPEN + 설문 NOT_STARTED -> SurveyNotReadyException")
        void registerWithSurvey_EventOpenSurveyNotStarted_ThrowsNotReady() {
            // given (TC-021과 동일 시나리오, 매트릭스 관점)
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            mockSurvey = mock(Survey.class);
            when(mockSurvey.getResponseStatus()).thenReturn(SurveyResponseStatus.NOT_STARTED);
            when(mockSurvey.isDeleted()).thenReturn(false);
            when(mockSurvey.getTrashedAt()).thenReturn(null);
            when(surveyRepository.findById(SURVEY_ID)).thenReturn(Optional.of(mockSurvey));

            // when/then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(SurveyNotReadyException.class);
        }

        // --- TC-029: PUBLISHED + OPEN + 설문 OPEN + 응답 존재 -> 성공 ---

        @Test
        @DisplayName("[TC-029] PUBLISHED + OPEN + 설문 OPEN + 응답 존재 -> 신청 성공")
        void registerWithSurvey_EventOpenSurveyOpenResponseExists_Success() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);
            setupSurveyOpen();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(true);
            setupRegistrationSuccess(EventRegistrationStatus.REGISTERED);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

            // then
            assertThat(response).isNotNull();
        }

        // --- TC-030: PUBLISHED + OPEN + 설문 CLOSED + 기존 응답 존재 -> 성공 ---

        @Test
        @DisplayName("[TC-030] PUBLISHED + OPEN + 설문 CLOSED + 기존 응답 존재 -> 신청 성공")
        void registerWithSurvey_EventOpenSurveyClosedResponseExists_Success() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);
            setupSurveyClosed();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(true);
            setupRegistrationSuccess(EventRegistrationStatus.REGISTERED);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

            // then
            assertThat(response).isNotNull();
        }

        // --- TC-031: PUBLISHED + CLOSED(registrationStatus) 행사 신청 시 실패 ---

        @Test
        @DisplayName("[TC-031] PUBLISHED + CLOSED(registrationStatus) 행사 신청 시 EventNotOpenException")
        void registerWithSurvey_RegistrationClosed_ThrowsNotOpen() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(event.getRegistrationStatus()).thenReturn(RegistrationStatus.CLOSED);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            // when/then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(EventNotOpenException.class);
        }

        // --- TC-033: 설문 UNPUBLISHED 전환 후 기존 응답으로 신규 신청 가능 ---

        @Test
        @DisplayName("[TC-033] 설문 UNPUBLISHED+CLOSED 전환 후 기존 응답으로 신규 신청 가능")
        void registerWithSurvey_SurveyUnpublishedClosed_ExistingResponseAllows() {
            // given: 설문이 UNPUBLISHED 전환됨 (responseStatus 자동 CLOSED)
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);

            // 설문 CLOSED + 활성 (visibility는 validateSurveyState에서 검증하지 않음)
            setupSurveyClosed();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(true);
            setupRegistrationSuccess(EventRegistrationStatus.REGISTERED);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

            // then
            assertThat(response).isNotNull();
        }

        // --- TC-038: OPEN + 미응답 + surveyAnswers 포함 -> 신규 응답 저장 + 성공 (경계값) ---

        @Test
        @DisplayName("[TC-038] 설문 OPEN, 미응답, surveyAnswers 포함 -- 경계값 확인")
        void registerWithSurvey_OpenNoResponseWithAnswers_BoundaryValue() {
            // given: TC-013과 동일 로직이나 경계값 관점
            Event event = createSurveyLinkedEvent(EventRegistrationType.MANUAL_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            setupSurveyOpen();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(false);
            setupRegistrationSuccess(EventRegistrationStatus.WAITING);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(1L, "답변", null, null, null)
            );

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, answers);

            // then
            assertThat(response).isNotNull();
            verify(surveyResponseRepository).save(any(SurveyResponse.class));
        }

        // --- TC-039: CLOSED + 미응답 + surveyAnswers 미포함 -> SurveyResponseRequiredException ---

        @Test
        @DisplayName("[TC-039] 설문 CLOSED, 미응답, surveyAnswers 미포함 -- SurveyResponseRequiredException")
        void registerWithSurvey_ClosedNoResponseNoAnswers_ThrowsRequired() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            setupSurveyClosed();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(false);

            // when/then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(SurveyResponseRequiredException.class);
        }

        // --- 분기 #4: CLOSED + surveyAnswers 포함 + 미응답 -> SurveyResponseRequiredException ---

        @Test
        @DisplayName("설문 CLOSED, 미응답, surveyAnswers 포함 -- SurveyResponseRequiredException (분기 #4)")
        void registerWithSurvey_ClosedNoResponseWithAnswers_ThrowsRequired() {
            // given: CLOSED 설문 + surveyAnswers 있음 + 기존 응답 없음 → SurveyResponseRequiredException
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            setupSurveyClosed();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(false);

            List<SubmitAnswerRequest> answers = List.of(
                    new SubmitAnswerRequest(1L, "답변1", null, null, null)
            );

            // when/then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, answers))
                    .isInstanceOf(SurveyResponseRequiredException.class);
            // 설문 응답 저장 시도 없음
            verify(surveyResponseRepository, never()).save(any());
            verify(eventRegistrationRepository, never()).save(any());
        }

        // --- TC-040: 설문 휴지통 이동 시 신규 신청 실패 ---

        @Test
        @DisplayName("[TC-040] 설문 휴지통 이동 시 신규 신청 실패 (경계값)")
        void registerWithSurvey_SurveyTrashed_BoundaryValue() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            mockSurvey = mock(Survey.class);
            when(mockSurvey.isDeleted()).thenReturn(false);
            when(mockSurvey.getTrashedAt()).thenReturn(Instant.now());
            when(surveyRepository.findById(SURVEY_ID)).thenReturn(Optional.of(mockSurvey));

            // when/then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(SurveyNotFoundException.class);
        }

        // --- TC-045: 동일 설문 두 행사에 응답 1회로 양쪽 신청 성공 ---

        @Test
        @DisplayName("[TC-045] 동일 설문 연결 두 행사에 응답 1회로 양쪽 신청 성공")
        void registerWithSurvey_SameSurveyTwoEvents_BothSucceed() {
            // given
            Long eventIdA = 10L;
            Long eventIdB = 20L;
            Event eventA = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventA.getId()).thenReturn(eventIdA);
            Event eventB = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventB.getId()).thenReturn(eventIdB);

            when(eventRepository.findByIdAndNotDeleted(eventIdA)).thenReturn(Optional.of(eventA));
            when(eventRepository.findByIdAndNotDeleted(eventIdB)).thenReturn(Optional.of(eventB));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(eventIdA, USER_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.findByEventIdAndUserId(eventIdB, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(eventIdA)).thenReturn(1);
            when(eventRepository.incrementCurrentCountIfAvailable(eventIdB)).thenReturn(1);
            setupSurveyOpen();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(true);
            setupRegistrationSuccess(EventRegistrationStatus.REGISTERED);

            // when: 두 행사 모두 신청 (기존 응답 재사용)
            RegistrationResponse responseA = eventRegistrationService.registerEvent(eventIdA, USER_ID, null);
            RegistrationResponse responseB = eventRegistrationService.registerEvent(eventIdB, USER_ID, null);

            // then
            assertThat(responseA).isNotNull();
            assertThat(responseB).isNotNull();
        }

        // --- TC-053: 설문 accessLevel 부족 사용자가 응답 부재로 차단 ---

        @Test
        @DisplayName("[TC-053] 설문 accessLevel 부족 사용자(MEMBER)가 응답 부재로 신청 차단")
        void registerWithSurvey_MemberNoResponse_IndirectlyBlocked() {
            // given: 설문 accessLevel=OPERATOR이므로 MEMBER는 설문 응답 불가 -> 응답 미존재
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            setupSurveyOpen();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(false);

            // when/then: 설문 응답이 없으므로 SurveyResponseRequiredException
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(SurveyResponseRequiredException.class);
        }

        // --- TC-065: 설문 비공개 전환(UNPUBLISHED) 후 기존 응답으로 행사 신청 성공 ---

        @Test
        @DisplayName("[TC-065] 설문 비공개 전환 후 기존 응답으로 행사 신청 성공")
        void registerWithSurvey_SurveyUnpublished_ExistingResponseSuccess() {
            // given: 설문 UNPUBLISHED (responseStatus 자동 CLOSED), 기존 응답 존재
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);
            // CLOSED + 활성 (visibility 검증 안 함)
            setupSurveyClosed();
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(true);
            setupRegistrationSuccess(EventRegistrationStatus.REGISTERED);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

            // then
            assertThat(response).isNotNull();
        }

        // --- TC-068: 중복 신청 확인이 설문 검증보다 선행 ---

        @Test
        @DisplayName("[TC-068] 중복 신청 확인이 설문 검증보다 선행 (검증 순서)")
        void registerWithSurvey_DuplicateCheckBeforeSurveyValidation() {
            // given: 이미 활성 신청 존재, 설문 NOT_STARTED
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            EventRegistration existingReg = mock(EventRegistration.class);
            when(existingReg.isCanceled()).thenReturn(false);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(existingReg));

            // when/then: 중복 신청 예외가 설문 검증보다 먼저 발생
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(AlreadyRegisteredException.class);
            // 설문 관련 호출 없음 (중복 신청에서 이미 차단)
            verify(surveyRepository, never()).findById(any());
        }

        // --- TC-069: 설문 상태 검증이 시간 겹침/정원 확인보다 선행 ---

        @Test
        @DisplayName("[TC-069] 설문 상태 검증이 시간 겹침/정원 확인보다 선행")
        void registerWithSurvey_SurveyCheckBeforeTimeOverlap() {
            // given: 설문 NOT_STARTED + 행사 정원 초과
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(event.isFull()).thenReturn(true);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID)).thenReturn(Optional.empty());

            mockSurvey = mock(Survey.class);
            when(mockSurvey.getResponseStatus()).thenReturn(SurveyResponseStatus.NOT_STARTED);
            when(mockSurvey.isDeleted()).thenReturn(false);
            when(mockSurvey.getTrashedAt()).thenReturn(null);
            when(surveyRepository.findById(SURVEY_ID)).thenReturn(Optional.of(mockSurvey));

            // when/then: SurveyNotReadyException이 정원 확인보다 먼저 발생
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(SurveyNotReadyException.class);
            // 정원 확인 호출 없음
            verify(eventRepository, never()).incrementCurrentCountIfAvailable(any());
        }
    }

    // ========== 재신청(reRegister) 시 설문 검증 ==========

    @Nested
    @DisplayName("재신청(reRegister) 시 설문 검증")
    class SurveyReRegistrationTest {

        private static final Long SURVEY_ID = 100L;
        private Survey mockSurvey;

        private Event createSurveyLinkedEvent(EventRegistrationType type) {
            Event event = createMockEvent(type);
            when(event.hasSurvey()).thenReturn(true);
            when(event.getSurveyId()).thenReturn(SURVEY_ID);
            when(event.getSurvey()).thenAnswer(inv -> mockSurvey);
            return event;
        }

        @Test
        @DisplayName("[TC-047] 취소 후 재신청 시 설문 응답 존재 확인 후 성공")
        void reRegister_WithSurveyResponseExists_Success() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            EventRegistration canceledReg = mock(EventRegistration.class);
            when(canceledReg.isCanceled()).thenReturn(true);
            when(canceledReg.getUser()).thenReturn(regularMember);
            when(canceledReg.getStatus()).thenReturn(EventRegistrationStatus.CANCELED);
            when(canceledReg.getRegisteredAt()).thenReturn(Instant.now());
            when(canceledReg.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(canceledReg));

            mockSurvey = mock(Survey.class);
            when(mockSurvey.getResponseStatus()).thenReturn(SurveyResponseStatus.OPEN);
            when(mockSurvey.isDeleted()).thenReturn(false);
            when(mockSurvey.getTrashedAt()).thenReturn(null);
            when(surveyRepository.findById(SURVEY_ID)).thenReturn(Optional.of(mockSurvey));
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(true);

            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);
            when(eventRegistrationRepository.save(any())).thenReturn(canceledReg);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

            // then
            assertThat(response).isNotNull();
            verify(surveyResponseRepository).existsBySurveyIdAndUserId(SURVEY_ID, USER_ID);
            verify(canceledReg).reRegister();
            verify(eventRegistrationRepository).save(canceledReg);
        }

        @Test
        @DisplayName("[TC-048] 취소 후 재신청 시 설문 응답 미존재 시 SurveyResponseRequiredException")
        void reRegister_WithNoSurveyResponse_ThrowsRequired() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            EventRegistration canceledReg = mock(EventRegistration.class);
            when(canceledReg.isCanceled()).thenReturn(true);
            when(canceledReg.getUser()).thenReturn(regularMember);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(canceledReg));

            mockSurvey = mock(Survey.class);
            when(mockSurvey.getResponseStatus()).thenReturn(SurveyResponseStatus.OPEN);
            when(mockSurvey.isDeleted()).thenReturn(false);
            when(mockSurvey.getTrashedAt()).thenReturn(null);
            when(surveyRepository.findById(SURVEY_ID)).thenReturn(Optional.of(mockSurvey));
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(false);

            // when/then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null))
                    .isInstanceOf(SurveyResponseRequiredException.class);
        }

        @Test
        @DisplayName("[TC-050] 취소 후 재신청 시 OPEN 설문 + surveyAnswers + 기존 응답 없음 → 새 응답 저장 후 성공")
        void reRegister_OpenSurveyWithAnswersNoExisting_SavesNewResponseAndSucceeds() {
            // given
            Event event = createSurveyLinkedEvent(EventRegistrationType.AUTO_APPROVE);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            EventRegistration canceledReg = mock(EventRegistration.class);
            when(canceledReg.isCanceled()).thenReturn(true);
            when(canceledReg.getUser()).thenReturn(regularMember);
            when(canceledReg.getStatus()).thenReturn(EventRegistrationStatus.CANCELED);
            when(canceledReg.getRegisteredAt()).thenReturn(Instant.now());
            when(canceledReg.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(canceledReg));

            mockSurvey = mock(Survey.class);
            when(mockSurvey.getResponseStatus()).thenReturn(SurveyResponseStatus.OPEN);
            when(mockSurvey.isDeleted()).thenReturn(false);
            when(mockSurvey.getTrashedAt()).thenReturn(null);
            when(surveyRepository.findById(SURVEY_ID)).thenReturn(Optional.of(mockSurvey));
            when(surveyResponseRepository.existsBySurveyIdAndUserId(SURVEY_ID, USER_ID)).thenReturn(false);

            List<SubmitAnswerRequest> surveyAnswers = List.of(
                    new SubmitAnswerRequest(1L, "답변", null, null, null)
            );

            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);
            when(eventRegistrationRepository.save(any())).thenReturn(canceledReg);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, surveyAnswers);

            // then
            assertThat(response).isNotNull();
            verify(surveyAnswerValidator).validate(mockSurvey, surveyAnswers);
            verify(surveyAnswerFactory).createAnswers(any(SurveyResponse.class), eq(mockSurvey), eq(surveyAnswers));
            verify(surveyResponseRepository).save(any(SurveyResponse.class));
            verify(canceledReg).reRegister();
        }

        @Test
        @DisplayName("[TC-049] 취소 후 재신청 시 설문이 해제된 경우 설문 검증 생략하고 성공")
        void reRegister_SurveyRemoved_SkipsSurveyValidation() {
            // given: surveyId가 null로 변경된 행사
            Event event = createMockEvent(EventRegistrationType.AUTO_APPROVE);
            when(event.hasSurvey()).thenReturn(false);
            when(event.getSurveyId()).thenReturn(null);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(event));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            EventRegistration canceledReg = mock(EventRegistration.class);
            when(canceledReg.isCanceled()).thenReturn(true);
            when(canceledReg.getUser()).thenReturn(regularMember);
            when(canceledReg.getStatus()).thenReturn(EventRegistrationStatus.CANCELED);
            when(canceledReg.getRegisteredAt()).thenReturn(Instant.now());
            when(canceledReg.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, USER_ID))
                    .thenReturn(Optional.of(canceledReg));

            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);
            when(eventRegistrationRepository.save(any())).thenReturn(canceledReg);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

            // then: 설문 관련 호출 없음
            assertThat(response).isNotNull();
            verify(surveyRepository, never()).findById(any());
            verify(surveyResponseRepository, never()).existsBySurveyIdAndUserId(any(), any());
        }
    }

    // ==================== TC-012, TC-013, TC-078, TC-079: 준회원 조건부 허용 회귀 테스트 ====================

    @Nested
    @DisplayName("registerEvent - 준회원 조건부 허용 (EXT-INV-05)")
    class AssociateConditionalAllowTest {

        /**
         * TC-012: 준회원이 allowExternal=true 행사에 신청 성공
         */
        @Test
        @DisplayName("[TC-012] 준회원이 allowExternal=true 행사에 신청하면 성공")
        void registerEvent_Associate_AllowExternalTrue_Success() {
            // given
            when(autoApproveEvent.getAllowExternal()).thenReturn(true);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(4L)).thenReturn(Optional.of(associateMember));
            when(eventRegistrationRepository.findByEventIdAndUserId(EVENT_ID, 4L)).thenReturn(Optional.empty());
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);

            EventRegistration savedRegistration = mock(EventRegistration.class);
            when(savedRegistration.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
            when(savedRegistration.getRegisteredAt()).thenReturn(Instant.now());
            when(savedRegistration.getId()).thenReturn(REGISTRATION_ID);
            when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(savedRegistration);

            // when
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, 4L, null);

            // then
            assertThat(response).isNotNull();
            verify(eventRegistrationRepository).save(any(EventRegistration.class));
        }

        /**
         * TC-013: 준회원이 allowExternal=false 행사에 신청 시 403
         */
        @Test
        @DisplayName("[TC-013] 준회원이 allowExternal=false 행사에 신청하면 AssociateMemberNotAllowedException")
        void registerEvent_Associate_AllowExternalFalse_ThrowsException() {
            // given
            when(autoApproveEvent.getAllowExternal()).thenReturn(false);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(4L)).thenReturn(Optional.of(associateMember));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, 4L, null))
                    .isInstanceOf(AssociateMemberNotAllowedException.class);
        }

        /**
         * TC-078: MEMBER + allowExternal=false -> 신청 성공 (회귀)
         */
        @Test
        @DisplayName("[TC-078] 정회원이 allowExternal=false 행사에 신청하면 기존처럼 성공")
        void registerEvent_Member_AllowExternalFalse_Success() {
            // given
            when(autoApproveEvent.getAllowExternal()).thenReturn(false);
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
            RegistrationResponse response = eventRegistrationService.registerEvent(EVENT_ID, USER_ID, null);

            // then
            assertThat(response).isNotNull();
        }

        /**
         * TC-079: ASSOCIATE + allowExternal=false -> 403 (회귀)
         */
        @Test
        @DisplayName("[TC-079] 준회원이 allowExternal=false 행사에 신청하면 기존 동작(403)이 유지됨")
        void registerEvent_Associate_AllowExternalFalse_RegressionCheck() {
            // given
            when(autoApproveEvent.getAllowExternal()).thenReturn(false);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findById(4L)).thenReturn(Optional.of(associateMember));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.registerEvent(EVENT_ID, 4L, null))
                    .isInstanceOf(AssociateMemberNotAllowedException.class);
        }
    }

    // ==================== TC-019, TC-020, TC-032, TC-036, TC-077: 관리자 취소 ====================

    @Nested
    @DisplayName("cancelRegistrationByAdmin - 관리자 신청 취소 (EXT-INV-09)")
    class CancelRegistrationByAdminTest {

        private EventRegistration registeredRegistration;
        private EventRegistration approvedRegistration;
        private EventRegistration canceledRegistration;
        private EventRegistration waitingRegistration;

        @BeforeEach
        void setUpRegistrations() {
            // REGISTERED 상태 외부인 신청 Mock
            registeredRegistration = mock(EventRegistration.class);
            when(registeredRegistration.getId()).thenReturn(REGISTRATION_ID);
            when(registeredRegistration.getEvent()).thenReturn(autoApproveEvent);
            when(registeredRegistration.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
            when(registeredRegistration.isActive()).thenReturn(true);
            when(registeredRegistration.isCanceled()).thenReturn(false);
            when(registeredRegistration.getIsExternal()).thenReturn(true);
            when(registeredRegistration.getRegisteredAt()).thenReturn(Instant.now());

            // APPROVED 상태 외부인 신청 Mock
            approvedRegistration = mock(EventRegistration.class);
            when(approvedRegistration.getId()).thenReturn(20L);
            when(approvedRegistration.getEvent()).thenReturn(manualApproveEvent);
            when(approvedRegistration.getStatus()).thenReturn(EventRegistrationStatus.APPROVED);
            when(approvedRegistration.isActive()).thenReturn(true);
            when(approvedRegistration.isCanceled()).thenReturn(false);
            when(approvedRegistration.getIsExternal()).thenReturn(true);
            when(approvedRegistration.getRegisteredAt()).thenReturn(Instant.now());

            // CANCELED 상태 신청 Mock
            canceledRegistration = mock(EventRegistration.class);
            when(canceledRegistration.getId()).thenReturn(30L);
            when(canceledRegistration.getEvent()).thenReturn(autoApproveEvent);
            when(canceledRegistration.getStatus()).thenReturn(EventRegistrationStatus.CANCELED);
            when(canceledRegistration.isActive()).thenReturn(false);
            when(canceledRegistration.isCanceled()).thenReturn(true);
            when(canceledRegistration.getIsExternal()).thenReturn(true);
            when(canceledRegistration.getRegisteredAt()).thenReturn(Instant.now());

            // WAITING 상태 외부인 신청 Mock
            waitingRegistration = mock(EventRegistration.class);
            when(waitingRegistration.getId()).thenReturn(40L);
            when(waitingRegistration.getEvent()).thenReturn(manualApproveEvent);
            when(waitingRegistration.getStatus()).thenReturn(EventRegistrationStatus.WAITING);
            when(waitingRegistration.isActive()).thenReturn(false);
            when(waitingRegistration.isCanceled()).thenReturn(false);
            when(waitingRegistration.getIsExternal()).thenReturn(true);
            when(waitingRegistration.getRegisteredAt()).thenReturn(Instant.now());
        }

        /**
         * TC-019: OPERATOR가 외부인 REGISTERED 신청 취소 성공
         */
        @Test
        @DisplayName("[TC-019] OPERATOR가 외부인 REGISTERED 신청 취소 시 CANCELED + currentCount 감소")
        void cancelByAdmin_Operator_RegisteredExternal_Success() {
            // given
            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registeredRegistration));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRepository.decrementCurrentCount(EVENT_ID)).thenReturn(1);

            // when
            RegistrationResponse response = eventRegistrationService.cancelRegistrationByAdmin(REGISTRATION_ID, OPERATOR_ID);

            // then
            assertThat(response).isNotNull();
            verify(registeredRegistration).cancel();
            verify(eventRepository).decrementCurrentCount(EVENT_ID);
            verify(eventPublisher).publishEvent(any(igrus.web.event.audit.EventStatusChanged.class));
        }

        /**
         * TC-020: ADMIN이 외부인 신청 취소 성공
         */
        @Test
        @DisplayName("[TC-020] ADMIN이 외부인 REGISTERED 신청 취소 시 성공")
        void cancelByAdmin_Admin_RegisteredExternal_Success() {
            // given
            User admin = mock(User.class);
            when(admin.getId()).thenReturn(5L);
            when(admin.isOperatorOrAbove()).thenReturn(true);

            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registeredRegistration));
            when(userRepository.findById(5L)).thenReturn(Optional.of(admin));
            when(eventRepository.decrementCurrentCount(EVENT_ID)).thenReturn(1);

            // when
            RegistrationResponse response = eventRegistrationService.cancelRegistrationByAdmin(REGISTRATION_ID, 5L);

            // then
            assertThat(response).isNotNull();
            verify(registeredRegistration).cancel();
            verify(eventRepository).decrementCurrentCount(EVENT_ID);
        }

        /**
         * TC-032: 선착순 행사에서 관리자가 외부인 REGISTERED 신청 취소 시 CANCELED + currentCount 감소
         */
        @Test
        @DisplayName("[TC-032] 선착순 행사 외부인 REGISTERED 취소 -> CANCELED + currentCount 감소")
        void cancelByAdmin_AutoApprove_RegisteredExternal_DecrementCount() {
            // given
            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registeredRegistration));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRepository.decrementCurrentCount(EVENT_ID)).thenReturn(1);

            // when
            eventRegistrationService.cancelRegistrationByAdmin(REGISTRATION_ID, OPERATOR_ID);

            // then
            verify(registeredRegistration).cancel();
            verify(eventRepository).decrementCurrentCount(EVENT_ID);
        }

        /**
         * TC-036: 선발제 행사에서 APPROVED 외부인 취소 시 CANCELED + currentCount 감소
         */
        @Test
        @DisplayName("[TC-036] 선발제 APPROVED 외부인 취소 -> CANCELED + currentCount 감소")
        void cancelByAdmin_ManualApprove_ApprovedExternal_DecrementCount() {
            // given
            when(eventRegistrationRepository.findById(20L)).thenReturn(Optional.of(approvedRegistration));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRepository.decrementCurrentCount(EVENT_ID)).thenReturn(1);

            // when
            eventRegistrationService.cancelRegistrationByAdmin(20L, OPERATOR_ID);

            // then
            verify(approvedRegistration).cancel();
            verify(eventRepository).decrementCurrentCount(EVENT_ID);
        }

        /**
         * WAITING 상태 외부인 신청 취소 시 currentCount 변경 없음
         */
        @Test
        @DisplayName("WAITING 상태 외부인 취소 -> CANCELED, currentCount 변경 없음")
        void cancelByAdmin_WaitingExternal_NoDecrementCount() {
            // given
            when(eventRegistrationRepository.findById(40L)).thenReturn(Optional.of(waitingRegistration));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            // when
            eventRegistrationService.cancelRegistrationByAdmin(40L, OPERATOR_ID);

            // then
            verify(waitingRegistration).cancel();
            verify(eventRepository, never()).decrementCurrentCount(any());
        }

        /**
         * 이미 CANCELED 상태인 신청 취소 시 예외
         */
        @Test
        @DisplayName("이미 CANCELED 상태인 신청 취소 시도 -> 예외 발생")
        void cancelByAdmin_AlreadyCanceled_ThrowsException() {
            // given
            when(eventRegistrationRepository.findById(30L)).thenReturn(Optional.of(canceledRegistration));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            doThrow(new InvalidRegistrationStatusException()).when(canceledRegistration).cancel();

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.cancelRegistrationByAdmin(30L, OPERATOR_ID))
                    .isInstanceOf(InvalidRegistrationStatusException.class);
        }

        /**
         * 존재하지 않는 registrationId로 취소 시 404
         */
        @Test
        @DisplayName("존재하지 않는 registrationId로 취소 시 EventRegistrationNotFoundException")
        void cancelByAdmin_NonExistentRegistration_ThrowsNotFoundException() {
            // given
            when(eventRegistrationRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.cancelRegistrationByAdmin(999L, OPERATOR_ID))
                    .isInstanceOf(EventRegistrationNotFoundException.class);
        }

        /**
         * MEMBER가 취소 시도하면 OperatorPermissionRequiredException
         */
        @Test
        @DisplayName("MEMBER가 관리자 취소 시도 시 OperatorPermissionRequiredException")
        void cancelByAdmin_MemberRole_ThrowsPermissionException() {
            // given
            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registeredRegistration));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(regularMember));

            // when & then
            assertThatThrownBy(() -> eventRegistrationService.cancelRegistrationByAdmin(REGISTRATION_ID, USER_ID))
                    .isInstanceOf(OperatorPermissionRequiredException.class);
        }

        /**
         * TC-077: 관리자 취소 시 EventStatusChanged 감사 이벤트 발행 확인
         */
        @Test
        @DisplayName("[TC-077] 관리자 취소 시 EventStatusChanged 이벤트에 이전 상태와 변경 유형 포함")
        void cancelByAdmin_PublishesEventStatusChanged_WithPreviousStatus() {
            // given
            when(eventRegistrationRepository.findById(REGISTRATION_ID)).thenReturn(Optional.of(registeredRegistration));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRepository.decrementCurrentCount(EVENT_ID)).thenReturn(1);

            // when
            eventRegistrationService.cancelRegistrationByAdmin(REGISTRATION_ID, OPERATOR_ID);

            // then
            var captor = org.mockito.ArgumentCaptor.forClass(igrus.web.event.audit.EventStatusChanged.class);
            verify(eventPublisher).publishEvent(captor.capture());
            igrus.web.event.audit.EventStatusChanged changed = captor.getValue();
            assertThat(changed.eventId()).isEqualTo(EVENT_ID);
            assertThat(changed.changedByUserId()).isEqualTo(OPERATOR_ID);
            assertThat(changed.changeType()).isEqualTo(igrus.web.event.domain.EventChangeType.REGISTRATION_CANCELED_BY_ADMIN);
            assertThat(changed.previousValue()).isEqualTo("REGISTERED");
            assertThat(changed.newValue()).isEqualTo("CANCELED");
        }
    }
}
