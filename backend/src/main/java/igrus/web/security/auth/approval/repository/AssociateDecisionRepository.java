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

import java.util.List;
import java.util.Optional;

@Repository
public interface AssociateDecisionRepository extends JpaRepository<AssociateDecision, Long> {

    Optional<AssociateDecision> findByUserIdAndActiveTrue(Long userId);

    @Query("""
        SELECT u FROM User u WHERE u.role = :role
        AND NOT EXISTS (
            SELECT 1 FROM AssociateDecision ad
            WHERE ad.user = u AND ad.active = true AND ad.type IN (:excludeTypes)
        )
        """)
    Page<User> findPendingAssociates(@Param("role") UserRole role, @Param("excludeTypes") List<AssociateDecisionType> excludeTypes, Pageable pageable);

    @Query(value = """
        SELECT ad FROM AssociateDecision ad JOIN FETCH ad.user
        WHERE ad.active = true AND ad.type = :type AND ad.user.role = :role
        """,
        countQuery = """
        SELECT COUNT(ad) FROM AssociateDecision ad
        WHERE ad.active = true AND ad.type = :type AND ad.user.role = :role
        """)
    Page<AssociateDecision> findActiveByType(@Param("role") UserRole role, @Param("type") AssociateDecisionType type, Pageable pageable);
}
