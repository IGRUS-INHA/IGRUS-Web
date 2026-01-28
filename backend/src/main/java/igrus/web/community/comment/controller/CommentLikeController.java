package igrus.web.community.comment.controller;

import igrus.web.community.comment.service.CommentLikeService;
import igrus.web.common.exception.ErrorResponse;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 댓글 좋아요 컨트롤러.
 * 댓글 좋아요 추가/취소 API를 제공합니다.
 */
@Tag(name = "Comment Like", description = "댓글 좋아요 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/comments/{commentId}/likes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MEMBER', 'OPERATOR', 'ADMIN')")
public class CommentLikeController {

    private final CommentLikeService commentLikeService;

    @Operation(
            summary = "댓글 좋아요",
            description = "댓글에 좋아요를 추가합니다. 본인 댓글에는 좋아요할 수 없습니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "좋아요 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (본인 댓글에 좋아요, 이미 좋아요한 댓글)",
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
                    responseCode = "404",
                    description = "댓글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping
    public ResponseEntity<Void> likeComment(
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("댓글 좋아요 요청 - commentId: {}, userId: {}", commentId, user.userId());

        commentLikeService.likeComment(commentId, user.userId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(
            summary = "댓글 좋아요 취소",
            description = "댓글 좋아요를 취소합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "좋아요 취소 성공"
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
                    responseCode = "404",
                    description = "댓글 또는 좋아요 정보를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @DeleteMapping
    public ResponseEntity<Void> unlikeComment(
            @Parameter(description = "댓글 ID", example = "1")
            @PathVariable Long commentId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("댓글 좋아요 취소 요청 - commentId: {}, userId: {}", commentId, user.userId());

        commentLikeService.unlikeComment(commentId, user.userId());
        return ResponseEntity.noContent().build();
    }
}
