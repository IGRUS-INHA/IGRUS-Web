package igrus.web.security.auth.common.service.consent;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.dto.response.PrivacyConsentResponse;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetLatestConsentService 통합 테스트")
class GetLatestConsentServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetLatestConsentService getLatestConsentService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private User createAndSaveTestUser() {
        return createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
    }

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
        Optional<PrivacyConsentResponse> response = getLatestConsentService.getLatestConsent(user.getId());

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
        Optional<PrivacyConsentResponse> response = getLatestConsentService.getLatestConsent(user.getId());

        // then
        assertThat(response).isEmpty();
    }
}
