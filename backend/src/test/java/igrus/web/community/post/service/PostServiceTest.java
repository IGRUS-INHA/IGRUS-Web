package igrus.web.community.post.service;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.dto.request.CreatePostRequest;
import igrus.web.community.post.dto.request.UpdatePostRequest;
import igrus.web.community.post.dto.response.PostCreateResponse;
import igrus.web.community.post.dto.response.PostUpdateResponse;
import igrus.web.community.board.exception.BoardWriteDeniedException;
import igrus.web.community.board.service.BoardService;
import igrus.web.community.board.service.BoardPermissionService;
import igrus.web.community.post.exception.InvalidPostOptionException;
import igrus.web.community.post.exception.PostAccessDeniedException;
import igrus.web.community.post.exception.PostImageLimitExceededException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

/**
 * PostService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>BRD-050: 자유게시판 익명 옵션 성공</li>
 *     <li>BRD-051: 공지사항 익명 옵션 예외</li>
 *     <li>BRD-052: 정보공유 익명 옵션 예외</li>
 *     <li>BRD-060: 자유게시판 질문 태그 성공</li>
 *     <li>BRD-061: 정보공유 질문 태그 예외</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostService 단위 테스트")
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BoardService boardService;

    @Mock
    private BoardPermissionService boardPermissionService;

    @Mock
    private PostRateLimitService postRateLimitService;

    @InjectMocks
    private PostService postService;

    private Board generalBoard;
    private Board noticesBoard;
    private Board insightBoard;
    private User memberUser;
    private User operatorUser;
    private AuthenticatedUser memberAuth;
    private AuthenticatedUser operatorAuth;

    @BeforeEach
    void setUp() {
        // 게시판 생성
        generalBoard = Board.create(BoardCode.GENERAL, "자유게시판", "자유롭게 이야기를 나눌 수 있는 공간입니다.", true, true, 2);
        noticesBoard = Board.create(BoardCode.NOTICES, "공지사항", "동아리 공지사항을 확인하세요.", false, false, 1);
        insightBoard = Board.create(BoardCode.INSIGHT, "정보공유", "유용한 정보를 공유하세요.", false, false, 3);

        // 사용자 생성
        memberUser = User.create("20200001", "테스트유저", "member@inha.edu", "010-1234-5678", "컴퓨터공학과", "테스트 동기");
        memberUser.changeRole(UserRole.MEMBER);
        memberUser.verifyEmail();
        ReflectionTestUtils.setField(memberUser, "id", 1L);

        operatorUser = User.create("20200002", "운영진유저", "operator@inha.edu", "010-2345-6789", "컴퓨터공학과", "운영진 동기");
        operatorUser.changeRole(UserRole.OPERATOR);
        operatorUser.verifyEmail();
        ReflectionTestUtils.setField(operatorUser, "id", 2L);

        memberAuth = new AuthenticatedUser(1L, "20200001", "MEMBER");
        operatorAuth = new AuthenticatedUser(2L, "20200002", "OPERATOR");
    }

    @Nested
    @DisplayName("익명 옵션 테스트")
    class AnonymousOptionTest {

        @DisplayName("BRD-050: 자유게시판에서 익명 옵션으로 게시글 작성 성공")
        @Test
        void createPost_InGeneral_WithAnonymousOption_Success() {
            // given
            String boardCode = "general";
            CreatePostRequest request = new CreatePostRequest(
                    "테스트 제목",
                    "테스트 내용",
                    true,  // isAnonymous
                    false, // isQuestion
                    false, // isVisibleToAssociate
                    List.of()
            );

            Post savedPost = Post.createAnonymousPost(generalBoard, memberUser, "테스트 제목", "테스트 내용");
            ReflectionTestUtils.setField(savedPost, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isEqualTo(1L);
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("BRD-051: 공지사항에서 익명 옵션 사용 시 InvalidPostOptionException 발생")
        @Test
        void createPost_InNotices_WithAnonymousOption_ThrowsException() {
            // given
            String boardCode = "notices";
            CreatePostRequest request = new CreatePostRequest(
                    "테스트 공지",
                    "공지 내용",
                    true,  // isAnonymous
                    false, // isQuestion
                    false, // isVisibleToAssociate
                    List.of()
            );

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(noticesBoard);
            doNothing().when(boardPermissionService).checkWritePermission(noticesBoard, operatorUser.getRole());

            // when & then
            assertThatThrownBy(() -> postService.createPost(boardCode, request, operatorAuth))
                    .isInstanceOf(InvalidPostOptionException.class)
                    .hasMessageContaining("anonymous");
        }

        @DisplayName("BRD-052: 정보공유에서 익명 옵션 사용 시 InvalidPostOptionException 발생")
        @Test
        void createPost_InInsight_WithAnonymousOption_ThrowsException() {
            // given
            String boardCode = "insight";
            CreatePostRequest request = new CreatePostRequest(
                    "정보공유 제목",
                    "정보공유 내용",
                    true,  // isAnonymous
                    false, // isQuestion
                    false, // isVisibleToAssociate
                    List.of()
            );

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(insightBoard);
            doNothing().when(boardPermissionService).checkWritePermission(insightBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);

            // when & then
            assertThatThrownBy(() -> postService.createPost(boardCode, request, memberAuth))
                    .isInstanceOf(InvalidPostOptionException.class)
                    .hasMessageContaining("anonymous");
        }
    }

    @Nested
    @DisplayName("질문 태그 테스트")
    class QuestionTagTest {

        @DisplayName("BRD-060: 자유게시판에서 질문 태그로 게시글 작성 성공")
        @Test
        void createPost_InGeneral_WithQuestionTag_Success() {
            // given
            String boardCode = "general";
            CreatePostRequest request = new CreatePostRequest(
                    "질문입니다",
                    "질문 내용",
                    false, // isAnonymous
                    true,  // isQuestion
                    false, // isVisibleToAssociate
                    List.of()
            );

            Post savedPost = Post.createPost(generalBoard, memberUser, "질문입니다", "질문 내용");
            savedPost.setQuestion(true);
            ReflectionTestUtils.setField(savedPost, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isEqualTo(1L);
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("BRD-061: 정보공유에서 질문 태그 사용 시 InvalidPostOptionException 발생")
        @Test
        void createPost_InInsight_WithQuestionTag_ThrowsException() {
            // given
            String boardCode = "insight";
            CreatePostRequest request = new CreatePostRequest(
                    "정보공유 제목",
                    "정보공유 내용",
                    false, // isAnonymous
                    true,  // isQuestion
                    false, // isVisibleToAssociate
                    List.of()
            );

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(insightBoard);
            doNothing().when(boardPermissionService).checkWritePermission(insightBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);

            // when & then
            assertThatThrownBy(() -> postService.createPost(boardCode, request, memberAuth))
                    .isInstanceOf(InvalidPostOptionException.class)
                    .hasMessageContaining("question");
        }

        @DisplayName("공지사항에서 질문 태그 사용 시 InvalidPostOptionException 발생")
        @Test
        void createPost_InNotices_WithQuestionTag_ThrowsException() {
            // given
            String boardCode = "notices";
            CreatePostRequest request = new CreatePostRequest(
                    "공지사항 제목",
                    "공지사항 내용",
                    false, // isAnonymous
                    true,  // isQuestion
                    false, // isVisibleToAssociate
                    List.of()
            );

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(noticesBoard);
            doNothing().when(boardPermissionService).checkWritePermission(noticesBoard, operatorUser.getRole());

            // when & then
            assertThatThrownBy(() -> postService.createPost(boardCode, request, operatorAuth))
                    .isInstanceOf(InvalidPostOptionException.class)
                    .hasMessageContaining("question");
        }
    }

    @Nested
    @DisplayName("일반 게시글 작성 테스트")
    class CreatePostTest {

        @DisplayName("일반 게시글 작성 성공")
        @Test
        void createPost_WithValidRequest_Success() {
            // given
            String boardCode = "general";
            CreatePostRequest request = new CreatePostRequest(
                    "일반 게시글",
                    "일반 내용",
                    false,
                    false,
                    false, // isVisibleToAssociate
                    List.of()
            );

            Post savedPost = Post.createPost(generalBoard, memberUser, "일반 게시글", "일반 내용");
            ReflectionTestUtils.setField(savedPost, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isEqualTo(1L);
        }

        @DisplayName("익명 + 질문 옵션 동시 사용 성공 (자유게시판)")
        @Test
        void createPost_WithBothAnonymousAndQuestion_Success() {
            // given
            String boardCode = "general";
            CreatePostRequest request = new CreatePostRequest(
                    "익명 질문",
                    "익명 질문 내용",
                    true,  // isAnonymous
                    true,  // isQuestion
                    false, // isVisibleToAssociate
                    List.of()
            );

            Post savedPost = Post.createAnonymousPost(generalBoard, memberUser, "익명 질문", "익명 질문 내용");
            savedPost.setQuestion(true);
            ReflectionTestUtils.setField(savedPost, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
        }
    }

    // ============================================================
    // PST 테스트 케이스 (post-test-cases.md 기준)
    // ============================================================

    @Nested
    @DisplayName("PST: 게시글 작성 테스트")
    class PstCreatePostTest {

        @DisplayName("PST-001: 일반 게시글 작성")
        @Test
        void createPost_Normal_Success() {
            // given
            String boardCode = "general";
            CreatePostRequest request = new CreatePostRequest(
                    "일반 게시글 제목",
                    "일반 게시글 내용입니다.",
                    false, // isAnonymous
                    false, // isQuestion
                    false, // isVisibleToAssociate
                    List.of()
            );

            Post savedPost = Post.createPost(generalBoard, memberUser, request.title(), request.content());
            ReflectionTestUtils.setField(savedPost, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isEqualTo(1L);
            assertThat(response.title()).isEqualTo("일반 게시글 제목");
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("PST-002: 익명 게시글 작성")
        @Test
        void createPost_Anonymous_Success() {
            // given
            String boardCode = "general";
            CreatePostRequest request = new CreatePostRequest(
                    "익명 게시글 제목",
                    "익명 게시글 내용입니다.",
                    true,  // isAnonymous
                    false, // isQuestion
                    false, // isVisibleToAssociate
                    List.of()
            );

            Post savedPost = Post.createAnonymousPost(generalBoard, memberUser, request.title(), request.content());
            ReflectionTestUtils.setField(savedPost, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isEqualTo(1L);
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("PST-003: 질문 태그 게시글 작성")
        @Test
        void createPost_WithQuestionTag_Success() {
            // given
            String boardCode = "general";
            CreatePostRequest request = new CreatePostRequest(
                    "질문 게시글 제목",
                    "질문 내용입니다. 도움 부탁드립니다.",
                    false, // isAnonymous
                    true,  // isQuestion
                    false, // isVisibleToAssociate
                    List.of()
            );

            Post savedPost = Post.createPost(generalBoard, memberUser, request.title(), request.content());
            savedPost.setQuestion(true);
            ReflectionTestUtils.setField(savedPost, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("PST-004: 제목 100자 초과 거부")
        @Test
        void createPost_TitleExceeds100Chars_ThrowsException() {
            // given
            String boardCode = "general";
            String longTitle = "가".repeat(101); // 101자 제목
            CreatePostRequest request = new CreatePostRequest(
                    longTitle,
                    "내용",
                    false,
                    false,
                    false,
                    List.of()
            );

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);

            // when & then
            assertThatThrownBy(() -> postService.createPost(boardCode, request, memberAuth))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("100");
        }

        @DisplayName("PST-005: 제목 100자 정상 저장")
        @Test
        void createPost_TitleExactly100Chars_Success() {
            // given
            String boardCode = "general";
            String exactTitle = "가".repeat(100); // 정확히 100자
            CreatePostRequest request = new CreatePostRequest(
                    exactTitle,
                    "내용",
                    false,
                    false,
                    false,
                    List.of()
            );

            Post savedPost = Post.createPost(generalBoard, memberUser, exactTitle, "내용");
            ReflectionTestUtils.setField(savedPost, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("PST: 이미지 첨부 테스트")
    class PstImageAttachmentTest {

        @DisplayName("PST-010: 이미지 1개 첨부")
        @Test
        void createPost_WithOneImage_Success() {
            // given
            String boardCode = "general";
            CreatePostRequest request = new CreatePostRequest(
                    "이미지 첨부 게시글",
                    "이미지가 첨부된 게시글입니다.",
                    false,
                    false,
                    false,
                    List.of("https://example.com/image1.jpg")
            );

            Post savedPost = Post.createPost(generalBoard, memberUser, request.title(), request.content());
            ReflectionTestUtils.setField(savedPost, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("PST-011: 이미지 5개 첨부")
        @Test
        void createPost_WithFiveImages_Success() {
            // given
            String boardCode = "general";
            List<String> imageUrls = List.of(
                    "https://example.com/image1.jpg",
                    "https://example.com/image2.jpg",
                    "https://example.com/image3.jpg",
                    "https://example.com/image4.jpg",
                    "https://example.com/image5.jpg"
            );
            CreatePostRequest request = new CreatePostRequest(
                    "이미지 5개 첨부 게시글",
                    "최대 개수 이미지 첨부",
                    false,
                    false,
                    false,
                    imageUrls
            );

            Post savedPost = Post.createPost(generalBoard, memberUser, request.title(), request.content());
            ReflectionTestUtils.setField(savedPost, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("PST-012: 이미지 5개 초과 첨부 거부")
        @Test
        void createPost_WithSixImages_ThrowsException() {
            // given
            String boardCode = "general";
            List<String> imageUrls = List.of(
                    "https://example.com/image1.jpg",
                    "https://example.com/image2.jpg",
                    "https://example.com/image3.jpg",
                    "https://example.com/image4.jpg",
                    "https://example.com/image5.jpg",
                    "https://example.com/image6.jpg"
            );
            CreatePostRequest request = new CreatePostRequest(
                    "이미지 6개 첨부 시도",
                    "이미지 개수 초과",
                    false,
                    false,
                    false,
                    imageUrls
            );

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);

            // when & then
            assertThatThrownBy(() -> postService.createPost(boardCode, request, memberAuth))
                    .isInstanceOf(PostImageLimitExceededException.class);
        }
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
            UpdatePostRequest request = new UpdatePostRequest(
                    "수정된 제목",
                    "수정된 내용입니다.",
                    false,
                    List.of()
            );

            Post existingPost = Post.createPost(generalBoard, memberUser, "원래 제목", "원래 내용");
            ReflectionTestUtils.setField(existingPost, "id", postId);
            ReflectionTestUtils.setField(generalBoard, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(existingPost));

            // when
            PostUpdateResponse response = postService.updatePost(boardCode, postId, request, memberAuth);

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
            UpdatePostRequest request = new UpdatePostRequest(
                    "익명 게시글 수정",
                    "익명 게시글 수정 내용",
                    false,
                    List.of()
            );

            Post anonymousPost = Post.createAnonymousPost(generalBoard, memberUser, "익명 원래 제목", "익명 원래 내용");
            ReflectionTestUtils.setField(anonymousPost, "id", postId);
            ReflectionTestUtils.setField(generalBoard, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(anonymousPost));

            // when
            PostUpdateResponse response = postService.updatePost(boardCode, postId, request, memberAuth);

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
            UpdatePostRequest request = new UpdatePostRequest(
                    "타인 수정 시도",
                    "타인이 익명 게시글 수정 시도",
                    false,
                    List.of()
            );

            // 다른 사용자(operatorUser)가 작성한 익명 게시글
            Post anonymousPost = Post.createAnonymousPost(generalBoard, operatorUser, "익명 제목", "익명 내용");
            ReflectionTestUtils.setField(anonymousPost, "id", postId);
            ReflectionTestUtils.setField(generalBoard, "id", 1L);

            // memberUser가 수정 시도
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(anonymousPost));

            // when & then
            assertThatThrownBy(() -> postService.updatePost(boardCode, postId, request, memberAuth))
                    .isInstanceOf(PostAccessDeniedException.class)
                    .hasMessageContaining("권한");
        }
    }

    @Nested
    @DisplayName("PST: 게시글 삭제 테스트")
    class PstDeletePostTest {

        @DisplayName("PST-040: 본인 게시글 삭제 (Soft Delete)")
        @Test
        void deletePost_ByAuthor_Success() {
            // given
            String boardCode = "general";
            Long postId = 1L;

            Post post = Post.createPost(generalBoard, memberUser, "삭제할 게시글", "삭제할 내용");
            ReflectionTestUtils.setField(post, "id", postId);
            ReflectionTestUtils.setField(generalBoard, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            postService.deletePost(boardCode, postId, memberAuth);

            // then
            assertThat(post.isDeleted()).isTrue();
        }

        @DisplayName("PST-046: 익명 게시글 본인 삭제")
        @Test
        void deletePost_AnonymousPost_ByAuthor_Success() {
            // given
            String boardCode = "general";
            Long postId = 1L;

            Post anonymousPost = Post.createAnonymousPost(generalBoard, memberUser, "익명 게시글", "익명 내용");
            ReflectionTestUtils.setField(anonymousPost, "id", postId);
            ReflectionTestUtils.setField(generalBoard, "id", 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(anonymousPost));

            // when
            postService.deletePost(boardCode, postId, memberAuth);

            // then
            assertThat(anonymousPost.isDeleted()).isTrue();
        }

        @DisplayName("PST-047: 익명 게시글 타인 삭제 시도")
        @Test
        void deletePost_AnonymousPost_ByOther_ThrowsException() {
            // given
            String boardCode = "general";
            Long postId = 1L;

            // operatorUser가 작성한 익명 게시글
            Post anonymousPost = Post.createAnonymousPost(generalBoard, operatorUser, "익명 게시글", "익명 내용");
            ReflectionTestUtils.setField(anonymousPost, "id", postId);
            ReflectionTestUtils.setField(generalBoard, "id", 1L);

            // memberUser가 삭제 시도 (OPERATOR가 아닌 일반 MEMBER이므로 삭제 불가)
            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(anonymousPost));

            // when & then
            assertThatThrownBy(() -> postService.deletePost(boardCode, postId, memberAuth))
                    .isInstanceOf(PostAccessDeniedException.class)
                    .hasMessageContaining("권한");
        }

        @DisplayName("PST-043: 관리자 타인 게시글 삭제")
        @Test
        void deletePost_ByOperator_Success() {
            // given
            String boardCode = "general";
            Long postId = 1L;

            // memberUser가 작성한 게시글
            Post post = Post.createPost(generalBoard, memberUser, "일반 게시글", "내용");
            ReflectionTestUtils.setField(post, "id", postId);
            ReflectionTestUtils.setField(generalBoard, "id", 1L);

            // operatorUser가 삭제 (OPERATOR 권한으로 타인 게시글 삭제 가능)
            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            given(postRepository.findById(postId)).willReturn(Optional.of(post));

            // when
            postService.deletePost(boardCode, postId, operatorAuth);

            // then
            assertThat(post.isDeleted()).isTrue();
        }
    }

    @Nested
    @DisplayName("PST: 공지사항 작성 테스트")
    class PstNoticeTest {

        @DisplayName("PST-050: 운영진 공지사항 작성")
        @Test
        void createNotice_ByOperator_Success() {
            // given
            String boardCode = "notices";
            CreatePostRequest request = new CreatePostRequest(
                    "공지사항 제목",
                    "공지사항 내용입니다.",
                    false, // isAnonymous
                    false, // isQuestion
                    false, // isVisibleToAssociate
                    List.of()
            );

            Post savedNotice = Post.createNotice(noticesBoard, operatorUser, request.title(), request.content(), false);
            ReflectionTestUtils.setField(savedNotice, "id", 1L);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(noticesBoard);
            doNothing().when(boardPermissionService).checkWritePermission(noticesBoard, operatorUser.getRole());
            given(postRepository.save(any(Post.class))).willReturn(savedNotice);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, operatorAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isEqualTo(1L);
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("PST-052: 공지사항 익명 옵션 없음")
        @Test
        void createNotice_WithAnonymousOption_ThrowsException() {
            // given
            String boardCode = "notices";
            CreatePostRequest request = new CreatePostRequest(
                    "익명 공지사항 시도",
                    "익명 옵션으로 공지사항 작성 시도",
                    true,  // isAnonymous - 공지사항에서 불가
                    false,
                    false,
                    List.of()
            );

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(noticesBoard);
            doNothing().when(boardPermissionService).checkWritePermission(noticesBoard, operatorUser.getRole());

            // when & then
            assertThatThrownBy(() -> postService.createPost(boardCode, request, operatorAuth))
                    .isInstanceOf(InvalidPostOptionException.class)
                    .hasMessageContaining("anonymous");
        }

        @DisplayName("PST-053: 정회원 공지사항 작성 불가")
        @Test
        void createNotice_ByMember_ThrowsException() {
            // given
            String boardCode = "notices";
            CreatePostRequest request = new CreatePostRequest(
                    "정회원 공지사항 시도",
                    "정회원이 공지사항 작성 시도",
                    false,
                    false,
                    false,
                    List.of()
            );

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(noticesBoard);
            doNothing().when(boardPermissionService).checkWritePermission(noticesBoard, memberUser.getRole());

            // when & then
            assertThatThrownBy(() -> postService.createPost(boardCode, request, memberAuth))
                    .isInstanceOf(BoardWriteDeniedException.class);
        }
    }
}
