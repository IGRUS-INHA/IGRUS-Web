package igrus.web.community.pinnedpost.service.write;

import igrus.web.community.pinnedpost.domain.PinnedPost;
import igrus.web.community.pinnedpost.exception.PinnedPostNotFoundException;
import igrus.web.community.pinnedpost.repository.PinnedPostRepository;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeletePinnedPostService {

    private final PinnedPostRepository pinnedPostRepository;

    /**
     * 고정 게시글을 삭제합니다 (soft delete).
     *
     * @param id                고정 게시글 ID
     * @param authenticatedUser 인증된 사용자 (OPERATOR 이상)
     */
    public void deletePinnedPost(Long id, AuthenticatedUser authenticatedUser) {
        PinnedPost pinnedPost = pinnedPostRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new PinnedPostNotFoundException(id));

        pinnedPost.delete(authenticatedUser.userId());

        log.info("고정 게시글 삭제 - id: {}, deletedBy: {}", id, authenticatedUser.userId());
    }
}
