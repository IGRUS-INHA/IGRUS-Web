package igrus.web.security.auth.password.service.signup;

import igrus.web.security.auth.common.dto.request.ResendVerificationRequest;
import igrus.web.security.auth.common.exception.verification.VerificationResendRateLimitedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 시 이메일 미인증 사용자에게 자동으로 인증 코드를 재발송하는 서비스입니다.
 * 인증 코드 재발송이 호출자의 트랜잭션 롤백에 영향받지 않도록 REQUIRES_NEW 트랜잭션을 사용합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoResendVerificationService {

    private final ResendVerificationService resendVerificationService;

    /**
     * 이메일 인증 코드를 자동 재발송합니다.
     * Rate Limit이나 기타 예외 발생 시 무시하고 정상 반환합니다.
     *
     * @param email 인증 코드를 발송할 이메일 주소
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void autoResendIfPossible(String email) {
        try {
            resendVerificationService.resendVerification(new ResendVerificationRequest(email));
            log.info("이메일 미인증 로그인 시도 - 인증 코드 자동 재발송 완료: email={}", email);
        } catch (VerificationResendRateLimitedException e) {
            log.info("이메일 미인증 로그인 시도 - 최근 발송됨 (Rate Limit): email={}", email);
        } catch (Exception e) {
            log.warn("이메일 미인증 로그인 시도 - 자동 재발송 실패: email={}", email, e);
        }
    }
}
