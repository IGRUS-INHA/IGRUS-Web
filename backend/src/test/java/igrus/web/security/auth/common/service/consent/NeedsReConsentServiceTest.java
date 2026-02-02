package igrus.web.security.auth.common.service.consent;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NeedsReConsentService 통합 테스트")
class NeedsReConsentServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private NeedsReConsentService needsReConsentService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private User createAndSaveTestUser() {
        return createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
    }

    @Test
    @DisplayName("동의 기록이 없으면 재동의 필요")
    void needsReConsent_NoConsent_ReturnsTrue() {
        // given
        User user = createAndSaveTestUser();
        String currentPolicyVersion = "v1.0";

        // when
        boolean result = needsReConsentService.needsReConsent(user.getId(), currentPolicyVersion);

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
        boolean result = needsReConsentService.needsReConsent(user.getId(), currentPolicyVersion);

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
        boolean result = needsReConsentService.needsReConsent(user.getId(), currentPolicyVersion);

        // then
        assertThat(result).isFalse();
    }
}
