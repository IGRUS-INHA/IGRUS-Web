package igrus.web.user.domain;

import igrus.web.user.domain.Gender;
import igrus.web.user.exception.SameRoleChangeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserRoleHistory 도메인")
class UserRoleHistoryTest {

    private User createTestUser() {
        return User.create("20231234", "홍길동", "test@inha.edu", "010-1234-5678", "컴퓨터공학과", "테스트 동기", Gender.MALE, 1);
    }

    @Nested
    @DisplayName("create 메서드")
    class CreateTest {

        @Test
        @DisplayName("유효한 역할 변경 시 UserRoleHistory 생성 성공")
        void create_WithValidRoleChange_ReturnsUserRoleHistory() {
            // given
            User user = createTestUser();
            UserRole previousRole = UserRole.ASSOCIATE;
            UserRole newRole = UserRole.MEMBER;
            String reason = "정회원 승인";

            // when
            UserRoleHistory history = UserRoleHistory.create(user, previousRole, newRole, reason);

            // then
            assertThat(history).isNotNull();
            assertThat(history.getUser()).isEqualTo(user);
            assertThat(history.getPreviousRole()).isEqualTo(previousRole);
            assertThat(history.getNewRole()).isEqualTo(newRole);
            assertThat(history.getReason()).isEqualTo(reason);
        }

        @Test
        @DisplayName("이전 역할과 새 역할이 동일하면 예외 발생")
        void create_WithSameRole_ThrowsException() {
            // given
            User user = createTestUser();
            UserRole sameRole = UserRole.MEMBER;

            // when & then
            assertThatThrownBy(() -> UserRoleHistory.create(user, sameRole, sameRole, "사유"))
                    .isInstanceOf(SameRoleChangeException.class)
                    .hasMessageContaining("이전 역할과 새 역할이 동일합니다");
        }

        @Test
        @DisplayName("사유 없이도 생성 가능")
        void create_WithoutReason_ReturnsUserRoleHistory() {
            // given
            User user = createTestUser();

            // when
            UserRoleHistory history = UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, null);

