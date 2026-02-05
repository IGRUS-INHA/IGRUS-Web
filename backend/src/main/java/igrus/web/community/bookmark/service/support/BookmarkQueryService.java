package igrus.web.community.bookmark.service.support;

import igrus.web.community.bookmark.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 북마크 조회 헬퍼 서비스.
 * 다른 서비스에서 북마크 여부를 확인할 때 사용합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkQueryService {

    private final BookmarkRepository bookmarkRepository;

    /**
     * 사용자가 게시글을 북마크했는지 확인합니다.
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 북마크 여부
     */
    @Transactional(readOnly = true)
    public boolean isBookmarkedByUser(Long postId, Long userId) {
        return bookmarkRepository.existsByPostIdAndUserId(postId, userId);
    }
}
