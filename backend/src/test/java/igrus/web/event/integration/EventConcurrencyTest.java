package igrus.web.event.integration;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistration;
import igrus.web.event.domain.EventRegistrationStatus;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.exception.CancelAlreadyRequestedException;
import igrus.web.event.repository.EventRegistrationRepository;
import igrus.web.event.repository.EventRepository;
import igrus.web.event.service.EventRegistrationService;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 행사 동시성 통합 테스트.
 *
 * <p>원자적 UPDATE 쿼리({@code incrementCurrentCountIfAvailable},
 * {@code incrementCurrentCountForApproval}, {@code decrementCurrentCount})가
 * 동시 요청 환경에서 정원 초과를 방지하고 데이터 정합성을 유지하는지 검증합니다.</p>
 *
 * <p>테스트 케이스:</p>
 * <ul>
 *     <li>INT-019: 동시 신청 — capacity=1, 3명 동시 → 정확히 1명 성공</li>
 *     <li>INT-020: 동시 신청 — capacity=10, 15명 동시 → 정확히 10명 성공</li>
 *     <li>INT-021: 동시 취소+신청 — 정원 경계에서 데이터 정합성 유지</li>
 *     <li>INT-022: 동시 승인 — capacity=1, 2명 대기 → 정확히 1명 승인</li>
 * </ul>
 */
@DisplayName("행사 동시성 통합 테스트")
class EventConcurrencyTest extends ServiceIntegrationTestBase {

    @Autowired
    private EventRegistrationService eventRegistrationService;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EventRegistrationRepository eventRegistrationRepository;

