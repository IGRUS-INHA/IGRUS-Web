package igrus.web.admin.user.service;

import igrus.web.admin.user.dto.UserListResponse;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetUserListService 단위 테스트")
class GetUserListServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetUserListService getUserListService;

    @Nested
    @DisplayName("회원 목록 조회")
    class GetUserListTest {

        @Test
        @DisplayName("필터 없이 전체 목록 조회 시 페이징된 결과 반환")
        void getUserList_WithNoFilters_ReturnsPagedResult() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            User user = createTestUser();
            Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
            given(userRepository.findByFilters(null, null, null, pageable)).willReturn(userPage);

            // when
            Page<UserListResponse> result = getUserListService.getUserList(null, null, null, pageable);

            // then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).studentId()).isEqualTo("20231234");
            assertThat(result.getContent().get(0).name()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("키워드, 역할, 상태 필터를 모두 전달하면 Repository에 그대로 위임")
        void getUserList_WithAllFilters_DelegatesToRepository() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            String keyword = "홍";
            UserRole role = UserRole.MEMBER;
            UserStatus status = UserStatus.ACTIVE;
            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            given(userRepository.findByFilters(keyword, role, status, pageable)).willReturn(emptyPage);

            // when
            getUserListService.getUserList(keyword, role, status, pageable);

            // then
            verify(userRepository).findByFilters(keyword, role, status, pageable);
        }

        @Test
        @DisplayName("결과가 없으면 빈 페이지 반환")
        void getUserList_WithNoResults_ReturnsEmptyPage() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            given(userRepository.findByFilters(null, null, null, pageable)).willReturn(emptyPage);

            // when
            Page<UserListResponse> result = getUserListService.getUserList(null, null, null, pageable);

            // then
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("User 엔티티가 UserListResponse DTO로 올바르게 변환됨")
        void getUserList_MapsUserToResponseCorrectly() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            User user = createTestUser();
            user.verifyEmail();
            user.changeRole(UserRole.ADMIN);
            Page<User> userPage = new PageImpl<>(List.of(user), pageable, 1);
            given(userRepository.findByFilters(null, null, null, pageable)).willReturn(userPage);

            // when
            Page<UserListResponse> result = getUserListService.getUserList(null, null, null, pageable);

            // then
            UserListResponse response = result.getContent().get(0);
            assertThat(response.studentId()).isEqualTo("20231234");
            assertThat(response.name()).isEqualTo("홍길동");
            assertThat(response.email()).isEqualTo("hong@inha.edu");
            assertThat(response.role()).isEqualTo(UserRole.ADMIN);
            assertThat(response.status()).isEqualTo(UserStatus.ACTIVE);
        }
    }

    private User createTestUser() {
        return User.create(
                "20231234",
                "홍길동",
                "hong@inha.edu",
                "010-1234-5678",
                "컴퓨터공학과",
                "테스트 동기",
                Gender.MALE,
                1
        );
    }
}
