package igrus.web.user.semester.service.read;

import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.semester.dto.response.CandidateMemberResponse;
import igrus.web.user.semester.repository.SemesterMemberRepository;
import igrus.web.user.semester.service.support.SemesterValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 등록 후보 회원 목록 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetCandidateMembersService {

    private final SemesterMemberRepository semesterMemberRepository;
    private final UserRepository userRepository;
    private final SemesterValidator semesterValidator;

    /**
     * 등록 후보 회원 목록을 조회합니다.
     * ASSOCIATE 이상 + ACTIVE 상태 회원 목록을 반환하며, 해당 학기 등록 여부를 포함합니다.
     */
    @Transactional(readOnly = true)
    public List<CandidateMemberResponse> getCandidateMembers(int year, int semester) {
        semesterValidator.validateYearAndSemester(year, semester);

        List<User> activeUsers = userRepository.findAll().stream()
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .filter(user -> user.getRole().ordinal() >= UserRole.ASSOCIATE.ordinal())
                .toList();

        return activeUsers.stream()
                .map(user -> {
                    boolean alreadyRegistered = semesterMemberRepository.existsByUserAndYearAndSemester(user, year, semester);
                    return CandidateMemberResponse.from(user, alreadyRegistered);
                })
                .toList();
    }
}
