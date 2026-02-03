package igrus.web.community.post.service.read;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import igrus.web.community.board.service.read.GetBoardEntityService;
import igrus.web.community.board.service.permission.CheckReadPermissionService;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.dto.response.PostListPageResponse;
import igrus.web.community.post.service.support.PostQueryHelper;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 목록 조회 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetPostListService {

    private final UserRepository userRepository;
    private final GetBoardEntityService getBoardEntityService;
    private final CheckReadPermissionService checkReadPermissionService;
    private final PostQueryHelper postQueryHelper;

    /**
     * 게시글 목록을 조회합니다.
     *
     * @param boardCode 게시판 코드
     * @param user 인증된 사용자 정보
     * @param keyword 검색 키워드 (null 가능)
     * @param questionOnly 질문 게시글만 조회 여부 (null 가능)
     * @param pageable 페이징 정보
     * @return 게시글 목록 페이지 응답
     */
    @Transactional(readOnly = true)
    public PostListPageResponse getPostList(String boardCode, AuthenticatedUser user, String keyword, Boolean questionOnly, Pageable pageable) {
        // 사용자 조회
        User currentUser = userRepository.findById(user.userId())
                .orElseThrow(UserNotFoundException::new);

        // 게시판 조회 및 읽기 권한 확인
        Board board = getBoardEntityService.getBoardEntity(boardCode);
        checkReadPermissionService.checkReadPermission(board, currentUser.getRole());

        // 준회원인 경우 공지사항은 공개된 글만 조회
        boolean isAssociate = currentUser.getRole() == UserRole.ASSOCIATE;
        boolean isNoticeBoard = board.getCode() == BoardCode.NOTICES;

        Page<Post> postPage;

        if (isAssociate && isNoticeBoard) {
            // 준회원이 공지사항 조회 시 준회원 공개 게시글만 조회
            postPage = postQueryHelper.getPostsForAssociateInNotices(board, keyword, pageable);
        } else if (Boolean.TRUE.equals(questionOnly)) {
            // 질문 게시글만 조회 (자유게시판에서만 의미 있음)
            postPage = postQueryHelper.getQuestionPosts(board, keyword, pageable);
        } else {
            // 일반 조회
            postPage = postQueryHelper.getRegularPosts(board, keyword, pageable);
        }

        return PostListPageResponse.from(postPage);
    }
}
