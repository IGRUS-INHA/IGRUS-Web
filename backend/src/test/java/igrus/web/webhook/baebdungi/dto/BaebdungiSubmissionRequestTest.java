package igrus.web.webhook.baebdungi.dto;

import igrus.web.user.domain.Gender;
import igrus.web.user.domain.EnrollmentStatus;
import igrus.web.user.domain.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BaebdungiSubmissionRequest 단위 테스트")
class BaebdungiSubmissionRequestTest {

    @Test
    @DisplayName("User 엔티티로부터 올바르게 변환된다")
    void fromUser_ConvertsAllFields() {
        // given
        User user = User.create(
                "20231234", "홍길동", "test@inha.edu",
                "010-1234-5678", "컴퓨터공학과", "동기",
                List.of(), Gender.MALE, 2,
                EnrollmentStatus.ENROLLED,
                List.of(), null, null, null
        );
        Instant createdAt = Instant.parse("2025-03-15T10:30:00Z");
        ReflectionTestUtils.setField(user, "createdAt", createdAt);

        // when
        BaebdungiSubmissionRequest request = BaebdungiSubmissionRequest.fromUser(user);

        // then
        assertThat(request.name()).isEqualTo("홍길동");
        assertThat(request.studentId()).isEqualTo("20231234");
        assertThat(request.email()).isEqualTo("test@inha.edu");
        assertThat(request.department()).isEqualTo("컴퓨터공학과");
        assertThat(request.phone()).isEqualTo("010-1234-5678");
        assertThat(request.gender()).isEqualTo("남");
        assertThat(request.grade()).isEqualTo("2학년");
        assertThat(request.enrollmentStatus()).isEqualTo("재학");
        assertThat(request.hasPaid()).isEqualTo("네");
        assertThat(request.submittedAt()).isEqualTo("2025-03-15T10:30:00Z");
    }

    @Test
    @DisplayName("FEMALE 성별이 '여'로 변환된다")
    void fromUser_FemaleGender_ConvertedCorrectly() {
        // given
        User user = User.create(
                "20231235", "김영희", "test2@inha.edu",
                "010-5678-1234", "소프트웨어학과", null,
                List.of(), Gender.FEMALE, 1,
                EnrollmentStatus.ENROLLED,
                List.of(), null, null, null
        );

        // when
        BaebdungiSubmissionRequest request = BaebdungiSubmissionRequest.fromUser(user);

        // then
        assertThat(request.gender()).isEqualTo("여");
        assertThat(request.grade()).isEqualTo("1학년");
    }

    @Test
    @DisplayName("GENERAL_LEAVE가 '휴학(일반)'으로 변환된다")
    void fromUser_GeneralLeave_ConvertedCorrectly() {
        // given
        User user = User.create(
                "20231237", "박민수", "test4@inha.edu",
                "010-1111-2222", "컴퓨터공학과", null,
                List.of(), Gender.MALE, 2,
                EnrollmentStatus.GENERAL_LEAVE,
                List.of(), null, null, null
        );

        // when
        BaebdungiSubmissionRequest request = BaebdungiSubmissionRequest.fromUser(user);

        // then
        assertThat(request.enrollmentStatus()).isEqualTo("휴학(일반)");
    }

    @Test
    @DisplayName("MILITARY_LEAVE가 '휴학(군)'으로 변환된다")
    void fromUser_MilitaryLeave_ConvertedCorrectly() {
        // given
        User user = User.create(
                "20231238", "최대한", "test5@inha.edu",
                "010-3333-4444", "정보통신공학과", null,
                List.of(), Gender.MALE, 3,
                EnrollmentStatus.MILITARY_LEAVE,
                List.of(), null, null, null
        );

        // when
        BaebdungiSubmissionRequest request = BaebdungiSubmissionRequest.fromUser(user);

        // then
        assertThat(request.enrollmentStatus()).isEqualTo("휴학(군)");
    }

    @Test
    @DisplayName("createdAt이 null이면 submittedAt도 null이다")
    void fromUser_NullCreatedAt_ReturnsNullSubmittedAt() {
        // given
        User user = User.create(
                "20231236", "이철수", "test3@inha.edu",
                null, "정보통신공학과", null,
                List.of(), Gender.MALE, 3,
                EnrollmentStatus.ENROLLED,
                List.of(), null, null, null
        );
        // createdAt은 @CreatedDate로 자동 설정되므로 테스트에서는 null 상태

        // when
        BaebdungiSubmissionRequest request = BaebdungiSubmissionRequest.fromUser(user);

        // then
        assertThat(request.submittedAt()).isNull();
        assertThat(request.phone()).isNull();
    }
}
