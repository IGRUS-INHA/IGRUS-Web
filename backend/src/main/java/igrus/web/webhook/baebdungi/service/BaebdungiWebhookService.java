package igrus.web.webhook.baebdungi.service;

import igrus.web.user.domain.User;

/**
 * 뱁둥이봇 웹훅 서비스 인터페이스.
 * 회원가입 완료 시 신규 가입자 정보를 뱁둥이봇에 전송합니다.
 */
public interface BaebdungiWebhookService {

    /**
     * 회원 가입 정보를 뱁둥이봇 웹훅으로 전송합니다.
     * 비동기로 실행되며, 실패 시 회원가입 프로세스에 영향을 주지 않습니다.
     *
     * @param user 회원가입 완료된 사용자
     */
    void sendSubmission(User user);
}
