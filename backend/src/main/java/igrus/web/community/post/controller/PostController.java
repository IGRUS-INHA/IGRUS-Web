package igrus.web.community.post.controller;

import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.community.post.dto.request.CreatePostRequest;
import igrus.web.community.post.dto.request.UpdatePostRequest;
import igrus.web.community.post.service.read.GetPostDetailService;
import igrus.web.community.post.service.read.GetPostListService;
import igrus.web.community.post.service.read.GetPostViewHistoryService;
import igrus.web.community.post.service.read.GetPostViewStatsService;
import igrus.web.community.post.service.write.CreatePostService;
import igrus.web.community.post.service.write.DeletePostService;
import igrus.web.community.post.dto.response.PostViewHistoryResponse;
import igrus.web.community.post.service.write.UpdatePostService;
import igrus.web.generated.api.PostApi;
import igrus.web.generated.model.ApiPostCreateResponse;
import igrus.web.generated.model.ApiPostDetailResponse;
import igrus.web.generated.model.ApiPostListPageResponse;
import igrus.web.generated.model.ApiPostListResponse;
import igrus.web.generated.model.ApiPostViewHistoryPageResponse;
import igrus.web.generated.model.ApiPostViewHistoryResponse;
import igrus.web.generated.model.ApiPostViewStatsResponse;
import igrus.web.generated.model.ApiPostUpdateResponse;
import igrus.web.generated.model.ApiCreatePostRequest;
import igrus.web.generated.model.ApiUpdatePostRequest;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 게시글 컨트롤러.
 * 게시글 작성, 조회, 수정, 삭제 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PostController implements PostApi {

    private final CreatePostService createPostService;
    private final UpdatePostService updatePostService;
    private final DeletePostService deletePostService;
    private final GetPostListService getPostListService;
    private final GetPostDetailService getPostDetailService;
    private final GetPostViewStatsService getPostViewStatsService;
    private final GetPostViewHistoryService getPostViewHistoryService;

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiPostCreateResponse> createPost(
            String boardCode,
            ApiCreatePostRequest createPostRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("게시글 작성 요청 - boardCode: {}, userId: {}, title: {}",
                boardCode, user.userId(), createPostRequest.getTitle());

        CreatePostRequest request = new CreatePostRequest(
                createPostRequest.getTitle(),
                createPostRequest.getContent(),
                Boolean.TRUE.equals(createPostRequest.getIsAnonymous()),
                Boolean.TRUE.equals(createPostRequest.getIsQuestion()),
                Boolean.TRUE.equals(createPostRequest.getIsVisibleToAssociate()),
                createPostRequest.getImageUrls()
        );

        var result = createPostService.createPost(boardCode, request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ApiPostCreateResponse()
                .postId(result.postId())
                .boardCode(result.boardCode())
                .title(result.title())
                .createdAt(result.createdAt()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiPostListPageResponse> getPostList(
            String boardCode,
            String keyword,
            Boolean questionOnly,
            Integer page,
            Integer size,
            List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        log.info("게시글 목록 조회 요청 - boardCode: {}, keyword: {}, questionOnly: {}, page: {}, size: {}",
                boardCode, keyword, questionOnly, pageable.getPageNumber(), pageable.getPageSize());

        var result = getPostListService.getPostList(boardCode, user, keyword, questionOnly, pageable);

        return ResponseEntity.ok(new ApiPostListPageResponse()
                .posts(result.posts().stream()
                        .map(p -> new ApiPostListResponse()
                                .postId(p.postId())
                                .title(p.title())
                                .authorName(p.authorName())
                                .isAnonymous(p.isAnonymous())
                                .isQuestion(p.isQuestion())
                                .isVisibleToAssociate(p.isVisibleToAssociate())
                                .viewCount(p.viewCount())
                                .likeCount(p.likeCount())
                                .commentCount(p.commentCount())
                                .bookmarkCount(p.bookmarkCount())
                                .createdAt(p.createdAt()))
                        .toList())
                .totalElements(result.totalElements())
                .totalPages(result.totalPages())
                .currentPage(result.currentPage())
                .hasNext(result.hasNext()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiPostDetailResponse> getPostDetail(String boardCode, Long postId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("게시글 상세 조회 요청 - boardCode: {}, postId: {}, userId: {}",
                boardCode, postId, user.userId());

        var result = getPostDetailService.getPostDetail(boardCode, postId, user);
        return ResponseEntity.ok(new ApiPostDetailResponse()
                .postId(result.postId())
                .boardCode(result.boardCode())
                .title(result.title())
                .content(result.content())
                .authorId(result.authorId())
                .authorName(result.authorName())
                .isAnonymous(result.isAnonymous())
                .isQuestion(result.isQuestion())
                .isVisibleToAssociate(result.isVisibleToAssociate())
                .viewCount(result.viewCount())
                .likeCount(result.likeCount())
                .bookmarkCount(result.bookmarkCount())
                .commentCount(result.commentCount())
                .imageUrls(result.imageUrls())
                .createdAt(result.createdAt())
                .updatedAt(result.updatedAt())
                .isAuthor(result.isAuthor())
                .liked(result.liked())
                .bookmarked(result.bookmarked()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiPostUpdateResponse> updatePost(
            String boardCode,
            Long postId,
            ApiUpdatePostRequest updatePostRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("게시글 수정 요청 - boardCode: {}, postId: {}, userId: {}, title: {}",
                boardCode, postId, user.userId(), updatePostRequest.getTitle());

        UpdatePostRequest request = new UpdatePostRequest(
                updatePostRequest.getTitle(),
                updatePostRequest.getContent(),
                Boolean.TRUE.equals(updatePostRequest.getIsQuestion()),
                Boolean.TRUE.equals(updatePostRequest.getIsVisibleToAssociate()),
                updatePostRequest.getImageUrls()
        );

        var result = updatePostService.updatePost(boardCode, postId, request, user);
        return ResponseEntity.ok(new ApiPostUpdateResponse()
                .postId(result.postId())
                .boardCode(result.boardCode())
                .title(result.title())
                .updatedAt(result.updatedAt()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePost(String boardCode, Long postId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("게시글 삭제 요청 - boardCode: {}, postId: {}, userId: {}",
                boardCode, postId, user.userId());

        deletePostService.deletePost(boardCode, postId, user);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiPostViewStatsResponse> getPostViewStats(String boardCode, Long postId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("게시글 조회 통계 요청 - boardCode: {}, postId: {}, userId: {}",
                boardCode, postId, user.userId());

        var result = getPostViewStatsService.getPostViewStats(boardCode, postId, user);
        return ResponseEntity.ok(new ApiPostViewStatsResponse()
                .postId(result.postId())
                .totalViews(result.totalViews())
                .uniqueViewers(result.uniqueViewers()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiPostViewHistoryPageResponse> getPostViewHistory(
            String boardCode,
            Long postId,
            Integer page,
            Integer size,
            List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        log.info("게시글 조회 기록 요청 - boardCode: {}, postId: {}, userId: {}, page: {}, size: {}",
                boardCode, postId, user.userId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<PostViewHistoryResponse> resultPage = getPostViewHistoryService.getPostViewHistory(boardCode, postId, user, pageable);
        return ResponseEntity.ok(new ApiPostViewHistoryPageResponse()
                .viewHistory(resultPage.getContent().stream()
                        .map(vh -> new ApiPostViewHistoryResponse()
                                .viewId(vh.viewId())
                                .viewerId(vh.viewerId())
                                .viewerName(vh.viewerName())
                                .viewedAt(vh.viewedAt()))
                        .toList())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .currentPage(resultPage.getNumber())
                .hasNext(resultPage.hasNext()));
    }
}
