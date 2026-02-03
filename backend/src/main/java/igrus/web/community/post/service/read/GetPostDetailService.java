package igrus.web.community.post.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.service.read.GetBoardEntityService;
import igrus.web.community.board.service.permission.CheckReadPermissionService;
import igrus.web.community.bookmark.repository.BookmarkRepository;
import igrus.web.community.like.post_like.repository.PostLikeRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.dto.response.PostDetailResponse;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.community.post.service.support.IncrementViewCountService;
import igrus.web.community.post.service.support.RecordPostViewService;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 상세 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetPostDetailService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final GetBoardEntityService getBoardEntityService;
    private final CheckReadPermissionService checkReadPermissionService;
    private final RecordPostViewService recordPostViewService;
    private final IncrementViewCountService incrementViewCountService;
    private final PostLikeRepository postLikeRepository;
    private final BookmarkRepository bookmarkRepository;

    /**
     * 게시글 상세 정보를 조회합니다.
     *
     * @param boardCode 게시판 코드
     * @param postId 게시글 ID
     * @param user 인증된 사용자 정보
     * @return 게시글 상세 응답
     */
    public PostDetailResponse getPostDetail(String boardCode, Long postId, AuthenticatedUser user) {
        // 사용자 조회
        User currentUser = userRepository.findById(user.userId())
                .orElseThrow(UserNotFoundException::new);

        // 게시판 조회 및 읽기 권한 확인
        Board board = getBoardEntityService.getBoardEntity(boardCode);
        checkReadPermissionService.checkReadPermission(board, currentUser.getRole());

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
        recordPostViewService.recordViewAsync(post.getId(), currentUser.getId());

        // 조회수 증가 (재시도 2회)
        incrementViewCountService.incrementViewCountWithRetry(post, 2);

        // 현재 사용자가 작성자인지 확인 (탈퇴한 사용자는 author가 null일 수 있음)
        boolean isCurrentUserAuthor = post.getAuthor() != null
                && post.getAuthor().getId().equals(currentUser.getId());

        // 좋아요/북마크 상태 조회
        boolean liked = postLikeRepository.existsByPostIdAndUserId(postId, currentUser.getId());
        boolean bookmarked = bookmarkRepository.existsByPostIdAndUserId(postId, currentUser.getId());

        return PostDetailResponse.from(post, isCurrentUserAuthor, liked, bookmarked);
    }
}
