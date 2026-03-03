package igrus.web.survey.domain;

import igrus.web.survey.exception.SurveyAlreadyTrashedException;
import igrus.web.survey.exception.SurveyInvalidStateTransitionException;
import igrus.web.survey.exception.SurveyNotTrashedException;
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

        @DisplayName("SRV-009: 설명 null 생성 성공 (선택 필드)")
        @Test
        void create_DescriptionNull_Success() {
            Survey survey = Survey.create("제목", null, SurveyAccessLevel.PUBLIC, null);
            assertThat(survey.getDescription()).isNull();
        }

        @DisplayName("SRV-012: accessLevel PUBLIC 생성 성공")
        @Test
        void create_AccessLevelPublic_Success() {
            Survey survey = Survey.create("제목", "설명", SurveyAccessLevel.PUBLIC, null);
            assertThat(survey.getAccessLevel()).isEqualTo(SurveyAccessLevel.PUBLIC);
        }

        @DisplayName("SRV-013: accessLevel ASSOCIATE 생성 성공")
        @Test
        void create_AccessLevelAssociate_Success() {
            Survey survey = Survey.create("제목", "설명", SurveyAccessLevel.ASSOCIATE, null);
            assertThat(survey.getAccessLevel()).isEqualTo(SurveyAccessLevel.ASSOCIATE);
        }

        @DisplayName("SRV-014: accessLevel MEMBER 생성 성공")
        @Test
        void create_AccessLevelMember_Success() {
            Survey survey = Survey.create("제목", "설명", SurveyAccessLevel.MEMBER, null);
            assertThat(survey.getAccessLevel()).isEqualTo(SurveyAccessLevel.MEMBER);
        }

        @DisplayName("SRV-016: deadline null (미설정) 생성 성공")
        @Test
        void create_DeadlineNull_Success() {
            Survey survey = Survey.create("제목", "설명", SurveyAccessLevel.PUBLIC, null);
            assertThat(survey.getDeadline()).isNull();
        }

        @DisplayName("SRV-017: deadline 미래 시점 생성 성공")
        @Test
        void create_DeadlineFuture_Success() {
            Instant future = Instant.now().plusSeconds(86400);
            Survey survey = Survey.create("제목", "설명", SurveyAccessLevel.PUBLIC, future);
            assertThat(survey.getDeadline()).isEqualTo(future);
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
                    .isInstanceOf(SurveyInvalidStateTransitionException.class);
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

        @DisplayName("unpublish: OPEN 상태에서 비공개 시 자동 CLOSED (SRV-040)")
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

        @DisplayName("SRV-023: 이미 UNPUBLISHED에서 unpublish 시도 -> 에러")
        @Test
        void unpublish_AlreadyUnpublished_ThrowsException() {
            // given
            Survey survey = createSurvey();

            // when & then
            assertThatThrownBy(survey::unpublish)
                    .isInstanceOf(SurveyInvalidStateTransitionException.class);
        }

        @DisplayName("SRV-025/042: P+NS -> unpublish -> U+NS (NOT_STARTED 유지)")
        @Test
        void unpublish_WhenNotStarted_MaintainsResponseStatus() {
            // given
            Survey survey = createPublishedSurvey();

            // when
            survey.unpublish();

            // then
            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.UNPUBLISHED);
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.NOT_STARTED);
        }

        @DisplayName("SRV-041: P+C -> unpublish -> U+C (이미 CLOSED, 변경 없음)")
        @Test
        void unpublish_WhenClosed_MaintainsClosedStatus() {
            // given
            Survey survey = createClosedSurvey();

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
                    .isInstanceOf(SurveyInvalidStateTransitionException.class);
        }

        @DisplayName("openResponse: 이미 OPEN이면 예외")
        @Test
        void openResponse_AlreadyOpen_ThrowsException() {
            // given
            Survey survey = createPublishedAndOpenSurvey();

            // when & then
            assertThatThrownBy(survey::openResponse)
                    .isInstanceOf(SurveyInvalidStateTransitionException.class);
        }

        @DisplayName("openResponse: 마감일 경과 시 예외")
        @Test
        void openResponse_DeadlinePassed_ThrowsException() {
            // given
            Survey survey = createPublishedSurvey();
            ReflectionTestUtils.setField(survey, "deadline", Instant.now().minusSeconds(3600));

            // when & then
            assertThatThrownBy(survey::openResponse)
                    .isInstanceOf(SurveyInvalidStateTransitionException.class);
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

        @DisplayName("SRV-033: NOT_STARTED에서 closeResponse 시도 -> 에러 (금지된 전이)")
        @Test
        void closeResponse_FromNotStarted_ThrowsException() {
            // given
            Survey survey = createPublishedSurvey();

            // when & then
            assertThatThrownBy(survey::closeResponse)
                    .isInstanceOf(SurveyInvalidStateTransitionException.class);
        }

        @DisplayName("SRV-038: 이미 CLOSED에서 closeResponse 시도 -> 에러")
        @Test
        void closeResponse_AlreadyClosed_ThrowsException() {
            // given
            Survey survey = createClosedSurvey();

            // when & then
            assertThatThrownBy(survey::closeResponse)
                    .isInstanceOf(SurveyInvalidStateTransitionException.class);
        }

        @DisplayName("SRV-050: deadline=null -> openResponse 성공")
        @Test
        void openResponse_DeadlineNull_Success() {
            // given
            Survey survey = createPublishedSurvey();

            // when
            survey.openResponse();

            // then
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @DisplayName("SRV-051: deadline=미래 -> openResponse 성공")
        @Test
        void openResponse_DeadlineFuture_Success() {
            // given
            Survey survey = createPublishedSurvey();
            ReflectionTestUtils.setField(survey, "deadline", Instant.now().plusSeconds(86400));

            // when
            survey.openResponse();

            // then
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @DisplayName("SRV-053: CLOSED, deadline=null -> openResponse(재개) 성공")
        @Test
        void openResponse_FromClosedDeadlineNull_ResumeSuccess() {
            // given
            Survey survey = createClosedSurvey();

            // when
            survey.openResponse();

            // then
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @DisplayName("SRV-055: CLOSED, deadline=과거 -> openResponse(재개) 거부")
        @Test
        void openResponse_FromClosedDeadlinePassed_ThrowsException() {
            // given
            Survey survey = createClosedSurvey();
            ReflectionTestUtils.setField(survey, "deadline", Instant.now().minusSeconds(3600));

            // when & then
            assertThatThrownBy(survey::openResponse)
                    .isInstanceOf(SurveyInvalidStateTransitionException.class);
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

        @DisplayName("publishAndOpen: 이미 PUBLISHED면 예외 (P+NS)")
        @Test
        void publishAndOpen_AlreadyPublished_ThrowsException() {
            // given
            Survey survey = createPublishedSurvey();

            // when & then
            assertThatThrownBy(survey::publishAndOpen)
                    .isInstanceOf(SurveyInvalidStateTransitionException.class);
        }

        @DisplayName("SRV-048: P+O -> publishAndOpen -> 에러 (이미 공개)")
        @Test
        void publishAndOpen_PublishedOpen_ThrowsException() {
            // given
            Survey survey = createPublishedAndOpenSurvey();

            // when & then
            assertThatThrownBy(survey::publishAndOpen)
                    .isInstanceOf(SurveyInvalidStateTransitionException.class);
        }

        @DisplayName("SRV-049: P+C -> publishAndOpen -> 에러 (이미 공개)")
        @Test
        void publishAndOpen_PublishedClosed_ThrowsException() {
            // given
            Survey survey = createClosedSurvey();

            // when & then
            assertThatThrownBy(survey::publishAndOpen)
                    .isInstanceOf(SurveyInvalidStateTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Lazy Evaluation - updateStatusIfNeeded")
    class UpdateStatusIfNeeded {

        @DisplayName("OPEN + deadline 경과 → CLOSED 전환")
        @Test
        void updateStatusIfNeeded_OpenWithExpiredDeadline_TransitionsToClosed() {
            // given
            Survey survey = createOpenSurveyWithExpiredDeadline();

            // when
            survey.updateStatusIfNeeded(Instant.now());

            // then
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.CLOSED);
        }

        @DisplayName("OPEN + deadline null → OPEN 유지")
        @Test
        void updateStatusIfNeeded_OpenWithNullDeadline_RemainsOpen() {
            // given
            Survey survey = createPublishedAndOpenSurvey();
            // deadline은 null (기본)

            // when
            survey.updateStatusIfNeeded(Instant.now());

            // then
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @DisplayName("OPEN + deadline 미래 → OPEN 유지")
        @Test
        void updateStatusIfNeeded_OpenWithFutureDeadline_RemainsOpen() {
            // given
            Survey survey = createPublishedAndOpenSurvey();
            ReflectionTestUtils.setField(survey, "deadline", Instant.now().plusSeconds(86400));

            // when
            survey.updateStatusIfNeeded(Instant.now());

            // then
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.OPEN);
        }

        @DisplayName("CLOSED + deadline 경과 → CLOSED 유지 (멱등)")
        @Test
        void updateStatusIfNeeded_ClosedWithExpiredDeadline_RemainsClosed() {
            // given
            Survey survey = createClosedSurvey();
            ReflectionTestUtils.setField(survey, "deadline", Instant.now().minusSeconds(3600));

            // when
            survey.updateStatusIfNeeded(Instant.now());

            // then
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.CLOSED);
        }

        @DisplayName("NOT_STARTED + deadline 경과 → NOT_STARTED 유지")
        @Test
        void updateStatusIfNeeded_NotStartedWithExpiredDeadline_RemainsNotStarted() {
            // given
            Survey survey = createPublishedSurvey();
            ReflectionTestUtils.setField(survey, "deadline", Instant.now().minusSeconds(3600));

            // when
            survey.updateStatusIfNeeded(Instant.now());

            // then
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.NOT_STARTED);
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

        @DisplayName("SRV-087: P+O에서 update 성공")
        @Test
        void update_WhenPublishedAndOpen_Success() {
            // given
            Survey survey = createPublishedAndOpenSurvey();

            // when
            survey.update("응답 중 수정", "응답 중 설명", SurveyAccessLevel.MEMBER, null);

            // then
            assertThat(survey.getTitle()).isEqualTo("응답 중 수정");
        }

        @DisplayName("SRV-088: P+C에서 update 성공")
        @Test
        void update_WhenClosed_Success() {
            // given
            Survey survey = createClosedSurvey();

            // when
            survey.update("마감 후 수정", "마감 후 설명", SurveyAccessLevel.PUBLIC, null);

            // then
            assertThat(survey.getTitle()).isEqualTo("마감 후 수정");
        }

        @DisplayName("SRV-091: accessLevel 변경 성공")
        @Test
        void update_ChangeAccessLevel_Success() {
            // given
            Survey survey = createPublishedAndOpenSurvey();
            assertThat(survey.getAccessLevel()).isEqualTo(SurveyAccessLevel.PUBLIC);

            // when
            survey.update(survey.getTitle(), survey.getDescription(), SurveyAccessLevel.MEMBER, survey.getDeadline());

            // then
            assertThat(survey.getAccessLevel()).isEqualTo(SurveyAccessLevel.MEMBER);
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

        @DisplayName("SRV-061: P+NS -> trash 성공")
        @Test
        void trash_PublishedNotStarted_Success() {
            // given
            Survey survey = createPublishedSurvey();

            // when
            survey.trash();

            // then
            assertThat(survey.isTrashed()).isTrue();
            assertThat(survey.getVisibility()).isEqualTo(SurveyVisibility.PUBLISHED);
            assertThat(survey.getResponseStatus()).isEqualTo(SurveyResponseStatus.NOT_STARTED);
        }

        @DisplayName("SRV-062: P+O -> trash 성공")
        @Test
        void trash_PublishedOpen_Success() {
            // given
            Survey survey = createPublishedAndOpenSurvey();

            // when
            survey.trash();

            // then
            assertThat(survey.isTrashed()).isTrue();
        }

        @DisplayName("SRV-063: P+C -> trash 성공")
        @Test
        void trash_PublishedClosed_Success() {
            // given
            Survey survey = createClosedSurvey();

            // when
            survey.trash();

            // then
            assertThat(survey.isTrashed()).isTrue();
        }

        @DisplayName("trash: 이미 휴지통이면 예외")
        @Test
        void trash_AlreadyTrashed_ThrowsException() {
            // given
            Survey survey = createTrashedSurvey();

            // when & then
            assertThatThrownBy(survey::trash)
                    .isInstanceOf(SurveyAlreadyTrashedException.class);
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
                    .isInstanceOf(SurveyNotTrashedException.class);
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

        @DisplayName("permanentDelete: 활성 설문이면 예외")
        @Test
        void permanentDelete_ActiveSurvey_ThrowsException() {
            // given
            Survey survey = createSurvey();

            // when & then
            assertThatThrownBy(() -> survey.permanentDelete(1L))
                    .isInstanceOf(SurveyNotTrashedException.class);
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

        @DisplayName("SRV-097: P+NS -> isAcceptingResponses=false (NOT_STARTED)")
        @Test
        void isAcceptingResponses_WhenPublishedNotStarted_ReturnsFalse() {
            Survey survey = createPublishedSurvey();
            assertThat(survey.isAcceptingResponses()).isFalse();
        }

        @DisplayName("isAcceptingResponses: PUBLISHED+OPEN이지만 deadline 경과 → false (안전망)")
        @Test
        void isAcceptingResponses_WhenOpenButDeadlinePassed_ReturnsFalse() {
            Survey survey = createOpenSurveyWithExpiredDeadline();
            assertThat(survey.isAcceptingResponses()).isFalse();
        }
    }
}
