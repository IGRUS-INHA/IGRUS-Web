package igrus.web.community.post.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.dto.request.CreatePostRequest;
import igrus.web.community.post.dto.request.UpdatePostRequest;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.community.board.repository.BoardRepository;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PostController 통합 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>BRD-004: 게시판 목록 조회 성능</li>
 *     <li>BRD-020~022: 준회원 공개 설정</li>
 *     <li>BRD-030~033: 게시판 검색 기능</li>
 *     <li>BRD-040~042: 페이지네이션</li>
 *     <li>BRD-062: 질문만 필터링</li>
 * </ul>
 * </p>
 */
@AutoConfigureMockMvc
@DisplayName("PostController 통합 테스트")
class PostControllerTest extends ServiceIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardPermissionRepository boardPermissionRepository;

    @Autowired
    private PostRepository postRepository;

    private static final String BASE_URL = "/api/v1/boards";

    private User memberUser;
    private User associateUser;
    private User operatorUser;

    private Board noticesBoard;
    private Board generalBoard;
    private Board insightBoard;

    @BeforeEach
    void setUp() {
        setUpBase();
        setupBoardData();
        memberUser = createAndSaveUser("20200001", "member@inha.edu", UserRole.MEMBER);
        associateUser = createAndSaveUser("20230001", "associate@inha.edu", UserRole.ASSOCIATE);
        operatorUser = createAndSaveUser("20200002", "operator@inha.edu", UserRole.OPERATOR);
    }

    private void setupBoardData() {
        // 게시판 생성
        noticesBoard = Board.create(BoardCode.NOTICES, "공지사항", "동아리 공지사항을 확인할 수 있습니다.", false, false, 1);
        generalBoard = Board.create(BoardCode.GENERAL, "자유게시판", "자유롭게 이야기를 나눌 수 있는 공간입니다.", true, true, 2);
        insightBoard = Board.create(BoardCode.INSIGHT, "정보공유", "유용한 정보를 공유하는 게시판입니다.", false, false, 3);

        boardRepository.save(noticesBoard);
        boardRepository.save(generalBoard);
        boardRepository.save(insightBoard);

        // 권한 설정 - notices
        boardPermissionRepository.save(BoardPermission.create(noticesBoard, UserRole.ASSOCIATE, true, false));
        boardPermissionRepository.save(BoardPermission.create(noticesBoard, UserRole.MEMBER, true, false));
        boardPermissionRepository.save(BoardPermission.create(noticesBoard, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(noticesBoard, UserRole.ADMIN, true, true));

        // 권한 설정 - general
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.ASSOCIATE, false, false));
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.MEMBER, true, true));
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(generalBoard, UserRole.ADMIN, true, true));

        // 권한 설정 - insight
        boardPermissionRepository.save(BoardPermission.create(insightBoard, UserRole.ASSOCIATE, false, false));
        boardPermissionRepository.save(BoardPermission.create(insightBoard, UserRole.MEMBER, true, true));
        boardPermissionRepository.save(BoardPermission.create(insightBoard, UserRole.OPERATOR, true, true));
        boardPermissionRepository.save(BoardPermission.create(insightBoard, UserRole.ADMIN, true, true));
    }

    private RequestPostProcessor withAuth(User user) {
        AuthenticatedUser authenticatedUser = new AuthenticatedUser(
                user.getId(),
                user.getStudentId(),
                user.getRole().name()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                authenticatedUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        return authentication(auth);
    }

    private Post createAndSavePost(Board board, User author, String title, String content) {
        Post post = Post.createPost(board, author, title, content);
        return postRepository.save(post);
    }

    private Post createAndSaveAnonymousPost(Board board, User author, String title, String content) {
        Post post = Post.createAnonymousPost(board, author, title, content);
        return postRepository.save(post);
    }

    private Post createAndSaveNotice(Board board, User author, String title, String content, boolean visibleToAssociate) {
        Post post = Post.createNotice(board, author, title, content, visibleToAssociate);
        return postRepository.save(post);
    }

    private Post createAndSaveQuestionPost(Board board, User author, String title, String content) {
        Post post = Post.createPost(board, author, title, content);
        post.setQuestion(true);
        return postRepository.save(post);
    }

    @Nested
    @DisplayName("게시글 목록 조회 성능 테스트")
    class PerformanceTest {

        @DisplayName("BRD-004: 게시판 목록 조회 성능 - 3초 이내 응답")
        @Test
        void getPostList_Performance_CompletesWithin3Seconds() {
            // given: 30개 이상의 게시글 생성
            for (int i = 0; i < 35; i++) {
                createAndSavePost(generalBoard, memberUser, "제목 " + i, "내용 " + i);
            }

            // when & then: 3초 이내에 응답
            assertTimeout(Duration.ofSeconds(3), () -> {
                mockMvc.perform(get(BASE_URL + "/general/posts")
                                .with(withAuth(memberUser))
                                .with(csrf()))
                        .andExpect(status().isOk());
            });
        }
    }

    @Nested
    @DisplayName("준회원 공개 설정 테스트 (공지사항)")
    class AssociateVisibilityTest {

        @DisplayName("BRD-020: 운영진이 준회원 공개 옵션으로 공지사항 작성")
        @Test
        void createPost_InNotices_WithVisibleToAssociate_Success() throws Exception {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "준회원 공개 공지",
                    "준회원도 볼 수 있는 공지입니다.",
                    false,
                    false,
                    true,  // isVisibleToAssociate - 준회원 공개
                    List.of()
            );

            // when & then
            mockMvc.perform(post(BASE_URL + "/notices/posts")
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.postId").isNumber());
        }

        @DisplayName("BRD-021: 준회원이 비공개 공지 미표시")
        @Test
        void getNoticeList_AsAssociate_HiddenNonVisibleNotices() throws Exception {
            // given: 준회원 공개/비공개 공지 각각 생성
            createAndSaveNotice(noticesBoard, operatorUser, "준회원 공개 공지", "내용1", true);
            createAndSaveNotice(noticesBoard, operatorUser, "준회원 비공개 공지", "내용2", false);

            // when & then: 준회원이 조회 시 공개된 공지만 표시
            mockMvc.perform(get(BASE_URL + "/notices/posts")
                            .with(withAuth(associateUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.posts[0].title").value("준회원 공개 공지"));
        }

        @DisplayName("BRD-022: 정회원이 모든 공지 조회")
        @Test
        void getNoticeList_AsMember_ShowsAllNotices() throws Exception {
            // given: 준회원 공개/비공개 공지 각각 생성
            createAndSaveNotice(noticesBoard, operatorUser, "준회원 공개 공지", "내용1", true);
            createAndSaveNotice(noticesBoard, operatorUser, "준회원 비공개 공지", "내용2", false);

            // when & then: 정회원이 조회 시 모든 공지 표시
            mockMvc.perform(get(BASE_URL + "/notices/posts")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }
    }

    @Nested
    @DisplayName("게시판 검색 기능 테스트")
    class SearchTest {

        @DisplayName("BRD-030: 제목 검색")
        @Test
        void searchPosts_ByTitle_ReturnsMatchingPosts() throws Exception {
            // given
            createAndSavePost(generalBoard, memberUser, "테스트 제목입니다", "다른 내용");
            createAndSavePost(generalBoard, memberUser, "다른 제목", "다른 내용");

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .param("keyword", "테스트")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.posts[0].title").value("테스트 제목입니다"));
        }

        @DisplayName("BRD-031: 내용 검색")
        @Test
        void searchPosts_ByContent_ReturnsMatchingPosts() throws Exception {
            // given
            createAndSavePost(generalBoard, memberUser, "제목1", "키워드가 포함된 내용");
            createAndSavePost(generalBoard, memberUser, "제목2", "다른 내용");

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .param("keyword", "키워드")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.posts[0].title").value("제목1"));
        }

        @DisplayName("BRD-032: 제목+내용 검색")
        @Test
        void searchPosts_ByTitleOrContent_ReturnsCombinedResults() throws Exception {
            // given
            createAndSavePost(generalBoard, memberUser, "검색어가 제목에", "다른 내용");
            createAndSavePost(generalBoard, memberUser, "다른 제목", "검색어가 내용에");
            createAndSavePost(generalBoard, memberUser, "무관한 제목", "무관한 내용");

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .param("keyword", "검색어")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @DisplayName("BRD-033: 검색 결과 없음")
        @Test
        void searchPosts_NoMatches_ReturnsEmptyList() throws Exception {
            // given
            createAndSavePost(generalBoard, memberUser, "제목", "내용");

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .param("keyword", "존재하지않는키워드")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.posts").isEmpty());
        }
    }

    @Nested
    @DisplayName("페이지네이션 테스트")
    class PaginationTest {

        @DisplayName("BRD-040: 기본 페이지 크기 20개")
        @Test
        void getPostList_DefaultPageSize_Returns20Posts() throws Exception {
            // given: 30개 게시글 생성
            for (int i = 0; i < 30; i++) {
                createAndSavePost(generalBoard, memberUser, "제목 " + i, "내용 " + i);
            }

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts.length()").value(20))
                    .andExpect(jsonPath("$.totalElements").value(30))
                    .andExpect(jsonPath("$.hasNext").value(true));
        }

        @DisplayName("BRD-041: 페이지 이동 - 2페이지")
        @Test
        void getPostList_Page2_Returns21To40Posts() throws Exception {
            // given: 40개 게시글 생성
            for (int i = 0; i < 40; i++) {
                createAndSavePost(generalBoard, memberUser, "제목 " + i, "내용 " + i);
            }

            // when & then: page=1 (0-indexed)
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .param("page", "1")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts.length()").value(20))
                    .andExpect(jsonPath("$.currentPage").value(1));
        }

        @DisplayName("BRD-042: 최신순 정렬")
        @Test
        void getPostList_DefaultOrder_SortedByCreatedAtDesc() throws Exception {
            // given: 순차적으로 게시글 생성
            createAndSavePost(generalBoard, memberUser, "첫번째 게시글", "내용1");
            createAndSavePost(generalBoard, memberUser, "두번째 게시글", "내용2");
            createAndSavePost(generalBoard, memberUser, "세번째 게시글", "내용3");

            // when & then: 최신순으로 정렬되어야 함
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts[0].title").value("세번째 게시글"))
                    .andExpect(jsonPath("$.posts[1].title").value("두번째 게시글"))
                    .andExpect(jsonPath("$.posts[2].title").value("첫번째 게시글"));
        }
    }

    @Nested
    @DisplayName("질문 태그 필터링 테스트")
    class QuestionFilterTest {

        @DisplayName("BRD-062: 질문만 필터링")
        @Test
        void getPostList_WithQuestionOnlyFilter_ReturnsOnlyQuestions() throws Exception {
            // given: 일반 게시글 + 질문 게시글 생성
            createAndSavePost(generalBoard, memberUser, "일반 게시글", "내용1");
            createAndSaveQuestionPost(generalBoard, memberUser, "질문 게시글 1", "질문 내용1");
            createAndSaveQuestionPost(generalBoard, memberUser, "질문 게시글 2", "질문 내용2");

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .param("questionOnly", "true")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }
    }

    @Nested
    @DisplayName("게시글 CRUD 테스트")
    class CrudTest {

        @DisplayName("게시글 작성 성공")
        @Test
        void createPost_WithValidRequest_ReturnsCreated() throws Exception {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "테스트 제목",
                    "테스트 내용",
                    false,
                    false,
                    false, // isVisibleToAssociate
                    List.of()
            );

            // when & then
            mockMvc.perform(post(BASE_URL + "/general/posts")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.postId").isNumber())
                    .andExpect(jsonPath("$.title").value("테스트 제목"));
        }

        @DisplayName("게시글 상세 조회 성공")
        @Test
        void getPostDetail_WithValidId_ReturnsOk() throws Exception {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "테스트 제목", "테스트 내용");

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.postId").value(post.getId()))
                    .andExpect(jsonPath("$.title").value("테스트 제목"))
                    .andExpect(jsonPath("$.content").value("테스트 내용"));
        }

        @DisplayName("존재하지 않는 게시글 조회 시 404")
        @Test
        void getPostDetail_WithInvalidId_ReturnsNotFound() throws Exception {
            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts/99999")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }
    }

    // ============================================================
    // PST 테스트 케이스 (post-test-cases.md 기준)
    // ============================================================

    @Nested
    @DisplayName("PST: 게시글 작성 Validation 테스트")
    class PstCreateValidationTest {

        @DisplayName("PST-004: 제목 101자 시 400 Bad Request")
        @Test
        void createPost_TitleTooLong_Returns400() throws Exception {
            // given
            String longTitle = "가".repeat(101);
            CreatePostRequest request = new CreatePostRequest(
                    longTitle,
                    "내용",
                    false,
                    false,
                    false,
                    List.of()
            );

            // when & then
            mockMvc.perform(post(BASE_URL + "/general/posts")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("PST-007: 제목 빈 값 시 400 Bad Request")
        @Test
        void createPost_EmptyTitle_Returns400() throws Exception {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "",
                    "내용",
                    false,
                    false,
                    false,
                    List.of()
            );

            // when & then
            mockMvc.perform(post(BASE_URL + "/general/posts")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("PST-008: 내용 빈 값 시 400 Bad Request")
        @Test
        void createPost_EmptyContent_Returns400() throws Exception {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "제목",
                    "",
                    false,
                    false,
                    false,
                    List.of()
            );

            // when & then
            mockMvc.perform(post(BASE_URL + "/general/posts")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        @DisplayName("PST-012: 이미지 6개 첨부 시 400 Bad Request")
        @Test
        void createPost_TooManyImages_Returns400() throws Exception {
            // given
            List<String> imageUrls = List.of(
                    "https://example.com/1.jpg",
                    "https://example.com/2.jpg",
                    "https://example.com/3.jpg",
                    "https://example.com/4.jpg",
                    "https://example.com/5.jpg",
                    "https://example.com/6.jpg"
            );
            CreatePostRequest request = new CreatePostRequest(
                    "제목",
                    "내용",
                    false,
                    false,
                    false,
                    imageUrls
            );

            // when & then
            mockMvc.perform(post(BASE_URL + "/general/posts")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PST: 게시글 조회 테스트")
    class PstReadPostTest {

        @DisplayName("PST-020: 게시글 상세 조회")
        @Test
        void getPostDetail_Success() throws Exception {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "상세 조회 테스트", "상세 조회 내용");

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.postId").value(post.getId()))
                    .andExpect(jsonPath("$.title").value("상세 조회 테스트"))
                    .andExpect(jsonPath("$.content").value("상세 조회 내용"))
                    .andExpect(jsonPath("$.authorName").isNotEmpty())
                    .andExpect(jsonPath("$.viewCount").isNumber());
        }

        @DisplayName("PST-021: 조회수 자동 증가")
        @Test
        void getPostDetail_IncreasesViewCount() throws Exception {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "조회수 테스트", "조회수 내용");
            int initialViewCount = post.getViewCount();

            // when: 조회
            mockMvc.perform(get(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.viewCount").value(initialViewCount + 1));
        }

        @DisplayName("PST-022: 익명 게시글 작성자 비노출")
        @Test
        void getPostDetail_Anonymous_HidesAuthorName() throws Exception {
            // given
            Post anonymousPost = createAndSaveAnonymousPost(generalBoard, memberUser, "익명 게시글", "익명 내용");

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts/" + anonymousPost.getId())
                            .with(withAuth(operatorUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authorName").value("익명"))
                    .andExpect(jsonPath("$.authorId").isEmpty())
                    .andExpect(jsonPath("$.isAnonymous").value(true));
        }

        @DisplayName("PST-023: 삭제된 게시글 접근")
        @Test
        void getPostDetail_DeletedPost_Returns404() throws Exception {
            // given
            Post post = createAndSavePost(generalBoard, memberUser, "삭제될 게시글", "내용");
            post.delete(memberUser.getId());
            postRepository.save(post);

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound());
        }

        @DisplayName("PST-024: 게시글 목록 최신순 정렬")
        @Test
        void getPostList_SortedByCreatedAtDesc() throws Exception {
            // given
            createAndSavePost(generalBoard, memberUser, "첫번째", "내용1");
            createAndSavePost(generalBoard, memberUser, "두번째", "내용2");
            createAndSavePost(generalBoard, memberUser, "세번째", "내용3");

            // when & then: 최신순 정렬
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts[0].title").value("세번째"))
                    .andExpect(jsonPath("$.posts[1].title").value("두번째"))
                    .andExpect(jsonPath("$.posts[2].title").value("첫번째"));
        }

        @DisplayName("PST-025: 게시글 목록 기본 20개")
        @Test
        void getPostList_DefaultPageSize20() throws Exception {
            // given: 25개 게시글 생성
            for (int i = 0; i < 25; i++) {
                createAndSavePost(generalBoard, memberUser, "게시글 " + i, "내용 " + i);
            }

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts.length()").value(20))
                    .andExpect(jsonPath("$.totalElements").value(25))
                    .andExpect(jsonPath("$.hasNext").value(true));
        }

        @DisplayName("PST-026: 게시글 상세 이미지 로드")
        @Test
        void getPostDetail_WithImages() throws Exception {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "이미지 포함 게시글",
                    "이미지가 포함된 게시글입니다.",
                    false,
                    false,
                    false,
                    List.of("https://example.com/img1.jpg", "https://example.com/img2.jpg")
            );

            // 게시글 생성
            String response = mockMvc.perform(post(BASE_URL + "/general/posts")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            Long postId = objectMapper.readTree(response).get("postId").asLong();

            // when & then: 이미지 포함 확인
            mockMvc.perform(get(BASE_URL + "/general/posts/" + postId)
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.imageUrls").isArray())
                    .andExpect(jsonPath("$.imageUrls.length()").value(2));
        }
    }

    @Nested
    @DisplayName("PST: 게시글 수정 권한 테스트")
    class PstUpdatePermissionTest {

        @DisplayName("PST-034: 타인 게시글 수정 API 접근 거부")
        @Test
        void updatePost_ByOther_Returns403() throws Exception {
            // given: memberUser가 작성한 게시글
            Post post = createAndSavePost(generalBoard, memberUser, "타인 게시글", "내용");

            // 다른 사용자 생성
            User otherUser = createAndSaveUser("20200099", "other@inha.edu", UserRole.MEMBER);

            // when & then: otherUser가 수정 시도
            mockMvc.perform(put(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(otherUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdatePostRequest("수정 시도", "수정 내용", false, List.of())
                            )))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PST: 게시글 삭제 권한 테스트")
    class PstDeletePermissionTest {

        @DisplayName("PST-043: 관리자 타인 게시글 삭제")
        @Test
        void deletePost_ByOperator_Success() throws Exception {
            // given: memberUser가 작성한 게시글
            Post post = createAndSavePost(generalBoard, memberUser, "삭제될 게시글", "내용");

            // when & then: operatorUser가 삭제 (OPERATOR는 타인 게시글 삭제 가능)
            mockMvc.perform(delete(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(operatorUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNoContent());
        }

        @DisplayName("PST-045: 타인 게시글 삭제 API 접근 거부")
        @Test
        void deletePost_ByOther_Returns403() throws Exception {
            // given: operatorUser가 작성한 게시글
            Post post = createAndSavePost(generalBoard, operatorUser, "타인 게시글", "내용");

            // when & then: memberUser가 삭제 시도 (MEMBER는 타인 게시글 삭제 불가)
            mockMvc.perform(delete(BASE_URL + "/general/posts/" + post.getId())
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PST: 공지사항 권한 테스트")
    class PstNoticePermissionTest {

        @DisplayName("PST-051: 공지사항 준회원 공개 설정")
        @Test
        void createNotice_VisibleToAssociate_Success() throws Exception {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "준회원 공개 공지",
                    "준회원도 볼 수 있는 공지입니다.",
                    false,
                    false,
                    true,  // isVisibleToAssociate
                    List.of()
            );

            // when & then: operatorUser가 공지 생성
            mockMvc.perform(post(BASE_URL + "/notices/posts")
                            .with(withAuth(operatorUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isCreated());

            // 준회원이 조회 가능한지 확인
            mockMvc.perform(get(BASE_URL + "/notices/posts")
                            .with(withAuth(associateUser))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @DisplayName("PST-053: 정회원 공지사항 작성 불가")
        @Test
        void createNotice_ByMember_Returns403() throws Exception {
            // given
            CreatePostRequest request = new CreatePostRequest(
                    "정회원 공지 시도",
                    "정회원이 공지사항 작성 시도",
                    false,
                    false,
                    false,
                    List.of()
            );

            // when & then: memberUser가 공지 작성 시도
            mockMvc.perform(post(BASE_URL + "/notices/posts")
                            .with(withAuth(memberUser))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("PST: 검색 및 필터 테스트")
    class PstSearchFilterTest {

        @DisplayName("PST-070: 제목+내용 검색")
        @Test
        void searchPosts_ByTitleAndContent() throws Exception {
            // given
            createAndSavePost(generalBoard, memberUser, "검색어제목", "다른내용");
            createAndSavePost(generalBoard, memberUser, "다른제목", "검색어내용");
            createAndSavePost(generalBoard, memberUser, "무관", "무관");

            // when & then: 제목 또는 내용에 "검색어" 포함
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .param("keyword", "검색어")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @DisplayName("PST-071: 질문만 필터")
        @Test
        void filterPosts_QuestionsOnly() throws Exception {
            // given
            createAndSavePost(generalBoard, memberUser, "일반 게시글", "내용");
            createAndSaveQuestionPost(generalBoard, memberUser, "질문 게시글 1", "질문 내용");
            createAndSaveQuestionPost(generalBoard, memberUser, "질문 게시글 2", "질문 내용");

            // when & then
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .param("questionOnly", "true")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @DisplayName("PST-072: 검색 결과 정렬")
        @Test
        void searchPosts_SortedByCreatedAtDesc() throws Exception {
            // given
            createAndSavePost(generalBoard, memberUser, "검색 첫번째", "내용");
            createAndSavePost(generalBoard, memberUser, "검색 두번째", "내용");
            createAndSavePost(generalBoard, memberUser, "검색 세번째", "내용");

            // when & then: 검색 결과도 최신순 정렬
            mockMvc.perform(get(BASE_URL + "/general/posts")
                            .param("keyword", "검색")
                            .with(withAuth(memberUser))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.posts[0].title").value("검색 세번째"))
                    .andExpect(jsonPath("$.posts[1].title").value("검색 두번째"))
                    .andExpect(jsonPath("$.posts[2].title").value("검색 첫번째"));
        }
    }
}
