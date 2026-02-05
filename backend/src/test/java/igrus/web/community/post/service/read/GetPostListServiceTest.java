package igrus.web.community.post.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.service.read.GetBoardEntityService;
import igrus.web.community.board.service.permission.CheckReadPermissionService;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.dto.response.PostListPageResponse;
import igrus.web.community.post.service.support.PostQueryHelper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static igrus.web.common.fixture.TestConstants.*;
import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;

/**
 * GetPostListService 단위 테스트.
 *
 * <p>탈퇴 사용자 게시글 목록 조회 테스트를 포함합니다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetPostListService 단위 테스트")
class GetPostListServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private GetBoardEntityService getBoardEntityService;

    @Mock
    private CheckReadPermissionService checkReadPermissionService;

    @Mock
    private PostQueryHelper postQueryHelper;

    @InjectMocks
    private GetPostListService getPostListService;

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
    @DisplayName("탈퇴 사용자 게시글 목록 조회 테스트")
    class WithdrawnUserPostListTest {

        @DisplayName("목록에 탈퇴한 사용자의 게시글이 포함된 경우 authorName='탈퇴한 사용자'")
        @Test
        void getPostList_IncludesWithdrawnAuthorPost_ReturnsWithdrawnDisplayName() {
            // given
            String boardCode = "general";
            Pageable pageable = PageRequest.of(0, 10);

            Post normalPostEntity = normalPost(generalBoard, memberUser, 2L);
            Post withdrawnPost = normalPostWithNullAuthor(generalBoard, 3L);

            Page<Post> postPage = new PageImpl<>(
                    List.of(normalPostEntity, withdrawnPost),
                    pageable,
                    2
            );

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkReadPermissionService).checkReadPermission(generalBoard, memberUser.getRole());
            given(postQueryHelper.getRegularPosts(eq(generalBoard), isNull(), eq(pageable)))
                    .willReturn(postPage);

            // when
            PostListPageResponse response = getPostListService.getPostList(boardCode, memberAuth, null, null, pageable);

            // then
            assertThat(response.posts()).hasSize(2);
            assertThat(response.posts().get(0).authorName()).isEqualTo(DEFAULT_NAME);
            assertThat(response.posts().get(1).authorName()).isEqualTo(User.WITHDRAWN_DISPLAY_NAME);
        }
    }
}
