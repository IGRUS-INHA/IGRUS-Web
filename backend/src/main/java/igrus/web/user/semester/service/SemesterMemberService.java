package igrus.web.user.semester.service;

import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.domain.UserStatus;
import igrus.web.user.repository.UserRepository;
import igrus.web.user.semester.domain.SemesterMember;
import igrus.web.user.semester.dto.response.CandidateMemberResponse;
import igrus.web.user.semester.dto.response.RegisterSemesterMembersResponse;
import igrus.web.user.semester.dto.response.SemesterMemberListResponse;
import igrus.web.user.semester.dto.response.SemesterSummaryResponse;
import igrus.web.user.semester.exception.InvalidSemesterException;
import igrus.web.user.semester.repository.SemesterMemberRepository;
import igrus.web.user.semester.repository.SemesterMemberWithUserProjection;
import igrus.web.user.semester.repository.SemesterSummaryProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SemesterMemberService {

    private final SemesterMemberRepository semesterMemberRepository;
    private final UserRepository userRepository;

    // === US1: 회원 등록 ===

    /**
     * 등록 후보 회원 목록을 조회합니다.
     * ASSOCIATE 이상 + ACTIVE 상태 회원 목록을 반환하며, 해당 학기 등록 여부를 포함합니다.
     */
    @Transactional(readOnly = true)
    public List<CandidateMemberResponse> getCandidateMembers(int year, int semester) {
        validateYearAndSemester(year, semester);

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

    /**
     * 선택된 회원들을 해당 학기에 일괄 등록합니다.
     */
    public RegisterSemesterMembersResponse registerMembers(int year, int semester, List<Long> userIds) {
        validateYearAndSemester(year, semester);

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

    // === US2: 회원 제외 ===

    /**
     * 선택된 회원들을 해당 학기에서 제외합니다.
     */
    public int removeMembers(int year, int semester, List<Long> userIds) {
        validateYearAndSemester(year, semester);

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

    // === US3: 명단 조회 ===

    /**
     * 학기 목록을 조회합니다 (회원 수 포함, 최신순).
     */
    @Transactional(readOnly = true)
    public List<SemesterSummaryResponse> getSemesterList() {
        List<SemesterSummaryProjection> results = semesterMemberRepository.findDistinctSemestersWithCount();

        return results.stream()
                .map(p -> SemesterSummaryResponse.of(p.getSemesterYear(), p.getSemesterTerm(), p.getMemberCount()))
                .toList();
    }

    /**
     * 학기별 회원 명단을 조회합니다 (탈퇴자 포함).
     */
    @Transactional(readOnly = true)
    public List<SemesterMemberListResponse> getMemberList(int year, int semester, String keyword) {
        validateYearAndSemester(year, semester);

        List<SemesterMemberWithUserProjection> results =
                semesterMemberRepository.findAllWithUserIncludingDeleted(year, semester);

        return results.stream()
                .map(this::mapToMemberListResponse)
                .filter(response -> matchesKeyword(response, keyword))
                .toList();
    }

    // === Private helpers ===

    private SemesterMemberListResponse mapToMemberListResponse(SemesterMemberWithUserProjection projection) {
        return new SemesterMemberListResponse(
                projection.getUserId(),
                projection.getStudentId(),
                projection.getName(),
                projection.getDepartment(),
                projection.getEmail(),
                projection.getPhoneNumber(),
                UserRole.valueOf(projection.getMemberRole()),
                Boolean.TRUE.equals(projection.getDeleted())
        );
    }

    private boolean matchesKeyword(SemesterMemberListResponse response, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String lower = keyword.toLowerCase();
        return (response.studentId() != null && response.studentId().toLowerCase().contains(lower))
                || (response.name() != null && response.name().toLowerCase().contains(lower));
    }

    private void validateYearAndSemester(int year, int semester) {
        if (year < 2000 || year > 2100) {
            throw new InvalidSemesterException("유효하지 않은 연도입니다: " + year);
        }
        if (semester != 1 && semester != 2) {
            throw new InvalidSemesterException("학기는 1 또는 2만 가능합니다: " + semester);
        }
    }
}
