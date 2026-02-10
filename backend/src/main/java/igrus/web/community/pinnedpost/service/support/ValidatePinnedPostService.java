package igrus.web.community.pinnedpost.service.support;

import igrus.web.community.pinnedpost.exception.PinnedPostAlreadyExistsException;
import igrus.web.community.pinnedpost.repository.PinnedPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValidatePinnedPostService {

    private final PinnedPostRepository pinnedPostRepository;

    /**
     * 게시글이 이미 고정되어 있는지 검증합니다.
     *
     * @param postId 게시글 ID
     * @throws PinnedPostAlreadyExistsException 이미 고정된 게시글인 경우
     */
    public void validateNotAlreadyPinned(Long postId) {
        if (pinnedPostRepository.existsByPostIdAndDeletedFalse(postId)) {
            throw new PinnedPostAlreadyExistsException(postId);
        }
    }
}
