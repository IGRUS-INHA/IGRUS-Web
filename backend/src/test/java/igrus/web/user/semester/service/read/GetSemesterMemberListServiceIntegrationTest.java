package igrus.web.user.semester.service.read;

import igrus.web.common.ServiceIntegrationTestBase;
import igrus.web.user.domain.User;
import igrus.web.user.domain.UserRole;
import igrus.web.user.semester.dto.response.SemesterMemberListResponse;
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

@DisplayName("GetSemesterMemberListService 통합 테스트")
class GetSemesterMemberListServiceIntegrationTest extends ServiceIntegrationTestBase {

    @Autowired
    private GetSemesterMemberListService getSemesterMemberListService;

    @Autowired
    private RegisterSemesterMembersService registerSemesterMembersService;

    @BeforeEach
    void setUp() {
        setUpBase();
    }

    private User createAndSaveUserWithName(String studentId, String email, UserRole role, String name) {
        User user = createAndSaveUser(studentId, email, role);
        setField(user, "name", name);
        return userRepository.save(user);
    }

    @Nested
    @DisplayName("US3: 명단 조회")
    class Query {

        @Test
        @DisplayName("회원 명단 조회 시 탈퇴한 사용자도 포함된다")
        void getMemberList_withWithdrawnUser_includesWithdrawnUser() {
            // given
            User activeUser = createAndSaveUser("11111111", "active@inha.edu", UserRole.MEMBER);
            User withdrawnUser = createAndSaveUser("22222222", "withdrawn@inha.edu", UserRole.MEMBER);

            registerSemesterMembersService.registerMembers(2026, 1,
                    List.of(activeUser.getId(), withdrawnUser.getId()));

            // 등록 이후에 탈퇴 처리 (native query로 직접 업데이트)
            transactionTemplate.execute(status -> {
                entityManager.createNativeQuery(
                        "UPDATE users SET users_status = 'WITHDRAWN', users_deleted = true, users_deleted_at = NOW() " +
                                "WHERE users_id = :userId")
                        .setParameter("userId", withdrawnUser.getId())
                        .executeUpdate();
                entityManager.flush();
                entityManager.clear();
                return null;
            });

            // when
            List<SemesterMemberListResponse> members = getSemesterMemberListService.getMemberList(2026, 1, null);

            // then
            assertThat(members).hasSize(2);

            SemesterMemberListResponse withdrawn = members.stream()
                    .filter(m -> m.userId().equals(withdrawnUser.getId()))
                    .findFirst().orElseThrow();
            assertThat(withdrawn.isWithdrawn()).isTrue();

            SemesterMemberListResponse active = members.stream()
                    .filter(m -> m.userId().equals(activeUser.getId()))
                    .findFirst().orElseThrow();
            assertThat(active.isWithdrawn()).isFalse();
        }

        @Test
        @DisplayName("회원 명단 조회 시 이름 키워드로 필터링된다")
        void getMemberList_withNameKeyword_filtersCorrectly() {
            // given
            User kim = createAndSaveUserWithName("11111111", "kim@inha.edu", UserRole.MEMBER, "김철수");
            User lee = createAndSaveUserWithName("22222222", "lee@inha.edu", UserRole.MEMBER, "이영희");
            User park = createAndSaveUserWithName("33333333", "park@inha.edu", UserRole.MEMBER, "박철수");

            registerSemesterMembersService.registerMembers(2026, 1,
                    List.of(kim.getId(), lee.getId(), park.getId()));

            // when
            List<SemesterMemberListResponse> results = getSemesterMemberListService.getMemberList(2026, 1, "철수");

            // then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(SemesterMemberListResponse::name)
                    .containsExactlyInAnyOrder("김철수", "박철수");
        }

        @Test
        @DisplayName("회원 명단 조회 시 학번 키워드로 필터링된다")
        void getMemberList_withStudentIdKeyword_filtersCorrectly() {
            // given
            User user1 = createAndSaveUser("12345678", "user1@inha.edu", UserRole.MEMBER);
            User user2 = createAndSaveUser("22222222", "user2@inha.edu", UserRole.MEMBER);
            User user3 = createAndSaveUser("12340000", "user3@inha.edu", UserRole.MEMBER);

            registerSemesterMembersService.registerMembers(2026, 1,
                    List.of(user1.getId(), user2.getId(), user3.getId()));

            // when
            List<SemesterMemberListResponse> results = getSemesterMemberListService.getMemberList(2026, 1, "1234");

            // then
            assertThat(results).hasSize(2);
            assertThat(results).extracting(SemesterMemberListResponse::studentId)
                    .containsExactlyInAnyOrder("12345678", "12340000");
        }
    }

    @Nested
    @DisplayName("Validation")
    class Validation {

        @Test
        @DisplayName("getMemberList에서도 유효하지 않은 연도 시 InvalidSemesterException이 발생한다")
        void getMemberList_withInvalidYear_throwsInvalidSemesterException() {
            assertThatThrownBy(() -> getSemesterMemberListService.getMemberList(1999, 1, null))
                    .isInstanceOf(InvalidSemesterException.class);
        }
    }
}
