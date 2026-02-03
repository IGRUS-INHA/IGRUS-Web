package igrus.web.user.semester.service.write;

import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.semester.domain.SemesterMember;
import igrus.web.user.semester.dto.response.RegisterSemesterMembersResponse;
import igrus.web.user.semester.repository.SemesterMemberRepository;
import igrus.web.user.semester.service.support.SemesterValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 학기별 회원 일괄 등록 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RegisterSemesterMembersService {

    private final SemesterMemberRepository semesterMemberRepository;
    private final UserRepository userRepository;
    private final SemesterValidator semesterValidator;

    /**
     * 선택된 회원들을 해당 학기에 일괄 등록합니다.
     */
    public RegisterSemesterMembersResponse registerMembers(int year, int semester, List<Long> userIds) {
        semesterValidator.validateYearAndSemester(year, semester);

        int registeredCount = 0;
        int skippedCount = 0;

        for (Long userId : userIds) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                skippedCount++;
                continue;
            }

            if (semesterMemberRepository.existsByUserAndYearAndSemester(user, year, semester)) {
                skippedCount++;
                continue;
            }

            SemesterMember member = SemesterMember.create(user, year, semester, user.getRole());
            semesterMemberRepository.save(member);
            registeredCount++;
        }

        log.info("학기별 회원 등록 완료: year={}, semester={}, registered={}, skipped={}",
                year, semester, registeredCount, skippedCount);

        return new RegisterSemesterMembersResponse(registeredCount, skippedCount, userIds.size());
    }
}
