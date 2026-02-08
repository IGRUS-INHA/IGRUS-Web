package igrus.web.event.service;

import igrus.web.event.domain.*;
import igrus.web.event.dto.request.CreateEventRequest;
import igrus.web.event.dto.request.UpdateEventRequest;
import igrus.web.event.dto.response.EventCreateResponse;
import igrus.web.event.dto.response.EventDetailResponse;
import igrus.web.event.dto.response.EventListResponse;
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

    // 날짜 설정: 신청 시작 → 신청 마감 → 행사 시작 → 행사 종료
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
        when(event.getStatus()).thenReturn(EventStatus.OPEN);
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

        @Test
        @DisplayName("운영진이 유효한 요청으로 행사를 생성하면 성공한다")
        void createEvent_WithValidRequest_Success() {
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventEnd, regStart, regEnd,
                    30, EventRegistrationType.AUTO_APPROVE
            );
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));
            when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
                Event saved = invocation.getArgument(0);
                // save 이후 ID가 부여된 것처럼 mock
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

        @Test
        @DisplayName("일반 회원이 행사를 생성하려고 하면 EventAccessDeniedException 발생")
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

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 행사를 생성하면 UserNotFoundException 발생")
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

        @Test
        @DisplayName("행사 종료일이 시작일보다 이전이면 InvalidEventDateException 발생")
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

        @Test
        @DisplayName("신청 마감일이 행사 시작일 이후이면 InvalidEventDateException 발생")
        void createEvent_WithRegEndAfterEventStart_ThrowsException() {
            // 신청 마감일이 행사 시작일 이후
            Instant badRegEnd = eventStart.plus(1, ChronoUnit.DAYS);
            CreateEventRequest request = new CreateEventRequest(
                    "테스트 행사", "설명", "장소",
                    eventStart, eventEnd, regStart, badRegEnd,
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

        @Test
        @DisplayName("정회원이 유효한 행사 ID로 조회하면 성공한다")
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

        @Test
        @DisplayName("삭제된 행사를 조회하면 EventNotFoundException 발생")
        void getEvent_DeletedEvent_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.getEvent(EVENT_ID, MEMBER_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("준회원이 행사를 조회하면 AssociateMemberNotAllowedException 발생")
        void getEvent_WithAssociateMember_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(4L)).thenReturn(Optional.of(associateMember));

            assertThatThrownBy(() -> eventService.getEvent(EVENT_ID, 4L))
                    .isInstanceOf(AssociateMemberNotAllowedException.class);
        }

        @Test
        @DisplayName("운영진이 조회하면 canEdit이 true인 응답을 반환한다")
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
    @DisplayName("getEventList - 행사 목록 조회")
    class GetEventList {

        @Test
        @DisplayName("상태 필터 없이 전체 조회하면 삭제되지 않은 행사 목록을 반환한다")
        void getEventList_NoFilter_ReturnsAllNotDeleted() {
            when(eventRepository.findAllNotDeleted()).thenReturn(List.of(mockEvent));

            List<EventListResponse> result = eventService.getEventList(null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(EVENT_ID);
            verify(eventRepository).findAllNotDeleted();
            verify(mockEvent).updateStatusIfNeeded(any(Instant.class));
        }

        @Test
        @DisplayName("OPEN 상태 필터로 조회하면 해당 상태의 행사만 반환한다")
        void getEventList_WithStatusFilter_ReturnsFiltered() {
            when(eventRepository.findByStatusAndNotDeleted(EventStatus.OPEN)).thenReturn(List.of(mockEvent));

            List<EventListResponse> result = eventService.getEventList(EventStatus.OPEN);

            assertThat(result).hasSize(1);
            verify(eventRepository).findByStatusAndNotDeleted(EventStatus.OPEN);
        }

        @Test
        @DisplayName("행사가 없으면 빈 목록을 반환한다")
        void getEventList_NoEvents_ReturnsEmptyList() {
            when(eventRepository.findAllNotDeleted()).thenReturn(List.of());

            List<EventListResponse> result = eventService.getEventList(null);

            assertThat(result).isEmpty();
        }
    }

    // ========== updateEvent ==========

    @Nested
    @DisplayName("updateEvent - 행사 수정")
    class UpdateEvent {

        @Test
        @DisplayName("운영진이 유효한 요청으로 행사를 수정하면 성공한다")
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

        @Test
        @DisplayName("일반 회원이 행사를 수정하려고 하면 EventAccessDeniedException 발생")
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

        @Test
        @DisplayName("삭제된 행사를 수정하려고 하면 EventNotFoundException 발생")
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

        @Test
        @DisplayName("운영진이 행사를 삭제하면 soft delete가 수행된다")
        void deleteEvent_WithOperator_SoftDeletes() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            eventService.deleteEvent(EVENT_ID, OPERATOR_ID);

            verify(mockEvent).delete(OPERATOR_ID);
        }

        @Test
        @DisplayName("일반 회원이 삭제하려고 하면 EventAccessDeniedException 발생")
        void deleteEvent_WithRegularMember_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(regularMember));

            assertThatThrownBy(() -> eventService.deleteEvent(EVENT_ID, MEMBER_ID))
                    .isInstanceOf(EventAccessDeniedException.class);
        }

        @Test
        @DisplayName("이미 삭제된 행사를 삭제하려고 하면 EventNotFoundException 발생")
        void deleteEvent_AlreadyDeleted_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.deleteEvent(EVENT_ID, OPERATOR_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자가 삭제하려고 하면 UserNotFoundException 발생")
        void deleteEvent_NonExistentUser_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.deleteEvent(EVENT_ID, 999L))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    // ========== closeEvent ==========

    @Nested
    @DisplayName("closeEvent - 행사 수동 마감")
    class CloseEvent {

        @Test
        @DisplayName("운영진이 행사를 수동 마감하면 성공한다")
        void closeEvent_WithOperator_Success() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(OPERATOR_ID)).thenReturn(Optional.of(operator));

            EventDetailResponse response = eventService.closeEvent(EVENT_ID, OPERATOR_ID);

            assertThat(response).isNotNull();
            verify(mockEvent).closeManually();
        }

        @Test
        @DisplayName("일반 회원이 마감하려고 하면 EventAccessDeniedException 발생")
        void closeEvent_WithRegularMember_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.of(mockEvent));
            when(userRepository.findById(MEMBER_ID)).thenReturn(Optional.of(regularMember));

            assertThatThrownBy(() -> eventService.closeEvent(EVENT_ID, MEMBER_ID))
                    .isInstanceOf(EventAccessDeniedException.class);
        }

        @Test
        @DisplayName("삭제된 행사를 마감하려고 하면 EventNotFoundException 발생")
        void closeEvent_DeletedEvent_ThrowsException() {
            when(eventRepository.findByIdAndNotDeleted(EVENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.closeEvent(EVENT_ID, OPERATOR_ID))
                    .isInstanceOf(EventNotFoundException.class);
        }
    }

}
