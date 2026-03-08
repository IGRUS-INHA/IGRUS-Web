package igrus.web.event.integration;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.event.domain.Event;
import igrus.web.event.domain.EventRegistrationType;
import igrus.web.event.repository.EventRepository;
import igrus.web.event.service.EventRegistrationService;
import igrus.web.event.service.ExternalEventRegistrationService;
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
 * 외부인 행사 신청 동시성 통합 테스트.
 * 테스트 케이스 문서: docs/test-case/event/external-event-registration-test-cases.md
 *
 * <p>TASK-028: 동시성 테스트 (TC-070~TC-073)</p>
 * <p>실제 DB를 사용하여 멀티스레드 환경에서의 정원 정합성과 중복 방지를 검증합니다.</p>
 */
@DisplayName("외부인 행사 동시성 통합 테스트")
class ExternalEventConcurrencyTest extends ServiceIntegrationTestBase {

    @Autowired
    private ExternalEventRegistrationService externalEventRegistrationService;

    @Autowired
    private EventRegistrationService eventRegistrationService;

    @Autowired
    private EventRepository eventRepository;

    private User operator;
    private List<User> members;

    @BeforeEach
    void setUp() {
        setUpBase();
        transactionTemplate.execute(status -> {
            operator = createAndSaveUser("20230001", "operator@inha.edu", UserRole.OPERATOR);

            members = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
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
                    null,
                    true // allowExternal
            );
            event.publish();
            event.openRegistration();
            return eventRepository.save(event);
        });
    }

    /**
     * TC-070: 회원 + 외부인이 마지막 1자리에 동시 신청.
     * 원자적 UPDATE(incrementCurrentCountIfAvailable)로 정확히 1명만 성공해야 합니다.
     */
    @Test
    @DisplayName("[TC-070] 마지막 1자리에 회원+외부인 동시 신청 -> 정확히 1명 성공")
    void concurrentRegister_MemberAndExternal_LastSlot_ExactlyOneSucceeds() throws Exception {
        // given
        Event event = createAndSaveOpenEvent(1, EventRegistrationType.AUTO_APPROVE);
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        // 회원 스레드
        futures.add(executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                eventRegistrationService.registerEvent(event.getId(), members.get(0).getId(), null);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            }
        }));

        // 외부인 스레드
        futures.add(executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                externalEventRegistrationService.registerExternal(
                        event.getId(), "외부인", "99998888", "01099998888", "컴퓨터공학과", null);
                successCount.incrementAndGet();
            } catch (Exception e) {
                failCount.incrementAndGet();
            }
        }));

        // when
        readyLatch.await();
        startLatch.countDown();
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failCount.get()).isEqualTo(1);

        Event updated = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
        assertThat(updated.getCurrentCount()).isEqualTo(1);
        assertThat(updated.getCurrentCount()).isLessThanOrEqualTo(updated.getCapacity());
    }

    /**
     * TC-071: 2명의 외부인이 동일 studentId로 동시 신청.
     * DECISION-02: DB UNIQUE 없으므로 둘 다 성공할 수 있음 (극히 드문 중복 허용).
     */
    @Test
    @DisplayName("[TC-071] 동일 studentId로 2명 동시 외부인 신청 -> 둘 다 성공 가능 (DB UNIQUE 미적용)")
    void concurrentRegister_SameStudentId_BothMaySucceed() throws Exception {
        // given
        Event event = createAndSaveOpenEvent(10, EventRegistrationType.AUTO_APPROVE);
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final String phone = "0101111" + String.format("%04d", i);
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    externalEventRegistrationService.registerExternal(
                            event.getId(), "외부인", "12345678", phone, "컴퓨터공학과", null);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }));
        }

        // when
        readyLatch.await();
        startLatch.countDown();
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then: DECISION-02 - DB UNIQUE 없으므로 둘 다 성공 가능
        // 서비스 레벨 검증만 있으므로 동시 실행 시 race condition으로 둘 다 통과할 수 있음
        int total = successCount.get() + failCount.get();
        assertThat(total).isEqualTo(threadCount);
        // 최소 1명은 성공
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);

        Event updated = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
        assertThat(updated.getCurrentCount()).isEqualTo(successCount.get());
    }

    /**
     * TC-072: 2명의 외부인이 동일 phone으로 동시 신청.
     * DECISION-02: DB UNIQUE 없으므로 둘 다 성공할 수 있음.
     */
    @Test
    @DisplayName("[TC-072] 동일 phone으로 2명 동시 외부인 신청 -> 둘 다 성공 가능 (DB UNIQUE 미적용)")
    void concurrentRegister_SamePhone_BothMaySucceed() throws Exception {
        // given
        Event event = createAndSaveOpenEvent(10, EventRegistrationType.AUTO_APPROVE);
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final String studentId = "8888" + String.format("%04d", i);
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    externalEventRegistrationService.registerExternal(
                            event.getId(), "외부인", studentId, "01012345678", "경영학과", null);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            }));
        }

        // when
        readyLatch.await();
        startLatch.countDown();
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then
        int total = successCount.get() + failCount.get();
        assertThat(total).isEqualTo(threadCount);
        assertThat(successCount.get()).isGreaterThanOrEqualTo(1);

        Event updated = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
        assertThat(updated.getCurrentCount()).isEqualTo(successCount.get());
    }

    /**
     * TC-073: 외부인 신청 + 관리자 취소 동시 발생.
     * 트랜잭션 격리에 의해 순차 처리되며, currentCount 정합성이 유지되어야 합니다.
     */
    @Test
    @DisplayName("[TC-073] 외부인 신청 + 관리자 취소 동시 -> currentCount 정합성 유지")
    void concurrentRegisterAndCancel_CorrectCurrentCount() throws Exception {
        // given - 기존 외부인 신청 1건 존재
        Event event = createAndSaveOpenEvent(10, EventRegistrationType.AUTO_APPROVE);
        var existingResponse = externalEventRegistrationService.registerExternal(
                event.getId(), "기존외부인", "77776666", "01077776666", "물리학과", null);
        Long existingRegistrationId = existingResponse.registrationId();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger registerSuccess = new AtomicInteger(0);
        AtomicInteger cancelSuccess = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();

        // 새 외부인 신청 스레드
        futures.add(executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                externalEventRegistrationService.registerExternal(
                        event.getId(), "새외부인", "55554444", "01055554444", "화학과", null);
                registerSuccess.incrementAndGet();
            } catch (Exception e) {
                // 실패해도 OK
            }
        }));

        // 기존 신청 취소 스레드
        futures.add(executor.submit(() -> {
            readyLatch.countDown();
            try {
                startLatch.await();
                eventRegistrationService.cancelRegistrationByAdmin(existingRegistrationId, operator.getId());
                cancelSuccess.incrementAndGet();
            } catch (Exception e) {
                // 실패해도 OK
            }
        }));

        // when
        readyLatch.await();
        startLatch.countDown();
        for (Future<?> f : futures) f.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // then - 둘 다 성공해야 함 (서로 다른 레코드에 대한 작업)
        assertThat(registerSuccess.get()).isEqualTo(1);
        assertThat(cancelSuccess.get()).isEqualTo(1);

        // currentCount 정합성: +1(새 신청) -1(기존 취소) = 원래값(1) - 1 + 1 = 1
        Event updated = eventRepository.findByIdAndNotDeleted(event.getId()).orElseThrow();
        assertThat(updated.getCurrentCount()).isEqualTo(1);
        assertThat(updated.getCurrentCount()).isGreaterThanOrEqualTo(0);
    }
}
