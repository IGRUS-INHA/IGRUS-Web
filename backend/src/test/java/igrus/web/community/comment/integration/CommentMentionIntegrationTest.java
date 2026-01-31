package igrus.web.community.comment.integration;

import igrus.web.common.ServiceIntegrationTestBase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 댓글 멘션 알림 통합 테스트.
 *
 * <p>테스트 케이스:
 * <ul>
 *     <li>CMT-MENTION-IT-001: 댓글 작성 시 멘션된 사용자에게 알림 발송</li>
 *     <li>CMT-MENTION-IT-002: 대댓글 작성 시 멘션된 사용자에게 알림 발송</li>
 *     <li>CMT-MENTION-IT-003: 존재하지 않는 사용자 멘션 시 무시</li>
 *     <li>CMT-MENTION-IT-004: 준회원 멘션 시 무시 (정회원 이상만 알림 대상)</li>
 * </ul>
 * </p>
 *
 * <p><b>TODO: NotificationService 구현 후 활성화 필요</b></p>
 */
@DisplayName("댓글 멘션 알림 통합 테스트")
class CommentMentionIntegrationTest extends ServiceIntegrationTestBase {

    @Test
    @Disabled("TODO: NotificationService 구현 후 활성화")
    @DisplayName("CMT-MENTION-IT-001: 댓글 작성 시 멘션된 사용자에게 알림 발송")
    void createComment_withMention_sendsNotification() {
        // given
        // - 정회원 사용자 2명 생성 (작성자, 멘션 대상자)
        // - 게시판, 게시글 생성
        // - 멘션이 포함된 댓글 작성 요청 준비

        // when
        // - 댓글 작성 API 호출 또는 서비스 메서드 호출

        // then
        // - NotificationService.sendNotification() 호출 확인
        // - 인앱 알림 저장 확인
        // - 이메일 알림 발송 확인 (Mocking)
    }

    @Test
    @Disabled("TODO: NotificationService 구현 후 활성화")
    @DisplayName("CMT-MENTION-IT-002: 대댓글 작성 시 멘션된 사용자에게 알림 발송")
    void createReply_withMention_sendsNotification() {
        // given
        // - 정회원 사용자 3명 생성 (작성자, 부모 댓글 작성자, 멘션 대상자)
        // - 게시판, 게시글, 부모 댓글 생성
        // - 멘션이 포함된 대댓글 작성 요청 준비

        // when
        // - 대댓글 작성 API 호출

        // then
        // - 멘션 대상자에게 알림 발송 확인
    }

    @Test
    @Disabled("TODO: NotificationService 구현 후 활성화")
    @DisplayName("CMT-MENTION-IT-003: 존재하지 않는 사용자 멘션 시 무시")
    void createComment_withNonExistentUserMention_ignoresMention() {
        // given
        // - 정회원 사용자 1명 생성 (작성자)
        // - 존재하지 않는 @nonexistent 멘션이 포함된 댓글

        // when
        // - 댓글 작성

        // then
        // - 예외 없이 정상 완료
        // - 알림 발송되지 않음
    }

    @Test
    @Disabled("TODO: NotificationService 구현 후 활성화")
    @DisplayName("CMT-MENTION-IT-004: 준회원 멘션 시 무시")
    void createComment_withAssociateMention_ignoresMention() {
        // given
        // - 정회원 사용자 1명 생성 (작성자)
        // - 준회원 사용자 1명 생성 (멘션 대상, 하지만 알림 대상 아님)
        // - 준회원을 멘션하는 댓글

        // when
        // - 댓글 작성

        // then
        // - 예외 없이 정상 완료
        // - 준회원에게는 알림 발송되지 않음
    }
}
