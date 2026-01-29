package igrus.web.community.fixture;

import igrus.web.community.board.domain.Board;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.dto.request.CreatePostRequest;
import igrus.web.community.post.dto.request.UpdatePostRequest;
import igrus.web.user.domain.User;

import igrus.web.common.fixture.TestConstants;

import java.util.List;

import static igrus.web.common.fixture.TestConstants.*;
import static igrus.web.common.fixture.TestEntityIdAssigner.withId;

/**
 * Post 도메인 관련 테스트 픽스처 클래스.
 *
 * <p>테스트에서 사용되는 Post 엔티티와 관련 DTO를 생성하는 팩토리 메서드를 제공합니다.
 */
public final class PostTestFixture {

    private PostTestFixture() {
        // 유틸리티 클래스 인스턴스화 방지
    }

    // ==================== Post 생성 (ID 없음) ====================

    /**
     * 일반 게시글을 생성합니다.
     *
     * @param board  게시판
     * @param author 작성자
     * @return 일반 게시글
     */
    public static Post createNormalPost(Board board, User author) {
        return Post.createPost(board, author, DEFAULT_POST_TITLE, DEFAULT_POST_CONTENT);
    }

    /**
     * 지정된 제목과 내용으로 일반 게시글을 생성합니다.
     *
     * @param board   게시판
     * @param author  작성자
     * @param title   제목
     * @param content 내용
     * @return 일반 게시글
     */
    public static Post createNormalPost(Board board, User author, String title, String content) {
        return Post.createPost(board, author, title, content);
    }

    /**
     * 익명 게시글을 생성합니다.
     *
     * <p>자유게시판에서만 사용 가능합니다.
     *
     * @param board  게시판 (자유게시판이어야 함)
     * @param author 작성자
     * @return 익명 게시글
     */
    public static Post createAnonymousPost(Board board, User author) {
        return Post.createAnonymousPost(board, author, DEFAULT_POST_TITLE, DEFAULT_POST_CONTENT);
    }

    /**
     * 지정된 제목과 내용으로 익명 게시글을 생성합니다.
     *
     * @param board   게시판 (자유게시판이어야 함)
     * @param author  작성자
     * @param title   제목
     * @param content 내용
     * @return 익명 게시글
     */
    public static Post createAnonymousPost(Board board, User author, String title, String content) {
        return Post.createAnonymousPost(board, author, title, content);
    }

    /**
     * 질문 태그가 설정된 게시글을 생성합니다.
     *
     * <p>자유게시판에서만 사용 가능합니다.
     *
     * @param board  게시판 (자유게시판이어야 함)
     * @param author 작성자
     * @return 질문 게시글
     */
    public static Post createQuestionPost(Board board, User author) {
        Post post = Post.createPost(board, author, DEFAULT_POST_TITLE, DEFAULT_POST_CONTENT);
        post.setQuestion(true);
        return post;
    }

    /**
     * 공지사항을 생성합니다.
     *
     * <p>공지사항 게시판에서만 사용 가능합니다.
     *
     * @param board  게시판 (공지사항이어야 함)
     * @param author 작성자
     * @return 공지사항
     */
    public static Post createNotice(Board board, User author) {
        return Post.createNotice(board, author, DEFAULT_POST_TITLE, DEFAULT_POST_CONTENT, false);
    }

    /**
     * 준회원 공개 공지사항을 생성합니다.
     *
     * @param board  게시판 (공지사항이어야 함)
     * @param author 작성자
     * @return 준회원 공개 공지사항
     */
    public static Post createVisibleToAssociateNotice(Board board, User author) {
        return Post.createNotice(board, author, DEFAULT_POST_TITLE, DEFAULT_POST_CONTENT, true);
    }

    // ==================== Post 생성 (ID 포함) ====================

    /**
     * ID가 설정된 일반 게시글을 생성합니다.
     *
     * @param board  게시판
     * @param author 작성자
     * @return ID가 설정된 일반 게시글
     */
    public static Post normalPost(Board board, User author) {
        return withId(createNormalPost(board, author), DEFAULT_POST_ID);
    }

    /**
     * 지정된 ID가 설정된 일반 게시글을 생성합니다.
     *
     * @param board  게시판
     * @param author 작성자
     * @param id     설정할 ID
     * @return ID가 설정된 일반 게시글
     */
    public static Post normalPost(Board board, User author, Long id) {
        return withId(createNormalPost(board, author), id);
    }

    /**
     * ID가 설정된 익명 게시글을 생성합니다.
     *
     * @param board  게시판 (자유게시판이어야 함)
     * @param author 작성자
     * @return ID가 설정된 익명 게시글
     */
    public static Post anonymousPost(Board board, User author) {
        return withId(createAnonymousPost(board, author), DEFAULT_POST_ID);
    }

    /**
     * 지정된 ID가 설정된 익명 게시글을 생성합니다.
     *
     * @param board  게시판 (자유게시판이어야 함)
     * @param author 작성자
     * @param id     설정할 ID
     * @return ID가 설정된 익명 게시글
     */
    public static Post anonymousPost(Board board, User author, Long id) {
        return withId(createAnonymousPost(board, author), id);
    }

