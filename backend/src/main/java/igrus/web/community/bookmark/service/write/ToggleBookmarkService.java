package igrus.web.community.bookmark.service.write;

import igrus.web.community.bookmark.domain.Bookmark;
import igrus.web.community.bookmark.dto.response.BookmarkToggleResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 북마크 토글 서비스.
 * 게시글 북마크를 추가하거나 취소합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ToggleBookmarkService {

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
}
