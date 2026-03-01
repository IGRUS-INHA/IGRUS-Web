package igrus.web.community.bookmark.controller;

import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.community.bookmark.dto.response.BookmarkStatusResponse;
import igrus.web.community.bookmark.dto.response.BookmarkToggleResponse;
import igrus.web.community.bookmark.dto.response.BookmarkedPostResponse;
import igrus.web.community.bookmark.service.read.GetBookmarkStatusService;
import igrus.web.community.bookmark.service.read.GetMyBookmarksService;
import igrus.web.community.bookmark.service.write.ToggleBookmarkService;
import igrus.web.generated.api.BookmarkApi;
import igrus.web.generated.model.GetBookmarkStatus200Response;
import igrus.web.generated.model.GetMyBookmarks200Response;
import igrus.web.generated.model.GetMyBookmarks200ResponsePostsInner;
import igrus.web.generated.model.ToggleBookmark200Response;
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
 * 북마크 컨트롤러.
 * 게시글 북마크 토글, 상태 조회, 목록 조회 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class BookmarkController implements BookmarkApi {

    private final ToggleBookmarkService toggleBookmarkService;
    private final GetBookmarkStatusService getBookmarkStatusService;
    private final GetMyBookmarksService getMyBookmarksService;

    @Override
    @PreAuthorize("hasAnyRole('ASSOCIATE', 'MEMBER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<ToggleBookmark200Response> toggleBookmark(Long postId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("북마크 토글 요청 - postId: {}, userId: {}", postId, user.userId());

        BookmarkToggleResponse result = toggleBookmarkService.toggleBookmark(postId, user.userId());
        return ResponseEntity.ok(new ToggleBookmark200Response()
                .bookmarked(result.bookmarked()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GetBookmarkStatus200Response> getBookmarkStatus(Long postId) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        log.info("북마크 상태 조회 요청 - postId: {}, userId: {}", postId, user.userId());

        BookmarkStatusResponse result = getBookmarkStatusService.getBookmarkStatus(postId, user.userId());
        return ResponseEntity.ok(new GetBookmarkStatus200Response()
                .bookmarked(result.bookmarked()));
    }

    @Override
    @PreAuthorize("hasAnyRole('ASSOCIATE', 'MEMBER', 'OPERATOR', 'ADMIN')")
    public ResponseEntity<GetMyBookmarks200Response> getMyBookmarks(Integer page, Integer size, List<String> sort) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        log.info("내 북마크 목록 조회 요청 - userId: {}, page: {}, size: {}",
                user.userId(), pageable.getPageNumber(), pageable.getPageSize());

        Page<BookmarkedPostResponse> resultPage = getMyBookmarksService.getMyBookmarks(user.userId(), pageable);
        return ResponseEntity.ok(new GetMyBookmarks200Response()
                .posts(resultPage.getContent().stream()
                        .map(p -> new GetMyBookmarks200ResponsePostsInner()
                                .postId(p.postId())
                                .title(p.title())
                                .boardCode(p.boardCode())
                                .boardName(p.boardName())
                                .authorName(p.authorName())
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
