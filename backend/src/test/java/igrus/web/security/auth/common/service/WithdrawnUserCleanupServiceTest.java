package igrus.web.security.auth.common.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.domain.RefreshToken;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("WithdrawnUserCleanupService 통합 테스트")
class WithdrawnUserCleanupServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private WithdrawnUserCleanupService withdrawnUserCleanupService;

    private static final String TEST_EMAIL = "test@inha.edu";
    private static final String TEST_STUDENT_ID = "12345678";
    private static final String TEST_PASSWORD = "password123!";

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private User createAndSaveWithdrawnUser(Instant deletedAt) {
        User user = User.create(
                TEST_STUDENT_ID,
                "홍길동",
                TEST_EMAIL,
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기"
        );
        user.changeRole(UserRole.MEMBER);
        User savedUser = userRepository.save(user);

        // Withdraw the user
        savedUser.withdraw();
        setField(savedUser, "deleted", true);
        setField(savedUser, "deletedAt", deletedAt);
        setField(savedUser, "deletedBy", savedUser.getId());

        return userRepository.save(savedUser);
    }

    private PasswordCredential createAndSavePasswordCredential(User user) {
        String encodedPassword = passwordEncoder.encode(TEST_PASSWORD);
        PasswordCredential credential = PasswordCredential.create(user, encodedPassword);
        return passwordCredentialRepository.save(credential);
    }

    private PrivacyConsent createAndSavePrivacyConsent(User user) {
        PrivacyConsent consent = PrivacyConsent.create(user, "1.0");
        return privacyConsentRepository.save(consent);
    }

    private EmailVerification createAndSaveEmailVerification(String email) {
        EmailVerification verification = EmailVerification.create(email, "123456", 3600000L);
        return emailVerificationRepository.save(verification);
    }

    private RefreshToken createAndSaveRefreshToken(User user) {
        long sevenDaysInMillis = Duration.ofDays(7).toMillis();
        RefreshToken token = RefreshToken.create(user, "test-refresh-token", sevenDaysInMillis);
        return refreshTokenRepository.save(token);
    }

    @Nested
    @DisplayName("5일 경과 사용자 익명화")
    class AnonymizeExpiredUsersTest {

        @Test
        @DisplayName("탈퇴 후 5일 경과한 사용자의 개인정보가 익명화된다")
        void anonymizeExpiredWithdrawnUsers_afterFiveDays_anonymizesUser() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));
            User user = createAndSaveWithdrawnUser(deletedAt);
            createAndSavePasswordCredential(user);
            createAndSavePrivacyConsent(user);
            createAndSaveEmailVerification(TEST_EMAIL);
            createAndSaveRefreshToken(user);

            // when
            int processedCount = withdrawnUserCleanupService.anonymizeExpiredWithdrawnUsers();

            // then
            assertThat(processedCount).isEqualTo(1);

            // 사용자 정보가 익명화되었는지 확인
            User anonymizedUser = userRepository.findByIdIncludingDeleted(user.getId()).orElseThrow();
            assertThat(anonymizedUser.getName()).startsWith("탈퇴회원_");
            assertThat(anonymizedUser.getEmail()).matches("deleted_[a-f0-9]{8}@deleted\\.local");
            assertThat(anonymizedUser.getStudentId()).isEqualTo("DELETED_" + user.getId());
            assertThat(anonymizedUser.getPhoneNumber()).isNull();
            assertThat(anonymizedUser.getDepartment()).isNull();
            assertThat(anonymizedUser.getMotivation()).isNull();
            assertThat(anonymizedUser.isAnonymized()).isTrue();

            // 연관 데이터가 삭제되었는지 확인
            assertThat(passwordCredentialRepository.findByUserId(user.getId())).isEmpty();
            assertThat(privacyConsentRepository.findByUserId(user.getId())).isEmpty();
            assertThat(refreshTokenRepository.findByUserIdAndRevokedFalse(user.getId())).isEmpty();
        }

        @Test
        @DisplayName("탈퇴 후 5일 미경과 사용자는 익명화되지 않는다")
        void anonymizeExpiredWithdrawnUsers_withinFiveDays_doesNotAnonymize() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(3));
            User user = createAndSaveWithdrawnUser(deletedAt);
            createAndSavePasswordCredential(user);

            // when
            int processedCount = withdrawnUserCleanupService.anonymizeExpiredWithdrawnUsers();

            // then
            assertThat(processedCount).isEqualTo(0);

            // 사용자 정보가 그대로인지 확인
            User unchangedUser = userRepository.findByIdIncludingDeleted(user.getId()).orElseThrow();
            assertThat(unchangedUser.getName()).isEqualTo("홍길동");
            assertThat(unchangedUser.getEmail()).isEqualTo(TEST_EMAIL);
            assertThat(unchangedUser.isAnonymized()).isFalse();

            // 연관 데이터도 그대로인지 확인
            assertThat(passwordCredentialRepository.findByUserIdIncludingDeleted(user.getId())).isPresent();
        }

        @Test
        @DisplayName("이미 익명화된 사용자는 중복 처리되지 않는다")
        void anonymizeExpiredWithdrawnUsers_alreadyAnonymized_skips() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));
            User user = createAndSaveWithdrawnUser(deletedAt);

            // 이미 익명화 처리
            user.anonymize("test1234");
            userRepository.save(user);

            // when
            int processedCount = withdrawnUserCleanupService.anonymizeExpiredWithdrawnUsers();

            // then
            assertThat(processedCount).isEqualTo(0);
        }

        @Test
        @DisplayName("처리 대상이 없으면 0을 반환한다")
        void anonymizeExpiredWithdrawnUsers_noTargets_returnsZero() {
            // given - 데이터 없음

            // when
            int processedCount = withdrawnUserCleanupService.anonymizeExpiredWithdrawnUsers();

            // then
            assertThat(processedCount).isEqualTo(0);
        }

        @Test
        @DisplayName("여러 사용자가 익명화 대상이면 모두 처리된다")
        void anonymizeExpiredWithdrawnUsers_multipleUsers_processesAll() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));

            User user1 = User.create("11111111", "유저1", "user1@inha.edu", "010-1111-1111", "컴퓨터공학과", "동기1");
            user1.withdraw();
            setField(user1, "deleted", true);
            setField(user1, "deletedAt", deletedAt);
            userRepository.save(user1);

            User user2 = User.create("22222222", "유저2", "user2@inha.edu", "010-2222-2222", "전자공학과", "동기2");
            user2.withdraw();
            setField(user2, "deleted", true);
            setField(user2, "deletedAt", deletedAt);
            userRepository.save(user2);

            // when
            int processedCount = withdrawnUserCleanupService.anonymizeExpiredWithdrawnUsers();

            // then
            assertThat(processedCount).isEqualTo(2);

            User anonymizedUser1 = userRepository.findByIdIncludingDeleted(user1.getId()).orElseThrow();
            User anonymizedUser2 = userRepository.findByIdIncludingDeleted(user2.getId()).orElseThrow();

            assertThat(anonymizedUser1.isAnonymized()).isTrue();
            assertThat(anonymizedUser2.isAnonymized()).isTrue();
        }
    }

    @Nested
    @DisplayName("연관 데이터 삭제")
    class RelatedDataDeletionTest {

        @Test
        @DisplayName("비밀번호 자격 증명이 삭제된다")
        void anonymizeExpiredWithdrawnUsers_deletesPasswordCredential() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));
            User user = createAndSaveWithdrawnUser(deletedAt);
            PasswordCredential credential = createAndSavePasswordCredential(user);

            // when
            withdrawnUserCleanupService.anonymizeExpiredWithdrawnUsers();

            // then
            assertThat(passwordCredentialRepository.findById(credential.getId())).isEmpty();
        }

        @Test
        @DisplayName("개인정보 동의 기록이 삭제된다")
        void anonymizeExpiredWithdrawnUsers_deletesPrivacyConsent() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));
            User user = createAndSaveWithdrawnUser(deletedAt);
            PrivacyConsent consent = createAndSavePrivacyConsent(user);

            // when
            withdrawnUserCleanupService.anonymizeExpiredWithdrawnUsers();

            // then
            assertThat(privacyConsentRepository.findById(consent.getId())).isEmpty();
        }

        @Test
        @DisplayName("이메일 인증 기록이 삭제된다")
        void anonymizeExpiredWithdrawnUsers_deletesEmailVerification() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));
            createAndSaveWithdrawnUser(deletedAt);
            EmailVerification verification = createAndSaveEmailVerification(TEST_EMAIL);

            // when
            withdrawnUserCleanupService.anonymizeExpiredWithdrawnUsers();

            // then
            assertThat(emailVerificationRepository.findById(verification.getId())).isEmpty();
        }

        @Test
        @DisplayName("리프레시 토큰이 삭제된다")
        void anonymizeExpiredWithdrawnUsers_deletesRefreshToken() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(6));
            User user = createAndSaveWithdrawnUser(deletedAt);
            RefreshToken token = createAndSaveRefreshToken(user);

            // when
            withdrawnUserCleanupService.anonymizeExpiredWithdrawnUsers();

            // then
            assertThat(refreshTokenRepository.findById(token.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("경계 조건 테스트")
    class BoundaryConditionsTest {

        @Test
        @DisplayName("정확히 5일 경과 시 익명화된다")
        void anonymizeExpiredWithdrawnUsers_exactlyFiveDays_anonymizes() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(5).plusSeconds(1));
            createAndSaveWithdrawnUser(deletedAt);

            // when
            int processedCount = withdrawnUserCleanupService.anonymizeExpiredWithdrawnUsers();

            // then
            assertThat(processedCount).isEqualTo(1);
        }

        @Test
        @DisplayName("4일 23시간 59분 경과 시 익명화되지 않는다")
        void anonymizeExpiredWithdrawnUsers_almostFiveDays_doesNotAnonymize() {
            // given
            Instant deletedAt = Instant.now().minus(Duration.ofDays(4).plusHours(23).plusMinutes(59));
            createAndSaveWithdrawnUser(deletedAt);

            // when
            int processedCount = withdrawnUserCleanupService.anonymizeExpiredWithdrawnUsers();

            // then
            assertThat(processedCount).isEqualTo(0);
        }

        @Test
        @DisplayName("활성 상태 사용자는 익명화 대상이 아니다")
        void anonymizeExpiredWithdrawnUsers_activeUser_notAffected() {
            // given
            User activeUser = createAndSaveUser(TEST_STUDENT_ID, TEST_EMAIL, UserRole.MEMBER);

            // when
            int processedCount = withdrawnUserCleanupService.anonymizeExpiredWithdrawnUsers();

            // then
            assertThat(processedCount).isEqualTo(0);

            User unchangedUser = userRepository.findById(activeUser.getId()).orElseThrow();
            assertThat(unchangedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(unchangedUser.isAnonymized()).isFalse();
        }
    }
}
