package igrus.web.security.auth.password.service.signup;

import igrus.web.security.auth.common.exception.signup.DuplicateEmailException;
import igrus.web.security.auth.common.exception.signup.DuplicateStudentIdException;
import igrus.web.security.auth.password.dto.response.DuplicateCheckResponse;
import igrus.web.user.exception.InvalidEmailException;
import igrus.web.user.exception.InvalidStudentIdException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CheckDuplicateService {

    private static final String STUDENT_ID_PATTERN = "^\\d{8}$";
    private static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";

    private final UserRepository userRepository;

    /**
     * 학번 중복을 확인합니다.
     * soft-deleted 사용자를 포함하여 중복을 확인합니다.
     *
     * @param studentId 확인할 학번
     * @return 사용 가능 여부 응답
     * @throws InvalidStudentIdException 학번 형식이 올바르지 않은 경우
     * @throws DuplicateStudentIdException 이미 가입된 학번인 경우
     */
    public DuplicateCheckResponse checkStudentId(String studentId) {
        if (studentId == null || !studentId.matches(STUDENT_ID_PATTERN)) {
            throw new InvalidStudentIdException(studentId);
        }

        if (userRepository.existsByStudentIdIncludingDeleted(studentId)) {
            throw new DuplicateStudentIdException();
        }

        return DuplicateCheckResponse.studentIdAvailable();
    }

    /**
     * 이메일 중복을 확인합니다.
     * soft-deleted 사용자를 포함하여 중복을 확인합니다.
     *
     * @param email 확인할 이메일
     * @return 사용 가능 여부 응답
     * @throws InvalidEmailException 이메일 형식이 올바르지 않은 경우
     * @throws DuplicateEmailException 이미 존재하는 이메일인 경우
     */
    public DuplicateCheckResponse checkEmail(String email) {
        if (email == null || !email.matches(EMAIL_PATTERN)) {
            throw new InvalidEmailException(email);
        }

        if (userRepository.existsByEmailIncludingDeleted(email)) {
            throw new DuplicateEmailException();
        }

        return DuplicateCheckResponse.emailAvailable();
    }
}
