package igrus.web.user.profile.service;

import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.profile.dto.response.PublicProfileResponse;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공개 프로필 조회 서비스 (인증 불필요).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPublicProfileService {

    private final UserRepository userRepository;

    public PublicProfileResponse getPublicProfile(String studentId) {
        User user = userRepository.findByStudentId(studentId)
                .orElseThrow(UserNotFoundException::new);

        return PublicProfileResponse.from(user);
    }
}
