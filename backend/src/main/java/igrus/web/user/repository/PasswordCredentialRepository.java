package igrus.web.user.repository;

import igrus.web.user.domain.PasswordCredential;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordCredentialRepository extends JpaRepository<PasswordCredential, Long> {

    Optional<PasswordCredential> findByUser(User user);

    Optional<PasswordCredential> findByUserId(Long userId);

    boolean existsByUser(User user);

    Optional<PasswordCredential> findByUserAndStatus(User user, UserStatus status);
}
