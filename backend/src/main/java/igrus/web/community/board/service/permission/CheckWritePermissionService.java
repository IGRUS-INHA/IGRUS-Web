package igrus.web.community.board.service.permission;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.exception.BoardWriteDeniedException;
import igrus.web.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시판 쓰기 권한 검증 서비스.
 * 쓰기 권한을 검증하고, 권한이 없으면 예외를 발생시킵니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CheckWritePermissionService {

    private final CanWriteBoardService canWriteBoardService;

    /**
     * 쓰기 권한을 검증하고, 권한이 없으면 예외를 발생시킵니다.
     */
    @Transactional(readOnly = true)
    public void checkWritePermission(Board board, UserRole role) {
        if (!canWriteBoardService.canWrite(board, role)) {
            log.warn("쓰기 권한 거부 - board: {}, role: {}", board.getCode().name(), role);
            throw new BoardWriteDeniedException(
                    String.format("'%s' 게시판 쓰기 권한이 없습니다. 필요 권한: %s 이상",
                            board.getName(), getRequiredRoleForWrite(board)));
        }
    }

    /**
     * 쓰기에 필요한 최소 역할을 반환합니다.
     */
    private String getRequiredRoleForWrite(Board board) {
        for (UserRole role : UserRole.values()) {
            if (canWriteBoardService.canWrite(board, role)) {
                return role.name();
            }
        }
        return "ADMIN";
    }
}
