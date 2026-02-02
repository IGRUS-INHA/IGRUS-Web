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

@DisplayName("HasAnyConsentService 통합 테스트")
class HasAnyConsentServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private HasAnyConsentService hasAnyConsentService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private User createAndSaveTestUser() {
        return createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
    }

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
        boolean result = hasAnyConsentService.hasAnyConsent(user.getId());

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("동의 기록이 없으면 false 반환")
    void hasAnyConsent_WhenNotExists_ReturnsFalse() {
        // given
        User user = createAndSaveTestUser();

        // when
        boolean result = hasAnyConsentService.hasAnyConsent(user.getId());

        // then
        assertThat(result).isFalse();
    }
}
