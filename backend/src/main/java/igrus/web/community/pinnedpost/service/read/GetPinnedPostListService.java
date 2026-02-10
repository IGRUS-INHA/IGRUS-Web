package igrus.web.community.pinnedpost.service.read;

import igrus.web.community.pinnedpost.dto.response.PinnedPostListResponse;
import igrus.web.community.pinnedpost.repository.PinnedPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetPinnedPostListService {

    private final PinnedPostRepository pinnedPostRepository;

    /**
     * 모든 고정 게시글을 표시 순서대로 조회합니다.
     *
     * @return 고정 게시글 목록
     */
    public List<PinnedPostListResponse> getPinnedPostList() {
        return pinnedPostRepository.findAllByDeletedFalseOrderByDisplayOrderAsc()
                .stream()
                .map(PinnedPostListResponse::from)
                .toList();
    }
}
