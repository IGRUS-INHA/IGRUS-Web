package igrus.web.admin.user.service;

import igrus.web.admin.user.dto.UserDetailResponse;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserDetailService 단위 테스트")
class GetUserDetailServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetUserDetailService getUserDetailService;

    @Nested
    @DisplayName("회원 상세 조회")
    class GetUserDetailTest {

        @Test
        @DisplayName("존재하는 사용자 ID로 조회 시 상세 정보 반환")
        void getUserDetail_WithValidId_ReturnsDetail() {
            // given
            Long userId = 1L;
            User user = createTestUser();
            user.verifyEmail();
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            UserDetailResponse response = getUserDetailService.getUserDetail(userId);

            // then
            assertThat(response.studentId()).isEqualTo("20231234");
            assertThat(response.name()).isEqualTo("홍길동");
            assertThat(response.email()).isEqualTo("hong@inha.edu");
            assertThat(response.phoneNumber()).isEqualTo("010-1234-5678");
            assertThat(response.department()).isEqualTo("컴퓨터공학과");
            assertThat(response.motivation()).isEqualTo("프로그래밍을 배우고 싶어서");
            assertThat(response.gender()).isEqualTo(Gender.MALE);
            assertThat(response.grade()).isEqualTo(2);
            assertThat(response.role()).isEqualTo(UserRole.ASSOCIATE);
            assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 ID로 조회 시 UserNotFoundException 발생")
        void getUserDetail_WithInvalidId_ThrowsException() {
            // given
            Long userId = 999L;
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> getUserDetailService.getUserDetail(userId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    private User createTestUser() {
        return User.create(
                "20231234",
                "홍길동",
                "hong@inha.edu",
                "010-1234-5678",
                "컴퓨터공학과",
                "프로그래밍을 배우고 싶어서",
                List.of(),
                Gender.MALE,
                2,
                List.of(), null, null, null
        );
    }
}
