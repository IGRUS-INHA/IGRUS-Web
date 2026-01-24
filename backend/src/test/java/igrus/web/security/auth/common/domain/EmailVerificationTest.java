package igrus.web.security.auth.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EmailVerification 도메인")
class EmailVerificationTest {

    private static final String TEST_EMAIL = "test@inha.edu";
    private static final String TEST_CODE = "123456";
    private static final long TEN_MINUTES_IN_MILLIS = 10 * 60 * 1000;

    @Nested
    @DisplayName("이메일 인증 생성")
    class CreateTest {

        @Test
        @DisplayName("유효한 정보로 이메일 인증 생성 시 성공 [REG-040]")
        void create_WithValidInfo_ReturnsEmailVerification() {
            // given
            String email = TEST_EMAIL;
            String code = TEST_CODE;
            long expiryMillis = TEN_MINUTES_IN_MILLIS;

            // when
            EmailVerification verification = EmailVerification.create(email, code, expiryMillis);

            // then
            assertThat(verification).isNotNull();
            assertThat(verification.getEmail()).isEqualTo(email);
            assertThat(verification.getCode()).isEqualTo(code);
            assertThat(verification.getCode()).hasSize(6);
        }

        @Test
        @DisplayName("생성 시 시도 횟수는 0으로 초기화됨 [REG-040]")
        void create_Attempts_InitializedToZero() {
            // given & when
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, TEST_CODE, TEN_MINUTES_IN_MILLIS);

            // then
            assertThat(verification.getAttempts()).isZero();
        }

        @Test
        @DisplayName("생성 시 인증 완료 여부는 false로 초기화됨 [REG-040]")
        void create_Verified_InitializedToFalse() {
            // given & when
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, TEST_CODE, TEN_MINUTES_IN_MILLIS);

