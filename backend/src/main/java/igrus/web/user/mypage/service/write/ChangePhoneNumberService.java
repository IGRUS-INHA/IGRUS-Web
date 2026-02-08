package igrus.web.user.mypage.service.write;

import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.DuplicatePhoneNumberException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.mypage.dto.request.ChangePhoneNumberRequest;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전화번호 변경 서비스.
 * 비밀번호를 확인한 후 전화번호를 변경합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChangePhoneNumberService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 전화번호를 변경합니다.
     *
     * @param userId  사용자 ID
     * @param request 전화번호 변경 요청 정보
     */
    public void changePhoneNumber(Long userId, ChangePhoneNumberRequest request) {
        log.info("전화번호 변경 요청 - userId: {}", userId);

        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 2. 비밀번호 확인
        PasswordCredential credential = passwordCredentialRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!passwordEncoder.matches(request.password(), credential.getPasswordHash())) {
            log.warn("비밀번호 불일치 - userId: {}", userId);
            throw new InvalidCredentialsException();
        }

        // 3. 현재 전화번호와 동일한지 체크
        if (request.newPhoneNumber().equals(user.getPhoneNumber())) {
            log.warn("현재 전화번호와 동일 - userId: {}", userId);
            throw new DuplicatePhoneNumberException(request.newPhoneNumber());
        }

        // 4. 새 전화번호 중복 체크
        if (userRepository.existsByPhoneNumber(request.newPhoneNumber())) {
            log.warn("전화번호 중복 - phoneNumber: {}", request.newPhoneNumber());
            throw new DuplicatePhoneNumberException(request.newPhoneNumber());
        }

        // 5. 전화번호 변경
        user.updatePhoneNumber(request.newPhoneNumber());

        log.info("전화번호 변경 완료 - userId: {}", userId);
    }
}
