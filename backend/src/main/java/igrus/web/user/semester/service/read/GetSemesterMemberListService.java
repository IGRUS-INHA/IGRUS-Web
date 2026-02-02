package igrus.web.user.semester.service.read;

import igrus.web.user.domain.UserRole;
import igrus.web.user.semester.dto.response.SemesterMemberListResponse;
import igrus.web.user.semester.repository.SemesterMemberRepository;
import igrus.web.user.semester.repository.SemesterMemberWithUserProjection;
import igrus.web.user.semester.service.support.SemesterValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 학기별 회원 명단 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetSemesterMemberListService {

    private final SemesterMemberRepository semesterMemberRepository;
    private final SemesterValidator semesterValidator;

    /**
     * 학기별 회원 명단을 조회합니다 (탈퇴자 포함).
     */
    @Transactional(readOnly = true)
    public List<SemesterMemberListResponse> getMemberList(int year, int semester, String keyword) {
        semesterValidator.validateYearAndSemester(year, semester);

        List<SemesterMemberWithUserProjection> results =
                semesterMemberRepository.findAllWithUserIncludingDeleted(year, semester);

        return results.stream()
                .map(this::mapToMemberListResponse)
                .filter(response -> matchesKeyword(response, keyword))
                .toList();
    }

    private SemesterMemberListResponse mapToMemberListResponse(SemesterMemberWithUserProjection projection) {
        return new SemesterMemberListResponse(
                projection.getUserId(),
                projection.getStudentId(),
                projection.getName(),
                projection.getDepartment(),
                projection.getEmail(),
                projection.getPhoneNumber(),
                UserRole.valueOf(projection.getMemberRole()),
                Boolean.TRUE.equals(projection.getDeleted()),
                projection.getGrade(),
                projection.getMotivation()
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
}
