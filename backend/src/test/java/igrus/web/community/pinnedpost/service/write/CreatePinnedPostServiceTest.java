package igrus.web.community.pinnedpost.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.pinnedpost.domain.PinnedPost;
import igrus.web.community.pinnedpost.dto.request.CreatePinnedPostRequest;
import igrus.web.community.pinnedpost.dto.response.PinnedPostDetailResponse;
import igrus.web.community.pinnedpost.exception.PinnedPostAlreadyExistsException;
import igrus.web.community.pinnedpost.repository.PinnedPostRepository;
import igrus.web.community.pinnedpost.service.support.ValidatePinnedPostService;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
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

import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PinnedPostTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePinnedPostService 단위 테스트")
class CreatePinnedPostServiceTest {

    @Mock
    private PinnedPostRepository pinnedPostRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ValidatePinnedPostService validatePinnedPostService;

    @InjectMocks
    private CreatePinnedPostService createPinnedPostService;

    private Board board;
    private User operator;
    private Post post;
    private AuthenticatedUser operatorAuth;

    @BeforeEach
    void setUp() {
        board = generalBoard();
        operator = createOperatorWithId();
        post = normalPost(board, createMemberWithId());
        operatorAuth = operatorAuth();
    }

    @Nested
    @DisplayName("게시글 고정 성공")
    class CreateSuccess {

        @Test
        @DisplayName("유효한 요청으로 게시글 고정 성공")
        void createPinnedPost_WithValidRequest_Success() {
            // given
            CreatePinnedPostRequest request = createRequest(post.getId(), 1);
            given(postRepository.findByIdAndDeletedFalse(post.getId())).willReturn(Optional.of(post));
            doNothing().when(validatePinnedPostService).validateNotAlreadyPinned(post.getId());
            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operator));
            given(pinnedPostRepository.save(any(PinnedPost.class))).willAnswer(invocation -> {
                PinnedPost saved = invocation.getArgument(0);
                return saved;
            });

            // when
            PinnedPostDetailResponse response = createPinnedPostService.createPinnedPost(request, operatorAuth);

            // then
            assertThat(response.postId()).isEqualTo(post.getId());
            assertThat(response.displayOrder()).isEqualTo(1);
            verify(pinnedPostRepository).save(any(PinnedPost.class));
        }
    }

    @Nested
    @DisplayName("게시글 고정 실패")
    class CreateFailure {

        @Test
        @DisplayName("존재하지 않는 게시글이면 예외 발생")
        void createPinnedPost_PostNotFound_ThrowsException() {
            // given
            CreatePinnedPostRequest request = createRequest(999L, 1);
            given(postRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> createPinnedPostService.createPinnedPost(request, operatorAuth))
                    .isInstanceOf(PostNotFoundException.class);
        }

        @Test
        @DisplayName("이미 고정된 게시글이면 예외 발생")
        void createPinnedPost_AlreadyPinned_ThrowsException() {
            // given
            CreatePinnedPostRequest request = createRequest(post.getId(), 1);
            given(postRepository.findByIdAndDeletedFalse(post.getId())).willReturn(Optional.of(post));
            doThrow(new PinnedPostAlreadyExistsException(post.getId()))
                    .when(validatePinnedPostService).validateNotAlreadyPinned(post.getId());

            // when & then
            assertThatThrownBy(() -> createPinnedPostService.createPinnedPost(request, operatorAuth))
                    .isInstanceOf(PinnedPostAlreadyExistsException.class);
        }
    }
}
