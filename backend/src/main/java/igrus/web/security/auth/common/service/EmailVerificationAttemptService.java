package igrus.web.security.auth.common.service;

import igrus.web.security.auth.common.domain.EmailVerification;
import igrus.web.security.auth.common.repository.EmailVerificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이메일 인증 시도 횟수를 별도 트랜잭션으로 관리하는 서비스입니다.
 * 인증 실패 시 예외가 발생해도 시도 횟수가 롤백되지 않도록 REQUIRES_NEW 트랜잭션을 사용합니다.
 */
@Service
@RequiredArgsConstructor
public class EmailVerificationAttemptService {

    private final EmailVerificationRepository emailVerificationRepository;

    /**
     * 인증 시도 횟수를 증가시킵니다.
     * 별도 트랜잭션으로 실행되어 호출자의 트랜잭션이 롤백되어도 시도 횟수는 유지됩니다.
     *
     * @param verificationId 이메일 인증 레코드 ID
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementAttempts(Long verificationId) {
        EmailVerification verification = emailVerificationRepository.findById(verificationId)
                .orElseThrow();
        verification.incrementAttempts();
        emailVerificationRepository.save(verification);
    }
}
