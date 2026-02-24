package igrus.web.event.integration;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventCloseReason;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.domain.EventChangeType;
import igrus.web.event.domain.EventStatusChangeHistory;
import igrus.web.event.domain.EventStatus;
import igrus.web.event.domain.RegistrationStatus;
import igrus.web.event.dto.request.UpdateEventRequest;
import igrus.web.event.exception.EventNotEditableException;
import igrus.web.event.exception.EventNotOpenException;
import igrus.web.event.exception.EventRegistrationNotReopenableException;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.event.repository.EventStatusChangeHistoryRepository;
import igrus.web.event.service.EventRegistrationService;
import igrus.web.event.service.EventService;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
 *     <li>INT-004: OPEN+ONGOING 겹침 기간에서 신청 성공</li>
 *     <li>INT-005: 겹침 기간 정원 마감 → 취소 → 자동 재오픈</li>
 *     <li>INT-006: 겹침 기간 중 수동 마감 후 행사 계속</li>
 *     <li>INT-007: 행사 취소 → registrationStatus=CLOSED 강제 DB 검증</li>
 *     <li>INT-008: 행사 취소 → 재활성화 → Lazy Evaluation 복원</li>
 *     <li>INT-009: CANCELED 행사에서 신청 시도 차단</li>
 *     <li>INT-010: COMPLETED 행사에서 승인 시도 차단</li>
 *     <li>INT-011: 수동 마감 → 수동 재오픈 전체 흐름 (감사 이력 포함)</li>
 *     <li>INT-012: 기한 만료 후 수동 재오픈 거부 → regEnd 연장 → 재오픈 성공</li>
 *     <li>INT-013: ONGOING 중 수동 마감 후 수동 재오픈</li>
 *     <li>INT-014: ONGOING 부분 수정 DB 반영</li>
 *     <li>INT-015: CANCELED 수정 → 재활성화 E2E</li>
 *     <li>INT-016: Lazy 두 축 동시 전이 DB 반영</li>
 *     <li>INT-017: 선발제 신청 → 승인 → 정원 마감 → 되돌리기 → 재오픈</li>
 *     <li>INT-018: 선발제 registrationStatus=CLOSED 후 승인 가능</li>
 * </ul>
 */
