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

@DisplayName("HasConsentedToVersionService 통합 테스트")
class HasConsentedToVersionServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private HasConsentedToVersionService hasConsentedToVersionService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private User createAndSaveTestUser() {
        return createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
    }

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
        boolean result = hasConsentedToVersionService.hasConsentedToVersion(user.getId(), policyVersion);

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
        boolean result = hasConsentedToVersionService.hasConsentedToVersion(user.getId(), policyVersion);

        // then
        assertThat(result).isFalse();
    }
}
