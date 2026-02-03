package igrus.web.community.post.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.service.read.GetBoardEntityService;
import igrus.web.community.board.service.permission.CheckReadPermissionService;
import igrus.web.community.bookmark.repository.BookmarkRepository;
import igrus.web.community.like.post_like.repository.PostLikeRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.dto.response.PostDetailResponse;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.community.post.service.support.IncrementViewCountService;
import igrus.web.community.post.service.support.RecordPostViewService;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
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

import static igrus.web.common.fixture.TestConstants.*;
import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

/**
 * GetPostDetailService 단위 테스트.
 *
 * <p>탈퇴 사용자 게시글 상세 조회 테스트를 포함합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetPostDetailService 단위 테스트")
class GetPostDetailServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GetBoardEntityService getBoardEntityService;

    @Mock
    private CheckReadPermissionService checkReadPermissionService;

    @Mock
    private RecordPostViewService recordPostViewService;

    @Mock
    private IncrementViewCountService incrementViewCountService;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @InjectMocks
    private GetPostDetailService getPostDetailService;

    private Board generalBoard;
    private User memberUser;
    private AuthenticatedUser memberAuth;

    @BeforeEach
    void setUp() {
        generalBoard = generalBoard();
        memberUser = createMemberWithId();
        memberAuth = memberAuth();
    }

    @Nested
    @DisplayName("탈퇴 사용자 게시글 상세 조회 테스트")
    class WithdrawnUserPostDetailTest {

        @DisplayName("탈퇴한 사용자의 게시글 상세 조회 시 authorId=null, authorName='탈퇴한 사용자', isAuthor=false")
        @Test
        void getPostDetail_WithdrawnAuthor_ReturnsWithdrawnDisplayName() {
            // given
            String boardCode = "general";
            Long postId = DEFAULT_POST_ID;

            Post post = normalPostWithNullAuthor(generalBoard);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkReadPermissionService).checkReadPermission(generalBoard, memberUser.getRole());
            given(postRepository.findByBoardAndIdAndDeletedFalse(generalBoard, postId)).willReturn(Optional.of(post));
            doNothing().when(recordPostViewService).recordViewAsync(post.getId(), memberUser.getId());
            given(postLikeRepository.existsByPostIdAndUserId(postId, memberUser.getId())).willReturn(false);
            given(bookmarkRepository.existsByPostIdAndUserId(postId, memberUser.getId())).willReturn(false);

            // when
            PostDetailResponse response = getPostDetailService.getPostDetail(boardCode, postId, memberAuth);

            // then
            assertThat(response.authorId()).isNull();
            assertThat(response.authorName()).isEqualTo(User.WITHDRAWN_DISPLAY_NAME);
            assertThat(response.isAuthor()).isFalse();
            assertThat(response.title()).isEqualTo(DEFAULT_POST_TITLE);
            assertThat(response.content()).isEqualTo(DEFAULT_POST_CONTENT);
        }

        @DisplayName("탈퇴한 사용자의 익명 게시글 상세 조회 시 authorId=null, authorName='익명'")
        @Test
        void getPostDetail_WithdrawnAuthor_AnonymousPost_ReturnsAnonymousName() {
            // given
            String boardCode = "general";
            Long postId = DEFAULT_POST_ID;

            Post post = anonymousPostWithNullAuthor(generalBoard);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkReadPermissionService).checkReadPermission(generalBoard, memberUser.getRole());
            given(postRepository.findByBoardAndIdAndDeletedFalse(generalBoard, postId)).willReturn(Optional.of(post));
            doNothing().when(recordPostViewService).recordViewAsync(post.getId(), memberUser.getId());
            given(postLikeRepository.existsByPostIdAndUserId(postId, memberUser.getId())).willReturn(false);
            given(bookmarkRepository.existsByPostIdAndUserId(postId, memberUser.getId())).willReturn(false);

            // when
            PostDetailResponse response = getPostDetailService.getPostDetail(boardCode, postId, memberAuth);

            // then
            assertThat(response.authorId()).isNull();
            assertThat(response.authorName()).isEqualTo("익명");
            assertThat(response.isAuthor()).isFalse();
            assertThat(response.isAnonymous()).isTrue();
            assertThat(response.content()).isEqualTo(DEFAULT_POST_CONTENT);
        }
    }
}
