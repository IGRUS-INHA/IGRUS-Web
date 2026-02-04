package igrus.web.user.semester.service.read;

import igrus.web.user.semester.dto.response.SemesterSummaryResponse;
import igrus.web.user.semester.repository.SemesterMemberRepository;
import igrus.web.user.semester.repository.SemesterSummaryProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("GetSemesterListService 단위 테스트")
@ExtendWith(MockitoExtension.class)
class GetSemesterListServiceTest {

    @InjectMocks
    private GetSemesterListService getSemesterListService;

    @Mock
    private SemesterMemberRepository semesterMemberRepository;

    private SemesterSummaryProjection createSummaryProjection(int year, int semester, long memberCount) {
        SemesterSummaryProjection projection = mock(SemesterSummaryProjection.class);
        given(projection.getSemesterYear()).willReturn(year);
        given(projection.getSemesterTerm()).willReturn(semester);
        given(projection.getMemberCount()).willReturn(memberCount);
        return projection;
    }

    @DisplayName("리포지토리 결과를 올바른 매핑으로 학기 목록을 반환한다")
    @Test
    void getSemesterList_repositoryHasData_returnsSemesterListWithCorrectMapping() {
        // given
        SemesterSummaryProjection p1 = createSummaryProjection(2026, 1, 30L);
        SemesterSummaryProjection p2 = createSummaryProjection(2025, 2, 25L);

        given(semesterMemberRepository.findDistinctSemestersWithCount())
                .willReturn(List.of(p1, p2));

        // when
        List<SemesterSummaryResponse> result = getSemesterListService.getSemesterList();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).year()).isEqualTo(2026);
        assertThat(result.get(0).semester()).isEqualTo(1);
        assertThat(result.get(0).memberCount()).isEqualTo(30L);
        assertThat(result.get(0).displayName()).isEqualTo("2026년 1학기");
        assertThat(result.get(1).year()).isEqualTo(2025);
        assertThat(result.get(1).semester()).isEqualTo(2);
        assertThat(result.get(1).memberCount()).isEqualTo(25L);
    }

    @DisplayName("리포지토리에 데이터가 없으면 빈 리스트를 반환한다")
    @Test
    void getSemesterList_emptyRepository_returnsEmptyList() {
        // given
        given(semesterMemberRepository.findDistinctSemestersWithCount())
                .willReturn(Collections.emptyList());

        // when
        List<SemesterSummaryResponse> result = getSemesterListService.getSemesterList();

        // then
        assertThat(result).isEmpty();
    }
}
