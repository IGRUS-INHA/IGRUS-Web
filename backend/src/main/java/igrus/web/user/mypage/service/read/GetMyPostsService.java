package igrus.web.user.mypage.service.read;

import igrus.web.community.post.repository.PostRepository;
import igrus.web.user.mypage.dto.response.MyPostResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetMyPostsService {

    private final PostRepository postRepository;

    public Page<MyPostResponse> getMyPosts(Long userId, Pageable pageable) {
        log.info("내 게시글 목록 조회 - userId: {}, page: {}, size: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        // 1. 내가 작성한 게시글 조회 (삭제 안 된 것만, 최신순)
        // 2. Entity → DTO 변환
        return postRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable)
                .map(MyPostResponse::from);
    }
}
