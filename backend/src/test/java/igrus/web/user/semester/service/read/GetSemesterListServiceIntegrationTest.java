package igrus.web.user.semester.service.read;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.semester.dto.response.SemesterSummaryResponse;
import igrus.web.user.semester.service.write.RegisterSemesterMembersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GetSemesterListService 통합 테스트")
class GetSemesterListServiceIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetSemesterListService getSemesterListService;

    @Autowired
    private RegisterSemesterMembersService registerSemesterMembersService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Test
    @DisplayName("학기 목록 조회 시 연도/학기/회원 수가 올바르게 반환된다")
    void getSemesterList_withMultipleSemesters_returnsCorrectSummary() {
        // given
        User user1 = createAndSaveUser("11111111", "user1@inha.edu", UserRole.MEMBER);
        User user2 = createAndSaveUser("22222222", "user2@inha.edu", UserRole.MEMBER);
        User user3 = createAndSaveUser("33333333", "user3@inha.edu", UserRole.OPERATOR);

        registerSemesterMembersService.registerMembers(2025, 2, List.of(user1.getId(), user2.getId()));
        registerSemesterMembersService.registerMembers(2026, 1, List.of(user1.getId(), user2.getId(), user3.getId()));

        // when
        List<SemesterSummaryResponse> semesters = getSemesterListService.getSemesterList();

        // then
        assertThat(semesters).hasSize(2);

        SemesterSummaryResponse first = semesters.get(0);
        assertThat(first.year()).isEqualTo(2026);
        assertThat(first.semester()).isEqualTo(1);
        assertThat(first.memberCount()).isEqualTo(3);
        assertThat(first.displayName()).isEqualTo("2026년 1학기");

        SemesterSummaryResponse second = semesters.get(1);
        assertThat(second.year()).isEqualTo(2025);
        assertThat(second.semester()).isEqualTo(2);
        assertThat(second.memberCount()).isEqualTo(2);
    }
}
