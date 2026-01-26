package igrus.web.board.service;

import igrus.web.board.domain.Board;
import igrus.web.board.domain.BoardPermission;
import igrus.web.board.exception.BoardReadDeniedException;
import igrus.web.board.exception.BoardWriteDeniedException;
import igrus.web.board.repository.BoardPermissionRepository;
import igrus.web.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시판 권한 검증 서비스.
 * 게시판별 역할에 따른 읽기/쓰기 권한을 검증합니다.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BoardPermissionService {

    private final BoardPermissionRepository boardPermissionRepository;

    /**
     * 해당 역할이 게시판 읽기 권한을 가지는지 확인합니다.
     */
    public boolean canRead(Board board, UserRole role) {
        return boardPermissionRepository.findByBoardAndRole(board, role)
                .map(BoardPermission::hasReadPermission)
                .orElse(false);
    }

    /**
     * 해당 역할이 게시판 쓰기 권한을 가지는지 확인합니다.
     */
    public boolean canWrite(Board board, UserRole role) {
        return boardPermissionRepository.findByBoardAndRole(board, role)
                .map(BoardPermission::hasWritePermission)
                .orElse(false);
    }

    /**
     * 읽기 권한을 검증하고, 권한이 없으면 예외를 발생시킵니다.
     */
    public void checkReadPermission(Board board, UserRole role) {
        if (!canRead(board, role)) {
            log.warn("읽기 권한 거부 - board: {}, role: {}", board.getCode().name(), role);
            throw new BoardReadDeniedException(
                    String.format("'%s' 게시판 읽기 권한이 없습니다. 필요 권한: %s 이상",
                            board.getName(), getRequiredRoleForRead(board)));
        }
    }

    /**
     * 쓰기 권한을 검증하고, 권한이 없으면 예외를 발생시킵니다.
     */
    public void checkWritePermission(Board board, UserRole role) {
        if (!canWrite(board, role)) {
            log.warn("쓰기 권한 거부 - board: {}, role: {}", board.getCode().name(), role);
            throw new BoardWriteDeniedException(
                    String.format("'%s' 게시판 쓰기 권한이 없습니다. 필요 권한: %s 이상",
                            board.getName(), getRequiredRoleForWrite(board)));
        }
    }

    /**
     * 읽기에 필요한 최소 역할을 반환합니다.
     */
    private String getRequiredRoleForRead(Board board) {
        for (UserRole role : UserRole.values()) {
            if (canRead(board, role)) {
                return role.name();
            }
        }
        return "ADMIN";
    }

    /**
     * 쓰기에 필요한 최소 역할을 반환합니다.
     */
    private String getRequiredRoleForWrite(Board board) {
        for (UserRole role : UserRole.values()) {
            if (canWrite(board, role)) {
                return role.name();
            }
        }
        return "ADMIN";
    }
}
