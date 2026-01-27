package igrus.web.community.post.domain;

import igrus.web.community.board.domain.Board;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.titleWithLength;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Post 도메인 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 */
@DisplayName("Post 도메인")
class PostTest {

    @Nested
    @DisplayName("createPost 정적 팩토리 메서드")
    class CreatePostTest {

        @Test
        @DisplayName("유효한 제목과 내용으로 일반 게시글 생성 성공")
        void createPost_WithValidTitleAndContent_ReturnsPost() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            String title = "테스트 제목";
            String content = "테스트 내용입니다.";

            // when
            Post post = Post.createPost(board, author, title, content);

            // then
            assertThat(post).isNotNull();
            assertThat(post.getBoard()).isEqualTo(board);
            assertThat(post.getAuthor()).isEqualTo(author);
            assertThat(post.getTitle()).isEqualTo(title);
            assertThat(post.getContent()).isEqualTo(content);
            assertThat(post.getViewCount()).isZero();
            assertThat(post.isAnonymous()).isFalse();
            assertThat(post.isQuestion()).isFalse();
            assertThat(post.isVisibleToAssociate()).isFalse();
        }

        @Test
        @DisplayName("제목이 정확히 100자일 때 생성 성공")
        void createPost_WithTitleAt100Chars_ReturnsPost() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            String title = titleWithLength(100);
            String content = "테스트 내용입니다.";

            // when
            Post post = Post.createPost(board, author, title, content);

