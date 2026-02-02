package igrus.web.community.post.service.support;

import igrus.web.community.board.domain.Board;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.domain.PostView;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.community.post.repository.PostViewRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static igrus.web.common.fixture.TestEntityIdAssigner.assignId;
import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.generalBoard;
import static igrus.web.community.fixture.PostTestFixture.normalPost;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * RecordPostViewService 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RecordPostViewService 단위 테스트")
class RecordPostViewServiceTest {

    @Mock
    private PostViewRepository postViewRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RecordPostViewService recordPostViewService;

    private Board board;
    private User author;
    private User viewer;
    private Post post;

    @BeforeEach
    void setUp() {
        board = generalBoard();
        author = createMemberWithId();
        viewer = createMemberWithId(2L);
        post = normalPost(board, author);
    }

    @Test
    @DisplayName("유효한 게시글과 조회자 ID로 조회 기록 저장 성공")
    void recordViewAsync_WithValidPostAndViewer_SavesPostView() {
        // given
        given(postRepository.findById(post.getId())).willReturn(Optional.of(post));
        given(userRepository.findById(viewer.getId())).willReturn(Optional.of(viewer));
        given(postViewRepository.save(any(PostView.class))).willAnswer(invocation -> {
            PostView pv = invocation.getArgument(0);
            assignId(pv, 1L);
            return pv;
        });

        // when
        recordPostViewService.recordViewAsync(post.getId(), viewer.getId());

        // then
        ArgumentCaptor<PostView> captor = ArgumentCaptor.forClass(PostView.class);
        verify(postViewRepository).save(captor.capture());

        PostView savedPostView = captor.getValue();
        assertThat(savedPostView.getPost()).isEqualTo(post);
        assertThat(savedPostView.getViewer()).isEqualTo(viewer);
        assertThat(savedPostView.getViewedAt()).isNotNull();
    }
}
