package igrus.web.user.semester.service.write;

import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.semester.repository.SemesterMemberRepository;
import igrus.web.user.semester.service.support.SemesterValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 학기별 회원 일괄 제외 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RemoveSemesterMembersService {

    private final SemesterMemberRepository semesterMemberRepository;
    private final UserRepository userRepository;
    private final SemesterValidator semesterValidator;

    /**
     * 선택된 회원들을 해당 학기에서 제외합니다.
     */
    public int removeMembers(int year, int semester, List<Long> userIds) {
        semesterValidator.validateYearAndSemester(year, semester);

        int removedCount = 0;

        for (Long userId : userIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                continue;
            }

            if (semesterMemberRepository.existsByUserAndYearAndSemester(user, year, semester)) {
                semesterMemberRepository.deleteByUserAndYearAndSemester(user, year, semester);
                removedCount++;
            }
        }

        log.info("학기별 회원 제외 완료: year={}, semester={}, removed={}", year, semester, removedCount);

        return removedCount;
    }
}
