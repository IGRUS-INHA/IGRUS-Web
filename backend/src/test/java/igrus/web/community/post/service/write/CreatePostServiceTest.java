package igrus.web.community.post.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.exception.BoardWriteDeniedException;
import igrus.web.community.board.service.read.GetBoardEntityService;
import igrus.web.community.board.service.permission.CheckWritePermissionService;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.dto.request.CreatePostRequest;
import igrus.web.community.post.dto.response.PostCreateResponse;
import igrus.web.community.post.exception.InvalidPostOptionException;
import igrus.web.community.post.exception.PostImageLimitExceededException;
import igrus.web.community.post.exception.PostTitleTooLongException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.community.post.service.support.CheckPostRateLimitService;
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

import static igrus.web.common.fixture.TestEntityIdAssigner.withId;
import static igrus.web.common.fixture.UserTestFixture.*;
import static igrus.web.community.fixture.BoardTestFixture.*;
import static igrus.web.community.fixture.PostTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * CreatePostService 단위 테스트.
 *
 * <p>테스트 픽스처를 활용하여 변경에 강건한 테스트를 작성합니다.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>BRD-050: 자유게시판 익명 옵션 성공</li>
 *     <li>BRD-051: 공지사항 익명 옵션 예외</li>
 *     <li>BRD-052: 정보공유 익명 옵션 예외</li>
 *     <li>BRD-060: 자유게시판 질문 태그 성공</li>
 *     <li>BRD-061: 정보공유 질문 태그 예외</li>
 *     <li>PST-001~005: 게시글 작성</li>
 *     <li>PST-010~012: 이미지 첨부</li>
 *     <li>PST-050~053: 공지사항 작성</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreatePostService 단위 테스트")
class CreatePostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GetBoardEntityService getBoardEntityService;

    @Mock
    private CheckWritePermissionService checkWritePermissionService;

    @Mock
    private CheckPostRateLimitService checkPostRateLimitService;

    @Mock
    private PostValidator postValidator;

    @InjectMocks
    private CreatePostService createPostService;

    private Board generalBoard;
    private Board noticesBoard;
    private Board insightBoard;
    private User memberUser;
    private User operatorUser;
    private AuthenticatedUser memberAuth;
    private AuthenticatedUser operatorAuth;

    @BeforeEach
    void setUp() {
        generalBoard = generalBoard();
        noticesBoard = noticesBoard();
        insightBoard = insightBoard();

        memberUser = createMemberWithId();
        operatorUser = createOperatorWithId();

        memberAuth = memberAuth();
        operatorAuth = operatorAuth();
    }

    @Nested
    @DisplayName("익명 옵션 테스트")
    class AnonymousOptionTest {

        @DisplayName("BRD-050: 자유게시판에서 익명 옵션으로 게시글 작성 성공")
        @Test
        void createPost_InGeneral_WithAnonymousOption_Success() {
            // given
            String boardCode = "general";
            CreatePostRequest request = anonymousCreateRequest();

            Post savedPost = anonymousPost(generalBoard, memberUser);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = createPostService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isNotNull().isPositive();
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("BRD-051: 공지사항에서 익명 옵션 사용 시 InvalidPostOptionException 발생")
        @Test
        void createPost_InNotices_WithAnonymousOption_ThrowsException() {
            // given
            String boardCode = "notices";
            CreatePostRequest request = anonymousCreateRequest();

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(noticesBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(noticesBoard, operatorUser.getRole());

            // when & then
            assertThatThrownBy(() -> createPostService.createPost(boardCode, request, operatorAuth))
                    .isInstanceOf(InvalidPostOptionException.class)
                    .hasMessageContaining("익명");
        }

        @DisplayName("BRD-052: 정보공유에서 익명 옵션 사용 시 InvalidPostOptionException 발생")
        @Test
        void createPost_InInsight_WithAnonymousOption_ThrowsException() {
            // given
            String boardCode = "insight";
            CreatePostRequest request = anonymousCreateRequest();

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(insightBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(insightBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);

            // when & then - 도메인(Post.createAnonymousPost)에서 게시판 옵션 검증
            assertThatThrownBy(() -> createPostService.createPost(boardCode, request, memberAuth))
                    .isInstanceOf(InvalidPostOptionException.class)
                    .hasMessageContaining("익명");
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
            CreatePostRequest request = questionCreateRequest();

            Post savedPost = questionPost(generalBoard, memberUser);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = createPostService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isNotNull().isPositive();
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("BRD-061: 정보공유에서 질문 태그 사용 시 InvalidPostOptionException 발생")
        @Test
        void createPost_InInsight_WithQuestionTag_ThrowsException() {
            // given
            String boardCode = "insight";
            CreatePostRequest request = questionCreateRequest();

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(insightBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(insightBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);

            // when & then - 도메인(Post.setQuestion)에서 게시판 옵션 검증
            assertThatThrownBy(() -> createPostService.createPost(boardCode, request, memberAuth))
                    .isInstanceOf(InvalidPostOptionException.class)
                    .hasMessageContaining("질문");
        }

        @DisplayName("공지사항에서 질문 태그 사용 시 InvalidPostOptionException 발생")
        @Test
        void createPost_InNotices_WithQuestionTag_ThrowsException() {
            // given
            String boardCode = "notices";
            CreatePostRequest request = questionCreateRequest();

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(noticesBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(noticesBoard, operatorUser.getRole());

            // when & then
            assertThatThrownBy(() -> createPostService.createPost(boardCode, request, operatorAuth))
                    .isInstanceOf(InvalidPostOptionException.class)
                    .hasMessageContaining("질문");
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
            CreatePostRequest request = createRequest();

            Post savedPost = normalPost(generalBoard, memberUser);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = createPostService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isNotNull().isPositive();
        }

        @DisplayName("익명 + 질문 옵션 동시 사용 성공 (자유게시판)")
        @Test
        void createPost_WithBothAnonymousAndQuestion_Success() {
            // given
            String boardCode = "general";
            CreatePostRequest request = anonymousQuestionCreateRequest();

            Post savedPost = withId(createAnonymousPost(generalBoard, memberUser), 1L);
            savedPost.setQuestion(true);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = createPostService.createPost(boardCode, request, memberAuth);

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
            CreatePostRequest request = createRequest("일반 게시글 제목", "일반 게시글 내용입니다.");

            Post savedPost = withId(createNormalPost(generalBoard, memberUser, request.title(), request.content()), 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = createPostService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isNotNull().isPositive();
            assertThat(response.title()).isEqualTo("일반 게시글 제목");
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("PST-002: 익명 게시글 작성")
        @Test
        void createPost_Anonymous_Success() {
            // given
            String boardCode = "general";
            CreatePostRequest request = anonymousCreateRequest();

            Post savedPost = anonymousPost(generalBoard, memberUser);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = createPostService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isNotNull().isPositive();
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("PST-003: 질문 태그 게시글 작성")
        @Test
        void createPost_WithQuestionTag_Success() {
            // given
            String boardCode = "general";
            CreatePostRequest request = questionCreateRequest();

            Post savedPost = questionPost(generalBoard, memberUser);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = createPostService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("PST-004: 제목 100자 초과 거부")
        @Test
        void createPost_TitleExceeds100Chars_ThrowsException() {
            // given
            String boardCode = "general";
            String longTitle = titleWithLength(101);
            CreatePostRequest request = createRequest(longTitle, "내용");

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);

            // when & then
            assertThatThrownBy(() -> createPostService.createPost(boardCode, request, memberAuth))
                    .isInstanceOf(PostTitleTooLongException.class)
                    .hasMessageContaining("제목이 너무 깁니다");
        }

        @DisplayName("PST-005: 제목 100자 정상 저장")
        @Test
        void createPost_TitleExactly100Chars_Success() {
            // given
            String boardCode = "general";
            String exactTitle = titleWithLength(100);
            CreatePostRequest request = createRequest(exactTitle, "내용");

            Post savedPost = withId(createNormalPost(generalBoard, memberUser, exactTitle, "내용"), 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = createPostService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isNotNull().isPositive();
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
            CreatePostRequest request = createRequestWithImages(1);

            Post savedPost = normalPost(generalBoard, memberUser);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = createPostService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("PST-011: 이미지 5개 첨부")
        @Test
        void createPost_WithFiveImages_Success() {
            // given
            String boardCode = "general";
            CreatePostRequest request = createRequestWithImages(5);

            Post savedPost = normalPost(generalBoard, memberUser);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = createPostService.createPost(boardCode, request, memberAuth);

            // then
            assertThat(response).isNotNull();
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("PST-012: 이미지 5개 초과 첨부 거부")
        @Test
        void createPost_WithSixImages_ThrowsException() {
            // given
            String boardCode = "general";
            CreatePostRequest request = createRequestWithImages(6);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(checkPostRateLimitService).checkRateLimit(memberUser);
            doThrow(new PostImageLimitExceededException(5, 6))
                    .when(postValidator).validateImageCount(6);

            // when & then
            assertThatThrownBy(() -> createPostService.createPost(boardCode, request, memberAuth))
                    .isInstanceOf(PostImageLimitExceededException.class);
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
            CreatePostRequest request = createRequest("공지사항 제목", "공지사항 내용입니다.");

            Post savedNotice = notice(noticesBoard, operatorUser);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(noticesBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(noticesBoard, operatorUser.getRole());
            given(postRepository.save(any(Post.class))).willReturn(savedNotice);

            // when
            PostCreateResponse response = createPostService.createPost(boardCode, request, operatorAuth);

            // then
            assertThat(response).isNotNull();
            assertThat(response.postId()).isNotNull().isPositive();
            verify(postRepository).save(any(Post.class));
        }

        @DisplayName("PST-052: 공지사항 익명 옵션 없음")
        @Test
        void createNotice_WithAnonymousOption_ThrowsException() {
            // given
            String boardCode = "notices";
            CreatePostRequest request = anonymousCreateRequest();

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(noticesBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(noticesBoard, operatorUser.getRole());

            // when & then
            assertThatThrownBy(() -> createPostService.createPost(boardCode, request, operatorAuth))
                    .isInstanceOf(InvalidPostOptionException.class)
                    .hasMessageContaining("익명");
        }

        @DisplayName("PST-053: 정회원 공지사항 작성 불가")
        @Test
        void createNotice_ByMember_ThrowsException() {
            // given
            String boardCode = "notices";
            CreatePostRequest request = createRequest("정회원 공지사항 시도", "정회원이 공지사항 작성 시도");

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(getBoardEntityService.getBoardEntity(boardCode)).willReturn(noticesBoard);
            doNothing().when(checkWritePermissionService).checkWritePermission(noticesBoard, memberUser.getRole());

            // when & then
            assertThatThrownBy(() -> createPostService.createPost(boardCode, request, memberAuth))
                    .isInstanceOf(BoardWriteDeniedException.class);
        }
    }
}
