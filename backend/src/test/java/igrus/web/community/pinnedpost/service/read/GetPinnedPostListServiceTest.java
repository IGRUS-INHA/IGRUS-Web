package igrus.web.community.pinnedpost.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.pinnedpost.domain.PinnedPost;
import igrus.web.community.pinnedpost.dto.response.PinnedPostListResponse;
import igrus.web.community.pinnedpost.repository.PinnedPostRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PinnedPostTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetPinnedPostListService 단위 테스트")
class GetPinnedPostListServiceTest {

    @Mock
    private PinnedPostRepository pinnedPostRepository;

    @InjectMocks
    private GetPinnedPostListService getPinnedPostListService;

    private Board board;
    private User member;
    private User operator;

    @BeforeEach
    void setUp() {
        board = generalBoard();
        member = createMemberWithId();
        operator = createOperatorWithId();
    }

    @Nested
    @DisplayName("고정 게시글 목록 조회")
    class GetList {

        @Test
        @DisplayName("표시 순서대로 정렬된 목록 반환")
        void getPinnedPostList_ReturnsOrderedList() {
            // given
            Post post1 = normalPost(board, member, 10L);
            Post post2 = normalPost(board, member, 11L);
            Post post3 = normalPost(board, member, 12L);

            PinnedPost pinned1 = pinnedPost(post1, operator, 1, 100L);
            PinnedPost pinned2 = pinnedPost(post2, operator, 2, 101L);
            PinnedPost pinned3 = pinnedPost(post3, operator, 3, 102L);

            given(pinnedPostRepository.findAllByDeletedFalseOrderByDisplayOrderAsc())
                    .willReturn(List.of(pinned1, pinned2, pinned3));

            // when
            List<PinnedPostListResponse> result = getPinnedPostListService.getPinnedPostList();

            // then
            assertThat(result).hasSize(3);
            assertThat(result.get(0).displayOrder()).isEqualTo(1);
            assertThat(result.get(1).displayOrder()).isEqualTo(2);
            assertThat(result.get(2).displayOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("고정 게시글이 없으면 빈 목록 반환")
        void getPinnedPostList_WhenEmpty_ReturnsEmptyList() {
            // given
            given(pinnedPostRepository.findAllByDeletedFalseOrderByDisplayOrderAsc())
                    .willReturn(List.of());

            // when
            List<PinnedPostListResponse> result = getPinnedPostListService.getPinnedPostList();

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("게시글 정보가 응답에 포함됨")
        void getPinnedPostList_IncludesPostInfo() {
            // given
            Post post = normalPost(board, member, 10L);
            PinnedPost pinned = pinnedPost(post, operator, 1, 100L);

            given(pinnedPostRepository.findAllByDeletedFalseOrderByDisplayOrderAsc())
                    .willReturn(List.of(pinned));

            // when
            List<PinnedPostListResponse> result = getPinnedPostListService.getPinnedPostList();

            // then
            assertThat(result).hasSize(1);
            PinnedPostListResponse response = result.get(0);
            assertThat(response.post().id()).isEqualTo(post.getId());
            assertThat(response.post().title()).isEqualTo(post.getTitle());
            assertThat(response.post().boardCode()).isEqualTo(board.getCode().name());
        }
    }
}
