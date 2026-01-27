package igrus.web.community.board.repository;

import igrus.web.community.board.domain.Board;
import igrus.web.community.board.domain.BoardCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 게시판 Repository.
 */
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    Optional<Board> findByCode(BoardCode code);

    List<Board> findAllByOrderByDisplayOrderAsc();
}