            // then
            assertThat(history).isNotNull();
            assertThat(history.getReason()).isNull();
        }
    }

    @Nested
    @DisplayName("isPromotion 메서드")
    class IsPromotionTest {

        @Test
        @DisplayName("상위 역할로 변경 시 true 반환")
        void isPromotion_WhenHigherRole_ReturnsTrue() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, null).isPromotion()).isTrue();
            assertThat(UserRoleHistory.create(user, UserRole.MEMBER, UserRole.OPERATOR, null).isPromotion()).isTrue();
            assertThat(UserRoleHistory.create(user, UserRole.OPERATOR, UserRole.ADMIN, null).isPromotion()).isTrue();
            assertThat(UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.ADMIN, null).isPromotion()).isTrue();
        }

        @Test
        @DisplayName("하위 역할로 변경 시 false 반환")
        void isPromotion_WhenLowerRole_ReturnsFalse() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(UserRoleHistory.create(user, UserRole.MEMBER, UserRole.ASSOCIATE, null).isPromotion()).isFalse();
            assertThat(UserRoleHistory.create(user, UserRole.ADMIN, UserRole.OPERATOR, null).isPromotion()).isFalse();
        }
    }

    @Nested
    @DisplayName("isDemotion 메서드")
    class IsDemotionTest {

        @Test
        @DisplayName("하위 역할로 변경 시 true 반환")
        void isDemotion_WhenLowerRole_ReturnsTrue() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(UserRoleHistory.create(user, UserRole.MEMBER, UserRole.ASSOCIATE, null).isDemotion()).isTrue();
            assertThat(UserRoleHistory.create(user, UserRole.OPERATOR, UserRole.MEMBER, null).isDemotion()).isTrue();
            assertThat(UserRoleHistory.create(user, UserRole.ADMIN, UserRole.ASSOCIATE, null).isDemotion()).isTrue();
        }

        @Test
        @DisplayName("상위 역할로 변경 시 false 반환")
        void isDemotion_WhenHigherRole_ReturnsFalse() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, null).isDemotion()).isFalse();
            assertThat(UserRoleHistory.create(user, UserRole.MEMBER, UserRole.ADMIN, null).isDemotion()).isFalse();
        }
    }

    @Nested
    @DisplayName("isPromotionToAdmin 메서드")
    class IsPromotionToAdminTest {

        @Test
        @DisplayName("관리자로 승급 시 true 반환")
        void isPromotionToAdmin_WhenPromotedToAdmin_ReturnsTrue() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.ADMIN, null).isPromotionToAdmin()).isTrue();
            assertThat(UserRoleHistory.create(user, UserRole.MEMBER, UserRole.ADMIN, null).isPromotionToAdmin()).isTrue();
            assertThat(UserRoleHistory.create(user, UserRole.OPERATOR, UserRole.ADMIN, null).isPromotionToAdmin()).isTrue();
        }

        @Test
        @DisplayName("관리자가 아닌 역할로 변경 시 false 반환")
        void isPromotionToAdmin_WhenNotPromotedToAdmin_ReturnsFalse() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, null).isPromotionToAdmin()).isFalse();
            assertThat(UserRoleHistory.create(user, UserRole.MEMBER, UserRole.OPERATOR, null).isPromotionToAdmin()).isFalse();
        }
    }

    @Nested
    @DisplayName("isPromotionToOperator 메서드")
    class IsPromotionToOperatorTest {

        @Test
        @DisplayName("운영진으로 승급 시 true 반환")
        void isPromotionToOperator_WhenPromotedToOperator_ReturnsTrue() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.OPERATOR, null).isPromotionToOperator()).isTrue();
            assertThat(UserRoleHistory.create(user, UserRole.MEMBER, UserRole.OPERATOR, null).isPromotionToOperator()).isTrue();
        }

        @Test
        @DisplayName("관리자에서 운영진으로 강등 시 false 반환")
        void isPromotionToOperator_WhenDemotedFromAdmin_ReturnsFalse() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(UserRoleHistory.create(user, UserRole.ADMIN, UserRole.OPERATOR, null).isPromotionToOperator()).isFalse();
        }

        @Test
        @DisplayName("운영진이 아닌 역할로 변경 시 false 반환")
        void isPromotionToOperator_WhenNotOperator_ReturnsFalse() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, null).isPromotionToOperator()).isFalse();
            assertThat(UserRoleHistory.create(user, UserRole.MEMBER, UserRole.ADMIN, null).isPromotionToOperator()).isFalse();
        }
    }

    @Nested
    @DisplayName("isPromotionToMember 메서드")
    class IsPromotionToMemberTest {

        @Test
        @DisplayName("준회원에서 정회원으로 승급 시 true 반환")
        void isPromotionToMember_WhenAssociateToMember_ReturnsTrue() {
            // given
            User user = createTestUser();

            // when
            UserRoleHistory history = UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, null);

            // then
            assertThat(history.isPromotionToMember()).isTrue();
        }

        @Test
        @DisplayName("준회원 외의 역할에서 정회원으로 변경 시 false 반환")
        void isPromotionToMember_WhenNotFromAssociate_ReturnsFalse() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(UserRoleHistory.create(user, UserRole.OPERATOR, UserRole.MEMBER, null).isPromotionToMember()).isFalse();
            assertThat(UserRoleHistory.create(user, UserRole.ADMIN, UserRole.MEMBER, null).isPromotionToMember()).isFalse();
        }

        @Test
        @DisplayName("정회원이 아닌 역할로 변경 시 false 반환")
        void isPromotionToMember_WhenNotToMember_ReturnsFalse() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.OPERATOR, null).isPromotionToMember()).isFalse();
        }
    }

    @Nested
    @DisplayName("isDemotionFromAdmin 메서드")
    class IsDemotionFromAdminTest {

        @Test
        @DisplayName("관리자에서 강등 시 true 반환")
        void isDemotionFromAdmin_WhenDemotedFromAdmin_ReturnsTrue() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(UserRoleHistory.create(user, UserRole.ADMIN, UserRole.OPERATOR, null).isDemotionFromAdmin()).isTrue();
            assertThat(UserRoleHistory.create(user, UserRole.ADMIN, UserRole.MEMBER, null).isDemotionFromAdmin()).isTrue();
            assertThat(UserRoleHistory.create(user, UserRole.ADMIN, UserRole.ASSOCIATE, null).isDemotionFromAdmin()).isTrue();
        }

        @Test
        @DisplayName("관리자가 아닌 역할에서 변경 시 false 반환")
        void isDemotionFromAdmin_WhenNotFromAdmin_ReturnsFalse() {
            // given
            User user = createTestUser();

            // when & then
            assertThat(UserRoleHistory.create(user, UserRole.OPERATOR, UserRole.MEMBER, null).isDemotionFromAdmin()).isFalse();
            assertThat(UserRoleHistory.create(user, UserRole.MEMBER, UserRole.ASSOCIATE, null).isDemotionFromAdmin()).isFalse();
        }
    }

    @Nested
    @DisplayName("isChangeTo 메서드")
    class IsChangeToTest {

        @Test
        @DisplayName("지정한 역할로 변경 시 true 반환")
        void isChangeTo_WhenMatchingTargetRole_ReturnsTrue() {
            // given
            User user = createTestUser();
            UserRoleHistory history = UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, null);

            // when & then
            assertThat(history.isChangeTo(UserRole.MEMBER)).isTrue();
        }

        @Test
        @DisplayName("다른 역할로 변경 시 false 반환")
        void isChangeTo_WhenNotMatchingTargetRole_ReturnsFalse() {
            // given
            User user = createTestUser();
            UserRoleHistory history = UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, null);

            // when & then
            assertThat(history.isChangeTo(UserRole.OPERATOR)).isFalse();
            assertThat(history.isChangeTo(UserRole.ADMIN)).isFalse();
        }
    }

    @Nested
    @DisplayName("isChangeFrom 메서드")
    class IsChangeFromTest {

        @Test
        @DisplayName("지정한 역할에서 변경 시 true 반환")
        void isChangeFrom_WhenMatchingSourceRole_ReturnsTrue() {
            // given
            User user = createTestUser();
            UserRoleHistory history = UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, null);

            // when & then
            assertThat(history.isChangeFrom(UserRole.ASSOCIATE)).isTrue();
        }

        @Test
        @DisplayName("다른 역할에서 변경 시 false 반환")
        void isChangeFrom_WhenNotMatchingSourceRole_ReturnsFalse() {
            // given
            User user = createTestUser();
            UserRoleHistory history = UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, null);

            // when & then
            assertThat(history.isChangeFrom(UserRole.MEMBER)).isFalse();
            assertThat(history.isChangeFrom(UserRole.OPERATOR)).isFalse();
        }
    }

    @Nested
    @DisplayName("updateReason 메서드")
    class UpdateReasonTest {

        @Test
        @DisplayName("사유 업데이트 성공")
        void updateReason_WithNewReason_UpdatesReason() {
            // given
            User user = createTestUser();
            UserRoleHistory history = UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, "초기 사유");

            // when
            history.updateReason("업데이트된 사유");

            // then
            assertThat(history.getReason()).isEqualTo("업데이트된 사유");
        }

        @Test
        @DisplayName("null로 사유 업데이트 가능")
        void updateReason_WithNull_UpdatesReasonToNull() {
            // given
            User user = createTestUser();
            UserRoleHistory history = UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, "초기 사유");

            // when
            history.updateReason(null);

            // then
            assertThat(history.getReason()).isNull();
        }
    }

    @Nested
    @DisplayName("hasReason 메서드")
    class HasReasonTest {

        @Test
        @DisplayName("사유가 존재하면 true 반환")
        void hasReason_WhenReasonExists_ReturnsTrue() {
            // given
            User user = createTestUser();
            UserRoleHistory history = UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, "정회원 승인");

            // when & then
            assertThat(history.hasReason()).isTrue();
        }

        @Test
        @DisplayName("사유가 null이면 false 반환")
        void hasReason_WhenReasonIsNull_ReturnsFalse() {
            // given
            User user = createTestUser();
            UserRoleHistory history = UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, null);

            // when & then
            assertThat(history.hasReason()).isFalse();
        }

        @Test
        @DisplayName("사유가 빈 문자열이면 false 반환")
        void hasReason_WhenReasonIsEmpty_ReturnsFalse() {
            // given
            User user = createTestUser();
            UserRoleHistory history = UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, "");

            // when & then
            assertThat(history.hasReason()).isFalse();
        }

        @Test
        @DisplayName("사유가 공백만 있으면 false 반환")
        void hasReason_WhenReasonIsBlank_ReturnsFalse() {
            // given
            User user = createTestUser();
            UserRoleHistory history = UserRoleHistory.create(user, UserRole.ASSOCIATE, UserRole.MEMBER, "   ");

            // when & then
            assertThat(history.hasReason()).isFalse();
        }
    }
}
