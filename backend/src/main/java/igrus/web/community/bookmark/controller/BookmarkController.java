package igrus.web.community.bookmark.controller;

import igrus.web.community.bookmark.dto.response.BookmarkStatusResponse;
import igrus.web.community.bookmark.dto.response.BookmarkToggleResponse;
import igrus.web.community.bookmark.dto.response.BookmarkedPostPageResponse;
import igrus.web.community.bookmark.dto.response.BookmarkedPostResponse;
import igrus.web.community.bookmark.service.read.GetBookmarkStatusService;
import igrus.web.community.bookmark.service.read.GetMyBookmarksService;
import igrus.web.community.bookmark.service.write.ToggleBookmarkService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 북마크 컨트롤러.
 * 게시글 북마크 토글, 상태 조회, 목록 조회 API를 제공합니다.
 */
@Tag(name = "Bookmark", description = "북마크 API")
@Slf4j
@RestController
@RequiredArgsConstructor
public class BookmarkController {

    private final ToggleBookmarkService toggleBookmarkService;
    private final GetBookmarkStatusService getBookmarkStatusService;
    private final GetMyBookmarksService getMyBookmarksService;

    @Operation(
            summary = "북마크 토글",
            description = "게시글 북마크를 토글합니다. 북마크가 없으면 추가하고, 있으면 취소합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "북마크 토글 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BookmarkToggleResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "게시글 접근 권한 필요"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음"
            ),
            @ApiResponse(
                    responseCode = "410",
                    description = "삭제된 게시글"
            )
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/api/v1/posts/{postId}/bookmarks")
    @PreAuthorize("hasAnyRole('ASSOCIATE', 'MEMBER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<BookmarkToggleResponse> toggleBookmark(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("북마크 토글 요청 - postId: {}, userId: {}", postId, user.userId());

        BookmarkToggleResponse response = toggleBookmarkService.toggleBookmark(postId, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "북마크 상태 조회",
            description = "게시글의 북마크 상태를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "북마크 상태 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = BookmarkStatusResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시글을 찾을 수 없음"
            )
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @GetMapping("/api/v1/posts/{postId}/bookmarks/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookmarkStatusResponse> getBookmarkStatus(
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("북마크 상태 조회 요청 - postId: {}, userId: {}", postId, user.userId());

        BookmarkStatusResponse response = getBookmarkStatusService.getBookmarkStatus(postId, user.userId());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "내 북마크 목록 조회",
            description = "내가 북마크한 게시글 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "북마크 목록 조회 성공"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            )
    })
    @SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
    @GetMapping("/api/v1/users/me/bookmarks")
    @PreAuthorize("hasAnyRole('ASSOCIATE', 'MEMBER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<BookmarkedPostPageResponse> getMyBookmarks(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("내 북마크 목록 조회 요청 - userId: {}, page: {}, size: {}",
                user.userId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<BookmarkedPostResponse> page = getMyBookmarksService.getMyBookmarks(user.userId(), pageable);
        return ResponseEntity.ok(BookmarkedPostPageResponse.from(page));
    }
}
