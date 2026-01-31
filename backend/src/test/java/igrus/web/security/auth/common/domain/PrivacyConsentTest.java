package igrus.web.security.auth.common.domain;

import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PrivacyConsent 도메인")
class PrivacyConsentTest {

    private User createTestUser() {
        return User.create("20231234", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "테스트 동기", Gender.MALE, 1);
    }

    @Nested
    @DisplayName("create 정적 팩토리 메서드")
    class CreateTest {

        @Test
        @DisplayName("유효한 정보로 PrivacyConsent 생성 성공")
        void create_WithValidInfo_ReturnsPrivacyConsent() {
            // given
            User user = createTestUser();
            String policyVersion = "v1.0";

            // when
            PrivacyConsent consent = PrivacyConsent.create(user, policyVersion);

            // then
            assertThat(consent).isNotNull();
            assertThat(consent.getUser()).isEqualTo(user);
            assertThat(consent.getPolicyVersion()).isEqualTo(policyVersion);
        }

        @Test
        @DisplayName("생성 시 동의 상태가 true로 설정됨")
        void create_ConsentGiven_IsTrue() {
            // given
            User user = createTestUser();

            // when
            PrivacyConsent consent = PrivacyConsent.create(user, "v1.0");

            // then
            assertThat(consent.isConsentGiven()).isTrue();
        }

        @Test
        @DisplayName("생성 시 동의 일시가 현재 시각으로 설정됨")
        void create_ConsentDate_IsSetToCurrentTime() {
            // given
            User user = createTestUser();
            Instant beforeCreation = Instant.now().minusSeconds(1);

            // when
            PrivacyConsent consent = PrivacyConsent.create(user, "v1.0");

            // then
            Instant afterCreation = Instant.now().plusSeconds(1);
            assertThat(consent.getConsentDate()).isNotNull();
            assertThat(consent.getConsentDate()).isAfter(beforeCreation);
            assertThat(consent.getConsentDate()).isBefore(afterCreation);
        }
    }

    @Nested
    @DisplayName("N:1 관계 테스트")
    class ManyToOneRelationTest {

        @Test
        @DisplayName("동일 사용자로 여러 동의 기록 생성 가능")
        void create_SameUser_MultipleConsents() {
            // given
            User user = createTestUser();

            // when
            PrivacyConsent consent1 = PrivacyConsent.create(user, "v1.0");
            PrivacyConsent consent2 = PrivacyConsent.create(user, "v2.0");

            // then
            assertThat(consent1.getUser()).isEqualTo(user);
            assertThat(consent2.getUser()).isEqualTo(user);
            assertThat(consent1.getPolicyVersion()).isNotEqualTo(consent2.getPolicyVersion());
        }

        @Test
        @DisplayName("다른 버전의 약관에 대해 각각 동의 기록 생성 가능")
        void create_DifferentVersions_IndependentConsents() {
            // given
            User user = createTestUser();
            String version1 = "v1.0";
            String version2 = "v2.0";
            String version3 = "v3.0";

            // when
            PrivacyConsent consent1 = PrivacyConsent.create(user, version1);
            PrivacyConsent consent2 = PrivacyConsent.create(user, version2);
            PrivacyConsent consent3 = PrivacyConsent.create(user, version3);

            // then
            assertThat(consent1.getPolicyVersion()).isEqualTo(version1);
            assertThat(consent2.getPolicyVersion()).isEqualTo(version2);
            assertThat(consent3.getPolicyVersion()).isEqualTo(version3);
            assertThat(consent1.isConsentGiven()).isTrue();
            assertThat(consent2.isConsentGiven()).isTrue();
            assertThat(consent3.isConsentGiven()).isTrue();
        }
    }
}
