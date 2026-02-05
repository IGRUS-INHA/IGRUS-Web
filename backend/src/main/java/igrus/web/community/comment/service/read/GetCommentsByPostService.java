package igrus.web.community.comment.service.read;

import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.dto.response.CommentListResponse;
import igrus.web.community.comment.dto.response.CommentResponse;
import igrus.web.community.comment.dto.response.CommentWithRepliesResponse;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.like.comment_like.repository.CommentLikeRepository;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 게시글 댓글 목록 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class GetCommentsByPostService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;

    /**
     * 게시글의 댓글 목록을 계층 구조로 조회합니다.
     *
     * @param postId        게시글 ID
     * @param currentUserId 현재 사용자 ID (null 가능)
     * @return 댓글 목록 응답
     */
    @Transactional(readOnly = true)
    public CommentListResponse getCommentsByPostId(Long postId, Long currentUserId) {
        if (!postRepository.existsById(postId)) {
            throw new PostNotFoundException(postId);
        }

        List<Comment> allComments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        Map<Long, CommentWithRepliesResponse> parentCommentMap = new LinkedHashMap<>();
        List<Comment> replies = new ArrayList<>();

        // TODO: 리팩토링 필요. 좋아요 카운트를 위한 쿼리 호출 횟수 개선 필요.
        // TODO: 게시물 좋아요 카운트 로직도 개선해야 하는지 검토 필요.
        for (Comment comment : allComments) {
            if (comment.isReply()) {
                replies.add(comment);
            } else {
                long likeCount = commentLikeRepository.countByCommentId(comment.getId());
                boolean isLikedByMe = currentUserId != null &&
                        commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), currentUserId);
                parentCommentMap.put(comment.getId(), CommentWithRepliesResponse.from(comment, likeCount, isLikedByMe));
            }
        }

        for (Comment reply : replies) {
            Long parentId = reply.getParentComment().getId();
            CommentWithRepliesResponse parent = parentCommentMap.get(parentId);
            if (parent != null) {
                long likeCount = commentLikeRepository.countByCommentId(reply.getId());
                boolean isLikedByMe = currentUserId != null &&
                        commentLikeRepository.existsByCommentIdAndUserId(reply.getId(), currentUserId);
                parent.addReply(CommentResponse.from(reply, likeCount, isLikedByMe));
            }
        }

        List<CommentWithRepliesResponse> comments = new ArrayList<>(parentCommentMap.values());
        long totalCount = commentRepository.countByPostIdAndNotDeleted(postId);

        return CommentListResponse.of(comments, totalCount);
    }
}
