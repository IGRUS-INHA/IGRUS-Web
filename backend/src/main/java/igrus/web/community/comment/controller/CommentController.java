package igrus.web.community.comment.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.community.comment.dto.request.CreateCommentRequest;
import igrus.web.community.comment.dto.response.CommentListResponse;
import igrus.web.community.comment.dto.response.CommentResponse;
import igrus.web.community.comment.dto.response.CommentWithRepliesResponse;
import igrus.web.community.comment.service.read.GetCommentsByPostService;
import igrus.web.community.comment.service.write.CreateCommentReplyService;
import igrus.web.community.comment.service.write.CreateCommentService;
import igrus.web.community.comment.service.write.DeleteCommentService;
import igrus.web.generated.api.CommentApi;
import igrus.web.generated.model.ApiCommentListResponse;
import igrus.web.generated.model.ApiCommentWithRepliesResponse;
import igrus.web.generated.model.ApiCommentResponse;
import igrus.web.generated.model.ApiCreateCommentRequest;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * 댓글 컨트롤러.
 * 댓글 작성, 조회, 삭제 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class CommentController implements CommentApi {

    private final CreateCommentService createCommentService;
    private final CreateCommentReplyService createCommentReplyService;
    private final GetCommentsByPostService getCommentsByPostService;
    private final DeleteCommentService deleteCommentService;

    @Override
    @PreAuthorize("hasAnyRole('MEMBER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiCommentResponse> createComment(
            Long postId,
            ApiCreateCommentRequest createCommentRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("댓글 작성 요청 - postId: {}, userId: {}, isAnonymous: {}",
                postId, user.userId(), createCommentRequest.getAnonymous());

        CreateCommentRequest request = new CreateCommentRequest(
                createCommentRequest.getContent(),
                Boolean.TRUE.equals(createCommentRequest.getAnonymous())
        );

        CommentResponse result = createCommentService.createComment(postId, request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToReplyInner(result));
    }

    @Override
    @PreAuthorize("hasAnyRole('MEMBER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiCommentResponse> createReply1(
            Long postId,
            Long commentId,
            ApiCreateCommentRequest createCommentRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("대댓글 작성 요청 - postId: {}, parentCommentId: {}, userId: {}, isAnonymous: {}",
                postId, commentId, user.userId(), createCommentRequest.getAnonymous());

        CreateCommentRequest request = new CreateCommentRequest(
                createCommentRequest.getContent(),
                Boolean.TRUE.equals(createCommentRequest.getAnonymous())
        );

        CommentResponse result = createCommentReplyService.createReply(postId, commentId, request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToReplyInner(result));
    }

    @Override
    @PreAuthorize("hasAnyRole('ASSOCIATE', 'MEMBER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiCommentListResponse> getComments(Long postId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("댓글 목록 조회 요청 - postId: {}, userId: {}",
                postId, user.userId());

        CommentListResponse result = getCommentsByPostService.getCommentsByPostId(postId, user.userId());
        return ResponseEntity.ok(new ApiCommentListResponse()
                .comments(result.getComments().stream()
                        .map(this::mapToCommentsInner)
                        .toList())
                .totalCount(result.getTotalCount()));
    }

    @Override
    @PreAuthorize("hasAnyRole('MEMBER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> deleteComment(Long postId, Long commentId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("댓글 삭제 요청 - postId: {}, commentId: {}, userId: {}",
                postId, commentId, user.userId());

        deleteCommentService.deleteComment(postId, commentId, user.userId());
        return ResponseEntity.noContent().build();
    }

    private ApiCommentResponse mapToReplyInner(CommentResponse r) {
        return new ApiCommentResponse()
                .id(r.getId())
                .postId(r.getPostId())
                .parentCommentId(r.getParentCommentId())
                .content(r.getContent())
                .authorId(r.getAuthorId())
                .authorName(r.getAuthorName())
                .anonymous(r.isAnonymous())
                .deleted(r.isDeleted())
                .likeCount(r.getLikeCount())
                .likedByMe(r.isLikedByMe())
                .createdAt(r.getCreatedAt());
    }

    private ApiCommentWithRepliesResponse mapToCommentsInner(CommentWithRepliesResponse c) {
        return new ApiCommentWithRepliesResponse()
                .id(c.getId())
                .postId(c.getPostId())
                .content(c.getContent())
                .authorId(c.getAuthorId())
                .authorName(c.getAuthorName())
                .anonymous(c.isAnonymous())
                .deleted(c.isDeleted())
                .likeCount(c.getLikeCount())
                .likedByMe(c.isLikedByMe())
                .createdAt(c.getCreatedAt())
                .replies(c.getReplies().stream()
                        .map(this::mapToReplyInner)
                        .toList());
    }
}
