package igrus.web.community.bookmark.service.read;

import igrus.web.community.bookmark.domain.Bookmark;
import igrus.web.community.bookmark.dto.response.BookmarkedPostResponse;
import igrus.web.community.bookmark.repository.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 북마크 목록 조회 서비스.
 * 사용자가 북마크한 게시글 목록을 조회합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetMyBookmarksService {

    private final BookmarkRepository bookmarkRepository;

    /**
     * 사용자가 북마크한 게시글 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 북마크한 게시글 목록
     */
    @Transactional(readOnly = true)
    public Page<BookmarkedPostResponse> getMyBookmarks(Long userId, Pageable pageable) {
        Page<Bookmark> bookmarks = bookmarkRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
        return bookmarks.map(BookmarkedPostResponse::from);
    }
}
