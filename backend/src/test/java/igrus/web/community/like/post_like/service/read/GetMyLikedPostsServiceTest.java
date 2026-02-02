package igrus.web.community.like.post_like.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.like.post_like.domain.PostLike;
import igrus.web.community.like.post_like.dto.response.LikedPostResponse;
import igrus.web.community.like.post_like.repository.PostLikeRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

import static igrus.web.common.fixture.TestConstants.*;
import static igrus.web.common.fixture.TestEntityIdAssigner.withId;
import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * GetMyLikedPostsService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>LKB-030: 좋아요 목록 조회 (최신순)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetMyLikedPostsService 단위 테스트")
class GetMyLikedPostsServiceTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GetMyLikedPostsService getMyLikedPostsService;

    private Board generalBoard;
    private User memberUser;
    private User anotherMemberUser;

    @BeforeEach
    void setUp() {
        // 게시판 생성 - 픽스처 사용
        generalBoard = generalBoard();

        // 사용자 생성 - 픽스처 사용
        memberUser = createMemberWithId();
        anotherMemberUser = createAnotherMemberWithId();
    }

    @Nested
    @DisplayName("좋아요 목록 조회 테스트")
    class GetMyLikesTest {

        @DisplayName("LKB-030: 좋아요 목록 조회 (최신순) - 사용자가 좋아요한 게시글 목록을 최신순으로 조회한다")
        @Test
        void getMyLikes_ReturnsLikedPostsInDescendingOrder() {
            // given
            Long userId = DEFAULT_MEMBER_ID;
            Pageable pageable = PageRequest.of(0, 10);

            Post post1 = normalPost(generalBoard, anotherMemberUser, 1L);
            Post post2 = normalPost(generalBoard, anotherMemberUser, 2L);
            Post post3 = normalPost(generalBoard, anotherMemberUser, 3L);

            PostLike like1 = PostLike.create(post1, memberUser);
            PostLike like2 = PostLike.create(post2, memberUser);
            PostLike like3 = PostLike.create(post3, memberUser);
            withId(like1, 1L);
            withId(like2, 2L);
            withId(like3, 3L);

            // 최신순 정렬 (like3 -> like2 -> like1)
            List<PostLike> likes = List.of(like3, like2, like1);
            Page<PostLike> likePage = new PageImpl<>(likes, pageable, likes.size());

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(likePage);

            // when
            Page<LikedPostResponse> result = getMyLikedPostsService.getMyLikes(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(0).postId()).isEqualTo(3L);
            assertThat(result.getContent().get(1).postId()).isEqualTo(2L);
            assertThat(result.getContent().get(2).postId()).isEqualTo(1L);
        }

        @DisplayName("좋아요한 게시글이 없는 경우 빈 목록을 반환한다")
        @Test
        void getMyLikes_WhenNoLikes_ReturnsEmptyPage() {
            // given
            Long userId = DEFAULT_MEMBER_ID;
            Pageable pageable = PageRequest.of(0, 10);

            Page<PostLike> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(emptyPage);

            // when
            Page<LikedPostResponse> result = getMyLikedPostsService.getMyLikes(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }

        @DisplayName("삭제된 게시글도 좋아요 목록에 포함된다")
        @Test
        void getMyLikes_IncludesDeletedPosts() {
            // given
            Long userId = DEFAULT_MEMBER_ID;
            Pageable pageable = PageRequest.of(0, 10);

            Post deletedPost = normalPost(generalBoard, anotherMemberUser, 1L);
            deletedPost.delete(ANOTHER_MEMBER_ID);

            PostLike likeOnDeletedPost = PostLike.create(deletedPost, memberUser);
            withId(likeOnDeletedPost, 1L);

            Page<PostLike> likePage = new PageImpl<>(List.of(likeOnDeletedPost), pageable, 1);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(likePage);

            // when
            Page<LikedPostResponse> result = getMyLikedPostsService.getMyLikes(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            LikedPostResponse response = result.getContent().get(0);
            assertThat(response.isDeleted()).isTrue();
            assertThat(response.title()).isNull();
            assertThat(response.deletedMessage()).isEqualTo("삭제된 게시글입니다");
        }

        @DisplayName("존재하지 않는 사용자의 좋아요 목록 조회 시 UserNotFoundException 발생")
        @Test
        void getMyLikes_WithNonExistentUser_ThrowsUserNotFoundException() {
            // given
            Long userId = 999L;
            Pageable pageable = PageRequest.of(0, 10);

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> getMyLikedPostsService.getMyLikes(userId, pageable))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @DisplayName("페이징이 올바르게 적용된다")
        @Test
        void getMyLikes_AppliesPagingCorrectly() {
            // given
            Long userId = DEFAULT_MEMBER_ID;
            Pageable pageable = PageRequest.of(1, 2); // 두 번째 페이지, 페이지당 2개

            Post post1 = normalPost(generalBoard, anotherMemberUser, 3L);
            PostLike like1 = PostLike.create(post1, memberUser);
            withId(like1, 3L);

            // 전체 5개 중 두 번째 페이지 (인덱스 2, 3)
            List<PostLike> secondPageLikes = List.of(like1);
            Page<PostLike> likePage = new PageImpl<>(secondPageLikes, pageable, 5);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(likePage);

            // when
            Page<LikedPostResponse> result = getMyLikedPostsService.getMyLikes(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getNumber()).isEqualTo(1); // 두 번째 페이지 (0-indexed)
            assertThat(result.getSize()).isEqualTo(2);
            assertThat(result.getTotalElements()).isEqualTo(5);
            assertThat(result.getTotalPages()).isEqualTo(3); // 5개를 2개씩 나누면 3페이지
        }

        @DisplayName("익명 게시글의 좋아요 목록 조회 시 작성자 이름이 '익명'으로 표시된다")
        @Test
        void getMyLikes_AnonymousPostShowsAnonymousAuthor() {
            // given
            Long userId = DEFAULT_MEMBER_ID;
            Pageable pageable = PageRequest.of(0, 10);

            Post anonymousPost = anonymousPost(generalBoard, anotherMemberUser, 1L);
            PostLike likeOnAnonymousPost = PostLike.create(anonymousPost, memberUser);
            withId(likeOnAnonymousPost, 1L);

            Page<PostLike> likePage = new PageImpl<>(List.of(likeOnAnonymousPost), pageable, 1);

            given(userRepository.findById(userId)).willReturn(Optional.of(memberUser));
            given(postLikeRepository.findAllByUserOrderByCreatedAtDesc(memberUser, pageable)).willReturn(likePage);

            // when
            Page<LikedPostResponse> result = getMyLikedPostsService.getMyLikes(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            LikedPostResponse response = result.getContent().get(0);
            assertThat(response.authorName()).isEqualTo("익명");
        }
    }
}
