package igrus.web.community.pinnedpost.controller;

import igrus.web.common.util.SecurityUtils;
import igrus.web.community.pinnedpost.dto.response.PinnedPostDetailResponse;
import igrus.web.community.pinnedpost.dto.response.PinnedPostListResponse;
import igrus.web.community.pinnedpost.service.read.GetPinnedPostListService;
import igrus.web.community.pinnedpost.service.write.CreatePinnedPostService;
import igrus.web.community.pinnedpost.service.write.DeletePinnedPostService;
import igrus.web.community.pinnedpost.service.write.UpdatePinnedPostDisplayOrderService;
import igrus.web.generated.api.PinnedPostApi;
import igrus.web.generated.model.CreatePinnedPostRequest;
import igrus.web.generated.model.GetPinnedPostList200ResponseInner;
import igrus.web.generated.model.GetPinnedPostList200ResponseInnerPost;
import igrus.web.generated.model.UpdateDisplayOrder200Response;
import igrus.web.generated.model.UpdateDisplayOrder200ResponsePinnedBy;
import igrus.web.generated.model.UpdateDisplayOrderRequest;
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
    public ResponseEntity<UpdateDisplayOrder200Response> createPinnedPost(
            CreatePinnedPostRequest createPinnedPostRequest) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("게시글 고정 요청 - postId: {}, displayOrder: {}, userId: {}",
                createPinnedPostRequest.getPostId(), createPinnedPostRequest.getDisplayOrder(), user.userId());

        igrus.web.community.pinnedpost.dto.request.CreatePinnedPostRequest internalRequest =
                new igrus.web.community.pinnedpost.dto.request.CreatePinnedPostRequest(
                        createPinnedPostRequest.getPostId(),
                        createPinnedPostRequest.getDisplayOrder());

        PinnedPostDetailResponse result = createPinnedPostService.createPinnedPost(internalRequest, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDetailResponse(result));
    }

    @Override
    public ResponseEntity<List<GetPinnedPostList200ResponseInner>> getPinnedPostList() {
        log.debug("고정 게시글 목록 조회 요청");

        List<PinnedPostListResponse> results = getPinnedPostListService.getPinnedPostList();
        List<GetPinnedPostList200ResponseInner> response = results.stream()
                .map(this::mapToListResponse)
                .toList();
        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasAnyRole('OPERATOR', 'ADMIN')")
    public ResponseEntity<UpdateDisplayOrder200Response> updateDisplayOrder(
            Long id, UpdateDisplayOrderRequest updateDisplayOrderRequest) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("고정 게시글 순서 변경 요청 - id: {}, newOrder: {}, userId: {}",
                id, updateDisplayOrderRequest.getDisplayOrder(), user.userId());

        igrus.web.community.pinnedpost.dto.request.UpdateDisplayOrderRequest internalRequest =
                new igrus.web.community.pinnedpost.dto.request.UpdateDisplayOrderRequest(
                        updateDisplayOrderRequest.getDisplayOrder());

        PinnedPostDetailResponse result = updatePinnedPostDisplayOrderService.updateDisplayOrder(id, internalRequest);
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

    private UpdateDisplayOrder200Response mapToDetailResponse(PinnedPostDetailResponse result) {
        return new UpdateDisplayOrder200Response()
                .id(result.id())
                .postId(result.postId())
                .postTitle(result.postTitle())
                .boardCode(result.boardCode())
                .displayOrder(result.displayOrder())
                .pinnedBy(new UpdateDisplayOrder200ResponsePinnedBy()
                        .id(result.pinnedBy().id())
                        .name(result.pinnedBy().name()))
                .createdAt(result.createdAt());
    }

    private GetPinnedPostList200ResponseInner mapToListResponse(PinnedPostListResponse result) {
        PinnedPostListResponse.PostInfo postInfo = result.post();
        return new GetPinnedPostList200ResponseInner()
                .id(result.id())
                .post(new GetPinnedPostList200ResponseInnerPost()
                        .id(postInfo.id())
                        .title(postInfo.title())
                        .contentPreview(postInfo.contentPreview())
                        .boardCode(postInfo.boardCode())
                        .boardName(postInfo.boardName())
                        .author(new UpdateDisplayOrder200ResponsePinnedBy()
                                .id(postInfo.author().id())
                                .name(postInfo.author().name()))
                        .isVisibleToAssociate(postInfo.isVisibleToAssociate())
                        .viewCount(postInfo.viewCount())
                        .likeCount(postInfo.likeCount())
                        .commentCount(postInfo.commentCount())
                        .createdAt(postInfo.createdAt()))
                .displayOrder(result.displayOrder())
                .pinnedBy(new UpdateDisplayOrder200ResponsePinnedBy()
                        .id(result.pinnedBy().id())
                        .name(result.pinnedBy().name()))
                .pinnedAt(result.pinnedAt());
    }
}
