package igrus.web.community.post.service.write;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.service.read.GetBoardEntityService;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.domain.PostImage;
import igrus.web.community.post.dto.request.UpdatePostRequest;
import igrus.web.community.post.dto.response.PostUpdateResponse;
import igrus.web.community.post.exception.PostAccessDeniedException;
import igrus.web.community.post.exception.PostDeletedException;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
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
 * 게시글 수정 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdatePostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final GetBoardEntityService getBoardEntityService;
    private final PostValidator postValidator;

    /**
     * 게시글을 수정합니다.
     *
     * @param boardCode 게시판 코드
     * @param postId 게시글 ID
     * @param request 게시글 수정 요청
     * @param authenticatedUser 인증된 사용자 정보
     * @return 수정된 게시글 응답
     */
    public PostUpdateResponse updatePost(String boardCode, Long postId, UpdatePostRequest request, AuthenticatedUser authenticatedUser) {
        // 사용자 조회
        User user = userRepository.findById(authenticatedUser.userId())
                .orElseThrow(UserNotFoundException::new);

        // 게시판 조회
        Board board = getBoardEntityService.getBoardEntity(boardCode);

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

        // 제목과 내용 수정
        post.updateContent(request.title(), request.content());

        // 질문 옵션 변경 (도메인에서 게시판 옵션 검증)
        post.setQuestion(request.isQuestion());

        // 준회원 공개 옵션 변경 (도메인에서 게시판 검증)
        post.setVisibleToAssociate(request.isVisibleToAssociate());

        // 이미지 수정 (기존 이미지 삭제 후 새 이미지 추가)
        post.clearImages();
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            postValidator.validateImageCount(request.imageUrls().size());
            for (int i = 0; i < request.imageUrls().size(); i++) {
                PostImage image = PostImage.create(post, request.imageUrls().get(i), i);
                post.addImage(image);
            }
        }

        return PostUpdateResponse.from(post);
    }
}
