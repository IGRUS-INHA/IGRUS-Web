package igrus.web.user.semester.service.read;

import igrus.web.user.semester.dto.response.SemesterSummaryResponse;
import igrus.web.user.semester.repository.SemesterMemberRepository;
import igrus.web.user.semester.repository.SemesterSummaryProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 학기 목록 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetSemesterListService {

    private final SemesterMemberRepository semesterMemberRepository;

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
}
