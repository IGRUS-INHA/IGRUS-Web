package igrus.web.user.repository;

import igrus.web.user.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PositionRepository extends JpaRepository<Position, Long> {

    // === 기본 조회 (soft delete 자동 필터링 by @SQLRestriction) ===

    Optional<Position> findByName(String name);

    boolean existsByName(String name);

    List<Position> findAllByOrderByDisplayOrderAsc();

    // === 삭제된 데이터 포함 조회 (native query로 @SQLRestriction 우회) ===

    @Query(value = "SELECT * FROM positions p WHERE p.positions_id = :id", nativeQuery = true)
    Optional<Position> findByIdIncludingDeleted(@Param("id") Long id);

    @Query(value = "SELECT * FROM positions p WHERE p.positions_name = :name", nativeQuery = true)
    Optional<Position> findByNameIncludingDeleted(@Param("name") String name);
}
