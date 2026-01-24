package igrus.web.security.auth.common.repository;

import igrus.web.security.auth.common.domain.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findByEmailAndVerifiedFalse(String email);
    Optional<EmailVerification> findByEmailAndCodeAndVerifiedFalse(String email, String code);
    List<EmailVerification> findByExpiresAtBeforeAndVerifiedFalse(Instant dateTime);
    void deleteByExpiresAtBeforeAndVerifiedFalse(Instant dateTime);

    /**
     * 이메일 인증 완료 여부를 확인합니다.
     *
     * @param email 확인할 이메일 주소
     * @return 해당 이메일에 대해 인증이 완료된 레코드가 있으면 true
     */
    boolean existsByEmailAndVerifiedTrue(String email);

    /**
     * 특정 이메일에 대해 지정 시간 이후에 생성된 미인증 레코드가 있는지 확인합니다.
     * Rate Limiting 체크에 사용됩니다.
     *
     * @param email 확인할 이메일 주소
     * @param after 기준 시간
     * @return 기준 시간 이후에 생성된 미인증 레코드가 있으면 true
     */
    boolean existsByEmailAndVerifiedFalseAndCreatedAtAfter(String email, Instant after);
}
