package igrus.web.community.pinnedpost.service.write;

import igrus.web.community.pinnedpost.domain.PinnedPost;
import igrus.web.community.pinnedpost.dto.request.UpdateDisplayOrderRequest;
import igrus.web.community.pinnedpost.dto.response.PinnedPostDetailResponse;
import igrus.web.community.pinnedpost.exception.PinnedPostNotFoundException;
import igrus.web.community.pinnedpost.repository.PinnedPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UpdatePinnedPostDisplayOrderService {

    private final PinnedPostRepository pinnedPostRepository;

    /**
     * 고정 게시글의 표시 순서를 변경합니다.
     *
     * @param id      고정 게시글 ID
     * @param request 표시 순서 변경 요청
     * @return 수정된 고정 게시글 응답
     */
    public PinnedPostDetailResponse updateDisplayOrder(Long id, UpdateDisplayOrderRequest request) {
        PinnedPost pinnedPost = pinnedPostRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new PinnedPostNotFoundException(id));

        pinnedPost.updateDisplayOrder(request.displayOrder());

        log.info("고정 게시글 순서 변경 - id: {}, newOrder: {}", id, request.displayOrder());

        return PinnedPostDetailResponse.from(pinnedPost);
    }
}
