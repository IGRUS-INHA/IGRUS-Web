package igrus.web.survey.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static igrus.web.common.fixture.SurveyTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Survey 엔티티 테스트")
class SurveyTest {

    @Nested
    @DisplayName("생성")
    class Create {

        @DisplayName("설문 생성 시 초기 상태 UNPUBLISHED + NOT_STARTED")
        @Test
        void create_InitialState_UnpublishedAndNotStarted() {
            // when
            Survey survey = createSurvey();

            // then
            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.UNPUBLISHED);
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.NOT_STARTED);
            assertThat(survey.isTrashed()).isFalse();
        }

        @DisplayName("설문 생성 시 모든 필드 정상 설정")
        @Test
        void create_AllFields_SetCorrectly() {
            // given
            Instant deadline = Instant.now().plusSeconds(86400);

            // when
            Survey survey = Survey.create("제목", "설명", SurveyAccessLevel.MEMBER, deadline);

            // then
            assertThat(survey.getTitle()).isEqualTo("제목");
            assertThat(survey.getDescription()).isEqualTo("설명");
            assertThat(survey.getAccessLevel()).isEqualTo(SurveyAccessLevel.MEMBER);
            assertThat(survey.getDeadline()).isEqualTo(deadline);
        }
    }

    @Nested
    @DisplayName("공개 상태 전이")
    class VisibilityTransition {

        @DisplayName("publish: UNPUBLISHED -> PUBLISHED 성공")
        @Test
        void publish_FromUnpublished_Success() {
            // given
            Survey survey = createSurvey();

            // when
            survey.publish();

            // then
            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.PUBLISHED);
        }

        @DisplayName("publish: 이미 PUBLISHED면 예외")
        @Test
        void publish_AlreadyPublished_ThrowsException() {
            // given
            Survey survey = createPublishedSurvey();

            // when & then
            assertThatThrownBy(survey::publish)
                    .isInstanceOf(IllegalStateException.class);
        }

        @DisplayName("unpublish: PUBLISHED -> UNPUBLISHED 성공")
        @Test
        void unpublish_FromPublished_Success() {
            // given
            Survey survey = createPublishedSurvey();

            // when
            survey.unpublish();

            // then
            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.UNPUBLISHED);
        }

        @DisplayName("unpublish: OPEN 상태에서 비공개 시 자동 CLOSED (INV-20)")
        @Test
        void unpublish_WhenOpen_AutoClosesResponse() {
            // given
            Survey survey = createPublishedAndOpenSurvey();

            // when
            survey.unpublish();

            // then
            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.UNPUBLISHED);
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("응답 수집 상태 전이")
    class ResponseStatusTransition {

        @DisplayName("openResponse: NOT_STARTED -> OPEN 성공 (PUBLISHED 상태)")
        @Test
        void openResponse_FromNotStarted_Success() {
            // given
            Survey survey = createPublishedSurvey();

            // when
            survey.openResponse();

            // then
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @DisplayName("openResponse: CLOSED -> OPEN 재개 성공")
        @Test
        void openResponse_FromClosed_ResumeSuccess() {
            // given
            Survey survey = createClosedSurvey();

            // when
            survey.openResponse();

            // then
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @DisplayName("openResponse: UNPUBLISHED에서 호출 시 예외")
        @Test
        void openResponse_WhenUnpublished_ThrowsException() {
            // given
            Survey survey = createSurvey();

            // when & then
            assertThatThrownBy(survey::openResponse)
                    .isInstanceOf(IllegalStateException.class);
        }

        @DisplayName("openResponse: 이미 OPEN이면 예외")
        @Test
        void openResponse_AlreadyOpen_ThrowsException() {
            // given
            Survey survey = createPublishedAndOpenSurvey();

            // when & then
            assertThatThrownBy(survey::openResponse)
                    .isInstanceOf(IllegalStateException.class);
        }

        @DisplayName("openResponse: 마감일 경과 시 예외")
        @Test
        void openResponse_DeadlinePassed_ThrowsException() {
            // given
            Survey survey = createPublishedSurvey();
            ReflectionTestUtils.setField(survey, "deadline", Instant.now().minusSeconds(3600));

            // when & then
            assertThatThrownBy(survey::openResponse)
                    .isInstanceOf(IllegalStateException.class);
        }

        @DisplayName("closeResponse: OPEN -> CLOSED 성공")
        @Test
        void closeResponse_FromOpen_Success() {
            // given
            Survey survey = createPublishedAndOpenSurvey();

            // when
            survey.closeResponse();

            // then
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.CLOSED);
        }
    }

    @Nested
    @DisplayName("편의 메서드")
    class ConvenienceMethods {

        @DisplayName("publishAndOpen: UNPUBLISHED에서 PUBLISHED+OPEN 한 번에 전이")
        @Test
        void publishAndOpen_FromUnpublished_Success() {
            // given
            Survey survey = createSurvey();

            // when
            survey.publishAndOpen();

            // then
            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.PUBLISHED);
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @DisplayName("publishAndOpen: 이미 PUBLISHED면 예외")
        @Test
        void publishAndOpen_AlreadyPublished_ThrowsException() {
            // given
            Survey survey = createPublishedSurvey();

            // when & then
            assertThatThrownBy(survey::publishAndOpen)
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("수정")
    class Update {

        @DisplayName("UNPUBLISHED 상태에서 수정 가능")
        @Test
        void update_WhenUnpublished_Success() {
            // given
            Survey survey = createSurvey();
            Instant newDeadline = Instant.now().plusSeconds(86400);

            // when
            survey.update("수정 제목", "수정 설명", SurveyAccessLevel.MEMBER, newDeadline);

            // then
            assertThat(survey.getTitle()).isEqualTo("수정 제목");
            assertThat(survey.getDescription()).isEqualTo("수정 설명");
            assertThat(survey.getAccessLevel()).isEqualTo(SurveyAccessLevel.MEMBER);
            assertThat(survey.getDeadline()).isEqualTo(newDeadline);
        }

        @DisplayName("PUBLISHED 상태에서도 수정 가능")
        @Test
        void update_WhenPublished_Success() {
            // given
            Survey survey = createPublishedSurvey();

            // when
            survey.update("공개 중 수정", "공개 중 설명 수정", SurveyAccessLevel.ASSOCIATE, null);

            // then
            assertThat(survey.getTitle()).isEqualTo("공개 중 수정");
            assertThat(survey.getAccessLevel()).isEqualTo(SurveyAccessLevel.ASSOCIATE);
        }
    }

    @Nested
    @DisplayName("휴지통")
    class Trash {

        @DisplayName("trash: 활성 설문 휴지통 이동 성공")
        @Test
        void trash_ActiveSurvey_Success() {
            // given
            Survey survey = createSurvey();

            // when
            survey.trash();

            // then
            assertThat(survey.isTrashed()).isTrue();
            assertThat(survey.getTrashedAt()).isNotNull();
        }

        @DisplayName("trash: 이미 휴지통이면 예외")
        @Test
        void trash_AlreadyTrashed_ThrowsException() {
            // given
            Survey survey = createTrashedSurvey();

            // when & then
            assertThatThrownBy(survey::trash)
                    .isInstanceOf(IllegalStateException.class);
        }

        @DisplayName("restoreFromTrash: 복원 성공")
        @Test
        void restoreFromTrash_TrashedSurvey_Success() {
            // given
            Survey survey = createTrashedSurvey();

            // when
            survey.restoreFromTrash();

            // then
            assertThat(survey.isTrashed()).isFalse();
            assertThat(survey.getTrashedAt()).isNull();
        }

        @DisplayName("restoreFromTrash: 활성 설문이면 예외")
        @Test
        void restoreFromTrash_ActiveSurvey_ThrowsException() {
            // given
            Survey survey = createSurvey();

            // when & then
            assertThatThrownBy(survey::restoreFromTrash)
                    .isInstanceOf(IllegalStateException.class);
        }

        @DisplayName("permanentDelete: 휴지통 -> 영구 삭제 성공")
        @Test
        void permanentDelete_TrashedSurvey_Success() {
            // given
            Survey survey = createTrashedSurvey();

            // when
            survey.permanentDelete(1L);

            // then
            assertThat(survey.isDeleted()).isTrue();
        }

        @DisplayName("permanentDelete: 활성 설문이면 예외 (INV-18)")
        @Test
        void permanentDelete_ActiveSurvey_ThrowsException() {
            // given
            Survey survey = createSurvey();

            // when & then
            assertThatThrownBy(() -> survey.permanentDelete(1L))
                    .isInstanceOf(IllegalStateException.class);
        }

        @DisplayName("isTrashed: trashedAt 유무에 따라 정상 반환")
        @Test
        void isTrashed_ReturnsCorrectValue() {
            // given
            Survey activeSurvey = createSurvey();
            Survey trashedSurvey = createTrashedSurvey();

            // then
            assertThat(activeSurvey.isTrashed()).isFalse();
            assertThat(trashedSurvey.isTrashed()).isTrue();
        }
    }

    @Nested
    @DisplayName("상태 조회")
    class StateQuery {

        @DisplayName("isUnpublished: UNPUBLISHED일 때 true")
        @Test
        void isUnpublished_WhenUnpublished_ReturnsTrue() {
            Survey survey = createSurvey();
            assertThat(survey.isUnpublished()).isTrue();
        }

        @DisplayName("isPublished: PUBLISHED일 때 true")
        @Test
        void isPublished_WhenPublished_ReturnsTrue() {
            Survey survey = createPublishedSurvey();
            assertThat(survey.isPublished()).isTrue();
        }

        @DisplayName("isResponseOpen: OPEN일 때 true")
        @Test
        void isResponseOpen_WhenOpen_ReturnsTrue() {
            Survey survey = createPublishedAndOpenSurvey();
            assertThat(survey.isResponseOpen()).isTrue();
        }

        @DisplayName("isAcceptingResponses: PUBLISHED+OPEN+비휴지통이면 true")
        @Test
        void isAcceptingResponses_WhenAllConditionsMet_ReturnsTrue() {
            Survey survey = createPublishedAndOpenSurvey();
            assertThat(survey.isAcceptingResponses()).isTrue();
        }

        @DisplayName("isAcceptingResponses: 조건 미충족 시 false (UNPUBLISHED)")
        @Test
        void isAcceptingResponses_WhenUnpublished_ReturnsFalse() {
            Survey unpublished = createSurvey();
            assertThat(unpublished.isAcceptingResponses()).isFalse();
        }

        @DisplayName("isAcceptingResponses: 조건 미충족 시 false (휴지통)")
        @Test
        void isAcceptingResponses_WhenTrashed_ReturnsFalse() {
            Survey survey = createPublishedAndOpenSurvey();
            ReflectionTestUtils.setField(survey, "trashedAt", Instant.now());
            assertThat(survey.isAcceptingResponses()).isFalse();
        }

        @DisplayName("isAcceptingResponses: 조건 미충족 시 false (CLOSED)")
        @Test
        void isAcceptingResponses_WhenClosed_ReturnsFalse() {
            Survey survey = createClosedSurvey();
            assertThat(survey.isAcceptingResponses()).isFalse();
        }
    }
}
