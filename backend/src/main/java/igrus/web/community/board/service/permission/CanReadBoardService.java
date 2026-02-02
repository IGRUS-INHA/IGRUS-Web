package igrus.web.community.board.service.permission;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardPermission;
import igrus.web.community.board.repository.BoardPermissionRepository;
import igrus.web.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시판 읽기 권한 확인 서비스.
 * 해당 역할이 게시판 읽기 권한을 가지는지 확인합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CanReadBoardService {

    private final BoardPermissionRepository boardPermissionRepository;

    /**
     * 해당 역할이 게시판 읽기 권한을 가지는지 확인합니다.
     */
    @Transactional(readOnly = true)
    public boolean canRead(Board board, UserRole role) {
        return boardPermissionRepository.findByBoardAndRole(board, role)
                .map(BoardPermission::hasReadPermission)
                .orElse(false);
    }
}
