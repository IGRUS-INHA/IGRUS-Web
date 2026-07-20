package igrus.web.user.mypage.service.write;

import igrus.web.user.domain.ProfileLink;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static igrus.web.common.fixture.UserTestFixture.createMemberWithId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * UpdateMyProfileService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdateMyProfileService 단위 테스트")
class UpdateMyProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UpdateMyProfileService updateMyProfileService;

    private User memberUser;

    @BeforeEach
    void setUp() {
        memberUser = createMemberWithId();
    }

    @DisplayName("공개 프로필 수정 - 닉네임/자기소개/링크가 교체된다")
    @Test
    void updateMyProfile_ReplacesPublicProfile() {
        // given
        given(userRepository.findById(memberUser.getId())).willReturn(Optional.of(memberUser));
        List<ProfileLink> links = List.of(new ProfileLink("github", "https://github.com/user"));

        // when
        updateMyProfileService.updateMyProfile(memberUser.getId(), "유찬", "안녕하세요", links);

        // then
        assertThat(memberUser.getNickname()).isEqualTo("유찬");
        assertThat(memberUser.getIntroduction()).isEqualTo("안녕하세요");
        assertThat(memberUser.getLinks()).containsExactlyElementsOf(links);
        assertThat(memberUser.getPublicDisplayName()).isEqualTo("유찬");
    }

    @DisplayName("공개 프로필 수정 - null/빈값이면 프로필이 비워지고 표시 이름은 이름으로 폴백된다")
    @Test
    void updateMyProfile_WithNulls_ClearsProfileAndFallsBackToName() {
        // given
        given(userRepository.findById(memberUser.getId())).willReturn(Optional.of(memberUser));
        updateMyProfileService.updateMyProfile(memberUser.getId(), "유찬", "소개", List.of());

        // when
        updateMyProfileService.updateMyProfile(memberUser.getId(), "  ", null, null);

        // then
        assertThat(memberUser.getNickname()).isNull();
        assertThat(memberUser.getIntroduction()).isNull();
        assertThat(memberUser.getLinks()).isEmpty();
        assertThat(memberUser.getPublicDisplayName()).isEqualTo(memberUser.getName());
    }

    @DisplayName("공개 프로필 수정 - 존재하지 않는 사용자면 UserNotFoundException 발생")
    @Test
    void updateMyProfile_WhenUserNotFound_ThrowsUserNotFoundException() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> updateMyProfileService.updateMyProfile(999L, "닉", null, null))
                .isInstanceOf(UserNotFoundException.class);
    }
}
