package igrus.web.community.post.service;

import igrus.web.community.post.exception.PostRateLimitExceededException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.user.domain.Gender;
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

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * PostRateLimitService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>BRD-090: Rate Limit 초과 시 429 응답</li>
 *     <li>BRD-091: Rate Limit 이내 요청 성공</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostRateLimitService 단위 테스트")
class PostRateLimitServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostRateLimitService postRateLimitService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.create("20200001", "테스트유저", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "테스트 동기", Gender.MALE, 1);
        testUser.changeRole(UserRole.MEMBER);
        testUser.verifyEmail();
        ReflectionTestUtils.setField(testUser, "id", 1L);
    }

    @Nested
    @DisplayName("Rate Limit 확인 테스트")
    class CheckRateLimitTest {

        @DisplayName("BRD-090: 시간당 20회 초과 시 PostRateLimitExceededException 발생")
        @Test
        void checkRateLimit_ExceedsLimit_ThrowsException() {
            // given
            // 지난 1시간 동안 20개의 게시글 작성
            given(postRepository.countByAuthorAndCreatedAtAfter(eq(testUser), any(Instant.class)))
                    .willReturn(20L);

            // when & then
            assertThatThrownBy(() -> postRateLimitService.checkRateLimit(testUser))
                    .isInstanceOf(PostRateLimitExceededException.class);
        }

        @DisplayName("BRD-091: Rate Limit 이내 요청 시 예외 없이 성공")
        @Test
        void checkRateLimit_WithinLimit_Success() {
            // given
            // 지난 1시간 동안 19개의 게시글 작성
            given(postRepository.countByAuthorAndCreatedAtAfter(eq(testUser), any(Instant.class)))
                    .willReturn(19L);

            // when & then
            postRateLimitService.checkRateLimit(testUser); // 예외 발생하지 않음
        }

        @DisplayName("게시글이 없는 경우 성공")
        @Test
        void checkRateLimit_NoPosts_Success() {
            // given
            given(postRepository.countByAuthorAndCreatedAtAfter(eq(testUser), any(Instant.class)))
                    .willReturn(0L);

            // when & then
            postRateLimitService.checkRateLimit(testUser); // 예외 발생하지 않음
        }

        @DisplayName("정확히 20개에서 추가 작성 시 예외 발생")
        @Test
        void checkRateLimit_ExactlyAtLimit_ThrowsException() {
            // given
            given(postRepository.countByAuthorAndCreatedAtAfter(eq(testUser), any(Instant.class)))
                    .willReturn(20L);

            // when & then
            assertThatThrownBy(() -> postRateLimitService.checkRateLimit(testUser))
                    .isInstanceOf(PostRateLimitExceededException.class);
        }
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
            int remaining = postRateLimitService.getRemainingPosts(testUser);

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
            int remaining = postRateLimitService.getRemainingPosts(testUser);

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
            int remaining = postRateLimitService.getRemainingPosts(testUser);

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
            int remaining = postRateLimitService.getRemainingPosts(testUser);

            // then
            assertThat(remaining).isEqualTo(0);
        }
    }

    // ============================================================
    // PST 테스트 케이스 (post-test-cases.md 기준)
    // ============================================================

    @Nested
    @DisplayName("PST: 작성 제한 테스트")
    class PstRateLimitTest {

        @DisplayName("PST-080: 시간당 20회 작성 제한")
        @Test
        void pst080_RateLimitExceeded_ThrowsException() {
            // given: 지난 1시간 동안 이미 20개의 게시글 작성
            given(postRepository.countByAuthorAndCreatedAtAfter(eq(testUser), any(Instant.class)))
                    .willReturn(20L);

            // when & then: 21번째 요청에서 429 Too Many Requests (PostRateLimitExceededException)
            assertThatThrownBy(() -> postRateLimitService.checkRateLimit(testUser))
                    .isInstanceOf(PostRateLimitExceededException.class);
        }

        @DisplayName("PST-081: 시간당 20회 이내 정상 작성")
        @Test
        void pst081_WithinRateLimit_Success() {
            // given: 지난 1시간 동안 19개의 게시글 작성
            given(postRepository.countByAuthorAndCreatedAtAfter(eq(testUser), any(Instant.class)))
                    .willReturn(19L);

            // when & then: 20번째 요청은 정상 처리
            postRateLimitService.checkRateLimit(testUser); // 예외 발생하지 않음

            // verify remaining posts
            int remaining = postRateLimitService.getRemainingPosts(testUser);
            assertThat(remaining).isEqualTo(1);
        }
    }
}
