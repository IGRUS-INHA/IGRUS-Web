package igrus.web.user.mypage.service.read;

import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.mypage.dto.response.MyProfileResponse;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyProfileService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;

    public MyProfileResponse getMyProfile(Long userId) {
        log.info("프로필 조회 요청 - userId: {}", userId);

        // 1. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 2. PasswordCredential에서 승인일 가져오기
        Instant approvedAt = passwordCredentialRepository.findByUserId(userId)
                .map(PasswordCredential::getApprovedAt)
                .orElse(null);

        // 3. DTO로 변환해서 반환
        return MyProfileResponse.from(user, approvedAt);
    }
}
