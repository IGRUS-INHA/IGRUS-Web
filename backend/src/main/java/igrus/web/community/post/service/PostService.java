package igrus.web.community.post.service;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.bookmark.repository.BookmarkRepository;
import igrus.web.community.like.postlike.repository.PostLikeRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.domain.PostImage;
import igrus.web.community.post.dto.request.CreatePostRequest;
import igrus.web.community.post.dto.request.UpdatePostRequest;
import igrus.web.community.board.exception.BoardWriteDeniedException;
import igrus.web.community.board.service.BoardService;
import igrus.web.community.board.service.BoardPermissionService;
import igrus.web.community.post.dto.response.PostCreateResponse;
import igrus.web.community.post.dto.response.PostDetailResponse;
import igrus.web.community.post.dto.response.PostListPageResponse;
import igrus.web.community.post.dto.response.PostUpdateResponse;
import igrus.web.community.post.dto.response.PostViewHistoryResponse;
import igrus.web.community.post.dto.response.PostViewStatsResponse;
import igrus.web.community.post.exception.InvalidPostOptionException;
import igrus.web.community.post.exception.PostAccessDeniedException;
import igrus.web.community.post.exception.PostAnonymousUnchangeableException;
import igrus.web.community.post.exception.PostDeletedException;
import igrus.web.community.post.exception.PostImageLimitExceededException;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final BoardService boardService;
    private final BoardPermissionService boardPermissionService;
    private final PostRateLimitService postRateLimitService;
    private final PostViewService postViewService;
    private final PostLikeRepository postLikeRepository;
    private final BookmarkRepository bookmarkRepository;

    /**
     * 게시글 작성
     *
     * @param boardCode 게시판 코드
     * @param request 게시글 작성 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 생성된 게시글 응답
     */
    @Transactional
    public PostCreateResponse createPost(String boardCode, CreatePostRequest request, AuthenticatedUser authenticatedUser) {
        // 사용자 조회
        User author = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(UserNotFoundException::new);

        // 1. 게시판 조회 및 쓰기 권한 확인
        Board board = boardService.getBoardEntity(boardCode);
        boardPermissionService.checkWritePermission(board, author.getRole());

        // 2. 공지사항 게시판 특수 처리
        if (board.getCode() == BoardCode.NOTICES) {
            return createNoticeFromPostRequest(board, author, request);
        }

        // 3. Rate Limit 확인 (공지사항은 제외)
        postRateLimitService.checkRateLimit(author);

        // 4. 게시글 생성 (익명 여부에 따라 분기)
        Post post;
        if (request.isAnonymous()) {
            validateAnonymousOption(board);
            post = Post.createAnonymousPost(board, author, request.title(), request.content());
        } else {
            post = Post.createPost(board, author, request.title(), request.content());
        }

        // 5. 질문 옵션 설정
        if (request.isQuestion()) {
            validateQuestionOption(board);
            post.setQuestion(true);
        }

        // 6. 이미지 추가
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            validateImageCount(request.imageUrls().size());
            for (int i = 0; i < request.imageUrls().size(); i++) {
                PostImage image = PostImage.create(post, request.imageUrls().get(i), i);
                post.addImage(image);
            }
        }

        // 7. 저장
        Post savedPost = postRepository.save(post);

        return PostCreateResponse.from(savedPost);
    }

    /**
     * 일반 게시글 요청으로 공지사항을 생성합니다.
     * OPERATOR 이상만 작성 가능하며, 익명/질문 옵션 사용 시 예외가 발생합니다.
     */
    private PostCreateResponse createNoticeFromPostRequest(Board board, User author, CreatePostRequest request) {
        // OPERATOR 이상 권한 확인
        if (!author.isOperatorOrAbove()) {
            throw new BoardWriteDeniedException();
        }

        // 익명/질문 옵션 사용 불가
        if (request.isAnonymous()) {
            throw new InvalidPostOptionException("anonymous", BoardCode.NOTICES.name());
        }
        if (request.isQuestion()) {
            throw new InvalidPostOptionException("question", BoardCode.NOTICES.name());
        }

        // 공지사항 생성 (요청의 isVisibleToAssociate 값 사용)
        Post post = Post.createNotice(board, author, request.title(), request.content(), request.isVisibleToAssociate());

        // 이미지 추가
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            validateImageCount(request.imageUrls().size());
            for (int i = 0; i < request.imageUrls().size(); i++) {
                PostImage image = PostImage.create(post, request.imageUrls().get(i), i);
                post.addImage(image);
            }
        }

        Post savedPost = postRepository.save(post);
        return PostCreateResponse.from(savedPost);
    }

    private void validateAnonymousOption(Board board) {
        if (board.getCode() != BoardCode.GENERAL) {
            throw new InvalidPostOptionException("anonymous", board.getCode().name());
        }
    }

    private void validateQuestionOption(Board board) {
        if (board.getCode() != BoardCode.GENERAL) {
            throw new InvalidPostOptionException("question", board.getCode().name());
        }
    }

    private void validateImageCount(int count) {
        if (count > 5) {
            throw new PostImageLimitExceededException(5, count);
        }
    }

    /**
     * 게시글 수정
     *
     * @param boardCode 게시판 코드
     * @param postId 게시글 ID
     * @param request 게시글 수정 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 수정된 게시글 응답
     * @throws PostNotFoundException 게시글을 찾을 수 없는 경우
     * @throws PostDeletedException 삭제된 게시글인 경우
     * @throws PostAccessDeniedException 수정 권한이 없는 경우
     * @throws PostAnonymousUnchangeableException 익명 설정 변경 시도 시
     */
    @Transactional
    public PostUpdateResponse updatePost(String boardCode, Long postId, UpdatePostRequest request, AuthenticatedUser authenticatedUser) {
        // 사용자 조회
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(UserNotFoundException::new);

        // 게시판 조회
        Board board = boardService.getBoardEntity(boardCode);

        // 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        // 삭제된 게시글인지 확인
        if (post.isDeleted()) {
            throw new PostDeletedException(postId);
        }

        // 게시글이 해당 게시판에 속하는지 확인
        if (!post.getBoard().getId().equals(board.getId())) {
            throw new PostNotFoundException(postId);
        }

        // 수정 권한 확인 (작성자 본인 또는 ADMIN)
        if (!post.canModify(user)) {
            throw new PostAccessDeniedException("게시글 수정 권한이 없습니다");
        }

        // 질문 옵션 변경 검증 (자유게시판에서만 가능)
        if (request.isQuestion() && !post.isQuestion()) {
            validateQuestionOption(board);
        }

        // 제목과 내용 수정
        post.updateContent(request.title(), request.content());

        // 질문 옵션 변경
        if (board.getCode() == BoardCode.GENERAL) {
            post.setQuestion(request.isQuestion());
        }

        // 이미지 수정 (기존 이미지 삭제 후 새 이미지 추가)
        post.clearImages();
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            validateImageCount(request.imageUrls().size());
            for (int i = 0; i < request.imageUrls().size(); i++) {
                PostImage image = PostImage.create(post, request.imageUrls().get(i), i);
                post.addImage(image);
            }
        }

        return PostUpdateResponse.from(post);
    }

    /**
     * 게시글 삭제 (Soft Delete)
     *
     * @param boardCode 게시판 코드
     * @param postId 게시글 ID
     * @param authenticatedUser 인증된 사용자 정보
     * @throws PostNotFoundException 게시글을 찾을 수 없는 경우
     * @throws PostDeletedException 이미 삭제된 게시글인 경우
     * @throws PostAccessDeniedException 삭제 권한이 없는 경우
     */
    @Transactional
    public void deletePost(String boardCode, Long postId, AuthenticatedUser authenticatedUser) {
        // 사용자 조회
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(UserNotFoundException::new);

        // 게시판 조회
        Board board = boardService.getBoardEntity(boardCode);

        // 게시글 조회
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        // 이미 삭제된 게시글인지 확인
        if (post.isDeleted()) {
            throw new PostDeletedException(postId);
        }

        // 게시글이 해당 게시판에 속하는지 확인
        if (!post.getBoard().getId().equals(board.getId())) {
            throw new PostNotFoundException(postId);
        }

        // 삭제 권한 확인 (작성자 본인 또는 OPERATOR 이상)
        if (!post.canDelete(user)) {
            throw new PostAccessDeniedException("게시글 삭제 권한이 없습니다");
        }

        // Soft Delete 적용
        post.delete(user.getId());
    }

    /**
     * 게시글 목록 조회
     *
     * @param boardCode 게시판 코드
     * @param user 인증된 사용자 정보
     * @param keyword 검색 키워드 (null 가능)
     * @param questionOnly 질문 게시글만 조회 여부 (null 가능)
     * @param pageable 페이징 정보
     * @return 게시글 목록 페이지 응답
     */
    public PostListPageResponse getPostList(String boardCode, AuthenticatedUser user, String keyword, Boolean questionOnly, Pageable pageable) {
        // 사용자 조회
        User currentUser = userRepository.findById(user.userId())
                .orElseThrow(UserNotFoundException::new);

        // 게시판 조회 및 읽기 권한 확인
        Board board = boardService.getBoardEntity(boardCode);
        boardPermissionService.checkReadPermission(board, currentUser.getRole());

        // 준회원인 경우 공지사항은 공개된 글만 조회
        boolean isAssociate = currentUser.getRole() == UserRole.ASSOCIATE;
        boolean isNoticeBoard = board.getCode() == BoardCode.NOTICES;

        Page<Post> postPage;

        if (isAssociate && isNoticeBoard) {
            // 준회원이 공지사항 조회 시 준회원 공개 게시글만 조회
            postPage = getPostsForAssociateInNotices(board, keyword, pageable);
        } else if (Boolean.TRUE.equals(questionOnly)) {
            // 질문 게시글만 조회 (자유게시판에서만 의미 있음)
            postPage = getQuestionPosts(board, keyword, pageable);
        } else {
            // 일반 조회
            postPage = getRegularPosts(board, keyword, pageable);
        }

        return PostListPageResponse.from(postPage);
    }

    /**
     * 게시글 상세 조회
     *
     * @param boardCode 게시판 코드
     * @param postId 게시글 ID
     * @param user 인증된 사용자 정보
     * @return 게시글 상세 응답
     * @throws PostNotFoundException 게시글을 찾을 수 없는 경우
     * @throws PostDeletedException 삭제된 게시글인 경우
     */
    @Transactional
    public PostDetailResponse getPostDetail(String boardCode, Long postId, AuthenticatedUser user) {
        // 사용자 조회
        User currentUser = userRepository.findById(user.userId())
                .orElseThrow(UserNotFoundException::new);

        // 게시판 조회 및 읽기 권한 확인
        Board board = boardService.getBoardEntity(boardCode);
        boardPermissionService.checkReadPermission(board, currentUser.getRole());

        // 게시글 조회
        Post post = postRepository.findByBoardAndIdAndDeletedFalse(board, postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        // 준회원인 경우 공지사항의 비공개 글 접근 제한
        boolean isAssociate = currentUser.getRole() == UserRole.ASSOCIATE;
        boolean isNoticeBoard = board.getCode() == BoardCode.NOTICES;

        if (isAssociate && isNoticeBoard && !post.isVisibleToAssociate()) {
            throw new PostNotFoundException(postId);
        }

        // 조회 기록 저장 (비동기 - 항상 성공)
        postViewService.recordViewAsync(post.getId(), currentUser.getId());

        // 조회수 증가 (재시도 2회)
        incrementViewCountWithRetry(post, 2);

        // 현재 사용자가 작성자인지 확인
        boolean isCurrentUserAuthor = post.getAuthor().getId().equals(currentUser.getId());

        // 좋아요/북마크 상태 조회
        boolean liked = postLikeRepository.existsByPostIdAndUserId(postId, currentUser.getId());
        boolean bookmarked = bookmarkRepository.existsByPostIdAndUserId(postId, currentUser.getId());

        return PostDetailResponse.from(post, isCurrentUserAuthor, liked, bookmarked);
    }

    /**
     * 조회수를 증가시킵니다. 낙관적 락 충돌 시 재시도합니다.
     *
     * @param post       게시글
     * @param maxRetries 최대 재시도 횟수
     */
    private void incrementViewCountWithRetry(Post post, int maxRetries) {
        Long postId = post.getId();
        Post currentPost = post;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                currentPost.incrementViewCount();
                postRepository.saveAndFlush(currentPost);
                return;
            } catch (OptimisticLockException e) {
                if (attempt == maxRetries) {
                    log.warn("조회수 증가 실패 ({}회 재시도 후): postId={}", maxRetries, postId);
                    return;
                }
                // 엔티티 새로고침 후 재시도
                currentPost = postRepository.findById(postId)
                        .orElseThrow(() -> new PostNotFoundException(postId));
            }
        }
    }

    // === Private Helper Methods for Post List Query ===

    private Page<Post> getPostsForAssociateInNotices(Board board, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return postRepository.searchVisibleToAssociateByTitleOrContent(board, keyword, pageable);
        }
        return postRepository.findVisibleToAssociateByBoard(board, pageable);
    }

    private Page<Post> getQuestionPosts(Board board, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return postRepository.searchQuestionsByTitleOrContent(board, keyword, pageable);
        }
        return postRepository.findQuestionsByBoard(board, pageable);
    }

    private Page<Post> getRegularPosts(Board board, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.isBlank()) {
            return postRepository.searchByTitleOrContent(board, keyword, pageable);
        }
        return postRepository.findByBoardAndDeletedFalseOrderByCreatedAtDesc(board, pageable);
    }

    // === View Statistics Methods ===

    /**
     * 게시글 조회 통계를 조회합니다.
     * OPERATOR 이상만 조회 가능합니다.
     *
     * @param boardCode 게시판 코드
     * @param postId    게시글 ID
     * @param user      인증된 사용자 정보
     * @return 조회 통계 응답
     */
    @Transactional(readOnly = true)
    public PostViewStatsResponse getPostViewStats(String boardCode, Long postId, AuthenticatedUser user) {
        User currentUser = userRepository.findById(user.userId())
                .orElseThrow(UserNotFoundException::new);

        // OPERATOR 이상만 조회 가능
        if (!currentUser.isOperatorOrAbove()) {
            throw new PostAccessDeniedException();
        }

        Board board = boardService.getBoardEntity(boardCode);
        Post post = postRepository.findByBoardAndIdAndDeletedFalse(board, postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        return postViewService.getPostViewStats(post);
    }

    /**
     * 게시글 조회 기록 목록을 조회합니다.
     * OPERATOR 이상만 조회 가능합니다.
     *
     * @param boardCode 게시판 코드
     * @param postId    게시글 ID
     * @param user      인증된 사용자 정보
     * @param pageable  페이징 정보
     * @return 조회 기록 페이지
     */
    @Transactional(readOnly = true)
    public Page<PostViewHistoryResponse> getPostViewHistory(String boardCode, Long postId,
                                                            AuthenticatedUser user, Pageable pageable) {
        User currentUser = userRepository.findById(user.userId())
                .orElseThrow(UserNotFoundException::new);

        // OPERATOR 이상만 조회 가능
        if (!currentUser.isOperatorOrAbove()) {
            throw new PostAccessDeniedException();
        }

        Board board = boardService.getBoardEntity(boardCode);
        Post post = postRepository.findByBoardAndIdAndDeletedFalse(board, postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        return postViewService.getPostViewHistory(post, pageable);
    }
}
