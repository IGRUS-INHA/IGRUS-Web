package igrus.web.community.comment.service.write;

import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.dto.request.CreateCommentRequest;
import igrus.web.community.comment.dto.response.CommentResponse;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.comment.service.support.CommentFinder;
import igrus.web.community.comment.service.support.CommentValidator;
import igrus.web.community.post.domain.Post;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대댓글 작성 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CreateCommentReplyService {

    private final CommentRepository commentRepository;
    private final CommentFinder commentFinder;
    private final CommentValidator commentValidator;

    /**
     * 대댓글을 작성합니다.
     *
     * @param postId          게시글 ID
     * @param parentCommentId 부모 댓글 ID
     * @param request         댓글 작성 요청
     * @param userId          작성자 ID
     * @return 생성된 대댓글 응답
     */
    public CommentResponse createReply(Long postId, Long parentCommentId, CreateCommentRequest request, Long userId) {
        Post post = commentFinder.findPostById(postId);
        User author = commentFinder.findUserById(userId);
        Comment parentComment = commentFinder.findCommentById(parentCommentId);

        commentValidator.validatePostNotDeleted(post);
        commentValidator.validateParentCommentBelongsToPost(parentComment, postId);
        commentValidator.validateCanReplyTo(parentComment);
        commentValidator.validateAnonymousOption(post, request.isAnonymous());

        Comment reply = Comment.createReply(post, parentComment, author, request.getContent(), request.isAnonymous());
        Comment savedReply = commentRepository.save(reply);

        return CommentResponse.from(savedReply, 0, false);
    }
}
