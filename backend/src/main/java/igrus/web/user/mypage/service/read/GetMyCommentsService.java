package igrus.web.user.mypage.service.read;

import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.user.mypage.dto.response.MyCommentResponse;
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
public class GetMyCommentsService {

    private final CommentRepository commentRepository;

    public Page<MyCommentResponse> getMyComments(Long userId, Pageable pageable) {
        log.info("내 댓글 목록 조회 - userId: {}, page: {}, size: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());

        // 1. 내가 작성한 댓글 조회 (삭제 안 된 것만, 최신순, 게시글 정보 포함)
        // 2. Entity → DTO 변환
        return commentRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable)
                .map(MyCommentResponse::from);
    }
}
