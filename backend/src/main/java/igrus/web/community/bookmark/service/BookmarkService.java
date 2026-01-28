package igrus.web.community.bookmark.service;

import igrus.web.community.bookmark.domain.Bookmark;
import igrus.web.community.bookmark.dto.response.BookmarkStatusResponse;
import igrus.web.community.bookmark.dto.response.BookmarkToggleResponse;
import igrus.web.community.bookmark.dto.response.BookmarkedPostResponse;
import igrus.web.community.bookmark.repository.BookmarkRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostDeletedException;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.user.domain.User;
import igrus.web.user.exception.UserNotFoundException;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 북마크 서비스.
 * 게시글 북마크 토글, 상태 조회, 목록 조회 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 북마크를 토글합니다.
     * 북마크가 없으면 추가하고, 있으면 취소합니다.
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 북마크 토글 결과
     * @throws PostNotFoundException 게시글을 찾을 수 없는 경우
     * @throws PostDeletedException 삭제된 게시글인 경우
     */
    public BookmarkToggleResponse toggleBookmark(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));

        if (post.isDeleted()) {
            throw new PostDeletedException(postId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Optional<Bookmark> existingBookmark = bookmarkRepository.findByPostAndUser(post, user);

        if (existingBookmark.isPresent()) {
            // 북마크 취소 (Hard Delete)
            bookmarkRepository.delete(existingBookmark.get());

            log.info("북마크 취소 - postId: {}, userId: {}", postId, userId);
            return BookmarkToggleResponse.of(false);
        } else {
            // 북마크 추가
            Bookmark bookmark = Bookmark.create(post, user);
            bookmarkRepository.save(bookmark);

            log.info("북마크 추가 - postId: {}, userId: {}", postId, userId);
            return BookmarkToggleResponse.of(true);
        }
    }

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

    /**
     * 게시글의 북마크 상태를 조회합니다.
     *
     * @param postId 게시글 ID
     * @param userId 사용자 ID
     * @return 북마크 상태 응답
     * @throws PostNotFoundException 게시글을 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public BookmarkStatusResponse getBookmarkStatus(Long postId, Long userId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }

        boolean bookmarked = bookmarkRepository.existsByPostIdAndUserId(postId, userId);
        return BookmarkStatusResponse.of(bookmarked);
    }

    /**
     * 사용자가 북마크한 게시글 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 북마크한 게시글 목록
     */
    @Transactional(readOnly = true)
    public Page<BookmarkedPostResponse> getMyBookmarks(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Page<Bookmark> bookmarks = bookmarkRepository.findAllByUserOrderByCreatedAtDesc(user, pageable);
        return bookmarks.map(BookmarkedPostResponse::from);
    }
}
