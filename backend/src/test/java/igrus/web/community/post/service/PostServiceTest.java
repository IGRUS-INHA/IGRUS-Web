package igrus.web.community.post.service;

import igrus.web.community.board.domain.Board;
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
import igrus.web.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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
import static org.mockito.Mockito.verify;

/**
 * PostService 단위 테스트.
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
 * </ul>
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

    @Mock
    private PostViewService postViewService;

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
        // 게시판 생성 - 픽스처 사용
        generalBoard = generalBoard();
        noticesBoard = noticesBoard();
        insightBoard = insightBoard();

        // 사용자 생성 - 픽스처 사용
        memberUser = createMemberWithId();
        operatorUser = createOperatorWithId();

        // 인증 정보 생성 - 픽스처 사용
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
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

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
            CreatePostRequest request = anonymousCreateRequest();

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
            CreatePostRequest request = questionCreateRequest();

            Post savedPost = questionPost(generalBoard, memberUser);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

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
            CreatePostRequest request = questionCreateRequest();

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
            CreatePostRequest request = createRequest();

            Post savedPost = normalPost(generalBoard, memberUser);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

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
            CreatePostRequest request = createRequest("일반 게시글 제목", "일반 게시글 내용입니다.");

            Post savedPost = withId(createNormalPost(generalBoard, memberUser, request.title(), request.content()), 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

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
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

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
            String longTitle = titleWithLength(101);
            CreatePostRequest request = createRequest(longTitle, "내용");

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
            String exactTitle = titleWithLength(100);
            CreatePostRequest request = createRequest(exactTitle, "내용");

            Post savedPost = withId(createNormalPost(generalBoard, memberUser, exactTitle, "내용"), 1L);

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(generalBoard);
            doNothing().when(boardPermissionService).checkWritePermission(generalBoard, memberUser.getRole());
            doNothing().when(postRateLimitService).checkRateLimit(memberUser);
            given(postRepository.save(any(Post.class))).willReturn(savedPost);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, memberAuth);

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
            CreatePostRequest request = createRequestWithImages(5);

            Post savedPost = normalPost(generalBoard, memberUser);

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
            CreatePostRequest request = createRequestWithImages(6);

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
            UpdatePostRequest request = updateRequest("수정된 제목", "수정된 내용입니다.");

            Post existingPost = normalPost(generalBoard, memberUser, postId);

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
            UpdatePostRequest request = updateRequest("익명 게시글 수정", "익명 게시글 수정 내용");

            Post anonymousPost = anonymousPost(generalBoard, memberUser, postId);

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
            UpdatePostRequest request = updateRequest("타인 수정 시도", "타인이 익명 게시글 수정 시도");

            // 다른 사용자(operatorUser)가 작성한 익명 게시글
            Post anonymousPost = anonymousPost(generalBoard, operatorUser, postId);

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

            Post post = normalPost(generalBoard, memberUser, postId);

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

            Post anonymousPost = anonymousPost(generalBoard, memberUser, postId);

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
            Post anonymousPost = anonymousPost(generalBoard, operatorUser, postId);

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
            Post post = normalPost(generalBoard, memberUser, postId);

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
            CreatePostRequest request = createRequest("공지사항 제목", "공지사항 내용입니다.");

            Post savedNotice = notice(noticesBoard, operatorUser);

            given(userRepository.findById(operatorAuth.userId())).willReturn(Optional.of(operatorUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(noticesBoard);
            doNothing().when(boardPermissionService).checkWritePermission(noticesBoard, operatorUser.getRole());
            given(postRepository.save(any(Post.class))).willReturn(savedNotice);

            // when
            PostCreateResponse response = postService.createPost(boardCode, request, operatorAuth);

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
            CreatePostRequest request = createRequest("정회원 공지사항 시도", "정회원이 공지사항 작성 시도");

            given(userRepository.findById(memberAuth.userId())).willReturn(Optional.of(memberUser));
            given(boardService.getBoardEntity(boardCode)).willReturn(noticesBoard);
            doNothing().when(boardPermissionService).checkWritePermission(noticesBoard, memberUser.getRole());

            // when & then
            assertThatThrownBy(() -> postService.createPost(boardCode, request, memberAuth))
                    .isInstanceOf(BoardWriteDeniedException.class);
        }
    }
}