    private User operator;
    private List<User> members;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            operator = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);

            members = new ArrayList<>();
            for (int i = 1; i <= 15; i++) {
                String studentId = String.format("202300%02d", i + 1);
                String email = String.format("member%d@inha.edu", i);
                members.add(createAndSaveUser(studentId, email, UserRole.MEMBER));
            }
            return null;
        });
    }

    private Event createAndSaveOpenEvent(int capacity, EventRegistrationType type) {
        return transactionTemplate.execute(status -> {
            Instant now = Instant.now();
            Event event = Event.create(
                    operator, "동시성 테스트 행사", "설명", "장소",
                    now.plus(7, ChronoUnit.DAYS),
                    now.plus(8, ChronoUnit.DAYS),
                    now.minus(1, ChronoUnit.DAYS),
                    now.plus(6, ChronoUnit.DAYS),
                    capacity,
                    type,
                    null
            );
            event.publish();
            event.openRegistration();
            return eventRepository.save(event);
        });
    }

    /**
     * INT-019: 동시 신청 — capacity=1, 3명 동시 → 정확히 1명 성공.
     * 원자적 UPDATE의 WHERE 조건(currentCount < capacity)이
     * 동시 요청에서 정원 초과를 방지하는지 검증합니다.
     */
    @Test
    @DisplayName("[INT-019] 동시 신청: capacity=1, 3명 동시 → 정확히 1명 성공")
    void concurrentRegister_Capacity1_3Users_ExactlyOneSucceeds() throws Exception {
        // given
        Event event = createAndSaveOpenEvent(1, EventRegistrationType.AUTO_APPROVE);
        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final User user = members.get(i);
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    eventRegistrationService.registerEvent(event.getId(), user.getId(), null);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown(); // 동시 시작
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(2);

        Event updated = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
        assertThat(updated.getCurrentCount()).isEqualTo(1);
        assertThat(updated.getCurrentCount()).isGreaterThanOrEqualTo(0);
        assertThat(updated.getCurrentCount()).isLessThanOrEqualTo(event.getCapacity());
    }

    /**
     * INT-020: 동시 신청 — capacity=10, 15명 동시 → 정확히 10명 성공.
     * 대규모 동시 요청에서도 정원이 정확하게 지켜지는지 검증합니다.
     */
    @Test
    @DisplayName("[INT-020] 동시 신청: capacity=10, 15명 동시 → 정확히 10명 성공")
    void concurrentRegister_Capacity10_15Users_Exactly10Succeed() throws Exception {
        // given
        Event event = createAndSaveOpenEvent(10, EventRegistrationType.AUTO_APPROVE);
        int threadCount = 15;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final User user = members.get(i);
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    eventRegistrationService.registerEvent(event.getId(), user.getId(), null);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown(); // 동시 시작
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failCount.get()).isEqualTo(5);

        Event updated = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
        assertThat(updated.getCurrentCount()).isEqualTo(10);
        assertThat(updated.getCurrentCount()).isGreaterThanOrEqualTo(0);
        assertThat(updated.getCurrentCount()).isLessThanOrEqualTo(event.getCapacity());
    }

    /**
     * INT-021: 동시 취소+신청 — 정원 경계에서 데이터 정합성 유지.
     * capacity=10에서 9명 신청 후, 1명 취소 + 2명 신규 신청을 동시에 실행하여
     * currentCount가 항상 [0, capacity] 범위 내에 있는지 검증합니다.
     */
    @Test
    @DisplayName("[INT-021] 동시 취소+신청: 정원 경계에서 데이터 정합성 유지")
    void concurrentCancelAndRegister_AtCapacityBoundary_DataIntegrityMaintained() throws Exception {
        // given: capacity=10, 9명 사전 신청
        Event event = createAndSaveOpenEvent(10, EventRegistrationType.AUTO_APPROVE);
        for (int i = 0; i < 9; i++) {
            eventRegistrationService.registerEvent(event.getId(), members.get(i).getId(), null);
        }

        // 동시 실행: 1명 취소 (member at index 0) + 2명 신규 신청 (members at index 9, 10)
        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        // when
        List<Future<?>> futures = new ArrayList<>();

        // 취소 스레드
        futures.add(executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                // 신청자는 직접 취소할 수 없다: 취소 요청 → 운영진 취소 처리
                Long registrationId = eventRegistrationService.requestCancelRegistration(
                        event.getId(), members.get(0).getId(), "개인 사정").registrationId();
                eventRegistrationService.cancelRegistrationByAdmin(registrationId, operator.getId());
            } catch (Exception e) {
                // 취소 실패는 무시
            }
        }));

        // 신규 신청 스레드 2개
        for (int i = 9; i <= 10; i++) {
            final User user = members.get(i);
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    eventRegistrationService.registerEvent(event.getId(), user.getId(), null);
                } catch (Exception e) {
                    // 정원 초과 실패는 예상 가능
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown(); // 동시 시작
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then: 데이터 정합성 확인
        Event updated = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
        assertThat(updated.getCurrentCount()).isGreaterThanOrEqualTo(0);
        assertThat(updated.getCurrentCount()).isLessThanOrEqualTo(event.getCapacity());
    }

    /**
     * INT-022: 동시 승인 — capacity=1, 2명 대기(WAITING) → 정확히 1명 승인.
     * 선발제 행사에서 원자적 UPDATE({@code incrementCurrentCountForApproval})가
     * 동시 승인 요청에서 정원 초과를 방지하는지 검증합니다.
     */
    @Test
    @DisplayName("[INT-022] 동시 승인: capacity=1, 2명 대기 → 정확히 1명 승인")
    void concurrentApproval_Capacity1_2Waiting_ExactlyOneApproved() throws Exception {
        // given: MANUAL_APPROVE 행사, capacity=1, 2명 신청 (WAITING 상태)
        Event event = createAndSaveOpenEvent(1, EventRegistrationType.MANUAL_APPROVE);
        eventRegistrationService.registerEvent(event.getId(), members.get(0).getId(), null);
        eventRegistrationService.registerEvent(event.getId(), members.get(1).getId(), null);

        // 신청 ID 조회
        EventRegistration reg1 = eventRegistrationRepository
                .findByEventIdAndUserId(event.getId(), members.get(0).getId())
                .orElseThrow();
        EventRegistration reg2 = eventRegistrationRepository
                .findByEventIdAndUserId(event.getId(), members.get(1).getId())
                .orElseThrow();

        assertThat(reg1.getStatus()).isEqualTo(EventRegistrationStatus.WAITING);
        assertThat(reg2.getStatus()).isEqualTo(EventRegistrationStatus.WAITING);

        // 동시 승인 실행
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        // when
        List<Future<?>> futures = new ArrayList<>();
        Long[] registrationIds = {reg1.getId(), reg2.getId()};

        for (Long registrationId : registrationIds) {
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    eventRegistrationService.approveRegistration(registrationId, operator.getId());
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown(); // 동시 시작
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);

        Event updated = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
        assertThat(updated.getCurrentCount()).isEqualTo(1);
        assertThat(updated.getCurrentCount()).isGreaterThanOrEqualTo(0);
        assertThat(updated.getCurrentCount()).isLessThanOrEqualTo(event.getCapacity());
    }

    /**
     * INT-049: 동시 중복 취소 요청 — 같은 신청에 3건 동시 요청 → 정확히 1건만 기록.
     * 조건부 원자적 UPDATE({@code requestCancelIfActive})가 중복 요청을 막는지 검증합니다.
     */
    @Test
    @DisplayName("[INT-049] 동시 중복 취소 요청: 같은 신청에 3건 동시 → 정확히 1건 성공, 나머지는 409")
    void concurrentCancelRequest_SameRegistration_ExactlyOneSucceeds() throws Exception {
        // given
        Event event = createAndSaveOpenEvent(10, EventRegistrationType.AUTO_APPROVE);
        User user = members.get(0);
        eventRegistrationService.registerEvent(event.getId(), user.getId(), null);

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger alreadyRequestedCount = new AtomicInteger(0);
        AtomicInteger otherFailCount = new AtomicInteger(0);

        // when
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final String reason = "사유 " + i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    eventRegistrationService.requestCancelRegistration(event.getId(), user.getId(), reason);
                    successCount.incrementAndGet();
                } catch (CancelAlreadyRequestedException e) {
                    alreadyRequestedCount.incrementAndGet();
                } catch (Exception e) {
                    otherFailCount.incrementAndGet();
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown(); // 동시 시작
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(alreadyRequestedCount.get()).isEqualTo(2);
        assertThat(otherFailCount.get()).isZero();

        EventRegistration saved = eventRegistrationRepository
                .findByEventIdAndUserId(event.getId(), user.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EventRegistrationStatus.REGISTERED);
        assertThat(saved.getCancelReason()).startsWith("사유 ");
        assertThat(eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow().getCurrentCount())
                .isEqualTo(1);
    }

    /**
     * INT-091: 승인 트랜잭션이 신청을 읽은 뒤 커밋하기 전에 취소 요청이 커밋되는 상황.
     * {@code @DynamicUpdate}가 없으면 승인 커밋이 전체 컬럼 UPDATE로 취소 요청 기록을 null로 덮어쓴다.
     * (스레드 타이밍에 의존하지 않도록 승인 측 트랜잭션을 테스트에서 직접 열어 순서를 고정한다)
     */
    @Test
    @DisplayName("[INT-091] 승인 트랜잭션 도중 취소 요청이 커밋돼도 요청 기록이 유지된다")
    void approveInProgress_CancelRequestCommitted_RequestRecordKept() {
        // given: 선발제 행사에 WAITING 신청
        Event event = createAndSaveOpenEvent(10, EventRegistrationType.MANUAL_APPROVE);
        User user = members.get(0);
        Long registrationId = eventRegistrationService
                .registerEvent(event.getId(), user.getId(), null).registrationId();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // when: 승인 트랜잭션이 신청을 읽은 뒤, 다른 트랜잭션에서 취소 요청이 커밋되고, 그 다음 승인이 커밋됨
        transactionTemplate.execute(status -> {
            EventRegistration registration = eventRegistrationRepository.findById(registrationId).orElseThrow();
            try {
                executor.submit(() -> eventRegistrationService.requestCancelRegistration(
                        event.getId(), user.getId(), "개인 사정")).get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            registration.approve();
            return null;
        });
        executor.shutdown();

        // then: 승인도 반영되고 취소 요청 기록도 남아 있음
        EventRegistration saved = eventRegistrationRepository.findById(registrationId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(EventRegistrationStatus.APPROVED);
        assertThat(saved.getCancelReason()).isEqualTo("개인 사정");
        assertThat(saved.getCancelRequestedAt()).isNotNull();
    }
}
