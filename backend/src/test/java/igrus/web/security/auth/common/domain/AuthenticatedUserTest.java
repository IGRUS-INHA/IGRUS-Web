package igrus.web.security.auth.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AuthenticatedUser 단위 테스트")
class AuthenticatedUserTest {

    @Test
    @DisplayName("AuthenticatedUser 생성 및 값 조회")
    void createAndGetValues() {
        // given
        Long userId = 1L;
        String studentId = "12345678";
        String role = "USER";

        // when
        AuthenticatedUser principal = new AuthenticatedUser(userId, studentId, role);

        // then
        assertThat(principal.userId()).isEqualTo(userId);
        assertThat(principal.studentId()).isEqualTo(studentId);
        assertThat(principal.role()).isEqualTo(role);
    }

    @Test
    @DisplayName("동일한 값을 가진 AuthenticatedUser는 동등함")
    void equals_WithSameValues_ReturnsTrue() {
        // given
        AuthenticatedUser principal1 = new AuthenticatedUser(1L, "12345678", "USER");
        AuthenticatedUser principal2 = new AuthenticatedUser(1L, "12345678", "USER");

        // when & then
        assertThat(principal1).isEqualTo(principal2);
        assertThat(principal1.hashCode()).isEqualTo(principal2.hashCode());
    }
}
