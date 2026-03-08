package igrus.web.community.pinnedpost.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.community.pinnedpost.dto.request.CreatePinnedPostRequest;
import igrus.web.community.pinnedpost.dto.request.UpdateDisplayOrderRequest;
import igrus.web.community.pinnedpost.dto.response.PinnedPostDetailResponse;
import igrus.web.community.pinnedpost.dto.response.PinnedPostListResponse;
import igrus.web.community.pinnedpost.dto.response.PinnedPostListResponse.PostInfo;
import igrus.web.community.pinnedpost.service.read.GetPinnedPostListService;
import igrus.web.community.pinnedpost.service.write.CreatePinnedPostService;
import igrus.web.community.pinnedpost.service.write.DeletePinnedPostService;
import igrus.web.community.pinnedpost.service.write.UpdatePinnedPostDisplayOrderService;
import igrus.web.generated.api.PinnedPostApi;
import igrus.web.generated.model.ApiCreatePinnedPostRequest;
import igrus.web.generated.model.ApiPinnedPostListResponse;
import igrus.web.generated.model.ApiPostInfo;
import igrus.web.generated.model.ApiPinnedPostDetailResponse;
import igrus.web.generated.model.ApiPinnedByInfo;
import igrus.web.generated.model.ApiUpdateDisplayOrderRequest;
import igrus.web.generated.model.ApiAuthorInfo;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PinnedPostController implements PinnedPostApi {

    private final CreatePinnedPostService createPinnedPostService;
    private final GetPinnedPostListService getPinnedPostListService;
    private final UpdatePinnedPostDisplayOrderService updatePinnedPostDisplayOrderService;
    private final DeletePinnedPostService deletePinnedPostService;

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiPinnedPostDetailResponse> createPinnedPost(
            ApiCreatePinnedPostRequest createPinnedPostRequest) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("게시글 고정 요청 - postId: {}, displayOrder: {}, userId: {}",
                createPinnedPostRequest.getPostId(), createPinnedPostRequest.getDisplayOrder(), user.userId());

        var internalRequest = new CreatePinnedPostRequest(
                createPinnedPostRequest.getPostId(),
                createPinnedPostRequest.getDisplayOrder());

        var result = createPinnedPostService.createPinnedPost(internalRequest, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDetailResponse(result));
    }

    @Override
    public ResponseEntity<List<ApiPinnedPostListResponse>> getPinnedPostList() {
        log.debug("고정 게시글 목록 조회 요청");

        var results = getPinnedPostListService.getPinnedPostList();
        List<ApiPinnedPostListResponse> response = results.stream()
                .map(this::mapToListResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiPinnedPostDetailResponse> updateDisplayOrder(
            Long id, ApiUpdateDisplayOrderRequest updateDisplayOrderRequest) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("고정 게시글 순서 변경 요청 - id: {}, newOrder: {}, userId: {}",
                id, updateDisplayOrderRequest.getDisplayOrder(), user.userId());

        var internalRequest = new UpdateDisplayOrderRequest(
                updateDisplayOrderRequest.getDisplayOrder());

        var result = updatePinnedPostDisplayOrderService.updateDisplayOrder(id, internalRequest);
        return ResponseEntity.ok(mapToDetailResponse(result));
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<Void> deletePinnedPost(Long id) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("게시글 고정 해제 요청 - id: {}, userId: {}", id, user.userId());

        deletePinnedPostService.deletePinnedPost(id, user);
        return ResponseEntity.noContent().build();
    }

    private ApiPinnedPostDetailResponse mapToDetailResponse(PinnedPostDetailResponse result) {
        return new ApiPinnedPostDetailResponse()
                .id(result.id())
                .postId(result.postId())
                .postTitle(result.postTitle())
                .boardCode(result.boardCode())
                .displayOrder(result.displayOrder())
                .pinnedBy(new ApiPinnedByInfo()
                        .id(result.pinnedBy().id())
                        .name(result.pinnedBy().name()))
                .createdAt(result.createdAt());
    }

    private ApiPinnedPostListResponse mapToListResponse(PinnedPostListResponse result) {
        PostInfo postInfo = result.post();
        return new ApiPinnedPostListResponse()
                .id(result.id())
                .post(new ApiPostInfo()
                        .id(postInfo.id())
                        .title(postInfo.title())
                        .contentPreview(postInfo.contentPreview())
                        .boardCode(postInfo.boardCode())
                        .boardName(postInfo.boardName())
                        .author(new ApiAuthorInfo()
                                .id(postInfo.author().id())
                                .name(postInfo.author().name()))
                        .isVisibleToAssociate(postInfo.isVisibleToAssociate())
                        .viewCount(postInfo.viewCount())
                        .likeCount(postInfo.likeCount())
                        .commentCount(postInfo.commentCount())
                        .createdAt(postInfo.createdAt()))
                .displayOrder(result.displayOrder())
                .pinnedBy(new ApiPinnedByInfo()
                        .id(result.pinnedBy().id())
                        .name(result.pinnedBy().name()))
                .pinnedAt(result.pinnedAt());
    }
}
