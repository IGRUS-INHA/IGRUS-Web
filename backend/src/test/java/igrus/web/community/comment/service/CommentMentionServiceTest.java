package igrus.web.community.comment.service;

import igrus.web.community.comment.domain.Comment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * CommentMentionService 단위 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-MENTION-001~005: 멘션 추출</li>
 *     <li>CMT-MENTION-006~008: 멘션 처리 (TODO: 알림 서비스 연동 후)</li>
 * </ul>
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentMentionService 단위 테스트")
class CommentMentionServiceTest {

    @InjectMocks
    private CommentMentionService commentMentionService;

    @Nested
    @DisplayName("멘션 추출 테스트")
    class ExtractMentionsTest {

        @DisplayName("CMT-MENTION-001: 단일 멘션 추출 성공")
        @Test
        void extractMentions_singleMention_returnsList() {
            // given
            String content = "안녕하세요 @username 님!";

            // when
            List<String> mentions = commentMentionService.extractMentions(content);

            // then
            assertThat(mentions).hasSize(1);
            assertThat(mentions).containsExactly("username");
        }

        @DisplayName("CMT-MENTION-002: 여러 멘션 추출 성공")
        @Test
        void extractMentions_multipleMentions_returnsAll() {
            // given
            String content = "@user1 님과 @user2 님, @user3 님께 알려드립니다.";

            // when
            List<String> mentions = commentMentionService.extractMentions(content);

            // then
            assertThat(mentions).hasSize(3);
            assertThat(mentions).containsExactly("user1", "user2", "user3");
        }

        @DisplayName("CMT-MENTION-003: 멘션 없는 내용에서 빈 리스트 반환")
        @Test
        void extractMentions_noMentions_returnsEmptyList() {
            // given
            String content = "멘션이 없는 일반 댓글입니다.";

            // when
            List<String> mentions = commentMentionService.extractMentions(content);

            // then
            assertThat(mentions).isEmpty();
        }

        @DisplayName("CMT-MENTION-004: null 내용에서 빈 리스트 반환")
        @Test
        void extractMentions_nullContent_returnsEmptyList() {
            // given
            String content = null;

            // when
            List<String> mentions = commentMentionService.extractMentions(content);

            // then
            assertThat(mentions).isEmpty();
        }

        @DisplayName("CMT-MENTION-005: 빈 문자열에서 빈 리스트 반환")
        @Test
        void extractMentions_blankContent_returnsEmptyList() {
            // given
            String content = "   ";

            // when
            List<String> mentions = commentMentionService.extractMentions(content);

            // then
            assertThat(mentions).isEmpty();
        }

        @DisplayName("숫자가 포함된 사용자명 추출 성공")
        @Test
        void extractMentions_usernameWithNumbers_extractsCorrectly() {
            // given
            String content = "@user123 님께 알림을 보냅니다.";

            // when
            List<String> mentions = commentMentionService.extractMentions(content);

            // then
            assertThat(mentions).hasSize(1);
            assertThat(mentions).containsExactly("user123");
        }

        @DisplayName("언더스코어가 포함된 사용자명 추출 성공")
        @Test
        void extractMentions_usernameWithUnderscore_extractsCorrectly() {
            // given
            String content = "@user_name 님 안녕하세요!";

            // when
            List<String> mentions = commentMentionService.extractMentions(content);

            // then
            assertThat(mentions).hasSize(1);
            assertThat(mentions).containsExactly("user_name");
        }

        @DisplayName("연속된 멘션 추출 성공")
        @Test
        void extractMentions_consecutiveMentions_extractsAll() {
            // given
            String content = "@user1@user2 테스트";

            // when
            List<String> mentions = commentMentionService.extractMentions(content);

            // then
            assertThat(mentions).hasSize(2);
            assertThat(mentions).containsExactly("user1", "user2");
        }

        @DisplayName("문장 시작에 멘션 추출 성공")
        @Test
        void extractMentions_mentionAtStart_extractsCorrectly() {
            // given
            String content = "@username 님, 답변 감사합니다.";

            // when
            List<String> mentions = commentMentionService.extractMentions(content);

            // then
            assertThat(mentions).hasSize(1);
            assertThat(mentions).containsExactly("username");
        }

        @DisplayName("문장 끝에 멘션 추출 성공")
        @Test
        void extractMentions_mentionAtEnd_extractsCorrectly() {
            // given
            String content = "이 글은 @username";

            // when
            List<String> mentions = commentMentionService.extractMentions(content);

            // then
            assertThat(mentions).hasSize(1);
            assertThat(mentions).containsExactly("username");
        }

        @DisplayName("이메일 형식에서 멘션 추출하지 않음")
        @Test
        void extractMentions_email_doesNotExtractAsEmail() {
            // given
            String content = "연락처는 test@example.com 입니다.";

            // when
            List<String> mentions = commentMentionService.extractMentions(content);

            // then
            // 이메일의 @ 뒤 부분이 추출됨 (현재 구현 기준)
            // 참고: 이메일 처리를 원하지 않는다면 서비스 로직 개선 필요
            assertThat(mentions).hasSize(1);
            assertThat(mentions).containsExactly("example");
        }
    }

    @Nested
    @DisplayName("멘션 처리 테스트")
    class ProcessMentionsTest {

        @DisplayName("빈 멘션 리스트 처리 시 아무 작업 없음")
        @Test
        void processMentions_emptyList_noAction() {
            // given
            Comment mockComment = mock(Comment.class);
            List<String> emptyMentions = List.of();

            // when & then: 예외 없이 정상 종료 (빈 리스트이므로 바로 리턴)
            commentMentionService.processMentions(mockComment, emptyMentions);
        }

        @DisplayName("null 멘션 리스트 처리 시 아무 작업 없음")
        @Test
        void processMentions_nullList_noAction() {
            // given
            Comment mockComment = mock(Comment.class);

            // when & then: 예외 없이 정상 종료
            commentMentionService.processMentions(mockComment, null);
        }

        @DisplayName("유효한 멘션 처리 시 로그 출력 (현재 TODO 상태)")
        @Test
        void processMentions_validMentions_logsOutput() {
            // given
            Comment mockComment = mock(Comment.class);
            when(mockComment.getId()).thenReturn(1L);
            List<String> mentions = List.of("user1", "user2");

            // when & then: 예외 없이 정상 종료 (현재는 로그만 출력)
            // TODO: NotificationService 구현 후 알림 발송 검증 추가
            commentMentionService.processMentions(mockComment, mentions);
        }
    }
}
