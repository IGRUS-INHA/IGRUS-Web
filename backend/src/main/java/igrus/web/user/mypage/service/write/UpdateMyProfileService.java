package igrus.web.user.mypage.service.write;

import igrus.web.user.domain.User;
import igrus.web.user.exception.DuplicateEmailException;
import igrus.web.user.exception.DuplicatePhoneNumberException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.mypage.dto.request.UpdateProfileRequest;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateMyProfileService {

    private final UserRepository userRepository;

    public void updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("프로필 수정 요청 - userId: {}", userId);

        // 1. User 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        // 2. 이메일 변경 시 중복 체크
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                log.warn("이메일 중복 - email: {}", request.email());
                throw new DuplicateEmailException(request.email());
            }
            user.updateEmail(request.email());
        }

        // 3. 전화번호 변경 시 중복 체크
        if (request.phoneNumber() != null && !request.phoneNumber().equals(user.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumber(request.phoneNumber())) {
                log.warn("전화번호 중복 - phoneNumber: {}", request.phoneNumber());
                throw new DuplicatePhoneNumberException(request.phoneNumber());
            }
            user.updatePhoneNumber(request.phoneNumber());
        }

        log.info("프로필 수정 완료 - userId: {}", userId);
    }
}
