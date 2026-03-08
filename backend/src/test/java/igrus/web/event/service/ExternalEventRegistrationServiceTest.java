package igrus.web.event.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.event.domain.*;
import igrus.web.event.dto.response.RegistrationResponse;
import igrus.web.event.exception.*;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.event.repository.ExternalSurveyResponseRepository;
import igrus.web.survey.domain.Survey;
import igrus.web.survey.domain.SurveyResponseStatus;
import igrus.web.survey.repository.SurveyRepository;
import igrus.web.survey.response.dto.request.SubmitAnswerRequest;
import igrus.web.survey.response.service.SurveyAnswerValidator;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
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
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * ExternalEventRegistrationService Mockito 기반 단위 테스트.
 * 테스트 케이스 문서: docs/test-case/event/external-event-registration-test-cases.md
 *
 * @see ExternalEventRegistrationService
 */
@DisplayName("ExternalEventRegistrationService")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExternalEventRegistrationServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExternalSurveyResponseRepository externalSurveyResponseRepository;

    @Mock
    private SurveyRepository surveyRepository;

    @Mock
    private SurveyAnswerValidator surveyAnswerValidator;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ExternalEventRegistrationService service;

    // 테스트 데이터
    private static final Long EVENT_ID = 1L;
    private static final Long EVENT_ID_B = 2L;
    private static final Long REGISTRATION_ID = 10L;
    private static final String NAME = "홍길동";
    private static final String STUDENT_ID = "12345678";
    private static final String PHONE = "01012345678";
    private static final String DEPARTMENT = "컴퓨터공학과";

    private Event autoApproveEvent;
    private Event manualApproveEvent;

    @BeforeEach
    void setUp() {
        autoApproveEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE, true);
        manualApproveEvent = createMockEvent(EventRegistrationType.MANUAL_APPROVE, true);
    }

    private Event createMockEvent(EventRegistrationType type, boolean allowExternal) {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn(EVENT_ID);
        when(event.getRegistrationStatus()).thenReturn(RegistrationStatus.OPEN);
        when(event.getVisibility()).thenReturn(EventVisibility.PUBLISHED);
        when(event.getRegistrationStartAt()).thenReturn(Instant.now().minus(1, ChronoUnit.DAYS));
        when(event.getRegistrationEndAt()).thenReturn(Instant.now().plus(7, ChronoUnit.DAYS));
        when(event.getEventStartAt()).thenReturn(Instant.now().plus(14, ChronoUnit.DAYS));
        when(event.getEventEndAt()).thenReturn(Instant.now().plus(15, ChronoUnit.DAYS));
        when(event.isAutoApprove()).thenReturn(type == EventRegistrationType.AUTO_APPROVE);
        when(event.isManualApprove()).thenReturn(type == EventRegistrationType.MANUAL_APPROVE);
        when(event.getAllowExternal()).thenReturn(allowExternal);
        when(event.hasSurvey()).thenReturn(false);
        when(event.isFull()).thenReturn(false);
        return event;
    }

    private void setupDefaultMocks() {
        when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
        when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
        when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                any(), eq(STUDENT_ID), eq(EventRegistrationStatus.CANCELED))).thenReturn(false);
        when(eventRegistrationRepository.existsByEventAndExternalPhoneAndStatusNot(
                any(), eq(PHONE), eq(EventRegistrationStatus.CANCELED))).thenReturn(false);
        when(eventRegistrationRepository.existsOverlappingExternalRegistration(
                eq(STUDENT_ID), any(), any(), eq(EventRegistrationStatus.CANCELED))).thenReturn(false);
        when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);
        // After increment, event is re-fetched
        when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));

        EventRegistration savedRegistration = mock(EventRegistration.class);
        when(savedRegistration.getId()).thenReturn(REGISTRATION_ID);
        when(savedRegistration.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
        when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(savedRegistration);
    }

    // ==================== TC-001: 정상 신청 성공 ====================

    @Nested
    @DisplayName("정상 흐름")
    class NormalFlowTest {

        @Test
        @DisplayName("[TC-001] allowExternal=true 행사에 외부인 신청 성공")
        void registerExternal_AllowExternalTrue_Success() {
            // given
            setupDefaultMocks();

            // when
            RegistrationResponse response = service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null);

            // then
            assertThat(response).isNotNull();
            assertThat(response.registrationId()).isEqualTo(REGISTRATION_ID);
            verify(eventRegistrationRepository).save(any(EventRegistration.class));
            verify(eventRepository).incrementCurrentCountIfAvailable(EVENT_ID);
        }

        @Test
        @DisplayName("[TC-008] 다른 행사에 동일 정보로 신청 성공")
        void registerExternal_DifferentEvent_SameInfo_Success() {
            // given
            Event eventB = createMockEvent(EventRegistrationType.AUTO_APPROVE, true);
            when(eventB.getId()).thenReturn(EVENT_ID_B);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID_B)).thenReturn(Optional.of(eventB));
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    eq(eventB), eq(STUDENT_ID), any())).thenReturn(false);
            when(eventRegistrationRepository.existsByEventAndExternalPhoneAndStatusNot(
                    eq(eventB), eq(PHONE), any())).thenReturn(false);
            when(eventRegistrationRepository.existsOverlappingExternalRegistration(
                    eq(STUDENT_ID), any(), any(), any())).thenReturn(false);
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID_B)).thenReturn(1);

            EventRegistration saved = mock(EventRegistration.class);
            when(saved.getId()).thenReturn(20L);
            when(saved.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
            when(eventRegistrationRepository.save(any())).thenReturn(saved);

            // when
            RegistrationResponse response = service.registerExternal(
                    EVENT_ID_B, NAME, STUDENT_ID, PHONE, DEPARTMENT, null);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("[TC-030] 동일 studentId 회원 미존재 시 외부인 신청 성공")
        void registerExternal_NoRegisteredMember_Success() {
            // given
            setupDefaultMocks();
            when(userRepository.findByStudentId("99999999")).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    any(), eq("99999999"), any())).thenReturn(false);
            when(eventRegistrationRepository.existsOverlappingExternalRegistration(
                    eq("99999999"), any(), any(), any())).thenReturn(false);

            // when
            RegistrationResponse response = service.registerExternal(
                    EVENT_ID, NAME, "99999999", PHONE, DEPARTMENT, null);

            // then
            assertThat(response).isNotNull();
        }
    }

    // ==================== TC-002: allowExternal 검증 ====================

    @Nested
    @DisplayName("allowExternal 검증")
    class AllowExternalTest {

        @Test
        @DisplayName("[TC-002] allowExternal=false 행사에 외부인 신청 시 ExternalRegistrationNotAllowedException")
        void registerExternal_AllowExternalFalse_ThrowsException() {
            // given
            Event disallowedEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE, false);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(disallowedEvent));

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null))
                    .isInstanceOf(ExternalRegistrationNotAllowedException.class);
        }
    }

    // ==================== TC-003~007: 중복 방지 ====================

    @Nested
    @DisplayName("중복 방지")
    class DuplicatePreventionTest {

        @Test
        @DisplayName("[TC-003] 동일 studentId 중복 신청 시 409")
        void registerExternal_DuplicateStudentId_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    eq(autoApproveEvent), eq(STUDENT_ID), eq(EventRegistrationStatus.CANCELED))).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, "01099999999", DEPARTMENT, null))
                    .isInstanceOf(ExternalAlreadyRegisteredException.class);
        }

        @Test
        @DisplayName("[TC-004] CANCELED 상태 동일 studentId 재신청 성공")
        void registerExternal_CanceledStudentId_Success() {
            // given
            setupDefaultMocks();
            // existsByEvent... returns false (CANCELED excluded) -> success

            // when
            RegistrationResponse response = service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("[TC-005] 동일 phone 중복 신청 시 409")
        void registerExternal_DuplicatePhone_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findByStudentId("99999999")).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    any(), eq("99999999"), any())).thenReturn(false);
            when(eventRegistrationRepository.existsByEventAndExternalPhoneAndStatusNot(
                    eq(autoApproveEvent), eq(PHONE), eq(EventRegistrationStatus.CANCELED))).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, "99999999", PHONE, DEPARTMENT, null))
                    .isInstanceOf(ExternalAlreadyRegisteredException.class);
        }

        @Test
        @DisplayName("[TC-006] CANCELED 상태 동일 phone 재신청 성공")
        void registerExternal_CanceledPhone_Success() {
            // given
            setupDefaultMocks();
            when(eventRegistrationRepository.existsByEventAndExternalPhoneAndStatusNot(
                    any(), eq(PHONE), eq(EventRegistrationStatus.CANCELED))).thenReturn(false);

            // when
            RegistrationResponse response = service.registerExternal(
                    EVENT_ID, NAME, "11111111", PHONE, DEPARTMENT, null);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("[TC-007] 동일 studentId + 동일 phone 중복 신청 시 409")
        void registerExternal_DuplicateBoth_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    any(), eq(STUDENT_ID), any())).thenReturn(true);

            // when & then (studentId 먼저 체크되므로 studentId 중복으로 실패)
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null))
                    .isInstanceOf(ExternalAlreadyRegisteredException.class);
        }
    }

    // ==================== TC-009~011: 정원 공유 ====================

    @Nested
    @DisplayName("정원 공유")
    class CapacityTest {

        @Test
        @DisplayName("[TC-009] 마지막 1자리 외부인 신청 성공")
        void registerExternal_LastSlot_Success() {
            // given
            setupDefaultMocks();
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);

            // when
            RegistrationResponse response = service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null);

            // then
            assertThat(response).isNotNull();
            verify(eventRepository).incrementCurrentCountIfAvailable(EVENT_ID);
        }

        @Test
        @DisplayName("[TC-010] 정원 가득 찬 상태에서 외부인 신청 시 EventCapacityFullException")
        void registerExternal_CapacityFull_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsByEventAndExternalPhoneAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsOverlappingExternalRegistration(
                    any(), any(), any(), any())).thenReturn(false);
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(0);

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null))
                    .isInstanceOf(EventCapacityFullException.class);
        }

        @Test
        @DisplayName("[TC-055] 외부인만 4명 + 정원 5 -> 외부인 추가 성공 (마지막 자리)")
        void registerExternal_ExternalOnlyLastSlot_Success() {
            // given (same as TC-009, incrementCurrentCountIfAvailable returns 1)
            setupDefaultMocks();
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);

            // when
            RegistrationResponse response = service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null);

            // then
            assertThat(response).isNotNull();
        }

        @Test
        @DisplayName("[TC-056] 회원 5명으로 정원 가득 -> 외부인 신청 실패")
        void registerExternal_MembersFull_ExternalFails() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsByEventAndExternalPhoneAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsOverlappingExternalRegistration(
                    any(), any(), any(), any())).thenReturn(false);
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(0);

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null))
                    .isInstanceOf(EventCapacityFullException.class);
        }
    }

    // ==================== TC-016~018: OPEN/기간/UNPUBLISHED 검증 ====================

    @Nested
    @DisplayName("OPEN/기간/UNPUBLISHED 검증")
    class EventStatusValidationTest {

        @Test
        @DisplayName("[TC-016] CLOSED 행사에 외부인 신청 시 EventNotOpenException")
        void registerExternal_EventClosed_ThrowsException() {
            // given
            Event closedEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE, true);
            when(closedEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.CLOSED);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(closedEvent));
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsByEventAndExternalPhoneAndStatusNot(
                    any(), any(), any())).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null))
                    .isInstanceOf(EventNotOpenException.class);
        }

        @Test
        @DisplayName("[TC-017] 신청 기간 외 외부인 신청 시 EventNotInRegistrationPeriodException")
        void registerExternal_OutOfPeriod_ThrowsException() {
            // given
            Event futureEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE, true);
            when(futureEvent.getRegistrationStartAt()).thenReturn(Instant.now().plus(10, ChronoUnit.DAYS));
            when(futureEvent.getRegistrationEndAt()).thenReturn(Instant.now().plus(20, ChronoUnit.DAYS));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(futureEvent));
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsByEventAndExternalPhoneAndStatusNot(
                    any(), any(), any())).thenReturn(false);

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null))
                    .isInstanceOf(EventNotInRegistrationPeriodException.class);
        }

        @Test
        @DisplayName("[TC-018] UNPUBLISHED 행사에 외부인 신청 시 EventNotFoundException (404)")
        void registerExternal_Unpublished_ThrowsNotFoundException() {
            // given
            Event unpublishedEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE, true);
            when(unpublishedEvent.getVisibility()).thenReturn(EventVisibility.UNPUBLISHED);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(unpublishedEvent));

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    // ==================== TC-029: 동일 학번 가입 회원 존재 ====================

    @Nested
    @DisplayName("동일 학번 가입 회원 존재")
    class RegisteredMemberTest {

        @Test
        @DisplayName("[TC-029] 동일 studentId 가입 회원 존재 시 RegisteredMemberExistsException")
        void registerExternal_RegisteredMemberExists_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            User existingUser = mock(User.class);
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.of(existingUser));

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null))
                    .isInstanceOf(RegisteredMemberExistsException.class);
        }
    }

    // ==================== TC-027~028, TC-053~054: 설문 연동 ====================

    @Nested
    @DisplayName("설문 연동")
    class SurveyTest {

        @Test
        @DisplayName("[TC-027] 설문 연결 행사에 외부인이 응답과 함께 신청 성공")
        void registerExternal_WithSurvey_Success() throws JsonProcessingException {
            // given
            Event surveyEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE, true);
            when(surveyEvent.hasSurvey()).thenReturn(true);
            when(surveyEvent.getSurveyId()).thenReturn(100L);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(surveyEvent));
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsByEventAndExternalPhoneAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsOverlappingExternalRegistration(
                    any(), any(), any(), any())).thenReturn(false);
            when(eventRepository.incrementCurrentCountIfAvailable(EVENT_ID)).thenReturn(1);

            Survey survey = mock(Survey.class);
            when(survey.isDeleted()).thenReturn(false);
            when(survey.getTrashedAt()).thenReturn(null);
            when(survey.getResponseStatus()).thenReturn(SurveyResponseStatus.OPEN);
            when(surveyRepository.findById(100L)).thenReturn(Optional.of(survey));

            List<SubmitAnswerRequest> answers = List.of(mock(SubmitAnswerRequest.class));
            when(objectMapper.writeValueAsString(answers)).thenReturn("[{\"questionId\":1}]");

            EventRegistration saved = mock(EventRegistration.class);
            when(saved.getId()).thenReturn(REGISTRATION_ID);
            when(saved.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
            when(eventRegistrationRepository.save(any())).thenReturn(saved);

            // when
            RegistrationResponse response = service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, answers);

            // then
            assertThat(response).isNotNull();
            verify(externalSurveyResponseRepository).save(any(ExternalSurveyResponse.class));
        }

        @Test
        @DisplayName("[TC-028] 설문 연결 행사에 응답 없이 신청 시 SurveyResponseRequiredException")
        void registerExternal_SurveyRequired_NoAnswers_ThrowsException() {
            // given
            Event surveyEvent = createMockEvent(EventRegistrationType.AUTO_APPROVE, true);
            when(surveyEvent.hasSurvey()).thenReturn(true);
            when(surveyEvent.getSurveyId()).thenReturn(100L);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(surveyEvent));
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsByEventAndExternalPhoneAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsOverlappingExternalRegistration(
                    any(), any(), any(), any())).thenReturn(false);

            Survey survey = mock(Survey.class);
            when(survey.isDeleted()).thenReturn(false);
            when(survey.getTrashedAt()).thenReturn(null);
            when(survey.getResponseStatus()).thenReturn(SurveyResponseStatus.OPEN);
            when(surveyRepository.findById(100L)).thenReturn(Optional.of(survey));

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null))
                    .isInstanceOf(SurveyResponseRequiredException.class);
        }

        @Test
        @DisplayName("[TC-053] 설문 미연결 행사에 surveyAnswers=null로 신청 성공")
        void registerExternal_NoSurvey_NullAnswers_Success() {
            // given
            setupDefaultMocks();
            when(autoApproveEvent.hasSurvey()).thenReturn(false);

            // when
            RegistrationResponse response = service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null);

            // then
            assertThat(response).isNotNull();
            verify(externalSurveyResponseRepository, never()).save(any());
        }

        @Test
        @DisplayName("[TC-054] 설문 미연결 행사에 surveyAnswers 제공 시 무시하고 성공")
        void registerExternal_NoSurvey_AnswersProvided_Ignored() {
            // given
            setupDefaultMocks();
            when(autoApproveEvent.hasSurvey()).thenReturn(false);

            List<SubmitAnswerRequest> answers = List.of(mock(SubmitAnswerRequest.class));

            // when
            RegistrationResponse response = service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, answers);

            // then
            assertThat(response).isNotNull();
            verify(externalSurveyResponseRepository, never()).save(any());
        }
    }

    // ==================== TC-031, TC-033: 상태 모델 ====================

    @Nested
    @DisplayName("상태 모델")
    class StateModelTest {

        @Test
        @DisplayName("[TC-031] 선착순 행사 외부인 신청 시 REGISTERED 상태")
        void registerExternal_AutoApprove_ReturnsRegistered() {
            // given
            setupDefaultMocks();

            EventRegistration saved = mock(EventRegistration.class);
            when(saved.getId()).thenReturn(REGISTRATION_ID);
            when(saved.getStatus()).thenReturn(EventRegistrationStatus.REGISTERED);
            when(eventRegistrationRepository.save(any())).thenReturn(saved);

            // when
            RegistrationResponse response = service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null);

            // then
            assertThat(response.status()).isEqualTo(EventRegistrationStatus.REGISTERED);
        }

        @Test
        @DisplayName("[TC-033] 선발제 행사 외부인 신청 시 WAITING 상태")
        void registerExternal_ManualApprove_ReturnsWaiting() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsByEventAndExternalPhoneAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsOverlappingExternalRegistration(
                    any(), any(), any(), any())).thenReturn(false);

            EventRegistration saved = mock(EventRegistration.class);
            when(saved.getId()).thenReturn(REGISTRATION_ID);
            when(saved.getStatus()).thenReturn(EventRegistrationStatus.WAITING);
            when(eventRegistrationRepository.save(any())).thenReturn(saved);

            // when
            RegistrationResponse response = service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null);

            // then
            assertThat(response.status()).isEqualTo(EventRegistrationStatus.WAITING);
            verify(eventRepository, never()).incrementCurrentCountIfAvailable(any());
        }

        @Test
        @DisplayName("[TC-040] WAITING 중 동일 studentId 중복 시도 시 409")
        void registerExternal_WaitingDuplicateStudentId_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(manualApproveEvent));
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    eq(manualApproveEvent), eq(STUDENT_ID), eq(EventRegistrationStatus.CANCELED))).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, "01099999999", DEPARTMENT, null))
                    .isInstanceOf(ExternalAlreadyRegisteredException.class);
        }
    }

    // ==================== TC-074~076: 로그 검증 ====================

    @Nested
    @DisplayName("로그 검증")
    class LogVerificationTest {

        private ListAppender<ILoggingEvent> listAppender;
        private Logger serviceLogger;

        @BeforeEach
        void setUpLogCapture() {
            // Lombok @Slf4j가 생성하는 static log 필드와 동일한 Logger 인스턴스를 가져옴
            serviceLogger = (Logger) LoggerFactory.getLogger(ExternalEventRegistrationService.class);
            listAppender = new ListAppender<>();
            listAppender.start();
            serviceLogger.addAppender(listAppender);
            // Spring Boot 통합 테스트 이후 Logback 레벨이 변경될 수 있으므로 명시적 설정
            serviceLogger.setLevel(Level.DEBUG);
        }

        @AfterEach
        void tearDownLogCapture() {
            serviceLogger.detachAppender(listAppender);
            serviceLogger.setLevel(null); // 부모 레벨로 복원
        }

        @Test
        @DisplayName("[TC-074] 외부인 신청 성공 시 INFO 로그: '외부인 행사 신청 완료'")
        void registerExternal_Success_LogsInfoMessage() {
            // given
            setupDefaultMocks();

            // when
            RegistrationResponse response = service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null);

            // then
            assertThat(response).isNotNull();
            assertThat(listAppender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.INFO);
                assertThat(event.getFormattedMessage()).contains("외부인 행사 신청 완료");
            });
        }

        @Test
        @DisplayName("[TC-075] 외부인 중복 신청 시 INFO 로그: '외부인 행사 신청 거부'")
        void registerExternal_DuplicateStudentId_LogsInfoMessage() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    any(), eq(STUDENT_ID), any())).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null))
                    .isInstanceOf(ExternalAlreadyRegisteredException.class);

            assertThat(listAppender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.INFO);
                assertThat(event.getFormattedMessage()).contains("외부인 행사 신청 거부");
                assertThat(event.getFormattedMessage()).contains("학번 중복");
            });
        }

        @Test
        @DisplayName("[TC-076] allowExternal=false 행사에 외부인 신청 시 INFO 로그: '외부인 신청 비허용 행사'")
        void registerExternal_AllowExternalFalse_LogsInfoMessage() {
            // given
            Event disallowed = createMockEvent(EventRegistrationType.AUTO_APPROVE, false);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(disallowed));

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null))
                    .isInstanceOf(ExternalRegistrationNotAllowedException.class);

            assertThat(listAppender.list).anySatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.INFO);
                assertThat(event.getFormattedMessage()).contains("외부인 행사 신청 거부");
                assertThat(event.getFormattedMessage()).contains("외부인 신청 비허용 행사");
            });
        }
    }

    // ==================== 시간 겹침 검증 ====================

    @Nested
    @DisplayName("시간 겹침 검증")
    class TimeOverlapTest {

        @Test
        @DisplayName("시간 겹침 시 EventTimeOverlapException 발생")
        void registerExternal_TimeOverlap_ThrowsException() {
            // given
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(autoApproveEvent));
            when(userRepository.findByStudentId(STUDENT_ID)).thenReturn(Optional.empty());
            when(eventRegistrationRepository.existsByEventAndExternalStudentIdAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsByEventAndExternalPhoneAndStatusNot(
                    any(), any(), any())).thenReturn(false);
            when(eventRegistrationRepository.existsOverlappingExternalRegistration(
                    eq(STUDENT_ID), any(), any(), eq(EventRegistrationStatus.CANCELED))).thenReturn(true);

            // when & then
            assertThatThrownBy(() -> service.registerExternal(
                    EVENT_ID, NAME, STUDENT_ID, PHONE, DEPARTMENT, null))
                    .isInstanceOf(EventTimeOverlapException.class);
        }
    }
}
