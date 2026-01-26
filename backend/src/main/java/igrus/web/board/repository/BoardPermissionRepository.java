package igrus.web.board.repository;

import igrus.web.board.domain.Board;
import igrus.web.board.domain.BoardPermission;
import igrus.web.user.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 게시판 권한 Repository.
 */
@Repository
public interface BoardPermissionRepository extends JpaRepository<BoardPermission, Long> {

    Optional<BoardPermission> findByBoardAndRole(Board board, UserRole role);

    List<BoardPermission> findAllByBoard(Board board);
}
