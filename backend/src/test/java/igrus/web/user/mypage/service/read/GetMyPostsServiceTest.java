package igrus.web.user.mypage.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.user.domain.User;
import igrus.web.user.mypage.dto.response.MyPostResponse;
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

import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

/**
 * GetMyPostsService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>MP-004: 내 게시글 목록 조회 성공</li>
 *     <li>MP-005: 게시글 없는 경우 빈 페이지 반환</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GetMyPostsService 단위 테스트")
class GetMyPostsServiceTest {

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private GetMyPostsService getMyPostsService;

    private User memberUser;
    private Board generalBoard;

    @BeforeEach
    void setUp() {
        memberUser = createMemberWithId();
        generalBoard = generalBoard();
    }

    @Nested
    @DisplayName("내 게시글 목록 조회 테스트")
    class GetMyPostsTest {

        @DisplayName("MP-004: 내 게시글 목록 조회 성공")
        @Test
        void getMyPosts_ReturnsPosts() {
            // given
            Long userId = memberUser.getId();
            Pageable pageable = PageRequest.of(0, 20);

            Post post1 = normalPost(generalBoard, memberUser, 1L);
            Post post2 = normalPost(generalBoard, memberUser, 2L);

            Page<Post> postPage = new PageImpl<>(List.of(post1, post2), pageable, 2);

            given(postRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable))
                    .willReturn(postPage);

            // when
            Page<MyPostResponse> result = getMyPostsService.getMyPosts(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).id()).isEqualTo(1L);
            assertThat(result.getContent().get(1).id()).isEqualTo(2L);
            assertThat(result.getContent().get(0).boardCode()).isEqualTo("GENERAL");
        }

        @DisplayName("MP-005: 게시글 없는 경우 빈 페이지 반환")
        @Test
        void getMyPosts_WhenEmpty_ReturnsEmptyPage() {
            // given
            Long userId = memberUser.getId();
            Pageable pageable = PageRequest.of(0, 20);

            Page<Post> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(postRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable))
                    .willReturn(emptyPage);

            // when
            Page<MyPostResponse> result = getMyPostsService.getMyPosts(userId, pageable);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }
}
