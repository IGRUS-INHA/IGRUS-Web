package igrus.web.security.auth.common.service;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.dto.response.PrivacyConsentHistoryResponse;
import igrus.web.security.auth.common.dto.response.PrivacyConsentResponse;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PrivacyConsentService 통합 테스트")
class PrivacyConsentServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private PrivacyConsentService privacyConsentService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private User createAndSaveTestUser() {
        return createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
    }

    @Nested
    @DisplayName("getConsentHistory")
    class GetConsentHistoryTest {

        @Test
        @DisplayName("동의 이력이 있을 때 이력 응답 반환")
        void getConsentHistory_WithConsents_ReturnsHistory() {
            // given
            User user = createAndSaveTestUser();
            PrivacyConsent consent1 = PrivacyConsent.create(user, "v1.0");
            PrivacyConsent consent2 = PrivacyConsent.create(user, "v2.0");

            transactionTemplate.execute(status -> {
                privacyConsentRepository.save(consent1);
                privacyConsentRepository.save(consent2);
                return null;
            });

            // when
            PrivacyConsentHistoryResponse response = privacyConsentService.getConsentHistory(user.getId());

            // then
            assertThat(response.userId()).isEqualTo(user.getId());
            assertThat(response.totalCount()).isEqualTo(2);
            assertThat(response.consents()).hasSize(2);
        }

        @Test
        @DisplayName("동의 이력이 없을 때 빈 이력 응답 반환")
        void getConsentHistory_NoConsents_ReturnsEmptyHistory() {
            // given
            User user = createAndSaveTestUser();

            // when
            PrivacyConsentHistoryResponse response = privacyConsentService.getConsentHistory(user.getId());

            // then
            assertThat(response.userId()).isEqualTo(user.getId());
            assertThat(response.totalCount()).isZero();
            assertThat(response.consents()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getLatestConsent")
    class GetLatestConsentTest {

        @Test
        @DisplayName("최신 동의 기록이 있으면 응답 반환")
        void getLatestConsent_WhenExists_ReturnsResponse() {
            // given
            User user = createAndSaveTestUser();
            PrivacyConsent consent = PrivacyConsent.create(user, "v2.0");

            transactionTemplate.execute(status -> {
                privacyConsentRepository.save(consent);
                return null;
            });

            // when
            Optional<PrivacyConsentResponse> response = privacyConsentService.getLatestConsent(user.getId());

            // then
            assertThat(response).isPresent();
            assertThat(response.get().policyVersion()).isEqualTo("v2.0");
        }

        @Test
        @DisplayName("동의 기록이 없으면 Optional.empty 반환")
        void getLatestConsent_WhenNotExists_ReturnsEmpty() {
            // given
            User user = createAndSaveTestUser();

            // when
            Optional<PrivacyConsentResponse> response = privacyConsentService.getLatestConsent(user.getId());

            // then
            assertThat(response).isEmpty();
        }
    }

    @Nested
    @DisplayName("hasConsentedToVersion")
    class HasConsentedToVersionTest {

        @Test
        @DisplayName("해당 버전에 동의했으면 true 반환")
        void hasConsentedToVersion_WhenConsented_ReturnsTrue() {
            // given
            User user = createAndSaveTestUser();
            String policyVersion = "v1.0";
            PrivacyConsent consent = PrivacyConsent.create(user, policyVersion);

            transactionTemplate.execute(status -> {
                privacyConsentRepository.save(consent);
                return null;
            });

            // when
            boolean result = privacyConsentService.hasConsentedToVersion(user.getId(), policyVersion);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("해당 버전에 동의하지 않았으면 false 반환")
        void hasConsentedToVersion_WhenNotConsented_ReturnsFalse() {
            // given
            User user = createAndSaveTestUser();
            String policyVersion = "v2.0";

            // when
            boolean result = privacyConsentService.hasConsentedToVersion(user.getId(), policyVersion);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("needsReConsent")
    class NeedsReConsentTest {

        @Test
        @DisplayName("동의 기록이 없으면 재동의 필요")
        void needsReConsent_NoConsent_ReturnsTrue() {
            // given
            User user = createAndSaveTestUser();
            String currentPolicyVersion = "v1.0";

            // when
            boolean result = privacyConsentService.needsReConsent(user.getId(), currentPolicyVersion);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("최신 동의 버전과 현재 버전이 다르면 재동의 필요")
        void needsReConsent_DifferentVersion_ReturnsTrue() {
            // given
            User user = createAndSaveTestUser();
            String currentPolicyVersion = "v2.0";
            PrivacyConsent consent = PrivacyConsent.create(user, "v1.0");

            transactionTemplate.execute(status -> {
                privacyConsentRepository.save(consent);
                return null;
            });

            // when
            boolean result = privacyConsentService.needsReConsent(user.getId(), currentPolicyVersion);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("최신 동의 버전과 현재 버전이 같으면 재동의 불필요")
        void needsReConsent_SameVersion_ReturnsFalse() {
            // given
            User user = createAndSaveTestUser();
            String currentPolicyVersion = "v1.0";
            PrivacyConsent consent = PrivacyConsent.create(user, "v1.0");

            transactionTemplate.execute(status -> {
                privacyConsentRepository.save(consent);
                return null;
            });

            // when
            boolean result = privacyConsentService.needsReConsent(user.getId(), currentPolicyVersion);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("hasAnyConsent")
    class HasAnyConsentTest {

        @Test
        @DisplayName("동의 기록이 있으면 true 반환")
        void hasAnyConsent_WhenExists_ReturnsTrue() {
            // given
            User user = createAndSaveTestUser();
            PrivacyConsent consent = PrivacyConsent.create(user, "v1.0");

            transactionTemplate.execute(status -> {
                privacyConsentRepository.save(consent);
                return null;
            });

            // when
            boolean result = privacyConsentService.hasAnyConsent(user.getId());

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("동의 기록이 없으면 false 반환")
        void hasAnyConsent_WhenNotExists_ReturnsFalse() {
            // given
            User user = createAndSaveTestUser();

            // when
            boolean result = privacyConsentService.hasAnyConsent(user.getId());

            // then
            assertThat(result).isFalse();
        }
    }
}
