package igrus.web.user.mypage.service.write;

import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.security.auth.password.service.support.ValidatePasswordFormatService;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.mypage.dto.request.ChangePasswordRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ChangeMyPasswordService {

    private final PasswordCredentialRepository passwordCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final ValidatePasswordFormatService validatePasswordFormatService;

    public void changePassword(Long userId, ChangePasswordRequest request) {
        log.info("비밀번호 변경 요청 - userId: {}", userId);

        // 1. PasswordCredential 조회
        PasswordCredential credential = passwordCredentialRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 2. 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword(), credential.getPasswordHash())) {
            log.warn("현재 비밀번호 불일치 - userId: {}", userId);
            throw new InvalidCredentialsException();
        }

        // 3. 새 비밀번호 형식 검증
        validatePasswordFormatService.validatePasswordFormat(request.newPassword());

        // 4. 새 비밀번호 해시해서 저장
        String newPasswordHash = passwordEncoder.encode(request.newPassword());
        credential.changePassword(newPasswordHash);

        log.info("비밀번호 변경 완료 - userId: {}", userId);
    }
}
