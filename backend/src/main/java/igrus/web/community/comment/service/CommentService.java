package igrus.web.community.comment.service;

import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.dto.request.CreateCommentRequest;
import igrus.web.community.comment.dto.response.CommentResponse;
import igrus.web.community.comment.dto.response.CommentListResponse;
import igrus.web.community.comment.dto.response.CommentWithRepliesResponse;
import igrus.web.community.comment.exception.CommentAccessDeniedException;
import igrus.web.community.comment.exception.CommentNotFoundException;
import igrus.web.community.comment.exception.InvalidCommentException;
import igrus.web.community.comment.repository.CommentLikeRepository;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.exception.PostNotFoundException;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.user.domain.User;
import igrus.web.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 댓글 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 댓글을 작성합니다.
     *
     * @param postId  게시글 ID
     * @param request 댓글 작성 요청
     * @param userId  작성자 ID
     * @return 생성된 댓글 응답
     */
    @Transactional
    public CommentResponse createComment(Long postId, CreateCommentRequest request, Long userId) {
        Post post = findPostById(postId);
        User author = findUserById(userId);

        validatePostNotDeleted(post);
        validateAnonymousOption(post, request.isAnonymous());

        Comment comment = Comment.createComment(post, author, request.getContent(), request.isAnonymous());
        Comment savedComment = commentRepository.save(comment);

        return CommentResponse.from(savedComment, 0, false);
    }

    /**
     * 대댓글을 작성합니다.
     *
     * @param postId          게시글 ID
     * @param parentCommentId 부모 댓글 ID
     * @param request         댓글 작성 요청
     * @param userId          작성자 ID
     * @return 생성된 대댓글 응답
     */
    @Transactional
    public CommentResponse createReply(Long postId, Long parentCommentId, CreateCommentRequest request, Long userId) {
        Post post = findPostById(postId);
        User author = findUserById(userId);
        Comment parentComment = findCommentById(parentCommentId);

        validatePostNotDeleted(post);
        validateParentCommentBelongsToPost(parentComment, postId);
        validateCanReplyTo(parentComment);
        validateAnonymousOption(post, request.isAnonymous());

        Comment reply = Comment.createReply(post, parentComment, author, request.getContent(), request.isAnonymous());
        Comment savedReply = commentRepository.save(reply);

        return CommentResponse.from(savedReply, 0, false);
    }

    /**
     * 게시글의 댓글 목록을 계층 구조로 조회합니다.
     *
     * @param postId        게시글 ID
     * @param currentUserId 현재 사용자 ID (null 가능)
     * @return 댓글 목록 응답
     */
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

    /**
     * 댓글을 삭제합니다 (Soft Delete).
     *
     * @param postId    게시글 ID
     * @param commentId 댓글 ID
     * @param userId    삭제 요청자 ID
     */
    @Transactional
    public void deleteComment(Long postId, Long commentId, Long userId) {
        Comment comment = findCommentById(commentId);
        User user = findUserById(userId);

        validateCommentBelongsToPost(comment, postId);
        validateCanDelete(comment, user);

        comment.delete(userId);
    }

    // === Private Helper Methods ===

    private Post findPostById(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException(postId));
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }

    private Comment findCommentById(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    private void validatePostNotDeleted(Post post) {
        if (post.isDeleted()) {
            throw InvalidCommentException.postDeletedCannotComment();
        }
    }

    private void validateAnonymousOption(Post post, boolean isAnonymous) {
        if (isAnonymous && !post.getBoard().getAllowsAnonymous()) {
            throw InvalidCommentException.anonymousNotAllowed();
        }
    }

    private void validateParentCommentBelongsToPost(Comment parentComment, Long postId) {
        if (!parentComment.getPost().getId().equals(postId)) {
            throw new CommentNotFoundException(parentComment.getId());
        }
    }

    private void validateCommentBelongsToPost(Comment comment, Long postId) {
        if (!comment.getPost().getId().equals(postId)) {
            throw new CommentNotFoundException(comment.getId());
        }
    }

    private void validateCanReplyTo(Comment parentComment) {
        if (!parentComment.canReplyTo()) {
            throw InvalidCommentException.replyToReplyNotAllowed();
        }
    }

    private void validateCanDelete(Comment comment, User user) {
        if (!comment.canDelete(user)) {
            throw new CommentAccessDeniedException();
        }
    }
}
