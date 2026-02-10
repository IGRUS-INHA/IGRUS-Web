package igrus.web.community.pinnedpost.controller;

import igrus.web.common.config.SwaggerConfig;
import igrus.web.community.pinnedpost.dto.request.CreatePinnedPostRequest;
import igrus.web.community.pinnedpost.dto.request.UpdateDisplayOrderRequest;
import igrus.web.community.pinnedpost.dto.response.PinnedPostDetailResponse;
import igrus.web.community.pinnedpost.dto.response.PinnedPostListResponse;
import igrus.web.community.pinnedpost.service.read.GetPinnedPostListService;
import igrus.web.community.pinnedpost.service.write.CreatePinnedPostService;
import igrus.web.community.pinnedpost.service.write.DeletePinnedPostService;
import igrus.web.community.pinnedpost.service.write.UpdatePinnedPostDisplayOrderService;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Pinned Post", description = "고정 게시글 API")
@SecurityRequirement(name = SwaggerConfig.SECURITY_SCHEME_NAME)
@Slf4j
@RestController
@RequestMapping("/api/v1/pinned-posts")
@RequiredArgsConstructor
public class PinnedPostController {

    private final CreatePinnedPostService createPinnedPostService;
    private final GetPinnedPostListService getPinnedPostListService;
    private final UpdatePinnedPostDisplayOrderService updatePinnedPostDisplayOrderService;
    private final DeletePinnedPostService deletePinnedPostService;

    @Operation(
            summary = "게시글 고정",
            description = "게시글을 메인 페이지에 고정합니다. OPERATOR 이상만 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "고정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PinnedPostDetailResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (OPERATOR 이상 필요)"),
            @ApiResponse(responseCode = "404", description = "게시글을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 고정된 게시글")
    })
    @PostMapping
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<PinnedPostDetailResponse> createPinnedPost(
            @Valid @RequestBody CreatePinnedPostRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("게시글 고정 요청 - postId: {}, displayOrder: {}, userId: {}",
                request.postId(), request.displayOrder(), user.userId());

        PinnedPostDetailResponse response = createPinnedPostService.createPinnedPost(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
            summary = "고정 게시글 목록 조회",
            description = "메인 페이지에 고정된 모든 게시글을 표시 순서대로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true
            )
    })
    @GetMapping
    public ResponseEntity<List<PinnedPostListResponse>> getPinnedPostList() {
        log.debug("고정 게시글 목록 조회 요청");

        List<PinnedPostListResponse> response = getPinnedPostListService.getPinnedPostList();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "고정 게시글 표시 순서 변경",
            description = "고정 게시글의 표시 순서를 변경합니다. OPERATOR 이상만 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PinnedPostDetailResponse.class)
                    )
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (OPERATOR 이상 필요)"),
            @ApiResponse(responseCode = "404", description = "고정 게시글을 찾을 수 없음")
    })
    @PutMapping("/{id}/display-order")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<PinnedPostDetailResponse> updateDisplayOrder(
            @Parameter(description = "고정 게시글 ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UpdateDisplayOrderRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("고정 게시글 순서 변경 요청 - id: {}, newOrder: {}, userId: {}",
                id, request.displayOrder(), user.userId());

        PinnedPostDetailResponse response = updatePinnedPostDisplayOrderService.updateDisplayOrder(id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "게시글 고정 해제",
            description = "게시글 고정을 해제합니다. OPERATOR 이상만 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "고정 해제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (OPERATOR 이상 필요)"),
            @ApiResponse(responseCode = "404", description = "고정 게시글을 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> deletePinnedPost(
            @Parameter(description = "고정 게시글 ID", example = "1")
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        log.info("게시글 고정 해제 요청 - id: {}, userId: {}", id, user.userId());

        deletePinnedPostService.deletePinnedPost(id, user);
        return ResponseEntity.noContent().build();
    }
}