@DisplayName("행사 신청 통합 테스트")
class EventRegistrationIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private EventRegistrationService eventRegistrationService;

    @Autowired
    private EventService eventService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    @Autowired
    private EventStatusChangeHistoryRepository eventStatusChangeHistoryRepository;

    private User member;
    private User member2;
    private User operator;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            operator = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);
            member = createAndSaveUser("20230002", "member@inha.edu", UserRole.MEMBER);
            member2 = createAndSaveUser("20230003", "member2@inha.edu", UserRole.MEMBER);
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
            event.openRegistration();
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
            event.openRegistration();
            return eventRepository.save(event);
        });
    }

    /**
     * 등록 기간과 행사 기간이 겹치는 행사를 생성합니다.
     * regStart < eventStart < now < regEnd < eventEnd
     * Lazy evaluation으로 OPEN + ONGOING 상태로 전이됩니다.
     */
    private Event createAndSaveOverlapEvent() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator, "겹침 행사", "설명", "장소",
                    now.minus(1, ChronoUnit.DAYS),    // eventStart: past → ONGOING
                    now.plus(5, ChronoUnit.DAYS),     // eventEnd: future
                    now.minus(3, ChronoUnit.DAYS),    // regStart: past → OPEN
                    now.plus(3, ChronoUnit.DAYS),     // regEnd: future → still OPEN
                    10,
                    EventRegistrationType.AUTO_APPROVE
            );
            return eventRepository.save(event);
        });
    }

    /**
     * 등록 기간과 행사 기간이 겹치는 행사를 지정된 정원으로 생성합니다.
     */
    private Event createAndSaveOverlapEventWithCapacity(int capacity) {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator, "겹침 행사", "설명", "장소",
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(5, ChronoUnit.DAYS),
                    now.minus(3, ChronoUnit.DAYS),
                    now.plus(3, ChronoUnit.DAYS),
                    capacity,
                    EventRegistrationType.AUTO_APPROVE
            );
            return eventRepository.save(event);
        });
    }

    /**
     * 선발제(MANUAL_APPROVE) 겹침 기간 행사를 생성합니다.
     */
    private Event createAndSaveOverlapManualEvent(int capacity) {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator, "선발제 겹침 행사", "설명", "장소",
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(5, ChronoUnit.DAYS),
                    now.minus(3, ChronoUnit.DAYS),
                    now.plus(3, ChronoUnit.DAYS),
                    capacity,
                    EventRegistrationType.MANUAL_APPROVE
            );
            return eventRepository.save(event);
        });
    }

    /**
     * 미래 행사를 생성합니다. (등록 시작 전)
     */
    private Event createAndSaveFutureEvent() {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator, "미래 행사", "설명", "장소",
                    now.plus(7, ChronoUnit.DAYS),     // eventStart: future
                    now.plus(8, ChronoUnit.DAYS),     // eventEnd: future
                    now.plus(1, ChronoUnit.DAYS),     // regStart: future
                    now.plus(6, ChronoUnit.DAYS),     // regEnd: future
                    10,
                    EventRegistrationType.AUTO_APPROVE
            );
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

    @Nested
    @DisplayName("2축 모델 — 겹침 기간 시나리오")
    class OverlapPeriodTest {

        @Test
        @DisplayName("[INT-004] OPEN+ONGOING 겹침 기간에서 신청 성공")
        void registerInOverlapPeriod_OpenAndOngoing_Success() {
            // given: 겹침 기간 행사 (regStart < eventStart < now < regEnd < eventEnd)
            Event event = createAndSaveOverlapEvent();

            // when: 신청 시도 (Lazy evaluation으로 OPEN+ONGOING 전이 후 신청)
            eventRegistrationService.registerEvent(event.getId(), member.getId());

            // then: DB에서 상태 확인
            Event updatedEvent = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            updatedEvent.updateStatusIfNeeded(Instant.now());
            assertThat(updatedEvent.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(updatedEvent.getEventStatus()).isEqualTo(EventStatus.ONGOING);

            EventRegistration reg = eventRegistrationRepository
                    .findByEventIdAndUserId(event.getId(), member.getId())
                    .orElseThrow();
            assertThat(reg.getStatus()).isEqualTo(EventRegistrationStatus.REGISTERED);
        }

        @Test
        @DisplayName("[INT-005] 겹침 기간 정원 마감→취소→자동 재오픈")
        void overlapPeriod_CapacityFull_Cancel_AutoReopen() {
            // given: capacity=2 겹침 행사
            Event event = createAndSaveOverlapEventWithCapacity(2);

            // when: 2명 신청 → 정원 마감
            eventRegistrationService.registerEvent(event.getId(), member.getId());
            eventRegistrationService.registerEvent(event.getId(), member2.getId());

            // then: CLOSED, CAPACITY_FULL
            Event afterFull = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(afterFull.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(afterFull.getCloseReason()).isEqualTo(EventCloseReason.CAPACITY_FULL);
            assertThat(afterFull.getCurrentCount()).isEqualTo(2);

            // when: 1명 취소
            eventRegistrationService.cancelRegistration(event.getId(), member.getId());

            // then: OPEN 복원, currentCount=1, eventStatus=ONGOING 유지
            Event afterCancel = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            afterCancel.updateStatusIfNeeded(Instant.now());
            assertThat(afterCancel.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(afterCancel.getCurrentCount()).isEqualTo(1);
            assertThat(afterCancel.getEventStatus()).isEqualTo(EventStatus.ONGOING);
        }

        @Test
        @DisplayName("[INT-006] 겹침 기간 중 수동 마감 후 행사 계속")
        void overlapPeriod_ManualClose_EventContinues() {
            // given: 겹침 기간 행사
            Event event = createAndSaveOverlapEvent();
            eventRegistrationService.registerEvent(event.getId(), member.getId());

            // when: 운영자 수동 마감
            eventService.closeEvent(event.getId(), operator.getId());

            // then: CLOSED(MANUAL_CLOSE), ONGOING
            Event afterClose = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(afterClose.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(afterClose.getCloseReason()).isEqualTo(EventCloseReason.MANUAL_CLOSE);
            assertThat(afterClose.getEventStatus()).isEqualTo(EventStatus.ONGOING);
        }
    }

    @Nested
    @DisplayName("2축 모델 — 행사 취소/재활성화")
    class CancelReactivateIntegrationTest {

        @Test
        @DisplayName("[INT-007] 행사 취소→registrationStatus=CLOSED 강제 DB 검증")
        void cancelEvent_ForcesRegistrationClosed_InDb() {
            // given: OPEN 상태 행사
            Event event = createAndSaveAutoApproveEvent();

            // when: 취소
            eventService.cancelEvent(event.getId(), operator.getId());

            // then: DB에서 CANCELED + CLOSED(MANUAL_CLOSE) 확인
            Event canceled = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(canceled.getEventStatus()).isEqualTo(EventStatus.CANCELED);
            assertThat(canceled.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(canceled.getCloseReason()).isEqualTo(EventCloseReason.MANUAL_CLOSE);
        }

        @Test
        @DisplayName("[INT-008] 행사 취소→재활성화→Lazy Evaluation 복원")
        void cancelAndReactivate_LazyEvaluationRestores() {
            // given: 행사 생성 후 취소
            Event event = createAndSaveAutoApproveEvent();
            eventService.cancelEvent(event.getId(), operator.getId());

            // when: 재활성화
            eventService.reactivateEvent(event.getId(), operator.getId());

            // then: eventStatus=UPCOMING, registrationStatus Lazy 복원
            Event reactivated = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            reactivated.updateStatusIfNeeded(Instant.now());
            assertThat(reactivated.getEventStatus()).isEqualTo(EventStatus.UPCOMING);
            // regStart is past so should be OPEN
            assertThat(reactivated.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
        }

        @Test
        @DisplayName("[INT-009] CANCELED 행사에서 신청 시도 차단")
        void registerOnCanceledEvent_ThrowsEventNotOpenException() {
            // given: 취소된 행사
            Event event = createAndSaveAutoApproveEvent();
            eventService.cancelEvent(event.getId(), operator.getId());

            // when/then: 신청 시도 → registrationStatus=CLOSED이므로 EventNotOpenException
            assertThatThrownBy(() ->
                    eventRegistrationService.registerEvent(event.getId(), member.getId()))
                    .isInstanceOf(EventNotOpenException.class);
        }

        @Test
        @DisplayName("[INT-010] COMPLETED 행사에서 승인 시도 차단")
        void approveOnCompletedEvent_ThrowsEventNotEditableException() {
            // given: 선발제 행사에 WAITING 신청 생성, 이후 행사 COMPLETED 전이
            Event event = transactionTemplate.execute(status -> {
                Instant now = Instant.now();
                Event e = Event.create(
                        operator, "완료 행사", "설명", "장소",
                        now.minus(3, ChronoUnit.DAYS),   // eventStart: past
                        now.minus(1, ChronoUnit.DAYS),   // eventEnd: past → COMPLETED
                        now.minus(5, ChronoUnit.DAYS),   // regStart: past
                        now.minus(2, ChronoUnit.DAYS),   // regEnd: past
                        10,
                        EventRegistrationType.MANUAL_APPROVE
                );
                // OPEN 상태로 설정하여 COMPLETED 전이 전 상태를 시뮬레이션
                e.openRegistration();
                return eventRepository.save(e);
            });

            // 서비스 검증을 우회하여 WAITING 신청을 직접 생성
            Long registrationId = transactionTemplate.execute(status -> {
                Event e = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
                EventRegistration reg = EventRegistration.create(e, member);
                return eventRegistrationRepository.save(reg).getId();
            });

            // when/then: 승인 시도 → Lazy로 COMPLETED 전이 후 EventNotEditableException
            assertThatThrownBy(() ->
                    eventRegistrationService.approveRegistration(registrationId, operator.getId()))
                    .isInstanceOf(EventNotEditableException.class);
        }
    }

    @Nested
    @DisplayName("2축 모델 — 수동 재오픈 E2E")
    class ManualReopenE2ETest {

        @Test
        @DisplayName("[INT-011] 수동 마감→수동 재오픈 전체 흐름 (감사 이력 포함)")
        void manualClose_ThenReopen_FullFlow() {
            // given: OPEN 행사
            Event event = createAndSaveAutoApproveEvent();

            // when: 수동 마감
            eventService.closeEvent(event.getId(), operator.getId());
            Event afterClose = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(afterClose.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(afterClose.getCloseReason()).isEqualTo(EventCloseReason.MANUAL_CLOSE);

            // when: 수동 재오픈
            eventService.reopenRegistration(event.getId(), operator.getId(), "재오픈 사유");

            // then: OPEN, closeReason=null
            Event afterReopen = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(afterReopen.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(afterReopen.getCloseReason()).isNull();

            // then: 감사 이력 확인 (closeEvent + reopenRegistration = 2건)
            List<EventStatusChangeHistory> histories = eventStatusChangeHistoryRepository.findByEventIdOrderByCreatedAtDesc(event.getId());
            assertThat(histories).hasSize(2);
            // 최신순 정렬이므로 첫 번째가 재오픈 이력
            assertThat(histories.get(0).getChangeType()).isEqualTo(EventChangeType.REGISTRATION_REOPENED);
            assertThat(histories.get(0).getReason()).isEqualTo("재오픈 사유");
        }

        @Test
        @DisplayName("[INT-012] 기한 만료 후 수동 재오픈 거부→regEnd 연장→재오픈 성공")
        void deadlineExpired_ExtendRegEnd_ThenReopen() {
            // given: 기한 만료된 행사 (regEnd in past)
            Event event = transactionTemplate.execute(status -> {
                Instant now = Instant.now();
                Event e = Event.create(
                        operator, "기한 만료 행사", "설명", "장소",
                        now.plus(7, ChronoUnit.DAYS),
                        now.plus(8, ChronoUnit.DAYS),
                        now.minus(3, ChronoUnit.DAYS),
                        now.minus(1, ChronoUnit.HOURS),  // regEnd: past (Lazy → CLOSED by DEADLINE)
                        10,
                        EventRegistrationType.AUTO_APPROVE
                );
                return eventRepository.save(e);
            });

            // Lazy evaluation 결과를 DB에 영속화 (NOT_STARTED → OPEN → CLOSED by DEADLINE)
            transactionTemplate.execute(status -> {
                Event e = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
                e.updateStatusIfNeeded(Instant.now());
                return null;
            });

            // then: 재오픈 시도 → 거부 (기한 만료)
            assertThatThrownBy(() ->
                    eventService.reopenRegistration(event.getId(), operator.getId(), "사유"))
                    .isInstanceOf(EventRegistrationNotReopenableException.class);

            // when: regEnd 연장 후 재오픈
            transactionTemplate.execute(status -> {
                Event e = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
                e.update("기한 만료 행사", "설명", "장소",
                        e.getEventStartAt(), e.getEventEndAt(),
                        e.getRegistrationStartAt(),
                        Instant.now().plus(3, ChronoUnit.DAYS), // regEnd 연장
                        10);
                return null;
            });

            // then: 재오픈 성공
            eventService.reopenRegistration(event.getId(), operator.getId(), "기한 연장 후 재오픈");
            Event reopened = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(reopened.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
        }

        @Test
        @DisplayName("[INT-013] ONGOING 중 수동 마감 후 수동 재오픈")
        void ongoingEvent_ManualClose_ThenReopen() {
            // given: ONGOING 겹침 행사
            Event event = createAndSaveOverlapEvent();

            // when: 수동 마감
            eventService.closeEvent(event.getId(), operator.getId());

            // then: CLOSED + ONGOING
            Event closed = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            closed.updateStatusIfNeeded(Instant.now());
            assertThat(closed.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(closed.getEventStatus()).isEqualTo(EventStatus.ONGOING);

            // when: 수동 재오픈
            eventService.reopenRegistration(event.getId(), operator.getId(), "ONGOING 재오픈");

            // then: OPEN + ONGOING
            Event reopened = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            reopened.updateStatusIfNeeded(Instant.now());
            assertThat(reopened.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
            assertThat(reopened.getEventStatus()).isEqualTo(EventStatus.ONGOING);
        }
    }

    @Nested
    @DisplayName("2축 모델 — 수정/상태 연동")
    class EditStatusIntegrationTest {

        @Test
        @DisplayName("[INT-014] ONGOING 부분 수정 DB 반영")
        void ongoingEvent_PartialUpdate_DbReflected() {
            // given: ONGOING 겹침 행사
            Event event = createAndSaveOverlapEvent();

            // Lazy evaluation으로 ONGOING 상태 설정
            transactionTemplate.execute(status -> {
                Event e = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
                e.updateStatusIfNeeded(Instant.now());
                return null;
            });

            // when: title, description 수정 (eventStartAt, registrationStartAt은 기존 값 유지)
            Event current = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            UpdateEventRequest request = new UpdateEventRequest(
                    "수정된 제목",
                    "수정된 설명",
                    "수정된 장소",
                    current.getEventStartAt(),         // 변경 안 함
                    current.getEventEndAt(),
                    current.getRegistrationStartAt(),  // 변경 안 함
                    current.getRegistrationEndAt(),
                    current.getCapacity()
            );
            eventService.updateEvent(event.getId(), request, operator.getId());

            // then: 허용 필드만 수정됨
            Event updated = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(updated.getTitle()).isEqualTo("수정된 제목");
            assertThat(updated.getDescription()).isEqualTo("수정된 설명");
            assertThat(updated.getLocation()).isEqualTo("수정된 장소");
        }

        @Test
        @DisplayName("[INT-015] CANCELED 수정→재활성화 E2E")
        void canceledEvent_Update_ThenReactivate() {
            // given: 행사 취소
            Event event = createAndSaveAutoApproveEvent();
            eventService.cancelEvent(event.getId(), operator.getId());

            // when: 취소 상태에서 날짜 수정 (DB 저장 시 나노초 손실 방지를 위해 밀리초로 절삭)
            Event current = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            Instant newEventStart = Instant.now().plus(10, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
            Instant newEventEnd = Instant.now().plus(11, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
            Instant newRegEnd = Instant.now().plus(9, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MILLIS);
            UpdateEventRequest request = new UpdateEventRequest(
                    current.getTitle(), current.getDescription(), current.getLocation(),
                    newEventStart, newEventEnd,
                    current.getRegistrationStartAt(), newRegEnd,
                    current.getCapacity()
            );
            eventService.updateEvent(event.getId(), request, operator.getId());

            // when: 재활성화
            eventService.reactivateEvent(event.getId(), operator.getId());

            // then: 수정된 날짜 기반 Lazy Evaluation 정상 동작
            Event reactivated = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            reactivated.updateStatusIfNeeded(Instant.now());
            assertThat(reactivated.getEventStatus()).isEqualTo(EventStatus.UPCOMING);
            assertThat(reactivated.getEventStartAt()).isEqualTo(newEventStart);
        }

        @Test
        @DisplayName("[INT-016] Lazy 두 축 동시 전이 DB 반영")
        void lazyTwoAxisSimultaneousTransition() {
            // given: regEnd가 과거, eventEnd가 미래인 행사 → 동시에 CLOSED(DEADLINE) + ONGOING 전이
            Event event = transactionTemplate.execute(status -> {
                Instant now = Instant.now();
                Event e = Event.create(
                        operator, "동시 전이 행사", "설명", "장소",
                        now.minus(2, ChronoUnit.DAYS),   // eventStart: past → ONGOING
                        now.plus(1, ChronoUnit.DAYS),    // eventEnd: future
                        now.minus(5, ChronoUnit.DAYS),   // regStart: past → OPEN
                        now.minus(1, ChronoUnit.DAYS),   // regEnd: past → CLOSED (DEADLINE_PASSED)
                        10,
                        EventRegistrationType.AUTO_APPROVE
                );
                return eventRepository.save(e);
            });

            // when: Lazy evaluation 수행
            Event found = transactionTemplate.execute(status -> {
                Event e = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
                e.updateStatusIfNeeded(Instant.now());
                return e;
            });

            // then: registrationStatus=CLOSED (deadline), eventStatus=ONGOING
            assertThat(found.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);
            assertThat(found.getCloseReason()).isEqualTo(EventCloseReason.DEADLINE_PASSED);
            assertThat(found.getEventStatus()).isEqualTo(EventStatus.ONGOING);
        }
    }

    @Nested
    @DisplayName("선발제 전체 흐름")
    class ManualApproveFullFlowTest {

        @Test
        @DisplayName("[INT-017] 선발제: 신청→승인→정원 마감→되돌리기→재오픈")
        void manualApprove_RegisterApproveFullRevertReopen() {
            // given: capacity=1 선발제 행사 (UPCOMING, OPEN)
            Event event = transactionTemplate.execute(status -> {
                Instant now = Instant.now();
                Event e = Event.create(
                        operator, "선발제 1명", "설명", "장소",
                        now.plus(7, ChronoUnit.DAYS),
                        now.plus(8, ChronoUnit.DAYS),
                        now.minus(1, ChronoUnit.DAYS),
                        now.plus(6, ChronoUnit.DAYS),
                        1,  // capacity=1
                        EventRegistrationType.MANUAL_APPROVE
                );
                e.openRegistration();
                return eventRepository.save(e);
            });

            // when: 신청 (WAITING, count=0)
            eventRegistrationService.registerEvent(event.getId(), member.getId());
            EventRegistration reg = eventRegistrationRepository
                    .findByEventIdAndUserId(event.getId(), member.getId())
                    .orElseThrow();
            assertThat(reg.getStatus()).isEqualTo(EventRegistrationStatus.WAITING);
            assertThat(eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow()
                    .getCurrentCount()).isEqualTo(0);

            // when: 승인 (APPROVED, count=1, 정원 마감)
            eventRegistrationService.approveRegistration(reg.getId(), operator.getId());
            EventRegistration approved = eventRegistrationRepository
                    .findByEventIdAndUserId(event.getId(), member.getId())
                    .orElseThrow();
            assertThat(approved.getStatus()).isEqualTo(EventRegistrationStatus.APPROVED);

            Event full = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(full.getCurrentCount()).isEqualTo(1);
            assertThat(full.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);

            // when: 되돌리기 (WAITING, count=0, 재오픈)
            eventRegistrationService.revertRegistration(approved.getId(), operator.getId());
            EventRegistration reverted = eventRegistrationRepository
                    .findByEventIdAndUserId(event.getId(), member.getId())
                    .orElseThrow();
            assertThat(reverted.getStatus()).isEqualTo(EventRegistrationStatus.WAITING);

            Event reopened = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(reopened.getCurrentCount()).isEqualTo(0);
            assertThat(reopened.getRegistrationStatus()).isEqualTo(RegistrationStatus.OPEN);
        }

        @Test
        @DisplayName("[INT-018] 선발제: registrationStatus=CLOSED 후 승인 가능")
        void manualApprove_ClosedRegistration_ApproveStillWorks() {
            // given: capacity=2 선발제 행사, 2명 신청
            Event event = transactionTemplate.execute(status -> {
                Instant now = Instant.now();
                Event e = Event.create(
                        operator, "선발제 마감 후 승인", "설명", "장소",
                        now.plus(7, ChronoUnit.DAYS),
                        now.plus(8, ChronoUnit.DAYS),
                        now.minus(1, ChronoUnit.DAYS),
                        now.plus(6, ChronoUnit.DAYS),
                        2,
                        EventRegistrationType.MANUAL_APPROVE
                );
                e.openRegistration();
                return eventRepository.save(e);
            });

            eventRegistrationService.registerEvent(event.getId(), member.getId());
            eventRegistrationService.registerEvent(event.getId(), member2.getId());

            // when: 수동 마감
            eventService.closeEvent(event.getId(), operator.getId());
            Event closed = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(closed.getRegistrationStatus()).isEqualTo(RegistrationStatus.CLOSED);

            // when: 마감 상태에서 1명 승인
            EventRegistration reg1 = eventRegistrationRepository
                    .findByEventIdAndUserId(event.getId(), member.getId())
                    .orElseThrow();
            eventRegistrationService.approveRegistration(reg1.getId(), operator.getId());

            // then: 승인 성공
            EventRegistration approved = eventRegistrationRepository
                    .findByEventIdAndUserId(event.getId(), member.getId())
                    .orElseThrow();
            assertThat(approved.getStatus()).isEqualTo(EventRegistrationStatus.APPROVED);

            Event afterApprove = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
            assertThat(afterApprove.getCurrentCount()).isEqualTo(1);
        }
    }
}
