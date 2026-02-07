package igrus.web.community.comment.service.write;

import igrus.web.community.comment.domain.Comment;
import igrus.web.community.comment.dto.request.CreateCommentRequest;
import igrus.web.community.comment.dto.response.CommentResponse;
import igrus.web.community.comment.repository.CommentRepository;
import igrus.web.community.comment.service.support.CommentFinder;
import igrus.web.community.comment.service.support.CommentValidator;
import igrus.web.community.post.domain.Post;
import igrus.web.community.post.repository.PostRepository;
import igrus.web.community.post.service.support.PostAccessChecker;
import igrus.web.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 작성 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CreateCommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final CommentFinder commentFinder;
    private final CommentValidator commentValidator;
    private final PostAccessChecker postAccessChecker;

    /**
     * 댓글을 작성합니다.
     *
     * @param postId  게시글 ID
     * @param request 댓글 작성 요청
     * @param userId  작성자 ID
     * @return 생성된 댓글 응답
     */
    public CommentResponse createComment(Long postId, CreateCommentRequest request, Long userId) {
        Post post = commentFinder.findPostById(postId);
        User author = commentFinder.findUserById(userId);

        commentValidator.validatePostNotDeleted(post);
        postAccessChecker.checkPostAccess(post, author);
        commentValidator.validateAnonymousOption(post, request.isAnonymous());

        Comment comment = Comment.createComment(post, author, request.getContent(), request.isAnonymous());
        Comment savedComment = commentRepository.save(comment);
        postRepository.incrementCommentCount(postId);

        return CommentResponse.from(savedComment, 0, false);
    }
}
