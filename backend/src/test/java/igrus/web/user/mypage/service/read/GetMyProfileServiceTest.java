package igrus.web.user.mypage.service.read;

import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.mypage.dto.response.MyProfileResponse;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static igrus.web.common.fixture.UserTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * GetMyProfileService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>MP-001: 프로필 조회 성공</li>
 *     <li>MP-003: 존재하지 않는 사용자 프로필 조회</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetMyProfileService 단위 테스트")
class GetMyProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetMyProfileService getMyProfileService;

    private User memberUser;

    @BeforeEach
    void setUp() {
        memberUser = createMemberWithId();
    }

    @Nested
    @DisplayName("프로필 조회 테스트")
    class GetMyProfileTest {

        @DisplayName("MP-001: 프로필 조회 성공")
        @Test
        void getMyProfile_ReturnsProfile() {
            // given
            Long userId = memberUser.getId();

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));

            // when
            MyProfileResponse response = getMyProfileService.getMyProfile(userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.studentId()).isEqualTo(memberUser.getStudentId());
            assertThat(response.name()).isEqualTo(memberUser.getName());
            assertThat(response.email()).isEqualTo(memberUser.getEmail());
            assertThat(response.phoneNumber()).isEqualTo(memberUser.getPhoneNumber());
            assertThat(response.department()).isEqualTo(memberUser.getDepartment());
            assertThat(response.role()).isEqualTo(memberUser.getRole());
        }

        @DisplayName("MP-003: 존재하지 않는 사용자 프로필 조회 시 UserNotFoundException 발생")
        @Test
        void getMyProfile_WhenUserNotFound_ThrowsUserNotFoundException() {
            // given
            Long nonExistentUserId = 999L;
            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> getMyProfileService.getMyProfile(nonExistentUserId))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
