package igrus.web.community.post.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.exception.BoardWriteDeniedException;
import igrus.web.community.board.service.read.GetBoardEntityService;
import igrus.web.community.board.service.permission.CheckWritePermissionService;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.domain.PostImage;
import igrus.web.community.post.dto.request.CreatePostRequest;
import igrus.web.community.post.dto.response.PostCreateResponse;
import igrus.web.community.post.exception.InvalidPostOptionException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.community.post.service.support.CheckPostRateLimitService;
import igrus.web.community.post.service.support.PostValidator;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 작성 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CreatePostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final GetBoardEntityService getBoardEntityService;
    private final CheckWritePermissionService checkWritePermissionService;
    private final CheckPostRateLimitService checkPostRateLimitService;
    private final PostValidator postValidator;

    /**
     * 게시글을 작성합니다.
     *
     * @param boardCode 게시판 코드
     * @param request 게시글 작성 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 생성된 게시글 응답
     */
    public PostCreateResponse createPost(String boardCode, CreatePostRequest request, AuthenticatedUser authenticatedUser) {
        // 사용자 조회
        User author = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(UserNotFoundException::new);

        // 1. 게시판 조회 및 쓰기 권한 확인
        Board board = getBoardEntityService.getBoardEntity(boardCode);
        checkWritePermissionService.checkWritePermission(board, author.getRole());

        // 2. 공지사항 게시판 특수 처리
        if (board.getCode() == BoardCode.NOTICES) {
            return createNoticeFromPostRequest(board, author, request);
        }

        // 3. Rate Limit 확인 (공지사항은 제외)
        checkPostRateLimitService.checkRateLimit(author);

        // 4. 게시글 생성 (익명 여부에 따라 분기, 도메인에서 게시판 옵션 검증)
        Post post;
        if (request.isAnonymous()) {
            post = Post.createAnonymousPost(board, author, request.title(), request.content());
        } else {
            post = Post.createPost(board, author, request.title(), request.content());
        }

        // 5. 질문 옵션 설정 (도메인에서 게시판 옵션 검증)
        if (request.isQuestion()) {
            post.setQuestion(true);
        }

        // 6. 이미지 추가
        addImages(post, request);

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
            throw new InvalidPostOptionException("익명", BoardCode.NOTICES.name());
        }
        if (request.isQuestion()) {
            throw new InvalidPostOptionException("질문", BoardCode.NOTICES.name());
        }

        // 공지사항 생성 (요청의 isVisibleToAssociate 값 사용)
        Post post = Post.createNotice(board, author, request.title(), request.content(), request.isVisibleToAssociate());

        // 이미지 추가
        addImages(post, request);

        Post savedPost = postRepository.save(post);
        return PostCreateResponse.from(savedPost);
    }

    private void addImages(Post post, CreatePostRequest request) {
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            postValidator.validateImageCount(request.imageUrls().size());
            for (int i = 0; i < request.imageUrls().size(); i++) {
                PostImage image = PostImage.create(post, request.imageUrls().get(i), i);
                post.addImage(image);
            }
        }
    }
}
