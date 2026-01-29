package igrus.web.security.auth.common.repository;

import igrus.web.security.auth.common.domain.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 로그인 시도 정보를 관리하는 리포지토리.
 *
 * @see LoginAttempt
 */
@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    /**
     * 학번으로 로그인 시도 정보를 조회합니다.
     *
     * @param studentId 학번
     * @return 로그인 시도 정보
     */
    Optional<LoginAttempt> findByStudentId(String studentId);
}
