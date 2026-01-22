package igrus.web.user.repository;

import igrus.web.user.domain.UserPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPositionRepository extends JpaRepository<UserPosition, Long> {

    List<UserPosition> findByUserId(Long userId);

    List<UserPosition> findByPositionId(Long positionId);

    Optional<UserPosition> findByUserIdAndPositionId(Long userId, Long positionId);

    boolean existsByUserIdAndPositionId(Long userId, Long positionId);

    void deleteByUserId(Long userId);

    void deleteByPositionId(Long positionId);
}
