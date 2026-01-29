package igrus.web.security.auth.common.service;

import igrus.web.security.auth.common.exception.account.AccountSuspendedException;
import igrus.web.security.auth.common.exception.account.AccountWithdrawnException;
import igrus.web.security.auth.common.exception.email.EmailNotVerifiedException;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정 상태 검증 서비스.
 *
 * <p>JWT 토큰 유효성 검증 후 DB에서 계정 상태를 조회하여
 * SUSPENDED/WITHDRAWN/PENDING_VERIFICATION 계정의 접근을 차단합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountStatusService {

    private final UserRepository userRepository;

    /**
     * 계정 상태를 검증합니다.
     *
     * @param userId 검증할 사용자 ID
     * @throws UserNotFoundException 사용자가 존재하지 않는 경우
     * @throws AccountSuspendedException 계정이 정지된 경우
     * @throws AccountWithdrawnException 계정이 탈퇴된 경우
     * @throws EmailNotVerifiedException 이메일 인증이 완료되지 않은 경우
     */
    @Transactional(readOnly = true)
    public void validateAccountStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("계정 상태 검증 실패 - 사용자 없음: userId={}", userId);
                    return new UserNotFoundException(userId);
                });

        switch (user.getStatus()) {
            case ACTIVE -> {
                // 유효한 상태 - 정상 통과
            }
            case SUSPENDED -> {
                log.warn("정지된 계정으로 접근 시도: userId={}", userId);
                throw new AccountSuspendedException();
            }
            case WITHDRAWN -> {
                log.warn("탈퇴된 계정으로 접근 시도: userId={}", userId);
                throw new AccountWithdrawnException();
            }
            case PENDING_VERIFICATION -> {
                log.warn("이메일 미인증 계정으로 접근 시도: userId={}", userId);
                throw new EmailNotVerifiedException();
            }
        }
    }
}
