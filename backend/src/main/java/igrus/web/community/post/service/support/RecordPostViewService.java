package igrus.web.community.post.service.support;

import igrus.web.community.post.domain.Post;
import igrus.web.community.post.domain.PostView;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.community.post.repository.PostViewRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 조회 기록을 비동기적으로 저장하는 서비스.
 * 메인 요청 처리를 차단하지 않기 위해 별도 스레드에서 실행됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RecordPostViewService {

    private final PostViewRepository postViewRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 조회 기록을 비동기적으로 저장합니다.
     * 비동기 실행으로 인해 엔티티 ID를 받아 내부에서 다시 조회합니다.
     *
     * @param postId   조회된 게시글 ID
     * @param viewerId 조회한 사용자 ID
     */
    @Async("postViewTaskExecutor")
    public void recordViewAsync(Long postId, Long viewerId) {
        try {
            Post post = postRepository.findById(postId).orElse(null);
            User viewer = userRepository.findById(viewerId).orElse(null);

            if (post == null || viewer == null) {
                log.warn("게시글 조회 기록 저장 실패: post 또는 viewer를 찾을 수 없음 - postId={}, viewerId={}", postId, viewerId);
                return;
            }

            PostView postView = PostView.create(post, viewer);
            postViewRepository.save(postView);
            log.debug("게시글 조회 기록 저장: postId={}, viewerId={}", postId, viewerId);
        } catch (Exception e) {
            log.warn("게시글 조회 기록 저장 실패: postId={}, viewerId={}, error={}",
                    postId, viewerId, e.getMessage());
        }
    }
}
