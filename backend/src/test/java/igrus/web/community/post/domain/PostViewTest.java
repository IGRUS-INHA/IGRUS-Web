package igrus.web.community.post.domain;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PostView 도메인")
class PostViewTest {

    // === 헬퍼 메서드 ===

    private Board createGeneralBoard() {
        Board board = Board.create(BoardCode.GENERAL, "자유게시판", "자유롭게 글을 작성하세요", true, true, 1);
        ReflectionTestUtils.setField(board, "id", 1L);
        return board;
    }

    private User createMemberUser() {
        User user = User.create("20231234", "테스트멤버", "member@inha.edu", "010-1234-5678", "컴퓨터공학과", "가입동기");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.promoteToMember();
        return user;
    }

    private User createAnotherMemberUser() {
        User user = User.create("20234567", "다른멤버", "another@inha.edu", "010-4567-8901", "컴퓨터공학과", "가입동기");
        ReflectionTestUtils.setField(user, "id", 2L);
        user.promoteToMember();
        return user;
    }

    private Post createPost(Board board, User author) {
        Post post = Post.createPost(board, author, "테스트 제목", "테스트 내용");
        ReflectionTestUtils.setField(post, "id", 1L);
        return post;
    }

    @Nested
    @DisplayName("create 정적 팩토리 메서드")
    class CreateTest {

        @Test
        @DisplayName("유효한 게시글과 조회자로 조회 기록 생성 성공")
        void create_WithValidPostAndViewer_ReturnsPostView() {
            // given
            Board board = createGeneralBoard();
            User author = createMemberUser();
            User viewer = createAnotherMemberUser();
            Post post = createPost(board, author);

            // when
            Instant beforeCreate = Instant.now();
            PostView postView = PostView.create(post, viewer);
            Instant afterCreate = Instant.now();

            // then
            assertThat(postView).isNotNull();
            assertThat(postView.getPost()).isEqualTo(post);
            assertThat(postView.getViewer()).isEqualTo(viewer);
            assertThat(postView.getViewedAt())
                    .isAfterOrEqualTo(beforeCreate)
                    .isBeforeOrEqualTo(afterCreate);
        }

        @Test
        @DisplayName("작성자가 자신의 게시글을 조회해도 기록 생성 성공")
        void create_WithAuthorAsViewer_ReturnsPostView() {
            // given
            Board board = createGeneralBoard();
            User author = createMemberUser();
            Post post = createPost(board, author);

            // when
            PostView postView = PostView.create(post, author);

            // then
            assertThat(postView).isNotNull();
            assertThat(postView.getPost()).isEqualTo(post);
            assertThat(postView.getViewer()).isEqualTo(author);
        }

        @Test
        @DisplayName("조회 시각이 현재 시간으로 설정됨")
        void create_SetsViewedAtToCurrentTime() {
            // given
            Board board = createGeneralBoard();
            User author = createMemberUser();
            User viewer = createAnotherMemberUser();
            Post post = createPost(board, author);

            // when
            Instant beforeCreate = Instant.now();
            PostView postView = PostView.create(post, viewer);
            Instant afterCreate = Instant.now();

            // then
            assertThat(postView.getViewedAt())
                    .isAfterOrEqualTo(beforeCreate)
                    .isBeforeOrEqualTo(afterCreate);
        }
    }

    @Nested
    @DisplayName("Getter 메서드")
    class GetterTest {

        @Test
        @DisplayName("getPost가 올바른 게시글을 반환")
        void getPost_ReturnsCorrectPost() {
            // given
            Board board = createGeneralBoard();
            User author = createMemberUser();
            User viewer = createAnotherMemberUser();
            Post post = createPost(board, author);
            PostView postView = PostView.create(post, viewer);

            // when & then
            assertThat(postView.getPost()).isEqualTo(post);
        }

        @Test
        @DisplayName("getViewer가 올바른 조회자를 반환")
        void getViewer_ReturnsCorrectViewer() {
            // given
            Board board = createGeneralBoard();
            User author = createMemberUser();
            User viewer = createAnotherMemberUser();
            Post post = createPost(board, author);
            PostView postView = PostView.create(post, viewer);

            // when & then
            assertThat(postView.getViewer()).isEqualTo(viewer);
        }

        @Test
        @DisplayName("getViewedAt가 올바른 조회 시각을 반환")
        void getViewedAt_ReturnsCorrectViewedAt() {
            // given
            Board board = createGeneralBoard();
            User author = createMemberUser();
            User viewer = createAnotherMemberUser();
            Post post = createPost(board, author);
            PostView postView = PostView.create(post, viewer);

            // when & then
            assertThat(postView.getViewedAt()).isNotNull();
        }
    }
}