            // then
            assertThat(verification.isVerified()).isFalse();
        }

        @Test
        @DisplayName("생성 시 만료 시간이 올바르게 설정됨 [REG-040]")
        void create_ExpiresAt_SetCorrectly() {
            // given
            Instant beforeCreation = Instant.now();
            long expiryMillis = TEN_MINUTES_IN_MILLIS;

            // when
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, TEST_CODE, expiryMillis);

            // then
            Instant expectedExpiresAt = beforeCreation.plusMillis(expiryMillis);
            assertThat(verification.getExpiresAt()).isNotNull();
            assertThat(verification.getExpiresAt()).isAfterOrEqualTo(expectedExpiresAt.minusMillis(1000));
            assertThat(verification.getExpiresAt()).isBeforeOrEqualTo(expectedExpiresAt.plusMillis(1000));
        }
    }

    @Nested
    @DisplayName("인증 코드 만료 확인")
    class IsExpiredTest {

        @Test
        @DisplayName("10분 이내 인증 코드는 만료되지 않음 [REG-041]")
        void isExpired_WithinExpiry_ReturnsFalse() {
            // given
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, TEST_CODE, TEN_MINUTES_IN_MILLIS);

            // when
            boolean expired = verification.isExpired();

            // then
            assertThat(expired).isFalse();
        }

        @Test
        @DisplayName("만료 시간 경과 후 인증 코드는 만료됨 [REG-042]")
        void isExpired_AfterExpiry_ReturnsTrue() {
            // given - 음수 만료 시간으로 생성하여 즉시 만료되도록 함
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, TEST_CODE, -1);

            // when
            boolean expired = verification.isExpired();

            // then
            assertThat(expired).isTrue();
        }

        @Test
        @DisplayName("음수 만료 시간으로 생성 시 즉시 만료됨 [REG-042]")
        void isExpired_NegativeExpiry_ReturnsTrue() {
            // given
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, TEST_CODE, -1000);

            // when
            boolean expired = verification.isExpired();

            // then
            assertThat(expired).isTrue();
        }
    }

    @Nested
    @DisplayName("인증 시도 횟수 관리")
    class AttemptsTest {

        @Test
        @DisplayName("시도 횟수 증가 시 1씩 증가함 [REG-043]")
        void incrementAttempts_Increments_ByOne() {
            // given
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, TEST_CODE, TEN_MINUTES_IN_MILLIS);
            int initialAttempts = verification.getAttempts();

            // when
            verification.incrementAttempts();

            // then
            assertThat(verification.getAttempts()).isEqualTo(initialAttempts + 1);
        }

        @Test
        @DisplayName("여러 번 시도 횟수 증가 시 정상적으로 누적됨 [REG-043]")
        void incrementAttempts_MultipleTimes_AccumulatesCorrectly() {
            // given
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, TEST_CODE, TEN_MINUTES_IN_MILLIS);

            // when
            verification.incrementAttempts();
            verification.incrementAttempts();
            verification.incrementAttempts();

            // then
            assertThat(verification.getAttempts()).isEqualTo(3);
        }

        @Test
        @DisplayName("최대 시도 횟수 미만일 때 추가 시도 가능 [REG-043]")
        void canAttempt_BelowMaxAttempts_ReturnsTrue() {
            // given
            int maxAttempts = 5;
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, TEST_CODE, TEN_MINUTES_IN_MILLIS);

            // when
            for (int i = 0; i < 4; i++) {
                verification.incrementAttempts();
            }

            // then
            assertThat(verification.canAttempt(maxAttempts)).isTrue();
        }

        @Test
        @DisplayName("5회 시도 후 추가 시도 불가능 [REG-043]")
        void canAttempt_AtMaxAttempts_ReturnsFalse() {
            // given
            int maxAttempts = 5;
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, TEST_CODE, TEN_MINUTES_IN_MILLIS);

            // when
            for (int i = 0; i < 5; i++) {
                verification.incrementAttempts();
            }

            // then
            assertThat(verification.canAttempt(maxAttempts)).isFalse();
        }

        @Test
        @DisplayName("최대 시도 횟수 초과 시 추가 시도 불가능 [REG-043]")
        void canAttempt_ExceedsMaxAttempts_ReturnsFalse() {
            // given
            int maxAttempts = 5;
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, TEST_CODE, TEN_MINUTES_IN_MILLIS);

            // when
            for (int i = 0; i < 6; i++) {
                verification.incrementAttempts();
            }

            // then
            assertThat(verification.canAttempt(maxAttempts)).isFalse();
        }
    }

    @Nested
    @DisplayName("인증 완료 처리")
    class VerifyTest {

        @Test
        @DisplayName("인증 완료 시 verified가 true로 변경됨 [REG-041]")
        void verify_SetsVerified_ToTrue() {
            // given
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, TEST_CODE, TEN_MINUTES_IN_MILLIS);

            // when
            verification.verify();

            // then
            assertThat(verification.isVerified()).isTrue();
        }

        @Test
        @DisplayName("인증 완료 후 재호출해도 true 유지 [REG-041]")
        void verify_MultipleCalls_RemainsTrue() {
            // given
            EmailVerification verification = EmailVerification.create(TEST_EMAIL, TEST_CODE, TEN_MINUTES_IN_MILLIS);

            // when
            verification.verify();
            verification.verify();

            // then
            assertThat(verification.isVerified()).isTrue();
        }
    }

    @Nested
    @DisplayName("인증 코드 재발송 조건")
    class ResendConditionTest {

        @Test
        @DisplayName("새 인증 객체 생성 시 기존 객체와 독립적 [REG-044, REG-045]")
        void create_NewVerification_IndependentFromOld() {
            // given
            EmailVerification oldVerification = EmailVerification.create(TEST_EMAIL, "111111", TEN_MINUTES_IN_MILLIS);
            oldVerification.incrementAttempts();
            oldVerification.incrementAttempts();

            // when
            EmailVerification newVerification = EmailVerification.create(TEST_EMAIL, "222222", TEN_MINUTES_IN_MILLIS);

            // then
            assertThat(newVerification.getAttempts()).isZero();
            assertThat(newVerification.isVerified()).isFalse();
            assertThat(newVerification.getCode()).isEqualTo("222222");
            assertThat(oldVerification.getAttempts()).isEqualTo(2);
        }

        @Test
        @DisplayName("재발송 시 새 만료 시간 적용됨 [REG-045]")
        void create_NewVerification_HasNewExpiresAt() {
            // given
            EmailVerification oldVerification = EmailVerification.create(TEST_EMAIL, "111111", TEN_MINUTES_IN_MILLIS);
            Instant oldExpiresAt = oldVerification.getExpiresAt();

            // 짧은 지연 후 새 인증 생성
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // when
            EmailVerification newVerification = EmailVerification.create(TEST_EMAIL, "222222", TEN_MINUTES_IN_MILLIS);

            // then
            assertThat(newVerification.getExpiresAt()).isAfter(oldExpiresAt);
        }
    }
}
