package igrus.web.community.like.comment_like.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.community.like.comment_like.service.write.LikeCommentService;
import igrus.web.community.like.comment_like.service.write.UnlikeCommentService;
import igrus.web.generated.api.CommentLikeApi;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * 댓글 좋아요 컨트롤러.
 * 댓글 좋아요 추가/취소 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ASSOCIATE', 'MEMBER', 'OPERATOR', 'ADMIN')")
public class CommentLikeController implements CommentLikeApi {

    private final LikeCommentService likeCommentService;
    private final UnlikeCommentService unlikeCommentService;

    @Override
    public ResponseEntity<Void> likeComment(Long commentId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("댓글 좋아요 요청 - commentId: {}, userId: {}", commentId, user.userId());

        likeCommentService.likeComment(commentId, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<Void> unlikeComment(Long commentId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("댓글 좋아요 취소 요청 - commentId: {}, userId: {}", commentId, user.userId());

        unlikeCommentService.unlikeComment(commentId, user.userId());
        return ResponseEntity.noContent().build();
    }
}
