package igrus.web.security.auth.common.service.consent;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.PrivacyConsent;
import igrus.web.security.auth.common.dto.response.PrivacyConsentHistoryResponse;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetConsentHistoryService 통합 테스트")
class GetConsentHistoryServiceTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetConsentHistoryService getConsentHistoryService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private User createAndSaveTestUser() {
        return createAndSaveUser("20231234", "test@inha.edu", UserRole.ASSOCIATE);
    }

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
        PrivacyConsentHistoryResponse response = getConsentHistoryService.getConsentHistory(user.getId());

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
        PrivacyConsentHistoryResponse response = getConsentHistoryService.getConsentHistory(user.getId());

        // then
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.totalCount()).isZero();
        assertThat(response.consents()).isEmpty();
    }
}
