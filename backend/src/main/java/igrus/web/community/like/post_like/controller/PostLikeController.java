package igrus.web.community.like.post_like.controller;

import igrus.web.common.exception.ErrorResponse;
import igrus.web.community.like.post_like.dto.response.PostLikeStatusResponse;
import igrus.web.community.like.post_like.dto.response.PostLikeToggleResponse;
import igrus.web.community.like.post_like.dto.response.LikedPostResponse;
import igrus.web.community.like.post_like.service.PostLikeService;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import igrus.web.common.config.SwaggerConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 게시글 좋아요 컨트롤러.
 * 게시글 좋아요 토글, 상태 조회, 목록 조회 API를 제공합니다.
 */
@Tag(name = "PostLike", description = "게시글 좋아요 API")
@Slf4j
@RestController
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;

    @Operation(
            summary = "게시글 좋아요 토글",
            description = "게시글 좋아요를 토글합니다. 좋아요가 없으면 추가하고, 있으면 취소합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "좋아요 토글 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostLikeToggleResponse.class)
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
                    description = "정회원 이상 권한 필요",
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
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "삭제된 게시글",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/api/v1/posts/{postId}/likes")
    @PreAuthorize("hasAnyRole('MEMBER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<PostLikeToggleResponse> toggleLike(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("게시글 좋아요 토글 요청 - postId: {}, userId: {}", postId, user.userId());

        PostLikeToggleResponse response = postLikeService.toggleLike(postId, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "게시글 좋아요 상태 조회",
            description = "게시글의 좋아요 상태를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "좋아요 상태 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostLikeStatusResponse.class)
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
                    description = "게시글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @GetMapping("/api/v1/posts/{postId}/likes/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PostLikeStatusResponse> getLikeStatus(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("게시글 좋아요 상태 조회 요청 - postId: {}, userId: {}", postId, user.userId());

        PostLikeStatusResponse response = postLikeService.getLikeStatus(postId, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "내 게시글 좋아요 목록 조회",
            description = "내가 좋아요한 게시글 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "좋아요 목록 조회 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @GetMapping("/api/v1/users/me/likes")
    @PreAuthorize("hasAnyRole('MEMBER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<Page<LikedPostResponse>> getMyLikes(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("내 게시글 좋아요 목록 조회 요청 - userId: {}, page: {}, size: {}",
                user.userId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<LikedPostResponse> response = postLikeService.getMyLikes(user.userId(), pageable);
        return ResponseEntity.ok(response);
    }
}
