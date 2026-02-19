package igrus.web.community.post.service.support;

import igrus.web.community.post.repository.PostRepository;
import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * GetRemainingPostsService 단위 테스트.
 *
 * <p>사용자가 현재 시간 기준으로 추가 작성 가능한 게시글 수를 올바르게 반환하는지 테스트합니다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetRemainingPostsService 단위 테스트")
class GetRemainingPostsServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private GetRemainingPostsService getRemainingPostsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.create("20200001", "테스트유저", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "테스트 동기", List.of(), Gender.MALE, 1, EnrollmentStatus.ENROLLED, List.of(), null, null, null);
        testUser.changeRole(UserRole.MEMBER);
        testUser.verifyEmail();
        ReflectionTestUtils.setField(testUser, "id", 1L);
    }

    @Nested
    @DisplayName("남은 게시글 수 조회 테스트")
    class GetRemainingPostsTest {

        @DisplayName("게시글이 없으면 20개 남음")
        @Test
        void getRemainingPosts_NoPosts_Returns20() {
            // given
            given(postRepository.countByAuthorAndCreatedAtAfter(eq(testUser), any(Instant.class)))
                    .willReturn(0L);

            // when
            int remaining = getRemainingPostsService.getRemainingPosts(testUser);

            // then
            assertThat(remaining).isEqualTo(20);
        }

        @DisplayName("15개 작성 시 5개 남음")
        @Test
        void getRemainingPosts_15Posts_Returns5() {
            // given
            given(postRepository.countByAuthorAndCreatedAtAfter(eq(testUser), any(Instant.class)))
                    .willReturn(15L);

            // when
            int remaining = getRemainingPostsService.getRemainingPosts(testUser);

            // then
            assertThat(remaining).isEqualTo(5);
        }

        @DisplayName("20개 작성 시 0개 남음")
        @Test
        void getRemainingPosts_20Posts_Returns0() {
            // given
            given(postRepository.countByAuthorAndCreatedAtAfter(eq(testUser), any(Instant.class)))
                    .willReturn(20L);

            // when
            int remaining = getRemainingPostsService.getRemainingPosts(testUser);

            // then
            assertThat(remaining).isEqualTo(0);
        }

        @DisplayName("25개 작성해도 음수 아닌 0 반환")
        @Test
        void getRemainingPosts_OverLimit_ReturnsZeroNotNegative() {
            // given
            given(postRepository.countByAuthorAndCreatedAtAfter(eq(testUser), any(Instant.class)))
                    .willReturn(25L);

            // when
            int remaining = getRemainingPostsService.getRemainingPosts(testUser);

            // then
            assertThat(remaining).isEqualTo(0);
        }
    }
}
