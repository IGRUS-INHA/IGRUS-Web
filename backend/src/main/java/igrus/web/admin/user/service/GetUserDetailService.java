package igrus.web.admin.user.service;

import igrus.web.admin.user.dto.UserDetailResponse;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class GetUserDetailService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserDetailResponse getUserDetail(Long userId) {
        return userRepository.findById(userId)
                .map(UserDetailResponse::from)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }
}
