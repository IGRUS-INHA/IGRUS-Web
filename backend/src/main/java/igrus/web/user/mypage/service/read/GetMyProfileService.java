package igrus.web.user.mypage.service.read;

import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.mypage.dto.response.MyProfileResponse;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyProfileService {

    private final UserRepository userRepository;

    public MyProfileResponse getMyProfile(Long userId) {
        log.info("프로필 조회 요청 - userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        return MyProfileResponse.from(user);
    }
}
