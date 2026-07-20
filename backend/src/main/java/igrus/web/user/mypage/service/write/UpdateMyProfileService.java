package igrus.web.user.mypage.service.write;

import igrus.web.user.domain.ProfileLink;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 공개 프로필(닉네임/자기소개/링크) 수정 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMyProfileService {

    private final UserRepository userRepository;

    public void updateMyProfile(Long userId, String nickname, String introduction, List<ProfileLink> links) {
        log.info("공개 프로필 수정 요청 - userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.updatePublicProfile(nickname, introduction, links);
    }
}
