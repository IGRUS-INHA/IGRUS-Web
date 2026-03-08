package igrus.web.event.dto;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.domain.EventVisibility;
import igrus.web.event.domain.RegistrationStatus;
import igrus.web.event.dto.response.EventDetailResponse;
import igrus.web.event.dto.response.EventListResponse;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * EventDetailResponse, EventListResponse의 visibility 필드 포함 검증 테스트.
 * 관련 검증 기준: EVT-INV-22 (Visibility DTO 필드 포함)
 * 관련 테스트 케이스: GAP-EVT-38
 *
 * @see igrus.web.event.dto.response.EventDetailResponse
 * @see igrus.web.event.dto.response.EventListResponse
 */
@DisplayName("Event DTO visibility 필드 테스트")
class EventResponseVisibilityTest {

    private static final Instant NOW = Instant.now();
    private static final Instant EVENT_START = NOW.plus(14, ChronoUnit.DAYS);
    private static final Instant EVENT_END = NOW.plus(15, ChronoUnit.DAYS);
    private static final Instant REG_START = NOW.plus(1, ChronoUnit.DAYS);
    private static final Instant REG_END = NOW.plus(7, ChronoUnit.DAYS);

    @Nested
    @DisplayName("EventDetailResponse visibility 필드")
    class EventDetailResponseVisibilityTest {

        @Test
        @DisplayName("[GAP-EVT-38] EventDetailResponse.from()에 UNPUBLISHED visibility가 정상 매핑된다")
        void from_UnpublishedEvent_MapsVisibilityCorrectly() {
            Event event = createMockEvent(EventVisibility.UNPUBLISHED);

            EventDetailResponse response = EventDetailResponse.from(event, true, false);

            assertThat(response.visibility()).isEqualTo(EventVisibility.UNPUBLISHED);
        }

        @Test
        @DisplayName("[GAP-EVT-38] EventDetailResponse.from()에 PUBLISHED visibility가 정상 매핑된다")
        void from_PublishedEvent_MapsVisibilityCorrectly() {
            Event event = createMockEvent(EventVisibility.PUBLISHED);

            EventDetailResponse response = EventDetailResponse.from(event, false, false);

            assertThat(response.visibility()).isEqualTo(EventVisibility.PUBLISHED);
        }

        @Test
        @DisplayName("[GAP-EVT-38] EventDetailResponse.from(event) 단일 인자에서도 visibility가 매핑된다")
        void from_SingleArg_MapsVisibilityCorrectly() {
            Event event = createMockEvent(EventVisibility.PUBLISHED);

            EventDetailResponse response = EventDetailResponse.from(event);

            assertThat(response.visibility()).isEqualTo(EventVisibility.PUBLISHED);
        }
    }

    @Nested
    @DisplayName("EventListResponse visibility 필드")
    class EventListResponseVisibilityTest {

        @Test
        @DisplayName("[GAP-EVT-38] EventListResponse.from()에 UNPUBLISHED visibility가 정상 매핑된다")
        void from_UnpublishedEvent_MapsVisibilityCorrectly() {
            Event event = createMockEvent(EventVisibility.UNPUBLISHED);

            EventListResponse response = EventListResponse.from(event);

            assertThat(response.visibility()).isEqualTo(EventVisibility.UNPUBLISHED);
        }

        @Test
        @DisplayName("[GAP-EVT-38] EventListResponse.from()에 PUBLISHED visibility가 정상 매핑된다")
        void from_PublishedEvent_MapsVisibilityCorrectly() {
            Event event = createMockEvent(EventVisibility.PUBLISHED);

            EventListResponse response = EventListResponse.from(event);

            assertThat(response.visibility()).isEqualTo(EventVisibility.PUBLISHED);
        }
    }

    // === Helper ===

    private Event createMockEvent(EventVisibility visibility) {
        User mockUser = mock(User.class);
        when(mockUser.getName()).thenReturn("테스트 운영자");

        Event event = mock(Event.class);
        when(event.getId()).thenReturn(1L);
        when(event.getTitle()).thenReturn("테스트 행사");
        when(event.getDescription()).thenReturn("설명");
        when(event.getLocation()).thenReturn("장소");
        when(event.getUser()).thenReturn(mockUser);
        when(event.getEventStartAt()).thenReturn(EVENT_START);
        when(event.getEventEndAt()).thenReturn(EVENT_END);
        when(event.getRegistrationStartAt()).thenReturn(REG_START);
        when(event.getRegistrationEndAt()).thenReturn(REG_END);
        when(event.getCapacity()).thenReturn(30);
        when(event.getCurrentCount()).thenReturn(0);
        when(event.getVisibility()).thenReturn(visibility);
        when(event.getRegistrationStatus()).thenReturn(RegistrationStatus.NOT_STARTED);
        when(event.getEventStatus()).thenReturn(EventStatus.UPCOMING);
        when(event.getRegistrationType()).thenReturn(EventRegistrationType.AUTO_APPROVE);
        when(event.isRegistrable()).thenReturn(false);
        when(event.getCloseReason()).thenReturn(null);
        when(event.getCreatedAt()).thenReturn(NOW);
        when(event.getUpdatedAt()).thenReturn(NOW);
        return event;
    }
}
