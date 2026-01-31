package igrus.web.community.comment.service;

import igrus.web.community.comment.domain.Comment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 댓글 멘션 서비스.
 * @username 형태의 멘션을 파싱하고 알림을 발송합니다.
 *
 * TODO: 알림 서비스(NotificationService) 구현 후 완전한 연동 필요
 * - UserRepository에 findByName 메서드 추가 필요
 * - 인앱 알림 및 이메일 알림 발송 로직 구현 필요
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommentMentionService {

    /** 멘션 패턴: @로 시작하고 공백이 아닌 문자들 */
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    /**
     * 댓글 내용에서 멘션된 사용자명을 추출합니다.
     *
     * @param content 댓글 내용
     * @return 멘션된 사용자명 목록
     */
    public List<String> extractMentions(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        List<String> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);

        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }

        return mentions;
    }

    /**
     * 멘션된 사용자들에게 알림을 발송합니다.
     * 비동기로 처리됩니다.
     *
     * @param comment             작성된 댓글
     * @param mentionedUsernames  멘션된 사용자명 목록
     */
    @Async
    public void processMentions(Comment comment, List<String> mentionedUsernames) {
        if (mentionedUsernames == null || mentionedUsernames.isEmpty()) {
            return;
        }

        // TODO: 알림 서비스 연동 시 구현
        for (String username : mentionedUsernames) {
            log.info("멘션 감지 - commentId: {}, username: {}", comment.getId(), username);
            // 향후 구현:
            // 1. userRepository.findByName(username)으로 사용자 조회
            // 2. 정회원 이상인지 확인
            // 3. 인앱 알림 발송
            // 4. 이메일 알림 발송
        }
    }
}