            // then
            assertThat(post).isNotNull();
            assertThat(post.getTitle()).hasSize(100);
        }

        @Test
        @DisplayName("제목이 100자를 초과하면 IllegalArgumentException 발생")
        void createPost_WithTitleOver100Chars_ThrowsException() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            String title = titleWithLength(101);
            String content = "테스트 내용입니다.";

            // when & then
            assertThatThrownBy(() -> Post.createPost(board, author, title, content))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("100");
        }
    }

    @Nested
    @DisplayName("createAnonymousPost 정적 팩토리 메서드")
    class CreateAnonymousPostTest {

        @Test
        @DisplayName("자유게시판에서 익명 게시글 생성 성공")
        void createAnonymousPost_InGeneralBoard_ReturnsAnonymousPost() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            String title = "익명 게시글 제목";
            String content = "익명 게시글 내용입니다.";

            // when
            Post post = Post.createAnonymousPost(board, author, title, content);

            // then
            assertThat(post).isNotNull();
            assertThat(post.isAnonymous()).isTrue();
            assertThat(post.getBoard()).isEqualTo(board);
            assertThat(post.getAuthor()).isEqualTo(author);
            assertThat(post.getTitle()).isEqualTo(title);
            assertThat(post.getContent()).isEqualTo(content);
        }

        @Test
        @DisplayName("공지사항 게시판에서 익명 게시글 생성 시 IllegalArgumentException 발생")
        void createAnonymousPost_InNoticesBoard_ThrowsException() {
            // given
            Board board = noticesBoard();
            User author = createMemberWithId();
            String title = "익명 게시글 제목";
            String content = "익명 게시글 내용입니다.";

            // when & then
            assertThatThrownBy(() -> Post.createAnonymousPost(board, author, title, content))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("자유게시판");
        }

        @Test
        @DisplayName("인사이트 게시판에서 익명 게시글 생성 시 IllegalArgumentException 발생")
        void createAnonymousPost_InInsightBoard_ThrowsException() {
            // given
            Board board = insightBoard();
            User author = createMemberWithId();
            String title = "익명 게시글 제목";
            String content = "익명 게시글 내용입니다.";

            // when & then
            assertThatThrownBy(() -> Post.createAnonymousPost(board, author, title, content))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("자유게시판");
        }
    }

    @Nested
    @DisplayName("createNotice 정적 팩토리 메서드")
    class CreateNoticeTest {

        @Test
        @DisplayName("공지사항 게시판에서 공지사항 생성 성공")
        void createNotice_InNoticesBoard_ReturnsNotice() {
            // given
            Board board = noticesBoard();
            User author = createAdminWithId();
            String title = "공지사항 제목";
            String content = "공지사항 내용입니다.";

            // when
            Post post = Post.createNotice(board, author, title, content, false);

            // then
            assertThat(post).isNotNull();
            assertThat(post.getBoard()).isEqualTo(board);
            assertThat(post.isVisibleToAssociate()).isFalse();
        }

        @Test
        @DisplayName("준회원 공개 옵션으로 공지사항 생성 성공")
        void createNotice_WithVisibleToAssociate_ReturnsNoticeWithVisibility() {
            // given
            Board board = noticesBoard();
            User author = createAdminWithId();
            String title = "준회원 공개 공지사항";
            String content = "준회원도 볼 수 있는 공지사항입니다.";

            // when
            Post post = Post.createNotice(board, author, title, content, true);

            // then
            assertThat(post).isNotNull();
            assertThat(post.isVisibleToAssociate()).isTrue();
        }

        @Test
        @DisplayName("자유게시판에서 공지사항 생성 시 IllegalArgumentException 발생")
        void createNotice_InGeneralBoard_ThrowsException() {
            // given
            Board board = generalBoard();
            User author = createAdminWithId();
            String title = "공지사항 제목";
            String content = "공지사항 내용입니다.";

            // when & then
            assertThatThrownBy(() -> Post.createNotice(board, author, title, content, true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("공지사항");
        }

        @Test
        @DisplayName("인사이트 게시판에서 공지사항 생성 시 IllegalArgumentException 발생")
        void createNotice_InInsightBoard_ThrowsException() {
            // given
            Board board = insightBoard();
            User author = createAdminWithId();
            String title = "공지사항 제목";
            String content = "공지사항 내용입니다.";

            // when & then
            assertThatThrownBy(() -> Post.createNotice(board, author, title, content, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("공지사항");
        }
    }

    @Nested
    @DisplayName("updateContent 메서드")
    class UpdateContentTest {

        @Test
        @DisplayName("유효한 제목과 내용으로 수정 성공")
        void updateContent_WithValidTitleAndContent_UpdatesSuccessfully() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            Post post = Post.createPost(board, author, "원본 제목", "원본 내용");
            String newTitle = "수정된 제목";
            String newContent = "수정된 내용입니다.";

            // when
            post.updateContent(newTitle, newContent);

            // then
            assertThat(post.getTitle()).isEqualTo(newTitle);
            assertThat(post.getContent()).isEqualTo(newContent);
        }

        @Test
        @DisplayName("제목이 100자를 초과하면 IllegalArgumentException 발생")
        void updateContent_WithTitleOver100Chars_ThrowsException() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            Post post = Post.createPost(board, author, "원본 제목", "원본 내용");
            String newTitle = titleWithLength(101);
            String newContent = "수정된 내용입니다.";

            // when & then
            assertThatThrownBy(() -> post.updateContent(newTitle, newContent))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("100");
        }

        @Test
        @DisplayName("제목이 정확히 100자일 때 수정 성공")
        void updateContent_WithTitleAt100Chars_UpdatesSuccessfully() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            Post post = Post.createPost(board, author, "원본 제목", "원본 내용");
            String newTitle = titleWithLength(100);
            String newContent = "수정된 내용입니다.";

            // when
            post.updateContent(newTitle, newContent);

            // then
            assertThat(post.getTitle()).hasSize(100);
        }
    }

    @Nested
    @DisplayName("incrementViewCount 메서드")
    class IncrementViewCountTest {

        @Test
        @DisplayName("조회수 1 증가")
        void incrementViewCount_IncrementsBy1() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            Post post = Post.createPost(board, author, "제목", "내용");
            assertThat(post.getViewCount()).isZero();

            // when
            post.incrementViewCount();

            // then
            assertThat(post.getViewCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("여러 번 호출 시 조회수 누적")
        void incrementViewCount_MultipleIncrements_Accumulates() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            Post post = Post.createPost(board, author, "제목", "내용");

            // when
            post.incrementViewCount();
            post.incrementViewCount();
            post.incrementViewCount();

            // then
            assertThat(post.getViewCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("canModify 메서드")
    class CanModifyTest {

        @Test
        @DisplayName("작성자는 자신의 게시글 수정 가능")
        void canModify_ByAuthor_ReturnsTrue() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            Post post = Post.createPost(board, author, "제목", "내용");

            // when
            boolean result = post.canModify(author);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("관리자는 다른 사용자의 게시글 수정 가능")
        void canModify_ByAdmin_ReturnsTrue() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            User admin = createAdminWithId();
            Post post = Post.createPost(board, author, "제목", "내용");

            // when
            boolean result = post.canModify(admin);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("작성자가 아닌 일반 회원은 수정 불가")
        void canModify_ByNonAuthorMember_ReturnsFalse() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            User anotherMember = createAnotherMemberWithId();
            Post post = Post.createPost(board, author, "제목", "내용");

            // when
            boolean result = post.canModify(anotherMember);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("운영자는 다른 사용자의 게시글 수정 불가")
        void canModify_ByOperator_ReturnsFalse() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            User operator = createOperatorWithId();
            Post post = Post.createPost(board, author, "제목", "내용");

            // when
            boolean result = post.canModify(operator);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null 사용자는 수정 불가")
        void canModify_ByNullUser_ReturnsFalse() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            Post post = Post.createPost(board, author, "제목", "내용");

            // when
            boolean result = post.canModify(null);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("canDelete 메서드")
    class CanDeleteTest {

        @Test
        @DisplayName("작성자는 자신의 게시글 삭제 가능")
        void canDelete_ByAuthor_ReturnsTrue() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            Post post = Post.createPost(board, author, "제목", "내용");

            // when
            boolean result = post.canDelete(author);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("운영자는 다른 사용자의 게시글 삭제 가능")
        void canDelete_ByOperator_ReturnsTrue() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            User operator = createOperatorWithId();
            Post post = Post.createPost(board, author, "제목", "내용");

            // when
            boolean result = post.canDelete(operator);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("관리자는 다른 사용자의 게시글 삭제 가능")
        void canDelete_ByAdmin_ReturnsTrue() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            User admin = createAdminWithId();
            Post post = Post.createPost(board, author, "제목", "내용");

            // when
            boolean result = post.canDelete(admin);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("작성자가 아닌 일반 회원은 삭제 불가")
        void canDelete_ByNonAuthorMember_ReturnsFalse() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            User anotherMember = createAnotherMemberWithId();
            Post post = Post.createPost(board, author, "제목", "내용");

            // when
            boolean result = post.canDelete(anotherMember);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("null 사용자는 삭제 불가")
        void canDelete_ByNullUser_ReturnsFalse() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            Post post = Post.createPost(board, author, "제목", "내용");

            // when
            boolean result = post.canDelete(null);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("setQuestion 메서드")
    class SetQuestionTest {

        @Test
        @DisplayName("자유게시판에서 질문 플래그를 true로 설정 성공")
        void setQuestion_TrueInGeneralBoard_SetsSuccessfully() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            Post post = Post.createPost(board, author, "제목", "내용");
            assertThat(post.isQuestion()).isFalse();

            // when
            post.setQuestion(true);

            // then
            assertThat(post.isQuestion()).isTrue();
        }

        @Test
        @DisplayName("자유게시판에서 질문 플래그를 false로 설정 성공")
        void setQuestion_FalseInGeneralBoard_SetsSuccessfully() {
            // given
            Board board = generalBoard();
            User author = createMemberWithId();
            Post post = Post.createPost(board, author, "제목", "내용");
            post.setQuestion(true);

            // when
            post.setQuestion(false);

            // then
            assertThat(post.isQuestion()).isFalse();
        }

        @Test
        @DisplayName("공지사항 게시판에서 질문 플래그 설정 시 IllegalArgumentException 발생")
        void setQuestion_InNoticesBoard_ThrowsException() {
            // given
            Board board = noticesBoard();
            User author = createAdminWithId();
            Post post = Post.createNotice(board, author, "제목", "내용", false);

            // when & then
            assertThatThrownBy(() -> post.setQuestion(true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("자유게시판");
        }

        @Test
        @DisplayName("인사이트 게시판에서 질문 플래그 설정 시 IllegalArgumentException 발생")
        void setQuestion_InInsightBoard_ThrowsException() {
            // given
            Board board = insightBoard();
            User author = createMemberWithId();
            Post post = Post.createPost(board, author, "제목", "내용");

            // when & then
            assertThatThrownBy(() -> post.setQuestion(true))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("자유게시판");
        }
    }

    @Nested
    @DisplayName("이미지 관리 메서드")
    class ImageManagementTest {

        @Nested
        @DisplayName("addImage")
        class AddImageTest {

            @Test
            @DisplayName("이미지 1개 추가 성공")
            void addImage_SingleImage_AddsSuccessfully() {
                // given
                Board board = generalBoard();
                User author = createMemberWithId();
                Post post = Post.createPost(board, author, "제목", "내용");
                PostImage image = PostImage.create(post, "https://example.com/image1.jpg", 0);

                // when
                post.addImage(image);

                // then
                assertThat(post.getImages()).hasSize(1);
                assertThat(post.getImages().get(0)).isEqualTo(image);
            }

            @Test
            @DisplayName("이미지 5개까지 추가 성공")
            void addImage_UpTo5Images_AddsSuccessfully() {
                // given
                Board board = generalBoard();
                User author = createMemberWithId();
                Post post = Post.createPost(board, author, "제목", "내용");

                // when
                for (int i = 0; i < 5; i++) {
                    PostImage image = PostImage.create(post, "https://example.com/image" + i + ".jpg", i);
                    post.addImage(image);
                }

                // then
                assertThat(post.getImages()).hasSize(5);
            }

            @Test
            @DisplayName("이미지 6개 추가 시 IllegalArgumentException 발생")
            void addImage_6thImage_ThrowsException() {
                // given
                Board board = generalBoard();
                User author = createMemberWithId();
                Post post = Post.createPost(board, author, "제목", "내용");

                for (int i = 0; i < 5; i++) {
                    PostImage image = PostImage.create(post, "https://example.com/image" + i + ".jpg", i);
                    post.addImage(image);
                }

                PostImage sixthImage = PostImage.create(post, "https://example.com/image6.jpg", 5);

                // when & then
                assertThatThrownBy(() -> post.addImage(sixthImage))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("5");
            }
        }

        @Nested
        @DisplayName("clearImages")
        class ClearImagesTest {

            @Test
            @DisplayName("모든 이미지 삭제 성공")
            void clearImages_WithImages_RemovesAllImages() {
                // given
                Board board = generalBoard();
                User author = createMemberWithId();
                Post post = Post.createPost(board, author, "제목", "내용");

                for (int i = 0; i < 3; i++) {
                    PostImage image = PostImage.create(post, "https://example.com/image" + i + ".jpg", i);
                    post.addImage(image);
                }
                assertThat(post.getImages()).hasSize(3);

                // when
                post.clearImages();

                // then
                assertThat(post.getImages()).isEmpty();
            }

            @Test
            @DisplayName("이미지가 없을 때 clearImages 호출해도 정상 동작")
            void clearImages_WhenEmpty_NoException() {
                // given
                Board board = generalBoard();
                User author = createMemberWithId();
                Post post = Post.createPost(board, author, "제목", "내용");

                // when & then - 예외 없이 정상 동작
                post.clearImages();
                assertThat(post.getImages()).isEmpty();
            }
        }

        @Nested
        @DisplayName("getImages")
        class GetImagesTest {

            @Test
            @DisplayName("불변 리스트 반환")
            void getImages_ReturnsUnmodifiableList() {
                // given
                Board board = generalBoard();
                User author = createMemberWithId();
                Post post = Post.createPost(board, author, "제목", "내용");
                PostImage image = PostImage.create(post, "https://example.com/image.jpg", 0);
                post.addImage(image);

                // when
                var images = post.getImages();

                // then
                assertThatThrownBy(() -> images.add(PostImage.create(post, "https://example.com/new.jpg", 1)))
                        .isInstanceOf(UnsupportedOperationException.class);
            }

            @Test
            @DisplayName("이미지 목록 정상 조회")
            void getImages_WithImages_ReturnsImageList() {
                // given
                Board board = generalBoard();
                User author = createMemberWithId();
                Post post = Post.createPost(board, author, "제목", "내용");

                PostImage image1 = PostImage.create(post, "https://example.com/image1.jpg", 0);
                PostImage image2 = PostImage.create(post, "https://example.com/image2.jpg", 1);
                post.addImage(image1);
                post.addImage(image2);

                // when
                var images = post.getImages();

                // then
                assertThat(images).hasSize(2);
                assertThat(images).containsExactly(image1, image2);
            }

            @Test
            @DisplayName("이미지가 없을 때 빈 리스트 반환")
            void getImages_WhenEmpty_ReturnsEmptyList() {
                // given
                Board board = generalBoard();
                User author = createMemberWithId();
                Post post = Post.createPost(board, author, "제목", "내용");

                // when
                var images = post.getImages();

                // then
                assertThat(images).isEmpty();
            }
        }
    }
}
