package igrus.web.user.semester.service.read;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.semester.dto.response.CandidateMemberResponse;
import igrus.web.user.semester.exception.InvalidSemesterException;
import igrus.web.user.semester.service.write.RegisterSemesterMembersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GetCandidateMembersService 통합 테스트")
class GetCandidateMembersServiceIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetCandidateMembersService getCandidateMembersService;

    @Autowired
    private RegisterSemesterMembersService registerSemesterMembersService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    @Nested
    @DisplayName("US1: 등록 후보 조회")
    class Registration {

        @Test
        @DisplayName("등록 후보 회원 목록 조회 시 등록 상태가 올바르게 표시된다")
        void getCandidateMembers_afterPartialRegistration_showsCorrectStatus() {
            // given
            User registered = createAndSaveUser("11111111", "reg@inha.edu", UserRole.MEMBER);
            User notRegistered = createAndSaveUser("22222222", "noreg@inha.edu", UserRole.MEMBER);
            registerSemesterMembersService.registerMembers(2026, 1, List.of(registered.getId()));

            // when
            List<CandidateMemberResponse> candidates = getCandidateMembersService.getCandidateMembers(2026, 1);

            // then
            assertThat(candidates).hasSize(2);

            CandidateMemberResponse registeredCandidate = candidates.stream()
                    .filter(c -> c.userId().equals(registered.getId()))
                    .findFirst().orElseThrow();
            assertThat(registeredCandidate.alreadyRegistered()).isTrue();

            CandidateMemberResponse notRegisteredCandidate = candidates.stream()
                    .filter(c -> c.userId().equals(notRegistered.getId()))
                    .findFirst().orElseThrow();
            assertThat(notRegisteredCandidate.alreadyRegistered()).isFalse();
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("getCandidateMembers에서도 유효하지 않은 학기 시 InvalidSemesterException이 발생한다")
        void getCandidateMembers_withInvalidSemester_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> getCandidateMembersService.getCandidateMembers(2026, 0))
                    .isInstanceOf(InvalidSemesterException.class);
        }
    }
}
