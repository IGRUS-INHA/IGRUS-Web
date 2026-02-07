package igrus.web.community.post.service.scheduler;

import igrus.web.community.bookmark.repository.BookmarkRepository;
import igrus.web.community.like.post_like.repository.PostLikeRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 게시글 좋아요/북마크 카운트 동기화 스케줄러.
 * PostLike/Bookmark 테이블의 실제 레코드 수와 Post의 likeCount/bookmarkCount 필드를 주기적으로 동기화합니다.
 * 원자적 카운터 업데이트 실패 등으로 인한 불일치를 보정합니다.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
@Transactional
public class PostCountSyncScheduler {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final BookmarkRepository bookmarkRepository;

    /**
     * 좋아요/북마크 카운트를 10분마다 동기화합니다.
     * 모든 게시글의 likeCount/bookmarkCount를 실제 레코드 수와 비교하여 불일치 시 업데이트합니다.
     */
    @Scheduled(fixedRate = 600000) // 10분마다
    public void syncCounts() {
        log.debug("좋아요/북마크 카운트 동기화 시작");

        // ID와 카운트만 먼저 추출 (영속성 컨텍스트 초기화 영향 방지)
        List<Post> allPosts = postRepository.findAll();
        record PostCountSnapshot(Long postId, int likeCount, int bookmarkCount) {}
        List<PostCountSnapshot> snapshots = allPosts.stream()
                .map(p -> new PostCountSnapshot(p.getId(), p.getLikeCount(), p.getBookmarkCount()))
                .toList();

        int likeSyncedCount = 0;
        int bookmarkSyncedCount = 0;

        for (PostCountSnapshot snapshot : snapshots) {
            // 좋아요 카운트 동기화
            long actualLikeCount = postLikeRepository.countByPostId(snapshot.postId());
            if (snapshot.likeCount() != actualLikeCount) {
                int updated = postRepository.syncLikeCount(snapshot.postId(), (int) actualLikeCount);
                if (updated > 0) {
                    likeSyncedCount++;
                    log.debug("좋아요 카운트 동기화: postId={}, {} -> {}", snapshot.postId(), snapshot.likeCount(), actualLikeCount);
                }
            }

            // 북마크 카운트 동기화
            long actualBookmarkCount = bookmarkRepository.countByPostId(snapshot.postId());
            if (snapshot.bookmarkCount() != actualBookmarkCount) {
                int updated = postRepository.syncBookmarkCount(snapshot.postId(), (int) actualBookmarkCount);
                if (updated > 0) {
                    bookmarkSyncedCount++;
                    log.debug("북마크 카운트 동기화: postId={}, {} -> {}", snapshot.postId(), snapshot.bookmarkCount(), actualBookmarkCount);
                }
            }
        }

        if (likeSyncedCount > 0 || bookmarkSyncedCount > 0) {
            log.info("좋아요/북마크 카운트 동기화 완료: 좋아요 {}건, 북마크 {}건 업데이트", likeSyncedCount, bookmarkSyncedCount);
        } else {
            log.debug("좋아요/북마크 카운트 동기화 완료: 업데이트 없음");
        }
    }
}
