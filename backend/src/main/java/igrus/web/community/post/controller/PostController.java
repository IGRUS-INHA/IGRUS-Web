package igrus.web.community.post.controller;

import igrus.web.community.post.dto.request.CreatePostRequest;
import igrus.web.community.post.dto.request.UpdatePostRequest;
import igrus.web.community.post.dto.response.PostCreateResponse;
import igrus.web.community.post.dto.response.PostDetailResponse;
import igrus.web.community.post.dto.response.PostListPageResponse;
import igrus.web.community.post.dto.response.PostUpdateResponse;
import igrus.web.community.post.dto.response.PostViewHistoryResponse;
import igrus.web.community.post.dto.response.PostViewStatsResponse;
import igrus.web.community.post.service.PostService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 게시글 컨트롤러.
 * 게시글 작성, 조회, 수정, 삭제 API를 제공합니다.
 */
@Tag(name = "Post", description = "게시글 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/boards/{boardCode}/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(
            summary = "게시글 작성",
            description = "게시판에 새 게시글을 작성합니다"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "게시글 작성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostCreateResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (제목 누락, 100자 초과, 이미지 5개 초과 등)",
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
                    description = "쓰기 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시판을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "게시글 작성 제한 초과 (시간당 20회)",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @PostMapping
    public ResponseEntity<PostCreateResponse> createPost(
            @Parameter(description = "게시판 코드", example = "general")
            @PathVariable String boardCode,
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("게시글 작성 요청 - boardCode: {}, userId: {}, title: {}",
                boardCode, user.userId(), request.title());

        PostCreateResponse response = postService.createPost(boardCode, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "게시글 목록 조회",
            description = "게시판의 게시글 목록을 페이징하여 조회합니다. 키워드 검색 및 질문글 필터링을 지원합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostListPageResponse.class)
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
                    description = "읽기 권한 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "게시판을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @GetMapping
    public ResponseEntity<PostListPageResponse> getPostList(
            @Parameter(description = "게시판 코드", example = "GENERAL")
            @PathVariable String boardCode,
            @Parameter(description = "검색 키워드")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "질문글만 조회 여부")
            @RequestParam(required = false) Boolean questionOnly,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("게시글 목록 조회 요청 - boardCode: {}, keyword: {}, questionOnly: {}, page: {}, size: {}",
                boardCode, keyword, questionOnly, pageable.getPageNumber(), pageable.getPageSize());

        PostListPageResponse response = postService.getPostList(boardCode, user, keyword, questionOnly, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "게시글 상세 조회",
            description = "게시글의 상세 정보를 조회합니다. 조회 시 조회수가 1 증가합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 상세 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostDetailResponse.class)
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
                    description = "읽기 권한 없음",
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
    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPostDetail(
            @Parameter(description = "게시판 코드", example = "GENERAL")
            @PathVariable String boardCode,
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("게시글 상세 조회 요청 - boardCode: {}, postId: {}, userId: {}",
                boardCode, postId, user.userId());

        PostDetailResponse response = postService.getPostDetail(boardCode, postId, user);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "게시글 수정",
            description = "게시글을 수정합니다. 작성자 본인 또는 관리자만 수정 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게시글 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostUpdateResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (제목 누락, 100자 초과, 이미지 5개 초과 등)",
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
                    description = "수정 권한 없음",
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
    @PutMapping("/{postId}")
    public ResponseEntity<PostUpdateResponse> updatePost(
            @Parameter(description = "게시판 코드", example = "GENERAL")
            @PathVariable String boardCode,
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("게시글 수정 요청 - boardCode: {}, postId: {}, userId: {}, title: {}",
                boardCode, postId, user.userId(), request.title());

        PostUpdateResponse response = postService.updatePost(boardCode, postId, request, user);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "게시글 삭제",
            description = "게시글을 삭제합니다. 작성자 본인 또는 운영자 이상 권한을 가진 사용자만 삭제 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "게시글 삭제 성공"
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
                    description = "게시글을 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            )
    })
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @Parameter(description = "게시판 코드", example = "GENERAL")
            @PathVariable String boardCode,
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("게시글 삭제 요청 - boardCode: {}, postId: {}, userId: {}",
                boardCode, postId, user.userId());

        postService.deletePost(boardCode, postId, user);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "게시글 조회 통계",
            description = "게시글의 조회 통계를 조회합니다. OPERATOR 이상만 조회 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 통계 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PostViewStatsResponse.class)
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
                    description = "조회 권한 없음 (OPERATOR 이상 필요)",
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
    @GetMapping("/{postId}/view-stats")
    public ResponseEntity<PostViewStatsResponse> getPostViewStats(
            @Parameter(description = "게시판 코드", example = "GENERAL")
            @PathVariable String boardCode,
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("게시글 조회 통계 요청 - boardCode: {}, postId: {}, userId: {}",
                boardCode, postId, user.userId());

        PostViewStatsResponse response = postService.getPostViewStats(boardCode, postId, user);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "게시글 조회 기록 목록",
            description = "게시글의 조회 기록을 페이징하여 조회합니다. OPERATOR 이상만 조회 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 기록 목록 조회 성공"
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
                    description = "조회 권한 없음 (OPERATOR 이상 필요)",
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
    @GetMapping("/{postId}/view-history")
    public ResponseEntity<Page<PostViewHistoryResponse>> getPostViewHistory(
            @Parameter(description = "게시판 코드", example = "GENERAL")
            @PathVariable String boardCode,
            @Parameter(description = "게시글 ID", example = "1")
            @PathVariable Long postId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("게시글 조회 기록 요청 - boardCode: {}, postId: {}, userId: {}, page: {}, size: {}",
                boardCode, postId, user.userId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<PostViewHistoryResponse> response = postService.getPostViewHistory(boardCode, postId, user, pageable);
        return ResponseEntity.ok(response);
    }
}
