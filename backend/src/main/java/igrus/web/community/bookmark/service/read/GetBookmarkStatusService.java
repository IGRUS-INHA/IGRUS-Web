package igrus.web.community.bookmark.service.read;

import igrus.web.community.bookmark.dto.response.BookmarkStatusResponse;
import igrus.web.community.bookmark.repository.BookmarkRepository;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 북마크 상태 조회 서비스.
 * 게시글의 북마크 상태를 조회합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GetBookmarkStatusService {

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;

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
}
