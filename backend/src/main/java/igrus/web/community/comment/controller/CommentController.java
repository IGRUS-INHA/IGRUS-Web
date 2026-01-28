package igrus.web.community.comment.controller;

import igrus.web.community.comment.dto.request.CreateCommentRequest;
import igrus.web.community.comment.dto.response.CommentListResponse;
import igrus.web.community.comment.dto.response.CommentResponse;
import igrus.web.community.comment.service.CommentService;
import igrus.web.common.exception.ErrorResponse;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 댓글 컨트롤러.
 * 댓글 작성, 조회, 삭제 API를 제공합니다.
 */
@Tag(name = "Comment", description = "댓글 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MEMBER', 'OPERATOR', 'ADMIN')")
public class CommentController {

    private final CommentService commentService;

    @Operation(
            summary = "댓글 작성",
            description = "게시글에 댓글을 작성합니다. 정회원 이상만 작성 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "댓글 작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (내용 누락, 500자 초과, 익명 불가 게시판 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "작성 권한 없음 (정회원 이상 필요)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("댓글 작성 요청 - postId: {}, userId: {}, isAnonymous: {}",
                postId, user.userId(), request.isAnonymous());

        CommentResponse response = commentService.createComment(postId, request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "대댓글 작성",
            description = "댓글에 대댓글을 작성합니다. 정회원 이상만 작성 가능하며, 대댓글에는 답글을 달 수 없습니다 (1단계까지만 허용)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "대댓글 작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (대댓글에 답글 시도, 내용 누락, 500자 초과 등)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "작성 권한 없음 (정회원 이상 필요)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글 또는 부모 댓글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping("/{commentId}/replies")
    public ResponseEntity<CommentResponse> createReply(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @Parameter(description = "부모 댓글 ID", example = "1")
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("대댓글 작성 요청 - postId: {}, parentCommentId: {}, userId: {}, isAnonymous: {}",
                postId, commentId, user.userId(), request.isAnonymous());

        CommentResponse response = commentService.createReply(postId, commentId, request, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "댓글 목록 조회",
            description = "게시글의 댓글 목록을 계층 구조로 조회합니다. 등록순(오래된 순)으로 정렬됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "댓글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommentListResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<CommentListResponse> getComments(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("댓글 목록 조회 요청 - postId: {}, userId: {}",
                postId, user != null ? user.userId() : null);

        Long currentUserId = user != null ? user.userId() : null;
        CommentListResponse response = commentService.getCommentsByPostId(postId, currentUserId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "댓글 삭제",
            description = "댓글을 삭제합니다. 작성자 본인 또는 운영자 이상 권한을 가진 사용자만 삭제 가능합니다. Soft Delete로 처리되며, 삭제된 댓글은 '삭제된 댓글입니다'로 표시됩니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "댓글 삭제 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "삭제 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "댓글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("댓글 삭제 요청 - postId: {}, commentId: {}, userId: {}",
                postId, commentId, user.userId());

        commentService.deleteComment(postId, commentId, user.userId());
        return ResponseEntity.noContent().build();
    }
}
