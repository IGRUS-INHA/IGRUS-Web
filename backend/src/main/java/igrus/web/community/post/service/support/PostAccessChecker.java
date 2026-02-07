package igrus.web.community.post.service.support;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.service.permission.CheckReadPermissionService;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 접근 권한 검증기.
 * 사용자가 해당 게시글에 접근(좋아요, 댓글, 북마크 등) 가능한지 검증합니다.
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostAccessChecker {

    private final CheckReadPermissionService checkReadPermissionService;

    /**
     * 사용자가 해당 게시글에 접근 가능한지 검증합니다.
     * 게시판 읽기 권한과 준회원 공개 여부를 확인합니다.
     *
     * @param post 게시글
     * @param user 사용자
     * @throws igrus.web.community.board.exception.BoardReadDeniedException 게시판 읽기 권한이 없는 경우
     * @throws PostNotFoundException 준회원이 비공개 공지사항에 접근하는 경우
     */
    public void checkPostAccess(Post post, User user) {
        Board board = post.getBoard();
        checkReadPermissionService.checkReadPermission(board, user.getRole());

        if (user.getRole() == UserRole.ASSOCIATE
                && board.getCode() == BoardCode.NOTICES
                && !post.isVisibleToAssociate()) {
            throw new PostNotFoundException(post.getId());
        }
    }
}