    /**
     * ID가 설정된 질문 게시글을 생성합니다.
     *
     * @param board  게시판 (자유게시판이어야 함)
     * @param author 작성자
     * @return ID가 설정된 질문 게시글
     */
    public static Post questionPost(Board board, User author) {
        return withId(createQuestionPost(board, author), DEFAULT_POST_ID);
    }

    /**
     * ID가 설정된 공지사항을 생성합니다.
     *
     * @param board  게시판 (공지사항이어야 함)
     * @param author 작성자
     * @return ID가 설정된 공지사항
     */
    public static Post notice(Board board, User author) {
        return withId(createNotice(board, author), DEFAULT_POST_ID);
    }

    // ==================== CreatePostRequest 생성 ====================

    /**
     * 기본 게시글 생성 요청을 생성합니다.
     *
     * @return 기본 게시글 생성 요청
     */
    public static CreatePostRequest createRequest() {
        return new CreatePostRequest(
                DEFAULT_POST_TITLE,
                DEFAULT_POST_CONTENT,
                false, // isAnonymous
                false, // isQuestion
                false, // isVisibleToAssociate
                List.of()
        );
    }

    /**
     * 지정된 제목과 내용으로 게시글 생성 요청을 생성합니다.
     *
     * @param title   제목
     * @param content 내용
     * @return 게시글 생성 요청
     */
    public static CreatePostRequest createRequest(String title, String content) {
        return new CreatePostRequest(
                title,
                content,
                false,
                false,
                false,
                List.of()
        );
    }

    /**
     * 익명 게시글 생성 요청을 생성합니다.
     *
     * @return 익명 게시글 생성 요청
     */
    public static CreatePostRequest anonymousCreateRequest() {
        return new CreatePostRequest(
                DEFAULT_POST_TITLE,
                DEFAULT_POST_CONTENT,
                true,  // isAnonymous
                false, // isQuestion
                false, // isVisibleToAssociate
                List.of()
        );
    }

    /**
     * 질문 태그 게시글 생성 요청을 생성합니다.
     *
     * @return 질문 태그 게시글 생성 요청
     */
    public static CreatePostRequest questionCreateRequest() {
        return new CreatePostRequest(
                DEFAULT_POST_TITLE,
                DEFAULT_POST_CONTENT,
                false, // isAnonymous
                true,  // isQuestion
                false, // isVisibleToAssociate
                List.of()
        );
    }

    /**
     * 익명 + 질문 태그 게시글 생성 요청을 생성합니다.
     *
     * @return 익명 질문 게시글 생성 요청
     */
    public static CreatePostRequest anonymousQuestionCreateRequest() {
        return new CreatePostRequest(
                DEFAULT_POST_TITLE,
                DEFAULT_POST_CONTENT,
                true, // isAnonymous
                true, // isQuestion
                false,
                List.of()
        );
    }

    /**
     * 이미지가 포함된 게시글 생성 요청을 생성합니다.
     *
     * @param imageCount 이미지 개수
     * @return 이미지 포함 게시글 생성 요청
     */
    public static CreatePostRequest createRequestWithImages(int imageCount) {
        List<String> imageUrls = java.util.stream.IntStream.range(0, imageCount)
                .mapToObj(TestConstants::testImageUrl)
                .toList();

        return new CreatePostRequest(
                DEFAULT_POST_TITLE,
                DEFAULT_POST_CONTENT,
                false,
                false,
                false,
                imageUrls
        );
    }

    /**
     * 준회원 공개 공지사항 생성 요청을 생성합니다.
     *
     * @return 준회원 공개 공지사항 생성 요청
     */
    public static CreatePostRequest visibleToAssociateCreateRequest() {
        return new CreatePostRequest(
                DEFAULT_POST_TITLE,
                DEFAULT_POST_CONTENT,
                false,
                false,
                true, // isVisibleToAssociate
                List.of()
        );
    }

    // ==================== UpdatePostRequest 생성 ====================

    /**
     * 기본 게시글 수정 요청을 생성합니다.
     *
     * @return 기본 게시글 수정 요청
     */
    public static UpdatePostRequest updateRequest() {
        return new UpdatePostRequest(
                "수정된 제목",
                "수정된 내용입니다.",
                false, // isQuestion
                List.of()
        );
    }

    /**
     * 지정된 제목과 내용으로 게시글 수정 요청을 생성합니다.
     *
     * @param title   수정할 제목
     * @param content 수정할 내용
     * @return 게시글 수정 요청
     */
    public static UpdatePostRequest updateRequest(String title, String content) {
        return new UpdatePostRequest(
                title,
                content,
                false,
                List.of()
        );
    }

    /**
     * 질문 태그가 포함된 게시글 수정 요청을 생성합니다.
     *
     * @param title   수정할 제목
     * @param content 수정할 내용
     * @return 질문 태그 포함 게시글 수정 요청
     */
    public static UpdatePostRequest questionUpdateRequest(String title, String content) {
        return new UpdatePostRequest(
                title,
                content,
                true, // isQuestion
                List.of()
        );
    }

    // ==================== 유틸리티 메서드 ====================

    /**
     * 지정된 길이의 제목을 생성합니다.
     *
     * <p>제목 길이 제한 테스트 시 사용합니다.
     *
     * @param length 제목 길이
     * @return 지정된 길이의 제목
     */
    public static String titleWithLength(int length) {
        return "가".repeat(length);
    }
}
