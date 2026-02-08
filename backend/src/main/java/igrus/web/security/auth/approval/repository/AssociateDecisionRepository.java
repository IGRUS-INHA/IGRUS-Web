package igrus.web.security.auth.approval.repository;

import igrus.web.security.auth.approval.domain.AssociateDecision;
import igrus.web.security.auth.approval.domain.AssociateDecisionType;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssociateDecisionRepository extends JpaRepository<AssociateDecision, Long> {

    Optional<AssociateDecision> findByUserId(Long userId);

    @Query("SELECT u FROM User u WHERE u.role = :role AND NOT EXISTS (SELECT 1 FROM AssociateDecision ad WHERE ad.user = u)")
    Page<User> findPendingAssociates(@Param("role") UserRole role, Pageable pageable);

    @Query(value = "SELECT ad FROM AssociateDecision ad JOIN FETCH ad.user WHERE ad.user.role = :role AND ad.type = :type",
            countQuery = "SELECT COUNT(ad) FROM AssociateDecision ad WHERE ad.user.role = :role AND ad.type = :type")
    Page<AssociateDecision> findByUserRoleAndType(@Param("role") UserRole role, @Param("type") AssociateDecisionType type, Pageable pageable);
}
