package igrus.web.community.post.service;

import igrus.web.community.board.domain.Board;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.domain.PostView;
import igrus.web.community.post.dto.response.PostViewHistoryResponse;
import igrus.web.community.post.dto.response.PostViewStatsResponse;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.community.post.repository.PostViewRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
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
 * PostViewService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostViewService 단위 테스트")
class PostViewServiceTest {

    @Mock
    private PostViewRepository postViewRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostViewService postViewService;

    private Board board;
    private User author;
    private User viewer;
    private Post post;

    @BeforeEach
    void setUp() {
        // 게시판 생성 - 픽스처 사용
        board = generalBoard();

        // 사용자 생성 - 픽스처 사용
        author = createMemberWithId();
        viewer = createMemberWithId(2L);

        // 게시글 생성 - 픽스처 사용
        post = normalPost(board, author);
    }

    @Nested
    @DisplayName("recordViewAsync")
    class RecordViewAsyncTest {

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
            postViewService.recordViewAsync(post.getId(), viewer.getId());

            // then
            ArgumentCaptor<PostView> captor = ArgumentCaptor.forClass(PostView.class);
            verify(postViewRepository).save(captor.capture());

            PostView savedPostView = captor.getValue();
            assertThat(savedPostView.getPost()).isEqualTo(post);
            assertThat(savedPostView.getViewer()).isEqualTo(viewer);
            assertThat(savedPostView.getViewedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getPostViewStats")
    class GetPostViewStatsTest {

        @Test
        @DisplayName("게시글 조회 통계 조회 성공")
        void getPostViewStats_ReturnsCorrectStats() {
            // given
            long totalViews = 100L;
            long uniqueViewers = 50L;

            given(postViewRepository.countByPost(post)).willReturn(totalViews);
            given(postViewRepository.countDistinctViewersByPost(post)).willReturn(uniqueViewers);

            // when
            PostViewStatsResponse response = postViewService.getPostViewStats(post);

            // then
            assertThat(response.postId()).isEqualTo(post.getId());
            assertThat(response.totalViews()).isEqualTo(totalViews);
            assertThat(response.uniqueViewers()).isEqualTo(uniqueViewers);
        }

        @Test
        @DisplayName("조회 기록이 없는 게시글은 0을 반환")
        void getPostViewStats_WithNoViews_ReturnsZero() {
            // given
            given(postViewRepository.countByPost(post)).willReturn(0L);
            given(postViewRepository.countDistinctViewersByPost(post)).willReturn(0L);

            // when
            PostViewStatsResponse response = postViewService.getPostViewStats(post);

            // then
            assertThat(response.totalViews()).isZero();
            assertThat(response.uniqueViewers()).isZero();
        }
    }

    @Nested
    @DisplayName("getPostViewHistory")
    class GetPostViewHistoryTest {

        @Test
        @DisplayName("게시글 조회 기록 목록 조회 성공")
        void getPostViewHistory_ReturnsPaginatedHistory() {
            // given
            Pageable pageable = PageRequest.of(0, 20);

            PostView postView1 = PostView.create(post, viewer);
            assignId(postView1, 1L);

            PostView postView2 = PostView.create(post, author);
            assignId(postView2, 2L);

            Page<PostView> postViewPage = new PageImpl<>(List.of(postView1, postView2), pageable, 2);
            given(postViewRepository.findByPostWithViewer(post, pageable)).willReturn(postViewPage);

            // when
            Page<PostViewHistoryResponse> response = postViewService.getPostViewHistory(post, pageable);

            // then
            assertThat(response.getTotalElements()).isEqualTo(2);
            assertThat(response.getContent()).hasSize(2);
            assertThat(response.getContent().get(0).viewerId()).isEqualTo(viewer.getId());
            assertThat(response.getContent().get(1).viewerId()).isEqualTo(author.getId());
        }

        @Test
        @DisplayName("조회 기록이 없으면 빈 페이지 반환")
        void getPostViewHistory_WithNoHistory_ReturnsEmptyPage() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            Page<PostView> emptyPage = Page.empty(pageable);
            given(postViewRepository.findByPostWithViewer(post, pageable)).willReturn(emptyPage);

            // when
            Page<PostViewHistoryResponse> response = postViewService.getPostViewHistory(post, pageable);

            // then
            assertThat(response.getTotalElements()).isZero();
            assertThat(response.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getActualViewCount")
    class GetActualViewCountTest {

        @Test
        @DisplayName("게시글의 실제 조회 수 조회 성공")
        void getActualViewCount_ReturnsCorrectCount() {
            // given
            Long postId = 1L;
            long expectedCount = 42L;
            given(postViewRepository.countByPostId(postId)).willReturn(expectedCount);

            // when
            long actualCount = postViewService.getActualViewCount(postId);

            // then
            assertThat(actualCount).isEqualTo(expectedCount);
        }
    }
}
