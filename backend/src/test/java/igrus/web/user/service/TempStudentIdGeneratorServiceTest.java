package igrus.web.user.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.user.domain.TempStudentIdSequence;
import igrus.web.user.exception.TempStudentIdExhaustedException;
import igrus.web.user.repository.TempStudentIdSequenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@DisplayName("TempStudentIdGeneratorService 통합 테스트")
class TempStudentIdGeneratorServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private TempStudentIdGeneratorService tempStudentIdGeneratorService;

    @Autowired
    private TempStudentIdSequenceRepository sequenceRepository;

    @MockitoBean
    private Clock clock;

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setUp() {
        setUpBase();
        // 2026년 2월로 고정
        setClock(2026, 2, 1);
    }

    private void setClock(int year, int month, int day) {
        Instant fixedInstant = java.time.LocalDate.of(year, month, day)
                .atStartOfDay(KOREA_ZONE).toInstant();
        when(clock.instant()).thenReturn(fixedInstant);
        when(clock.getZone()).thenReturn(KOREA_ZONE);
    }

    @Nested
    @DisplayName("임시 학번 생성 - 정상 케이스")
    class GenerateSuccessTest {

        @Test
        @DisplayName("첫 발급 시 99YY0001 형식으로 생성 [TEMP-INV-01]")
        void generate_FirstOfYear_Returns99YY0001() {
            // when
            String tempStudentId = transactionTemplate.execute(status ->
                    tempStudentIdGeneratorService.generateTempStudentId());

            // then
            assertThat(tempStudentId).isEqualTo("99260001");
        }

        @Test
        @DisplayName("연속 호출 시 순차적으로 증가 [TEMP-INV-04]")
        void generate_MultipleCalls_ReturnsSequential() {
            // when
            String first = transactionTemplate.execute(status ->
                    tempStudentIdGeneratorService.generateTempStudentId());
            String second = transactionTemplate.execute(status ->
                    tempStudentIdGeneratorService.generateTempStudentId());
            String third = transactionTemplate.execute(status ->
                    tempStudentIdGeneratorService.generateTempStudentId());

            // then
            assertThat(first).isEqualTo("99260001");
            assertThat(second).isEqualTo("99260002");
            assertThat(third).isEqualTo("99260003");
        }

        @Test
        @DisplayName("연도 전환 시 새 시퀀스 생성 [TEMP-INV-01]")
        void generate_NewYear_StartsNewSequence() {
            // given - 2026년에 하나 발급
            String id2026 = transactionTemplate.execute(status ->
                    tempStudentIdGeneratorService.generateTempStudentId());
            assertThat(id2026).isEqualTo("99260001");

            // when - 2027년으로 전환
            setClock(2027, 1, 15);
            String id2027 = transactionTemplate.execute(status ->
                    tempStudentIdGeneratorService.generateTempStudentId());

            // then
            assertThat(id2027).isEqualTo("99270001");
        }
    }

    @Nested
    @DisplayName("임시 학번 생성 - 예외 케이스")
    class GenerateExceptionTest {

        @Test
        @DisplayName("9999 초과 시 TempStudentIdExhaustedException 발생 [TEMP-INV-01]")
        void generate_Exhausted_ThrowsException() {
            // given - 시퀀스를 9999로 설정 (nextValue=10000이면 소진)
            transactionTemplate.execute(status -> {
                TempStudentIdSequence sequence = new TempStudentIdSequence(26);
                // 9999까지 사용된 상태를 시뮬레이션
                for (int i = 0; i < 9999; i++) {
                    sequence.getAndIncrement();
                }
                sequenceRepository.save(sequence);
                return null;
            });

            // when & then
            assertThatThrownBy(() -> transactionTemplate.execute(status ->
                    tempStudentIdGeneratorService.generateTempStudentId()))
                    .isInstanceOf(TempStudentIdExhaustedException.class);
        }
    }
}
