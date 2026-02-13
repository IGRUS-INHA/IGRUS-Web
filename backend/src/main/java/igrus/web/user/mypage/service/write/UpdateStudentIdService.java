package igrus.web.user.mypage.service.write;

import igrus.web.security.auth.common.exception.signup.DuplicateStudentIdException;
import igrus.web.security.auth.password.domain.PasswordCredential;
import igrus.web.security.auth.password.exception.InvalidCredentialsException;
import igrus.web.security.auth.password.repository.PasswordCredentialRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.StudentIdNotTemporaryException;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.mypage.dto.request.UpdateStudentIdRequest;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 학번 변경 서비스.
 * 임시 학번을 사용하는 사용자가 실제 학번으로 변경할 수 있도록 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdateStudentIdService {

    private final UserRepository userRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 학번을 변경합니다.
     *
     * @param userId  사용자 ID
     * @param request 학번 변경 요청 정보
     */
    public void updateStudentId(Long userId, UpdateStudentIdRequest request) {
        log.info("학번 변경 요청 - userId: {}", userId);

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

        // 3. 임시 학번 사용자인지 확인
        if (!user.isHasTemporaryStudentId()) {
            log.warn("임시 학번이 아닌 사용자의 학번 변경 시도 - userId: {}", userId);
            throw new StudentIdNotTemporaryException();
        }

        // 4. 중복 검증 (soft-delete 포함)
        if (userRepository.existsByStudentId(request.newStudentId())) {
            log.warn("학번 중복 - newStudentId: {}", request.newStudentId());
            throw new DuplicateStudentIdException();
        }
        if (userRepository.countByStudentIdIncludingDeleted(request.newStudentId()) > 0) {
            log.warn("학번 중복 (삭제된 사용자 포함) - newStudentId: {}", request.newStudentId());
            throw new DuplicateStudentIdException();
        }

        // 5. 학번 변경 (99 접두사 검증은 엔티티 내부에서 수행)
        user.updateStudentId(request.newStudentId());

        log.info("학번 변경 완료 - userId: {}, newStudentId: {}", userId, request.newStudentId());
    }
}
