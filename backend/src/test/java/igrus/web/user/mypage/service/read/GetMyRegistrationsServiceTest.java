package igrus.web.user.mypage.service.read;

import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.dto.response.MyRegistrationResponse;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static igrus.web.common.fixture.TestEntityIdAssigner.withId;
import static igrus.web.common.fixture.UserTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * GetMyRegistrationsService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>MP-008: 내 행사 신청 목록 조회 성공</li>
 *     <li>MP-009: 신청 없는 경우 빈 리스트 반환</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetMyRegistrationsService 단위 테스트")
class GetMyRegistrationsServiceTest {

    @Mock
    private EventRegistrationRepository eventRegistrationRepository;

    @InjectMocks
    private GetMyRegistrationsService getMyRegistrationsService;

    private User memberUser;
    private User operatorUser;

    @BeforeEach
    void setUp() {
        memberUser = createMemberWithId();
        operatorUser = createOperatorWithId();
    }

    private Event createTestEvent(User organizer, Long id, String title) {
        Event event = Event.create(
                organizer, title, "설명", "장소",
                Instant.parse("2026-03-01T10:00:00Z"),
                Instant.parse("2026-03-01T18:00:00Z"),
                Instant.parse("2026-02-01T00:00:00Z"),
                Instant.parse("2026-02-28T23:59:59Z"),
                30, EventRegistrationType.AUTO_APPROVE
        );
        return withId(event, id);
    }

    @Nested
    @DisplayName("내 행사 신청 목록 조회 테스트")
    class GetMyRegistrationsTest {

        @DisplayName("MP-008: 내 행사 신청 목록 조회 성공")
        @Test
        void getMyRegistrations_ReturnsRegistrations() {
            // given
            Long userId = memberUser.getId();

            Event event1 = createTestEvent(operatorUser, 1L, "행사1");
            Event event2 = createTestEvent(operatorUser, 2L, "행사2");

            EventRegistration reg1 = withId(EventRegistration.create(event1, memberUser), 1L);
            EventRegistration reg2 = withId(EventRegistration.create(event2, memberUser), 2L);

            given(eventRegistrationRepository.findByUserId(userId))
                    .willReturn(List.of(reg1, reg2));

            // when
            List<MyRegistrationResponse> result = getMyRegistrationsService.getMyRegistrations(userId);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).eventId()).isEqualTo(1L);
            assertThat(result.get(0).eventTitle()).isEqualTo("행사1");
            assertThat(result.get(1).eventId()).isEqualTo(2L);
            assertThat(result.get(1).eventTitle()).isEqualTo("행사2");
        }

        @DisplayName("MP-009: 신청 없는 경우 빈 리스트 반환")
        @Test
        void getMyRegistrations_WhenEmpty_ReturnsEmptyList() {
            // given
            Long userId = memberUser.getId();

            given(eventRegistrationRepository.findByUserId(userId))
                    .willReturn(List.of());

            // when
            List<MyRegistrationResponse> result = getMyRegistrationsService.getMyRegistrations(userId);

            // then
            assertThat(result).isEmpty();
        }
    }
}
