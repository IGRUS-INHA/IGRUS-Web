package igrus.web.event.service;

import igrus.web.event.domain.*;
import igrus.web.event.dto.request.CreateEventRequest;
import igrus.web.event.dto.request.UpdateEventRequest;
import igrus.web.event.dto.response.EventCreateResponse;
import igrus.web.event.dto.response.EventDetailResponse;
import igrus.web.event.dto.response.EventListResponse;
import igrus.web.event.exception.*;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventReopenHistoryRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EventService 테스트.
 * 2축 상태 모델 (RegistrationStatus + EventStatus) 기반.
 * 테스트 케이스 문서: docs/test-case/event/event-domain-test-cases.md
 *
 * @see igrus.web.event.service.EventService
 */
@DisplayName("EventService")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @Mock
    private EventReopenHistoryRepository eventReopenHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventService eventService;

    private static final Long EVENT_ID = 1L;
    private static final Long OPERATOR_ID = 2L;
    private static final Long MEMBER_ID = 3L;

    private User operator;
    private User regularMember;
    private User associateMember;
    private Event mockEvent;

    // 날짜 설정: regStart < regEnd < eventStart < eventEnd
    // 2축 모델 검증: regStart < eventStart, regEnd <= eventEnd
    private final Instant regStart = Instant.now().plus(1, ChronoUnit.DAYS);
    private final Instant regEnd = Instant.now().plus(5, ChronoUnit.DAYS);
    private final Instant eventStart = Instant.now().plus(10, ChronoUnit.DAYS);
    private final Instant eventEnd = Instant.now().plus(11, ChronoUnit.DAYS);

    @BeforeEach
    void setUp() {
        operator = mock(User.class);
        when(operator.getId()).thenReturn(OPERATOR_ID);
        when(operator.isOperatorOrAbove()).thenReturn(true);
        when(operator.isAssociate()).thenReturn(false);
        when(operator.getName()).thenReturn("운영자");

        regularMember = mock(User.class);
        when(regularMember.getId()).thenReturn(MEMBER_ID);
        when(regularMember.isOperatorOrAbove()).thenReturn(false);
        when(regularMember.isAssociate()).thenReturn(false);
        when(regularMember.getName()).thenReturn("정회원");

        associateMember = mock(User.class);
        when(associateMember.getId()).thenReturn(4L);
        when(associateMember.isAssociate()).thenReturn(true);

        mockEvent = createMockEvent();
    }

    private Event createMockEvent() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn(EVENT_ID);
        when(event.getTitle()).thenReturn("테스트 행사");
        when(event.getDescription()).thenReturn("설명");
        when(event.getLocation()).thenReturn("장소");
        when(event.getUser()).thenReturn(operator);
        when(event.getEventStartAt()).thenReturn(eventStart);
        when(event.getEventEndAt()).thenReturn(eventEnd);
        when(event.getRegistrationStartAt()).thenReturn(regStart);
        when(event.getRegistrationEndAt()).thenReturn(regEnd);
        when(event.getCapacity()).thenReturn(30);
        when(event.getCurrentCount()).thenReturn(0);
        when(event.getRegistrationStatus()).thenReturn(RegistrationStatus.OPEN);
        when(event.getEventStatus()).thenReturn(EventStatus.UPCOMING);
        when(event.getRegistrationType()).thenReturn(EventRegistrationType.AUTO_APPROVE);
        when(event.isRegistrable()).thenReturn(true);
        when(event.getCreatedAt()).thenReturn(Instant.now());
        when(event.getUpdatedAt()).thenReturn(Instant.now());
        when(event.getCloseReason()).thenReturn(null);
        return event;
    }

    // ========== createEvent ==========

    @Nested
    @DisplayName("createEvent - 행사 생성")
    class CreateEvent {

        /**
         * SVC-EVT-001: 운영진 유효한 요청으로 행사 생성
         */
        @Test
        @DisplayName("[SVC-EVT-001] 운영진이 유효한 요청으로 행사를 생성하면 성공한다")
        void createEvent_WithValidRequest_Success() {
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventEnd, regStart, regEnd,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
                Event savedEvent = mock(Event.class);
                when(savedEvent.getId()).thenReturn(EVENT_ID);
                when(savedEvent.getTitle()).thenReturn("테스트 행사");
                when(savedEvent.getCreatedAt()).thenReturn(Instant.now());
                return savedEvent;
            });

            EventCreateResponse response = eventService.createEvent(request, OPERATOR_ID);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(EVENT_ID);
            assertThat(response.title()).isEqualTo("테스트 행사");
            verify(eventRepository).save(any(Event.class));
        }

        /**
         * SVC-EVT-002: 일반 회원 행사 생성 거부
         */
        @Test
        @DisplayName("[SVC-EVT-002] 일반 회원이 행사를 생성하려고 하면 EventAccessDeniedException 발생")
        void createEvent_WithRegularMember_ThrowsException() {
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventEnd, regStart, regEnd,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(regularMember));

            assertThatThrownBy(() -> eventService.createEvent(request, MEMBER_ID))
                    .isInstanceOf(EventAccessDeniedException.class);
        }

        /**
         * SVC-EVT-003: 존재하지 않는 사용자 생성
         */
        @Test
        @DisplayName("[SVC-EVT-003] 존재하지 않는 사용자 ID로 행사를 생성하면 UserNotFoundException 발생")
        void createEvent_WithNonExistentUser_ThrowsException() {
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventEnd, regStart, regEnd,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.createEvent(request, 999L))
                    .isInstanceOf(UserNotFoundException.class);
        }

        /**
         * SVC-EVT-004: 행사 날짜 역전 시 생성 거부
         */
        @Test
        @DisplayName("[SVC-EVT-004] 행사 종료일이 시작일보다 이전이면 InvalidEventDateException 발생")
        void createEvent_WithInvalidEventDates_ThrowsException() {
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventEnd, eventStart, // 시작과 종료 역전
                    regStart, regEnd,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.createEvent(request, OPERATOR_ID))
                    .isInstanceOf(InvalidEventDateException.class);
        }

        /**
         * SVC-EVT-034: regEnd > eventEnd 새 제약 검증
         */
        @Test
        @DisplayName("[SVC-EVT-034] 신청 마감일이 행사 종료일 이후이면 InvalidEventDateException 발생")
        void createEvent_WithRegEndAfterEventEnd_ThrowsException() {
            Instant badRegEnd = eventEnd.plus(1, ChronoUnit.DAYS);
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventEnd, regStart, badRegEnd,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.createEvent(request, OPERATOR_ID))
                    .isInstanceOf(InvalidEventDateException.class);
        }

        /**
         * SVC-EVT-033: regStart >= eventStart 새 제약 검증
         */
        @Test
        @DisplayName("[SVC-EVT-033] 신청 시작일이 행사 시작일 이후이면 InvalidEventDateException 발생")
        void createEvent_WithRegStartAfterEventStart_ThrowsException() {
            Instant badRegStart = eventStart.plus(1, ChronoUnit.DAYS);
            Instant badRegEnd = eventEnd;
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventEnd, badRegStart, badRegEnd,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.createEvent(request, OPERATOR_ID))
                    .isInstanceOf(InvalidEventDateException.class);
        }

        /**
         * SVC-EVT-032: 신청 시작일 미래 제약 검증
         */
        @Test
        @DisplayName("[SVC-EVT-032] 신청 시작일이 현재 시간 이전이면 InvalidEventDateException 발생")
        void createEvent_WithRegStartInPast_ThrowsException() {
            Instant pastRegStart = Instant.now().minus(1, ChronoUnit.DAYS);
            Instant pastRegEnd = Instant.now().plus(5, ChronoUnit.DAYS);
            Instant futureEventStart = Instant.now().plus(10, ChronoUnit.DAYS);
            Instant futureEventEnd = Instant.now().plus(11, ChronoUnit.DAYS);
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    futureEventStart, futureEventEnd, pastRegStart, pastRegEnd,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.createEvent(request, OPERATOR_ID))
                    .isInstanceOf(InvalidEventDateException.class);
        }

        /**
         * EVT-130: regStart == eventStart (무효, 경계값)
         */
        @Test
        @DisplayName("[EVT-130] 신청 시작일 == 행사 시작일이면 InvalidEventDateException 발생")
        void createEvent_WithRegStartEqualsEventStart_ThrowsException() {
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventEnd, eventStart, eventEnd,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.createEvent(request, OPERATOR_ID))
                    .isInstanceOf(InvalidEventDateException.class);
        }

        /**
         * EVT-131: regStart 1ms before eventStart (유효, 경계값)
         */
        @Test
        @DisplayName("[EVT-131] 신청 시작일이 행사 시작일 1ms 전이면 성공")
        void createEvent_WithRegStart1msBeforeEventStart_Success() {
            Instant regStart1msBefore = eventStart.minusMillis(1);
            Instant regEndForTest = eventEnd; // regEnd <= eventEnd 제약 만족
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventEnd, regStart1msBefore, regEndForTest,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
                Event savedEvent = mock(Event.class);
                when(savedEvent.getId()).thenReturn(EVENT_ID);
                when(savedEvent.getTitle()).thenReturn("테스트 행사");
                when(savedEvent.getCreatedAt()).thenReturn(Instant.now());
                return savedEvent;
            });

            EventCreateResponse response = eventService.createEvent(request, OPERATOR_ID);

            assertThat(response).isNotNull();
        }

        /**
         * EVT-132: regEnd == eventStart (유효, 2축 모델)
         */
        @Test
        @DisplayName("[EVT-132] 신청 마감일 == 행사 시작일이면 성공 (2축 모델)")
        void createEvent_WithRegEndEqualsEventStart_Success() {
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventEnd, regStart, eventStart,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
                Event savedEvent = mock(Event.class);
                when(savedEvent.getId()).thenReturn(EVENT_ID);
                when(savedEvent.getTitle()).thenReturn("테스트 행사");
                when(savedEvent.getCreatedAt()).thenReturn(Instant.now());
                return savedEvent;
            });

            EventCreateResponse response = eventService.createEvent(request, OPERATOR_ID);

            assertThat(response).isNotNull();
        }

        /**
         * EVT-133: regEnd == eventEnd (유효)
         */
        @Test
        @DisplayName("[EVT-133] 신청 마감일 == 행사 종료일이면 성공")
        void createEvent_WithRegEndEqualsEventEnd_Success() {
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventEnd, regStart, eventEnd,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
                Event savedEvent = mock(Event.class);
                when(savedEvent.getId()).thenReturn(EVENT_ID);
                when(savedEvent.getTitle()).thenReturn("테스트 행사");
                when(savedEvent.getCreatedAt()).thenReturn(Instant.now());
                return savedEvent;
            });

            EventCreateResponse response = eventService.createEvent(request, OPERATOR_ID);

            assertThat(response).isNotNull();
        }

        /**
         * EVT-135: regStart > regEnd (무효)
         */
        @Test
        @DisplayName("[EVT-135] 신청 시작일 > 신청 마감일이면 InvalidEventDateException 발생")
        void createEvent_WithRegStartAfterRegEnd_ThrowsException() {
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventEnd, regEnd, regStart,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.createEvent(request, OPERATOR_ID))
                    .isInstanceOf(InvalidEventDateException.class);
        }

        /**
         * EVT-137: eventStart == eventEnd (유효)
         */
        @Test
        @DisplayName("[EVT-137] 행사 시작일 == 행사 종료일이면 성공")
        void createEvent_WithEventStartEqualsEventEnd_Success() {
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventStart, regStart, regEnd,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
                Event savedEvent = mock(Event.class);
                when(savedEvent.getId()).thenReturn(EVENT_ID);
                when(savedEvent.getTitle()).thenReturn("테스트 행사");
                when(savedEvent.getCreatedAt()).thenReturn(Instant.now());
                return savedEvent;
            });

            EventCreateResponse response = eventService.createEvent(request, OPERATOR_ID);

            assertThat(response).isNotNull();
        }

        /**
         * EVT-138: regStart == regEnd (무효 — 현재 검증 로직에서는 regStart < regEnd 필요)
         */
        @Test
        @DisplayName("[EVT-138] 신청 시작일 == 신청 마감일이면 InvalidEventDateException 발생")
        void createEvent_WithRegStartEqualsRegEnd_ThrowsException() {
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventEnd, regStart, regStart,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.createEvent(request, OPERATOR_ID))
                    .isInstanceOf(InvalidEventDateException.class);
        }
    }

    // ========== getEvent ==========

    @Nested
    @DisplayName("getEvent - 행사 단건 조회")
    class GetEvent {

        /**
         * SVC-EVT-006: 정회원 행사 단건 조회
         */
        @Test
        @DisplayName("[SVC-EVT-006] 정회원이 유효한 행사 ID로 조회하면 성공한다")
        void getEvent_WithValidId_Success() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(regularMember));
            when(eventRegistrationRepository.existsByEventIdAndUserIdAndStatusIn(
                    eq(EVENT_ID), eq(MEMBER_ID), any(Set.class))).thenReturn(false);

            EventDetailResponse response = eventService.getEvent(EVENT_ID, MEMBER_ID);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(EVENT_ID);
            assertThat(response.title()).isEqualTo("테스트 행사");
            verify(mockEvent).updateStatusIfNeeded(any(Instant.class));
        }

        /**
         * SVC-EVT-007: 삭제된 행사 단건 조회
         */
        @Test
        @DisplayName("[SVC-EVT-007] 삭제된 행사를 조회하면 EventNotFoundException 발생")
        void getEvent_DeletedEvent_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.getEvent(EVENT_ID, MEMBER_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        /**
         * SVC-EVT-008: 준회원 행사 조회 거부
         */
        @Test
        @DisplayName("[SVC-EVT-008] 준회원이 행사를 조회하면 AssociateMemberNotAllowedException 발생")
        void getEvent_WithAssociateMember_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(4L)).thenReturn(Optional.of(associateMember));

            assertThatThrownBy(() -> eventService.getEvent(EVENT_ID, 4L))
                    .isInstanceOf(AssociateMemberNotAllowedException.class);
        }

        /**
         * SVC-EVT-009: 운영진 조회 시 canEdit=true
         */
        @Test
        @DisplayName("[SVC-EVT-009] 운영진이 조회하면 canEdit이 true인 응답을 반환한다")
        void getEvent_ByOperator_ReturnsCanEditTrue() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRegistrationRepository.existsByEventIdAndUserIdAndStatusIn(
                    eq(EVENT_ID), eq(OPERATOR_ID), any(Set.class))).thenReturn(false);

            EventDetailResponse response = eventService.getEvent(EVENT_ID, OPERATOR_ID);

            assertThat(response.canEdit()).isTrue();
        }
    }

    // ========== getEventList ==========

    @Nested
    @DisplayName("getEventList - 행사 목록 조회 (2축 필터)")
    class GetEventList {

        /**
         * SVC-EVT-010: 상태 필터 없이 목록 조회
         */
        @Test
        @DisplayName("[SVC-EVT-010] 필터 없이 전체 조회하면 삭제되지 않은 행사 목록을 반환한다")
        void getEventList_NoFilter_ReturnsAllNotDeleted() {
            when(eventRepository.findAllNotDeleted()).thenReturn(List.of(mockEvent));

            List<EventListResponse> result = eventService.getEventList(null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(EVENT_ID);
            verify(eventRepository).findAllNotDeleted();
            verify(mockEvent).updateStatusIfNeeded(any(Instant.class));
        }

        /**
         * SVC-EVT-011: EventStatus 필터로 목록 조회 (2축 모델)
         */
        @Test
        @DisplayName("[SVC-EVT-011] EventStatus 필터로 조회하면 해당 상태의 행사만 반환한다")
        void getEventList_WithEventStatusFilter_ReturnsFiltered() {
            when(eventRepository.findByEventStatus(EventStatus.UPCOMING)).thenReturn(List.of(mockEvent));

            List<EventListResponse> result = eventService.getEventList(EventStatus.UPCOMING, null);

            assertThat(result).hasSize(1);
            verify(eventRepository).findByEventStatus(EventStatus.UPCOMING);
        }

        /**
         * SVC-EVT-011 확장: RegistrationStatus 필터로 목록 조회
         */
        @Test
        @DisplayName("[SVC-EVT-011-ext] RegistrationStatus 필터로 조회하면 해당 상태의 행사만 반환한다")
        void getEventList_WithRegistrationStatusFilter_ReturnsFiltered() {
            when(eventRepository.findByRegistrationStatus(RegistrationStatus.OPEN)).thenReturn(List.of(mockEvent));

            List<EventListResponse> result = eventService.getEventList(null, RegistrationStatus.OPEN);

            assertThat(result).hasSize(1);
            verify(eventRepository).findByRegistrationStatus(RegistrationStatus.OPEN);
        }

        /**
         * SVC-EVT-012: 행사 없을 때 빈 목록
         */
        @Test
        @DisplayName("[SVC-EVT-012] 행사가 없으면 빈 목록을 반환한다")
        void getEventList_NoEvents_ReturnsEmptyList() {
            when(eventRepository.findAllNotDeleted()).thenReturn(List.of());

            List<EventListResponse> result = eventService.getEventList(null, null);

            assertThat(result).isEmpty();
        }

        /**
         * SVC-EVT-013: Lazy 갱신 후 필터 제외
         */
        @Test
        @DisplayName("[SVC-EVT-013] Lazy 갱신 후 eventStatus가 변경된 행사는 필터에서 제외된다")
        void getEventList_LazyUpdateChangesEventStatus_FilteredOut() {
            // given: UPCOMING 상태로 DB 조회되지만, updateStatusIfNeeded 호출 시 ONGOING으로 변경
            Event changingEvent = mock(Event.class);
            when(changingEvent.getId()).thenReturn(2L);
            when(changingEvent.getTitle()).thenReturn("상태 변경 행사");
            when(changingEvent.getLocation()).thenReturn("장소");
            when(changingEvent.getEventStartAt()).thenReturn(eventStart);
            when(changingEvent.getEventEndAt()).thenReturn(eventEnd);
            when(changingEvent.getRegistrationStartAt()).thenReturn(regStart);
            when(changingEvent.getRegistrationEndAt()).thenReturn(regEnd);
            when(changingEvent.getCapacity()).thenReturn(30);
            when(changingEvent.getCurrentCount()).thenReturn(0);
            when(changingEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.OPEN);
            when(changingEvent.getRegistrationType()).thenReturn(EventRegistrationType.AUTO_APPROVE);
            when(changingEvent.isRegistrable()).thenReturn(false);
            when(changingEvent.getCreatedAt()).thenReturn(Instant.now());
            when(changingEvent.getUpdatedAt()).thenReturn(Instant.now());
            when(changingEvent.getCloseReason()).thenReturn(null);

            // updateStatusIfNeeded 호출 시 eventStatus가 ONGOING으로 변경
            when(changingEvent.getEventStatus()).thenReturn(EventStatus.UPCOMING);
            doAnswer(invocation -> {
                when(changingEvent.getEventStatus()).thenReturn(EventStatus.ONGOING);
                return null;
            }).when(changingEvent).updateStatusIfNeeded(any(Instant.class));

            when(eventRepository.findByEventStatus(EventStatus.UPCOMING))
                    .thenReturn(new java.util.ArrayList<>(List.of(mockEvent, changingEvent)));

            // when: UPCOMING 필터로 조회
            List<EventListResponse> result = eventService.getEventList(EventStatus.UPCOMING, null);

            // then: 상태가 변경된 행사는 제외되어 1개만 반환
            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(EVENT_ID);
        }
    }

    // ========== updateEvent ==========

    @Nested
    @DisplayName("updateEvent - 행사 수정")
    class UpdateEvent {

        /**
         * SVC-EVT-014: 운영진 행사 수정 성공
         */
        @Test
        @DisplayName("[SVC-EVT-014] 운영진이 유효한 요청으로 행사를 수정하면 성공한다")
        void updateEvent_WithValidRequest_Success() {
            UpdateEventRequest request = new UpdateEventRequest(
                    "수정된 제목", "수정된 설명", "수정된 장소",
                    eventStart, eventEnd, regStart, regEnd, 50
            );
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            EventDetailResponse response = eventService.updateEvent(EVENT_ID, request, OPERATOR_ID);

            assertThat(response).isNotNull();
            verify(mockEvent).update("수정된 제목", "수정된 설명", "수정된 장소",
                    eventStart, eventEnd, regStart, regEnd, 50);
        }

        /**
         * SVC-EVT-015: 일반 회원 행사 수정 거부
         */
        @Test
        @DisplayName("[SVC-EVT-015] 일반 회원이 행사를 수정하려고 하면 EventAccessDeniedException 발생")
        void updateEvent_WithRegularMember_ThrowsException() {
            UpdateEventRequest request = new UpdateEventRequest(
                    "수정", "설명", "장소",
                    eventStart, eventEnd, regStart, regEnd, 50
            );
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(regularMember));

            assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, request, MEMBER_ID))
                    .isInstanceOf(EventAccessDeniedException.class);
        }

        /**
         * SVC-EVT-016: 삭제된 행사 수정 거부
         */
        @Test
        @DisplayName("[SVC-EVT-016] 삭제된 행사를 수정하려고 하면 EventNotFoundException 발생")
        void updateEvent_DeletedEvent_ThrowsException() {
            UpdateEventRequest request = new UpdateEventRequest(
                    "수정", "설명", "장소",
                    eventStart, eventEnd, regStart, regEnd, 50
            );
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, request, OPERATOR_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    // ========== deleteEvent ==========

    @Nested
    @DisplayName("deleteEvent - 행사 삭제 (Soft Delete)")
    class DeleteEvent {

        /**
         * SVC-EVT-017: 운영진 행사 삭제 (soft delete)
         */
        @Test
        @DisplayName("[SVC-EVT-017] 운영진이 행사를 삭제하면 soft delete가 수행된다")
        void deleteEvent_WithOperator_SoftDeletes() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            eventService.deleteEvent(EVENT_ID, OPERATOR_ID);

            verify(mockEvent).delete(OPERATOR_ID);
        }

        /**
         * SVC-EVT-018: 일반 회원 행사 삭제 거부
         */
        @Test
        @DisplayName("[SVC-EVT-018] 일반 회원이 삭제하려고 하면 EventAccessDeniedException 발생")
        void deleteEvent_WithRegularMember_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(regularMember));

            assertThatThrownBy(() -> eventService.deleteEvent(EVENT_ID, MEMBER_ID))
                    .isInstanceOf(EventAccessDeniedException.class);
        }

        /**
         * SVC-EVT-019: 이미 삭제된 행사 삭제
         */
        @Test
        @DisplayName("[SVC-EVT-019] 이미 삭제된 행사를 삭제하려고 하면 EventNotFoundException 발생")
        void deleteEvent_AlreadyDeleted_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.deleteEvent(EVENT_ID, OPERATOR_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        /**
         * SVC-EVT-020: 존재하지 않는 사용자 삭제
         */
        @Test
        @DisplayName("[SVC-EVT-020] 존재하지 않는 사용자가 삭제하려고 하면 UserNotFoundException 발생")
        void deleteEvent_NonExistentUser_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.deleteEvent(EVENT_ID, 999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ========== closeEvent ==========

    @Nested
    @DisplayName("closeEvent - 등록 수동 마감")
    class CloseEvent {

        /**
         * SVC-EVT-021: 운영진 행사 수동 마감
         */
        @Test
        @DisplayName("[SVC-EVT-021] 운영진이 등록을 수동 마감하면 성공한다")
        void closeEvent_WithOperator_Success() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            EventDetailResponse response = eventService.closeEvent(EVENT_ID, OPERATOR_ID);

            assertThat(response).isNotNull();
            verify(mockEvent).closeRegistrationManually();
        }

        /**
         * SVC-EVT-022: 일반 회원 행사 마감 거부
         */
        @Test
        @DisplayName("[SVC-EVT-022] 일반 회원이 마감하려고 하면 EventAccessDeniedException 발생")
        void closeEvent_WithRegularMember_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(regularMember));

            assertThatThrownBy(() -> eventService.closeEvent(EVENT_ID, MEMBER_ID))
                    .isInstanceOf(EventAccessDeniedException.class);
        }

        /**
         * SVC-EVT-023: 삭제된 행사 마감 거부
         */
        @Test
        @DisplayName("[SVC-EVT-023] 삭제된 행사를 마감하려고 하면 EventNotFoundException 발생")
        void closeEvent_DeletedEvent_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.closeEvent(EVENT_ID, OPERATOR_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    // ========== cancelEvent ==========

    @Nested
    @DisplayName("cancelEvent - 행사 취소")
    class CancelEvent {

        /**
         * SVC-EVT-024: 운영진 행사 취소 성공
         */
        @Test
        @DisplayName("[SVC-EVT-024] 운영진이 행사를 취소하면 성공한다")
        void cancelEvent_WithOperator_Success() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            EventDetailResponse response = eventService.cancelEvent(EVENT_ID, OPERATOR_ID);

            assertThat(response).isNotNull();
            verify(mockEvent).cancel();
        }

        /**
         * SVC-EVT-025: 일반 회원 행사 취소 거부
         */
        @Test
        @DisplayName("[SVC-EVT-025] 일반 회원이 취소하려고 하면 EventAccessDeniedException 발생")
        void cancelEvent_WithRegularMember_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(regularMember));

            assertThatThrownBy(() -> eventService.cancelEvent(EVENT_ID, MEMBER_ID))
                    .isInstanceOf(EventAccessDeniedException.class);
        }

        /**
         * SVC-EVT-026: COMPLETED 행사 취소 거부
         */
        @Test
        @DisplayName("[SVC-EVT-026] COMPLETED 행사를 취소하려고 하면 InvalidEventStateTransitionException 발생")
        void cancelEvent_CompletedEvent_ThrowsException() {
            when(mockEvent.getEventStatus()).thenReturn(EventStatus.COMPLETED);
            doThrow(new InvalidEventStateTransitionException(EventStatus.COMPLETED, EventStatus.CANCELED))
                    .when(mockEvent).cancel();
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.cancelEvent(EVENT_ID, OPERATOR_ID))
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }

        /**
         * SVC-EVT-026-2: 삭제된 행사 취소 거부
         */
        @Test
        @DisplayName("[SVC-EVT-026-2] 삭제된 행사를 취소하려고 하면 EventNotFoundException 발생")
        void cancelEvent_DeletedEvent_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.cancelEvent(EVENT_ID, OPERATOR_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    // ========== reactivateEvent ==========

    @Nested
    @DisplayName("reactivateEvent - 행사 재활성화")
    class ReactivateEvent {

        /**
         * SVC-EVT-027: 운영진 행사 재활성화 성공
         */
        @Test
        @DisplayName("[SVC-EVT-027] 운영진이 취소된 행사를 재활성화하면 성공한다")
        void reactivateEvent_WithOperator_Success() {
            when(mockEvent.getEventStatus()).thenReturn(EventStatus.CANCELED);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            EventDetailResponse response = eventService.reactivateEvent(EVENT_ID, OPERATOR_ID);

            assertThat(response).isNotNull();
            verify(mockEvent).reactivate(any(Instant.class));
        }

        /**
         * SVC-EVT-028: 일반 회원 행사 재활성화 거부
         */
        @Test
        @DisplayName("[SVC-EVT-028] 일반 회원이 재활성화하려고 하면 EventAccessDeniedException 발생")
        void reactivateEvent_WithRegularMember_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(regularMember));

            assertThatThrownBy(() -> eventService.reactivateEvent(EVENT_ID, MEMBER_ID))
                    .isInstanceOf(EventAccessDeniedException.class);
        }

        /**
         * SVC-EVT-029: 이미 활성 상태 재활성화 거부
         */
        @Test
        @DisplayName("[SVC-EVT-029] 이미 활성(UPCOMING) 상태인 행사를 재활성화하려고 하면 InvalidEventStateTransitionException 발생")
        void reactivateEvent_AlreadyActiveEvent_ThrowsException() {
            when(mockEvent.getEventStatus()).thenReturn(EventStatus.UPCOMING);
            doThrow(new InvalidEventStateTransitionException(EventStatus.UPCOMING, EventStatus.UPCOMING))
                    .when(mockEvent).reactivate(any(Instant.class));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.reactivateEvent(EVENT_ID, OPERATOR_ID))
                    .isInstanceOf(InvalidEventStateTransitionException.class);
        }

        /**
         * SVC-EVT-029-2: 삭제된 행사 재활성화 거부
         */
        @Test
        @DisplayName("[SVC-EVT-029-2] 삭제된 행사를 재활성화하려고 하면 EventNotFoundException 발생")
        void reactivateEvent_DeletedEvent_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.reactivateEvent(EVENT_ID, OPERATOR_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

    // ========== reopenRegistration ==========

    @Nested
    @DisplayName("reopenRegistration - 등록 수동 재오픈 (EVT-INV-13)")
    class ReopenRegistration {

        /**
         * SVC-EVT-030: 운영진 수동 재오픈 성공 (5가지 조건 충족)
         */
        @Test
        @DisplayName("[SVC-EVT-030] 모든 조건 충족 시 등록 재오픈에 성공한다")
        void reopenRegistration_AllConditionsMet_Success() {
            when(mockEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.CLOSED);
            when(mockEvent.getEventStatus()).thenReturn(EventStatus.UPCOMING);
            when(mockEvent.isFull()).thenReturn(false);
            when(mockEvent.getRegistrationEndAt()).thenReturn(Instant.now().plus(7, ChronoUnit.DAYS));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            EventDetailResponse response = eventService.reopenRegistration(EVENT_ID, OPERATOR_ID, "추가 모집 필요");

            assertThat(response).isNotNull();
            verify(mockEvent).reopenRegistration();
            verify(eventReopenHistoryRepository).save(any(EventReopenHistory.class));
        }

        /**
         * SVC-EVT-030 실패: 등록이 마감 상태가 아닌 경우
         */
        @Test
        @DisplayName("[SVC-EVT-030-fail-1] 등록이 마감 상태가 아니면 EventRegistrationNotReopenableException 발생")
        void reopenRegistration_NotClosed_ThrowsException() {
            when(mockEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.OPEN);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.reopenRegistration(EVENT_ID, OPERATOR_ID, "사유"))
                    .isInstanceOf(EventRegistrationNotReopenableException.class);
        }

        /**
         * SVC-EVT-030 실패: 행사가 COMPLETED 상태인 경우
         */
        @Test
        @DisplayName("[SVC-EVT-030-fail-2] 행사가 COMPLETED 상태이면 EventRegistrationNotReopenableException 발생")
        void reopenRegistration_CompletedEvent_ThrowsException() {
            when(mockEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.CLOSED);
            when(mockEvent.getEventStatus()).thenReturn(EventStatus.COMPLETED);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.reopenRegistration(EVENT_ID, OPERATOR_ID, "사유"))
                    .isInstanceOf(EventRegistrationNotReopenableException.class);
        }

        /**
         * SVC-EVT-030 실패: 행사가 CANCELED 상태인 경우
         */
        @Test
        @DisplayName("[SVC-EVT-030-fail-3] 행사가 CANCELED 상태이면 EventRegistrationNotReopenableException 발생")
        void reopenRegistration_CanceledEvent_ThrowsException() {
            when(mockEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.CLOSED);
            when(mockEvent.getEventStatus()).thenReturn(EventStatus.CANCELED);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.reopenRegistration(EVENT_ID, OPERATOR_ID, "사유"))
                    .isInstanceOf(EventRegistrationNotReopenableException.class);
        }

        /**
         * SVC-EVT-030 실패: 정원이 가득 찬 경우
         */
        @Test
        @DisplayName("[SVC-EVT-030-fail-4] 정원이 가득 차면 EventRegistrationNotReopenableException 발생")
        void reopenRegistration_FullCapacity_ThrowsException() {
            when(mockEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.CLOSED);
            when(mockEvent.getEventStatus()).thenReturn(EventStatus.UPCOMING);
            when(mockEvent.isFull()).thenReturn(true);
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.reopenRegistration(EVENT_ID, OPERATOR_ID, "사유"))
                    .isInstanceOf(EventRegistrationNotReopenableException.class);
        }

        /**
         * SVC-EVT-030 실패: 등록 마감일 경과
         */
        @Test
        @DisplayName("[SVC-EVT-030-fail-5] 등록 마감일이 경과하면 EventRegistrationNotReopenableException 발생")
        void reopenRegistration_DeadlinePassed_ThrowsException() {
            when(mockEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.CLOSED);
            when(mockEvent.getEventStatus()).thenReturn(EventStatus.UPCOMING);
            when(mockEvent.isFull()).thenReturn(false);
            when(mockEvent.getRegistrationEndAt()).thenReturn(Instant.now().minus(1, ChronoUnit.DAYS));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            assertThatThrownBy(() -> eventService.reopenRegistration(EVENT_ID, OPERATOR_ID, "사유"))
                    .isInstanceOf(EventRegistrationNotReopenableException.class);
        }

        /**
         * EVT-125: 수동 재오픈 후 감사 이력 기록 확인
         */
        @Test
        @DisplayName("[EVT-125] 수동 재오픈 성공 시 EventReopenHistory에 사유와 운영자 ID가 기록된다")
        void reopenRegistration_Success_SavesAuditHistory() {
            when(mockEvent.getRegistrationStatus()).thenReturn(RegistrationStatus.CLOSED);
            when(mockEvent.getEventStatus()).thenReturn(EventStatus.UPCOMING);
            when(mockEvent.isFull()).thenReturn(false);
            when(mockEvent.getRegistrationEndAt()).thenReturn(Instant.now().plus(7, ChronoUnit.DAYS));
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            eventService.reopenRegistration(EVENT_ID, OPERATOR_ID, "추가 모집 사유");

            verify(eventReopenHistoryRepository).save(argThat(history -> {
                assertThat(history.getReason()).isEqualTo("추가 모집 사유");
                assertThat(history.getReopenedBy()).isEqualTo(OPERATOR_ID);
                assertThat(history.getEvent()).isEqualTo(mockEvent);
                assertThat(history.getReopenedAt()).isNotNull();
                return true;
            }));
        }

        /**
         * SVC-EVT-031: 일반 회원 수동 재오픈 거부
         */
        @Test
        @DisplayName("[SVC-EVT-031] 일반 회원이 재오픈하려고 하면 EventAccessDeniedException 발생")
        void reopenRegistration_WithRegularMember_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(regularMember));

            assertThatThrownBy(() -> eventService.reopenRegistration(EVENT_ID, MEMBER_ID, "사유"))
                    .isInstanceOf(EventAccessDeniedException.class);
        }
    }

}
