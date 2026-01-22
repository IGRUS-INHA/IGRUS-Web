package igrus.web.user.repository;

import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserRoleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleHistoryRepository extends JpaRepository<UserRoleHistory, Long> {

    List<UserRoleHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<UserRoleHistory> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    List<UserRoleHistory> findByNewRole(UserRole newRole);
}
