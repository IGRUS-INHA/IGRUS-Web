package igrus.web.user.mypage.controller;

import igrus.web.common.util.PageableUtils;
import igrus.web.common.util.SecurityUtils;
import igrus.web.community.bookmark.dto.response.BookmarkedPostPageResponse;
import igrus.web.community.bookmark.dto.response.BookmarkedPostResponse;
import igrus.web.community.bookmark.service.read.GetMyBookmarksService;
import igrus.web.community.like.post_like.dto.response.LikedPostPageResponse;
import igrus.web.community.like.post_like.dto.response.LikedPostResponse;
import igrus.web.community.like.post_like.service.read.GetMyLikedPostsService;
import igrus.web.event.service.EventRegistrationService;
import igrus.web.generated.api.MyPageApi;
import igrus.web.generated.model.ApiBookmarkedPostPageResponse;
import igrus.web.generated.model.ApiBookmarkedPostResponse;
import igrus.web.generated.model.ApiMyCommentPageResponse;
import igrus.web.generated.model.ApiMyCommentResponse;
import igrus.web.generated.model.ApiLikedPostPageResponse;
import igrus.web.generated.model.ApiLikedPostResponse;
import igrus.web.generated.model.ApiMyPostPageResponse;
import igrus.web.generated.model.ApiMyPostResponse;
import igrus.web.generated.model.ApiMyProfileResponse;
import igrus.web.generated.model.ApiMyRegistrationResponse;
import igrus.web.generated.model.ApiChangeEmailRequest;
import igrus.web.generated.model.ApiEmailVerificationRequest;
import igrus.web.generated.model.ApiChangePhoneNumberRequest;
import igrus.web.generated.model.ApiUpdateStudentIdRequest;
import igrus.web.generated.model.ApiChangePasswordRequest;
import igrus.web.generated.model.ApiWithdrawRequest;
import igrus.web.security.auth.common.domain.AuthenticatedUser;
import igrus.web.security.auth.common.dto.request.EmailVerificationRequest;
import igrus.web.user.mypage.dto.request.ChangeEmailRequest;
import igrus.web.user.mypage.dto.request.ChangePasswordRequest;
import igrus.web.user.mypage.dto.request.ChangePhoneNumberRequest;
import igrus.web.user.mypage.dto.request.UpdateStudentIdRequest;
import igrus.web.user.mypage.dto.response.MyCommentPageResponse;
import igrus.web.user.mypage.dto.response.MyCommentResponse;
import igrus.web.user.mypage.dto.response.MyPostPageResponse;
import igrus.web.user.mypage.dto.response.MyPostResponse;
import igrus.web.user.mypage.service.read.GetMyCommentsService;
import igrus.web.user.mypage.service.read.GetMyPostsService;
import igrus.web.user.mypage.service.read.GetMyProfileService;
import igrus.web.user.mypage.service.write.ChangeEmailService;
import igrus.web.user.mypage.service.write.ChangeMyPasswordService;
import igrus.web.user.mypage.service.write.ChangePhoneNumberService;
import igrus.web.user.mypage.service.write.UpdateStudentIdService;
import igrus.web.user.mypage.service.write.VerifyEmailChangeService;
import igrus.web.user.withdrawal.dto.request.WithdrawRequest;
import igrus.web.user.withdrawal.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 마이페이지 컨트롤러.
 * 프로필 조회/수정, 비밀번호 변경, 내 활동 조회 API를 제공합니다.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class MyPageController implements MyPageApi {

    private final GetMyProfileService getMyProfileService;
    private final GetMyPostsService getMyPostsService;
    private final GetMyCommentsService getMyCommentsService;
    private final EventRegistrationService eventRegistrationService;
    private final GetMyLikedPostsService getMyLikedPostsService;
    private final GetMyBookmarksService getMyBookmarksService;
    private final ChangeEmailService changeEmailService;
    private final VerifyEmailChangeService verifyEmailChangeService;
    private final ChangePhoneNumberService changePhoneNumberService;
    private final ChangeMyPasswordService changeMyPasswordService;
    private final UpdateStudentIdService updateStudentIdService;
    private final WithdrawService withdrawService;

    // === 프로필 ===

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiMyProfileResponse> getMyProfile() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        var response = getMyProfileService.getMyProfile(user.userId());
        return ResponseEntity.ok(new ApiMyProfileResponse()
                .studentId(response.studentId())
                .name(response.name())
                .email(response.email())
                .phoneNumber(response.phoneNumber())
                .department(response.department())
                .role(response.role() != null
                        ? ApiMyProfileResponse.RoleEnum.fromValue(response.role().name()) : null)
                .createdAt(response.createdAt())
                .hasTemporaryStudentId(response.hasTemporaryStudentId()));
    }

    // === 이메일 변경 ===

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> requestEmailChange(
            ApiChangeEmailRequest changeEmailRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        ChangeEmailRequest request = new ChangeEmailRequest(
                changeEmailRequest.getPassword(),
                changeEmailRequest.getNewEmail()
        );
        changeEmailService.changeEmail(user.userId(), request);
        return ResponseEntity.ok().build();
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> verifyEmailChange(
            ApiEmailVerificationRequest emailVerificationRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        EmailVerificationRequest request = new EmailVerificationRequest(
                emailVerificationRequest.getEmail(),
                emailVerificationRequest.getCode()
        );
        verifyEmailChangeService.verifyAndChangeEmail(user.userId(), request);
        return ResponseEntity.ok().build();
    }

    // === 전화번호 변경 ===

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changePhoneNumber(
            ApiChangePhoneNumberRequest changePhoneNumberRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        ChangePhoneNumberRequest request = new ChangePhoneNumberRequest(
                changePhoneNumberRequest.getPassword(),
                changePhoneNumberRequest.getNewPhoneNumber()
        );
        changePhoneNumberService.changePhoneNumber(user.userId(), request);
        return ResponseEntity.ok().build();
    }

    // === 학번 변경 (임시 학번 -> 실제 학번) ===

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateStudentId(
            ApiUpdateStudentIdRequest updateStudentIdRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        UpdateStudentIdRequest request = new UpdateStudentIdRequest(
                updateStudentIdRequest.getPassword(),
                updateStudentIdRequest.getNewStudentId()
        );
        updateStudentIdService.updateStudentId(user.userId(), request);
        return ResponseEntity.ok().build();
    }

    // === 비밀번호 변경 ===

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> changeMyPassword(
            ApiChangePasswordRequest changePasswordRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        ChangePasswordRequest request = new ChangePasswordRequest(
                changePasswordRequest.getCurrentPassword(),
                changePasswordRequest.getNewPassword()
        );
        changeMyPasswordService.changePassword(user.userId(), request);
        return ResponseEntity.ok().build();
    }

    // === 회원 탈퇴 ===

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> withdraw(
            ApiWithdrawRequest withdrawRequest
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        WithdrawRequest request = new WithdrawRequest(
                withdrawRequest.getPassword(),
                withdrawRequest.getReason()
        );
        withdrawService.withdraw(user.userId(), request);
        return ResponseEntity.ok().build();
    }

    // === 내 활동 조회 ===

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiMyPostPageResponse> getMyPosts(
            Integer page, Integer size, List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        var postPage = getMyPostsService.getMyPosts(user.userId(), pageable);
        var pageResponse = MyPostPageResponse.from(postPage);

        return ResponseEntity.ok(new ApiMyPostPageResponse()
                .posts(pageResponse.posts().stream()
                        .map(this::mapToMyPostInner)
                        .toList())
                .totalElements(pageResponse.totalElements())
                .totalPages(pageResponse.totalPages())
                .currentPage(pageResponse.currentPage())
                .hasNext(pageResponse.hasNext()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiMyCommentPageResponse> getMyComments(
            Integer page, Integer size, List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        var commentPage = getMyCommentsService.getMyComments(user.userId(), pageable);
        var pageResponse = MyCommentPageResponse.from(commentPage);

        return ResponseEntity.ok(new ApiMyCommentPageResponse()
                .comments(pageResponse.comments().stream()
                        .map(this::mapToMyCommentInner)
                        .toList())
                .totalElements(pageResponse.totalElements())
                .totalPages(pageResponse.totalPages())
                .currentPage(pageResponse.currentPage())
                .hasNext(pageResponse.hasNext()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ApiMyRegistrationResponse>> getMyRegistrations() {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        var response = eventRegistrationService.getMyRegistrations(user.userId());
        return ResponseEntity.ok(response.stream()
                .map(this::mapToMyRegistrationInner)
                .toList());
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiLikedPostPageResponse> getMyLikes1(
            Integer page, Integer size, List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        var likePage = getMyLikedPostsService.getMyLikes(user.userId(), pageable);
        var pageResponse = LikedPostPageResponse.from(likePage);

        return ResponseEntity.ok(new ApiLikedPostPageResponse()
                .posts(pageResponse.posts().stream()
                        .map(this::mapToLikedPostInner)
                        .toList())
                .totalElements(pageResponse.totalElements())
                .totalPages(pageResponse.totalPages())
                .currentPage(pageResponse.currentPage())
                .hasNext(pageResponse.hasNext()));
    }

    @Override
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiBookmarkedPostPageResponse> getMyBookmarks1(
            Integer page, Integer size, List<String> sort
    ) {
        AuthenticatedUser user = SecurityUtils.requireCurrentUser();
        Pageable pageable = PageableUtils.of(page, size, sort);
        var bookmarkPage = getMyBookmarksService.getMyBookmarks(user.userId(), pageable);
        var pageResponse = BookmarkedPostPageResponse.from(bookmarkPage);

        return ResponseEntity.ok(new ApiBookmarkedPostPageResponse()
                .posts(pageResponse.posts().stream()
                        .map(this::mapToBookmarkedPostInner)
                        .toList())
                .totalElements(pageResponse.totalElements())
                .totalPages(pageResponse.totalPages())
                .currentPage(pageResponse.currentPage())
                .hasNext(pageResponse.hasNext()));
    }

    // === Private helper methods ===

    private ApiMyPostResponse mapToMyPostInner(MyPostResponse p) {
        return new ApiMyPostResponse()
                .id(p.id())
                .boardCode(p.boardCode())
                .boardName(p.boardName())
                .title(p.title())
                .viewCount(p.viewCount())
                .likeCount(p.likeCount())
                .isAnonymous(p.isAnonymous())
                .createdAt(p.createdAt());
    }

    private ApiMyCommentResponse mapToMyCommentInner(MyCommentResponse c) {
        return new ApiMyCommentResponse()
                .id(c.id())
                .postId(c.postId())
                .postTitle(c.postTitle())
                .content(c.content())
                .isAnonymous(c.isAnonymous())
                .isReply(c.isReply())
                .createdAt(c.createdAt());
    }

    private ApiMyRegistrationResponse mapToMyRegistrationInner(igrus.web.event.dto.response.MyRegistrationResponse r) {
        return new ApiMyRegistrationResponse()
                .registrationId(r.registrationId())
                .eventId(r.eventId())
                .eventTitle(r.eventTitle())
                .eventStartAt(r.eventStartAt())
                .status(r.status() != null
                        ? ApiMyRegistrationResponse.StatusEnum.fromValue(r.status().name()) : null)
                .registeredAt(r.registeredAt());
    }

    private ApiLikedPostResponse mapToLikedPostInner(LikedPostResponse p) {
        return new ApiLikedPostResponse()
                .postId(p.postId())
                .title(p.title())
                .boardCode(p.boardCode())
                .boardName(p.boardName())
                .authorName(p.authorName())
                .likeCount(p.likeCount())
                .createdAt(p.createdAt())
                .isDeleted(p.isDeleted())
                .deletedMessage(p.deletedMessage());
    }

    private ApiBookmarkedPostResponse mapToBookmarkedPostInner(BookmarkedPostResponse p) {
        return new ApiBookmarkedPostResponse()
                .postId(p.postId())
                .title(p.title())
                .boardCode(p.boardCode())
                .boardName(p.boardName())
                .authorName(p.authorName())
                .createdAt(p.createdAt())
                .isDeleted(p.isDeleted())
                .deletedMessage(p.deletedMessage());
    }
}
