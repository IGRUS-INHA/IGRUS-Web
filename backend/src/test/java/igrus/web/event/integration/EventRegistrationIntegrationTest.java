package igrus.web.event.integration;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.event.service.EventRegistrationService;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 행사 신청 통합 테스트.
 *
 * <p>영속성 컨텍스트와 @Modifying 쿼리의 상호작용을 검증합니다.
 * Mockito 단위 테스트에서는 발견할 수 없는 flush/clear 관련 버그를 검출합니다.</p>
 *
 * <p>테스트 케이스:</p>
 * <ul>
 *     <li>INT-001: 선착순 행사 신청 → 취소 → DB 상태 검증</li>
 *     <li>INT-002: 선착순 행사 신청 → 취소 → 재신청 → DB 상태 검증</li>
 *     <li>INT-003: 선발제 행사 신청 → 취소 → 재신청 → DB 상태 검증</li>
 * </ul>
 */
@DisplayName("행사 신청 통합 테스트")
class EventRegistrationIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private EventRegistrationService eventRegistrationService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    private User member;
    private User operator;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            operator = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);
            member = createAndSaveUser("20230002", "member@inha.edu", UserRole.MEMBER);
            return null;
        });
    }

    private Event createAndSaveAutoApproveEvent() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator,
                    "테스트 행사",
                    "설명",
                    "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    10,
                    EventRegistrationType.AUTO_APPROVE
            );
            event.open();
            return eventRepository.save(event);
        });
    }

    private Event createAndSaveManualApproveEvent() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator,
                    "선발제 행사",
                    "설명",
                    "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    10,
                    EventRegistrationType.MANUAL_APPROVE
            );
            event.open();
            return eventRepository.save(event);
        });
    }

    @Nested
    @DisplayName("선착순(AUTO_APPROVE) 행사 - 신청/취소/재신청 흐름")
    class AutoApproveFlowTest {

        /**
         * INT-001: 선착순 행사 취소 시 DB에 CANCELED 상태가 반영되는지 검증.
         * flushAutomatically 누락 시 decrementCurrentCount의 clearAutomatically가
         * EventRegistration의 cancel() 변경을 소실시키는 버그를 검출.
         */
        @Test
        @DisplayName("[INT-001] 선착순 행사 신청 후 취소하면 DB에 CANCELED 상태가 반영됨")
        void registerAndCancel_AutoApprove_CanceledStatusPersistedInDb() {
            // given
            Event event = createAndSaveAutoApproveEvent();

            // when: 신청
            eventRegistrationService.registerEvent(event.getId(), member.getId());

            // then: DB에서 REGISTERED 확인
            EventRegistration afterRegister = eventRegistrationRepository
                    .findByEventIdAndUserId(event.getId(), member.getId())
                    .orElseThrow();
            assertThat(afterRegister.getStatus()).isEqualTo(EventRegistrationStatus.REGISTERED);

            // when: 취소
            eventRegistrationService.cancelRegistration(event.getId(), member.getId());

            // then: DB에서 CANCELED 확인 (flush 누락 시 여전히 REGISTERED)
            EventRegistration afterCancel = eventRegistrationRepository
                    .findByEventIdAndUserId(event.getId(), member.getId())
                    .orElseThrow();
            assertThat(afterCancel.getStatus()).isEqualTo(EventRegistrationStatus.CANCELED);

            // then: 행사 currentCount가 0으로 복원됨
            Event updatedEvent = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(updatedEvent.getCurrentCount()).isEqualTo(0);
        }

        /**
         * INT-002: 선착순 행사 신청 → 취소 → 재신청 전체 흐름 검증.
         * 두 가지 버그를 동시에 검출:
         * 1. cancel() 변경 소실 → 재신청 시 AlreadyRegisteredException
         * 2. reRegister() 변경 소실 → DB에 CANCELED 상태 유지
         */
        @Test
        @DisplayName("[INT-002] 선착순 행사 신청 → 취소 → 재신청하면 DB에 REGISTERED 상태가 반영됨")
        void registerCancelAndReRegister_AutoApprove_ReRegisteredStatusPersistedInDb() {
            // given
            Event event = createAndSaveAutoApproveEvent();

            // when: 신청 → 취소 → 재신청
            eventRegistrationService.registerEvent(event.getId(), member.getId());
            eventRegistrationService.cancelRegistration(event.getId(), member.getId());
            eventRegistrationService.registerEvent(event.getId(), member.getId());

            // then: DB에서 REGISTERED 확인
            EventRegistration afterReRegister = eventRegistrationRepository
                    .findByEventIdAndUserId(event.getId(), member.getId())
                    .orElseThrow();
            assertThat(afterReRegister.getStatus()).isEqualTo(EventRegistrationStatus.REGISTERED);

            // then: 행사 currentCount가 1
            Event updatedEvent = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(updatedEvent.getCurrentCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("선발제(MANUAL_APPROVE) 행사 - 신청/취소/재신청 흐름")
    class ManualApproveFlowTest {

        /**
         * INT-003: 선발제 행사 신청 → 취소 → 재신청.
         * WAITING 상태 취소는 decrementCurrentCount를 호출하지 않으므로
         * clearAutomatically 문제가 발생하지 않는 것을 검증.
         */
        @Test
        @DisplayName("[INT-003] 선발제 행사 신청 → 취소 → 재신청하면 DB에 WAITING 상태가 반영됨")
        void registerCancelAndReRegister_ManualApprove_WaitingStatusPersistedInDb() {
            // given
            Event event = createAndSaveManualApproveEvent();

            // when: 신청 → 취소 → 재신청
            eventRegistrationService.registerEvent(event.getId(), member.getId());
            eventRegistrationService.cancelRegistration(event.getId(), member.getId());
            eventRegistrationService.registerEvent(event.getId(), member.getId());

            // then: DB에서 WAITING 확인
            EventRegistration afterReRegister = eventRegistrationRepository
                    .findByEventIdAndUserId(event.getId(), member.getId())
                    .orElseThrow();
            assertThat(afterReRegister.getStatus()).isEqualTo(EventRegistrationStatus.WAITING);
        }
    }
}
