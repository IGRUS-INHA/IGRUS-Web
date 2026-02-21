package igrus.web.security.auth.common.repository;

import igrus.web.common.config.JpaAuditingConfig;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
@DisplayName("PrivacyConsentRepository 통합 테스트")
class PrivacyConsentRepositoryTest {

    @Autowired
    private PrivacyConsentRepository privacyConsentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    private User createAndSaveUser(String studentId, String email) {
        User user = User.create(studentId, "홍길동", email, "010-1234-5678", "컴퓨터공학과", "테스트 동기", List.of(), Gender.MALE, 1, EnrollmentStatus.ENROLLED, List.of(), null, null, null);
        userRepository.save(user);
        entityManager.flush();
        entityManager.clear();
        return user;
    }

    private void saveConsent(User user, String policyVersion) {
        User attachedUser = entityManager.find(User.class, user.getId());
        PrivacyConsent consent = PrivacyConsent.create(attachedUser, policyVersion);
        privacyConsentRepository.save(consent);
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("N:1 관계 저장 테스트")
    class ManyToOneSaveTest {

        @Test
        @DisplayName("동일 사용자로 여러 동의 기록 저장 가능")
        void save_SameUser_MultipleConsents() {
            // given
            User user = createAndSaveUser("20231001", "test1@inha.edu");
            saveConsent(user, "v1.0");
            saveConsent(user, "v2.0");

            // when
            List<PrivacyConsent> consents = privacyConsentRepository.findByUserIdOrderByConsentDateDesc(user.getId());

            // then
            assertThat(consents).hasSize(2);
        }
    }

    @Nested
    @DisplayName("findByUserIdOrderByConsentDateDesc")
    class FindByUserIdOrderByConsentDateDescTest {

        @Test
        @DisplayName("사용자의 동의 이력을 최신순으로 조회")
        void findByUserIdOrderByConsentDateDesc_ReturnsInOrder() throws InterruptedException {
            // given
            User user = createAndSaveUser("20231002", "test2@inha.edu");
            saveConsent(user, "v1.0");
            Thread.sleep(10); // 동의 시간 차이를 위한 짧은 대기
            saveConsent(user, "v2.0");

            // when
            List<PrivacyConsent> consents = privacyConsentRepository.findByUserIdOrderByConsentDateDesc(user.getId());

            // then
            assertThat(consents).hasSize(2);
            assertThat(consents.get(0).getPolicyVersion()).isEqualTo("v2.0");
            assertThat(consents.get(1).getPolicyVersion()).isEqualTo("v1.0");
        }

        @Test
        @DisplayName("동의 기록이 없으면 빈 리스트 반환")
        void findByUserIdOrderByConsentDateDesc_NoConsents_ReturnsEmptyList() {
            // given
            User user = createAndSaveUser("20231003", "test3@inha.edu");

            // when
            List<PrivacyConsent> consents = privacyConsentRepository.findByUserIdOrderByConsentDateDesc(user.getId());

            // then
            assertThat(consents).isEmpty();
        }
    }

    @Nested
    @DisplayName("findFirstByUserIdOrderByConsentDateDesc")
    class FindFirstByUserIdOrderByConsentDateDescTest {

        @Test
        @DisplayName("사용자의 가장 최근 동의 기록 조회")
        void findFirstByUserIdOrderByConsentDateDesc_ReturnsLatest() throws InterruptedException {
            // given
            User user = createAndSaveUser("20231004", "test4@inha.edu");
            saveConsent(user, "v1.0");
            Thread.sleep(10);
            saveConsent(user, "v2.0");

            // when
            Optional<PrivacyConsent> latestConsent = privacyConsentRepository
                    .findFirstByUserIdOrderByConsentDateDesc(user.getId());

            // then
            assertThat(latestConsent).isPresent();
            assertThat(latestConsent.get().getPolicyVersion()).isEqualTo("v2.0");
        }

        @Test
        @DisplayName("동의 기록이 없으면 Optional.empty 반환")
        void findFirstByUserIdOrderByConsentDateDesc_NoConsents_ReturnsEmpty() {
            // given
            User user = createAndSaveUser("20231005", "test5@inha.edu");

            // when
            Optional<PrivacyConsent> latestConsent = privacyConsentRepository
                    .findFirstByUserIdOrderByConsentDateDesc(user.getId());

            // then
            assertThat(latestConsent).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByUserIdAndPolicyVersion")
    class ExistsByUserIdAndPolicyVersionTest {

        @Test
        @DisplayName("해당 버전에 동의한 기록이 있으면 true 반환")
        void existsByUserIdAndPolicyVersion_WhenExists_ReturnsTrue() {
            // given
            User user = createAndSaveUser("20231006", "test6@inha.edu");
            saveConsent(user, "v1.0");

            // when
            boolean exists = privacyConsentRepository.existsByUserIdAndPolicyVersion(user.getId(), "v1.0");

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("해당 버전에 동의한 기록이 없으면 false 반환")
        void existsByUserIdAndPolicyVersion_WhenNotExists_ReturnsFalse() {
            // given
            User user = createAndSaveUser("20231007", "test7@inha.edu");
            saveConsent(user, "v1.0");

            // when
            boolean exists = privacyConsentRepository.existsByUserIdAndPolicyVersion(user.getId(), "v2.0");

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findByUserIdAndPolicyVersion")
    class FindByUserIdAndPolicyVersionTest {

        @Test
        @DisplayName("특정 버전의 동의 기록 조회 성공")
        void findByUserIdAndPolicyVersion_WhenExists_ReturnsConsent() {
            // given
            User user = createAndSaveUser("20231008", "test8@inha.edu");
            saveConsent(user, "v1.0");

            // when
            Optional<PrivacyConsent> foundConsent = privacyConsentRepository
                    .findByUserIdAndPolicyVersion(user.getId(), "v1.0");

            // then
            assertThat(foundConsent).isPresent();
            assertThat(foundConsent.get().getPolicyVersion()).isEqualTo("v1.0");
        }

        @Test
        @DisplayName("특정 버전의 동의 기록이 없으면 Optional.empty 반환")
        void findByUserIdAndPolicyVersion_WhenNotExists_ReturnsEmpty() {
            // given
            User user = createAndSaveUser("20231009", "test9@inha.edu");
            saveConsent(user, "v1.0");

            // when
            Optional<PrivacyConsent> foundConsent = privacyConsentRepository
                    .findByUserIdAndPolicyVersion(user.getId(), "v2.0");

            // then
            assertThat(foundConsent).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByUserIdAndConsentGivenTrue")
    class ExistsByUserIdAndConsentGivenTrueTest {

        @Test
        @DisplayName("동의 기록이 있으면 true 반환")
        void existsByUserIdAndConsentGivenTrue_WhenExists_ReturnsTrue() {
            // given
            User user = createAndSaveUser("20231010", "test10@inha.edu");
            saveConsent(user, "v1.0");

            // when
            boolean exists = privacyConsentRepository.existsByUserIdAndConsentGivenTrue(user.getId());

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("동의 기록이 없으면 false 반환")
        void existsByUserIdAndConsentGivenTrue_WhenNotExists_ReturnsFalse() {
            // given
            User user = createAndSaveUser("20231011", "test11@inha.edu");

            // when
            boolean exists = privacyConsentRepository.existsByUserIdAndConsentGivenTrue(user.getId());

            // then
            assertThat(exists).isFalse();
        }
    }
}
