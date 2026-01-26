package igrus.web.board.repository;

import igrus.web.board.domain.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 게시판 Repository.
 */
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    Optional<Board> findByCode(String code);

    List<Board> findAllByOrderByDisplayOrderAsc();
}
