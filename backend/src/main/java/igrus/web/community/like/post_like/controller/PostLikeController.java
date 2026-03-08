package igrus.web.community.like.post_like.controller;

import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.community.like.post_like.dto.response.LikedPostResponse;
import igrus.web.community.like.post_like.service.read.GetMyLikedPostsService;
import igrus.web.community.like.post_like.service.read.GetPostLikeStatusService;
import igrus.web.community.like.post_like.service.write.TogglePostLikeService;
import igrus.web.generated.api.PostLikeApi;
import igrus.web.generated.model.ApiPostLikeStatusResponse;
import igrus.web.generated.model.ApiLikedPostPageResponse;
import igrus.web.generated.model.ApiLikedPostResponse;
import igrus.web.generated.model.ApiPostLikeToggleResponse;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 게시글 좋아요 컨트롤러.
 * 게시글 좋아요 토글, 상태 조회, 목록 조회 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PostLikeController implements PostLikeApi {

    private final TogglePostLikeService togglePostLikeService;
    private final GetPostLikeStatusService getPostLikeStatusService;
    private final GetMyLikedPostsService getMyLikedPostsService;

    @Override
    @PreAuthorize("hasAnyRole('ASSOCIATE', 'MEMBER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiPostLikeToggleResponse> toggleLike(Long postId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("게시글 좋아요 토글 요청 - postId: {}, userId: {}", postId, user.userId());

        var result = togglePostLikeService.toggleLike(postId, user.userId());
        return ResponseEntity.ok(new ApiPostLikeToggleResponse()
                .liked(result.liked())
                .likeCount(result.likeCount()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiPostLikeStatusResponse> getLikeStatus(Long postId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("게시글 좋아요 상태 조회 요청 - postId: {}, userId: {}", postId, user.userId());

        var result = getPostLikeStatusService.getLikeStatus(postId, user.userId());
        return ResponseEntity.ok(new ApiPostLikeStatusResponse()
                .liked(result.liked())
                .likeCount(result.likeCount()));
    }

    @Override
    @PreAuthorize("hasAnyRole('ASSOCIATE', 'MEMBER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<ApiLikedPostPageResponse> getMyLikes(Integer page, Integer size, List<String> sort) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        log.info("내 게시글 좋아요 목록 조회 요청 - userId: {}, page: {}, size: {}",
                user.userId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<LikedPostResponse> resultPage = getMyLikedPostsService.getMyLikes(user.userId(), pageable);
        return ResponseEntity.ok(new ApiLikedPostPageResponse()
                .posts(resultPage.getContent().stream()
                        .map(p -> new ApiLikedPostResponse()
                                .postId(p.postId())
                                .title(p.title())
                                .boardCode(p.boardCode())
                                .boardName(p.boardName())
                                .authorName(p.authorName())
                                .likeCount(p.likeCount())
                                .createdAt(p.createdAt())
                                .isDeleted(p.isDeleted())
                                .deletedMessage(p.deletedMessage()))
                        .toList())
                .totalElements(resultPage.getTotalElements())
                .totalPages(resultPage.getTotalPages())
                .currentPage(resultPage.getNumber())
                .hasNext(resultPage.hasNext()));
    }
}
