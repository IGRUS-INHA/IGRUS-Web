package igrus.web.community.board.service.permission;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.exception.BoardReadDeniedException;
import igrus.web.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시판 읽기 권한 검증 서비스.
 * 읽기 권한을 검증하고, 권한이 없으면 예외를 발생시킵니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CheckReadPermissionService {

    private final CanReadBoardService canReadBoardService;

    /**
     * 읽기 권한을 검증하고, 권한이 없으면 예외를 발생시킵니다.
     */
    @Transactional(readOnly = true)
    public void checkReadPermission(Board board, UserRole role) {
        if (!canReadBoardService.canRead(board, role)) {
            log.warn("읽기 권한 거부 - board: {}, role: {}", board.getCode().name(), role);
            throw new BoardReadDeniedException(
                    String.format("'%s' 게시판 읽기 권한이 없습니다. 필요 권한: %s 이상",
                            board.getName(), getRequiredRoleForRead(board)));
        }
    }

    /**
     * 읽기에 필요한 최소 역할을 반환합니다.
     */
    private String getRequiredRoleForRead(Board board) {
        for (UserRole role : UserRole.values()) {
            if (canReadBoardService.canRead(board, role)) {
                return role.name();
            }
        }
        return "ADMIN";
    }
}
