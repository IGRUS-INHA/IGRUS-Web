package igrus.web.community.post.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.service.read.GetBoardEntityService;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.dto.request.UpdatePostRequest;
import igrus.web.community.post.dto.response.PostUpdateResponse;
import igrus.web.community.post.exception.PostAccessDeniedException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.community.post.service.support.PostValidator;
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
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

/**
 * UpdatePostService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>PST-031: 게시글 제목/내용 수정</li>
 *     <li>PST-035: 익명 게시글 본인 수정</li>
 *     <li>PST-036: 익명 게시글 타인 수정 시도</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UpdatePostService 단위 테스트")
class UpdatePostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GetBoardEntityService getBoardEntityService;

    @Mock
    private PostValidator postValidator;

    @InjectMocks
    private UpdatePostService updatePostService;

    private Board generalBoard;
    private User memberUser;
    private User operatorUser;
    private AuthenticatedUser memberAuth;

    @BeforeEach
    void setUp() {
        generalBoard = generalBoard();

        memberUser = createMemberWithId();
        operatorUser = createOperatorWithId();

        memberAuth = memberAuth();
    }

    @Nested
    @DisplayName("PST: 게시글 수정 테스트")
    class PstUpdatePostTest {

        @DisplayName("PST-031: 게시글 제목/내용 수정")
        @Test
        void updatePost_TitleAndContent_Success() {
            // given
            String boardCode = "general";
            Long postId = 1L;
            UpdatePostRequest request = updateRequest("수정된 제목", "수정된 내용입니다.");

            Post existingPost = normalPost(generalBoard, memberUser, postId);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(existingPost));

            // when
            PostUpdateResponse response = updatePostService.updatePost(boardCode, postId, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(existingPost.getTitle()).isEqualTo("수정된 제목");
            assertThat(existingPost.getContent()).isEqualTo("수정된 내용입니다.");
        }

        @DisplayName("PST-035: 익명 게시글 본인 수정")
        @Test
        void updatePost_AnonymousPost_ByAuthor_Success() {
            // given
            String boardCode = "general";
            Long postId = 1L;
            UpdatePostRequest request = updateRequest("익명 게시글 수정", "익명 게시글 수정 내용");

            Post anonymousPost = anonymousPost(generalBoard, memberUser, postId);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(anonymousPost));

            // when
            PostUpdateResponse response = updatePostService.updatePost(boardCode, postId, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(anonymousPost.getTitle()).isEqualTo("익명 게시글 수정");
        }

        @DisplayName("PST-036: 익명 게시글 타인 수정 시도")
        @Test
        void updatePost_AnonymousPost_ByOther_ThrowsException() {
            // given
            String boardCode = "general";
            Long postId = 1L;
            UpdatePostRequest request = updateRequest("타인 수정 시도", "타인이 익명 게시글 수정 시도");

            // 다른 사용자(operatorUser)가 작성한 익명 게시글
            Post anonymousPost = anonymousPost(generalBoard, operatorUser, postId);

            // memberUser가 수정 시도
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(anonymousPost));

            // when & then
            assertThatThrownBy(() -> updatePostService.updatePost(boardCode, postId, request, memberAuth))
                    .isInstanceOf(PostAccessDeniedException.class)
                    .hasMessageContaining("권한");
        }
    }
}
